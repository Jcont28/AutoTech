package hn.autotech.ui;

import hn.autotech.dao.OpcionesDao;
import hn.autotech.exception.NegocioException;
import hn.autotech.model.CambioMotorSolicitud;
import hn.autotech.model.Opcion;
import hn.autotech.service.MotorService;
import hn.autotech.service.OrdenService;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FrmAsistenteCambioMotor extends JFrame {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final MotorService motores = new MotorService();
    private final OrdenService ordenes = new OrdenService();
    private final JComboBox<Opcion> orden = new JComboBox<>();
    private final JComboBox<Opcion> motor = new JComboBox<>();
    private final JTextField serieAnterior = new JTextField(24);
    private final JComboBox<String> destino = new JComboBox<>(new String[] {"Reciclaje", "Devuelto al cliente", "Venta como repuesto", "Desecho", "Bodega"});
    private final JTextField desmontaje = new JTextField(LocalDateTime.now().format(FORMATO), 16);
    private final JTextField instalacion = new JTextField(LocalDateTime.now().plusHours(8).format(FORMATO), 16);
    private final JTextArea pruebas = new JTextArea(3, 40);
    private final JCheckBox aprobadas = new JCheckBox("Las pruebas fueron aprobadas");
    private final JTextField observaciones = new JTextField(40);
    private final JTextArea validacion = new JTextArea(6, 40);

    private record Catalogo(List<Opcion> ordenes, List<Opcion> motores) {
    }

    public FrmAsistenteCambioMotor() {
        setTitle("AutoTech | Sustitución de motor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 760);
        setLocationRelativeTo(null);
        JPanel raiz = new JPanel(new BorderLayout(12, 12));
        raiz.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        raiz.setBackground(Estilo.FONDO);
        raiz.add(Estilo.titulo("Sustitución de motor"), BorderLayout.NORTH);

        JPanel formulario = Estilo.tarjeta();
        formulario.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        pruebas.setLineWrap(true);
        validacion.setEditable(false);
        validacion.setLineWrap(true);
        validacion.setWrapStyleWord(true);
        agregar(formulario, c, 0, "Orden en reparación", orden);
        agregar(formulario, c, 1, "Motor nuevo (compatible)", motor);
        agregar(formulario, c, 2, "Serie del motor anterior", serieAnterior);
        agregar(formulario, c, 3, "Destino del motor retirado", destino);
        agregar(formulario, c, 4, "Fecha de desmontaje", desmontaje);
        agregar(formulario, c, 5, "Fecha de instalación", instalacion);
        agregar(formulario, c, 6, "Resultado de pruebas", new JScrollPane(pruebas));
        agregar(formulario, c, 7, "Pruebas", aprobadas);
        agregar(formulario, c, 8, "Observaciones", observaciones);
        agregar(formulario, c, 9, "Validacion", new JScrollPane(validacion));
        raiz.add(formulario, BorderLayout.CENTER);
        raiz.add(Estilo.fila(Estilo.boton("Validar compatibilidad", Estilo.AZUL, this::validar), Estilo.boton("Registrar cambio de motor", Estilo.VERDE, this::registrar),
                Estilo.boton("Refrescar datos", Estilo.GRIS, this::cargar)), BorderLayout.SOUTH);
        setContentPane(raiz);
        orden.addActionListener(e -> cargarMotores());
        cargar();
    }

    private void cargarMotores() {
        Opcion elegida = (Opcion) orden.getSelectedItem();
        if (elegida == null) {
            motor.removeAllItems();
            return;
        }
        Tareas.ejecutar(this, () -> OpcionesDao.motoresCompatibles(elegida.id()), lista -> {
            motor.removeAllItems();
            lista.forEach(motor::addItem);
            validacion.setText(lista.isEmpty() ? "No hay motores disponibles compatibles con el vehículo de esta orden. Registre uno en Inventario > Motores." : "");
        });
    }

    private void agregar(JPanel panel, GridBagConstraints c, int fila, String texto, java.awt.Component componente) {
        c.gridx = 0;
        c.gridy = fila;
        c.weightx = 0;
        panel.add(new JLabel(Etiquetas.acentuar(texto)), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(componente, c);
    }

    private void cargar() {
        Tareas.ejecutar(this, () -> new Catalogo(OpcionesDao.ordenesCambioMotor(), java.util.List.of()), catalogo -> {
            orden.removeAllItems();
            catalogo.ordenes().forEach(orden::addItem);
            cargarMotores();
        });
    }

    private LocalDateTime fecha(String texto, String nombre) {
        try {
            return LocalDateTime.parse(texto.trim(), FORMATO);
        } catch (DateTimeParseException e) {
            throw new NegocioException("La fecha de " + nombre + " debe tener el formato aaaa-mm-dd hh:mm.");
        }
    }

    private void validar() {
        Opcion elegidaOrden = (Opcion) orden.getSelectedItem();
        Opcion elegidoMotor = (Opcion) motor.getSelectedItem();
        if (elegidaOrden == null || elegidoMotor == null) {
            Mensajes.advertencia(this, "Seleccione una orden de sustitución en reparación y un motor.");
            return;
        }
        Tareas.ejecutar(this, () -> {
            int idVehiculo = ordenes.obtener(elegidaOrden.id()).orElseThrow(() -> new NegocioException("La orden no existe.")).idVehiculo();
            return motores.validarCompatibilidad(elegidoMotor.id(), idVehiculo);
        }, problemas -> validacion.setText(problemas.isEmpty() ? "El motor es compatible, esta disponible y tiene los datos obligatorios." : "- " + String.join("\n- ", problemas)));
    }

    private void registrar() {
        Opcion elegidaOrden = (Opcion) orden.getSelectedItem();
        Opcion elegidoMotor = (Opcion) motor.getSelectedItem();
        if (elegidaOrden == null || elegidoMotor == null) {
            Mensajes.advertencia(this, "Seleccione una orden de sustitución en reparación y un motor.");
            return;
        }
        CambioMotorSolicitud solicitud;
        try {
            solicitud = new CambioMotorSolicitud(elegidaOrden.id(), null, elegidoMotor.id(), serieAnterior.getText(), null, (String) destino.getSelectedItem(),
                    fecha(desmontaje.getText(), "desmontaje"), fecha(instalacion.getText(), "instalacion"), pruebas.getText(), aprobadas.isSelected(),
                    observaciones.getText());
        } catch (NegocioException e) {
            Mensajes.error(this, e);
            return;
        }
        CambioMotorSolicitud datos = solicitud;
        Tareas.ejecutar(this, () -> motores.registrarCambio(datos), id -> {
            validacion.setText("Cambio de motor registrado correctamente.");
            Mensajes.info(this, "Cambio de motor registrado. Continue en Gestión de orden: control de calidad, procesar y facturar.");
            cargar();
        });
    }
}
