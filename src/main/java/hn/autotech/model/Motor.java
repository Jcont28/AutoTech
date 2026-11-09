package hn.autotech.model;

import java.math.BigDecimal;

public record Motor(int id, String numeroSerie, String marca, String modelo, String cilindraje, String combustible,
                    String estado, BigDecimal costo, Integer idRepuesto, Integer idProveedor, Integer idVehiculoActual) {
}
