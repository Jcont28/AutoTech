package hn.autotech.model;

public enum Reporte {
    ORDENES_Y_SALDOS("Órdenes de trabajo, facturas y saldos",
            "SELECT NumeroOrden, FechaIngreso, Estado, Cliente, Placa, Vehiculo, Total, NumeroFactura, TotalFactura, TotalPagado, Saldo "
                    + "FROM vw_OrdenesResumen ORDER BY FechaIngreso DESC"),
    HISTORIAL_VEHICULOS("Historial de vehículos (servicios, mecánicos, repuestos y costos)",
            "SELECT Placa, Vehiculo, NumeroOrden, FechaIngreso, FechaEntrega, Estado, Servicios, Mecanicos, Repuestos, Costo "
                    + "FROM vw_HistorialVehiculo ORDER BY Placa, FechaIngreso DESC"),
    INVENTARIO_BAJO("Repuestos en o bajo el stock mínimo",
            "SELECT Codigo, Nombre, Categoria, Proveedor, StockActual, StockMinimo, Faltante FROM vw_RepuestosBajoMinimo ORDER BY Faltante DESC"),
    REPUESTOS_CONSUMIDOS("Repuestos mas consumidos",
            "SELECT r.Codigo, r.Nombre, SUM(c.Cantidad) AS CantidadConsumida, SUM(c.Cantidad * c.PrecioAplicado) AS ImporteVendido, "
                    + "COUNT(DISTINCT c.IdOrden) AS Ordenes FROM ConsumoRepuestoOrden c INNER JOIN Repuestos r ON r.IdRepuesto = c.IdRepuesto "
                    + "WHERE c.Registrado = 1 GROUP BY r.Codigo, r.Nombre ORDER BY CantidadConsumida DESC"),
    INGRESOS_MENSUALES("Ingresos facturados y cobrados por mes (últimos 12 meses)", "EXEC sp_ReporteIngresos"),
    CARGA_MECANICOS("Carga de trabajo por mecánico",
            "SELECT Mecanico, Activo, Disponible, OrdenesActivas FROM vw_CargaMecanicos ORDER BY OrdenesActivas DESC, Mecanico"),
    CLIENTES_Y_VEHICULOS("Clientes, vehículos y órdenes",
            "SELECT c.Nombre + N' ' + c.Apellido AS Cliente, c.Telefono, COUNT(v.IdVehiculo) AS Vehiculos, "
                    + "(SELECT COUNT(*) FROM OrdenesTrabajo o WHERE o.IdCliente = c.IdCliente) AS Ordenes "
                    + "FROM Clientes c LEFT JOIN Vehiculos v ON v.IdCliente = c.IdCliente AND v.Activo = 1 "
                    + "WHERE c.Activo = 1 GROUP BY c.IdCliente, c.Nombre, c.Apellido, c.Telefono ORDER BY Vehiculos DESC, Cliente"),
    SERVICIOS_SOLICITADOS("Servicios mas solicitados",
            "SELECT s.Codigo, s.Nombre, s.Nivel, COUNT(d.IdDetalle) AS Veces, COALESCE(SUM(d.Subtotal), 0) AS ManoObraFacturable "
                    + "FROM Servicios s LEFT JOIN DetalleOrden d ON d.IdServicio = s.IdServicio "
                    + "GROUP BY s.Codigo, s.Nombre, s.Nivel ORDER BY Veces DESC, s.Codigo"),
    DETALLE_FACTURAS("Detalle de facturas emitidas",
            "SELECT f.NumeroFactura, o.NumeroOrden, N'Servicio' AS Tipo, s.Nombre AS Concepto, CAST(1 AS DECIMAL(10,2)) AS Cantidad, "
                    + "d.PrecioAplicado AS PrecioUnitario, d.Subtotal FROM Facturas f INNER JOIN OrdenesTrabajo o ON o.IdOrden = f.IdOrden "
                    + "INNER JOIN DetalleOrden d ON d.IdOrden = o.IdOrden INNER JOIN Servicios s ON s.IdServicio = d.IdServicio "
                    + "UNION ALL "
                    + "SELECT f.NumeroFactura, o.NumeroOrden, N'Repuesto', r.Nombre, CAST(c.Cantidad AS DECIMAL(10,2)), "
                    + "c.PrecioAplicado, c.Cantidad * c.PrecioAplicado FROM Facturas f INNER JOIN OrdenesTrabajo o ON o.IdOrden = f.IdOrden "
                    + "INNER JOIN ConsumoRepuestoOrden c ON c.IdOrden = o.IdOrden AND c.Registrado = 1 INNER JOIN Repuestos r ON r.IdRepuesto = c.IdRepuesto "
                    + "ORDER BY 1, 3 DESC");

    private final String titulo;
    private final String sql;

    Reporte(String titulo, String sql) {
        this.titulo = titulo;
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }

    @Override
    public String toString() {
        return titulo;
    }
}
