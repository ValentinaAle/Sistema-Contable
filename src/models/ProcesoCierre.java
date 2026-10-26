package models;

import java.sql.Date;

public class ProcesoCierre {
    private final int id;
    private final String tipo;
    private final Date desde;
    private final Date hasta;
    private final int asientoId;
    private final int asientoNumero;
    public ProcesoCierre(int id, String tipo, Date desde, Date hasta, int asientoId, int asientoNumero) {
        this.id=id; this.tipo=tipo; this.desde=desde; this.hasta=hasta; this.asientoId=asientoId; this.asientoNumero=asientoNumero;
    }
    public int getId(){return id;} public String getTipo(){return tipo;} public Date getDesde(){return desde;}
    public Date getHasta(){return hasta;} public int getAsientoId(){return asientoId;} public int getAsientoNumero(){return asientoNumero;}
}
