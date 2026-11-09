package hn.autotech.service;

import hn.autotech.dao.JdbcUtil;
import hn.autotech.dao.Transaccion;
import hn.autotech.exception.NegocioException;
import hn.autotech.model.EstadoVehiculo;
import hn.autotech.model.ResultadoConsulta;

import java.util.ArrayList;
import java.util.List;

public class SeguimientoService {
    public List<EstadoVehiculo> vehiculosEnReparacion(int idCliente) {
        return Transaccion.consulta(c -> {
            List<Object[]> ordenes = JdbcUtil.tabla(c,
                    "SELECT o.IdOrden, o.NumeroOrden, v.Marca + N' ' + v.Modelo + N' ' + CAST(v.Anio AS NVARCHAR(10)), v.Placa, o.Estado, o.FechaIngreso, o.Sintomas "
                            + "FROM OrdenesTrabajo o INNER JOIN Vehiculos v ON v.IdVehiculo = o.IdVehiculo "
                            + "WHERE o.IdCliente = ? AND o.Estado NOT IN (N'Rechazada', N'Cancelada') ORDER BY o.FechaIngreso DESC", idCliente).filas();
            List<EstadoVehiculo> resultado = new ArrayList<>();
            for (Object[] f : ordenes) {
                int idOrden = (Integer) f[0];
                List<EstadoVehiculo.Comentario> comentarios = JdbcUtil.lista(c,
                        "SELECT Fecha, Autor, Comentario FROM ComentariosOrden WHERE IdOrden = ? ORDER BY Fecha DESC, IdComentario DESC",
                        rs -> new EstadoVehiculo.Comentario(rs.getTimestamp(1), rs.getString(2), rs.getString(3)), idOrden);
                resultado.add(new EstadoVehiculo(idOrden, (String) f[1], (String) f[2], (String) f[3], (String) f[4], (java.sql.Timestamp) f[5], (String) f[6], comentarios));
            }
            return resultado;
        });
    }

    public ResultadoConsulta comentarios(int idOrden) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c,
                "SELECT Fecha, Autor, Comentario FROM ComentariosOrden WHERE IdOrden = ? ORDER BY Fecha DESC, IdComentario DESC", idOrden));
    }

    public void agregarComentario(int idOrden, String texto, String autor) {
        if (texto == null || texto.isBlank()) {
            throw new NegocioException("Escriba el comentario para el cliente.");
        }
        if (texto.trim().length() > 500) {
            throw new NegocioException("El comentario no puede superar 500 caracteres.");
        }
        Transaccion.ejecutar(c -> {
            if (!JdbcUtil.existe(c, "SELECT 1 FROM OrdenesTrabajo WHERE IdOrden = ?", idOrden)) {
                throw new NegocioException("La orden seleccionada no existe.");
            }
            return JdbcUtil.actualizar(c, "INSERT INTO ComentariosOrden (IdOrden, Comentario, Autor) VALUES (?, ?, ?)", idOrden, texto.trim(), autor);
        });
    }

    public int idClientePorCedula(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            throw new NegocioException("Escriba el número de cédula del cliente.");
        }
        return Transaccion.consulta(c -> JdbcUtil.unico(c, "SELECT IdCliente FROM Clientes WHERE Identidad = ? AND Activo = 1", rs -> rs.getInt(1), cedula.trim()))
                .orElseThrow(() -> new NegocioException("No existe un cliente con la cédula " + cedula.trim() + ". El cliente debe registrarse primero."));
    }

    public String nombreCliente(int idCliente) {
        return Transaccion.consulta(c -> JdbcUtil.unico(c, "SELECT Nombre + N' ' + Apellido FROM Clientes WHERE IdCliente = ?", rs -> rs.getString(1), idCliente)).orElse("");
    }
}
