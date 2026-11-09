package hn.autotech.dao;

import hn.autotech.model.Servicio;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServicioDao extends RepositorioJdbc<Servicio> {
    @Override
    protected String tabla() {
        return "Servicios";
    }

    @Override
    protected String columnaId() {
        return "IdServicio";
    }

    @Override
    protected String[] columnas() {
        return new String[]{"Codigo", "Nombre", "Descripcion", "Nivel", "CostoManoObra", "TiempoEstimadoHoras", "Activo"};
    }

    @Override
    protected Object[] valores(Servicio s) {
        return new Object[]{s.codigo(), s.nombre(), s.descripcion(), s.nivel(), s.costoManoObra(), s.tiempoEstimadoHoras(), s.activo()};
    }

    @Override
    protected int idDe(Servicio s) {
        return s.id();
    }

    @Override
    protected Servicio mapear(ResultSet rs) throws SQLException {
        return Servicio.crear(rs.getInt("IdServicio"), rs.getString("Codigo"), rs.getString("Nombre"), rs.getString("Descripcion"),
                rs.getInt("Nivel"), rs.getBigDecimal("CostoManoObra"), rs.getBigDecimal("TiempoEstimadoHoras"), rs.getBoolean("Activo"));
    }

    public List<Servicio> listarActivos() {
        return Transaccion.consulta(c -> JdbcUtil.lista(c,
                "SELECT IdServicio, Codigo, Nombre, Descripcion, Nivel, CostoManoObra, TiempoEstimadoHoras, Activo FROM Servicios WHERE Activo = 1 ORDER BY Nivel, Codigo",
                this::mapear));
    }

    public Map<Integer, Integer> repuestosAsociados(Connection c, int idServicio) throws SQLException {
        Map<Integer, Integer> resultado = new LinkedHashMap<>();
        for (int[] par : JdbcUtil.lista(c, "SELECT IdRepuesto, Cantidad FROM ServicioRepuesto WHERE IdServicio = ?",
                rs -> new int[]{rs.getInt(1), rs.getInt(2)}, idServicio)) {
            resultado.put(par[0], par[1]);
        }
        return resultado;
    }
}
