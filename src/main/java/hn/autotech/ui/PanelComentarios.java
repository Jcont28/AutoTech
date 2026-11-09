package hn.autotech.ui;

import hn.autotech.service.SeguimientoService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

class PanelComentarios extends JPanel {
    private final SeguimientoService servicio = new SeguimientoService();
    private final String autor;
    private final JTable tabla = new JTable();
    private final JTextArea texto = new JTextArea(4, 50);
    private final JLabel titulo = new JLabel("Seleccione una orden");
    private Integer idOrden;

    PanelComentarios(String autor) {
        super(new BorderLayout(10, 10));
        this.autor = autor;
        setBackground(Estilo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(titulo, BorderLayout.NORTH);
        add(Estilo.tabla(tabla), BorderLayout.CENTER);
        JPanel nuevo = new JPanel(new BorderLayout(8, 8));
        nuevo.setOpaque(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        JLabel ayuda = new JLabel("Comentario visible para el cliente (máximo 500 caracteres)");
        ayuda.setForeground(Color.BLACK);
        nuevo.add(ayuda, BorderLayout.NORTH);
        nuevo.add(new JScrollPane(texto), BorderLayout.CENTER);
        nuevo.add(Estilo.fila(Estilo.boton("Publicar comentario", Estilo.VERDE, this::publicar)), BorderLayout.SOUTH);
        add(nuevo, BorderLayout.SOUTH);
    }

    void mostrar(Integer id, String descripcion) {
        idOrden = id;
        if (id == null) {
            titulo.setText("Seleccione una orden");
            tabla.setModel(new javax.swing.table.DefaultTableModel());
            return;
        }
        titulo.setText("Comentarios de la orden " + descripcion);
        Tareas.ejecutar(this, () -> servicio.comentarios(id), resultado -> {
            if (id.equals(idOrden)) {
                tabla.setModel(Formato.modelo(resultado));
                if (tabla.getColumnCount() == 3) {
                    tabla.getColumnModel().getColumn(0).setPreferredWidth(140);
                    tabla.getColumnModel().getColumn(1).setPreferredWidth(160);
                    tabla.getColumnModel().getColumn(2).setPreferredWidth(620);
                }
            }
        });
    }

    private void publicar() {
        Integer id = idOrden;
        if (id == null) {
            Mensajes.advertencia(this, "Seleccione una orden.");
            return;
        }
        String contenido = texto.getText();
        Tareas.ejecutar(this, () -> servicio.agregarComentario(id, contenido, autor), () -> {
            texto.setText("");
            mostrar(id, String.valueOf(id));
        });
    }
}
