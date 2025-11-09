/*
package com.example.filter.fragments;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.FaceModeViewModel;

public class EditMyStickerFragment2 extends Fragment {
    private View stickerWrapper;
    private ImageView editFrame, stickerImage, rotateController, sizeController;
    private CheckBox checkBox;
    private ImageButton cancelBtn, checkBtn;
    private float origX, origY, origRotation;
    private int origW, origH;
    private FrameLayout stickerOverlay;
    private static final int WRAPPER_MIN_DP = 100;
    public static String sLastReturnOriginAction = null;
    private boolean[] prevEnabledSnapshot;
    //ImageButton undoSticker, redoSticker, originalSticker;
    private Float prevElevation = null;
    private Float entryX = null, entryY = null, entryR = null;
    private Integer entryW = null, entryH = null;
    private static final float EPS_POS = 0.5f;
    private static final float EPS_ROT = 0.5f;
    private static final int EPS_SIZE = 1;
    private FaceModeViewModel viewModel;
    private String stickerId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_edit_my_sticker, container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        checkBox = v.findViewById(R.id.checkBox);
        cancelBtn = v.findViewById(R.id.cancelBtn);
        checkBtn = v.findViewById(R.id.checkBtn);

        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        if (stickerOverlay == null) return;

        */
/*undoSticker = requireActivity().findViewById(R.id.undoSticker);
        redoSticker = requireActivity().findViewById(R.id.redoSticker);
        originalSticker = requireActivity().findViewById(R.id.originalSticker);

        if (undoSticker != null) undoSticker.setVisibility(View.INVISIBLE);
        if (redoSticker != null) redoSticker.setVisibility(View.INVISIBLE);
        if (originalSticker != null) originalSticker.setVisibility(View.INVISIBLE);*//*


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
        Controller.raiseStickerToAbsoluteTop(stickerWrapper, stickerOverlay);

        editFrame = stickerWrapper.findViewById(R.id.editFrame);
        stickerImage = stickerWrapper.findViewById(R.id.stickerImage);
        rotateController = stickerWrapper.findViewById(R.id.rotateController);
        sizeController = stickerWrapper.findViewById(R.id.sizeController);

        if (stickerImage == null || editFrame == null || rotateController == null || sizeController == null) {
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        Controller.enableStickerControl(stickerWrapper, editFrame, rotateController, sizeController, stickerOverlay, getResources());

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        String origin = args.getString("origin", "mystickers");
        prevEnabledSnapshot = args.getBooleanArray("prevEnabled");

        origX = args.getFloat("x", stickerWrapper.getX());
        origY = args.getFloat("y", stickerWrapper.getY());
        origW = args.getInt("w", stickerWrapper.getLayoutParams().width);
        origH = args.getInt("h", stickerWrapper.getLayoutParams().height);
        origRotation = args.getFloat("rotation", stickerWrapper.getRotation());

        int minPx = Controller.dp(WRAPPER_MIN_DP, getResources());
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
            Controller.positionControllers(stickerWrapper, editFrame, rotateController, sizeController);

            entryX = stickerWrapper.getX();
            entryY = stickerWrapper.getY();
            entryW = stickerWrapper.getLayoutParams().width;
            entryH = stickerWrapper.getLayoutParams().height;
            entryR = stickerWrapper.getRotation();
        });

        stickerWrapper.setOnClickListener(null);
        stickerWrapper.setClickable(false);

        Controller.updateControllersSizeAndAngle(stickerWrapper, editFrame, rotateController, sizeController, getResources());

        cancelBtn.setOnClickListener(x -> {
            if (ClickUtils.isFastClick(x, 400)) return;

            if ("stickers".equals(origin)) {
                restoreElevationIfNeeded();

                */
/*FilterActivity a = (FilterActivity) requireActivity();
                if (stickerWrapper != null) {
                    a.recordStickerDelete(stickerWrapper);
                }*//*

            }

            Controller.removeStickerFrame(stickerWrapper);
            Controller.hideControllers(editFrame, rotateController, sizeController, stickerWrapper);

            if ("stickers".equals(origin)) {
                if (stickerOverlay != null) {
                    for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                        View child = stickerOverlay.getChildAt(i);
                        Controller.setStickerActive(child, true);
                        child.setTag(R.id.tag_prev_enabled, null);
                    }
                }
                goBackTo(new StickersFragment2());
            } else {
                if (stickerOverlay != null) {
                    if (prevEnabledSnapshot != null &&
                            prevEnabledSnapshot.length == stickerOverlay.getChildCount()) {
                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            View child = stickerOverlay.getChildAt(i);
                            Controller.setStickerActive(child, prevEnabledSnapshot[i]);
                            child.setTag(R.id.tag_prev_enabled, null);
                        }
                    } else {
                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            View child = stickerOverlay.getChildAt(i);
                            Object prev = child.getTag(R.id.tag_prev_enabled);
                            boolean prevEnabled = (prev instanceof Boolean) ? (Boolean) prev : true;
                            Controller.setStickerActive(child, prevEnabled);
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

            else {
                if (stickerWrapper != null) {
                    Controller.setStickerActive(stickerWrapper, true);
                    stickerWrapper.setTag("sessionAdded");
                }

                Controller.hideControllers(editFrame, rotateController, sizeController, stickerWrapper);

                if ("stickers".equals(origin)) {
                    FilterActivity a = (FilterActivity) requireActivity();

                    */
/*int aw = stickerWrapper.getLayoutParams().width;
                    int ah = stickerWrapper.getLayoutParams().height;
                    float ax = stickerWrapper.getX();
                    float ay = stickerWrapper.getY();
                    float ar = stickerWrapper.getRotation();*//*


                    */
/*boolean samePos = (entryX != null && entryY != null)
                            && Math.abs(ax - entryX) < EPS_POS
                            && Math.abs(ay - entryY) < EPS_POS;
                    boolean sameRot = (entryR != null) && Math.abs(ar - entryR) < EPS_ROT;
                    boolean sameSize = (entryW != null && entryH != null)
                            && Math.abs(aw - entryW) <= EPS_SIZE
                            && Math.abs(ah - entryH) <= EPS_SIZE;*//*


                    */
/*if (!(samePos && sameRot && sameSize)) {
                        float bx = getArguments().getFloat("prevX", stickerWrapper.getX());
                        float by = getArguments().getFloat("prevY", stickerWrapper.getY());
                        int bw = getArguments().getInt("prevW", stickerWrapper.getLayoutParams().width);
                        int bh = getArguments().getInt("prevH", stickerWrapper.getLayoutParams().height);
                        float br = getArguments().getFloat("prevR", stickerWrapper.getRotation());

                        a.recordStickerEdit(stickerWrapper, bx, by, bw, bh, br, ax, ay, aw, ah, ar);
                    }*//*


                    if (stickerOverlay != null) {
                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            View child = stickerOverlay.getChildAt(i);
                            Controller.setStickerActive(child, true);
                            child.setTag(R.id.tag_prev_enabled, null);
                        }
                    }
                    restoreElevationIfNeeded();
                    goBackTo(new StickersFragment2());
                } else {
                    if (stickerOverlay != null) {
                        if (prevEnabledSnapshot != null &&
                                prevEnabledSnapshot.length == stickerOverlay.getChildCount()) {
                            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                                View child = stickerOverlay.getChildAt(i);
                                Controller.setStickerActive(child, prevEnabledSnapshot[i]);
                                child.setTag(R.id.tag_prev_enabled, null);
                            }
                        } else {
                            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                                View child = stickerOverlay.getChildAt(i);
                                Object prev = child.getTag(R.id.tag_prev_enabled);
                                boolean prevEnabled = (prev instanceof Boolean) ? (Boolean) prev : true;
                                Controller.setStickerActive(child, prevEnabled);
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

        viewModel = new ViewModelProvider(requireActivity()).get(FaceModeViewModel.class);

        stickerId = (String) stickerWrapper.getTag(R.id.tag_sticker_id);
        if (stickerId == null) {
            stickerId = "sticker_" + System.currentTimeMillis();
            stickerWrapper.setTag(R.id.tag_sticker_id, stickerId);
        }

        viewModel.getStickerState(stickerId).observe(getViewLifecycleOwner(), isChecked -> {
            if (checkBox.isChecked() != isChecked) {
                checkBox.setChecked(isChecked);
            }
        });

        checkBox.setChecked(Boolean.TRUE.equals(stickerWrapper.getTag(R.id.tag_face_mode)));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setStickerState(stickerId, isChecked);

            if (isChecked) {
                ImageView stickerImage = stickerWrapper.findViewById(R.id.stickerImage);

                Bitmap bitmap = null;
                if (stickerImage.getDrawable() != null) {
                    stickerImage.setDrawingCacheEnabled(false);
                    stickerImage.buildDrawingCache();

                    bitmap = Bitmap.createBitmap(
                            stickerImage.getDrawable().getIntrinsicWidth(),
                            stickerImage.getDrawable().getIntrinsicHeight(),
                            Bitmap.Config.ARGB_8888
                    );
                    Canvas canvas = new Canvas(bitmap);
                    stickerImage.getDrawable().setBounds(
                            0,
                            0,
                            stickerImage.getDrawable().getIntrinsicWidth(),
                            stickerImage.getDrawable().getIntrinsicHeight()
                    );
                    stickerImage.getDrawable().draw(canvas);
                }

                if (bitmap == null) {
                    checkBox.setChecked(false);
                    return;
                }

                FaceFragment2 faceFragment2 = new FaceFragment2();
                Bundle args2 = new Bundle();

                viewModel.setTempBitmap(bitmap);
                args2.putBoolean("useTempBitmap", true);

                args2.putString("stickerId", stickerId);
                args2.putString("returnOrigin", origin);

                args2.putString("batchId", (String) stickerWrapper.getTag(R.id.tag_face_batch_id));
                args2.putBoolean("isBatchEdit", true);

                long sid = -1L;
                Object s = stickerWrapper.getTag(R.id.tag_session_id);
                if (s instanceof Long) sid = (Long) s;
                else sid = getArguments() != null ? getArguments().getLong("sessionId", -1L) : -1L;
                args2.putLong("sessionId", sid);

                final int sessionBaseline = getArguments() != null
                        ? getArguments().getInt("sessionBaseline", (stickerOverlay != null) ? stickerOverlay.getChildCount() : 0)
                        : (stickerOverlay != null ? stickerOverlay.getChildCount() : 0);
                args2.putInt("sessionBaseline", sessionBaseline);

                faceFragment2.setArguments(args2);

                requireActivity().findViewById(R.id.faceContainer).setVisibility(View.VISIBLE);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.faceContainer, faceFragment2, "face_fragment")
                        .addToBackStack("face_from_edit")
                        .commit();
            }
        });
    }

    private void restoreElevationIfNeeded() {
        if (stickerWrapper != null && prevElevation != null) {
            ViewCompat.setZ(stickerWrapper, prevElevation);
            stickerWrapper.invalidate();
        }
    }

    private void goBackTo(Fragment f) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_up, 0)
                .replace(R.id.bottomArea2, f)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        */
/*if (StickersFragment.faceBox != null) {
            StickersFragment.faceBox.clearBoxes();
            StickersFragment.faceBox.setVisibility(View.GONE);
        }*//*

    }
}*/
