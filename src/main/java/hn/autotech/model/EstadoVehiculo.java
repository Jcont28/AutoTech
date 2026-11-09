package hn.autotech.model;

import java.sql.Timestamp;
import java.util.List;

public record EstadoVehiculo(int idOrden, String numeroOrden, String vehiculo, String placa, String estado, Timestamp ingreso, String sintomas,
                             List<Comentario> comentarios) {
    public record Comentario(Timestamp fecha, String autor, String texto) {
    }
}
