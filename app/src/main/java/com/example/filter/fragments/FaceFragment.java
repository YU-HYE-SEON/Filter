package com.example.filter.fragments;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.example.filter.etc.Controller;
import com.example.filter.etc.FaceDetect;
import com.example.filter.etc.FaceModeViewModel;
import com.example.filter.etc.FaceStickerData;
import com.example.filter.overlayviews.FaceBoxOverlayView;
import com.google.mlkit.vision.face.Face;

import java.io.File;
import java.io.FileOutputStream;

public class FaceFragment extends Fragment {
    private FrameLayout faceOverlay;
    private ImageView faceModel;
    private View stickerWrapper;
    private CheckBox checkBox;
    private ImageButton cancelBtn, checkBtn;
    private ImageView stickerImage, editFrame, rotateController, sizeController;
    private static final int WRAPPER_MIN_DP = 100;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_face, container, false);
        faceOverlay = view.findViewById(R.id.faceOverlay);
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

        FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        stickerWrapper = inflater.inflate(R.layout.v_sticker_edit, faceOverlay, false);
        editFrame = stickerWrapper.findViewById(R.id.editFrame);
        stickerImage = stickerWrapper.findViewById(R.id.stickerImage);
        rotateController = stickerWrapper.findViewById(R.id.rotateController);
        sizeController = stickerWrapper.findViewById(R.id.sizeController);
        stickerImage.setImageBitmap(bitmap);

        faceOverlay.post(() -> {
            int sizePx = Controller.dp(230, getResources());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
            stickerWrapper.setLayoutParams(lp);
            float cx = (faceOverlay.getWidth() - sizePx) / 2f;
            float cy = (faceOverlay.getHeight() - sizePx) / 2f;
            stickerWrapper.setX(cx);
            stickerWrapper.setY(cy);

            ViewParent parent = stickerWrapper.getParent();
            if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(stickerWrapper);

            faceOverlay.addView(stickerWrapper);
        });

        Controller.enableStickerControl(stickerWrapper, editFrame, rotateController, sizeController, faceModel, getResources());
        String stickerId = args.getString("stickerId", "unknown");

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

            requireActivity().findViewById(R.id.faceContainer).setVisibility(View.GONE);
        });

        checkBtn.setOnClickListener(v -> {
            String batchId = getArguments().getString("batchId", null);
            boolean isBatchEdit = getArguments().getBoolean("isBatchEdit", false);
            if (checkBox.isChecked() && photoPreviewContainer != null) {
                // ⭐ [추가된 부분 시작] 재수정일 경우, FilterActivity의 ViewModel에 이전 데이터 삭제 요청
                if (isBatchEdit && batchId != null) {
                    FaceModeViewModel vm = new ViewModelProvider(requireActivity()).get(FaceModeViewModel.class);
                    // FilterActivity에서 해당 batchId를 가진 데이터를 삭제하도록 요청
                    vm.setFaceStickerDataToDelete(batchId);
                }
                // ⭐ [추가된 부분 끝]

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

                String finalBatchId = isBatchEdit && batchId != null ? batchId : currentFaceBatchId;
                FaceStickerData data = new FaceStickerData(relX, relY, relW, relH, stickerR, finalBatchId, stickerBitmap, stickerPath);

                Log.d("StickerFlow", String.format(
                        "[FaceFragment] relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, batchId=%s",
                        relX, relY, relW, relH, stickerR, currentFaceBatchId
                ));

                FaceModeViewModel vm = new ViewModelProvider(requireActivity()).get(FaceModeViewModel.class);
                vm.setFaceStickerData(data);

                View stickerToClone = stickerWrapper;
                Controller.removeStickerWrapper(stickerWrapper);

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

                                requireActivity().findViewById(R.id.faceContainer).setVisibility(View.GONE);
                            });
                        }
                        return;
                    }
                    activity.runOnUiThread(() -> FaceDetect.detectFaces(bmp, faceBox, (faces, originalBitmap) -> {
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

                                    int minStickerPx = Controller.dp(WRAPPER_MIN_DP, getResources());
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

                                    Controller.hideControllers(newSticker);
                                    Controller.setStickerActive(newSticker, true);

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

                        requireActivity().findViewById(R.id.faceContainer).setVisibility(View.GONE);
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
                requireActivity().findViewById(R.id.faceContainer).setVisibility(View.GONE);
            }
        });

        faceBox = new FaceBoxOverlayView(requireContext());
        faceOverlay.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
                FaceDetect.detectFaces(bmp, faceBox, (faces, originalBitmap) -> {
                    if (faces.isEmpty()) return;
                });
            }
        });
    }

    /*private void updateMeta() {
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
        tv.setPadding(Controller.dp(14, getResources()), Controller.dp(10, getResources()), Controller.dp(14, getResources()), Controller.dp(10, getResources()));
        tv.setElevation(Controller.dp(4, getResources()));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC222222);
        bg.setCornerRadius(Controller.dp(16, getResources()));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.faceBox != null) {
            this.faceBox.clearBoxes();
            this.faceBox.setVisibility(View.GONE);
        }
    }
}