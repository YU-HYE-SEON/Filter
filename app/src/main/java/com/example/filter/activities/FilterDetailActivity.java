package com.example.filter.activities;

import android.content.Intent;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.LeadingMarginSpan;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class FilterDetailActivity extends BaseActivity {
    private ImageButton backBtn, shareBtn, originalBtn;
    private TextView nickname, reportBtn, filterTitle, moreBtn, noReviewTxt;
    private TextView tag1, tag2, tag3, tag4, tag5;
    private TextView saveCount, useCount, reviewCount;
    private ImageView img, bookmark;
    private LinearLayout reviewBox1, reviewBox2;
    private ImageView rb1Img1, rb1Img2, rb2Img1, rb2Img2, rb2Img3, rb2Img4, rb2Img5;
    private ConstraintLayout tagBox, btnBox;
    private AppCompatButton buyBtn, useBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_filter_detail);
        backBtn = findViewById(R.id.backBtn);
        shareBtn = findViewById(R.id.shareBtn);
        originalBtn = findViewById(R.id.originalBtn);
        nickname = findViewById(R.id.nickname);
        reportBtn = findViewById(R.id.reportBtn);
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
        buyBtn = findViewById(R.id.buyBtn);
        useBtn = findViewById(R.id.useBtn);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int btnBoxBottom = btnBox.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(btnBox, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), btnBoxBottom + nav.bottom);
            return insets;
        });

        Intent intent = getIntent();
        if (intent != null) {
            String imgUrl = intent.getStringExtra("imgUrl");
            String title = intent.getStringExtra("filterTitle");
            String price = intent.getStringExtra("price");
            String nick = intent.getStringExtra("nickname");
            int count = intent.getIntExtra("count", 0);

            if (imgUrl != null) Glide.with(this).load(imgUrl).fitCenter().into(img);
            if (title != null) filterTitle.setText(title);
            if (price != null) buyBtn.setText("구매 : " + price + "P");
            if (nick != null) nickname.setText(nick);
            useCount.setText(String.valueOf(count));
        }

        applyHashtagHanging(tag1);
        applyHashtagHanging(tag2);
        applyHashtagHanging(tag3);
        applyHashtagHanging(tag4);
        applyHashtagHanging(tag5);

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            finish();
        });
    }

    private void applyHashtagHanging(TextView tv) {
        CharSequence original = tv.getText();
        if (original == null) return;

        int indent = (int) Math.ceil(tv.getPaint().measureText("#"));

        SpannableString span = new SpannableString(original);
        span.setSpan(new LeadingMarginSpan.Standard(0, indent), 0, span.length(), 0);
        tv.setText(span, TextView.BufferType.SPANNABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tv.setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE);
            tv.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        }

        tv.setSingleLine(false);
        tv.setEllipsize(null);
        tv.setHorizontallyScrolling(false);
        tv.setIncludeFontPadding(false);

        tv.requestLayout();
    }
}