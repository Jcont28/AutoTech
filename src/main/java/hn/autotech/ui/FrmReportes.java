package hn.autotech.ui;

import hn.autotech.model.Reporte;
import hn.autotech.service.ReporteService;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.MessageFormat;

public class FrmReportes extends JFrame {
    private final ReporteService servicio = new ReporteService();
    private final JComboBox<Reporte> reportes = new JComboBox<>(Reporte.values());
    private final JTextField filtro = new JTextField(22);
    private final JTable tabla = new JTable();
    private final JLabel estado = new JLabel(" ");

    public FrmReportes() {
        setTitle("AutoTech | Reportes");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        raiz.setBackground(Estilo.FONDO);
        JPanel superior = new JPanel(new BorderLayout());
        superior.setOpaque(false);
        superior.add(Estilo.titulo("Reportes administrativos"), BorderLayout.NORTH);
        reportes.setPreferredSize(new Dimension(520, 28));
        superior.add(Estilo.fila(new JLabel("Reporte:"), reportes, Estilo.boton("Ejecutar", Estilo.VERDE, this::ejecutar), new JLabel("Filtrar:"), filtro, Estilo.boton("Imprimir", Estilo.AZUL, Iconos.de(Iconos.Tipo.DOCUMENTO, 16, java.awt.Color.WHITE), this::imprimir)), BorderLayout.CENTER);
        raiz.add(superior, BorderLayout.NORTH);
        raiz.add(Estilo.tabla(tabla), BorderLayout.CENTER);
        raiz.add(estado, BorderLayout.SOUTH);
        setContentPane(raiz);
        filtro.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltro();
            }
        });
        ejecutar();
    }

    private void ejecutar() {
        Reporte elegido = (Reporte) reportes.getSelectedItem();
        estado.setText("Ejecutando...");
        Tareas.ejecutar(this, () -> servicio.ejecutar(elegido), resultado -> {
            DefaultTableModel modelo = Formato.modelo(resultado);
            tabla.setModel(modelo);
            tabla.setRowSorter(new TableRowSorter<>(modelo));
            for (int i = 0; i < tabla.getColumnCount(); i++) {
                tabla.getColumnModel().getColumn(i).setPreferredWidth(150);
            }
            aplicarFiltro();
            estado.setText(elegido + " | " + resultado.filas().size() + " fila(s)");
        });
    }

    @SuppressWarnings("unchecked")
    private void aplicarFiltro() {
        if (tabla.getRowSorter() instanceof TableRowSorter<?> ordenador) {
            String texto = filtro.getText().trim();
            ((TableRowSorter<DefaultTableModel>) ordenador).setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(texto)));
        }
    }

    private void imprimir() {
        if (tabla.getRowCount() == 0) {
            Mensajes.advertencia(this, "No hay datos para imprimir. Ejecute un reporte primero.");
            return;
        }
        Reporte elegido = (Reporte) reportes.getSelectedItem();
        try {
            PrinterJob trabajo = PrinterJob.getPrinterJob();
            PageFormat formato = trabajo.defaultPage();
            formato.setOrientation(PageFormat.LANDSCAPE);
            trabajo.setJobName("AutoTech - " + elegido);
            trabajo.setPrintable(tabla.getPrintable(JTable.PrintMode.FIT_WIDTH, new MessageFormat("AutoTech Motors - " + elegido), new MessageFormat("Página {0}")), formato);
            if (trabajo.printDialog()) {
                trabajo.print();
            }
        } catch (PrinterException e) {
            Mensajes.advertencia(this, "No se pudo imprimir: " + e.getMessage());
        }
    }
}
