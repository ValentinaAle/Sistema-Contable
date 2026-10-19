package models;

/** Línea sintetizada de un estado contable, agrupada por rubro. */
public class LineaEstadoContable {
    private final String rubro;
    private final double importe;

    public LineaEstadoContable(String rubro, double importe) {
        this.rubro = rubro;
        this.importe = importe;
    }

    public String getRubro() { return rubro; }
    public double getImporte() { return importe; }
}
