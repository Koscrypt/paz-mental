package co.pazmental;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/** Configuración e historial, solo en el teléfono. */
final class Almacen {

    private static final String ARCHIVO = "paz_mental";
    private static final String K_PAISES = "paises";
    private static final String K_INTENTOS = "intentos";
    /** Registro de texto de la versión 1.1; se migra a K_EVENTOS al primer uso. */
    private static final String K_REGISTRO = "registro";
    private static final String K_EVENTOS = "eventos";
    private static final String K_DOMICILIO_PAIS = "domicilio_pais";
    private static final String K_DOMICILIO_INICIO = "domicilio_inicio";
    private static final String K_DOMICILIO_FIN = "domicilio_fin";
    private static final String K_TEMA = "tema";
    private static final String K_CONFIANZA = "confianza";
    private static final String K_BLOQUEADOS = "bloqueados";
    private static final String K_DESCANSO = "descanso";
    private static final String K_DESCANSO_INICIO = "descanso_inicio";
    private static final String K_DESCANSO_FIN = "descanso_fin";
    private static final String K_DOMICILIO_ULTIMO_PAIS = "domicilio_ultimo_pais";
    private static final String K_DOMICILIO_ULTIMA_DURACION = "domicilio_ultima_duracion";
    /** Llamadas que se guardan para el historial y las estadísticas. */
    static final int MAX_EVENTOS = 1000;

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

    /** Instante (ms) en que se abrió el modo domicilio; 0 si no se sabe. */
    long domicilioInicio() {
        return sp.getLong(K_DOMICILIO_INICIO, 0L);
    }

    /** Instante (ms) en que se cierra el modo domicilio; 0 si no hay. */
    long domicilioFin() {
        return sp.getLong(K_DOMICILIO_FIN, 0L);
    }

    /** Abre el modo domicilio y recuerda país y duración para el botón de ajustes rápidos. */
    void abrirDomicilio(String pais, long inicio, long fin) {
        int min = (int) Math.round((fin - inicio) / 60_000.0);
        sp.edit().putString(K_DOMICILIO_PAIS, pais).putLong(K_DOMICILIO_INICIO, inicio)
                .putLong(K_DOMICILIO_FIN, fin)
                .putString(K_DOMICILIO_ULTIMO_PAIS, pais).putInt(K_DOMICILIO_ULTIMA_DURACION, min).apply();
    }

    /** País del último modo domicilio abierto; Colombia si nunca se abrió. */
    String ultimoPaisDomicilio() {
        return sp.getString(K_DOMICILIO_ULTIMO_PAIS, Reglas.PAIS_LOCAL);
    }

    /** Duración (min) del último modo domicilio abierto; 30 si nunca se abrió. */
    int ultimaDuracionDomicilio() {
        return sp.getInt(K_DOMICILIO_ULTIMA_DURACION, 30);
    }

    // ---------------------------------------------------------------- Listas por número

    /** Números normalizados que siempre suenan. */
    Set<String> confianza() {
        return new HashSet<>(sp.getStringSet(K_CONFIANZA, Collections.<String>emptySet()));
    }

    /** Números normalizados que siempre se silencian. */
    Set<String> bloqueados() {
        return new HashSet<>(sp.getStringSet(K_BLOQUEADOS, Collections.<String>emptySet()));
    }

    /** Pone el número en confianza (true) o bloqueados (false); lo saca de la otra lista. */
    void ponerEnLista(String numero, boolean confiable) {
        Set<String> c = confianza(), b = bloqueados();
        if (confiable) { c.add(numero); b.remove(numero); }
        else { b.add(numero); c.remove(numero); }
        sp.edit().putStringSet(K_CONFIANZA, c).putStringSet(K_BLOQUEADOS, b).apply();
    }

    /** Saca el número de ambas listas. */
    void quitarDeListas(String numero) {
        Set<String> c = confianza(), b = bloqueados();
        c.remove(numero);
        b.remove(numero);
        sp.edit().putStringSet(K_CONFIANZA, c).putStringSet(K_BLOQUEADOS, b).apply();
    }

    // ---------------------------------------------------------------- Horario de descanso

    boolean descansoActivo() {
        return sp.getBoolean(K_DESCANSO, false);
    }

    /** Minutos desde la medianoche; por defecto 22:00. */
    int descansoInicio() {
        return sp.getInt(K_DESCANSO_INICIO, 22 * 60);
    }

    /** Minutos desde la medianoche; por defecto 07:00. */
    int descansoFin() {
        return sp.getInt(K_DESCANSO_FIN, 7 * 60);
    }

    void guardarDescanso(boolean activo, int inicio, int fin) {
        sp.edit().putBoolean(K_DESCANSO, activo).putInt(K_DESCANSO_INICIO, inicio)
                .putInt(K_DESCANSO_FIN, fin).apply();
    }

    /** ¿Está activo el horario de descanso y cae {@code ahora} dentro de él (hora local)? */
    boolean enDescanso(long ahora) {
        if (!descansoActivo()) return false;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ahora);
        int min = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        return Reglas.enDescanso(min, descansoInicio(), descansoFin());
    }

    void cerrarDomicilio() {
        sp.edit().remove(K_DOMICILIO_PAIS).remove(K_DOMICILIO_INICIO).remove(K_DOMICILIO_FIN).apply();
    }

    /** Anota una llamada filtrada al principio del historial. */
    void anotar(Evento e) {
        String[] previas = sp.getString(K_EVENTOS, "").split("\n");
        StringBuilder sb = new StringBuilder(e.serializar());
        int n = 1;
        for (String l : previas) {
            if (n >= MAX_EVENTOS) break;
            if (!l.isEmpty()) { sb.append('\n').append(l); n++; }
        }
        sp.edit().putString(K_EVENTOS, sb.toString()).apply();
    }

    /** Historial, del más reciente al más antiguo. */
    List<Evento> eventos() {
        migrarRegistro();
        List<Evento> l = new ArrayList<>();
        for (String linea : sp.getString(K_EVENTOS, "").split("\n")) {
            Evento e = Evento.leer(linea);
            if (e != null) l.add(e);
        }
        Collections.sort(l, (a, b) -> Long.compare(b.ts, a.ts));
        return l;
    }

    /** Pasa el registro de texto de la 1.1 al historial estructurado, una sola vez. */
    private void migrarRegistro() {
        String viejo = sp.getString(K_REGISTRO, null);
        if (viejo == null) return;
        long ahora = System.currentTimeMillis();
        TimeZone tz = TimeZone.getDefault();
        StringBuilder sb = new StringBuilder(sp.getString(K_EVENTOS, ""));
        for (String linea : viejo.split("\n")) {
            Evento e = Evento.migrar(linea, ahora, tz);
            if (e == null) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(e.serializar());
        }
        sp.edit().putString(K_EVENTOS, sb.toString()).remove(K_REGISTRO).commit();
    }

    void limpiarRegistro() {
        sp.edit().remove(K_EVENTOS).remove(K_REGISTRO).apply();
    }

    /** Tema.SISTEMA, Tema.CLARO o Tema.OSCURO. */
    String tema() {
        return sp.getString(K_TEMA, Tema.SISTEMA);
    }

    void guardarTema(String tema) {
        sp.edit().putString(K_TEMA, tema).apply();
    }
}
