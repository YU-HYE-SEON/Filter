package com.example.filter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class ReviewDetailActivity extends BaseActivity {
    private ImageButton backBtn;
    private ImageView img;
    private TextView nickname, snsId, deleteBtn;
    private String reviewImg, reviewNick, reviewSnsId;
    private ScrollView scrollView;
    private ConstraintLayout use;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_review_detail);
        backBtn = findViewById(R.id.backBtn);
        deleteBtn = findViewById(R.id.deleteBtn);
        img = findViewById(R.id.img);
        nickname = findViewById(R.id.nickname);
        snsId = findViewById(R.id.snsId);
        scrollView = findViewById(R.id.scrollView);
        use = findViewById(R.id.use);

        use.post(() -> {
            int useHeight = use.getHeight();
            scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(), scrollView.getPaddingRight(), useHeight);
        });

        deleteBtn.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("deleted_review_url", reviewImg);
            setResult(RESULT_OK, result);
            finish();
        });

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });

        reviewImg = getIntent().getStringExtra("reviewImg");
        reviewNick = getIntent().getStringExtra("reviewNick");
        reviewSnsId = getIntent().getStringExtra("reviewSnsId");
        if (reviewNick == null) reviewNick = "닉네임";
        if (reviewSnsId == null) reviewSnsId = " @user_sns ";

        Glide.with(this).load(reviewImg).into(this.img);
        this.nickname.setText(reviewNick);
        this.snsId.setText(reviewSnsId);
    }
}
