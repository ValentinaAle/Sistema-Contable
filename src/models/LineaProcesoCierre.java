package models;

import java.math.BigDecimal;

/** Línea calculada de un asiento de Balance y cierre. */
public class LineaProcesoCierre {
    private final String cuentaCodigo;
    private final BigDecimal debe;
    private final BigDecimal haber;

    public LineaProcesoCierre(String cuentaCodigo, BigDecimal debe, BigDecimal haber) {
        this.cuentaCodigo = cuentaCodigo;
        this.debe = debe;
        this.haber = haber;
    }
    public String getCuentaCodigo() { return cuentaCodigo; }
    public BigDecimal getDebe() { return debe; }
    public BigDecimal getHaber() { return haber; }
}
