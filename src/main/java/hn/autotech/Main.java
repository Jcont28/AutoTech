package hn.autotech;

import hn.autotech.ui.Arranque;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Arranque::iniciar);
    }
}
