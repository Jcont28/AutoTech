package hn.autotech.model;

import java.math.BigDecimal;

public class ServicioReparacionAvanzada extends Servicio {
    public ServicioReparacionAvanzada(int id, String codigo, String nombre, String descripcion,
            BigDecimal costoManoObra, BigDecimal tiempoEstimadoHoras, boolean activo) {
        super(id, codigo, nombre, descripcion, 4, costoManoObra, tiempoEstimadoHoras, activo);
    }

    @Override
    public String categoria() {
        return "Reparacion avanzada";
    }

    @Override
    protected BigDecimal factorComplejidad() {
        return new BigDecimal("1.20");
    }
}
