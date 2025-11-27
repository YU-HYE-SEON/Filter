package com.example.filter.activities.review;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.MainActivity;
import com.example.filter.activities.apply.ApplyFilterActivity;
import com.example.filter.activities.apply.CameraActivity;
import com.example.filter.activities.filterinfo.FilterInfoActivity;
import com.example.filter.adapters.ReviewInfoAdapter;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.api_datas.response_dto.ReviewResponse;
import com.example.filter.apis.ReviewApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.etc.ClickUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewInfoActivity extends BaseActivity {
    private ImageButton backBtn;
    private RecyclerView recyclerView;
    private ReviewInfoAdapter adapter;
    //private List<ReviewItem> reviewList = new ArrayList<>();
    private ConstraintLayout use;
    private AppCompatButton useBtn;
    private Long reviewId;
    private String filterId;
    private Long filterIdLong;
    private int currentPage = 0;
    private boolean isLastPage = false;
    private boolean isLoading = false;
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri photoUri = result.getData().getData();
            if (photoUri != null) {
                Intent intent = new Intent(ReviewInfoActivity.this, ApplyFilterActivity.class);
                intent.setData(photoUri);
                intent.putExtra("filterId", filterId);

                startActivity(intent);
            } else {
                Toast.makeText(this, "사진 선택 실패: URI 없음", Toast.LENGTH_SHORT).show();
            }
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("Review", "[Review Info Activity] onCreate called");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_review_info);
        backBtn = findViewById(R.id.backBtn);
        recyclerView = findViewById(R.id.recyclerView);
        use = findViewById(R.id.use);
        useBtn = findViewById(R.id.useBtn);

        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(lm);
        adapter = new ReviewInfoAdapter();
        recyclerView.setAdapter(adapter);

        filterId = getIntent().getStringExtra("filterId");
        filterIdLong = filterId != null ? Long.parseLong(filterId) : null;
        loadReviews(filterIdLong);

        reviewId = getIntent().getLongExtra("reviewId", -1);
        //loadReviews(reviewId);

        adapter.setOnItemDeleteListener((reviewId, position) -> {
            deleteReview(reviewId, position);
        });

        use.post(() -> {
            int useHeight = use.getHeight();
            recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(), recyclerView.getPaddingRight(), useHeight);
        });

        /*ClickUtils.clickDim(useBtn);
        useBtn.setOnClickListener(v -> {

        });*/

        backBtn.setOnClickListener(v -> {
            finish();
        });
    }

    private void loadReviews(long filterId) {
        if (isLoading || isLastPage) return;
        isLoading = true;

        ReviewApi api = AppRetrofitClient.getInstance(this).create(ReviewApi.class);
        api.getReviewsByFilter(filterId, currentPage, 20).enqueue(new Callback<PageResponse<ReviewResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<ReviewResponse>> call, Response<PageResponse<ReviewResponse>> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<ReviewResponse> pageData = response.body();
                    List<ReviewResponse> reviews = pageData.content;

                    if (reviews != null && !reviews.isEmpty()) {
                        adapter.addItems(reviews);
                        currentPage++;
                    }

                    isLastPage = pageData.last;

                } else {
                    Log.e("ReviewList", "목록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ReviewResponse>> call, Throwable t) {
                isLoading = false;
                Log.e("ReviewList", "통신 오류", t);
            }
        });
    }

    private void deleteReview(long reviewId, int position) {
        ReviewApi api = AppRetrofitClient.getInstance(this).create(ReviewApi.class);

        api.deleteReview(reviewId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.e("리뷰삭제", "리뷰 삭제 성공");
                    adapter.removeItem(position);
                    Toast.makeText(ReviewInfoActivity.this, "리뷰가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("리뷰삭제", "리뷰 삭제 실패 : " + response.code());
                    Toast.makeText(ReviewInfoActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("리뷰삭제", "통신 오류", t);
                Toast.makeText(ReviewInfoActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
