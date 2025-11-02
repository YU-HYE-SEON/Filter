package com.example.filter.etc;

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
}
