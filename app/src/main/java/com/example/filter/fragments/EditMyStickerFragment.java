package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.ClickUtils;
import com.google.mlkit.vision.face.Face;

public class EditMyStickerFragment extends Fragment {
    private ConstraintLayout topArea;
    private FrameLayout photoPreviewContainer, faceOverlay;
    private View stickerWrapper;
    private ImageView editFrame, stickerImage, rotateController, sizeController;
    private CheckBox checkBox;
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
    ImageButton undoSticker, redoSticker, originalSticker;
    private Float prevElevation = null;
    private Float entryX = null, entryY = null, entryR = null;
    private Integer entryW = null, entryH = null;
    private static final float EPS_POS = 0.5f;
    private static final float EPS_ROT = 0.5f;
    private static final int EPS_SIZE = 1;
    private static final float FACE_STICKER_SCALE_BOOST = 1.0f;
    //private FaceBoxOverlayView faceBox;
    private boolean isToastVisible = false;

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

        checkBox = v.findViewById(R.id.checkBox);
        cancelBtn = v.findViewById(R.id.cancelBtn);
        checkBtn = v.findViewById(R.id.checkBtn);

        topArea = requireActivity().findViewById(R.id.topArea);
        photoPreviewContainer = requireActivity().findViewById(R.id.photoPreviewContainer);
        faceOverlay = requireActivity().findViewById(R.id.faceOverlay);

        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        if (stickerOverlay == null) return;

        undoSticker = requireActivity().findViewById(R.id.undoSticker);
        redoSticker = requireActivity().findViewById(R.id.redoSticker);
        originalSticker = requireActivity().findViewById(R.id.originalSticker);

        if (undoSticker != null) undoSticker.setVisibility(View.INVISIBLE);
        if (redoSticker != null) redoSticker.setVisibility(View.INVISIBLE);
        if (originalSticker != null) originalSticker.setVisibility(View.INVISIBLE);

        stickerWrapper = stickerOverlay.findViewWithTag("editingSticker");
        if (stickerWrapper == null && stickerOverlay.getChildCount() > 0) {
            stickerWrapper = stickerOverlay.getChildAt(stickerOverlay.getChildCount() - 1);
        }
        if (stickerWrapper == null) return;

        if (getArguments() != null && getArguments().containsKey("prevElevation")) {
            prevElevation = getArguments().getFloat("prevElevation", ViewCompat.getZ(stickerWrapper));
        } else {
            prevElevation = ViewCompat.getZ(stickerWrapper);
        }
        raiseStickerToAbsoluteTop(stickerWrapper, stickerOverlay);

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

            entryX = stickerWrapper.getX();
            entryY = stickerWrapper.getY();
            entryW = stickerWrapper.getLayoutParams().width;
            entryH = stickerWrapper.getLayoutParams().height;
            entryR = stickerWrapper.getRotation();
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
            if (ClickUtils.isFastClick(x, 400)) return;

            if (checkBox.isChecked() && photoPreviewContainer != null && faceOverlay != null) {
                setView(false);
            }

            if ("stickers".equals(origin)) {
                restoreElevationIfNeeded();

                FilterActivity a = (FilterActivity) requireActivity();
                if (stickerWrapper != null) {
                    a.recordStickerDelete(stickerWrapper);
                }
            }

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
                restoreElevationIfNeeded();

                sLastReturnOriginAction = "mystickers_cancel";
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        checkBtn.setOnClickListener(x -> {
            if (ClickUtils.isFastClick(x, 400)) return;

            if (checkBox.isChecked() && photoPreviewContainer != null && faceOverlay != null) {
                requireActivity().getSupportFragmentManager().setFragmentResultListener(
                        "stickerMeta", this, (key, meta) -> {
                            if (!isAdded()) return;

                            float relX = meta.getFloat("relX");
                            float relY = meta.getFloat("relY");
                            float relW = meta.getFloat("relW");
                            float relH = meta.getFloat("relH");
                            float rot = meta.getFloat("rot");

                            Log.d("얼굴인식", "========================= 스티커 메타데이터 최종 =========================");
                            Log.d("얼굴인식", String.format("relX=%.3f, relY=%.3f, relW=%.3f, relH=%.3f, rot=%.1f", relX, relY, relW, relH, rot));

                            setView(false);
                            View stickerToClone = stickerWrapper;
                            removeStickerWrapper();

                            FilterActivity activity = (FilterActivity) getActivity();
                            if (activity == null) return;

                            activity.getPhotoPreview().queueEvent(() -> {
                                Bitmap bmp = activity.getRenderer().getCurrentBitmap();
                                if (bmp == null) {
                                    Log.e("얼굴인식", "메인 사진 비트맵 없음");

                                    if (isAdded()) {
                                        activity.runOnUiThread(() -> {
                                            showToast("얼굴을 감지하지 못했습니다");
                                            sLastReturnOriginAction = "mystickers_check";
                                            requireActivity().getSupportFragmentManager().popBackStack();
                                        });
                                    }
                                    return;
                                }
                                activity.runOnUiThread(() -> StickersFragment.detectFaces(bmp, (faces, originalBitmap) -> {
                                    if (!isAdded()) return;

                                    Log.d("얼굴인식", "메인 사진에서 " + faces.size() + "개의 얼굴 감지. 스티커 배치 시작.");

                                    if (faces.isEmpty()) {
                                        showToast("얼굴을 감지하지 못했습니다");
                                    } else {
                                        FrameLayout stickerOverlay = activity.findViewById(R.id.stickerOverlay);
                                        if (stickerOverlay != null) {
                                            int vpX = activity.getRenderer().getViewportX();
                                            int vpY = activity.getRenderer().getViewportY();
                                            int vpW = activity.getRenderer().getViewportWidth();
                                            int vpH = activity.getRenderer().getViewportHeight();
                                            int bmpW = originalBitmap.getWidth();
                                            int bmpH = originalBitmap.getHeight();

                                            float scale = Math.min((float) vpW / bmpW, (float) vpH / bmpH);
                                            float offsetX = vpX + (vpW - bmpW * scale) / 2f;
                                            float offsetY = vpY + (vpH - bmpH * scale) / 2f;

                                            for (Face face : faces) {
                                                Rect box = face.getBoundingBox();

                                                float faceTiltAngle = face.getHeadEulerAngleZ();
                                                //faceTiltAngle = Math.max(-40f, Math.min(40f, faceTiltAngle));
                                                //float finalRotation = rot - faceTiltAngle;
                                                float finalRotation = rot;

                                                float stickerCenterX_bmp = box.left + relX * box.width();
                                                float stickerCenterY_bmp = box.top + relY * box.height();
                                                float stickerW_bmp = Math.round(relW * box.width() * FACE_STICKER_SCALE_BOOST);
                                                float stickerH_bmp = Math.round(relH * box.height() * FACE_STICKER_SCALE_BOOST);

                                                if (stickerW_bmp <= 0 || stickerH_bmp <= 0)
                                                    continue;

                                                float stickerCenterX_view = (stickerCenterX_bmp * scale) + offsetX;
                                                float stickerCenterY_view = (stickerCenterY_bmp * scale) + offsetY;
                                                int stickerW_view = Math.round(stickerW_bmp * scale);
                                                int stickerH_view = Math.round(stickerH_bmp * scale);

                                                float stickerX_view = stickerCenterX_view - (stickerW_view / 2f);
                                                float stickerY_view = stickerCenterY_view - (stickerH_view / 2f);

                                                View newSticker = cloneSticker(stickerToClone);
                                                if (newSticker == null) continue;

                                                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(stickerW_view, stickerH_view);
                                                newSticker.setLayoutParams(lp);
                                                newSticker.setX(stickerX_view);
                                                newSticker.setY(stickerY_view);
                                                newSticker.setRotation(finalRotation);
                                                newSticker.setTag("sessionAdded");

                                                MyStickersFragment.hideControllers(newSticker);
                                                MyStickersFragment.setStickerActive(newSticker, true);

                                                stickerOverlay.addView(newSticker);
                                            }
                                            activity.recordStickerPlacement(sessionBaseline);
                                        }
                                        showToast("얼굴 인식 성공");
                                    }
                                    sLastReturnOriginAction = "mystickers_check";
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                }));
                            });
                        });

                Fragment ff = getParentFragmentManager().findFragmentById(R.id.faceOverlay);
                if (ff instanceof FaceFragment) {
                    ((FaceFragment) ff).detectFaces();
                }

                /*setView(false);
                removeStickerWrapper();

                FilterActivity activity = (FilterActivity) requireActivity();
                StickersFragment.faceBox = new FaceBoxOverlayView(requireContext());
                photoPreviewContainer.addView(StickersFragment.faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                activity.getPhotoPreview().queueEvent(() -> {
                    Bitmap bmp = activity.getRenderer().getCurrentBitmap();
                    activity.runOnUiThread(() -> StickersFragment.detectFaces(bmp));

                });*/
            }

            /*if (faceFragment instanceof FaceFragment && checkBox.isChecked()) {
                requireActivity().getSupportFragmentManager().setFragmentResultListener("faceMeta", this, (k, meta) -> {
                    setView(false);

                    photoPreviewContainer.postDelayed(() -> {
                        FGLRenderer renderer = ((FilterActivity) requireActivity()).getRenderer();
                        Bitmap bmp = renderer.getCurrentBitmap();

                        if (bmp == null) {
                            Log.e("얼굴인식", "Renderer bitmap is null, skip detection");
                            return;
                        }

                        List<Face> faces = detectAllFaces(bmp);

                        if (faces.isEmpty()) {
                            Log.w("얼굴인식", "No faces detected in photoPreview");
                            //requireActivity().getSupportFragmentManager().popBackStack();
                            return;
                        }

                        float relX = meta.getFloat("relX");
                        float relY = meta.getFloat("relY");
                        float relW = meta.getFloat("relW");
                        float relH = meta.getFloat("relH");
                        float rot = meta.getFloat("rot");

                        FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
                        for (Face fc : faces) {
                            Rect box = fc.getBoundingBox();
                            float absX = box.left + relX * box.width();
                            float absY = box.top + relY * box.height();
                            int absW = Math.round(relW * box.width());
                            int absH = Math.round(relH * box.height());

                            View clone = StickerUtils.cloneSticker(requireContext(), stickerWrapper);
                            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(absW, absH);
                            clone.setLayoutParams(lp);
                            clone.setX(absX);
                            clone.setY(absY);
                            //clone.setX(absCenterX - absW / 2f);
                            //clone.setY(absCenterY - absH / 2f);
                            clone.setRotation(rot);
                            clone.setTag("sessionAdded");
                            stickerOverlay.addView(clone);
                        }
                    }, 120);

                    //requireActivity().getSupportFragmentManager().popBackStack();
                });

                //((FaceFragment) faceFragment).detectFaces();
            }*/
            else {
                if (stickerWrapper != null) {
                    MyStickersFragment.setStickerActive(stickerWrapper, true);
                    stickerWrapper.setTag("sessionAdded");
                }

                hideControllers();

                if ("stickers".equals(origin)) {
                    FilterActivity a = (FilterActivity) requireActivity();

                    int aw = stickerWrapper.getLayoutParams().width;
                    int ah = stickerWrapper.getLayoutParams().height;
                    float ax = stickerWrapper.getX();
                    float ay = stickerWrapper.getY();
                    float ar = stickerWrapper.getRotation();

                    boolean samePos = (entryX != null && entryY != null)
                            && Math.abs(ax - entryX) < EPS_POS
                            && Math.abs(ay - entryY) < EPS_POS;
                    boolean sameRot = (entryR != null) && Math.abs(ar - entryR) < EPS_ROT;
                    boolean sameSize = (entryW != null && entryH != null)
                            && Math.abs(aw - entryW) <= EPS_SIZE
                            && Math.abs(ah - entryH) <= EPS_SIZE;

                    if (!(samePos && sameRot && sameSize)) {
                        float bx = getArguments().getFloat("prevX", stickerWrapper.getX());
                        float by = getArguments().getFloat("prevY", stickerWrapper.getY());
                        int bw = getArguments().getInt("prevW", stickerWrapper.getLayoutParams().width);
                        int bh = getArguments().getInt("prevH", stickerWrapper.getLayoutParams().height);
                        float br = getArguments().getFloat("prevR", stickerWrapper.getRotation());

                        a.recordStickerEdit(stickerWrapper, bx, by, bw, bh, br, ax, ay, aw, ah, ar);
                    }

                    if (stickerOverlay != null) {
                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            View child = stickerOverlay.getChildAt(i);
                            MyStickersFragment.setStickerActive(child, true);
                            child.setTag(R.id.tag_prev_enabled, null);
                        }
                    }
                    restoreElevationIfNeeded();
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
                    restoreElevationIfNeeded();

                    sLastReturnOriginAction = "mystickers_check";
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setView(isChecked);
        });

        /*checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            stickerWrapper.setTag(R.id.tag_face_checked, isChecked);
            StickerUtils.updateStickerMetadata(stickerWrapper, isChecked);

            if (faceBox == null) {
                faceBox = new FaceBoxOverlayView(requireContext());
                faceBox.setVisibility(View.GONE);
                stickerOverlay.addView(faceBox, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            if (!isChecked) {
                faceBox.clearBoxes();
                faceBox.setVisibility(View.GONE);
                return;
            }

            FilterActivity activity = (FilterActivity) requireActivity();
            FGLRenderer renderer = activity.getRenderer();
            Bitmap bitmap = renderer.getCurrentBitmap();
            if (renderer == null || bitmap == null) {
                showToast("사진을 불러오지 못했습니다");

                Log.e("MLKit", "사진 없음");
                return;
            }

            InputImage image = InputImage.fromBitmap(bitmap, 0);

            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        List<Rect> faceRects = new ArrayList<>();

                        for (int i = 0; i < faces.size(); i++) {
                            Face face = faces.get(i);
                            faceRects.add(face.getBoundingBox());
                            Log.d("MLKit", "========================= Face [" + (i + 1) + "] =========================");
                            getFaceLandmarks(face);
                            getFaceContours(face);
                        }

                        if (!faces.isEmpty()) {
                            showToast("얼굴 인식 성공");

                            faceBox.setVisibility(View.VISIBLE);
                            faceBox.setFaceBoxes(faceRects, bitmap.getWidth(), bitmap.getHeight());

                            //스티커 메타데이터
                            if (stickerWrapper != null) {
                                float stickerX = stickerWrapper.getX();
                                float stickerY = stickerWrapper.getY();
                                float stickerW = stickerWrapper.getWidth();
                                float stickerH = stickerWrapper.getHeight();
                                float rotation = stickerWrapper.getRotation();

                                //스티커 중심점
                                float stickerCenterX = stickerX + stickerW / 2f;
                                float stickerCenterY = stickerY + stickerH / 2f;

                                //첫 번째 얼굴 기준
                                //Rect face = faces.get(0).getBoundingBox();
                                //float faceCenterX = face.centerX();
                                //float faceCenterY = face.centerY();

                                //가까운 얼굴 기준
                                float minDist = Float.MAX_VALUE;
                                Rect closestFace = null;
                                for (Face face : faces) {
                                    Rect box = face.getBoundingBox();
                                    float faceCenterX = box.centerX();
                                    float faceCenterY = box.centerY();
                                    float dx = stickerCenterX - faceCenterX;
                                    float dy = stickerCenterY - faceCenterY;
                                    float dist = (float) Math.hypot(dx, dy);
                                    if (dist < minDist) {
                                        minDist = dist;
                                        closestFace = box;
                                    }
                                }

                                if (closestFace == null) {
                                    Log.w("StickerMeta", "얼굴 없음");
                                    return;
                                }

                                Rect face = closestFace;
                                float faceCenterX = face.centerX();
                                float faceCenterY = face.centerY();

                                //절대좌표
                                Log.d("StickerMeta", String.format(
                                        "절대좌표 (사진 상 좌표) : x=%.1f, y=%.1f, w=%.1f, h=%.1f, 회전각=%.1f",
                                        stickerX, stickerY, stickerW, stickerH, rotation
                                ));

                                //얼굴 기준 상대좌표
                                float relX = (stickerCenterX - face.left) / face.width();
                                float relY = (stickerCenterY - face.top) / face.height();
                                float relW = stickerW / (float) face.width();
                                float relH = stickerH / (float) face.height();

                                Log.d("StickerMeta", String.format(
                                        "상대좌표 (가까운 얼굴 기준) -> relX=%.3f, relY=%.3f, relW=%.3f, relH=%.3f, 회전각=%.1f",
                                        relX, relY, relW, relH, rotation
                                ));

                                //얼굴 중심 거리
                                float distX = stickerCenterX - faceCenterX;
                                float distY = stickerCenterY - faceCenterY;
                                double dist = Math.hypot(distX, distY);

                                Log.d("StickerMeta", String.format(
                                        "얼굴 중심에서 거리 -> dx=%.1f, dy=%.1f, dist=%.1fpx", distX, distY, dist
                                ));
                            }

                        } else {
                            showToast("얼굴 인식 실패");
                            if (cancelBtn != null) {
                                cancelBtn.setEnabled(false);
                                cancelBtn.setAlpha(0.4f);
                            }
                            if (checkBtn != null) {
                                checkBtn.setEnabled(false);
                                checkBtn.setAlpha(0.4f);
                            }
                            topArea.postDelayed(() -> {
                                showToast("인물 또는 정면의 사진이 아닐 경우\n감지하지 못할 수 있습니다");
                                if (cancelBtn != null) {
                                    cancelBtn.setEnabled(true);
                                    cancelBtn.setAlpha(1f);
                                }
                                if (checkBtn != null) {
                                    checkBtn.setEnabled(true);
                                    checkBtn.setAlpha(1f);
                                }
                            }, 2000);

                            Log.e("MLKit", "얼굴 인식 실패");
                            faceBox.clearBoxes();
                            faceBox.setVisibility(View.GONE);
                        }
                        detector.close();
                    })
                    .addOnFailureListener(e -> {
                        showToast("얼굴 인식 실패");
                        if (cancelBtn != null) {
                            cancelBtn.setEnabled(false);
                            cancelBtn.setAlpha(0.4f);
                        }
                        if (checkBtn != null) {
                            checkBtn.setEnabled(false);
                            checkBtn.setAlpha(0.4f);
                        }
                        topArea.postDelayed(() -> {
                            showToast("인물 또는 정면의 사진이 아닐 경우\n감지하지 못할 수 있습니다");
                            if (cancelBtn != null) {
                                cancelBtn.setEnabled(true);
                                cancelBtn.setAlpha(1f);
                            }
                            if (checkBtn != null) {
                                checkBtn.setEnabled(true);
                                checkBtn.setAlpha(1f);
                            }
                        }, 2000);

                        Log.e("MLKit", "얼굴 인식 실패");
                        faceBox.clearBoxes();
                        faceBox.setVisibility(View.GONE);
                        detector.close();
                    });
        });*/
    }

    private void showToast(String message) {
        isToastVisible = true;

        View old = topArea.findViewWithTag("inline_banner");
        if (old != null) topArea.removeView(old);

        TextView tv = new TextView(requireContext());
        tv.setTag("inline_banner");
        tv.setText(message);
        tv.setTextColor(0XFFFFFFFF);
        tv.setTextSize(16);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));
        tv.setElevation(dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC222222);
        bg.setCornerRadius(dp(16));
        tv.setBackground(bg);

        ConstraintLayout.LayoutParams lp =
                new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);
        lp.startToStart = topArea.getId();
        lp.endToEnd = topArea.getId();
        lp.topToTop = topArea.getId();
        lp.bottomToBottom = topArea.getId();
        tv.setLayoutParams(lp);

        tv.setAlpha(0f);
        topArea.addView(tv);
        tv.animate().alpha(1f).setDuration(150).start();

        tv.postDelayed(() -> tv.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (tv.getParent() == topArea) topArea.removeView(tv);
                    isToastVisible = false;
                })
                .start(), 2000);
    }

    private void setView(boolean isCheck) {
        if (isCheck) {
            if (stickerImage != null) {
                FaceFragment.stickerEditor = stickerWrapper;
            }

            faceOverlay.setAlpha(0f);
            faceOverlay.setVisibility(View.VISIBLE);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.faceOverlay, new FaceFragment())
                    .commit();

            faceOverlay.animate()
                    .alpha(1f)
                    .setDuration(10)
                    .start();

            photoPreviewContainer.animate()
                    .alpha(0f)
                    .setDuration(10)
                    .withEndAction(() -> photoPreviewContainer.setVisibility(View.INVISIBLE))
                    .start();
        } else {
            photoPreviewContainer.setAlpha(0f);
            photoPreviewContainer.setVisibility(View.VISIBLE);

            photoPreviewContainer.animate()
                    .alpha(1f)
                    .setDuration(10)
                    .start();

            faceOverlay.animate()
                    .alpha(0f)
                    .setDuration(10)
                    .withEndAction(() -> faceOverlay.setVisibility(View.GONE))
                    .start();
        }
    }

    /*private List<Face> detectAllFaces(Bitmap bitmap) {
        List<Face> faces = new ArrayList<>();
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            FaceDetectorOptions opts = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();
            FaceDetector detector = FaceDetection.getClient(opts);
            faces.addAll(Tasks.await(detector.process(image)));
            detector.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return faces;
    }*/

    private View cloneSticker(View originalSticker) {
        if (originalSticker == null || getContext() == null) return null;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View newSticker = inflater.inflate(R.layout.v_sticker_edit, (ViewGroup) stickerOverlay, false);

        ImageView oldImage = originalSticker.findViewById(R.id.stickerImage);
        ImageView newImage = newSticker.findViewById(R.id.stickerImage);

        if (oldImage != null && oldImage.getDrawable() != null && newImage != null) {
            newImage.setImageDrawable(oldImage.getDrawable().getConstantState().newDrawable());
        }

        return newSticker;
    }

    private void restoreElevationIfNeeded() {
        if (stickerWrapper != null && prevElevation != null) {
            ViewCompat.setZ(stickerWrapper, prevElevation);
            stickerWrapper.invalidate();
        }
    }

    private void raiseStickerToAbsoluteTop(@NonNull View sticker, @NonNull ViewGroup parent) {
        float maxZ = 0f;
        for (int i = 0; i < parent.getChildCount(); i++) {
            maxZ = Math.max(maxZ, ViewCompat.getZ(parent.getChildAt(i)));
        }
        ViewCompat.setZ(sticker, maxZ + 1000f);
        sticker.bringToFront();
        parent.invalidate();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (StickersFragment.faceBox != null) {
            StickersFragment.faceBox.clearBoxes();
            StickersFragment.faceBox.setVisibility(View.GONE);
        }
    }
}