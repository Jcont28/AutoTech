package hn.autotech.model;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoOrden {
    RECIBIDA("Recibida"),
    EN_DIAGNOSTICO("En diagnostico"),
    PENDIENTE_APROBACION("Pendiente de aprobacion"),
    EN_REPARACION("En reparacion"),
    EN_CONTROL_CALIDAD("En control de calidad"),
    FINALIZADA("Finalizada"),
    RECHAZADA("Rechazada"),
    CANCELADA("Cancelada");

    private final String texto;

    EstadoOrden(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }

    public boolean esFinal() {
        return this == FINALIZADA || this == RECHAZADA || this == CANCELADA;
    }

    public Set<EstadoOrden> siguientes() {
        return switch (this) {
            case RECIBIDA -> EnumSet.of(EN_DIAGNOSTICO, CANCELADA);
            case EN_DIAGNOSTICO -> EnumSet.of(PENDIENTE_APROBACION, CANCELADA);
            case PENDIENTE_APROBACION -> EnumSet.of(EN_REPARACION, RECHAZADA, CANCELADA);
            case EN_REPARACION -> EnumSet.of(EN_CONTROL_CALIDAD, CANCELADA);
            case EN_CONTROL_CALIDAD -> EnumSet.of(FINALIZADA, EN_REPARACION);
            default -> EnumSet.noneOf(EstadoOrden.class);
        };
    }

    public static EstadoOrden desde(String texto) {
        for (EstadoOrden estado : values()) {
            if (estado.texto.equalsIgnoreCase(texto)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de orden desconocido: " + texto);
    }

    @Override
    public String toString() {
        return texto;
    }
}
