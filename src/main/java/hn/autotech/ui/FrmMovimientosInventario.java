package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;
import hn.autotech.dao.Registro;
import hn.autotech.exception.NegocioException;
import hn.autotech.service.InventarioService;
import java.util.Map;

public class FrmMovimientosInventario extends FormularioTabla {
    public FrmMovimientosInventario() {
        super("MovimientosInventario", EsquemaTabla.identidad("MovimientosInventario", "IdMovimiento"), new String[][]{
            {"IdRepuesto", "FK: Repuesto", "0"},
            {"Tipo", "Lista: Entrada|Salida", "0"},
            {"Cantidad", "Número", "0"},
            {"CostoUnitario", "Decimal", "0"},
            {"Fecha", "Fecha/hora (automática)", "1"},
            {"IdOrden", "FK: Orden", "0"},
            {"IdProveedor", "FK: Proveedor", "0"},
            {"Motivo", "Texto", "0"},
            {"Usuario", "Texto", "1"}
        });
    }

    @Override
    protected void insertar(Map<String, Object> valores) {
        new InventarioService().registrarMovimiento(entero(valores, "IdRepuesto"), texto(valores, "Tipo"), entero(valores, "Cantidad"),
                decimalOpcional(valores, "CostoUnitario"), enteroOpcional(valores, "IdOrden"), enteroOpcional(valores, "IdProveedor"),
                textoOpcional(valores, "Motivo"));
    }

    @Override
    protected void actualizar(Registro anterior, Map<String, Object> valores) {
        throw new NegocioException("Los movimientos de inventario no se modifican; registre un movimiento de ajuste.");
    }

    @Override
    protected boolean eliminar(Registro registro) {
        throw new NegocioException("Los movimientos de inventario no se eliminan.");
    }
}
