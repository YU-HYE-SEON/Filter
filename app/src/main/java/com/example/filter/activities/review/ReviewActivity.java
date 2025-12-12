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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.filterinfo.FilterInfoActivity;
import com.example.filter.adapters.ReviewAdapter;
import com.example.filter.api_datas.response_dto.FilterResponse;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.api_datas.response_dto.ReviewResponse;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.ReviewApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.GridSpaceItemDecoration;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewActivity extends BaseActivity {
    private boolean isDataLoading = false;
    private boolean loadingAnimFinishedOnce = false;
    private int loadingAnimPlayCount = 0;
    private final int MIN_PLAY_COUNT = 1;
    private FrameLayout loadingContainer;
    private LottieAnimationView loadingAnim;

    private ImageButton backBtn;
    private String nick, imgUrl, title;
    ImageView img;
    TextView filterTitle, filterNickName;
    private RecyclerView recyclerView;
    private TextView textView;
    private ReviewAdapter adapter;
    private String filterId;
    private Long filterIdLong;
    private int nextPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_review);
        backBtn = findViewById(R.id.backBtn);
        img = findViewById(R.id.filterImage);
        filterTitle = findViewById(R.id.filterTitle);
        filterNickName = findViewById(R.id.nickname);
        recyclerView = findViewById(R.id.recyclerView);
        textView = findViewById(R.id.textView);
        loadingContainer = findViewById(R.id.loadingContainer);
        loadingAnim = findViewById(R.id.loadingAnim);
        loadingContainer.setVisibility(View.GONE);

        backBtn.setOnClickListener(v -> {
            moveToFilterInfo();
        });

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        adapter = new ReviewAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(10), dp(10)));

        // 상단에 띄우는 필터 기본 정보를 받아오기
        filterId = getIntent().getStringExtra("filterId");
        if (filterId != null) {
            loadFilterInfo(Long.parseLong(filterId));
        }

        // 리뷰 목록 불러오기
        filterIdLong = filterId != null ? Long.parseLong(filterId) : null;
        showLoading();
        loadReviews(filterIdLong, 0);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateRecyclerVisibility();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateRecyclerVisibility();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateRecyclerVisibility();
            }
        });

        updateRecyclerVisibility();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                if (!recyclerView.canScrollVertically(1)) {
                    loadReviews(filterIdLong, nextPage);
                }
            }
        });

        adapter.setOnItemClickListener((v, item, position) -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            // 리뷰 상세 정보 페이지로 이동
            Intent intent = new Intent(this, ReviewInfoActivity.class);
            intent.putExtra("filterId", filterId);
            intent.putExtra("reviewId", String.valueOf(item.id));
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    /**
     * 필터 상세 정보 불러오기 (리뷰 목록 페이지의 헤더를 위해서)
     */
    private void loadFilterInfo(long id) {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);
        api.getFilter(id).enqueue(new Callback<FilterResponse>() {
            @Override
            public void onResponse(Call<FilterResponse> call, Response<FilterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    setFilterData(response.body());
                } else {
                    Log.e("Review", "리뷰액티비티 | 상세 조회 실패: " + response.code());
                    Toast.makeText(ReviewActivity.this, "정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FilterResponse> call, Throwable t) {
                Log.e("Review", "통신 오류", t);
            }
        });
    }

    /**
     * 리뷰 목록 페이지 헤더의 '필터 정보' 내용 채우기
     */
    private void setFilterData(FilterResponse data) {
        this.title = data.name;
        this.nick = data.creator;
        this.imgUrl = data.editedImageUrl;

        if (title != null) filterTitle.setText(title);
        if (nick != null) filterNickName.setText(nick);
        if (imgUrl != null) Glide.with(ReviewActivity.this).load(imgUrl).fitCenter().into(img);
    }

    /**
     * 리뷰 목록 불러오기
     */
    private void loadReviews(long filterId, int page) {
        if (isLoading || isLastPage) return;
        isLoading = true;

        ReviewApi api = AppRetrofitClient.getInstance(this).create(ReviewApi.class);
        api.getReviewsByFilter(filterId, page, 20).enqueue(new Callback<PageResponse<ReviewResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<ReviewResponse>> call, Response<PageResponse<ReviewResponse>> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<ReviewResponse> pageData = response.body();
                    List<ReviewResponse> reviews = pageData.content;

                    if (reviews == null || reviews.isEmpty()) {
                        isLastPage = true;

                        hideLoading();

                        return;
                    }

                    adapter.addItems(reviews);
                    nextPage++;

                    if (reviews.size() < 20) {
                        isLastPage = true;
                    }

                } else {
                    Log.e("ReviewList", "리뷰액티비티 | 목록 조회 실패: " + response.code());
                }
                // 업데이트 반영 후, 가시성 갱신
                updateRecyclerVisibility();

                isDataLoading = false;
                checkAndHideLoading();
            }

            @Override
            public void onFailure(Call<PageResponse<ReviewResponse>> call, Throwable t) {
                isLoading = false;
                Log.e("ReviewList", "통신 오류", t);

                isDataLoading = false;
                checkAndHideLoading();
            }
        });
    }

    private void updateRecyclerVisibility() {
        if (adapter.getItemCount() == 0) {
            textView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        } else {
            textView.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void moveToFilterInfo(){
        Intent intent = new Intent(ReviewActivity.this, FilterInfoActivity.class);
        intent.putExtra("filterId", filterId);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveToFilterInfo();
    }

    private void showLoading() {
        isDataLoading = true;
        loadingAnimPlayCount = 0;
        loadingAnimFinishedOnce = false;

        loadingContainer.setVisibility(View.VISIBLE);
        loadingAnim.setProgress(0f);
        loadingAnim.setSpeed(2.5f);

        loadingAnim.addAnimatorUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            if (progress >= 0.33f && !isDataLoading && !loadingAnimFinishedOnce) {
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
        if (!isDataLoading && loadingAnimPlayCount >= MIN_PLAY_COUNT) {
            if (!loadingAnimFinishedOnce) {
                loadingAnimFinishedOnce = true;
                hideLoading();
            }
        }
    }
}