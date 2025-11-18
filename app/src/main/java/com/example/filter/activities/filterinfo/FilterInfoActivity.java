package com.example.filter.activities.filterinfo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.MainActivity;
import com.example.filter.activities.apply.ApplyFilterActivity;
import com.example.filter.activities.apply.CameraActivity;
import com.example.filter.activities.review.ReviewActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.apis.dto.FilterDtoCreateRequest;
import com.example.filter.etc.FaceStickerData;
import com.example.filter.etc.ReviewStore;
import com.example.filter.etc.UserManager;
import com.example.filter.items.ReviewItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FilterInfoActivity extends BaseActivity {
    private ImageButton backBtn, originalBtn;
    private ImageView shareBtn;
    private TextView nickname, deleteORreportBtn, filterTitle, moreBtn, noReviewTxt;
    private TextView tag1, tag2, tag3, tag4, tag5;
    private TextView saveCount, useCount, reviewCount;
    private ImageView img, bookmark;
    private LinearLayout reviewBox1, reviewBox2;
    private ImageView rb1Img1, rb1Img2, rb2Img1, rb2Img2, rb2Img3, rb2Img4, rb2Img5;
    private ConstraintLayout tagBox, btnBox;
    private AppCompatButton changeORbuyBtn, selectModeBtn, selectModeBtn2;
    private boolean isFree = false, isBuy = false;
    private FrameLayout modalOff;
    private View chooseUseModeOn, buyFilterOn, buyFilterSuccessOn, dimBackground;
    private ConstraintLayout chooseUseMode, buyFilter, buyFilterSuccess;
    private ImageButton galleryModeBtn, cameraModeBtn, buyBtn, useBtn, closeBtn;
    private TextView alertTxt, point, currentPoint1, currentPoint2;
    private boolean isModalVisible = false;
    private FilterDtoCreateRequest.ColorAdjustments adj;
    private ArrayList<FaceStickerData> faceStickers;
    private String filterId, nick, originalPath, imgUrl, title, tagsStr, price, brushPath, stickerImageNoFacePath;
    private float cropN_l = -1f, cropN_t = -1f, cropN_r = -1f, cropN_b = -1f;
    private int accumRotationDeg = 0;
    private boolean accumFlipH = false, accumFlipV = false;
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri photoUri = result.getData().getData();
            if (photoUri != null) {
                Intent intent = new Intent(FilterInfoActivity.this, ApplyFilterActivity.class);
                intent.setData(photoUri);

                intent.putExtra("color_adjustments", adj);
                intent.putExtra("brush_image_path", brushPath);

                /// 얼굴인식스티커 정보 전달 ///
                intent.putExtra("stickerImageNoFacePath", stickerImageNoFacePath);
                intent.putExtra("face_stickers", new ArrayList<>(faceStickers));

                List<FilterDtoCreateRequest.Sticker> stickers = new ArrayList<>();
                for (FaceStickerData d : faceStickers) {
                    FilterDtoCreateRequest.Sticker s = new FilterDtoCreateRequest.Sticker();
                    s.placementType = "face";
                    s.x = d.relX;
                    s.y = d.relY;
                    s.scale = (d.relW + d.relH) / 2f;
                    //s.relW = d.relW;
                    //s.relH = d.relH;
                    s.rotation = d.rot;
                    s.stickerId = d.groupId;
                    stickers.add(s);

                    /*Log.d("StickerFlow", String.format(
                            "[FilterDetailActivity] 전달 준비 → relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, groupId=%d",
                            d.relX, d.relY, d.relW, d.relH, d.rot, d.groupId
                    ));*/
                }

                intent.putExtra("filterId", filterId);
                intent.putExtra("filterImage", imgUrl);
                intent.putExtra("filterTitle", title);

                /// 본인이 만든 필터냐 남이 만든 필터냐에 따라 다름
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
        setContentView(R.layout.a_filter_info);
        backBtn = findViewById(R.id.backBtn);
        shareBtn = findViewById(R.id.shareBtn);
        originalBtn = findViewById(R.id.originalBtn);
        nickname = findViewById(R.id.nickname);
        deleteORreportBtn = findViewById(R.id.deleteORreportBtn);
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
        changeORbuyBtn = findViewById(R.id.changeORbuyBtn);
        selectModeBtn = findViewById(R.id.selectModeBtn);
        selectModeBtn2 = findViewById(R.id.selectModeBtn2);
        modalOff = findViewById(R.id.modalOff);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int btnBoxBottom = btnBox.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(btnBox, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), btnBoxBottom + nav.bottom);
            return insets;
        });

        filterId = getIntent().getStringExtra("filterId");

        /// 등록화면에서 전달받은 닉
        nick = getIntent().getStringExtra("nickname");
        Log.d("닉네임 테스트", "필터 상세 | 닉네임 : " + nick);


        originalPath = getIntent().getStringExtra("original_image_path");
        imgUrl = getIntent().getStringExtra("imgUrl");
        title = getIntent().getStringExtra("filterTitle");
        tagsStr = getIntent().getStringExtra("tags");
        price = getIntent().getStringExtra("price");

        cropN_l = getIntent().getFloatExtra("cropRectN_l", -1f);
        cropN_t = getIntent().getFloatExtra("cropRectN_t", -1f);
        cropN_r = getIntent().getFloatExtra("cropRectN_r", -1f);
        cropN_b = getIntent().getFloatExtra("cropRectN_b", -1f);

        accumRotationDeg = getIntent().getIntExtra("accumRotationDeg", 0);
        accumFlipH = getIntent().getBooleanExtra("accumFlipH", false);
        accumFlipV = getIntent().getBooleanExtra("accumFlipV", false);

        adj = (FilterDtoCreateRequest.ColorAdjustments) getIntent().getSerializableExtra("color_adjustments");
        brushPath = getIntent().getStringExtra("brush_image_path");

        /// 얼굴인식스티커 정보 받기 ///
        stickerImageNoFacePath = getIntent().getStringExtra("stickerImageNoFacePath");
        faceStickers = (ArrayList<FaceStickerData>) getIntent().getSerializableExtra("face_stickers");
        /*if (faceStickers != null && !faceStickers.isEmpty()) {
            for (FaceStickerData d : faceStickers) {
                Log.d("StickerFlow", String.format(
                        "[FilterDetailActivity] 받은 FaceStickerData → relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, groupId=%d",
                        d.relX, d.relY, d.relW, d.relH, d.rot, d.groupId
                ));
            }
        } else {
            Log.d("StickerFlow", "[FilterDetailActivity] faceStickers가 비어있음 혹은 null입니다.");
        }*/

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

        /// 본인 닉
        String n = UserManager.get(FilterInfoActivity.this).getNickname();
        if (n.equals(nick)) {
            deleteORreportBtn.setText("삭제");
            //changeORbuyBtn.setText("가격 수정");
            changeORbuyBtn.setText(price + "P 구매");
        } else {
            deleteORreportBtn.setText("신고");
            changeORbuyBtn.setText(price + "P 구매");
        }

        /// 일단 여기가 필터 삭제 버튼 눌렀을 때, 해당 필터를 삭제하라고 전달하는 부분 ///
        /// 이제 닉네임이나 로그인 정보를 가지고 나인지 타인인지 구분해서 삭제버튼할지 신고버튼할지 추가할 것 ///
        deleteORreportBtn.setOnClickListener(v -> {
            if (deleteORreportBtn.getText().equals("삭제")) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("deleted_filter_id", filterId);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                /// 신고버튼 → 메일 보내기
            }
        });

        if (price.equals("0")) {
            isFree = true;
        }

        //한번 구매한건 제작자가 가격 바꿔도 구매한 상태가 되게
        SharedPreferences sp = getSharedPreferences("filter_buy", MODE_PRIVATE);
        isBuy = sp.getBoolean("buy_" + filterId, false);

        ClickUtils.clickDim(changeORbuyBtn);
        ClickUtils.clickDim(selectModeBtn);
        ClickUtils.clickDim(selectModeBtn2);

        changeORbuyBtn.setOnClickListener(v -> {
            if (changeORbuyBtn.getText().equals("가격 수정")) {

            } else {
                if (ClickUtils.isFastClick(v, 400)) return;
                if (isModalVisible) return;
                showModal(buyFilterOn);
            }
        });

        selectModeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (isModalVisible) return;
            showModal(chooseUseModeOn);
        });

        selectModeBtn2.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (isModalVisible) return;
            showModal(chooseUseModeOn);
        });

        setupModal();

        /*deleteORreportBtn.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("deleted_filter_id", filterId);
            setResult(RESULT_OK, resultIntent);
            finish();
        });*/


        moreBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Intent intent2 = new Intent(FilterInfoActivity.this, ReviewActivity.class);
            intent2.putExtra("filterId", filterId);
            intent2.putExtra("filterImage", imgUrl);
            intent2.putExtra("filterTitle", title);
            intent2.putExtra("nickname", nick);
            startActivityForResult(intent2, 1001);
        });


        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            Intent mainIntent = new Intent(FilterInfoActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            putFilterInfo(mainIntent);
            startActivity(mainIntent);
            finish();
        });
    }

    private void putFilterInfo(Intent intent) {
        intent.putExtra("filterId", filterId);


        intent.putExtra("nickname", nick);


        intent.putExtra("original_image_path", originalPath);
        intent.putExtra("imgUrl", imgUrl);
        intent.putExtra("filterTitle", title);
        intent.putExtra("tags", tagsStr);
        intent.putExtra("price", price);

        intent.putExtra("cropRectN_l", cropN_l);
        intent.putExtra("cropRectN_t", cropN_t);
        intent.putExtra("cropRectN_r", cropN_r);
        intent.putExtra("cropRectN_b", cropN_b);

        intent.putExtra("accumRotationDeg", accumRotationDeg);
        intent.putExtra("accumFlipH", accumFlipH);
        intent.putExtra("accumFlipV", accumFlipV);

        intent.putExtra("color_adjustments", adj);

        /// 스티커 + 브러쉬
        intent.putExtra("brush_image_path", brushPath);
        ///  얼굴인식스티커 정보 전달 ///
        intent.putExtra("stickerImageNoFacePath", stickerImageNoFacePath);
        intent.putExtra("face_stickers", new ArrayList<>(faceStickers));

        List<FilterDtoCreateRequest.Sticker> stickers = new ArrayList<>();
        for (FaceStickerData d : faceStickers) {
            FilterDtoCreateRequest.Sticker s = new FilterDtoCreateRequest.Sticker();
            s.placementType = "face";
            s.x = d.relX;
            s.y = d.relY;
            s.scale = (d.relW + d.relH) / 2f;
            //s.relW = d.relW;
            //s.relH = d.relH;
            s.rotation = d.rot;
            s.stickerId = d.groupId;
            stickers.add(s);

            /*Log.d("StickerFlow", String.format(
                    "[RegisterActivity] 전달 준비 → relX=%.4f, relY=%.4f, relW=%.4f, relH=%.4f, rot=%.4f, groupId=%d",
                    d.relX, d.relY, d.relW, d.relH, d.rot, d.groupId
            ));*/
        }
    }

    private void updateButtonState() {
        boolean boughtOrFree = isBuy || isFree;

        if (selectModeBtn2 != null) {
            selectModeBtn2.setVisibility(boughtOrFree ? View.VISIBLE : View.INVISIBLE);
        }
        if (changeORbuyBtn != null) {
            changeORbuyBtn.setVisibility(boughtOrFree ? View.INVISIBLE : View.VISIBLE);
        }
        if (selectModeBtn != null) {
            selectModeBtn.setVisibility(boughtOrFree ? View.INVISIBLE : View.VISIBLE);
        }
        if (alertTxt != null) {
            alertTxt.setVisibility(boughtOrFree ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void setupModal() {
        FrameLayout rootView = findViewById(R.id.modalOff);

        /// 필터 사용하기 모달
        chooseUseModeOn = getLayoutInflater().inflate(R.layout.f_choose_use_mode, null);
        chooseUseMode = chooseUseModeOn.findViewById(R.id.chooseUseMode);
        galleryModeBtn = chooseUseModeOn.findViewById(R.id.galleryModeBtn);
        cameraModeBtn = chooseUseModeOn.findViewById(R.id.cameraModeBtn);
        alertTxt = chooseUseModeOn.findViewById(R.id.alertTxt);

        /// 필터 구매하기 모달
        buyFilterOn = getLayoutInflater().inflate(R.layout.f_buy_filter, null);
        buyFilter = buyFilterOn.findViewById(R.id.buyFilter);
        point = buyFilterOn.findViewById(R.id.point);
        currentPoint1 = buyFilterOn.findViewById(R.id.currentPoint1);
        buyBtn = buyFilterOn.findViewById(R.id.buyBtn);

        /// 필터 구매완료 모달
        buyFilterSuccessOn = getLayoutInflater().inflate(R.layout.f_buy_filter_success, null);
        buyFilterSuccess = buyFilterSuccessOn.findViewById(R.id.buyFilterSuccess);
        currentPoint2 = buyFilterSuccessOn.findViewById(R.id.currentPoint2);
        useBtn = buyFilterSuccessOn.findViewById(R.id.useBtn);
        closeBtn = buyFilterSuccessOn.findViewById(R.id.closeBtn);

        /// 딤 셋팅
        dimBackground = new View(this);
        dimBackground.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        dimBackground.setBackgroundColor(Color.parseColor("#B3000000"));
        dimBackground.setVisibility(View.GONE);
        rootView.addView(dimBackground);

        dimBackground.setOnClickListener(v -> {
            hideModal();
        });

        /// 사용하기 모달 추가해놓고 숨김 처리
        rootView.addView(chooseUseModeOn);
        chooseUseModeOn.setVisibility(View.GONE);
        chooseUseModeOn.setTranslationY(800);

        /// 구매하기 모달 추가해놓고 숨김 처리
        rootView.addView(buyFilterOn);
        buyFilterOn.setVisibility(View.GONE);
        buyFilterOn.setTranslationY(800);

        /// 구매완료 모달 추가해놓고 숨김 처리
        rootView.addView(buyFilterSuccessOn);
        buyFilterSuccessOn.setVisibility(View.GONE);
        buyFilterSuccessOn.setTranslationY(800);

        /// 모달 영역 밖 누르면 모달 끄기
        dimBackground.setOnClickListener(v -> hideModal());

        /// 사용하기 모달 버튼 클릭 리스너와 경고 문구 설정
        updateButtonState();

        ClickUtils.clickDim(galleryModeBtn);
        galleryModeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            chooseUseModeOn.setVisibility(View.GONE);
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        ClickUtils.clickDim(cameraModeBtn);
        cameraModeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Intent intent = new Intent();
            intent.setClass(FilterInfoActivity.this, CameraActivity.class);

            /// adj, brushPath, stickerImageNoFacePath, 얼굴인식스티커 정보 전달 ///
            intent.putExtra("color_adjustments", adj);
            intent.putExtra("brush_image_path", brushPath);
            intent.putExtra("stickerImageNoFacePath", stickerImageNoFacePath);
            intent.putExtra("face_stickers", new ArrayList<>(faceStickers));

            startActivity(intent);
        });

        /// 구매하기 모달 가격 설정과 버튼 클릭 리스너 설정
        point.setText(price + "P");

        SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
        int current = sp.getInt("current_point", 0);
        currentPoint1.setText(current + "P");

        ClickUtils.clickDim(buyBtn);
        buyBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            buyFilterOn.animate()
                    .translationY(800)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            buyFilterOn.setVisibility(View.GONE);

                            SharedPreferences spLocal = getSharedPreferences("points", MODE_PRIVATE);
                            int currentLocal = spLocal.getInt("current_point", 0);
                            int priceInt = Integer.parseInt(price);

                            if (currentLocal < priceInt) {
                                Toast.makeText(FilterInfoActivity.this, "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            int newCurrent = currentLocal - priceInt;
                            SharedPreferences.Editor editor = spLocal.edit();
                            editor.putInt("current_point", newCurrent);
                            editor.putBoolean("buy_" + filterId, true); //한번 구매한건 제작자가 가격 바꿔도 구매한 상태가 되게
                            editor.apply();
                            saveBuyHistory(priceInt, newCurrent);

                            currentPoint2.setText(newCurrent + "P");
                            isBuy = true;
                            updateButtonState();
                        }
                    })
                    .start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showModal(buyFilterSuccessOn);
                }
            }, 300);
        });

        /// 구매완료 모달 잔여 포인트 설정과 버튼 클릭 리스너 설정
        ClickUtils.clickDim(useBtn);
        useBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            buyFilterSuccessOn.animate()
                    .translationY(800)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            buyFilterSuccessOn.setVisibility(View.GONE);
                        }
                    })
                    .start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showModal(chooseUseModeOn);
                }
            }, 300);
        });

        ClickUtils.clickDim(closeBtn);
        closeBtn.setOnClickListener(v -> hideModal());
    }

    private void showModal(View view) {
        isModalVisible = true;
        dimBackground.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
    }

    private void hideModal() {
        if (chooseUseModeOn != null && chooseUseModeOn.getVisibility() == View.VISIBLE) {
            chooseUseModeOn.animate()
                    .translationY(800)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            chooseUseModeOn.setVisibility(View.GONE);
                            dimBackground.setVisibility(View.GONE);
                            isModalVisible = false;
                        }
                    })
                    .start();
        } else if (buyFilterOn != null && buyFilterOn.getVisibility() == View.VISIBLE) {
            buyFilterOn.animate()
                    .translationY(800)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            buyFilterOn.setVisibility(View.GONE);
                            dimBackground.setVisibility(View.GONE);
                            isModalVisible = false;
                        }
                    })
                    .start();
        } else if (buyFilterSuccessOn != null && buyFilterSuccessOn.getVisibility() == View.VISIBLE) {
            buyFilterSuccessOn.animate()
                    .translationY(800)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            buyFilterSuccessOn.setVisibility(View.GONE);
                            dimBackground.setVisibility(View.GONE);
                            isModalVisible = false;
                        }
                    })
                    .start();
        } else {
            if (dimBackground != null) {
                dimBackground.setVisibility(View.GONE);
            }
            isModalVisible = false;
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void saveBuyHistory(int priceUsed, int newCurrentPoint) {
        SharedPreferences sp = getSharedPreferences("point_buy_history", MODE_PRIVATE);
        String json = sp.getString("buy_history_list", "[]");

        try {
            JSONArray oldArray = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            JSONObject obj = new JSONObject();

            obj.put("point1", priceUsed);
            obj.put("point2", newCurrentPoint);
            obj.put("txt", String.format("[%s] 필터 구매", title));
            obj.put("date", getCurrentDate());

            newArray.put(obj);
            for (int i = 0; i < oldArray.length(); i++) {
                newArray.put(oldArray.getJSONObject(i));
            }

            sp.edit().putString("buy_history_list", newArray.toString()).apply();
        } catch (Exception e) {
            Log.e("HistorySave", "구매 내역 저장 오류", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupOriginalButton();

        if (dimBackground != null && dimBackground.getVisibility() == View.VISIBLE) {
            dimBackground.setVisibility(View.GONE);
            isModalVisible = false;
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

            Intent intent = new Intent(FilterInfoActivity.this, ReviewActivity.class);
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
                        Glide.with(FilterInfoActivity.this)
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent mainIntent = new Intent(FilterInfoActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        putFilterInfo(mainIntent);

        startActivity(mainIntent);
        finish();
    }
}