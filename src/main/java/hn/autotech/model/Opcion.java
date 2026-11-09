package hn.autotech.model;

public record Opcion(Integer id, String texto) {
    public static final Opcion VACIA = new Opcion(null, "");

    @Override
    public String toString() {
        return texto;
    }
}
