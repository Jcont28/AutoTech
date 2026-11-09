package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmCategoriasRepuesto extends FormularioTabla {
    public FrmCategoriasRepuesto() {
        super("CategoriasRepuesto", EsquemaTabla.identidad("CategoriasRepuesto", "IdCategoria"), new String[][]{
            {"Nombre", "Texto", "0"}
        });
    }
}
