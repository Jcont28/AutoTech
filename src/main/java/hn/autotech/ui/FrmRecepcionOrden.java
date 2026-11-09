package hn.autotech.ui;

import hn.autotech.dao.OpcionesDao;
import hn.autotech.dao.ServicioDao;
import hn.autotech.dao.VehiculoDao;
import hn.autotech.model.NuevaOrden;
import hn.autotech.model.Opcion;
import hn.autotech.model.Servicio;
import hn.autotech.service.OrdenService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class FrmRecepcionOrden extends JFrame {
    private final OrdenService ordenes = new OrdenService();
    private final JComboBox<Opcion> cliente = new JComboBox<>();
    private final JComboBox<Opcion> vehiculo = new JComboBox<>();
    private final JComboBox<Opcion> mecanico = new JComboBox<>();
    private final JSpinner kilometraje = new JSpinner(new SpinnerNumberModel(0, 0, 10_000_000, 1));
    private final JCheckBox cambioMotor = new JCheckBox("La orden es una sustitución de motor");
    private final JTextArea sintomas = new JTextArea(4, 40);
    private final DefaultListModel<Servicio> modeloServicios = new DefaultListModel<>();
    private final JList<Servicio> listaServicios = new JList<>(modeloServicios);
    private final JLabel resultado = new JLabel(" ");
    private final IntConsumer alCrear;
    private boolean cargando;

    private record Catalogo(List<Opcion> clientes, List<Opcion> mecanicos, List<Servicio> servicios) {
    }

    public FrmRecepcionOrden(IntConsumer alCrear) {
        this.alCrear = alCrear;
        setTitle("AutoTech | Recepción de orden");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(980, 720);
        setLocationRelativeTo(null);
        JPanel raiz = new JPanel(new BorderLayout(12, 12));
        raiz.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        raiz.setBackground(Estilo.FONDO);
        raiz.add(Estilo.titulo("Recepción de orden de trabajo"), BorderLayout.NORTH);

        JPanel formulario = Estilo.tarjeta();
        formulario.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        agregar(formulario, c, 0, "Cliente", cliente);
        agregar(formulario, c, 1, "Vehiculo", vehiculo);
        agregar(formulario, c, 2, "Kilometraje de ingreso", kilometraje);
        agregar(formulario, c, 3, "Mecánico responsable", mecanico);
        c.gridx = 1;
        c.gridy = 4;
        formulario.add(cambioMotor, c);
        sintomas.setLineWrap(true);
        sintomas.setWrapStyleWord(true);
        agregar(formulario, c, 5, "Síntomas reportados", new JScrollPane(sintomas));
        listaServicios.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane desplazamiento = new JScrollPane(listaServicios);
        desplazamiento.setPreferredSize(new java.awt.Dimension(500, 190));
        agregar(formulario, c, 6, "Servicios (Ctrl para varios)", desplazamiento);
        raiz.add(formulario, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(Estilo.fila(Estilo.boton("Crear orden", Estilo.VERDE, this::crear), Estilo.boton("Refrescar datos", Estilo.GRIS, this::cargarCatalogo)), BorderLayout.WEST);
        pie.add(resultado, BorderLayout.SOUTH);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);

        cliente.addActionListener(e -> {
            if (!cargando) {
                cargarVehiculos();
            }
        });
        vehiculo.addActionListener(e -> {
            if (!cargando) {
                cargarKilometraje();
            }
        });
        cargarCatalogo();
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

    private void cargarCatalogo() {
        cargando = true;
        Tareas.ejecutar(this, () -> new Catalogo(OpcionesDao.clientesActivos(), OpcionesDao.mecanicosDisponibles(), new ServicioDao().listarActivos()), catalogo -> {
            llenar(cliente, catalogo.clientes());
            llenar(mecanico, catalogo.mecanicos());
            modeloServicios.clear();
            catalogo.servicios().forEach(modeloServicios::addElement);
            cargando = false;
            cargarVehiculos();
        });
    }

    private void llenar(JComboBox<Opcion> combo, List<Opcion> opciones) {
        combo.removeAllItems();
        opciones.forEach(combo::addItem);
    }

    private void cargarVehiculos() {
        Opcion elegido = (Opcion) cliente.getSelectedItem();
        if (elegido == null) {
            vehiculo.removeAllItems();
            return;
        }
        cargando = true;
        Tareas.ejecutar(this, () -> OpcionesDao.vehiculosDeCliente(elegido.id()), opciones -> {
            llenar(vehiculo, opciones);
            cargando = false;
            cargarKilometraje();
        });
    }

    private void cargarKilometraje() {
        Opcion elegido = (Opcion) vehiculo.getSelectedItem();
        if (elegido == null) {
            return;
        }
        Tareas.ejecutar(this, () -> new VehiculoDao().buscar(elegido.id()), encontrado -> encontrado.ifPresent(v -> kilometraje.setValue(v.kilometraje())));
    }

    private void crear() {
        Opcion elegidoCliente = (Opcion) cliente.getSelectedItem();
        Opcion elegidoVehiculo = (Opcion) vehiculo.getSelectedItem();
        Opcion elegidoMecanico = (Opcion) mecanico.getSelectedItem();
        if (elegidoCliente == null || elegidoVehiculo == null) {
            Mensajes.advertencia(this, "Seleccione un cliente y uno de sus vehículos. Regístrelos primero en las pestañas Cliente y Vehículo si no existen.");
            return;
        }
        if (elegidoMecanico == null) {
            Mensajes.advertencia(this, "No hay mecánicos disponibles para asignar como responsables.");
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (Servicio servicio : listaServicios.getSelectedValuesList()) {
            ids.add(servicio.id());
        }
        NuevaOrden nueva = new NuevaOrden(elegidoCliente.id(), elegidoVehiculo.id(), (Integer) kilometraje.getValue(), sintomas.getText(),
                elegidoMecanico.id(), ids, cambioMotor.isSelected());
        Tareas.ejecutar(this, () -> {
            int id = ordenes.crearOrden(nueva);
            String texto = ordenes.obtener(id).map(o -> o.numeroOrden() + " (total estimado " + o.total() + ")").orElse("#" + id);
            return new Object[]{id, texto};
        }, datos -> {
            resultado.setText("Orden creada: " + datos[1]);
            Mensajes.info(this, "Orden creada: " + datos[1] + "\nContinúe con el diagnóstico del vehículo.");
            sintomas.setText("");
            listaServicios.clearSelection();
            cambioMotor.setSelected(false);
            cargarCatalogo();
            alCrear.accept((Integer) datos[0]);
        });
    }
}
