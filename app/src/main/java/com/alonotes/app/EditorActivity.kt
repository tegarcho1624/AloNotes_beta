package com.alonotes.app

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Editor de una nota: barra de herramientas (por ahora todas "Próximamente" salvo
 * volver y crear páginas) y lienzo con las páginas creadas.
 * No hay botón de lápiz flotante: el lápiz del borde es el panel edge de la tablet.
 */
class EditorActivity : Activity() {
    private companion object {
        val DIMS = arrayOf("A5", "A4", "A3", "A2")
        val DIM_LABELS = arrayOf("A5 (148 x 210 mm)", "A4 (210 x 297 mm)", "A3 (297 x 420 mm)", "A2 (420 x 594 mm)")
        val MM = arrayOf(
            floatArrayOf(148f, 210f), floatArrayOf(210f, 297f),
            floatArrayOf(297f, 420f), floatArrayOf(420f, 594f)
        )
        val PAGE_COLORS = intArrayOf(
            0xFFFFFFFF.toInt(), 0xFFF5EBD7.toInt(), 0xFFFFF6C9.toInt(), 0xFFDDF3E4.toInt(),
            0xFFDCEBFA.toInt(), 0xFFFADCE6.toInt(), 0xFFEDEDED.toInt(), 0xFF1A1A1A.toInt()
        )
    }

    private var docId = ""
    private lateinit var pagesCol: LinearLayout
    private lateinit var sv: ScrollView
    private lateinit var counter: TextView
    private var total = 0

    private fun dp(v: Number) = UiKit.dp(this, v)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        ThemeUtils.applyTo(this)
        val id = intent.getStringExtra("id")
        if (id == null || NoteStore.get(this, id) == null) {
            finish()
            return
        }
        docId = id
        val ink = getColor(R.color.ink)
        val mut = getColor(R.color.mut)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setBackgroundColor(getColor(R.color.bg))

        // Barra de herramientas
        val bar = LinearLayout(this)
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(6), 0, dp(6), 0)
        bar.addView(tb(Icons.BACK, ink) { finish() }, UiKit.box(this, 44))
        bar.addView(tb(Icons.GRID, ink) { UiKit.soon(this) }, UiKit.box(this, 44))
        bar.addView(tb(Icons.PLUS, ink) { createPages() }, UiKit.box(this, 44))
        bar.addView(View(this), LinearLayout.LayoutParams(dp(22), 1))
        val tools = intArrayOf(
            Icons.PEN, Icons.MARKER, Icons.ERASER, Icons.TEXT,
            Icons.SHAPES, Icons.IMAGE, Icons.STAR, Icons.SELECT
        )
        for (t in tools) bar.addView(tb(t, ink) { UiKit.soon(this) }, UiKit.box(this, 44))
        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        bar.addView(tb(Icons.UNDO, mut) { UiKit.soon(this) }, UiKit.box(this, 44))
        counter = UiKit.text(this, "0 / 0", 17, ink)
        counter.gravity = Gravity.CENTER
        bar.addView(counter, LinearLayout.LayoutParams(dp(90), -2))
        bar.addView(tb(Icons.REDO, mut) { UiKit.soon(this) }, UiKit.box(this, 44))
        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        bar.addView(tb(Icons.LOCK, ink) { UiKit.soon(this) }, UiKit.box(this, 44))
        bar.addView(tb(Icons.ROT_L, mut) { UiKit.soon(this) }, UiKit.box(this, 44))
        bar.addView(tb(Icons.ROT_R, mut) { UiKit.soon(this) }, UiKit.box(this, 44))
        val div = View(this)
        div.setBackgroundColor(getColor(R.color.ln2))
        val dv = LinearLayout.LayoutParams(dp(1), dp(24))
        dv.setMargins(dp(8), 0, dp(8), 0)
        bar.addView(div, dv)
        bar.addView(tb(Icons.GEAR, ink) { UiKit.soon(this) }, UiKit.box(this, 44))
        val hsv = HorizontalScrollView(this)
        hsv.isFillViewport = true
        hsv.isHorizontalScrollBarEnabled = false
        hsv.setBackgroundColor(getColor(R.color.sf))
        hsv.addView(bar, ViewGroup.LayoutParams(-1, -1))
        col.addView(hsv, LinearLayout.LayoutParams(-1, dp(52)))
        val line = View(this)
        line.setBackgroundColor(getColor(R.color.ln))
        col.addView(line, LinearLayout.LayoutParams(-1, dp(1)))

        // Lienzo de páginas
        sv = ScrollView(this)
        sv.setBackgroundColor(getColor(R.color.sf2))
        pagesCol = LinearLayout(this)
        pagesCol.orientation = LinearLayout.VERTICAL
        pagesCol.gravity = Gravity.CENTER_HORIZONTAL
        pagesCol.setPadding(dp(24), dp(24), dp(24), dp(120))
        sv.addView(pagesCol, ViewGroup.LayoutParams(-1, -2))
        sv.setOnScrollChangeListener { _, _, _, _, _ -> updateCounter() }
        col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(col)
        renderPages()
        if (b == null && intent.getBooleanExtra("new", false) && total == 0) createPages()
    }

    private fun tb(type: Int, color: Int, l: () -> Unit): View =
        UiKit.iconBtn(this, type, color, 44, 24, l)

    // ---------- Páginas ----------

    private fun renderPages() {
        val doc = NoteStore.get(this, docId)
        pagesCol.removeAllViews()
        total = doc?.pages?.length() ?: 0
        if (doc == null || total == 0) {
            val e = UiKit.text(this, "Sin páginas todavía", 16, getColor(R.color.mut))
            e.setPadding(0, dp(80), 0, dp(16))
            pagesCol.addView(e)
            val bt = UiKit.btn(this, "Crear páginas", ThemeUtils.accent(this))
            bt.setOnClickListener { createPages() }
            pagesCol.addView(bt, LinearLayout.LayoutParams(-2, -2))
            updateCounter()
            return
        }
        val sw = resources.displayMetrics.widthPixels
        val base = minOf(sw - dp(64), dp(720))
        for (i in 0 until total) {
            val pg = doc.pages.optJSONObject(i) ?: continue
            var di = 1
            for (k in DIMS.indices) if (DIMS[k] == pg.optString("d")) di = k
            val ratio = MM[di][1] / MM[di][0]
            val land = pg.optString("o") != "P"
            val w = if (land) base else Math.round(base * 0.72f)
            val h = if (land) Math.round(base / ratio) else Math.round(w * ratio)
            val page = View(this)
            page.background = UiKit.rect(this, pg.optInt("bg", 0xFFFFFFFF.toInt()), 2)
            page.elevation = dp(4).toFloat()
            val lp = LinearLayout.LayoutParams(w, h)
            lp.bottomMargin = dp(20)
            pagesCol.addView(page, lp)
        }
        updateCounter()
    }

    private fun updateCounter() {
        if (total == 0) {
            counter.text = "0 / 0"
            return
        }
        val center = sv.scrollY + sv.height / 2
        var cur = total
        for (i in 0 until pagesCol.childCount) {
            if (pagesCol.getChildAt(i).bottom >= center) {
                cur = i + 1
                break
            }
        }
        counter.text = minOf(cur, total).toString() + " / " + total
    }

    // ---------- Diálogo "Crear páginas nuevas" ----------

    private fun selector(initial: String, opts: Array<String>, sel: IntArray, softBlockOthers: Boolean): View {
        val s = LinearLayout(this)
        s.gravity = Gravity.CENTER_VERTICAL
        s.setPadding(dp(10), dp(8), dp(6), dp(8))
        val t = UiKit.text(this, initial, 16, getColor(R.color.ink))
        s.addView(t)
        val ch = ImageView(this)
        ch.setImageDrawable(Icons(Icons.CHEV, getColor(R.color.ink), false))
        val cp = UiKit.box(this, 20)
        cp.leftMargin = dp(6)
        s.addView(ch, cp)
        UiKit.ripple(this, s, 8, 0)
        s.setOnClickListener {
            UiKit.dropdown(this, s, opts) { i ->
                if (softBlockOthers && i != 0) {
                    UiKit.soon(this) // Imagen y Patrón llegarán más adelante
                } else {
                    sel[0] = i
                    t.text = opts[i]
                }
            }
        }
        return s
    }

    private fun formRow(label: String, right: View, rightWidth: Int = -2): View {
        val r = LinearLayout(this)
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(UiKit.text(this, label, 16, getColor(R.color.ink)), LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(right, LinearLayout.LayoutParams(rightWidth, -2))
        r.setPadding(0, dp(6), 0, dp(6))
        return r
    }

    private fun createPages() {
        val dim = intArrayOf(1)
        val ori = intArrayOf(0)
        val bgSel = intArrayOf(0)
        val color = intArrayOf(PAGE_COLORS[0])
        val l = UiKit.vbox(this, 22)
        val title = UiKit.text(this, "Crear páginas nuevas", 18, getColor(R.color.ink))
        title.setPadding(0, 0, 0, dp(8))
        l.addView(title)
        l.addView(formRow("Dimensiones", selector(DIM_LABELS[1], DIM_LABELS, dim, false)))
        l.addView(formRow("Orientación", selector("Horizontal", arrayOf("Horizontal", "Vertical"), ori, false)))
        l.addView(formRow("Fondo", selector("Color", arrayOf("Color", "Imagen", "Patrón"), bgSel, true)))
        val sw = View(this)
        sw.background = UiKit.oval(this, color[0], getColor(R.color.ln2), 1)
        val colorBox = LinearLayout(this)
        colorBox.gravity = Gravity.CENTER_VERTICAL
        colorBox.addView(UiKit.text(this, "Color:", 15, getColor(R.color.mut)))
        val swp = UiKit.box(this, 36)
        swp.leftMargin = dp(10)
        colorBox.addView(sw, swp)
        sw.setOnClickListener {
            pickColor { c ->
                color[0] = c
                sw.background = UiKit.oval(this, c, getColor(R.color.ln2), 1)
            }
        }
        val cr = LinearLayout(this)
        cr.gravity = Gravity.END
        cr.addView(colorBox)
        l.addView(cr)
        val num = EditText(this)
        num.inputType = InputType.TYPE_CLASS_NUMBER
        num.setText("1")
        num.gravity = Gravity.END
        num.setSingleLine(true)
        num.setTextColor(getColor(R.color.ink))
        num.backgroundTintList = ColorStateList.valueOf(ThemeUtils.accent(this))
        l.addView(formRow("Número de páginas", num, dp(90)))
        val ref = DlgRef()
        val no = UiKit.btn(this, "Cancelar", getColor(R.color.mut))
        val ok = UiKit.btn(this, "Crear", ThemeUtils.accent(this))
        no.setOnClickListener { ref.dismiss() }
        ok.setOnClickListener {
            val n = (num.text.toString().trim().toIntOrNull() ?: 1).coerceIn(1, 50)
            NoteStore.addPages(this, docId, n, DIMS[dim[0]], ori[0] == 0, color[0])
            ref.dismiss()
            renderPages()
        }
        val br = UiKit.buttons(this, no, ok)
        br.setPadding(0, dp(14), 0, 0)
        l.addView(br)
        ref.d = UiKit.show(this, l, false)
    }

    private fun pickColor(got: (Int) -> Unit) {
        val l = UiKit.vbox(this, 18)
        val t = UiKit.text(this, "Color de la página", 17, getColor(R.color.ink))
        t.setPadding(dp(4), 0, 0, dp(10))
        l.addView(t)
        val ref = DlgRef()
        var row: LinearLayout? = null
        for ((i, c) in PAGE_COLORS.withIndex()) {
            if (i % 4 == 0) {
                val nr = LinearLayout(this)
                l.addView(nr)
                row = nr
            }
            val s = View(this)
            s.background = UiKit.oval(this, c, getColor(R.color.ln2), 1)
            s.setOnClickListener {
                ref.dismiss()
                got(c)
            }
            val sp = UiKit.box(this, 52)
            sp.setMargins(dp(8), dp(8), dp(8), dp(8))
            row!!.addView(s, sp)
        }
        ref.d = UiKit.show(this, l, false)
    }
}
