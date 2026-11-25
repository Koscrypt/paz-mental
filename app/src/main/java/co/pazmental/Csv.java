package co.pazmental;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Exporta el historial a CSV. Lógica pura, sin Android. */
public final class Csv {

    private Csv() {}

    /** Historial completo, una llamada por fila, con encabezado. */
    public static String generar(List<Evento> eventos, TimeZone tz) {
        SimpleDateFormat fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        SimpleDateFormat hora = new SimpleDateFormat("HH:mm", Locale.ROOT);
        fecha.setTimeZone(tz);
        hora.setTimeZone(tz);
        StringBuilder sb = new StringBuilder("fecha,hora,numero,pais,resultado,sono\r\n");
        for (Evento e : eventos) {
            Date d = new Date(e.ts);
            String p = e.pais();
            sb.append(fecha.format(d)).append(',')
                    .append(hora.format(d)).append(',')
                    .append(celda(e.numero.isEmpty() ? "oculto" : e.numero)).append(',')
                    .append(celda(p.isEmpty() ? "" : Reglas.nombrePais(p))).append(',')
                    .append(celda(Textos.etiqueta(e.decision))).append(',')
                    .append(e.sono() ? "si" : "no").append("\r\n");
        }
        return sb.toString();
    }

    /**
     * Escapa una celda. Una celda que empieza por = + - @ o un control la toma la hoja de
     * cálculo como fórmula: se le antepone un apóstrofo (inyección de CSV). Así, además,
     * "+573001234567" se ve como número de teléfono y no como 5,73E+11.
     */
    static String celda(String s) {
        if (s == null) return "";
        String v = s;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
