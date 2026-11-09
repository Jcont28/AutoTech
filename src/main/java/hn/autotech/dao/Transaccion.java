package hn.autotech.dao;

import hn.autotech.config.DBConnection;
import hn.autotech.exception.AccesoDatosException;
import hn.autotech.exception.TraductorSql;

import java.sql.Connection;
import java.sql.SQLException;

public final class Transaccion {
    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    private Transaccion() {
    }

    public static <T> T ejecutar(Operacion<T> operacion) {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(c);
                c.commit();
                return resultado;
            } catch (SQLException e) {
                deshacer(c);
                throw new AccesoDatosException(TraductorSql.mensaje(e), e);
            } catch (RuntimeException e) {
                deshacer(c);
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(TraductorSql.mensaje(e), e);
        }
    }

    public static <T> T consulta(Operacion<T> operacion) {
        try (Connection c = DBConnection.getConnection()) {
            return operacion.ejecutar(c);
        } catch (SQLException e) {
            throw new AccesoDatosException(TraductorSql.mensaje(e), e);
        }
    }

    private static void deshacer(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignorada) {
            return;
        }
    }
}
