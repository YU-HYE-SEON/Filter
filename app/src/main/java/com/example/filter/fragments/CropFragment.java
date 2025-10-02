package com.example.filter.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.CropBoxOverlayView;

public class CropFragment extends Fragment {
    private ImageButton freeCutIcon, OTORatioIcon, TTFRatioIcon, NTSRatioIcon;
    private TextView freeCutTxt, OTOTxt, TTFTxt, NTSTxt;
    private ImageButton cancelBtn, checkBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_crop, container, false);

        freeCutIcon = view.findViewById(R.id.freeCutIcon);
        OTORatioIcon = view.findViewById(R.id.OTORatioIcon);
        TTFRatioIcon = view.findViewById(R.id.TTFRatioIcon);
        NTSRatioIcon = view.findViewById(R.id.NTSRatioIcon);
        freeCutTxt = view.findViewById(R.id.freeCutTxt);
        OTOTxt = view.findViewById(R.id.OTOTxt);
        TTFTxt = view.findViewById(R.id.TTFTxt);
        NTSTxt = view.findViewById(R.id.NTSTxt);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        FilterActivity activity = (FilterActivity) requireActivity();

        freeCutIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                activity.setCurrentCropMode(FilterActivity.CropMode.FREE);
                updateIconUI(FilterActivity.CropMode.FREE);
                activity.showCropOverlay(true, false, 0, 0, false);
            }
        });

        OTORatioIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                activity.setCurrentCropMode(FilterActivity.CropMode.OTO);
                updateIconUI(FilterActivity.CropMode.OTO);
                activity.showCropOverlay(false, true, 1, 1, false);
            }
        });

        TTFRatioIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                activity.setCurrentCropMode(FilterActivity.CropMode.TTF);
                updateIconUI(FilterActivity.CropMode.TTF);
                activity.showCropOverlay(false, true, 3, 4, false);
            }
        });

        NTSRatioIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                activity.setCurrentCropMode(FilterActivity.CropMode.NTS);
                updateIconUI(FilterActivity.CropMode.NTS);
                activity.showCropOverlay(false, true, 9, 16, false);
            }
        });

        restoreLastSelection(activity);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                activity.setCurrentCropMode(FilterActivity.CropMode.NONE);
                activity.restoreCropBeforeState();
                activity.hideCropOverlay();
                activity.getPhotoPreview().requestRender();

                activity.revertLastSelectionToApplied();

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new ToolsFragment())
                        .commit();
            }
        });

        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                activity.snapshotViewTransformForRestore();
                activity.setCurrentCropMode(FilterActivity.CropMode.NONE);

                CropBoxOverlayView overlay = activity.getCropOverlay();
                if (overlay == null) return;

                Rect cropRect = overlay.getCropRect();
                if (cropRect == null) return;

                int vpX = activity.getRenderer().getViewportX();
                int vpY = activity.getRenderer().getViewportY();
                int vpW = activity.getRenderer().getViewportWidth();
                int vpH = activity.getRenderer().getViewportHeight();

                int tol = 2;
                boolean isFullViewport =
                        nearly(cropRect.left, vpX, tol) &&
                                nearly(cropRect.top, vpY, tol) &&
                                nearly(cropRect.right, vpX + vpW, tol) &&
                                nearly(cropRect.bottom, vpY + vpH, tol);

                if (isFullViewport && activity.isViewportIdentity()) {
                    activity.hideCropOverlay();
                    activity.resetViewportTransform();
                    activity.getPhotoPreview().requestRender();

                    activity.setCropEdited(false);
                    activity.revertLastSelectionToApplied();
                    activity.commitTransformations(false);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, new ToolsFragment())
                            .commit();
                    return;
                }

                float lN = (cropRect.left - vpX) / (float) vpW;
                float tN = (cropRect.top - vpY) / (float) vpH;
                float rN = (cropRect.right - vpX) / (float) vpW;
                float bN = (cropRect.bottom - vpY) / (float) vpH;

                activity.setLastCropRectNormalized(new RectF(lN, tN, rN, bN));

                activity.lockInCurrentCropMode();

                activity.getRenderer().setOnBitmapCaptureListener(fullBitmap -> {
                    int x = cropRect.left;
                    int y = cropRect.top;
                    int width = cropRect.width();
                    int height = cropRect.height();

                    //유효 범위 체크
                    if (x < 0 || y < 0
                            || width <= 0 || height <= 0
                            || x + width > fullBitmap.getWidth()
                            || y + height > fullBitmap.getHeight()) {
                        return;
                    }

                    Bitmap cropped = Bitmap.createBitmap(fullBitmap, x, y, width, height);

                    activity.getRenderer().setBitmap(cropped);

                    activity.resetViewportTransform();
                    activity.hideCropOverlay();
                    activity.getPhotoPreview().requestRender();
                    activity.commitTransformations();
                    activity.setCropEdited(true);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, new ToolsFragment())
                            .commit();

                });

                activity.getRenderer().captureBitmapUnfiltered();
            }
        });

        return view;
    }

    private void restoreLastSelection(FilterActivity activity) {
        FilterActivity.CropMode mode = activity.getLastCropMode();
        if (mode == FilterActivity.CropMode.NONE) return;

        activity.setCurrentCropMode(mode);
        updateIconUI(mode);

        switch (mode) {
            case FREE:
                activity.showCropOverlay(true, false, 0, 0, true);
                break;
            case OTO:
                activity.showCropOverlay(false, true, 1, 1, true);
                break;
            case TTF:
                activity.showCropOverlay(false, true, 3, 4, true);
                break;
            case NTS:
                activity.showCropOverlay(false, true, 9, 16, true);
                break;
        }
    }

    private void updateIconUI(FilterActivity.CropMode mode) {
        freeCutIcon.setImageResource(R.drawable.icon_rotation_no);
        OTORatioIcon.setImageResource(R.drawable.icon_rotation_no);
        TTFRatioIcon.setImageResource(R.drawable.icon_rotation_no);
        NTSRatioIcon.setImageResource(R.drawable.icon_rotation_no);

        freeCutTxt.setTextColor(Color.WHITE);
        OTOTxt.setTextColor(Color.WHITE);
        TTFTxt.setTextColor(Color.WHITE);
        NTSTxt.setTextColor(Color.WHITE);

        switch (mode) {
            case FREE:
                freeCutIcon.setImageResource(R.drawable.icon_rotation_yes);
                freeCutTxt.setTextColor(Color.parseColor("#C2FA7A"));
                break;
            case OTO:
                OTORatioIcon.setImageResource(R.drawable.icon_rotation_yes);
                OTOTxt.setTextColor(Color.parseColor("#C2FA7A"));
                break;
            case TTF:
                TTFRatioIcon.setImageResource(R.drawable.icon_rotation_yes);
                TTFTxt.setTextColor(Color.parseColor("#C2FA7A"));
                break;
            case NTS:
                NTSRatioIcon.setImageResource(R.drawable.icon_rotation_yes);
                NTSTxt.setTextColor(Color.parseColor("#C2FA7A"));
                break;
        }
    }

    private boolean nearly(int a, int b, int tolPx) {
        return Math.abs(a - b) <= tolPx;
    }
}