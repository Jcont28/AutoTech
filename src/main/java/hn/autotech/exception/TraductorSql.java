package hn.autotech.exception;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TraductorSql {
    private static final Pattern COLUMNA = Pattern.compile("column '([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALOR = Pattern.compile("duplicate key value is \\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESTRICCION = Pattern.compile("constraint \"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private TraductorSql() {
    }

    public static String mensaje(SQLException e) {
        String texto = e.getMessage() == null ? "Error de base de datos" : e.getMessage();
        switch (e.getErrorCode()) {
            case 2601, 2627 -> {
                Matcher m = VALOR.matcher(texto);
                return m.find() ? "Ya existe un registro con el valor (" + m.group(1) + ")." : "Ya existe un registro con esos datos.";
            }
            case 515 -> {
                Matcher m = COLUMNA.matcher(texto);
                return m.find() ? "El campo '" + m.group(1) + "' es obligatorio." : "Falta un dato obligatorio.";
            }
            case 8152, 2628 -> {
                Matcher m = COLUMNA.matcher(texto);
                return m.find() ? "El valor del campo '" + m.group(1) + "' excede la longitud permitida." : "Un texto excede la longitud permitida.";
            }
            case 547 -> {
                Matcher m = RESTRICCION.matcher(texto);
                String nombre = m.find() ? " (" + m.group(1) + ")" : "";
                return "La operacion viola una regla de integridad o una restriccion" + nombre + ".";
            }
            case 245, 8114, 241, 242 -> {
                return "Algun valor tiene un formato invalido.";
            }
            default -> {
                return texto;
            }
        }
    }
}
