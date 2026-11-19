package com.alonotes.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Componentes de interfaz compartidos: hojas inferiores, diálogos, botones, filas. */
final class UiKit {
    private UiKit() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rect(Context c, int fill, float rDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, rDp));
        return g;
    }

    static GradientDrawable rect(Context c, int fill, float rDp, int stroke) {
        GradientDrawable g = rect(c, fill, rDp);
        g.setStroke(dp(c, 1), stroke);
        return g;
    }

    static GradientDrawable oval(Context c, int fill, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        if (strokeDp > 0) g.setStroke(dp(c, strokeDp), stroke);
        return g;
    }

    static void soon(Context c) {
        Toast.makeText(c, R.string.soon, Toast.LENGTH_SHORT).show();
    }

    static TextView text(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    static void ripple(Context c, View v, float rDp, int fill) {
        Drawable content = fill != 0 ? rect(c, fill, rDp) : null;
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, rect(c, 0xFFFFFFFF, rDp)));
    }

    static LinearLayout.LayoutParams box(Context c, float dpSize) {
        return new LinearLayout.LayoutParams(dp(c, dpSize), dp(c, dpSize));
    }

    /** Botón de ícono: caja clicable con el ícono centrado. */
    static FrameLayout iconBtn(Context c, int type, int color, float boxDp, float iconDp, View.OnClickListener l) {
        FrameLayout f = new FrameLayout(c);
        ImageView iv = new ImageView(c);
        iv.setImageDrawable(new Icons(type, color, false));
        f.addView(iv, new FrameLayout.LayoutParams(dp(c, iconDp), dp(c, iconDp), Gravity.CENTER));
        ripple(c, f, 999, 0);
        if (l != null) f.setOnClickListener(l);
        return f;
    }

    static LinearLayout vbox(Context c, float padDp) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, padDp);
        l.setPadding(p, p, p, p);
        return l;
    }

    static TextView btn(Context c, String s, int color) {
        TextView t = text(c, s.toUpperCase(Locale.getDefault()), 13, color);
        t.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
        t.setGravity(Gravity.CENTER);
        ripple(c, t, 8, c.getColor(R.color.sf3));
        return t;
    }

    /** Fila de botones alineada a la derecha. */
    static LinearLayout buttons(Context c, View... bs) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.END);
        for (View b : bs) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = dp(c, 10);
            r.addView(b, lp);
        }
        return r;
    }

    /** Muestra contenido en una tarjeta: hoja inferior (bottom) o diálogo centrado. */
    static Dialog show(Activity a, View content, boolean bottom) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout card = new FrameLayout(a);
        card.setBackground(rect(a, a.getColor(R.color.sheet), bottom ? 24 : 20, a.getColor(R.color.ln)));
        card.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        d.setContentView(card, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setDimAmount(0.6f);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        int sw = a.getResources().getDisplayMetrics().widthPixels;
        w.setLayout(Math.min(sw - dp(a, 32), dp(a, bottom ? 560 : 440)), WindowManager.LayoutParams.WRAP_CONTENT);
        w.setGravity(bottom ? Gravity.BOTTOM : Gravity.CENTER);
        if (bottom) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.y = dp(a, 12);
            w.setAttributes(lp);
        }
        d.show();
        return d;
    }

    /** Fila de una hoja inferior; cierra la hoja (d[0]) antes de ejecutar la acción. */
    static View sheetRow(Activity a, int icon, String label, int color, Dialog[] d, Runnable r) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, 14), dp(a, 14), dp(a, 14), dp(a, 14));
        ImageView iv = new ImageView(a);
        iv.setImageDrawable(new Icons(icon, color, false));
        row.addView(iv, box(a, 22));
        TextView t = text(a, label, 16, color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(a, 16);
        row.addView(t, lp);
        ripple(a, row, 12, 0);
        row.setOnClickListener(v -> {
            if (d[0] != null) d[0].dismiss();
            r.run();
        });
        return row;
    }

    /** Diálogo para pedir un nombre (crear/renombrar). */
    static void askName(Activity a, String title, String initial, Consumer<String> ok) {
        LinearLayout l = vbox(a, 22);
        l.addView(text(a, title, 17, a.getColor(R.color.ink)));
        EditText e = new EditText(a);
        e.setSingleLine(true);
        e.setText(initial);
        e.setSelection(initial.length());
        e.setTextColor(a.getColor(R.color.ink));
        e.setBackgroundTintList(ColorStateList.valueOf(ThemeUtils.accent(a)));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.topMargin = dp(a, 14);
        ep.bottomMargin = dp(a, 18);
        l.addView(e, ep);
        Dialog[] d = new Dialog[1];
        TextView no = btn(a, "Cancelar", a.getColor(R.color.mut));
        TextView yes = btn(a, "Aceptar", ThemeUtils.accent(a));
        no.setOnClickListener(v -> d[0].dismiss());
        yes.setOnClickListener(v -> {
            String s = e.getText().toString().trim();
            if (s.isEmpty()) return;
            d[0].dismiss();
            ok.accept(s);
        });
        l.addView(buttons(a, no, yes));
        d[0] = show(a, l, false);
        e.requestFocus();
    }

    /** Confirmación simple (por ejemplo, eliminar). */
    static void confirm(Activity a, String msg, String yesLabel, Runnable yes) {
        LinearLayout l = vbox(a, 22);
        TextView t = text(a, msg, 16, a.getColor(R.color.ink));
        t.setPadding(0, 0, 0, dp(a, 18));
        l.addView(t);
        Dialog[] d = new Dialog[1];
        TextView no = btn(a, "Cancelar", a.getColor(R.color.mut));
        TextView ok = btn(a, yesLabel, a.getColor(R.color.danger));
        no.setOnClickListener(v -> d[0].dismiss());
        ok.setOnClickListener(v -> {
            d[0].dismiss();
            yes.run();
        });
        l.addView(buttons(a, no, ok));
        d[0] = show(a, l, false);
    }

    /** Lista desplegable oscura anclada a una vista. */
    static void dropdown(Activity a, View anchor, String[] opts, IntConsumer pick) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rect(a, a.getColor(R.color.sf2), 12, a.getColor(R.color.ln2)));
        PopupWindow pw = new PopupWindow(l, dp(a, 210), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setElevation(dp(a, 8));
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            TextView t = text(a, opts[i], 15, a.getColor(R.color.ink));
            t.setPadding(dp(a, 18), dp(a, 14), dp(a, 18), dp(a, 14));
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            ripple(a, t, 8, 0);
            t.setOnClickListener(v -> {
                pw.dismiss();
                pick.accept(idx);
            });
            l.addView(t);
        }
        pw.showAsDropDown(anchor, 0, 0, Gravity.END);
    }
}
