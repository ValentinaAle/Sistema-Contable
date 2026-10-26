package views;

import dao.LibroIvaDAO;
import models.RegistroIva;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PrinterException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Panel del Libro Auxiliar de IVA (Compras / Ventas) - Sistema Contable.
 * Muestra, para un mes y año elegidos, los comprobantes registrados con sus
 * montos de Neto Gravado, IVA y Total, con fila de totales, y permite
 * imprimirlo.
 */
public class LibroIvaPanel extends JFrame {

    private static final Color AZUL_HEADER = new Color(30, 80, 160);
    private static final Color AZUL_OSCURO = new Color(20, 55, 120);
    private static final Color AZUL_BOTON = new Color(33, 118, 233);
    private static final Color GRIS_BOTON = new Color(108, 117, 125);
    private static final Color FONDO_PANEL = new Color(245, 247, 250);
    private static final Color BORDE_COLOR = new Color(220, 225, 235);
    private static final Color TEXTO_LABEL = new Color(50, 60, 80);
    private static final Color FONDO_CAMPO = new Color(250, 251, 253);
    private static final Color FILA_PAR = Color.WHITE;
    private static final Color FILA_IMPAR = new Color(248, 250, 253);
    private static final Color CABECERA_TABLA = new Color(240, 243, 250);
    private static final Color FILA_TOTAL = new Color(232, 240, 253);

    private static final Font FUENTE_TITULO = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FUENTE_SUBTIT = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FUENTE_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_CAMPO = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_BOTON = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FUENTE_TABLA = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_CAB = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FUENTE_TOTAL = new Font("Segoe UI", Font.BOLD, 13);

    private static final String[] MESES = {
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private final LibroIvaDAO ivaDAO = new LibroIvaDAO();

    private SelectorFecha selectorPeriodo;
    private JRadioButton rbCompras;
    private JRadioButton rbVentas;
    private JTable tablaIva;
    private DefaultTableModel modeloIva;
    private final JLabel estado = new JLabel(" ");
    private JButton btnImprimir;
    private String periodoMostrado = "";

    public LibroIvaPanel() {
        configurarVentana();
        construirUI();
        generarInforme();
        setVisible(true);
    }

    private void configurarVentana() {
        setTitle("Libro de IVA - Sistema Contable");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1180, 720);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);
        setBackground(FONDO_PANEL);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void construirUI() {
        setLayout(new BorderLayout());
        add(crearHeader(), BorderLayout.NORTH);

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBackground(FONDO_PANEL);
        contenido.setBorder(new EmptyBorder(20, 24, 20, 24));
        contenido.add(crearBarraFiltros(), BorderLayout.NORTH);
        contenido.add(crearTablaIva(), BorderLayout.CENTER);
        contenido.add(estado, BorderLayout.SOUTH);

        add(contenido, BorderLayout.CENTER);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, AZUL_HEADER, getWidth(), 0, AZUL_OSCURO);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 88));
        header.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel titulos = new JPanel();
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));
        titulos.setOpaque(false);
        JLabel lblTitulo = new JLabel("LIBRO DE IVA");
        lblTitulo.setFont(FUENTE_TITULO);
        lblTitulo.setForeground(Color.WHITE);
        JLabel lblSubtitulo = new JLabel("Registro auxiliar de Compras y Ventas");
        lblSubtitulo.setFont(FUENTE_SUBTIT);
        lblSubtitulo.setForeground(new Color(200, 215, 240));
        titulos.add(lblTitulo);
        titulos.add(lblSubtitulo);

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        izq.setOpaque(false);
        izq.add(BarraNavegacion.crearIconoLibro());
        izq.add(titulos);
        header.add(izq, BorderLayout.WEST);
        header.add(BarraNavegacion.crear(LibroIvaPanel.class), BorderLayout.EAST);
        return header;
    }

    private JPanel crearBarraFiltros() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));

        JLabel lblMes = new JLabel("Período:");
        lblMes.setFont(FUENTE_LABEL);
        lblMes.setForeground(TEXTO_LABEL);
        selectorPeriodo = new SelectorFecha();
        selectorPeriodo.setPreferredSize(new Dimension(155, 38));
        selectorPeriodo.setFecha(LocalDate.now());

        rbCompras = new JRadioButton("IVA Compras", true);
        rbVentas = new JRadioButton("IVA Ventas");
        rbCompras.setFont(FUENTE_LABEL);
        rbVentas.setFont(FUENTE_LABEL);
        rbCompras.setOpaque(false);
        rbVentas.setOpaque(false);
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbCompras);
        grupo.add(rbVentas);

        JButton btnGenerar = crearBoton("Filtrar", AZUL_BOTON);
        btnGenerar.setPreferredSize(new Dimension(160, 40));
        btnGenerar.addActionListener(e -> generarInforme());

        btnImprimir = crearBoton("Imprimir", GRIS_BOTON);
        btnImprimir.setPreferredSize(new Dimension(160, 40));
        btnImprimir.addActionListener(e -> imprimirInforme());
        rbCompras.addActionListener(e -> generarInforme());
        rbVentas.addActionListener(e -> generarInforme());
        selectorPeriodo.setAlElegir(e -> generarInforme());

        tarjeta.add(lblMes);
        tarjeta.add(selectorPeriodo);
        tarjeta.add(Box.createHorizontalStrut(10));
        tarjeta.add(rbCompras);
        tarjeta.add(rbVentas);
        tarjeta.add(Box.createHorizontalStrut(10));
        tarjeta.add(btnGenerar);
        tarjeta.add(btnImprimir);

        return tarjeta;
    }

    private JPanel crearTablaIva() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.setLayout(new BorderLayout());

        String[] columnas = {"Fecha", "Comprobante", "Razón social", "CUIT", "DNI", "Condición IVA",
            "Neto gravado", "Alícuota", "IVA", "Exento", "No gravado", "Horas", "Otros / percepciones", "Total"};
        modeloIva = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaIva = new JTable(modeloIva) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    boolean esTotal = row == getRowCount() - 1;
                    c.setBackground(esTotal ? FILA_TOTAL : (row % 2 == 0 ? FILA_PAR : FILA_IMPAR));
                    c.setFont(esTotal ? FUENTE_TOTAL : FUENTE_TABLA);
                }
                return c;
            }
        };
        tablaIva.setFont(FUENTE_TABLA);
        tablaIva.setRowHeight(34);
        tablaIva.setShowGrid(false);
        tablaIva.setIntercellSpacing(new Dimension(0, 0));
        tablaIva.setFillsViewportHeight(true);
        tablaIva.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader header = tablaIva.getTableHeader();
        header.setFont(FUENTE_CAB);
        header.setBackground(CABECERA_TABLA);
        header.setForeground(TEXTO_LABEL);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDE_COLOR));
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        tablaIva.getColumnModel().getColumn(6).setCellRenderer(derecha);  // Neto Gravado
        tablaIva.getColumnModel().getColumn(7).setCellRenderer(derecha);  // Neto No Grav.
        tablaIva.getColumnModel().getColumn(8).setCellRenderer(derecha);  // Exento
        tablaIva.getColumnModel().getColumn(9).setCellRenderer(derecha);  // Alícuota
        tablaIva.getColumnModel().getColumn(10).setCellRenderer(derecha); // IVA
        tablaIva.getColumnModel().getColumn(11).setCellRenderer(derecha); // Total

        tablaIva.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaIva.getColumnModel().getColumn(1).setPreferredWidth(120);
        tablaIva.getColumnModel().getColumn(2).setPreferredWidth(50);
        tablaIva.getColumnModel().getColumn(3).setPreferredWidth(120);
        tablaIva.getColumnModel().getColumn(4).setPreferredWidth(200);
        tablaIva.getColumnModel().getColumn(5).setPreferredWidth(110);
        tablaIva.getColumnModel().getColumn(6).setPreferredWidth(100);
        tablaIva.getColumnModel().getColumn(7).setPreferredWidth(100);
        tablaIva.getColumnModel().getColumn(8).setPreferredWidth(90);
        tablaIva.getColumnModel().getColumn(9).setPreferredWidth(80);
        tablaIva.getColumnModel().getColumn(10).setPreferredWidth(90);
        tablaIva.getColumnModel().getColumn(11).setPreferredWidth(110);
        int[] anchos = {95, 225, 200, 125, 100, 180, 120, 90, 120, 110, 120, 80, 150, 125};
        for (int i = 0; i < anchos.length; i++) {
            tablaIva.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            if (i >= 6) tablaIva.getColumnModel().getColumn(i).setCellRenderer(derecha);
        }

        JScrollPane scroll = new JScrollPane(tablaIva);
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
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isRollover() ? bg.darker() : bg;
                g2.setColor(c);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
            }
        };
        btn.setFont(FUENTE_BOTON);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GENERAR EL INFORME
    // ══════════════════════════════════════════════════════════════════════════
    private void generarInforme() {
        LocalDate fecha = selectorPeriodo.getFecha();
        if (fecha == null) {
            btnImprimir.setEnabled(false);
            JOptionPane.showMessageDialog(this, "Ingresá una fecha válida con formato dd/MM/yyyy.",
                    "Dato inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int mes = fecha.getMonthValue();
        int anio = fecha.getYear();

        boolean esCompras = rbCompras.isSelected();
        List<RegistroIva> registros;
        try {
            registros = esCompras ? ivaDAO.listarCompras(mes, anio) : ivaDAO.listarVentas(mes, anio);
        } catch (IllegalStateException ex) {
            modeloIva.setRowCount(0);
            btnImprimir.setEnabled(false);
            estado.setText(ex.getMessage());
            return;
        }
        periodoMostrado = "Libro IVA " + (esCompras ? "Compras" : "Ventas") + " - " + MESES[mes - 1] + " " + anio;

        modeloIva.setRowCount(0);
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

        double totalNeto = 0, totalNoGravado = 0, totalExento = 0, totalIva = 0, totalGeneral = 0;
        double totalOtros = 0, totalHoras = 0;
        for (RegistroIva r : registros) {
            modeloIva.addRow(new Object[]{
                formatoFecha.format(r.getFecha()),
                r.getTipoComprobante() + " " + r.getNroComprobante(),
                r.getRazonSocial(),
                r.getCuit(),
                r.getDni(),
                r.getCondicionIva(),
                formatear(r.getNetoGravado()),
                formatear(r.getAlicuota()) + "%",
                formatear(r.getIva()),
                formatear(r.getExento()),
                formatear(r.getNetoNoGravado()),
                formatear(r.getHoras()),
                formatear(r.getOtrosPercepciones()),
                formatear(r.getTotal())
            });
            totalNeto += r.getNetoGravado();
            totalNoGravado += r.getNetoNoGravado();
            totalExento += r.getExento();
            totalIva += r.getIva();
            totalGeneral += r.getTotal();
            totalOtros += r.getOtrosPercepciones();
            totalHoras += r.getHoras();
        }

        if (!registros.isEmpty()) {
            modeloIva.addRow(new Object[]{
                "", "", "Totales", "", "", "",
                formatear(totalNeto), "", formatear(totalIva), formatear(totalExento),
                formatear(totalNoGravado), formatear(totalHoras), formatear(totalOtros), formatear(totalGeneral)
            });
        }
        btnImprimir.setEnabled(!registros.isEmpty());
        estado.setText(periodoMostrado + " · " + (registros.isEmpty() ? "Sin registros para este período." : registros.size() + " registros"));
    }

    private String formatear(double valor) {
        return String.format(Locale.forLanguageTag("es-AR"), "%,.2f", valor);
    }

    private void imprimirInforme() {
        generarInforme();
        if (!btnImprimir.isEnabled()) return;
        try {
            java.awt.print.PrinterJob trabajo = java.awt.print.PrinterJob.getPrinterJob();
            trabajo.setJobName(periodoMostrado);
            java.awt.print.PageFormat pagina = trabajo.defaultPage();
            pagina.setOrientation(java.awt.print.PageFormat.LANDSCAPE);
            trabajo.setPrintable(tablaIva.getPrintable(JTable.PrintMode.FIT_WIDTH,
                new java.text.MessageFormat(periodoMostrado), new java.text.MessageFormat("Página {0}")), pagina);
            boolean imprimio = trabajo.printDialog();
            if (imprimio) trabajo.print();
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
        SwingUtilities.invokeLater(LibroIvaPanel::new);
    }
}
