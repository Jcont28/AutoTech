package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmMecanicos extends FormularioTabla {
    public FrmMecanicos() {
        super("Mecanicos", EsquemaTabla.identidad("Mecanicos", "IdMecanico"), new String[][]{
            {"Nombre", "Texto", "0"},
            {"Apellido", "Texto", "0"},
            {"Identidad", "Texto", "0"},
            {"Telefono", "Texto", "0"},
            {"Correo", "Texto", "0"},
            {"FechaIngreso", "Fecha (automática)", "1"},
            {"Activo", "Sí/No", "0"},
            {"Disponible", "Sí/No", "0"}
        });
    }
}
