package hn.autotech.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;

final class Banner extends JPanel {
    private final boolean vertical;

    Banner(String titulo, String subtitulo, boolean vertical) {
        this.vertical = vertical;
        setLayout(new BorderLayout(14, 0));
        setBorder(new EmptyBorder(vertical ? 30 : 12, 22, vertical ? 30 : 12, 22));
        setOpaque(false);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));
        JLabel t = new JLabel(titulo);
        t.setFont(new Font("Segoe UI", Font.BOLD, vertical ? 34 : 26));
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel(subtitulo);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        s.setForeground(new Color(255, 214, 102));
        textos.add(t);
        textos.add(s);
        JLabel logo = new JLabel(Iconos.de(Iconos.Tipo.ENGRANAJE, vertical ? 84 : 52, new Color(255, 196, 45)));
        if (vertical) {
            JPanel arriba = new JPanel(new BorderLayout(0, 14));
            arriba.setOpaque(false);
            arriba.add(logo, BorderLayout.NORTH);
            arriba.add(textos, BorderLayout.CENTER);
            add(arriba, BorderLayout.NORTH);
            JPanel fila = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 18, 0));
            fila.setOpaque(false);
            fila.add(new JLabel(Iconos.de(Iconos.Tipo.LLAVE, 64, new Color(255, 255, 255, 220))));
            fila.add(new JLabel(Iconos.de(Iconos.Tipo.AUTO, 80, new Color(255, 255, 255, 220))));
            add(fila, BorderLayout.SOUTH);
        } else {
            add(logo, BorderLayout.WEST);
            add(textos, BorderLayout.CENTER);
        }
    }

    void agregarDerecha(javax.swing.JComponent componente) {
        add(componente, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(vertical
                ? new GradientPaint(0, 0, new Color(10, 24, 40), 0, getHeight(), new Color(28, 70, 110))
                : new GradientPaint(0, 0, new Color(10, 24, 40), getWidth(), 0, new Color(28, 70, 110)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(255, 196, 45));
        g2.fillRect(0, getHeight() - 4, getWidth(), 4);
        g2.dispose();
        super.paintComponent(g);
    }
}
