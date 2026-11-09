package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmEspecialidades extends FormularioTabla {
    public FrmEspecialidades() {
        super("Especialidades", EsquemaTabla.identidad("Especialidades", "IdEspecialidad"), new String[][]{
            {"Nombre", "Texto", "0"},
            {"Descripcion", "Texto", "0"}
        });
    }
}
