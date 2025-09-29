package com.example.filter.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.filter.R;

public class StartActivity extends BaseActivity {
    private ConstraintLayout bg;
    private TextView txt, logo;
    private LinearLayout logoBox, btn;
    private AppCompatButton googleLogin, naverLogin, kakaoTalkLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_start);
        bg = findViewById(R.id.bg);
        logoBox = findViewById(R.id.logoBox);
        txt = findViewById(R.id.txt);
        logo = findViewById(R.id.logo);
        btn = findViewById(R.id.btn);
        googleLogin = findViewById(R.id.googleLogin);
        naverLogin = findViewById(R.id.naverLogin);
        kakaoTalkLogin = findViewById(R.id.kakaoTalkLogin);

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

                    logo.post(() -> {
                        final int h = Math.max(1, logo.getHeight());
                        final int brand = Color.parseColor("#C2FA7A");
                        final Interpolator interp = new AccelerateDecelerateInterpolator();
                        final ArgbEvaluator eval = new ArgbEvaluator();

                        ValueAnimator wipe = ValueAnimator.ofFloat(0f, 1f);
                        wipe.setDuration(1000);
                        wipe.setInterpolator(interp);
                        wipe.addUpdateListener(anim -> {
                            float t = (float) anim.getAnimatedValue();
                            float border = 1f - t;
                            float feather = 0.08f + 0.12f * t;
                            float p1 = Math.max(0f, border - feather);
                            float p2 = border;
                            float p3 = Math.min(1f, border + feather);
                            int mid1 = (int) eval.evaluate(0.35f, brand, Color.WHITE);
                            int mid2 = (int) eval.evaluate(0.85f, brand, Color.WHITE);

                            Shader shader = new LinearGradient(
                                    0, 0, 0, h,
                                    new int[]{brand, brand, mid1, mid2, Color.WHITE},
                                    new float[]{0f, p1, p2, p3, 1f},
                                    Shader.TileMode.CLAMP
                            );
                            logo.getPaint().setShader(shader);
                            logo.invalidate();
                        });
                        wipe.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                logo.getPaint().setShader(null);
                                logo.setTextColor(Color.WHITE);
                            }
                        });

                        wipe.start();
                    });

                    logoBox.clearAnimation();
                    logoBox.startAnimation(moveUp);

                    return true;
                }
            });
        }, 1000);
    }
}