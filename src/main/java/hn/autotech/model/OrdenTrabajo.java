package hn.autotech.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdenTrabajo(int id, String numeroOrden, int idCliente, int idVehiculo, LocalDateTime fechaIngreso,
                           LocalDateTime fechaEntrega, int kilometrajeIngreso, String sintomas, String diagnosticoPreliminar,
                           Integer idMecanicoResponsable, EstadoOrden estado, BigDecimal total, boolean esCambioMotor) {
}
