package com.example.filter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CustomSeekbar extends View {
    private int max = 100;  //최대값
    private int min = -100; //최소값
    private int progress = 0;   //현재값
    private Paint backgroundPaint;  //seekbar 배경색
    private Paint progressPaint;    //움직인 만큼 채워질 색
    private Paint thumbPaint;   //조작 버튼
    private Paint progressText; //현재값 텍스트
    private int textOffset = 80; //seekbar와 텍스트 사이 간격
    private int textSize = 50; //텍스트 크기
    private int thumbRadius = 25;   //버튼 반지름 → 크기
    private int barStroke = 10; //seekbar 굵기

    private OnProgressChangeListener onProgressChangeListener;

    public interface OnProgressChangeListener {
        void onProgressChanged(CustomSeekbar customSeekbar, int progress);
    }

    public void setOnProgressChangeListener(OnProgressChangeListener listener) {
        this.onProgressChangeListener = listener;
    }

    public CustomSeekbar(Context context) {
        super(context);
        init();
    }

    public CustomSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#DEDEDE"));
        backgroundPaint.setStrokeWidth(barStroke);

        progressPaint = new Paint();
        progressPaint.setColor(Color.parseColor("#838383"));
        progressPaint.setStrokeWidth(barStroke);

        thumbPaint = new Paint();
        thumbPaint.setColor(Color.parseColor("#BDBDBD"));
        thumbPaint.setStyle(Paint.Style.FILL);

        progressText = new Paint();
        progressText.setColor(Color.BLACK);
        progressText.setTextSize(textSize);
        progressText.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerY = getHeight() - 25;
        int centerX = getWidth() / 2;

        canvas.drawLine(thumbRadius, centerY, getWidth() - thumbRadius, centerY, backgroundPaint);

        float ratio = (float) (progress - min) / (max - min);
        int thumbX = (int) (thumbRadius + ratio * (getWidth() - 2 * thumbRadius));

        if (min == 0) {
            canvas.drawLine(0, centerY, thumbX, centerY, progressPaint);
        } else {
            float zeroRatio = (float) (0 - min) / (max - min);
            int zeroX = (int) (thumbRadius + zeroRatio * (getWidth() - 2 * thumbRadius));

            if (progress >= 0) {
                canvas.drawLine(zeroX, centerY, thumbX, centerY, progressPaint);
            } else {
                canvas.drawLine(thumbX, centerY, zeroX, centerY, progressPaint);
            }
        }
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbPaint);
        canvas.drawText(String.valueOf(progress), centerX, centerY - textOffset, progressText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            int width = getWidth() - 2 * thumbRadius;
            float ratio = Math.max(0, Math.min(1, (x - thumbRadius) / width));
            int newProgress = (int) (min + ratio * (max - min));
            if (newProgress != this.progress) {
                this.progress = newProgress;
                invalidate();
                if (onProgressChangeListener != null) {
                    onProgressChangeListener.onProgressChanged(this, this.progress);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        int clampedProgress = Math.max(min, Math.min(max, progress));
        if (clampedProgress != this.progress) {
            this.progress = clampedProgress;
            invalidate();
        }
    }

    public void setMinZero(String filterType) {
        if (filterType == "선명하게") {
            this.min = 0;
            if (this.progress < 0) {
                this.progress = 0;
            }
        } else this.min = -100;

        this.progress = Math.max(min, Math.min(max, progress));
        invalidate();
    }
}