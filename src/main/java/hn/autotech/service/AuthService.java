package hn.autotech.service;

import hn.autotech.dao.JdbcUtil;
import hn.autotech.dao.Transaccion;
import hn.autotech.exception.NegocioException;
import hn.autotech.model.Rol;
import hn.autotech.model.Sesion;
import hn.autotech.model.SolicitudRegistro;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Optional;

public class AuthService {
    private static final String ADMIN_USUARIO = "admin";
    private static final String ADMIN_CLAVE = "admin123";
    private static final int ITERACIONES = 120_000;
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static volatile boolean administradorVerificado;

    private record Credencial(int id, String usuario, byte[] salt, byte[] hash, String rol, Integer idCliente, boolean activo, String nombre) {
    }

    public Sesion autenticar(String usuario, String clave) {
        String nombre = usuario == null ? "" : usuario.trim();
        if (nombre.isEmpty() || clave == null || clave.isEmpty()) {
            throw new NegocioException("Escriba su usuario y su contraseña.");
        }
        asegurarAdministrador();
        Optional<Credencial> encontrada = Transaccion.consulta(c -> JdbcUtil.unico(c,
                "SELECT u.IdUsuario, u.NombreUsuario, u.Salt, u.ClaveHash, u.Rol, u.IdCliente, u.Activo, "
                        + "COALESCE(cl.Nombre + N' ' + cl.Apellido, u.NombreUsuario) "
                        + "FROM Usuarios u LEFT JOIN Clientes cl ON cl.IdCliente = u.IdCliente WHERE u.NombreUsuario = ?",
                rs -> {
                    int idCliente = rs.getInt(6);
                    Integer cliente = rs.wasNull() ? null : idCliente;
                    return new Credencial(rs.getInt(1), rs.getString(2), rs.getBytes(3), rs.getBytes(4), rs.getString(5), cliente, rs.getBoolean(7), rs.getString(8));
                }, nombre));
        if (encontrada.isEmpty() || !encontrada.get().activo() || !MessageDigest.isEqual(encontrada.get().hash(), derivar(clave, encontrada.get().salt()))) {
            throw new NegocioException("Usuario o contraseña incorrectos.");
        }
        Credencial c = encontrada.get();
        return new Sesion(c.id(), c.usuario(), Rol.desde(c.rol()), c.idCliente(), c.nombre());
    }

    public void asegurarAdministrador() {
        if (administradorVerificado) {
            return;
        }
        Transaccion.ejecutar(c -> {
            if (JdbcUtil.existe(c, "SELECT 1 FROM Usuarios WHERE Rol = ?", Rol.ADMINISTRADOR.texto())) {
                return null;
            }
            byte[] salt = nuevaSal();
            JdbcUtil.actualizar(c, "INSERT INTO Usuarios (NombreUsuario, Salt, ClaveHash, Rol) VALUES (?, ?, ?, ?)",
                    ADMIN_USUARIO, salt, derivar(ADMIN_CLAVE, salt), Rol.ADMINISTRADOR.texto());
            return null;
        });
        administradorVerificado = true;
    }

    public Sesion registrarCliente(SolicitudRegistro s) {
        String cedula = requerido(s.cedula(), "la cedula");
        String nombre = requerido(s.nombre(), "el nombre");
        String apellido = requerido(s.apellido(), "el apellido");
        String telefono = requerido(s.telefono(), "el telefono");
        String usuario = requerido(s.usuario(), "el usuario");
        if (usuario.length() < 4) {
            throw new NegocioException("El usuario debe tener al menos 4 caracteres.");
        }
        if (s.clave() == null || s.clave().length() < 6) {
            throw new NegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
        String correo = s.correo() == null || s.correo().isBlank() ? null : s.correo().trim();
        asegurarAdministrador();
        return Transaccion.ejecutar(c -> {
            if (JdbcUtil.existe(c, "SELECT 1 FROM Usuarios WHERE NombreUsuario = ?", usuario)) {
                throw new NegocioException("Ese nombre de usuario ya esta en uso.");
            }
            Optional<Integer> existente = JdbcUtil.unico(c, "SELECT IdCliente FROM Clientes WHERE Identidad = ?", rs -> rs.getInt(1), cedula);
            int idCliente;
            if (existente.isPresent()) {
                idCliente = existente.get();
                if (JdbcUtil.existe(c, "SELECT 1 FROM Usuarios WHERE IdCliente = ?", idCliente)) {
                    throw new NegocioException("Esa cédula ya tiene una cuenta registrada.");
                }
            } else {
                idCliente = JdbcUtil.insertar(c, "INSERT INTO Clientes (Nombre, Apellido, Identidad, Telefono, Correo) VALUES (?, ?, ?, ?, ?)",
                        nombre, apellido, cedula, telefono, correo);
            }
            byte[] salt = nuevaSal();
            int idUsuario = JdbcUtil.insertar(c, "INSERT INTO Usuarios (NombreUsuario, Salt, ClaveHash, Rol, IdCliente) VALUES (?, ?, ?, ?, ?)",
                    usuario, salt, derivar(s.clave(), salt), Rol.CLIENTE.texto(), idCliente);
            String mostrado = JdbcUtil.unico(c, "SELECT Nombre + N' ' + Apellido FROM Clientes WHERE IdCliente = ?", rs -> rs.getString(1), idCliente).orElse(usuario);
            return new Sesion(idUsuario, usuario, Rol.CLIENTE, idCliente, mostrado);
        });
    }

    private static String requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new NegocioException("Escriba " + campo + ".");
        }
        return valor.trim();
    }

    private static byte[] nuevaSal() {
        byte[] salt = new byte[16];
        ALEATORIO.nextBytes(salt);
        return salt;
    }

    private static byte[] derivar(String clave, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), salt, ITERACIONES, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }
}
