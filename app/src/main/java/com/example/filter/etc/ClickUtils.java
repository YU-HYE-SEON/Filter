package com.example.filter.etc;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class ClickUtils {
    private static final Map<Integer, Long> lastClickMap = new HashMap<>();

    public static boolean isFastClick(View v, long intervalMillis) {
        int viewId = v.getId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastClickMap.get(viewId);

        if (lastTime != null && (currentTime - lastTime) < intervalMillis) {
            return true;
        }

        lastClickMap.put(viewId, currentTime);
        return false;
    }

    public static void disableTemporarily(View v, long intervalMillis) {
        v.setEnabled(false);
        v.postDelayed(() -> v.setEnabled(true), intervalMillis);
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void clickDim(View button) {
        button.setOnTouchListener((v, event) -> {
            Drawable drawable = v.getForeground();
            if (drawable == null) {
                drawable = v.getBackground();
            }
            if (drawable == null) return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    drawable.setColorFilter(0x50000000, PorterDuff.Mode.SRC_ATOP);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    drawable.clearColorFilter();
                    break;
            }
            return false;
        });
    }
}
