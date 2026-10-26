import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import models.BalanceCierreCalculos;
import models.LineaProcesoCierre;

/** Pruebas ejecutables de las reglas de Balance y cierre. */
public class BalanceCierreCalculosTest {
    private static BigDecimal n(String v) { return new BigDecimal(v); }
    private static void igual(String esperado, BigDecimal recibido) {
        if (n(esperado).compareTo(recibido) != 0) throw new AssertionError("Esperado "+esperado+"; recibido "+recibido);
    }
    public static void main(String[] args) {
        if (!BalanceCierreCalculos.esMesCompleto(LocalDate.of(2026,10,1),LocalDate.of(2026,10,31)))
            throw new AssertionError("No reconoció el mes completo.");
        if (BalanceCierreCalculos.esMesCompleto(LocalDate.of(2026,10,2),LocalDate.of(2026,10,31)))
            throw new AssertionError("Aceptó un mes incompleto.");

        List<LineaProcesoCierre> iva=BalanceCierreCalculos.posicionIva("DF",n("84000"),"CF",n("58800"),"SF",n("0"),"PAGAR");
        igual("84000",iva.get(0).getDebe()); igual("58800",iva.get(1).getHaber()); igual("25200",iva.get(2).getHaber());

        List<LineaProcesoCierre> ref=BalanceCierreCalculos.refundicion(
            List.of("VENTAS","COMPRAS","ALQUILER"),List.of(n("-400000"),n("200000"),n("80000")),"RESULTADO");
        igual("400000",ref.get(0).getDebe()); igual("200000",ref.get(1).getHaber());
        igual("80000",ref.get(2).getHaber()); igual("120000",ref.get(3).getHaber());
        System.out.println("OK: mes completo, posición mensual de IVA y refundición.");
    }
}
