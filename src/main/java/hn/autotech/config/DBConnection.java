package hn.autotech.config;

import hn.autotech.exception.AccesoDatosException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

public final class DBConnection {
    private static final Deque<Connection> LIBRES = new ArrayDeque<>();
    private static final Properties CONFIG = cargar();

    private DBConnection() {
    }

    private static Properties cargar() {
        Properties propiedades = new Properties();
        Path archivo = Path.of(System.getProperty("user.dir"), "db.properties");
        if (Files.exists(archivo)) {
            try (InputStream entrada = Files.newInputStream(archivo);
                 InputStreamReader lector = new InputStreamReader(entrada, StandardCharsets.UTF_8)) {
                propiedades.load(lector);
            } catch (IOException e) {
                throw new AccesoDatosException("No se pudo leer el archivo db.properties", e);
            }
        }
        return propiedades;
    }

    private static String valor(String clave, String variable) {
        String entorno = System.getenv(variable);
        if (entorno != null && !entorno.isBlank()) {
            return entorno.trim();
        }
        String valor = CONFIG.getProperty(clave);
        return valor == null ? "" : valor.trim();
    }

    public static String usuario() {
        String usuario = valor("user", "AUTOTECH_DB_USER");
        if (usuario.isEmpty() || usuario.startsWith("TU_")) {
            return "AutoTech";
        }
        return usuario.length() > 60 ? usuario.substring(0, 60) : usuario;
    }

    private static Connection abrir() throws SQLException {
        String servidor = valor("server", "AUTOTECH_DB_SERVER");
        String base = valor("database", "AUTOTECH_DB_NAME");
        String usuario = valor("user", "AUTOTECH_DB_USER");
        String clave = valor("password", "AUTOTECH_DB_PASSWORD");
        if (servidor.isEmpty() || base.isEmpty() || usuario.isEmpty() || servidor.startsWith("TU_") || usuario.startsWith("TU_") || clave.startsWith("TU_")) {
            throw new SQLException("Configure los datos de Azure SQL en el archivo db.properties (server, database, user, password).");
        }
        if (!servidor.contains(".")) {
            servidor = servidor + ".database.windows.net";
        }
        String url = "jdbc:sqlserver://" + servidor + ":1433;database=" + base
                + ";encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";
        return DriverManager.getConnection(url, usuario, clave);
    }

    public static Connection getConnection() throws SQLException {
        Connection real = null;
        synchronized (LIBRES) {
            while (real == null && !LIBRES.isEmpty()) {
                Connection candidata = LIBRES.poll();
                if (esValida(candidata)) {
                    real = candidata;
                }
            }
        }
        if (real == null) {
            real = abrir();
        }
        return envolver(real);
    }

    private static boolean esValida(Connection conexion) {
        try {
            return !conexion.isClosed() && conexion.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    private static Connection envolver(Connection real) {
        boolean[] cerrada = {false};
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, metodo, argumentos) -> {
            String nombre = metodo.getName();
            if (nombre.equals("close")) {
                if (!cerrada[0]) {
                    cerrada[0] = true;
                    devolver(real);
                }
                return null;
            }
            if (nombre.equals("isClosed")) {
                return cerrada[0] || real.isClosed();
            }
            if (cerrada[0]) {
                throw new SQLException("La conexion ya fue cerrada.");
            }
            try {
                return metodo.invoke(real, argumentos);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    private static void devolver(Connection real) {
        try {
            if (real.isClosed()) {
                return;
            }
            if (!real.getAutoCommit()) {
                real.rollback();
                real.setAutoCommit(true);
            }
            synchronized (LIBRES) {
                LIBRES.push(real);
            }
        } catch (SQLException e) {
            try {
                real.close();
            } catch (SQLException ignorada) {
                return;
            }
        }
    }
}
