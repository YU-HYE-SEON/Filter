package com.example.filter.activities.mypage;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;

public class SalesManageActivity extends BaseActivity {
    private ImageButton backBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_sales_manage);
        backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(v -> {
            finish();
        });
    }
}
