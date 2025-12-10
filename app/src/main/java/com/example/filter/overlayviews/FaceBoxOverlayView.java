package com.example.filter.overlayviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FaceBoxOverlayView extends View {
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmp = new RectF();
    private final List<Rect> boxes = new ArrayList<>();
    private int imageW = 0, imageH = 0;

    private int viewportW = 0, viewportH = 0;
    private int viewportX = 0, viewportY = 0;


    public FaceBoxOverlayView(Context c) {
        super(c);
        init();
    }

    public FaceBoxOverlayView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setColor(Color.TRANSPARENT);
    }

    // 카메라 화면용
    public void setFaceBoxes(List<Rect> faceBoxes, int srcW, int srcH, int vW, int vH, int vX, int vY) {
        boxes.clear();
        if (faceBoxes != null) {
            boxes.addAll(faceBoxes);
        }
        imageW = srcW;
        imageH = srcH;

        viewportW = vW;
        viewportH = vH;
        viewportX = vX;
        viewportY = vY;

        invalidate();
    }

    // 나머지 고정된 이미지일 떄 (필터 제작할 때, 구매 후 적용할 때)
    public void setFaceBoxes(List<Rect> faceBoxes, int srcW, int srcH) {
        boxes.clear();
        if (faceBoxes != null) {
            boxes.addAll(faceBoxes);
        }
        imageW = srcW;
        imageH = srcH;

        viewportW = 0;
        viewportH = 0;
        viewportX = 0;
        viewportY = 0;

        invalidate();
    }

    public void clearBoxes() {
        boxes.clear();

        viewportW = 0;
        viewportH = 0;
        viewportX = 0;
        viewportY = 0;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (imageW <= 0 || imageH <= 0 || boxes.isEmpty())
            return;                                                                         //imageW, imageH는 원본 이미지 크기

        float viewW = getWidth();                                                           //viewW, viewH는 FaceBoxOverlayView의 너비와 높이
        float viewH = getHeight();                                                          //(ex)CameraActivity에서는 cameraContainer 크기가 됨, 필터 제작할 때랑 구매 후 적용할 때는 사진 이미지 크기가 됨

        float drawAreaW = viewportW > 0 ? (float) viewportW : viewW;                        //viewportW, viewportH는 GLRenderer 뷰포트 크기 / viewportX, viewportY는 GL 뷰포트 시작 좌표
        float drawAreaH = viewportH > 0 ? (float) viewportH : viewH;                        //(CGLRenderer는 카메라 뷰포트 / FGLRenderer는 필터 제작과 구매 후 적용할 때 뷰포트)
        float drawAreaX = viewportW > 0 ? (float) viewportX : 0f;                           //drawAreaW, drawAreaH는 카메라뷰 영역 크기 / drawAreaX, drawAreaY는 카메라뷰 영역 시작 좌표
        float drawAreaY = viewportH > 0 ? (float) viewportY : 0f;                           //카메라뷰포트는 선택한 비율에 따라서 바뀌기 때문에 이렇게 설정해야 페이스박스크기가 비율에 따라 맞게 변함

        float scaleX = drawAreaW / (float) imageW;                                          //scaleX, scaleY는 원본 이미지의 너비와 높이를 최종 드로잉 영역에 맞추기 위한 비율
        float scaleY = drawAreaH / (float) imageH;                                          //scale는 최종 스케일 비율 (scaleX와 scaleY 중 더 작은 값을 선택하여 비율을 유지하며 최대한 크게 넣는 Fit-Center 방식)
        float scale = Math.min(scaleX, scaleY);

        float renderedW = (float) imageW * scale;                                           //renderedW, renderedH는 실제 화면에서 보이는 영역의 너비와 높이
        float renderedH = (float) imageH * scale;                                           //(ex)CameraActivity에서는 카메라뷰 영역 안에서 보이는 사진 이미지 크기가 됨(비율에 따라 바뀜), 필터 제작할 때랑 구매 후 적용할 때는 사진 이미지 크기가 됨

        float offsetX = drawAreaX + (drawAreaW - renderedW) / 2f;                           //offsetX, offsetY는 최종 시작 오프셋 (뷰포트 영역 내에서 이미지가 중앙에 오도록)
        float offsetY = drawAreaY + (drawAreaH - renderedH) / 2f;

        for (Rect r : boxes) {
            tmp.set(
                    offsetX + r.left * scale,
                    offsetY + r.top * scale,
                    offsetX + r.right * scale,
                    offsetY + r.bottom * scale
            );
            canvas.drawRect(tmp, boxPaint);
        }
    }
}
