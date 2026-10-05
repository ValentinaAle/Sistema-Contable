import dao.EstadosContablesDAO;
import java.sql.Date;

/** Verifica la síntesis de resultados y situación patrimonial por fecha de cierre. */
public class EstadosContablesTest {
    private static void igual(double esperado, double real, String texto) {
        if (Math.abs(esperado - real) > 0.005) throw new AssertionError(texto + ": " + real);
    }

    public static void main(String[] args) {
        EstadosContablesDAO dao = new EstadosContablesDAO();
        Date cierre = Date.valueOf("2026-10-07");
        igual(4000, dao.totalGrupo(4, cierre), "Ingresos");
        igual(2000, dao.totalGrupo(5, cierre), "Egresos");
        igual(4910, dao.totalGrupo(1, cierre), "Activo");
        igual(2910, dao.totalGrupo(2, cierre), "Pasivo");
        if (dao.resumirPorRubro(1, 1, cierre).isEmpty()) throw new AssertionError("Activo corriente vacío");
        if (dao.resumirPorGrupo(4, cierre).isEmpty()) throw new AssertionError("Resultados positivos vacíos");
        System.out.println("OK: estados contables sintetizados por rubro y fecha de cierre.");
    }
}
