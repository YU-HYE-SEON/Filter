package com.example.filter.etc;

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

    public FaceBoxOverlayView(Context c) { super(c); init(); }
    public FaceBoxOverlayView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    private void init() {
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setColor(Color.TRANSPARENT);
    }

    public void setFaceBoxes(List<Rect> faceBoxes, int srcW, int srcH) {
        boxes.clear();
        if (faceBoxes != null) {
            boxes.addAll(faceBoxes);
        }
        imageW = srcW;
        imageH = srcH;
        invalidate();
    }

        public void clearBoxes() {
        boxes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (imageW <= 0 || imageH <= 0 || boxes.isEmpty()) return;

        float viewW = getWidth();
        float viewH = getHeight();
        float scaleX = viewW / (float) imageW;
        float scaleY = viewH / (float) imageH;
        float scale = Math.min(scaleX, scaleY);

        float offsetX = (viewW - imageW * scale) / 2f;
        float offsetY = (viewH - imageH * scale) / 2f;

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
