package hn.autotech.model;

import java.time.LocalDateTime;

public record CambioMotorSolicitud(int idOrden, Integer idVehiculo, int idMotorNuevo, String serieAnterior, String serieNueva,
                                   String destinoRetirado, LocalDateTime fechaDesmontaje, LocalDateTime fechaInstalacion,
                                   String resultadoPruebas, boolean pruebasAprobadas, String observaciones) {
}
