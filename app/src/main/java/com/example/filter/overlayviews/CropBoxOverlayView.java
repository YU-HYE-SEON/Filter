package com.example.filter.overlayviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.filter.R;

public class CropBoxOverlayView extends View {
    private Rect cropRect;
    private boolean fixedAspectRatio = false;
    private float aspectRatioX = 0, aspectRatioY = 0;
    private Paint borderPaint, shadePaint;
    private int viewportWidth = 0, viewportHeight = 0;
    private static final int MIN_SIZE = 150;

    private enum Mode {NONE, RESIZE}

    private Mode currentMode = Mode.NONE;

    private enum HandleType {
        NONE,
        TOP, BOTTOM, LEFT, RIGHT,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private HandleType currentHandle = HandleType.NONE;
    private static final int HANDLE_TOUCH_AREA = 80;
    private float lastX, lastY;
    private int viewportX = 0, viewportY = 0;

    public CropBoxOverlayView(Context context) {
        super(context);
        init();
    }

    public CropBoxOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(5);
        borderPaint.setStyle(Paint.Style.STROKE);

        shadePaint = new Paint();
        shadePaint.setColor(Color.parseColor("#88000000"));
        shadePaint.setStyle(Paint.Style.FILL);
    }

    public void initializeCropBox(int viewportX, int viewportY, int viewportWidth, int viewportHeight, boolean fullSize) {
        this.viewportX = viewportX;
        this.viewportY = viewportY;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;

        int centerX = viewportX + viewportWidth / 2;
        int centerY = viewportY + viewportHeight / 2;

        if (fixedAspectRatio && aspectRatioX > 0 && aspectRatioY > 0) {
            float targetRatio = aspectRatioX / aspectRatioY;
            int boxWidth = (int) viewportWidth;
            int boxHeight = (int) (boxWidth / targetRatio);

            if (boxHeight > viewportHeight) {
                boxHeight = (int) viewportHeight;
                boxWidth = (int) (boxHeight * targetRatio);
            }

            cropRect = new Rect(
                    centerX - boxWidth / 2,
                    centerY - boxHeight / 2,
                    centerX + boxWidth / 2,
                    centerY + boxHeight / 2
            );
        } else if (fullSize) {
            cropRect = new Rect(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        }

        invalidate();
    }

    public void setAspectRatio(int x, int y) {
        this.aspectRatioX = x;
        this.aspectRatioY = y;
        if (fixedAspectRatio && cropRect != null) {
            enforceAspectRatio();
            invalidate();
        }
    }

    public void setFixedAspectRatio(boolean fixed) {
        this.fixedAspectRatio = fixed;
    }

    private void enforceAspectRatio() {
        if (aspectRatioX == 0 || aspectRatioY == 0) return;

        float targetRatio = aspectRatioX / aspectRatioY;
        int width = (int) (viewportWidth * 0.8f);
        int height = (int) (width / targetRatio);

        if (height > viewportHeight) {
            height = (int) (viewportHeight * 0.8f);
            width = (int) (height * targetRatio);
        }

        int centerX = viewportX + viewportWidth / 2;
        int centerY = viewportY + viewportHeight / 2;

        cropRect = new Rect(
                centerX - width / 2,
                centerY - height / 2,
                centerX + width / 2,
                centerY + height / 2
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cropRect == null) return;

        canvas.drawRect(0, 0, getWidth(), cropRect.top, shadePaint);
        canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), shadePaint);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, shadePaint);
        canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, shadePaint);

        Drawable overlay = ContextCompat.getDrawable(getContext(), R.drawable.crop_box);
        if (overlay != null) {
            int expand = (int) (2 * getResources().getDisplayMetrics().density + 0.5f);
            overlay.setBounds(cropRect.left - expand, cropRect.top - expand, cropRect.right + expand, cropRect.bottom + expand);
            overlay.draw(canvas);
        } else {
            canvas.drawRect(cropRect, borderPaint);
        }
    }

    public Rect getCropRect() {
        return cropRect;
    }

    private HandleType getTouchedHandle(float x, float y) {
        int t = cropRect.top;
        int b = cropRect.bottom;
        int l = cropRect.left;
        int r = cropRect.right;

        if (near(x, l) && near(y, t)) return HandleType.TOP_LEFT;
        if (near(x, r) && near(y, t)) return HandleType.TOP_RIGHT;
        if (near(x, l) && near(y, b)) return HandleType.BOTTOM_LEFT;
        if (near(x, r) && near(y, b)) return HandleType.BOTTOM_RIGHT;

        if (near(x, l) && y > t && y < b) return HandleType.LEFT;
        if (near(x, r) && y > t && y < b) return HandleType.RIGHT;
        if (near(y, t) && x > l && x < r) return HandleType.TOP;
        if (near(y, b) && x > l && x < r) return HandleType.BOTTOM;

        return HandleType.NONE;
    }

    private boolean near(float a, int b) {
        return Math.abs(a - b) <= HANDLE_TOUCH_AREA;
    }

    private void resizeLeft(float dx) {
        int newLeft = cropRect.left + (int) dx;
        if (newLeft < viewportX) newLeft = viewportX;

        if (cropRect.right - newLeft >= MIN_SIZE)
            cropRect.left = newLeft;
    }

    private void resizeRight(float dx) {
        int newRight = cropRect.right + (int) dx;
        if (newRight > viewportX + viewportWidth) newRight = viewportX + viewportWidth;

        if (newRight - cropRect.left >= MIN_SIZE)
            cropRect.right = newRight;
    }

    private void resizeTop(float dy) {
        int newTop = cropRect.top + (int) dy;
        if (newTop < viewportY) newTop = viewportY;

        if (cropRect.bottom - newTop >= MIN_SIZE)
            cropRect.top = newTop;
    }

    private void resizeBottom(float dy) {
        int newBottom = cropRect.bottom + (int) dy;
        if (newBottom > viewportY + viewportHeight) newBottom = viewportY + viewportHeight;

        if (newBottom - cropRect.top >= MIN_SIZE)
            cropRect.bottom = newBottom;
    }

    private void resizeTopLeft(float dx, float dy) {
        if (fixedAspectRatio && aspectRatioX > 0 && aspectRatioY > 0) {
            float ratio = aspectRatioX / aspectRatioY;

            float proposedWidth = cropRect.width() - dx;
            float proposedHeight = proposedWidth / ratio;

            int maxWidth = cropRect.right - viewportX;
            int maxHeight = cropRect.bottom - viewportY;

            float maxWidthByHeight = maxHeight * ratio;
            float maxHeightByWidth = maxWidth / ratio;

            if (proposedWidth > maxWidth || proposedHeight > maxHeight) {
                if (maxWidthByHeight <= maxWidth) {
                    proposedWidth = maxWidthByHeight;
                    proposedHeight = maxHeight;
                } else {
                    proposedWidth = maxWidth;
                    proposedHeight = maxHeightByWidth;
                }
            }

            if (proposedWidth >= MIN_SIZE && proposedHeight >= MIN_SIZE) {
                cropRect.left = cropRect.right - (int) proposedWidth;
                cropRect.top = cropRect.bottom - (int) proposedHeight;
            }
        } else {
            resizeTop(dy);
            resizeLeft(dx);
        }
    }

    private void resizeTopRight(float dx, float dy) {
        if (fixedAspectRatio && aspectRatioX > 0 && aspectRatioY > 0) {
            float ratio = aspectRatioX / aspectRatioY;

            float proposedWidth = cropRect.width() + dx;
            float proposedHeight = proposedWidth / ratio;

            int maxWidth = (viewportX + viewportWidth) - cropRect.left;
            int maxHeight = cropRect.bottom - viewportY;

            float maxWidthByHeight = maxHeight * ratio;
            float maxHeightByWidth = maxWidth / ratio;

            if (proposedWidth > maxWidth || proposedHeight > maxHeight) {
                if (maxWidthByHeight <= maxWidth) {
                    proposedWidth = maxWidthByHeight;
                    proposedHeight = maxHeight;
                } else {
                    proposedWidth = maxWidth;
                    proposedHeight = maxHeightByWidth;
                }
            }

            if (proposedWidth >= MIN_SIZE && proposedHeight >= MIN_SIZE) {
                cropRect.right = cropRect.left + (int) proposedWidth;
                cropRect.top = cropRect.bottom - (int) proposedHeight;
            }
        } else {
            resizeTop(dy);
            resizeRight(dx);
        }
    }

    private void resizeBottomLeft(float dx, float dy) {
        if (fixedAspectRatio && aspectRatioX > 0 && aspectRatioY > 0) {
            float ratio = aspectRatioX / aspectRatioY;

            float proposedWidth = cropRect.width() - dx;
            float proposedHeight = proposedWidth / ratio;

            int maxWidth = cropRect.right - viewportX;
            int maxHeight = (viewportY + viewportHeight) - cropRect.top;

            float maxWidthByHeight = maxHeight * ratio;
            float maxHeightByWidth = maxWidth / ratio;

            if (proposedWidth > maxWidth || proposedHeight > maxHeight) {
                if (maxWidthByHeight <= maxWidth) {
                    proposedWidth = maxWidthByHeight;
                    proposedHeight = maxHeight;
                } else {
                    proposedWidth = maxWidth;
                    proposedHeight = maxHeightByWidth;
                }
            }

            if (proposedWidth >= MIN_SIZE && proposedHeight >= MIN_SIZE) {
                cropRect.left = cropRect.right - (int) proposedWidth;
                cropRect.bottom = cropRect.top + (int) proposedHeight;
            }
        } else {
            resizeLeft(dx);
            resizeBottom(dy);
        }
    }

    private void resizeBottomRight(float dx, float dy) {
        if (fixedAspectRatio && aspectRatioX > 0 && aspectRatioY > 0) {
            float ratio = aspectRatioX / aspectRatioY;

            float proposedWidth = cropRect.width() + dx;
            float proposedHeight = proposedWidth / ratio;

            int maxWidth = (viewportX + viewportWidth) - cropRect.left;
            int maxHeight = (viewportY + viewportHeight) - cropRect.top;

            float maxWidthByHeight = maxHeight * ratio;
            float maxHeightByWidth = maxWidth / ratio;

            if (proposedWidth > maxWidth || proposedHeight > maxHeight) {
                if (maxWidthByHeight <= maxWidth) {
                    proposedWidth = maxWidthByHeight;
                    proposedHeight = maxHeight;
                } else {
                    proposedWidth = maxWidth;
                    proposedHeight = maxHeightByWidth;
                }
            }

            if (proposedWidth >= MIN_SIZE && proposedHeight >= MIN_SIZE) {
                cropRect.right = cropRect.left + (int) proposedWidth;
                cropRect.bottom = cropRect.top + (int) proposedHeight;
            }
        } else {
            resizeRight(dx);
            resizeBottom(dy);
        }
    }

    private void moveCropRect(float dx, float dy) {
        if (cropRect == null) return;

        int newLeft = cropRect.left + (int) dx;
        int newTop = cropRect.top + (int) dy;
        int newRight = cropRect.right + (int) dx;
        int newBottom = cropRect.bottom + (int) dy;

        int offsetX = 0;
        int offsetY = 0;

        if (newLeft < viewportX) offsetX = viewportX - newLeft;
        if (newRight > viewportX + viewportWidth) offsetX = viewportX + viewportWidth - newRight;
        if (newTop < viewportY) offsetY = viewportY - newTop;
        if (newBottom > viewportY + viewportHeight)
            offsetY = viewportY + viewportHeight - newBottom;

        cropRect.offset((int) dx + offsetX, (int) dy + offsetY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cropRect == null) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentHandle = getTouchedHandle(x, y);
                if (currentHandle != HandleType.NONE) {
                    currentMode = Mode.RESIZE;
                    lastX = x;
                    lastY = y;
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;

                if (currentMode == Mode.RESIZE) {
                    if (fixedAspectRatio) {
                        switch (currentHandle) {
                            case LEFT:
                            case RIGHT:
                            case TOP:
                            case BOTTOM:
                                moveCropRect(dx, dy);
                                break;
                            case TOP_LEFT:
                                resizeTopLeft(dx, dy);
                                break;
                            case TOP_RIGHT:
                                resizeTopRight(dx, dy);
                                break;
                            case BOTTOM_LEFT:
                                resizeBottomLeft(dx, dy);
                                break;
                            case BOTTOM_RIGHT:
                                resizeBottomRight(dx, dy);
                                break;
                        }
                    } else {
                        switch (currentHandle) {
                            case LEFT:
                                resizeLeft(dx);
                                break;
                            case RIGHT:
                                resizeRight(dx);
                                break;
                            case TOP:
                                resizeTop(dy);
                                break;
                            case BOTTOM:
                                resizeBottom(dy);
                                break;
                            case TOP_LEFT:
                                resizeTopLeft(dx, dy);
                                break;
                            case TOP_RIGHT:
                                resizeTopRight(dx, dy);
                                break;
                            case BOTTOM_LEFT:
                                resizeBottomLeft(dx, dy);
                                break;
                            case BOTTOM_RIGHT:
                                resizeBottomRight(dx, dy);
                                break;
                        }
                    }
                    lastX = x;
                    lastY = y;
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                currentMode = Mode.NONE;
                break;
        }

        return super.onTouchEvent(event);
    }

    public void setViewportInfo(int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        this.viewportX = viewportX;
        this.viewportY = viewportY;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public void setCropRect(Rect rect) {
        int l = Math.max(viewportX, Math.min(rect.left, viewportX + viewportWidth));
        int t = Math.max(viewportY, Math.min(rect.top, viewportY + viewportHeight));
        int r = Math.max(l + MIN_SIZE, Math.min(rect.right, viewportX + viewportWidth));
        int b = Math.max(t + MIN_SIZE, Math.min(rect.bottom, viewportY + viewportHeight));
        this.cropRect = new Rect(l, t, r, b);
        invalidate();
    }
}