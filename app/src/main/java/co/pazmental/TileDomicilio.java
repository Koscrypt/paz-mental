package co.pazmental;

import android.content.ComponentName;
import android.content.Context;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Botón de ajustes rápidos para el modo domicilio. Un toque lo abre con el país y la duración
 * que se usaron la última vez (Colombia, 30 min si nunca se abrió); otro toque lo cierra.
 */
public class TileDomicilio extends TileService {

    @Override
    public void onStartListening() {
        pintar();
    }

    @Override
    public void onClick() {
        Almacen a = new Almacen(this);
        long ahora = System.currentTimeMillis();
        if (Reglas.domicilioActivo(a.domicilioPais(), a.domicilioFin(), ahora)) {
            a.cerrarDomicilio();
        } else {
            a.abrirDomicilio(a.ultimoPaisDomicilio(), ahora, ahora + a.ultimaDuracionDomicilio() * 60_000L);
        }
        pintar();
    }

    private void pintar() {
        Tile t = getQsTile();
        if (t == null) return;
        Almacen a = new Almacen(this);
        long ahora = System.currentTimeMillis();
        String pd = a.domicilioPais();
        boolean abierto = Reglas.domicilioActivo(pd, a.domicilioFin(), ahora);
        t.setLabel("Modo domicilio");
        if (abierto) {
            String hora = new SimpleDateFormat("h:mm a", Locale.forLanguageTag("es-CO"))
                    .format(new Date(a.domicilioFin()));
            t.setState(Tile.STATE_ACTIVE);
            t.setSubtitle(Reglas.nombrePais(pd) + " · hasta " + hora);
        } else {
            int m = a.ultimaDuracionDomicilio();
            t.setState(Tile.STATE_INACTIVE);
            t.setSubtitle(Reglas.nombrePais(a.ultimoPaisDomicilio()) + " · " + (m < 60 ? m + " min" : (m / 60) + " h"));
        }
        t.setContentDescription("Modo domicilio, " + t.getSubtitle());
        t.updateTile();
    }

    /** Pide a Android que vuelva a pintar el botón (tras abrir o cerrar el modo desde la app). */
    static void refrescar(Context c) {
        try {
            TileService.requestListeningState(c, new ComponentName(c, TileDomicilio.class));
        } catch (RuntimeException ignored) {
            // Sin el botón agregado no hay nada que refrescar.
        }
    }
}
