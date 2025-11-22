package com.example.filter.activities.filterinfo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.filter.api_datas.FaceStickerData; // ✅ 하나만 유지
import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.response_dto.FilterResponse;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.dialogs.PointChangeDialog; // ✅ Import 추가
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.ReviewStore;
import com.example.filter.items.ReviewItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; // Map 사용을 위해 추가

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilterInfoActivity extends BaseActivity {
    // UI 요소
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

    // 모달(팝업) 관련 UI
    private FrameLayout modalOff;
    private View chooseUseModeOn, buyFilterOn, buyFilterSuccessOn, dimBackground;
    private ConstraintLayout chooseUseMode, buyFilter, buyFilterSuccess;
    private ImageButton galleryModeBtn, cameraModeBtn, buyBtn, useBtn, closeBtn;
    private TextView alertTxt, point, currentPoint1, currentPoint2;
    private boolean isModalVisible = false;

    // 상태 변수
    private boolean isFree = false;
    private boolean isBuy = false; // 현재 사용자가 구매했는지 여부
    private boolean isMine = false; // 내가 만든 필터인지 여부
    private boolean isBookmarked = false;

    // 데이터 변수
    private FilterResponse filterData; // 서버 응답 객체
    private FilterDtoCreateRequest.ColorAdjustments adj;
    private ArrayList<FaceStickerData> faceStickers;
    private String filterId, nick, originalPath, imgUrl, title, tagsStr, price, brushPath, stickerImageNoFacePath;

    // 편집 정보 (로컬 이미지 복원용)
    private float cropN_l = -1f, cropN_t = -1f, cropN_r = -1f, cropN_b = -1f;
    private int accumRotationDeg = 0;
    private boolean accumFlipH = false, accumFlipV = false;

    // 갤러리 런처
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri photoUri = result.getData().getData();
            if (photoUri != null) {
                Intent intent = new Intent(FilterInfoActivity.this, ApplyFilterActivity.class);
                intent.setData(photoUri);
                // 필요한 데이터 전달
                intent.putExtra("color_adjustments", adj);
                intent.putExtra("brush_image_path", brushPath);
                intent.putExtra("stickerImageNoFacePath", stickerImageNoFacePath);
                if (faceStickers != null) {
                    intent.putExtra("face_stickers", new ArrayList<>(this.faceStickers));
                }
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
        setContentView(R.layout.a_filter_info);

        // 1. View 초기화
        initViews();

        // 2. 시스템 바 인셋 설정
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int btnBoxBottom = btnBox.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(btnBox, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), btnBoxBottom + nav.bottom);
            return insets;
        });

        // 3. 데이터 수신 및 처리
        FilterResponse responseObj = (FilterResponse) getIntent().getSerializableExtra("filter_response");

        if (responseObj != null) {
            // [Case A] 등록 직후 (객체 데이터 사용)
            setFilterData(responseObj);
        } else {
            // [Case B] 목록 진입 (ID만 있거나 일부 정보만 있음)
            setBasicInfoFromIntent();
            if (filterId != null) {
                loadFilterInfo(Long.parseLong(filterId));
            }
        }

        // 4. 모달 및 리스너 설정
        setupModal();
        setupListeners();
    }

    private void initViews() {
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
    }

    // ✅ [서버 API] 필터 상세 정보 조회
    private void loadFilterInfo(long id) {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);
        api.getFilter(id).enqueue(new Callback<FilterResponse>() {
            @Override
            public void onResponse(Call<FilterResponse> call, Response<FilterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    setFilterData(response.body());
                } else {
                    Log.e("FilterInfo", "상세 조회 실패: " + response.code());
                    Toast.makeText(FilterInfoActivity.this, "정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<FilterResponse> call, Throwable t) {
                Log.e("FilterInfo", "통신 오류", t);
            }
        });
    }

    // ✅ 데이터 세팅 및 UI 갱신
    private void setFilterData(FilterResponse data) {
        this.filterData = data;
        this.filterId = String.valueOf(data.id);
        this.title = data.name;
        this.nick = data.creator;
        this.imgUrl = data.editedImageUrl;
        this.originalPath = data.originalImageUrl;
        this.price = (data.price != null) ? String.valueOf(data.price) : "0";
        this.stickerImageNoFacePath = data.stickerImageNoFaceUrl;

        this.isMine = Boolean.TRUE.equals(data.isMine);
        this.isBuy = Boolean.TRUE.equals(data.isUsed);
        this.isFree = (data.price == null || data.price == 0);
        this.isBookmarked = Boolean.TRUE.equals(data.isBookmarked);

        if (data.tags != null && !data.tags.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String t : data.tags) sb.append(t).append(" ");
            this.tagsStr = sb.toString().trim();
        }

        updateUI();
        updateButtonState();
    }

    // Intent로부터 기본 정보 세팅
    private void setBasicInfoFromIntent() {
        Intent intent = getIntent();
        filterId = intent.getStringExtra("filterId");
        nick = intent.getStringExtra("nickname");
        imgUrl = intent.getStringExtra("imgUrl");
        title = intent.getStringExtra("filterTitle");
        price = intent.getStringExtra("price");
        if (price == null) price = "0";

        cropN_l = intent.getFloatExtra("cropRectN_l", -1f);
        cropN_t = intent.getFloatExtra("cropRectN_t", -1f);
        cropN_r = intent.getFloatExtra("cropRectN_r", -1f);
        cropN_b = intent.getFloatExtra("cropRectN_b", -1f);
        accumRotationDeg = intent.getIntExtra("accumRotationDeg", 0);
        accumFlipH = intent.getBooleanExtra("accumFlipH", false);
        accumFlipV = intent.getBooleanExtra("accumFlipV", false);

        adj = (FilterDtoCreateRequest.ColorAdjustments) intent.getSerializableExtra("color_adjustments");
        brushPath = intent.getStringExtra("brush_image_path");
        stickerImageNoFacePath = intent.getStringExtra("stickerImageNoFacePath");
        faceStickers = (ArrayList<FaceStickerData>) intent.getSerializableExtra("face_stickers");

        isFree = "0".equals(price);
        updateUI();
        updateButtonState();
    }

    private void updateUI() {
        if (title != null) filterTitle.setText(title);
        if (nick != null) nickname.setText(nick);
        if (saveCount != null && filterData != null && filterData.saveCount != null) {
            saveCount.setText(filterData.saveCount + " 저장");
        }
        if (useCount != null && filterData != null && filterData.useCount != null) {
            useCount.setText(filterData.useCount + " 사용");
        }

        if (imgUrl != null) {
            Glide.with(this).asBitmap().load(imgUrl).fitCenter().into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    img.setImageBitmap(resource);
                    Palette.from(resource).maximumColorCount(8).generate(palette -> {
                        int dom = palette.getDominantColor(0xFF7F7F7F);
                        double lum = ColorUtils.calculateLuminance(dom);
                        double contrast = ColorUtils.calculateContrast(dom, Color.WHITE);
                        boolean isDark = (lum < 0.4) && (contrast > 1.5);
                        originalBtn.setImageResource(isDark ? R.drawable.icon_original_white : R.drawable.icon_original_black);
                    });
                }
                @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
            });
        }

        TextView[] tagViews = {tag1, tag2, tag3, tag4, tag5};
        for (TextView tv : tagViews) tv.setVisibility(View.GONE);
        if (tagsStr != null && !tagsStr.trim().isEmpty()) {
            String[] tags = tagsStr.trim().split("\\s+");
            for (int i = 0; i < Math.min(tags.length, tagViews.length); i++) {
                String t = tags[i];
                if (!t.startsWith("#")) t = "#" + t;
                tagViews[i].setText(t);
                tagViews[i].setVisibility(View.VISIBLE);
            }
        }

        if (deleteORreportBtn != null) {
            deleteORreportBtn.setText(isMine ? "삭제" : "신고");
        }
    }

    // ✅ 버튼 상태 업데이트
    private void updateButtonState() {
        if (isMine) {
            if (changeORbuyBtn != null) {
                changeORbuyBtn.setText("가격 수정");
                changeORbuyBtn.setVisibility(View.VISIBLE);
            }
            if (selectModeBtn != null) selectModeBtn.setVisibility(View.VISIBLE);
            if (selectModeBtn2 != null) selectModeBtn2.setVisibility(View.INVISIBLE);
            if (alertTxt != null) alertTxt.setVisibility(View.INVISIBLE);
            return;
        }

        boolean canUseImmediately = isFree || isBuy;

        if (canUseImmediately) {
            if (selectModeBtn2 != null) selectModeBtn2.setVisibility(View.VISIBLE);
            if (changeORbuyBtn != null) changeORbuyBtn.setVisibility(View.INVISIBLE);
            if (selectModeBtn != null) selectModeBtn.setVisibility(View.INVISIBLE);
            if (alertTxt != null) alertTxt.setVisibility(View.INVISIBLE);
        } else {
            if (changeORbuyBtn != null) {
                changeORbuyBtn.setText(price + "P 구매");
                changeORbuyBtn.setVisibility(View.VISIBLE);
            }
            if (selectModeBtn != null) selectModeBtn.setVisibility(View.VISIBLE);
            if (selectModeBtn2 != null) selectModeBtn2.setVisibility(View.INVISIBLE);
            if (alertTxt != null) alertTxt.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        // 구매 / 가격 수정 버튼
        if (changeORbuyBtn != null) {
            changeORbuyBtn.setOnClickListener(v -> {
                if (isMine) {
                    showPointChangePopUp();
                } else {
                    if (ClickUtils.isFastClick(v, 400)) return;
                    if (isModalVisible) return;

                    if(point != null) point.setText(price + "P");
                    SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
                    if(currentPoint1 != null) currentPoint1.setText(sp.getInt("current_point", 0) + "P");

                    showModal(buyFilterOn);
                }
            });
        }

        // 사용하기 버튼
        if (selectModeBtn != null) {
            selectModeBtn.setOnClickListener(v -> {
                if (ClickUtils.isFastClick(v, 400)) return;
                if (isModalVisible) return;
                if (isMine || isFree || isBuy) {
                    showModal(chooseUseModeOn);
                } else {
                    showModal(buyFilterOn);
                }
            });
        }

        // 바로 사용 버튼
        if (selectModeBtn2 != null) {
            selectModeBtn2.setOnClickListener(v -> {
                if (ClickUtils.isFastClick(v, 400)) return;
                if (isModalVisible) return;
                showModal(chooseUseModeOn);
            });
        }

        // ★ [추가] 북마크 버튼 클릭
        if (bookmark != null) {
            bookmark.setOnClickListener(v -> {
                if (ClickUtils.isFastClick(v, 400)) return;
                // API 호출
                requestToggleBookmark(Long.parseLong(filterId));
            });
        }

        // 삭제 / 신고
        deleteORreportBtn.setOnClickListener(v -> {
            if (isMine) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("deleted_filter_id", filterId);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "신고 기능 준비중", Toast.LENGTH_SHORT).show();
            }
        });

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Intent mainIntent = new Intent(FilterInfoActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            finish();
        });

        moreBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Intent intent2 = new Intent(FilterInfoActivity.this, ReviewActivity.class);
            intent2.putExtra("filterId", filterId);
            intent2.putExtra("filterImage", imgUrl);
            intent2.putExtra("filterTitle", title);
            intent2.putExtra("nickname", nick);
            startActivityForResult(intent2, 1001);
        });
    }

    private void showPointChangePopUp() {
        int currentPrice = 0;
        try { currentPrice = Integer.parseInt(price); } catch (Exception ignored) {}

        // 가격 수정 다이얼로그 호출 (무료 필터 제한 없음)
        new PointChangeDialog(this, title, currentPrice, (oldPrice, newPrice) -> {
            requestUpdatePrice(Long.parseLong(filterId), newPrice);
        }).show();
    }

    // ✅ 2. 북마크 서버 API 호출 (토글 요청)
    private void requestToggleBookmark(long id) {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);

        api.toggleBookmark(id).enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean newState = response.body(); // 서버가 준 현재 상태 (true/false)

                    // UI 업데이트
                    updateBookmarkUI(newState);

                    // (선택) 토스트 메시지
                    String msg = newState ? "북마크에 저장되었습니다." : "북마크가 해제되었습니다.";
                    Toast.makeText(FilterInfoActivity.this, msg, Toast.LENGTH_SHORT).show();

                } else {
                    Log.e("FilterInfo", "북마크 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                Log.e("FilterInfo", "통신 오류", t);
            }
        });
    }

    // ✅ 3. 북마크 UI 업데이트 (아이콘 변경)
    private void updateBookmarkUI(boolean active) {
        this.isBookmarked = active; // 상태 변수 업데이트

        if (active) {
            // 북마크 된 상태 아이콘 (리소스 이름 확인 필요!)
            bookmark.setImageResource(R.drawable.icon_bookmark_no_blue);
        } else {
            // 북마크 해제된 상태 아이콘
            bookmark.setImageResource(R.drawable.icon_bookmark_no_gray);
        }
    }

    // ✅ [추가] 서버에 가격 수정 요청
    private void requestUpdatePrice(long id, int newPrice) {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);

        // Map으로 데이터 전송
        HashMap<String, Object> params = new HashMap<>();
        params.put("price", newPrice);

        api.updatePrice(id, params).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    price = String.valueOf(newPrice);
                    isFree = (newPrice == 0);
                    updateButtonState();
                    Toast.makeText(FilterInfoActivity.this, "가격이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("FilterInfo", "가격 수정 실패: " + response.code());
                    Toast.makeText(FilterInfoActivity.this, "가격 수정 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("FilterInfo", "통신 오류", t);
                Toast.makeText(FilterInfoActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ 모달 설정
    private void setupModal() {
        FrameLayout rootView = findViewById(R.id.modalOff);

        chooseUseModeOn = getLayoutInflater().inflate(R.layout.f_choose_use_mode, null);
        chooseUseMode = chooseUseModeOn.findViewById(R.id.chooseUseMode);
        galleryModeBtn = chooseUseModeOn.findViewById(R.id.galleryModeBtn);
        cameraModeBtn = chooseUseModeOn.findViewById(R.id.cameraModeBtn);
        alertTxt = chooseUseModeOn.findViewById(R.id.alertTxt);

        buyFilterOn = getLayoutInflater().inflate(R.layout.f_buy_filter, null);
        buyFilter = buyFilterOn.findViewById(R.id.buyFilter);
        point = buyFilterOn.findViewById(R.id.point);
        currentPoint1 = buyFilterOn.findViewById(R.id.currentPoint1);
        buyBtn = buyFilterOn.findViewById(R.id.buyBtn);

        buyFilterSuccessOn = getLayoutInflater().inflate(R.layout.f_buy_filter_success, null);
        buyFilterSuccess = buyFilterSuccessOn.findViewById(R.id.buyFilterSuccess);
        currentPoint2 = buyFilterSuccessOn.findViewById(R.id.currentPoint2);
        useBtn = buyFilterSuccessOn.findViewById(R.id.useBtn);
        closeBtn = buyFilterSuccessOn.findViewById(R.id.closeBtn);

        dimBackground = new View(this);
        dimBackground.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dimBackground.setBackgroundColor(Color.parseColor("#B3000000"));
        dimBackground.setVisibility(View.GONE);

        rootView.addView(dimBackground);
        rootView.addView(chooseUseModeOn);
        rootView.addView(buyFilterOn);
        rootView.addView(buyFilterSuccessOn);

        chooseUseModeOn.setVisibility(View.GONE); chooseUseModeOn.setTranslationY(800);
        buyFilterOn.setVisibility(View.GONE); buyFilterOn.setTranslationY(800);
        buyFilterSuccessOn.setVisibility(View.GONE); buyFilterSuccessOn.setTranslationY(800);

        dimBackground.setOnClickListener(v -> hideModal());
        if(closeBtn != null) closeBtn.setOnClickListener(v -> hideModal());

        // 갤러리/카메라 선택
        galleryModeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            hideModal();
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        cameraModeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("color_adjustments", adj);
            intent.putExtra("brush_image_path", brushPath);
            intent.putExtra("stickerImageNoFacePath", stickerImageNoFacePath);
            if (faceStickers != null) intent.putExtra("face_stickers", new ArrayList<>(faceStickers));
            startActivity(intent);
            hideModal();
        });

        // 구매 버튼
        if(buyBtn != null) {
            buyBtn.setOnClickListener(v -> {
                if (ClickUtils.isFastClick(v, 400)) return;

                // 1. 로컬 포인트 체크 (사전 검증)
                SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
                int current = sp.getInt("current_point", 0);
                int priceInt = 0;
                try { priceInt = Integer.parseInt(price); } catch(Exception e){}

                if (current < priceInt) {
                    Toast.makeText(this, "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ✅ [수정] 2. 서버 구매 API 호출
                requestPurchaseFilter(Long.parseLong(filterId), current, priceInt);
            });
        }
    }

    // ✅ [추가] 서버에 구매/사용 요청
    private void requestPurchaseFilter(long id, int currentPoint, int priceInt) {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);

        api.useFilter(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // 1. 성공 시 로컬 포인트 차감 및 저장
                    int newCurrent = currentPoint - priceInt;
                    SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
                    sp.edit().putInt("current_point", newCurrent).apply();

                    // 2. 상태 변경
                    isBuy = true;
                    updateButtonState();

                    // 3. 모달 전환 애니메이션 (구매 -> 성공)
                    buyFilterOn.setVisibility(View.GONE);
                    if (currentPoint2 != null) currentPoint2.setText(newCurrent + "P");
                    showModal(buyFilterSuccessOn);

                    Log.d("FilterInfo", "구매 성공");
                } else {
                    // 실패 시 (이미 구매했거나, 포인트 부족 등 서버 에러)
                    Log.e("FilterInfo", "구매 실패: " + response.code());
                    Toast.makeText(FilterInfoActivity.this, "구매에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("FilterInfo", "통신 오류", t);
                Toast.makeText(FilterInfoActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showModal(View view) {
        isModalVisible = true;
        dimBackground.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);
        view.animate().translationY(0).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    private void hideModal() {
        View tempTarget = null;
        if (chooseUseModeOn.getVisibility() == View.VISIBLE) tempTarget = chooseUseModeOn;
        else if (buyFilterOn.getVisibility() == View.VISIBLE) tempTarget = buyFilterOn;
        else if (buyFilterSuccessOn.getVisibility() == View.VISIBLE) tempTarget = buyFilterSuccessOn;

        if (tempTarget != null) {
            final View target = tempTarget;
            target.animate().translationY(800).setDuration(250).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    target.setVisibility(View.GONE);
                    dimBackground.setVisibility(View.GONE);
                    isModalVisible = false;
                }
            }).start();
        } else {
            dimBackground.setVisibility(View.GONE);
            isModalVisible = false;
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
                        loadOriginalImage(originalPath);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (imgUrl != null) {
                        v.setPressed(false);
                        originalBtn.setAlpha(1f);
                        Glide.with(FilterInfoActivity.this).load(imgUrl).dontAnimate().fitCenter().into(img);
                    }
                    return true;
            }
            return true;
        });
    }

    private void loadOriginalImage(String path) {
        Glide.with(this).asBitmap().load(path).dontAnimate().fitCenter().into(new CustomTarget<Bitmap>() {
            @Override public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) { img.setImageBitmap(resource); }
            @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupOriginalButton();
        if (dimBackground != null && dimBackground.getVisibility() == View.VISIBLE) hideModal();

        String key = (filterId != null && !filterId.isEmpty()) ? filterId : (nick + "_" + title);
        List<ReviewItem> reviews = ReviewStore.getReviews(key);
        int size = reviews.size();
        if (reviewCount != null) reviewCount.setText("리뷰 (" + size + ")");

        if (noReviewTxt != null) noReviewTxt.setVisibility(size == 0 ? View.VISIBLE : View.GONE);
        if (reviewBox1 != null) reviewBox1.setVisibility(size > 0 && size <= 4 ? View.VISIBLE : View.GONE);
        if (reviewBox2 != null) reviewBox2.setVisibility(size > 4 ? View.VISIBLE : View.GONE);

        if (size > 0 && size <= 4) {
            if(rb1Img1 != null) { rb1Img1.setVisibility(View.INVISIBLE); if(size >= 1) { rb1Img1.setVisibility(View.VISIBLE); Glide.with(this).load(reviews.get(0).imageUrl).into(rb1Img1); } }
            if(rb1Img2 != null) { rb1Img2.setVisibility(View.INVISIBLE); if(size >= 2) { rb1Img2.setVisibility(View.VISIBLE); Glide.with(this).load(reviews.get(1).imageUrl).into(rb1Img2); } }
        } else if (size > 4) {
            ImageView[] ivs = {rb2Img1, rb2Img2, rb2Img3, rb2Img4, rb2Img5};
            for(int i=0; i<5; i++) if(ivs[i] != null) Glide.with(this).load(reviews.get(i).imageUrl).into(ivs[i]);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent mainIntent = new Intent(FilterInfoActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainIntent);
        finish();
    }
}