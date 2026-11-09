package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmProveedores extends FormularioTabla {
    public FrmProveedores() {
        super("Proveedores", EsquemaTabla.identidad("Proveedores", "IdProveedor"), new String[][]{
            {"Nombre", "Texto", "0"},
            {"Contacto", "Texto", "0"},
            {"Telefono", "Texto", "0"},
            {"Correo", "Texto", "0"},
            {"Direccion", "Texto", "0"},
            {"Activo", "Sí/No", "0"}
        });
    }
}
