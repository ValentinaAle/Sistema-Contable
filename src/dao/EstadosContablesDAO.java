package dao;

import conexion.Conexion;
import models.LineaEstadoContable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/** Consultas de síntesis para los estados de exposición. No modifica datos. */
public class EstadosContablesDAO {
    public List<LineaEstadoContable> resumirPorRubro(int grupoId, int tipo, Date cierre) {
        List<LineaEstadoContable> lineas = new ArrayList<>();
        String sql = "SELECT r.nombre, COALESCE(SUM(CASE WHEN a.id IS NULL THEN 0 WHEN c.tipo_saldo IN ('D','Deudor') "
                + "THEN d.debe - d.haber ELSE d.haber - d.debe END), 0) AS importe "
                + "FROM rubros r LEFT JOIN cuentas c ON c.rubro_id = r.id "
                + "LEFT JOIN asiento_detalle d ON d.cuenta_codigo = c.codigo "
                + "LEFT JOIN asientos a ON a.id = d.asiento_id AND a.fecha <= ? "
                + "WHERE r.grupo_id = ? AND r.tipo = ? "
                + "GROUP BY r.id, r.nombre, r.codigo ORDER BY r.codigo";
        try (Connection conn = Conexion.conectar(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setDate(1, cierre);
            pst.setInt(2, grupoId);
            pst.setInt(3, tipo);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) lineas.add(new LineaEstadoContable(rs.getString(1), rs.getDouble(2)));
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el estado contable.", e);
        }
        return lineas;
    }

    public List<LineaEstadoContable> resumirPorGrupo(int grupoId, Date cierre) {
        List<LineaEstadoContable> lineas = new ArrayList<>();
        String sql = "SELECT r.nombre, COALESCE(SUM(CASE WHEN a.id IS NULL THEN 0 WHEN c.tipo_saldo IN ('D','Deudor') "
                + "THEN d.debe - d.haber ELSE d.haber - d.debe END), 0) AS importe "
                + "FROM rubros r LEFT JOIN cuentas c ON c.rubro_id = r.id "
                + "LEFT JOIN asiento_detalle d ON d.cuenta_codigo = c.codigo "
                + "LEFT JOIN asientos a ON a.id = d.asiento_id AND a.fecha <= ? "
                + "WHERE r.grupo_id = ? GROUP BY r.id, r.nombre, r.codigo ORDER BY r.codigo";
        try (Connection conn = Conexion.conectar(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setDate(1, cierre);
            pst.setInt(2, grupoId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) lineas.add(new LineaEstadoContable(rs.getString(1), rs.getDouble(2)));
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el estado contable.", e);
        }
        return lineas;
    }

    public double totalGrupo(int grupoId, Date cierre) {
        String sql = "SELECT COALESCE(SUM(CASE WHEN a.id IS NULL THEN 0 WHEN c.tipo_saldo IN ('D','Deudor') THEN d.debe - d.haber "
                + "ELSE d.haber - d.debe END), 0) FROM cuentas c LEFT JOIN asiento_detalle d ON d.cuenta_codigo = c.codigo "
                + "LEFT JOIN asientos a ON a.id = d.asiento_id AND a.fecha <= ? WHERE c.grupo_id = ?";
        try (Connection conn = Conexion.conectar(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setDate(1, cierre);
            pst.setInt(2, grupoId);
            try (ResultSet rs = pst.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el total del estado contable.", e);
        }
    }
}
