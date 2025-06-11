package com.example.filter.fragments;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;

public class AiStickerMakeFragment extends Fragment {
    private ConstraintLayout aiStickerMake;
    private TextView img;
    private TextView warningTxt;
    private EditText editText;
    private boolean isEnabled = false;
    private ImageButton makeBtn;
    private TextView makeTxt;
    private float upView = 220f;  //aiStickerView 올라갈/내려갈 높이
    private int animDuration = 500;   //aiStickerView 올라가는데 걸리는 시간

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aisticker_make, container, false);
        aiStickerMake = view.findViewById(R.id.aiStickerMake);
        img = view.findViewById(R.id.img);
        warningTxt = view.findViewById(R.id.warningTxt);
        editText = view.findViewById(R.id.editText);
        makeBtn = view.findViewById(R.id.makeBtn);
        makeTxt = view.findViewById(R.id.makeTxt);

        makeBtn.setEnabled(false);
        makeBtn.setVisibility(View.INVISIBLE);
        makeTxt.setAlpha(0f);
        makeTxt.setVisibility(View.INVISIBLE);

        editText.addTextChangedListener(new TextWatcher() {
            private boolean isBlocked = false;
            private InputFilter blockInputFilter = (source, start, end, dest, dstart, dend) -> "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                int englishCount = countEnglishLetters(text);
                int koreanCount = countKoreanLetters(text);

                boolean isOverLimit = englishCount > 300 || koreanCount > 100;

                if (isOverLimit) {
                    if (!isBlocked) {
                        isBlocked = true;
                        warningTxt.setVisibility(View.VISIBLE);
                        editText.setFilters(new InputFilter[]{blockInputFilter});

                    }
                } else {
                    if (isBlocked) {
                        isBlocked = false;
                        warningTxt.setVisibility(View.INVISIBLE);
                        editText.setFilters(new InputFilter[]{});
                    }
                }

                //비활
                //isEmpty       !isEmpty
                //텍스트x    <->    텍스트o
                //isOverLimit <-> !isOverLimit
                //제한o <-> 제한x
                //비활 <-> 활성

                if (!text.isEmpty() && !isOverLimit) {
                    makeBtn.setEnabled(true);
                    //makeBtn.setAlpha(1.0f);
                    makeTxt.setAlpha(1.0f);
                    isEnabled = true;
                } else {
                    makeBtn.setEnabled(false);
                    //makeBtn.setAlpha(0.5f);
                    makeTxt.setAlpha(0.4f);
                    isEnabled = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                view.getWindowVisibleDisplayFrame(r);
                int screenHeight = view.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                if (keypadHeight > screenHeight * 0.1) {
                    float px = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, upView, view.getResources().getDisplayMetrics()
                    );

                    ObjectAnimator animator = ObjectAnimator.ofFloat(aiStickerMake, "translationY", -px);
                    animator.setDuration(animDuration).start();

                    img.animate()
                            .alpha(0f)
                            .translationY(0f)
                            .setDuration(animDuration)
                            .withEndAction(() -> {
                                img.setAlpha(1f);
                                img.setTranslationY(30f);
                                img.setVisibility(View.INVISIBLE);
                            })
                            .start();

                    if (makeBtn.getVisibility() == View.INVISIBLE) {
                        makeTxt.setAlpha(0f);
                        makeTxt.setVisibility(View.VISIBLE);
                        if (isEnabled) makeTxt.setAlpha(1f);
                        else makeTxt.setAlpha(0.4f);

                        makeBtn.setAlpha(0f);
                        makeBtn.setTranslationY(30f);
                        makeBtn.setVisibility(View.VISIBLE);
                        makeBtn.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(animDuration)
                                .start();
                    }
                } else {
                    if (editText.hasFocus()) editText.clearFocus();

                    ObjectAnimator animator = ObjectAnimator.ofFloat(aiStickerMake, "translationY", 0);
                    animator.setDuration(animDuration).start();

                    if (img.getVisibility() == View.INVISIBLE) {
                        img.setAlpha(0f);
                        img.setTranslationY(0f);
                        img.setVisibility(View.VISIBLE);
                        img.animate()
                                .alpha(1f)
                                .translationY(30f)
                                .setDuration(animDuration)
                                .start();
                    }

                    makeBtn.animate()
                            .alpha(0f)
                            .translationY(30f)
                            .setDuration(animDuration)
                            .withEndAction(() -> {
                                makeBtn.setAlpha(1f);
                                makeBtn.setTranslationY(0f);
                                makeBtn.setVisibility(View.INVISIBLE);
                            })
                            .start();
                }
            }
        });

        makeBtn.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.aiStickerView, new AiStickerSuccessFragment())
                    .commit();
        });

        makeBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    makeTxt.setTextColor(Color.WHITE);
                    break;
                case MotionEvent.ACTION_UP:
                    makeTxt.setTextColor(Color.parseColor("#FFFFFF"));
                    break;
            }
            return false;
        });

        return view;
    }

    private int countEnglishLetters(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                count++;
            }
        }
        return count;
    }

    private int countKoreanLetters(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_JAMO) {
                count++;
            }
        }
        return count;
    }
}
