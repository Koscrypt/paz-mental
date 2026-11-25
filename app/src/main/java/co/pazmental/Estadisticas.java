package co.pazmental;

import co.pazmental.Reglas.Decision;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/** Cifras de un periodo, calculadas del historial. Lógica pura, sin Android. */
public final class Estadisticas {

    /** Clave y cuántas veces aparece. */
    public static final class Conteo {
        public final String clave;
        public final int n;

        Conteo(String clave, int n) {
            this.clave = clave;
            this.n = n;
        }
    }

    /** Días del periodo, contando hoy. */
    public final int dias;
    /** Primer instante del periodo (medianoche local). */
    public final long desde;
    public final int total;
    public final int sonaron;
    public final int ocultas;
    /** Silenciadas o rechazadas: las que no timbraron. */
    public final int bloqueadas;
    /** Total del periodo anterior de igual duración. */
    public final int totalAnterior;
    public final int hoy;
    public final int hoyBloqueadas;
    /** Por {@link Decision#ordinal()}. */
    public final int[] porDecision = new int[Decision.values().length];
    /** Llamadas por día (o por semana si el periodo pasa de 31 días); el último es el actual. */
    public final int[] serie;
    public final boolean semanal;
    /** [día de la semana, lunes = 0][franja de 3 horas, 0 = 00:00–02:59]. */
    public final int[][] calor = new int[7][8];
    /** Prefijo de país → llamadas, de mayor a menor. "?" agrupa los prefijos fuera de la tabla. */
    public final List<Conteo> paises;
    /** Números que llamaron 2 o más veces, de mayor a menor, máximo 5. */
    public final List<Conteo> insistentes;
    /** Eventos del periodo, del más reciente al más antiguo. */
    public final List<Evento> eventos;

    private Estadisticas(List<Evento> todos, long ahora, int dias, TimeZone tz) {
        this.dias = dias;
        long hoyInicio = Textos.inicioDia(ahora, tz);
        desde = retroceder(hoyInicio, dias - 1, tz);
        long anteriorDesde = retroceder(desde, dias, tz);
        semanal = dias > 31;
        serie = new int[semanal ? (dias + 6) / 7 : dias];

        Map<String, Integer> pais = new HashMap<>();
        Map<String, Integer> num = new HashMap<>();
        List<Evento> lista = new ArrayList<>();
        int t = 0, s = 0, o = 0, ant = 0, h = 0, hb = 0;
        Calendar c = Calendar.getInstance(tz);

        for (Evento e : todos) {
            if (e.ts >= anteriorDesde && e.ts < desde) ant++;
            if (e.ts < desde || e.ts > ahora) continue;
            lista.add(e);
            t++;
            if (e.sono()) s++;
            if (e.decision == Decision.RECHAZAR_OCULTO) o++;
            if (e.ts >= hoyInicio) {
                h++;
                if (!e.sono()) hb++;
            }
            porDecision[e.decision.ordinal()]++;

            int d = (int) Textos.diasEntre(desde, e.ts, tz);
            int i = semanal ? d / 7 : d;
            if (i >= 0 && i < serie.length) serie[i]++;

            c.setTimeInMillis(e.ts);
            int dow = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            calor[dow][c.get(Calendar.HOUR_OF_DAY) / 3]++;

            if (!e.numero.isEmpty()) {
                String p = e.pais();
                if (p.isEmpty()) p = "?";
                pais.put(p, pais.getOrDefault(p, 0) + 1);
                num.put(e.numero, num.getOrDefault(e.numero, 0) + 1);
            }
        }
        total = t;
        sonaron = s;
        ocultas = o;
        bloqueadas = t - s;
        totalAnterior = ant;
        hoy = h;
        hoyBloqueadas = hb;
        Collections.sort(lista, (a, b) -> Long.compare(b.ts, a.ts));
        eventos = Collections.unmodifiableList(lista);
        paises = ordenar(pais, 1, Integer.MAX_VALUE);
        insistentes = ordenar(num, 2, 5);
    }

    /** Calcula las cifras de los últimos {@code dias} días, hoy incluido. */
    public static Estadisticas calcular(List<Evento> todos, long ahora, int dias, TimeZone tz) {
        if (dias < 1) throw new IllegalArgumentException("dias < 1");
        return new Estadisticas(todos, ahora, dias, tz);
    }

    /** Variación porcentual frente al periodo anterior; null si el anterior no tuvo llamadas. */
    public Integer cambioPct() {
        if (totalAnterior == 0) return null;
        return Math.round((total - totalAnterior) * 100f / totalAnterior);
    }

    /** Mayor valor del mapa de calor (0 si no hay datos). */
    public int maxCalor() {
        int m = 0;
        for (int[] fila : calor) for (int v : fila) m = Math.max(m, v);
        return m;
    }

    private static long retroceder(long medianoche, int dias, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(medianoche);
        c.add(Calendar.DAY_OF_MONTH, -dias);
        return c.getTimeInMillis();
    }

    private static List<Conteo> ordenar(Map<String, Integer> m, int minimo, int max) {
        List<Conteo> l = new ArrayList<>();
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (e.getValue() >= minimo) l.add(new Conteo(e.getKey(), e.getValue()));
        }
        Collections.sort(l, (a, b) -> a.n != b.n ? Integer.compare(b.n, a.n) : a.clave.compareTo(b.clave));
        return Collections.unmodifiableList(l.size() > max ? new ArrayList<>(l.subList(0, max)) : l);
    }
}
