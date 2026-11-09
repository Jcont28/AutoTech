package hn.autotech.dao;

import hn.autotech.model.ResultadoConsulta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcUtil {
    @FunctionalInterface
    public interface Mapeador<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    private JdbcUtil() {
    }

    public static void asignar(PreparedStatement ps, int indice, Object valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.VARCHAR);
        } else if (valor instanceof byte[] bytes) {
            ps.setBytes(indice, bytes);
        } else {
            ps.setObject(indice, valor);
        }
    }

    public static PreparedStatement preparar(Connection c, String sql, Object... parametros) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < parametros.length; i++) {
            asignar(ps, i + 1, parametros[i]);
        }
        return ps;
    }

    public static int actualizar(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = preparar(c, sql, parametros)) {
            return ps.executeUpdate();
        }
    }

    public static int insertar(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < parametros.length; i++) {
                asignar(ps, i + 1, parametros[i]);
            }
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                return claves.next() ? claves.getInt(1) : -1;
            }
        }
    }

    public static <T> List<T> lista(Connection c, String sql, Mapeador<T> mapeador, Object... parametros) throws SQLException {
        try (PreparedStatement ps = preparar(c, sql, parametros); ResultSet rs = ps.executeQuery()) {
            List<T> resultado = new ArrayList<>();
            while (rs.next()) {
                resultado.add(mapeador.mapear(rs));
            }
            return resultado;
        }
    }

    public static <T> Optional<T> unico(Connection c, String sql, Mapeador<T> mapeador, Object... parametros) throws SQLException {
        try (PreparedStatement ps = preparar(c, sql, parametros); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.ofNullable(mapeador.mapear(rs)) : Optional.empty();
        }
    }

    public static boolean existe(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = preparar(c, sql, parametros); ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }

    public static int contar(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = preparar(c, sql, parametros); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static Integer entero(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    public static ResultadoConsulta tabla(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = preparar(c, sql, parametros); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int total = meta.getColumnCount();
            List<String> columnas = new ArrayList<>();
            for (int i = 1; i <= total; i++) {
                columnas.add(meta.getColumnLabel(i));
            }
            List<Object[]> filas = new ArrayList<>();
            while (rs.next()) {
                Object[] fila = new Object[total];
                for (int i = 0; i < total; i++) {
                    fila[i] = rs.getObject(i + 1);
                }
                filas.add(fila);
            }
            return new ResultadoConsulta(columnas, filas);
        }
    }
}
