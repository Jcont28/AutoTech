package hn.autotech.ui;

import hn.autotech.dao.ConsultaOrdenDao;
import hn.autotech.dao.OpcionesDao;
import hn.autotech.exception.NegocioException;
import hn.autotech.model.EstadoOrden;
import hn.autotech.model.Sesion;
import hn.autotech.model.Factura;
import hn.autotech.model.Opcion;
import hn.autotech.model.OrdenTrabajo;
import hn.autotech.model.ResultadoConsulta;
import hn.autotech.service.FacturacionService;
import hn.autotech.service.OrdenService;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.IntConsumer;

public class FrmGestionOrden extends JFrame {
    private final OrdenService ordenService = new OrdenService();
    private final FacturacionService facturacion = new FacturacionService();

    private final JComboBox<Opcion> ordenes = new JComboBox<>();
    private final JLabel resumen = new JLabel(" ");
    private final JTable tablaHistorial = new JTable();
    private final JComboBox<Opcion> mecanicoAsignar = new JComboBox<>();
    private final JCheckBox esResponsable = new JCheckBox("Es el responsable principal");
    private final JTable tablaMecanicos = new JTable();

    private final JComboBox<Opcion> mecanicoDiagnostico = new JComboBox<>();
    private final JTextArea evaluacion = new JTextArea(3, 50);
    private final JCheckBox requiereMotor = new JCheckBox("Requiere sustitución de motor");
    private final JTextField descripcionEvidencia = new JTextField(28);
    private final JTextField rutaEvidencia = new JTextField(28);
    private final JTextField descuento = new JTextField("0", 8);
    private final JTextField autorizadoPor = new JTextField(16);
    private final JComboBox<Opcion> impuesto = new JComboBox<>();
    private final JTable tablaPresupuesto = new JTable();

    private final JTable tablaActividades = new JTable();
    private final JTable tablaControles = new JTable();
    private final JComboBox<Opcion> inspector = new JComboBox<>();
    private final JTextField observacionControl = new JTextField(24);
    private final JTextField descripcionControl = new JTextField(24);
    private final JCheckBox controlObligatorio = new JCheckBox("Obligatorio", true);

    private final JComboBox<Opcion> repuesto = new JComboBox<>();
    private final JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
    private final JTable tablaConsumos = new JTable();

    private final JButton botonProcesar = Estilo.boton("Finalizar y facturar", Estilo.VERDE, this::procesar);
    private final JProgressBar progreso = new JProgressBar();
    private final JTextArea bitacora = new JTextArea(8, 60);
    private final JLabel datosFactura = new JLabel("Sin factura");
    private final JTextField montoPago = new JTextField(10);
    private final JComboBox<String> metodoPago = new JComboBox<>(new String[]{"Efectivo", "Tarjeta", "Transferencia", "Cheque"});
    private final JTextField referenciaPago = new JTextField(14);
    private final JTable tablaPagos = new JTable();
    private boolean cargando;

    private record Carga(List<Opcion> ordenes, List<Opcion> mecanicos, List<Opcion> disponibles, List<Opcion> repuestos, List<Opcion> impuestos) {
    }

    private record Vista(Optional<OrdenTrabajo> orden, ResultadoConsulta resumen, ResultadoConsulta historial, ResultadoConsulta presupuesto,
                         ResultadoConsulta mecanicos, ResultadoConsulta actividades, ResultadoConsulta controles, ResultadoConsulta consumos, ResultadoConsulta pagos,
                         Optional<Factura> factura, BigDecimal pagado) {
    }

    private static final String[] NOMBRES_ETAPAS = {"Recepción", "Diagnóstico", "Cotización", "Reparación", "Control de calidad", "Entrega y cobro"};
    private static final EstadoOrden[] ESTADOS_ETAPAS = {EstadoOrden.RECIBIDA, EstadoOrden.EN_DIAGNOSTICO, EstadoOrden.PENDIENTE_APROBACION,
        EstadoOrden.EN_REPARACION, EstadoOrden.EN_CONTROL_CALIDAD, EstadoOrden.FINALIZADA};

    private final PanelComentarios comentarios;
    private final IntConsumer navegar;
    private final JPanel raiz = new JPanel(new BorderLayout(10, 10));
    private final CardLayout cartas = new CardLayout();
    private final JPanel contenidoEtapas = new JPanel(cartas);
    private final JLabel[] pasos = new JLabel[6];
    private final JButton[] botonesPie = new JButton[6];
    private final JButton botonComentarios = Estilo.boton("Comentarios al cliente", Estilo.AZUL, this::abrirComentarios);
    private final JButton botonCancelar = Estilo.boton("Cancelar orden", Estilo.ROJO, this::cancelarOrden);
    private JDialog dialogoComentarios;
    private EstadoOrden estadoActual;
    private int etapaVista = 2;
    private Integer idForzado;

    public FrmGestionOrden(Sesion sesion, IntConsumer navegar) {
        this.navegar = navegar;
        comentarios = new PanelComentarios("Administrador (" + sesion.usuario() + ")");
        setTitle("AutoTech | Atención de la orden");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1150, 780);
        setLocationRelativeTo(null);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        raiz.setBackground(Estilo.FONDO);

        JPanel superior = new JPanel();
        superior.setLayout(new BoxLayout(superior, BoxLayout.Y_AXIS));
        superior.setOpaque(false);
        ordenes.setPreferredSize(new Dimension(460, 30));
        superior.add(Estilo.fila(new JLabel("Orden de trabajo:"), ordenes, Estilo.boton("Actualizar", Estilo.GRIS, this::cargarTodo), botonComentarios, botonCancelar));
        superior.add(indicadorEtapas());
        resumen.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        resumen.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        superior.add(resumen);
        raiz.add(superior, BorderLayout.NORTH);

        contenidoEtapas.setOpaque(false);
        contenidoEtapas.add(etapa(2, panelDiagnostico()), "2");
        contenidoEtapas.add(etapa(3, panelPresupuesto()), "3");
        contenidoEtapas.add(etapa(4, panelReparacion()), "4");
        contenidoEtapas.add(etapa(5, panelCalidad()), "5");
        contenidoEtapas.add(etapa(6, panelEntrega()), "6");
        raiz.add(contenidoEtapas, BorderLayout.CENTER);
        setContentPane(raiz);

        ordenes.addActionListener(e -> {
            if (!cargando) {
                recargarOrden();
            }
        });
        cargarTodo();
    }

    public void ir(int etapa) {
        etapaVista = etapa;
        cartas.show(contenidoEtapas, String.valueOf(etapa));
        pintarEtapas();
        if (idOrden() != null) {
            recargarOrden();
        }
    }

    public void seleccionar(int idOrden) {
        idForzado = idOrden;
        cargarTodo();
    }

    private JPanel indicadorEtapas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        panel.setOpaque(false);
        panel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        for (int i = 0; i < pasos.length; i++) {
            pasos[i] = new JLabel(" " + (i + 1) + ". " + NOMBRES_ETAPAS[i] + " ");
            pasos[i].setOpaque(true);
            pasos[i].setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
            panel.add(pasos[i]);
        }
        pintarEtapas();
        return panel;
    }

    private void pintarEtapas() {
        int actual = -1;
        if (estadoActual != null) {
            for (int i = 0; i < ESTADOS_ETAPAS.length; i++) {
                if (ESTADOS_ETAPAS[i] == estadoActual) {
                    actual = i;
                }
            }
        }
        for (int i = 0; i < pasos.length; i++) {
            JLabel paso = pasos[i];
            if (actual < 0) {
                paso.setBackground(new Color(205, 212, 222));
                paso.setForeground(Estilo.TEXTO);
            } else if (i < actual || (i == actual && i == pasos.length - 1)) {
                paso.setBackground(Estilo.VERDE);
                paso.setForeground(Color.WHITE);
            } else if (i == actual) {
                paso.setBackground(new Color(230, 140, 0));
                paso.setForeground(Color.WHITE);
            } else {
                paso.setBackground(new Color(205, 212, 222));
                paso.setForeground(Estilo.TEXTO);
            }
            boolean vista = i + 1 == etapaVista;
            paso.setBorder(BorderFactory.createMatteBorder(0, 0, 4, 0, vista ? Estilo.PRIMARIO : paso.getBackground()));
        }
    }

    private JPanel etapa(int numero, JComponent contenido) {
        JPanel pagina = new JPanel(new BorderLayout(0, 8));
        pagina.setOpaque(false);
        JLabel titulo = new JLabel(numero + ". " + NOMBRES_ETAPAS[numero - 1]);
        titulo.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
        titulo.setForeground(Estilo.PRIMARIO);
        titulo.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 4));
        pagina.add(titulo, BorderLayout.NORTH);
        pagina.add(contenido, BorderLayout.CENTER);
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        pie.setOpaque(false);
        if (numero == 5) {
            pie.add(Estilo.boton("Volver a reparación", Estilo.GRIS, () -> avanzar(EstadoOrden.EN_REPARACION, 4)));
        }
        if (numero < 6) {
            botonesPie[numero] = Estilo.boton("Siguiente", Estilo.VERDE, () -> pie(numero));
            pie.add(botonesPie[numero]);
        }
        pagina.add(pie, BorderLayout.SOUTH);
        return pagina;
    }

    private void pie(int numero) {
        EstadoOrden estado = estadoActual;
        if (estado == null) {
            Mensajes.advertencia(this, "Seleccione una orden.");
            return;
        }
        switch (numero) {
            case 2 -> {
                if (estado == EstadoOrden.RECIBIDA) {
                    avanzar(EstadoOrden.EN_DIAGNOSTICO, 2);
                } else if (estado == EstadoOrden.EN_DIAGNOSTICO) {
                    avanzar(EstadoOrden.PENDIENTE_APROBACION, 3);
                } else {
                    navegar.accept(3);
                }
            }
            case 3 -> navegar.accept(4);
            case 4 -> {
                if (estado == EstadoOrden.EN_REPARACION) {
                    avanzar(EstadoOrden.EN_CONTROL_CALIDAD, 5);
                } else {
                    navegar.accept(5);
                }
            }
            default -> navegar.accept(6);
        }
    }

    private void actualizarPies() {
        EstadoOrden estado = estadoActual;
        String texto2 = "Siguiente: Cotización";
        if (estado == EstadoOrden.RECIBIDA) {
            texto2 = "Iniciar diagnóstico";
        } else if (estado == EstadoOrden.EN_DIAGNOSTICO) {
            texto2 = "Enviar a cotización";
        }
        botonesPie[2].setText(texto2);
        botonesPie[3].setText("Siguiente: Reparación");
        botonesPie[4].setText(estado == EstadoOrden.EN_REPARACION ? "Pasar a control de calidad" : "Siguiente: Control de calidad");
        botonesPie[5].setText("Siguiente: Entrega y cobro");
    }

    private void avanzar(EstadoOrden destino, int irA) {
        protegido(() -> {
            int id = idOrden();
            accion(null, () -> ordenService.cambiarEstado(id, destino, null), () -> navegar.accept(irA));
        });
    }

    private void cancelarOrden() {
        if (idOrden() == null) {
            Mensajes.advertencia(this, "Seleccione una orden.");
            return;
        }
        if (Mensajes.confirmar(this, "¿Cancelar la orden seleccionada?")) {
            avanzar(EstadoOrden.CANCELADA, etapaVista);
        }
    }

    private void abrirComentarios() {
        Integer id = idOrden();
        if (id == null) {
            Mensajes.advertencia(this, "Seleccione una orden.");
            return;
        }
        if (dialogoComentarios == null) {
            dialogoComentarios = new JDialog(SwingUtilities.getWindowAncestor(raiz), "Comentarios al cliente", java.awt.Dialog.ModalityType.MODELESS);
            dialogoComentarios.setContentPane(comentarios);
            dialogoComentarios.setSize(820, 520);
            dialogoComentarios.setLocationRelativeTo(raiz);
        }
        comentarios.mostrar(id, String.valueOf(ordenes.getSelectedItem()));
        dialogoComentarios.setVisible(true);
    }

    private JPanel vertical() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Estilo.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private JLabel guia(String texto) {
        JLabel etiqueta = new JLabel("<html><b>" + texto + "</b></html>");
        etiqueta.setForeground(Estilo.PRIMARIO);
        etiqueta.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(0, 6, 8, 6));
        return etiqueta;
    }

    private JPanel panelDiagnostico() {
        JPanel panel = vertical();
        panel.add(guia("Escuche la falla reportada y registre la evaluación técnica y las evidencias. Si hace falta sustituir el motor, márquelo."));
        evaluacion.setLineWrap(true);
        evaluacion.setWrapStyleWord(true);
        panel.add(Estilo.fila(new JLabel("Mecánico que diagnostica:"), mecanicoDiagnostico, requiereMotor));
        JScrollPane desplazamientoEvaluacion = new JScrollPane(evaluacion);
        desplazamientoEvaluacion.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        desplazamientoEvaluacion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        panel.add(desplazamientoEvaluacion);
        panel.add(Estilo.fila(Estilo.boton("Guardar diagnóstico", Estilo.VERDE, this::guardarDiagnostico)));
        panel.add(Estilo.fila(new JLabel("Evidencia (opcional):"), descripcionEvidencia, new JLabel("Archivo:"), rutaEvidencia,
                Estilo.boton("Examinar", Estilo.GRIS, this::examinar), Estilo.boton("Agregar evidencia", Estilo.AZUL, this::agregarEvidencia)));
        return panel;
    }

    private JPanel panelPresupuesto() {
        JPanel panel = vertical();
        panel.add(guia("Genere la cotización con servicios, repuestos, descuento e impuesto. Luego registre la decisión del cliente."));
        panel.add(Estilo.fila(new JLabel("Descuento:"), descuento, new JLabel("Autorizado por:"), autorizadoPor, new JLabel("Impuesto:"), impuesto,
                Estilo.boton("Generar cotización", Estilo.VERDE, this::generarPresupuesto)));
        panel.add(Estilo.tabla(tablaPresupuesto));
        panel.add(Estilo.fila(new JLabel("Decisión del cliente:"), Estilo.boton("Aprueba: iniciar reparación", Estilo.AZUL, () -> responder(true)),
                Estilo.boton("Rechaza la cotización", Estilo.ROJO, () -> responder(false))));
        return panel;
    }

    private JComponent panelReparacion() {
        JTabbedPane pestanas = new JTabbedPane();
        JPanel trabajo = vertical();
        trabajo.add(guia("Asigne uno o varios mecánicos (uno principal) y marque cada actividad cuando termine."));
        mecanicoAsignar.setPreferredSize(new Dimension(260, 28));
        trabajo.add(Estilo.fila(new JLabel("Mecánico:"), mecanicoAsignar, esResponsable, Estilo.boton("Asignar a la orden", Estilo.AZUL, this::asignarMecanico)));
        trabajo.add(Estilo.tabla(tablaMecanicos));
        trabajo.add(Estilo.fila(new JLabel("Actividades:"), Estilo.boton("Marcar actividad terminada", Estilo.VERDE, this::completarActividad)));
        trabajo.add(Estilo.tabla(tablaActividades));
        pestanas.addTab("Mecánicos y actividades", trabajo);
        JPanel repuestos = vertical();
        repuestos.add(guia("Agregue los repuestos usados; se descuentan del inventario al registrarlos o al finalizar."));
        repuesto.setPreferredSize(new Dimension(420, 28));
        repuestos.add(Estilo.fila(new JLabel("Repuesto:"), repuesto, new JLabel("Cantidad:"), cantidad,
                Estilo.boton("Agregar repuesto a la orden", Estilo.AZUL, this::agregarConsumo),
                Estilo.boton("Descontar inventario pendiente", Estilo.VERDE, this::registrarConsumos)));
        repuestos.add(Estilo.tabla(tablaConsumos));
        pestanas.addTab("Repuestos", repuestos);
        JPanel motor = new JPanel(new BorderLayout());
        pestanas.addTab("Sustitución de motor", motor);
        pestanas.addChangeListener(e -> {
            if (pestanas.getSelectedComponent() == motor && motor.getComponentCount() == 0) {
                motor.add(Pagina.de(FrmAsistenteCambioMotor::new), BorderLayout.CENTER);
                motor.revalidate();
            }
        });
        return pestanas;
    }

    private JPanel panelCalidad() {
        JPanel panel = vertical();
        panel.add(guia("Verifique el trabajo: apruebe cada control de calidad. Los obligatorios deben estar aprobados para entregar el vehículo."));
        panel.add(Estilo.fila(new JLabel("Inspector:"), inspector, new JLabel("Observaciones:"), observacionControl,
                Estilo.boton("Aprobar control seleccionado", Estilo.VERDE, this::aprobarControl),
                Estilo.boton("Qué falta para finalizar", Estilo.GRIS, this::verPendientes)));
        panel.add(Estilo.tabla(tablaControles));
        panel.add(Estilo.fila(new JLabel("Nuevo control:"), descripcionControl, controlObligatorio, Estilo.boton("Agregar control", Estilo.AZUL, this::agregarControl)));
        return panel;
    }

    private JComponent panelEntrega() {
        JTabbedPane pestanas = new JTabbedPane();
        JPanel proceso = vertical();
        proceso.add(guia("Con la orden en control de calidad, este botón descuenta repuestos, finaliza y factura. Si la orden ya está finalizada, solo genera la factura."));
        progreso.setStringPainted(true);
        progreso.setString("En espera");
        bitacora.setEditable(false);
        proceso.add(Estilo.fila(botonProcesar));
        progreso.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        proceso.add(progreso);
        JScrollPane desplazamientoBitacora = new JScrollPane(bitacora);
        desplazamientoBitacora.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        proceso.add(desplazamientoBitacora);
        pestanas.addTab("Finalizar y facturar", proceso);
        JPanel cobro = vertical();
        cobro.add(guia("Registre pagos parciales o totales; el sistema calcula el saldo e impide sobrepagos."));
        cobro.add(Estilo.fila(datosFactura));
        cobro.add(Estilo.fila(new JLabel("Monto:"), montoPago, new JLabel("Método:"), metodoPago, new JLabel("Referencia:"), referenciaPago,
                Estilo.boton("Registrar pago", Estilo.VERDE, this::registrarPago)));
        cobro.add(Estilo.tabla(tablaPagos));
        pestanas.addTab("Pagos", cobro);
        JPanel historial = vertical();
        historial.add(Estilo.tabla(tablaHistorial));
        pestanas.addTab("Historial de la orden", historial);
        return pestanas;
    }

    private Integer idOrden() {
        Opcion elegida = (Opcion) ordenes.getSelectedItem();
        return elegida == null ? null : elegida.id();
    }

    private void cargarTodo() {
        Integer anterior = idForzado != null ? idForzado : idOrden();
        idForzado = null;
        Tareas.ejecutar(this, () -> new Carga(OpcionesDao.ordenes(), OpcionesDao.mecanicosActivos(), OpcionesDao.mecanicosDisponibles(), OpcionesDao.repuestosActivos(), OpcionesDao.impuestosVigentes()), carga -> {
            cargando = true;
            llenar(ordenes, carga.ordenes());
            llenar(mecanicoDiagnostico, carga.mecanicos());
            llenar(inspector, carga.mecanicos());
            llenar(mecanicoAsignar, carga.disponibles());
            llenar(repuesto, carga.repuestos());
            llenar(impuesto, carga.impuestos());
            for (int i = 0; i < ordenes.getItemCount(); i++) {
                if (ordenes.getItemAt(i).id().equals(anterior)) {
                    ordenes.setSelectedIndex(i);
                }
            }
            cargando = false;
            recargarOrden();
        });
    }

    private void llenar(JComboBox<Opcion> combo, List<Opcion> opciones) {
        combo.removeAllItems();
        opciones.forEach(combo::addItem);
    }

    private void recargarOrden() {
        Integer id = idOrden();
        if (id == null) {
            resumen.setText("No hay órdenes registradas. Cree una en Recepción de orden.");
            comentarios.mostrar(null, "");
            return;
        }
        comentarios.mostrar(id, String.valueOf(ordenes.getSelectedItem()));
        Tareas.ejecutar(this, () -> new Vista(ordenService.obtener(id), ConsultaOrdenDao.resumen(id), ConsultaOrdenDao.historial(id),
                ConsultaOrdenDao.presupuesto(id), ConsultaOrdenDao.mecanicos(id), ConsultaOrdenDao.actividades(id), ConsultaOrdenDao.controles(id), ConsultaOrdenDao.consumos(id),
                ConsultaOrdenDao.pagos(id), facturacion.facturaDeOrden(id), facturacion.totalPagado(id)), this::mostrar);
    }

    private void mostrar(Vista vista) {
        estadoActual = vista.orden().map(OrdenTrabajo::estado).orElse(null);
        pintarEtapas();
        actualizarPies();
        if (!vista.resumen().filas().isEmpty()) {
            Object[] f = vista.resumen().filas().get(0);
            resumen.setText("<html><b>" + f[0] + "</b> | Estado: <b>" + f[1] + "</b> | Cliente: " + f[2] + " | Vehículo: " + f[3] + " " + f[4]
                    + " | Total orden: " + Formato.valor(f[5]) + " | Factura: " + (f[6] == null ? "sin emitir" : f[6] + " por " + Formato.valor(f[7]))
                    + " | Pagado: " + Formato.valor(f[8]) + " | Saldo: " + Formato.valor(f[9]) + "</html>");
        }
        tablaHistorial.setModel(Formato.modelo(vista.historial()));
        tablaPresupuesto.setModel(Formato.modelo(vista.presupuesto()));
        tablaMecanicos.setModel(Formato.modelo(vista.mecanicos()));
        tablaActividades.setModel(Formato.modelo(vista.actividades()));
        tablaControles.setModel(Formato.modelo(vista.controles()));
        tablaConsumos.setModel(Formato.modelo(vista.consumos()));
        tablaPagos.setModel(Formato.modelo(vista.pagos()));
        if (vista.factura().isPresent()) {
            Factura f = vista.factura().get();
            datosFactura.setText("Factura " + f.numeroFactura() + " | Subtotal " + f.subtotal() + " | Descuento " + f.descuento() + " | Impuesto " + f.impuesto()
                    + " (" + f.porcentajeImpuesto() + "%) | Total " + f.total() + " | Pagado " + vista.pagado() + " | Saldo " + f.total().subtract(vista.pagado())
                    + " | " + f.estado());
        } else {
            datosFactura.setText("Sin factura emitida");
        }
    }

    private void accion(String exito, Runnable trabajo) {
        accion(exito, trabajo, null);
    }

    private void accion(String exito, Runnable trabajo, Runnable alFinal) {
        if (idOrden() == null) {
            Mensajes.advertencia(this, "Seleccione una orden.");
            return;
        }
        Tareas.ejecutar(this, trabajo, () -> {
            if (exito != null) {
                Mensajes.info(this, exito);
            }
            if (alFinal != null) {
                alFinal.run();
            }
            cargarTodo();
        });
    }

    private int idDeFila(JTable tabla, String nombre) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            throw new NegocioException("Seleccione una fila de la tabla de " + nombre + ".");
        }
        return Integer.parseInt(String.valueOf(tabla.getValueAt(fila, 0)));
    }

    private Opcion exigir(JComboBox<Opcion> combo, String nombre) {
        Opcion opcion = (Opcion) combo.getSelectedItem();
        if (opcion == null) {
            throw new NegocioException("Seleccione " + nombre + ".");
        }
        return opcion;
    }

    private void protegido(Runnable bloque) {
        try {
            bloque.run();
        } catch (NegocioException e) {
            Mensajes.error(this, e);
        }
    }

    private void verPendientes() {
        Integer id = idOrden();
        if (id == null) {
            return;
        }
        Tareas.ejecutar(this, () -> ordenService.pendientesFinalizacion(id), pendientes ->
                Mensajes.info(this, pendientes.isEmpty() ? "La orden no tiene pendientes para finalizar." : "Pendientes:\n- " + String.join("\n- ", pendientes)));
    }

    private void guardarDiagnostico() {
        protegido(() -> {
            int id = idOrden();
            int mecanico = exigir(mecanicoDiagnostico, "un mecánico").id();
            String texto = evaluacion.getText();
            boolean motor = requiereMotor.isSelected();
            accion("Diagnóstico guardado.", () -> ordenService.guardarDiagnostico(id, mecanico, texto, motor));
        });
    }

    private void examinar() {
        JFileChooser selector = new JFileChooser();
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            rutaEvidencia.setText(selector.getSelectedFile().getAbsolutePath());
        }
    }

    private void agregarEvidencia() {
        protegido(() -> {
            int id = idOrden();
            String descripcionTexto = descripcionEvidencia.getText();
            String ruta = rutaEvidencia.getText();
            accion("Evidencia registrada.", () -> ordenService.agregarEvidencia(id, descripcionTexto, ruta));
        });
    }

    private void generarPresupuesto() {
        protegido(() -> {
            int id = idOrden();
            BigDecimal rebaja;
            try {
                rebaja = descuento.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(descuento.getText().trim());
            } catch (NumberFormatException e) {
                throw new NegocioException("El descuento debe ser un número.");
            }
            String autoriza = autorizadoPor.getText();
            Opcion opcion = (Opcion) impuesto.getSelectedItem();
            Integer idImpuesto = opcion == null ? null : opcion.id();
            accion("Presupuesto generado.", () -> ordenService.generarPresupuesto(id, rebaja, autoriza, idImpuesto));
        });
    }

    private void responder(boolean aprobado) {
        protegido(() -> {
            int id = idOrden();
            accion(null, () -> ordenService.responderPresupuesto(id, aprobado), () -> navegar.accept(aprobado ? 4 : 3));
        });
    }

    private void asignarMecanico() {
        protegido(() -> {
            int id = idOrden();
            int mecanico = exigir(mecanicoAsignar, "un mecánico disponible").id();
            boolean principal = esResponsable.isSelected();
            accion("Mecánico asignado a la orden.", () -> ordenService.asignarMecanico(id, mecanico, principal));
        });
    }

    private void completarActividad() {
        protegido(() -> {
            int actividad = idDeFila(tablaActividades, "actividades");
            accion("Actividad terminada.", () -> ordenService.completarActividad(actividad));
        });
    }

    private void aprobarControl() {
        protegido(() -> {
            int control = idDeFila(tablaControles, "controles");
            int quien = exigir(inspector, "un inspector").id();
            String observaciones = observacionControl.getText();
            accion("Control de calidad aprobado.", () -> ordenService.aprobarControl(control, quien, observaciones));
        });
    }

    private void agregarControl() {
        protegido(() -> {
            int id = idOrden();
            String texto = descripcionControl.getText();
            boolean obligatorio = controlObligatorio.isSelected();
            accion("Control agregado.", () -> ordenService.agregarControl(id, texto, obligatorio));
        });
    }

    private void agregarConsumo() {
        protegido(() -> {
            int id = idOrden();
            int idRepuesto = exigir(repuesto, "un repuesto").id();
            int unidades = (Integer) cantidad.getValue();
            accion("Repuesto agregado a la orden (pendiente de descontar del inventario).", () -> ordenService.agregarConsumo(id, idRepuesto, unidades));
        });
    }

    private void registrarConsumos() {
        protegido(() -> {
            int id = idOrden();
            Tareas.ejecutar(this, () -> ordenService.registrarConsumosPendientes(id), registrados -> {
                Mensajes.info(this, "Consumos descontados del inventario: " + registrados);
                cargarTodo();
            });
        });
    }

    private void registrarPago() {
        protegido(() -> {
            int id = idOrden();
            BigDecimal monto;
            try {
                monto = new BigDecimal(montoPago.getText().trim());
            } catch (NumberFormatException e) {
                throw new NegocioException("El monto debe ser un número.");
            }
            String metodo = (String) metodoPago.getSelectedItem();
            String referencia = referenciaPago.getText();
            accion("Pago registrado.", () -> facturacion.registrarPago(id, monto, metodo, referencia));
        });
    }

    private void procesar() {
        Integer id = idOrden();
        if (id == null) {
            Mensajes.advertencia(this, "Seleccione una orden.");
            return;
        }
        bitacora.setText("");
        botonProcesar.setEnabled(false);
        progreso.setIndeterminate(true);
        progreso.setString("Procesando en segundo plano...");
        new SwingWorker<Factura, String>() {
            @Override
            protected Factura doInBackground() {
                return ordenService.procesarYFacturar(id, this::publish);
            }

            @Override
            protected void process(List<String> mensajes) {
                mensajes.forEach(m -> bitacora.append(m + "\n"));
            }

            @Override
            protected void done() {
                progreso.setIndeterminate(false);
                botonProcesar.setEnabled(true);
                try {
                    Factura factura = get();
                    progreso.setString("Proceso terminado");
                    bitacora.append("Proceso terminado correctamente.\n");
                    Mensajes.info(FrmGestionOrden.this, "Orden finalizada y facturada: " + factura.numeroFactura() + " por " + factura.total());
                    cargarTodo();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    progreso.setString("El proceso fallo y se revirtio");
                    bitacora.append("ERROR: " + e.getCause().getMessage() + "\nNo se aplico ningun cambio.\n");
                    Mensajes.error(FrmGestionOrden.this, e.getCause());
                }
            }
        }.execute();
    }
}
