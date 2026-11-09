package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmCompatibilidadMotor extends FormularioTabla {
    public FrmCompatibilidadMotor() {
        super("CompatibilidadMotor", EsquemaTabla.identidad("CompatibilidadMotor", "IdCompatibilidad"), new String[][]{
            {"IdMotor", "FK: Motor", "0"},
            {"MarcaVehiculo", "Texto", "0"},
            {"ModeloVehiculo", "Texto", "0"},
            {"AnioDesde", "Número", "0"},
            {"AnioHasta", "Número", "0"}
        });
    }
}
