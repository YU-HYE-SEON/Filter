package com.example.filter.etc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import com.example.filter.R;

public class CustomSeekbar extends View {
    private int max = 100;  //최대값
    private int min = -100; //최소값
    private int progress = 0;   //현재값
    private Paint backgroundPaint;  //seekbar 배경색
    private Paint progressPaint;    //움직인 만큼 채워질 색
    private Paint thumbPaint;   //조작 버튼
    private Paint progressText; //현재값 텍스트
    private int textOffset = dp(40); //seekbar와 텍스트 사이 간격
    private int thumbRadius = dp(11);   //버튼 반지름 → 크기
    private int barStroke = dp(3); //seekbar 굵기
    private static final int START_COLOR = Color.parseColor("#007BFF"); //파랑
    private static final int END_COLOR = Color.parseColor("#C2FA7A"); //초록

    public interface OnProgressChangeListener {

        void onProgressChanged(CustomSeekbar customSeekbar, int progress);
    }

    private OnProgressChangeListener onProgressChangeListener;

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
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStrokeWidth(barStroke);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.parseColor("#6B6B6B"));
        progressPaint.setStrokeWidth(barStroke);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);

        progressText = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressText.setTypeface(ResourcesCompat.getFont(getContext(), R.font.roboto_bold));
        progressText.setColor(Color.parseColor("#6B6B6B"));
        progressText.setTextSize(dp(18));
        progressText.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int centerY = getHeight() - dp(60);
        int centerX = getWidth() / 2;
        float ratio = (float) (progress - min) / (max - min);
        ratio = Math.max(0f, Math.min(1f, ratio));
        int thumbX = (int) (thumbRadius + ratio * (getWidth() - 2 * thumbRadius));

        LinearGradient gradient = new LinearGradient(
                thumbRadius, 0, getWidth() - thumbRadius, 0,
                START_COLOR, END_COLOR, Shader.TileMode.CLAMP);
        backgroundPaint.setShader(gradient);
        int currentColor = interpolateColor(START_COLOR, END_COLOR, ratio);
        thumbPaint.setColor(currentColor);
        progressText.setColor(currentColor);

        canvas.drawLine(thumbRadius, centerY, getWidth() - thumbRadius, centerY, backgroundPaint);
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbPaint);
        canvas.drawText(String.valueOf(progress), centerX, centerY - textOffset, progressText);
    }

    //ARGB 보간
    private int interpolateColor(int start, int end, float t) {
        int sa = (start >> 24) & 0xFF, sr = (start >> 16) & 0xFF, sg = (start >> 8) & 0xFF, sb = start & 0xFF;
        int ea = (end >> 24) & 0xFF, er = (end >> 16) & 0xFF, eg = (end >> 8) & 0xFF, eb = end & 0xFF;

        int a = (int) (sa + (ea - sa) * t);
        int r = (int) (sr + (er - sr) * t);
        int g = (int) (sg + (eg - sg) * t);
        int b = (int) (sb + (eb - sb) * t);
        return Color.argb(a, r, g, b);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float centerY = getHeight() - dp(60);
        float touchX = event.getX();
        float touchY = event.getY();
        float thumbTouchPadding = thumbRadius * 3.5f;
        if (Math.abs(touchY - centerY) > thumbTouchPadding) return false;

        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
            int width = getWidth() - 2 * thumbRadius;
            float ratio = Math.max(0, Math.min(1, (touchX - thumbRadius) / width));
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
        if ("선명하게".equals(filterType) || "흐리게".equals(filterType) ||
                "비네트".equals(filterType) || "노이즈".equals(filterType)) {
            this.min = 0;
            if (this.progress < 0) {
                this.progress = 0;
            }
        } else this.min = -100;

        this.progress = Math.max(min, Math.min(max, progress));
        invalidate();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}