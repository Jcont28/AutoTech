package hn.autotech.dao;

import hn.autotech.exception.NegocioException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public abstract class RepositorioJdbc<T> implements Repositorio<T> {
    protected abstract String tabla();

    protected abstract String columnaId();

    protected abstract String[] columnas();

    protected abstract Object[] valores(T entidad);

    protected abstract T mapear(java.sql.ResultSet rs) throws SQLException;

    protected abstract int idDe(T entidad);

    protected String[] columnasLectura() {
        String[] propias = columnas();
        String[] todas = new String[propias.length + 1];
        todas[0] = columnaId();
        System.arraycopy(propias, 0, todas, 1, propias.length);
        return todas;
    }

    protected String seleccion() {
        return "SELECT " + String.join(", ", columnasLectura()) + " FROM " + tabla();
    }

    @Override
    public List<T> listar() {
        return Transaccion.consulta(this::listar);
    }

    public List<T> listar(Connection c) throws SQLException {
        return JdbcUtil.lista(c, seleccion() + " ORDER BY " + columnaId(), this::mapear);
    }

    @Override
    public Optional<T> buscar(int id) {
        return Transaccion.consulta(c -> buscar(c, id));
    }

    public Optional<T> buscar(Connection c, int id) throws SQLException {
        return JdbcUtil.unico(c, seleccion() + " WHERE " + columnaId() + " = ?", this::mapear, id);
    }

    @Override
    public int guardar(T entidad) {
        return Transaccion.ejecutar(c -> guardar(c, entidad));
    }

    public int guardar(Connection c, T entidad) throws SQLException {
        String[] columnas = columnas();
        String marcas = String.join(", ", java.util.Collections.nCopies(columnas.length, "?"));
        String sql = "INSERT INTO " + tabla() + " (" + String.join(", ", columnas) + ") VALUES (" + marcas + ")";
        return JdbcUtil.insertar(c, sql, valores(entidad));
    }

    @Override
    public void actualizar(T entidad) {
        Transaccion.ejecutar(c -> {
            actualizar(c, entidad);
            return null;
        });
    }

    public void actualizar(Connection c, T entidad) throws SQLException {
        String[] columnas = columnas();
        StringBuilder asignaciones = new StringBuilder();
        for (int i = 0; i < columnas.length; i++) {
            if (i > 0) {
                asignaciones.append(", ");
            }
            asignaciones.append(columnas[i]).append(" = ?");
        }
        Object[] valores = valores(entidad);
        Object[] parametros = java.util.Arrays.copyOf(valores, valores.length + 1);
        parametros[valores.length] = idDe(entidad);
        int filas = JdbcUtil.actualizar(c, "UPDATE " + tabla() + " SET " + asignaciones + " WHERE " + columnaId() + " = ?", parametros);
        if (filas == 0) {
            throw new NegocioException("El registro no existe en " + tabla() + ".");
        }
    }

    @Override
    public void eliminar(int id) {
        Transaccion.ejecutar(c -> {
            eliminar(c, id);
            return null;
        });
    }

    public void eliminar(Connection c, int id) throws SQLException {
        int filas = JdbcUtil.actualizar(c, "DELETE FROM " + tabla() + " WHERE " + columnaId() + " = ?", id);
        if (filas == 0) {
            throw new NegocioException("El registro no existe en " + tabla() + ".");
        }
    }
}
