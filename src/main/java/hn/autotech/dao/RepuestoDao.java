package hn.autotech.dao;

import hn.autotech.model.Repuesto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class RepuestoDao extends RepositorioJdbc<Repuesto> {
    @Override
    protected String tabla() {
        return "Repuestos";
    }

    @Override
    protected String columnaId() {
        return "IdRepuesto";
    }

    @Override
    protected String[] columnas() {
        return new String[]{"Codigo", "Nombre", "IdCategoria", "IdProveedor", "StockActual", "StockMinimo", "CostoUnitario", "PrecioVenta", "Activo"};
    }

    @Override
    protected Object[] valores(Repuesto r) {
        return new Object[]{r.codigo(), r.nombre(), r.idCategoria(), r.idProveedor(), r.stockActual(), r.stockMinimo(), r.costoUnitario(), r.precioVenta(), r.activo()};
    }

    @Override
    protected int idDe(Repuesto r) {
        return r.id();
    }

    @Override
    protected Repuesto mapear(ResultSet rs) throws SQLException {
        return new Repuesto(rs.getInt("IdRepuesto"), rs.getString("Codigo"), rs.getString("Nombre"), rs.getInt("IdCategoria"),
                JdbcUtil.entero(rs, "IdProveedor"), rs.getInt("StockActual"), rs.getInt("StockMinimo"),
                rs.getBigDecimal("CostoUnitario"), rs.getBigDecimal("PrecioVenta"), rs.getBoolean("Activo"));
    }

    public List<Repuesto> listarActivos() {
        return Transaccion.consulta(c -> JdbcUtil.lista(c,
                "SELECT IdRepuesto, Codigo, Nombre, IdCategoria, IdProveedor, StockActual, StockMinimo, CostoUnitario, PrecioVenta, Activo "
                        + "FROM Repuestos WHERE Activo = 1 ORDER BY Nombre",
                this::mapear));
    }
}
