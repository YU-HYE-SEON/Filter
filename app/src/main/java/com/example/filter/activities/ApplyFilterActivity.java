package com.example.filter.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.etc.FilterDtoCreateRequest;
import com.example.filter.etc.ImageUtils;

import java.util.List;

public class ApplyFilterActivity extends BaseActivity {
    private ImageButton backBtn;
    private ConstraintLayout bottomArea;
    private ImageView toArchiveBtn, toReviewBtn;

    private GLSurfaceView glSurfaceView;
    private FGLRenderer renderer;
    private FilterDtoCreateRequest.ColorAdjustments adj;
    private List<FilterDtoCreateRequest.Sticker> stickerList;
    private Bitmap finalBitmapWithStickers = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_apply_photo);
        backBtn = findViewById(R.id.backBtn);
        bottomArea = findViewById(R.id.bottomArea);
        toArchiveBtn = findViewById(R.id.toArchiveBtn);
        toReviewBtn = findViewById(R.id.toReviewBtn);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        renderer = new FGLRenderer(this, glSurfaceView);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        FrameLayout photoContainer = findViewById(R.id.photoContainer);
        photoContainer.addView(glSurfaceView, 0);

        View photoMask = findViewById(R.id.photoMask);

        photoContainer.post(() -> {
            photoMask.post(() -> updatePhotoMask(photoMask));
        });

        //디바이스 사진 저장
        renderer.setOnBitmapCaptureListener(baseBitmap -> {
            if (baseBitmap != null) {
                Bitmap finalBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(finalBitmap);

                if (stickerList != null && !stickerList.isEmpty()) {
                    int photoW = canvas.getWidth();
                    int photoH = canvas.getHeight();
                    float photoCenterX = photoW / 2f;
                    float photoCenterY = photoH / 2f;

                    Log.d("ApplyFilter", "사진 크기: " + photoW + "x" + photoH + ", 스티커 " + stickerList.size() + "개 그리기 시작");

                    for (FilterDtoCreateRequest.Sticker s : stickerList) {
                        drawStickerOnCanvas(canvas, s, photoW, photoH, photoCenterX, photoCenterY);
                    }
                }

                finalBitmapWithStickers = finalBitmap;

                //ImageUtils.saveBitmapToGallery(ApplyFilterActivity.this, finalBitmapWithStickers);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(bottomArea, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = 0;
            v.setLayoutParams(lp);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return insets;
        });

        Uri imageUri = getIntent().getData();
        if (imageUri != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                if (bitmap != null) {
                    renderer.setBitmap(bitmap);
                    glSurfaceView.requestRender();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        adj = (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
        stickerList = (List<FilterDtoCreateRequest.Sticker>) getIntent().getSerializableExtra("stickers");

        if (adj != null) {
            applyAdjustments(adj);
        } else if (stickerList != null && !stickerList.isEmpty()) {
            glSurfaceView.postDelayed(() -> renderer.captureBitmap(), 100);
        }

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            finish();
        });

        toArchiveBtn.setOnClickListener(v -> {

        });

        toReviewBtn.setOnClickListener(v -> {

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void updatePhotoMask(View mask) {
        int x = renderer.getViewportX();
        int y = renderer.getViewportY();
        int w = renderer.getViewportWidth();
        int h = renderer.getViewportHeight();

        mask.setBackground(new Drawable() {
            @Override
            public void draw(Canvas canvas) {
                Paint p = new Paint();
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.FILL);

                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), p);

                p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                canvas.drawRect(x, y, x + w, y + h, p);
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        });

        mask.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void applyAdjustments(FilterDtoCreateRequest.ColorAdjustments a) {
        Log.d("ColorAdjustments",
                "[적용화면]\n" +
                        "밝기: " + adj.brightness + " 노출: " + adj.exposure +
                        " 대비: " + adj.contrast + " 하이라이트: " + adj.highlight +
                        " 그림자: " + adj.shadow + " 온도: " + adj.temperature +
                        " 색조: " + adj.hue + " 채도: " + adj.saturation +
                        " 선명하게: " + adj.sharpen + " 흐리게: " + adj.blur +
                        " 비네트: " + adj.vignette + " 노이즈: " + adj.noise);

        renderer.updateValue("밝기", a.brightness * 100f);
        renderer.updateValue("노출", a.exposure * 100f);
        renderer.updateValue("대비", a.contrast * 100f);
        renderer.updateValue("하이라이트", a.highlight * 100f);
        renderer.updateValue("그림자", a.shadow * 100f);
        renderer.updateValue("온도", a.temperature * 100f);
        renderer.updateValue("색조", a.hue * 100f);
        renderer.updateValue("채도", (a.saturation - 1.0f) * 100f);
        renderer.updateValue("선명하게", a.sharpen * 100f);
        renderer.updateValue("흐리게", a.blur * 100f);
        renderer.updateValue("비네트", a.vignette * 100f);
        renderer.updateValue("노이즈", a.noise * 100f);

        glSurfaceView.requestRender();
        glSurfaceView.postDelayed(() -> renderer.captureBitmap(), 100);
    }

    private void drawStickerOnCanvas(Canvas canvas, FilterDtoCreateRequest.Sticker sticker,
                                     int photoW, int photoH,
                                     float photoCenterX, float photoCenterY) {
        if (sticker == null) return;

        String resName = "sticker_id_" + sticker.stickerId;
        int resId = getResources().getIdentifier(resName, "drawable", getPackageName());

        if (resId == 0) {
            Log.e("스티커테스트", "스티커 리소스를 찾을 수 없습니다: ID=" + sticker.stickerId + ", 이름=" + resName);
            return;
        }
        Bitmap stickerBmp = BitmapFactory.decodeResource(getResources(), resId);
        if (stickerBmp == null) {
            Log.e("스티커테스트", "스티커 비트맵 로드 실패: ID=" + sticker.stickerId);
            return;
        }

        int targetW = (int) (sticker.scale * photoW);

        float originalAspect = (float) stickerBmp.getHeight() / stickerBmp.getWidth();
        int targetH = (int) (targetW * originalAspect);

        float targetCenterX = photoCenterX + (sticker.x * (photoW / 2f));
        float targetCenterY = photoCenterY + (sticker.y * (photoH / 2f));

        Matrix matrix = new Matrix();

        matrix.postScale((float) targetW / stickerBmp.getWidth(),
                (float) targetH / stickerBmp.getHeight());

        matrix.postRotate(sticker.rotation, targetW / 2f, targetH / 2f);

        matrix.postTranslate(targetCenterX - (targetW / 2f),
                targetCenterY - (targetH / 2f));

        canvas.drawBitmap(stickerBmp, matrix, null);
    }
}
