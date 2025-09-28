package com.example.filter.activities;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.filter.R;

public class StartActivity extends BaseActivity {
    private TextView txt, logo;
    private LinearLayout logoBox, btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_start);
        logoBox = findViewById(R.id.logoBox);
        txt = findViewById(R.id.txt);
        logo = findViewById(R.id.logo);
        btn = findViewById(R.id.btn);

        new Handler().postDelayed(() -> {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

            final int[] start = new int[2];
            logoBox.getLocationOnScreen(start);
            final int startY = start[1];

            txt.setVisibility(View.VISIBLE);
            btn.setVisibility(View.VISIBLE);
            txt.startAnimation(fadeIn);
            btn.startAnimation(fadeIn);

            final View root = findViewById(android.R.id.content);
            root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    root.getViewTreeObserver().removeOnPreDrawListener(this);
                    int[] end = new int[2];
                    logoBox.getLocationOnScreen(end);
                    int endY = end[1];
                    float dy = startY - endY;

                    TranslateAnimation moveUp = new TranslateAnimation(0f, 0f, dy, 0f);
                    moveUp.setDuration(1000);
                    moveUp.setInterpolator(new AccelerateDecelerateInterpolator());
                    moveUp.setFillAfter(true);

                    logoBox.clearAnimation();
                    logoBox.startAnimation(moveUp);

                    return true;
                }
            });
        }, 1000);
    }
}