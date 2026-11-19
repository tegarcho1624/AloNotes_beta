package com.alonotes.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Window;

/**
 * Tema y acento de AloNotes. El acento se guarda en SharedPreferences "settings",
 * clave "accent" (ARGB int). Por defecto dorado (#C9A24B). Nunca uses colores de
 * marca fijos: lee siempre de aquí.
 */
final class ThemeUtils {
    private ThemeUtils() {}

    static final int[] ACCENT_COLORS = {
        0xFFC9A24B, 0xFFE57373, 0xFFFF8A65, 0xFFFFD54F, 0xFF81C784, 0xFF4DB6AC,
        0xFF4FC3F7, 0xFF5C8DDB, 0xFF9575CD, 0xFFBA68C8, 0xFFF06292, 0xFFB0BEC5
    };

    private static int version = 0;

    static int version() { return version; }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static int accent(Context c) { return sp(c).getInt("accent", ACCENT_COLORS[0]); }

    static int accentHi(Context c) {
        float[] h = new float[3];
        Color.colorToHSV(accent(c), h);
        h[2] = Math.min(1f, h[2] + 0.12f);
        h[1] = Math.max(0f, h[1] - 0.10f);
        return Color.HSVToColor(h);
    }

    static int accentBg(Context c) { return (accent(c) & 0x00FFFFFF) | 0x26000000; }

    static void setAccent(Context c, int color) {
        sp(c).edit().putInt("accent", color).apply();
        version++;
    }

    /** Llamar en onCreate de toda Activity, justo después de super.onCreate. */
    static void applyTo(Activity a) {
        Window w = a.getWindow();
        w.setStatusBarColor(a.getColor(R.color.bg));
        w.setNavigationBarColor(a.getColor(R.color.bg));
    }
}
