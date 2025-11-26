package com.example.filter.overlayviews;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LassoOverlayView extends View {
    public interface LassoListener {
        void onShapeCompleted();
    }

    private LassoListener listener = null;
    private boolean drawingEnabled = true;

    public void setLassoListener(LassoListener l) {
        this.listener = l;
    }

    public void setDrawingEnabled(boolean enabled) {
        this.drawingEnabled = enabled;
    }

    public boolean isDrawingEnabled() {
        return drawingEnabled;
    }

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final PointF startPt = new PointF();
    private final List<Path> shapes = new ArrayList<>();
    private boolean drawing = false;
    private RectF imageBounds;
    private float dp, strokeWidthPx;
    private float closeThresholdPx;
    private float minCloseLengthPx;
    private float prevX, prevY;
    private float currX, currY;
    private float pathLength;
    private boolean nearStart = false;
    private boolean useDots = true;
    private boolean forceDim = false;
    private boolean fullSelectionShown = false;

    private static final float EPS = 0.5f;
    private boolean inverseDim = false;

    private RectF getVisibleImageRect() {
        if (imageBounds == null) return null;
        RectF vis = new RectF(imageBounds);
        boolean has = vis.intersect(0f, 0f, getWidth(), getHeight());
        return has ? vis : null;
    }

    private boolean isFullRectSelection() {
        RectF target = getVisibleImageRect();
        if (!fullSelectionShown || shapes.size() != 1 || target == null) return false;

        Path p = shapes.get(0);
        RectF r = new RectF();
        boolean isRect = p.isRect(r);
        if (!isRect) return false;

        return Math.abs(r.left - target.left) <= EPS &&
                Math.abs(r.top - target.top) <= EPS &&
                Math.abs(r.right - target.right) <= EPS &&
                Math.abs(r.bottom - target.bottom) <= EPS;
    }

    public LassoOverlayView(Context c) {
        super(c);
        init();
    }

    public LassoOverlayView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        init();
    }

    public LassoOverlayView(Context c, @Nullable AttributeSet a, int d) {
        super(c, a, d);
        init();
    }

    private void init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        } else {
            setLayerType(LAYER_TYPE_HARDWARE, null);
        }

        dp = getResources().getDisplayMetrics().density;

        strokeWidthPx = 2.0f * dp;
        closeThresholdPx = 18f * dp;
        minCloseLengthPx = 20f * dp;

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidthPx);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        updateDashEffect();

        dimPaint.setColor(0x99000000);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    private void updateDashEffect() {
        if (useDots) {
            Path dot = new Path();
            dot.addCircle(0, 0, strokeWidthPx * 0.55f, Path.Direction.CW);
            PathEffect pe = new PathDashPathEffect(
                    dot,
                    strokeWidthPx * 2.1f,
                    0f,
                    PathDashPathEffect.Style.ROTATE);
            strokePaint.setPathEffect(pe);
        } else {
            float on = 8f * dp, off = 6f * dp;
            strokePaint.setPathEffect(new DashPathEffect(new float[]{on, off}, 0f));
        }
    }

    public void setInverseDim(boolean inverse) {
        this.inverseDim = inverse;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (forceDim && imageBounds != null) {
            int id = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);

            if (!inverseDim) {
                canvas.drawRect(imageBounds, dimPaint);

                if (!shapes.isEmpty()) {
                    for (Path s : shapes) {
                        Path fill = new Path(s);
                        fill.setFillType(Path.FillType.WINDING);
                        canvas.drawPath(fill, clearPaint);
                    }
                }
            } else {
                if (!shapes.isEmpty()) {
                    for (Path s : shapes) {
                        Path fill = new Path(s);
                        fill.setFillType(Path.FillType.WINDING);
                        canvas.drawPath(fill, dimPaint);
                    }
                }
            }
            canvas.restoreToCount(id);
        }

        if (!shapes.isEmpty()) {
            boolean drawWithoutClip = isFullRectSelection();

            int save = 0;
            if (!drawWithoutClip) {
                save = clipOutAllShapes(canvas);
            }

            for (Path s : shapes) {
                canvas.drawPath(s, strokePaint);
            }

            if (!drawWithoutClip) {
                canvas.restoreToCount(save);
            }
        }

        if (!path.isEmpty()) {
            canvas.drawPath(path, strokePaint);

            if (nearStart) {
                Path connect = new Path();
                connect.moveTo(currX, currY);
                connect.lineTo(startPt.x, startPt.y);
                canvas.drawPath(connect, strokePaint);
            }
        }
    }

    private int clipOutAllShapes(Canvas canvas) {
        int save = canvas.save();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (Path p : shapes) canvas.clipOutPath(p);
        } else {
            for (Path p : shapes) canvas.clipPath(p, Region.Op.DIFFERENCE);
        }
        return save;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!drawingEnabled) return false;
        if (e.getPointerCount() > 1) return false;

        final float x = clampX(e.getX());
        final float y = clampY(e.getY());

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!isInsideImage(x, y)) return false;
                getParent().requestDisallowInterceptTouchEvent(true);

                if (fullSelectionShown) {
                    shapes.clear();
                    fullSelectionShown = false;
                }

                path.reset();
                path.moveTo(x, y);
                startPt.set(x, y);

                prevX = x;
                prevY = y;
                currX = x;
                currY = y;
                pathLength = 0f;
                nearStart = false;
                drawing = true;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!drawing) return false;

                path.lineTo(x, y);
                currX = x;
                currY = y;

                float dx = x - prevX, dy = y - prevY;
                pathLength += (float) Math.hypot(dx, dy);
                prevX = x;
                prevY = y;

                nearStart = (pathLength >= minCloseLengthPx)
                        && (dist(x, y, startPt.x, startPt.y) <= closeThresholdPx);

                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);

                if (pathLength < minCloseLengthPx) {
                    cancelCurrent();
                    invalidate();
                    return true;
                }

                if (!nearStart) {
                    path.lineTo(startPt.x, startPt.y);
                }

                path.close();

                Path done = new Path(path);
                done.setFillType(Path.FillType.WINDING);
                shapes.add(done);

                cancelCurrent();

                if (listener != null) listener.onShapeCompleted();

                invalidate();
                return true;
        }
        return false;
    }

    private void cancelCurrent() {
        path.reset();
        drawing = false;
        nearStart = false;
        pathLength = 0f;
    }

    private boolean isInsideImage(float x, float y) {
        return imageBounds == null || imageBounds.contains(x, y);
    }

    private float clampX(float x) {
        if (imageBounds == null) return Math.max(0, Math.min(x, getWidth()));
        return Math.max(imageBounds.left, Math.min(x, imageBounds.right));
    }

    private float clampY(float y) {
        if (imageBounds == null) return Math.max(0, Math.min(y, getHeight()));
        return Math.max(imageBounds.top, Math.min(y, imageBounds.bottom));
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.hypot(dx, dy);
    }

    public void setImageBounds(RectF rect) {
        imageBounds = rect != null ? new RectF(rect) : null;
        invalidate();
    }

    public void beginFullSelection() {
        RectF vis = getVisibleImageRect();
        if (vis == null) return;

        shapes.clear();
        Path rect = new Path();
        rect.addRect(vis, Path.Direction.CW);
        rect.setFillType(Path.FillType.WINDING);
        shapes.add(rect);

        fullSelectionShown = true;
        forceDim = true;
        invalidate();
    }

    public void setLassoVisible(boolean visible) {
        setVisibility(visible ? VISIBLE : GONE);
        if (!visible) {
            clearAll();
            forceDim = false;
            fullSelectionShown = false;
        } else {
            forceDim = true;
            invalidate();
        }
    }

    public List<Path> getShapes() {
        ArrayList<Path> copy = new ArrayList<>(shapes.size());
        for (Path p : shapes) copy.add(new Path(p));
        return copy;
    }

    public void clearAll() {
        path.reset();
        drawing = false;
        nearStart = false;
        pathLength = 0f;
        shapes.clear();
        invalidate();
    }
}