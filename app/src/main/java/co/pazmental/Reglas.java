package co.pazmental;

import java.util.Map;
import java.util.Set;

/**
 * Reglas de Paz Mental. Lógica pura, sin Android, para poder probarla.
 *
 * Los contactos no llegan aquí: Android solo consulta al filtro por números
 * que NO están en la agenda.
 */
public final class Reglas {

    public static final String PAIS_LOCAL = "57"; // Colombia
    public static final long VENTANA_INSISTENCIA_MS = 5 * 60 * 1000L;

    public enum Decision { RECHAZAR_OCULTO, SILENCIAR_PAIS, SILENCIAR_PRIMERA, SONAR_INSISTE }

    /** País (prefijo), nombre visible. Orden: el de la pantalla. */
    public static final String[][] PAISES = {
            {"57", "Colombia"},
            {"1", "EE. UU. / Canadá"},
            {"52", "México"},
            {"34", "España"},
            {"593", "Ecuador"},
            {"51", "Perú"},
            {"56", "Chile"},
            {"54", "Argentina"},
            {"55", "Brasil"},
            {"58", "Venezuela"},
            {"507", "Panamá"},
            {"506", "Costa Rica"},
            {"44", "Reino Unido"},
            {"49", "Alemania"},
            {"33", "Francia"},
            {"39", "Italia"},
    };

    private Reglas() {}

    /** Deja el número en formato +<prefijo><número>. Vacío si no hay número. */
    public static String normalizar(String crudo) {
        if (crudo == null) return "";
        String t = crudo.trim();
        boolean internacional = t.startsWith("+");
        StringBuilder d = new StringBuilder();
        for (char c : t.toCharArray()) if (Character.isDigit(c)) d.append(c);
        String dig = d.toString();
        if (dig.isEmpty()) return "";
        if (internacional) return "+" + dig;
        if (dig.startsWith("00")) return "+" + dig.substring(2);
        // Marcación nacional colombiana: se asume país local.
        return "+" + PAIS_LOCAL + dig;
    }

    /** Prefijo de país de un número normalizado, o "" si no está en la tabla. */
    public static String pais(String normalizado) {
        if (!normalizado.startsWith("+")) return "";
        String dig = normalizado.substring(1);
        String mejor = "";
        for (String[] p : PAISES) {
            if (dig.startsWith(p[0]) && p[0].length() > mejor.length()) mejor = p[0];
        }
        return mejor;
    }

    /**
     * Decide qué hacer con una llamada de un desconocido.
     * @param intentos último intento silenciado por número; se actualiza aquí.
     */
    public static Decision decidir(String crudo, boolean oculto, Set<String> permitidos,
                                   Map<String, Long> intentos, long ahora) {
        String n = normalizar(crudo);
        if (oculto || n.isEmpty()) return Decision.RECHAZAR_OCULTO;

        String pais = pais(n);
        if (pais.isEmpty() || !permitidos.contains(pais)) return Decision.SILENCIAR_PAIS;

        Long previo = intentos.get(n);
        if (previo != null && ahora - previo <= VENTANA_INSISTENCIA_MS) {
            intentos.remove(n);
            return Decision.SONAR_INSISTE;
        }
        intentos.put(n, ahora);
        return Decision.SILENCIAR_PRIMERA;
    }
}
