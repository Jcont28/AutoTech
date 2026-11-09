package hn.autotech.dao;

import hn.autotech.model.EstadoOrden;
import hn.autotech.model.OrdenTrabajo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.sql.Connection;

public class OrdenDao extends RepositorioJdbc<OrdenTrabajo> {
    @Override
    protected String tabla() {
        return "OrdenesTrabajo";
    }

    @Override
    protected String columnaId() {
        return "IdOrden";
    }

    @Override
    protected String[] columnas() {
        return new String[]{"NumeroOrden", "IdCliente", "IdVehiculo", "KilometrajeIngreso", "Sintomas", "DiagnosticoPreliminar",
                "IdMecanicoResponsable", "Estado", "Total", "EsCambioMotor"};
    }

    @Override
    protected String[] columnasLectura() {
        return new String[]{"IdOrden", "NumeroOrden", "IdCliente", "IdVehiculo", "FechaIngreso", "FechaEntrega", "KilometrajeIngreso", "Sintomas",
                "DiagnosticoPreliminar", "IdMecanicoResponsable", "Estado", "Total", "EsCambioMotor"};
    }

    @Override
    protected Object[] valores(OrdenTrabajo o) {
        return new Object[]{o.numeroOrden(), o.idCliente(), o.idVehiculo(), o.kilometrajeIngreso(), o.sintomas(), o.diagnosticoPreliminar(),
                o.idMecanicoResponsable(), o.estado().texto(), o.total(), o.esCambioMotor()};
    }

    @Override
    protected int idDe(OrdenTrabajo o) {
        return o.id();
    }

    @Override
    protected OrdenTrabajo mapear(ResultSet rs) throws SQLException {
        Timestamp ingreso = rs.getTimestamp("FechaIngreso");
        Timestamp entrega = rs.getTimestamp("FechaEntrega");
        LocalDateTime fechaIngreso = ingreso == null ? null : ingreso.toLocalDateTime();
        LocalDateTime fechaEntrega = entrega == null ? null : entrega.toLocalDateTime();
        return new OrdenTrabajo(rs.getInt("IdOrden"), rs.getString("NumeroOrden"), rs.getInt("IdCliente"), rs.getInt("IdVehiculo"), fechaIngreso,
                fechaEntrega, rs.getInt("KilometrajeIngreso"), rs.getString("Sintomas"), rs.getString("DiagnosticoPreliminar"),
                JdbcUtil.entero(rs, "IdMecanicoResponsable"), EstadoOrden.desde(rs.getString("Estado")), rs.getBigDecimal("Total"),
                rs.getBoolean("EsCambioMotor"));
    }

    public Optional<OrdenTrabajo> bloquear(Connection c, int idOrden) throws SQLException {
        return JdbcUtil.unico(c, seleccion() + " WITH (UPDLOCK, ROWLOCK) WHERE IdOrden = ?", this::mapear, idOrden);
    }
}
