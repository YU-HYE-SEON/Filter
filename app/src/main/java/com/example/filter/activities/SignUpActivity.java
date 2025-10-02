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
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;

import com.example.filter.R;
import com.example.filter.dialogs.SignUpDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                } /*else if () {
                    alertTxt.setTextColor(Color.RED);
                    alertTxt.setText("이미 사용중인 닉네임입니다");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(false);
                }*/ else if (bad != null) {
                    alertTxt.setTextColor(Color.RED);
                    alertTxt.setText(bad + "은 사용할 수 없는 문자입니다");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(false);
                } else if (lengthByCodePoint == 0) {
                    alertTxt.setTextColor(Color.RED);
                    alertTxt.setText("닉네임을 입력해주세요");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(false);
                } else {
                    alertTxt.setTextColor(Color.BLUE);
                    alertTxt.setText("사용 가능한 닉네임입니다");
                    alertTxt.setVisibility(View.VISIBLE);
                    btn.setEnabled(true);
                }
            }
        });

        btn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            //Intent intent = new Intent(SignUpActivity.this, OnBoardingActivity.class);
            //intent.putExtra("nickname", nickname.getText().toString());
            startActivity(intent);
            finish();
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

    private boolean isPointInsideView(MotionEvent ev, View v) {
        if (v == null) return false;
        Rect r = new Rect();
        boolean visible = v.getGlobalVisibleRect(r);
        if (!visible) return false;
        final int x = (int) ev.getRawX();
        final int y = (int) ev.getRawY();
        return r.contains(x, y);
    }

    private boolean isPointInsideAnyEditText(MotionEvent ev) {
        return isPointInsideView(ev, nickname);
    }

    private void hideKeyboardAndClearFocus() {
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
                    boolean tapInsideAnyEdit = isPointInsideAnyEditText(ev);

                    if (focusedIsEdit && !tapInsideAnyEdit) {
                        hideKeyboardAndClearFocus();
                    }
                }
                break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}