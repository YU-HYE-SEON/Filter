package com.example.filter.etc;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;

import com.example.filter.R;

public class Controller {
    private static final int WRAPPER_MIN_DP = 100;
    private static final int CTRL_BASE_DP = 30;
    private static final int CTRL_MIN_DP = 22;
    private static final float ROT_MIN_RADIUS_DP = 24f;
    private static float downRawX, downRawY, startX, startY;
    private static int startW, startH;
    private static float rotStartWrapperDeg, rotLastAngleDeg, rotAccumDeg;

    public static void setStickerActive(View view, boolean active) {
        ImageView img = view.findViewById(R.id.stickerImage);

        view.setEnabled(active);
        view.setClickable(active);

        if (img != null) {
            if (active) {
                img.setAlpha(1.0f);
                img.clearColorFilter();
            } else {
                img.setAlpha(0.4f);
                img.setColorFilter(Color.parseColor("#808080"), PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    public static void raiseStickerToAbsoluteTop(View sticker, ViewGroup parent) {
        float maxZ = 0f;
        for (int i = 0; i < parent.getChildCount(); i++) {
            maxZ = Math.max(maxZ, ViewCompat.getZ(parent.getChildAt(i)));
        }
        ViewCompat.setZ(sticker, maxZ + 1000f);
        sticker.bringToFront();
        parent.invalidate();
    }

    public static void updateControllerAngles(View stickerWrapper, View rotateController) {
        if (stickerWrapper == null) return;
        float r = stickerWrapper.getRotation();
        if (rotateController != null) rotateController.setRotation(-r);
    }

    public static void updateControllersSizeAndAngle(View stickerWrapper, View editFrame, View rotateController, View sizeController, Resources resources) {
        if (stickerWrapper == null) return;
        int ctrlPx = Math.max(dp(CTRL_MIN_DP, resources), dp(CTRL_BASE_DP, resources));
        applySize(rotateController, ctrlPx, ctrlPx);
        applySize(sizeController, ctrlPx, ctrlPx);
        updateControllerAngles(stickerWrapper, rotateController);
        stickerWrapper.post(() -> positionControllers(stickerWrapper, editFrame, rotateController, sizeController));
    }

    public static float[] getTouchInOverlay(MotionEvent e, View view) {
        int[] ov = new int[2];
        view.getLocationOnScreen(ov);
        return new float[]{e.getRawX() - ov[0], e.getRawY() - ov[1]};
    }

    public static float[] getWrapperCenterInOverlay(View stickerWrapper) {
        return new float[]{
                stickerWrapper.getX() + stickerWrapper.getPivotX(),
                stickerWrapper.getY() + stickerWrapper.getPivotY()
        };
    }

    public static void positionControllers(View stickerWrapper, View editFrame, View rotateController, View sizeController) {
        if (stickerWrapper == null || editFrame == null || rotateController == null || sizeController == null)
            return;

        float fx = editFrame.getX();
        float fy = editFrame.getY();
        float fw = editFrame.getWidth();
        float fh = editFrame.getHeight();

        int rcW = rotateController.getWidth(), rcH = rotateController.getHeight();
        int scW = sizeController.getWidth(), scH = sizeController.getHeight();

        rotateController.setX(fx - rcW / 2f);
        rotateController.setY(fy - rcH / 2f);

        sizeController.setX(fx + fw - scW / 2f);
        sizeController.setY(fy + fh - scH / 2f);
    }

    public static float angleDeg(float cx, float cy, float x, float y) {
        return (float) Math.toDegrees(Math.atan2(y - cy, x - cx));
    }

    public static float shortestDeltaDeg(float fromDeg, float toDeg) {
        float d = toDeg - fromDeg;
        while (d > 180f) d -= 360f;
        while (d <= -180f) d += 360f;
        return d;
    }

    public static void applySize(View v, int w, int h) {
        if (v == null) return;
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp == null) return;
        lp.width = w;
        lp.height = h;
        v.setLayoutParams(lp);
    }

    public static void removeStickerWrapper(View stickerWrapper) {
        if (stickerWrapper == null) return;
        ViewGroup parent = (ViewGroup) stickerWrapper.getParent();
        if (parent != null) parent.removeView(stickerWrapper);
        //stickerWrapper = null;
    }

    public static void setControllersVisible(View sticker, boolean visible) {
        View editFrame = sticker.findViewById(R.id.editFrame);
        View rotate = sticker.findViewById(R.id.rotateController);
        View size = sticker.findViewById(R.id.sizeController);

        int vis = visible ? View.VISIBLE : View.INVISIBLE;

        if (editFrame != null) editFrame.setVisibility(vis);
        if (rotate != null) rotate.setVisibility(vis);
        if (size != null) size.setVisibility(vis);
    }

    public static void hideControllers(View sticker) {
        View editFrame = sticker.findViewById(R.id.editFrame);
        View rotateController = sticker.findViewById(R.id.rotateController);
        View sizeController = sticker.findViewById(R.id.sizeController);
        hideControllers(editFrame, rotateController, sizeController, null);
    }

    public static void hideControllers(View editFrame, View rotateController, View sizeController, View stickerWrapper) {
        if (editFrame != null) editFrame.setVisibility(View.INVISIBLE);
        if (rotateController != null) rotateController.setVisibility(View.INVISIBLE);
        if (sizeController != null) sizeController.setVisibility(View.INVISIBLE);
        if (stickerWrapper != null) stickerWrapper.setTag(null);
    }

    public static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.hypot(dx, dy);
    }

    public static int dp(int dp, Resources resources) {
        float d = resources.getDisplayMetrics().density;
        return Math.round(dp * d);
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void enableStickerControl(View stickerWrapper, View editFrame, View rotateController, View sizeController, View view, Resources resources) {
        editFrame.setVisibility(View.VISIBLE);
        rotateController.setVisibility(View.VISIBLE);
        sizeController.setVisibility(View.VISIBLE);

        View.OnTouchListener moveListener = (v, event) -> {
            if (stickerWrapper == null) return false;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = stickerWrapper.getX();
                    startY = stickerWrapper.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    stickerWrapper.setX(startX + (event.getRawX() - downRawX));
                    stickerWrapper.setY(startY + (event.getRawY() - downRawY));
                    return true;
                /*case MotionEvent.ACTION_UP:
                    updateMeta(lastFaceBox, stickerWrapper, meta);
                    return true;*/
            }
            return false;
        };
        editFrame.setOnTouchListener(moveListener);

        rotateController.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    ViewParent p = v.getParent();
                    if (p != null) p.requestDisallowInterceptTouchEvent(true);
                    float[] c = getWrapperCenterInOverlay(stickerWrapper);
                    float[] t = getTouchInOverlay(event, view);
                    float r = dist(t[0], t[1], c[0], c[1]);
                    if (r < dp((int) ROT_MIN_RADIUS_DP, resources)) return false;
                    rotStartWrapperDeg = stickerWrapper.getRotation();
                    rotLastAngleDeg = angleDeg(c[0], c[1], t[0], t[1]);
                    rotAccumDeg = 0f;
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float[] c = getWrapperCenterInOverlay(stickerWrapper);
                    float[] t = getTouchInOverlay(event, view);
                    float r = dist(t[0], t[1], c[0], c[1]);
                    if (r < dp((int) ROT_MIN_RADIUS_DP, resources)) return true;
                    float cur = angleDeg(c[0], c[1], t[0], t[1]);
                    float step = shortestDeltaDeg(rotLastAngleDeg, cur);
                    if (Math.abs(step) > 60f) {
                        rotLastAngleDeg = cur;
                        return true;
                    }
                    rotAccumDeg += step;
                    rotLastAngleDeg = cur;
                    float target = rotStartWrapperDeg + rotAccumDeg;
                    stickerWrapper.setRotation(target);
                    updateControllerAngles(stickerWrapper, rotateController);
                    return true;
                }
                /*case MotionEvent.ACTION_UP:
                    updateMeta(lastFaceBox, stickerWrapper, meta);
                    return true;*/
            }
            return false;
        });

        sizeController.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    float[] c = getWrapperCenterInOverlay(stickerWrapper);
                    float[] t = getTouchInOverlay(event, view);
                    float vx = t[0] - c[0];
                    float vy = t[1] - c[1];
                    float startRadius = (float) Math.hypot(vx, vy);
                    if (startRadius < 1f) return false;
                    startW = Math.max(1, stickerWrapper.getLayoutParams().width);
                    startH = Math.max(1, stickerWrapper.getLayoutParams().height);
                    v.setTag(new float[]{c[0], c[1], vx / startRadius, vy / startRadius, startRadius});
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float[] tag = (float[]) v.getTag();
                    if (tag == null) return false;
                    float cX = tag[0], cY = tag[1], dirX = tag[2], dirY = tag[3], startRadius = tag[4];
                    float[] t = getTouchInOverlay(event, view);
                    float cx = t[0] - cX, cy = t[1] - cY;
                    float proj = cx * dirX + cy * dirY;
                    float rawScale = proj / startRadius;

                    int minSizePx = dp(WRAPPER_MIN_DP, resources);
                    float minScaleW = (float) minSizePx / (float) startW;
                    float minScaleH = (float) minSizePx / (float) startH;
                    float minScale = Math.max(minScaleW, minScaleH);
                    if (rawScale < minScale) rawScale = minScale;

                    int newW = Math.max(minSizePx, Math.round(startW * rawScale));
                    int newH = Math.max(minSizePx, Math.round(startH * rawScale));

                    float centerX = stickerWrapper.getX() + stickerWrapper.getWidth() / 2f;
                    float centerY = stickerWrapper.getY() + stickerWrapper.getHeight() / 2f;

                    stickerWrapper.getLayoutParams().width = newW;
                    stickerWrapper.getLayoutParams().height = newH;
                    stickerWrapper.requestLayout();
                    stickerWrapper.setPivotX(newW / 2f);
                    stickerWrapper.setPivotY(newH / 2f);
                    stickerWrapper.setX(centerX - newW / 2f);
                    stickerWrapper.setY(centerY - newH / 2f);

                    updateControllersSizeAndAngle(stickerWrapper, editFrame, rotateController, sizeController, resources);
                    return true;
                }
                /*case MotionEvent.ACTION_UP:
                    updateMeta(lastFaceBox, stickerWrapper, meta);
                    return true;*/
            }
            return false;
        });
    }

    /*public static void updateMeta(Rect lastFaceBox, View stickerWrapper, Bundle meta) {
        if (lastFaceBox == null || stickerWrapper == null) return;

        float stickerX = stickerWrapper.getX();
        float stickerY = stickerWrapper.getY();
        float stickerW = stickerWrapper.getWidth();
        float stickerH = stickerWrapper.getHeight();
        float stickerR = stickerWrapper.getRotation();

        float stickerCenterX = stickerX + stickerW / 2f;
        float stickerCenterY = stickerY + stickerH / 2f;

        float faceCenterX = lastFaceBox.centerX();
        float faceCenterY = lastFaceBox.centerY();

        float relX = (stickerCenterX - faceCenterX) / lastFaceBox.width();
        float relY = (stickerCenterY - faceCenterY) / lastFaceBox.height();
        float relW = stickerW / (float) lastFaceBox.width();
        float relH = stickerH / (float) lastFaceBox.height();

        meta = new Bundle();
        meta.putFloat("relX", relX);
        meta.putFloat("relY", relY);
        meta.putFloat("relW", relW);
        meta.putFloat("relH", relH);
        meta.putFloat("stickerR", stickerR);
    }*/
}