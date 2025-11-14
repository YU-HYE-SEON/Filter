package com.example.filter.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class LoginFailActivity extends BaseActivity {
    private TextView txt;
    private ConstraintLayout email;
    private ImageButton btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_login_fail);
        txt = findViewById(R.id.txt);
        email = findViewById(R.id.email);
        btn = findViewById(R.id.btn);

        ClickUtils.clickDim(btn);
        btn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });
    }
}