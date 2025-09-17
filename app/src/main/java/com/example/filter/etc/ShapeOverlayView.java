package com.example.filter.etc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.example.filter.R;

public class ShapeOverlayView extends View {
    public enum ShapeType {
        NONE, SQUARE, STAR, TRIANGLE, CIRCLE, HEART
    }

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ShapeType currentShape = ShapeType.NONE;
    private Bitmap maskBitmap;
    private float maskScale = 0.6f;
    private final RectF maskRect = new RectF();
    private float centerX, centerY;
    private float lastX, lastY;
    private boolean dragging = false;
    private RectF imageBounds = null;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private float grabOffsetX, grabOffsetY;
    private float lastDrawW, lastDrawH;

    public ShapeOverlayView(Context context) {
        super(context);
        init();
    }

    public ShapeOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShapeOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        dimPaint.setColor(0x99000000);
        outPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (centerX == 0 && centerY == 0) {
            centerX = w / 2f;
            centerY = h / 2f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentShape == ShapeType.NONE || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        int layerId = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);

        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);

        if (maskBitmap != null && !maskBitmap.isRecycled()) {
            float shortSide = Math.min(getWidth(), getHeight());
            float targetSize = shortSide * maskScale;

            float bw = maskBitmap.getWidth();
            float bh = maskBitmap.getHeight();

            float scale = Math.min(targetSize / bw, targetSize / bh);

            float drawW = bw * scale;
            float drawH = bh * scale;

            lastDrawW = drawW;
            lastDrawH = drawH;

            float left = centerX - drawW / 2f;
            float top = centerY - drawH / 2f;

            if (imageBounds != null) {
                left = Math.max(imageBounds.left, Math.min(left, imageBounds.right - drawW));
                top = Math.max(imageBounds.top, Math.min(top, imageBounds.bottom - drawH));
            } else {
                left = Math.max(0, Math.min(left, getWidth() - drawW));
                top = Math.max(0, Math.min(top, getHeight() - drawH));
            }

            maskRect.set(left, top, left + drawW, top + drawH);

            canvas.drawBitmap(maskBitmap, null, maskRect, outPaint);
        }

        canvas.restoreToCount(layerId);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (currentShape == ShapeType.NONE) return false;

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                float x = e.getX(), y = e.getY();
                if (!maskRect.contains(x, y)) return false;

                activePointerId = e.getPointerId(0);
                grabOffsetX = x - centerX;
                grabOffsetY = y - centerY;
                dragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) return false;

                int idx = e.findPointerIndex(activePointerId);
                if (idx < 0) return false;

                float fx = e.getX(idx);
                float fy = e.getY(idx);

                centerX = fx - grabOffsetX;
                centerY = fy - grabOffsetY;

                float halfW = lastDrawW * 0.5f;
                float halfH = lastDrawH * 0.5f;
                float minX = (imageBounds != null ? imageBounds.left : 0f) + halfW;
                float maxX = (imageBounds != null ? imageBounds.right : getWidth()) - halfW;
                float minY = (imageBounds != null ? imageBounds.top : 0f) + halfH;
                float maxY = (imageBounds != null ? imageBounds.bottom : getHeight()) - halfH;

                centerX = Math.max(minX, Math.min(centerX, maxX));
                centerY = Math.max(minY, Math.min(centerY, maxY));

                postInvalidateOnAnimation();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                dragging = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    public void setShape(ShapeType type) {
        currentShape = type;
        loadMaskFor(type);

        if (type == ShapeType.NONE) {
            setVisibility(GONE);
        } else {
            if (getWidth() > 0 && getHeight() > 0) {
                centerX = getWidth() / 2f;
                centerY = getHeight() / 2f;
            }
            setVisibility(VISIBLE);
        }
        postInvalidateOnAnimation();
    }

    public void setMaskScale(float scale) {
        maskScale = Math.max(0.1f, Math.min(0.95f, scale));
        postInvalidateOnAnimation();
    }

    private void loadMaskFor(ShapeType type) {
        if (maskBitmap != null && !maskBitmap.isRecycled()) {
            maskBitmap.recycle();
            maskBitmap = null;
        }

        @DrawableRes int resId = 0;
        switch (type) {
            case SQUARE:
                resId = R.drawable.square_no;
                break;
            case STAR:
                resId = R.drawable.star_no;
                break;
            case TRIANGLE:
                resId = R.drawable.triangle_no;
                break;
            case CIRCLE:
                resId = R.drawable.circle_no;
                break;
            case HEART:
                resId = R.drawable.heart_no;
                break;
            case NONE:
            default:
        }

        if (resId != 0) {
            maskBitmap = BitmapFactory.decodeResource(getResources(), resId);
        }
    }

    public void setImageBounds(RectF rect) {
        this.imageBounds = new RectF(rect);
        postInvalidateOnAnimation();
    }

    public RectF getMaskRectOnView() { return new RectF(maskRect); }
    public boolean hasActiveShape() { return currentShape != ShapeType.NONE && maskBitmap != null; }
    public Bitmap getMaskBitmapForExport() { return maskBitmap; }
    public ShapeType getCurrentShape() { return currentShape; }
}