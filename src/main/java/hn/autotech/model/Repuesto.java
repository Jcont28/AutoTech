package hn.autotech.model;

import java.math.BigDecimal;

public record Repuesto(int id, String codigo, String nombre, int idCategoria, Integer idProveedor,
                       int stockActual, int stockMinimo, BigDecimal costoUnitario, BigDecimal precioVenta, boolean activo) {
    @Override
    public String toString() {
        return codigo + " - " + nombre + " (stock " + stockActual + ")";
    }
}
