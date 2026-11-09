package hn.autotech.dao;

import hn.autotech.model.Vehiculo;

import java.sql.ResultSet;
import java.sql.SQLException;

public class VehiculoDao extends RepositorioJdbc<Vehiculo> {
    @Override
    protected String tabla() {
        return "Vehiculos";
    }

    @Override
    protected String columnaId() {
        return "IdVehiculo";
    }

    @Override
    protected String[] columnas() {
        return new String[]{"IdCliente", "Placa", "VIN", "Marca", "Modelo", "Anio", "Color", "Kilometraje", "Combustible", "NumeroMotorActual", "Activo"};
    }

    @Override
    protected Object[] valores(Vehiculo v) {
        return new Object[]{v.idCliente(), v.placa(), v.vin(), v.marca(), v.modelo(), v.anio(), v.color(), v.kilometraje(), v.combustible(), v.numeroMotorActual(), v.activo()};
    }

    @Override
    protected int idDe(Vehiculo v) {
        return v.id();
    }

    @Override
    protected Vehiculo mapear(ResultSet rs) throws SQLException {
        return new Vehiculo(rs.getInt("IdVehiculo"), rs.getInt("IdCliente"), rs.getString("Placa"), rs.getString("VIN"), rs.getString("Marca"),
                rs.getString("Modelo"), rs.getInt("Anio"), rs.getString("Color"), rs.getInt("Kilometraje"), rs.getString("Combustible"),
                rs.getString("NumeroMotorActual"), rs.getBoolean("Activo"));
    }
}
