package com.example.filter.activities.review;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.adapters.ReviewAdapter;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.GridSpaceItemDecoration;
import com.example.filter.etc.ReviewStore;
import com.example.filter.items.ReviewItem;

import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends BaseActivity {
    private ImageButton backBtn;
    ImageView img;
    TextView title, nick;
    private RecyclerView recyclerView;
    private TextView textView;
    private ReviewAdapter adapter;
    private String filterId, filterImage, filterTitle, nickname, reviewImg, reviewNick, reviewSnsId;
    private List<ReviewItem> reviewList = new ArrayList<>();
    private final ActivityResultLauncher<Intent> detailLauncher =
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
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_review);
        backBtn = findViewById(R.id.backBtn);
        img = findViewById(R.id.filterImage);
        title = findViewById(R.id.filterTitle);
        nick = findViewById(R.id.nickname);
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
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(12), dp(18)));

        filterId = getIntent().getStringExtra("filterId");
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
            nick.setText(nickname);
        }

        reviewList = ReviewStore.getReviews(key);
        if (reviewImg != null && !reviewImg.isEmpty()) {
            ReviewItem newItem = new ReviewItem(reviewImg, reviewNick, reviewSnsId);
            ReviewStore.addReview(key, newItem);
        }

        adapter.setItems(reviewList);
        updateRecyclerVisibility();

        adapter.setOnItemClickListener((v, item) -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            Intent i = new Intent(ReviewActivity.this, ReviewInfoActivity.class);
            i.putExtra("reviewImg", item.imageUrl);
            i.putExtra("reviewNick", item.nickname);
            i.putExtra("reviewSnsId", item.snsId);
            detailLauncher.launch(i);
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
