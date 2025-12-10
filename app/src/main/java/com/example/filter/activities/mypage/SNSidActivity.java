package com.example.filter.activities.mypage;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.apis.UserApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.etc.ClickUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SNSidActivity extends BaseActivity {
    private ImageButton backBtn;
    private EditText instaEditText, twitterEditText;
    private AppCompatButton btn;
    private String instaId, xId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_sns_id);
        backBtn = findViewById(R.id.backBtn);
        instaEditText = findViewById(R.id.instaEditText);
        twitterEditText = findViewById(R.id.twitterEditText);
        btn = findViewById(R.id.btn);

        loadSocial();

        backBtn.setOnClickListener(v -> {
            finish();
        });

        instaEditText.addTextChangedListener(new TextWatcher() {
            boolean editing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (editing) return;
                editing = true;

                if (!s.toString().startsWith("@")) {
                    instaEditText.setText("@" + s.toString().replace("@",""));
                }
                if (s.toString().equals("")) {
                    instaEditText.setText("@");
                }
                if (instaEditText.getSelectionStart() < 1) {
                    instaEditText.setSelection(1);
                } else {
                    instaEditText.setSelection(instaEditText.getText().length());
                }

                instaId = instaEditText.getText().toString().replace("@","");

                editing = false;
            }
        });

        twitterEditText.addTextChangedListener(new TextWatcher() {
            boolean editing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (editing) return;
                editing = true;

                if (!s.toString().startsWith("@")) {
                    twitterEditText.setText("@" + s.toString().replace("@",""));
                }

                if (s.toString().equals("")) {
                    twitterEditText.setText("@");
                }

                if (twitterEditText.getSelectionStart() < 1) {
                    twitterEditText.setSelection(1);
                } else {
                    twitterEditText.setSelection(twitterEditText.getText().length());
                }

                xId = twitterEditText.getText().toString().replace("@","");

                editing = false;
            }
        });

        ClickUtils.clickDim(btn);
        btn.setOnClickListener(v -> {
            Map<String, String> map = new HashMap<>();
            map.put("instagramId", instaId);
            map.put("xId", xId);

            setSocial(map);
        });

        twitterEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    twitterEditText.clearFocus();

                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    return true;
                }
                return false;
            }
        });
    }

    private void setSocial(Map map) {
        UserApi userApi = AppRetrofitClient.getInstance(this).create(UserApi.class);
        userApi.setSocialIds(map).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SNSidActivity.this, "저장 완료!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e("SNS아이디설정", "SNS 아이디 설정 실패 : " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("SNS아이디설정", "통신 실패", t);
            }
        });
    }

    private void loadSocial() {
        UserApi userApi = AppRetrofitClient.getInstance(this).create(UserApi.class);

        userApi.getSocialIds().enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                Map<String, String> ids = response.body();

                instaId = ids.get("instagramId");
                xId = ids.get("xId");

                instaEditText.setText(instaId);
                twitterEditText.setText(xId);
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e("Review", "통신 오류", t);
                Toast.makeText(SNSidActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isTouchInsideView(View view, MotionEvent event) {
        if (view == null) return false;

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int width = view.getWidth();
        int height = view.getHeight();

        return event.getRawX() >= x && event.getRawX() <= (x + width) &&
                event.getRawY() >= y && event.getRawY() <= (y + height);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();

            if (v instanceof EditText) {
                EditText currentFocusEditText = (EditText) v;

                if (isTouchInsideView(instaEditText, ev) || isTouchInsideView(twitterEditText, ev)) {
                    return super.dispatchTouchEvent(ev);
                }

                Rect outRect = new Rect();
                currentFocusEditText.getGlobalVisibleRect(outRect);

                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    currentFocusEditText.clearFocus();

                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(currentFocusEditText.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
