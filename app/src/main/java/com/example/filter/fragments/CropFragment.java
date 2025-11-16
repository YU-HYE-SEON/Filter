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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.overlayviews.CropBoxOverlayView;

public class CropFragment extends Fragment {
    private AppCompatButton saveBtn;
    private LinearLayout freeCropBtn, OTORatioBtn, TTFRatioBtn, NTSRatioBtn;
    private ImageView freeCropIcon, OTORatioIcon, TTFRatioIcon, NTSRatioIcon;
    private TextView freeCropTxt, OTOTxt, TTFTxt, NTSTxt;
    private ImageButton cancelBtn, checkBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_crop, container, false);

        freeCropBtn = view.findViewById(R.id.freeCropBtn);
        freeCropIcon = view.findViewById(R.id.freeCropIcon);
        freeCropTxt = view.findViewById(R.id.freeCropTxt);

        OTORatioBtn = view.findViewById(R.id.OTORatioBtn);
        OTORatioIcon = view.findViewById(R.id.OTORatioIcon);
        OTOTxt = view.findViewById(R.id.OTOTxt);

        TTFRatioBtn = view.findViewById(R.id.TTFRatioBtn);
        TTFRatioIcon = view.findViewById(R.id.TTFRatioIcon);
        TTFTxt = view.findViewById(R.id.TTFTxt);

        NTSRatioBtn = view.findViewById(R.id.NTSRatioBtn);
        NTSRatioIcon = view.findViewById(R.id.NTSRatioIcon);
        NTSTxt = view.findViewById(R.id.NTSTxt);

        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        FilterActivity activity = (FilterActivity) requireActivity();

        checkBtn.setEnabled(false);
        checkBtn.setAlpha(0.4f);

        freeCropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.setCurrentCropMode(FilterActivity.CropMode.FREE);
                updateIconUI(FilterActivity.CropMode.FREE);
                activity.showCropOverlay(true, false, 0, 0, false);
                updateCheckButtonState(activity);
            }
        });

        OTORatioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.setCurrentCropMode(FilterActivity.CropMode.OTO);
                updateIconUI(FilterActivity.CropMode.OTO);
                activity.showCropOverlay(true, true, 1, 1, false);
                updateCheckButtonState(activity);
            }
        });

        TTFRatioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.setCurrentCropMode(FilterActivity.CropMode.TTF);
                updateIconUI(FilterActivity.CropMode.TTF);
                activity.showCropOverlay(true, true, 3, 4, false);
                updateCheckButtonState(activity);
            }
        });

        NTSRatioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.setCurrentCropMode(FilterActivity.CropMode.NTS);
                updateIconUI(FilterActivity.CropMode.NTS);
                activity.showCropOverlay(true, true, 9, 16, false);
                updateCheckButtonState(activity);
            }
        });

        restoreLastSelection(activity);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;
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
                if (ClickUtils.isFastClick(view, 400)) return;

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
                    /*int x = cropRect.left;
                    int y = cropRect.top;
                    int width = cropRect.width();
                    int height = cropRect.height();

                    //유효 범위 체크
                    if (x < 0 || y < 0
                            || width <= 0 || height <= 0
                            || x + width > fullBitmap.getWidth()
                            || y + height > fullBitmap.getHeight()) {
                        return;
                    }*/
                    int x = cropRect.left - vpX;
                    int y = cropRect.top - vpY;
                    int width = cropRect.width();
                    int height = cropRect.height();

                    int bmpW = fullBitmap.getWidth();
                    int bmpH = fullBitmap.getHeight();

                    if (x < 0) {
                        width += x;
                        x = 0;
                    }
                    if (y < 0) {
                        height += y;
                        y = 0;
                    }
                    if (x + width > bmpW) width = bmpW - x;
                    if (y + height > bmpH) height = bmpH - y;

                    if (width <= 0 || height <= 0) {
                        return;
                    }

                    Bitmap cropped = Bitmap.createBitmap(fullBitmap, x, y, width, height);

                    activity.getRenderer().setBitmap(cropped);

                    activity.resetViewportTransform();
                    activity.hideCropOverlay();
                    activity.getPhotoPreview().requestRender();
                    activity.commitTransformations();
                    activity.setCropEdited(true);

                    activity.appliedCropRectN = new RectF(lN, tN, rN, bN);

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

    @Override
    public void onResume() {
        super.onResume();
        saveBtn = requireActivity().findViewById(R.id.saveBtn);
        if (saveBtn != null) {
            saveBtn.setEnabled(false);
            saveBtn.setAlpha(0.4f);
        }
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
                activity.showCropOverlay(true, true, 1, 1, true);
                break;
            case TTF:
                activity.showCropOverlay(true, true, 3, 4, true);
                break;
            case NTS:
                activity.showCropOverlay(true, true, 9, 16, true);
                break;
        }

        updateCheckButtonState(activity);
    }

    private void updateIconUI(FilterActivity.CropMode mode) {
        freeCropIcon.setImageResource(R.drawable.icon_crop_free_no);
        OTORatioIcon.setImageResource(R.drawable.icon_crop_oto_no);
        TTFRatioIcon.setImageResource(R.drawable.icon_crop_ttf_no);
        NTSRatioIcon.setImageResource(R.drawable.icon_crop_nts_no);

        freeCropTxt.setTextColor(Color.parseColor("#90989F"));
        OTOTxt.setTextColor(Color.parseColor("#90989F"));
        TTFTxt.setTextColor(Color.parseColor("#90989F"));
        NTSTxt.setTextColor(Color.parseColor("#90989F"));

        switch (mode) {
            case FREE:
                freeCropIcon.setImageResource(R.drawable.icon_crop_free_yes);
                freeCropTxt.setTextColor(Color.parseColor("#C2FA7A"));
                break;
            case OTO:
                OTORatioIcon.setImageResource(R.drawable.icon_crop_oto_yes);
                OTOTxt.setTextColor(Color.parseColor("#C2FA7A"));
                break;
            case TTF:
                TTFRatioIcon.setImageResource(R.drawable.icon_crop_ttf_yes);
                TTFTxt.setTextColor(Color.parseColor("#C2FA7A"));
                break;
            case NTS:
                NTSRatioIcon.setImageResource(R.drawable.icon_crop_nts_yes);
                NTSTxt.setTextColor(Color.parseColor("#C2FA7A"));
                break;
        }
    }

    private boolean nearly(int a, int b, int tolPx) {
        return Math.abs(a - b) <= tolPx;
    }

    private void updateCheckButtonState(FilterActivity activity) {
        if (checkBtn == null) return;

        CropBoxOverlayView overlay = activity.getCropOverlay();
        boolean hasValidCropRect = false;

        if (overlay != null) {
            Rect cropRect = overlay.getCropRect();
            if (cropRect != null && cropRect.width() > 0 && cropRect.height() > 0) {
                hasValidCropRect = true;
            }
        }

        boolean enabled = hasValidCropRect;

        checkBtn.setEnabled(enabled);
        checkBtn.setAlpha(enabled ? 1f : 0.4f);
    }
}