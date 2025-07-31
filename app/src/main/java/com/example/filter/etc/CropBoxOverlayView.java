package com.example.filter.etc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class CropBoxOverlayView extends View {
    private Rect cropRect;  //크롭 영역
    private boolean fixedAspectRatio = false;   //크롭 영역 비율 유지할지 말지
    private float aspectRatioX = 0, aspectRatioY = 0;
    private Paint borderPaint, shadePaint;  //크롭 영역 테두리, 크롭 영역 밖 (어둡게)
    private int viewportWidth = 0, viewportHeight = 0;
    //private static final int HANDLE_SIZE = 60;
    private static final int MIN_SIZE = 150;    //크롭 영역 최소 사이즈 설정

    private enum Mode {NONE, RESIZE}  //사이즈 조절x 모드 or 조절o 모드

    private Mode currentMode = Mode.NONE;

    private enum HandleType {   //사이즈 조절 시 터치한 테두리 -> 터치x/위/아래/왼/오/왼+위/오+위/왼+아래/오+아래
        NONE,
        TOP, BOTTOM, LEFT, RIGHT,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private HandleType currentHandle = HandleType.NONE;
    private static final int HANDLE_TOUCH_AREA = 80;    //모서리 핸들 터치인지 꼭지점 핸들 터치인지를 인식할 범위 설정
    private float lastX, lastY;
    private int viewportX = 0, viewportY = 0;
    //private boolean resizingFromCorner = false;   //코너 드래그 여부
    //private ScaleGestureDetector scaleDetector; //사진 확대/축소 (두 손가락) 감지

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

    //크롭 박스 초기 설정
    public void initializeCropBox(int viewportX, int viewportY, int viewportWidth, int viewportHeight, boolean fullSize) {
        this.viewportX = viewportX;
        this.viewportY = viewportY;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;

        int centerX = viewportX + viewportWidth / 2;
        int centerY = viewportY + viewportHeight / 2;

        //고정비율모드(1:1/3:4/9:16)일 때 사이즈 조절 시에도 크롭 영역 비율 유지되게
        if (fixedAspectRatio && aspectRatioX > 0 && aspectRatioY > 0) {
            float targetRatio = aspectRatioX / aspectRatioY;
            int boxWidth = (int) (viewportWidth * 0.8f);
            int boxHeight = (int) (boxWidth / targetRatio);

            //크롭 영역 세로가 사진 세로길이보다 길면 세로기준으로 재설정
            if (boxHeight > viewportHeight) {
                boxHeight = (int) (viewportHeight * 0.8f);
                boxWidth = (int) (boxHeight * targetRatio);
            }

            //크롭 영역 초기 위치 중앙으로 설정
            cropRect = new Rect(
                    centerX - boxWidth / 2,
                    centerY - boxHeight / 2,
                    centerX + boxWidth / 2,
                    centerY + boxHeight / 2
            );
        } else if (fullSize) {  //고정비율모드 아니면 사진 전체 영역을 잡음
            cropRect = new Rect(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        }/* else {
            int boxWidth = viewportWidth / 2;
            int boxHeight = viewportHeight / 2;
            cropRect = new Rect(
                    centerX - boxWidth / 2,
                    centerY - boxHeight / 2,
                    centerX + boxWidth / 2,
                    centerY + boxHeight / 2
            );
        }*/

        invalidate();
    }

    //크롭박스 비율 설정
    public void setAspectRatio(int x, int y) {
        this.aspectRatioX = x;
        this.aspectRatioY = y;
        if (fixedAspectRatio && cropRect != null) {
            enforceAspectRatio();
            invalidate();
        }
    }

    //비율 고정 모드인지 아닌지
    public void setFixedAspectRatio(boolean fixed) {
        this.fixedAspectRatio = fixed;
    }

    /*public void setScaleGestureDetector(ScaleGestureDetector detector) {
        this.scaleDetector = detector;
    }*/

    //비율 고정 모드일 때 사이즈 조절해도 비율 유지되게
    private void enforceAspectRatio() {
        //비율값 0이면 (freeCut모드일 때) 메서드 실행x
        if (aspectRatioX == 0 || aspectRatioY == 0) return;

        float targetRatio = aspectRatioX / aspectRatioY;
        int width = (int) (viewportWidth * 0.8f);
        int height = (int) (width / targetRatio);

        if (height > viewportHeight) {
            height = (int) (viewportHeight * 0.8f);
            width = (int) (height * targetRatio);
        }

        int centerX = viewportWidth / 2;
        int centerY = viewportHeight / 2;

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

        //크롭 영역 밖 어둡게 그리기
        canvas.drawRect(0, 0, getWidth(), cropRect.top, shadePaint);
        canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), shadePaint);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, shadePaint);
        canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, shadePaint);

        //크롭 영역 테두리 하얗게 그리기
        canvas.drawRect(cropRect, borderPaint);
    }

    public Rect getCropRect() {
        return cropRect;
    }

    //크롭 영역 터치된 곳 판단 (위/아래/왼/오/왼+위/오+위/왼+아래/오+아래)
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

        //if (cropRect.contains((int) x, (int) y)) return HandleType.MOVE;

        return HandleType.NONE;
    }

    //(터치좌표 - 크롭박스좌표) <= 설정한 범위 -> true, 크롭영역 모서리나 꼭지점을 터치했다고 인식
    private boolean near(float a, int b) {
        return Math.abs(a - b) <= HANDLE_TOUCH_AREA;
    }

    //왼쪽 모서리 선택 후 사이즈 조절할 때 (고정비율모드x)
    private void resizeLeft(float dx) {
        int newLeft = cropRect.left + (int) dx;
        if (newLeft < viewportX) newLeft = viewportX;

        //설정한 최소 사이즈보다 작아지지 않게
        if (cropRect.right - newLeft >= MIN_SIZE)
            cropRect.left = newLeft;
    }

    //오른쪽 모서리 선택 후 사이즈 조절할 때 (고정비율모드x)
    private void resizeRight(float dx) {
        int newRight = cropRect.right + (int) dx;
        if (newRight > viewportX + viewportWidth) newRight = viewportX + viewportWidth;

        if (newRight - cropRect.left >= MIN_SIZE)
            cropRect.right = newRight;
    }

    //위쪽 모서리 선택 후 사이즈 조절할 때 (고정비율모드x)
    private void resizeTop(float dy) {
        int newTop = cropRect.top + (int) dy;
        if (newTop < viewportY) newTop = viewportY;

        if (cropRect.bottom - newTop >= MIN_SIZE)
            cropRect.top = newTop;
    }

    //아래쪽 모서리 선택 후 사이즈 조절할 때 (고정비율모드x)
    private void resizeBottom(float dy) {
        int newBottom = cropRect.bottom + (int) dy;
        if (newBottom > viewportY + viewportHeight) newBottom = viewportY + viewportHeight;

        if (newBottom - cropRect.top >= MIN_SIZE)
            cropRect.bottom = newBottom;
    }

    //왼+위 꼭지점 선택 후 사이즈 조절할 때
    private void resizeTopLeft(float dx, float dy) {
        //비율 고정 모드일 때 비율 유지된 채로 크기 조절되게
        if (fixedAspectRatio && aspectRatioX > 0 && aspectRatioY > 0) {
            float ratio = aspectRatioX / aspectRatioY;

            float proposedWidth = cropRect.width() - dx;
            float proposedHeight = proposedWidth / ratio;

            //사진 안에서의 크롭 영역 최대 크기 설정
            int maxWidth = cropRect.right - viewportX;
            int maxHeight = cropRect.bottom - viewportY;

            float maxWidthByHeight = maxHeight * ratio;
            float maxHeightByWidth = maxWidth / ratio;

            //크롭 영역이 사진 밖을 벗어나지 못하도록
            if (proposedWidth > maxWidth || proposedHeight > maxHeight) {
                if (maxWidthByHeight <= maxWidth) {
                    proposedWidth = maxWidthByHeight;
                    proposedHeight = maxHeight;
                } else {
                    proposedWidth = maxWidth;
                    proposedHeight = maxHeightByWidth;
                }
            }

            //크롭 영역이 설정한 크롭 최소 사이즈보다 작아지지 않도록
            if (proposedWidth >= MIN_SIZE && proposedHeight >= MIN_SIZE) {
                cropRect.left = cropRect.right - (int) proposedWidth;
                cropRect.top = cropRect.bottom - (int) proposedHeight;
            }
        } else {    //freeCut모드일 땐 비율 유지x, 자유롭게 조절
            resizeTop(dy);
            resizeLeft(dx);
        }
    }

    //오+위 꼭지점 선택 후 사이즈 조절할 때
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

    //왼+아래 꼭지점 선택 후 사이즈 조절할 때
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

    //오+아래 꼭지점 선택 후 사이즈 조절할 때
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
            /*int newRight = cropRect.right + (int) dx;
            int newBottom = cropRect.bottom + (int) dy;

            if (newRight > cropRect.left + MIN_SIZE && newRight <= viewportX + viewportWidth)
                cropRect.right = newRight;
            if (newBottom > cropRect.top + MIN_SIZE && newBottom <= viewportY + viewportHeight)
                cropRect.bottom = newBottom;*/
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /*if (scaleDetector != null) {
            scaleDetector.onTouchEvent(event);  // scale 제스처 전달
        }*/

        if (cropRect == null) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            //크롭박스를 눌렀을 때 모서리 핸들을 터치했는지 꼭지점 핸들을 터치했는지 판별, RESIZE모드로 전환
            case MotionEvent.ACTION_DOWN:
                currentHandle = getTouchedHandle(x, y);
                if (currentHandle != HandleType.NONE) {
                    currentMode = Mode.RESIZE;
                    //currentMode = currentHandle == HandleType.MOVE ? Mode.MOVE : Mode.RESIZE;
                    lastX = x;
                    lastY = y;
                    return true;
                }
                break;

            //크롭박스 핸들 터치+이동, RESIZE모드, 크롭 영역 사이즈 조절
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;

                /*if (currentMode == Mode.MOVE) {
                    int offsetX = (int) dx;
                    int offsetY = (int) dy;

                    int newLeft = cropRect.left + offsetX;
                    int newTop = cropRect.top + offsetY;
                    int newRight = cropRect.right + offsetX;
                    int newBottom = cropRect.bottom + offsetY;

                    // 이미지 뷰포트 내부로 제한
                    if (newLeft < viewportX) offsetX -= (newLeft - viewportX);
                    if (newTop < viewportY) offsetY -= (newTop - viewportY);
                    if (newRight > viewportX + viewportWidth) offsetX -= (newRight - (viewportX + viewportWidth));
                    if (newBottom > viewportY + viewportHeight) offsetY -= (newBottom - (viewportY + viewportHeight));

                    cropRect.offset(offsetX, offsetY);
                    lastX = x;
                    lastY = y;
                    invalidate();
                    return true;
                }*/

                if (currentMode == Mode.RESIZE) {
                    switch (currentHandle) {
                        case LEFT:
                            if (!fixedAspectRatio) resizeLeft(dx);
                            break;
                        case RIGHT:
                            if (!fixedAspectRatio) resizeRight(dx);
                            break;
                        case TOP:
                            if (!fixedAspectRatio) resizeTop(dy);
                            break;
                        case BOTTOM:
                            if (!fixedAspectRatio) resizeBottom(dy);
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
                    lastX = x;
                    lastY = y;
                    invalidate();
                    return true;
                }
                break;

            //손 떼면 다시 NONE모드로
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                currentMode = Mode.NONE;
                break;
        }

        return super.onTouchEvent(event);
    }
}
