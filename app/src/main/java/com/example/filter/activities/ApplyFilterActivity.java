package com.example.filter.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.FGLRenderer;
import com.example.filter.apis.dto.FilterDtoCreateRequest;
import com.example.filter.etc.ImageUtils;

public class ApplyFilterActivity extends BaseActivity {
    private ImageButton backBtn;
    private FrameLayout photoContainer, brushOverlay, stickerOverlay;
    private View photoMask;
    private ConstraintLayout bottomArea;
    private ImageButton toArchiveBtn, toReviewBtn;
    private GLSurfaceView glSurfaceView;
    private FGLRenderer renderer;
    private String brushPath, stickerPath;
    private FilterDtoCreateRequest.ColorAdjustments adj;
    private Bitmap finalBitmapWithStickers = null;
    private FrameLayout reviewPopOff;
    private View reviewPopOn, dimBackground;
    private ConstraintLayout reviewPop;
    private ImageView snsIcon;
    private TextView snsId;
    private ImageButton reviewBtn;
    private boolean isReviewPopVisible = false;
    private String filterId, imgUrl, title, nick;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_apply_photo);
        backBtn = findViewById(R.id.backBtn);
        photoContainer = findViewById(R.id.photoContainer);
        brushOverlay = findViewById(R.id.brushOverlay);
        stickerOverlay = findViewById(R.id.stickerOverlay);
        photoMask = findViewById(R.id.photoMask);
        bottomArea = findViewById(R.id.bottomArea);
        toArchiveBtn = findViewById(R.id.toArchiveBtn);
        toReviewBtn = findViewById(R.id.toReviewBtn);
        reviewPopOff = findViewById(R.id.reviewPopOff);

        setupReviewPop();

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        renderer = new FGLRenderer(this, glSurfaceView);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        photoContainer.addView(glSurfaceView, 0);

        photoMask.bringToFront();

        photoContainer.post(() -> {
            photoMask.post(() -> updatePhotoMask(photoMask));
        });

        renderer.setOnBitmapCaptureListener(baseBitmap -> {
            try {
                Bitmap finalBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(finalBitmap);

                int vX = renderer.getViewportX();
                int vY = renderer.getViewportY();
                int vW = renderer.getViewportWidth();
                int vH = renderer.getViewportHeight();

                Bitmap overlayBitmap = Bitmap.createBitmap(
                        photoContainer.getWidth(),
                        photoContainer.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
                Canvas overlayCanvas = new Canvas(overlayBitmap);
                brushOverlay.draw(overlayCanvas);
                stickerOverlay.draw(overlayCanvas);

                Rect src = new Rect(vX, vY, vX + vW, vY + vH);
                Rect dst = new Rect(0, 0, finalBitmap.getWidth(), finalBitmap.getHeight());
                canvas.drawBitmap(overlayBitmap, src, dst, null);
                overlayBitmap.recycle();

                finalBitmapWithStickers = finalBitmap;

                //사진 저장 메서드 호출
                ImageUtils.saveBitmapToGallery(ApplyFilterActivity.this, finalBitmapWithStickers);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        filterId = getIntent().getStringExtra("filterId");
        imgUrl = getIntent().getStringExtra("filterImage");
        title = getIntent().getStringExtra("filterTitle");
        nick = getIntent().getStringExtra("nickname");

        adj = (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
        brushPath = getIntent().getStringExtra("brush_image_path");
        stickerPath = getIntent().getStringExtra("sticker_image_path");

        if (adj != null) applyAdjustments(adj);
        if (brushPath != null) applyBrushStickerImage(brushOverlay, brushPath);
        if (stickerPath != null) applyBrushStickerImage(stickerOverlay, stickerPath);

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });
    }

    private void setupReviewPop() {
        FrameLayout rootView = findViewById(R.id.reviewPopOff);
        reviewPopOn = getLayoutInflater().inflate(R.layout.f_review_pop, null);
        reviewPop = reviewPopOn.findViewById(R.id.reviewPop);
        snsIcon = reviewPopOn.findViewById(R.id.snsIcon);
        snsId = reviewPopOn.findViewById(R.id.snsId);
        reviewBtn = reviewPopOn.findViewById(R.id.reviewBtn);

        dimBackground = new View(this);
        dimBackground.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        dimBackground.setBackgroundColor(Color.parseColor("#B3000000"));
        dimBackground.setVisibility(View.GONE);

        rootView.addView(dimBackground);
        rootView.addView(reviewPopOn);
        reviewPopOn.setVisibility(View.GONE);
        reviewPopOn.setTranslationY(800);

        dimBackground.setOnClickListener(v -> hideReviewPop());

        toReviewBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (isReviewPopVisible) return;
            showReviewPop();
        });

        /// 중첩 클릭되면 안 됨 ///
        reviewBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            if (finalBitmapWithStickers == null) return;

            String savedPath = ImageUtils.saveBitmapToCache(ApplyFilterActivity.this, finalBitmapWithStickers);

            Intent intent = new Intent(ApplyFilterActivity.this, ReviewActivity.class);
            intent.putExtra("filterId", filterId);
            intent.putExtra("filterImage", imgUrl);
            intent.putExtra("filterTitle", title);
            intent.putExtra("nickname", nick);
            intent.putExtra("reviewImg", savedPath);

            startActivity(intent);
            hideReviewPop();
            finish();
        });
    }

    private void showReviewPop() {
        isReviewPopVisible = true;
        dimBackground.setVisibility(View.VISIBLE);
        reviewPopOn.setVisibility(View.VISIBLE);
        reviewPopOn.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
    }

    private void hideReviewPop() {
        reviewPopOn.animate()
                .translationY(800)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        reviewPopOn.setVisibility(View.GONE);
                        dimBackground.setVisibility(View.GONE);
                        isReviewPopVisible = false;
                    }
                })
                .start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dimBackground != null && dimBackground.getVisibility() == View.VISIBLE) {
            dimBackground.setVisibility(View.GONE);
            isReviewPopVisible = false;
        }
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
        glSurfaceView.postDelayed(() -> {
            renderer.captureBitmap();
        }, 150);
    }

    private void applyBrushStickerImage(FrameLayout overlay, String path) {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        imageView.setImageBitmap(BitmapFactory.decodeFile(path));
        imageView.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
        overlay.addView(imageView);
    }
}
