package models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Reglas puras de Balance y cierre, para reutilizar desde UI y tests. */
public final class BalanceCierreCalculos {
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);
    private BalanceCierreCalculos() {}
    public static boolean esMesCompleto(LocalDate desde, LocalDate hasta) {
        return desde != null && hasta != null && desde.getDayOfMonth() == 1
                && desde.getYear() == hasta.getYear() && desde.getMonth() == hasta.getMonth()
                && hasta.equals(desde.withDayOfMonth(desde.lengthOfMonth()));
    }
    public static List<LineaProcesoCierre> posicionIva(String debito, BigDecimal df, String credito, BigDecimal cf,
            String saldoFavor, BigDecimal sfAnterior, String ivaPagar) {
        df = n(df); cf = n(cf); sfAnterior = n(sfAnterior).max(CERO);
        BigDecimal neto = df.subtract(cf).subtract(sfAnterior).setScale(2, RoundingMode.HALF_UP);
        List<LineaProcesoCierre> r = new ArrayList<>();
        agregar(r, debito, df, CERO);
        agregar(r, credito, CERO, cf);
        if (neto.signum() > 0) {
            agregar(r, ivaPagar, CERO, neto);
            agregar(r, saldoFavor, CERO, sfAnterior);
        } else {
            BigDecimal cambio = neto.negate().subtract(sfAnterior);
            agregar(r, saldoFavor, cambio.max(CERO), cambio.negate().max(CERO));
        }
        return r;
    }
    public static List<LineaProcesoCierre> refundicion(List<String> codigos, List<BigDecimal> netos, String resultado) {
        List<LineaProcesoCierre> r = new ArrayList<>();
        BigDecimal diferencia = CERO;
        for (int i=0;i<codigos.size();i++) {
            BigDecimal neto = n(netos.get(i));
            if (neto.signum() > 0) agregar(r, codigos.get(i), CERO, neto);
            else if (neto.signum() < 0) agregar(r, codigos.get(i), neto.negate(), CERO);
            diferencia = diferencia.add(neto);
        }
        // La suma de líneas anteriores es -diferencia; se completa contra resultado.
        agregar(r, resultado, diferencia.max(CERO), diferencia.negate().max(CERO));
        return r;
    }
    private static void agregar(List<LineaProcesoCierre> r, String cuenta, BigDecimal debe, BigDecimal haber) {
        if (debe.signum()!=0 || haber.signum()!=0) r.add(new LineaProcesoCierre(cuenta, n(debe), n(haber)));
    }
    private static BigDecimal n(BigDecimal n) { return n == null ? CERO : n.setScale(2, RoundingMode.HALF_UP); }
}
