package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmServicioRepuesto extends FormularioTabla {
    public FrmServicioRepuesto() {
        super("ServicioRepuesto", EsquemaTabla.compuesta("ServicioRepuesto", "IdServicio", "IdRepuesto"), new String[][]{
            {"IdServicio", "FK: Servicio", "0"},
            {"IdRepuesto", "FK: Repuesto", "0"},
            {"Cantidad", "Número", "0"}
        });
    }
}
