package com.example.filter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;

public class LoginFailActivity extends BaseActivity {
    private TextView txt2, email;
    private AppCompatButton btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_login_fail);
        txt2 = findViewById(R.id.txt2);
        email = findViewById(R.id.email);
        btn = findViewById(R.id.btn);

        btn.setOnClickListener(v -> {
            finish();
        });
    }
}