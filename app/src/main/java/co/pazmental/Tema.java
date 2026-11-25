package co.pazmental;

import android.content.Context;
import android.content.res.Configuration;

import co.pazmental.Reglas.Decision;

/** Paleta de la interfaz: clara (blanca) u oscura (negra, para pantallas OLED). */
final class Tema {

    static final String SISTEMA = "sistema";
    static final String CLARO = "claro";
    static final String OSCURO = "oscuro";

    final boolean oscuro;
    final int fondo;
    final int tarjeta;
    final int tarjetaAlt;
    final int borde;
    final int texto;
    final int textoSuave;
    /** Tarjeta principal de Inicio. */
    final int heroe;
    final int sobreHeroe;
    final int sobreHeroeSuave;
    final int primario;
    final int sobrePrimario;
    final int primarioSuave;
    final int acento;
    final int rojo;
    final int ambar;
    final int azul;
    final int violeta;

    private Tema(boolean oscuro) {
        this.oscuro = oscuro;
        azul = oscuro ? 0xFF6EA2FF : 0xFF3B76F0;
        violeta = oscuro ? 0xFFB197FC : 0xFF7C5CDB;
        if (oscuro) {
            fondo = 0xFF000000;
            tarjeta = 0xFF0F1514;
            tarjetaAlt = 0xFF1A2321;
            borde = 0xFF1F2927;
            texto = 0xFFEFF4F3;
            textoSuave = 0xFF8D9A97;
            heroe = 0xFF0D3833;
            sobreHeroe = 0xFFFFFFFF;
            sobreHeroeSuave = 0xB3FFFFFF;
            primario = 0xFF3DBFA8;
            sobrePrimario = 0xFF03201B;
            primarioSuave = 0xFF2A6E64;
            acento = 0xFF34D399;
            rojo = 0xFFF2676B;
            ambar = 0xFFF5B54F;
        } else {
            fondo = 0xFFEEF2F1;
            tarjeta = 0xFFFFFFFF;
            tarjetaAlt = 0xFFF1F4F3;
            borde = 0xFFE0E6E4;
            texto = 0xFF0E1A18;
            textoSuave = 0xFF67736F;
            heroe = 0xFF124A44;
            sobreHeroe = 0xFFFFFFFF;
            sobreHeroeSuave = 0xB3FFFFFF;
            primario = 0xFF124A44;
            sobrePrimario = 0xFFFFFFFF;
            primarioSuave = 0xFF8CBDB4;
            acento = 0xFF22C55E;
            rojo = 0xFFE5484D;
            ambar = 0xFFEFA23A;
        }
    }

    /** Paleta según la preferencia guardada; "sistema" sigue el modo oscuro del teléfono. */
    static Tema para(Context c, String preferencia) {
        boolean o;
        if (OSCURO.equals(preferencia)) o = true;
        else if (CLARO.equals(preferencia)) o = false;
        else o = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        return new Tema(o);
    }

    /** Color con que se pinta cada resultado en gráficos y etiquetas. */
    int color(Decision d) {
        switch (d) {
            case RECHAZAR_OCULTO: return rojo;
            case SILENCIAR_PAIS: return primarioSuave;
            case SILENCIAR_PRIMERA: return primario;
            case SONAR_DOMICILIO: return acento;
            case SONAR_CONFIANZA: return azul;
            case SILENCIAR_BLOQUEADO: return textoSuave;
            case SILENCIAR_DESCANSO: return violeta;
            default: return ambar;
        }
    }

    /** El mismo color con otra opacidad (0–1). */
    static int alfa(int color, float a) {
        return (Math.round(a * 255) << 24) | (color & 0x00FFFFFF);
    }

    /** Mezcla lineal entre dos colores. */
    static int mezclar(int de, int a, float f) {
        float g = 1 - f;
        int al = Math.round(((de >>> 24) & 0xFF) * g + ((a >>> 24) & 0xFF) * f);
        int r = Math.round(((de >> 16) & 0xFF) * g + ((a >> 16) & 0xFF) * f);
        int v = Math.round(((de >> 8) & 0xFF) * g + ((a >> 8) & 0xFF) * f);
        int b = Math.round((de & 0xFF) * g + (a & 0xFF) * f);
        return (al << 24) | (r << 16) | (v << 8) | b;
    }
}
