package dao;

import conexion.Conexion;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import models.Cuenta;
import models.ProcesoCierre;

/** Consultas del módulo de Balance y cierre. No actualiza saldos. */
public class BalanceCierreDAO {
    public BigDecimal movimientosDebe(String cuenta, Date desde, Date hasta) { return movimientos(cuenta, desde, hasta, "debe"); }
    public BigDecimal movimientosHaber(String cuenta, Date desde, Date hasta) { return movimientos(cuenta, desde, hasta, "haber"); }
    private BigDecimal movimientos(String cuenta, Date desde, Date hasta, String columna) {
        String sql = "SELECT COALESCE(SUM(d." + columna + "),0) FROM asiento_detalle d JOIN asientos a ON a.id=d.asiento_id "
                + "WHERE d.cuenta_codigo=? AND a.fecha BETWEEN ? AND ?";
        try (Connection c=Conexion.conectar(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1, cuenta); ps.setDate(2, desde); ps.setDate(3, hasta);
            try(ResultSet rs=ps.executeQuery()){ return rs.next()?rs.getBigDecimal(1):BigDecimal.ZERO; }
        } catch(Exception e) { throw new IllegalStateException("No se pudieron consultar los movimientos.", e); }
    }
    public BigDecimal saldoDeudorAnterior(String cuenta, LocalDate desde) {
        String sql = "SELECT COALESCE(SUM(d.debe-d.haber),0) FROM asiento_detalle d JOIN asientos a ON a.id=d.asiento_id "
                + "WHERE d.cuenta_codigo=? AND a.fecha < ?";
        try(Connection c=Conexion.conectar(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1, cuenta); ps.setDate(2, Date.valueOf(desde));
            try(ResultSet rs=ps.executeQuery()){ return rs.next()?rs.getBigDecimal(1).max(BigDecimal.ZERO):BigDecimal.ZERO; }
        } catch(Exception e) { throw new IllegalStateException("No se pudo consultar el saldo anterior.", e); }
    }
    public BigDecimal netoMes(String cuenta, Date desde, Date hasta) {
        return movimientosDebe(cuenta,desde,hasta).subtract(movimientosHaber(cuenta,desde,hasta));
    }
    public List<Cuenta> cuentasPorGrupos(int... grupos) {
        List<Cuenta> todas = new CuentaDAO().listarCuentas(), r = new ArrayList<>();
        for(Cuenta c:todas) for(int g:grupos) if(c.getGrupoId()==g) { r.add(c); break; }
        return r;
    }
    public List<ProcesoCierre> listarProcesos() {
        List<ProcesoCierre> r=new ArrayList<>();
        String sql="SELECT p.id,p.tipo,p.fecha_desde,p.fecha_hasta,p.asiento_id,a.numero FROM procesos p JOIN asientos a ON a.id=p.asiento_id ORDER BY p.fecha_hasta DESC,p.id DESC";
        try(Connection c=Conexion.conectar(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()){
            while(rs.next()) r.add(new ProcesoCierre(rs.getInt(1),rs.getString(2),rs.getDate(3),rs.getDate(4),rs.getInt(5),rs.getInt(6)));
        }catch(Exception e){throw new IllegalStateException("No se pudieron listar los procesos.",e);}
        return r;
    }
}
