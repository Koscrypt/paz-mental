package co.pazmental;

import co.pazmental.Reglas.Decision;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Textos visibles. Lógica pura, sin Android. */
public final class Textos {

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    private Textos() {}

    /** Qué pasó con la llamada, en una frase. */
    public static String etiqueta(Decision d) {
        switch (d) {
            case RECHAZAR_OCULTO: return "Rechazada · número oculto";
            case SILENCIAR_PAIS: return "Silenciada · país no permitido";
            case SILENCIAR_PRIMERA: return "Silenciada · primera vez";
            case SONAR_DOMICILIO: return "Sonó · modo domicilio";
            case SONAR_CONFIANZA: return "Sonó · número de confianza";
            case SILENCIAR_BLOQUEADO: return "Silenciada · número bloqueado";
            case SILENCIAR_DESCANSO: return "Silenciada · horario de descanso";
            default: return "Sonó · insistió";
        }
    }

    /** Nombre corto para la etiqueta de una llamada. */
    public static String corta(Decision d) {
        switch (d) {
            case RECHAZAR_OCULTO: return "Oculto";
            case SILENCIAR_PAIS: return "Otro país";
            case SILENCIAR_PRIMERA: return "Primera vez";
            case SONAR_DOMICILIO: return "Domicilio";
            case SONAR_CONFIANZA: return "Confianza";
            case SILENCIAR_BLOQUEADO: return "Bloqueado";
            case SILENCIAR_DESCANSO: return "Descanso";
            default: return "Insistió";
        }
    }

    /** Bandera del prefijo como emoji, o "" si no se conoce. */
    public static String bandera(String prefijo) {
        String iso;
        switch (prefijo) {
            case "57": iso = "CO"; break;
            case "1": iso = "US"; break;
            case "52": iso = "MX"; break;
            case "34": iso = "ES"; break;
            case "593": iso = "EC"; break;
            case "51": iso = "PE"; break;
            case "56": iso = "CL"; break;
            case "54": iso = "AR"; break;
            case "55": iso = "BR"; break;
            case "58": iso = "VE"; break;
            case "507": iso = "PA"; break;
            case "506": iso = "CR"; break;
            case "44": iso = "GB"; break;
            case "49": iso = "DE"; break;
            case "33": iso = "FR"; break;
            case "39": iso = "IT"; break;
            default: return "";
        }
        int base = 0x1F1E6 - 'A';
        return new String(Character.toChars(base + iso.charAt(0)))
                + new String(Character.toChars(base + iso.charAt(1)));
    }

    /** Número para leer: "+57 300 123 4567" para un celular colombiano; si no, tal cual. */
    public static String numero(String normalizado) {
        if (normalizado == null || normalizado.isEmpty()) return "Número oculto";
        if (normalizado.length() == 13 && normalizado.startsWith("+573")) {
            return "+57 " + normalizado.substring(3, 6) + " " + normalizado.substring(6, 9)
                    + " " + normalizado.substring(9);
        }
        return normalizado;
    }

    /** "hace 5 min", "hoy 14:32", "ayer 09:10" o "12 nov 08:00". */
    public static String cuando(long ts, long ahora, TimeZone tz) {
        long dif = ahora - ts;
        if (dif >= 0 && dif < 60_000L) return "ahora";
        if (dif >= 0 && dif < 3600_000L) return "hace " + (dif / 60_000L) + " min";
        SimpleDateFormat hora = new SimpleDateFormat("HH:mm", ES);
        hora.setTimeZone(tz);
        long dias = diasEntre(ts, ahora, tz);
        if (dias == 0) return "hoy " + hora.format(new Date(ts));
        if (dias == 1) return "ayer " + hora.format(new Date(ts));
        SimpleDateFormat f = new SimpleDateFormat("d MMM HH:mm", ES);
        f.setTimeZone(tz);
        return f.format(new Date(ts)).replace(".", "");
    }

    /** Días de calendario entre dos instantes (0 si es el mismo día). */
    static long diasEntre(long desde, long hasta, TimeZone tz) {
        return (inicioDia(hasta, tz) - inicioDia(desde, tz) + 12 * 3600_000L) / 86_400_000L;
    }

    /** Medianoche local del día de {@code ts}. */
    static long inicioDia(long ts, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Hora del día a partir de minutos desde la medianoche: "10:00 p. m.". */
    public static String hora(int minutos) {
        SimpleDateFormat f = new SimpleDateFormat("h:mm a", ES);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(minutos * 60_000L));
    }

    /** Fecha larga para el encabezado: "25 de noviembre de 2026". */
    public static String fechaLarga(long ts, TimeZone tz) {
        SimpleDateFormat f = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", ES);
        f.setTimeZone(tz);
        return f.format(new Date(ts));
    }
}
