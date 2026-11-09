package hn.autotech.service;

import hn.autotech.config.DBConnection;
import hn.autotech.dao.JdbcUtil;
import hn.autotech.dao.Transaccion;
import hn.autotech.exception.NegocioException;

import java.math.BigDecimal;

public class InventarioService {
    public int registrarMovimiento(int idRepuesto, String tipo, int cantidad, BigDecimal costoUnitario,
                                   Integer idOrden, Integer idProveedor, String motivo) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad debe ser mayor que cero.");
        }
        if (!tipo.equals("Entrada") && !tipo.equals("Salida")) {
            throw new NegocioException("El tipo de movimiento debe ser Entrada o Salida.");
        }
        BigDecimal costo = costoUnitario == null ? BigDecimal.ZERO : costoUnitario;
        if (costo.signum() < 0) {
            throw new NegocioException("El costo unitario no puede ser negativo.");
        }
        return Transaccion.ejecutar(c -> {
            int filas;
            if (tipo.equals("Entrada")) {
                filas = JdbcUtil.actualizar(c, "UPDATE Repuestos SET StockActual = StockActual + ? WHERE IdRepuesto = ?", cantidad, idRepuesto);
            } else {
                filas = JdbcUtil.actualizar(c, "UPDATE Repuestos SET StockActual = StockActual - ? WHERE IdRepuesto = ? AND StockActual >= ?", cantidad, idRepuesto, cantidad);
                if (filas == 0) {
                    throw new NegocioException("Stock insuficiente para registrar la salida.");
                }
            }
            if (filas == 0) {
                throw new NegocioException("El repuesto indicado no existe.");
            }
            return JdbcUtil.insertar(c,
                    "INSERT INTO MovimientosInventario (IdRepuesto, Tipo, Cantidad, CostoUnitario, IdOrden, IdProveedor, Motivo, Usuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    idRepuesto, tipo, cantidad, costo, idOrden, idProveedor, motivo, DBConnection.usuario());
        });
    }
}
