package com.example.filter.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.example.filter.R;
import com.example.filter.etc.ClickUtils;
import com.example.filter.apis.FilterDtoCreateRequest;
import com.example.filter.etc.ReviewStore;
import com.example.filter.items.ReviewItem;

import java.util.List;

public class FilterDetailActivity extends BaseActivity {
    private ImageButton backBtn, originalBtn;
    private ImageView shareBtn;
    private TextView nickname, deleteBtn, filterTitle, moreBtn, noReviewTxt;
    private TextView tag1, tag2, tag3, tag4, tag5;
    private TextView saveCount, useCount, reviewCount;
    private ImageView img, bookmark;
    private LinearLayout reviewBox1, reviewBox2;
    private ImageView rb1Img1, rb1Img2, rb2Img1, rb2Img2, rb2Img3, rb2Img4, rb2Img5;
    private ConstraintLayout tagBox, btnBox;
    private AppCompatButton changeBtn, useBtn;
    private FrameLayout chooseUseModeOff;
    private View chooseUseModeOn, dimBackground;
    private ConstraintLayout chooseUseMode;
    private ImageButton galleryModeBtn, cameraModeBtn;
    private boolean isChooseUseModeVisible = false;
    private FilterDtoCreateRequest.ColorAdjustments adj;
    private String filterId, nick, originalPath, imgUrl, title, tagsStr, brushPath, stickerPath;
    private float cropN_l = -1f, cropN_t = -1f, cropN_r = -1f, cropN_b = -1f;
    private int accumRotationDeg = 0;
    private boolean accumFlipH = false, accumFlipV = false;
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri photoUri = result.getData().getData();
            if (photoUri != null) {
                Intent intent = new Intent(FilterDetailActivity.this, ApplyFilterActivity.class);
                intent.setData(photoUri);

                intent.putExtra("color_adjustments", adj);
                intent.putExtra("brush_image_path", brushPath);
                intent.putExtra("sticker_image_path", stickerPath);

                intent.putExtra("filterId", filterId);
                intent.putExtra("filterImage", imgUrl);
                intent.putExtra("filterTitle", title);
                intent.putExtra("nickname", nick);

                startActivity(intent);
            } else {
                Toast.makeText(this, "사진 선택 실패: URI 없음", Toast.LENGTH_SHORT).show();
            }
        }
    });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_filter_detail);
        backBtn = findViewById(R.id.backBtn);
        shareBtn = findViewById(R.id.shareBtn);
        originalBtn = findViewById(R.id.originalBtn);
        nickname = findViewById(R.id.nickname);
        deleteBtn = findViewById(R.id.deleteBtn);
        filterTitle = findViewById(R.id.filterTitle);
        moreBtn = findViewById(R.id.moreBtn);
        noReviewTxt = findViewById(R.id.noReviewTxt);
        tagBox = findViewById(R.id.tagBox);
        tag1 = findViewById(R.id.tag1);
        tag2 = findViewById(R.id.tag2);
        tag3 = findViewById(R.id.tag3);
        tag4 = findViewById(R.id.tag4);
        tag5 = findViewById(R.id.tag5);
        saveCount = findViewById(R.id.saveCount);
        useCount = findViewById(R.id.useCount);
        reviewCount = findViewById(R.id.reviewCount);
        img = findViewById(R.id.img);
        bookmark = findViewById(R.id.bookmark);
        reviewBox1 = findViewById(R.id.reviewBox1);
        reviewBox2 = findViewById(R.id.reviewBox2);
        rb1Img1 = findViewById(R.id.rb1Img1);
        rb1Img2 = findViewById(R.id.rb1Img2);
        rb2Img1 = findViewById(R.id.rb2Img1);
        rb2Img2 = findViewById(R.id.rb2Img2);
        rb2Img3 = findViewById(R.id.rb2Img3);
        rb2Img4 = findViewById(R.id.rb2Img4);
        rb2Img5 = findViewById(R.id.rb2Img5);
        btnBox = findViewById(R.id.btnBox);
        changeBtn = findViewById(R.id.changeBtn);
        useBtn = findViewById(R.id.useBtn);
        chooseUseModeOff = findViewById(R.id.chooseUseModeOff);

        setupChooseUseMode();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int btnBoxBottom = btnBox.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(btnBox, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), btnBoxBottom + nav.bottom);
            return insets;
        });

        filterId = getIntent().getStringExtra("filterId");
        nick = getIntent().getStringExtra("nickname");
        originalPath = getIntent().getStringExtra("original_image_path");
        imgUrl = getIntent().getStringExtra("imgUrl");
        title = getIntent().getStringExtra("filterTitle");
        tagsStr = getIntent().getStringExtra("tags");

        cropN_l = getIntent().getFloatExtra("cropRectN_l", -1f);
        cropN_t = getIntent().getFloatExtra("cropRectN_t", -1f);
        cropN_r = getIntent().getFloatExtra("cropRectN_r", -1f);
        cropN_b = getIntent().getFloatExtra("cropRectN_b", -1f);

        accumRotationDeg = getIntent().getIntExtra("accumRotationDeg", 0);
        accumFlipH = getIntent().getBooleanExtra("accumFlipH", false);
        accumFlipV = getIntent().getBooleanExtra("accumFlipV", false);

        adj = (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
        brushPath = getIntent().getStringExtra("brush_image_path");
        stickerPath = getIntent().getStringExtra("sticker_image_path");

        if (imgUrl != null) {
            Glide.with(this)
                    .asBitmap()
                    .load(imgUrl)
                    .signature(new ObjectKey(filterId))
                    .fitCenter()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            img.setImageBitmap(resource);

                            Palette.from(resource).maximumColorCount(8).generate(palette -> {
                                int dom = palette.getDominantColor(0xFF7F7F7F);
                                double lum = ColorUtils.calculateLuminance(dom);
                                double contrast = ColorUtils.calculateContrast(dom, Color.WHITE);
                                boolean isDark = (lum < 0.4) && (contrast > 1.5);
                                originalBtn.setImageResource(isDark
                                        ? R.drawable.icon_original_white
                                        : R.drawable.icon_original_black);
                            });
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
        if (title != null) filterTitle.setText(title);
        if (nick != null) nickname.setText(nick);

        //saveCount.setText(String.valueOf(save)+" 저장");
        //useCount.setText(String.valueOf(count) + " 사용");

        TextView[] tagViews = {tag1, tag2, tag3, tag4, tag5};

        for (TextView tagView : tagViews) {
            tagView.setVisibility(View.GONE);
        }

        if (tagsStr != null && !tagsStr.trim().isEmpty()) {
            String[] tags = tagsStr.trim().split("\\s+");

            for (int i = 0; i < Math.min(tags.length, tagViews.length); i++) {
                if (tags[i] != null && !tags[i].isEmpty()) {
                    tagViews[i].setText(tags[i]);
                    tagViews[i].setVisibility(View.VISIBLE);
                }
            }
        }

        setupOriginalButton();

        deleteBtn.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("deleted_filter_id", filterId);
            setResult(RESULT_OK, resultIntent);

            Intent mainIntent = new Intent(FilterDetailActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mainIntent.putExtra("DELETED_ID_FROM_DETAIL", filterId);

            startActivity(mainIntent);

            finish();
        });

        moreBtn.setOnClickListener(v -> {
            Intent intent2 = new Intent(FilterDetailActivity.this, ReviewActivity.class);
            intent2.putExtra("filterId", filterId);
            intent2.putExtra("filterImage", imgUrl);
            intent2.putExtra("filterTitle", title);
            intent2.putExtra("nickname", nick);
            startActivityForResult(intent2, 1001);
        });

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            finish();
        });
    }

    private void setupChooseUseMode() {
        FrameLayout rootView = findViewById(R.id.chooseUseModeOff);
        chooseUseModeOn = getLayoutInflater().inflate(R.layout.f_choose_use_mode, null);
        chooseUseMode = chooseUseModeOn.findViewById(R.id.chooseUseMode);
        galleryModeBtn = chooseUseModeOn.findViewById(R.id.galleryModeBtn);
        cameraModeBtn = chooseUseModeOn.findViewById(R.id.cameraModeBtn);

        dimBackground = new View(this);
        dimBackground.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        dimBackground.setBackgroundColor(Color.parseColor("#B3000000"));
        dimBackground.setVisibility(View.GONE);

        rootView.addView(dimBackground);
        rootView.addView(chooseUseModeOn);
        chooseUseModeOn.setVisibility(View.GONE);
        chooseUseModeOn.setTranslationY(800);

        useBtn.setOnClickListener(v -> {
            if (isChooseUseModeVisible) return;
            showChooseUseMode();
        });

        dimBackground.setOnClickListener(v -> hideChooseUseMode());

        galleryModeBtn.setOnClickListener(v -> {
            chooseUseModeOn.setVisibility(View.GONE);
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

            /*cameraModeBtn.setOnClickListener(v -> {
                hideChooseUseMode();
            });*/
    }

    private void showChooseUseMode() {
        isChooseUseModeVisible = true;
        dimBackground.setVisibility(View.VISIBLE);
        chooseUseModeOn.setVisibility(View.VISIBLE);
        chooseUseModeOn.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
    }

    private void hideChooseUseMode() {
        chooseUseModeOn.animate()
                .translationY(800)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        chooseUseModeOn.setVisibility(View.GONE);
                        dimBackground.setVisibility(View.GONE);
                        isChooseUseModeVisible = false;
                    }
                })
                .start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupOriginalButton();

        if (dimBackground != null && dimBackground.getVisibility() == View.VISIBLE) {
            dimBackground.setVisibility(View.GONE);
            isChooseUseModeVisible = false;
        }

        String key = (filterId != null && !filterId.isEmpty()) ? filterId : (nick + "_" + title);
        List<ReviewItem> reviews = ReviewStore.getReviews(key);

        int size = reviews.size();
        reviewCount.setText("리뷰 (" + size + ")");
        noReviewTxt.setVisibility(View.GONE);
        reviewBox1.setVisibility(View.GONE);
        reviewBox2.setVisibility(View.GONE);

        if (size == 0) {
            noReviewTxt.setVisibility(View.VISIBLE);
        } else if (size <= 4) {
            reviewBox1.setVisibility(View.VISIBLE);
            rb1Img1.setVisibility(View.INVISIBLE);
            rb1Img2.setVisibility(View.INVISIBLE);

            Glide.with(this).clear(rb1Img1);
            Glide.with(this).clear(rb1Img2);

            if (size >= 1) {
                rb1Img1.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(reviews.get(0).imageUrl)
                        .centerCrop()
                        .into(rb1Img1);
            }
            if (size >= 2) {
                rb1Img2.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(reviews.get(1).imageUrl)
                        .centerCrop()
                        .into(rb1Img2);
            }

        } else {
            reviewBox2.setVisibility(View.VISIBLE);
            ImageView[] rb2Imgs = {rb2Img1, rb2Img2, rb2Img3, rb2Img4, rb2Img5};
            for (ImageView iv : rb2Imgs) Glide.with(this).clear(iv);
            for (int i = 0; i < Math.min(size, 5); i++) {
                Glide.with(this)
                        .load(reviews.get(i).imageUrl)
                        .centerCrop()
                        .into(rb2Imgs[i]);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            String reviewImg = data.getStringExtra("reviewImg");
            String reviewNick = data.getStringExtra("reviewNick");
            String reviewSnsId = data.getStringExtra("reviewSnsId");

            Intent intent = new Intent(FilterDetailActivity.this, ReviewActivity.class);
            intent.putExtra("filterId", filterId);
            intent.putExtra("filterImage", imgUrl);
            intent.putExtra("filterTitle", title);
            intent.putExtra("nickname", nick);
            intent.putExtra("reviewImg", reviewImg);
            intent.putExtra("reviewNick", reviewNick);
            intent.putExtra("reviewSnsId", reviewSnsId);
            startActivity(intent);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupOriginalButton() {
        if (originalBtn == null) return;

        originalBtn.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (originalPath != null) {
                        v.setPressed(true);
                        originalBtn.setAlpha(0.4f);
                        loadOriginalImage(originalPath, img);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (imgUrl != null) {
                        v.setPressed(false);
                        originalBtn.setAlpha(1f);
                        Glide.with(FilterDetailActivity.this)
                                .load(imgUrl)
                                .dontAnimate()
                                .placeholder(img.getDrawable())
                                .fitCenter()
                                .into(img);
                    }
                    return true;
            }
            return true;
        });
    }

    private void loadOriginalImage(String path, ImageView target) {
        Glide.with(this)
                .asBitmap()
                .load(path)
                .dontAnimate()
                .placeholder(img.getDrawable())
                .fitCenter()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Bitmap transformedBitmap = resource;

                        if (accumRotationDeg != 0 || accumFlipH || accumFlipV) {
                            Matrix matrix = new Matrix();
                            matrix.postRotate(accumRotationDeg);
                            float sx = accumFlipH ? -1f : 1f;
                            float sy = accumFlipV ? -1f : 1f;
                            matrix.postScale(sx, sy);

                            try {
                                transformedBitmap = Bitmap.createBitmap(resource, 0, 0,
                                        resource.getWidth(), resource.getHeight(), matrix, true
                                );
                            } catch (Exception e) {
                                transformedBitmap = resource;
                            }
                        }

                        Bitmap finalBitmap = transformedBitmap;

                        if (cropN_l >= 0 && cropN_t >= 0 && cropN_r >= 0 && cropN_b >= 0) {
                            int w = transformedBitmap.getWidth();
                            int h = transformedBitmap.getHeight();

                            int x = (int) (cropN_l * w);
                            int y = (int) (cropN_t * h);
                            int cropW = (int) ((cropN_r - cropN_l) * w);
                            int cropH = (int) ((cropN_b - cropN_t) * h);

                            try {
                                x = Math.max(0, x);
                                y = Math.max(0, y);
                                cropW = Math.min(w - x, cropW);
                                cropH = Math.min(h - y, cropH);

                                if (cropW > 0 && cropH > 0) {
                                    finalBitmap = Bitmap.createBitmap(transformedBitmap, x, y, cropW, cropH);
                                }
                            } catch (Exception e) {
                                finalBitmap = transformedBitmap;
                            }
                        }

                        target.setImageBitmap(finalBitmap);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        target.setImageDrawable(placeholder);
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("original_image_path", originalPath);
        outState.putString("imgUrl", imgUrl);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        originalPath = savedInstanceState.getString("original_image_path");
        imgUrl = savedInstanceState.getString("imgUrl");
    }
}