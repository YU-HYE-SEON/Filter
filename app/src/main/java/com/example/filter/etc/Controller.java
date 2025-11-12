package com.example.filter.etc;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.filter.R;
import com.example.filter.fragments.EditStickerFragment;
import com.google.mlkit.vision.face.Face;

import java.util.List;

public class Controller {
    private static final int FRAME_MIN_DP = 100;
    private static final int CTRL_BASE_DP = 30;
    private static final int CTRL_MIN_DP = 22;
    private static final float ROT_MIN_RADIUS_DP = 24f;
    private static float downRawX, downRawY, startX, startY;
    private static int startW, startH;
    private static float startDeg, endDeg, accumDeg;

    public static void clearCurrentSticker(FrameLayout stickerOverlay, View selectSticker) {
        if (selectSticker != null && selectSticker.getParent() == stickerOverlay) {
            //Object isTemp = selectSticker.getTag(TAG_TEMP_PREVIEW);
            //if (Boolean.TRUE.equals(isTemp)) {
            stickerOverlay.removeView(selectSticker);
            //}
        }
        selectSticker = null;
    }

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
                img.setColorFilter(Color.parseColor("#505050"), PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    public static void updateControllerAngles(View stickerFrame, View controller) {
        if (stickerFrame == null) return;
        float r = stickerFrame.getRotation();
        if (controller != null) controller.setRotation(-r);
    }

    public static void updateControllersSizeAndAngle(View stickerFrame, Resources resources) {
        if (stickerFrame == null) return;

        View moveController = stickerFrame.findViewById(R.id.moveController);
        View rotateController = stickerFrame.findViewById(R.id.rotateController);
        View sizeController = stickerFrame.findViewById(R.id.sizeController);
        View deleteController = stickerFrame.findViewById(R.id.deleteController);

        int ctrlPx = Math.max(dp(CTRL_MIN_DP, resources), dp(CTRL_BASE_DP, resources));
        applySize(rotateController, ctrlPx, ctrlPx);
        applySize(sizeController, ctrlPx, ctrlPx);
        applySize(deleteController, ctrlPx, ctrlPx);
        updateControllerAngles(stickerFrame, rotateController);
        updateControllerAngles(stickerFrame, deleteController);
        stickerFrame.post(() -> positionControllers(stickerFrame));
    }

    public static float[] getTouchInOverlay(MotionEvent e, View view) {
        int[] ov = new int[2];
        view.getLocationOnScreen(ov);
        return new float[]{e.getRawX() - ov[0], e.getRawY() - ov[1]};
    }

    public static float[] getFrameCenterInOverlay(View stickerFrame) {
        return new float[]{
                stickerFrame.getX() + stickerFrame.getPivotX(),
                stickerFrame.getY() + stickerFrame.getPivotY()
        };
    }

    public static void positionControllers(View stickerFrame) {
        View moveController = stickerFrame.findViewById(R.id.moveController);
        View rotateController = stickerFrame.findViewById(R.id.rotateController);
        View sizeController = stickerFrame.findViewById(R.id.sizeController);
        View deleteController = stickerFrame.findViewById(R.id.deleteController);

        if (stickerFrame == null || moveController == null || rotateController == null || sizeController == null)
            return;

        float fx = moveController.getX();
        float fy = moveController.getY();
        float fw = moveController.getWidth();
        float fh = moveController.getHeight();

        int rcW = rotateController.getWidth(), rcH = rotateController.getHeight();
        int scW = sizeController.getWidth(), scH = sizeController.getHeight();
        int dcW = deleteController.getWidth(), dcH = deleteController.getHeight();

        rotateController.setX(fx - rcW / 2f);
        rotateController.setY(fy - rcH / 2f);

        sizeController.setX(fx + fw - scW / 2f);
        sizeController.setY(fy + fh - scH / 2f);

        deleteController.setX(fx + fw - dcW / 2f);
        deleteController.setY(fy - dcH / 2f);
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

    public static void removeStickerFrame(View stickerFrame) {
        if (stickerFrame == null) return;
        ViewGroup parent = (ViewGroup) stickerFrame.getParent();
        if (parent != null) parent.removeView(stickerFrame);
        //stickerFrame = null;
    }

    public static void setControllersVisible(View stickerFrame, boolean visible) {
        View moveController = stickerFrame.findViewById(R.id.moveController);
        View rotateController = stickerFrame.findViewById(R.id.rotateController);
        View sizeController = stickerFrame.findViewById(R.id.sizeController);
        View deleteController = stickerFrame.findViewById(R.id.deleteController);

        int vis = visible ? View.VISIBLE : View.INVISIBLE;

        if (moveController != null) moveController.setVisibility(vis);
        if (rotateController != null) rotateController.setVisibility(vis);
        if (sizeController != null) sizeController.setVisibility(vis);
        if (deleteController != null) deleteController.setVisibility(vis);
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
    public static void enableStickerControl(Face face, Bitmap bitmap, View stickerFrame, FrameLayout stickerOverlay, FrameLayout faceOverlay, Resources resources) {
        View moveController = stickerFrame.findViewById(R.id.moveController);
        View rotateController = stickerFrame.findViewById(R.id.rotateController);
        View sizeController = stickerFrame.findViewById(R.id.sizeController);
        View deleteController = stickerFrame.findViewById(R.id.deleteController);

        moveController.setVisibility(View.VISIBLE);
        rotateController.setVisibility(View.VISIBLE);
        sizeController.setVisibility(View.VISIBLE);
        deleteController.setVisibility(View.VISIBLE);

        if (stickerFrame == null) return;

        moveController.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = stickerFrame.getX();
                    startY = stickerFrame.getY();
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    stickerFrame.setX(startX + (event.getRawX() - downRawX));
                    stickerFrame.setY(startY + (event.getRawY() - downRawY));
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    if (EditStickerFragment.isFace && face != null && bitmap != null) {
                        StickerMeta.calculate(face, bitmap, stickerFrame, faceOverlay);
                    }

                    //stickerFrame.post(() -> {
                    //    Log.d("스티커", String.format("에딧 스티커 이동 | 스티커프레임 pivotX = %.1f, pivotY = %.1f, x = %.1f, y = %.1f, w=%d, h=%d, r=%.1f",
                    //            stickerFrame.getPivotX(), stickerFrame.getPivotY(), stickerFrame.getX(), stickerFrame.getY(), stickerFrame.getWidth(), stickerFrame.getHeight(), stickerFrame.getRotation()));
                    //});
                }
            }
            return false;
        });


        rotateController.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    ViewParent p = v.getParent();
                    if (p != null) p.requestDisallowInterceptTouchEvent(true);
                    float[] c = getFrameCenterInOverlay(stickerFrame);
                    float[] t = getTouchInOverlay(event, stickerOverlay);
                    float r = dist(t[0], t[1], c[0], c[1]);
                    if (r < dp((int) ROT_MIN_RADIUS_DP, resources)) return false;
                    startDeg = stickerFrame.getRotation();
                    endDeg = angleDeg(c[0], c[1], t[0], t[1]);
                    accumDeg = 0f;
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float[] c = getFrameCenterInOverlay(stickerFrame);
                    float[] t = getTouchInOverlay(event, stickerOverlay);
                    float r = dist(t[0], t[1], c[0], c[1]);
                    if (r < dp((int) ROT_MIN_RADIUS_DP, resources)) return true;
                    float cur = angleDeg(c[0], c[1], t[0], t[1]);
                    float step = shortestDeltaDeg(endDeg, cur);
                    if (Math.abs(step) > 30f) {
                        endDeg = cur;
                        return true;
                    }
                    accumDeg += step;
                    endDeg = cur;
                    float target = startDeg + accumDeg;
                    stickerFrame.setRotation(target);
                    updateControllerAngles(stickerFrame, rotateController);
                    updateControllerAngles(stickerFrame, deleteController);
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    if (EditStickerFragment.isFace && face != null && bitmap != null) {
                        StickerMeta.calculate(face, bitmap, stickerFrame, faceOverlay);
                    }

                    //stickerFrame.post(() -> {
                    //    Log.d("스티커", String.format("에딧 스티커 회전 | 스티커프레임 pivotX = %.1f, pivotY = %.1f, x = %.1f, y = %.1f, w=%d, h=%d, r=%.1f",
                    //            stickerFrame.getPivotX(), stickerFrame.getPivotY(), stickerFrame.getX(), stickerFrame.getY(), stickerFrame.getWidth(), stickerFrame.getHeight(), stickerFrame.getRotation()));
                    //});
                }
            }
            return false;
        });

        sizeController.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    float[] c = getFrameCenterInOverlay(stickerFrame);
                    float[] t = getTouchInOverlay(event, stickerOverlay);
                    float vx = t[0] - c[0];
                    float vy = t[1] - c[1];
                    float startRadius = (float) Math.hypot(vx, vy);
                    if (startRadius < 1f) return false;
                    startW = Math.max(1, stickerFrame.getLayoutParams().width);
                    startH = Math.max(1, stickerFrame.getLayoutParams().height);
                    v.setTag(new float[]{c[0], c[1], vx / startRadius, vy / startRadius, startRadius});
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float[] tag = (float[]) v.getTag();
                    if (tag == null) return false;
                    float cX = tag[0], cY = tag[1], dirX = tag[2], dirY = tag[3], startRadius = tag[4];
                    float[] t = getTouchInOverlay(event, stickerOverlay);
                    float cx = t[0] - cX, cy = t[1] - cY;
                    float proj = cx * dirX + cy * dirY;
                    float rawScale = proj / startRadius;

                    int minSizePx = dp(FRAME_MIN_DP, resources);
                    float minScaleW = (float) minSizePx / (float) startW;
                    float minScaleH = (float) minSizePx / (float) startH;
                    float minScale = Math.max(minScaleW, minScaleH);
                    if (rawScale < minScale) rawScale = minScale;

                    int newW = Math.max(minSizePx, Math.round(startW * rawScale));
                    int newH = Math.max(minSizePx, Math.round(startH * rawScale));

                    float centerX = stickerFrame.getX() + stickerFrame.getWidth() / 2f;
                    float centerY = stickerFrame.getY() + stickerFrame.getHeight() / 2f;

                    stickerFrame.getLayoutParams().width = newW;
                    stickerFrame.getLayoutParams().height = newH;
                    stickerFrame.requestLayout();
                    stickerFrame.setPivotX(newW / 2f);
                    stickerFrame.setPivotY(newH / 2f);
                    stickerFrame.setX(centerX - newW / 2f);
                    stickerFrame.setY(centerY - newH / 2f);

                    updateControllersSizeAndAngle(stickerFrame, resources);
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    if (EditStickerFragment.isFace && face != null && bitmap != null) {
                        StickerMeta.calculate(face, bitmap, stickerFrame, faceOverlay);
                    }

                    //stickerFrame.post(() -> {
                    //    Log.d("스티커", String.format("에딧 스티커 크기 | 스티커프레임 pivotX = %.1f, pivotY = %.1f, x = %.1f, y = %.1f, w=%d, h=%d, r=%.1f",
                    //            stickerFrame.getPivotX(), stickerFrame.getPivotY(), stickerFrame.getX(), stickerFrame.getY(), stickerFrame.getWidth(), stickerFrame.getHeight(), stickerFrame.getRotation()));
                    //});
                }
            }
            return false;
        });
    }
}