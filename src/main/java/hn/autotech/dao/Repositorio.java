package hn.autotech.dao;

import java.util.List;
import java.util.Optional;

public interface Repositorio<T> {
    List<T> listar();

    Optional<T> buscar(int id);

    int guardar(T entidad);

    void actualizar(T entidad);

    void eliminar(int id);
}
