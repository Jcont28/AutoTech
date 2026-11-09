package hn.autotech.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Factura(int id, String numeroFactura, int idOrden, LocalDateTime fecha, BigDecimal subtotal,
                      BigDecimal descuento, BigDecimal porcentajeImpuesto, BigDecimal impuesto, BigDecimal total, String estado) {
}
