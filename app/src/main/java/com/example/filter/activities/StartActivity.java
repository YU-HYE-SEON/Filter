package com.example.filter.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
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
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;
import com.example.filter.dialogs.PopUpDialog;
import com.example.filter.dialogs.SignUpDialog;
import com.example.filter.etc.AuthApi;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.TokenRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;


import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StartActivity extends BaseActivity {
    private GoogleSignInOptions gso;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;
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

        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    try {
                        /*Log.d("GoogleLogin", "resultCode = " + result.getResultCode());

                        if (result.getData() == null) {
                            Log.e("GoogleLogin", "result.getData() == null");
                        } else {
                            Log.d("GoogleLogin", "result.getData(): " + result.getData().toString());
                        }*/

                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Task<GoogleSignInAccount> task =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleSignInResult(task);
                        } else {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            if (task.isComplete()) {
                                Exception e = task.getException();
                                if (e != null) Log.e("GoogleLogin", "로그인 실패 원인", e);
                            } else {
                                Log.e("GoogleLogin", "ActivityResult 실패 또는 취소됨 (사용자가 뒤로가기 눌렀거나 OAuth 오류)");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("GoogleLogin", "ActivityResult 처리 중 예외 발생", e);
                    }
                });

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

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

                        //float cx = bg.getWidth() / 2f;
                        //float r0 = 1f;
                        //float r1 = bg.getWidth() * 2.5f;
                        //bgDrawable.setStretch(bg.getWidth(), 1.0f);

                        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
                        anim.setDuration(1500);
                        anim.setInterpolator(new AccelerateDecelerateInterpolator());
                        anim.addUpdateListener(a -> {
                            float t = (float) a.getAnimatedValue();
                            float y = bg.getHeight() / 2f;
                            float bgY = (y) + (-y * 1.7f) * t;
                            //float bgY = (y) + (-y * 2.5f) * t;
                            float radius = bg.getWidth() + bg.getWidth() * t;
                            //float radius = r0 + (r1 - r0) * t;
                            float glow = new DecelerateInterpolator(2f).getInterpolation(Math.min(1f, t * 1.6f));

                            bgDrawable.update(bg.getWidth() / 2f, bgY, radius, glow);
                            //bgDrawable.update(cx, bgY, radius, glow);
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
            if (ClickUtils.isFastClick(800)) return;
            googleLogin.setEnabled(false);
            signIn();

            /*if (!isSignUp && !isLogin) {
                showSignUpDialog();
            } else if (!isLogin) {
                loginFail();
            } else if (isLogin) {
                loginSuccess();
            }*/
        });

        kakaoTalkLogin.setOnClickListener(v -> {
            /*if (!isSignUp && !isLogin) {
                showSignUpDialog();
            } else if (!isLogin) {
                loginFail();
            } else if (isLogin) {
                loginSuccess();
            }*/
        });
    }

    private void signIn() {
        try {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            if (signInIntent == null) {
                Log.e("GoogleLogin", "signInIntent가 null입니다. GoogleSignInClient가 초기화되지 않았을 가능성");
                return;
            }
            signInLauncher.launch(signInIntent);
        } catch (Exception e) {
            Log.e("GoogleLogin", "signIn() 중 예외 발생", e);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            String idToken = account.getIdToken();
            //String email = account.getEmail();
            //String displayName = account.getDisplayName();

            if (account == null || idToken == null) {
                Log.e("GoogleLogin", "GoogleSignInAccount 혹은 idToken이 null");
                return;
            }

            if (account != null) {
                Log.d("GoogleLogin", "ID Token: " + idToken);

                getSharedPreferences("Auth", MODE_PRIVATE)
                        .edit()
                        .putString("idToken", idToken)
                        .apply();
            }

            Log.d("Go//////ogleLogin", "GoogleSignIn 성공");
            Log.d("GoogleLogin", "idToken: " + idToken);

            sendTokenToBackend(idToken);

        } catch (ApiException e) {
            Log.e("GoogleLogin", "GoogleSignIn 실패", e);
        }
    }

    private void sendTokenToBackend(String idToken) {
        Log.d("GoogleLogin", "백엔드 전송 시작");

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.124.105.243/")  // 꼭 슬래시 포함
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthApi api = retrofit.create(AuthApi.class);

        TokenRequest body = new TokenRequest(idToken);
        Log.d("GoogleLogin", "요청 바디: " + new Gson().toJson(body));

        Call<ResponseBody> call = api.verifyGoogleToken(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                googleLogin.setEnabled(true);
                Log.d("GoogleLogin", "Retrofit 응답 수신됨");
                //Log.d("GoogleLogin", "HTTP 코드: " + response.code());

                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Log.e("GoogleLogin", "서버가 보낸 실제 응답 (HTML 예상): " + responseBody);
                        JSONObject json = new JSONObject(responseBody);
                        //loginFail();

                        boolean hasNickname = json.optBoolean("hasNickname", false);
                        boolean loginSuccess = json.optBoolean("loginSuccess", false);

                        //닉네임 없음 → 회원가입 팝업
                        if (!hasNickname) {
                            isSignUp = false;
                            isLogin = false;
                            showSignUpDialog();
                            return;
                        }

                        //닉네임 있음, 로그인 성공
                        if (hasNickname && loginSuccess) {
                            isSignUp = true;
                            isLogin = true;
                            loginSuccess();
                            return;
                        }

                        //닉네임 있음, 로그인 실패
                        if (hasNickname && !loginSuccess) {
                            isSignUp = true;
                            isLogin = false;
                            loginFail();
                            return;
                        }

                    } catch (Exception e) {
                        Log.e("GoogleLogin", "JSON 파싱 실패", e);
                        loginFail();
                    }
                } else {
                    Log.e("GoogleLogin", "서버 에러 코드: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.e("GoogleLogin", "서버 에러 본문: " + response.errorBody().string());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                googleLogin.setEnabled(true);
                Log.e("GoogleLogin", "Retrofit 통신 실패", t);
                loginFail();
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

        public void update(float cx, float cy, float r, float g) {
            this.centerX = cx;
            this.centerY = cy;
            this.radius = r;
            this.glow = g;
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

            /*float halfW = radius * stretchX;
            float halfH = radius * stretchY;
            float left = centerX - halfW;
            float top = centerY - halfH;
            float right = centerX + halfW;
            float bottom = centerY + halfH;
            float softness = 0.15f;

            Shader fadeX = new LinearGradient(
                    left, centerY, right, centerY,
                    new int[]{Color.TRANSPARENT, accentWithA, accentWithA, Color.TRANSPARENT},
                    new float[]{0f, 0.5f - softness, 0.5f + softness, 1f},
                    Shader.TileMode.CLAMP
            );

            Shader fadeY = new LinearGradient(
                    centerX, top, centerX, bottom,
                    new int[]{Color.TRANSPARENT, accentWithA, accentWithA, Color.TRANSPARENT},
                    new float[]{0f, 0.5f - softness, 0.5f + softness, 1f},
                    Shader.TileMode.CLAMP
            );

            ComposeShader boxGlow = new ComposeShader(fadeX, fadeY, PorterDuff.Mode.MULTIPLY);
            glowPaint.setShader(boxGlow);

            canvas.drawRect(left, top, right, bottom, glowPaint);*/

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