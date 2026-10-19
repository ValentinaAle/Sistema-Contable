package views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

// Barra de botones que permite saltar entre las 4 pantallas del sistema
// (Plan de Cuentas, Libro Diario, Libro Mayor, Libro de IVA) desde
// cualquiera de ellas. Cada pantalla la agrega a su propio header
// pasando su propia clase, así no se muestra un botón para "volver"
// a la misma ventana en la que ya estás parado.
public class BarraNavegacion {

    /** Tamaño del distintivo visual que acompaña el nombre de cada módulo. */
    private static final int TAMANIO_ICONO = 46;

    /**
     * Devuelve el ícono circular común de los libros contables. Centralizarlo
     * evita que un módulo quede sin el distintivo al cambiar el encabezado.
     */
    public static JLabel crearIconoLibro() {
        JLabel icono = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // No dependemos de emojis: no todos los escritorios Linux
                    // incluidos en Docker tienen una tipografía con esos glifos.
                    g2.setColor(new Color(255, 255, 255, 62));
                    g2.fillOval(0, 0, TAMANIO_ICONO, TAMANIO_ICONO);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    // Libro mayor abierto: un ícono vectorial siempre visible.
                    g2.drawRoundRect(11, 12, 11, 21, 2, 2);
                    g2.drawRoundRect(23, 12, 11, 21, 2, 2);
                    g2.drawLine(23, 12, 23, 34);
                    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(14, 18, 19, 18);
                    g2.drawLine(14, 23, 19, 23);
                    g2.drawLine(26, 18, 31, 18);
                    g2.drawLine(26, 23, 31, 23);
                } finally {
                    g2.dispose();
                }
            }
        };
        icono.setPreferredSize(new Dimension(TAMANIO_ICONO, TAMANIO_ICONO));
        return icono;
    }

    /** Ícono vectorial para el Plan de Cuentas, sin depender de una tipografía emoji. */
    public static JLabel crearIconoPlanCuentas() {
        JLabel icono = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 255, 255, 62));
                    g2.fillOval(0, 0, TAMANIO_ICONO, TAMANIO_ICONO);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    // Plan de cuentas: nodos vinculados que representan la jerarquía contable.
                    g2.drawRoundRect(18, 10, 10, 7, 2, 2);
                    g2.drawLine(23, 17, 23, 22);
                    g2.drawLine(13, 22, 33, 22);
                    g2.drawLine(13, 22, 13, 27);
                    g2.drawLine(23, 22, 23, 27);
                    g2.drawLine(33, 22, 33, 27);
                    g2.fillRoundRect(8, 27, 10, 8, 2, 2);
                    g2.fillRoundRect(18, 27, 10, 8, 2, 2);
                    g2.fillRoundRect(28, 27, 10, 8, 2, 2);
                } finally {
                    g2.dispose();
                }
            }
        };
        icono.setPreferredSize(new Dimension(TAMANIO_ICONO, TAMANIO_ICONO));
        return icono;
    }

    /** Barra completa: navegación entre módulos y controles de la ventana real. */
    public static JPanel crearConControles(Class<?> pantallaActual) {
        JPanel area = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        area.setOpaque(false);
        area.add(crear(pantallaActual));
        area.add(crearControl("−", "Minimizar", BarraNavegacion::minimizar));
        area.add(crearControl("□", "Maximizar o restaurar", BarraNavegacion::alternarMaximizado));
        area.add(crearControl("×", "Cerrar", BarraNavegacion::cerrar));
        return area;
    }

    public static JPanel crear(Class<?> pantallaActual) {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        barra.setOpaque(false);

        agregarBoton(barra, pantallaActual, GestionCuentasPanel.class, "Plan de Cuentas");
        agregarBoton(barra, pantallaActual, LibroDiarioPanel.class,    "Libro Diario");
        agregarBoton(barra, pantallaActual, LibroMayorPanel.class,     "Libro Mayor");
        agregarBoton(barra, pantallaActual, LibroIvaPanel.class,       "Libro de IVA");
        agregarBoton(barra, pantallaActual, EstadosContablesPanel.class, "Estados");

        return barra;
    }

    private static void agregarBoton(JPanel barra, Class<?> actual, Class<?> destino, String texto) {
        if (actual == destino) return; // no mostrar botón para volver a la pantalla actual

        JButton btn = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(255, 255, 255, 72) : new Color(255, 255, 255, 34));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(255, 255, 255, 55));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(120, 32));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> abrir(SwingUtilities.getWindowAncestor(btn), destino));
        barra.add(btn);
    }

    /**
     * Construye el módulo elegido y reemplaza el contenido de la ventana que
     * ya está abierta. Así nunca se acumulan ventanas al navegar.
     */
    private static void abrir(Window origen, Class<?> destino) {
        SwingUtilities.invokeLater(() -> {
            if (!(origen instanceof JFrame actual)) return;

            JFrame siguiente;
            if (destino == GestionCuentasPanel.class) siguiente = new GestionCuentasPanel();
            else if (destino == LibroDiarioPanel.class) siguiente = new LibroDiarioPanel();
            else if (destino == LibroMayorPanel.class) siguiente = new LibroMayorPanel();
            else if (destino == LibroIvaPanel.class) siguiente = new LibroIvaPanel();
            else if (destino == EstadosContablesPanel.class) siguiente = new EstadosContablesPanel();
            else return;

            // Los constructores actuales muestran el JFrame. Se lo oculta de
            // inmediato y se transfiere su contenido a la ventana original.
            siguiente.setVisible(false);
            Container contenido = siguiente.getContentPane();
            siguiente.setContentPane(new JPanel());

            actual.setContentPane(contenido);
            actual.setTitle(siguiente.getTitle());
            // La ventana es siempre la misma: se preservan sus dimensiones
            // actuales para que cambiar de libro no provoque saltos visuales.
            actual.revalidate();
            actual.repaint();
            siguiente.dispose();
            actual.toFront();
        });
    }

    private static JButton crearControl(String texto, String descripcion, java.util.function.Consumer<Window> accion) {
        JButton btn = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color base = switch (texto) {
                        case "×" -> new Color(220, 53, 69);
                        case "□" -> new Color(72, 104, 170);
                        default -> new Color(45, 118, 210);
                    };
                    Color fondo = getModel().isPressed() ? base.darker()
                            : getModel().isRollover() ? base.brighter() : base;
                    g2.setColor(fondo);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
                    g2.setColor(new Color(255, 255, 255, 115));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);

                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    if ("−".equals(texto)) g2.drawLine(cx - 6, cy, cx + 6, cy);
                    else if ("□".equals(texto)) g2.drawRoundRect(cx - 6, cy - 5, 12, 10, 2, 2);
                    else {
                        g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                        g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                    }
                } finally {
                    g2.dispose();
                }
            }
        };
        btn.setToolTipText(descripcion);
        btn.setPreferredSize(new Dimension(34, 30));
        btn.setOpaque(false); btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> accion.accept(SwingUtilities.getWindowAncestor(btn)));
        return btn;
    }

    private static void minimizar(Window ventana) {
        if (ventana instanceof Frame frame) frame.setState(Frame.ICONIFIED);
    }

    private static void alternarMaximizado(Window ventana) {
        if (!(ventana instanceof Frame frame)) return;
        int estado = frame.getExtendedState();
        frame.setExtendedState((estado & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH
                ? Frame.NORMAL : estado | Frame.MAXIMIZED_BOTH);
    }

    private static void cerrar(Window ventana) {
        if (ventana != null) ventana.dispatchEvent(new WindowEvent(ventana, WindowEvent.WINDOW_CLOSING));
    }
}
