package com.example.filter.etc;

import android.content.Context;
import android.content.SharedPreferences;

public final class BrushPrefs {
    private static final String P = "brush_prefs";
    private static final String K_COLOR = "pen_argb";
    private static final String K_SIZE = "pen_size_px";

    private BrushPrefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static void save(Context c, int argb, int sizePx) {
        sp(c).edit()
                .putInt(K_COLOR, argb)
                .putInt(K_SIZE, Math.max(1, sizePx))
                .apply();
    }

    public static int getColor(Context c, int defArgb) {
        return sp(c).getInt(K_COLOR, defArgb);
    }

    public static int getSize(Context c, int defSizePx) {
        return sp(c).getInt(K_SIZE, Math.max(1, defSizePx));
    }
}