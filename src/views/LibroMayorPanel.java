package views;

import dao.CuentaDAO;
import dao.LibroMayorDAO;
import models.Cuenta;
import models.FilaMayor;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PrinterException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Panel del Libro Mayor - Sistema Contable.
 * Muestra, para una cuenta elegida, el historial de movimientos
 * (Fecha, Asiento, Detalle, Debe, Haber, Saldo) con saldo corrido,
 * y permite imprimirlo.
 */
public class LibroMayorPanel extends JFrame {

    // ── Paleta de colores (misma estética que el resto del sistema) ────────────
    private static final Color AZUL_HEADER   = new Color(30,  80, 160);
    private static final Color AZUL_OSCURO   = new Color(20,  55, 120);
    private static final Color AZUL_BOTON    = new Color(33, 118, 233);
    private static final Color GRIS_BOTON    = new Color(108, 117, 125);
    private static final Color FONDO_PANEL   = new Color(245, 247, 250);
    private static final Color BORDE_COLOR   = new Color(220, 225, 235);
    private static final Color AZUL_TAB      = new Color(33, 118, 233);
    private static final Color TEXTO_LABEL   = new Color( 50,  60,  80);
    private static final Color FONDO_CAMPO   = new Color(250, 251, 253);
    private static final Color FILA_PAR      = Color.WHITE;
    private static final Color FILA_IMPAR    = new Color(248, 250, 253);
    private static final Color CABECERA_TABLA= new Color(240, 243, 250);
    private static final Color VERDE_TEXTO   = new Color( 21, 128,  61);
    private static final Color ROJO_TEXTO    = new Color(155,  28,  48);

    private static final Font FUENTE_TITULO  = new Font("Segoe UI", Font.BOLD,  18);
    private static final Font FUENTE_SUBTIT  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FUENTE_SECCION = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FUENTE_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_CAMPO   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_BOTON   = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FUENTE_TABLA   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_CAB     = new Font("Segoe UI", Font.BOLD,  13);

    private final CuentaDAO cuentaDAO = new CuentaDAO();
    private final LibroMayorDAO mayorDAO = new LibroMayorDAO();

    private JComboBox<String> cboCuenta;
    private List<Cuenta> cuentasDisponibles;
    private JTable tablaMayor;
    private DefaultTableModel modeloMayor;
    private JLabel lblCuentaActual;

    public LibroMayorPanel() {
        configurarVentana();
        construirUI();
        cargarCuentas();
        setVisible(true);
    }

    private void configurarVentana() {
        setTitle("Libro Mayor - Sistema Contable");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        setBackground(FONDO_PANEL);
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
    }

    private void construirUI() {
        setLayout(new BorderLayout());
        add(crearHeader(), BorderLayout.NORTH);

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBackground(FONDO_PANEL);
        contenido.setBorder(new EmptyBorder(20, 24, 20, 24));
        contenido.add(crearBarraSeleccion(), BorderLayout.NORTH);
        contenido.add(crearTablaMayor(), BorderLayout.CENTER);

        add(contenido, BorderLayout.CENTER);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, AZUL_HEADER, getWidth(), 0, AZUL_OSCURO);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 88));
        header.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        izq.setOpaque(false);

        JPanel titulos = new JPanel();
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));
        titulos.setOpaque(false);
        JLabel lblTitulo = new JLabel("LIBRO MAYOR");
        lblTitulo.setFont(FUENTE_TITULO);
        lblTitulo.setForeground(Color.WHITE);
        JLabel lblSubtitulo = new JLabel("Sistema Contable");
        lblSubtitulo.setFont(FUENTE_SUBTIT);
        lblSubtitulo.setForeground(new Color(200, 215, 240));
        titulos.add(lblTitulo);
        titulos.add(lblSubtitulo);

        izq.add(BarraNavegacion.crearIconoLibro());
        izq.add(titulos);
        header.add(izq, BorderLayout.WEST);
        header.add(BarraNavegacion.crear(LibroMayorPanel.class), BorderLayout.EAST);
        return header;
    }

    private JPanel crearBarraSeleccion() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));

        JLabel lbl = new JLabel("Cuenta:");
        lbl.setFont(FUENTE_LABEL);
        lbl.setForeground(TEXTO_LABEL);

        cboCuenta = new JComboBox<>();
        cboCuenta.setFont(FUENTE_CAMPO);
        cboCuenta.setPreferredSize(new Dimension(420, 38));
        cboCuenta.setBackground(FONDO_CAMPO);

        JButton btnGenerar = crearBoton("GENERAR", AZUL_BOTON);
        btnGenerar.setPreferredSize(new Dimension(160, 40));
        btnGenerar.addActionListener(e -> generarMayor());

        JButton btnImprimir = crearBoton("IMPRIMIR", GRIS_BOTON);
        btnImprimir.setPreferredSize(new Dimension(160, 40));
        btnImprimir.addActionListener(e -> imprimirMayor());

        lblCuentaActual = new JLabel("");
        lblCuentaActual.setFont(FUENTE_SECCION);
        lblCuentaActual.setForeground(AZUL_TAB);

        tarjeta.add(lbl);
        tarjeta.add(cboCuenta);
        tarjeta.add(btnGenerar);
        tarjeta.add(btnImprimir);
        tarjeta.add(Box.createHorizontalStrut(20));
        tarjeta.add(lblCuentaActual);

        return tarjeta;
    }

    private JPanel crearTablaMayor() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.setLayout(new BorderLayout());

        String[] columnas = {"Fecha", "Asiento", "Detalle", "Debe", "Haber", "Saldo"};
        modeloMayor = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaMayor = new JTable(modeloMayor) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? FILA_PAR : FILA_IMPAR);
                return c;
            }
        };
        tablaMayor.setFont(FUENTE_TABLA);
        tablaMayor.setRowHeight(34);
        tablaMayor.setShowGrid(false);
        tablaMayor.setIntercellSpacing(new Dimension(0, 0));
        tablaMayor.setFillsViewportHeight(true);
        tablaMayor.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = tablaMayor.getTableHeader();
        header.setFont(FUENTE_CAB);
        header.setBackground(CABECERA_TABLA);
        header.setForeground(TEXTO_LABEL);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDE_COLOR));
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        tablaMayor.getColumnModel().getColumn(3).setCellRenderer(derecha);
        tablaMayor.getColumnModel().getColumn(4).setCellRenderer(derecha);
        tablaMayor.getColumnModel().getColumn(5).setCellRenderer(derecha);

        tablaMayor.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaMayor.getColumnModel().getColumn(1).setPreferredWidth(70);
        tablaMayor.getColumnModel().getColumn(2).setPreferredWidth(340);
        tablaMayor.getColumnModel().getColumn(3).setPreferredWidth(110);
        tablaMayor.getColumnModel().getColumn(4).setPreferredWidth(110);
        tablaMayor.getColumnModel().getColumn(5).setPreferredWidth(130);

        JScrollPane scroll = new JScrollPane(tablaMayor);
        scroll.setBorder(new LineBorder(BORDE_COLOR, 1, true));
        scroll.getViewport().setBackground(Color.WHITE);

        tarjeta.add(scroll, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel crearTarjeta() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDE_COLOR, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));
        return p;
    }

    private JButton crearBoton(String texto, Color bg) {
        JButton btn = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isRollover() ? bg.darker() : bg;
                g2.setColor(c);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
            }
        };
        btn.setFont(FUENTE_BOTON);
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARGA DE CUENTAS Y GENERACIÓN DEL MAYOR
    // ══════════════════════════════════════════════════════════════════════════
    private void cargarCuentas() {
        cuentasDisponibles = cuentaDAO.listarCuentas();
        cboCuenta.removeAllItems();
        for (Cuenta c : cuentasDisponibles) {
            cboCuenta.addItem(c.getCodigo() + " - " + c.getNombre());
        }
        if (cboCuenta.getItemCount() > 0) {
            cboCuenta.setSelectedIndex(0);
            generarMayor();
        }
    }

    private void generarMayor() {
        String seleccion = (String) cboCuenta.getSelectedItem();
        if (seleccion == null || seleccion.isEmpty()) return;

        String codigoCuenta = seleccion.split(" - ", 2)[0].trim();
        List<FilaMayor> filas = mayorDAO.generarMayor(codigoCuenta);

        modeloMayor.setRowCount(0);
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

        for (FilaMayor f : filas) {
            modeloMayor.addRow(new Object[]{
                    formatoFecha.format(f.getFecha()),
                    f.getNumeroAsiento(),
                    f.getDetalle(),
                    f.getDebe() == 0 ? "" : formatear(f.getDebe()),
                    f.getHaber() == 0 ? "" : formatear(f.getHaber()),
                    formatear(f.getSaldo()) + " " + f.getNaturalezaSaldo()
            });
        }

        lblCuentaActual.setText(seleccion +
                (filas.isEmpty() ? "  (sin movimientos)" : "  (" + filas.size() + " movimiento" + (filas.size() == 1 ? "" : "s") + ")"));
    }

    private String formatear(double valor) {
        return String.format(Locale.forLanguageTag("es-AR"), "%,.2f", valor);
    }

    private void imprimirMayor() {
        try {
            boolean imprimio = tablaMayor.print(JTable.PrintMode.FIT_WIDTH);
            if (!imprimio) {
                JOptionPane.showMessageDialog(this, "Impresión cancelada.");
            }
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo imprimir: " + e.getMessage(),
                    "Error de impresión", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LibroMayorPanel::new);
    }
}
