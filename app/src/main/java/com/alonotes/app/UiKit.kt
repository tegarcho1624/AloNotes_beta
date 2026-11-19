package com.alonotes.app

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

/** Referencia mutable a un diálogo (para poder cerrarlo desde sus propios botones). */
internal class DlgRef {
    var d: Dialog? = null
    fun dismiss() { d?.dismiss() }
}

/** Componentes de interfaz compartidos: hojas inferiores, diálogos, botones, filas. */
internal object UiKit {
    fun dp(c: Context, v: Number): Int =
        Math.round(v.toFloat() * c.resources.displayMetrics.density)

    fun rect(c: Context, fill: Int, rDp: Number): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.cornerRadius = dp(c, rDp).toFloat()
        return g
    }

    fun rect(c: Context, fill: Int, rDp: Number, stroke: Int): GradientDrawable {
        val g = rect(c, fill, rDp)
        g.setStroke(dp(c, 1), stroke)
        return g
    }

    fun oval(c: Context, fill: Int, stroke: Int, strokeDp: Number): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(fill)
        if (strokeDp.toFloat() > 0f) g.setStroke(dp(c, strokeDp), stroke)
        return g
    }

    fun soon(c: Context) {
        Toast.makeText(c, R.string.soon, Toast.LENGTH_SHORT).show()
    }

    fun text(c: Context, s: CharSequence, sp: Number, color: Int): TextView {
        val t = TextView(c)
        t.text = s
        t.textSize = sp.toFloat()
        t.setTextColor(color)
        return t
    }

    fun ripple(c: Context, v: View, rDp: Number, fill: Int) {
        val content: Drawable? = if (fill != 0) rect(c, fill, rDp) else null
        v.background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, rect(c, Color.WHITE, rDp))
    }

    fun box(c: Context, dpSize: Number): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(c, dpSize), dp(c, dpSize))

    /** Botón de ícono: caja clicable con el ícono centrado. */
    fun iconBtn(c: Context, type: Int, color: Int, boxDp: Number, iconDp: Number, l: (() -> Unit)? = null): FrameLayout {
        val f = FrameLayout(c)
        val iv = ImageView(c)
        iv.setImageDrawable(Icons(type, color, false))
        f.addView(iv, FrameLayout.LayoutParams(dp(c, iconDp), dp(c, iconDp), Gravity.CENTER))
        ripple(c, f, 999, 0)
        if (l != null) f.setOnClickListener { l() }
        return f
    }

    fun vbox(c: Context, padDp: Number): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        val p = dp(c, padDp)
        l.setPadding(p, p, p, p)
        return l
    }

    fun btn(c: Context, s: String, color: Int): TextView {
        val t = text(c, s.uppercase(Locale.getDefault()), 13, color)
        t.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10))
        t.gravity = Gravity.CENTER
        ripple(c, t, 8, c.getColor(R.color.sf3))
        return t
    }

    /** Fila de botones alineada a la derecha. */
    fun buttons(c: Context, vararg bs: View): LinearLayout {
        val r = LinearLayout(c)
        r.gravity = Gravity.END
        for (b in bs) {
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.leftMargin = dp(c, 10)
            r.addView(b, lp)
        }
        return r
    }

    /** Muestra contenido en una tarjeta: hoja inferior (bottom) o diálogo centrado. */
    fun show(a: Activity, content: View, bottom: Boolean): Dialog {
        val d = Dialog(a)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val card = FrameLayout(a)
        card.background = rect(a, a.getColor(R.color.sheet), if (bottom) 24 else 20, a.getColor(R.color.ln))
        card.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        d.setContentView(card, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val w = d.window!!
        w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        w.setDimAmount(0.6f)
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val sw = a.resources.displayMetrics.widthPixels
        w.setLayout(minOf(sw - dp(a, 32), dp(a, if (bottom) 560 else 440)), WindowManager.LayoutParams.WRAP_CONTENT)
        w.setGravity(if (bottom) Gravity.BOTTOM else Gravity.CENTER)
        if (bottom) {
            val lp = w.attributes
            lp.y = dp(a, 12)
            w.attributes = lp
        }
        d.show()
        return d
    }

    /** Fila de una hoja inferior; cierra la hoja (ref) antes de ejecutar la acción. */
    fun sheetRow(a: Activity, icon: Int, label: String, color: Int, ref: DlgRef, r: () -> Unit): View {
        val row = LinearLayout(a)
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(a, 14), dp(a, 14), dp(a, 14), dp(a, 14))
        val iv = ImageView(a)
        iv.setImageDrawable(Icons(icon, color, false))
        row.addView(iv, box(a, 22))
        val t = text(a, label, 16, color)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.leftMargin = dp(a, 16)
        row.addView(t, lp)
        ripple(a, row, 12, 0)
        row.setOnClickListener {
            ref.dismiss()
            r()
        }
        return row
    }

    /** Diálogo para pedir un nombre (crear/renombrar). */
    fun askName(a: Activity, title: String, initial: String, ok: (String) -> Unit) {
        val l = vbox(a, 22)
        l.addView(text(a, title, 17, a.getColor(R.color.ink)))
        val e = EditText(a)
        e.setSingleLine(true)
        e.setText(initial)
        e.setSelection(initial.length)
        e.setTextColor(a.getColor(R.color.ink))
        e.backgroundTintList = ColorStateList.valueOf(ThemeUtils.accent(a))
        val ep = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ep.topMargin = dp(a, 14)
        ep.bottomMargin = dp(a, 18)
        l.addView(e, ep)
        val ref = DlgRef()
        val no = btn(a, "Cancelar", a.getColor(R.color.mut))
        val yes = btn(a, "Aceptar", ThemeUtils.accent(a))
        no.setOnClickListener { ref.dismiss() }
        yes.setOnClickListener {
            val s = e.text.toString().trim()
            if (s.isNotEmpty()) {
                ref.dismiss()
                ok(s)
            }
        }
        l.addView(buttons(a, no, yes))
        ref.d = show(a, l, false)
        e.requestFocus()
    }

    /** Confirmación simple (por ejemplo, eliminar). */
    fun confirm(a: Activity, msg: String, yesLabel: String, yes: () -> Unit) {
        val l = vbox(a, 22)
        val t = text(a, msg, 16, a.getColor(R.color.ink))
        t.setPadding(0, 0, 0, dp(a, 18))
        l.addView(t)
        val ref = DlgRef()
        val no = btn(a, "Cancelar", a.getColor(R.color.mut))
        val ok = btn(a, yesLabel, a.getColor(R.color.danger))
        no.setOnClickListener { ref.dismiss() }
        ok.setOnClickListener {
            ref.dismiss()
            yes()
        }
        l.addView(buttons(a, no, ok))
        ref.d = show(a, l, false)
    }

    /** Lista desplegable oscura anclada a una vista. */
    fun dropdown(a: Activity, anchor: View, opts: Array<String>, pick: (Int) -> Unit) {
        val l = LinearLayout(a)
        l.orientation = LinearLayout.VERTICAL
        l.background = rect(a, a.getColor(R.color.sf2), 12, a.getColor(R.color.ln2))
        val pw = PopupWindow(l, dp(a, 210), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        pw.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        pw.elevation = dp(a, 8).toFloat()
        for (i in opts.indices) {
            val t = text(a, opts[i], 15, a.getColor(R.color.ink))
            t.setPadding(dp(a, 18), dp(a, 14), dp(a, 18), dp(a, 14))
            t.setSingleLine(true)
            t.ellipsize = TextUtils.TruncateAt.END
            ripple(a, t, 8, 0)
            t.setOnClickListener {
                pw.dismiss()
                pick(i)
            }
            l.addView(t)
        }
        pw.showAsDropDown(anchor, 0, 0, Gravity.END)
    }
}
