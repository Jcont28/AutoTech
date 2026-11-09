package hn.autotech.ui;

import hn.autotech.dao.VehiculoDao;
import hn.autotech.exception.NegocioException;
import hn.autotech.model.ResultadoConsulta;
import hn.autotech.model.Vehiculo;
import hn.autotech.dao.OpcionesDao;
import hn.autotech.service.SeguimientoService;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class FrmRegistroVehiculo extends JFrame {
    private final SeguimientoService seguimiento = new SeguimientoService();
    private final JTextField cedula = new JTextField(18);
    private final JLabel cliente = new JLabel("Escriba la cédula y presione Buscar cliente");
    private final JTextField placa = new JTextField(14);
    private final JTextField vin = new JTextField(18);
    private final JTextField marca = new JTextField(14);
    private final JTextField modelo = new JTextField(14);
    private final JTextField anio = new JTextField(6);
    private final JTextField color = new JTextField(12);
    private final JTextField kilometraje = new JTextField("0", 10);
    private final JComboBox<String> combustible = new JComboBox<>(new String[]{"Gasolina", "Diesel", "Hibrido", "Electrico", "Gas"});
    private final JTextField motor = new JTextField(18);
    private final JTable tabla = new JTable();
    private Integer idCliente;

    public FrmRegistroVehiculo() {
        setTitle("AutoTech | Registro de vehículo");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        JPanel raiz = new JPanel(new BorderLayout(12, 12));
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        raiz.setBackground(Estilo.FONDO);
        raiz.add(Estilo.titulo("Registro de vehículo"), BorderLayout.NORTH);

        JPanel formulario = Estilo.tarjeta();
        formulario.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        formulario.add(etiqueta("Cédula del cliente"), c);
        c.gridx = 1;
        formulario.add(cedula, c);
        c.gridx = 2;
        formulario.add(Estilo.boton("Buscar cliente", Estilo.AZUL, Iconos.de(Iconos.Tipo.PERSONA, 16, Color.WHITE), this::buscar), c);
        c.gridx = 3;
        cliente.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cliente.setForeground(Estilo.ROJO);
        formulario.add(cliente, c);
        campo(formulario, 1, 0, "Placa", placa);
        campo(formulario, 1, 2, "VIN", vin);
        campo(formulario, 2, 0, "Marca", marca);
        campo(formulario, 2, 2, "Modelo", modelo);
        campo(formulario, 3, 0, "Anio", anio);
        campo(formulario, 3, 2, "Color", color);
        campo(formulario, 4, 0, "Kilometraje", kilometraje);
        campo(formulario, 4, 2, "Combustible", combustible);
        campo(formulario, 5, 0, "Número de motor", motor);
        c.gridx = 2;
        c.gridy = 6;
        c.gridwidth = 2;
        formulario.add(Estilo.fila(Estilo.boton("Guardar vehículo", Estilo.VERDE, Iconos.de(Iconos.Tipo.AUTO, 20, Color.WHITE), this::guardar),
                Estilo.boton("Limpiar", Estilo.GRIS, this::limpiar)), c);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(formulario, BorderLayout.NORTH);
        JLabel titulo = new JLabel("Vehículos registrados del cliente");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JPanel inferior = new JPanel(new BorderLayout(0, 6));
        inferior.setOpaque(false);
        inferior.add(titulo, BorderLayout.NORTH);
        inferior.add(Estilo.tabla(tabla), BorderLayout.CENTER);
        centro.add(inferior, BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);
        cedula.addActionListener(e -> buscar());
    }

    private JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(Etiquetas.acentuar(texto));
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return etiqueta;
    }

    private void campo(JPanel panel, int fila, int columna, String texto, Component componente) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = fila;
        c.gridx = columna;
        panel.add(etiqueta(texto), c);
        c.gridx = columna + 1;
        c.weightx = 1;
        panel.add(componente, c);
    }

    private void buscar() {
        String texto = cedula.getText();
        idCliente = null;
        Tareas.ejecutar(this, () -> {
            int id = seguimiento.idClientePorCedula(texto);
            return new Object[]{id, seguimiento.nombreCliente(id), OpcionesDao.vehiculosDeClienteTabla(id)};
        }, datos -> {
            idCliente = (Integer) datos[0];
            cliente.setForeground(Estilo.VERDE);
            cliente.setText("Cliente: " + datos[1]);
            tabla.setModel(Formato.modelo((ResultadoConsulta) datos[2]));
        }, error -> {
            cliente.setForeground(Estilo.ROJO);
            cliente.setText("Cliente no encontrado");
            Mensajes.error(this, error);
        });
    }

    private void guardar() {
        if (idCliente == null) {
            Mensajes.advertencia(this, "Busque primero al cliente por su número de cédula.");
            return;
        }
        Vehiculo nuevo;
        try {
            nuevo = new Vehiculo(0, idCliente, requerido(placa, "la placa"), requerido(vin, "el VIN"), requerido(marca, "la marca"), requerido(modelo, "el modelo"),
                    numero(anio, "el año"), color.getText().trim().isEmpty() ? null : color.getText().trim(), numero(kilometraje, "el kilometraje"),
                    (String) combustible.getSelectedItem(), motor.getText().trim().isEmpty() ? null : motor.getText().trim(), true);
        } catch (NegocioException e) {
            Mensajes.advertencia(this, e.getMessage());
            return;
        }
        Vehiculo vehiculo = nuevo;
        Tareas.ejecutar(this, () -> new VehiculoDao().guardar(vehiculo), id -> {
            Mensajes.info(this, "Vehículo registrado correctamente.");
            limpiar();
            buscar();
        });
    }

    private String requerido(JTextField campo, String nombre) {
        String valor = campo.getText().trim();
        if (valor.isEmpty()) {
            throw new NegocioException("Escriba " + nombre + ".");
        }
        return valor;
    }

    private int numero(JTextField campo, String nombre) {
        try {
            return Integer.parseInt(requerido(campo, nombre));
        } catch (NumberFormatException e) {
            throw new NegocioException("Escriba un número válido para " + nombre + ".");
        }
    }

    private void limpiar() {
        placa.setText("");
        vin.setText("");
        marca.setText("");
        modelo.setText("");
        anio.setText("");
        color.setText("");
        kilometraje.setText("0");
        motor.setText("");
    }
}
