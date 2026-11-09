package hn.autotech.ui;

import hn.autotech.exception.NegocioException;

import javax.swing.JOptionPane;
import java.awt.Component;

public final class Mensajes {
    private static Component principal;

    private Mensajes() {
    }

    static void principal(Component ventana) {
        principal = ventana;
    }

    private static Component padre(Component candidato) {
        if (candidato != null && candidato.isShowing()) {
            return candidato;
        }
        return principal != null && principal.isShowing() ? principal : null;
    }

    public static void info(Component padre, String texto) {
        JOptionPane.showMessageDialog(padre(padre), texto, "AutoTech", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void advertencia(Component padre, String texto) {
        JOptionPane.showMessageDialog(padre(padre), texto, "AutoTech", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component padre, Throwable error) {
        if (error instanceof NegocioException) {
            advertencia(padre, error.getMessage());
            return;
        }
        String texto = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        JOptionPane.showMessageDialog(padre(padre), texto, "AutoTech - Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component padre, String pregunta) {
        return JOptionPane.showConfirmDialog(padre(padre), pregunta, "AutoTech", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
