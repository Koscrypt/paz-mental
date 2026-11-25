package co.pazmental;

import static org.junit.Assert.assertEquals;

import co.pazmental.Reglas.Decision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ReglasTest {

    private static final long MIN = 60_000L;

    private Set<String> soloColombia() {
        Set<String> s = new HashSet<>();
        s.add("57");
        return s;
    }

    @Test public void normaliza_formatos() {
        assertEquals("+573001234567", Reglas.normalizar("300 123 4567"));
        assertEquals("+573001234567", Reglas.normalizar("+57 300-123-4567"));
        assertEquals("+13055551234", Reglas.normalizar("0013055551234"));
        assertEquals("", Reglas.normalizar(null));
    }

    @Test public void detecta_pais_por_prefijo_mas_largo() {
        assertEquals("57", Reglas.pais("+573001234567"));
        assertEquals("593", Reglas.pais("+593991234567"));
        assertEquals("1", Reglas.pais("+13055551234"));
        assertEquals("", Reglas.pais("+862112345678"));
    }

    @Test public void oculto_se_rechaza() {
        assertEquals(Decision.RECHAZAR_OCULTO,
                Reglas.decidir("", false, soloColombia(), new HashMap<>(), 0));
        assertEquals(Decision.RECHAZAR_OCULTO,
                Reglas.decidir("3001234567", true, soloColombia(), new HashMap<>(), 0));
    }

    @Test public void pais_no_permitido_se_silencia_siempre() {
        Map<String, Long> m = new HashMap<>();
        assertEquals(Decision.SILENCIAR_PAIS, Reglas.decidir("+13055551234", false, soloColombia(), m, 0));
        assertEquals(Decision.SILENCIAR_PAIS, Reglas.decidir("+13055551234", false, soloColombia(), m, MIN));
    }

    @Test public void insiste_en_5_min_suena() {
        Map<String, Long> m = new HashMap<>();
        assertEquals(Decision.SILENCIAR_PRIMERA, Reglas.decidir("3001234567", false, soloColombia(), m, 0));
        assertEquals(Decision.SONAR_INSISTE, Reglas.decidir("+573001234567", false, soloColombia(), m, 5 * MIN));
    }

    @Test public void despues_de_5_min_vuelve_a_silenciar() {
        Map<String, Long> m = new HashMap<>();
        Reglas.decidir("3001234567", false, soloColombia(), m, 0);
        assertEquals(Decision.SILENCIAR_PRIMERA,
                Reglas.decidir("3001234567", false, soloColombia(), m, 5 * MIN + 1));
    }

    @Test public void otro_pais_permitido_sigue_la_misma_regla() {
        Set<String> s = soloColombia();
        s.add("1");
        Map<String, Long> m = new HashMap<>();
        assertEquals(Decision.SILENCIAR_PRIMERA, Reglas.decidir("+13055551234", false, s, m, 0));
        assertEquals(Decision.SONAR_INSISTE, Reglas.decidir("+13055551234", false, s, m, 2 * MIN));
    }

    // --- Modo domicilio ---

    @Test public void domicilio_abierto_suena_a_la_primera() {
        Map<String, Long> m = new HashMap<>();
        assertEquals(Decision.SONAR_DOMICILIO,
                Reglas.decidir("3001234567", false, soloColombia(), m, 0, "57", 30 * MIN));
        assertEquals("no deja intento pendiente", 0, m.size());
    }

    @Test public void domicilio_abre_un_pais_no_permitido() {
        assertEquals(Decision.SONAR_DOMICILIO,
                Reglas.decidir("+13055551234", false, soloColombia(), new HashMap<>(), 0, "1", 30 * MIN));
    }

    @Test public void domicilio_no_abre_otros_paises() {
        assertEquals(Decision.SILENCIAR_PAIS,
                Reglas.decidir("+13055551234", false, soloColombia(), new HashMap<>(), 0, "57", 30 * MIN));
        assertEquals(Decision.SILENCIAR_PRIMERA,
                Reglas.decidir("3001234567", false, soloColombia(), new HashMap<>(), 0, "1", 30 * MIN));
    }

    @Test public void domicilio_no_deja_pasar_ocultos() {
        assertEquals(Decision.RECHAZAR_OCULTO,
                Reglas.decidir("3001234567", true, soloColombia(), new HashMap<>(), 0, "57", 30 * MIN));
        assertEquals(Decision.RECHAZAR_OCULTO,
                Reglas.decidir("", false, soloColombia(), new HashMap<>(), 0, "57", 30 * MIN));
    }

    @Test public void domicilio_vencido_vuelve_a_las_reglas_normales() {
        assertEquals(Decision.SILENCIAR_PRIMERA,
                Reglas.decidir("3001234567", false, soloColombia(), new HashMap<>(), 30 * MIN, "57", 30 * MIN));
        assertEquals(Decision.SILENCIAR_PAIS,
                Reglas.decidir("+13055551234", false, soloColombia(), new HashMap<>(), 31 * MIN, "1", 30 * MIN));
    }

    @Test public void domicilio_sin_pais_no_hace_nada() {
        assertEquals(Decision.SILENCIAR_PRIMERA,
                Reglas.decidir("3001234567", false, soloColombia(), new HashMap<>(), 0, "", 30 * MIN));
    }

    // --- Celular colombiano que llega con +1 delante ---

    @Test public void mas_uno_seguido_de_celular_colombiano_es_colombiano() {
        assertEquals("+573001234567", Reglas.normalizar("+1573001234567"));
        assertEquals("+573001234567", Reglas.normalizar("001573001234567"));
        assertEquals("57", Reglas.pais(Reglas.normalizar("+1 573 001 234 567")));
    }

    @Test public void mas_uno_legitimo_no_cambia() {
        assertEquals("+13055551234", Reglas.normalizar("+13055551234"));
        // +1 57x de 10 dígitos: número de EE. UU. con código de área 57x, se respeta.
        assertEquals("+15735551234", Reglas.normalizar("+15735551234"));
        // 12 dígitos tras +1 que no son celular colombiano: no se adivina.
        assertEquals("+1571234567890", Reglas.normalizar("+1571234567890"));
    }

    @Test public void mas_uno_colombiano_sigue_las_reglas_de_colombia() {
        assertEquals(Decision.SILENCIAR_PRIMERA,
                Reglas.decidir("+1573001234567", false, soloColombia(), new HashMap<>(), 0));
        assertEquals(Decision.SONAR_DOMICILIO,
                Reglas.decidir("+1573001234567", false, soloColombia(), new HashMap<>(), 0, "57", 30 * MIN));
    }

    @Test public void mas_uno_colombiano_cuenta_como_el_mismo_numero_al_insistir() {
        Map<String, Long> m = new HashMap<>();
        Reglas.decidir("+1573001234567", false, soloColombia(), m, 0);
        assertEquals(Decision.SONAR_INSISTE,
                Reglas.decidir("3001234567", false, soloColombia(), m, 2 * MIN));
    }
}
