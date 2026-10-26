package views;

import dao.AsientoDAO;
import dao.CuentaDAO;
import models.Asiento;
import models.AsientoDetalle;
import models.Cuenta;
import models.RegistroIva;
import models.ImportesIva;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Panel de Libro Diario - Sistema Contable.
 * Permite registrar asientos contables respetando la partida doble
 * (Debe == Haber) y consultar los asientos ya registrados, usando
 * las cuentas definidas en el Plan de Cuentas.
 */
public class LibroDiarioPanel extends JFrame {

    // ── Paleta de colores (misma estética que Gestión de Cuentas) ──────────────
    private static final Color AZUL_HEADER   = new Color(30,  80, 160);
    private static final Color AZUL_OSCURO   = new Color(20,  55, 120);
    private static final Color AZUL_BOTON    = new Color(33, 118, 233);
    private static final Color VERDE_BOTON   = new Color(40, 167,  69);
    private static final Color ROJO_BOTON    = new Color(220,  53,  69);
    private static final Color GRIS_BOTON    = new Color(108, 117, 125);
    private static final Color FONDO_PANEL   = new Color(245, 247, 250);
    private static final Color BORDE_COLOR   = new Color(220, 225, 235);
    private static final Color AZUL_TAB      = new Color(33, 118, 233);
    private static final Color TEXTO_LABEL   = new Color( 50,  60,  80);
    private static final Color FONDO_CAMPO   = new Color(250, 251, 253);
    private static final Color PLACEHOLDER   = new Color(170, 175, 185);
    private static final Color VERDE_BADGE   = new Color(212, 237, 218);
    private static final Color VERDE_TEXTO   = new Color( 21, 128,  61);
    private static final Color ROJO_BADGE    = new Color(248, 215, 218);
    private static final Color ROJO_TEXTO    = new Color(155,  28,  48);
    private static final Color FILA_PAR      = Color.WHITE;
    private static final Color FILA_IMPAR    = new Color(248, 250, 253);
    private static final Color CABECERA_TABLA= new Color(240, 243, 250);

    // ── Fuentes ────────────────────────────────────────────────────────────────
    private static final Font FUENTE_TITULO  = new Font("Segoe UI", Font.BOLD,  18);
    private static final Font FUENTE_SUBTIT  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FUENTE_SECCION = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FUENTE_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_CAMPO   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_BOTON   = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FUENTE_TABLA   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_CAB     = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FUENTE_INFO    = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FUENTE_TOTAL   = new Font("Segoe UI", Font.BOLD,  15);

    // ── DAOs ────────────────────────────────────────────────────────────────────
    private final AsientoDAO asientoDAO = new AsientoDAO();
    private final CuentaDAO  cuentaDAO  = new CuentaDAO();

    // ── Componentes del formulario de alta ─────────────────────────────────────
    private SelectorFecha selectorFecha;
    private JTextField txtConcepto;
    private JComboBox<String> cboOperacion;
    private DatosIvaPanel datosIva;
    private JTable      tablaDetalle;
    private DefaultTableModel modeloDetalle;
    private JLabel      lblTotalDebe;
    private JLabel      lblTotalHaber;
    private JLabel      lblDiferencia;
    private JButton      btnGuardar;

    // ── Componentes del listado de asientos ────────────────────────────────────
    private JTextField txtBuscar;
    private JTable      tablaAsientos;
    private DefaultTableModel modeloAsientos;
    private JLabel      lblPaginacion;

    // Lista de cuentas del Plan de Cuentas, usada para poblar el combo de la tabla
    private List<Cuenta> cuentasDisponibles = new ArrayList<>();

    // ID del asiento actualmente seleccionado en la tabla de abajo (para eliminar/ver)
    private Integer asientoSeleccionadoId = null;
    private int      asientoSeleccionadoNumero = -1;

    public LibroDiarioPanel() {
        configurarVentana();
        construirUI();
        cargarCuentas();
        agregarLineaDetalle();
        agregarLineaDetalle();
        cargarAsientosDesdeBD();
        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CONFIGURACIÓN DE VENTANA
    // ══════════════════════════════════════════════════════════════════════════
    private void configurarVentana() {
        setTitle("Libro Diario - Sistema Contable");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1320, 860);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
        setBackground(FONDO_PANEL);
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE UI
    // ══════════════════════════════════════════════════════════════════════════
    private void construirUI() {
        setLayout(new BorderLayout());
        add(crearHeader(), BorderLayout.NORTH);

        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBackground(FONDO_PANEL);
        contenido.setBorder(new EmptyBorder(20, 24, 20, 24));
        contenido.add(crearSeccionNuevoAsiento());
        contenido.add(Box.createVerticalStrut(20));
        contenido.add(crearSeccionListado());

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(FONDO_PANEL);

        add(scroll, BorderLayout.CENTER);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HEADER
    // ══════════════════════════════════════════════════════════════════════════
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

        JLabel icono = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillOval(0, 0, 46, 46);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 22));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("📖", (46 - fm.stringWidth("📖")) / 2, 31);
            }
        };
        icono.setPreferredSize(new Dimension(46, 46));

        JPanel titulos = new JPanel();
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));
        titulos.setOpaque(false);
        JLabel lblTitulo = new JLabel("LIBRO DIARIO");
        lblTitulo.setFont(FUENTE_TITULO);
        lblTitulo.setForeground(Color.WHITE);
        JLabel lblSubtitulo = new JLabel("Sistema Contable");
        lblSubtitulo.setFont(FUENTE_SUBTIT);
        lblSubtitulo.setForeground(new Color(200, 215, 240));
        titulos.add(lblTitulo);
        titulos.add(lblSubtitulo);

        izq.add(icono);
        izq.add(titulos);
        header.add(izq, BorderLayout.WEST);

        // Navegación hacia el resto de las pantallas del sistema
        header.add(BarraNavegacion.crear(LibroDiarioPanel.class), BorderLayout.EAST);

        return header;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SECCIÓN: NUEVO ASIENTO CONTABLE
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel crearSeccionNuevoAsiento() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.setLayout(new BorderLayout(0, 14));

        JLabel titulo = new JLabel("[ + ]  NUEVO ASIENTO CONTABLE");
        titulo.setFont(FUENTE_SECCION);
        titulo.setForeground(AZUL_TAB);
        titulo.setBorder(new EmptyBorder(0, 0, 4, 0));

        // ── Fecha + Concepto ────────────────────────────────────────────────
        JPanel cabecera = new JPanel(new GridBagLayout());
        cabecera.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 0, 4, 12);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblFecha = new JLabel("Fecha:");
        lblFecha.setFont(FUENTE_LABEL);
        lblFecha.setForeground(TEXTO_LABEL);
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE;
        cabecera.add(lblFecha, gbc);

        selectorFecha = new SelectorFecha();
        selectorFecha.setPreferredSize(new Dimension(155, 38));
        selectorFecha.setFecha(java.time.LocalDate.now());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.NONE;
        cabecera.add(selectorFecha, gbc);

        JLabel lblConcepto = new JLabel("Concepto:");
        lblConcepto.setFont(FUENTE_LABEL);
        lblConcepto.setForeground(TEXTO_LABEL);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE;
        cabecera.add(lblConcepto, gbc);

        txtConcepto = crearCampoTexto("Descripción de la operación...", false);
        txtConcepto.setPreferredSize(new Dimension(480, 38));
        aplicarLimiteCaracteres(txtConcepto, 200);
        gbc.gridx = 3; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        cabecera.add(txtConcepto, gbc);

        JLabel lblOperacion = new JLabel("Tipo de operación:");
        lblOperacion.setFont(FUENTE_LABEL);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        cabecera.add(lblOperacion, gbc);
        cboOperacion = new JComboBox<>(new String[]{"Asiento general sin IVA", "Compra", "Venta"});
        cboOperacion.setFont(FUENTE_CAMPO);
        cboOperacion.setPreferredSize(new Dimension(260, 38));
        lblOperacion.setLabelFor(cboOperacion);
        gbc.gridx = 1; gbc.gridwidth = 3;
        cabecera.add(cboOperacion, gbc);
        datosIva = new DatosIvaPanel();
        datosIva.setVisible(false);
        cboOperacion.addActionListener(e -> {
            datosIva.setCompra(cboOperacion.getSelectedIndex() == 1);
            datosIva.setVisible(cboOperacion.getSelectedIndex() != 0);
            revalidate();
            repaint();
        });

        // ── Tabla de líneas (Cuenta / Debe / Haber) ─────────────────────────
        String[] columnasDetalle = {"Cuenta", "Debe", "Haber"};
        modeloDetalle = new DefaultTableModel(columnasDetalle, 0) {
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
        tablaDetalle = new JTable(modeloDetalle) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? FILA_PAR : FILA_IMPAR);
                return c;
            }
        };
        estilizarTablaDetalle();
        modeloDetalle.addTableModelListener(e -> recalcularTotales());

        JScrollPane scrollDetalle = new JScrollPane(tablaDetalle);
        scrollDetalle.setBorder(new LineBorder(BORDE_COLOR, 1, true));
        scrollDetalle.getViewport().setBackground(Color.WHITE);
        scrollDetalle.setPreferredSize(new Dimension(0, 200));

        // ── Botones agregar/quitar línea ────────────────────────────────────
        JPanel botonesLinea = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        botonesLinea.setOpaque(false);
        JButton btnAgregarLinea = crearBotonPequeno("+ AGREGAR LÍNEA", Color.WHITE);
        btnAgregarLinea.setForeground(AZUL_BOTON);
        btnAgregarLinea.setBorder(new LineBorder(AZUL_BOTON, 1, true));
        btnAgregarLinea.setPreferredSize(new Dimension(160, 34));
        btnAgregarLinea.addActionListener(e -> agregarLineaDetalle());

        JButton btnQuitarLinea = crearBotonPequeno("- QUITAR LÍNEA", Color.WHITE);
        btnQuitarLinea.setForeground(ROJO_BOTON);
        btnQuitarLinea.setBorder(new LineBorder(ROJO_BOTON, 1, true));
        btnQuitarLinea.setPreferredSize(new Dimension(150, 34));
        btnQuitarLinea.addActionListener(e -> quitarLineaDetalle());

        botonesLinea.add(btnAgregarLinea);
        botonesLinea.add(btnQuitarLinea);

        // ── Panel de totales (Debe / Haber / Diferencia) ────────────────────
        JPanel totales = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 6));
        totales.setOpaque(false);

        lblTotalDebe  = crearEtiquetaTotal("Total Debe: 0,00", TEXTO_LABEL);
        lblTotalHaber = crearEtiquetaTotal("Total Haber: 0,00", TEXTO_LABEL);
        lblDiferencia = crearEtiquetaTotal("Diferencia: 0,00", VERDE_TEXTO);

        totales.add(lblTotalDebe);
        totales.add(lblTotalHaber);
        totales.add(lblDiferencia);

        // ── Aviso ────────────────────────────────────────────────────────────
        JPanel aviso = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        aviso.setBackground(new Color(232, 244, 255));
        aviso.setBorder(new CompoundBorder(
            new LineBorder(new Color(190, 220, 250), 1, true),
            new EmptyBorder(2, 6, 2, 6)
        ));
        JLabel icAviso = new JLabel("(i)");
        icAviso.setFont(new Font("Segoe UI", Font.BOLD, 13));
        icAviso.setForeground(new Color(30, 100, 200));
        JLabel txtAviso = new JLabel("El asiento solo se puede guardar si el Debe es igual al Haber.");
        txtAviso.setFont(FUENTE_INFO);
        txtAviso.setForeground(new Color(40, 80, 160));
        aviso.add(icAviso); aviso.add(txtAviso);

        JPanel piePanel = new JPanel(new BorderLayout());
        piePanel.setOpaque(false);
        piePanel.add(aviso, BorderLayout.WEST);
        piePanel.add(totales, BorderLayout.EAST);

        // ── Botones de acción (Guardar / Limpiar) ───────────────────────────
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        acciones.setOpaque(false);
        btnGuardar = crearBoton("GUARDAR ASIENTO", VERDE_BOTON);
        btnGuardar.setPreferredSize(new Dimension(220, 46));
        JButton btnLimpiar = crearBoton("LIMPIAR", GRIS_BOTON);
        btnLimpiar.setPreferredSize(new Dimension(140, 46));

        btnGuardar.addActionListener(e -> accionGuardarAsiento());
        btnLimpiar.addActionListener(e -> limpiarFormularioAsiento());

        acciones.add(btnLimpiar);
        acciones.add(btnGuardar);

        // ── Ensamblado ───────────────────────────────────────────────────────
        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setOpaque(false);
        centro.add(cabecera);
        centro.add(datosIva);
        centro.add(Box.createVerticalStrut(12));
        centro.add(scrollDetalle);
        centro.add(botonesLinea);
        centro.add(piePanel);
        centro.add(acciones);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(titulo, BorderLayout.NORTH);

        tarjeta.add(norte, BorderLayout.NORTH);
        tarjeta.add(centro, BorderLayout.CENTER);
        return tarjeta;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SECCIÓN: ASIENTOS REGISTRADOS
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel crearSeccionListado() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.setLayout(new BorderLayout(0, 14));

        JLabel titulo = new JLabel("ASIENTOS REGISTRADOS");
        titulo.setFont(FUENTE_SECCION);
        titulo.setForeground(AZUL_TAB);
        titulo.setBorder(new EmptyBorder(0, 0, 6, 0));

        JPanel barraBusq = new JPanel(new BorderLayout(10, 0));
        barraBusq.setOpaque(false);

        txtBuscar = crearCampoTexto("Buscar por concepto...", false);
        JButton btnBuscar = crearBotonPequeno("BUSCAR", AZUL_BOTON);
        JButton btnRefresh = crearBotonPequeno("REFRESCAR", Color.WHITE);
        btnRefresh.setForeground(AZUL_BOTON);
        btnRefresh.setBorder(new LineBorder(AZUL_BOTON, 1, true));
        JButton btnVerDetalle = crearBotonPequeno("VER DETALLE", Color.WHITE);
        btnVerDetalle.setForeground(TEXTO_LABEL);
        btnVerDetalle.setBorder(new LineBorder(BORDE_COLOR, 1, true));
        btnVerDetalle.setPreferredSize(new Dimension(140, 36));
        JButton btnEliminar = crearBotonPequeno("ELIMINAR", ROJO_BOTON);

        btnBuscar.addActionListener(e -> {
            String texto = txtBuscar.getText().trim();
            List<Asiento> resultado = texto.isEmpty()
                    ? asientoDAO.listarAsientos()
                    : asientoDAO.buscarPorConcepto(texto);
            cargarAsientosEnTabla(resultado);
        });
        btnRefresh.addActionListener(e -> {
            txtBuscar.setText("");
            cargarAsientosDesdeBD();
        });
        btnVerDetalle.addActionListener(e -> accionVerDetalle());
        btnEliminar.addActionListener(e -> accionEliminarAsiento());

        JPanel izqBarra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izqBarra.setOpaque(false);
        izqBarra.add(txtBuscar);
        izqBarra.add(btnBuscar);

        JPanel derBarra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derBarra.setOpaque(false);
        derBarra.add(btnVerDetalle);
        derBarra.add(btnEliminar);
        derBarra.add(btnRefresh);

        barraBusq.add(izqBarra, BorderLayout.WEST);
        barraBusq.add(derBarra, BorderLayout.EAST);

        String[] columnas = {"N°", "Fecha", "Concepto", "Total Debe", "Total Haber"};
        modeloAsientos = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaAsientos = new JTable(modeloAsientos) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? FILA_PAR : FILA_IMPAR);
                return c;
            }
        };
        estilizarTablaAsientos();
        tablaAsientos.getSelectionModel().addListSelectionListener(this::alSeleccionarAsiento);

        JScrollPane scrollTabla = new JScrollPane(tablaAsientos);
        scrollTabla.setBorder(new LineBorder(BORDE_COLOR, 1, true));
        scrollTabla.getViewport().setBackground(Color.WHITE);
        scrollTabla.setPreferredSize(new Dimension(0, 260));

        lblPaginacion = new JLabel("");
        lblPaginacion.setFont(FUENTE_INFO);
        lblPaginacion.setForeground(new Color(100, 110, 130));
        JPanel piePaginacion = new JPanel(new BorderLayout());
        piePaginacion.setOpaque(false);
        piePaginacion.setBorder(new EmptyBorder(10, 0, 0, 0));
        piePaginacion.add(lblPaginacion, BorderLayout.WEST);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(titulo, BorderLayout.NORTH);
        norte.add(barraBusq, BorderLayout.CENTER);

        tarjeta.add(norte, BorderLayout.NORTH);
        tarjeta.add(scrollTabla, BorderLayout.CENTER);
        tarjeta.add(piePaginacion, BorderLayout.SOUTH);
        return tarjeta;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS DE ESTILO (idénticos en espíritu a GestionCuentasPanel)
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel crearTarjeta() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
            new LineBorder(BORDE_COLOR, 1, true),
            new EmptyBorder(20, 22, 20, 22)
        ));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private JTextField crearCampoTexto(String placeholder, boolean disabled) {
        JTextField tf = new JTextField(20) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    g.setColor(PLACEHOLDER);
                    g.setFont(FUENTE_CAMPO.deriveFont(Font.ITALIC));
                    g.drawString(placeholder, 10, getHeight() / 2 + 5);
                }
            }
        };
        tf.setFont(FUENTE_CAMPO);
        tf.setForeground(TEXTO_LABEL);
        tf.setBackground(disabled ? new Color(240, 243, 248) : FONDO_CAMPO);
        tf.setEnabled(!disabled);
        tf.setBorder(new CompoundBorder(
            new LineBorder(BORDE_COLOR, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        tf.setPreferredSize(new Dimension(280, 38));
        return tf;
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
        btn.setPreferredSize(new Dimension(190, 46));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton crearBotonPequeno(String texto, Color bg) {
        JButton btn = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? (bg.equals(Color.WHITE) ? new Color(240,245,255) : bg.darker()) : bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(bg.equals(Color.WHITE) ? AZUL_BOTON : Color.WHITE);
        btn.setPreferredSize(new Dimension(130, 36));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel crearEtiquetaTotal(String texto, Color color) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(FUENTE_TOTAL);
        lbl.setForeground(color);
        return lbl;
    }

    private void aplicarLimiteCaracteres(JTextField tf, int max) {
        ((PlainDocument) tf.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (fb.getDocument().getLength() + string.length() <= max) super.insertString(fb, offset, string, attr);
            }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (fb.getDocument().getLength() - length + text.length() <= max) super.replace(fb, offset, length, text, attrs);
            }
        });
    }

    // Solo permite dígitos y '/' , máximo 10 caracteres (dd/mm/aaaa)
    private void aplicarFiltroFecha(JTextField tf) {
        ((PlainDocument) tf.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (esValido(fb, string)) super.insertString(fb, offset, string, attr);
            }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (esValido(fb, text)) super.replace(fb, offset, length, text, attrs);
            }
            private boolean esValido(FilterBypass fb, String textoNuevo) throws BadLocationException {
                String resultante = fb.getDocument().getText(0, fb.getDocument().getLength()) + textoNuevo;
                return resultante.length() <= 10 && textoNuevo.matches("[0-9/]*");
            }
        });
    }

    // Solo permite números, uno solo '.' o ',' (importes en Debe/Haber)
    private void aplicarFiltroNumerico(JTextField tf) {
        ((PlainDocument) tf.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (esValido(string)) super.insertString(fb, offset, string, attr);
            }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (esValido(text)) super.replace(fb, offset, length, text, attrs);
            }
            private boolean esValido(String texto) {
                return texto.matches("[0-9.,]*");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TABLA DE DETALLE (líneas del asiento): combo de cuentas + Debe/Haber
    // ══════════════════════════════════════════════════════════════════════════
    private void estilizarTablaDetalle() {
        tablaDetalle.setFont(FUENTE_TABLA);
        tablaDetalle.setRowHeight(38);
        tablaDetalle.setShowGrid(false);
        tablaDetalle.setIntercellSpacing(new Dimension(0, 0));
        tablaDetalle.setSelectionBackground(new Color(210, 230, 255));
        tablaDetalle.setSelectionForeground(TEXTO_LABEL);
        tablaDetalle.setFillsViewportHeight(true);
        tablaDetalle.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = tablaDetalle.getTableHeader();
        header.setFont(FUENTE_CAB);
        header.setBackground(CABECERA_TABLA);
        header.setForeground(TEXTO_LABEL);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDE_COLOR));
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);

        // Columna Cuenta: combo con todas las cuentas del Plan de Cuentas
        JComboBox<String> comboCuentas = new JComboBox<>();
        comboCuentas.setFont(FUENTE_CAMPO);
        DefaultCellEditor editorCuenta = new DefaultCellEditor(comboCuentas);
        tablaDetalle.getColumnModel().getColumn(0).setCellEditor(editorCuenta);
        tablaDetalle.getColumnModel().getColumn(0).setPreferredWidth(420);

        // Columnas Debe / Haber: campo de texto numérico
        JTextField campoImporte = new JTextField();
        campoImporte.setFont(FUENTE_CAMPO);
        aplicarFiltroNumerico(campoImporte);
        DefaultCellEditor editorImporte = new DefaultCellEditor(campoImporte);
        tablaDetalle.getColumnModel().getColumn(1).setCellEditor(editorImporte);
        tablaDetalle.getColumnModel().getColumn(2).setCellEditor(editorImporte);

        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        tablaDetalle.getColumnModel().getColumn(1).setCellRenderer(derecha);
        tablaDetalle.getColumnModel().getColumn(2).setCellRenderer(derecha);
        tablaDetalle.getColumnModel().getColumn(1).setPreferredWidth(140);
        tablaDetalle.getColumnModel().getColumn(2).setPreferredWidth(140);
    }

    private void estilizarTablaAsientos() {
        tablaAsientos.setFont(FUENTE_TABLA);
        tablaAsientos.setRowHeight(38);
        tablaAsientos.setShowGrid(false);
        tablaAsientos.setIntercellSpacing(new Dimension(0, 0));
        tablaAsientos.setSelectionBackground(new Color(210, 230, 255));
        tablaAsientos.setSelectionForeground(TEXTO_LABEL);
        tablaAsientos.setFillsViewportHeight(true);
        tablaAsientos.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = tablaAsientos.getTableHeader();
        header.setFont(FUENTE_CAB);
        header.setBackground(CABECERA_TABLA);
        header.setForeground(TEXTO_LABEL);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDE_COLOR));
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer centro = new DefaultTableCellRenderer();
        centro.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);

        tablaAsientos.getColumnModel().getColumn(0).setCellRenderer(centro);
        tablaAsientos.getColumnModel().getColumn(1).setCellRenderer(centro);
        tablaAsientos.getColumnModel().getColumn(3).setCellRenderer(derecha);
        tablaAsientos.getColumnModel().getColumn(4).setCellRenderer(derecha);

        tablaAsientos.getColumnModel().getColumn(0).setPreferredWidth(60);
        tablaAsientos.getColumnModel().getColumn(1).setPreferredWidth(100);
        tablaAsientos.getColumnModel().getColumn(2).setPreferredWidth(420);
        tablaAsientos.getColumnModel().getColumn(3).setPreferredWidth(140);
        tablaAsientos.getColumnModel().getColumn(4).setPreferredWidth(140);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARGA DE CUENTAS EN EL COMBO DE LA TABLA DE DETALLE
    // ══════════════════════════════════════════════════════════════════════════
    private void cargarCuentas() {
        cuentasDisponibles = cuentaDAO.listarCuentas();
        JComboBox<String> combo = (JComboBox<String>)
                ((DefaultCellEditor) tablaDetalle.getColumnModel().getColumn(0).getCellEditor()).getComponent();
        combo.removeAllItems();
        combo.addItem("");
        for (Cuenta c : cuentasDisponibles) {
            combo.addItem(c.getCodigo() + " - " + c.getNombre());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LÍNEAS DE DETALLE (agregar / quitar filas)
    // ══════════════════════════════════════════════════════════════════════════
    private void agregarLineaDetalle() {
        modeloDetalle.addRow(new Object[]{"", "0,00", "0,00"});
    }

    private void quitarLineaDetalle() {
        int fila = tablaDetalle.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná una línea de la tabla para quitarla.");
            return;
        }
        if (tablaDetalle.isEditing()) tablaDetalle.getCellEditor().stopCellEditing();
        modeloDetalle.removeRow(fila);
        recalcularTotales();
    }

    // Convierte "1.234,56" o "1234.56" a double de forma tolerante
    private double parsearImporte(Object valor) {
        if (valor == null) return 0.0;
        String texto = valor.toString().trim();
        if (texto.isEmpty()) return 0.0;
        try {
            return ImportesIva.leer(texto).doubleValue();
        } catch (IllegalArgumentException e) {
            return 0.0;
        }
    }

    private void recalcularTotales() {
        double sumaDebe = 0, sumaHaber = 0;
        for (int i = 0; i < modeloDetalle.getRowCount(); i++) {
            sumaDebe  += parsearImporte(modeloDetalle.getValueAt(i, 1));
            sumaHaber += parsearImporte(modeloDetalle.getValueAt(i, 2));
        }
        sumaDebe  = Math.round(sumaDebe  * 100) / 100.0;
        sumaHaber = Math.round(sumaHaber * 100) / 100.0;
        double diferencia = Math.round((sumaDebe - sumaHaber) * 100) / 100.0;

        lblTotalDebe.setText("Total Debe: " + formatear(sumaDebe));
        lblTotalHaber.setText("Total Haber: " + formatear(sumaHaber));
        lblDiferencia.setText("Diferencia: " + formatear(diferencia));

        boolean balanceado = Math.abs(diferencia) < 0.005 && (sumaDebe > 0 || sumaHaber > 0);
        lblDiferencia.setForeground(balanceado ? VERDE_TEXTO : ROJO_TEXTO);

        // El sistema impide guardar mientras el asiento no esté balanceado
        if (btnGuardar != null) btnGuardar.setEnabled(balanceado);
    }

    private String formatear(double valor) {
        return String.format(Locale.forLanguageTag("es-AR"), "%,.2f", valor);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GUARDAR ASIENTO
    // ══════════════════════════════════════════════════════════════════════════
    private void accionGuardarAsiento() {
        if (tablaDetalle.isEditing()) tablaDetalle.getCellEditor().stopCellEditing();

        String fechaTexto = selectorFecha.getTexto().trim();
        String concepto = txtConcepto.getText().trim();

        if (concepto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el concepto de la operación.",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fechaSql = convertirFechaASql(fechaTexto);
        if (fechaSql == null) {
            JOptionPane.showMessageDialog(this, "La fecha debe tener el formato dd/mm/aaaa y ser válida.",
                    "Fecha inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<AsientoDetalle> detalles = new ArrayList<>();
        double sumaDebe = 0, sumaHaber = 0;

        for (int i = 0; i < modeloDetalle.getRowCount(); i++) {
            String cuentaTexto = (String) modeloDetalle.getValueAt(i, 0);
            try {
                ImportesIva.leer(String.valueOf(modeloDetalle.getValueAt(i, 1)));
                ImportesIva.leer(String.valueOf(modeloDetalle.getValueAt(i, 2)));
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Revisá los importes de la línea " + (i + 1) + ". " + ex.getMessage(),
                        "Importe inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double debe  = parsearImporte(modeloDetalle.getValueAt(i, 1));
            double haber = parsearImporte(modeloDetalle.getValueAt(i, 2));

            if ((cuentaTexto == null || cuentaTexto.isEmpty()) && debe == 0 && haber == 0) continue;

            if (cuentaTexto == null || cuentaTexto.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Seleccioná una cuenta para todas las líneas con importe.",
                        "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (debe > 0 && haber > 0) {
                JOptionPane.showMessageDialog(this,
                        "Una misma línea no puede tener importe en el Debe y en el Haber a la vez.",
                        "Dato inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (debe == 0 && haber == 0) {
                JOptionPane.showMessageDialog(this,
                        "Ingresá un importe en el Debe o en el Haber para la cuenta seleccionada.",
                        "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String codigoCuenta = cuentaTexto.split(" - ", 2)[0].trim();
            detalles.add(new AsientoDetalle(codigoCuenta, debe, haber));
            sumaDebe  += debe;
            sumaHaber += haber;
        }

        if (detalles.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "Un asiento necesita al menos dos líneas (una en el Debe y otra en el Haber).",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        sumaDebe  = Math.round(sumaDebe  * 100) / 100.0;
        sumaHaber = Math.round(sumaHaber * 100) / 100.0;

        // ── VALIDACIÓN DE PARTIDA DOBLE EN LA VISTA ─────────────────────────
        // Refuerza en la interfaz lo que el DAO también valida: el asiento
        // NO se guarda si el Debe no coincide exactamente con el Haber.
        if (Math.abs(sumaDebe - sumaHaber) >= 0.005) {
            JOptionPane.showMessageDialog(this,
                    "El asiento no está balanceado. Total Debe (" + formatear(sumaDebe) +
                    ") debe ser igual a Total Haber (" + formatear(sumaHaber) + ").",
                    "Asiento desbalanceado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Asiento asiento = new Asiento(fechaSql, concepto, detalles);
        RegistroIva registroIva = null;
        if (cboOperacion.getSelectedIndex() != 0) {
            try {
                registroIva = datosIva.leer(java.sql.Date.valueOf(fechaSql));
                if (Math.abs(registroIva.getTotal() - sumaDebe) >= 0.005) {
                    throw new IllegalArgumentException("El total del comprobante (" + formatear(registroIva.getTotal())
                        + ") debe coincidir con el total Debe y Haber del asiento.");
                }
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Revisá los datos de IVA", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        String resultado = asientoDAO.crearAsiento(asiento, registroIva, cboOperacion.getSelectedIndex() == 1);

        switch (resultado) {
            case "OK":
                JOptionPane.showMessageDialog(this,
                        "Asiento N° " + asiento.getNumero() + " guardado correctamente."
                        + (registroIva == null ? "" : "\nRegistro IVA: " + registroIva.getNroComprobante()));
                limpiarFormularioAsiento();
                cargarAsientosDesdeBD();
                break;
            case "DESBALANCEADO":
                JOptionPane.showMessageDialog(this,
                        "El asiento no está balanceado. El Debe debe ser igual al Haber.",
                        "Asiento desbalanceado", JOptionPane.ERROR_MESSAGE);
                break;
            case "SIN_LINEAS":
                JOptionPane.showMessageDialog(this,
                        "El asiento no tiene líneas válidas para guardar.",
                        "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                break;
            default:
                JOptionPane.showMessageDialog(this,
                        "No se pudo guardar el asiento. " + asientoDAO.getUltimoError(),
                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Convierte "dd/mm/aaaa" a "yyyy-MM-dd", o null si no es válida
    private String convertirFechaASql(String fechaTexto) {
        if (!fechaTexto.matches("\\d{2}/\\d{2}/\\d{4}")) return null;
        try {
            SimpleDateFormat entrada = new SimpleDateFormat("dd/MM/yyyy");
            entrada.setLenient(false);
            Date fecha = entrada.parse(fechaTexto);
            return new SimpleDateFormat("yyyy-MM-dd").format(fecha);
        } catch (Exception e) {
            return null;
        }
    }

    private void limpiarFormularioAsiento() {
        datosIva.limpiar();
        cboOperacion.setSelectedIndex(0);
        txtConcepto.setText("");
        selectorFecha.setFecha(java.time.LocalDate.now());
        modeloDetalle.setRowCount(0);
        agregarLineaDetalle();
        agregarLineaDetalle();
        recalcularTotales();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARGA Y CONSULTA DE ASIENTOS
    // ══════════════════════════════════════════════════════════════════════════
    private void cargarAsientosDesdeBD() {
        cargarAsientosEnTabla(asientoDAO.listarAsientos());
    }

    private void cargarAsientosEnTabla(List<Asiento> asientos) {
        modeloAsientos.setRowCount(0);
        for (Asiento a : asientos) {
            modeloAsientos.addRow(new Object[]{
                    a.getNumero(),
                    formatearFechaVisual(a.getFecha()),
                    a.getConcepto(),
                    formatear(a.getTotalDebe()),
                    formatear(a.getTotalHaber())
            });
        }
        int total = modeloAsientos.getRowCount();
        lblPaginacion.setText(total == 0 ? "Sin registros"
                : "Mostrando 1 a " + total + " de " + total + " registro" + (total == 1 ? "" : "s"));
        asientoSeleccionadoId = null;
        asientoSeleccionadoNumero = -1;
    }

    private String formatearFechaVisual(String fechaSql) {
        try {
            SimpleDateFormat origen = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat destino = new SimpleDateFormat("dd/MM/yyyy");
            return destino.format(origen.parse(fechaSql));
        } catch (Exception e) {
            return fechaSql;
        }
    }

    private void alSeleccionarAsiento(javax.swing.event.ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int fila = tablaAsientos.getSelectedRow();
        if (fila < 0) {
            asientoSeleccionadoId = null;
            asientoSeleccionadoNumero = -1;
            return;
        }
        asientoSeleccionadoNumero = (Integer) modeloAsientos.getValueAt(fila, 0);
        // El ID real no se muestra en la tabla; se recupera buscando por número
        asientoSeleccionadoId = buscarIdPorNumero(asientoSeleccionadoNumero);
    }

    // Como la tabla no expone el id interno, se recupera consultando
    // los asientos actuales y comparando por número (columna visible).
    private Integer buscarIdPorNumero(int numero) {
        for (Asiento a : asientoDAO.listarAsientos()) {
            if (a.getNumero() == numero) return a.getId();
        }
        return null;
    }

    private void accionVerDetalle() {
        if (asientoSeleccionadoId == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un asiento de la tabla para ver su detalle.");
            return;
        }
        List<AsientoDetalle> detalles = asientoDAO.listarDetalle(asientoSeleccionadoId);
        mostrarDialogoDetalle(asientoSeleccionadoNumero, detalles);
    }

    private void mostrarDialogoDetalle(int numeroAsiento, List<AsientoDetalle> detalles) {
        JDialog dialogo = new JDialog(this, "Detalle del Asiento N° " + numeroAsiento, true);
        dialogo.setSize(560, 380);
        dialogo.setLocationRelativeTo(this);

        String[] columnas = {"Cuenta", "Debe", "Haber"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        double sumaDebe = 0, sumaHaber = 0;
        for (AsientoDetalle d : detalles) {
            modelo.addRow(new Object[]{
                    d.getCuentaCodigo() + " - " + d.getCuentaNombre(),
                    formatear(d.getDebe()),
                    formatear(d.getHaber())
            });
            sumaDebe  += d.getDebe();
            sumaHaber += d.getHaber();
        }
        modelo.addRow(new Object[]{"TOTALES", formatear(sumaDebe), formatear(sumaHaber)});

        JTable tabla = new JTable(modelo);
        tabla.setFont(FUENTE_TABLA);
        tabla.setRowHeight(32);
        tabla.setEnabled(false);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(BORDE_COLOR, 1, true));

        dialogo.setLayout(new BorderLayout(0, 10));
        dialogo.getContentPane().setBackground(FONDO_PANEL);
        ((JComponent) dialogo.getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));
        dialogo.add(scroll, BorderLayout.CENTER);

        JButton btnCerrar = crearBoton("CERRAR", GRIS_BOTON);
        btnCerrar.setPreferredSize(new Dimension(120, 40));
        btnCerrar.addActionListener(e -> dialogo.dispose());
        JPanel piePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        piePanel.setOpaque(false);
        piePanel.add(btnCerrar);
        dialogo.add(piePanel, BorderLayout.SOUTH);

        dialogo.setVisible(true);
    }

    private void accionEliminarAsiento() {
        if (asientoSeleccionadoId == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un asiento de la tabla para eliminarlo.");
            return;
        }

        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el asiento N° " + asientoSeleccionadoNumero + "?\n" +
                "Esta acción revertirá su efecto en los saldos de las cuentas.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        String resultado = asientoDAO.eliminarAsiento(asientoSeleccionadoId);
        switch (resultado) {
            case "OK":
                JOptionPane.showMessageDialog(this, "Asiento eliminado correctamente.");
                cargarAsientosDesdeBD();
                break;
            case "NO_EXISTE":
                JOptionPane.showMessageDialog(this, "El asiento ya no existe.");
                cargarAsientosDesdeBD();
                break;
            default:
                JOptionPane.showMessageDialog(this,
                        "Ocurrió un error al eliminar el asiento.",
                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LibroDiarioPanel::new);
    }
}
