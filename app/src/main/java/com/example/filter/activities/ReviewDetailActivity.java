package com.example.filter.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class ReviewDetailActivity extends BaseActivity {
    private ImageButton backBtn;
    private ImageView img;
    private TextView nickname, snsId;
    private String reviewImg, reviewNick, reviewSnsId;
    private ScrollView scrollView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_review_detail);
        backBtn = findViewById(R.id.backBtn);
        img = findViewById(R.id.img);
        nickname = findViewById(R.id.nickname);
        snsId = findViewById(R.id.snsId);
        scrollView = findViewById(R.id.scrollView);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int btnBoxBottom = scrollView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), btnBoxBottom + nav.bottom);
            return insets;
        });

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
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
