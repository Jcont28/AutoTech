package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmMecanicoEspecialidad extends FormularioTabla {
    public FrmMecanicoEspecialidad() {
        super("MecanicoEspecialidad", EsquemaTabla.compuesta("MecanicoEspecialidad", "IdMecanico", "IdEspecialidad"), new String[][]{
            {"IdMecanico", "FK: Mecánico", "0"},
            {"IdEspecialidad", "FK: Especialidad", "0"}
        });
    }
}
