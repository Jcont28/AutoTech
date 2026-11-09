package hn.autotech.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Etiquetas {
    private static final Map<String, String> PALABRAS = new LinkedHashMap<>();
    private static final Pattern PALABRA = Pattern.compile("\\p{L}+");

    static {
        String[] pares = {
            "vehiculo", "vehículo", "vehiculos", "vehículos", "ordenes", "órdenes", "mecanico", "mecánico", "mecanicos", "mecánicos",
            "diagnostico", "diagnóstico", "diagnosticos", "diagnósticos", "cedula", "cédula", "telefono", "teléfono", "contrasena", "contraseña",
            "contrasenas", "contraseñas", "sesion", "sesión", "anio", "año", "descripcion", "descripción", "direccion", "dirección",
            "categoria", "categoría", "categorias", "categorías", "sustitucion", "sustitución", "numero", "número", "maximo", "máximo",
            "minimo", "mínimo", "aprobacion", "aprobación", "finalizacion", "finalización", "recepcion", "recepción", "reparacion", "reparación",
            "tecnica", "técnica", "tecnico", "técnico", "evaluacion", "evaluación", "atencion", "atención", "catalogo", "catálogo",
            "catalogos", "catálogos", "sintomas", "síntomas", "gestion", "gestión", "metodo", "método", "garantia", "garantía",
            "garantias", "garantías", "codigo", "código", "inspeccion", "inspección", "pais", "país", "aun", "aún", "ultimos", "últimos",
            "ultimo", "último", "asignacion", "asignación", "informacion", "información", "autorizacion", "autorización", "facturacion", "facturación",
            "operacion", "operación", "observacion", "observación", "observaciones", "observaciones", "fecha", "fecha", "especificacion", "especificación",
            "conexion", "conexión", "version", "versión", "accion", "acción", "validacion", "validación", "instalacion", "instalación", "desinstalacion", "desinstalación", "compatibilidad", "compatibilidad",
            "duracion", "duración", "electronico", "electrónico", "proxima", "próxima", "serie", "serie", "ingresos", "ingresos",
            "dias", "días", "tamano", "tamaño", "cantidad", "cantidad", "valido", "válido", "invalido", "inválido", "esta", "está",
            "mas", "más", "que", "qué", "como", "cómo", "tambien", "también", "ademas", "además"
        };
        for (int i = 0; i < pares.length; i += 2) {
            PALABRAS.put(pares[i], pares[i + 1]);
        }
    }

    private Etiquetas() {
    }

    static String acentuar(String texto) {
        if (texto == null) {
            return null;
        }
        Matcher m = PALABRA.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String palabra = m.group();
            String clave = palabra.toLowerCase();
            String cambio = PALABRAS.get(clave);
            if (cambio == null || clave.equals("esta") || clave.equals("mas") || clave.equals("que") || clave.equals("como")) {
                m.appendReplacement(sb, Matcher.quoteReplacement(palabra));
            } else {
                String resultado = Character.isUpperCase(palabra.charAt(0)) ? Character.toUpperCase(cambio.charAt(0)) + cambio.substring(1) : cambio;
                m.appendReplacement(sb, Matcher.quoteReplacement(resultado));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String titulo(String tabla) {
        return switch (tabla) {
            case "ServicioRepuesto" -> "Repuestos por servicio";
            case "ConfiguracionImpuestos" -> "Impuestos configurables";
            case "MecanicoEspecialidad" -> "Especialidades por mecánico";
            case "MovimientosInventario" -> "Entradas y salidas de inventario";
            case "CompatibilidadMotor" -> "Compatibilidad de motores";
            case "CategoriasRepuesto" -> "Categorías de repuestos";
            case "Mecanicos" -> "Mecánicos";
            default -> campo(tabla);
        };
    }

    static String campo(String nombre) {
        if (nombre.equals("Identidad")) {
            return "Cédula";
        }
        String base = nombre.startsWith("Id") && nombre.length() > 2 && Character.isUpperCase(nombre.charAt(2)) ? nombre.substring(2) : nombre;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(base.charAt(i - 1))) {
                sb.append(' ').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return acentuar(sb.toString());
    }
}
