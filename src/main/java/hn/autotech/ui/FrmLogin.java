package hn.autotech.ui;

import hn.autotech.model.Rol;
import hn.autotech.model.Sesion;
import hn.autotech.model.SolicitudRegistro;
import hn.autotech.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class FrmLogin extends JFrame {
    private final AuthService auth = new AuthService();
    private final JTextField usuario = new JTextField(20);
    private final JPasswordField clave = new JPasswordField(20);
    private final JTextField regCedula = new JTextField(20);
    private final JTextField regNombre = new JTextField(20);
    private final JTextField regApellido = new JTextField(20);
    private final JTextField regTelefono = new JTextField(20);
    private final JTextField regCorreo = new JTextField(20);
    private final JTextField regUsuario = new JTextField(20);
    private final JPasswordField regClave = new JPasswordField(20);
    private final JPasswordField regConfirmar = new JPasswordField(20);
    private final JButton botonIngresar;
    private final JButton botonRegistrar;
    private final JLabel estado = new JLabel(" ");

    public FrmLogin() {
        setTitle("AutoTech Manager | Acceso");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(new Banner("AutoTech", "Taller mecánico", true), BorderLayout.WEST);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(new Font("Segoe UI", Font.BOLD, 14));
        botonIngresar = Estilo.boton("Ingresar", Estilo.VERDE, Iconos.de(Iconos.Tipo.CANDADO, 18, Color.WHITE), this::ingresar);
        botonRegistrar = Estilo.boton("Crear mi cuenta", Estilo.AZUL, Iconos.de(Iconos.Tipo.PERSONA, 18, Color.WHITE), this::registrar);
        pestanas.addTab("Iniciar sesión", panelIngreso());
        pestanas.addTab("Soy cliente: registrarme", panelRegistro());
        JPanel derecha = new JPanel(new BorderLayout(0, 8));
        derecha.setBackground(Estilo.FONDO);
        derecha.setBorder(BorderFactory.createEmptyBorder(24, 28, 16, 28));
        JLabel bienvenida = new JLabel("Bienvenido");
        bienvenida.setFont(new Font("Segoe UI", Font.BOLD, 28));
        bienvenida.setForeground(Estilo.PRIMARIO);
        derecha.add(bienvenida, BorderLayout.NORTH);
        derecha.add(pestanas, BorderLayout.CENTER);
        estado.setForeground(Estilo.ROJO);
        estado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        derecha.add(estado, BorderLayout.SOUTH);
        raiz.add(derecha, BorderLayout.CENTER);
        setContentPane(raiz);
        getRootPane().setDefaultButton(botonIngresar);
        clave.addActionListener(e -> ingresar());
    }

    private JPanel formulario() {
        JPanel panel = Estilo.tarjeta();
        panel.setLayout(new GridBagLayout());
        return panel;
    }

    private void fila(JPanel panel, int fila, String texto, Component campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 8, 7, 8);
        c.gridx = 0;
        c.gridy = fila;
        c.anchor = GridBagConstraints.WEST;
        JLabel etiqueta = new JLabel(Etiquetas.acentuar(texto));
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(etiqueta, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, c);
    }

    private JPanel panelIngreso() {
        JPanel panel = formulario();
        fila(panel, 0, "Usuario", usuario);
        fila(panel, 1, "Contrasena", clave);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.insets = new Insets(16, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;
        panel.add(botonIngresar, c);
        c.gridy = 3;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        JLabel ayuda = new JLabel("Cliente nuevo: use la pestana de registro con su cédula.");
        ayuda.setForeground(Estilo.GRIS);
        panel.add(ayuda, c);
        return panel;
    }

    private JPanel panelRegistro() {
        JPanel panel = formulario();
        fila(panel, 0, "Número de cédula", regCedula);
        fila(panel, 1, "Nombre", regNombre);
        fila(panel, 2, "Apellido", regApellido);
        fila(panel, 3, "Telefono", regTelefono);
        fila(panel, 4, "Correo (opcional)", regCorreo);
        fila(panel, 5, "Usuario", regUsuario);
        fila(panel, 6, "Contrasena", regClave);
        fila(panel, 7, "Confirmar contraseña", regConfirmar);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 8;
        c.insets = new Insets(14, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;
        c.weighty = 1;
        panel.add(botonRegistrar, c);
        return panel;
    }

    private void ingresar() {
        estado.setText(" ");
        String nombre = usuario.getText();
        String contrasena = new String(clave.getPassword());
        botonIngresar.setEnabled(false);
        Tareas.ejecutar(this, () -> auth.autenticar(nombre, contrasena), this::entrar, this::falla);
    }

    private void registrar() {
        estado.setText(" ");
        String contrasena = new String(regClave.getPassword());
        if (!contrasena.equals(new String(regConfirmar.getPassword()))) {
            estado.setText("Las contraseñas no coinciden.");
            return;
        }
        SolicitudRegistro solicitud = new SolicitudRegistro(regCedula.getText(), regNombre.getText(), regApellido.getText(), regTelefono.getText(),
                regCorreo.getText(), regUsuario.getText(), contrasena);
        botonRegistrar.setEnabled(false);
        Tareas.ejecutar(this, () -> auth.registrarCliente(solicitud), this::entrar, this::falla);
    }

    private void falla(Throwable error) {
        botonIngresar.setEnabled(true);
        botonRegistrar.setEnabled(true);
        if (error instanceof hn.autotech.exception.NegocioException) {
            estado.setText(error.getMessage());
        } else {
            Mensajes.error(this, error);
        }
    }

    private void entrar(Sesion sesion) {
        JFrame siguiente = sesion.rol() == Rol.ADMINISTRADOR ? new VentanaPrincipal(sesion) : new VentanaCliente(sesion);
        siguiente.setVisible(true);
        dispose();
    }
}
