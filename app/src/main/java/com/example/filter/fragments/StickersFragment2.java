/*
package com.example.filter.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import com.example.filter.etc.Controller;
import com.example.filter.etc.FaceDetect;
import com.example.filter.overlayviews.FaceBoxOverlayView;

public class StickersFragment2 extends Fragment {
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

        FilterActivity activity = (FilterActivity) getActivity();
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
        }


        if (bottomArea1 != null) {
            undoColor.setVisibility(View.INVISIBLE);
            redoColor.setVisibility(View.INVISIBLE);
            originalColor.setVisibility(View.INVISIBLE);
            undoSticker.setVisibility(View.VISIBLE);
            redoSticker.setVisibility(View.VISIBLE);
            originalSticker.setVisibility(View.VISIBLE);

            bottomArea1.setVisibility(View.VISIBLE);
            brushToSticker.setVisibility(View.GONE);
        }

        FilterActivity activity = (FilterActivity) requireActivity();
        FrameLayout photoPreviewContainer = activity.findViewById(R.id.photoPreviewContainer);

        faceBox = new FaceBoxOverlayView(requireContext());
        photoPreviewContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        activity.getPhotoPreview().queueEvent(() -> {
            Bitmap bmp = activity.getRenderer().getCurrentBitmap();
            activity.runOnUiThread(() -> FaceDetect.detectFaces(bmp, faceBox, (faces, originalBitmap) -> {
                if (faces.isEmpty()) return;
            }));
        });

        myStickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
                int sessionBaseline = (stickerOverlay != null) ? stickerOverlay.getChildCount() : 0;

                long sessionId = System.currentTimeMillis();

                MyStickersFragment2 f = new MyStickersFragment2();
                Bundle args = new Bundle();
                args.putInt("sessionBaseline", sessionBaseline);

                args.putLong("sessionId", sessionId);

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

    private void rewireOverlayClickListeners() {
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
    }


    private void attachEditListenerForSticker(@NonNull View stickerView, @NonNull FrameLayout overlay) {
        stickerView.setOnClickListener(null);

        stickerView.setOnClickListener(v -> {
            if (!isAdded()) return;
            FragmentActivity act = requireActivity();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;
            if (!stickerView.isEnabled()) return;

            Fragment cur = act.getSupportFragmentManager().findFragmentById(R.id.bottomArea2);
            if (cur instanceof EditMyStickerFragment2) return;
            //if (ClickUtils.isFastClick(v, 400)) return;

            if (stickerView.findViewById(R.id.stickerImage) == null ||
                    stickerView.findViewById(R.id.editFrame) == null ||
                    stickerView.findViewById(R.id.rotateController) == null ||
                    stickerView.findViewById(R.id.sizeController) == null) {
                return;
            }

            int beforeIndex = overlay.indexOfChild(stickerView);
            float beforeZ = ViewCompat.getZ(stickerView);

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
                Controller.raiseStickerToAbsoluteTop(stickerView, overlay);
                afterIndex = overlay.indexOfChild(stickerView);
                afterZ = ViewCompat.getZ(stickerView);

                boolean indexChanged = (afterIndex != beforeIndex);
                boolean zChanged = Math.abs(afterZ - beforeZ) > EPS_Z;

                if (indexChanged || zChanged) {
                    FilterActivity fa = (FilterActivity) act;
                    fa.recordStickerZOrderChange(stickerView, beforeIndex, beforeZ, afterIndex, afterZ);
                }

            }

            stickerView.bringToFront();
            overlay.requestLayout();
            overlay.invalidate();

            for (int i = 0; i < overlay.getChildCount(); i++) {
                View child = overlay.getChildAt(i);
                Controller.hideControllers(child);
                Object t = child.getTag();
                if ("editingSticker".equals(t)) {
                    child.setTag(null);
                }
            }

            for (int i = 0; i < overlay.getChildCount(); i++) {
                View child = overlay.getChildAt(i);
                child.setTag(R.id.tag_prev_enabled, child.isEnabled());
                Controller.setStickerActive(child, child == stickerView);
            }

            Controller.setControllersVisible(stickerView, true);
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

            EditMyStickerFragment2 edit = new EditMyStickerFragment2();
            edit.setArguments(args);

            act.getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, edit)
                    .addToBackStack("edit_from_stickers")
                    .commit();
        });
    }

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

            Controller.setStickerActive(child, true);
            Controller.hideControllers(child);
            attachEditListenerForSticker(child, stickerOverlay);
        }
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
*/
