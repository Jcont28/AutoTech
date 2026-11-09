package hn.autotech.model;

public record Sesion(int idUsuario, String usuario, Rol rol, Integer idCliente, String nombre) {
}
