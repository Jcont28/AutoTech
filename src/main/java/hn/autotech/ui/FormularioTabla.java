package hn.autotech.ui;

import hn.autotech.dao.Campo;
import hn.autotech.dao.EsquemaTabla;
import hn.autotech.dao.OpcionesDao;
import hn.autotech.dao.Registro;
import hn.autotech.dao.TablaDao;
import hn.autotech.exception.NegocioException;
import hn.autotech.model.Opcion;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FormularioTabla extends JFrame {
    private final EsquemaTabla esquema;
    private final List<Campo> campos = new ArrayList<>();
    private final TablaDao dao;
    private final Map<String, JComponent> entradas = new LinkedHashMap<>();
    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JLabel estado = new JLabel(" ");
    private List<Registro> registros = new ArrayList<>();
    private Registro seleccionado;

    private record Datos(Map<String, List<Opcion>> opciones, List<Registro> registros) {
    }

    public FormularioTabla(String titulo, EsquemaTabla esquema, String[][] definiciones) {
        this.esquema = esquema;
        for (String[] definicion : definiciones) {
            campos.add(Campo.de(definicion));
        }
        this.dao = new TablaDao(esquema, campos);
        setTitle("AutoTech | " + titulo);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 740);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout(14, 14));
        raiz.setBorder(new EmptyBorder(20, 22, 20, 22));
        raiz.setBackground(Estilo.FONDO);
        JLabel encabezado = new JLabel(Etiquetas.titulo(titulo));
        encabezado.setFont(new Font("Segoe UI", Font.BOLD, 25));
        encabezado.setForeground(Estilo.PRIMARIO);
        raiz.add(encabezado, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(10, 10));
        centro.setOpaque(false);
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Estilo.BORDE), new EmptyBorder(16, 16, 16, 16)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        int columna = 0;
        int fila = 0;
        for (Campo campo : campos) {
            c.gridx = columna * 2;
            c.gridy = fila;
            c.weightx = 0;
            JLabel etiqueta = new JLabel(Etiquetas.campo(campo.nombre()));
            etiqueta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            formulario.add(etiqueta, c);
            c.gridx = columna * 2 + 1;
            c.weightx = 1;
            formulario.add(crearEntrada(campo), c);
            columna++;
            if (columna == 2) {
                columna = 0;
                fila++;
            }
        }
        JScrollPane desplazamientoFormulario = new JScrollPane(formulario);
        desplazamientoFormulario.setBorder(null);
        desplazamientoFormulario.setPreferredSize(new java.awt.Dimension(900, Math.min(300, 60 + 42 * ((campos.size() + 1) / 2))));
        centro.add(desplazamientoFormulario, BorderLayout.NORTH);

        List<String> encabezados = new ArrayList<>();
        if (esquema.identidad()) {
            encabezados.add("ID");
        }
        for (Campo campo : campos) {
            encabezados.add(Etiquetas.campo(campo.nombre()));
        }
        modelo = new DefaultTableModel(encabezados.toArray(), 0) {
            @Override
            public boolean isCellEditable(int f, int col) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(25);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                alSeleccionar();
            }
        });
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 4));
        acciones.setOpaque(false);
        agregarBoton(acciones, "Nuevo", Estilo.AZUL, this::limpiar);
        agregarBoton(acciones, "Guardar", Estilo.VERDE, this::guardar);
        agregarBoton(acciones, "Actualizar", Estilo.AZUL, this::actualizarSeleccionado);
        agregarBoton(acciones, "Eliminar", Estilo.ROJO, this::eliminarSeleccionado);
        agregarBoton(acciones, "Limpiar", Estilo.GRIS, this::limpiar);
        agregarBoton(acciones, "Refrescar", Estilo.GRIS, this::cargar);
        pie.add(acciones, BorderLayout.WEST);
        estado.setForeground(Estilo.TEXTO);
        pie.add(estado, BorderLayout.EAST);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);
        limpiar();
        cargar();
    }

    private JComponent crearEntrada(Campo campo) {
        JComponent entrada;
        if (campo.esLista()) {
            entrada = new JComboBox<>(campo.lista().toArray(new String[0]));
        } else if (campo.esFk()) {
            JComboBox<Opcion> combo = new JComboBox<>();
            combo.addItem(Opcion.VACIA);
            entrada = combo;
        } else if (campo.esBooleano()) {
            entrada = new JCheckBox();
        } else if (campo.esTextoLargo()) {
            entrada = new JScrollPane(new JTextArea(2, 16));
        } else {
            JTextField texto = new JTextField(16);
            if (campo.esFechaHora()) {
                texto.setToolTipText("Formato: aaaa-mm-dd hh:mm");
            } else if (campo.esFecha()) {
                texto.setToolTipText("Formato: aaaa-mm-dd");
            }
            entrada = texto;
        }
        entradas.put(campo.nombre(), entrada);
        return entrada;
    }

    private void agregarBoton(JPanel panel, String texto, Color color, Runnable accion) {
        panel.add(Estilo.boton(texto, color, accion));
    }

    private void cargar() {
        estado.setText("Cargando...");
        Set<String> relaciones = new LinkedHashSet<>();
        for (Campo campo : campos) {
            if (campo.esFk()) {
                relaciones.add(campo.relacion());
            }
        }
        Tareas.ejecutar(this, () -> {
            Map<String, List<Opcion>> opciones = new LinkedHashMap<>();
            for (String relacion : relaciones) {
                opciones.put(relacion, OpcionesDao.relacion(relacion));
            }
            return new Datos(opciones, dao.listar());
        }, this::mostrar);
    }

    private void mostrar(Datos datos) {
        for (Campo campo : campos) {
            if (campo.esFk()) {
                @SuppressWarnings("unchecked")
                JComboBox<Opcion> combo = (JComboBox<Opcion>) entradas.get(campo.nombre());
                Opcion actual = (Opcion) combo.getSelectedItem();
                combo.removeAllItems();
                combo.addItem(Opcion.VACIA);
                for (Opcion opcion : datos.opciones().get(campo.relacion())) {
                    combo.addItem(opcion);
                }
                seleccionarOpcion(combo, actual == null ? null : actual.id());
            }
        }
        registros = datos.registros();
        modelo.setRowCount(0);
        for (Registro registro : registros) {
            List<Object> fila = new ArrayList<>();
            if (esquema.identidad()) {
                fila.add(Formato.valor(registro.claves()[0]));
            }
            for (Object valor : registro.valores()) {
                fila.add(Formato.valor(valor));
            }
            modelo.addRow(fila.toArray());
        }
        estado.setText(registros.size() + " registro(s)");
        seleccionado = null;
    }

    private void alSeleccionar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        seleccionado = registros.get(tabla.convertRowIndexToModel(fila));
        for (int i = 0; i < campos.size(); i++) {
            colocar(campos.get(i), seleccionado.valores()[i]);
            if (esquema.claves().contains(campos.get(i).nombre()) && !esquema.identidad()) {
                entradas.get(campos.get(i).nombre()).setEnabled(false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void colocar(Campo campo, Object valor) {
        JComponent entrada = entradas.get(campo.nombre());
        if (entrada instanceof JCheckBox casilla) {
            casilla.setSelected(Boolean.TRUE.equals(valor));
        } else if (campo.esLista()) {
            ((JComboBox<String>) entrada).setSelectedItem(valor);
        } else if (campo.esFk()) {
            seleccionarOpcion((JComboBox<Opcion>) entrada, valor instanceof Number numero ? numero.intValue() : null);
        } else if (entrada instanceof JScrollPane panel) {
            ((JTextArea) panel.getViewport().getView()).setText(Formato.entrada(valor));
        } else if (entrada instanceof JTextField texto) {
            texto.setText(Formato.entrada(valor));
        }
    }

    private void seleccionarOpcion(JComboBox<Opcion> combo, Integer id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (java.util.Objects.equals(combo.getItemAt(i).id(), id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(0);
    }

    private void limpiar() {
        seleccionado = null;
        tabla.clearSelection();
        for (Campo campo : campos) {
            JComponent entrada = entradas.get(campo.nombre());
            entrada.setEnabled(!campo.generado());
            if (entrada instanceof JCheckBox casilla) {
                casilla.setSelected(campo.nombre().equals("Activo") || campo.nombre().equals("Vigente") || campo.nombre().equals("Disponible") || campo.nombre().equals("Obligatorio"));
            } else if (entrada instanceof JComboBox<?> combo) {
                combo.setSelectedIndex(0);
            } else if (entrada instanceof JScrollPane panel) {
                ((JTextArea) panel.getViewport().getView()).setText("");
                ((JTextArea) panel.getViewport().getView()).setEnabled(!campo.generado());
            } else if (entrada instanceof JTextField texto) {
                texto.setText("");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object leer(Campo campo) {
        JComponent entrada = entradas.get(campo.nombre());
        if (entrada instanceof JCheckBox casilla) {
            return casilla.isSelected();
        }
        if (campo.esLista()) {
            return ((JComboBox<String>) entrada).getSelectedItem();
        }
        if (campo.esFk()) {
            Opcion opcion = (Opcion) ((JComboBox<Opcion>) entrada).getSelectedItem();
            return opcion == null ? null : opcion.id();
        }
        String texto;
        if (entrada instanceof JScrollPane panel) {
            texto = ((JTextArea) panel.getViewport().getView()).getText();
        } else {
            texto = ((JTextField) entrada).getText();
        }
        texto = texto.trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            if (campo.esNumero()) {
                return Integer.valueOf(texto);
            }
            if (campo.esDecimal()) {
                return new BigDecimal(texto.replace(",", ""));
            }
            if (campo.esFechaHora()) {
                return Timestamp.valueOf(LocalDateTime.parse(texto.replace(' ', 'T')));
            }
            if (campo.esFecha()) {
                return Date.valueOf(LocalDate.parse(texto));
            }
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new NegocioException("El campo " + campo.nombre() + " tiene un valor inválido" + pista(campo) + ".");
        }
        return texto;
    }

    private String pista(Campo campo) {
        if (campo.esNumero()) {
            return " (use un número entero)";
        }
        if (campo.esDecimal()) {
            return " (use un número decimal)";
        }
        if (campo.esFechaHora()) {
            return " (use aaaa-mm-dd hh:mm)";
        }
        if (campo.esFecha()) {
            return " (use aaaa-mm-dd)";
        }
        return "";
    }

    private Map<String, Object> leerValores() {
        Map<String, Object> valores = new LinkedHashMap<>();
        for (Campo campo : campos) {
            if (!campo.generado()) {
                valores.put(campo.nombre(), leer(campo));
            }
        }
        return valores;
    }

    private void guardar() {
        ejecutarAccion("Registro guardado.", valores -> insertar(valores), true);
    }

    private void actualizarSeleccionado() {
        if (seleccionado == null) {
            Mensajes.advertencia(this, "Seleccione un registro de la tabla para actualizarlo.");
            return;
        }
        Registro objetivo = seleccionado;
        ejecutarAccion("Registro actualizado.", valores -> actualizar(objetivo, valores), true);
    }

    private void eliminarSeleccionado() {
        if (seleccionado == null) {
            Mensajes.advertencia(this, "Seleccione un registro de la tabla para eliminarlo.");
            return;
        }
        if (!Mensajes.confirmar(this, "Desea eliminar el registro seleccionado?")) {
            return;
        }
        Registro objetivo = seleccionado;
        Tareas.ejecutar(this, () -> eliminar(objetivo), desactivado -> {
            Mensajes.info(this, desactivado ? "El registro fue desactivado." : "Registro eliminado.");
            limpiar();
            cargar();
        });
    }

    private void ejecutarAccion(String mensajeExito, java.util.function.Consumer<Map<String, Object>> accion, boolean recargar) {
        Map<String, Object> valores;
        try {
            valores = leerValores();
        } catch (NegocioException e) {
            Mensajes.error(this, e);
            return;
        }
        Map<String, Object> capturados = valores;
        Tareas.ejecutar(this, () -> accion.accept(capturados), () -> {
            Mensajes.info(this, mensajeExito);
            if (recargar) {
                limpiar();
                cargar();
            }
        });
    }

    protected void insertar(Map<String, Object> valores) {
        dao.insertar(valores);
    }

    protected void actualizar(Registro anterior, Map<String, Object> valores) {
        dao.actualizar(anterior, valores);
    }

    protected boolean eliminar(Registro registro) {
        return dao.eliminar(registro);
    }

    protected static Object requerido(Map<String, Object> valores, String clave) {
        Object valor = valores.get(clave);
        if (valor == null || (valor instanceof String texto && texto.isBlank())) {
            throw new NegocioException("El campo " + clave + " es obligatorio.");
        }
        return valor;
    }

    protected static int entero(Map<String, Object> valores, String clave) {
        return ((Number) requerido(valores, clave)).intValue();
    }

    protected static Integer enteroOpcional(Map<String, Object> valores, String clave) {
        Object valor = valores.get(clave);
        return valor == null ? null : ((Number) valor).intValue();
    }

    protected static BigDecimal decimal(Map<String, Object> valores, String clave) {
        return (BigDecimal) requerido(valores, clave);
    }

    protected static BigDecimal decimalOpcional(Map<String, Object> valores, String clave) {
        return (BigDecimal) valores.get(clave);
    }

    protected static String texto(Map<String, Object> valores, String clave) {
        return (String) requerido(valores, clave);
    }

    protected static String textoOpcional(Map<String, Object> valores, String clave) {
        return (String) valores.get(clave);
    }

    protected static boolean booleano(Map<String, Object> valores, String clave) {
        return Boolean.TRUE.equals(valores.get(clave));
    }

}
