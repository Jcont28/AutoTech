package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmClientes extends FormularioTabla {
    public FrmClientes() {
        super("Clientes", EsquemaTabla.identidad("Clientes", "IdCliente"), new String[][]{
            {"Nombre", "Texto", "0"},
            {"Apellido", "Texto", "0"},
            {"Identidad", "Texto", "0"},
            {"Telefono", "Texto", "0"},
            {"Correo", "Texto", "0"},
            {"Direccion", "Texto", "0"},
            {"FechaRegistro", "Fecha/hora (automática)", "1"},
            {"Activo", "Sí/No", "0"}
        });
    }
}
