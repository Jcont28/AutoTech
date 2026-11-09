package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmRepuestos extends FormularioTabla {
    public FrmRepuestos() {
        super("Repuestos", EsquemaTabla.identidad("Repuestos", "IdRepuesto").conVersion(), new String[][]{
            {"Codigo", "Texto", "0"},
            {"Nombre", "Texto", "0"},
            {"IdCategoria", "FK: Categoría", "0"},
            {"IdProveedor", "FK: Proveedor", "0"},
            {"StockActual", "Número", "0"},
            {"StockMinimo", "Número", "0"},
            {"CostoUnitario", "Decimal", "0"},
            {"PrecioVenta", "Decimal", "0"},
            {"Activo", "Sí/No", "0"}
        });
    }
}
