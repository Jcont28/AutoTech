package hn.autotech.model;

import java.math.BigDecimal;

public class ServicioMantenimientoBasico extends Servicio {
    public ServicioMantenimientoBasico(int id, String codigo, String nombre, String descripcion,
            BigDecimal costoManoObra, BigDecimal tiempoEstimadoHoras, boolean activo) {
        super(id, codigo, nombre, descripcion, 1, costoManoObra, tiempoEstimadoHoras, activo);
    }

    @Override
    public String categoria() {
        return "Mantenimiento basico";
    }

    @Override
    protected BigDecimal factorComplejidad() {
        return new BigDecimal("1.00");
    }
}
