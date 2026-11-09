package hn.autotech.model;

public record Vehiculo(int id, int idCliente, String placa, String vin, String marca, String modelo, int anio,
                       String color, int kilometraje, String combustible, String numeroMotorActual, boolean activo) {
}
