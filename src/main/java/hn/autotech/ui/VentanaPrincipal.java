package hn.autotech.ui;

import hn.autotech.model.Sesion;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class VentanaPrincipal extends JFrame {
    private static final Color LATERAL = new Color(10, 30, 52);
    private static final Color ACTIVO = new Color(255, 196, 45);
    private static final String ETAPA_RECEPCION = "1. Recepción";
    private static final String[] ETAPAS = {ETAPA_RECEPCION, "2. Diagnóstico", "3. Cotización", "4. Reparación", "5. Control de calidad", "6. Entrega y cobro"};
    private static final String CARTA_ORDEN = "orden";

    private final Sesion sesion;
    private final CardLayout cartas = new CardLayout();
    private final JPanel centro = new JPanel(cartas);
    private final Map<String, Supplier<JComponent>> fabricas = new LinkedHashMap<>();
    private final Map<String, JButton> botones = new LinkedHashMap<>();
    private final Map<String, Iconos.Tipo> iconos = new LinkedHashMap<>();
    private final Map<String, Boolean> creadas = new LinkedHashMap<>();
    private FrmGestionOrden gestion;

    public VentanaPrincipal(Sesion sesion) {
        this.sesion = sesion;
        setTitle("AutoTech Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 820);
        setLocationRelativeTo(null);
        Mensajes.principal(this);

        JPanel raiz = new JPanel(new BorderLayout());
        Banner encabezado = new Banner("AutoTech Manager", "Administrador del taller: " + sesion.usuario(), false);
        encabezado.agregarDerecha(Estilo.fila(Estilo.boton("Cerrar sesión", Estilo.ROJO, Iconos.de(Iconos.Tipo.CANDADO, 16, Color.WHITE), this::cerrarSesion)));
        raiz.add(encabezado, BorderLayout.NORTH);
        raiz.add(lateral(), BorderLayout.WEST);
        centro.setBackground(Estilo.FONDO);
        centro.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);
        mostrar("Inicio");
    }

    private JPanel lateral() {
        JPanel panel = new JPanel();
        panel.setBackground(LATERAL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        panel.setPreferredSize(new Dimension(240, 0));
        item(panel, "Inicio", Iconos.Tipo.DOCUMENTO, this::inicio);
        seccion(panel, "FLUJO DEL TALLER");
        item(panel, ETAPAS[0], Iconos.Tipo.AUTO, () -> Pagina.pestanas(new String[]{"Cliente", "Vehículo", "Orden de ingreso"},
                FrmClientes::new, FrmRegistroVehiculo::new, () -> new FrmRecepcionOrden(this::ordenCreada)));
        item(panel, ETAPAS[1], Iconos.Tipo.LLAVE, this::componenteOrden);
        item(panel, ETAPAS[2], Iconos.Tipo.DOCUMENTO, this::componenteOrden);
        item(panel, ETAPAS[3], Iconos.Tipo.ENGRANAJE, this::componenteOrden);
        item(panel, ETAPAS[4], Iconos.Tipo.CANDADO, this::componenteOrden);
        item(panel, ETAPAS[5], Iconos.Tipo.DINERO, this::componenteOrden);
        seccion(panel, "CATÁLOGOS");
        item(panel, "Servicios", Iconos.Tipo.ENGRANAJE, () -> Pagina.pestanas(new String[]{"Servicios", "Repuestos por servicio", "Impuestos"},
                FrmServicios::new, FrmServicioRepuesto::new, FrmConfiguracionImpuestos::new));
        item(panel, "Mecánicos", Iconos.Tipo.PERSONA, () -> Pagina.pestanas(new String[]{"Mecánicos", "Especialidades", "Especialidades por mecánico"},
                FrmMecanicos::new, FrmEspecialidades::new, FrmMecanicoEspecialidad::new));
        item(panel, "Inventario", Iconos.Tipo.CAJA, () -> Pagina.pestanas(new String[]{"Repuestos", "Proveedores", "Categorías", "Entradas y salidas", "Motores", "Compatibilidad"},
                FrmRepuestos::new, FrmProveedores::new, FrmCategoriasRepuesto::new, FrmMovimientosInventario::new, FrmMotores::new, FrmCompatibilidadMotor::new));
        seccion(panel, "CONSULTAS");
        item(panel, "Reportes e historial", Iconos.Tipo.GRAFICO, () -> Pagina.de(FrmReportes::new));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JComponent componenteOrden() {
        if (gestion == null) {
            gestion = new FrmGestionOrden(sesion, etapa -> mostrar(ETAPAS[etapa - 1]));
        }
        return Pagina.de(() -> gestion);
    }

    private int etapaDe(String nombre) {
        for (int i = 1; i < ETAPAS.length; i++) {
            if (ETAPAS[i].equals(nombre)) {
                return i + 1;
            }
        }
        return 0;
    }

    private void ordenCreada(int idOrden) {
        if (!creadas.containsKey(CARTA_ORDEN)) {
            creadas.put(CARTA_ORDEN, true);
            centro.add(componenteOrden(), CARTA_ORDEN);
        }
        gestion.seleccionar(idOrden);
        mostrar(ETAPAS[1]);
    }

    private void seccion(JPanel panel, String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(new Color(150, 175, 205));
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 11));
        etiqueta.setBorder(BorderFactory.createEmptyBorder(16, 8, 6, 0));
        etiqueta.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(etiqueta);
    }

    private void item(JPanel panel, String nombre, Iconos.Tipo icono, Supplier<JComponent> fabrica) {
        JButton boton = new JButton(nombre, Iconos.de(icono, 20, Color.WHITE));
        boton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        boton.setHorizontalAlignment(JButton.LEFT);
        boton.setIconTextGap(12);
        boton.setForeground(Color.WHITE);
        boton.setBackground(LATERAL);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        boton.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setAlignmentX(LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        boton.addActionListener(e -> mostrar(nombre));
        fabricas.put(nombre, fabrica);
        botones.put(nombre, boton);
        iconos.put(nombre, icono);
        panel.add(boton);
    }

    private void mostrar(String nombre) {
        int etapa = etapaDe(nombre);
        String carta = etapa > 0 ? CARTA_ORDEN : nombre;
        if (!creadas.containsKey(carta)) {
            creadas.put(carta, true);
            centro.add(fabricas.get(nombre).get(), carta);
        }
        if (etapa > 0) {
            gestion.ir(etapa);
        }
        cartas.show(centro, carta);
        botones.forEach((clave, boton) -> {
            boolean activo = clave.equals(nombre);
            boton.setBackground(activo ? ACTIVO : LATERAL);
            boton.setForeground(activo ? Estilo.TEXTO : Color.WHITE);
            boton.setIcon(Iconos.de(iconos.get(clave), 20, activo ? Estilo.TEXTO : Color.WHITE));
        });
    }

    private JComponent inicio() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        JLabel titulo = new JLabel("Flujo de atención de un vehículo");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titulo.setForeground(Estilo.PRIMARIO);
        panel.add(titulo, BorderLayout.NORTH);
        JPanel pasos = new JPanel(new GridLayout(2, 3, 14, 14));
        pasos.setOpaque(false);
        pasos.add(paso("1", ETAPAS[0], "Registre al cliente y su vehículo, y cree la orden de ingreso con los síntomas y servicios.", Estilo.AZUL, Iconos.Tipo.AUTO));
        pasos.add(paso("2", ETAPAS[1], "Registre la evaluación técnica y las evidencias. Marque si se requiere cambio de motor.", Estilo.AZUL, Iconos.Tipo.LLAVE));
        pasos.add(paso("3", ETAPAS[2], "Genere la cotización con repuestos, mano de obra e impuestos y registre la decisión del cliente.", Estilo.AZUL, Iconos.Tipo.DOCUMENTO));
        pasos.add(paso("4", ETAPAS[3], "Asigne mecánicos, registre actividades y repuestos, o la sustitución del motor.", Estilo.VERDE, Iconos.Tipo.ENGRANAJE));
        pasos.add(paso("5", ETAPAS[4], "Apruebe los controles de calidad antes de entregar el vehículo.", Estilo.VERDE, Iconos.Tipo.CANDADO));
        pasos.add(paso("6", ETAPAS[5], "Finalice y facture la orden, registre los pagos y consulte el historial del vehículo.", Estilo.GRIS, Iconos.Tipo.DINERO));
        panel.add(pasos, BorderLayout.CENTER);
        return panel;
    }

    private JPanel paso(String numero, String nombre, String detalle, Color color, Iconos.Tipo icono) {
        JPanel tarjeta = Estilo.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 8));
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        arriba.setOpaque(false);
        JLabel circulo = new JLabel(numero, JLabel.CENTER);
        circulo.setOpaque(true);
        circulo.setBackground(color);
        circulo.setForeground(Color.WHITE);
        circulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        circulo.setPreferredSize(new Dimension(40, 40));
        arriba.add(circulo);
        arriba.add(new JLabel(Iconos.de(icono, 40, color)));
        tarjeta.add(arriba, BorderLayout.NORTH);
        JLabel texto = new JLabel("<html><div style='width:230px'><b style='font-size:15px'>" + nombre.substring(3) + "</b><br><br>" + detalle + "</div></html>");
        texto.setVerticalAlignment(JLabel.TOP);
        tarjeta.add(texto, BorderLayout.CENTER);
        tarjeta.add(Estilo.boton("Ir a " + nombre.substring(3), color, () -> mostrar(nombre)), BorderLayout.SOUTH);
        return tarjeta;
    }

    private void cerrarSesion() {
        new FrmLogin().setVisible(true);
        dispose();
    }
}
