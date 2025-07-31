package com.example.filter.fragments;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.CropBoxOverlayView;

public class CropFragment extends Fragment {
    private ImageView freeCutIcon;
    private ImageView OTORatioIcon;
    private ImageView TTFRatioIcon;
    private ImageView NTSRatioIcon;
    private ImageButton cancelBtn;
    private ImageButton checkBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crop, container, false);

        freeCutIcon = view.findViewById(R.id.freeCutIcon);
        OTORatioIcon = view.findViewById(R.id.OTORatioIcon);
        TTFRatioIcon = view.findViewById(R.id.TTFRatioIcon);
        NTSRatioIcon = view.findViewById(R.id.NTSRatioIcon);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        FilterActivity activity = (FilterActivity) requireActivity();

        freeCutIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                //FilterActivity의 photoPreview 이미지 자르기
                //기본으로 사진 전체 영역으로 잡기, 크기 자유롭게 조절 가능
                // 필터 액티비티에 있는 cropOverlay 가져오기
                activity.setCurrentCropMode(FilterActivity.CropMode.FREE);
                activity.showCropOverlay(true, false, 0, 0);
            }
        });

        OTORatioIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                //FilterActivity의 photoPreview 이미지 자르기
                //기본으로 사진의 정중앙에서 가로세로 1대1 비율로 잡기, 크기 조절은 1대1 비율 고정
                // 필터 액티비티에 있는 cropOverlay 가져오기
                activity.setCurrentCropMode(FilterActivity.CropMode.OTO);
                activity.showCropOverlay(false, true, 1, 1);
            }
        });

        TTFRatioIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                //FilterActivity의 photoPreview 이미지 자르기
                //기본으로 사진의 정중앙에서 가로세로 3대4 비율로 잡기, 크기 조절은 3대4 비율 고정
                // 필터 액티비티에 있는 cropOverlay 가져오기
                activity.setCurrentCropMode(FilterActivity.CropMode.TTF);
                activity.showCropOverlay(false, true, 3, 4);
            }
        });

        NTSRatioIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                //FilterActivity의 photoPreview 이미지 자르기
                //기본으로 사진의 정중앙에서 가로세로 9대16 비율로 잡기, 크기 조절은 9대16 비율 고정
                activity.setCurrentCropMode(FilterActivity.CropMode.NTS);
                activity.showCropOverlay(false, true, 9, 16);
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                activity.setCurrentCropMode(FilterActivity.CropMode.NONE);
                activity.restoreCropBeforeState();
                activity.hideCropOverlay();
                activity.getPhotoPreview().requestRender();
                activity.getSupportFragmentManager().popBackStack();
            }
        });

        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                //FilterActivity의 photoPreview 이미지 자르기 작업 저장한 채로 ToolsFragment로 돌아감
                activity.setCurrentCropMode(FilterActivity.CropMode.NONE);

                CropBoxOverlayView overlay = activity.getCropOverlay();
                if (overlay == null) return;

                Rect cropRect = overlay.getCropRect();
                if (cropRect == null) return;

                int vpX = activity.getRenderer().getViewportX();
                int vpY = activity.getRenderer().getViewportY();
                int vpW = activity.getRenderer().getViewportWidth();
                int vpH = activity.getRenderer().getViewportHeight();

                float lN = (cropRect.left   - vpX) / (float) vpW;
                float tN = (cropRect.top    - vpY) / (float) vpH;
                float rN = (cropRect.right  - vpX) / (float) vpW;
                float bN = (cropRect.bottom - vpY) / (float) vpH;

                activity.setLastCropRectNormalized(new RectF(lN, tN, rN, bN));

                activity.getRenderer().setOnBitmapCaptureListener(fullBitmap -> {
                    int x = cropRect.left;
                    int y = cropRect.top;
                    int width = cropRect.width();
                    int height = cropRect.height();

                    // 유효 범위 체크
                    if (x < 0 || y < 0 || x + width > fullBitmap.getWidth() || y + height > fullBitmap.getHeight()) {
                        return;
                    }

                    Bitmap cropped = Bitmap.createBitmap(fullBitmap, x, y, width, height);

                    activity.getRenderer().setBitmap(cropped);
                    activity.resetViewportTransform();
                    activity.hideCropOverlay();
                    activity.getPhotoPreview().requestRender();
                    activity.commitTransformations();
                    activity.getSupportFragmentManager().popBackStack();
                });

                activity.getRenderer().captureBitmap();
            }
        });

        return view;
    }
}
