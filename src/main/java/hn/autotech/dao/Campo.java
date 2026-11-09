package hn.autotech.dao;

import java.util.List;

public record Campo(String nombre, String tipo, boolean generado, String relacion, List<String> lista) {
    public static Campo de(String[] definicion) {
        String tipo = definicion[1];
        String relacion = tipo.startsWith("FK:") ? tipo.substring(3).trim() : null;
        List<String> lista = tipo.startsWith("Lista:") ? List.of(tipo.substring(6).trim().split("\\|")) : List.of();
        return new Campo(definicion[0], tipo, definicion[2].equals("1"), relacion, lista);
    }

    public boolean esFk() {
        return relacion != null;
    }

    public boolean esLista() {
        return !lista.isEmpty();
    }

    public boolean esBooleano() {
        return tipo.equals("Sí/No");
    }

    public boolean esNumero() {
        return tipo.equals("Número");
    }

    public boolean esDecimal() {
        return tipo.equals("Decimal");
    }

    public boolean esFechaHora() {
        return tipo.startsWith("Fecha/hora");
    }

    public boolean esFecha() {
        return tipo.startsWith("Fecha") && !esFechaHora();
    }

    public boolean esTextoLargo() {
        return tipo.equals("Texto largo");
    }
}
