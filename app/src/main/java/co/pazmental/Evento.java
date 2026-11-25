package co.pazmental;

import co.pazmental.Reglas.Decision;

import java.util.Calendar;
import java.util.TimeZone;

/** Una llamada filtrada. Lógica pura, sin Android. */
public final class Evento {

    /** Instante de la llamada, en ms. */
    public final long ts;
    /** Número normalizado (+<prefijo><número>), o "" si llegó oculto. */
    public final String numero;
    public final Decision decision;

    public Evento(long ts, String numero, Decision decision) {
        this.ts = ts;
        this.numero = numero == null ? "" : numero.replace("|", "").replace("\n", "");
        this.decision = decision;
    }

    /** ¿La llamada timbró? */
    public boolean sono() {
        return Reglas.suena(decision);
    }

    /** Prefijo de país, o "" si es oculto o de un país fuera de la tabla. */
    public String pais() {
        return numero.isEmpty() ? "" : Reglas.pais(numero);
    }

    /** Una línea del almacén: ts|numero|DECISION. */
    public String serializar() {
        return ts + "|" + numero + "|" + decision.name();
    }

    /** Lee una línea del almacén; null si está dañada. */
    public static Evento leer(String linea) {
        if (linea == null) return null;
        String[] p = linea.split("\\|", -1);
        if (p.length != 3) return null;
        try {
            return new Evento(Long.parseLong(p[0]), p[1], Decision.valueOf(p[2]));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Convierte una línea del registro de texto de la versión 1.1
     * ("dd/MM HH:mm  numero  etiqueta") en un evento. Ese formato no guardaba el año:
     * se asume el de {@code ahora}, o el anterior si la fecha quedaría en el futuro.
     * Devuelve null si la línea no se reconoce.
     */
    public static Evento migrar(String linea, long ahora, TimeZone tz) {
        if (linea == null) return null;
        String[] p = linea.trim().split(" {2}", 3);
        if (p.length != 3) return null;
        String[] fh = p[0].split(" ");
        if (fh.length != 2) return null;
        String[] dm = fh[0].split("/");
        String[] hm = fh[1].split(":");
        if (dm.length != 2 || hm.length != 2) return null;

        Decision d;
        String e = p[2];
        if (e.startsWith("oculto")) d = Decision.RECHAZAR_OCULTO;
        else if (e.startsWith("país no permitido")) d = Decision.SILENCIAR_PAIS;
        else if (e.startsWith("desconocido")) d = Decision.SILENCIAR_PRIMERA;
        else if (e.startsWith("modo domicilio")) d = Decision.SONAR_DOMICILIO;
        else if (e.startsWith("insistió")) d = Decision.SONAR_INSISTE;
        else return null;

        try {
            Calendar c = Calendar.getInstance(tz);
            c.setTimeInMillis(ahora);
            int anio = c.get(Calendar.YEAR);
            c.clear();
            c.set(anio, Integer.parseInt(dm[1]) - 1, Integer.parseInt(dm[0]),
                    Integer.parseInt(hm[0]), Integer.parseInt(hm[1]), 0);
            if (c.getTimeInMillis() > ahora + 24 * 3600_000L) c.add(Calendar.YEAR, -1);
            String n = p[1].equals("(oculto)") ? "" : Reglas.normalizar(p[1]);
            return new Evento(c.getTimeInMillis(), n, d);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
