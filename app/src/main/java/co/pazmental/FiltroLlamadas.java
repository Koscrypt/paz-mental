package co.pazmental;

import android.net.Uri;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.TelecomManager;

import java.util.Map;

/** Android lo llama antes de que timbre una llamada de un número que no está en contactos. */
public class FiltroLlamadas extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details d) {
        if (d.getCallDirection() != Call.Details.DIRECTION_INCOMING) {
            respondToCall(d, new CallResponse.Builder().build());
            return;
        }

        Uri h = d.getHandle();
        String numero = h == null ? "" : h.getSchemeSpecificPart();
        boolean oculto = d.getHandlePresentation() != TelecomManager.PRESENTATION_ALLOWED;

        Almacen a = new Almacen(this);
        long ahora = System.currentTimeMillis();
        Map<String, Long> intentos = a.intentos();
        Reglas.Decision dec = Reglas.decidir(numero, oculto, a.paises(), intentos, ahora,
                a.domicilioPais(), a.domicilioFin());
        a.guardarIntentos(intentos, ahora);

        CallResponse.Builder r = new CallResponse.Builder();
        String etiqueta;
        switch (dec) {
            case RECHAZAR_OCULTO:
                r.setDisallowCall(true).setRejectCall(true)
                        .setSkipCallLog(false).setSkipNotification(true);
                etiqueta = "oculto: rechazada";
                break;
            case SILENCIAR_PAIS:
                r.setSilenceCall(true);
                etiqueta = "país no permitido: silenciada";
                break;
            case SILENCIAR_PRIMERA:
                r.setSilenceCall(true);
                etiqueta = "desconocido: silenciada (si insiste en 5 min, suena)";
                break;
            case SONAR_DOMICILIO:
                etiqueta = "modo domicilio: SONÓ";
                break;
            default:
                etiqueta = "insistió: SONÓ";
        }
        respondToCall(d, r.build());
        a.anotar(oculto || numero.isEmpty() ? "(oculto)" : numero, etiqueta);
    }
}
