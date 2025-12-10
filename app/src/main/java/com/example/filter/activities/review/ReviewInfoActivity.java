package com.example.filter.activities.review;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.api_datas.response_dto.ReviewResponse;
import com.example.filter.apis.ReviewApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.dialogs.ReviewDeleteDialog;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewInfoActivity extends BaseActivity {
    private boolean isDeleteLoading = false;
    private boolean loadingAnimFinishedOnce = false;
    private int loadingAnimPlayCount = 0;
    private final int MIN_PLAY_COUNT = 1;
    private FrameLayout loadingContainer;
    private LottieAnimationView loadingAnim;
    private ImageButton backBtn;
    private ImageView img, snsIcon;
    private TextView nickname, snsId, deleteBtn;
    private String reviewId;
    private Long reviewIdLong;
    private String filterId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("Review", "[Review Info Activity] onCreate called");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_review_info);
        backBtn = findViewById(R.id.backBtn);
        img = findViewById(R.id.img);
        snsIcon = findViewById(R.id.snsIcon);
        nickname = findViewById(R.id.nickname);
        snsId = findViewById(R.id.snsId);
        deleteBtn = findViewById(R.id.deleteBtn);

        loadingContainer = findViewById(R.id.loadingContainer);
        loadingAnim = findViewById(R.id.loadingAnim);
        loadingContainer.setVisibility(View.GONE);

        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            root.setPadding(0, 0, 0, nav.bottom);
            return insets;
        });

        filterId = getIntent().getStringExtra("filterId");

        reviewId = getIntent().getStringExtra("reviewId");
        reviewIdLong = reviewId != null ? Long.parseLong(reviewId) : null;
        loadReviews(reviewIdLong);

        deleteBtn.setOnClickListener(v -> {
            confirmDeleteReview(reviewIdLong);
        });

        backBtn.setOnClickListener(v -> {
            moveToReview();
        });
    }

    private void loadReviews(long reviewId) {
        ReviewApi api = AppRetrofitClient.getInstance(this).create(ReviewApi.class);
        api.getReviewById(reviewId).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(Call<ReviewResponse> call, Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReviewResponse data = response.body();

                    Glide.with(ReviewInfoActivity.this)
                            .load(data.imageUrl)
                            .fitCenter()
                            .into(img);

                    String socialType = data.socialType;
                    switch (socialType) {
                        case "INSTAGRAM":
                            snsIcon.setImageResource(R.drawable.btn_review_sns_insta_yes);
                            break;
                        case "X":
                            snsIcon.setImageResource(R.drawable.btn_review_sns_twitter_yes);
                            break;
                        case "NONE":
                        default:
                            snsIcon.setImageResource(R.drawable.btn_review_sns_none_yes);
                            break;
                    }

                    nickname.setText(data.reviewerNickname);

                    if (data.socialValue == null || data.socialValue.isEmpty() || data.socialType.equals("NONE")) {
                        snsId.setText("선택 안 함");
                    } else {
                        snsId.setText("@" + data.socialValue);
                    }

                    deleteBtn.setVisibility(data.isMine ? View.VISIBLE : View.GONE);

                } else {
                    Log.e("ReviewInfo", "목록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ReviewResponse> call, Throwable t) {
                Log.e("ReviewInfo", "통신 오류", t);
            }
        });
    }

    private void deleteReview(long reviewId) {
        ReviewApi api = AppRetrofitClient.getInstance(this).create(ReviewApi.class);

        api.deleteReview(reviewId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.e("리뷰삭제", "리뷰 삭제 성공");
                    Toast.makeText(ReviewInfoActivity.this, "리뷰가 삭제되었습니다.", Toast.LENGTH_SHORT).show();

                    moveToReview();
                } else {
                    Log.e("리뷰삭제", "리뷰 삭제 실패 : " + response.code());
                    Toast.makeText(ReviewInfoActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }

                isDeleteLoading = false;
                checkAndHideLoading();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("리뷰삭제", "통신 오류", t);
                Toast.makeText(ReviewInfoActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();


                isDeleteLoading = false;
                checkAndHideLoading();
            }
        });
    }

    private void confirmDeleteReview(long reviewId ) {
        new ReviewDeleteDialog(this, new ReviewDeleteDialog.ReviewDeleteDialogListener() {
            @Override
            public void onCancel() {
            }

            @Override
            public void onDelete() {
                showLoading();
                deleteReview(reviewId);
            }
        }).show();
    }

    private void showLoading() {
        isDeleteLoading = true;
        loadingAnimPlayCount = 0;
        loadingAnimFinishedOnce = false;

        loadingContainer.setVisibility(View.VISIBLE);
        loadingAnim.setProgress(0f);
        loadingAnim.setSpeed(2.5f);

        loadingAnim.addAnimatorUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            if (progress >= 0.33f && !isDeleteLoading && !loadingAnimFinishedOnce) {
                loadingAnimFinishedOnce = true;
                hideLoading();
            }
        });

        loadingAnim.playAnimation();
    }

    private void hideLoading() {
        if (loadingContainer.getVisibility() == View.VISIBLE) {
            loadingAnim.cancelAnimation();
            loadingContainer.setVisibility(View.GONE);
        }
    }

    private void checkAndHideLoading() {
        if (!isDeleteLoading && loadingAnimPlayCount >= MIN_PLAY_COUNT) {
            if (!loadingAnimFinishedOnce) {
                loadingAnimFinishedOnce = true;
                hideLoading();
            }
        }
    }

    private void moveToReview(){
        Intent intent = new Intent(ReviewInfoActivity.this, ReviewActivity.class);
        intent.putExtra("filterId", filterId);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveToReview();
    }
}