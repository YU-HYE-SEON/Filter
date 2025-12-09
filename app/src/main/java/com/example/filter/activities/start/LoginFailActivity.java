package com.example.filter.activities.start;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
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

        email.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.setPackage("com.google.android.gm");

            /// 메일 Feelem으로 바꾸기 ///
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"usunsun38@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feel'em에 문의하기");

            try {
                startActivity(Intent.createChooser(intent, "이메일 보내기"));
            } catch (Exception e) {
                Toast.makeText(this, "구글메일 앱이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}