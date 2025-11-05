package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.FaceModeViewModel;
import com.example.filter.etc.FaceStickerData;
import com.example.filter.overlayviews.FaceBoxOverlayView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;

public class FaceFragment extends Fragment {
    private FrameLayout faceContainer;
    private ImageView faceModel;
    private View stickerWrapper;
    private CheckBox checkBox;
    private ImageButton cancelBtn, checkBtn;
    private ImageView editFrame, rotateController, sizeController;
    private float downRawX, downRawY, startX, startY;
    private float rotStartWrapperDeg, rotLastAngleDeg, rotAccumDeg;
    private int startW, startH;
    private static final int CTRL_BASE_DP = 30;
    private static final int CTRL_MIN_DP = 22;
    private static final int WRAPPER_MIN_DP = 100;
    private final float ROT_MIN_RADIUS_DP = 24f;
    //public static String sLastReturnOriginAction = null;
    private FaceModeViewModel viewModel;
    private FaceBoxOverlayView faceBox;
    private ConstraintLayout topArea;
    private FrameLayout photoPreviewContainer;
    private Rect lastFaceBox;
    private Bundle meta;
    private static final float FACE_STICKER_SCALE_BOOST = 1.3f;
    private boolean isToastVisible = false;
    private long sessionId = -1L;
    private int entrySessionBaseline = 0;
    //private final String currentFaceBatchId = "face_batch_" + System.currentTimeMillis();
    private final String currentFaceBatchId = "face_batch_" + System.currentTimeMillis();
    private String returnOrigin = "mystickers";
    //private float bx, by, bw, bh, br;
    //private float ax, ay, aw, ah, ar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_face, container, false);
        faceContainer = view.findViewById(R.id.faceContainer);
        faceModel = view.findViewById(R.id.faceModel);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkBox = view.findViewById(R.id.checkBox);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        topArea = requireActivity().findViewById(R.id.topArea);
        photoPreviewContainer = requireActivity().findViewById(R.id.photoPreviewContainer);

        Bundle args = getArguments();
        if (args == null) return;

        sessionId = args.getLong("sessionId", -1L);
        entrySessionBaseline = args.getInt("sessionBaseline", 0);
        returnOrigin = args.getString("returnOrigin", "mystickers");

        Bitmap bitmap = null;
        if (args.getBoolean("useTempBitmap", false)) {
            viewModel = new ViewModelProvider(requireActivity()).get(FaceModeViewModel.class);
            bitmap = viewModel.getTempBitmap();
            viewModel.clearTempBitmap();
        } else {
            bitmap = args.getParcelable("stickerBitmap");
        }

        if (bitmap == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        stickerWrapper = inflater.inflate(R.layout.v_sticker_edit, faceContainer, false);

        ImageView stickerImage = stickerWrapper.findViewById(R.id.stickerImage);
        stickerImage.setImageBitmap(bitmap);

        faceContainer.post(() -> {
            int sizePx = dpToPx(230);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
            stickerWrapper.setLayoutParams(lp);
            float cx = (faceContainer.getWidth() - sizePx) / 2f;
            float cy = (faceContainer.getHeight() - sizePx) / 2f;
            stickerWrapper.setX(cx);
            stickerWrapper.setY(cy);

            ViewParent parent = stickerWrapper.getParent();
            if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(stickerWrapper);

            faceContainer.addView(stickerWrapper);
        });

        enableStickerControl(stickerWrapper);
        String stickerId = args.getString("stickerId", "unknown");

        FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        if (stickerOverlay != null) {
            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);
                Object id = child.getTag(R.id.tag_sticker_id);
                if (stickerId.equals(id)) {
                    Object isFaceGenerated = child.getTag(R.id.tag_face_generated);
                    if (Boolean.TRUE.equals(isFaceGenerated)) {
                        checkBox.setChecked(true);
                        checkBox.setAlpha(0.4f);
                        checkBox.setEnabled(false);

                        stickerOverlay.removeViewAt(i);
                    }
                    break;
                }
            }
        }

        cancelBtn.setOnClickListener(v -> {
            FrameLayout overlay = requireActivity().findViewById(R.id.stickerOverlay);

            if (stickerOverlay != null) {
                for (int i = stickerOverlay.getChildCount() - 1; i >= 0; i--) {
                    View child = stickerOverlay.getChildAt(i);
                    Object batchTag = child.getTag(R.id.tag_face_batch_id);
                    if (batchTag != null && batchTag.equals(currentFaceBatchId)) {
                        stickerOverlay.removeViewAt(i);
                    }
                    Object tag = child.getTag();
                    if (tag != null && tag.equals("editingSticker")) {
                        stickerOverlay.removeViewAt(i);
                    }
                }
            }


            String batchId = getArguments().getString("batchId", null);
            if (batchId != null && overlay != null) {
                for (int i = overlay.getChildCount() - 1; i >= 0; i--) {
                    View child = overlay.getChildAt(i);
                    Object bid = child.getTag(R.id.tag_face_batch_id);
                    if (batchId.equals(bid)) overlay.removeViewAt(i);
                }
            }


            Fragment targetFragment;
            if ("stickers".equals(returnOrigin)) {
                targetFragment = new StickersFragment();
            } else {
                MyStickersFragment mystickers = new MyStickersFragment();
                Bundle args2 = new Bundle();
                args2.putBoolean("restoreAllActive", false);
                args2.putBoolean("fromFaceFragment", true);
                args2.putLong("sessionId", sessionId);
                args2.putInt("sessionBaseline", entrySessionBaseline);
                mystickers.setArguments(args2);
                targetFragment = mystickers;
            }

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, targetFragment)
                    .commit();

            requireActivity().findViewById(R.id.faceOverlay).setVisibility(View.GONE);
        });

        checkBtn.setOnClickListener(v -> {
            String batchId = getArguments().getString("batchId", null);
            boolean isBatchEdit = getArguments().getBoolean("isBatchEdit", false);
            if (checkBox.isChecked() && photoPreviewContainer != null) {
                if (stickerOverlay != null) {
                    for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                        View child = stickerOverlay.getChildAt(i);
                        Object tag = child.getTag(R.id.tag_sticker_id);
                        if (tag != null && tag.equals(stickerId)) {
                            stickerOverlay.removeView(child);
                            break;
                        }
                    }
                }

                float relX = meta.getFloat("relX");
                float relY = meta.getFloat("relY");
                float relW = meta.getFloat("relW");
                float relH = meta.getFloat("relH");
                float stickerR = meta.getFloat("stickerR");

                //bx = relX;
                //by = relY;
                //bw = relW;
                //bh = relH;
                //br = stickerR;

                //Log.d("StickerMeta", String.format("마이스티커프래그먼트 사진 속 얼굴들 :: X=%.3f, Y=%.3f, W=%.3f, H=%.3f, R=%.3f", relX, relY, relW, relH, stickerR));
                //Log.d("StickerMeta", String.format("마이스티커프래그먼트 사진 속 얼굴들 :: bX=%.3f, bY=%.3f, bW=%.3f, bH=%.3f, bR=%.3f", bx, by, bw, bh, br));


                ImageView stickerImageView = stickerWrapper.findViewById(R.id.stickerImage);
                Bitmap stickerBitmap = null;
                if (stickerImageView != null && stickerImageView.getDrawable() != null) {
                    stickerImageView.setDrawingCacheEnabled(true);
                    stickerBitmap = Bitmap.createBitmap(stickerImageView.getDrawingCache());
                    stickerImageView.setDrawingCacheEnabled(false);
                }

                String stickerPath = null;
                if (stickerBitmap != null) {
                    try {
                        File file = new File(requireContext().getCacheDir(),
                                "face_sticker_" + System.currentTimeMillis() + ".png");
                        FileOutputStream out = new FileOutputStream(file);
                        stickerBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.close();
                        stickerPath = file.getAbsolutePath();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                FaceStickerData data = new FaceStickerData(relX, relY, relW, relH, stickerR, currentFaceBatchId, stickerBitmap,stickerPath);

                Log.d("StickerFlow", String.format(
                        "[FaceFragment] relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, batchId=%s",
                        relX, relY, relW, relH, stickerR, currentFaceBatchId
                ));

                FaceModeViewModel vm = new ViewModelProvider(requireActivity()).get(FaceModeViewModel.class);
                vm.setFaceStickerData(data);

                View stickerToClone = stickerWrapper;
                removeStickerWrapper();

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity == null) return;

                activity.getPhotoPreview().queueEvent(() -> {
                    Bitmap bmp = activity.getRenderer().getCurrentBitmap();
                    if (bmp == null) {
                        if (isAdded()) {
                            activity.runOnUiThread(() -> {
                                showToast("얼굴을 감지하지 못했습니다");

                                requireActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .setCustomAnimations(R.anim.slide_up, 0)
                                        .replace(R.id.bottomArea2, new MyStickersFragment())
                                        .commit();

                                requireActivity().findViewById(R.id.faceOverlay).setVisibility(View.GONE);
                            });
                        }
                        return;
                    }
                    activity.runOnUiThread(() -> StickersFragment.detectFaces(bmp, (faces, originalBitmap) -> {
                        if (!isAdded()) return;

                        if (faces.isEmpty()) {
                            showToast("얼굴을 감지하지 못했습니다");
                        } else {
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

                                    float eulerZ = face.getHeadEulerAngleZ();
                                    float eulerY = face.getHeadEulerAngleY();
                                    float eulerX = face.getHeadEulerAngleX();

                                    float faceCenterX = box.centerX();
                                    float faceCenterY = box.centerY();
                                    float faceW = box.width();
                                    float faceH = box.height();

                                    float stickerCenterX = faceCenterX + (relX * faceW);
                                    float stickerCenterY = faceCenterY + (relY * faceH);

                                    float radiansZ = (float) Math.toRadians(-eulerZ);
                                    float dx = stickerCenterX - faceCenterX;
                                    float dy = stickerCenterY - faceCenterY;
                                    float rotatedX = (float) (dx * Math.cos(radiansZ) - dy * Math.sin(radiansZ));
                                    float rotatedY = (float) (dx * Math.sin(radiansZ) + dy * Math.cos(radiansZ));

                                    float radiansY = (float) Math.toRadians(eulerY);
                                    float depthShiftRatio = 0.35f;
                                    float yawShiftX = (float) (Math.sin(radiansY) * faceW * depthShiftRatio);
                                    float yawShiftY = (float) (Math.abs(Math.sin(radiansY)) * faceH * 0.1f);

                                    float radiansX = (float) Math.toRadians(eulerX);
                                    float pitchShiftRatio = 0.25f;
                                    float pitchShiftY = (float) (Math.sin(radiansX) * faceH * pitchShiftRatio);

                                    stickerCenterX = (faceCenterX + rotatedX) + yawShiftX;
                                    stickerCenterY = (faceCenterY + rotatedY) - yawShiftY + pitchShiftY;

                                    float stickerW = relW * faceW * FACE_STICKER_SCALE_BOOST;
                                    float stickerH = relH * faceH * FACE_STICKER_SCALE_BOOST;

                                    float stickerCenterX_view = (stickerCenterX * scale) + offsetX;
                                    float stickerCenterY_view = (stickerCenterY * scale) + offsetY;
                                    int stickerW_view = Math.round(stickerW * scale);
                                    int stickerH_view = Math.round(stickerH * scale);

                                    int minStickerPx = dp(WRAPPER_MIN_DP);
                                    if (stickerW_view < minStickerPx || stickerH_view < minStickerPx) {
                                        float ratio = (float) stickerW_view / (float) stickerH_view;
                                        if (stickerW_view < stickerH_view) {
                                            stickerW_view = minStickerPx;
                                            stickerH_view = Math.round(minStickerPx / ratio);
                                        } else {
                                            stickerH_view = minStickerPx;
                                            stickerW_view = Math.round(minStickerPx * ratio);
                                        }
                                    }

                                    float stickerX_view = stickerCenterX_view - stickerW_view / 2f;
                                    float stickerY_view = stickerCenterY_view - stickerH_view / 2f;

                                    View newSticker = cloneSticker(stickerToClone);
                                    if (newSticker == null) continue;

                                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(stickerW_view, stickerH_view);
                                    newSticker.setLayoutParams(lp);
                                    newSticker.setX(stickerX_view);
                                    newSticker.setY(stickerY_view);
                                    newSticker.setRotation(stickerR - eulerZ);

                                    newSticker.setTag(R.id.tag_session_id, sessionId);
                                    if (newSticker.getTag(R.id.tag_sticker_id) == null) {
                                        newSticker.setTag(R.id.tag_sticker_id, stickerId);
                                    }
                                    newSticker.setTag(R.id.tag_face_generated, true);
                                    newSticker.setTag(R.id.tag_face_batch_id, currentFaceBatchId);

                                    MyStickersFragment.hideControllers(newSticker);
                                    MyStickersFragment.setStickerActive(newSticker, true);

                                    if (isBatchEdit && batchId != null && stickerOverlay != null) {
                                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                                            View child = stickerOverlay.getChildAt(i);
                                            Object bid = child.getTag(R.id.tag_face_batch_id);
                                            if (batchId.equals(bid)) {
                                                stickerOverlay.removeViewAt(i);
                                                i--;
                                            }
                                        }
                                    }

                                    stickerOverlay.addView(newSticker);
                                }
                            }
                            showToast("얼굴 인식 성공");
                        }

                        Fragment targetFragment;
                        if ("stickers".equals(returnOrigin)) {
                            targetFragment = new StickersFragment();
                        } else {
                            targetFragment = new MyStickersFragment();
                            Bundle args2 = new Bundle();
                            args2.putBoolean("skipDisableOnce", true);
                            args2.putBoolean("restoreAllActive", false);
                            args2.putInt("sessionBaseline", entrySessionBaseline);
                            args2.putLong("sessionId", sessionId);
                            args2.putBoolean("fromFaceFragment", true);
                            targetFragment.setArguments(args2);
                        }

                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .setCustomAnimations(R.anim.slide_up, 0)
                                .replace(R.id.bottomArea2, targetFragment)
                                .commit();

                        requireActivity().findViewById(R.id.faceOverlay).setVisibility(View.GONE);
                    }));
                });
            }
        });

        viewModel = new ViewModelProvider(requireActivity()).get(FaceModeViewModel.class);
        viewModel.getStickerState(stickerId).observe(getViewLifecycleOwner(), isChecked -> {
            if (checkBox.isChecked() != isChecked) {
                checkBox.setChecked(isChecked);
            }
        });

        checkBox.setOnCheckedChangeListener((b, isChecked) -> {
            viewModel.setStickerState(stickerId, isChecked);
            if (!isChecked) {
                requireActivity().getSupportFragmentManager().popBackStack();
                requireActivity().findViewById(R.id.faceOverlay).setVisibility(View.GONE);
            }
        });


        //String batchId = args.getString("batchId", null);
        //boolean isBatchEdit = args.getBoolean("isBatchEdit", false);

        faceBox = new FaceBoxOverlayView(requireContext());
        faceContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        faceModel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int w = faceModel.getWidth();
                int h = faceModel.getHeight();
                if (w <= 0 || h <= 0) return;

                faceModel.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                faceModel.draw(c);
                detectFaces(bmp);
            }
        });

        /*if (isBatchEdit && batchId != null) {
            loadExistingBatch(batchId);
        }*/
    }

    public void detectFaces(Bitmap bitmap) {
        if (bitmap == null) return;

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        Face face = faces.get(0);
                        lastFaceBox = face.getBoundingBox();

                        faceBox.setVisibility(View.VISIBLE);
                        faceBox.setFaceBox(lastFaceBox, bitmap.getWidth(), bitmap.getHeight());

                        updateMeta();

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

    private void updateMeta() {
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

        //ax = relX;
        //ay = relY;
        //aw = relW;
        //ah = relH;
        //ar = stickerR;

        //Log.d("StickerMeta", String.format("페이스프래그먼트 faceModel :: X=%.3f, Y=%.3f, W=%.3f, H=%.3f, R=%.3f", relX, relY, relW, relH, stickerR));
        //Log.d("StickerMeta", String.format("페이스프래그먼트 faceModel :: aX=%.3f, aY=%.3f, aW=%.3f, aH=%.3f, aR=%.3f", ax, ay, aw, ah, ar));

        meta = new Bundle();
        meta.putFloat("relX", relX);
        meta.putFloat("relY", relY);
        meta.putFloat("relW", relW);
        meta.putFloat("relH", relH);
        meta.putFloat("stickerR", stickerR);
    }

    private View cloneSticker(View originalSticker) {
        if (originalSticker == null || getContext() == null) return null;

        View stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View newSticker = inflater.inflate(R.layout.v_sticker_edit, (ViewGroup) stickerOverlay, false);

        ImageView oldImage = originalSticker.findViewById(R.id.stickerImage);
        ImageView newImage = newSticker.findViewById(R.id.stickerImage);

        if (oldImage != null && oldImage.getDrawable() != null && newImage != null) {
            newImage.setImageDrawable(oldImage.getDrawable().getConstantState().newDrawable());
        }

        return newSticker;
    }

    /*private void loadExistingBatch(@NonNull String batchId) {
        FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        if (stickerOverlay == null) return;

        View firstSticker = null;
        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
            //View child = stickerOverlay.getChildAt(i);
            //Object bid = child.getTag(R.id.tag_face_batch_id);
            //if (batchId.equals(bid)) {
            //    firstSticker = child;
            //    break;
            //}
        }
        if (firstSticker == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        stickerWrapper = inflater.inflate(R.layout.v_sticker_edit, null, false);
        ImageView stickerImage = stickerWrapper.findViewById(R.id.stickerImage);
        ImageView oldImage = firstSticker.findViewById(R.id.stickerImage);
        if (oldImage != null && oldImage.getDrawable() != null) {
            stickerImage.setImageDrawable(oldImage.getDrawable().getConstantState().newDrawable());
        }

        faceContainer.post(() -> {
            int sizePx = dpToPx(230);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
            stickerWrapper.setLayoutParams(lp);
            float cx = (faceContainer.getWidth() - sizePx) / 2f;
            float cy = (faceContainer.getHeight() - sizePx) / 2f;
            stickerWrapper.setX(cx);
            stickerWrapper.setY(cy);

            ViewParent parent = stickerWrapper.getParent();
            if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(stickerWrapper);

            faceContainer.addView(stickerWrapper);
        });

        enableStickerControl(stickerWrapper);
    }*/

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

    @SuppressLint("ClickableViewAccessibility")
    private void enableStickerControl(View stickerWrapper) {
        editFrame = stickerWrapper.findViewById(R.id.editFrame);
        rotateController = stickerWrapper.findViewById(R.id.rotateController);
        sizeController = stickerWrapper.findViewById(R.id.sizeController);

        editFrame.setVisibility(View.VISIBLE);
        rotateController.setVisibility(View.VISIBLE);
        sizeController.setVisibility(View.VISIBLE);

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
                case MotionEvent.ACTION_UP:
                    updateMeta();
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
                    updateMeta();
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
                    updateMeta();
                    return true;
            }
            return false;
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
        faceModel.getLocationOnScreen(ov);
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

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.hypot(dx, dy);
    }

    private int dp(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }

    private int dpToPx(int dp) {
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