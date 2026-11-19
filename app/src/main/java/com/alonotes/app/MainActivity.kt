package com.alonotes.app

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Biblioteca "Mis notas": carpetas y notas en cuadrícula, menú flotante (+) y ajustes.
 * Botones sin función todavía => aviso "Próximamente".
 */
class MainActivity : Activity() {
    private var folderId = ""
    private val items = ArrayList<NoteStore.Item>()
    private lateinit var rv: RecyclerView
    private lateinit var glm: GridLayoutManager
    private lateinit var adapter: Adapter
    private lateinit var crumb: LinearLayout
    private lateinit var scrim: View
    private lateinit var fabMenu: View
    private lateinit var fab: View
    private var themeVer = 0
    private val fmt = SimpleDateFormat("EEE dd MMM, HH:mm", Locale("es"))

    private fun dp(v: Number) = UiKit.dp(this, v)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        ThemeUtils.applyTo(this)
        themeVer = ThemeUtils.version()
        if (b != null) folderId = b.getString("folder") ?: ""
        val ink = getColor(R.color.ink)

        val root = FrameLayout(this)
        root.setBackgroundColor(getColor(R.color.bg))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        root.addView(col, FrameLayout.LayoutParams(-1, -1))

        // Barra superior
        val bar = LinearLayout(this)
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setBackgroundColor(getColor(R.color.sf))
        bar.setPadding(dp(8), 0, dp(8), 0)
        bar.addView(UiKit.iconBtn(this, Icons.MENU, ink, 48, 24) { UiKit.soon(this) }, UiKit.box(this, 48))
        crumb = LinearLayout(this)
        crumb.gravity = Gravity.CENTER_VERTICAL
        crumb.setPadding(dp(8), 0, 0, 0)
        bar.addView(crumb, LinearLayout.LayoutParams(0, -2, 1f))
        for (t in intArrayOf(Icons.SORT, Icons.SEARCH, Icons.MAIL, Icons.HELP)) {
            bar.addView(UiKit.iconBtn(this, t, ink, 48, 24) { UiKit.soon(this) }, UiKit.box(this, 48))
        }
        bar.addView(UiKit.iconBtn(this, Icons.GEAR, ink, 48, 24) { openSettings() }, UiKit.box(this, 48))
        col.addView(bar, LinearLayout.LayoutParams(-1, dp(56)))
        val line = View(this)
        line.setBackgroundColor(getColor(R.color.ln))
        col.addView(line, LinearLayout.LayoutParams(-1, dp(1)))

        // Cuadrícula
        rv = RecyclerView(this)
        rv.clipToPadding = false
        rv.setPadding(dp(16), dp(12), dp(16), dp(110))
        glm = GridLayoutManager(this, spanCount())
        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (folderId.isNotEmpty() && position == 0) glm.spanCount else 1
        }
        rv.layoutManager = glm
        adapter = Adapter()
        rv.adapter = adapter
        col.addView(rv, LinearLayout.LayoutParams(-1, 0, 1f))

        // Menú flotante (+)
        scrim = View(this)
        scrim.setBackgroundColor(getColor(R.color.scrim))
        scrim.visibility = View.GONE
        scrim.setOnClickListener { toggleMenu(false) }
        root.addView(scrim, FrameLayout.LayoutParams(-1, -1))

        val menu = LinearLayout(this)
        menu.orientation = LinearLayout.VERTICAL
        menu.gravity = Gravity.END
        menu.addView(fabRow("Nuevo documento", Icons.DOC) { newDocument() })
        menu.addView(fabRow("Importar documento", Icons.PDF) { UiKit.soon(this) })
        menu.addView(fabRow("Crear carpeta", Icons.FOLDER) { newFolder() })
        menu.addView(fabRow("Cerrar menú", Icons.CLOSE) { toggleMenu(false) })
        menu.visibility = View.GONE
        fabMenu = menu
        val mp = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END)
        mp.setMargins(0, 0, dp(20), dp(20))
        root.addView(menu, mp)

        fab = circle(Icons.PLUS)
        fab.setOnClickListener { toggleMenu(true) }
        val fp = FrameLayout.LayoutParams(dp(60), dp(60), Gravity.BOTTOM or Gravity.END)
        fp.setMargins(0, 0, dp(20), dp(20))
        root.addView(fab, fp)

        setContentView(root)
    }

    private fun spanCount(): Int =
        maxOf(2, resources.displayMetrics.widthPixels / dp(180))

    override fun onConfigurationChanged(c: Configuration) {
        super.onConfigurationChanged(c)
        glm.spanCount = spanCount()
    }

    override fun onSaveInstanceState(o: Bundle) {
        super.onSaveInstanceState(o)
        o.putString("folder", folderId)
    }

    override fun onResume() {
        super.onResume()
        if (themeVer != ThemeUtils.version()) {
            recreate()
            return
        }
        refresh()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (fabMenu.visibility == View.VISIBLE) toggleMenu(false)
        else if (folderId.isNotEmpty()) goUp()
        else super.onBackPressed()
    }

    // ---------- Datos ----------

    private fun refresh() {
        if (folderId.isNotEmpty() && NoteStore.get(this, folderId) == null) folderId = ""
        items.clear()
        items.addAll(NoteStore.children(this, folderId))
        adapter.notifyDataSetChanged()
        buildCrumb()
    }

    private fun goUp() {
        val f = NoteStore.get(this, folderId)
        folderId = f?.parent ?: ""
        refresh()
    }

    private fun buildCrumb() {
        crumb.removeAllViews()
        val ink = getColor(R.color.ink)
        val r = UiKit.text(this, "Mis notas", 20, ink)
        r.setOnClickListener {
            folderId = ""
            refresh()
        }
        crumb.addView(r)
        val f = if (folderId.isEmpty()) null else NoteStore.get(this, folderId)
        if (f != null) {
            val ch = ImageView(this)
            ch.setImageDrawable(Icons(Icons.CHEV, getColor(R.color.mut), false))
            ch.rotation = -90f
            val cp = UiKit.box(this, 22)
            cp.setMargins(dp(6), 0, dp(6), 0)
            crumb.addView(ch, cp)
            val n = UiKit.text(this, f.name, 20, ink)
            n.setSingleLine(true)
            n.ellipsize = TextUtils.TruncateAt.END
            crumb.addView(n)
        }
    }

    // ---------- Menú flotante ----------

    private fun circle(icon: Int): View {
        val f = FrameLayout(this)
        val iv = ImageView(this)
        iv.setImageDrawable(Icons(icon, getColor(R.color.bg), false))
        f.addView(iv, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
        UiKit.ripple(this, f, 999, ThemeUtils.accent(this))
        f.elevation = dp(6).toFloat()
        return f
    }

    private fun fabRow(label: String, icon: Int, action: () -> Unit): View {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL
        val t = UiKit.text(this, label, 14, getColor(R.color.ink))
        t.setPadding(dp(14), dp(8), dp(14), dp(8))
        t.background = UiKit.rect(this, getColor(R.color.sf3), 8)
        val tp = LinearLayout.LayoutParams(-2, -2)
        tp.rightMargin = dp(12)
        row.addView(t, tp)
        row.addView(circle(icon), UiKit.box(this, 52))
        val rp = LinearLayout.LayoutParams(-2, -2)
        rp.bottomMargin = dp(14)
        row.layoutParams = rp
        row.setOnClickListener {
            toggleMenu(false)
            action()
        }
        return row
    }

    private fun toggleMenu(open: Boolean) {
        scrim.visibility = if (open) View.VISIBLE else View.GONE
        fabMenu.visibility = if (open) View.VISIBLE else View.GONE
        fab.visibility = if (open) View.GONE else View.VISIBLE
    }

    private fun newDocument() {
        val d = NoteStore.add(this, folderId, false, null)
        val i = Intent(this, EditorActivity::class.java)
        i.putExtra("id", d.id)
        i.putExtra("new", true)
        startActivity(i)
    }

    private fun newFolder() {
        UiKit.askName(this, "Escribe un nombre de carpeta", "") { n ->
            NoteStore.add(this, folderId, true, n)
            refresh()
        }
    }

    // ---------- Opciones de un elemento ----------

    private fun options(it: NoteStore.Item) {
        val ink = getColor(R.color.ink)
        val l = UiKit.vbox(this, 12)
        val title = UiKit.text(this, it.name, 17, ink)
        title.setSingleLine(true)
        title.ellipsize = TextUtils.TruncateAt.END
        title.setPadding(dp(14), dp(8), dp(14), dp(8))
        l.addView(title)
        val ref = DlgRef()
        l.addView(UiKit.sheetRow(this, Icons.PENCIL, "Renombrar", ink, ref) {
            UiKit.askName(this, "Renombrar", it.name) { n ->
                NoteStore.rename(this, it.id, n)
                refresh()
            }
        })
        l.addView(UiKit.sheetRow(this, Icons.FOLDER, "Mover", ink, ref) { UiKit.soon(this) })
        l.addView(UiKit.sheetRow(this, Icons.DOC, "Duplicar", ink, ref) { UiKit.soon(this) })
        l.addView(UiKit.sheetRow(this, Icons.CLOSE, "Eliminar", getColor(R.color.danger), ref) {
            val extra = if (it.folder) " Se borrará todo su contenido." else ""
            UiKit.confirm(this, "¿Eliminar «" + it.name + "»?" + extra, "Eliminar") {
                NoteStore.delete(this, it.id)
                refresh()
            }
        })
        ref.d = UiKit.show(this, l, true)
    }

    // ---------- Ajustes ----------

    private fun openSettings() {
        val ink = getColor(R.color.ink)
        val l = UiKit.vbox(this, 12)
        val title = UiKit.text(this, "Ajustes", 18, ink)
        title.setPadding(dp(14), dp(8), dp(14), dp(8))
        l.addView(title)
        val sec = UiKit.text(this, "Color de acento", 13, getColor(R.color.mut))
        sec.setPadding(dp(14), dp(6), dp(14), dp(6))
        l.addView(sec)
        val ref = DlgRef()
        val cur = ThemeUtils.accent(this)
        var rowL: LinearLayout? = null
        for ((i, color) in ThemeUtils.ACCENT_COLORS.withIndex()) {
            if (i % 6 == 0) {
                val nr = LinearLayout(this)
                nr.setPadding(dp(8), 0, 0, 0)
                l.addView(nr)
                rowL = nr
            }
            val sw = View(this)
            sw.background = UiKit.oval(this, color, ink, if (color == cur) 2.5f else 0f)
            sw.setOnClickListener {
                ref.dismiss()
                ThemeUtils.setAccent(this, color)
                recreate()
            }
            val sp = UiKit.box(this, 40)
            sp.setMargins(dp(6), dp(6), dp(6), dp(6))
            rowL!!.addView(sw, sp)
        }
        l.addView(UiKit.sheetRow(this, Icons.TEXT, "Fuentes", ink, ref) { UiKit.soon(this) })
        l.addView(UiKit.sheetRow(this, Icons.DOC, "Copia de seguridad", ink, ref) { UiKit.soon(this) })
        l.addView(UiKit.sheetRow(this, Icons.HELP, "Acerca de AloNotes", ink, ref) { UiKit.soon(this) })
        ref.d = UiKit.show(this, l, true)
    }

    // ---------- Adaptador ----------

    private class Views {
        lateinit var thumb: FrameLayout
        lateinit var name: TextView
        lateinit var date: TextView
        lateinit var more: View
    }

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int = items.size + (if (folderId.isEmpty()) 0 else 1)

        override fun getItemViewType(position: Int): Int =
            if (folderId.isNotEmpty() && position == 0) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = if (viewType == 0) buildBack() else buildItem()
            return object : RecyclerView.ViewHolder(v) {}
        }

        override fun onBindViewHolder(h: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == 0) return
            val it = items[position - (if (folderId.isEmpty()) 0 else 1)]
            val v = h.itemView.tag as Views
            v.name.text = it.name
            v.date.text = fmt.format(Date(it.t))
            v.thumb.removeAllViews()
            if (it.folder) {
                val iv = ImageView(this@MainActivity)
                iv.setImageDrawable(Icons(Icons.FOLDER, ThemeUtils.accent(this@MainActivity), true))
                v.thumb.addView(iv, FrameLayout.LayoutParams(dp(84), dp(84), Gravity.CENTER))
            } else {
                var bg = 0xFFFFFFFF.toInt()
                var land = true
                val first = it.pages.optJSONObject(0)
                if (first != null) {
                    bg = first.optInt("bg", bg)
                    land = first.optString("o") != "P"
                }
                val page = FrameLayout(this@MainActivity)
                page.background = UiKit.rect(this@MainActivity, bg, 4, getColor(R.color.ln2))
                val n = UiKit.text(this@MainActivity, it.pages.length().toString() + " pág.", 11, 0xFF777777.toInt())
                page.addView(n, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
                v.thumb.addView(
                    page,
                    FrameLayout.LayoutParams(dp(if (land) 150 else 82), dp(if (land) 100 else 112), Gravity.CENTER)
                )
            }
            h.itemView.setOnClickListener { _ ->
                if (it.folder) {
                    folderId = it.id
                    refresh()
                } else {
                    val i = Intent(this@MainActivity, EditorActivity::class.java)
                    i.putExtra("id", it.id)
                    startActivity(i)
                }
            }
            v.more.setOnClickListener { _ -> options(it) }
        }
    }

    private fun buildBack(): View {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(8), dp(10), dp(8), dp(14))
        val iv = ImageView(this)
        iv.setImageDrawable(Icons(Icons.BACK, getColor(R.color.ink), false))
        row.addView(iv, UiKit.box(this, 24))
        val t = UiKit.text(this, "Atrás", 17, getColor(R.color.ink))
        t.setPadding(dp(12), 0, 0, 0)
        row.addView(t)
        row.layoutParams = RecyclerView.LayoutParams(-1, -2)
        row.setOnClickListener { goUp() }
        return row
    }

    private fun buildItem(): View {
        val v = LinearLayout(this)
        v.orientation = LinearLayout.VERTICAL
        v.gravity = Gravity.CENTER_HORIZONTAL
        v.setPadding(dp(8), dp(8), dp(8), dp(8))
        v.layoutParams = RecyclerView.LayoutParams(-1, -2)
        val w = Views()
        w.thumb = FrameLayout(this)
        v.addView(w.thumb, LinearLayout.LayoutParams(-1, dp(120)))
        w.name = UiKit.text(this, "", 15, getColor(R.color.ink))
        w.name.setSingleLine(true)
        w.name.ellipsize = TextUtils.TruncateAt.END
        w.name.gravity = Gravity.CENTER
        val np = LinearLayout.LayoutParams(-1, -2)
        np.topMargin = dp(8)
        v.addView(w.name, np)
        val dr = LinearLayout(this)
        dr.gravity = Gravity.CENTER_VERTICAL
        w.date = UiKit.text(this, "", 11, getColor(R.color.mut))
        dr.addView(w.date)
        w.more = UiKit.iconBtn(this, Icons.CHEV, getColor(R.color.mut), 32, 20)
        dr.addView(w.more, UiKit.box(this, 32))
        v.addView(dr, LinearLayout.LayoutParams(-2, -2))
        v.tag = w
        return v
    }
}
