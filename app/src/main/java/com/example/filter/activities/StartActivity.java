package com.example.filter.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;
import com.example.filter.dialogs.PopUpDialog;
import com.example.filter.dialogs.SignUpDialog;

public class StartActivity extends BaseActivity {
    private ConstraintLayout bg;
    private TextView txt, logo;
    private LinearLayout logoBox, btn;
    private AppCompatButton googleLogin, kakaoTalkLogin;
    private boolean isLogin = false;
    private boolean isSignUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_start);
        bg = findViewById(R.id.bg);
        logoBox = findViewById(R.id.logoBox);
        txt = findViewById(R.id.txt);
        logo = findViewById(R.id.logo);
        btn = findViewById(R.id.btn);
        googleLogin = findViewById(R.id.googleLogin);
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

                    bg.post(() -> {
                        BgGradientDrawable bgDrawable = new BgGradientDrawable();
                        bg.setBackground(bgDrawable);
                        bgDrawable.setStretch(1.6f, 1.0f);

                        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
                        anim.setDuration(1500);
                        anim.setInterpolator(new AccelerateDecelerateInterpolator());
                        anim.addUpdateListener(a -> {
                            float t = (float) a.getAnimatedValue();
                            float y = bg.getHeight() / 2f;
                            float bgY = (y) + (-y * 1.7f) * t;
                            float radius = bg.getWidth() + bg.getWidth() * t;
                            float glow = new DecelerateInterpolator(2f).getInterpolation(Math.min(1f, t * 1.6f));

                            bgDrawable.update(bg.getWidth() / 2f, bgY, radius, glow);
                        });
                        anim.start();
                    });

                    logo.post(() -> {
                        final int h = Math.max(1, logo.getHeight());
                        final int brand = Color.parseColor("#C2FA7A");
                        final ArgbEvaluator eval = new ArgbEvaluator();

                        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
                        anim.setDuration(1000);
                        anim.setInterpolator(new AccelerateDecelerateInterpolator());
                        anim.addUpdateListener(a -> {
                            float t = (float) a.getAnimatedValue();
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
                        anim.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                logo.getPaint().setShader(null);
                                logo.setTextColor(Color.WHITE);
                            }
                        });

                        anim.start();
                    });

                    logoBox.clearAnimation();
                    logoBox.startAnimation(moveUp);

                    return true;
                }
            });
        }, 1000);

        googleLogin.setOnClickListener(v -> {
            if (!isSignUp && !isLogin) {
                showSignUpDialog();
            } else if (!isLogin) {
                loginFail();
            } else if (isLogin) {
                loginSuccess();
            }
        });

        kakaoTalkLogin.setOnClickListener(v -> {
            if (!isSignUp && !isLogin) {
                showSignUpDialog();
            } else if (!isLogin) {
                loginFail();
            } else if (isLogin) {
                loginSuccess();
            }
        });
    }

    private void loginSuccess() {
        Intent intent = new Intent(StartActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void loginFail() {
        Intent intent = new Intent(StartActivity.this, LoginFailActivity.class);
        startActivity(intent);
    }

    private void showSignUpDialog() {
        new SignUpDialog(this, new SignUpDialog.SignUpDialogListener() {
            @Override
            public void onSignUp() {
                Intent intent = new Intent(StartActivity.this, SignUpActivity.class);
                startActivity(intent);
            }

            @Override
            public void onPopUp1() {
                showPopUp1Dialog();
            }

            @Override
            public void onPopUp2() {
                showPopUp2Dialog();
            }
        }).show();
    }

    private void showPopUp1Dialog() {
        new PopUpDialog(this, () -> {
        }).withTitle("이용약관")
                .withMessage("")
                .show();
    }

    private void showPopUp2Dialog() {
        new PopUpDialog(this, () -> {
        }).withTitle("개인정보처리방침")
                .withMessage("")
                .show();
    }

    public class BgGradientDrawable extends Drawable {
        private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int baseColor = Color.parseColor("#007AFF");
        private int accentColor = Color.parseColor("#C2FA7A");
        private float centerX, centerY, radius;
        private float glow = 0f;
        private float stretchX = 1f;
        private float stretchY = 1f;

        public BgGradientDrawable() {
            basePaint.setStyle(Paint.Style.FILL);
            basePaint.setColor(baseColor);
            glowPaint.setStyle(Paint.Style.FILL);
        }

        public void update(float cx, float cy, float r, float glow) {
            this.centerX = cx;
            this.centerY = cy;
            this.radius = r;
            this.glow = glow;
            invalidateSelf();
        }

        public void setStretch(float sx, float sy) {
            this.stretchX = sx;
            this.stretchY = sy;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawRect(getBounds(), basePaint);

            int a = (int) (255 * glow);
            int accentWithA = Color.argb(a, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));

            Shader shader = new RadialGradient(
                    centerX, centerY, radius,
                    new int[]{accentWithA, Color.TRANSPARENT},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            );
            glowPaint.setShader(shader);
            canvas.save();
            canvas.scale(stretchX, stretchY, centerX, centerY);
            canvas.drawCircle(centerX, centerY, radius, glowPaint);
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            basePaint.setAlpha(alpha);
            glowPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            basePaint.setColorFilter(cf);
            glowPaint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}