package co.pazmental;

import java.util.Collections;
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

    /** Duraciones que ofrece el modo domicilio, en minutos. */
    public static final int[] DURACIONES_DOMICILIO_MIN = {15, 30, 60, 120};

    /**
     * Qué se hace con la llamada. Los valores nuevos van al final: el historial guarda el nombre,
     * así que reordenar no rompe nada, pero se mantiene el orden por claridad.
     */
    public enum Decision {
        RECHAZAR_OCULTO, SILENCIAR_PAIS, SILENCIAR_PRIMERA, SONAR_INSISTE, SONAR_DOMICILIO,
        /** El número está en la lista de confianza. */
        SONAR_CONFIANZA,
        /** El número está en la lista de bloqueados. */
        SILENCIAR_BLOQUEADO,
        /** Llegó dentro del horario de descanso. */
        SILENCIAR_DESCANSO
    }

    /** ¿Timbra la llamada con esta decisión? */
    public static boolean suena(Decision d) {
        return d == Decision.SONAR_INSISTE || d == Decision.SONAR_DOMICILIO || d == Decision.SONAR_CONFIANZA;
    }

    /**
     * ¿Cae {@code minuto} (minutos desde la medianoche, 0–1439) en el horario de descanso?
     * El horario puede cruzar la medianoche (22:00 a 07:00). Si inicio y fin son iguales, no hay horario.
     */
    public static boolean enDescanso(int minuto, int inicio, int fin) {
        if (inicio == fin) return false;
        if (inicio < fin) return minuto >= inicio && minuto < fin;
        return minuto >= inicio || minuto < fin;
    }

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
        if (internacional) return corregirMasUnoColombiano("+" + dig);
        if (dig.startsWith("00")) return corregirMasUnoColombiano("+" + dig.substring(2));
        // Marcación nacional colombiana: se asume país local.
        return "+" + PAIS_LOCAL + dig;
    }

    /**
     * Algunos operadores entregan un celular colombiano con un +1 delante: +1 57 3XX XXX XXXX.
     * Un número de EE. UU. o Canadá tiene 10 dígitos tras el +1; este tiene 12, así que no es
     * de ese plan. Si lo que sigue al +1 es un celular colombiano (57 y 10 dígitos que empiezan
     * por 3), se lee como colombiano.
     */
    static String corregirMasUnoColombiano(String n) {
        if (n.length() == 14 && n.startsWith("+1" + PAIS_LOCAL + "3")) return "+" + n.substring(2);
        return n;
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

    /** Nombre visible de un prefijo de la tabla, o "+<prefijo>" si no está. */
    public static String nombrePais(String prefijo) {
        for (String[] p : PAISES) if (p[0].equals(prefijo)) return p[1];
        return "+" + prefijo;
    }

    /** ¿Está abierto el modo domicilio en este instante? */
    public static boolean domicilioActivo(String paisDomicilio, long finDomicilio, long ahora) {
        return paisDomicilio != null && !paisDomicilio.isEmpty() && ahora < finDomicilio;
    }

    /** Decide sin modo domicilio. */
    public static Decision decidir(String crudo, boolean oculto, Set<String> permitidos,
                                   Map<String, Long> intentos, long ahora) {
        return decidir(crudo, oculto, permitidos, intentos, ahora, "", 0L);
    }

    /**
     * Decide qué hacer con una llamada de un desconocido.
     * @param intentos último intento silenciado por número; se actualiza aquí.
     * @param paisDomicilio prefijo del país abierto por el modo domicilio, o "" si no hay.
     * @param finDomicilio instante (ms) en que se cierra el modo domicilio.
     */
    public static Decision decidir(String crudo, boolean oculto, Set<String> permitidos,
                                   Map<String, Long> intentos, long ahora,
                                   String paisDomicilio, long finDomicilio) {
        return decidir(crudo, oculto, permitidos, intentos, ahora, paisDomicilio, finDomicilio,
                Collections.<String>emptySet(), Collections.<String>emptySet(), false);
    }

    /**
     * Decide con todas las reglas. Orden de precedencia:
     * oculto → bloqueado → confianza → modo domicilio → descanso → país → insistencia.
     * @param confianza números normalizados que siempre suenan.
     * @param bloqueados números normalizados que siempre se silencian.
     * @param descanso ¿estamos dentro del horario de descanso?
     */
    public static Decision decidir(String crudo, boolean oculto, Set<String> permitidos,
                                   Map<String, Long> intentos, long ahora,
                                   String paisDomicilio, long finDomicilio,
                                   Set<String> confianza, Set<String> bloqueados, boolean descanso) {
        String n = normalizar(crudo);
        // Los ocultos se rechazan siempre, también con el modo domicilio abierto.
        if (oculto || n.isEmpty()) return Decision.RECHAZAR_OCULTO;
        // Lo que el usuario decidió para un número gana sobre las reglas generales.
        if (bloqueados.contains(n)) return Decision.SILENCIAR_BLOQUEADO;
        if (confianza.contains(n)) return Decision.SONAR_CONFIANZA;

        String pais = pais(n);
        // Modo domicilio: el país elegido suena a la primera, esté o no en los permitidos.
        // Lo abre el usuario a propósito, así que también gana sobre el horario de descanso.
        if (domicilioActivo(paisDomicilio, finDomicilio, ahora) && pais.equals(paisDomicilio)) {
            return Decision.SONAR_DOMICILIO;
        }
        // En descanso no suena ningún desconocido, aunque insista; tampoco cuenta como intento.
        if (descanso) return Decision.SILENCIAR_DESCANSO;
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
