package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmServicios extends FormularioTabla {
    public FrmServicios() {
        super("Servicios", EsquemaTabla.identidad("Servicios", "IdServicio"), new String[][]{
            {"Codigo", "Texto", "0"},
            {"Nombre", "Texto", "0"},
            {"Descripcion", "Texto", "0"},
            {"Nivel", "Número", "0"},
            {"CostoManoObra", "Decimal", "0"},
            {"TiempoEstimadoHoras", "Decimal", "0"},
            {"Activo", "Sí/No", "0"}
        });
    }
}
