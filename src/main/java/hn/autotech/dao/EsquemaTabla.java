package hn.autotech.dao;

import java.util.List;

public record EsquemaTabla(String tabla, List<String> claves, boolean identidad, boolean versionada) {
    public static EsquemaTabla identidad(String tabla, String clave) {
        return new EsquemaTabla(tabla, List.of(clave), true, false);
    }

    public static EsquemaTabla compuesta(String tabla, String... claves) {
        return new EsquemaTabla(tabla, List.of(claves), false, false);
    }

    public EsquemaTabla conVersion() {
        return new EsquemaTabla(tabla, claves, identidad, true);
    }
}
