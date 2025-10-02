package com.example.filter.etc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;

import com.example.filter.R;

import java.util.ArrayList;
import java.util.List;

public class BrushOverlayView extends View {
    public interface OnStrokeProgressListener {
        void onStrokeProgress(Path deltaPath, float strokeWidthPx, BrushMode mode);

        void onStrokeEnd(Path fullPath, float strokeWidthPx, BrushMode mode);
    }

    private OnStrokeProgressListener progressListener;

    public void setOnStrokeProgressListener(OnStrokeProgressListener l) {
        this.progressListener = l;
    }

    public enum BrushMode {PEN, GLOW, CRAYON, ERASER}

    private class Stroke {
        final Path path = new Path();
        final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint glowPaint = null;

        Stroke(int color, float widthPx, BrushMode mode,
               float glowScale, float glowAlpha, float glowRadiusPx) {
            corePaint.setStyle(Paint.Style.STROKE);
            corePaint.setStrokeJoin(Paint.Join.ROUND);
            corePaint.setStrokeCap(Paint.Cap.ROUND);
            corePaint.setStrokeWidth(widthPx);

            if (mode == BrushMode.ERASER) {
                corePaint.setColor(Color.TRANSPARENT);
                corePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                return;
            }
            if (mode == BrushMode.GLOW) {
                corePaint.setColor(color);
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
                return;
            }
            if (mode == BrushMode.CRAYON) {
                loadCrayonShaderAlphaMask();
                if (crayonShader != null) {
                    corePaint.setShader(crayonShader);
                    int opaqueColor = (color | 0xFF000000);
                    corePaint.setColorFilter(new PorterDuffColorFilter(opaqueColor, PorterDuff.Mode.SRC_ATOP));
                    corePaint.setAlpha(Color.alpha(color));
                } else {
                    corePaint.setColor(color);
                }
                glowPaint = null;
                return;
            }
            corePaint.setColor(color);
        }
    }

    private final List<Stroke> strokes = new ArrayList<>();
    private final List<Stroke> undone = new ArrayList<>();
    private Stroke current;
    private Rect clipRectPx;
    private boolean drawingEnabled = true;
    private float lastX, lastY;
    private static final float TOUCH_TOLERANCE = 2f;
    private int brushColor = 0xFF000000;
    private float brushWidthPx = 8f;
    private BrushMode brushMode = BrushMode.PEN;
    private float glowScale = 1.8f;
    private float glowAlpha = 0.5f;
    private float glowRadiusPx = 12f;
    private BitmapShader crayonShader;
    private int visibleCount = 0;

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
        visibleCount = 0;
    }

    public void setBrush(int argbColor, int widthPx) {
        this.brushColor = argbColor;
        this.brushWidthPx = Math.max(1, widthPx);
    }

    public void setBrush(int argbColor, int widthPx, BrushMode mode) {
        this.brushMode = mode != null ? mode : BrushMode.PEN;
        setBrush(argbColor, widthPx);
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

    public Bitmap renderStrokes(int from, int to) {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        if (from < 0) from = 0;
        if (to > strokes.size()) to = strokes.size();
        if (from >= to) return null;

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        int save = c.save();
        if (clipRectPx != null) c.clipRect(clipRectPx);
        for (int i = from; i < to; i++) {
            Stroke s = strokes.get(i);
            if (s.glowPaint != null) c.drawPath(s.path, s.glowPaint);
            c.drawPath(s.path, s.corePaint);
        }
        c.restoreToCount(save);
        return out;
    }

    private void loadCrayonShaderAlphaMask() {
        if (crayonShader != null) return;

        Drawable d = AppCompatResources.getDrawable(getContext(), R.drawable.texture_crayon);

        if (d == null) return;

        Bitmap bmp;
        if (d instanceof BitmapDrawable) {
            bmp = ((BitmapDrawable) d).getBitmap();
        } else {
            int w = Math.max(2, d.getIntrinsicWidth());
            int h = Math.max(2, d.getIntrinsicHeight());
            if (w <= 0) w = 256;
            if (h <= 0) h = 256;
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, w, h);
            d.draw(c);
        }

        int w = bmp.getWidth(), h = bmp.getHeight();
        int[] in = new int[w * h];
        int[] out = new int[w * h];
        bmp.getPixels(in, 0, w, 0, 0, w, h);

        for (int i = 0; i < in.length; i++) {
            int c = in[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;

            int luminance = (int) (0.299f * r + 0.587f * g + 0.114f * b);
            //int alphaStrength = 255;
            int alpha = (255 - luminance);
            //alpha = (alpha * alphaStrength) / 255;
            if (alpha < 0) alpha = 0;
            if (alpha > 255) alpha = 255;

            out[i] = (alpha << 24) | 0x00FFFFFF;
        }

        Bitmap mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mask.setPixels(out, 0, w, 0, 0, w, h);

        crayonShader = new BitmapShader(mask, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        int save = c.save();
        if (clipRectPx != null) {
            c.clipRect(clipRectPx);
        }
        int n = Math.min(visibleCount, strokes.size());
        for (int i = 0; i < n; i++) {
            Stroke s = strokes.get(i);
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
                if (visibleCount < strokes.size()) {
                    strokes.subList(visibleCount, strokes.size()).clear();
                }
                undone.clear();

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
                        float mx = (x + lastX) / 2f;
                        float my = (y + lastY) / 2f;
                        current.path.quadTo(lastX, lastY, mx, my);

                        if (progressListener != null) {
                            Path delta = new Path();
                            delta.moveTo(lastX, lastY);
                            delta.quadTo(lastX, lastY, mx, my);
                            progressListener.onStrokeProgress(delta, brushWidthPx, brushMode);
                        }

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

                    if (progressListener != null) {
                        Path copy = new Path(current.path);
                        progressListener.onStrokeEnd(copy, brushWidthPx, brushMode);
                    }

                    current = null;
                    visibleCount = strokes.size();
                    invalidate();
                }
                return true;
        }
        return super.onTouchEvent(e);
    }

    public int getStrokeCount() {
        return strokes.size();
    }

    public int getVisibleStrokeCount() {
        return Math.min(visibleCount, strokes.size());
    }

    public boolean hasEffectiveContent() {
        return (current != null) || (getVisibleStrokeCount() > 0);
    }

    public void setVisibleStrokeCount(int count) {
        int clamped = Math.max(0, Math.min(count, strokes.size()));
        if (clamped != visibleCount) {
            visibleCount = clamped;
            invalidate();
        }
    }

    public void trimToCount(int count) {
        int safe = Math.max(0, Math.min(count, strokes.size()));
        if (safe < strokes.size()) {
            strokes.subList(safe, strokes.size()).clear();
            undone.clear();
            current = null;
            visibleCount = Math.min(visibleCount, strokes.size());
            invalidate();
        }
    }

    public void clear() {
        strokes.clear();
        undone.clear();
        current = null;
        visibleCount = 0;
        invalidate();
    }
}