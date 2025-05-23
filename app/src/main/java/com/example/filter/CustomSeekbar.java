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
    private int texSitze = 50; //텍스트 크기
    private int thumbRadius = 25;   //버튼 반지름 → 크기
    private int barStroke = 10; //seekbar 굵기

    //생성자
    public CustomSeekbar(Context context) {
        super(context);
        init();
    }

    public CustomSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    //처음 시작 시 한번만 실행, 초기화
    private void init() {
        //배경색 (옅게) 설정
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#DEDEDE"));
        backgroundPaint.setStrokeWidth(barStroke);

        //움직인 만큼 채워질 색 (진하게) 설정
        progressPaint = new Paint();
        progressPaint.setColor(Color.parseColor("#838383"));
        progressPaint.setStrokeWidth(barStroke);

        //조작 버튼 설정
        thumbPaint = new Paint();
        thumbPaint.setColor(Color.parseColor("#BDBDBD"));
        thumbPaint.setStyle(Paint.Style.FILL);

        //현재값 텍스트 설정
        progressText = new Paint();
        progressText.setColor(Color.BLACK);
        progressText.setTextSize(texSitze);
        progressText.setTextAlign(Paint.Align.CENTER);
    }

    //seekbar 그리기
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //seekbar 위치
        int centerY = getHeight() -25;
        int centerX = getWidth() / 2;

        //seekbar 배경색 그리기
        canvas.drawLine(thumbRadius, centerY, getWidth() - thumbRadius, centerY, backgroundPaint);

        //seekbar 조작 버튼 움직인 만큼 위치 이동
        //ex.현재값이 -50이면 (-50-(-100))/(100-(-100))=50/200 (4분의 1 지점)
        float ratio = (float) (progress - min) / (max - min);
        int thumbX = (int) (thumbRadius + ratio * (getWidth() - 2 * thumbRadius));

        //현재값이 음수면 왼쪽 이동 → 조작 버튼 기준 왼쪽 색상 변경, 양수면 오른쪽 이동 → 조작 버튼 기준 오른쪽 색상 변경
        if (progress < 0) {
            canvas.drawLine(thumbX, centerY, centerX, centerY, progressPaint);
        } else {
            canvas.drawLine(centerX, centerY, thumbX, centerY, progressPaint);
        }

        //조작 버튼 그리기
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbPaint);

        //현재값 텍스트 그리기
        canvas.drawText(String.valueOf(progress), centerX, centerY-textOffset, progressText);
    }

    //버튼 조작 시 이동하게 해주는 메서드
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            int width = getWidth() - 2 * thumbRadius;
            float ratio = Math.max(0, Math.min(1, (x - thumbRadius) / width));
            progress = (int) (min + ratio * (max - min));
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    //현재 값 불러오기
    public int getProgress() {
        return progress;
    }

    //현재값 설정하기
    public void setProgress(int progress) {
        this.progress = Math.max(min, Math.min(max, progress));
        invalidate();
    }
}
