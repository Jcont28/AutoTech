package hn.autotech.ui;

import hn.autotech.model.EstadoVehiculo;
import hn.autotech.model.Sesion;
import hn.autotech.service.SeguimientoService;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

public class VentanaCliente extends JFrame {
    private static final String[] ETAPAS = {"Recibida", "En diagnostico", "Pendiente de aprobacion", "En reparacion", "En control de calidad", "Finalizada"};

    private final Sesion sesion;
    private final SeguimientoService servicio = new SeguimientoService();
    private final JPanel contenido = new JPanel();

    public VentanaCliente(Sesion sesion) {
        this.sesion = sesion;
        setTitle("AutoTech | Mi vehículo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 720);
        setLocationRelativeTo(null);
        Mensajes.principal(this);
        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Estilo.FONDO);
        Banner banner = new Banner("Hola, " + sesion.nombre(), "Seguimiento de su vehículo en el taller", false);
        banner.agregarDerecha(Estilo.fila(Estilo.boton("Actualizar", Estilo.VERDE, this::cargar), Estilo.boton("Cerrar sesión", Estilo.ROJO, this::cerrarSesion)));
        raiz.add(banner, BorderLayout.NORTH);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBackground(Estilo.FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(0, 22, 18, 22));
        JScrollPane desplazamiento = new JScrollPane(contenido);
        desplazamiento.setBorder(null);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(16);
        raiz.add(desplazamiento, BorderLayout.CENTER);
        setContentPane(raiz);
        cargar();
    }

    private void cargar() {
        mostrarMensaje("Consultando el estado de su vehículo...");
        Tareas.ejecutar(this, () -> servicio.vehiculosEnReparacion(sesion.idCliente()), this::mostrar);
    }

    private void mostrar(List<EstadoVehiculo> vehiculos) {
        contenido.removeAll();
        if (vehiculos.isEmpty()) {
            mostrarMensaje("Usted no tiene vehículo ingresado");
            return;
        }
        for (EstadoVehiculo vehiculo : vehiculos) {
            contenido.add(tarjeta(vehiculo));
            contenido.add(javax.swing.Box.createVerticalStrut(14));
        }
        contenido.revalidate();
        contenido.repaint();
    }

    private void mostrarMensaje(String texto) {
        contenido.removeAll();
        JPanel tarjeta = Estilo.tarjeta();
        tarjeta.setLayout(new BorderLayout(18, 0));
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        tarjeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        tarjeta.add(new JLabel(Iconos.de(Iconos.Tipo.AUTO, 80, Estilo.GRIS)), BorderLayout.WEST);
        JLabel mensaje = new JLabel(texto);
        mensaje.setFont(new Font("Segoe UI", Font.BOLD, 24));
        mensaje.setForeground(Estilo.PRIMARIO);
        tarjeta.add(mensaje, BorderLayout.CENTER);
        contenido.add(javax.swing.Box.createVerticalStrut(18));
        contenido.add(tarjeta);
        contenido.revalidate();
        contenido.repaint();
    }

    private JPanel tarjeta(EstadoVehiculo v) {
        JPanel tarjeta = Estilo.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel encabezado = new JPanel(new BorderLayout(14, 0));
        encabezado.setOpaque(false);
        encabezado.add(new JLabel(Iconos.de(Iconos.Tipo.AUTO, 64, Estilo.AZUL)), BorderLayout.WEST);
        JLabel nombre = new JLabel("<html><span style='font-size:18px'><b>" + escapar(v.vehiculo()) + "</b></span><br>Placa " + escapar(v.placa())
                + " &nbsp;|&nbsp; Orden " + escapar(v.numeroOrden()) + " &nbsp;|&nbsp; Ingreso " + Formato.valor(v.ingreso()) + "</html>");
        encabezado.add(nombre, BorderLayout.CENTER);
        JLabel estado = new JLabel(" " + Etiquetas.acentuar(v.estado()) + " ");
        estado.setOpaque(true);
        estado.setBackground(Estilo.AZUL);
        estado.setForeground(Color.WHITE);
        estado.setFont(new Font("Segoe UI", Font.BOLD, 15));
        estado.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        JPanel etiquetaEstado = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        etiquetaEstado.setOpaque(false);
        etiquetaEstado.add(new JLabel("Estado actual:"));
        etiquetaEstado.add(estado);
        encabezado.add(etiquetaEstado, BorderLayout.EAST);
        tarjeta.add(encabezado, BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.add(etapas(v.estado()));
        if (v.sintomas() != null && !v.sintomas().isBlank()) {
            JLabel sintomas = new JLabel("<html><b>Motivo de ingreso:</b> " + escapar(v.sintomas()) + "</html>");
            sintomas.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            sintomas.setAlignmentX(Component.LEFT_ALIGNMENT);
            centro.add(sintomas);
        }
        JLabel titulo = new JLabel("Comentarios del administrador del taller");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(titulo);
        centro.add(comentarios(v));
        tarjeta.add(centro, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel etapas(String actual) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        int indice = -1;
        for (int i = 0; i < ETAPAS.length; i++) {
            if (ETAPAS[i].equalsIgnoreCase(actual)) {
                indice = i;
            }
        }
        for (int i = 0; i < ETAPAS.length; i++) {
            JLabel etapa = new JLabel(" " + (i + 1) + ". " + Etiquetas.acentuar(ETAPAS[i]) + " ");
            etapa.setOpaque(true);
            etapa.setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (i < indice) {
                etapa.setBackground(Estilo.VERDE);
                etapa.setForeground(Color.WHITE);
            } else if (i == indice && i == ETAPAS.length - 1) {
                etapa.setBackground(Estilo.VERDE);
                etapa.setForeground(Color.WHITE);
            } else if (i == indice) {
                etapa.setBackground(new Color(230, 140, 0));
                etapa.setForeground(Color.WHITE);
            } else {
                etapa.setBackground(new Color(205, 212, 222));
                etapa.setForeground(Estilo.TEXTO);
            }
            etapa.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            panel.add(etapa);
        }
        return panel;
    }

    private JScrollPane comentarios(EstadoVehiculo v) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setForeground(Estilo.TEXTO);
        if (v.comentarios().isEmpty()) {
            area.setText("El taller aún no ha publicado comentarios sobre su vehículo.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (EstadoVehiculo.Comentario c : v.comentarios()) {
                sb.append(Formato.valor(c.fecha())).append("  -  ").append(c.autor()).append('\n').append(c.texto()).append("\n\n");
            }
            area.setText(sb.toString().trim());
            area.setCaretPosition(0);
        }
        JScrollPane panel = new JScrollPane(area);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setPreferredSize(new Dimension(700, 150));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        panel.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        return panel;
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void cerrarSesion() {
        new FrmLogin().setVisible(true);
        dispose();
    }
}
