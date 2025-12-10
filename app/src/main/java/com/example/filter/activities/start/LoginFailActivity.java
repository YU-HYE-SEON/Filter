package com.example.filter.activities.start;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.etc.ClickUtils;

public class LoginFailActivity extends BaseActivity {
    private ConstraintLayout email;
    private ImageButton btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_login_fail);
        email = findViewById(R.id.email);
        btn = findViewById(R.id.btn);

        ClickUtils.clickDim(btn);
        btn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            finish();
        });

        email.setOnClickListener(v -> {
            String uri = "mailto:feelem2025@gmail.com?subject=Feel'em에 문의하기";
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));

            try {
                startActivity(Intent.createChooser(emailIntent, "Feel'em에 문의하기"));
            } catch (Exception e) {
                Toast.makeText(this, "구글메일 앱이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}