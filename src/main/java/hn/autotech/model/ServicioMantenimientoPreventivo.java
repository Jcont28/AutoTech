package hn.autotech.model;

import java.math.BigDecimal;

public class ServicioMantenimientoPreventivo extends Servicio {
    public ServicioMantenimientoPreventivo(int id, String codigo, String nombre, String descripcion,
            BigDecimal costoManoObra, BigDecimal tiempoEstimadoHoras, boolean activo) {
        super(id, codigo, nombre, descripcion, 2, costoManoObra, tiempoEstimadoHoras, activo);
    }

    @Override
    public String categoria() {
        return "Mantenimiento preventivo";
    }

    @Override
    protected BigDecimal factorComplejidad() {
        return new BigDecimal("1.05");
    }
}
