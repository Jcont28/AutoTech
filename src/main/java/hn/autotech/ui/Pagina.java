package hn.autotech.ui;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.function.Supplier;

final class Pagina {
    private Pagina() {
    }

    static JComponent de(Supplier<JFrame> fabrica) {
        JFrame ventana = fabrica.get();
        JComponent contenido = (JComponent) ventana.getContentPane();
        ventana.setContentPane(new JPanel());
        contenido.putClientProperty("ventana", ventana);
        return contenido;
    }

    @SafeVarargs
    static JComponent pestanas(String[] titulos, Supplier<JFrame>... fabricas) {
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JPanel[] contenedores = new JPanel[fabricas.length];
        boolean[] creadas = new boolean[fabricas.length];
        for (int i = 0; i < fabricas.length; i++) {
            contenedores[i] = new JPanel(new BorderLayout());
            pestanas.addTab(titulos[i], contenedores[i]);
        }
        Runnable cargar = () -> {
            int i = pestanas.getSelectedIndex();
            if (i >= 0 && !creadas[i]) {
                creadas[i] = true;
                contenedores[i].add(de(fabricas[i]), BorderLayout.CENTER);
                contenedores[i].revalidate();
            }
        };
        pestanas.addChangeListener(e -> cargar.run());
        cargar.run();
        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.add(pestanas, BorderLayout.CENTER);
        return envoltorio;
    }
}
