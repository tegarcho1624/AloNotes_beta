package com.alonotes.app;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * Editor de una nota: barra de herramientas (por ahora todas "Próximamente" salvo
 * volver y crear páginas) y lienzo con las páginas creadas.
 */
public class EditorActivity extends Activity {
    private static final String[] DIMS = {"A5", "A4", "A3", "A2"};
    private static final String[] DIM_LABELS = {"A5 (148 x 210 mm)", "A4 (210 x 297 mm)", "A3 (297 x 420 mm)", "A2 (420 x 594 mm)"};
    private static final float[][] MM = {{148, 210}, {210, 297}, {297, 420}, {420, 594}};
    private static final int[] PAGE_COLORS = {0xFFFFFFFF, 0xFFF5EBD7, 0xFFFFF6C9, 0xFFDDF3E4, 0xFFDCEBFA, 0xFFFADCE6, 0xFFEDEDED, 0xFF1A1A1A};

    private String docId;
    private LinearLayout pagesCol;
    private ScrollView sv;
    private TextView counter;
    private int total;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ThemeUtils.applyTo(this);
        docId = getIntent().getStringExtra("id");
        if (docId == null || NoteStore.get(this, docId) == null) {
            finish();
            return;
        }
        int ink = getColor(R.color.ink), mut = getColor(R.color.mut);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getColor(R.color.bg));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        // Barra de herramientas
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(UiKit.dp(this, 6), 0, UiKit.dp(this, 6), 0);
        bar.addView(tb(Icons.BACK, ink, v -> finish()), UiKit.box(this, 44));
        bar.addView(tb(Icons.GRID, ink, soon()), UiKit.box(this, 44));
        bar.addView(tb(Icons.PLUS, ink, v -> createPages()), UiKit.box(this, 44));
        bar.addView(new View(this), new LinearLayout.LayoutParams(UiKit.dp(this, 22), 1));
        int[] tools = {Icons.PEN, Icons.MARKER, Icons.ERASER, Icons.TEXT, Icons.SHAPES, Icons.IMAGE, Icons.STAR, Icons.SELECT};
        for (int t : tools) bar.addView(tb(t, ink, soon()), UiKit.box(this, 44));
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        bar.addView(tb(Icons.UNDO, mut, soon()), UiKit.box(this, 44));
        counter = UiKit.text(this, "0 / 0", 17, ink);
        counter.setGravity(Gravity.CENTER);
        bar.addView(counter, new LinearLayout.LayoutParams(UiKit.dp(this, 90), -2));
        bar.addView(tb(Icons.REDO, mut, soon()), UiKit.box(this, 44));
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        bar.addView(tb(Icons.LOCK, ink, soon()), UiKit.box(this, 44));
        bar.addView(tb(Icons.ROT_L, mut, soon()), UiKit.box(this, 44));
        bar.addView(tb(Icons.ROT_R, mut, soon()), UiKit.box(this, 44));
        View div = new View(this);
        div.setBackgroundColor(getColor(R.color.ln2));
        LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(UiKit.dp(this, 1), UiKit.dp(this, 24));
        dp1.setMargins(UiKit.dp(this, 8), 0, UiKit.dp(this, 8), 0);
        bar.addView(div, dp1);
        bar.addView(tb(Icons.GEAR, ink, soon()), UiKit.box(this, 44));
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setFillViewport(true);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setBackgroundColor(getColor(R.color.sf));
        hsv.addView(bar, new ViewGroup.LayoutParams(-1, -1));
        col.addView(hsv, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 52)));
        View line = new View(this);
        line.setBackgroundColor(getColor(R.color.ln));
        col.addView(line, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 1)));

        // Lienzo de páginas
        sv = new ScrollView(this);
        sv.setBackgroundColor(getColor(R.color.sf2));
        pagesCol = new LinearLayout(this);
        pagesCol.setOrientation(LinearLayout.VERTICAL);
        pagesCol.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = UiKit.dp(this, 24);
        pagesCol.setPadding(pad, pad, pad, UiKit.dp(this, 120));
        sv.addView(pagesCol, new ViewGroup.LayoutParams(-1, -2));
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> updateCounter());
        col.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        // Botón flotante del lápiz
        FrameLayout pen = new FrameLayout(this);
        ImageView pi = new ImageView(this);
        pi.setImageDrawable(new Icons(Icons.PENCIL, ink, false));
        pen.addView(pi, new FrameLayout.LayoutParams(UiKit.dp(this, 26), UiKit.dp(this, 26), Gravity.CENTER));
        UiKit.ripple(this, pen, 999, 0);
        pen.setBackground(UiKit.oval(this, getColor(R.color.sf3), ThemeUtils.accent(this), 1.5f));
        pen.setElevation(UiKit.dp(this, 6));
        pen.setOnClickListener(v -> UiKit.soon(this));
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(UiKit.dp(this, 56), UiKit.dp(this, 56), Gravity.END | Gravity.BOTTOM);
        pp.setMargins(0, 0, UiKit.dp(this, 14), UiKit.dp(this, 120));
        root.addView(pen, pp);

        setContentView(root);
        renderPages();
        if (b == null && getIntent().getBooleanExtra("new", false) && total == 0) createPages();
    }

    private View tb(int type, int color, View.OnClickListener l) {
        return UiKit.iconBtn(this, type, color, 44, 24, l);
    }

    private View.OnClickListener soon() { return v -> UiKit.soon(this); }

    // ---------- Páginas ----------

    private void renderPages() {
        NoteStore.Item doc = NoteStore.get(this, docId);
        pagesCol.removeAllViews();
        total = doc == null ? 0 : doc.pages.length();
        if (total == 0) {
            TextView e = UiKit.text(this, "Sin páginas todavía", 16, getColor(R.color.mut));
            e.setPadding(0, UiKit.dp(this, 80), 0, UiKit.dp(this, 16));
            pagesCol.addView(e);
            TextView bt = UiKit.btn(this, "Crear páginas", ThemeUtils.accent(this));
            bt.setOnClickListener(v -> createPages());
            pagesCol.addView(bt, new LinearLayout.LayoutParams(-2, -2));
            updateCounter();
            return;
        }
        int sw = getResources().getDisplayMetrics().widthPixels;
        int base = Math.min(sw - UiKit.dp(this, 64), UiKit.dp(this, 720));
        for (int i = 0; i < total; i++) {
            JSONObject pg = doc.pages.optJSONObject(i);
            if (pg == null) continue;
            int di = 1;
            for (int k = 0; k < DIMS.length; k++) if (DIMS[k].equals(pg.optString("d"))) di = k;
            float ratio = MM[di][1] / MM[di][0];
            boolean land = !"P".equals(pg.optString("o"));
            int w = land ? base : Math.round(base * 0.72f);
            int h = land ? Math.round(base / ratio) : Math.round(w * ratio);
            View page = new View(this);
            page.setBackground(UiKit.rect(this, pg.optInt("bg", 0xFFFFFFFF), 2));
            page.setElevation(UiKit.dp(this, 4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.bottomMargin = UiKit.dp(this, 20);
            pagesCol.addView(page, lp);
        }
        updateCounter();
    }

    private void updateCounter() {
        if (total == 0) {
            counter.setText("0 / 0");
            return;
        }
        int center = sv.getScrollY() + sv.getHeight() / 2;
        int cur = total;
        for (int i = 0; i < pagesCol.getChildCount(); i++) {
            if (pagesCol.getChildAt(i).getBottom() >= center) {
                cur = i + 1;
                break;
            }
        }
        counter.setText(Math.min(cur, total) + " / " + total);
    }

    // ---------- Diálogo "Crear páginas nuevas" ----------

    private View selector(String initial, String[] opts, int[] sel, boolean softBlockOthers) {
        LinearLayout s = new LinearLayout(this);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setPadding(UiKit.dp(this, 10), UiKit.dp(this, 8), UiKit.dp(this, 6), UiKit.dp(this, 8));
        TextView t = UiKit.text(this, initial, 16, getColor(R.color.ink));
        s.addView(t);
        ImageView ch = new ImageView(this);
        ch.setImageDrawable(new Icons(Icons.CHEV, getColor(R.color.ink), false));
        LinearLayout.LayoutParams cp = UiKit.box(this, 20);
        cp.leftMargin = UiKit.dp(this, 6);
        s.addView(ch, cp);
        UiKit.ripple(this, s, 8, 0);
        s.setOnClickListener(v -> UiKit.dropdown(this, s, opts, i -> {
            if (softBlockOthers && i != 0) {
                UiKit.soon(this); // Imagen y Patrón llegarán más adelante
                return;
            }
            sel[0] = i;
            t.setText(opts[i]);
        }));
        return s;
    }

    private View formRow(String label, View right) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.addView(UiKit.text(this, label, 16, getColor(R.color.ink)), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(right, new LinearLayout.LayoutParams(-2, -2));
        r.setPadding(0, UiKit.dp(this, 6), 0, UiKit.dp(this, 6));
        return r;
    }

    private void createPages() {
        int[] dim = {1}, ori = {0}, bgSel = {0}, color = {PAGE_COLORS[0]};
        LinearLayout l = UiKit.vbox(this, 22);
        TextView title = UiKit.text(this, "Crear páginas nuevas", 18, getColor(R.color.ink));
        title.setPadding(0, 0, 0, UiKit.dp(this, 8));
        l.addView(title);
        l.addView(formRow("Dimensiones", selector(DIM_LABELS[1], DIM_LABELS, dim, false)));
        l.addView(formRow("Orientación", selector("Horizontal", new String[]{"Horizontal", "Vertical"}, ori, false)));
        l.addView(formRow("Fondo", selector("Color", new String[]{"Color", "Imagen", "Patrón"}, bgSel, true)));
        View sw = new View(this);
        sw.setBackground(UiKit.oval(this, color[0], getColor(R.color.ln2), 1));
        LinearLayout colorBox = new LinearLayout(this);
        colorBox.setGravity(Gravity.CENTER_VERTICAL);
        colorBox.addView(UiKit.text(this, "Color:", 15, getColor(R.color.mut)));
        LinearLayout.LayoutParams swp = UiKit.box(this, 36);
        swp.leftMargin = UiKit.dp(this, 10);
        colorBox.addView(sw, swp);
        sw.setOnClickListener(v -> pickColor(c -> {
            color[0] = c;
            sw.setBackground(UiKit.oval(this, c, getColor(R.color.ln2), 1));
        }));
        LinearLayout cr = new LinearLayout(this);
        cr.setGravity(Gravity.END);
        cr.addView(colorBox);
        l.addView(cr);
        EditText num = new EditText(this);
        num.setInputType(InputType.TYPE_CLASS_NUMBER);
        num.setText("1");
        num.setGravity(Gravity.END);
        num.setSingleLine(true);
        num.setTextColor(getColor(R.color.ink));
        num.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ThemeUtils.accent(this)));
        l.addView(formRow("Número de páginas", num));
        ((LinearLayout) num.getParent()).getChildAt(1).setLayoutParams(new LinearLayout.LayoutParams(UiKit.dp(this, 90), -2));
        Dialog[] d = new Dialog[1];
        TextView no = UiKit.btn(this, "Cancelar", getColor(R.color.mut));
        TextView ok = UiKit.btn(this, "Crear", ThemeUtils.accent(this));
        no.setOnClickListener(v -> d[0].dismiss());
        ok.setOnClickListener(v -> {
            int n = 1;
            try { n = Integer.parseInt(num.getText().toString().trim()); } catch (Exception ignored) {
                // Vacío o inválido: se crea 1 página.
            }
            n = Math.max(1, Math.min(50, n));
            NoteStore.addPages(this, docId, n, DIMS[dim[0]], ori[0] == 0, color[0]);
            d[0].dismiss();
            renderPages();
        });
        LinearLayout br = UiKit.buttons(this, no, ok);
        br.setPadding(0, UiKit.dp(this, 14), 0, 0);
        l.addView(br);
        d[0] = UiKit.show(this, l, false);
    }

    private void pickColor(java.util.function.IntConsumer got) {
        LinearLayout l = UiKit.vbox(this, 18);
        TextView t = UiKit.text(this, "Color de la página", 17, getColor(R.color.ink));
        t.setPadding(UiKit.dp(this, 4), 0, 0, UiKit.dp(this, 10));
        l.addView(t);
        Dialog[] d = new Dialog[1];
        LinearLayout row = null;
        for (int i = 0; i < PAGE_COLORS.length; i++) {
            if (i % 4 == 0) {
                row = new LinearLayout(this);
                l.addView(row);
            }
            final int c = PAGE_COLORS[i];
            View s = new View(this);
            s.setBackground(UiKit.oval(this, c, getColor(R.color.ln2), 1));
            s.setOnClickListener(v -> {
                d[0].dismiss();
                got.accept(c);
            });
            LinearLayout.LayoutParams sp = UiKit.box(this, 52);
            sp.setMargins(UiKit.dp(this, 8), UiKit.dp(this, 8), UiKit.dp(this, 8), UiKit.dp(this, 8));
            row.addView(s, sp);
        }
        d[0] = UiKit.show(this, l, false);
    }
}
