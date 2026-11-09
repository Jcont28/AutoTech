package hn.autotech.service;

import hn.autotech.config.DBConnection;
import hn.autotech.dao.JdbcUtil;
import hn.autotech.dao.OrdenDao;
import hn.autotech.dao.RepuestoDao;
import hn.autotech.dao.ServicioDao;
import hn.autotech.dao.Transaccion;
import hn.autotech.dao.VehiculoDao;
import hn.autotech.exception.NegocioException;
import hn.autotech.model.EstadoOrden;
import hn.autotech.model.Factura;
import hn.autotech.model.NuevaOrden;
import hn.autotech.model.OrdenTrabajo;
import hn.autotech.model.Repuesto;
import hn.autotech.model.Servicio;
import hn.autotech.model.Vehiculo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class OrdenService {
    private final OrdenDao ordenes = new OrdenDao();
    private final VehiculoDao vehiculos = new VehiculoDao();
    private final ServicioDao servicios = new ServicioDao();
    private final RepuestoDao repuestos = new RepuestoDao();
    private final FacturacionService facturacion = new FacturacionService();
    private final String usuario = DBConnection.usuario();

    public int crearOrden(NuevaOrden nueva) {
        if (nueva.sintomas() == null || nueva.sintomas().isBlank()) {
            throw new NegocioException("Registre los síntomas reportados por el cliente.");
        }
        if (nueva.idsServicios() == null || nueva.idsServicios().isEmpty()) {
            throw new NegocioException("Seleccione al menos un servicio para la orden.");
        }
        return Transaccion.ejecutar(c -> {
            if (!JdbcUtil.existe(c, "SELECT 1 FROM Clientes WHERE IdCliente = ? AND Activo = 1", nueva.idCliente())) {
                throw new NegocioException("El cliente no existe o esta inactivo.");
            }
            Vehiculo vehiculo = vehiculos.buscar(c, nueva.idVehiculo()).orElseThrow(() -> new NegocioException("El vehículo no existe."));
            if (!vehiculo.activo() || vehiculo.idCliente() != nueva.idCliente()) {
                throw new NegocioException("El vehículo esta inactivo o no pertenece al cliente seleccionado.");
            }
            if (nueva.kilometraje() < vehiculo.kilometraje()) {
                throw new NegocioException("El kilometraje de ingreso no puede ser menor al registrado en el vehículo (" + vehiculo.kilometraje() + ").");
            }
            validarMecanicoLibre(c, nueva.idResponsable());

            List<Servicio> elegidos = new ArrayList<>();
            Map<Integer, Integer> repuestosPorId = new LinkedHashMap<>();
            for (int idServicio : new LinkedHashSet<>(nueva.idsServicios())) {
                Servicio servicio = servicios.buscar(c, idServicio).orElseThrow(() -> new NegocioException("Uno de los servicios no existe."));
                if (!servicio.activo()) {
                    throw new NegocioException("El servicio " + servicio.nombre() + " esta inactivo.");
                }
                elegidos.add(servicio);
                servicios.repuestosAsociados(c, idServicio).forEach((idRepuesto, cantidad) -> repuestosPorId.merge(idRepuesto, cantidad, Integer::sum));
            }
            List<Object[]> consumos = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Servicio servicio : elegidos) {
                total = total.add(servicio.precioAplicable());
            }
            for (Map.Entry<Integer, Integer> entrada : repuestosPorId.entrySet()) {
                Repuesto repuesto = repuestos.buscar(c, entrada.getKey()).orElseThrow(() -> new NegocioException("Un repuesto asociado no existe."));
                if (!repuesto.activo()) {
                    throw new NegocioException("El repuesto " + repuesto.nombre() + " esta inactivo.");
                }
                consumos.add(new Object[]{repuesto.id(), entrada.getValue(), repuesto.precioVenta()});
                total = total.add(repuesto.precioVenta().multiply(BigDecimal.valueOf(entrada.getValue())));
            }

            String numero = siguienteNumero(c);
            OrdenTrabajo base = new OrdenTrabajo(0, numero, nueva.idCliente(), nueva.idVehiculo(), null, null, nueva.kilometraje(), nueva.sintomas().trim(),
                    null, nueva.idResponsable(), EstadoOrden.RECIBIDA, total.setScale(2, RoundingMode.HALF_UP), nueva.cambioMotor());
            int idOrden = ordenes.guardar(c, base);

            for (Servicio servicio : elegidos) {
                JdbcUtil.actualizar(c, "INSERT INTO DetalleOrden (IdOrden, IdServicio, PrecioAplicado, Horas, Subtotal) VALUES (?, ?, ?, ?, ?)",
                        idOrden, servicio.id(), servicio.precioAplicable(), servicio.horasPlanificadas(), servicio.precioAplicable());
                JdbcUtil.actualizar(c, "INSERT INTO ActividadesTrabajo (IdOrden, IdMecanico, Descripcion, Horas) VALUES (?, ?, ?, ?)",
                        idOrden, nueva.idResponsable(), servicio.nombre(), servicio.horasPlanificadas());
            }
            for (Object[] consumo : consumos) {
                JdbcUtil.actualizar(c, "INSERT INTO ConsumoRepuestoOrden (IdOrden, IdRepuesto, Cantidad, PrecioAplicado) VALUES (?, ?, ?, ?)",
                        idOrden, consumo[0], consumo[1], consumo[2]);
            }
            JdbcUtil.actualizar(c, "INSERT INTO OrdenMecanico (IdOrden, IdMecanico, EsResponsable) VALUES (?, ?, 1)", idOrden, nueva.idResponsable());
            JdbcUtil.actualizar(c, "UPDATE Mecanicos SET Disponible = 0 WHERE IdMecanico = ?", nueva.idResponsable());
            JdbcUtil.actualizar(c, "INSERT INTO ControlesCalidad (IdOrden, Descripcion, Obligatorio) VALUES (?, ?, 1)",
                    idOrden, "Prueba de funcionamiento general y revision final");
            registrarHistorial(c, idOrden, null, EstadoOrden.RECIBIDA, "Orden creada");
            JdbcUtil.actualizar(c, "UPDATE Vehiculos SET Kilometraje = ? WHERE IdVehiculo = ?", nueva.kilometraje(), nueva.idVehiculo());
            return idOrden;
        });
    }

    private void validarMecanicoLibre(Connection c, int idMecanico) throws SQLException {
        Optional<boolean[]> estado = JdbcUtil.unico(c, "SELECT Activo, Disponible FROM Mecanicos WITH (UPDLOCK, ROWLOCK) WHERE IdMecanico = ?",
                rs -> new boolean[]{rs.getBoolean(1), rs.getBoolean(2)}, idMecanico);
        boolean[] datos = estado.orElseThrow(() -> new NegocioException("El mecánico indicado no existe."));
        if (!datos[0]) {
            throw new NegocioException("No se puede asignar personal inactivo.");
        }
        if (!datos[1]) {
            throw new NegocioException("El mecánico no esta disponible en este momento.");
        }
    }

    private String siguienteNumero(Connection c) throws SQLException {
        int secuencia = JdbcUtil.unico(c, "SELECT NEXT VALUE FOR seq_NumeroOrden", rs -> rs.getInt(1)).orElse(1);
        return "OT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format("%05d", secuencia);
    }

    private void registrarHistorial(Connection c, int idOrden, EstadoOrden anterior, EstadoOrden nuevo, String observacion) throws SQLException {
        JdbcUtil.actualizar(c, "INSERT INTO HistorialEstadosOrden (IdOrden, EstadoAnterior, EstadoNuevo, Usuario, Observacion) VALUES (?, ?, ?, ?, ?)",
                idOrden, anterior == null ? null : anterior.texto(), nuevo.texto(), usuario, observacion);
    }

    public void cambiarEstado(int idOrden, EstadoOrden nuevo, String observacion) {
        Transaccion.ejecutar(c -> {
            cambiarEstado(c, idOrden, nuevo, observacion);
            return null;
        });
    }

    private void cambiarEstado(Connection c, int idOrden, EstadoOrden nuevo, String observacion) throws SQLException {
        OrdenTrabajo orden = ordenes.bloquear(c, idOrden).orElseThrow(() -> new NegocioException("La orden no existe."));
        EstadoOrden actual = orden.estado();
        if (!actual.siguientes().contains(nuevo)) {
            throw new NegocioException("No se permite cambiar la orden de '" + actual + "' a '" + nuevo + "'.");
        }
        switch (nuevo) {
            case PENDIENTE_APROBACION -> {
                if (!JdbcUtil.existe(c, "SELECT 1 FROM Diagnosticos WHERE IdOrden = ?", idOrden)) {
                    throw new NegocioException("Registre el diagnóstico técnico antes de solicitar la aprobación del cliente.");
                }
            }
            case EN_REPARACION -> {
                if (actual == EstadoOrden.PENDIENTE_APROBACION
                        && !JdbcUtil.existe(c, "SELECT 1 FROM Presupuestos WHERE IdOrden = ? AND Estado = N'Aprobado'", idOrden)) {
                    throw new NegocioException("El cliente debe aprobar el presupuesto antes de iniciar la reparación.");
                }
            }
            case EN_CONTROL_CALIDAD -> {
                if (orden.esCambioMotor() && !JdbcUtil.existe(c, "SELECT 1 FROM CambiosMotor WHERE IdOrden = ?", idOrden)) {
                    throw new NegocioException("Registre la sustitución de motor (pestaña 'Sustitución de motor') antes de pasar a control de calidad.");
                }
            }
            case FINALIZADA -> {
                List<String> pendientes = pendientesFinalizacion(c, idOrden);
                if (!pendientes.isEmpty()) {
                    throw new NegocioException("No se puede finalizar la orden:\n- " + String.join("\n- ", pendientes));
                }
            }
            default -> {
            }
        }
        JdbcUtil.actualizar(c, "UPDATE OrdenesTrabajo SET Estado = ?, FechaEntrega = CASE WHEN ? = N'Finalizada' THEN SYSDATETIME() ELSE FechaEntrega END WHERE IdOrden = ?",
                nuevo.texto(), nuevo.texto(), idOrden);
        registrarHistorial(c, idOrden, actual, nuevo, observacion);
        if (nuevo.esFinal()) {
            JdbcUtil.actualizar(c,
                    "UPDATE Mecanicos SET Disponible = 1 WHERE IdMecanico IN (SELECT IdMecanico FROM OrdenMecanico WHERE IdOrden = ?) "
                            + "AND NOT EXISTS (SELECT 1 FROM OrdenMecanico om INNER JOIN OrdenesTrabajo o ON o.IdOrden = om.IdOrden "
                            + "WHERE om.IdMecanico = Mecanicos.IdMecanico AND o.IdOrden <> ? AND o.Estado NOT IN (N'Finalizada', N'Rechazada', N'Cancelada'))",
                    idOrden, idOrden);
        }
    }

    public List<String> pendientesFinalizacion(int idOrden) {
        return Transaccion.consulta(c -> pendientesFinalizacion(c, idOrden));
    }

    private List<String> pendientesFinalizacion(Connection c, int idOrden) throws SQLException {
        List<String> pendientes = new ArrayList<>();
        int actividades = JdbcUtil.contar(c, "SELECT COUNT(*) FROM ActividadesTrabajo WHERE IdOrden = ? AND Estado IN (N'Pendiente', N'En proceso')", idOrden);
        if (actividades > 0) {
            pendientes.add("Hay " + actividades + " actividad(es) de trabajo pendientes.");
        }
        int consumos = JdbcUtil.contar(c, "SELECT COUNT(*) FROM ConsumoRepuestoOrden WHERE IdOrden = ? AND Registrado = 0", idOrden);
        if (consumos > 0) {
            pendientes.add("Hay " + consumos + " consumo(s) de repuestos sin registrar en el inventario.");
        }
        int controles = JdbcUtil.contar(c, "SELECT COUNT(*) FROM ControlesCalidad WHERE IdOrden = ? AND Obligatorio = 1 AND Aprobado = 0", idOrden);
        if (controles > 0) {
            pendientes.add("Hay " + controles + " control(es) de calidad obligatorios sin aprobar.");
        }
        return pendientes;
    }

    private void exigirOrdenAbierta(OrdenTrabajo orden) {
        if (orden.estado().esFinal()) {
            throw new NegocioException("La orden " + orden.numeroOrden() + " esta " + orden.estado() + " y ya no admite cambios.");
        }
    }

    private void recalcularTotal(Connection c, int idOrden) throws SQLException {
        JdbcUtil.actualizar(c,
                "UPDATE OrdenesTrabajo SET Total = (SELECT COALESCE(SUM(Subtotal), 0) FROM DetalleOrden WHERE IdOrden = ?) "
                        + "+ (SELECT COALESCE(SUM(Cantidad * PrecioAplicado), 0) FROM ConsumoRepuestoOrden WHERE IdOrden = ?) WHERE IdOrden = ?",
                idOrden, idOrden, idOrden);
    }

    public int agregarConsumo(int idOrden, int idRepuesto, int cantidad) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad debe ser mayor que cero.");
        }
        return Transaccion.ejecutar(c -> {
            OrdenTrabajo orden = ordenes.bloquear(c, idOrden).orElseThrow(() -> new NegocioException("La orden no existe."));
            exigirOrdenAbierta(orden);
            Repuesto repuesto = repuestos.buscar(c, idRepuesto).orElseThrow(() -> new NegocioException("El repuesto no existe."));
            if (!repuesto.activo()) {
                throw new NegocioException("El repuesto esta inactivo.");
            }
            int id = JdbcUtil.insertar(c, "INSERT INTO ConsumoRepuestoOrden (IdOrden, IdRepuesto, Cantidad, PrecioAplicado) VALUES (?, ?, ?, ?)",
                    idOrden, idRepuesto, cantidad, repuesto.precioVenta());
            recalcularTotal(c, idOrden);
            return id;
        });
    }

    public int registrarConsumosPendientes(int idOrden) {
        return Transaccion.ejecutar(c -> registrarPendientes(c, idOrden));
    }

    private int registrarPendientes(Connection c, int idOrden) throws SQLException {
        List<String> faltantes = JdbcUtil.lista(c,
                "SELECT r.Nombre + N': requiere ' + CAST(SUM(k.Cantidad) AS NVARCHAR(10)) + N', disponible ' + CAST(r.StockActual AS NVARCHAR(10)) "
                        + "FROM ConsumoRepuestoOrden k INNER JOIN Repuestos r ON r.IdRepuesto = k.IdRepuesto "
                        + "WHERE k.IdOrden = ? AND k.Registrado = 0 GROUP BY r.IdRepuesto, r.Nombre, r.StockActual HAVING SUM(k.Cantidad) > r.StockActual",
                rs -> rs.getString(1), idOrden);
        if (!faltantes.isEmpty()) {
            throw new NegocioException("Stock insuficiente:\n- " + String.join("\n- ", faltantes));
        }
        List<Integer> ids = JdbcUtil.lista(c, "SELECT IdConsumo FROM ConsumoRepuestoOrden WHERE IdOrden = ? AND Registrado = 0 ORDER BY IdConsumo",
                rs -> rs.getInt(1), idOrden);
        for (int idConsumo : ids) {
            try (CallableStatement cs = c.prepareCall("{call sp_RegistrarConsumoRepuesto(?, ?)}")) {
                cs.setInt(1, idConsumo);
                cs.setString(2, usuario);
                cs.execute();
            }
        }
        return ids.size();
    }

    public void guardarDiagnostico(int idOrden, int idMecanico, String evaluacion, boolean requiereCambioMotor) {
        if (evaluacion == null || evaluacion.isBlank()) {
            throw new NegocioException("Escriba la evaluación técnica del diagnóstico.");
        }
        Transaccion.ejecutar(c -> {
            OrdenTrabajo orden = ordenes.bloquear(c, idOrden).orElseThrow(() -> new NegocioException("La orden no existe."));
            exigirOrdenAbierta(orden);
            if (JdbcUtil.existe(c, "SELECT 1 FROM Diagnosticos WHERE IdOrden = ?", idOrden)) {
                JdbcUtil.actualizar(c, "UPDATE Diagnosticos SET IdMecanico = ?, EvaluacionTecnica = ?, RequiereCambioMotor = ?, Fecha = SYSDATETIME() WHERE IdOrden = ?",
                        idMecanico, evaluacion.trim(), requiereCambioMotor, idOrden);
            } else {
                JdbcUtil.actualizar(c, "INSERT INTO Diagnosticos (IdOrden, IdMecanico, EvaluacionTecnica, RequiereCambioMotor) VALUES (?, ?, ?, ?)",
                        idOrden, idMecanico, evaluacion.trim(), requiereCambioMotor);
            }
            return null;
        });
    }

    public void agregarEvidencia(int idOrden, String descripcion, String ruta) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new NegocioException("Describa la evidencia técnica.");
        }
        Transaccion.ejecutar(c -> {
            Integer idDiagnostico = JdbcUtil.unico(c, "SELECT IdDiagnostico FROM Diagnosticos WHERE IdOrden = ?", rs -> rs.getInt(1), idOrden)
                    .orElseThrow(() -> new NegocioException("Registre primero el diagnóstico de la orden."));
            JdbcUtil.actualizar(c, "INSERT INTO EvidenciasDiagnostico (IdDiagnostico, Descripcion, RutaArchivo) VALUES (?, ?, ?)",
                    idDiagnostico, descripcion.trim(), ruta == null || ruta.isBlank() ? null : ruta.trim());
            return null;
        });
    }

    public int generarPresupuesto(int idOrden, BigDecimal descuento, String autorizadoPor, Integer idImpuesto) {
        return Transaccion.ejecutar(c -> {
            OrdenTrabajo orden = ordenes.bloquear(c, idOrden).orElseThrow(() -> new NegocioException("La orden no existe."));
            if (!EnumSet.of(EstadoOrden.EN_DIAGNOSTICO, EstadoOrden.PENDIENTE_APROBACION).contains(orden.estado())) {
                throw new NegocioException("El presupuesto se genera cuando la orden esta en diagnóstico o pendiente de aprobación.");
            }
            BigDecimal servicios = JdbcUtil.unico(c, "SELECT COALESCE(SUM(Subtotal), 0) FROM DetalleOrden WHERE IdOrden = ?", rs -> rs.getBigDecimal(1), idOrden).orElse(BigDecimal.ZERO);
            BigDecimal piezas = JdbcUtil.unico(c, "SELECT COALESCE(SUM(Cantidad * PrecioAplicado), 0) FROM ConsumoRepuestoOrden WHERE IdOrden = ?", rs -> rs.getBigDecimal(1), idOrden).orElse(BigDecimal.ZERO);
            BigDecimal subtotal = servicios.add(piezas).setScale(2, RoundingMode.HALF_UP);
            BigDecimal rebaja = descuento == null ? BigDecimal.ZERO : descuento.setScale(2, RoundingMode.HALF_UP);
            if (rebaja.signum() < 0 || rebaja.compareTo(subtotal) > 0) {
                throw new NegocioException("El descuento debe estar entre 0 y el subtotal del presupuesto.");
            }
            String autorizado = autorizadoPor == null || autorizadoPor.isBlank() ? null : autorizadoPor.trim();
            if (rebaja.signum() > 0 && autorizado == null) {
                throw new NegocioException("Todo descuento requiere indicar quien lo autoriza.");
            }
            BigDecimal porcentaje = BigDecimal.ZERO;
            if (idImpuesto != null) {
                porcentaje = JdbcUtil.unico(c, "SELECT Porcentaje FROM ConfiguracionImpuestos WHERE IdImpuesto = ? AND Vigente = 1", rs -> rs.getBigDecimal(1), idImpuesto)
                        .orElseThrow(() -> new NegocioException("El impuesto seleccionado no existe o no esta vigente."));
            }
            BigDecimal impuesto = subtotal.subtract(rebaja).multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal total = subtotal.subtract(rebaja).add(impuesto);

            Optional<Object[]> existente = JdbcUtil.unico(c, "SELECT IdPresupuesto, Estado FROM Presupuestos WITH (UPDLOCK) WHERE IdOrden = ?",
                    rs -> new Object[]{rs.getInt(1), rs.getString(2)}, idOrden);
            int idPresupuesto;
            if (existente.isPresent()) {
                if (!"Pendiente".equals(existente.get()[1])) {
                    throw new NegocioException("El presupuesto de esta orden ya fue " + existente.get()[1] + ".");
                }
                idPresupuesto = (Integer) existente.get()[0];
                JdbcUtil.actualizar(c, "DELETE FROM DetallePresupuesto WHERE IdPresupuesto = ?", idPresupuesto);
                JdbcUtil.actualizar(c,
                        "UPDATE Presupuestos SET Fecha = SYSDATETIME(), Subtotal = ?, Descuento = ?, AutorizadoDescuentoPor = ?, IdImpuesto = ?, "
                                + "PorcentajeImpuesto = ?, Impuesto = ?, Total = ? WHERE IdPresupuesto = ?",
                        subtotal, rebaja, autorizado, idImpuesto, porcentaje, impuesto, total, idPresupuesto);
            } else {
                idPresupuesto = JdbcUtil.insertar(c,
                        "INSERT INTO Presupuestos (IdOrden, Subtotal, Descuento, AutorizadoDescuentoPor, IdImpuesto, PorcentajeImpuesto, Impuesto, Total) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        idOrden, subtotal, rebaja, autorizado, idImpuesto, porcentaje, impuesto, total);
            }
            JdbcUtil.actualizar(c,
                    "INSERT INTO DetallePresupuesto (IdPresupuesto, Tipo, IdServicio, Descripcion, Cantidad, PrecioUnitario, Subtotal) "
                            + "SELECT ?, N'Servicio', d.IdServicio, LEFT(s.Nombre, 200), 1, d.PrecioAplicado, d.Subtotal "
                            + "FROM DetalleOrden d INNER JOIN Servicios s ON s.IdServicio = d.IdServicio WHERE d.IdOrden = ?",
                    idPresupuesto, idOrden);
            JdbcUtil.actualizar(c,
                    "INSERT INTO DetallePresupuesto (IdPresupuesto, Tipo, IdRepuesto, Descripcion, Cantidad, PrecioUnitario, Subtotal) "
                            + "SELECT ?, N'Repuesto', k.IdRepuesto, LEFT(r.Nombre, 200), k.Cantidad, k.PrecioAplicado, k.Cantidad * k.PrecioAplicado "
                            + "FROM ConsumoRepuestoOrden k INNER JOIN Repuestos r ON r.IdRepuesto = k.IdRepuesto WHERE k.IdOrden = ?",
                    idPresupuesto, idOrden);
            return idPresupuesto;
        });
    }

    public void responderPresupuesto(int idOrden, boolean aprobado) {
        Transaccion.ejecutar(c -> {
            OrdenTrabajo orden = ordenes.bloquear(c, idOrden).orElseThrow(() -> new NegocioException("La orden no existe."));
            exigirOrdenAbierta(orden);
            int filas = JdbcUtil.actualizar(c, "UPDATE Presupuestos SET Estado = ?, FechaRespuesta = SYSDATETIME() WHERE IdOrden = ? AND Estado = N'Pendiente'",
                    aprobado ? "Aprobado" : "Rechazado", idOrden);
            if (filas == 0) {
                throw new NegocioException("No hay un presupuesto pendiente de respuesta para esta orden.");
            }
            if (orden.estado() == EstadoOrden.PENDIENTE_APROBACION) {
                cambiarEstado(c, idOrden, aprobado ? EstadoOrden.EN_REPARACION : EstadoOrden.RECHAZADA,
                        aprobado ? "Presupuesto aprobado por el cliente" : "Presupuesto rechazado por el cliente");
            }
            return null;
        });
    }

    public void completarActividad(int idActividad) {
        Transaccion.ejecutar(c -> {
            String estado = JdbcUtil.unico(c, "SELECT o.Estado FROM ActividadesTrabajo a INNER JOIN OrdenesTrabajo o ON o.IdOrden = a.IdOrden WHERE a.IdActividad = ?",
                    rs -> rs.getString(1), idActividad).orElseThrow(() -> new NegocioException("La actividad no existe."));
            if (EstadoOrden.desde(estado) != EstadoOrden.EN_REPARACION) {
                throw new NegocioException("Solo se pueden terminar actividades de órdenes que estan en reparación.");
            }
            int filas = JdbcUtil.actualizar(c,
                    "UPDATE ActividadesTrabajo SET Estado = N'Terminada', FechaInicio = COALESCE(FechaInicio, SYSDATETIME()), FechaFin = SYSDATETIME() "
                            + "WHERE IdActividad = ? AND Estado IN (N'Pendiente', N'En proceso')",
                    idActividad);
            if (filas == 0) {
                throw new NegocioException("La actividad ya estaba terminada o cancelada.");
            }
            return null;
        });
    }

    public void agregarControl(int idOrden, String descripcion, boolean obligatorio) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new NegocioException("Describa el control de calidad.");
        }
        Transaccion.ejecutar(c -> {
            OrdenTrabajo orden = ordenes.bloquear(c, idOrden).orElseThrow(() -> new NegocioException("La orden no existe."));
            exigirOrdenAbierta(orden);
            JdbcUtil.actualizar(c, "INSERT INTO ControlesCalidad (IdOrden, Descripcion, Obligatorio) VALUES (?, ?, ?)", idOrden, descripcion.trim(), obligatorio);
            return null;
        });
    }

    public void aprobarControl(int idControl, int idInspector, String observaciones) {
        Transaccion.ejecutar(c -> {
            String estado = JdbcUtil.unico(c, "SELECT o.Estado FROM ControlesCalidad q INNER JOIN OrdenesTrabajo o ON o.IdOrden = q.IdOrden WHERE q.IdControl = ?",
                    rs -> rs.getString(1), idControl).orElseThrow(() -> new NegocioException("El control de calidad no existe."));
            if (EstadoOrden.desde(estado) != EstadoOrden.EN_CONTROL_CALIDAD) {
                throw new NegocioException("Los controles solo se aprueban cuando la orden esta en control de calidad.");
            }
            if (!JdbcUtil.existe(c, "SELECT 1 FROM Mecanicos WHERE IdMecanico = ? AND Activo = 1", idInspector)) {
                throw new NegocioException("El inspector debe ser un mecánico activo.");
            }
            int filas = JdbcUtil.actualizar(c, "UPDATE ControlesCalidad SET Aprobado = 1, IdInspector = ?, Fecha = SYSDATETIME(), Observaciones = ? WHERE IdControl = ? AND Aprobado = 0",
                    idInspector, observaciones == null || observaciones.isBlank() ? null : observaciones.trim(), idControl);
            if (filas == 0) {
                throw new NegocioException("El control ya estaba aprobado.");
            }
            return null;
        });
    }

    public void asignarMecanico(int idOrden, int idMecanico, boolean responsable) {
        Transaccion.ejecutar(c -> {
            OrdenTrabajo orden = ordenes.bloquear(c, idOrden).orElseThrow(() -> new NegocioException("La orden no existe."));
            exigirOrdenAbierta(orden);
            validarMecanicoLibre(c, idMecanico);
            JdbcUtil.actualizar(c, "INSERT INTO OrdenMecanico (IdOrden, IdMecanico, EsResponsable) VALUES (?, ?, ?)", idOrden, idMecanico, responsable);
            JdbcUtil.actualizar(c, "UPDATE Mecanicos SET Disponible = 0 WHERE IdMecanico = ?", idMecanico);
            if (responsable) {
                JdbcUtil.actualizar(c, "UPDATE OrdenesTrabajo SET IdMecanicoResponsable = ? WHERE IdOrden = ?", idMecanico, idOrden);
            }
            return null;
        });
    }

    public Factura procesarYFacturar(int idOrden, Consumer<String> progreso) {
        return Transaccion.ejecutar(c -> {
            progreso.accept("Bloqueando y validando la orden...");
            OrdenTrabajo orden = ordenes.bloquear(c, idOrden).orElseThrow(() -> new NegocioException("La orden no existe."));
            if (orden.estado() == EstadoOrden.FINALIZADA) {
                progreso.accept("La orden ya está finalizada; se generará solo la factura...");
                Factura emitida = facturacion.generarFactura(c, idOrden);
                progreso.accept("Factura " + emitida.numeroFactura() + " emitida por un total de " + emitida.total() + ".");
                return emitida;
            }
            if (orden.estado() != EstadoOrden.EN_CONTROL_CALIDAD) {
                throw new NegocioException("Solo se puede facturar una orden en control de calidad o ya finalizada. Estado actual: " + orden.estado() + ".");
            }
            if (orden.esCambioMotor() && !JdbcUtil.existe(c, "SELECT 1 FROM CambiosMotor WHERE IdOrden = ?", idOrden)) {
                throw new NegocioException("La orden es de sustitución de motor y aún no se registro el cambio de motor.");
            }
            progreso.accept("Descontando del inventario los repuestos pendientes...");
            int registrados = registrarPendientes(c, idOrden);
            progreso.accept("Consumos registrados en inventario: " + registrados);
            progreso.accept("Verificando actividades, consumos y controles de calidad...");
            cambiarEstado(c, idOrden, EstadoOrden.FINALIZADA, "Orden procesada y entregada");
            if (orden.esCambioMotor()) {
                progreso.accept("Generando la garantia del motor instalado...");
                generarGarantia(c, orden);
            }
            progreso.accept("Generando la factura...");
            Factura factura = facturacion.generarFactura(c, idOrden);
            progreso.accept("Factura " + factura.numeroFactura() + " emitida por un total de " + factura.total() + ".");
            return factura;
        });
    }

    private void generarGarantia(Connection c, OrdenTrabajo orden) throws SQLException {
        Integer idMotor = JdbcUtil.unico(c, "SELECT IdMotorNuevo FROM CambiosMotor WHERE IdOrden = ?", rs -> rs.getInt(1), orden.id())
                .orElseThrow(() -> new NegocioException("No existe el registro del cambio de motor."));
        LocalDate hoy = LocalDate.now();
        JdbcUtil.actualizar(c, "INSERT INTO Garantias (IdOrden, IdMotor, FechaInicio, FechaFin, KilometrajeLimite, Cobertura) VALUES (?, ?, ?, ?, ?, ?)",
                orden.id(), idMotor, java.sql.Date.valueOf(hoy), java.sql.Date.valueOf(hoy.plusYears(1)), orden.kilometrajeIngreso() + 20000,
                "Garantia de 12 meses o 20000 km sobre el motor sustituido y su instalacion");
    }

    public Optional<OrdenTrabajo> obtener(int idOrden) {
        return ordenes.buscar(idOrden);
    }

}
