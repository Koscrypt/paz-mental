package co.pazmental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import co.pazmental.Reglas.Decision;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;

public class EstadisticasTest {

    private static final TimeZone BOGOTA = TimeZone.getTimeZone("America/Bogota");
    private static final long MIN = 60_000L;
    private static final long HORA = 60 * MIN;
    private static final long DIA = 24 * HORA;

    /** Miércoles 25/11/2026, 15:00 en Bogotá. */
    private static long ahora() {
        Calendar c = Calendar.getInstance(BOGOTA);
        c.clear();
        c.set(2026, Calendar.NOVEMBER, 25, 15, 0, 0);
        return c.getTimeInMillis();
    }

    // --- Evento ---

    @Test public void evento_ida_y_vuelta() {
        Evento e = new Evento(123L, "+573001234567", Decision.SILENCIAR_PRIMERA);
        Evento l = Evento.leer(e.serializar());
        assertEquals(123L, l.ts);
        assertEquals("+573001234567", l.numero);
        assertEquals(Decision.SILENCIAR_PRIMERA, l.decision);
    }

    @Test public void evento_oculto_y_lineas_danadas() {
        Evento o = Evento.leer(new Evento(5L, "", Decision.RECHAZAR_OCULTO).serializar());
        assertEquals("", o.numero);
        assertEquals("", o.pais());
        assertNull(Evento.leer("basura"));
        assertNull(Evento.leer("x|+57|SILENCIAR_PAIS"));
        assertNull(Evento.leer("1|+57|NO_EXISTE"));
        assertNull(Evento.leer(""));
    }

    @Test public void evento_no_deja_colar_separadores() {
        Evento e = new Evento(1L, "+57|300\n1", Decision.SILENCIAR_PAIS);
        assertEquals("+573001", e.numero);
        assertEquals(e.numero, Evento.leer(e.serializar()).numero);
    }

    @Test public void migra_registro_de_texto_1_1() {
        long a = ahora();
        Evento e = Evento.migrar("24/11 08:15  3001234567  desconocido: silenciada (si insiste en 5 min, suena)",
                a, BOGOTA);
        assertEquals("+573001234567", e.numero);
        assertEquals(Decision.SILENCIAR_PRIMERA, e.decision);
        Calendar c = Calendar.getInstance(BOGOTA);
        c.setTimeInMillis(e.ts);
        assertEquals(2026, c.get(Calendar.YEAR));
        assertEquals(24, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(8, c.get(Calendar.HOUR_OF_DAY));

        assertEquals(Decision.RECHAZAR_OCULTO, Evento.migrar("25/11 09:00  (oculto)  oculto: rechazada", a, BOGOTA).decision);
        assertEquals("", Evento.migrar("25/11 09:00  (oculto)  oculto: rechazada", a, BOGOTA).numero);
        assertEquals(Decision.SILENCIAR_PAIS, Evento.migrar("25/11 09:00  +13055551234  país no permitido: silenciada", a, BOGOTA).decision);
        assertEquals(Decision.SONAR_DOMICILIO, Evento.migrar("25/11 09:00  3001234567  modo domicilio: SONÓ", a, BOGOTA).decision);
        assertEquals(Decision.SONAR_INSISTE, Evento.migrar("25/11 09:00  3001234567  insistió: SONÓ", a, BOGOTA).decision);
        assertNull(Evento.migrar("algo raro", a, BOGOTA));
    }

    @Test public void migra_fecha_futura_al_anio_anterior() {
        Evento e = Evento.migrar("30/12 10:00  3001234567  insistió: SONÓ", ahora(), BOGOTA);
        Calendar c = Calendar.getInstance(BOGOTA);
        c.setTimeInMillis(e.ts);
        assertEquals(2025, c.get(Calendar.YEAR));
    }

    // --- Textos ---

    @Test public void textos_numero_y_bandera() {
        assertEquals("+57 300 123 4567", Textos.numero("+573001234567"));
        assertEquals("+13055551234", Textos.numero("+13055551234"));
        assertEquals("Número oculto", Textos.numero(""));
        assertEquals("🇨🇴", Textos.bandera("57"));
        assertEquals("", Textos.bandera("999"));
    }

    @Test public void textos_cuando() {
        long a = ahora();
        assertEquals("ahora", Textos.cuando(a - 10_000L, a, BOGOTA));
        assertEquals("hace 5 min", Textos.cuando(a - 5 * MIN, a, BOGOTA));
        assertEquals("hoy 09:30", Textos.cuando(a - 5 * HORA - 30 * MIN, a, BOGOTA));
        assertEquals("ayer 22:00", Textos.cuando(a - 17 * HORA, a, BOGOTA));
        assertTrue(Textos.cuando(a - 3 * DIA, a, BOGOTA).startsWith("22 nov"));
    }

    // --- Estadisticas ---

    private List<Evento> muestra() {
        long a = ahora();
        List<Evento> l = new ArrayList<>();
        l.add(new Evento(a - 1 * HORA, "+573001111111", Decision.SILENCIAR_PRIMERA)); // hoy 14:00
        l.add(new Evento(a - 1 * HORA + 2 * MIN, "+573001111111", Decision.SONAR_INSISTE));
        l.add(new Evento(a - 2 * HORA, "", Decision.RECHAZAR_OCULTO));              // hoy 13:00
        l.add(new Evento(a - 1 * DIA, "+13055551234", Decision.SILENCIAR_PAIS));     // ayer
        l.add(new Evento(a - 3 * DIA, "+573001111111", Decision.SILENCIAR_PRIMERA)); // dom 22/11
        l.add(new Evento(a - 3 * DIA, "+862112345678", Decision.SILENCIAR_PAIS));    // prefijo fuera de tabla
        l.add(new Evento(a - 8 * DIA, "+573002222222", Decision.SILENCIAR_PRIMERA)); // periodo anterior
        l.add(new Evento(a - 9 * DIA, "+573002222222", Decision.SILENCIAR_PRIMERA)); // periodo anterior
        l.add(new Evento(a - 40 * DIA, "+573003333333", Decision.SILENCIAR_PRIMERA)); // fuera de todo
        return l;
    }

    @Test public void totales_de_7_dias() {
        Estadisticas s = Estadisticas.calcular(muestra(), ahora(), 7, BOGOTA);
        assertEquals(6, s.total);
        assertEquals(1, s.sonaron);
        assertEquals(5, s.bloqueadas);
        assertEquals(1, s.ocultas);
        assertEquals(3, s.hoy);
        assertEquals(2, s.hoyBloqueadas);
        assertEquals(2, s.totalAnterior);
        assertEquals(Integer.valueOf(200), s.cambioPct());
        assertEquals(2, s.porDecision[Decision.SILENCIAR_PRIMERA.ordinal()]);
        assertEquals(2, s.porDecision[Decision.SILENCIAR_PAIS.ordinal()]);
        assertEquals(6, s.eventos.size());
        assertTrue(s.eventos.get(0).ts >= s.eventos.get(1).ts);
    }

    @Test public void serie_diaria_termina_hoy() {
        Estadisticas s = Estadisticas.calcular(muestra(), ahora(), 7, BOGOTA);
        assertFalse(s.semanal);
        assertEquals(7, s.serie.length);
        assertEquals(3, s.serie[6]); // hoy
        assertEquals(1, s.serie[5]); // ayer
        assertEquals(2, s.serie[3]); // hace 3 días
    }

    @Test public void serie_semanal_para_90_dias() {
        Estadisticas s = Estadisticas.calcular(muestra(), ahora(), 90, BOGOTA);
        assertTrue(s.semanal);
        assertEquals(13, s.serie.length);
        int suma = 0;
        for (int v : s.serie) suma += v;
        assertEquals(9, suma);
        assertNull(s.cambioPct());
    }

    @Test public void mapa_de_calor_por_dia_y_franja() {
        Estadisticas s = Estadisticas.calcular(muestra(), ahora(), 7, BOGOTA);
        // miércoles = 2; 14:00 y 14:02 → franja 4 (12–15); 13:00 → franja 4
        assertEquals(3, s.calor[2][4]);
        // domingo = 6; 15:00 → franja 5
        assertEquals(2, s.calor[6][5]);
        assertEquals(3, s.maxCalor());
    }

    @Test public void paises_e_insistentes() {
        Estadisticas s = Estadisticas.calcular(muestra(), ahora(), 7, BOGOTA);
        assertEquals("57", s.paises.get(0).clave);
        assertEquals(3, s.paises.get(0).n);
        assertEquals(3, s.paises.size()); // 57, 1, ?
        assertEquals(1, s.insistentes.size());
        assertEquals("+573001111111", s.insistentes.get(0).clave);
        assertEquals(3, s.insistentes.get(0).n);
    }

    @Test public void sin_datos_no_revienta() {
        Estadisticas s = Estadisticas.calcular(new ArrayList<>(), ahora(), 30, BOGOTA);
        assertEquals(0, s.total);
        assertEquals(30, s.serie.length);
        assertEquals(0, s.maxCalor());
        assertNull(s.cambioPct());
    }

    // --- Csv ---

    @Test public void csv_neutraliza_formulas_y_comillas() {
        assertEquals("'+573001234567", Csv.celda("+573001234567"));
        assertEquals("'=HYPERLINK(1)", Csv.celda("=HYPERLINK(1)"));
        assertEquals("\"'=A1,B1\"", Csv.celda("=A1,B1"));
        assertEquals("\"di \"\"hola\"\"\"", Csv.celda("di \"hola\""));
        assertEquals("Colombia", Csv.celda("Colombia"));
    }

    @Test public void csv_completo() {
        long a = ahora();
        List<Evento> l = new ArrayList<>();
        l.add(new Evento(a, "+573001234567", Decision.SONAR_INSISTE));
        l.add(new Evento(a, "", Decision.RECHAZAR_OCULTO));
        String[] filas = Csv.generar(l, BOGOTA).split("\r\n");
        assertEquals("fecha,hora,numero,pais,resultado,sono", filas[0]);
        assertEquals("2026-11-25,15:00,'+573001234567,Colombia,Sonó · insistió,si", filas[1]);
        assertEquals("2026-11-25,15:00,oculto,,Rechazada · número oculto,no", filas[2]);
    }
}
