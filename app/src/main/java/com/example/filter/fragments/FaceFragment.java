package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.overlayviews.FaceBoxOverlayView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.List;

public class FaceFragment extends Fragment {
    private FrameLayout faceOverlay;
    private FaceBoxOverlayView faceBox;
    private ImageView faceModel;
    public static View stickerEditor = null;
    private View stickerWrapper;
    private ImageView editFrame, stickerImage, rotateController, sizeController;
    private float downRawX, downRawY, startX, startY;
    private float rotStartWrapperDeg, rotLastAngleDeg, rotAccumDeg;
    private int startW, startH;
    private static final int CTRL_BASE_DP = 30;
    private static final int CTRL_MIN_DP = 22;
    private static final int WRAPPER_MIN_DP = 100;
    private final float ROT_MIN_RADIUS_DP = 24f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_face, container, false);

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        faceOverlay = requireActivity().findViewById(R.id.faceOverlay);
        faceModel = view.findViewById(R.id.faceModel);
        stickerWrapper = view.findViewById(R.id.stickerWrapper);
        editFrame = stickerWrapper.findViewById(R.id.editFrame);
        stickerImage = stickerWrapper.findViewById(R.id.stickerImage);
        rotateController = stickerWrapper.findViewById(R.id.rotateController);
        sizeController = stickerWrapper.findViewById(R.id.sizeController);

        if (stickerEditor != null) {
            ImageView orginalStickerImg = stickerEditor.findViewById(R.id.stickerImage);

            if (orginalStickerImg != null) {
                stickerImage.setImageDrawable(orginalStickerImg.getDrawable());
            }

            stickerWrapper.setLayoutParams(new FrameLayout.LayoutParams(stickerEditor.getLayoutParams()));
            stickerWrapper.setX(stickerEditor.getX());
            stickerWrapper.setY(stickerEditor.getY());
            stickerWrapper.setRotation(stickerEditor.getRotation());

            editFrame.setVisibility(View.VISIBLE);
            rotateController.setVisibility(View.VISIBLE);
            sizeController.setVisibility(View.VISIBLE);
        }

        stickerWrapper.post(() -> {
            stickerWrapper.setPivotX(stickerWrapper.getWidth() / 2f);
            stickerWrapper.setPivotY(stickerWrapper.getHeight() / 2f);
            positionControllers();
        });

        stickerWrapper.setOnClickListener(null);
        stickerWrapper.setClickable(false);

        updateControllersSizeAndAngle();

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
                case MotionEvent.ACTION_UP:
                    detectFaces();
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
                case MotionEvent.ACTION_UP:
                    detectFaces();
                    return true;
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
                case MotionEvent.ACTION_UP:
                    detectFaces();
                    return true;
            }
            return false;
        });

        faceBox = new FaceBoxOverlayView(requireContext());
        ((ViewGroup) view).addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        faceModel.post(this::detectFaces);
    }

    public void detectFaces() {
        faceModel.setDrawingCacheEnabled(true);
        faceModel.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(faceModel.getDrawingCache());
        faceModel.setDrawingCacheEnabled(false);

        if (bitmap == null) {
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
                    List<Rect> rects = new ArrayList<>();

                    for (int i = 0; i < faces.size(); i++) {
                        Face face = faces.get(i);
                        rects.add(face.getBoundingBox());
                        Log.d("얼굴인식", "========================= Face 모델 =========================");
                        getFaceModelLandmarks(face);
                        getFaceModelContours(face);
                    }

                    if (!faces.isEmpty()) {
                        faceBox.setVisibility(View.VISIBLE);
                        faceBox.setFaceBoxes(rects, bitmap.getWidth(), bitmap.getHeight());

                        if (stickerWrapper != null) {
                            float stickerX = stickerWrapper.getX();
                            float stickerY = stickerWrapper.getY();
                            float stickerW = stickerWrapper.getWidth();
                            float stickerH = stickerWrapper.getHeight();
                            float rot = stickerWrapper.getRotation();

                            float stickerCenterX = stickerX + stickerW / 2f;
                            float stickerCenterY = stickerY + stickerH / 2f;

                            Rect face = faces.get(0).getBoundingBox();
                            float faceCenterX = face.centerX();
                            float faceCenterY = face.centerY();

                            float relX = (stickerCenterX - face.left) / face.width();
                            float relY = (stickerCenterY - face.top) / face.height();
                            float relW = stickerW / (float) face.width();
                            float relH = stickerH / (float) face.height();

                            Log.d("얼굴인식", String.format(
                                    "스티커 상대좌표 -> relX=%.3f, relY=%.3f, relW=%.3f, relH=%.3f, rot=%.1f",
                                    relX, relY, relW, relH, rot
                            ));

                            float distX = stickerCenterX - faceCenterX;
                            float distY = stickerCenterY - faceCenterY;
                            double dist = Math.hypot(distX, distY);

                            Log.d("얼굴인식", String.format(
                                    "얼굴 중심에서 거리 -> dx=%.1f, dy=%.1f, dist=%.1fpx", distX, distY, dist
                            ));

                            Bundle meta = new Bundle();
                            meta.putFloat("relX", relX);
                            meta.putFloat("relY", relY);
                            meta.putFloat("relW", relW);
                            meta.putFloat("relH", relH);
                            meta.putFloat("rot", rot);
                            getParentFragmentManager().setFragmentResult("stickerMeta", meta);
                        }
                    } else {
                        faceBox.clearBoxes();
                        faceBox.setVisibility(View.GONE);
                    }

                    detector.close();
                })
                .addOnFailureListener(e -> {
                    faceBox.clearBoxes();
                    faceBox.setVisibility(View.GONE);
                    detector.close();
                });
    }

    private void logFaceModelLandmark(Face face, String label, int type) {
        FaceLandmark lm = face.getLandmark(type);
        if (lm == null) {
            Log.d("얼굴인식", label + " : 인식 실패");
            return;
        }
        PointF p = lm.getPosition();
        Log.d("얼굴인식", label + " : (" + p.x + ", " + p.y + ")");
    }

    private void getFaceModelLandmarks(Face face) {
        logFaceModelLandmark(face, "LEFT_EYE : ", FaceLandmark.LEFT_EYE);
        logFaceModelLandmark(face, "RIGHT_EYE : ", FaceLandmark.RIGHT_EYE);
        logFaceModelLandmark(face, "NOSE_BASE : ", FaceLandmark.NOSE_BASE);
        logFaceModelLandmark(face, "MOUTH_BOTTOM : ", FaceLandmark.MOUTH_BOTTOM);
        logFaceModelLandmark(face, "LEFT_CHEEK : ", FaceLandmark.LEFT_CHEEK);
        logFaceModelLandmark(face, "RIGHT_CHEEK : ", FaceLandmark.RIGHT_CHEEK);
    }

    private void logFaceModelContour(Face face, String label, int type) {
        FaceContour contour = face.getContour(type);
        if (contour == null || contour.getPoints() == null || contour.getPoints().isEmpty()) {
            Log.d("얼굴인식", label + " : 인식 실패");
            return;
        }
        List<PointF> points = contour.getPoints();
        StringBuilder sb = new StringBuilder();
        sb.append(label).append("[");
        for (int i = 0; i < points.size(); i++) {
            PointF p = points.get(i);
            sb.append(String.format("(%.1f, %.1f)", p.x, p.y));
            if (i < points.size() - 1) sb.append(", ");
        }
        sb.append("]");
        Log.d("얼굴인식", sb.toString());
    }

    private void getFaceModelContours(Face face) {
        logFaceModelContour(face, "FaceContour : ", FaceContour.FACE);
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
        faceOverlay.getLocationOnScreen(ov);
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
        if (this.faceBox != null) {
            this.faceBox.clearBoxes();
            this.faceBox.setVisibility(View.GONE);
        }
    }
}