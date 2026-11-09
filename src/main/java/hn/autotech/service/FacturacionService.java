package hn.autotech.service;

import hn.autotech.dao.JdbcUtil;
import hn.autotech.dao.Transaccion;
import hn.autotech.exception.NegocioException;
import hn.autotech.model.Factura;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

public class FacturacionService {
    private static final List<String> METODOS = List.of("Efectivo", "Tarjeta", "Transferencia", "Cheque");

    private Factura mapear(ResultSet rs) throws SQLException {
        Timestamp fecha = rs.getTimestamp("Fecha");
        return new Factura(rs.getInt("IdFactura"), rs.getString("NumeroFactura"), rs.getInt("IdOrden"), fecha == null ? null : fecha.toLocalDateTime(),
                rs.getBigDecimal("Subtotal"), rs.getBigDecimal("Descuento"), rs.getBigDecimal("PorcentajeImpuesto"), rs.getBigDecimal("Impuesto"),
                rs.getBigDecimal("Total"), rs.getString("Estado"));
    }

    public Factura generarFactura(Connection c, int idOrden) throws SQLException {
        int idFactura;
        try (CallableStatement cs = c.prepareCall("{call sp_GenerarFactura(?, ?)}")) {
            cs.setInt(1, idOrden);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.execute();
            idFactura = cs.getInt(2);
        }
        return JdbcUtil.unico(c, "SELECT IdFactura, NumeroFactura, IdOrden, Fecha, Subtotal, Descuento, PorcentajeImpuesto, Impuesto, Total, Estado FROM Facturas WHERE IdFactura = ?",
                this::mapear, idFactura).orElseThrow(() -> new NegocioException("No se pudo leer la factura generada."));
    }

    public Optional<Factura> facturaDeOrden(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.unico(c,
                "SELECT IdFactura, NumeroFactura, IdOrden, Fecha, Subtotal, Descuento, PorcentajeImpuesto, Impuesto, Total, Estado FROM Facturas WHERE IdOrden = ?",
                this::mapear, idOrden));
    }

    public BigDecimal totalPagado(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.unico(c, "SELECT COALESCE(SUM(Monto), 0) FROM Pagos WHERE IdOrden = ?", rs -> rs.getBigDecimal(1), idOrden)
                .orElse(BigDecimal.ZERO));
    }

    public BigDecimal saldo(int idOrden) {
        Factura factura = facturaDeOrden(idOrden).orElseThrow(() -> new NegocioException("La orden aún no tiene factura."));
        return factura.total().subtract(totalPagado(idOrden));
    }

    public int registrarPago(int idOrden, BigDecimal monto, String metodo, String referencia) {
        if (monto == null || monto.signum() <= 0) {
            throw new NegocioException("El monto del pago debe ser mayor que cero.");
        }
        if (!METODOS.contains(metodo)) {
            throw new NegocioException("El método de pago debe ser uno de: " + String.join(", ", METODOS) + ".");
        }
        return Transaccion.ejecutar(c -> {
            try (CallableStatement cs = c.prepareCall("{call sp_RegistrarPago(?, ?, ?, ?, ?)}")) {
                cs.setInt(1, idOrden);
                cs.setBigDecimal(2, monto);
                cs.setString(3, metodo);
                if (referencia == null || referencia.isBlank()) {
                    cs.setNull(4, Types.NVARCHAR);
                } else {
                    cs.setString(4, referencia.trim());
                }
                cs.registerOutParameter(5, Types.INTEGER);
                cs.execute();
                return cs.getInt(5);
            }
        });
    }
}
