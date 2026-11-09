package hn.autotech.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

final class Iconos {
    enum Tipo { ENGRANAJE, LLAVE, AUTO, CAJA, PERSONA, DOCUMENTO, GRAFICO, CANDADO, DINERO }

    private Iconos() {
    }

    static Icon de(Tipo tipo, int tamano, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                g2.scale(tamano / 100.0, tamano / 100.0);
                g2.setColor(color);
                dibujar(g2, tipo);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return tamano;
            }

            @Override
            public int getIconHeight() {
                return tamano;
            }
        };
    }

    private static void dibujar(Graphics2D g, Tipo tipo) {
        switch (tipo) {
            case ENGRANAJE -> engranaje(g);
            case LLAVE -> llave(g);
            case AUTO -> auto(g);
            case CAJA -> caja(g);
            case PERSONA -> persona(g);
            case DOCUMENTO -> documento(g);
            case GRAFICO -> grafico(g);
            case CANDADO -> candado(g);
            case DINERO -> dinero(g);
        }
    }

    private static void engranaje(Graphics2D g) {
        Area area = new Area(new Ellipse2D.Double(20, 20, 60, 60));
        for (int i = 0; i < 8; i++) {
            Area diente = new Area(new RoundRectangle2D.Double(41, 4, 18, 24, 4, 4));
            diente.transform(java.awt.geom.AffineTransform.getRotateInstance(Math.PI / 4 * i, 50, 50));
            area.add(diente);
        }
        area.subtract(new Area(new Ellipse2D.Double(36, 36, 28, 28)));
        g.fill(area);
    }

    private static void llave(Graphics2D g) {
        Area area = new Area(new Ellipse2D.Double(8, 8, 44, 44));
        area.subtract(new Area(new Ellipse2D.Double(21, 21, 18, 18)));
        Path2D.Double boca = new Path2D.Double();
        boca.moveTo(30, 30);
        boca.lineTo(14, 4);
        boca.lineTo(4, 14);
        boca.closePath();
        area.subtract(new Area(boca));
        Area mango = new Area(new RoundRectangle2D.Double(38, 36, 62, 20, 10, 10));
        mango.transform(java.awt.geom.AffineTransform.getRotateInstance(Math.PI / 4, 38, 36));
        mango.transform(java.awt.geom.AffineTransform.getTranslateInstance(-2, -2));
        area.add(mango);
        g.fill(area);
    }

    private static void auto(Graphics2D g) {
        Path2D.Double carroceria = new Path2D.Double();
        carroceria.moveTo(6, 64);
        carroceria.lineTo(6, 50);
        carroceria.quadTo(6, 44, 14, 42);
        carroceria.lineTo(28, 38);
        carroceria.lineTo(38, 22);
        carroceria.lineTo(66, 22);
        carroceria.lineTo(78, 38);
        carroceria.lineTo(90, 42);
        carroceria.quadTo(96, 44, 96, 52);
        carroceria.lineTo(96, 64);
        carroceria.closePath();
        Area area = new Area(carroceria);
        Path2D.Double ventana = new Path2D.Double();
        ventana.moveTo(36, 38);
        ventana.lineTo(43, 27);
        ventana.lineTo(60, 27);
        ventana.lineTo(60, 38);
        ventana.closePath();
        area.subtract(new Area(ventana));
        Path2D.Double ventana2 = new Path2D.Double();
        ventana2.moveTo(65, 27);
        ventana2.lineTo(72, 38);
        ventana2.lineTo(65, 38);
        ventana2.closePath();
        area.subtract(new Area(ventana2));
        g.fill(area);
        Area llantas = new Area(new Ellipse2D.Double(18, 54, 26, 26));
        llantas.add(new Area(new Ellipse2D.Double(60, 54, 26, 26)));
        g.fill(llantas);
    }

    private static void caja(Graphics2D g) {
        Area area = new Area(new RoundRectangle2D.Double(10, 30, 80, 58, 8, 8));
        area.add(new Area(new RoundRectangle2D.Double(6, 14, 88, 22, 6, 6)));
        area.subtract(new Area(new RoundRectangle2D.Double(38, 44, 24, 8, 4, 4)));
        g.fill(area);
    }

    private static void persona(Graphics2D g) {
        g.fill(new Ellipse2D.Double(32, 8, 36, 36));
        Path2D.Double cuerpo = new Path2D.Double();
        cuerpo.moveTo(12, 92);
        cuerpo.curveTo(12, 56, 88, 56, 88, 92);
        cuerpo.closePath();
        g.fill(cuerpo);
    }

    private static void documento(Graphics2D g) {
        Area area = new Area(new RoundRectangle2D.Double(18, 6, 64, 88, 10, 10));
        for (int i = 0; i < 4; i++) {
            area.subtract(new Area(new RoundRectangle2D.Double(30, 26 + i * 16, 40, 7, 4, 4)));
        }
        g.fill(area);
    }

    private static void grafico(Graphics2D g) {
        g.fill(new RoundRectangle2D.Double(10, 52, 18, 40, 4, 4));
        g.fill(new RoundRectangle2D.Double(41, 30, 18, 62, 4, 4));
        g.fill(new RoundRectangle2D.Double(72, 8, 18, 84, 4, 4));
    }

    private static void candado(Graphics2D g) {
        g.setStroke(new BasicStroke(10f));
        g.draw(new java.awt.geom.Arc2D.Double(28, 8, 44, 52, 0, 180, java.awt.geom.Arc2D.OPEN));
        g.fill(new RoundRectangle2D.Double(16, 44, 68, 48, 10, 10));
    }

    private static void dinero(Graphics2D g) {
        Area area = new Area(new Ellipse2D.Double(6, 6, 88, 88));
        area.subtract(new Area(new Ellipse2D.Double(16, 16, 68, 68)));
        area.add(new Area(new RoundRectangle2D.Double(44, 26, 12, 48, 6, 6)));
        area.add(new Area(new RoundRectangle2D.Double(34, 34, 32, 8, 4, 4)));
        area.add(new Area(new RoundRectangle2D.Double(34, 58, 32, 8, 4, 4)));
        g.fill(area);
    }
}
