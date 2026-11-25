package co.pazmental;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import co.pazmental.Graficos.Icono;
import co.pazmental.Reglas.Decision;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class MainActivity extends Activity {

    private static final int PEDIR_ROL = 1;
    private static final int EXPORTAR = 2;
    private static final int INICIO = 0, ESTADISTICAS = 1, AJUSTES = 2;
    private static final int[] RANGOS = {7, 30, 90};
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    /** Orden en que se muestran los resultados en leyendas y gráficos. */
    private static final Decision[] ORDEN = {Decision.SILENCIAR_PRIMERA, Decision.SILENCIAR_PAIS,
            Decision.RECHAZAR_OCULTO, Decision.SONAR_INSISTE, Decision.SONAR_DOMICILIO};
    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private Almacen almacen;
    private Tema t;
    private ScrollView scroll;
    private LinearLayout contenido;
    private LinearLayout nav;
    private int pestana = INICIO;
    private int rango = 7;
    private int paisDomicilio = 0;
    private int barraElegida = -1;
    private final Runnable tic = this::pintar;

    // ================================================================= Ciclo de vida

    @Override
    protected void onCreate(Bundle b) {
        almacen = new Almacen(this);
        t = Tema.para(this, almacen.tema());
        setTheme(t.oscuro ? android.R.style.Theme_DeviceDefault_NoActionBar
                : android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        super.onCreate(b);
        if (b != null) {
            pestana = b.getInt("pestana", INICIO);
            rango = b.getInt("rango", 7);
            paisDomicilio = b.getInt("paisDomicilio", 0);
        }

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundColor(t.fondo);
        scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        contenido = new LinearLayout(this);
        contenido.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(contenido);
        raiz.addView(scroll);

        // Franja bajo la barra de estado: el contenido pasa por debajo sin tapar el reloj.
        View franja = new View(this);
        franja.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{t.fondo, t.fondo, Tema.alfa(t.fondo, 0f)}));
        FrameLayout.LayoutParams lpFranja = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.TOP);
        raiz.addView(franja, lpFranja);

        nav = construirNav();
        FrameLayout.LayoutParams lpNav = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        raiz.addView(nav, lpNav);

        raiz.setOnApplyWindowInsetsListener((v, ins) -> {
            android.graphics.Insets s = ins.getInsets(WindowInsets.Type.systemBars());
            contenido.setPadding(dp(18), s.top + dp(10), dp(18), s.bottom + dp(104));
            lpNav.bottomMargin = s.bottom + dp(14);
            nav.setLayoutParams(lpNav);
            lpFranja.height = s.top + dp(12);
            franja.setLayoutParams(lpFranja);
            return ins;
        });
        setContentView(raiz);

        // Barras del sistema transparentes, con íconos claros u oscuros según el tema.
        // Va después de setContentView: antes no existe la ventana y getInsetsController revienta.
        getWindow().setDecorFitsSystemWindows(false);
        getWindow().setNavigationBarContrastEnforced(false);
        int claras = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        WindowInsetsController wic = getWindow().getInsetsController();
        if (wic != null) wic.setSystemBarsAppearance(t.oscuro ? 0 : claras, claras);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pintar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        contenido.removeCallbacks(tic);
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putInt("pestana", pestana);
        b.putInt("rango", rango);
        b.putInt("paisDomicilio", paisDomicilio);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == EXPORTAR && res == RESULT_OK && data != null && data.getData() != null) {
            escribirCsv(data.getData());
        }
        pintar();
    }

    // ================================================================= Pintado

    private void irA(int p) {
        pestana = p;
        barraElegida = -1;
        pintar();
        scroll.post(() -> scroll.scrollTo(0, 0));
    }

    /** Vuelve a construir la pestaña actual con datos frescos. */
    private void pintar() {
        contenido.removeCallbacks(tic);
        int y = scroll.getScrollY();
        contenido.removeAllViews();
        long ahora = System.currentTimeMillis();
        List<Evento> eventos = almacen.eventos();
        encabezado(ahora);
        if (pestana == INICIO) pintarInicio(eventos, ahora);
        else if (pestana == ESTADISTICAS) pintarEstadisticas(eventos, ahora);
        else pintarAjustes(eventos);
        actualizarNav();
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    private void encabezado(long ahora) {
        LinearLayout fila = fila();
        fila.setPadding(0, dp(6), 0, dp(10));
        fila.addView(new Icono(this, Icono.LOGO, t.sobreHeroe, t.heroe), new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout col = columna();
        col.setPadding(dp(12), 0, 0, 0);
        col.addView(texto("Paz Mental", 20, t.texto, 1));
        col.addView(texto(Textos.fechaLarga(ahora, TimeZone.getDefault()), 13, t.textoSuave, 0));
        fila.addView(col, pesado());

        FrameLayout botonTema = circulo(t.tarjeta, 46);
        botonTema.addView(new Icono(this, t.oscuro ? Icono.SOL : Icono.LUNA, t.texto),
                new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        botonTema.setContentDescription(t.oscuro ? "Cambiar a tema blanco" : "Cambiar a tema negro");
        botonTema.setOnClickListener(v -> cambiarTema(t.oscuro ? Tema.CLARO : Tema.OSCURO));
        fila.addView(botonTema);
        contenido.addView(fila);
    }

    // ----------------------------------------------------------------- Inicio

    private void pintarInicio(List<Evento> eventos, long ahora) {
        Estadisticas s = Estadisticas.calcular(eventos, ahora, 7, TimeZone.getDefault());
        boolean activo = rolActivo();

        // Tarjeta principal
        LinearLayout h = columna();
        h.setBackground(redondeado(t.heroe, 28));
        h.setPadding(dp(20), dp(18), dp(20), dp(20));
        LinearLayout arriba = fila();
        LinearLayout estado = fila();
        estado.setBackground(redondeado(Tema.alfa(0xFFFFFFFF, 0.14f), 99));
        estado.setPadding(dp(10), dp(6), dp(12), dp(6));
        estado.addView(punto(activo ? t.acento : t.ambar, 8));
        TextView te = texto(activo ? "Activa · filtrando llamadas" : "Inactiva", 13, t.sobreHeroe, 1);
        te.setPadding(dp(8), 0, 0, 0);
        estado.addView(te);
        arriba.addView(estado);
        arriba.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        FrameLayout ir = circulo(Tema.alfa(0xFFFFFFFF, 0.14f), 40);
        ir.addView(new Icono(this, Icono.FLECHA, t.sobreHeroe), new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
        ir.setContentDescription("Ver estadísticas");
        ir.setOnClickListener(v -> irA(ESTADISTICAS));
        arriba.addView(ir);
        h.addView(arriba);

        TextView et = texto("Silenciadas hoy", 14, t.sobreHeroeSuave, 0);
        et.setPadding(0, dp(18), 0, dp(4));
        h.addView(et);
        h.addView(cifra(String.valueOf(s.hoyBloqueadas), s.hoyBloqueadas == 1 ? "llamada" : "llamadas",
                54, t.sobreHeroe, t.sobreHeroeSuave));
        TextView sem = texto("Esta semana: " + s.bloqueadas + " silenciadas · " + s.sonaron
                + (s.sonaron == 1 ? " sonó" : " sonaron"), 14, t.sobreHeroeSuave, 0);
        sem.setPadding(0, dp(8), 0, 0);
        h.addView(sem);
        if (!activo) {
            TextView expl = texto("Para filtrar, Android debe usar Paz Mental como app de identificación "
                    + "de llamadas y spam.", 14, t.sobreHeroeSuave, 0);
            expl.setPadding(0, dp(14), 0, dp(12));
            h.addView(expl);
            h.addView(boton("Activar Paz Mental", t.sobreHeroe, t.heroe, v -> pedirRol()), anchoCompleto());
        }
        contenido.addView(h);

        contenido.addView(parIndicadores(
                indicador("Filtradas · 7 días", String.valueOf(s.total), "llamadas", cambio(s)),
                indicador("Sonaron · 7 días", String.valueOf(s.sonaron), "de " + s.total, null)));

        tarjetaDomicilio(ahora);

        // Últimas llamadas
        LinearLayout u = tarjeta();
        LinearLayout tu = fila();
        tu.addView(titulo("Últimas llamadas"), pesado());
        if (!eventos.isEmpty()) {
            TextView todo = texto("Ver todo", 14, t.primario, 1);
            todo.setPadding(dp(8), dp(4), 0, dp(4));
            todo.setOnClickListener(v -> irA(ESTADISTICAS));
            tu.addView(todo);
        }
        u.addView(tu);
        if (eventos.isEmpty()) {
            u.addView(vacio("Sin llamadas filtradas todavía. Cuando Paz Mental silencie una, aparece aquí."));
        } else {
            for (int i = 0; i < Math.min(4, eventos.size()); i++) u.addView(filaEvento(eventos.get(i), ahora));
        }
        contenido.addView(u);

        // Reglas
        LinearLayout r = tarjeta();
        r.addView(titulo("Cómo decide"));
        r.addView(regla(t.acento, "Tus contactos suenan siempre."));
        r.addView(regla(t.primario, "Un desconocido de un país permitido se silencia la primera vez."));
        r.addView(regla(t.ambar, "Si vuelve a llamar en menos de 5 minutos, suena."));
        r.addView(regla(t.primarioSuave, "Los países que no permitiste se silencian siempre."));
        r.addView(regla(t.rojo, "Los números ocultos se rechazan."));
        contenido.addView(r);
    }

    private void tarjetaDomicilio(long ahora) {
        LinearLayout d = tarjeta();
        String pd = almacen.domicilioPais();
        long fin = almacen.domicilioFin();
        boolean abierto = Reglas.domicilioActivo(pd, fin, ahora);
        if (!abierto && !pd.isEmpty()) almacen.cerrarDomicilio();

        LinearLayout td = fila();
        td.addView(titulo("Modo domicilio"), pesado());
        td.addView(chip(abierto ? "Abierto" : "Cerrado", abierto ? t.acento : t.textoSuave));
        d.addView(td);

        if (abierto) {
            long inicio = almacen.domicilioInicio();
            long resta = fin - ahora;
            float prog = inicio > 0 && fin > inicio ? (float) resta / (fin - inicio) : 1f;
            int min = (int) Math.ceil(resta / 60_000.0);

            LinearLayout cuerpo = fila();
            cuerpo.setPadding(0, dp(14), 0, 0);
            Graficos.Anillo an = new Graficos.Anillo(this, t.tarjetaAlt, t.acento, t.texto, t.textoSuave);
            if (min >= 60) an.datos(prog, (min / 60) + ":" + String.format(Locale.ROOT, "%02d", min % 60), "horas");
            else an.datos(prog, String.valueOf(min), "min");
            cuerpo.addView(an, new LinearLayout.LayoutParams(dp(96), dp(96)));
            LinearLayout info = columna();
            info.setPadding(dp(16), 0, 0, 0);
            info.addView(texto(Textos.bandera(pd) + "  " + Reglas.nombrePais(pd), 17, t.texto, 1));
            String hora = new SimpleDateFormat("h:mm a", ES).format(new Date(fin));
            info.addView(texto("Suenan a la primera hasta las " + hora, 14, t.textoSuave, 0));
            TextView terminar = boton("Terminar ahora", t.tarjetaAlt, t.rojo, v -> {
                almacen.cerrarDomicilio();
                pintar();
            });
            LinearLayout.LayoutParams lf = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lf.topMargin = dp(10);
            info.addView(terminar, lf);
            cuerpo.addView(info, pesado());
            d.addView(cuerpo);
            contenido.postDelayed(tic, Math.min(30_000L, resta + 500));
        } else {
            TextView ex = texto("¿Esperas un domicilio? Elige el país y por cuánto tiempo: los desconocidos "
                    + "de ese país suenan a la primera. Los ocultos se siguen rechazando.", 14, t.textoSuave, 0);
            ex.setPadding(0, dp(6), 0, dp(12));
            d.addView(ex);
            d.addView(selectorPais());
            LinearLayout dur = fila();
            dur.setPadding(0, dp(10), 0, 0);
            int[] ds = Reglas.DURACIONES_DOMICILIO_MIN;
            for (int i = 0; i < ds.length; i++) {
                int m = ds[i];
                TextView b = boton(m < 60 ? m + " min" : (m / 60) + " h", t.tarjetaAlt, t.texto, v -> {
                    long a = System.currentTimeMillis();
                    almacen.abrirDomicilio(Reglas.PAISES[paisDomicilio][0], a, a + m * 60_000L);
                    pintar();
                });
                b.setPadding(0, dp(13), 0, dp(13));
                LinearLayout.LayoutParams lb = pesado();
                if (i > 0) lb.leftMargin = dp(8);
                dur.addView(b, lb);
            }
            d.addView(dur);
        }
        contenido.addView(d);
    }

    private View selectorPais() {
        String[] nombres = new String[Reglas.PAISES.length];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = Textos.bandera(Reglas.PAISES[i][0]) + "  " + Reglas.PAISES[i][1]
                    + "  (+" + Reglas.PAISES[i][0] + ")";
        }
        Spinner sp = new Spinner(this, Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, nombres) {
            @Override public View getView(int pos, View v, ViewGroup p) {
                TextView tv = (TextView) super.getView(pos, v, p);
                tv.setTextColor(t.texto);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tv.setPadding(dp(16), dp(14), dp(40), dp(14));
                return tv;
            }

            @Override public View getDropDownView(int pos, View v, ViewGroup p) {
                TextView tv = (TextView) super.getDropDownView(pos, v, p);
                tv.setTextColor(t.texto);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tv.setPadding(dp(16), dp(12), dp(16), dp(12));
                return tv;
            }
        };
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        sp.setSelection(paisDomicilio);
        sp.setBackground(redondeado(t.tarjetaAlt, 16));
        sp.setPopupBackgroundDrawable(redondeado(t.tarjeta, 16));
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                paisDomicilio = pos;
            }

            @Override public void onNothingSelected(AdapterView<?> a) {}
        });
        FrameLayout marco = new FrameLayout(this);
        marco.addView(sp, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams lf = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
        lf.rightMargin = dp(16);
        marco.addView(texto("▾", 16, t.textoSuave, 0), lf);
        return marco;
    }

    // ----------------------------------------------------------------- Estadísticas

    private void pintarEstadisticas(List<Evento> eventos, long ahora) {
        TimeZone tz = TimeZone.getDefault();
        Estadisticas s = Estadisticas.calcular(eventos, ahora, rango, tz);

        TextView tit = texto("Estadísticas", 30, t.texto, 1);
        tit.setPadding(0, dp(6), 0, 0);
        contenido.addView(tit);
        SimpleDateFormat f = new SimpleDateFormat("d MMM", ES);
        contenido.addView(texto("Últimos " + rango + " días · del " + sinPunto(f.format(new Date(s.desde)))
                + " al " + sinPunto(f.format(new Date(ahora))), 14, t.textoSuave, 0));

        // Selector de periodo
        LinearLayout seg = fila();
        seg.setBackground(redondeado(t.tarjeta, 99));
        seg.setPadding(dp(5), dp(5), dp(5), dp(5));
        for (int r : RANGOS) {
            boolean sel = r == rango;
            TextView o = texto(r + " días", 14, sel ? t.sobrePrimario : t.textoSuave, 1);
            o.setGravity(Gravity.CENTER);
            o.setPadding(0, dp(9), 0, dp(9));
            o.setBackground(sel ? redondeado(t.primario, 99) : conToque(redondeado(0, 99), 99));
            o.setOnClickListener(v -> { rango = r; barraElegida = -1; pintar(); });
            seg.addView(o, pesado());
        }
        LinearLayout.LayoutParams ls = anchoCompleto();
        ls.topMargin = dp(14);
        contenido.addView(seg, ls);

        // Exportar
        LinearLayout exp = fila();
        exp.setGravity(Gravity.CENTER);
        exp.setBackground(conToque(redondeado(t.primario, 99), 99));
        exp.setPadding(0, dp(15), 0, dp(15));
        exp.addView(new Icono(this, Icono.DESCARGA, t.sobrePrimario), new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView te = texto("Exportar CSV", 16, t.sobrePrimario, 1);
        te.setPadding(dp(10), 0, 0, 0);
        exp.addView(te);
        exp.setOnClickListener(v -> exportar());
        exp.setEnabled(s.total > 0);
        exp.setAlpha(s.total > 0 ? 1f : 0.45f);
        LinearLayout.LayoutParams le = anchoCompleto();
        le.topMargin = dp(12);
        contenido.addView(exp, le);

        // Indicadores 2 × 2
        float porDia = (float) s.total / rango;
        contenido.addView(parIndicadores(
                indicador("Filtradas", String.valueOf(s.total), "llamadas", cambio(s)),
                indicador("Silenciadas", String.valueOf(s.bloqueadas - s.ocultas), "no timbraron", null)));
        contenido.addView(parIndicadores(
                indicador("Sonaron", String.valueOf(s.sonaron), "timbraron", null),
                indicador("Promedio", String.format(ES, porDia < 10 ? "%.1f" : "%.0f", porDia), "por día", null)));

        // Qué pasó con cada llamada
        LinearLayout dist = tarjeta();
        dist.addView(titulo("Qué pasó con cada llamada"));
        Graficos.Rayitas ray = new Graficos.Rayitas(this, t);
        int[] vals = new int[ORDEN.length], cols = new int[ORDEN.length];
        for (int i = 0; i < ORDEN.length; i++) {
            vals[i] = s.porDecision[ORDEN[i].ordinal()];
            cols[i] = t.color(ORDEN[i]);
        }
        ray.datos(vals, cols);
        LinearLayout.LayoutParams lr = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        lr.topMargin = dp(14);
        lr.bottomMargin = dp(6);
        dist.addView(ray, lr);
        for (int i = 0; i < ORDEN.length; i++) {
            int pct = s.total == 0 ? 0 : Math.round(vals[i] * 100f / s.total);
            dist.addView(filaLeyenda(cols[i], Textos.etiqueta(ORDEN[i]), vals[i], pct));
        }
        contenido.addView(dist);

        // Serie por día o por semana
        LinearLayout ser = tarjeta();
        LinearLayout ts = fila();
        ts.addView(titulo(s.semanal ? "Por semana" : "Por día"), pesado());
        TextView detalle = texto("", 13, t.textoSuave, 0);
        ts.addView(detalle);
        ser.addView(ts);
        int n = s.serie.length;
        String[] etq = new String[n];
        String[] largas = new String[n];
        SimpleDateFormat fd = new SimpleDateFormat("EEE", ES);
        SimpleDateFormat fn = new SimpleDateFormat("d", ES);
        SimpleDateFormat fl = new SimpleDateFormat("EEE d MMM", ES);
        Calendar c = Calendar.getInstance(tz);
        for (int i = 0; i < n; i++) {
            c.setTimeInMillis(s.desde);
            c.add(Calendar.DAY_OF_MONTH, s.semanal ? i * 7 : i);
            Date dd = c.getTime();
            if (s.semanal) {
                etq[i] = fn.format(dd);
                largas[i] = (i == n - 1 ? "Esta semana" : "Semana del " + sinPunto(f.format(dd)));
            } else {
                String dia = sinPunto(fd.format(dd));
                etq[i] = n <= 7 ? dia.substring(0, 1).toUpperCase(ES) + dia.substring(1) : fn.format(dd);
                largas[i] = i == n - 1 ? "Hoy" : sinPunto(fl.format(dd));
            }
        }
        Graficos.Barras barras = new Graficos.Barras(this, t);
        Runnable rotular = () -> {
            int i = barraElegida >= 0 && barraElegida < n ? barraElegida : n - 1;
            detalle.setText(largas[i] + " · " + s.serie[i] + (s.serie[i] == 1 ? " llamada" : " llamadas"));
        };
        barras.datos(s.serie, etq, barraElegida >= 0 && barraElegida < n ? barraElegida : n - 1,
                i -> { barraElegida = i; rotular.run(); });
        rotular.run();
        LinearLayout.LayoutParams lbar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(190));
        lbar.topMargin = dp(10);
        ser.addView(barras, lbar);
        contenido.addView(ser);

        // Mapa de calor
        LinearLayout cal = tarjeta();
        cal.addView(titulo("A qué hora llaman"));
        TextView pista = texto("", 14, t.textoSuave, 0);
        pista.setPadding(0, dp(4), 0, 0);
        cal.addView(pista);
        int mDia = 0, mFr = 0, mx = 0;
        for (int dd = 0; dd < 7; dd++) {
            for (int fr = 0; fr < 8; fr++) {
                if (s.calor[dd][fr] > mx) { mx = s.calor[dd][fr]; mDia = dd; mFr = fr; }
            }
        }
        pista.setText(mx == 0 ? "Sin datos en este periodo."
                : "Pico: " + DIAS[mDia].toLowerCase(ES) + " de " + (mFr * 3) + " a " + (mFr * 3 + 3)
                + " h · " + mx + (mx == 1 ? " llamada" : " llamadas") + ". Toca una celda.");
        Graficos.MapaCalor mapa = new Graficos.MapaCalor(this, t);
        mapa.datos(s.calor, s.maxCalor(), (dd, fr) -> pista.setText(DIAS[dd] + " de " + (fr * 3) + " a "
                + (fr * 3 + 3) + " h · " + s.calor[dd][fr] + (s.calor[dd][fr] == 1 ? " llamada" : " llamadas")));
        LinearLayout.LayoutParams lm = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(230));
        lm.topMargin = dp(12);
        cal.addView(mapa, lm);
        contenido.addView(cal);

        // Países
        LinearLayout pa = tarjeta();
        pa.addView(titulo("De dónde llaman"));
        if (s.paises.isEmpty()) {
            pa.addView(vacio("Sin números visibles en este periodo."));
        } else {
            int tope = s.paises.get(0).n;
            for (Estadisticas.Conteo k : s.paises) {
                String nombre = k.clave.equals("?") ? "🌐  Otros países"
                        : Textos.bandera(k.clave) + "  " + Reglas.nombrePais(k.clave);
                pa.addView(filaBarra(nombre, k.n, (float) k.n / tope));
            }
        }
        contenido.addView(pa);

        // Insistentes
        LinearLayout in = tarjeta();
        in.addView(titulo("Los que más insisten"));
        if (s.insistentes.isEmpty()) {
            in.addView(vacio("Ningún número llamó más de una vez en este periodo."));
        } else {
            for (Estadisticas.Conteo k : s.insistentes) {
                LinearLayout fi = fila();
                fi.setPadding(0, dp(12), 0, 0);
                fi.addView(insignia(Textos.bandera(Reglas.pais(k.clave))));
                TextView num = texto(Textos.numero(k.clave), 15, t.texto, 0);
                num.setPadding(dp(12), 0, 0, 0);
                fi.addView(num, pesado());
                fi.addView(chip(k.n + " veces", t.primario));
                in.addView(fi);
            }
        }
        contenido.addView(in);

        // Historial del periodo
        LinearLayout hi = tarjeta();
        LinearLayout th = fila();
        th.addView(titulo("Historial"), pesado());
        th.addView(texto(s.eventos.size() + (s.eventos.size() == 1 ? " llamada" : " llamadas"), 13, t.textoSuave, 0));
        hi.addView(th);
        if (s.eventos.isEmpty()) {
            hi.addView(vacio("Sin llamadas filtradas en este periodo."));
        } else {
            int max = Math.min(100, s.eventos.size());
            for (int i = 0; i < max; i++) hi.addView(filaEvento(s.eventos.get(i), ahora));
            if (s.eventos.size() > max) hi.addView(vacio("Se muestran las 100 más recientes; el CSV las trae todas."));
        }
        contenido.addView(hi);
    }

    // ----------------------------------------------------------------- Ajustes

    private void pintarAjustes(List<Evento> eventos) {
        TextView tit = texto("Ajustes", 30, t.texto, 1);
        tit.setPadding(0, dp(6), 0, dp(2));
        contenido.addView(tit);

        // Apariencia
        LinearLayout ap = tarjeta();
        ap.addView(titulo("Apariencia"));
        LinearLayout op = fila();
        op.setPadding(0, dp(12), 0, 0);
        String pref = almacen.tema();
        String[][] temas = {{Tema.SISTEMA, "Sistema"}, {Tema.CLARO, "Blanco"}, {Tema.OSCURO, "Negro"}};
        int[] iconos = {Icono.AUTO, Icono.SOL, Icono.LUNA};
        for (int i = 0; i < temas.length; i++) {
            String clave = temas[i][0];
            boolean sel = clave.equals(pref);
            LinearLayout o = columna();
            o.setGravity(Gravity.CENTER_HORIZONTAL);
            o.setPadding(0, dp(14), 0, dp(12));
            GradientDrawable g = redondeado(sel ? Tema.alfa(t.primario, 0.12f) : t.tarjetaAlt, 18);
            if (sel) g.setStroke(dp(2), t.primario);
            o.setBackground(conToque(g, 18));
            o.addView(new Icono(this, iconos[i], sel ? t.primario : t.textoSuave), new LinearLayout.LayoutParams(dp(26), dp(26)));
            TextView l = texto(temas[i][1], 14, sel ? t.texto : t.textoSuave, sel ? 1 : 0);
            l.setGravity(Gravity.CENTER);
            l.setPadding(0, dp(8), 0, 0);
            o.addView(l);
            o.setContentDescription("Tema " + temas[i][1] + (sel ? ", elegido" : ""));
            o.setOnClickListener(v -> cambiarTema(clave));
            LinearLayout.LayoutParams lo = pesado();
            if (i > 0) lo.leftMargin = dp(10);
            op.addView(o, lo);
        }
        ap.addView(op);
        contenido.addView(ap);

        // Estado del filtro
        boolean activo = rolActivo();
        LinearLayout es = tarjeta();
        LinearLayout fe = fila();
        fe.addView(titulo("Filtro de llamadas"), pesado());
        fe.addView(chip(activo ? "Activo" : "Inactivo", activo ? t.acento : t.ambar));
        es.addView(fe);
        TextView de = texto(activo ? "Paz Mental es tu app de identificación de llamadas y spam."
                : "Android aún no le pasa las llamadas a Paz Mental.", 14, t.textoSuave, 0);
        de.setPadding(0, dp(6), 0, 0);
        es.addView(de);
        if (!activo) {
            LinearLayout.LayoutParams la = anchoCompleto();
            la.topMargin = dp(12);
            es.addView(boton("Activar Paz Mental", t.primario, t.sobrePrimario, v -> pedirRol()), la);
        }
        contenido.addView(es);

        // Países
        LinearLayout pa = tarjeta();
        pa.addView(titulo("Países permitidos"));
        TextView ayuda = texto("Un desconocido de un país marcado se silencia la primera vez y suena si insiste. "
                + "Los demás países se silencian siempre.", 14, t.textoSuave, 0);
        ayuda.setPadding(0, dp(6), 0, dp(4));
        pa.addView(ayuda);
        Set<String> permitidos = almacen.paises();
        for (String[] p : Reglas.PAISES) {
            LinearLayout fp = fila();
            fp.setPadding(0, dp(6), 0, dp(6));
            fp.addView(insignia(Textos.bandera(p[0])));
            LinearLayout cn = columna();
            cn.setPadding(dp(12), 0, 0, 0);
            cn.addView(texto(p[1], 15, t.texto, 0));
            cn.addView(texto("+" + p[0], 12, t.textoSuave, 0));
            fp.addView(cn, pesado());
            Graficos.Interruptor sw = new Graficos.Interruptor(this, t, permitidos.contains(p[0]));
            sw.setContentDescription("Permitir " + p[1]);
            sw.alCambiar(marcado -> {
                Set<String> s = almacen.paises();
                if (marcado) s.add(p[0]); else s.remove(p[0]);
                almacen.guardarPaises(s);
            });
            fp.addView(sw, new LinearLayout.LayoutParams(dp(48), dp(28)));
            pa.addView(fp);
        }
        contenido.addView(pa);

        // Privacidad
        LinearLayout pr = tarjeta();
        LinearLayout tp = fila();
        tp.addView(new Icono(this, Icono.ESCUDO, t.primario), new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView tpt = titulo("Privacidad");
        tpt.setPadding(dp(10), 0, 0, 0);
        tp.addView(tpt);
        pr.addView(tp);
        pr.addView(regla(t.acento, "Sin internet: la app no tiene permiso para conectarse."));
        pr.addView(regla(t.acento, "No lee tus contactos ni tu registro de llamadas."));
        pr.addView(regla(t.acento, "Guarda las últimas " + String.format(ES, "%,d", Almacen.MAX_EVENTOS)
                + " llamadas filtradas, solo en este teléfono. Ahora hay " + eventos.size() + "."));
        LinearLayout.LayoutParams lb = anchoCompleto();
        lb.topMargin = dp(14);
        TextView borrar = boton("Borrar historial", t.tarjetaAlt, t.rojo, v -> confirmarBorrado(eventos.size()));
        borrar.setEnabled(!eventos.isEmpty());
        borrar.setAlpha(eventos.isEmpty() ? 0.45f : 1f);
        pr.addView(borrar, lb);
        contenido.addView(pr);

        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            // sin versión visible
        }
        TextView pie = texto("Paz Mental " + version + " · Koscrypt · Licencia MIT", 12, t.textoSuave, 0);
        pie.setGravity(Gravity.CENTER);
        pie.setPadding(0, dp(20), 0, 0);
        contenido.addView(pie, anchoCompleto());
    }

    // ================================================================= Acciones

    private boolean rolActivo() {
        RoleManager rm = getSystemService(RoleManager.class);
        return rm != null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    private void pedirRol() {
        RoleManager rm = getSystemService(RoleManager.class);
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), PEDIR_ROL);
        }
    }

    private void cambiarTema(String clave) {
        if (clave.equals(almacen.tema())) return;
        almacen.guardarTema(clave);
        recreate();
    }

    private void exportar() {
        String fecha = new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date());
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/csv")
                .putExtra(Intent.EXTRA_TITLE, "paz-mental-" + rango + "d-" + fecha + ".csv");
        startActivityForResult(i, EXPORTAR);
    }

    /** Escribe el CSV del periodo elegido en el archivo que el usuario creó. */
    private void escribirCsv(Uri destino) {
        Estadisticas s = Estadisticas.calcular(almacen.eventos(), System.currentTimeMillis(), rango,
                TimeZone.getDefault());
        try (OutputStream o = getContentResolver().openOutputStream(destino, "wt")) {
            if (o == null) throw new IOException("sin flujo de salida");
            // Con BOM, Excel abre las tildes bien.
            o.write(("﻿" + Csv.generar(s.eventos, TimeZone.getDefault())).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Exportadas " + s.eventos.size() + " llamadas", Toast.LENGTH_SHORT).show();
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "No se pudo exportar", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmarBorrado(int n) {
        new AlertDialog.Builder(this)
                .setTitle("¿Borrar el historial?")
                .setMessage("Se borran " + n + (n == 1 ? " llamada guardada" : " llamadas guardadas")
                        + " y las estadísticas vuelven a cero. No se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Borrar", (d, w) -> { almacen.limpiarRegistro(); pintar(); })
                .show();
    }

    // ================================================================= Navegación

    private LinearLayout construirNav() {
        LinearLayout n = fila();
        GradientDrawable g = redondeado(t.tarjeta, 99);
        if (t.oscuro) g.setStroke(dp(1), t.borde);
        n.setBackground(g);
        n.setElevation(dp(10));
        n.setPadding(dp(6), dp(6), dp(6), dp(6));
        return n;
    }

    private void actualizarNav() {
        nav.removeAllViews();
        int[] iconos = {Icono.INICIO, Icono.GRAFICO, Icono.AJUSTES};
        String[] nombres = {"Inicio", "Estadísticas", "Ajustes"};
        for (int i = 0; i < 3; i++) {
            int p = i;
            boolean sel = i == pestana;
            LinearLayout it = fila();
            it.setGravity(Gravity.CENTER);
            it.setPadding(dp(sel ? 18 : 16), dp(12), dp(sel ? 20 : 16), dp(12));
            it.setBackground(sel ? redondeado(t.primario, 99) : conToque(redondeado(0, 99), 99));
            it.addView(new Icono(this, iconos[i], sel ? t.sobrePrimario : t.textoSuave),
                    new LinearLayout.LayoutParams(dp(22), dp(22)));
            if (sel) {
                TextView l = texto(nombres[i], 14, t.sobrePrimario, 1);
                l.setPadding(dp(8), 0, 0, 0);
                it.addView(l);
            }
            it.setContentDescription(nombres[i]);
            it.setOnClickListener(v -> { if (p != pestana) irA(p); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.leftMargin = dp(4);
            nav.addView(it, lp);
        }
    }

    // ================================================================= Piezas

    private LinearLayout tarjeta() {
        LinearLayout c = columna();
        GradientDrawable g = redondeado(t.tarjeta, 24);
        if (t.oscuro) g.setStroke(dp(1), t.borde);
        c.setBackground(g);
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams lp = anchoCompleto();
        lp.topMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout indicador(String etiqueta, String valor, String unidad, TextView extra) {
        LinearLayout k = columna();
        GradientDrawable g = redondeado(t.tarjeta, 22);
        if (t.oscuro) g.setStroke(dp(1), t.borde);
        k.setBackground(g);
        k.setPadding(dp(16), dp(14), dp(16), dp(16));
        LinearLayout arriba = fila();
        arriba.addView(texto(etiqueta, 13, t.textoSuave, 0), pesado());
        if (extra != null) arriba.addView(extra);
        k.addView(arriba);
        k.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        TextView c = cifra(valor, unidad, 32, t.texto, t.textoSuave);
        c.setPadding(0, dp(14), 0, 0);
        k.addView(c);
        return k;
    }

    private LinearLayout parIndicadores(LinearLayout a, LinearLayout b) {
        LinearLayout f = fila();
        f.setBaselineAligned(false);
        f.setGravity(Gravity.NO_GRAVITY);
        f.setPadding(0, dp(12), 0, 0);
        f.addView(a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lb.leftMargin = dp(12);
        f.addView(b, lb);
        return f;
    }

    /** Variación frente al periodo anterior: menos llamadas filtradas es mejor (verde). */
    private TextView cambio(Estadisticas s) {
        Integer c = s.cambioPct();
        if (c == null) return null;
        int color = c > 0 ? t.ambar : t.acento;
        return texto((c > 0 ? "↗ +" : c < 0 ? "↘ " : "") + c + "%", 12, color, 1);
    }

    private View filaEvento(Evento e, long ahora) {
        LinearLayout f = fila();
        f.setPadding(0, dp(12), 0, 0);
        f.addView(insignia(e.numero.isEmpty() ? "" : Textos.bandera(e.pais())));
        LinearLayout col = columna();
        col.setPadding(dp(12), 0, dp(8), 0);
        String cuando = Textos.cuando(e.ts, ahora, TimeZone.getDefault());
        col.addView(texto(Textos.numero(e.numero), 15, t.texto, 0));
        col.addView(texto(cuando, 12, t.textoSuave, 0));
        f.addView(col, pesado());
        f.addView(chip(Textos.corta(e.decision), t.color(e.decision)));
        f.setContentDescription(Textos.numero(e.numero) + ", " + Textos.etiqueta(e.decision) + ", " + cuando);
        return f;
    }

    private View filaLeyenda(int color, String nombre, int n, int pct) {
        LinearLayout f = fila();
        f.setPadding(0, dp(9), 0, 0);
        f.addView(punto(color, 10));
        TextView tn = texto(nombre, 14, t.texto, 0);
        tn.setPadding(dp(10), 0, 0, 0);
        f.addView(tn, pesado());
        f.addView(texto(String.valueOf(n), 14, t.textoSuave, 0));
        TextView tp = texto(pct + "%", 14, t.texto, 1);
        tp.setGravity(Gravity.END);
        f.addView(tp, new LinearLayout.LayoutParams(dp(50), ViewGroup.LayoutParams.WRAP_CONTENT));
        return f;
    }

    private View filaBarra(String nombre, int n, float frac) {
        LinearLayout c = columna();
        c.setPadding(0, dp(12), 0, 0);
        LinearLayout f = fila();
        f.addView(texto(nombre, 15, t.texto, 0), pesado());
        f.addView(texto(String.valueOf(n), 14, t.texto, 1));
        c.addView(f);
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        lb.topMargin = dp(8);
        c.addView(new Graficos.Fraccion(this, t.tarjetaAlt, t.primario, frac), lb);
        return c;
    }

    private View regla(int color, String s) {
        LinearLayout f = fila();
        f.setGravity(Gravity.TOP);
        f.setPadding(0, dp(10), 0, 0);
        View p = punto(color, 10);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(10), dp(10));
        lp.topMargin = dp(6);
        f.addView(p, lp);
        TextView tx = texto(s, 15, t.texto, 0);
        tx.setPadding(dp(12), 0, 0, 0);
        f.addView(tx, pesado());
        return f;
    }

    private TextView vacio(String s) {
        TextView v = texto(s, 14, t.textoSuave, 0);
        v.setPadding(0, dp(10), 0, 0);
        return v;
    }

    private TextView titulo(String s) {
        return texto(s, 18, t.texto, 1);
    }

    /** Círculo con la bandera, o un signo de interrogación si el número es oculto o desconocido. */
    private View insignia(String bandera) {
        FrameLayout c = new FrameLayout(this);
        c.setBackground(redondeado(t.tarjetaAlt, 99));
        boolean sin = bandera.isEmpty();
        c.addView(texto(sin ? "?" : bandera, sin ? 16 : 18, t.textoSuave, sin ? 1 : 0),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        c.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        return c;
    }

    private TextView chip(String s, int color) {
        TextView c = texto(s, 12, color, 1);
        c.setBackground(redondeado(Tema.alfa(color, t.oscuro ? 0.18f : 0.13f), 99));
        c.setPadding(dp(10), dp(5), dp(10), dp(5));
        return c;
    }

    private View punto(int color, int tam) {
        View v = new View(this);
        v.setBackground(redondeado(color, 99));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(tam), dp(tam)));
        return v;
    }

    private TextView boton(String s, int fondo, int color, View.OnClickListener al) {
        TextView b = texto(s, 15, color, 1);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(18), dp(13), dp(18), dp(13));
        b.setBackground(conToque(redondeado(fondo, 99), 99));
        b.setOnClickListener(al);
        return b;
    }

    private FrameLayout circulo(int fondo, int tam) {
        FrameLayout c = new FrameLayout(this);
        c.setBackground(conToque(redondeado(fondo, 99), 99));
        c.setLayoutParams(new LinearLayout.LayoutParams(dp(tam), dp(tam)));
        return c;
    }

    /** Cifra grande con su unidad pequeña al lado: "12  llamadas". */
    private TextView cifra(String valor, String unidad, int sp, int color, int colorUnidad) {
        SpannableStringBuilder b = new SpannableStringBuilder(valor);
        int i = b.length();
        b.append("  ").append(unidad);
        b.setSpan(new AbsoluteSizeSpan(14, true), i, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.setSpan(new ForegroundColorSpan(colorUnidad), i, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView v = new TextView(this);
        v.setText(b);
        v.setTextColor(color);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        v.setLetterSpacing(-0.02f);
        v.setIncludeFontPadding(false);
        return v;
    }

    /** estilo: 0 normal, 1 medio. */
    private TextView texto(String s, float sp, int color, int estilo) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(color);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTypeface(Typeface.create(estilo == 1 ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        v.setLineSpacing(0, 1.12f);
        return v;
    }

    private static String sinPunto(String s) {
        return s.replace(".", "");
    }

    private LinearLayout fila() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout columna() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams anchoCompleto() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams pesado() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private GradientDrawable redondeado(int color, int radioDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radioDp));
        return g;
    }

    private Drawable conToque(Drawable fondo, int radioDp) {
        return new RippleDrawable(ColorStateList.valueOf(Tema.alfa(t.texto, 0.12f)), fondo,
                redondeado(0xFF000000, radioDp));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
