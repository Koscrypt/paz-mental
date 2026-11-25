package co.pazmental;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/** Gráficos e íconos dibujados a mano: la app no tiene dependencias. */
final class Graficos {

    private Graficos() {}

    static float dp(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }

    static float sp(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, c.getResources().getDisplayMetrics());
    }

    private static Paint pincel(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        return p;
    }

    // ---------------------------------------------------------------- Ícono

    /** Ícono de trazo dibujado sobre una rejilla de 24 × 24. */
    static final class Icono extends View {
        static final int SOL = 0, LUNA = 1, AUTO = 2, INICIO = 3, GRAFICO = 4, AJUSTES = 5,
                DESCARGA = 6, FLECHA = 7, ESCUDO = 8, LOGO = 9;

        private final int tipo;
        private int color;
        private final int fondoLogo;
        private final Paint trazo = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path p = new Path();

        Icono(Context c, int tipo, int color) {
            this(c, tipo, color, 0);
        }

        Icono(Context c, int tipo, int color, int fondoLogo) {
            super(c);
            this.tipo = tipo;
            this.color = color;
            this.fondoLogo = fondoLogo;
            trazo.setStyle(Paint.Style.STROKE);
            trazo.setStrokeCap(Paint.Cap.ROUND);
            trazo.setStrokeJoin(Paint.Join.ROUND);
        }

        void setColor(int c) {
            color = c;
            invalidate();
        }

        @Override protected void onDraw(Canvas cv) {
            float e = Math.min(getWidth(), getHeight()) / 24f;
            cv.save();
            cv.translate((getWidth() - 24 * e) / 2f, (getHeight() - 24 * e) / 2f);
            cv.scale(e, e);
            trazo.setColor(color);
            trazo.setStrokeWidth(tipo == LOGO ? 0 : 2f);
            relleno.setColor(color);
            p.reset();
            switch (tipo) {
                case SOL:
                    cv.drawCircle(12, 12, 4.2f, trazo);
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI / 4 * i;
                        cv.drawLine(12 + (float) Math.cos(a) * 7.5f, 12 + (float) Math.sin(a) * 7.5f,
                                12 + (float) Math.cos(a) * 9.6f, 12 + (float) Math.sin(a) * 9.6f, trazo);
                    }
                    break;
                case LUNA:
                    luna(cv, 12, 12, 8.5f, relleno);
                    break;
                case AUTO:
                    cv.drawCircle(12, 12, 8.5f, trazo);
                    p.addArc(new RectF(3.5f, 3.5f, 20.5f, 20.5f), 90, 180);
                    p.close();
                    cv.drawPath(p, relleno);
                    break;
                case INICIO:
                    p.moveTo(4, 11);
                    p.lineTo(12, 4.5f);
                    p.lineTo(20, 11);
                    p.moveTo(6.5f, 9.5f);
                    p.lineTo(6.5f, 19.5f);
                    p.lineTo(17.5f, 19.5f);
                    p.lineTo(17.5f, 9.5f);
                    p.moveTo(10.5f, 19.5f);
                    p.lineTo(10.5f, 15);
                    p.lineTo(13.5f, 15);
                    p.lineTo(13.5f, 19.5f);
                    cv.drawPath(p, trazo);
                    break;
                case GRAFICO:
                    trazo.setStrokeWidth(2.6f);
                    cv.drawLine(6, 19.5f, 6, 13, trazo);
                    cv.drawLine(12, 19.5f, 12, 5, trazo);
                    cv.drawLine(18, 19.5f, 18, 10, trazo);
                    break;
                case AJUSTES:
                    cv.drawLine(4, 8, 20, 8, trazo);
                    cv.drawLine(4, 16, 20, 16, trazo);
                    cv.drawCircle(9, 8, 2.6f, relleno);
                    cv.drawCircle(15, 16, 2.6f, relleno);
                    break;
                case DESCARGA:
                    p.moveTo(12, 4);
                    p.lineTo(12, 14.5f);
                    p.moveTo(7.5f, 10.5f);
                    p.lineTo(12, 15);
                    p.lineTo(16.5f, 10.5f);
                    p.moveTo(5, 19.5f);
                    p.lineTo(19, 19.5f);
                    cv.drawPath(p, trazo);
                    break;
                case FLECHA:
                    p.moveTo(7, 17);
                    p.lineTo(17, 7);
                    p.moveTo(9, 7);
                    p.lineTo(17, 7);
                    p.lineTo(17, 15);
                    cv.drawPath(p, trazo);
                    break;
                case ESCUDO:
                    p.moveTo(12, 3.5f);
                    p.lineTo(19, 6.3f);
                    p.lineTo(19, 11);
                    p.cubicTo(19, 15.8f, 16, 19, 12, 20.5f);
                    p.cubicTo(8, 19, 5, 15.8f, 5, 11);
                    p.lineTo(5, 6.3f);
                    p.close();
                    p.moveTo(9, 12);
                    p.lineTo(11.2f, 14.2f);
                    p.lineTo(15.2f, 10.2f);
                    cv.drawPath(p, trazo);
                    break;
                case LOGO:
                    Paint f = pincel(fondoLogo);
                    cv.drawRoundRect(new RectF(0, 0, 24, 24), 7.5f, 7.5f, f);
                    luna(cv, 12, 12, 6.6f, relleno);
                    cv.drawCircle(16.4f, 7.2f, 1.1f, relleno);
                    break;
                default:
                    break;
            }
            cv.restore();
        }

        private void luna(Canvas cv, float x, float y, float r, Paint pt) {
            Path a = new Path();
            a.addCircle(x, y, r, Path.Direction.CW);
            Path b = new Path();
            b.addCircle(x + r * 0.55f, y - r * 0.45f, r * 0.82f, Path.Direction.CW);
            a.op(b, Path.Op.DIFFERENCE);
            cv.drawPath(a, pt);
        }
    }

    // ---------------------------------------------------------------- Barras por periodo

    /** Barras tipo píldora; la elegida va en color sólido con su cifra encima. Se toca para elegir. */
    static final class Barras extends View {
        interface AlElegir { void elegida(int i); }

        private final Tema t;
        private int[] valores = new int[0];
        private String[] etiquetas = new String[0];
        private int elegida = -1;
        private AlElegir oyente;
        private final Paint barra = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint burbuja = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Barras(Context c, Tema t) {
            super(c);
            this.t = t;
            borde.setStyle(Paint.Style.STROKE);
            borde.setStrokeWidth(dp(c, 1));
            txt.setTextAlign(Paint.Align.CENTER);
        }

        void datos(int[] v, String[] etq, int elegida, AlElegir oyente) {
            valores = v;
            etiquetas = etq;
            this.elegida = elegida;
            this.oyente = oyente;
            invalidate();
        }

        private float anchoBarra() {
            int n = Math.max(1, valores.length);
            return Math.min(dp(getContext(), 30), getWidth() / (n + (n - 1) * 0.4f));
        }

        private float paso() {
            int n = Math.max(1, valores.length);
            float w = anchoBarra();
            return n == 1 ? 0 : (getWidth() - w) / (n - 1);
        }

        @Override protected void onDraw(Canvas cv) {
            Context c = getContext();
            int n = valores.length;
            if (n == 0) return;
            int max = 1;
            for (int v : valores) max = Math.max(max, v);
            float arriba = dp(c, 34), abajo = dp(c, 22);
            float alto = getHeight() - arriba - abajo;
            float w = anchoBarra(), paso = paso();
            int cada = n <= 8 ? 1 : (n <= 14 ? 2 : 5);

            for (int i = 0; i < n; i++) {
                float x = i * paso;
                float h = Math.max(w, alto * valores[i] / max);
                float y = arriba + alto - h;
                r.set(x, y, x + w, arriba + alto);
                if (i == elegida) {
                    barra.setShader(null);
                    barra.setColor(t.primario);
                } else {
                    float a0 = valores[i] == 0 ? 0.18f : 0.55f;
                    barra.setShader(new LinearGradient(0, y, 0, arriba + alto,
                            Tema.alfa(t.primarioSuave, a0), Tema.alfa(t.primarioSuave, 0.10f),
                            Shader.TileMode.CLAMP));
                }
                cv.drawRoundRect(r, w / 2, w / 2, barra);

                boolean rotular = i == elegida || (n - 1 - i) % cada == 0;
                if (rotular && etiquetas[i] != null) {
                    txt.setTextSize(sp(c, 10.5f));
                    txt.setColor(i == elegida ? t.texto : t.textoSuave);
                    txt.setTypeface(i == elegida ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                    cv.drawText(etiquetas[i], x + w / 2, getHeight() - dp(c, 5), txt);
                }
            }

            if (elegida >= 0 && elegida < n) {
                float x = elegida * paso + w / 2;
                float h = Math.max(w, alto * valores[elegida] / max);
                float y = arriba + alto - h;
                burbuja.setColor(t.tarjeta);
                borde.setColor(t.primario);
                cv.drawCircle(x, y + w / 2, dp(c, 3.2f), burbuja);
                cv.drawCircle(x, y + w / 2, dp(c, 3.2f), borde);

                String s = String.valueOf(valores[elegida]);
                txt.setTextSize(sp(c, 13));
                txt.setTypeface(Typeface.DEFAULT_BOLD);
                float bw = Math.max(dp(c, 34), txt.measureText(s) + dp(c, 18));
                float bx = Math.max(0, Math.min(getWidth() - bw, x - bw / 2));
                float by = Math.max(0, y - dp(c, 32));
                r.set(bx, by, bx + bw, by + dp(c, 24));
                burbuja.setColor(t.tarjetaAlt);
                cv.drawRoundRect(r, dp(c, 12), dp(c, 12), burbuja);
                txt.setColor(t.texto);
                cv.drawText(s, r.centerX(), r.centerY() + txt.getTextSize() * 0.35f, txt);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (valores.length == 0) return false;
            if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                getParent().requestDisallowInterceptTouchEvent(e.getAction() == MotionEvent.ACTION_MOVE);
                float w = anchoBarra(), paso = paso();
                int i = paso == 0 ? 0 : Math.round((e.getX() - w / 2) / paso);
                i = Math.max(0, Math.min(valores.length - 1, i));
                if (i != elegida) {
                    elegida = i;
                    invalidate();
                    if (oyente != null) oyente.elegida(i);
                }
                return true;
            }
            return e.getAction() == MotionEvent.ACTION_UP || super.onTouchEvent(e);
        }
    }

    // ---------------------------------------------------------------- Distribución

    /** Una franja de rayitas verticales: cada tramo del color de un resultado. */
    static final class Rayitas extends View {
        private final Tema t;
        private int[] valores = new int[0];
        private int[] colores = new int[0];
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Rayitas(Context c, Tema t) {
            super(c);
            this.t = t;
            p.setStrokeCap(Paint.Cap.ROUND);
        }

        void datos(int[] valores, int[] colores) {
            this.valores = valores;
            this.colores = colores;
            invalidate();
        }

        @Override protected void onDraw(Canvas cv) {
            Context c = getContext();
            float grosor = dp(c, 3.2f), hueco = dp(c, 3.2f);
            p.setStrokeWidth(grosor);
            int n = (int) ((getWidth() + hueco) / (grosor + hueco));
            int total = 0;
            for (int v : valores) total += v;
            float y0 = grosor / 2, y1 = getHeight() - grosor / 2;
            for (int i = 0; i < n; i++) {
                float x = grosor / 2 + i * (grosor + hueco);
                int color = t.borde;
                if (total > 0) {
                    float f = (i + 0.5f) / n * total, acum = 0;
                    for (int k = 0; k < valores.length; k++) {
                        acum += valores[k];
                        if (f < acum) { color = colores[k]; break; }
                    }
                }
                p.setColor(color);
                cv.drawLine(x, y0, x, y1, p);
            }
        }
    }

    // ---------------------------------------------------------------- Mapa de calor

    /** 7 días × 8 franjas de 3 horas. Se toca una celda para ver su cifra. */
    static final class MapaCalor extends View {
        interface AlElegir { void elegida(int dia, int franja); }

        private static final String[] DIAS = {"L", "M", "X", "J", "V", "S", "D"};
        private final Tema t;
        private int[][] v = new int[7][8];
        private int max = 0;
        private int selDia = -1, selFranja = -1;
        private AlElegir oyente;
        private final Paint celda = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint marco = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        MapaCalor(Context c, Tema t) {
            super(c);
            this.t = t;
            txt.setTextAlign(Paint.Align.CENTER);
            marco.setStyle(Paint.Style.STROKE);
            marco.setStrokeWidth(dp(c, 2));
        }

        void datos(int[][] v, int max, AlElegir oyente) {
            this.v = v;
            this.max = max;
            this.oyente = oyente;
            invalidate();
        }

        private float izq() { return dp(getContext(), 22); }
        private float abajo() { return dp(getContext(), 20); }

        @Override protected void onDraw(Canvas cv) {
            Context c = getContext();
            float gap = dp(c, 4);
            float cw = (getWidth() - izq()) / 8f, ch = (getHeight() - abajo()) / 7f;
            txt.setTextSize(sp(c, 10.5f));
            txt.setColor(t.textoSuave);
            for (int d = 0; d < 7; d++) {
                float y = d * ch;
                cv.drawText(DIAS[d], izq() / 2 - dp(c, 3), y + ch / 2 + txt.getTextSize() * 0.35f, txt);
                for (int f = 0; f < 8; f++) {
                    float x = izq() + f * cw;
                    r.set(x + gap / 2, y + gap / 2, x + cw - gap / 2, y + ch - gap / 2);
                    int val = v[d][f];
                    celda.setColor(val == 0 || max == 0 ? t.tarjetaAlt
                            : Tema.alfa(t.primario, 0.22f + 0.78f * val / max));
                    float rad = dp(c, 6);
                    cv.drawRoundRect(r, rad, rad, celda);
                    if (d == selDia && f == selFranja) {
                        marco.setColor(t.texto);
                        cv.drawRoundRect(r, rad, rad, marco);
                    }
                }
            }
            for (int f = 0; f < 8; f += 2) {
                cv.drawText((f * 3) + "h", izq() + f * cw + cw / 2, getHeight() - dp(c, 4), txt);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                float cw = (getWidth() - izq()) / 8f, ch = (getHeight() - abajo()) / 7f;
                int f = (int) ((e.getX() - izq()) / cw), d = (int) (e.getY() / ch);
                if (f >= 0 && f < 8 && d >= 0 && d < 7) {
                    selDia = d;
                    selFranja = f;
                    invalidate();
                    if (oyente != null) oyente.elegida(d, f);
                }
                return true;
            }
            return super.onTouchEvent(e);
        }
    }

    // ---------------------------------------------------------------- Anillo

    /** Anillo de progreso con una cifra en el centro. */
    static final class Anillo extends View {
        private final int pista, color, texto, textoSuave;
        private float progreso;
        private String cifra = "", unidad = "";
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Anillo(Context c, int pista, int color, int texto, int textoSuave) {
            super(c);
            this.pista = pista;
            this.color = color;
            this.texto = texto;
            this.textoSuave = textoSuave;
            p.setStrokeCap(Paint.Cap.ROUND);
        }

        void datos(float progreso, String cifra, String unidad) {
            this.progreso = Math.max(0, Math.min(1, progreso));
            this.cifra = cifra;
            this.unidad = unidad;
            invalidate();
        }

        @Override protected void onDraw(Canvas cv) {
            Context c = getContext();
            float g = dp(c, 8), lado = Math.min(getWidth(), getHeight());
            r.set(g / 2, g / 2, lado - g / 2, lado - g / 2);
            r.offset((getWidth() - lado) / 2f, (getHeight() - lado) / 2f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(g);
            p.setColor(pista);
            cv.drawArc(r, 0, 360, false, p);
            p.setColor(color);
            cv.drawArc(r, -90, 360 * progreso, false, p);

            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(texto);
            p.setTextSize(sp(c, 24));
            p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            cv.drawText(cifra, r.centerX(), r.centerY() + dp(c, 3), p);
            p.setColor(textoSuave);
            p.setTextSize(sp(c, 11));
            p.setTypeface(Typeface.DEFAULT);
            cv.drawText(unidad, r.centerX(), r.centerY() + dp(c, 19), p);
        }
    }

    // ---------------------------------------------------------------- Interruptor

    /** Interruptor de encendido con los colores del tema; se anima al tocarlo. */
    static final class Interruptor extends View {
        interface AlCambiar { void cambio(boolean encendido); }

        private final Tema t;
        private boolean encendido;
        private float pos;
        private AlCambiar oyente;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Interruptor(Context c, Tema t, boolean encendido) {
            super(c);
            this.t = t;
            this.encendido = encendido;
            pos = encendido ? 1 : 0;
            setClickable(true);
            setFocusable(true);
            setOnClickListener(v -> {
                this.encendido = !this.encendido;
                android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(pos, this.encendido ? 1 : 0);
                a.setDuration(160);
                a.addUpdateListener(x -> { pos = (float) x.getAnimatedValue(); invalidate(); });
                a.start();
                sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED);
                if (oyente != null) oyente.cambio(this.encendido);
            });
        }

        void alCambiar(AlCambiar o) {
            oyente = o;
        }

        @Override protected void onDraw(Canvas cv) {
            float h = getHeight(), w = getWidth(), m = dp(getContext(), 3);
            int pistaApagada = Tema.mezclar(t.borde, t.textoSuave, t.oscuro ? 0.15f : 0.3f);
            r.set(0, 0, w, h);
            p.setColor(Tema.mezclar(pistaApagada, t.primario, pos));
            cv.drawRoundRect(r, h / 2, h / 2, p);
            float rad = h / 2 - m;
            float cx = m + rad + (w - 2 * m - 2 * rad) * pos;
            int perillaApagada = t.oscuro ? t.textoSuave : 0xFFFFFFFF;
            p.setColor(Tema.mezclar(perillaApagada, 0xFFFFFFFF, pos));
            cv.drawCircle(cx, h / 2, rad, p);
        }

        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(android.widget.Switch.class.getName());
            info.setCheckable(true);
            info.setChecked(encendido);
        }
    }

    // ---------------------------------------------------------------- Barra de fracción

    /** Barra horizontal delgada que muestra una proporción. */
    static final class Fraccion extends View {
        private final int pista, color;
        private final float f;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Fraccion(Context c, int pista, int color, float f) {
            super(c);
            this.pista = pista;
            this.color = color;
            this.f = Math.max(0, Math.min(1, f));
        }

        @Override protected void onDraw(Canvas cv) {
            float h = getHeight();
            r.set(0, 0, getWidth(), h);
            p.setColor(pista);
            cv.drawRoundRect(r, h / 2, h / 2, p);
            r.set(0, 0, Math.max(h, getWidth() * f), h);
            p.setColor(color);
            cv.drawRoundRect(r, h / 2, h / 2, p);
        }
    }
}
