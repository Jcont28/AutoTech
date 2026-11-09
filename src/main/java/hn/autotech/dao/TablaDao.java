package hn.autotech.dao;

import hn.autotech.exception.NegocioException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TablaDao {
    private final EsquemaTabla esquema;
    private final List<Campo> campos;

    public TablaDao(EsquemaTabla esquema, List<Campo> campos) {
        this.esquema = esquema;
        this.campos = campos;
    }

    public List<Registro> listar() {
        return Transaccion.consulta(this::listar);
    }

    public List<Registro> listar(Connection c) throws SQLException {
        List<String> columnas = new ArrayList<>();
        if (esquema.identidad()) {
            columnas.add(esquema.claves().get(0));
        }
        for (Campo campo : campos) {
            columnas.add(campo.nombre());
        }
        if (esquema.versionada()) {
            columnas.add("RowVer");
        }
        String orden = esquema.identidad() ? esquema.claves().get(0) + " DESC" : String.join(", ", esquema.claves());
        String sql = "SELECT TOP (1000) " + String.join(", ", columnas) + " FROM " + esquema.tabla() + " ORDER BY " + orden;
        return JdbcUtil.lista(c, sql, this::leer);
    }

    private Registro leer(ResultSet rs) throws SQLException {
        int desplazamiento = esquema.identidad() ? 1 : 0;
        Object[] valores = new Object[campos.size()];
        for (int i = 0; i < valores.length; i++) {
            valores[i] = rs.getObject(desplazamiento + i + 1);
        }
        Object[] claves;
        if (esquema.identidad()) {
            claves = new Object[]{rs.getObject(1)};
        } else {
            claves = new Object[esquema.claves().size()];
            for (int k = 0; k < claves.length; k++) {
                claves[k] = valores[posicion(esquema.claves().get(k))];
            }
        }
        byte[] version = esquema.versionada() ? rs.getBytes(desplazamiento + valores.length + 1) : null;
        return new Registro(valores, claves, version);
    }

    private int posicion(String nombre) {
        for (int i = 0; i < campos.size(); i++) {
            if (campos.get(i).nombre().equals(nombre)) {
                return i;
            }
        }
        throw new IllegalStateException("El campo " + nombre + " no existe en el formulario de " + esquema.tabla());
    }

    public int insertar(Map<String, Object> valores) {
        return Transaccion.ejecutar(c -> insertar(c, valores));
    }

    public int insertar(Connection c, Map<String, Object> valores) throws SQLException {
        List<String> columnas = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        for (Campo campo : campos) {
            Object valor = valores.get(campo.nombre());
            if (!campo.generado() && valor != null) {
                columnas.add(campo.nombre());
                parametros.add(valor);
            }
        }
        if (columnas.isEmpty()) {
            throw new NegocioException("Capture al menos un dato antes de guardar.");
        }
        String marcas = String.join(", ", java.util.Collections.nCopies(columnas.size(), "?"));
        String sql = "INSERT INTO " + esquema.tabla() + " (" + String.join(", ", columnas) + ") VALUES (" + marcas + ")";
        return JdbcUtil.insertar(c, sql, parametros.toArray());
    }

    public void actualizar(Registro anterior, Map<String, Object> valores) {
        Transaccion.ejecutar(c -> {
            actualizar(c, anterior, valores);
            return null;
        });
    }

    public void actualizar(Connection c, Registro anterior, Map<String, Object> valores) throws SQLException {
        List<String> asignaciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        for (Campo campo : campos) {
            if (!campo.generado() && !esquema.claves().contains(campo.nombre()) && valores.containsKey(campo.nombre())) {
                asignaciones.add(campo.nombre() + " = ?");
                parametros.add(valores.get(campo.nombre()));
            }
        }
        if (asignaciones.isEmpty()) {
            throw new NegocioException("No hay campos modificables en este registro. Elimine y vuelva a crearlo.");
        }
        StringBuilder condicion = new StringBuilder();
        for (int i = 0; i < esquema.claves().size(); i++) {
            condicion.append(i > 0 ? " AND " : "").append(esquema.claves().get(i)).append(" = ?");
            parametros.add(anterior.claves()[i]);
        }
        if (esquema.versionada()) {
            condicion.append(" AND RowVer = ?");
            parametros.add(anterior.version());
        }
        String sql = "UPDATE " + esquema.tabla() + " SET " + String.join(", ", asignaciones) + " WHERE " + condicion;
        if (JdbcUtil.actualizar(c, sql, parametros.toArray()) == 0) {
            throw new NegocioException(esquema.versionada()
                    ? "El registro fue modificado por otro usuario o ya no existe. Actualice la lista e intente de nuevo."
                    : "El registro ya no existe. Actualice la lista.");
        }
    }

    public boolean eliminar(Registro registro) {
        return Transaccion.ejecutar(c -> eliminar(c, registro));
    }

    public boolean eliminar(Connection c, Registro registro) throws SQLException {
        boolean desactivar = campos.stream().anyMatch(campo -> campo.nombre().equals("Activo"));
        StringBuilder condicion = new StringBuilder();
        for (int i = 0; i < esquema.claves().size(); i++) {
            condicion.append(i > 0 ? " AND " : "").append(esquema.claves().get(i)).append(" = ?");
        }
        String sql = desactivar
                ? "UPDATE " + esquema.tabla() + " SET Activo = 0 WHERE " + condicion
                : "DELETE FROM " + esquema.tabla() + " WHERE " + condicion;
        if (JdbcUtil.actualizar(c, sql, registro.claves()) == 0) {
            throw new NegocioException("El registro ya no existe. Actualice la lista.");
        }
        return desactivar;
    }
}
