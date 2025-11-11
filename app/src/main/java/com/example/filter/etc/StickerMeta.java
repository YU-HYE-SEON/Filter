package com.example.filter.etc;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.filter.R;
import com.example.filter.activities.ApplyFilterActivity;
import com.example.filter.activities.FilterActivity;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.List;

public class StickerMeta {
    public static float relX, relY, relW, relH, rot;
    private static final int WRAPPER_MIN_DP = 100;

    public StickerMeta(float relX, float relY, float relW, float relH, float rot) {
        this.relX = relX;
        this.relY = relY;
        this.relW = relW;
        this.relH = relH;
        this.rot = rot;
    }

    public static StickerMeta calculate(Face face, Bitmap bitmap, View stickerFrame, FrameLayout faceOverlay) {
        if (faceOverlay != null) {
            int vpW = faceOverlay.getWidth();
            int vpH = faceOverlay.getHeight();
            int bmpW = bitmap.getWidth();
            int bmpH = bitmap.getHeight();

            float scale = Math.min((float) vpW / bmpW, (float) vpH / bmpH);
            float offsetX = (vpW - bmpW * scale) / 2f;
            float offsetY = (vpH - bmpH * scale) / 2f;


            Rect faceBox = face.getBoundingBox();
            FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
            FaceLandmark mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);

            float faceCenterX = faceBox.centerX();
            float faceCenterY = faceBox.centerY();
            float faceW = faceBox.width();
            float faceH = faceBox.height();

            String center = "바운딩 박스";
            if (nose != null) {
                faceCenterX = nose.getPosition().x;
                faceCenterY = nose.getPosition().y;
                center = "코";
            } else if (leftEye != null && rightEye != null) {
                faceCenterX = (leftEye.getPosition().x + rightEye.getPosition().x) / 2;
                faceCenterY = (leftEye.getPosition().y + rightEye.getPosition().y) / 2;
                center = "눈";
            } else if (mouth != null) {
                faceCenterX = mouth.getPosition().x;
                faceCenterY = mouth.getPosition().y;
                center = "입";
            }

            float faceCenterX_view = (faceCenterX * scale) + offsetX;
            float faceCenterY_view = (faceCenterY * scale) + offsetY;
            float faceW_view = Math.round(faceW * scale);
            float faceH_view = Math.round(faceH * scale);

            float stickerX = stickerFrame.getX();
            float stickerY = stickerFrame.getY();
            float stickerW = stickerFrame.getWidth();
            float stickerH = stickerFrame.getHeight();
            float stickerR = stickerFrame.getRotation();

            float stickerCenterX = stickerX + stickerW / 2f;
            float stickerCenterY = stickerY + stickerH / 2f;

            float dx = stickerCenterX - faceCenterX_view;
            float dy = -(stickerCenterY - faceCenterY_view);

            relX = dx / faceW_view;
            relY = dy / faceH_view;
            relW = stickerW / faceW_view;
            relH = stickerH / faceH_view;
            rot = stickerR % 360f;
            if (rot < 0) rot += 360f;

            //Log.d("얼굴스티커", String.format("[얼굴모델] 중심= %s, relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f", center, relX, relY, relW, relH, rot));
        }
        return new StickerMeta(relX, relY, relW, relH, rot);
    }

    public static List<float[]> recalculate(List<Face> faces, Bitmap bitmap, FrameLayout stickerOverlay, StickerMeta metaData, Context context) {
        Activity activity = null;
        FGLRenderer renderer = null;
        Resources resources = null;

        if (context instanceof Activity) {
            activity = (Activity) context;
        }

        if (activity instanceof FilterActivity) {
            FilterActivity filterActivity = (FilterActivity) activity;
            renderer = filterActivity.getRenderer();
            resources = filterActivity.getResources();
        } else if (activity instanceof ApplyFilterActivity) {
            ApplyFilterActivity applyActivity = (ApplyFilterActivity) activity;
            renderer = applyActivity.getRenderer();
            resources = applyActivity.getResources();
        }

        if (renderer == null || resources == null) return new ArrayList<>();

        List<float[]> resultList = new ArrayList<>();

        if (stickerOverlay != null) {
            int vpX = renderer.getViewportX();
            int vpY = renderer.getViewportY();
            int vpW = renderer.getViewportWidth();
            int vpH = renderer.getViewportHeight();
            int bmpW = bitmap.getWidth();
            int bmpH = bitmap.getHeight();

            float scale = Math.min((float) vpW / bmpW, (float) vpH / bmpH);
            float offsetX = vpX + (vpW - bmpW * scale) / 2f;
            float offsetY = vpY + (vpH - bmpH * scale) / 2f;

            for (Face face : faces) {
                Rect faceBox = face.getBoundingBox();
                FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
                FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                FaceLandmark mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);

                float faceCenterX = faceBox.centerX();
                float faceCenterY = faceBox.centerY();
                float faceW = faceBox.width();
                float faceH = faceBox.height();

                String center = "바운딩 박스";
                if (nose != null) {
                    faceCenterX = nose.getPosition().x;
                    faceCenterY = nose.getPosition().y;
                    center = "코";
                } else if (leftEye != null && rightEye != null) {
                    faceCenterX = (leftEye.getPosition().x + rightEye.getPosition().x) / 2;
                    faceCenterY = (leftEye.getPosition().y + rightEye.getPosition().y) / 2;
                    center = "눈";
                } else if (mouth != null) {
                    faceCenterX = mouth.getPosition().x;
                    faceCenterY = mouth.getPosition().y;
                    center = "입";
                }

                float eulerZ = face.getHeadEulerAngleZ();
                //float eulerY = face.getHeadEulerAngleY();
                float eulerX = face.getHeadEulerAngleX();

                float stickerCenterX = faceCenterX + (metaData.relX * faceW);
                float stickerCenterY = faceCenterY + (metaData.relY * faceH);

                float radiansZ = (float) Math.toRadians(-eulerZ);
                float dx = stickerCenterX - faceCenterX;
                float dy = -(stickerCenterY - faceCenterY);
                float rotatedX = (float) (dx * Math.cos(radiansZ) - dy * Math.sin(radiansZ));
                float rotatedY = (float) (dx * Math.sin(radiansZ) + dy * Math.cos(radiansZ));

                //float radiansY = (float) Math.toRadians(eulerY);
                //float depthShiftRatio = 0.35f;
                //float yawShiftX = (float) (Math.sin(radiansY) * faceW * depthShiftRatio);
                //float yawShiftY = (float) (Math.abs(Math.sin(radiansY)) * faceH * 0.1f);

                float radiansX = (float) Math.toRadians(eulerX);
                float pitchShiftRatio = 0.25f;
                float pitchShiftY = (float) (Math.sin(radiansX) * faceH * pitchShiftRatio);

                stickerCenterX = (faceCenterX + rotatedX) /*+ yawShiftX*/;
                stickerCenterY = (faceCenterY + rotatedY) /*- yawShiftY*/ + pitchShiftY;

                float stickerW = metaData.relW * faceW;
                float stickerH = metaData.relH * faceH;

                float stickerCenterX_view = (stickerCenterX * scale) + offsetX;
                float stickerCenterY_view = (stickerCenterY * scale) + offsetY;
                int stickerW_view = Math.round(stickerW * scale);
                int stickerH_view = Math.round(stickerH * scale);

                int minStickerPx = Controller.dp(WRAPPER_MIN_DP, resources);
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
                float stickerR = metaData.rot - eulerZ;

                //Log.d("얼굴스티커", String.format("[사진얼굴] 중심= %s, relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f", center, stickerX_view, stickerY_view, (float) stickerW_view, (float) stickerH_view, stickerR));

                resultList.add(new float[]{stickerX_view, stickerY_view, stickerW_view, stickerH_view, stickerR});
            }
        }
        return resultList;
    }

    public static View cloneSticker(FrameLayout stickerOverlay, View stickerFrame, Context context, float[] placement) {
        if (stickerFrame == null || context == null) return null;

        LayoutInflater inflater = LayoutInflater.from(context);
        View cloneSticker = inflater.inflate(R.layout.v_sticker_edit, (ViewGroup) stickerOverlay, false);

        ImageView oldImage = stickerFrame.findViewById(R.id.stickerImage);
        ImageView newImage = cloneSticker.findViewById(R.id.stickerImage);

        if (oldImage != null && oldImage.getDrawable() != null && newImage != null) {
            newImage.setImageDrawable(oldImage.getDrawable().getConstantState().newDrawable());
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.round(placement[2]), Math.round(placement[3]));
        cloneSticker.setLayoutParams(lp);
        cloneSticker.setX(placement[0]);
        cloneSticker.setY(placement[1]);
        cloneSticker.setRotation(placement[4]);

        Controller.setControllersVisible(cloneSticker, false);
        Controller.setStickerActive(cloneSticker, true);

        stickerOverlay.addView(cloneSticker);

        return cloneSticker;
    }
}