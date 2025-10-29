    package com.example.filter.activities;

    import android.animation.Animator;
    import android.animation.AnimatorListenerAdapter;
    import android.annotation.SuppressLint;
    import android.content.Intent;
    import android.graphics.Bitmap;
    import android.graphics.Color;
    import android.graphics.drawable.Drawable;
    import android.net.Uri;
    import android.os.Bundle;
    import android.util.Log;
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
    import com.example.filter.etc.FilterDtoCreateRequest;
    import com.example.filter.fragments.RotationFragment;

    import java.io.Serializable;
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
        private ImageButton cameraModeBtn, galleryModeBtn;
        private boolean isChooseUseModeVisible = false;

        private FilterDtoCreateRequest.ColorAdjustments adj;
        private List<FilterDtoCreateRequest.Sticker> stickerList;
        private String originalPath;
        private String imgUrl;
        private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri photoUri = result.getData().getData();
                if (photoUri != null) {
                    Intent intent = new Intent(FilterDetailActivity.this, ApplyFilterActivity.class);
                    intent.setData(photoUri);

                    intent.putExtra("color_adjustments", adj);

                    if (adj != null) intent.putExtra("color_adjustments", adj);


                    intent.putExtra("stickers",(Serializable)  stickerList);


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

            adj = (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
            stickerList = (List<FilterDtoCreateRequest.Sticker>) getIntent().getSerializableExtra("stickers");

            if (adj != null) {
                Log.d("ColorAdjustments",
                        "[상세화면]\n" +
                                "밝기: " + adj.brightness + " 노출: " + adj.exposure +
                                " 대비: " + adj.contrast + " 하이라이트: " + adj.highlight +
                                " 그림자: " + adj.shadow + " 온도: " + adj.temperature +
                                " 색조: " + adj.hue + " 채도: " + adj.saturation +
                                " 선명하게: " + adj.sharpen + " 흐리게: " + adj.blur +
                                " 비네트: " + adj.vignette + " 노이즈: " + adj.noise);
            }

            Intent intent = getIntent();
            if (intent != null) {
                String filterId = intent.getStringExtra("filterId");
                String nick = intent.getStringExtra("nickname");
                imgUrl = intent.getStringExtra("imgUrl");
                String title = intent.getStringExtra("filterTitle");
                String tagsStr = intent.getStringExtra("tags");
                int count = intent.getIntExtra("count", 0);
                originalPath = getIntent().getStringExtra("original_image_path");

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
                                        int dom = (palette != null) ? palette.getDominantColor(0xFF7F7F7F) : 0xFF7F7F7F;
                                        double lum = ColorUtils.calculateLuminance(dom);
                                        boolean isDark = lum < 0.5;

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
                useCount.setText(String.valueOf(count) + " 사용");

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
                    finish();
                });
            }

            /*Intent intent = getIntent();
            if (intent != null) {
                String imgUrl = intent.getStringExtra("imgUrl");
                String title = intent.getStringExtra("filterTitle");
                String price = intent.getStringExtra("price");
                String nick = intent.getStringExtra("nickname");
                int count = intent.getIntExtra("count", 0);

                if (imgUrl != null) Glide.with(this).load(imgUrl).fitCenter().into(img);
                if (title != null) filterTitle.setText(title);
                if (price != null) changeBtn.setText("구매 : " + price + "P");
                if (nick != null) nickname.setText(nick);
                useCount.setText(String.valueOf(count));
            }*/

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

            /*cameraModeBtn.setOnClickListener(v -> {
                hideChooseUseMode();
            });*/

            galleryModeBtn.setOnClickListener(v -> {
                //hideChooseUseMode();
                chooseUseModeOn.setVisibility(View.GONE);

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            });
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
        }

        @SuppressLint("ClickableViewAccessibility")
        private void setupOriginalButton(){
            if (originalBtn == null) return;

            originalBtn.setOnTouchListener((v, ev) -> {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (originalPath != null) {
                            v.setPressed(true);
                            originalBtn.setAlpha(0.4f);
                            Glide.with(FilterDetailActivity.this)
                                    .load(originalPath)
                                    .dontAnimate()
                                    .placeholder(img.getDrawable())
                                    .fitCenter()
                                    .into(img);
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

        @Override
        protected void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("originalPath", originalPath);
            outState.putString("imgUrl", imgUrl);
        }

        @Override
        protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
            super.onRestoreInstanceState(savedInstanceState);
            originalPath = savedInstanceState.getString("originalPath");
            imgUrl = savedInstanceState.getString("imgUrl");
        }
    }