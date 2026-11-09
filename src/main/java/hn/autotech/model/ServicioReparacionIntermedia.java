package hn.autotech.model;

import java.math.BigDecimal;

public class ServicioReparacionIntermedia extends Servicio {
    public ServicioReparacionIntermedia(int id, String codigo, String nombre, String descripcion,
            BigDecimal costoManoObra, BigDecimal tiempoEstimadoHoras, boolean activo) {
        super(id, codigo, nombre, descripcion, 3, costoManoObra, tiempoEstimadoHoras, activo);
    }

    @Override
    public String categoria() {
        return "Reparacion intermedia";
    }

    @Override
    protected BigDecimal factorComplejidad() {
        return new BigDecimal("1.10");
    }
}
