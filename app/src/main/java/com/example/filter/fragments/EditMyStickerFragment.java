package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class EditMyStickerFragment extends Fragment {
    private View stickerWrapper;
    private ImageView editFrame, stickerImage, rotateController, sizeController;
    private ImageButton cancelBtn, checkBtn;
    private float origX, origY, origRotation;
    private int origW, origH;
    private float downRawX, downRawY, startX, startY;
    private float rotStartWrapperDeg, rotLastAngleDeg, rotAccumDeg;
    private int startW, startH;
    private FrameLayout stickerOverlay;
    private static final int CTRL_BASE_DP = 30;
    private static final int CTRL_MIN_DP = 22;
    private static final int WRAPPER_MIN_DP = 100;
    private final float ROT_MIN_RADIUS_DP = 24f;
    public static String sLastReturnOriginAction = null;
    private boolean[] prevEnabledSnapshot;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_edit_my_sticker, container, false);

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        cancelBtn = v.findViewById(R.id.cancelBtn);
        checkBtn = v.findViewById(R.id.checkBtn);

        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        if (stickerOverlay == null) return;

        stickerWrapper = stickerOverlay.findViewWithTag("editingSticker");
        if (stickerWrapper == null && stickerOverlay.getChildCount() > 0) {
            stickerWrapper = stickerOverlay.getChildAt(stickerOverlay.getChildCount() - 1);
        }
        if (stickerWrapper == null) return;

        editFrame = stickerWrapper.findViewById(R.id.editFrame);
        stickerImage = stickerWrapper.findViewById(R.id.stickerImage);
        rotateController = stickerWrapper.findViewById(R.id.rotateController);
        sizeController = stickerWrapper.findViewById(R.id.sizeController);

        if (stickerImage == null || editFrame == null || rotateController == null || sizeController == null) {
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        if (editFrame != null) editFrame.setVisibility(View.VISIBLE);
        if (rotateController != null) rotateController.setVisibility(View.VISIBLE);
        if (sizeController != null) sizeController.setVisibility(View.VISIBLE);

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        String origin = args.getString("origin", "mystickers");
        prevEnabledSnapshot = args.getBooleanArray("prevEnabled");

        final int sessionBaseline = args.getInt("sessionBaseline",
                (stickerOverlay != null) ? stickerOverlay.getChildCount() : 0);

        origX = args.getFloat("x", stickerWrapper.getX());
        origY = args.getFloat("y", stickerWrapper.getY());
        origW = args.getInt("w", stickerWrapper.getLayoutParams().width);
        origH = args.getInt("h", stickerWrapper.getLayoutParams().height);
        origRotation = args.getFloat("rotation", stickerWrapper.getRotation());

        int minPx = dp(WRAPPER_MIN_DP);
        int initW = Math.max(minPx, origW);
        int initH = Math.max(minPx, origH);

        stickerWrapper.setX(origX);
        stickerWrapper.setY(origY);
        stickerWrapper.getLayoutParams().width = initW;
        stickerWrapper.getLayoutParams().height = initH;
        stickerWrapper.requestLayout();
        stickerWrapper.setRotation(origRotation);

        stickerWrapper.post(() -> {
            stickerWrapper.setPivotX(stickerWrapper.getWidth() / 2f);
            stickerWrapper.setPivotY(stickerWrapper.getHeight() / 2f);
            positionControllers();
        });

        stickerWrapper.setOnClickListener(null);
        stickerWrapper.setClickable(false);

        updateControllersSizeAndAngle();

        View.OnTouchListener moveListener = (view, event) -> {
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
            }
            return false;
        };
        editFrame.setOnTouchListener(moveListener);

        rotateController.setOnTouchListener((view1, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    ViewParent p = view1.getParent();
                    if (p != null) p.requestDisallowInterceptTouchEvent(true);
                    float[] c = getWrapperCenterInOverlay();
                    float[] t = getTouchInOverlay(event);
                    float r = dist(t[0], t[1], c[0], c[1]);
                    if (r < dp((int) ROT_MIN_RADIUS_DP)) return false;
                    rotStartWrapperDeg = stickerWrapper.getRotation();
                    rotLastAngleDeg = angleDeg(c[0], c[1], t[0], t[1]);
                    rotAccumDeg = 0f;
                    view1.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float[] c = getWrapperCenterInOverlay();
                    float[] t = getTouchInOverlay(event);
                    float r = dist(t[0], t[1], c[0], c[1]);
                    if (r < dp((int) ROT_MIN_RADIUS_DP)) return true;
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
                    updateControllerAngles();
                    return true;
                }
            }
            return false;
        });

        sizeController.setOnTouchListener((view12, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    float[] c = getWrapperCenterInOverlay();
                    float[] t = getTouchInOverlay(event);
                    float vx = t[0] - c[0];
                    float vy = t[1] - c[1];
                    float startRadius = (float) Math.hypot(vx, vy);
                    if (startRadius < 1f) return false;
                    startW = Math.max(1, stickerWrapper.getLayoutParams().width);
                    startH = Math.max(1, stickerWrapper.getLayoutParams().height);
                    view12.setTag(new float[]{c[0], c[1], vx / startRadius, vy / startRadius, startRadius});
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float[] tag = (float[]) view12.getTag();
                    if (tag == null) return false;
                    float cX = tag[0], cY = tag[1], dirX = tag[2], dirY = tag[3], startRadius = tag[4];
                    float[] t = getTouchInOverlay(event);
                    float cx = t[0] - cX, cy = t[1] - cY;
                    float proj = cx * dirX + cy * dirY;
                    float rawScale = proj / startRadius;

                    int minSizePx = dp(WRAPPER_MIN_DP);
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

                    updateControllersSizeAndAngle();
                    return true;
                }
            }
            return false;
        });

        cancelBtn.setOnClickListener(x -> {
            if (ClickUtils.isFastClick(500)) return;

            removeStickerWrapper();
            hideControllers();

            if ("stickers".equals(origin)) {
                if (stickerOverlay != null) {
                    for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                        View child = stickerOverlay.getChildAt(i);
                        MyStickersFragment.setStickerActive(child, true);
                        child.setTag(R.id.tag_prev_enabled, null);
                    }
                }
                goBackTo(new StickersFragment());
            } else {
                if (stickerOverlay != null) {
                    if (prevEnabledSnapshot != null &&
                            prevEnabledSnapshot.length == stickerOverlay.getChildCount()) {
                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            View child = stickerOverlay.getChildAt(i);
                            MyStickersFragment.setStickerActive(child, prevEnabledSnapshot[i]);
                            child.setTag(R.id.tag_prev_enabled, null);
                        }
                    } else {
                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            View child = stickerOverlay.getChildAt(i);
                            Object prev = child.getTag(R.id.tag_prev_enabled);
                            boolean prevEnabled = (prev instanceof Boolean) ? (Boolean) prev : true;
                            MyStickersFragment.setStickerActive(child, prevEnabled);
                            child.setTag(R.id.tag_prev_enabled, null);
                        }
                    }
                }
                sLastReturnOriginAction = "mystickers_cancel";
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        checkBtn.setOnClickListener(x -> {
            if (ClickUtils.isFastClick(500)) return;

            if (stickerWrapper != null) {
                MyStickersFragment.setStickerActive(stickerWrapper, true);
                stickerWrapper.setTag("sessionAdded");
            }

            hideControllers();

            if ("stickers".equals(origin)) {
                if (stickerOverlay != null) {
                    for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                        View child = stickerOverlay.getChildAt(i);
                        MyStickersFragment.setStickerActive(child, true);
                        child.setTag(R.id.tag_prev_enabled, null);
                    }
                }
                goBackTo(new StickersFragment());
            } else {
                if (stickerOverlay != null) {
                    if (prevEnabledSnapshot != null &&
                            prevEnabledSnapshot.length == stickerOverlay.getChildCount()) {
                    for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                        View child = stickerOverlay.getChildAt(i);
                        MyStickersFragment.setStickerActive(child, prevEnabledSnapshot[i]);
                        child.setTag(R.id.tag_prev_enabled, null);
                    }
                }
                    else{
                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            View child = stickerOverlay.getChildAt(i);
                            Object prev = child.getTag(R.id.tag_prev_enabled);
                            boolean prevEnabled = (prev instanceof Boolean) ? (Boolean) prev : true;
                            MyStickersFragment.setStickerActive(child, prevEnabled);
                            child.setTag(R.id.tag_prev_enabled, null);
                        }
                    }
                }
                sLastReturnOriginAction = "mystickers_check";
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void updateControllerAngles() {
        if (stickerWrapper == null) return;
        float r = stickerWrapper.getRotation();
        if (rotateController != null) rotateController.setRotation(-r);
    }

    private void updateControllersSizeAndAngle() {
        if (stickerWrapper == null) return;
        int ctrlPx = Math.max(dp(CTRL_MIN_DP), dp(CTRL_BASE_DP));
        applySize(rotateController, ctrlPx, ctrlPx);
        applySize(sizeController, ctrlPx, ctrlPx);
        updateControllerAngles();
        stickerWrapper.post(this::positionControllers);
    }

    private float[] getTouchInOverlay(MotionEvent e) {
        int[] ov = new int[2];
        stickerOverlay.getLocationOnScreen(ov);
        return new float[]{e.getRawX() - ov[0], e.getRawY() - ov[1]};
    }

    private float[] getWrapperCenterInOverlay() {
        return new float[]{
                stickerWrapper.getX() + stickerWrapper.getPivotX(),
                stickerWrapper.getY() + stickerWrapper.getPivotY()
        };
    }

    private void positionControllers() {
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

    private float angleDeg(float cx, float cy, float x, float y) {
        return (float) Math.toDegrees(Math.atan2(y - cy, x - cx));
    }

    private float shortestDeltaDeg(float fromDeg, float toDeg) {
        float d = toDeg - fromDeg;
        while (d > 180f) d -= 360f;
        while (d <= -180f) d += 360f;
        return d;
    }

    private void applySize(View v, int w, int h) {
        if (v == null) return;
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp == null) return;
        lp.width = w;
        lp.height = h;
        v.setLayoutParams(lp);
    }

    private void removeStickerWrapper() {
        if (stickerWrapper == null) return;
        ViewGroup parent = (ViewGroup) stickerWrapper.getParent();
        if (parent != null) parent.removeView(stickerWrapper);
        stickerWrapper = null;
    }

    private void hideControllers() {
        if (editFrame != null) editFrame.setVisibility(View.INVISIBLE);
        if (rotateController != null) rotateController.setVisibility(View.INVISIBLE);
        if (sizeController != null) sizeController.setVisibility(View.INVISIBLE);
        if (stickerWrapper != null) stickerWrapper.setTag(null);
    }

    private void goBackTo(Fragment f) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_up, 0)
                .replace(R.id.bottomArea2, f)
                .commit();
    }

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.hypot(dx, dy);
    }

    private int dp(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }
}