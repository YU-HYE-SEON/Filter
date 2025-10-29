package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;
import com.example.filter.etc.UserApi;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignUpActivity extends BaseActivity {
    private EditText nickname;
    private TextView alertTxt;
    private AppCompatButton btn;
    private boolean maybeTap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_nickname);
        nickname = findViewById(R.id.nickname);
        alertTxt = findViewById(R.id.alertTxt);
        btn = findViewById(R.id.btn);

        View root = findViewById(android.R.id.content);
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            root.getWindowVisibleDisplayFrame(r);
            int screenHeight = root.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            boolean keypadVisible = keypadHeight > screenHeight * 0.15;
            if (!keypadVisible) nickname.clearFocus();
        });

        nickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                final String input = s.toString();
                final int lengthByCodePoint = input.codePointCount(0, input.length());
                String bad = findFirstForbiddenChar(input);

                if (lengthByCodePoint > 10) {
                    alertTxt.setTextColor(Color.RED);
                    alertTxt.setText("10자 이내로 입력해주세요");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(false);
                } else if (bad != null) {
                    alertTxt.setTextColor(Color.RED);
                    alertTxt.setText(bad + "은 사용할 수 없는 문자입니다");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(false);
                } else if (lengthByCodePoint == 0) {
                    alertTxt.setTextColor(Color.RED);
                    alertTxt.setText("닉네임을 입력해주세요");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(false);
                } /*else if () {
                    alertTxt.setTextColor(Color.RED);
                    alertTxt.setText("이미 사용중인 닉네임입니다");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(false);
                }*/ else {
                    checkNicknameDuplicate(input);
                    /*alertTxt.setTextColor(Color.BLUE);
                    alertTxt.setText("사용 가능한 닉네임입니다");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(true);*/
                }
            }
        });

        btn.setOnClickListener(v -> {
            /*Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            //Intent intent = new Intent(SignUpActivity.this, OnBoardingActivity.class);
            //intent.putExtra("nickname", nickname.getText().toString());
            startActivity(intent);
            finish();*/
            String nicknameStr = nickname.getText().toString();
            if (nicknameStr.isEmpty()) return;

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://13.124.105.243/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            UserApi api = retrofit.create(UserApi.class);

            // JSON 바디 생성
            JsonObject json = new JsonObject();
            json.addProperty("nickname", nicknameStr);

            Call<ResponseBody> call = api.setNickname(json); // POST /api/v1/users/nickname
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        //Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                        Intent intent = new Intent(SignUpActivity.this, OnBoardingActivity  .class);
                        startActivity(intent);
                        finish();
                    } else {
                        alertTxt.setTextColor(Color.RED);
                        alertTxt.setText("닉네임 등록 실패");
                        alertTxt.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    alertTxt.setTextColor(Color.RED);
                    alertTxt.setText("네트워크 오류");
                    alertTxt.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private String findFirstForbiddenChar(String s) {
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int next = i + Character.charCount(cp);

            if (!isAllowedCodePoint(cp)) {
                return new String(Character.toChars(cp));
            }
            i = next;
        }
        return null;
    }

    private boolean isAllowedCodePoint(int cp) {
        if (Character.isLetterOrDigit(cp)) return true;
        if (cp == '_') return true;
        if (isEmojiLike(cp)) return true;

        return false;
    }

    private boolean isEmojiLike(int cp) {
        if ((cp >= 0x1F600 && cp <= 0x1F64F) ||
                (cp >= 0x1F300 && cp <= 0x1F5FF) ||
                (cp >= 0x1F680 && cp <= 0x1F6FF) ||
                (cp >= 0x1F900 && cp <= 0x1F9FF) ||
                (cp >= 0x1FA70 && cp <= 0x1FAFF) ||
                (cp >= 0x2600 && cp <= 0x26FF) ||
                (cp >= 0x2700 && cp <= 0x27BF) ||
                (cp >= 0x1F1E6 && cp <= 0x1F1FF) ||
                (cp >= 0x1F3FB && cp <= 0x1F3FF) ||
                (cp >= 0xFE00 && cp <= 0xFE0F) ||
                cp == 0x200D ||
                cp == 0x20E3) {
            return true;
        }
        return false;
    }

    private void checkNicknameDuplicate(String nicknameStr) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.124.105.243/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        UserApi api = retrofit.create(UserApi.class);
        api.checkNickname(nicknameStr).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String body = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(body);
                        boolean exists = json.optBoolean("exists", false);

                        if (exists) {
                            alertTxt.setTextColor(Color.RED);
                            alertTxt.setText("이미 사용중인 닉네임입니다");
                            alertTxt.setVisibility(View.VISIBLE);
                            btn.setEnabled(false);
                        } else {
                            alertTxt.setTextColor(Color.BLUE);
                            alertTxt.setText("사용 가능한 닉네임입니다");
                            alertTxt.setVisibility(View.VISIBLE);
                            btn.setEnabled(true);
                        }
                    } else {
                        alertTxt.setTextColor(Color.RED);
                        alertTxt.setText("서버 오류: " + response.code());
                        alertTxt.setVisibility(View.VISIBLE);
                        btn.setEnabled(false);
                    }
                } catch (Exception e) {
                    alertTxt.setTextColor(Color.RED);
                    alertTxt.setText("응답 처리 중 오류 발생");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                alertTxt.setTextColor(Color.RED);
                alertTxt.setText("네트워크 오류");
                alertTxt.setVisibility(View.VISIBLE);
                btn.setEnabled(false);
            }
        });
    }

    /*private boolean isPointInsideView(MotionEvent ev, View v) {
        if (v == null) return false;
        Rect r = new Rect();
        boolean visible = v.getGlobalVisibleRect(r);
        if (!visible) return false;
        final int x = (int) ev.getRawX();
        final int y = (int) ev.getRawY();
        return r.contains(x, y);
    }*/

    private boolean isPoint(MotionEvent ev) {
        if (nickname == null) return false;
        Rect r = new Rect();
        boolean visible = nickname.getGlobalVisibleRect(r);
        if (!visible) return false;
        final int x = (int) ev.getRawX();
        final int y = (int) ev.getRawY();
        return r.contains(x, y);

        //return isPointInsideView(ev, nickname);
    }

    private void hideKeypadAndClearFocus() {
        View v = getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
        if (nickname != null) nickname.clearFocus();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                maybeTap = true;
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (maybeTap) {
                    View focused = getCurrentFocus();
                    boolean focusedIsEdit = focused instanceof EditText;
                    boolean tapInsideEdit = isPoint(ev);

                    if (focusedIsEdit && !tapInsideEdit) {
                        hideKeypadAndClearFocus();
                    }
                }
                break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}