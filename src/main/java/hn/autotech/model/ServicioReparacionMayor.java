package hn.autotech.model;

import java.math.BigDecimal;

public class ServicioReparacionMayor extends Servicio {
    public ServicioReparacionMayor(int id, String codigo, String nombre, String descripcion,
            BigDecimal costoManoObra, BigDecimal tiempoEstimadoHoras, boolean activo) {
        super(id, codigo, nombre, descripcion, 5, costoManoObra, tiempoEstimadoHoras, activo);
    }

    @Override
    public String categoria() {
        return "Reparacion mayor";
    }

    @Override
    protected BigDecimal factorComplejidad() {
        return new BigDecimal("1.30");
    }
}
