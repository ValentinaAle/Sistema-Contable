package dao;

import conexion.Conexion;
import models.Asiento;
import models.AsientoDetalle;
import models.Cuenta;
import models.RegistroIva;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AsientoDAO {
    private String ultimoError = "";
    public String getUltimoError() { return ultimoError; }

    // ══════════════════════════════════════════════════════════════
    //  CREAR ASIENTO (con actualización de saldos, transaccional)
    // ══════════════════════════════════════════════════════════════
    public String crearAsiento(Asiento asiento) {
        return crearAsiento(asiento, null, false);
    }

    public String crearAsiento(Asiento asiento, RegistroIva registroIva, boolean compra) {
        ultimoError = "";

        List<AsientoDetalle> detalles = asiento.getDetalles();
        if (detalles == null || detalles.isEmpty()) return "SIN_LINEAS";

        double sumaDebe = 0, sumaHaber = 0;
        for (AsientoDetalle d : detalles) {
            if (!Double.isFinite(d.getDebe()) || !Double.isFinite(d.getHaber())
                    || d.getDebe() < 0 || d.getHaber() < 0
                    || (d.getDebe() > 0 && d.getHaber() > 0)) {
                ultimoError = "Hay importes inválidos en el detalle.";
                return "ERROR";
            }
            sumaDebe += d.getDebe();
            sumaHaber += d.getHaber();
        }
        sumaDebe = Math.round(sumaDebe * 100) / 100.0;
        sumaHaber = Math.round(sumaHaber * 100) / 100.0;
        if (Math.abs(sumaDebe - sumaHaber) >= 0.005) return "DESBALANCEADO";
        if (sumaDebe <= 0 || detalles.size() < 2) return "SIN_LINEAS";
        if (registroIva != null && Math.abs(registroIva.getTotal() - sumaDebe) >= 0.005) {
            ultimoError = "El total de IVA no coincide con el asiento.";
            return "ERROR";
        }

        String sqlAsiento = "INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (?, ?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO asiento_detalle (asiento_id, cuenta_codigo, debe, haber, orden) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = Conexion.conectar();
            if (conn == null) { ultimoError = "No se pudo conectar a la base de datos."; return "ERROR"; }
            // Serializa la asignación del correlativo entre ventanas/conexiones.
            try (Statement lock = conn.createStatement();
                 ResultSet rs = lock.executeQuery("SELECT GET_LOCK(CONCAT(DATABASE(), '.asientos'), 10)")) {
                if (!rs.next() || rs.getInt(1) != 1) throw new SQLException("Hay otro asiento registrándose. Volvé a intentar.");
            }
            conn.setAutoCommit(false);

            int idGenerado;
            int siguienteNumero;
            try (PreparedStatement pstAsiento = conn.prepareStatement(sqlAsiento, Statement.RETURN_GENERATED_KEYS)) {

                siguienteNumero = obtenerSiguienteNumero(conn);

                pstAsiento.setInt(1, siguienteNumero);
                pstAsiento.setString(2, asiento.getFecha());
                pstAsiento.setString(3, asiento.getConcepto());
                pstAsiento.setDouble(4, sumaDebe);
                pstAsiento.setDouble(5, sumaHaber);
                pstAsiento.executeUpdate();

                try (ResultSet keys = pstAsiento.getGeneratedKeys()) {
                    if (keys.next()) {
                        idGenerado = keys.getInt(1);
                    } else {
                        conn.rollback();
                        return "ERROR";
                    }
                }
            }

            int orden = 1;
            try (PreparedStatement pstDetalle = conn.prepareStatement(sqlDetalle)) {
                for (AsientoDetalle d : detalles) {
                    pstDetalle.setInt(1, idGenerado);
                    pstDetalle.setString(2, d.getCuentaCodigo());
                    pstDetalle.setDouble(3, d.getDebe());
                    pstDetalle.setDouble(4, d.getHaber());
                    pstDetalle.setInt(5, orden++);
                    pstDetalle.addBatch();
                }
                pstDetalle.executeBatch();
            }

            // Actualizar saldos de las cuentas afectadas, dentro de la misma transacción
            for (AsientoDetalle d : detalles) {
                aplicarMovimiento(conn, d.getCuentaCodigo(), d.getDebe(), d.getHaber());
            }

            String numeroIva = null;
            if (registroIva != null) numeroIva = new LibroIvaDAO().registrar(conn, idGenerado, registroIva, compra);

            conn.commit();
            if (registroIva != null) registroIva.setNroComprobante(numeroIva);

            asiento.setId(idGenerado);
            asiento.setNumero(siguienteNumero);
            asiento.setTotalDebe(sumaDebe);
            asiento.setTotalHaber(sumaHaber);

            System.out.println("Asiento creado correctamente: N° " + siguienteNumero);
            return "OK";

        } catch (Exception e) {
            ultimoError = e.getMessage() == null ? "Error al guardar." : e.getMessage();
            System.out.println("Error al crear asiento");
            System.out.println(e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return "ERROR";
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Registra un asiento de cierre y su proceso en la misma transacción.
     * Los importes ya llegan calculados con BigDecimal desde el módulo de cierre.
     */
    public String crearAsientoConProceso(Asiento asiento, String origen, String tipoProceso,
                                         java.sql.Date desde, java.sql.Date hasta) {
        ultimoError = "";
        List<AsientoDetalle> detalles = asiento.getDetalles();
        if (detalles == null || detalles.size() < 2) return "SIN_LINEAS";
        double debe = 0, haber = 0;
        for (AsientoDetalle d : detalles) {
            if (d.getDebe() < 0 || d.getHaber() < 0 || (d.getDebe() > 0 && d.getHaber() > 0)) return "ERROR";
            debe += d.getDebe(); haber += d.getHaber();
        }
        if (Math.abs(debe - haber) >= 0.005 || debe <= 0) return "DESBALANCEADO";

        Connection conn = null;
        try {
            conn = Conexion.conectar();
            if (conn == null) return "ERROR";
            conn.setAutoCommit(false);
            if (!"AJUSTE".equals(tipoProceso)) {
                try (PreparedStatement existe = conn.prepareStatement(
                        "SELECT id FROM procesos WHERE tipo=? AND fecha_desde=? AND fecha_hasta=? FOR UPDATE")) {
                    existe.setString(1, tipoProceso); existe.setDate(2, desde); existe.setDate(3, hasta);
                    try (ResultSet rs = existe.executeQuery()) {
                        if (rs.next()) { conn.rollback(); ultimoError = "Ya existe un proceso de este tipo para el período."; return "DUPLICADO"; }
                    }
                }
            }
            int numero = obtenerSiguienteNumero(conn);
            int asientoId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO asientos(numero,fecha,concepto,total_debe,total_haber,origen) VALUES(?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, numero); ps.setString(2, asiento.getFecha()); ps.setString(3, asiento.getConcepto());
                ps.setDouble(4, debe); ps.setDouble(5, haber); ps.setString(6, origen); ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se pudo crear el asiento.");
                    asientoId = keys.getInt(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO asiento_detalle(asiento_id,cuenta_codigo,debe,haber,orden) VALUES(?,?,?,?,?)")) {
                int orden = 1;
                for (AsientoDetalle d : detalles) {
                    ps.setInt(1, asientoId); ps.setString(2, d.getCuentaCodigo());
                    ps.setDouble(3, d.getDebe()); ps.setDouble(4, d.getHaber()); ps.setInt(5, orden++);
                    ps.addBatch();
                    aplicarMovimiento(conn, d.getCuentaCodigo(), d.getDebe(), d.getHaber());
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO procesos(tipo,fecha_desde,fecha_hasta,asiento_id) VALUES(?,?,?,?)")) {
                ps.setString(1, tipoProceso); ps.setDate(2, desde); ps.setDate(3, hasta); ps.setInt(4, asientoId);
                ps.executeUpdate();
            }
            conn.commit();
            asiento.setId(asientoId); asiento.setNumero(numero); asiento.setTotalDebe(debe); asiento.setTotalHaber(haber);
            return "OK";
        } catch (Exception e) {
            ultimoError = e.getMessage() == null ? "No se pudo generar el proceso." : e.getMessage();
            try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
            return "ERROR";
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException ignored) {}
        }
    }

    private void aplicarMovimiento(Connection conn, String codigo, double debe, double haber) throws SQLException {
        String sql = "UPDATE cuentas SET saldo = saldo + CASE WHEN tipo_saldo IN ('D', 'Deudor') "
            + "THEN ? - ? ELSE ? - ? END WHERE codigo = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setDouble(1, debe); pst.setDouble(2, haber);
            pst.setDouble(3, haber); pst.setDouble(4, debe); pst.setString(5, codigo);
            if (pst.executeUpdate() != 1) throw new SQLException("No existe la cuenta " + codigo);
        }
    }

    // Número correlativo mostrado al usuario (nunca se reutiliza, aunque se borren asientos)
    private int obtenerSiguienteNumero(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(numero), 0) + 1 AS siguiente FROM asientos";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("siguiente");
        }
        return 1;
    }

    // Calcula el nuevo saldo de una cuenta según su naturaleza (Deudor/Acreedor)
    private double calcularNuevoSaldo(Cuenta cuenta, double debe, double haber) {
        if ("Deudor".equalsIgnoreCase(cuenta.getTipoSaldo())) {
            return cuenta.getSaldo() + debe - haber;
        } else {
            return cuenta.getSaldo() + haber - debe;
        }
    }

    private void actualizarSaldoEnTransaccion(Connection conn, String codigo, double nuevoSaldo) throws SQLException {
        String sql = "UPDATE cuentas SET saldo = ? WHERE codigo = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setDouble(1, nuevoSaldo);
            pst.setString(2, codigo);
            pst.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LISTAR / BUSCAR ASIENTOS
    // ══════════════════════════════════════════════════════════════
    public List<Asiento> listarAsientos() {
        List<Asiento> lista = new ArrayList<>();
        String sql = "SELECT id, numero, fecha, concepto, total_debe, total_haber FROM asientos ORDER BY id DESC";

        try (Connection conn = Conexion.conectar();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) lista.add(mapearAsiento(rs));

        } catch (Exception e) {
            System.out.println("Error al listar asientos");
            System.out.println(e.getMessage());
        }
        return lista;
    }

    public List<Asiento> buscarPorConcepto(String texto) {
        List<Asiento> lista = new ArrayList<>();
        String sql = "SELECT id, numero, fecha, concepto, total_debe, total_haber FROM asientos WHERE concepto LIKE ? ORDER BY id DESC";

        try (Connection conn = Conexion.conectar();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, "%" + texto + "%");
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) lista.add(mapearAsiento(rs));
            }

        } catch (Exception e) {
            System.out.println("Error al buscar asientos");
            System.out.println(e.getMessage());
        }
        return lista;
    }

    private Asiento mapearAsiento(ResultSet rs) throws SQLException {
        return new Asiento(
                rs.getInt("id"),
                rs.getInt("numero"),
                rs.getString("fecha"),
                rs.getString("concepto"),
                rs.getDouble("total_debe"),
                rs.getDouble("total_haber")
        );
    }

    // ══════════════════════════════════════════════════════════════
    //  DETALLE DE UN ASIENTO (con nombre de cuenta vía JOIN)
    // ══════════════════════════════════════════════════════════════
    public List<AsientoDetalle> listarDetalle(int asientoId) {
        List<AsientoDetalle> lista = new ArrayList<>();
        String sql = "SELECT d.id, d.asiento_id, d.cuenta_codigo, c.nombre AS cuenta_nombre, " +
                     "d.debe, d.haber, d.orden " +
                     "FROM asiento_detalle d JOIN cuentas c ON c.codigo = d.cuenta_codigo " +
                     "WHERE d.asiento_id = ? ORDER BY d.orden";

        try (Connection conn = Conexion.conectar();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, asientoId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lista.add(new AsientoDetalle(
                            rs.getInt("id"),
                            rs.getInt("asiento_id"),
                            rs.getString("cuenta_codigo"),
                            rs.getString("cuenta_nombre"),
                            rs.getDouble("debe"),
                            rs.getDouble("haber"),
                            rs.getInt("orden")
                    ));
                }
            }

        } catch (Exception e) {
            System.out.println("Error al listar detalle del asiento");
            System.out.println(e.getMessage());
        }
        return lista;
    }

    // ══════════════════════════════════════════════════════════════
    //  ELIMINAR ASIENTO (revierte saldos, transaccional)
    // ══════════════════════════════════════════════════════════════
    public String eliminarAsiento(int id) {
        Connection conn = null;
        try {
            conn = Conexion.conectar();
            if (conn == null) return "ERROR";

            conn.setAutoCommit(false);
            String sqlExiste = "SELECT id FROM asientos WHERE id = ? FOR UPDATE";
            try (PreparedStatement pstExiste = conn.prepareStatement(sqlExiste)) {
                pstExiste.setInt(1, id);
                try (ResultSet rs = pstExiste.executeQuery()) {
                    if (!rs.next()) return "NO_EXISTE";
                }
            }

            List<AsientoDetalle> detalles = new ArrayList<>();
            try (PreparedStatement pst = conn.prepareStatement("SELECT cuenta_codigo, debe, haber FROM asiento_detalle WHERE asiento_id = ?")) {
                pst.setInt(1, id);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) detalles.add(new AsientoDetalle(rs.getString(1), rs.getDouble(2), rs.getDouble(3)));
                }
            }

            for (AsientoDetalle d : detalles) {
                aplicarMovimiento(conn, d.getCuentaCodigo(), -d.getDebe(), -d.getHaber());
            }
            for (String tabla : new String[]{"iva_compras", "iva_ventas"}) {
                try (PreparedStatement pst = conn.prepareStatement("DELETE FROM " + tabla + " WHERE asiento_id = ?")) {
                    pst.setInt(1, id); pst.executeUpdate();
                }
            }

            try (PreparedStatement pstDetalle = conn.prepareStatement("DELETE FROM asiento_detalle WHERE asiento_id = ?")) {
                pstDetalle.setInt(1, id);
                pstDetalle.executeUpdate();
            }

            try (PreparedStatement pstAsiento = conn.prepareStatement("DELETE FROM asientos WHERE id = ?")) {
                pstAsiento.setInt(1, id);
                pstAsiento.executeUpdate();
            }

            conn.commit();
            return "OK";

        } catch (Exception e) {
            System.out.println("Error al eliminar asiento");
            System.out.println(e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return "ERROR";
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
