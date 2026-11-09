package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmConfiguracionImpuestos extends FormularioTabla {
    public FrmConfiguracionImpuestos() {
        super("ConfiguracionImpuestos", EsquemaTabla.identidad("ConfiguracionImpuestos", "IdImpuesto"), new String[][]{
            {"Nombre", "Texto", "0"},
            {"Porcentaje", "Decimal", "0"},
            {"Vigente", "Sí/No", "0"}
        });
    }
}
