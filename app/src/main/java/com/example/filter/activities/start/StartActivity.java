package com.example.filter.activities.start;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.MainActivity;
import com.example.filter.apis.UserApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.dialogs.PopUpDialog;
import com.example.filter.dialogs.SignUpDialog;
import com.example.filter.apis.AuthApi;
import com.example.filter.etc.ClickUtils;
import com.example.filter.api_datas.request_dto.TokenRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class StartActivity extends BaseActivity {
    private GoogleSignInOptions gso;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;
    private ConstraintLayout bg;
    private TextView txt;
    private ImageView logo;
    private ConstraintLayout btn;
    private ImageButton googleLogin, kakaoTalkLogin;

    private void TestGoogleSignOut() {
        // ✅ [1] GoogleSignInOptions 초기화
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // ✅ [2] 앱 실행 시 자동 로그아웃 (테스트용)
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    Log.d("GoogleLogin", "✅ 앱 실행 시 자동 로그아웃 완료 (테스트용)");

                    // SharedPreferences에 저장된 토큰/상태 초기화
                    getSharedPreferences("Auth", MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply();

                    Log.d("GoogleLogin", "✅ SharedPreferences(Auth) 초기화 완료");
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // ‼️ Test: 앱 실행 시 자동 로그아웃 (테스트용)
        TestGoogleSignOut();

        // UI 초기화
        setContentView(R.layout.a_start);
        bg = findViewById(R.id.bg);
        txt = findViewById(R.id.txt);
        logo = findViewById(R.id.logo);
        btn = findViewById(R.id.btn);
        googleLogin = findViewById(R.id.googleLogin);
        kakaoTalkLogin = findViewById(R.id.kakaoTalkLogin);

        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    try {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Task<GoogleSignInAccount> task =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleSignInResult(task);
                        } else {
                            googleLogin.setEnabled(true);
                            btn.setClickable(true);

                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            if (task.isComplete()) {
                                Exception e = task.getException();
                                if (e != null) Log.e("GoogleLogin", "로그인 실패 원인", e);
                            } else {
                                Log.e("GoogleLogin", "ActivityResult 실패 또는 취소됨 (사용자가 뒤로가기 눌렀거나 OAuth 오류)");
                            }
                        }
                    } catch (Exception e) {
                        googleLogin.setEnabled(true);
                        btn.setClickable(true);

                        Log.e("GoogleLogin", "ActivityResult 처리 중 예외 발생", e);
                    }
                });

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        float moveUp = 114 * getResources().getDisplayMetrics().density;
        long duration = 2500;

        new Handler().postDelayed(() -> {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

            txt.setVisibility(View.VISIBLE);
            btn.setVisibility(View.VISIBLE);
            txt.startAnimation(fadeIn);
            btn.startAnimation(fadeIn);

            ObjectAnimator logoAnimator = ObjectAnimator.ofFloat(logo, "translationY", -moveUp);
            logoAnimator.setDuration(duration);
            logoAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            logoAnimator.start();

            ObjectAnimator txtAnimator = ObjectAnimator.ofFloat(txt, "translationY", -moveUp);
            txtAnimator.setDuration(duration);
            txtAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            txtAnimator.start();

            Drawable logoLime = ContextCompat.getDrawable(this, R.drawable.logo_lime);
            Drawable logoWhite = ContextCompat.getDrawable(this, R.drawable.logo_white);
            ClipDrawable clipDrawable = new ClipDrawable(logoWhite, android.view.Gravity.BOTTOM, ClipDrawable.VERTICAL);
            Drawable[] layers = {logoLime, clipDrawable};
            LayerDrawable layerDrawable = new LayerDrawable(layers);
            logo.setImageDrawable(layerDrawable);

            ValueAnimator clipAnimator = ValueAnimator.ofInt(0, 10000);
            clipAnimator.setDuration(duration);
            clipAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            clipAnimator.addUpdateListener(animation -> {
                int level = (int) animation.getAnimatedValue();
                clipDrawable.setLevel(level);
            });
            clipAnimator.start();

            BgGradientDrawable bgDrawable = new BgGradientDrawable();
            bgDrawable.setStretch(1.6f, 1.0f);
            bg.postDelayed(() -> bg.setBackground(bgDrawable), 80);
            bg.post(() -> {
                float startCX = bg.getWidth() / 2f;
                float startCY = (bg.getHeight()) / 2f + logo.getHeight();
                float finalRadius = bg.getHeight() * 0.5f;
                float finalCenterY = -bg.getHeight() * 0.25f;

                AtomicBoolean gradientStarted = new AtomicBoolean(false);

                ValueAnimator moveAnim = ValueAnimator.ofFloat(0f, 1f);
                moveAnim.setDuration((int) (duration * 1.5f));
                moveAnim.setInterpolator(new DecelerateInterpolator(1.0f));
                moveAnim.addUpdateListener(a -> {
                    float t = (float) a.getAnimatedValue();
                    float currentY = startCY + (finalCenterY - startCY) * t;
                    bgDrawable.update(startCX, currentY, bgDrawable.radius, bgDrawable.glow);

                    if (t >= 0.9f && !gradientStarted.get()) {
                        gradientStarted.set(true);
                        Drawable finalGradient = ContextCompat.getDrawable(StartActivity.this, R.drawable.bg_gradient);
                        Drawable current = bg.getBackground();

                        Drawable[] layers2 = {current, finalGradient};
                        TransitionDrawable transition = new TransitionDrawable(layers2);

                        bg.setBackground(transition);
                        transition.startTransition(1200);
                    }
                });

                ValueAnimator sizeAnim = ValueAnimator.ofFloat(0f, 1f);
                sizeAnim.setDuration(duration);
                sizeAnim.setInterpolator(new DecelerateInterpolator(2.0f));
                sizeAnim.addUpdateListener(a -> {
                    float t = (float) a.getAnimatedValue();
                    float currentRadius = finalRadius * t;
                    float glow = (float) a.getAnimatedValue();
                    bgDrawable.update(startCX, bgDrawable.centerY, currentRadius, glow);

                    if (t >= 0.9f && !gradientStarted.get()) {
                        gradientStarted.set(true);
                        Drawable finalGradient = ContextCompat.getDrawable(StartActivity.this, R.drawable.bg_gradient);
                        Drawable current = bg.getBackground();

                        Drawable[] layers2 = {current, finalGradient};
                        TransitionDrawable transition = new TransitionDrawable(layers2);

                        bg.setBackground(transition);
                        transition.startTransition(1200);
                    }
                });

                moveAnim.start();
                sizeAnim.start();
            });
        }, 1000);

        googleLogin.setOnClickListener(v -> {
            googleLogin.setEnabled(false);
            googleLogin.setClickable(false);
            signIn();
        });

        kakaoTalkLogin.setOnClickListener(v -> {
        });

        ClickUtils.clickDim(googleLogin);
        ClickUtils.clickDim(kakaoTalkLogin);
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

            Log.d("GoogleLogin", "GoogleSignIn 성공");
            Log.d("GoogleLogin", "idToken: " + idToken);

            sendTokenToBackend(idToken);

        } catch (ApiException e) {
            Log.e("GoogleLogin", "GoogleSignIn 실패", e);
        }
    }

    /*
     * 백엔드에 ID 토큰 전송
     */
    private void sendTokenToBackend(String idToken) {
        Log.d("GoogleLogin", "✅ 백엔드에 구글 로그인 프로세스 시작");

        // 1. Retrofit 싱글톤 인스턴스 가져오기
        Retrofit retrofit = AppRetrofitClient.getInstance(this);
        AuthApi api = retrofit.create(AuthApi.class);

        // 2. 요청 바디 생성
        TokenRequest body = new TokenRequest(idToken);
        Log.d("GoogleLogin", "요청 바디: " + new Gson().toJson(body));

        // 3. 서버에 요청 보내기 (비동기)
        Call<ResponseBody> call = api.verifyGoogleToken(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                googleLogin.setEnabled(true);
                Log.d("GoogleLogin", "Retrofit 응답 수신됨");

                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Log.e("GoogleLogin", "서버 응답 본문: " + responseBody);

                        JSONObject json = new JSONObject(responseBody);
                        String accessToken = json.optString("accessToken", "");
                        String refreshToken = json.optString("refreshToken", "");

                        // ✅ 토큰 저장
                        if (!accessToken.isEmpty()) {
                            getSharedPreferences("Auth", MODE_PRIVATE)
                                    .edit()
                                    .putString("accessToken", accessToken)
                                    .putString("refreshToken", refreshToken)
                                    .apply();

                            Log.d("GoogleLogin", "토큰 저장 완료: " + accessToken);
                        }

                        // ✅ 로그인 성공 후 → 가입 여부 확인 추가
                        checkUserExists();  // ★ 추가된 부분 ★

                    } catch (Exception e) {
                        Log.e("GoogleLogin", "JSON 파싱 실패", e);
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
                btn.setClickable(true);
                Log.e("GoogleLogin", "Retrofit 통신 실패", t);
                loginFail();
            }
        });
    }

    // ✅ [새로 추가된 메서드]
    private void checkUserExists() {
        Log.d("UserCheck", "✅ 가입된 회원 여부 확인 시작");

        Retrofit retrofit = AppRetrofitClient.getInstance(this);
        UserApi userApi = retrofit.create(UserApi.class);

        Call<ResponseBody> call = userApi.checkUserExists();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(responseBody);
                        boolean exists = json.optBoolean("exists", false);

                        if (exists) {
                            Log.d("UserCheck", "✔️ 기존 회원 → MainActivity 이동");
                            loginSuccess();
                        } else {
                            Log.d("UserCheck", "🆕 신규 회원 → SignUpActivity 이동");
                            showSignUpDialog();
                        }
                    } catch (Exception e) {
                        Log.e("UserCheck", "JSON 파싱 실패", e);
                    }
                } else {
                    Log.e("UserCheck", "회원 여부 확인 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("UserCheck", "서버 연결 실패", t);
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
            public void onClose() {

            }

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
        private int accentColor = Color.parseColor("#C2FA7A");
        private int middleColor1 = Color.parseColor("#B8F684");
        private int middleColor2 = Color.parseColor("#AEF18D");
        private int middleColor3 = Color.parseColor("#99E8A0");
        private int middleColor4 = Color.parseColor("#84DFB3");
        private int middleColor5 = Color.parseColor("#6FD6C6");
        private int middleColor6 = Color.parseColor("#38A8E3");
        private int baseColor = Color.parseColor("#007AFF");
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
            int middleWithA1 = Color.argb(a, Color.red(middleColor1), Color.green(middleColor1), Color.blue(middleColor1));
            int middleWithA2 = Color.argb(a, Color.red(middleColor2), Color.green(middleColor2), Color.blue(middleColor2));
            int middleWithA3 = Color.argb(a, Color.red(middleColor3), Color.green(middleColor3), Color.blue(middleColor3));
            int middleWithA4 = Color.argb(a, Color.red(middleColor4), Color.green(middleColor4), Color.blue(middleColor4));
            int middleWithA5 = Color.argb(a, Color.red(middleColor5), Color.green(middleColor5), Color.blue(middleColor5));
            int middleWithA6 = Color.argb(a, Color.red(middleColor6), Color.green(middleColor6), Color.blue(middleColor6));
            int baseWithA = Color.argb(a, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
            int[] colors = {accentWithA, middleWithA1, middleWithA2, middleWithA3, middleWithA4, middleWithA5, middleWithA6, baseWithA};
            float[] positions = {0.22f, 0.28f, 0.33f, 0.41f, 0.46f, 0.51f, 0.73f, 1f};
            Shader shader = new RadialGradient(centerX, centerY, radius, colors, positions, Shader.TileMode.CLAMP);
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