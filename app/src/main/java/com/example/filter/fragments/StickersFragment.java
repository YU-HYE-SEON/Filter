package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.activities.LoadActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.overlayviews.FaceBoxOverlayView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;

public class StickersFragment extends Fragment {
    public interface FaceDetectionCallback {
        void onFacesDetected(List<Face> faces, Bitmap originalBitmap);
    }

    public static FaceBoxOverlayView faceBox;
    private ImageView myStickerIcon;
    private ImageView loadStickerIcon;
    private ImageView brushIcon;
    private ImageView AIStickerIcon;
    private FrameLayout fullScreenFragmentContainer;
    private ConstraintLayout filterActivity;
    private ConstraintLayout bottomArea1;
    private ImageButton undoColor, redoColor, originalColor;
    //private ImageButton undoSticker, redoSticker, originalSticker;
    private LinearLayout brushToSticker;
    private static final float EPS_Z = 0.5f;
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();

                    if (photoUri != null) {
                        Intent intent = new Intent(requireContext(), LoadActivity.class);
                        intent.setData(photoUri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } else {
                        Toast.makeText(requireContext(), "사진 선택 실패: URI 없음", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_stickers, container, false);

        myStickerIcon = view.findViewById(R.id.myStickerIcon);
        loadStickerIcon = view.findViewById(R.id.loadStickerIcon);
        brushIcon = view.findViewById(R.id.brushIcon);
        AIStickerIcon = view.findViewById(R.id.AIStickerIcon);

        bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);
        undoColor = requireActivity().findViewById(R.id.undoColor);
        redoColor = requireActivity().findViewById(R.id.redoColor);
        originalColor = requireActivity().findViewById(R.id.originalColor);

        //undoSticker = requireActivity().findViewById(R.id.undoSticker);
        //redoSticker = requireActivity().findViewById(R.id.redoSticker);
        //originalSticker = requireActivity().findViewById(R.id.originalSticker);
        brushToSticker = requireActivity().findViewById(R.id.brushToSticker);

        /*FilterActivity activity = (FilterActivity) getActivity();
        if (activity != null) {
            undoSticker.setOnClickListener(v -> {
                activity.previewOriginalStickers(false);
                activity.undoSticker();
                rewireOverlayClickListeners();
            });

            redoSticker.setOnClickListener(v -> {
                activity.previewOriginalStickers(false);
                activity.redoSticker();
                rewireOverlayClickListeners();
            });

            activity.refreshStickerButtons();
        }*/

        if (bottomArea1 != null) {
            undoColor.setVisibility(View.INVISIBLE);
            redoColor.setVisibility(View.INVISIBLE);
            originalColor.setVisibility(View.INVISIBLE);
            /*undoSticker.setVisibility(View.VISIBLE);
            redoSticker.setVisibility(View.VISIBLE);
            originalSticker.setVisibility(View.VISIBLE);*/
            bottomArea1.setVisibility(View.VISIBLE);
            brushToSticker.setVisibility(View.GONE);
        }

        myStickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
                int sessionBaseline = (stickerOverlay != null) ? stickerOverlay.getChildCount() : 0;

                long sessionId = System.currentTimeMillis();

                MyStickersFragment f = new MyStickersFragment();
                Bundle args = new Bundle();
                args.putInt("sessionBaseline", sessionBaseline);

                args.putLong("sessionId", sessionId);

                boolean skipDisableOnce = false;
                args.putBoolean("skipDisableOnce", skipDisableOnce);
                f.setArguments(args);

                FilterActivity activity = (FilterActivity) requireActivity();
                FrameLayout photoPreviewContainer = activity.findViewById(R.id.photoPreviewContainer);

                faceBox = new FaceBoxOverlayView(requireContext());
                photoPreviewContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                activity.getPhotoPreview().queueEvent(() -> {
                    Bitmap bmp = activity.getRenderer().getCurrentBitmap();
                    activity.runOnUiThread(() -> detectFaces(bmp, (faces, originalBitmap) -> {
                        if (faces.isEmpty()) return;
                    }));
                });

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, f)
                        .commit();
            }
        });

        loadStickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            }
        });

        brushIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
                int sessionBaseline = (stickerOverlay != null) ? stickerOverlay.getChildCount() : 0;

                BrushFragment f = new BrushFragment();
                Bundle args = new Bundle();
                args.putInt("sessionBaseline", sessionBaseline);

                args.putBoolean("insertAboveEditingSticker", true);

                boolean skipDisableOnce = false;
                args.putBoolean("skipDisableOnce", skipDisableOnce);
                f.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, f)
                        .commit();
            }
        });

        AIStickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                fullScreenFragmentContainer = requireActivity().findViewById(R.id.fullScreenFragmentContainer);
                filterActivity = requireActivity().findViewById(R.id.filterActivity);
                ConstraintLayout main = requireActivity().findViewById(R.id.main);

                fullScreenFragmentContainer.setVisibility(View.VISIBLE);
                filterActivity.setVisibility(View.GONE);
                main.setBackgroundColor(Color.parseColor("#007AFF"));

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fullScreenFragmentContainer, new AIStickerViewFragment())
                        .addToBackStack("ai_sticker_view")
                        .commit();
            }
        });

        return view;
    }

    public static void detectFaces(Bitmap bitmap, FaceDetectionCallback callback) {
        if (bitmap == null) {
            if (callback != null) {
                callback.onFacesDetected(new ArrayList<>(), null);
            }
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    List<Rect> rects = new ArrayList<>();

                    for (int i = 0; i < faces.size(); i++) {
                        Face face = faces.get(i);
                        rects.add(face.getBoundingBox());
                        //Log.d("얼굴인식", "========================= photoPreview 속 Face [" + (i + 1) + "] =========================");
                        //getFaceLandmarks(face);
                        //getFaceContours(face);
                    }

                    if (faceBox != null) {
                        if (!faces.isEmpty()) {
                            faceBox.setVisibility(View.VISIBLE);
                            faceBox.setFaceBoxes(rects, bitmap.getWidth(), bitmap.getHeight());
                        } else {
                            faceBox.clearBoxes();
                            faceBox.setVisibility(View.GONE);
                        }
                    }

                    if (callback != null) {
                        callback.onFacesDetected(faces, bitmap);
                    }

                    detector.close();
                })
                .addOnFailureListener(e -> {
                    if (faceBox != null) {
                        faceBox.clearBoxes();
                        faceBox.setVisibility(View.GONE);
                    }
                    if (callback != null) {
                        callback.onFacesDetected(new ArrayList<>(), bitmap);
                    }
                    detector.close();
                });
    }

    /*private static void logFaceLandmark(Face face, String label, int type) {
        FaceLandmark lm = face.getLandmark(type);
        if (lm == null) {
            Log.d("얼굴인식", label + " : 인식 실패");
            return;
        }
        PointF p = lm.getPosition();
        Log.d("얼굴인식", label + " : (" + p.x + ", " + p.y + ")");
    }

    private static void getFaceLandmarks(Face face) {
        logFaceLandmark(face, "LEFT_EYE : ", FaceLandmark.LEFT_EYE);
        logFaceLandmark(face, "RIGHT_EYE : ", FaceLandmark.RIGHT_EYE);
        logFaceLandmark(face, "NOSE_BASE : ", FaceLandmark.NOSE_BASE);
        logFaceLandmark(face, "MOUTH_BOTTOM : ", FaceLandmark.MOUTH_BOTTOM);
        logFaceLandmark(face, "LEFT_CHEEK : ", FaceLandmark.LEFT_CHEEK);
        logFaceLandmark(face, "RIGHT_CHEEK : ", FaceLandmark.RIGHT_CHEEK);
    }

    private static void logFaceContour(Face face, String label, int type) {
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

    private static void getFaceContours(Face face) {
        logFaceContour(face, "FaceContour : ", FaceContour.FACE);
    }*/

    @Override
    public void onResume() {
        super.onResume();
        FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        if (stickerOverlay == null) return;
        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
            View child = stickerOverlay.getChildAt(i);

            if (Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) {
                child.setOnClickListener(null);
                child.setClickable(false);
                child.setLongClickable(false);
                child.setEnabled(false);
                continue;
            }

            MyStickersFragment.setStickerActive(child, true);
            MyStickersFragment.hideControllers(child);
            attachEditListenerForSticker(child, stickerOverlay);
        }
    }

    private void attachEditListenerForSticker(@NonNull View stickerView, @NonNull FrameLayout overlay) {
        stickerView.setOnClickListener(null);

        stickerView.setOnClickListener(v -> {
            if (!isAdded()) return;
            FragmentActivity act = requireActivity();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;
            if (!stickerView.isEnabled()) return;

            Fragment cur = act.getSupportFragmentManager().findFragmentById(R.id.bottomArea2);
            if (cur instanceof EditMyStickerFragment) return;
            if (ClickUtils.isFastClick(v, 400)) return;

            if (stickerView.findViewById(R.id.stickerImage) == null ||
                    stickerView.findViewById(R.id.editFrame) == null ||
                    stickerView.findViewById(R.id.rotateController) == null ||
                    stickerView.findViewById(R.id.sizeController) == null) {
                return;
            }

            int beforeIndex = overlay.indexOfChild(stickerView);
            float beforeZ = androidx.core.view.ViewCompat.getZ(stickerView);

            int lastIndex = overlay.getChildCount() - 1;
            float maxZ = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < overlay.getChildCount(); i++) {
                maxZ = Math.max(maxZ, ViewCompat.getZ(overlay.getChildAt(i)));
            }
            boolean alreadyTopIndex = (beforeIndex == lastIndex);
            boolean alreadyTopZ = (beforeZ >= maxZ - EPS_Z);
            boolean alreadyAbsoluteTop = alreadyTopIndex && alreadyTopZ;

            int afterIndex = beforeIndex;
            float afterZ = beforeZ;

            if (!alreadyAbsoluteTop) {
                raiseStickerToAbsoluteTop(stickerView, overlay);
                afterIndex = overlay.indexOfChild(stickerView);
                afterZ = ViewCompat.getZ(stickerView);

                boolean indexChanged = (afterIndex != beforeIndex);
                boolean zChanged = Math.abs(afterZ - beforeZ) > EPS_Z;

                /*if (indexChanged || zChanged) {
                    FilterActivity fa = (FilterActivity) act;
                    fa.recordStickerZOrderChange(stickerView, beforeIndex, beforeZ, afterIndex, afterZ);
                }*/
            }

            stickerView.bringToFront();
            overlay.requestLayout();
            overlay.invalidate();

            for (int i = 0; i < overlay.getChildCount(); i++) {
                View child = overlay.getChildAt(i);
                MyStickersFragment.hideControllers(child);
                Object t = child.getTag();
                if ("editingSticker".equals(t)) {
                    child.setTag(null);
                }
            }

            for (int i = 0; i < overlay.getChildCount(); i++) {
                View child = overlay.getChildAt(i);
                child.setTag(R.id.tag_prev_enabled, child.isEnabled());
                MyStickersFragment.setStickerActive(child, child == stickerView);
            }

            setControllersVisible(stickerView, true);
            stickerView.setTag("editingSticker");

            Bundle args = new Bundle();
            args.putString("origin", "stickers");

            args.putFloat("prevElevation", beforeZ);
            args.putFloat("prevX", stickerView.getX());
            args.putFloat("prevY", stickerView.getY());

            ViewGroup.LayoutParams lp = stickerView.getLayoutParams();
            int pw = (lp != null ? lp.width : stickerView.getWidth());
            int ph = (lp != null ? lp.height : stickerView.getHeight());

            args.putInt("prevW", pw);
            args.putInt("prevH", ph);
            args.putFloat("prevR", stickerView.getRotation());

            EditMyStickerFragment edit = new EditMyStickerFragment();
            edit.setArguments(args);

            act.getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, edit)
                    .addToBackStack("edit_from_stickers")
                    .commit();
        });
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

    /*private void rewireOverlayClickListeners() {
        FrameLayout overlay = requireActivity().findViewById(R.id.stickerOverlay);
        if (overlay == null) return;

        for (int i = 0; i < overlay.getChildCount(); i++) {
            View child = overlay.getChildAt(i);

            if (Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) {
                continue;
            }

            child.setOnClickListener(null);
            child.setTag(null);
            child.setTag(R.id.tag_prev_enabled, null);

            MyStickersFragment.setStickerActive(child, true);
            MyStickersFragment.hideControllers(child);

            attachEditListenerForSticker(child, overlay);
        }

        FilterActivity activity = (FilterActivity) getActivity();
        if (activity != null) {
            activity.refreshStickerButtons();
        }
    }*/

    private void setControllersVisible(@NonNull View sticker, boolean visible) {
        View editFrame = sticker.findViewById(R.id.editFrame);
        View rotate = sticker.findViewById(R.id.rotateController);
        View size = sticker.findViewById(R.id.sizeController);

        int vis = visible ? View.VISIBLE : View.INVISIBLE;

        if (editFrame != null) editFrame.setVisibility(vis);
        if (rotate != null) rotate.setVisibility(vis);
        if (size != null) size.setVisibility(vis);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FrameLayout stickerOverlay = getActivity() != null ? getActivity().findViewById(R.id.stickerOverlay) : null;
        if (stickerOverlay != null) {
            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);
                child.setOnClickListener(null);
            }
        }
    }
}