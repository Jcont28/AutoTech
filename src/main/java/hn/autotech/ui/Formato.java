package hn.autotech.ui;

import hn.autotech.model.ResultadoConsulta;

import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public final class Formato {
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Formato() {
    }

    public static String valor(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Boolean booleano) {
            return booleano ? "Si" : "No";
        }
        if (valor instanceof Timestamp marca) {
            return marca.toLocalDateTime().format(FECHA_HORA);
        }
        if (valor instanceof Date fecha) {
            return fecha.toLocalDate().toString();
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return valor.toString();
    }

    public static String entrada(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Boolean || valor instanceof Timestamp || valor instanceof Date || valor instanceof BigDecimal) {
            return valor(valor);
        }
        return valor.toString();
    }

    public static DefaultTableModel modelo(ResultadoConsulta resultado) {
        DefaultTableModel modelo = new DefaultTableModel(resultado.columnas().toArray(), 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        for (Object[] fila : resultado.filas()) {
            Object[] texto = new Object[fila.length];
            for (int i = 0; i < fila.length; i++) {
                texto[i] = valor(fila[i]);
            }
            modelo.addRow(texto);
        }
        return modelo;
    }
}
