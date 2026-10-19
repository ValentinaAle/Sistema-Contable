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
        if (dao.resumirPorRubro(1, 1, cierre).size() != 6) throw new AssertionError("Faltan rubros de activo corriente.");
        if (dao.resumirPorRubro(1, 2, cierre).size() != 8) throw new AssertionError("Faltan rubros de activo no corriente.");
        if (dao.resumirPorRubro(2, 1, cierre).size() != 7) throw new AssertionError("Faltan rubros de pasivo corriente.");
        if (dao.resumirPorRubro(2, 2, cierre).size() != 5) throw new AssertionError("Faltan rubros de pasivo no corriente.");
        if (dao.resumirPorGrupo(4, cierre).size() != 6) throw new AssertionError("Faltan rubros de resultados positivos.");
        if (dao.resumirPorGrupo(5, cierre).size() != 7) throw new AssertionError("Faltan rubros de resultados negativos.");
        boolean inversionesEnCero = dao.resumirPorRubro(1, 1, cierre).stream()
                .anyMatch(l -> "Inversiones".equals(l.getRubro()) && l.getImporte() == 0);
        if (!inversionesEnCero) throw new AssertionError("Un rubro sin saldo debe figurar en $ 0,00.");
        System.out.println("OK: estados contables incluyen todos los rubros, incluso los de saldo cero.");
    }
}
