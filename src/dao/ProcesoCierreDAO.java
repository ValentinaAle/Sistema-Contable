package dao;

import conexion.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;

/** Elimina el proceso y delega la reversión de saldos en AsientoDAO. */
public class ProcesoCierreDAO {
    public String eliminar(int procesoId, int asientoId) {
        String resultado = new AsientoDAO().eliminarAsiento(asientoId);
        if (!"OK".equals(resultado)) return resultado;
        // ON DELETE CASCADE elimina procesos; esta sentencia conserva compatibilidad con bases sin FK.
        try(Connection c=Conexion.conectar(); PreparedStatement ps=c.prepareStatement("DELETE FROM procesos WHERE id=?")) {
            ps.setInt(1, procesoId); ps.executeUpdate();
        } catch(Exception ignored) {}
        return "OK";
    }
}
