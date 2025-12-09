package com.example.filter.activities.start;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.apis.UserApi;
import com.example.filter.etc.ClickUtils;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

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
        btn = findViewById(R.id.btn); // 다음 버튼

        // 키보드 닫힘 감지
        View root = findViewById(android.R.id.content);
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            root.getWindowVisibleDisplayFrame(r);
            int screenHeight = root.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            boolean keypadVisible = keypadHeight > screenHeight * 0.15;
            if (!keypadVisible) nickname.clearFocus();
        });

        // 닉네임 입력 검증 
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
                    alertTxt.setTextColor(Color.parseColor("#FF5C8A"));
                    alertTxt.setText("10자 이내로 입력해주세요");
                    setEditSizePos(320, 51, false);
                    nickname.setBackgroundResource(R.drawable.edit_nick_x);
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setVisibility(View.INVISIBLE);
                    btn.setEnabled(false);

                    Log.d("닉네임", "닉네임 초과");

                } else if (bad != null) {
                    alertTxt.setTextColor(Color.parseColor("#FF5C8A"));
                    alertTxt.setText(bad + "은 사용할 수 없는 문자입니다");
                    setEditSizePos(320, 51, false);
                    nickname.setBackgroundResource(R.drawable.edit_nick_x);
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(false);
                    btn.setVisibility(View.INVISIBLE);

                    Log.d("닉네임", "유효하지 않은 문자");

                } else if (lengthByCodePoint == 0) {
                    alertTxt.setTextColor(Color.parseColor("#FF5C8A"));
                    alertTxt.setText("닉네임을 입력해주세요");
                    setEditSizePos(320, 51, false);
                    nickname.setBackgroundResource(R.drawable.edit_nick_x);
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setVisibility(View.INVISIBLE);
                    btn.setEnabled(false);

                    Log.d("닉네임", "닉네임 비어있음");

                } else {  //닉네임 중복 확인
                    checkNicknameDuplicate(input);
                }
            }
        });

        ClickUtils.clickDim(btn);
        // ‼️ 닉네임 입력 후 다음 버튼 클릭
        btn.setOnClickListener(v -> {
            btn.setEnabled(false);
            btn.setClickable(false);
            //if (ClickUtils.isFastClick(v, 400)) return; // 더블 클릭 방지
            String inputNickname = nickname.getText().toString().trim(); // 입력된 닉네임 가져오기
            sendNicknameToServer(inputNickname); // 서버로 닉네임 전송
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

    /**
     * 닉네임을 Spring 서버에 전송하는 메서드
     */
    private void sendNicknameToServer(String nickname) {
        // ① Retrofit 싱글톤 인스턴스 가져오기
        Retrofit retrofit = AppRetrofitClient.getInstance(this);
        UserApi api = retrofit.create(UserApi.class);

        // ② 요청 바디 생성
        UserApi.NicknameRequest body = new UserApi.NicknameRequest(nickname);

        // ③ 비동기 요청 (enqueue: UI 스레드 블로킹 없음)
        api.setNickname(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // ✅ 성공 시
                    Log.d("닉네임", "✅ 닉네임 설정 성공");

                    // ④ 다음 화면으로 이동
                    Intent intent = new Intent(SignUpActivity.this, OnBoardingActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    btn.setEnabled(true);
                    btn.setClickable(true);

                    // ❌ 서버 응답은 왔지만 실패 코드 (400, 409 등)
                    Log.e("닉네임", "❌ 닉네임 설정 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btn.setEnabled(true);
                btn.setClickable(true);

                // ❌ 네트워크 자체 실패 (서버 다운, 연결 실패 등)
                Log.e("닉네임", "❌ 서버 연결 오류", t);
            }
        });
    }

    /// 닉네임 중복 판단
    private void checkNicknameDuplicate(String nicknameInput) {
        Retrofit retrofit = AppRetrofitClient.getInstance(this);
        UserApi userApi = retrofit.create(UserApi.class);

        Call<ResponseBody> call = userApi.checkNicknameExists(nicknameInput);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(responseBody);
                        boolean exists = json.optBoolean("exists", false);

                        if (exists) {
                            Log.d("닉네임", "닉네임 중복 o");

                            alertTxt.setTextColor(Color.RED);
                            alertTxt.setText("이미 사용중인 닉네임입니다");
                            setEditSizePos(320, 51, false);
                            nickname.setBackgroundResource(R.drawable.edit_nick_x);
                            alertTxt.setVisibility(View.VISIBLE);
                            btn.setVisibility(View.INVISIBLE);
                            btn.setEnabled(false);
                        } else {
                            Log.d("닉네임", "닉네임 중복 x");

                            alertTxt.setTextColor(Color.BLUE);
                            alertTxt.setText("사용 가능한 닉네임입니다");
                            setEditSizePos(344, 75, true);
                            nickname.setBackgroundResource(R.drawable.edit_nick_o);
                            alertTxt.setVisibility(View.VISIBLE);
                            btn.setVisibility(View.VISIBLE);
                            btn.setEnabled(true);
                        }
                    } catch (Exception e) {
                        Log.e("닉네임", "JSON 파싱 실패", e);
                    }
                } else {
                    Log.e("닉네임", "닉네임 중복 확인 실패 : " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("닉네임", "닉네임 중복 확인 실패", t);
            }
        });
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private void setEditSizePos(int w, int h, boolean isNew) {
        nickname.getLayoutParams().width = dp(w);
        nickname.getLayoutParams().height = dp(h);
        nickname.requestLayout();

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) nickname.getLayoutParams();
        ViewGroup.MarginLayoutParams lp2 = (ViewGroup.MarginLayoutParams) alertTxt.getLayoutParams();

        if (isNew) {
            nickname.setPadding(dp(37), 0, dp(37), 0);
            lp.topMargin = dp(210);
            lp2.topMargin = dp(-2);
        } else {
            nickname.setPadding(dp(25), 0, dp(25), 0);
            lp.topMargin = dp(222);
            lp2.topMargin = dp(10);
        }
        nickname.setLayoutParams(lp);
        alertTxt.setLayoutParams(lp2);
    }
}