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
}
