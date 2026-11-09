package hn.autotech.dao;

import hn.autotech.model.Motor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MotorDao extends RepositorioJdbc<Motor> {
    @Override
    protected String tabla() {
        return "Motores";
    }

    @Override
    protected String columnaId() {
        return "IdMotor";
    }

    @Override
    protected String[] columnas() {
        return new String[]{"NumeroSerie", "Marca", "Modelo", "Cilindraje", "Combustible", "Estado", "Costo", "IdRepuesto", "IdProveedor", "IdVehiculoActual"};
    }

    @Override
    protected Object[] valores(Motor m) {
        return new Object[]{m.numeroSerie(), m.marca(), m.modelo(), m.cilindraje(), m.combustible(), m.estado(), m.costo(), m.idRepuesto(), m.idProveedor(), m.idVehiculoActual()};
    }

    @Override
    protected int idDe(Motor m) {
        return m.id();
    }

    @Override
    protected Motor mapear(ResultSet rs) throws SQLException {
        return new Motor(rs.getInt("IdMotor"), rs.getString("NumeroSerie"), rs.getString("Marca"), rs.getString("Modelo"), rs.getString("Cilindraje"),
                rs.getString("Combustible"), rs.getString("Estado"), rs.getBigDecimal("Costo"), JdbcUtil.entero(rs, "IdRepuesto"),
                JdbcUtil.entero(rs, "IdProveedor"), JdbcUtil.entero(rs, "IdVehiculoActual"));
    }

    public boolean esCompatible(Connection c, int idMotor, String marca, String modelo, int anio) throws SQLException {
        return JdbcUtil.existe(c,
                "SELECT 1 FROM CompatibilidadMotor WHERE IdMotor = ? AND MarcaVehiculo = ? AND ModeloVehiculo = ? AND ? BETWEEN AnioDesde AND AnioHasta",
                idMotor, marca, modelo, anio);
    }
}
