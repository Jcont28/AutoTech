package hn.autotech.model;

import hn.autotech.exception.NegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class Servicio {
    private final int id;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final int nivel;
    private final BigDecimal costoManoObra;
    private final BigDecimal tiempoEstimadoHoras;
    private final boolean activo;

    protected Servicio(int id, String codigo, String nombre, String descripcion, int nivel,
                       BigDecimal costoManoObra, BigDecimal tiempoEstimadoHoras, boolean activo) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.costoManoObra = costoManoObra;
        this.tiempoEstimadoHoras = tiempoEstimadoHoras;
        this.activo = activo;
    }

    public static Servicio crear(int id, String codigo, String nombre, String descripcion, int nivel,
                                 BigDecimal costoManoObra, BigDecimal tiempoEstimadoHoras, boolean activo) {
        return switch (nivel) {
            case 1 -> new ServicioMantenimientoBasico(id, codigo, nombre, descripcion, costoManoObra, tiempoEstimadoHoras, activo);
            case 2 -> new ServicioMantenimientoPreventivo(id, codigo, nombre, descripcion, costoManoObra, tiempoEstimadoHoras, activo);
            case 3 -> new ServicioReparacionIntermedia(id, codigo, nombre, descripcion, costoManoObra, tiempoEstimadoHoras, activo);
            case 4 -> new ServicioReparacionAvanzada(id, codigo, nombre, descripcion, costoManoObra, tiempoEstimadoHoras, activo);
            case 5 -> new ServicioReparacionMayor(id, codigo, nombre, descripcion, costoManoObra, tiempoEstimadoHoras, activo);
            default -> throw new NegocioException("El nivel de servicio debe estar entre 1 y 5.");
        };
    }

    public abstract String categoria();

    protected abstract BigDecimal factorComplejidad();

    public BigDecimal horasPlanificadas() {
        return tiempoEstimadoHoras.multiply(factorComplejidad()).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal precioAplicable() {
        return costoManoObra.setScale(2, RoundingMode.HALF_UP);
    }

    public int id() {
        return id;
    }

    public String codigo() {
        return codigo;
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    public int nivel() {
        return nivel;
    }

    public BigDecimal costoManoObra() {
        return costoManoObra;
    }

    public BigDecimal tiempoEstimadoHoras() {
        return tiempoEstimadoHoras;
    }

    public boolean activo() {
        return activo;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " [" + categoria() + "] L " + precioAplicable();
    }
}
