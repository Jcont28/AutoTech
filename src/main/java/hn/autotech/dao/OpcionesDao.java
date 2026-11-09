package hn.autotech.dao;

import hn.autotech.model.Opcion;

import java.util.List;
import java.util.Map;

public final class OpcionesDao {
    private static final Map<String, String> RELACIONES = Map.ofEntries(
            Map.entry("Orden", "SELECT IdOrden, NumeroOrden FROM OrdenesTrabajo ORDER BY IdOrden DESC"),
            Map.entry("Cliente", "SELECT IdCliente, Nombre + N' ' + Apellido + N' (' + Identidad + N')' FROM Clientes ORDER BY Nombre, Apellido"),
            Map.entry("Vehículo", "SELECT IdVehiculo, Placa + N' - ' + Marca + N' ' + Modelo FROM Vehiculos ORDER BY Placa"),
            Map.entry("Mecánico", "SELECT IdMecanico, Nombre + N' ' + Apellido FROM Mecanicos ORDER BY Nombre, Apellido"),
            Map.entry("Motor", "SELECT IdMotor, NumeroSerie + N' (' + Estado + N')' FROM Motores ORDER BY NumeroSerie"),
            Map.entry("Repuesto", "SELECT IdRepuesto, Codigo + N' - ' + Nombre FROM Repuestos ORDER BY Nombre"),
            Map.entry("Proveedor", "SELECT IdProveedor, Nombre FROM Proveedores ORDER BY Nombre"),
            Map.entry("Servicio", "SELECT IdServicio, Codigo + N' - ' + Nombre FROM Servicios ORDER BY Codigo"),
            Map.entry("Presupuesto", "SELECT IdPresupuesto, N'Presupuesto ' + CAST(IdPresupuesto AS NVARCHAR(20)) + N' (orden ' + CAST(IdOrden AS NVARCHAR(20)) + N')' FROM Presupuestos ORDER BY IdPresupuesto DESC"),
            Map.entry("Diagnóstico", "SELECT IdDiagnostico, N'Diagnostico ' + CAST(IdDiagnostico AS NVARCHAR(20)) + N' (orden ' + CAST(IdOrden AS NVARCHAR(20)) + N')' FROM Diagnosticos ORDER BY IdDiagnostico DESC"),
            Map.entry("Impuesto", "SELECT IdImpuesto, Nombre FROM ConfiguracionImpuestos ORDER BY Nombre"),
            Map.entry("Categoría", "SELECT IdCategoria, Nombre FROM CategoriasRepuesto ORDER BY Nombre"),
            Map.entry("Especialidad", "SELECT IdEspecialidad, Nombre FROM Especialidades ORDER BY Nombre"),
            Map.entry("Movimiento", "SELECT IdMovimiento, N'Movimiento ' + CAST(IdMovimiento AS NVARCHAR(20)) FROM MovimientosInventario ORDER BY IdMovimiento DESC"));

    private OpcionesDao() {
    }

    private static List<Opcion> consultar(String sql, Object... parametros) {
        return Transaccion.consulta(c -> JdbcUtil.lista(c, sql, rs -> new Opcion(rs.getInt(1), rs.getString(2)), parametros));
    }

    public static List<Opcion> relacion(String nombre) {
        String sql = RELACIONES.get(nombre);
        if (sql == null) {
            throw new IllegalArgumentException("Relacion desconocida: " + nombre);
        }
        return consultar(sql);
    }

    public static List<Opcion> clientesActivos() {
        return consultar("SELECT IdCliente, Nombre + N' ' + Apellido + N' (' + Identidad + N')' FROM Clientes WHERE Activo = 1 ORDER BY Nombre, Apellido");
    }

    public static List<Opcion> vehiculosDeCliente(int idCliente) {
        return consultar("SELECT IdVehiculo, Placa + N' - ' + Marca + N' ' + Modelo + N' ' + CAST(Anio AS NVARCHAR(10)) FROM Vehiculos WHERE IdCliente = ? AND Activo = 1 ORDER BY Placa", idCliente);
    }

    public static List<Opcion> mecanicosDisponibles() {
        return consultar("SELECT IdMecanico, Nombre + N' ' + Apellido FROM Mecanicos WHERE Activo = 1 AND Disponible = 1 ORDER BY Nombre, Apellido");
    }

    public static List<Opcion> mecanicosActivos() {
        return consultar("SELECT IdMecanico, Nombre + N' ' + Apellido FROM Mecanicos WHERE Activo = 1 ORDER BY Nombre, Apellido");
    }

    public static List<Opcion> ordenes() {
        return consultar("SELECT o.IdOrden, o.NumeroOrden + N' | ' + o.Estado + N' | ' + v.Placa FROM OrdenesTrabajo o INNER JOIN Vehiculos v ON v.IdVehiculo = o.IdVehiculo ORDER BY o.IdOrden DESC");
    }

    public static List<Opcion> ordenesCambioMotor() {
        return consultar("SELECT o.IdOrden, o.NumeroOrden + N' | ' + v.Placa + N' ' + v.Marca + N' ' + v.Modelo + N' ' + CAST(v.Anio AS NVARCHAR(10)) FROM OrdenesTrabajo o INNER JOIN Vehiculos v ON v.IdVehiculo = o.IdVehiculo WHERE o.EsCambioMotor = 1 AND o.Estado IN (N'En reparacion', N'En control de calidad') AND NOT EXISTS (SELECT 1 FROM CambiosMotor cm WHERE cm.IdOrden = o.IdOrden) ORDER BY o.IdOrden DESC");
    }

    public static List<Opcion> motoresCompatibles(int idOrden) {
        return consultar("SELECT m.IdMotor, m.NumeroSerie + N' - ' + m.Marca + N' ' + m.Modelo FROM Motores m INNER JOIN OrdenesTrabajo o ON o.IdOrden = " + idOrden
                + " INNER JOIN Vehiculos v ON v.IdVehiculo = o.IdVehiculo WHERE m.Estado = N'Disponible' AND EXISTS (SELECT 1 FROM CompatibilidadMotor cp WHERE cp.IdMotor = m.IdMotor "
                + "AND cp.MarcaVehiculo = v.Marca AND cp.ModeloVehiculo = v.Modelo AND v.Anio BETWEEN cp.AnioDesde AND cp.AnioHasta) ORDER BY m.NumeroSerie");
    }

    public static List<Opcion> repuestosActivos() {
        return consultar("SELECT IdRepuesto, Codigo + N' - ' + Nombre + N' (stock ' + CAST(StockActual AS NVARCHAR(10)) + N')' FROM Repuestos WHERE Activo = 1 ORDER BY Nombre");
    }

    public static List<Opcion> impuestosVigentes() {
        return consultar("SELECT IdImpuesto, Nombre + N' (' + CAST(Porcentaje AS NVARCHAR(10)) + N'%)' FROM ConfiguracionImpuestos WHERE Vigente = 1 ORDER BY Nombre");
    }

    public static hn.autotech.model.ResultadoConsulta vehiculosDeClienteTabla(int idCliente) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c, "SELECT Placa, VIN, Marca, Modelo, Anio, Color, Kilometraje, Combustible FROM Vehiculos WHERE IdCliente = ? AND Activo = 1 ORDER BY Placa", idCliente));
    }
}
