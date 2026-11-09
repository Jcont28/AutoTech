package hn.autotech.model;

public enum Rol {
    ADMINISTRADOR("Administrador"),
    CLIENTE("Cliente");

    private final String texto;

    Rol(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }

    public static Rol desde(String texto) {
        for (Rol rol : values()) {
            if (rol.texto.equalsIgnoreCase(texto)) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol desconocido: " + texto);
    }
}
