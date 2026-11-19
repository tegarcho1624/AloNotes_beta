package com.alonotes.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

/**
 * Tema y acento de AloNotes. El acento se guarda en SharedPreferences "settings",
 * clave "accent" (ARGB int). Por defecto dorado (#C9A24B). Nunca uses colores de
 * marca fijos: lee siempre de aquí.
 */
internal object ThemeUtils {
    val ACCENT_COLORS = intArrayOf(
        0xFFC9A24B.toInt(), 0xFFE57373.toInt(), 0xFFFF8A65.toInt(), 0xFFFFD54F.toInt(),
        0xFF81C784.toInt(), 0xFF4DB6AC.toInt(), 0xFF4FC3F7.toInt(), 0xFF5C8DDB.toInt(),
        0xFF9575CD.toInt(), 0xFFBA68C8.toInt(), 0xFFF06292.toInt(), 0xFFB0BEC5.toInt()
    )

    private var ver = 0

    fun version(): Int = ver

    private fun sp(c: Context): SharedPreferences =
        c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun accent(c: Context): Int = sp(c).getInt("accent", ACCENT_COLORS[0])

    fun accentHi(c: Context): Int {
        val h = FloatArray(3)
        Color.colorToHSV(accent(c), h)
        h[2] = minOf(1f, h[2] + 0.12f)
        h[1] = maxOf(0f, h[1] - 0.10f)
        return Color.HSVToColor(h)
    }

    fun accentBg(c: Context): Int = (accent(c) and 0x00FFFFFF) or 0x26000000

    fun setAccent(c: Context, color: Int) {
        sp(c).edit().putInt("accent", color).apply()
        ver++
    }

    /** Llamar en onCreate de toda Activity, justo después de super.onCreate. */
    fun applyTo(a: Activity) {
        a.window.statusBarColor = a.getColor(R.color.bg)
        a.window.navigationBarColor = a.getColor(R.color.bg)
    }
}
