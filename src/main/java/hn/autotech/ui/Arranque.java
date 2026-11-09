package hn.autotech.ui;

public final class Arranque {
    private Arranque() {
    }

    public static void iniciar() {
        Estilo.instalar();
        new FrmLogin().setVisible(true);
    }
}
