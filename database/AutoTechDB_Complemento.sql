SET NOCOUNT ON;
GO

DROP TRIGGER IF EXISTS trg_Auditoria_Clientes;
DROP TRIGGER IF EXISTS trg_Auditoria_Repuestos;
DROP TRIGGER IF EXISTS trg_Auditoria_Ordenes;
DROP TRIGGER IF EXISTS trg_Ordenes_ValidarFinalizacion;
DROP TRIGGER IF EXISTS trg_OrdenMecanico_Validar;
DROP TRIGGER IF EXISTS trg_Pagos_Validar;
DROP VIEW IF EXISTS vw_HistorialVehiculo;
DROP VIEW IF EXISTS vw_OrdenesResumen;
DROP VIEW IF EXISTS vw_RepuestosBajoMinimo;
DROP VIEW IF EXISTS vw_CargaMecanicos;
DROP PROCEDURE IF EXISTS sp_RegistrarConsumoRepuesto;
DROP PROCEDURE IF EXISTS sp_GenerarFactura;
DROP PROCEDURE IF EXISTS sp_RegistrarPago;
DROP PROCEDURE IF EXISTS sp_ReporteIngresos;
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_CM_Destino')
    ALTER TABLE CambiosMotor DROP CONSTRAINT CK_CM_Destino;
GO
ALTER TABLE CambiosMotor ADD CONSTRAINT CK_CM_Destino
    CHECK (DestinoMotorRetirado IN (N'Reciclaje', N'Devuelto al cliente', N'Venta como repuesto', N'Desecho', N'Bodega'));
GO

IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'seq_NumeroOrden')
    CREATE SEQUENCE seq_NumeroOrden AS INT START WITH 1 INCREMENT BY 1;
IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'seq_NumeroFactura')
    CREATE SEQUENCE seq_NumeroFactura AS INT START WITH 1 INCREMENT BY 1;
GO

IF OBJECT_ID(N'dbo.Facturas', N'U') IS NULL
CREATE TABLE Facturas (
    IdFactura INT IDENTITY(1,1) CONSTRAINT PK_Facturas PRIMARY KEY,
    NumeroFactura NVARCHAR(30) NOT NULL CONSTRAINT UQ_Facturas_Numero UNIQUE,
    IdOrden INT NOT NULL CONSTRAINT UQ_Facturas_Orden UNIQUE CONSTRAINT FK_Fact_Ordenes FOREIGN KEY REFERENCES OrdenesTrabajo(IdOrden),
    Fecha DATETIME2(0) NOT NULL CONSTRAINT DF_Fact_Fecha DEFAULT SYSDATETIME(),
    Subtotal DECIMAL(14,2) NOT NULL,
    Descuento DECIMAL(14,2) NOT NULL,
    PorcentajeImpuesto DECIMAL(5,2) NOT NULL,
    Impuesto DECIMAL(14,2) NOT NULL,
    Total DECIMAL(14,2) NOT NULL,
    Estado NVARCHAR(20) NOT NULL CONSTRAINT DF_Fact_Estado DEFAULT N'Emitida',
    CONSTRAINT CK_Fact_Estado CHECK (Estado IN (N'Emitida', N'Pagada', N'Anulada')),
    CONSTRAINT CK_Fact_Montos CHECK (Subtotal >= 0 AND Descuento >= 0 AND Impuesto >= 0 AND Total >= 0)
);
GO

IF OBJECT_ID(N'dbo.Usuarios', N'U') IS NULL
CREATE TABLE Usuarios (
    IdUsuario INT IDENTITY(1,1) CONSTRAINT PK_Usuarios PRIMARY KEY,
    NombreUsuario NVARCHAR(50) NOT NULL CONSTRAINT UQ_Usuarios_Nombre UNIQUE,
    Salt VARBINARY(16) NOT NULL,
    ClaveHash VARBINARY(32) NOT NULL,
    Rol NVARCHAR(20) NOT NULL,
    IdCliente INT NULL CONSTRAINT FK_Usuarios_Clientes FOREIGN KEY REFERENCES Clientes(IdCliente),
    Activo BIT NOT NULL CONSTRAINT DF_Usuarios_Activo DEFAULT 1,
    FechaCreacion DATETIME2(0) NOT NULL CONSTRAINT DF_Usuarios_Fecha DEFAULT SYSDATETIME(),
    CONSTRAINT CK_Usuarios_Rol CHECK (Rol IN (N'Administrador', N'Cliente'))
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_Usuarios_Cliente')
    CREATE UNIQUE INDEX UX_Usuarios_Cliente ON Usuarios(IdCliente) WHERE IdCliente IS NOT NULL;
GO

IF OBJECT_ID(N'dbo.ComentariosOrden', N'U') IS NULL
CREATE TABLE ComentariosOrden (
    IdComentario INT IDENTITY(1,1) CONSTRAINT PK_ComentariosOrden PRIMARY KEY,
    IdOrden INT NOT NULL CONSTRAINT FK_Coment_Ordenes FOREIGN KEY REFERENCES OrdenesTrabajo(IdOrden),
    Fecha DATETIME2(0) NOT NULL CONSTRAINT DF_Coment_Fecha DEFAULT SYSDATETIME(),
    Comentario NVARCHAR(500) NOT NULL,
    Autor NVARCHAR(60) NOT NULL
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Coment_Orden')
    CREATE INDEX IX_Coment_Orden ON ComentariosOrden(IdOrden, Fecha);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_OrdenMecanico_Responsable')
    CREATE UNIQUE INDEX UX_OrdenMecanico_Responsable ON OrdenMecanico(IdOrden) WHERE EsResponsable = 1;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Vehiculos_Cliente')
    CREATE INDEX IX_Vehiculos_Cliente ON Vehiculos(IdCliente);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Ordenes_Vehiculo')
    CREATE INDEX IX_Ordenes_Vehiculo ON OrdenesTrabajo(IdVehiculo);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Ordenes_Estado')
    CREATE INDEX IX_Ordenes_Estado ON OrdenesTrabajo(Estado);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Consumo_Orden')
    CREATE INDEX IX_Consumo_Orden ON ConsumoRepuestoOrden(IdOrden, Registrado);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Pagos_Orden')
    CREATE INDEX IX_Pagos_Orden ON Pagos(IdOrden);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Mov_Repuesto')
    CREATE INDEX IX_Mov_Repuesto ON MovimientosInventario(IdRepuesto);
GO

DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql = @sql + N'ALTER TABLE ' + QUOTENAME(OBJECT_NAME(cc.parent_object_id)) + N' DROP CONSTRAINT ' + QUOTENAME(cc.name) + N';'
FROM sys.check_constraints cc
INNER JOIN (VALUES (N'OrdenesTrabajo', N'Estado'), (N'Presupuestos', N'Estado'), (N'ActividadesTrabajo', N'Estado'),
                   (N'Motores', N'Estado'), (N'MovimientosInventario', N'Tipo'), (N'Pagos', N'Metodo'),
                   (N'DetallePresupuesto', N'Tipo')) t (Tabla, Columna)
    ON t.Tabla = OBJECT_NAME(cc.parent_object_id) AND CHARINDEX(N'[' + t.Columna + N']', cc.definition) > 0;
EXEC sp_executesql @sql;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Ordenes_Estado')
    ALTER TABLE OrdenesTrabajo ADD CONSTRAINT CK_Ordenes_Estado CHECK (Estado IN (N'Recibida', N'En diagnostico', N'Pendiente de aprobacion', N'En reparacion', N'En control de calidad', N'Finalizada', N'Rechazada', N'Cancelada'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Pres_Estado')
    ALTER TABLE Presupuestos ADD CONSTRAINT CK_Pres_Estado CHECK (Estado IN (N'Pendiente', N'Aprobado', N'Rechazado'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Act_Estado')
    ALTER TABLE ActividadesTrabajo ADD CONSTRAINT CK_Act_Estado CHECK (Estado IN (N'Pendiente', N'En proceso', N'Terminada', N'Cancelada'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Motores_Estado')
    ALTER TABLE Motores ADD CONSTRAINT CK_Motores_Estado CHECK (Estado IN (N'Disponible', N'Instalado', N'Reservado', N'Retirado', N'Descartado'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Mov_Tipo')
    ALTER TABLE MovimientosInventario ADD CONSTRAINT CK_Mov_Tipo CHECK (Tipo IN (N'Entrada', N'Salida'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Pagos_Metodo')
    ALTER TABLE Pagos ADD CONSTRAINT CK_Pagos_Metodo CHECK (Metodo IN (N'Efectivo', N'Tarjeta', N'Transferencia', N'Cheque'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_DetPres_Tipo')
    ALTER TABLE DetallePresupuesto ADD CONSTRAINT CK_DetPres_Tipo CHECK (Tipo IN (N'Servicio', N'Repuesto', N'Otro'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Impuestos_Porcentaje')
    ALTER TABLE ConfiguracionImpuestos ADD CONSTRAINT CK_Impuestos_Porcentaje CHECK (Porcentaje BETWEEN 0 AND 100);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Vehiculos_Anio')
    ALTER TABLE Vehiculos ADD CONSTRAINT CK_Vehiculos_Anio CHECK (Anio BETWEEN 1950 AND 2100);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Vehiculos_Km')
    ALTER TABLE Vehiculos ADD CONSTRAINT CK_Vehiculos_Km CHECK (Kilometraje >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Repuestos_Stock')
    ALTER TABLE Repuestos ADD CONSTRAINT CK_Repuestos_Stock CHECK (StockActual >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Repuestos_Minimo')
    ALTER TABLE Repuestos ADD CONSTRAINT CK_Repuestos_Minimo CHECK (StockMinimo >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Repuestos_Costo')
    ALTER TABLE Repuestos ADD CONSTRAINT CK_Repuestos_Costo CHECK (CostoUnitario >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Repuestos_Precio')
    ALTER TABLE Repuestos ADD CONSTRAINT CK_Repuestos_Precio CHECK (PrecioVenta >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Servicios_Nivel')
    ALTER TABLE Servicios ADD CONSTRAINT CK_Servicios_Nivel CHECK (Nivel BETWEEN 1 AND 5);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Servicios_Costo')
    ALTER TABLE Servicios ADD CONSTRAINT CK_Servicios_Costo CHECK (CostoManoObra >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Servicios_Tiempo')
    ALTER TABLE Servicios ADD CONSTRAINT CK_Servicios_Tiempo CHECK (TiempoEstimadoHoras >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_ServRep_Cantidad')
    ALTER TABLE ServicioRepuesto ADD CONSTRAINT CK_ServRep_Cantidad CHECK (Cantidad > 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Motores_Costo')
    ALTER TABLE Motores ADD CONSTRAINT CK_Motores_Costo CHECK (Costo >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Compat_Anios')
    ALTER TABLE CompatibilidadMotor ADD CONSTRAINT CK_Compat_Anios CHECK (AnioDesde > 0 AND AnioHasta >= AnioDesde);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Ordenes_Km')
    ALTER TABLE OrdenesTrabajo ADD CONSTRAINT CK_Ordenes_Km CHECK (KilometrajeIngreso >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Ordenes_Total')
    ALTER TABLE OrdenesTrabajo ADD CONSTRAINT CK_Ordenes_Total CHECK (Total >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_DetOrd_Montos')
    ALTER TABLE DetalleOrden ADD CONSTRAINT CK_DetOrd_Montos CHECK (PrecioAplicado >= 0 AND Horas >= 0 AND Subtotal >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Act_Horas')
    ALTER TABLE ActividadesTrabajo ADD CONSTRAINT CK_Act_Horas CHECK (Horas >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Pres_Montos')
    ALTER TABLE Presupuestos ADD CONSTRAINT CK_Pres_Montos CHECK (Subtotal >= 0 AND Descuento >= 0 AND Impuesto >= 0 AND Total >= 0 AND PorcentajeImpuesto BETWEEN 0 AND 100);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Pres_DescMax')
    ALTER TABLE Presupuestos ADD CONSTRAINT CK_Pres_DescMax CHECK (Descuento <= Subtotal);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Pres_DescAut')
    ALTER TABLE Presupuestos ADD CONSTRAINT CK_Pres_DescAut CHECK (Descuento = 0 OR AutorizadoDescuentoPor IS NOT NULL);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_DetPres_Montos')
    ALTER TABLE DetallePresupuesto ADD CONSTRAINT CK_DetPres_Montos CHECK (Cantidad > 0 AND PrecioUnitario >= 0 AND Subtotal >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Mov_Montos')
    ALTER TABLE MovimientosInventario ADD CONSTRAINT CK_Mov_Montos CHECK (Cantidad > 0 AND CostoUnitario >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Cons_Montos')
    ALTER TABLE ConsumoRepuestoOrden ADD CONSTRAINT CK_Cons_Montos CHECK (Cantidad > 0 AND PrecioAplicado >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_CamMot_Fechas')
    ALTER TABLE CambiosMotor ADD CONSTRAINT CK_CamMot_Fechas CHECK (FechaInstalacion IS NULL OR FechaDesmontaje IS NULL OR FechaInstalacion >= FechaDesmontaje);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Gar_Fechas')
    ALTER TABLE Garantias ADD CONSTRAINT CK_Gar_Fechas CHECK (FechaFin >= FechaInicio);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Gar_Km')
    ALTER TABLE Garantias ADD CONSTRAINT CK_Gar_Km CHECK (KilometrajeLimite IS NULL OR KilometrajeLimite >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Pagos_Monto')
    ALTER TABLE Pagos ADD CONSTRAINT CK_Pagos_Monto CHECK (Monto > 0);

GO

CREATE TRIGGER trg_Auditoria_Clientes ON Clientes AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO AuditoriaLog (Tabla, Operacion, IdRegistro, DatosAnteriores, DatosNuevos)
    SELECT N'Clientes',
           CASE WHEN i.IdCliente IS NOT NULL AND d.IdCliente IS NOT NULL THEN N'UPDATE'
                WHEN i.IdCliente IS NOT NULL THEN N'INSERT' ELSE N'DELETE' END,
           CAST(COALESCE(i.IdCliente, d.IdCliente) AS NVARCHAR(50)),
           (SELECT x.* FROM deleted x WHERE x.IdCliente = d.IdCliente FOR JSON PATH, WITHOUT_ARRAY_WRAPPER),
           (SELECT y.* FROM inserted y WHERE y.IdCliente = i.IdCliente FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
    FROM inserted i FULL OUTER JOIN deleted d ON d.IdCliente = i.IdCliente;
END;
GO

CREATE TRIGGER trg_Auditoria_Repuestos ON Repuestos AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO AuditoriaLog (Tabla, Operacion, IdRegistro, DatosAnteriores, DatosNuevos)
    SELECT N'Repuestos',
           CASE WHEN i.IdRepuesto IS NOT NULL AND d.IdRepuesto IS NOT NULL THEN N'UPDATE'
                WHEN i.IdRepuesto IS NOT NULL THEN N'INSERT' ELSE N'DELETE' END,
           CAST(COALESCE(i.IdRepuesto, d.IdRepuesto) AS NVARCHAR(50)),
           (SELECT x.* FROM deleted x WHERE x.IdRepuesto = d.IdRepuesto FOR JSON PATH, WITHOUT_ARRAY_WRAPPER),
           (SELECT y.* FROM inserted y WHERE y.IdRepuesto = i.IdRepuesto FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
    FROM inserted i FULL OUTER JOIN deleted d ON d.IdRepuesto = i.IdRepuesto;
END;
GO

CREATE TRIGGER trg_Auditoria_Ordenes ON OrdenesTrabajo AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO AuditoriaLog (Tabla, Operacion, IdRegistro, DatosAnteriores, DatosNuevos)
    SELECT N'OrdenesTrabajo',
           CASE WHEN i.IdOrden IS NOT NULL AND d.IdOrden IS NOT NULL THEN N'UPDATE'
                WHEN i.IdOrden IS NOT NULL THEN N'INSERT' ELSE N'DELETE' END,
           CAST(COALESCE(i.IdOrden, d.IdOrden) AS NVARCHAR(50)),
           (SELECT x.* FROM deleted x WHERE x.IdOrden = d.IdOrden FOR JSON PATH, WITHOUT_ARRAY_WRAPPER),
           (SELECT y.* FROM inserted y WHERE y.IdOrden = i.IdOrden FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
    FROM inserted i FULL OUTER JOIN deleted d ON d.IdOrden = i.IdOrden;
END;
GO

CREATE TRIGGER trg_Ordenes_ValidarFinalizacion ON OrdenesTrabajo AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(Estado) AND EXISTS (
        SELECT 1
        FROM inserted i INNER JOIN deleted d ON d.IdOrden = i.IdOrden
        WHERE i.Estado = N'Finalizada' AND d.Estado <> N'Finalizada'
          AND (EXISTS (SELECT 1 FROM ActividadesTrabajo a WHERE a.IdOrden = i.IdOrden AND a.Estado IN (N'Pendiente', N'En proceso'))
            OR EXISTS (SELECT 1 FROM ConsumoRepuestoOrden c WHERE c.IdOrden = i.IdOrden AND c.Registrado = 0)
            OR EXISTS (SELECT 1 FROM ControlesCalidad q WHERE q.IdOrden = i.IdOrden AND q.Obligatorio = 1 AND q.Aprobado = 0)))
    BEGIN
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW 50030, 'No se puede finalizar la orden: hay actividades pendientes, consumos no registrados o controles de calidad obligatorios sin aprobar.', 1;
    END
END;
GO

CREATE TRIGGER trg_OrdenMecanico_Validar ON OrdenMecanico AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i INNER JOIN Mecanicos m ON m.IdMecanico = i.IdMecanico WHERE m.Activo = 0)
    BEGIN
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW 50010, 'No se puede asignar personal inactivo a una orden.', 1;
    END
END;
GO

CREATE TRIGGER trg_Pagos_Validar ON Pagos AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i WHERE NOT EXISTS (SELECT 1 FROM Facturas f WHERE f.IdOrden = i.IdOrden AND f.Estado <> N'Anulada'))
    BEGIN
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW 50021, 'No se puede registrar un pago para una orden sin factura emitida.', 1;
    END
    IF EXISTS (
        SELECT 1
        FROM Facturas f
        WHERE f.IdOrden IN (SELECT IdOrden FROM inserted)
          AND (SELECT COALESCE(SUM(p.Monto), 0) FROM Pagos p WHERE p.IdOrden = f.IdOrden) > f.Total)
    BEGIN
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW 50023, 'El pago excede el saldo pendiente de la factura.', 1;
    END
END;
GO

CREATE VIEW vw_OrdenesResumen AS
SELECT o.IdOrden, o.NumeroOrden, o.FechaIngreso, o.Estado,
       c.Nombre + N' ' + c.Apellido AS Cliente,
       v.Placa, v.Marca + N' ' + v.Modelo AS Vehiculo,
       o.Total,
       f.NumeroFactura,
       f.Total AS TotalFactura,
       COALESCE(p.TotalPagado, 0) AS TotalPagado,
       COALESCE(f.Total, 0) - COALESCE(p.TotalPagado, 0) AS Saldo
FROM OrdenesTrabajo o
INNER JOIN Clientes c ON c.IdCliente = o.IdCliente
INNER JOIN Vehiculos v ON v.IdVehiculo = o.IdVehiculo
LEFT JOIN Facturas f ON f.IdOrden = o.IdOrden AND f.Estado <> N'Anulada'
LEFT JOIN (SELECT IdOrden, SUM(Monto) AS TotalPagado FROM Pagos GROUP BY IdOrden) p ON p.IdOrden = o.IdOrden;
GO

CREATE VIEW vw_HistorialVehiculo AS
SELECT v.IdVehiculo, v.Placa, v.Marca + N' ' + v.Modelo AS Vehiculo,
       o.NumeroOrden, o.FechaIngreso, o.FechaEntrega, o.Estado,
       (SELECT STRING_AGG(s.Nombre, N', ')
        FROM DetalleOrden d INNER JOIN Servicios s ON s.IdServicio = d.IdServicio
        WHERE d.IdOrden = o.IdOrden) AS Servicios,
       (SELECT STRING_AGG(m.Nombre + N' ' + m.Apellido, N', ')
        FROM OrdenMecanico om INNER JOIN Mecanicos m ON m.IdMecanico = om.IdMecanico
        WHERE om.IdOrden = o.IdOrden) AS Mecanicos,
       (SELECT STRING_AGG(r.Nombre + N' x' + CAST(c.Cantidad AS NVARCHAR(10)), N', ')
        FROM ConsumoRepuestoOrden c INNER JOIN Repuestos r ON r.IdRepuesto = c.IdRepuesto
        WHERE c.IdOrden = o.IdOrden AND c.Registrado = 1) AS Repuestos,
       o.Total AS Costo
FROM Vehiculos v
INNER JOIN OrdenesTrabajo o ON o.IdVehiculo = v.IdVehiculo;
GO

CREATE VIEW vw_RepuestosBajoMinimo AS
SELECT r.IdRepuesto, r.Codigo, r.Nombre, cat.Nombre AS Categoria,
       pr.Nombre AS Proveedor, r.StockActual, r.StockMinimo,
       r.StockMinimo - r.StockActual AS Faltante
FROM Repuestos r
INNER JOIN CategoriasRepuesto cat ON cat.IdCategoria = r.IdCategoria
LEFT JOIN Proveedores pr ON pr.IdProveedor = r.IdProveedor
WHERE r.Activo = 1 AND r.StockActual <= r.StockMinimo;
GO

CREATE VIEW vw_CargaMecanicos AS
SELECT m.IdMecanico, m.Nombre + N' ' + m.Apellido AS Mecanico, m.Activo, m.Disponible,
       COUNT(o.IdOrden) AS OrdenesActivas
FROM Mecanicos m
LEFT JOIN OrdenMecanico om ON om.IdMecanico = m.IdMecanico
LEFT JOIN OrdenesTrabajo o ON o.IdOrden = om.IdOrden
     AND o.Estado NOT IN (N'Finalizada', N'Rechazada', N'Cancelada')
GROUP BY m.IdMecanico, m.Nombre, m.Apellido, m.Activo, m.Disponible;
GO

CREATE PROCEDURE sp_RegistrarConsumoRepuesto
    @IdConsumo INT,
    @Usuario NVARCHAR(60) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @Propia BIT = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
    DECLARE @IdOrden INT, @IdRepuesto INT, @Cantidad INT, @Registrado BIT, @Costo DECIMAL(12,2), @IdMov INT;
    BEGIN TRY
        IF @Propia = 1 BEGIN TRANSACTION;
        SELECT @IdOrden = IdOrden, @IdRepuesto = IdRepuesto, @Cantidad = Cantidad, @Registrado = Registrado
        FROM ConsumoRepuestoOrden WITH (UPDLOCK, ROWLOCK)
        WHERE IdConsumo = @IdConsumo;
        IF @IdRepuesto IS NULL THROW 50001, 'El consumo indicado no existe.', 1;
        IF @Registrado = 1 THROW 50002, 'El consumo ya fue registrado en el inventario.', 1;
        UPDATE Repuestos
        SET StockActual = StockActual - @Cantidad
        WHERE IdRepuesto = @IdRepuesto AND Activo = 1 AND StockActual >= @Cantidad;
        IF @@ROWCOUNT = 0 THROW 50003, 'Stock insuficiente o repuesto inactivo.', 1;
        SELECT @Costo = CostoUnitario FROM Repuestos WHERE IdRepuesto = @IdRepuesto;
        INSERT INTO MovimientosInventario (IdRepuesto, Tipo, Cantidad, CostoUnitario, IdOrden, Motivo, Usuario)
        VALUES (@IdRepuesto, N'Salida', @Cantidad, @Costo, @IdOrden, N'Consumo de orden de trabajo', COALESCE(@Usuario, LEFT(SUSER_SNAME(), 60)));
        SET @IdMov = SCOPE_IDENTITY();
        UPDATE ConsumoRepuestoOrden SET Registrado = 1, IdMovimiento = @IdMov WHERE IdConsumo = @IdConsumo;
        IF @Propia = 1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @Propia = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

CREATE PROCEDURE sp_GenerarFactura
    @IdOrden INT,
    @IdFactura INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @Propia BIT = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
    DECLARE @Subtotal DECIMAL(12,2), @Descuento DECIMAL(12,2) = 0, @Pct DECIMAL(5,2), @Impuesto DECIMAL(12,2), @Total DECIMAL(12,2);
    DECLARE @Numero NVARCHAR(30);
    BEGIN TRY
        IF @Propia = 1 BEGIN TRANSACTION;
        IF NOT EXISTS (SELECT 1 FROM OrdenesTrabajo WITH (UPDLOCK, ROWLOCK) WHERE IdOrden = @IdOrden)
            THROW 50010, 'La orden indicada no existe.', 1;
        IF EXISTS (SELECT 1 FROM Facturas WHERE IdOrden = @IdOrden)
            THROW 50011, 'La orden ya tiene una factura emitida.', 1;
        SELECT @Subtotal = COALESCE((SELECT SUM(Subtotal) FROM DetalleOrden WHERE IdOrden = @IdOrden), 0)
                         + COALESCE((SELECT SUM(Cantidad * PrecioAplicado) FROM ConsumoRepuestoOrden WHERE IdOrden = @IdOrden AND Registrado = 1), 0);
        SELECT TOP (1) @Descuento = Descuento, @Pct = PorcentajeImpuesto
        FROM Presupuestos WHERE IdOrden = @IdOrden AND Estado = N'Aprobado' ORDER BY IdPresupuesto DESC;
        IF @Pct IS NULL
            SELECT TOP (1) @Pct = Porcentaje FROM ConfiguracionImpuestos WHERE Vigente = 1 ORDER BY IdImpuesto;
        SET @Pct = COALESCE(@Pct, 0);
        IF @Descuento > @Subtotal SET @Descuento = @Subtotal;
        SET @Impuesto = ROUND((@Subtotal - @Descuento) * @Pct / 100.0, 2);
        SET @Total = @Subtotal - @Descuento + @Impuesto;
        SET @Numero = N'FAC-' + RIGHT(N'000000' + CAST(NEXT VALUE FOR seq_NumeroFactura AS NVARCHAR(10)), 6);
        INSERT INTO Facturas (NumeroFactura, IdOrden, Subtotal, Descuento, PorcentajeImpuesto, Impuesto, Total)
        VALUES (@Numero, @IdOrden, @Subtotal, @Descuento, @Pct, @Impuesto, @Total);
        SET @IdFactura = SCOPE_IDENTITY();
        UPDATE OrdenesTrabajo SET Total = @Total WHERE IdOrden = @IdOrden;
        IF @Propia = 1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @Propia = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

CREATE PROCEDURE sp_RegistrarPago
    @IdOrden INT,
    @Monto DECIMAL(12,2),
    @Metodo NVARCHAR(30),
    @Referencia NVARCHAR(80) = NULL,
    @IdPago INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @Propia BIT = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
    DECLARE @IdFactura INT, @Total DECIMAL(12,2), @Pagado DECIMAL(12,2);
    BEGIN TRY
        IF @Propia = 1 BEGIN TRANSACTION;
        IF @Monto <= 0 THROW 50022, 'El monto del pago debe ser mayor que cero.', 1;
        SELECT @IdFactura = IdFactura, @Total = Total
        FROM Facturas WITH (UPDLOCK, HOLDLOCK)
        WHERE IdOrden = @IdOrden AND Estado <> N'Anulada';
        IF @IdFactura IS NULL THROW 50021, 'La orden no tiene factura emitida.', 1;
        SELECT @Pagado = COALESCE(SUM(Monto), 0) FROM Pagos WHERE IdOrden = @IdOrden;
        IF @Pagado + @Monto > @Total THROW 50023, 'El pago excede el saldo pendiente de la factura.', 1;
        INSERT INTO Pagos (IdOrden, Monto, Metodo, Referencia) VALUES (@IdOrden, @Monto, @Metodo, @Referencia);
        SET @IdPago = SCOPE_IDENTITY();
        IF @Pagado + @Monto = @Total UPDATE Facturas SET Estado = N'Pagada' WHERE IdFactura = @IdFactura;
        IF @Propia = 1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @Propia = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

CREATE PROCEDURE sp_ReporteIngresos
    @Desde DATE = NULL,
    @Hasta DATE = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET @Desde = COALESCE(@Desde, DATEADD(MONTH, -12, CAST(SYSDATETIME() AS DATE)));
    SET @Hasta = COALESCE(@Hasta, CAST(SYSDATETIME() AS DATE));
    SELECT YEAR(f.Fecha) AS Anio, MONTH(f.Fecha) AS Mes,
           COUNT(*) AS Facturas,
           SUM(f.Subtotal) AS Subtotal,
           SUM(f.Descuento) AS Descuentos,
           SUM(f.Impuesto) AS Impuestos,
           SUM(f.Total) AS TotalFacturado,
           COALESCE(SUM(p.Pagado), 0) AS TotalCobrado
    FROM Facturas f
    LEFT JOIN (SELECT IdOrden, SUM(Monto) AS Pagado FROM Pagos GROUP BY IdOrden) p ON p.IdOrden = f.IdOrden
    WHERE f.Estado <> N'Anulada' AND CAST(f.Fecha AS DATE) BETWEEN @Desde AND @Hasta
    GROUP BY YEAR(f.Fecha), MONTH(f.Fecha)
    ORDER BY Anio, Mes;
END;
GO


INSERT INTO ConfiguracionImpuestos (Nombre, Porcentaje, Vigente)
SELECT v.Nombre, v.Porcentaje, 1
FROM (VALUES (N'ISV 15%', CAST(15 AS DECIMAL(5,2))), (N'Exento', CAST(0 AS DECIMAL(5,2)))) v (Nombre, Porcentaje)
WHERE NOT EXISTS (SELECT 1 FROM ConfiguracionImpuestos x WHERE x.Nombre = v.Nombre);

INSERT INTO Especialidades (Nombre, Descripcion)
SELECT v.Nombre, v.Descripcion
FROM (VALUES
    (N'Mecanica general', N'Mantenimiento y reparaciones comunes'),
    (N'Motores', N'Diagnostico, reconstruccion y sustitucion de motores'),
    (N'Transmision', N'Cajas manuales y automaticas'),
    (N'Sistema electrico', N'Alternadores, arranque y electronica'),
    (N'Frenos y suspension', N'Frenos, amortiguadores y direccion')) v (Nombre, Descripcion)
WHERE NOT EXISTS (SELECT 1 FROM Especialidades x WHERE x.Nombre = v.Nombre);

INSERT INTO Mecanicos (Nombre, Apellido, Identidad, Telefono, Correo)
SELECT v.Nombre, v.Apellido, v.Identidad, v.Telefono, v.Correo
FROM (VALUES
    (N'Carlos', N'Mejia', N'0801-1985-00101', N'9999-1001', N'carlos.mejia@autotech.hn'),
    (N'Luis', N'Fuentes', N'0801-1990-00202', N'9999-1002', N'luis.fuentes@autotech.hn'),
    (N'Jose', N'Rivera', N'0801-1988-00303', N'9999-1003', N'jose.rivera@autotech.hn'),
    (N'Marvin', N'Lopez', N'0801-1992-00404', N'9999-1004', N'marvin.lopez@autotech.hn')) v (Nombre, Apellido, Identidad, Telefono, Correo)
WHERE NOT EXISTS (SELECT 1 FROM Mecanicos x WHERE x.Identidad = v.Identidad);

INSERT INTO MecanicoEspecialidad (IdMecanico, IdEspecialidad)
SELECT m.IdMecanico, e.IdEspecialidad
FROM (VALUES
    (N'0801-1985-00101', N'Mecanica general'), (N'0801-1985-00101', N'Frenos y suspension'),
    (N'0801-1990-00202', N'Motores'), (N'0801-1990-00202', N'Mecanica general'),
    (N'0801-1988-00303', N'Transmision'), (N'0801-1992-00404', N'Sistema electrico')) v (Identidad, Especialidad)
INNER JOIN Mecanicos m ON m.Identidad = v.Identidad
INNER JOIN Especialidades e ON e.Nombre = v.Especialidad
WHERE NOT EXISTS (SELECT 1 FROM MecanicoEspecialidad x WHERE x.IdMecanico = m.IdMecanico AND x.IdEspecialidad = e.IdEspecialidad);

INSERT INTO Proveedores (Nombre, Contacto, Telefono, Correo, Direccion)
SELECT v.Nombre, v.Contacto, v.Telefono, v.Correo, v.Direccion
FROM (VALUES
    (N'Repuestos del Norte', N'Ana Perez', N'2550-1111', N'ventas@repuestosnorte.hn', N'San Pedro Sula'),
    (N'Autopartes Centro', N'Mario Diaz', N'2220-2222', N'contacto@autopartescentro.hn', N'Tegucigalpa'),
    (N'Motores y Mas', N'Rosa Aguilar', N'2230-3333', N'rosa@motoresymas.hn', N'Tegucigalpa')) v (Nombre, Contacto, Telefono, Correo, Direccion)
WHERE NOT EXISTS (SELECT 1 FROM Proveedores x WHERE x.Nombre = v.Nombre);

INSERT INTO CategoriasRepuesto (Nombre)
SELECT v.Nombre
FROM (VALUES (N'Aceites'), (N'Filtros'), (N'Bujias'), (N'Baterias'), (N'Frenos'), (N'Radiadores'), (N'Transmisiones'), (N'Motores')) v (Nombre)
WHERE NOT EXISTS (SELECT 1 FROM CategoriasRepuesto x WHERE x.Nombre = v.Nombre);

INSERT INTO Repuestos (Codigo, Nombre, IdCategoria, IdProveedor, StockActual, StockMinimo, CostoUnitario, PrecioVenta)
SELECT v.Codigo, v.Nombre, c.IdCategoria, p.IdProveedor, v.Stock, v.Minimo, v.Costo, v.Precio
FROM (VALUES
    (N'ACE-5W30', N'Aceite sintetico 5W30 (litro)', N'Aceites', N'Repuestos del Norte', 60, 12, CAST(95.00 AS DECIMAL(12,2)), CAST(150.00 AS DECIMAL(12,2))),
    (N'FIL-ACE-01', N'Filtro de aceite', N'Filtros', N'Repuestos del Norte', 30, 8, CAST(80.00 AS DECIMAL(12,2)), CAST(140.00 AS DECIMAL(12,2))),
    (N'FIL-AIR-01', N'Filtro de aire', N'Filtros', N'Repuestos del Norte', 25, 8, CAST(110.00 AS DECIMAL(12,2)), CAST(190.00 AS DECIMAL(12,2))),
    (N'BUJ-IR-04', N'Bujia de iridio', N'Bujias', N'Autopartes Centro', 40, 12, CAST(120.00 AS DECIMAL(12,2)), CAST(210.00 AS DECIMAL(12,2))),
    (N'BAT-12V-60', N'Bateria 12V 60Ah', N'Baterias', N'Autopartes Centro', 10, 3, CAST(1900.00 AS DECIMAL(12,2)), CAST(2600.00 AS DECIMAL(12,2))),
    (N'FRE-PAS-01', N'Pastillas de freno delanteras', N'Frenos', N'Autopartes Centro', 15, 4, CAST(650.00 AS DECIMAL(12,2)), CAST(980.00 AS DECIMAL(12,2))),
    (N'RAD-ALU-01', N'Radiador de aluminio', N'Radiadores', N'Autopartes Centro', 5, 2, CAST(2200.00 AS DECIMAL(12,2)), CAST(3100.00 AS DECIMAL(12,2))),
    (N'TRA-AUT-01', N'Transmision automatica reconstruida', N'Transmisiones', N'Motores y Mas', 2, 1, CAST(18000.00 AS DECIMAL(12,2)), CAST(24500.00 AS DECIMAL(12,2))),
    (N'MOT-2TRFE', N'Motor 2.7L 2TR-FE', N'Motores', N'Motores y Mas', 2, 1, CAST(42000.00 AS DECIMAL(12,2)), CAST(55000.00 AS DECIMAL(12,2))),
    (N'MOT-1GDFTV', N'Motor 2.8L 1GD-FTV', N'Motores', N'Motores y Mas', 1, 1, CAST(61000.00 AS DECIMAL(12,2)), CAST(78000.00 AS DECIMAL(12,2)))
    ) v (Codigo, Nombre, Categoria, Proveedor, Stock, Minimo, Costo, Precio)
INNER JOIN CategoriasRepuesto c ON c.Nombre = v.Categoria
INNER JOIN Proveedores p ON p.Nombre = v.Proveedor
WHERE NOT EXISTS (SELECT 1 FROM Repuestos x WHERE x.Codigo = v.Codigo);

INSERT INTO Servicios (Codigo, Nombre, Descripcion, Nivel, CostoManoObra, TiempoEstimadoHoras)
SELECT v.Codigo, v.Nombre, v.Descripcion, v.Nivel, v.Costo, v.Horas
FROM (VALUES
    (N'SRV-001', N'Cambio de aceite', N'Cambio de aceite de motor', CAST(1 AS TINYINT), CAST(250.00 AS DECIMAL(12,2)), CAST(0.50 AS DECIMAL(6,2))),
    (N'SRV-002', N'Cambio de filtros', N'Cambio de filtro de aceite y de aire', CAST(1 AS TINYINT), CAST(200.00 AS DECIMAL(12,2)), CAST(0.50 AS DECIMAL(6,2))),
    (N'SRV-003', N'Cambio de bujias', N'Sustitucion de bujias', CAST(1 AS TINYINT), CAST(300.00 AS DECIMAL(12,2)), CAST(1.00 AS DECIMAL(6,2))),
    (N'SRV-004', N'Revision de luces', N'Revision y cambio de luces', CAST(1 AS TINYINT), CAST(150.00 AS DECIMAL(12,2)), CAST(0.50 AS DECIMAL(6,2))),
    (N'SRV-005', N'Afinamiento', N'Afinamiento completo del motor', CAST(2 AS TINYINT), CAST(900.00 AS DECIMAL(12,2)), CAST(2.50 AS DECIMAL(6,2))),
    (N'SRV-006', N'Limpieza de inyectores', N'Limpieza y prueba de inyectores', CAST(2 AS TINYINT), CAST(1100.00 AS DECIMAL(12,2)), CAST(3.00 AS DECIMAL(6,2))),
    (N'SRV-007', N'Cambio de frenos', N'Sustitucion de pastillas y rectificacion', CAST(2 AS TINYINT), CAST(800.00 AS DECIMAL(12,2)), CAST(2.00 AS DECIMAL(6,2))),
    (N'SRV-008', N'Cambio de radiador', N'Sustitucion de radiador y sistema de enfriamiento', CAST(3 AS TINYINT), CAST(1500.00 AS DECIMAL(12,2)), CAST(4.00 AS DECIMAL(6,2))),
    (N'SRV-009', N'Cambio de alternador', N'Sustitucion de alternador', CAST(3 AS TINYINT), CAST(1300.00 AS DECIMAL(12,2)), CAST(3.00 AS DECIMAL(6,2))),
    (N'SRV-010', N'Reparacion de suspension', N'Amortiguadores y bujes', CAST(3 AS TINYINT), CAST(1800.00 AS DECIMAL(12,2)), CAST(5.00 AS DECIMAL(6,2))),
    (N'SRV-011', N'Cambio de embrague', N'Sustitucion de kit de embrague', CAST(3 AS TINYINT), CAST(2500.00 AS DECIMAL(12,2)), CAST(6.00 AS DECIMAL(6,2))),
    (N'SRV-012', N'Reparacion de transmision', N'Reparacion o cambio de transmision', CAST(4 AS TINYINT), CAST(5000.00 AS DECIMAL(12,2)), CAST(12.00 AS DECIMAL(6,2))),
    (N'SRV-013', N'Reparacion de culata', N'Rectificacion y armado de culata', CAST(4 AS TINYINT), CAST(6500.00 AS DECIMAL(12,2)), CAST(14.00 AS DECIMAL(6,2))),
    (N'SRV-014', N'Reparacion de sistema electrico', N'Diagnostico y reparacion electrica', CAST(4 AS TINYINT), CAST(3500.00 AS DECIMAL(12,2)), CAST(8.00 AS DECIMAL(6,2))),
    (N'SRV-015', N'Reconstruccion de motor', N'Reconstruccion completa de motor', CAST(5 AS TINYINT), CAST(15000.00 AS DECIMAL(12,2)), CAST(40.00 AS DECIMAL(6,2))),
    (N'SRV-016', N'Sustitucion de motor', N'Desmontaje e instalacion de motor completo', CAST(5 AS TINYINT), CAST(12000.00 AS DECIMAL(12,2)), CAST(30.00 AS DECIMAL(6,2)))
    ) v (Codigo, Nombre, Descripcion, Nivel, Costo, Horas)
WHERE NOT EXISTS (SELECT 1 FROM Servicios x WHERE x.Codigo = v.Codigo);

INSERT INTO ServicioRepuesto (IdServicio, IdRepuesto, Cantidad)
SELECT s.IdServicio, r.IdRepuesto, v.Cantidad
FROM (VALUES
    (N'SRV-001', N'ACE-5W30', 4), (N'SRV-001', N'FIL-ACE-01', 1), (N'SRV-002', N'FIL-ACE-01', 1),
    (N'SRV-002', N'FIL-AIR-01', 1), (N'SRV-003', N'BUJ-IR-04', 4), (N'SRV-007', N'FRE-PAS-01', 1),
    (N'SRV-008', N'RAD-ALU-01', 1)) v (Servicio, Repuesto, Cantidad)
INNER JOIN Servicios s ON s.Codigo = v.Servicio
INNER JOIN Repuestos r ON r.Codigo = v.Repuesto
WHERE NOT EXISTS (SELECT 1 FROM ServicioRepuesto x WHERE x.IdServicio = s.IdServicio AND x.IdRepuesto = r.IdRepuesto);

INSERT INTO Motores (NumeroSerie, Marca, Modelo, Cilindraje, Combustible, Estado, Costo, IdRepuesto, IdProveedor)
SELECT v.Serie, v.Marca, v.Modelo, v.Cilindraje, v.Combustible, N'Disponible', v.Costo, r.IdRepuesto, p.IdProveedor
FROM (VALUES
    (N'2TR-8841726', N'Toyota', N'2TR-FE', N'2700', N'Gasolina', CAST(42000.00 AS DECIMAL(12,2)), N'MOT-2TRFE'),
    (N'2TR-8841727', N'Toyota', N'2TR-FE', N'2700', N'Gasolina', CAST(42000.00 AS DECIMAL(12,2)), N'MOT-2TRFE'),
    (N'1GD-5530912', N'Toyota', N'1GD-FTV', N'2800', N'Diesel', CAST(61000.00 AS DECIMAL(12,2)), N'MOT-1GDFTV')) v (Serie, Marca, Modelo, Cilindraje, Combustible, Costo, CodigoRepuesto)
INNER JOIN Repuestos r ON r.Codigo = v.CodigoRepuesto
INNER JOIN Proveedores p ON p.Nombre = N'Motores y Mas'
WHERE NOT EXISTS (SELECT 1 FROM Motores x WHERE x.NumeroSerie = v.Serie);

INSERT INTO CompatibilidadMotor (IdMotor, MarcaVehiculo, ModeloVehiculo, AnioDesde, AnioHasta)
SELECT m.IdMotor, v.Marca, v.Modelo, v.Desde, v.Hasta
FROM (VALUES
    (N'2TR-8841726', N'Toyota', N'Hilux', CAST(2005 AS SMALLINT), CAST(2015 AS SMALLINT)),
    (N'2TR-8841727', N'Toyota', N'Hilux', CAST(2005 AS SMALLINT), CAST(2015 AS SMALLINT)),
    (N'1GD-5530912', N'Toyota', N'Hilux', CAST(2016 AS SMALLINT), CAST(2023 AS SMALLINT))) v (Serie, Marca, Modelo, Desde, Hasta)
INNER JOIN Motores m ON m.NumeroSerie = v.Serie
WHERE NOT EXISTS (SELECT 1 FROM CompatibilidadMotor x WHERE x.IdMotor = m.IdMotor AND x.MarcaVehiculo = v.Marca AND x.ModeloVehiculo = v.Modelo);
GO
