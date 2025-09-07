package com.example.filter.etc;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BrushOverlayView extends View {
    public enum BrushMode {NORMAL, GLOW, MOSAIC, ERASER}

    private static class Stroke {
        final Path path = new Path();
        final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint glowPaint = null;

        Stroke(int color, float widthPx, BrushMode mode, float glowScale, float glowAlpha, float glowRadiusPx) {
            corePaint.setStyle(Paint.Style.STROKE);
            corePaint.setStrokeJoin(Paint.Join.ROUND);
            corePaint.setStrokeCap(Paint.Cap.ROUND);
            corePaint.setStrokeWidth(widthPx);

            if (mode == BrushMode.ERASER) {
                corePaint.setColor(Color.TRANSPARENT);
                corePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                glowPaint = null;
            } else {
                corePaint.setColor(color);

                if (mode == BrushMode.GLOW) {
                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeJoin(Paint.Join.ROUND);
                    p.setStrokeCap(Paint.Cap.ROUND);

                    int a = Color.alpha(color);
                    int glowA = Math.round(a * glowAlpha);
                    int glowColor = (glowA << 24) | (color & 0x00FFFFFF);
                    p.setColor(glowColor);
                    p.setStrokeWidth(widthPx * glowScale);
                    p.setMaskFilter(new BlurMaskFilter(glowRadiusPx, BlurMaskFilter.Blur.NORMAL));
                    glowPaint = p;
                } else {
                    glowPaint = null;
                }
            }
        }
    }

    private final List<Stroke> strokes = new ArrayList<>();
    private Stroke current;
    private boolean drawingEnabled = true;
    private float lastX, lastY;
    private static final float TOUCH_TOLERANCE = 2f;
    private int brushColor = 0xFF000000;
    private float brushWidthPx = 8f;
    private BrushMode brushMode = BrushMode.NORMAL;
    private float glowScale = 1.8f;
    private float glowAlpha = 0.5f;
    private float glowRadiusPx = 12f;
    private Rect clipRectPx;

    public BrushOverlayView(Context ctx) {
        super(ctx);
        init();
    }

    public BrushOverlayView(Context ctx, AttributeSet a) {
        super(ctx, a);
        init();
    }

    public BrushOverlayView(Context ctx, AttributeSet a, int s) {
        super(ctx, a, s);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setBrush(int argbColor, int widthPx) {
        this.brushColor = argbColor;
        this.brushWidthPx = Math.max(1, widthPx);
    }

    public void setBrush(int argbColor, int widthPx, BrushMode mode) {
        this.brushMode = mode != null ? mode : BrushMode.NORMAL;
        setBrush(argbColor, widthPx);
    }

    public void setGlowParams(float glowScale, float glowAlpha, float glowRadiusPx) {
        this.glowScale = glowScale;
        this.glowAlpha = glowAlpha;
        this.glowRadiusPx = glowRadiusPx;
    }

    public void setDrawingEnabled(boolean enabled) {
        this.drawingEnabled = enabled;
        if (!enabled) current = null;
    }

    public void setClipRect(Rect r) {
        if (clipRectPx == null) clipRectPx = new Rect();
        clipRectPx.set(r);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        int save = c.save();
        if (clipRectPx != null) {
            c.clipRect(clipRectPx);
        }
        for (Stroke s : strokes) {
            if (s.glowPaint != null) c.drawPath(s.path, s.glowPaint);
            c.drawPath(s.path, s.corePaint);
        }
        if (current != null) {
            if (current.glowPaint != null) c.drawPath(current.path, current.glowPaint);
            c.drawPath(current.path, current.corePaint);
        }
        c.restoreToCount(save);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!drawingEnabled) return false;

        float x = e.getX();
        float y = e.getY();

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                current = new Stroke(brushColor, brushWidthPx, brushMode, glowScale, glowAlpha, glowRadiusPx);
                current.path.moveTo(x, y);
                lastX = x;
                lastY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (current != null) {
                    float dx = Math.abs(x - lastX);
                    float dy = Math.abs(y - lastY);
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        current.path.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f);
                        lastX = x;
                        lastY = y;
                    }
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (current != null) {
                    current.path.lineTo(x, y);
                    strokes.add(current);
                    current = null;
                    invalidate();
                }
                return true;
        }
        return super.onTouchEvent(e);
    }

    public void clear() {
        strokes.clear();
        current = null;
        invalidate();
    }
}