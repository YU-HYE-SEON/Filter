package com.example.filter.activities.review;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
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
    private ReviewResponse reviewResponse;
    private ImageButton backBtn;
    private String nick, imgUrl,title;
    ImageView img;
    TextView filterTitle, filterNickName;
    private RecyclerView recyclerView;
    private TextView textView;
    private ReviewAdapter adapter;
    //private List<ReviewItem> reviewList = new ArrayList<>();
    private String filterId;
    private Long filterIdLong;
    private int currentPage = 0;
    private boolean isLastPage = false;
    private boolean isLoading = false;
    //private String filterId, filterImage, filterTitle, nickname, reviewImg, reviewNick, reviewSnsId;
    /*private final ActivityResultLauncher<Intent> detailLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String deletedUrl = result.getData().getStringExtra("deleted_review_url");
                    if (deletedUrl != null) {
                        String key = (filterId != null && !filterId.isEmpty()) ? filterId : nickname + "_" + filterTitle;
                        ReviewStore.removeReview(key, deletedUrl);
                        adapter.removeItem(deletedUrl);
                        updateRecyclerVisibility();
                    }
                }
            });*/

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

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        adapter = new ReviewAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(10), dp(10)));

        /*filterId = getIntent().getStringExtra("filterId");
        filterImage = getIntent().getStringExtra("filterImage");
        filterTitle = getIntent().getStringExtra("filterTitle");
        nickname = getIntent().getStringExtra("nickname");

        reviewImg = getIntent().getStringExtra("reviewImg");
        reviewNick = getIntent().getStringExtra("reviewNick");
        reviewSnsId = getIntent().getStringExtra("reviewSnsId");

        String key = null;
        if (filterId != null && !filterId.isEmpty()) {
            key = filterId;
        } else {
            key = nickname + "_" + filterTitle;
        }
        if (filterImage != null && !filterImage.isEmpty()) {
            Glide.with(this).load(filterImage).fitCenter().into(img);
        }
        if (filterTitle != null && !filterTitle.isEmpty()) {
            title.setText(filterTitle);
        }
        if (nickname != null && !nickname.isEmpty()) {
            filterNickName.setText(nickname);
        }

        reviewList = ReviewStore.getReviews(key);
        if (reviewImg != null && !reviewImg.isEmpty()) {
            ReviewItem newItem = new ReviewItem(reviewImg, reviewNick, reviewSnsId);
            ReviewStore.addReview(key, newItem);
        }*/

        filterId = getIntent().getStringExtra("filterId");
        if (filterId != null) {
            loadFilterInfo(Long.parseLong(filterId));
        }

        filterIdLong = filterId != null ? Long.parseLong(filterId) : null;

        reviewResponse = (ReviewResponse) getIntent().getSerializableExtra("review_response");

        if (reviewResponse != null) {
            adapter.addItem(reviewResponse);
        }

        loadReviews(filterIdLong);

        updateRecyclerVisibility();

        adapter.setOnItemClickListener((v, item) -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Intent intent = new Intent(this, ReviewInfoActivity.class);
            startActivity(intent);
        });
    }

    private void loadFilterInfo(long id) {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);
        api.getFilter(id).enqueue(new Callback<FilterResponse>() {
            @Override
            public void onResponse(Call<FilterResponse> call, Response<FilterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    setFilterData(response.body());
                } else {
                    Log.e("FilterInfo", "리뷰액티비티 | 상세 조회 실패: " + response.code());
                    Toast.makeText(ReviewActivity.this, "정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FilterResponse> call, Throwable t) {
                Log.e("FilterInfo", "통신 오류", t);
            }
        });
    }

    private void setFilterData(FilterResponse data) {
        this.title = data.name;
        this.nick = data.creator;
        this.imgUrl = data.editedImageUrl;

        if (title != null) filterTitle.setText(title);
        if (nick != null) filterNickName.setText(nick);
        if (imgUrl != null) Glide.with(ReviewActivity.this).load(imgUrl).fitCenter().into(img);

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
                    Log.e("ReviewList", "리뷰액티비티 | 목록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ReviewResponse>> call, Throwable t) {
                isLoading = false;
                Log.e("ReviewList", "통신 오류", t);
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
}
