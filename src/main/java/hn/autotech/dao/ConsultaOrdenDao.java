package hn.autotech.dao;

import hn.autotech.model.ResultadoConsulta;

public final class ConsultaOrdenDao {
    private ConsultaOrdenDao() {
    }

    public static ResultadoConsulta actividades(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c,
                "SELECT a.IdActividad AS Id, m.Nombre + N' ' + m.Apellido AS Mecanico, a.Descripcion, a.Estado, a.Horas, a.FechaInicio, a.FechaFin "
                        + "FROM ActividadesTrabajo a INNER JOIN Mecanicos m ON m.IdMecanico = a.IdMecanico WHERE a.IdOrden = ? ORDER BY a.IdActividad", idOrden));
    }

    public static ResultadoConsulta controles(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c,
                "SELECT q.IdControl AS Id, q.Descripcion, q.Obligatorio, q.Aprobado, m.Nombre + N' ' + m.Apellido AS Inspector, q.Fecha, q.Observaciones "
                        + "FROM ControlesCalidad q LEFT JOIN Mecanicos m ON m.IdMecanico = q.IdInspector WHERE q.IdOrden = ? ORDER BY q.IdControl", idOrden));
    }

    public static ResultadoConsulta consumos(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c,
                "SELECT k.IdConsumo AS Id, r.Codigo, r.Nombre, k.Cantidad, k.PrecioAplicado, k.Cantidad * k.PrecioAplicado AS Importe, k.Registrado "
                        + "FROM ConsumoRepuestoOrden k INNER JOIN Repuestos r ON r.IdRepuesto = k.IdRepuesto WHERE k.IdOrden = ? ORDER BY k.IdConsumo", idOrden));
    }

    public static ResultadoConsulta historial(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c,
                "SELECT FechaCambio, EstadoAnterior, EstadoNuevo, Usuario, Observacion FROM HistorialEstadosOrden WHERE IdOrden = ? ORDER BY IdHistorial", idOrden));
    }

    public static ResultadoConsulta pagos(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c,
                "SELECT IdPago AS Id, Fecha, Monto, Metodo, Referencia FROM Pagos WHERE IdOrden = ? ORDER BY IdPago", idOrden));
    }

    public static ResultadoConsulta presupuesto(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c,
                "SELECT IdPresupuesto AS Id, Fecha, Subtotal, Descuento, AutorizadoDescuentoPor, PorcentajeImpuesto, Impuesto, Total, Estado, FechaRespuesta "
                        + "FROM Presupuestos WHERE IdOrden = ?", idOrden));
    }

    public static ResultadoConsulta resumen(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c,
                "SELECT NumeroOrden, Estado, Cliente, Placa, Vehiculo, Total, NumeroFactura, TotalFactura, TotalPagado, Saldo FROM vw_OrdenesResumen WHERE IdOrden = ?", idOrden));
    }

    public static ResultadoConsulta mecanicos(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c,
                "SELECT m.Nombre + N' ' + m.Apellido AS Mecanico, om.EsResponsable AS Responsable, om.FechaAsignacion AS Asignado "
                        + "FROM OrdenMecanico om INNER JOIN Mecanicos m ON m.IdMecanico = om.IdMecanico WHERE om.IdOrden = ? ORDER BY om.EsResponsable DESC, om.FechaAsignacion", idOrden));
    }
}
