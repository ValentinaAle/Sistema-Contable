package views;

import dao.EstadosContablesDAO;
import models.LineaEstadoContable;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PrinterException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Estados de Resultados y de Situación Patrimonial. Consulta, no edita datos. */
public class EstadosContablesPanel extends JFrame {
    private static final Color AZUL = new Color(30, 80, 160), AZUL_OSCURO = new Color(20, 55, 120);
    private static final Color FONDO = new Color(245, 247, 250), BORDE = new Color(211, 222, 239);
    private final EstadosContablesDAO dao = new EstadosContablesDAO();
    private JTextField txtEmpresa, txtCierre;
    private JTable tablaResultados, tablaActivo, tablaPasivo;
    private JLabel lblResultado, lblActivo, lblPasivo;

    public EstadosContablesPanel() {
        setTitle("Estados Contables - Sistema Contable");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280, 780); setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null); setBackground(FONDO);
        construir(); actualizar(); setVisible(true);
    }

    private void construir() {
        setLayout(new BorderLayout()); add(crearHeader(), BorderLayout.NORTH);
        JPanel cuerpo = new JPanel(new BorderLayout(0, 16)); cuerpo.setBackground(FONDO);
        cuerpo.setBorder(new EmptyBorder(20, 24, 20, 24));
        cuerpo.add(crearParametros(), BorderLayout.NORTH);
        JTabbedPane pestanas = new JTabbedPane(); pestanas.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pestanas.addTab("Estado de Resultados", crearResultados());
        pestanas.addTab("Situación Patrimonial", crearSituacion());
        cuerpo.add(pestanas, BorderLayout.CENTER); add(cuerpo, BorderLayout.CENTER);
    }

    private JPanel crearHeader() {
        JPanel h = new JPanel(new BorderLayout()) { @Override protected void paintComponent(Graphics g) {
            Graphics2D x=(Graphics2D)g; x.setPaint(new GradientPaint(0,0,AZUL,getWidth(),0,AZUL_OSCURO));
            x.fillRect(0,0,getWidth(),getHeight()); x.setColor(new Color(108,170,255)); x.fillRect(0,getHeight()-3,getWidth(),3); }};
        h.setPreferredSize(new Dimension(0,88)); h.setBorder(new EmptyBorder(10,20,10,20));
        JPanel izq=new JPanel(new FlowLayout(FlowLayout.LEFT,14,0)); izq.setOpaque(false); izq.add(BarraNavegacion.crearIconoLibro());
        JPanel textos=new JPanel(); textos.setOpaque(false); textos.setLayout(new BoxLayout(textos,BoxLayout.Y_AXIS));
        JLabel t=new JLabel("ESTADOS CONTABLES"); t.setFont(new Font("Segoe UI",Font.BOLD,18)); t.setForeground(Color.WHITE);
        JLabel s=new JLabel("Exposición contable — RT 8 y RT 9"); s.setFont(new Font("Segoe UI",Font.PLAIN,11)); s.setForeground(new Color(200,215,240));
        textos.add(t); textos.add(s); izq.add(textos); h.add(izq,BorderLayout.WEST);
        h.add(BarraNavegacion.crearConControles(EstadosContablesPanel.class),BorderLayout.EAST); return h;
    }

    private JPanel crearParametros() {
        JPanel p=tarjeta(); p.setLayout(new FlowLayout(FlowLayout.LEFT,12,4));
        txtEmpresa=new JTextField("Sistema Contable S.A.",24); txtCierre=new JTextField(LocalDate.now().toString(),10);
        p.add(etiqueta("Entidad:")); p.add(txtEmpresa); p.add(etiqueta("Fecha de cierre (AAAA-MM-DD):")); p.add(txtCierre);
        JButton actualizar=boton("ACTUALIZAR",new Color(33,118,233)); actualizar.addActionListener(e->actualizar()); p.add(actualizar);
        JButton imprimir=boton("IMPRIMIR",new Color(108,117,125)); imprimir.addActionListener(e->imprimir()); p.add(imprimir);
        return p;
    }

    private JPanel crearResultados() {
        JPanel p=new JPanel(new BorderLayout(0,12)); p.setBackground(FONDO);
        tablaResultados=tabla(); p.add(new JScrollPane(tablaResultados),BorderLayout.CENTER);
        lblResultado=total("Resultado del ejercicio: $ 0,00"); p.add(lblResultado,BorderLayout.SOUTH); return p;
    }

    private JPanel crearSituacion() {
        JPanel p=new JPanel(new GridLayout(1,2,16,0)); p.setBackground(FONDO);
        JPanel a=tarjeta(); a.setLayout(new BorderLayout(0,10)); JLabel ta=titulo("ACTIVO"); a.add(ta,BorderLayout.NORTH);
        tablaActivo=tabla(); a.add(new JScrollPane(tablaActivo),BorderLayout.CENTER); lblActivo=total("TOTAL ACTIVO: $ 0,00"); a.add(lblActivo,BorderLayout.SOUTH);
        JPanel b=tarjeta(); b.setLayout(new BorderLayout(0,10)); JLabel tb=titulo("PASIVO Y PATRIMONIO NETO"); b.add(tb,BorderLayout.NORTH);
        tablaPasivo=tabla(); b.add(new JScrollPane(tablaPasivo),BorderLayout.CENTER); lblPasivo=total("TOTAL PASIVO Y PN: $ 0,00"); b.add(lblPasivo,BorderLayout.SOUTH);
        p.add(a); p.add(b); return p;
    }

    private void actualizar() {
        try {
            Date cierre=Date.valueOf(txtCierre.getText().trim());
            cargarResultados(cierre); cargarSituacion(cierre);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Ingresá una fecha válida con formato AAAA-MM-DD.","Fecha de cierre",JOptionPane.WARNING_MESSAGE); }
    }
    private void cargarResultados(Date cierre) {
        DefaultTableModel m=modelo(tablaResultados); m.setRowCount(0);
        double ingresos=agregar(m,"Resultados positivos",dao.resumirPorGrupo(4,cierre));
        double egresos=agregar(m,"Resultados negativos",dao.resumirPorGrupo(5,cierre));
        m.addRow(new Object[]{"GANANCIA (PÉRDIDA) DEL EJERCICIO", dinero(ingresos-egresos)}); lblResultado.setText("Ganancia (Pérdida) del ejercicio: " + dinero(ingresos-egresos));
    }
    private void cargarSituacion(Date cierre) {
        DefaultTableModel a=modelo(tablaActivo), p=modelo(tablaPasivo); a.setRowCount(0); p.setRowCount(0);
        double ac=agregar(a,"Activo corriente",dao.resumirPorRubro(1,1,cierre));
        double anc=agregar(a,"Activo no corriente",dao.resumirPorRubro(1,2,cierre));
        a.addRow(new Object[]{"TOTAL ACTIVO",dinero(ac+anc)}); lblActivo.setText("TOTAL ACTIVO: "+dinero(ac+anc));
        double pc=agregar(p,"Pasivo corriente",dao.resumirPorRubro(2,1,cierre));
        double pnc=agregar(p,"Pasivo no corriente",dao.resumirPorRubro(2,2,cierre));
        double pn=agregar(p,"Patrimonio neto",dao.resumirPorGrupo(3,cierre));
        double resultado=dao.totalGrupo(4,cierre)-dao.totalGrupo(5,cierre); p.addRow(new Object[]{"Resultado del ejercicio",dinero(resultado)}); pn+=resultado;
        p.addRow(new Object[]{"TOTAL PASIVO Y PATRIMONIO NETO",dinero(pc+pnc+pn)}); lblPasivo.setText("TOTAL PASIVO Y PN: "+dinero(pc+pnc+pn));
    }
    private double agregar(DefaultTableModel m,String seccion,List<LineaEstadoContable> lineas) { double total=0; m.addRow(new Object[]{seccion,""}); for(LineaEstadoContable l:lineas){m.addRow(new Object[]{"   "+l.getRubro(),dinero(l.getImporte())});total+=l.getImporte();} return total; }
    private JTable tabla(){ JTable t=new JTable(new DefaultTableModel(new Object[]{"Concepto","Importe"},0){public boolean isCellEditable(int r,int c){return false;}}); t.setRowHeight(30);t.setFont(new Font("Segoe UI",Font.PLAIN,13));t.setShowGrid(false);t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,13));t.getTableHeader().setBackground(new Color(235,241,250));return t; }
    private DefaultTableModel modelo(JTable t){return(DefaultTableModel)t.getModel();}
    private String dinero(double n){return String.format(new Locale("es","AR"),"$ %,.2f",n);}
    private JLabel etiqueta(String x){JLabel l=new JLabel(x);l.setFont(new Font("Segoe UI",Font.BOLD,12));return l;}
    private JLabel titulo(String x){JLabel l=etiqueta(x);l.setForeground(AZUL);return l;}
    private JLabel total(String x){JLabel l=new JLabel(x);l.setFont(new Font("Segoe UI",Font.BOLD,14));l.setForeground(new Color(21,96,74));return l;}
    private JPanel tarjeta(){JPanel p=new JPanel();p.setBackground(Color.WHITE);p.setBorder(new CompoundBorder(new LineBorder(BORDE,1,true),new EmptyBorder(16,18,16,18)));return p;}
    private JButton boton(String x,Color c){JButton b=new JButton(x){protected void paintComponent(Graphics g){Graphics2D z=(Graphics2D)g;z.setColor(getModel().isRollover()?c.darker():c);z.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));z.setColor(Color.WHITE);z.setFont(getFont());FontMetrics f=z.getFontMetrics();z.drawString(getText(),(getWidth()-f.stringWidth(getText()))/2,(getHeight()+f.getAscent()-f.getDescent())/2);}};b.setFont(new Font("Segoe UI",Font.BOLD,12));b.setPreferredSize(new Dimension(120,36));b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);return b;}
    private void imprimir(){try{JTable t=tablaResultados; t.print(JTable.PrintMode.FIT_WIDTH,new java.text.MessageFormat(txtEmpresa.getText()+" — Estado contable al "+txtCierre.getText()),null);}catch(PrinterException e){JOptionPane.showMessageDialog(this,"No se pudo imprimir el informe.");}}
}
