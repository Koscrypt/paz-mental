package co.pazmental;

import android.app.Activity;
import android.app.role.RoleManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Set;

public class MainActivity extends Activity {

    private static final int PEDIR_ROL = 1;

    private Almacen almacen;
    private TextView estado;
    private Button activar;
    private TextView registro;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        almacen = new Almacen(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        scroll.setOnApplyWindowInsetsListener((v, ins) -> {
            android.graphics.Insets s = ins.getInsets(WindowInsets.Type.systemBars());
            col.setPadding(pad, pad + s.top, pad, pad + s.bottom);
            return ins;
        });
        scroll.addView(col);

        TextView titulo = texto("Paz Mental", 28, true);
        col.addView(titulo);
        col.addView(texto("Tus contactos siempre suenan. Los números ocultos se rechazan. "
                + "Un desconocido de un país permitido se silencia la primera vez; "
                + "si vuelve a llamar en menos de 5 minutos, suena.", 15, false));

        estado = texto("", 16, true);
        estado.setPadding(0, dp(16), 0, dp(8));
        col.addView(estado);
        activar = new Button(this);
        activar.setText("Activar Paz Mental");
        activar.setOnClickListener(v -> pedirRol());
        col.addView(activar);

        TextView tp = texto("Países permitidos", 20, true);
        tp.setPadding(0, dp(24), 0, dp(4));
        col.addView(tp);
        col.addView(texto("Las llamadas de países no marcados se silencian siempre.", 14, false));
        Set<String> permitidos = almacen.paises();
        for (String[] p : Reglas.PAISES) {
            CheckBox cb = new CheckBox(this);
            cb.setText(p[1] + "  (+" + p[0] + ")");
            cb.setChecked(permitidos.contains(p[0]));
            cb.setOnCheckedChangeListener((x, marcado) -> {
                Set<String> s = almacen.paises();
                if (marcado) s.add(p[0]); else s.remove(p[0]);
                almacen.guardarPaises(s);
            });
            col.addView(cb);
        }

        TextView tr = texto("Registro", 20, true);
        tr.setPadding(0, dp(24), 0, dp(4));
        col.addView(tr);
        registro = texto("", 13, false);
        registro.setTypeface(Typeface.MONOSPACE);
        col.addView(registro);
        Button limpiar = new Button(this);
        limpiar.setText("Limpiar registro");
        limpiar.setOnClickListener(v -> { almacen.limpiarRegistro(); refrescar(); });
        col.addView(limpiar);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrescar();
    }

    private void refrescar() {
        RoleManager rm = getSystemService(RoleManager.class);
        boolean activo = rm != null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        estado.setText(activo ? "● Activa: filtrando llamadas" : "○ Inactiva");
        activar.setEnabled(!activo);
        String r = almacen.registro();
        registro.setText(r.isEmpty() ? "Sin llamadas filtradas todavía." : r);
    }

    private void pedirRol() {
        RoleManager rm = getSystemService(RoleManager.class);
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), PEDIR_ROL);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        refrescar();
    }

    private TextView texto(String s, int sp, boolean negrita) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (negrita) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
