package com.example.filter.etc;

public class ClickUtils {
    private static long lastClickTime = 0;

    public static boolean isFastClick(long intervalMillis) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < intervalMillis) {
            return true;
        }
        lastClickTime = currentTime;
        return false;
    }
}
