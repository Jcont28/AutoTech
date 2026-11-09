package hn.autotech.model;

import java.util.List;

public record NuevaOrden(int idCliente, int idVehiculo, int kilometraje, String sintomas, int idResponsable,
                         List<Integer> idsServicios, boolean cambioMotor) {
}
