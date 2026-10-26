package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Campo de fecha reutilizable con calendario Swing, sin dependencias externas. */
public class SelectorFecha extends JPanel {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JTextField campo = new JTextField(10);
    private final JPopupMenu popup = new JPopupMenu();
    private YearMonth mesVisible = YearMonth.now();
    private Consumer<LocalDate> alElegir;

    public SelectorFecha() {
        setLayout(new BorderLayout(3, 0));
        setOpaque(false);
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 205, 228)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JButton calendario = new JButton("▣");
        calendario.setToolTipText("Elegir fecha");
        calendario.setFocusable(false);
        calendario.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 15));
        calendario.addActionListener(e -> abrirCalendario());
        add(campo, BorderLayout.CENTER);
        add(calendario, BorderLayout.EAST);
    }

    public LocalDate getFecha() {
        try { return LocalDate.parse(campo.getText().trim(), FORMATO); }
        catch (DateTimeParseException ex) { return null; }
    }
    public void setFecha(LocalDate fecha) {
        if (fecha != null) { campo.setText(fecha.format(FORMATO)); mesVisible = YearMonth.from(fecha); }
    }
    public String getTexto() { return campo.getText(); }
    public void setAlElegir(Consumer<LocalDate> accion) { alElegir = accion; }

    private void abrirCalendario() {
        LocalDate actual = getFecha();
        mesVisible = YearMonth.from(actual == null ? LocalDate.now() : actual);
        reconstruirCalendario();
        popup.show(this, 0, getHeight());
    }
    private void reconstruirCalendario() {
        popup.removeAll();
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel cabecera = new JPanel(new BorderLayout());
        JButton anterior = new JButton("‹"), siguiente = new JButton("›");
        anterior.addActionListener(e -> { mesVisible = mesVisible.minusMonths(1); reconstruirCalendario(); });
        siguiente.addActionListener(e -> { mesVisible = mesVisible.plusMonths(1); reconstruirCalendario(); });
        JLabel titulo = new JLabel(mesVisible.getMonth().toString().toLowerCase() + " " + mesVisible.getYear(), SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cabecera.add(anterior, BorderLayout.WEST); cabecera.add(titulo, BorderLayout.CENTER); cabecera.add(siguiente, BorderLayout.EAST);
        panel.add(cabecera, BorderLayout.NORTH);
        JPanel dias = new JPanel(new GridLayout(7, 7, 2, 2));
        for (String n : new String[]{"Lu","Ma","Mi","Ju","Vi","Sa","Do"}) {
            JLabel l = new JLabel(n, SwingConstants.CENTER); l.setFont(new Font("Segoe UI", Font.BOLD, 11)); dias.add(l);
        }
        int primero = mesVisible.atDay(1).getDayOfWeek().getValue();
        for (int i = 1; i < primero; i++) dias.add(new JLabel());
        LocalDate hoy = LocalDate.now();
        for (int dia = 1; dia <= mesVisible.lengthOfMonth(); dia++) {
            LocalDate fecha = mesVisible.atDay(dia);
            JButton boton = new JButton(String.valueOf(dia));
            boton.setMargin(new java.awt.Insets(2, 4, 2, 4));
            boton.setFocusable(false);
            if (fecha.equals(hoy)) boton.setBorder(BorderFactory.createLineBorder(new Color(30, 80, 160), 2));
            boton.addActionListener(e -> { setFecha(fecha); popup.setVisible(false); if (alElegir != null) alElegir.accept(fecha); });
            dias.add(boton);
        }
        panel.add(dias, BorderLayout.CENTER);
        popup.add(panel); popup.pack();
    }
}
