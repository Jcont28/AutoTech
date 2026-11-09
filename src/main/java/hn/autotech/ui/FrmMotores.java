package hn.autotech.ui;

import hn.autotech.dao.EsquemaTabla;

public class FrmMotores extends FormularioTabla {
    public FrmMotores() {
        super("Motores", EsquemaTabla.identidad("Motores", "IdMotor"), new String[][]{
            {"NumeroSerie", "Texto", "0"},
            {"Marca", "Texto", "0"},
            {"Modelo", "Texto", "0"},
            {"Cilindraje", "Texto", "0"},
            {"Combustible", "Texto", "0"},
            {"Estado", "Lista: Disponible|Instalado|Reservado|Retirado|Descartado", "0"},
            {"Costo", "Decimal", "0"},
            {"IdRepuesto", "FK: Repuesto", "0"},
            {"IdProveedor", "FK: Proveedor", "0"},
            {"IdVehiculoActual", "FK: Vehículo", "0"},
            {"FechaIngreso", "Fecha/hora (automática)", "1"}
        });
    }
}
