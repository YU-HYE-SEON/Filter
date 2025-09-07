package com.example.filter.etc;

import android.content.Context;
import android.content.SharedPreferences;

public final class BrushPrefs {
    private static final String P = "brush_prefs";
    private static final String K_PEN_COLOR = "pen_color";
    private static final String K_PEN_SIZE = "pen_size";
    private static final String K_GLOW_COLOR = "glow_color";
    private static final String K_GLOW_SIZE = "glow_size";
    private static final String K_ERASER_SIZE = "eraser_size";

    private BrushPrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static void savePen(Context c, int argb, int sizePx) {
        sp(c).edit()
                .putInt(K_PEN_COLOR, argb)
                .putInt(K_PEN_SIZE, Math.max(1, sizePx))
                .apply();
    }

    public static int getPenColor(Context c, int defArgb) {
        return sp(c).getInt(K_PEN_COLOR, defArgb);
    }

    public static int getPenSize(Context c, int defSizePx) {
        return sp(c).getInt(K_PEN_SIZE, Math.max(1, defSizePx));
    }

    public static void saveGlow(Context c, int argb, int sizePx) {
        sp(c).edit()
                .putInt(K_GLOW_COLOR, argb)
                .putInt(K_GLOW_SIZE, Math.max(1, sizePx))
                .apply();
    }

    public static int getGlowColor(Context c, int defArgb) {
        return sp(c).getInt(K_GLOW_COLOR, defArgb);
    }

    public static int getGlowSize(Context c, int defSizePx) {
        return sp(c).getInt(K_GLOW_SIZE, Math.max(1, defSizePx));
    }

    public static void saveEraser(Context c, int sizePx) {
        sp(c).edit().putInt(K_ERASER_SIZE, Math.max(1, sizePx)).apply();
    }

    public static int getEraserSize(Context c, int defSizePx) {
        return sp(c).getInt(K_ERASER_SIZE, Math.max(1, defSizePx));
    }
}