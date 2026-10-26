package views;

import dao.AsientoDAO;
import dao.BalanceCierreDAO;
import dao.CuentaDAO;
import dao.ProcesoCierreDAO;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import models.Asiento;
import models.AsientoDetalle;
import models.BalanceCierreCalculos;
import models.Cuenta;
import models.LineaProcesoCierre;
import models.ProcesoCierre;

/** Procesos mensuales de IVA, refundición y ajustes de cierre. */
public class BalanceCierrePanel extends JFrame {
    private static final Color AZUL = new Color(30,80,160), FONDO = new Color(245,247,250);
    private final CuentaDAO cuentaDAO=new CuentaDAO();
    private final BalanceCierreDAO balanceDAO=new BalanceCierreDAO();
    private final AsientoDAO asientoDAO=new AsientoDAO();
    private final List<Cuenta> cuentas=new ArrayList<>();
    private final SelectorFecha selectorDesde=new SelectorFecha(), selectorHasta=new SelectorFecha();
    private final JTextField txtConcepto=new JTextField();
    private final JComboBox<String> cmbProceso=new JComboBox<>(new String[]{"Posición mensual de IVA","Refundición de resultados","Asiento de ajuste"});
    private final JComboBox<String> cmbCredito=new JComboBox<>(), cmbDebito=new JComboBox<>(), cmbFavor=new JComboBox<>(), cmbPagar=new JComboBox<>(), cmbResultado=new JComboBox<>();
    private final CardLayout formulariosLayout=new CardLayout(); private final JPanel formularios=new JPanel(formulariosLayout);
    private final DefaultTableModel modeloVista=modelo("Cuenta","Debe","Haber");
    private final DefaultTableModel modeloManual=modelo("Cuenta","Debe","Haber");
    private final DefaultTableModel modeloProcesos=modelo("Proceso","Inicio","Cierre","N° de asiento");
    private final JTable tablaVista=new JTable(modeloVista), tablaManual=new JTable(modeloManual), tablaProcesos=new JTable(modeloProcesos);
    private final JLabel lblMensaje=new JLabel("Seleccione un mes completo. Se cancelan crédito y débito fiscal, se aplica el saldo a favor anterior y se determina el saldo a pagar o a favor.");
    private final JLabel lblBalance=new JLabel("Sin vista previa.");
    private final JButton btnPreparar=botonAccion("Preparar vista previa", AZUL);
    private final JButton btnGenerar=botonAccion("Generar asiento", new Color(40, 140, 85));
    private final JButton btnDescartar=botonAccion("Descartar", new Color(108, 117, 125));
    private List<LineaProcesoCierre> previa=new ArrayList<>();
    private List<ProcesoCierre> procesos=new ArrayList<>();

    public BalanceCierrePanel() {
        setTitle("Balance y cierre - Sistema Contable"); setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280,800); setMinimumSize(new Dimension(1000,680)); setLocationRelativeTo(null);
        LocalDate hoy=LocalDate.now(); YearMonth mes=YearMonth.from(hoy);
        selectorDesde.setFecha(mes.atDay(1));
        selectorHasta.setFecha(mes.atEndOfMonth());
        selectorDesde.setAlElegir(fecha -> { if (fecha.getDayOfMonth() == 1) selectorHasta.setFecha(fecha.withDayOfMonth(fecha.lengthOfMonth())); });
        cargarCuentas(); construir(); cargarProcesos(); setVisible(true);
    }
    private void construir() {
        JPanel raiz=new JPanel(new BorderLayout()); raiz.setBackground(FONDO);
        raiz.add(header(),BorderLayout.NORTH);
        JPanel cuerpo=new JPanel(new BorderLayout(0,12)); cuerpo.setBackground(FONDO); cuerpo.setBorder(new EmptyBorder(18,24,18,24));
        JPanel superior=new JPanel(); superior.setOpaque(false); superior.setLayout(new BoxLayout(superior,BoxLayout.Y_AXIS));
        superior.add(filtros()); superior.add(Box.createVerticalStrut(8)); superior.add(formulario());
        superior.add(Box.createVerticalStrut(8)); superior.add(barraAcciones());
        cuerpo.add(superior,BorderLayout.NORTH);
        cuerpo.add(vistaPrevia(),BorderLayout.CENTER); cuerpo.add(procesosRegistrados(),BorderLayout.SOUTH);
        raiz.add(cuerpo); setContentPane(raiz);
    }
    private JPanel header() {
        JPanel p=new JPanel(new BorderLayout()); p.setBackground(AZUL); p.setBorder(new EmptyBorder(12,22,12,18));
        JPanel izq=new JPanel(new FlowLayout(FlowLayout.LEFT,14,0)); izq.setOpaque(false);
        JLabel t=new JLabel("Balance y cierre  ·  Procesos y asientos"); t.setForeground(Color.WHITE); t.setFont(new Font("Segoe UI",Font.BOLD,20));
        izq.add(BarraNavegacion.crearIconoLibro()); izq.add(t);
        p.add(izq,BorderLayout.WEST); p.add(BarraNavegacion.crear(BalanceCierrePanel.class),BorderLayout.EAST); return p;
    }
    private JPanel filtros() {
        JPanel p=card(new FlowLayout(FlowLayout.LEFT,10,9));
        p.add(label("Desde")); selectorDesde.setPreferredSize(new Dimension(145,30)); p.add(selectorDesde);
        p.add(label("Hasta")); selectorHasta.setPreferredSize(new Dimension(145,30)); p.add(selectorHasta);
        p.add(label("Proceso")); cmbProceso.setPreferredSize(new Dimension(220,30)); p.add(cmbProceso);
        cmbProceso.addActionListener(e->cambiarProceso()); return p;
    }
    private JPanel formulario() {
        JPanel p=card(new BorderLayout(0,8)); p.setBorder(new EmptyBorder(13,15,13,15));
        lblMensaje.setForeground(new Color(75,85,99)); lblMensaje.setFont(new Font("Segoe UI",Font.PLAIN,12)); p.add(lblMensaje,BorderLayout.NORTH);
        formularios.setOpaque(false); formularios.add(formIva(),"IVA"); formularios.add(formRefundicion(),"REF"); formularios.add(formAjuste(),"AJU");
        p.add(formularios,BorderLayout.CENTER); return p;
    }
    private JPanel formIva() {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,10,4)); p.setOpaque(false);
        p.add(label("IVA Crédito Fiscal")); p.add(combo(cmbCredito)); p.add(label("IVA Débito Fiscal")); p.add(combo(cmbDebito));
        p.add(label("IVA Saldo a favor")); p.add(combo(cmbFavor)); p.add(label("IVA a pagar")); p.add(combo(cmbPagar)); return p;
    }
    private JPanel formRefundicion() {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,10,4)); p.setOpaque(false);
        p.add(label("Cuenta resultado del ejercicio")); p.add(combo(cmbResultado)); return p;
    }
    private JPanel formAjuste() {
        JPanel p=new JPanel(new BorderLayout(8,4)); p.setOpaque(false);
        JPanel superior=new JPanel(new FlowLayout(FlowLayout.LEFT,10,2)); superior.setOpaque(false);
        superior.add(label("Concepto")); txtConcepto.setPreferredSize(new Dimension(440,28)); superior.add(txtConcepto);
        JButton agregar=new JButton("Agregar línea"); agregar.addActionListener(e->modeloManual.addRow(new Object[]{cuentas.isEmpty()?"":texto(cuentas.get(0)),"",""})); superior.add(agregar);
        p.add(superior,BorderLayout.NORTH); configurarTablaManual(); JScrollPane scroll=new JScrollPane(tablaManual); scroll.setPreferredSize(new Dimension(0,110)); p.add(scroll,BorderLayout.CENTER);
        if(modeloManual.getRowCount()==0) modeloManual.addRow(new Object[]{cuentas.isEmpty()?"":texto(cuentas.get(0)),"",""});
        return p;
    }
    private JPanel vistaPrevia() {
        JPanel p=card(new BorderLayout(0,8)); p.setBorder(new EmptyBorder(13,15,13,15));
        JLabel titulo=new JLabel("Vista previa del asiento"); titulo.setFont(new Font("Segoe UI",Font.BOLD,14)); p.add(titulo,BorderLayout.NORTH);
        p.add(new JScrollPane(tablaVista),BorderLayout.CENTER); return p;
    }
    private JPanel barraAcciones() {
        JPanel p=card(new BorderLayout()); p.setBorder(new EmptyBorder(8,12,8,12));
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); botones.setOpaque(false);
        btnPreparar.addActionListener(e->preparar()); btnGenerar.addActionListener(e->generar());
        btnDescartar.addActionListener(e->descartar());
        btnGenerar.setEnabled(false);
        botones.add(btnPreparar);botones.add(btnGenerar);botones.add(btnDescartar);
        lblBalance.setFont(new Font("Segoe UI",Font.BOLD,12)); lblBalance.setForeground(new Color(100,116,139));
        p.add(botones,BorderLayout.WEST);p.add(lblBalance,BorderLayout.EAST);return p;
    }
    private JPanel procesosRegistrados() {
        JPanel p=card(new BorderLayout(0,7)); p.setBorder(new EmptyBorder(11,15,11,15)); p.setPreferredSize(new Dimension(0,185));
        JLabel t=new JLabel("Procesos registrados"); t.setFont(new Font("Segoe UI",Font.BOLD,14)); p.add(t,BorderLayout.NORTH); p.add(new JScrollPane(tablaProcesos),BorderLayout.CENTER);
        JPanel acciones=new JPanel(new FlowLayout(FlowLayout.RIGHT,7,0)); acciones.setOpaque(false);
        JButton ver=new JButton("Ver asiento"); ver.addActionListener(e->verAsiento()); JButton eliminar=new JButton("Eliminar proceso"); eliminar.addActionListener(e->eliminarProceso());
        acciones.add(ver);acciones.add(eliminar);p.add(acciones,BorderLayout.SOUTH); return p;
    }
    private void cambiarProceso() {
        int i=cmbProceso.getSelectedIndex();
        formulariosLayout.show(formularios,i==0?"IVA":i==1?"REF":"AJU");
        lblMensaje.setText(i==0?"Seleccione un mes completo. Se cancelan crédito y débito fiscal, se aplica el saldo a favor anterior y se determina el saldo a pagar o a favor."
                :i==1?"Cancela las cuentas de ingresos y egresos contra resultado del ejercicio."
                :"Ingrese concepto y líneas del ajuste. La fecha debe estar dentro del mes seleccionado.");
    }
    private void preparar() {
        LocalDate[] periodo=periodo(); if(periodo==null)return;
        try {
            int tipo=cmbProceso.getSelectedIndex(); previa=tipo==0?calcularIva(periodo):tipo==1?calcularRefundicion(periodo):calcularAjuste(periodo);
            cargarVista();
        } catch(IllegalArgumentException ex) { previa.clear(); cargarVista(); aviso(ex.getMessage()); }
        catch(RuntimeException ex) { previa.clear(); cargarVista(); aviso("No se pudo preparar el asiento: "+ex.getMessage()); }
    }
    private List<LineaProcesoCierre> calcularIva(LocalDate[] p) {
        String cr=codigo(cmbCredito), db=codigo(cmbDebito), sf=codigo(cmbFavor), pa=codigo(cmbPagar); distintas(cr,db,sf,pa);
        java.sql.Date d=java.sql.Date.valueOf(p[0]), h=java.sql.Date.valueOf(p[1]);
        BigDecimal DF=balanceDAO.movimientosHaber(db,d,h).subtract(balanceDAO.movimientosDebe(db,d,h));
        BigDecimal CF=balanceDAO.movimientosDebe(cr,d,h).subtract(balanceDAO.movimientosHaber(cr,d,h));
        BigDecimal ant=balanceDAO.saldoDeudorAnterior(sf,p[0]);
        return BalanceCierreCalculos.posicionIva(db,DF,cr,CF,sf,ant,pa);
    }
    private List<LineaProcesoCierre> calcularRefundicion(LocalDate[] p) {
        String res=codigo(cmbResultado); if(res.isEmpty())throw new IllegalArgumentException("Seleccione la cuenta resultado del ejercicio.");
        List<Cuenta> cs=balanceDAO.cuentasPorGrupos(4,5); List<String> codigos=new ArrayList<>(); List<BigDecimal> netos=new ArrayList<>();
        for(Cuenta c:cs){codigos.add(c.getCodigo());netos.add(balanceDAO.netoMes(c.getCodigo(),java.sql.Date.valueOf(p[0]),java.sql.Date.valueOf(p[1])));}
        return BalanceCierreCalculos.refundicion(codigos,netos,res);
    }
    private List<LineaProcesoCierre> calcularAjuste(LocalDate[] p) {
        if(txtConcepto.getText().trim().isEmpty())throw new IllegalArgumentException("Ingrese un concepto para el ajuste.");
        List<LineaProcesoCierre> r=new ArrayList<>(); BigDecimal d=BigDecimal.ZERO,h=BigDecimal.ZERO;
        for(int i=0;i<modeloManual.getRowCount();i++){BigDecimal debe=importe(modeloManual.getValueAt(i,1)),haber=importe(modeloManual.getValueAt(i,2));
            if(debe.signum()>0&&haber.signum()>0)throw new IllegalArgumentException("Cada línea debe tener importe solo en Debe o en Haber.");
            if(debe.signum()>0||haber.signum()>0){r.add(new LineaProcesoCierre(codigo(String.valueOf(modeloManual.getValueAt(i,0))),debe,haber));d=d.add(debe);h=h.add(haber);}
        } if(r.size()<2||d.compareTo(h)!=0)throw new IllegalArgumentException("El ajuste debe tener al menos dos líneas y Debe = Haber."); return r;
    }
    private void generar() {
        LocalDate[] p=periodo(); if(p==null)return; if(previa.isEmpty()){aviso("Primero prepare una vista previa balanceada.");return;}
        BigDecimal d=BigDecimal.ZERO,h=BigDecimal.ZERO; List<AsientoDetalle> detalle=new ArrayList<>();
        for(LineaProcesoCierre l:previa){d=d.add(l.getDebe());h=h.add(l.getHaber());detalle.add(new AsientoDetalle(l.getCuentaCodigo(),l.getDebe().doubleValue(),l.getHaber().doubleValue()));}
        if(d.compareTo(h)!=0){aviso("El asiento no está balanceado.");return;}
        int t=cmbProceso.getSelectedIndex(); String tipo=t==0?"POSICION_IVA":t==1?"REFUNDICION":"AJUSTE";
        String concepto=t==0?"Posición mensual de IVA":t==1?"Refundición de resultados":txtConcepto.getText().trim();
        Asiento a=new Asiento(p[1].toString(),concepto,detalle);
        String r=asientoDAO.crearAsientoConProceso(a,tipo,tipo,java.sql.Date.valueOf(p[0]),java.sql.Date.valueOf(p[1]));
        if("OK".equals(r)){descartar();cargarProcesos();aviso("Proceso registrado. Asiento N° "+a.getNumero());}
        else aviso("DUPLICADO".equals(r)?asientoDAO.getUltimoError():"No se pudo generar el asiento: "+asientoDAO.getUltimoError());
    }
    private LocalDate[] periodo() {
        try { LocalDate d=selectorDesde.getFecha(),h=selectorHasta.getFecha();
            if(!BalanceCierreCalculos.esMesCompleto(d,h)){aviso("Seleccione un mes completo.");return null;}return new LocalDate[]{d,h};
        }catch(Exception e){aviso("Seleccione un mes completo.");return null;}
    }
    private void cargarCuentas() {
        cuentas.addAll(cuentaDAO.listarCuentas()); for(Cuenta c:cuentas){String t=texto(c);cmbCredito.addItem(t);cmbDebito.addItem(t);cmbFavor.addItem(t);cmbPagar.addItem(t);if(c.getGrupoId()==3)cmbResultado.addItem(t);}
    }
    private void cargarVista(){modeloVista.setRowCount(0);BigDecimal d=BigDecimal.ZERO,h=BigDecimal.ZERO;for(LineaProcesoCierre l:previa){modeloVista.addRow(new Object[]{nombre(l.getCuentaCodigo()),l.getDebe(),l.getHaber()});d=d.add(l.getDebe());h=h.add(l.getHaber());}if(!previa.isEmpty())modeloVista.addRow(new Object[]{"TOTALES",d,h});if(previa.isEmpty()){btnGenerar.setEnabled(false);lblBalance.setText("Sin vista previa.");lblBalance.setForeground(new Color(100,116,139));return;}boolean ok=d.compareTo(h)==0;btnGenerar.setEnabled(ok);lblBalance.setText(ok?"Balanceado":"Desbalanceado");lblBalance.setForeground(ok?new Color(21,128,61):new Color(180,50,50));}
    private void descartar(){previa.clear();modeloVista.setRowCount(0);btnGenerar.setEnabled(false);lblBalance.setText("Sin vista previa.");lblBalance.setForeground(new Color(100,116,139));}
    private void cargarProcesos(){procesos=balanceDAO.listarProcesos();modeloProcesos.setRowCount(0);for(ProcesoCierre p:procesos)modeloProcesos.addRow(new Object[]{p.getTipo(),p.getDesde(),p.getHasta(),p.getAsientoNumero()});}
    private void verAsiento(){int i=tablaProcesos.getSelectedRow();if(i<0){aviso("Seleccione un proceso.");return;} StringBuilder s=new StringBuilder("Asiento N° "+procesos.get(i).getAsientoNumero()+"\n\n");for(AsientoDetalle d:asientoDAO.listarDetalle(procesos.get(i).getAsientoId()))s.append(d.getCuentaCodigo()).append(" - ").append(d.getCuentaNombre()).append("   Debe ").append(d.getDebe()).append("   Haber ").append(d.getHaber()).append("\n");JOptionPane.showMessageDialog(this,s.toString(),"Asiento",JOptionPane.INFORMATION_MESSAGE);}
    private void eliminarProceso(){int i=tablaProcesos.getSelectedRow();if(i<0){aviso("Seleccione un proceso.");return;}ProcesoCierre p=procesos.get(i);if(!"OK".equals(new ProcesoCierreDAO().eliminar(p.getId(),p.getAsientoId()))){aviso("No se pudo eliminar el proceso.");return;}cargarProcesos();}
    private void configurarTablaManual(){JComboBox<String> c=new JComboBox<>();for(Cuenta x:cuentas)c.addItem(texto(x));tablaManual.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(c));}
    private DefaultTableModel modelo(String... columnas){return new DefaultTableModel(columnas,0){public boolean isCellEditable(int r,int c){return true;}};}
    private JPanel card(LayoutManager l){JPanel p=new JPanel(l);p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createLineBorder(new Color(211,222,239)));return p;}
    private JLabel label(String t){JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",Font.BOLD,12));return l;}
    private JTextField campo(JTextField t,int ancho){t.setPreferredSize(new Dimension(ancho,30));return t;}
    private JComboBox<String> combo(JComboBox<String> c){c.setPreferredSize(new Dimension(180,30));return c;}
    private String texto(Cuenta c){return c.getCodigo()+" - "+c.getNombre();}
    private String codigo(JComboBox<String> c){return codigo(String.valueOf(c.getSelectedItem()));}
    private String codigo(String s){return s==null||s.equals("null")?"":s.split(" - ",2)[0].trim();}
    private String nombre(String cod){for(Cuenta c:cuentas)if(c.getCodigo().equals(cod))return texto(c);return cod;}
    private BigDecimal importe(Object x){try{return new BigDecimal(String.valueOf(x).trim().replace(",","."));}catch(Exception e){return BigDecimal.ZERO;}}
    private void distintas(String... v){Set<String>s=new HashSet<>();for(String x:v)if(x.isEmpty()||!s.add(x))throw new IllegalArgumentException("Las cuatro cuentas de IVA deben ser distintas.");}
    private void aviso(String s){JOptionPane.showMessageDialog(this,s,"Balance y cierre",JOptionPane.INFORMATION_MESSAGE);}
    private static JButton botonAccion(String texto, Color color){
        JButton b=new JButton(texto){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color fondo=!isEnabled()?new Color(155,165,180):(getModel().isPressed()?color.darker():getModel().isRollover()?color.brighter():color);
                g2.setColor(fondo); g2.fillRoundRect(0,0,getWidth(),getHeight(),9,9);
                g2.setColor(new Color(255,255,255,120)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,9,9);
                g2.setColor(Color.WHITE); g2.setFont(getFont());
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI",Font.BOLD,12));
        b.setPreferredSize(new Dimension(texto.equals("Preparar vista previa")?170:130,36));
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
