package hn.autotech.model;

import java.util.List;

public record ResultadoConsulta(List<String> columnas, List<Object[]> filas) {
}
