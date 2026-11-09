package hn.autotech.ui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Collections;

final class Estilo {
    static final Color FONDO = new Color(196, 207, 222);
    static final Color TEXTO = new Color(18, 28, 40);
    static final Color PRIMARIO = new Color(10, 30, 52);
    static final Color VERDE = new Color(18, 112, 68);
    static final Color AZUL = new Color(20, 80, 148);
    static final Color ROJO = new Color(170, 38, 38);
    static final Color GRIS = new Color(62, 76, 96);
    static final Color BORDE = new Color(120, 138, 160);

    private Estilo() {
    }

    static void instalar() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception e) {
            UIManager.getLookAndFeelDefaults();
        }
        for (Object clave : Collections.list(UIManager.getDefaults().keys())) {
            if (clave instanceof String texto && texto.endsWith(".font")) {
                Font actual = UIManager.getFont(texto);
                if (actual != null) {
                    UIManager.put(texto, new FontUIResource(new Font("Segoe UI", Font.PLAIN, 13)));
                }
            }
        }
        Color blanco = Color.WHITE;
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.background", FONDO);
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("CheckBox.foreground", TEXTO);
        UIManager.put("CheckBox.background", blanco);
        UIManager.put("RadioButton.foreground", TEXTO);
        UIManager.put("TextField.background", blanco);
        UIManager.put("TextField.foreground", TEXTO);
        UIManager.put("PasswordField.background", blanco);
        UIManager.put("PasswordField.foreground", TEXTO);
        UIManager.put("TextArea.background", blanco);
        UIManager.put("TextArea.foreground", TEXTO);
        UIManager.put("FormattedTextField.background", blanco);
        UIManager.put("FormattedTextField.foreground", TEXTO);
        UIManager.put("Spinner.background", blanco);
        UIManager.put("Spinner.foreground", TEXTO);
        UIManager.put("ComboBox.background", blanco);
        UIManager.put("ComboBox.foreground", TEXTO);
        UIManager.put("ComboBox.selectionBackground", AZUL);
        UIManager.put("ComboBox.selectionForeground", blanco);
        UIManager.put("List.background", blanco);
        UIManager.put("List.foreground", TEXTO);
        UIManager.put("List.selectionBackground", AZUL);
        UIManager.put("List.selectionForeground", blanco);
        UIManager.put("Table.background", blanco);
        UIManager.put("Table.foreground", TEXTO);
        UIManager.put("Table.gridColor", new Color(160, 174, 192));
        UIManager.put("Table.selectionBackground", AZUL);
        UIManager.put("Table.selectionForeground", blanco);
        UIManager.put("TableHeader.background", PRIMARIO);
        UIManager.put("TableHeader.foreground", blanco);
        UIManager.put("TabbedPane.background", new Color(170, 184, 202));
        UIManager.put("TabbedPane.foreground", TEXTO);
        UIManager.put("TabbedPane.selected", blanco);
        UIManager.put("TabbedPane.contentAreaColor", FONDO);
        UIManager.put("TabbedPane.borderHightlightColor", BORDE);
        UIManager.put("ScrollPane.background", FONDO);
        UIManager.put("Viewport.background", FONDO);
        UIManager.put("ProgressBar.foreground", VERDE);
        UIManager.put("ProgressBar.selectionForeground", blanco);
        UIManager.put("ProgressBar.selectionBackground", TEXTO);
        UIManager.put("ToolTip.background", new Color(255, 244, 200));
        UIManager.put("ToolTip.foreground", TEXTO);
    }

    static JButton boton(String texto, Color color, Runnable accion) {
        return boton(texto, color, null, accion);
    }

    static JButton boton(String texto, Color color, Icon icono, Runnable accion) {
        JButton boton = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = isEnabled() ? color : new Color(140, 150, 165);
                if (isEnabled() && getModel().isPressed()) {
                    base = color.darker().darker();
                } else if (isEnabled() && getModel().isRollover()) {
                    base = color.brighter();
                }
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        boton.setUI(new BasicButtonUI());
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setRolloverEnabled(true);
        boton.setForeground(Color.WHITE);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        boton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (icono != null) {
            boton.setIcon(icono);
            boton.setIconTextGap(8);
        }
        boton.addActionListener(e -> accion.run());
        return boton;
    }

    static JLabel titulo(String texto) {
        JLabel etiqueta = new JLabel(Etiquetas.acentuar(texto));
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 22));
        etiqueta.setForeground(PRIMARIO);
        return etiqueta;
    }

    static JPanel fila(JComponent... componentes) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)) {
            @Override
            public java.awt.Dimension getMaximumSize() {
                return new java.awt.Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        panel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);
        for (JComponent componente : componentes) {
            panel.add(componente);
        }
        return panel;
    }

    static JScrollPane tabla(JTable tabla) {
        tabla.setRowHeight(26);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane panel = new JScrollPane(tabla);
        panel.setBorder(BorderFactory.createLineBorder(BORDE));
        panel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return panel;
    }

    static JPanel tarjeta() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDE), BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        return panel;
    }
}
