package hn.autotech.service;

import hn.autotech.config.DBConnection;
import hn.autotech.dao.JdbcUtil;
import hn.autotech.dao.MotorDao;
import hn.autotech.dao.OrdenDao;
import hn.autotech.dao.RepuestoDao;
import hn.autotech.dao.Transaccion;
import hn.autotech.dao.VehiculoDao;
import hn.autotech.exception.NegocioException;
import hn.autotech.model.CambioMotorSolicitud;
import hn.autotech.model.EstadoOrden;
import hn.autotech.model.Motor;
import hn.autotech.model.OrdenTrabajo;
import hn.autotech.model.Repuesto;
import hn.autotech.model.Vehiculo;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MotorService {
    private final MotorDao motores = new MotorDao();
    private final OrdenDao ordenes = new OrdenDao();
    private final VehiculoDao vehiculos = new VehiculoDao();
    private final RepuestoDao repuestos = new RepuestoDao();

    public List<String> validarCompatibilidad(int idMotor, int idVehiculo) {
        return Transaccion.consulta(c -> problemasDeMotor(c, idMotor, idVehiculo));
    }

    private List<String> problemasDeMotor(Connection c, int idMotor, int idVehiculo) throws SQLException {
        List<String> problemas = new ArrayList<>();
        Optional<Motor> motor = motores.buscar(c, idMotor);
        Optional<Vehiculo> vehiculo = vehiculos.buscar(c, idVehiculo);
        if (motor.isEmpty()) {
            problemas.add("El motor seleccionado no existe.");
            return problemas;
        }
        if (vehiculo.isEmpty()) {
            problemas.add("El vehículo de la orden no existe.");
            return problemas;
        }
        Motor m = motor.get();
        Vehiculo v = vehiculo.get();
        if (!"Disponible".equals(m.estado())) {
            problemas.add("El motor " + m.numeroSerie() + " no esta disponible (estado actual: " + m.estado() + ").");
        }
        if (vacio(m.numeroSerie()) || vacio(m.marca()) || vacio(m.modelo()) || vacio(m.combustible())) {
            problemas.add("El motor no tiene todos los datos obligatorios (serie, marca, modelo y combustible).");
        }
        if (!motores.esCompatible(c, m.id(), v.marca(), v.modelo(), v.anio())) {
            problemas.add("El motor " + m.numeroSerie() + " (" + m.modelo() + ") es incompatible con " + v.marca() + " " + v.modelo() + " " + v.anio() + ".");
        }
        if (m.idRepuesto() != null) {
            Repuesto repuesto = repuestos.buscar(c, m.idRepuesto()).orElse(null);
            if (repuesto == null || !repuesto.activo() || repuesto.stockActual() < 1) {
                problemas.add("No hay existencia en inventario del repuesto asociado al motor.");
            }
        }
        return problemas;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public int registrarCambio(CambioMotorSolicitud s) {
        return Transaccion.ejecutar(c -> {
            OrdenTrabajo orden = ordenes.bloquear(c, s.idOrden()).orElseThrow(() -> new NegocioException("La orden no existe."));
            List<String> problemas = new ArrayList<>();
            if (!orden.esCambioMotor()) {
                problemas.add("La orden no esta marcada como sustitución de motor.");
            }
            if (orden.estado() != EstadoOrden.EN_REPARACION && orden.estado() != EstadoOrden.EN_CONTROL_CALIDAD) {
                problemas.add("La orden debe estar en reparación o en control de calidad (estado actual: " + orden.estado() + ").");
            }
            if (s.idVehiculo() != null && s.idVehiculo() != orden.idVehiculo()) {
                problemas.add("El vehículo indicado no corresponde al de la orden.");
            }
            if (JdbcUtil.existe(c, "SELECT 1 FROM CambiosMotor WHERE IdOrden = ?", orden.id())) {
                problemas.add("La orden ya tiene un cambio de motor registrado.");
            }
            if (!JdbcUtil.existe(c, "SELECT 1 FROM Diagnosticos WHERE IdOrden = ? AND RequiereCambioMotor = 1", orden.id())) {
                problemas.add("Falta el diagnóstico: vaya a '2. Diagnóstico', guárdelo y marque 'Requiere sustitución de motor'.");
            }
            if (!JdbcUtil.existe(c, "SELECT 1 FROM Presupuestos WHERE IdOrden = ? AND Estado = N'Aprobado'", orden.id())) {
                problemas.add("El cliente no ha aprobado la cotización: vaya a '3. Cotización'.");
            }
            if (!JdbcUtil.existe(c, "SELECT 1 FROM OrdenMecanico WHERE IdOrden = ?", orden.id())) {
                problemas.add("No hay mecánicos asignados: use la pestaña 'Mecánicos y actividades'.");
            }
            if (vacio(s.serieAnterior())) {
                problemas.add("Falta el número de serie del motor anterior.");
            } else if (s.serieAnterior().trim().length() > 50) {
                problemas.add("La serie del motor anterior excede 50 caracteres.");
            }
            if (vacio(s.destinoRetirado())) {
                problemas.add("Falta el destino del motor retirado.");
            } else if (s.destinoRetirado().trim().length() > 30) {
                problemas.add("El destino del motor retirado excede 30 caracteres.");
            }
            if (s.fechaDesmontaje() == null || s.fechaInstalacion() == null) {
                problemas.add("Registre las fechas de desmontaje e instalacion.");
            } else if (s.fechaInstalacion().isBefore(s.fechaDesmontaje())) {
                problemas.add("La instalacion no puede ser anterior al desmontaje.");
            }
            if (vacio(s.resultadoPruebas())) {
                problemas.add("Registre el resultado de las pruebas del motor nuevo.");
            }
            problemas.addAll(problemasDeMotor(c, s.idMotorNuevo(), orden.idVehiculo()));
            if (!problemas.isEmpty()) {
                throw new NegocioException(String.join("\n", problemas));
            }

            Motor motor = motores.buscar(c, s.idMotorNuevo()).orElseThrow();
            if (!vacio(s.serieNueva()) && !s.serieNueva().trim().equals(motor.numeroSerie())) {
                throw new NegocioException("La serie capturada del motor nuevo no coincide con el motor seleccionado (" + motor.numeroSerie() + ").");
            }
            int idCambio = JdbcUtil.insertar(c,
                    "INSERT INTO CambiosMotor (IdOrden, IdVehiculo, IdMotorNuevo, SerieMotorAnterior, SerieMotorNuevo, DestinoMotorRetirado, FechaDesmontaje, "
                            + "FechaInstalacion, ResultadoPruebas, PruebasAprobadas, Observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    orden.id(), orden.idVehiculo(), motor.id(), s.serieAnterior().trim(), motor.numeroSerie(), s.destinoRetirado().trim(),
                    Timestamp.valueOf(s.fechaDesmontaje()), Timestamp.valueOf(s.fechaInstalacion()), s.resultadoPruebas().trim(), s.pruebasAprobadas(),
                    vacio(s.observaciones()) ? null : s.observaciones().trim());
            int filas = JdbcUtil.actualizar(c, "UPDATE Motores SET Estado = N'Instalado', IdVehiculoActual = ? WHERE IdMotor = ? AND Estado = N'Disponible'",
                    orden.idVehiculo(), motor.id());
            if (filas == 0) {
                throw new NegocioException("El motor dejo de estar disponible mientras se registraba el cambio.");
            }
            JdbcUtil.actualizar(c, "UPDATE Vehiculos SET NumeroMotorActual = ? WHERE IdVehiculo = ?", motor.numeroSerie(), orden.idVehiculo());
            if (motor.idRepuesto() != null) {
                Repuesto repuesto = repuestos.buscar(c, motor.idRepuesto()).orElseThrow();
                int idConsumo = JdbcUtil.insertar(c, "INSERT INTO ConsumoRepuestoOrden (IdOrden, IdRepuesto, Cantidad, PrecioAplicado) VALUES (?, ?, 1, ?)",
                        orden.id(), repuesto.id(), repuesto.precioVenta());
                try (CallableStatement cs = c.prepareCall("{call sp_RegistrarConsumoRepuesto(?, ?)}")) {
                    cs.setInt(1, idConsumo);
                    cs.setString(2, DBConnection.usuario());
                    cs.execute();
                }
            }
            JdbcUtil.actualizar(c, "INSERT INTO ControlesCalidad (IdOrden, Descripcion, Obligatorio, Aprobado, Fecha, Observaciones) VALUES (?, ?, 1, ?, ?, ?)",
                    orden.id(), "Pruebas de funcionamiento del motor nuevo", s.pruebasAprobadas(),
                    s.pruebasAprobadas() ? Timestamp.valueOf(s.fechaInstalacion()) : null, s.resultadoPruebas().trim());
            JdbcUtil.actualizar(c,
                    "UPDATE OrdenesTrabajo SET Total = (SELECT COALESCE(SUM(Subtotal), 0) FROM DetalleOrden WHERE IdOrden = ?) "
                            + "+ (SELECT COALESCE(SUM(Cantidad * PrecioAplicado), 0) FROM ConsumoRepuestoOrden WHERE IdOrden = ?) WHERE IdOrden = ?",
                    orden.id(), orden.id(), orden.id());
            return idCambio;
        });
    }
}
