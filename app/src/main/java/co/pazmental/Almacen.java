package co.pazmental;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Configuración y registro, solo en el teléfono. */
final class Almacen {

    private static final String ARCHIVO = "paz_mental";
    private static final String K_PAISES = "paises";
    private static final String K_INTENTOS = "intentos";
    private static final String K_REGISTRO = "registro";
    private static final String K_DOMICILIO_PAIS = "domicilio_pais";
    private static final String K_DOMICILIO_FIN = "domicilio_fin";
    private static final int MAX_REGISTRO = 50;

    private final SharedPreferences sp;

    Almacen(Context c) {
        sp = c.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    Set<String> paises() {
        return new HashSet<>(sp.getStringSet(K_PAISES, Collections.singleton(Reglas.PAIS_LOCAL)));
    }

    void guardarPaises(Set<String> p) {
        sp.edit().putStringSet(K_PAISES, new HashSet<>(p)).apply();
    }

    Map<String, Long> intentos() {
        Map<String, Long> m = new HashMap<>();
        for (String linea : sp.getString(K_INTENTOS, "").split("\n")) {
            int i = linea.indexOf('|');
            if (i > 0) {
                try { m.put(linea.substring(0, i), Long.parseLong(linea.substring(i + 1))); }
                catch (NumberFormatException ignored) {}
            }
        }
        return m;
    }

    void guardarIntentos(Map<String, Long> m, long ahora) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> e : m.entrySet()) {
            if (ahora - e.getValue() <= Reglas.VENTANA_INSISTENCIA_MS) {
                sb.append(e.getKey()).append('|').append(e.getValue()).append('\n');
            }
        }
        sp.edit().putString(K_INTENTOS, sb.toString()).apply();
    }

    /** Prefijo del país del modo domicilio, o "" si nunca se abrió o se cerró. */
    String domicilioPais() {
        return sp.getString(K_DOMICILIO_PAIS, "");
    }

    /** Instante (ms) en que se cierra el modo domicilio; 0 si no hay. */
    long domicilioFin() {
        return sp.getLong(K_DOMICILIO_FIN, 0L);
    }

    void abrirDomicilio(String pais, long fin) {
        sp.edit().putString(K_DOMICILIO_PAIS, pais).putLong(K_DOMICILIO_FIN, fin).apply();
    }

    void cerrarDomicilio() {
        sp.edit().remove(K_DOMICILIO_PAIS).remove(K_DOMICILIO_FIN).apply();
    }

    void anotar(String numero, String decision) {
        String fecha = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
        String nuevo = fecha + "  " + numero + "  " + decision;
        String[] previas = sp.getString(K_REGISTRO, "").split("\n");
        StringBuilder sb = new StringBuilder(nuevo);
        for (int i = 0; i < previas.length && i < MAX_REGISTRO - 1; i++) {
            if (!previas[i].isEmpty()) sb.append('\n').append(previas[i]);
        }
        sp.edit().putString(K_REGISTRO, sb.toString()).apply();
    }

    String registro() {
        return sp.getString(K_REGISTRO, "");
    }

    void limpiarRegistro() {
        sp.edit().remove(K_REGISTRO).apply();
    }
}
