package com.alonotes.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Biblioteca "Mis notas": carpetas y notas en cuadrícula, menú flotante (+) y ajustes.
 * Botones sin función todavía => aviso "Próximamente".
 */
public class MainActivity extends Activity {
    private String folderId = "";
    private final List<NoteStore.Item> items = new ArrayList<>();
    private RecyclerView rv;
    private GridLayoutManager glm;
    private Adapter adapter;
    private LinearLayout crumb;
    private View scrim, fabMenu, fab;
    private int themeVer;
    private final SimpleDateFormat fmt = new SimpleDateFormat("EEE dd MMM, HH:mm", new Locale("es"));

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ThemeUtils.applyTo(this);
        themeVer = ThemeUtils.version();
        if (b != null) folderId = b.getString("folder", "");
        int ink = getColor(R.color.ink);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getColor(R.color.bg));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        // Barra superior
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(getColor(R.color.sf));
        bar.setPadding(UiKit.dp(this, 8), 0, UiKit.dp(this, 8), 0);
        bar.addView(UiKit.iconBtn(this, Icons.MENU, ink, 48, 24, v -> UiKit.soon(this)), UiKit.box(this, 48));
        crumb = new LinearLayout(this);
        crumb.setGravity(Gravity.CENTER_VERTICAL);
        crumb.setPadding(UiKit.dp(this, 8), 0, 0, 0);
        bar.addView(crumb, new LinearLayout.LayoutParams(0, -2, 1));
        int[] tools = {Icons.SORT, Icons.SEARCH, Icons.MAIL, Icons.HELP};
        for (int t : tools) bar.addView(UiKit.iconBtn(this, t, ink, 48, 24, v -> UiKit.soon(this)), UiKit.box(this, 48));
        bar.addView(UiKit.iconBtn(this, Icons.GEAR, ink, 48, 24, v -> openSettings()), UiKit.box(this, 48));
        col.addView(bar, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 56)));
        View line = new View(this);
        line.setBackgroundColor(getColor(R.color.ln));
        col.addView(line, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 1)));

        // Cuadrícula
        rv = new RecyclerView(this);
        rv.setClipToPadding(false);
        rv.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 12), UiKit.dp(this, 16), UiKit.dp(this, 110));
        glm = new GridLayoutManager(this, spanCount());
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int pos) {
                return (!folderId.isEmpty() && pos == 0) ? glm.getSpanCount() : 1;
            }
        });
        rv.setLayoutManager(glm);
        adapter = new Adapter();
        rv.setAdapter(adapter);
        col.addView(rv, new LinearLayout.LayoutParams(-1, 0, 1));

        // Menú flotante (+)
        scrim = new View(this);
        scrim.setBackgroundColor(getColor(R.color.scrim));
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(v -> toggleMenu(false));
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setGravity(Gravity.END);
        menu.addView(fabRow("Nuevo documento", Icons.DOC, this::newDocument));
        menu.addView(fabRow("Importar documento", Icons.PDF, () -> UiKit.soon(this)));
        menu.addView(fabRow("Crear carpeta", Icons.FOLDER, this::newFolder));
        menu.addView(fabRow("Cerrar menú", Icons.CLOSE, () -> toggleMenu(false)));
        menu.setVisibility(View.GONE);
        fabMenu = menu;
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        mp.setMargins(0, 0, UiKit.dp(this, 20), UiKit.dp(this, 20));
        root.addView(menu, mp);

        fab = circle(Icons.PLUS, 60);
        fab.setOnClickListener(v -> toggleMenu(true));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(UiKit.dp(this, 60), UiKit.dp(this, 60), Gravity.BOTTOM | Gravity.END);
        fp.setMargins(0, 0, UiKit.dp(this, 20), UiKit.dp(this, 20));
        root.addView(fab, fp);

        setContentView(root);
    }

    private int spanCount() {
        return Math.max(2, getResources().getDisplayMetrics().widthPixels / UiKit.dp(this, 180));
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        glm.setSpanCount(spanCount());
    }

    @Override
    protected void onSaveInstanceState(Bundle o) {
        super.onSaveInstanceState(o);
        o.putString("folder", folderId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (themeVer != ThemeUtils.version()) {
            recreate();
            return;
        }
        refresh();
    }

    @Override
    public void onBackPressed() {
        if (fabMenu.getVisibility() == View.VISIBLE) toggleMenu(false);
        else if (!folderId.isEmpty()) goUp();
        else super.onBackPressed();
    }

    // ---------- Datos ----------

    private void refresh() {
        if (!folderId.isEmpty() && NoteStore.get(this, folderId) == null) folderId = "";
        items.clear();
        items.addAll(NoteStore.children(this, folderId));
        adapter.notifyDataSetChanged();
        buildCrumb();
    }

    private void goUp() {
        NoteStore.Item f = NoteStore.get(this, folderId);
        folderId = f == null ? "" : f.parent;
        refresh();
    }

    private void buildCrumb() {
        crumb.removeAllViews();
        int ink = getColor(R.color.ink);
        TextView r = UiKit.text(this, "Mis notas", 20, ink);
        r.setOnClickListener(v -> { folderId = ""; refresh(); });
        crumb.addView(r);
        NoteStore.Item f = folderId.isEmpty() ? null : NoteStore.get(this, folderId);
        if (f != null) {
            ImageView ch = new ImageView(this);
            ch.setImageDrawable(new Icons(Icons.CHEV, getColor(R.color.mut), false));
            ch.setRotation(-90);
            LinearLayout.LayoutParams cp = UiKit.box(this, 22);
            cp.setMargins(UiKit.dp(this, 6), 0, UiKit.dp(this, 6), 0);
            crumb.addView(ch, cp);
            TextView n = UiKit.text(this, f.name, 20, ink);
            n.setSingleLine(true);
            n.setEllipsize(TextUtils.TruncateAt.END);
            crumb.addView(n);
        }
    }

    // ---------- Menú flotante ----------

    private View circle(int icon, float sizeDp) {
        FrameLayout f = new FrameLayout(this);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(new Icons(icon, getColor(R.color.bg), false));
        f.addView(iv, new FrameLayout.LayoutParams(UiKit.dp(this, 26), UiKit.dp(this, 26), Gravity.CENTER));
        UiKit.ripple(this, f, 999, ThemeUtils.accent(this));
        f.setElevation(UiKit.dp(this, 6));
        return f;
    }

    private View fabRow(String label, int icon, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = UiKit.text(this, label, 14, getColor(R.color.ink));
        t.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 8), UiKit.dp(this, 14), UiKit.dp(this, 8));
        t.setBackground(UiKit.rect(this, getColor(R.color.sf3), 8));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.rightMargin = UiKit.dp(this, 12);
        row.addView(t, tp);
        row.addView(circle(icon, 52), UiKit.box(this, 52));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2);
        rp.bottomMargin = UiKit.dp(this, 14);
        row.setLayoutParams(rp);
        row.setOnClickListener(v -> {
            toggleMenu(false);
            action.run();
        });
        return row;
    }

    private void toggleMenu(boolean open) {
        scrim.setVisibility(open ? View.VISIBLE : View.GONE);
        fabMenu.setVisibility(open ? View.VISIBLE : View.GONE);
        fab.setVisibility(open ? View.GONE : View.VISIBLE);
    }

    private void newDocument() {
        NoteStore.Item d = NoteStore.add(this, folderId, false, null);
        Intent i = new Intent(this, EditorActivity.class);
        i.putExtra("id", d.id);
        i.putExtra("new", true);
        startActivity(i);
    }

    private void newFolder() {
        UiKit.askName(this, "Escribe un nombre de carpeta", "", n -> {
            NoteStore.add(this, folderId, true, n);
            refresh();
        });
    }

    // ---------- Opciones de un elemento ----------

    private void options(NoteStore.Item it) {
        int ink = getColor(R.color.ink);
        LinearLayout l = UiKit.vbox(this, 12);
        TextView title = UiKit.text(this, it.name, 17, ink);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 8), UiKit.dp(this, 14), UiKit.dp(this, 8));
        l.addView(title);
        Dialog[] d = new Dialog[1];
        l.addView(UiKit.sheetRow(this, Icons.PENCIL, "Renombrar", ink, d, () ->
                UiKit.askName(this, "Renombrar", it.name, n -> { NoteStore.rename(this, it.id, n); refresh(); })));
        l.addView(UiKit.sheetRow(this, Icons.FOLDER, "Mover", ink, d, () -> UiKit.soon(this)));
        l.addView(UiKit.sheetRow(this, Icons.DOC, "Duplicar", ink, d, () -> UiKit.soon(this)));
        l.addView(UiKit.sheetRow(this, Icons.CLOSE, "Eliminar", getColor(R.color.danger), d, () ->
                UiKit.confirm(this, "¿Eliminar «" + it.name + "»?" + (it.folder ? " Se borrará todo su contenido." : ""),
                        "Eliminar", () -> { NoteStore.delete(this, it.id); refresh(); })));
        d[0] = UiKit.show(this, l, true);
    }

    // ---------- Ajustes ----------

    private void openSettings() {
        int ink = getColor(R.color.ink);
        LinearLayout l = UiKit.vbox(this, 12);
        TextView title = UiKit.text(this, "Ajustes", 18, ink);
        title.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 8), UiKit.dp(this, 14), UiKit.dp(this, 8));
        l.addView(title);
        TextView sec = UiKit.text(this, "Color de acento", 13, getColor(R.color.mut));
        sec.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 6), UiKit.dp(this, 14), UiKit.dp(this, 6));
        l.addView(sec);
        Dialog[] d = new Dialog[1];
        int cur = ThemeUtils.accent(this);
        int per = 6;
        LinearLayout rowL = null;
        for (int i = 0; i < ThemeUtils.ACCENT_COLORS.length; i++) {
            if (i % per == 0) {
                rowL = new LinearLayout(this);
                rowL.setPadding(UiKit.dp(this, 8), 0, 0, 0);
                l.addView(rowL);
            }
            final int color = ThemeUtils.ACCENT_COLORS[i];
            View sw = new View(this);
            sw.setBackground(UiKit.oval(this, color, ink, color == cur ? 2.5f : 0));
            sw.setOnClickListener(v -> {
                d[0].dismiss();
                ThemeUtils.setAccent(this, color);
                recreate();
            });
            LinearLayout.LayoutParams sp = UiKit.box(this, 40);
            sp.setMargins(UiKit.dp(this, 6), UiKit.dp(this, 6), UiKit.dp(this, 6), UiKit.dp(this, 6));
            rowL.addView(sw, sp);
        }
        l.addView(UiKit.sheetRow(this, Icons.TEXT, "Fuentes", ink, d, () -> UiKit.soon(this)));
        l.addView(UiKit.sheetRow(this, Icons.DOC, "Copia de seguridad", ink, d, () -> UiKit.soon(this)));
        l.addView(UiKit.sheetRow(this, Icons.HELP, "Acerca de AloNotes", ink, d, () -> UiKit.soon(this)));
        d[0] = UiKit.show(this, l, true);
    }

    // ---------- Adaptador ----------

    private static final class Views {
        FrameLayout thumb;
        TextView name, date;
        View more;
    }

    private final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public int getItemCount() { return items.size() + (folderId.isEmpty() ? 0 : 1); }

        @Override public int getItemViewType(int p) { return (!folderId.isEmpty() && p == 0) ? 0 : 1; }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            return new RecyclerView.ViewHolder(type == 0 ? buildBack() : buildItem()) {};
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
            if (getItemViewType(pos) == 0) return;
            NoteStore.Item it = items.get(pos - (folderId.isEmpty() ? 0 : 1));
            Views v = (Views) h.itemView.getTag();
            v.name.setText(it.name);
            v.date.setText(fmt.format(new Date(it.t)));
            v.thumb.removeAllViews();
            if (it.folder) {
                ImageView iv = new ImageView(MainActivity.this);
                iv.setImageDrawable(new Icons(Icons.FOLDER, ThemeUtils.accent(MainActivity.this), true));
                v.thumb.addView(iv, new FrameLayout.LayoutParams(UiKit.dp(MainActivity.this, 84), UiKit.dp(MainActivity.this, 84), Gravity.CENTER));
            } else {
                int bg = 0xFFFFFFFF;
                boolean land = true;
                JSONObject first = it.pages.optJSONObject(0);
                if (first != null) {
                    bg = first.optInt("bg", bg);
                    land = !"P".equals(first.optString("o"));
                }
                FrameLayout page = new FrameLayout(MainActivity.this);
                page.setBackground(UiKit.rect(MainActivity.this, bg, 4, getColor(R.color.ln2)));
                TextView n = UiKit.text(MainActivity.this, it.pages.length() + " pág.", 11, 0xFF777777);
                page.addView(n, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
                v.thumb.addView(page, new FrameLayout.LayoutParams(
                        UiKit.dp(MainActivity.this, land ? 150 : 82), UiKit.dp(MainActivity.this, land ? 100 : 112), Gravity.CENTER));
            }
            h.itemView.setOnClickListener(x -> {
                if (it.folder) {
                    folderId = it.id;
                    refresh();
                } else {
                    Intent i = new Intent(MainActivity.this, EditorActivity.class);
                    i.putExtra("id", it.id);
                    startActivity(i);
                }
            });
            v.more.setOnClickListener(x -> options(it));
        }
    }

    private View buildBack() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(this, 8), UiKit.dp(this, 10), UiKit.dp(this, 8), UiKit.dp(this, 14));
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(new Icons(Icons.BACK, getColor(R.color.ink), false));
        row.addView(iv, UiKit.box(this, 24));
        TextView t = UiKit.text(this, "Atrás", 17, getColor(R.color.ink));
        t.setPadding(UiKit.dp(this, 12), 0, 0, 0);
        row.addView(t);
        row.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        row.setOnClickListener(v -> goUp());
        return row;
    }

    private View buildItem() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setGravity(Gravity.CENTER_HORIZONTAL);
        v.setPadding(UiKit.dp(this, 8), UiKit.dp(this, 8), UiKit.dp(this, 8), UiKit.dp(this, 8));
        v.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        Views w = new Views();
        w.thumb = new FrameLayout(this);
        v.addView(w.thumb, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 120)));
        w.name = UiKit.text(this, "", 15, getColor(R.color.ink));
        w.name.setSingleLine(true);
        w.name.setEllipsize(TextUtils.TruncateAt.END);
        w.name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = UiKit.dp(this, 8);
        v.addView(w.name, np);
        LinearLayout dr = new LinearLayout(this);
        dr.setGravity(Gravity.CENTER_VERTICAL);
        w.date = UiKit.text(this, "", 11, getColor(R.color.mut));
        dr.addView(w.date);
        w.more = UiKit.iconBtn(this, Icons.CHEV, getColor(R.color.mut), 32, 20, null);
        dr.addView(w.more, UiKit.box(this, 32));
        v.addView(dr, new LinearLayout.LayoutParams(-2, -2));
        v.setTag(w);
        return v;
    }
}
