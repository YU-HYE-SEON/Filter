package com.example.filter.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class AiStickerCreateFragment extends Fragment {
    private ConstraintLayout aiStickerCreate;
    private LinearLayout contentBox;
    private ImageView img;
    private TextView txt;
    private TextView warningTxt;
    private EditText editText;
    private ImageButton createBtn;
    private TextView createTxt;
    private float upView = 280f;
    private int animDuration = 500;
    private boolean isUp = false;
    private boolean imeVisible = false;
    private int createTextColorDefault;
    private float baseTY = 0f;
    private boolean baseReady = false;
    private float upTY = Float.NaN;
    private boolean upTYReady = false;
    private int lastParentH = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_aisticker_create, container, false);
        aiStickerCreate = view.findViewById(R.id.aiStickerCreate);
        contentBox = view.findViewById(R.id.contentBox);
        img = view.findViewById(R.id.img);
        txt = view.findViewById(R.id.txt);
        warningTxt = view.findViewById(R.id.warningTxt);
        editText = view.findViewById(R.id.editText);
        createBtn = view.findViewById(R.id.createBtn);
        createTxt = view.findViewById(R.id.createTxt);

        isUp = false;
        imeVisible = false;
        baseReady = false;

        createTextColorDefault = createTxt.getCurrentTextColor();

        createBtn.setAlpha(0f);
        createTxt.setAlpha(0f);
        createBtn.setVisibility(View.INVISIBLE);
        createTxt.setVisibility(View.INVISIBLE);
        warningTxt.setVisibility(View.INVISIBLE);

        createBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            String prompt = editText.getText() != null ? editText.getText().toString().trim() : "";
            if (prompt.isEmpty()) return;

            //서버 재테스트할 때마다 변경해야 됨
            String baseUrl = "https://dba58ce20ef5.ngrok-free.app/";

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.aiStickerView, AiStickerLoadingFragment.newInstance(baseUrl, prompt))
                    .addToBackStack("sticker_flow")
                    .commit();
        });

        setupKeyboardVisibilityListener(view);
        setupEditTextBehavior();
        setupCreateButtonPressEffect();

        updateControlsByText(editText.getText() == null ? "" : editText.getText().toString());

        contentBox.setAlpha(0f);

        contentBox.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                contentBox.getViewTreeObserver().removeOnPreDrawListener(this);

                View parent = (View) contentBox.getParent();
                int parentH = parent.getHeight();
                int boxH = contentBox.getHeight();
                int top = contentBox.getTop();

                baseTY = (parentH - boxH) / 2f - top;
                contentBox.setTranslationY(baseTY);
                contentBox.setAlpha(1f);
                baseReady = true;

                if (!upTYReady || lastParentH != parentH) {
                    upTY = baseTY - dpToPx(upView);
                    upTYReady = true;
                }
                lastParentH = parentH;

                editText.scrollTo(0, 0);
                editText.setSelection(0);

                setupOutsideTapToHideIme(aiStickerCreate);

                return true;
            }
        });

        editText.setVerticalScrollBarEnabled(true);
        editText.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

        editText.setOnTouchListener((v, ev) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        return view;
    }

    private void setupKeyboardVisibilityListener(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            boolean nowVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (nowVisible != imeVisible) {
                if (!nowVisible) {
                    clearFocusAndHideIme();
                }
                if (baseReady) {
                    if (nowVisible) {
                        animateUp();
                    } else {
                        animateDown();
                    }
                }
                imeVisible = nowVisible;
            }
            return insets;
        });
    }

    private void setupEditTextBehavior() {
        editText.setHorizontallyScrolling(false);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateControlsByText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        editText.setSingleLine(false);
        editText.setImeOptions(editText.getImeOptions()
                | android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    }

    private void setupCreateButtonPressEffect() {
        createBtn.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    createTxt.setTextColor(0xFFFFFFFF); // white
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    createTxt.setTextColor(createTextColorDefault);
                    break;
            }

            return false;
        });
    }

    private void updateControlsByText(String text) {
        boolean overLimit = isOverLimit(text);
        boolean empty = text.trim().isEmpty();

        warningTxt.setVisibility(overLimit ? View.VISIBLE : View.INVISIBLE);

        boolean enableCreate = !empty && !overLimit;
        createBtn.setEnabled(enableCreate);
        createTxt.setEnabled(enableCreate);
        float targetAlpha = enableCreate ? 1.0f : 0.4f;

        if (createBtn.getVisibility() == View.VISIBLE) {
            createBtn.setAlpha(targetAlpha);
            createTxt.setAlpha(targetAlpha);
        }
    }

    private boolean isOverLimit(String s) {
        int limit = containsHangul(s) ? 100 : 300;
        int len = s.length();
        return len > limit;
    }

    private boolean containsHangul(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (block == Character.UnicodeBlock.HANGUL_SYLLABLES
                    || block == Character.UnicodeBlock.HANGUL_JAMO
                    || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
                return true;
            }
        }
        return false;
    }

    private void animateUp() {
        if (contentBox == null || isUp || !baseReady) return;

        AccelerateDecelerateInterpolator interp = new AccelerateDecelerateInterpolator();

        img.animate().alpha(0f).setDuration(animDuration).setInterpolator(interp).start();

        if (createBtn.getVisibility() != View.VISIBLE) {
            createBtn.setVisibility(View.VISIBLE);
            createTxt.setVisibility(View.VISIBLE);
        }

        String cur = editText.getText() == null ? "" : editText.getText().toString();

        float targetAlpha = (!isOverLimit(cur) && !cur.trim().isEmpty()) ? 1f : 0.4f;

        createBtn.setAlpha(0f);
        createTxt.setAlpha(0f);
        createBtn.animate().alpha(targetAlpha).setDuration(animDuration).setInterpolator(interp).start();
        createTxt.animate().alpha(targetAlpha).setDuration(animDuration).setInterpolator(interp).start();

        if (!upTYReady) {
            upTY = baseTY - dpToPx(upView);
            upTYReady = true;
        }

        contentBox.animate()
                .translationY(upTY)
                .setDuration(animDuration)
                .setInterpolator(interp)
                .withStartAction(() -> isUp = true)
                .start();
    }

    private void animateDown() {
        if (contentBox == null || !isUp || !baseReady) return;

        AccelerateDecelerateInterpolator interp = new AccelerateDecelerateInterpolator();

        img.animate().alpha(1f).setDuration(animDuration).setInterpolator(interp).start();

        createBtn.animate().alpha(0f).setDuration(animDuration).setInterpolator(interp)
                .withEndAction(() -> {
                    createBtn.setVisibility(View.INVISIBLE);
                    createTxt.setTextColor(createTextColorDefault);
                })
                .start();
        createTxt.animate().alpha(0f).setDuration(animDuration).setInterpolator(interp)
                .withEndAction(() -> createTxt.setVisibility(View.INVISIBLE))
                .start();

        contentBox.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                contentBox.getViewTreeObserver().removeOnPreDrawListener(this);

                View parent = (View) contentBox.getParent();
                int parentH = parent.getHeight();
                int boxH = contentBox.getHeight();
                int top = contentBox.getTop();

                baseTY = (parentH - boxH) / 2f - top;

                contentBox.animate()
                        .translationY(baseTY)
                        .setDuration(animDuration)
                        .setInterpolator(interp)
                        .withEndAction(() -> isUp = false)
                        .start();
                return true;
            }
        });
    }

    private float dpToPx(float dp) {
        if (getContext() == null) return dp;
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }

    private void recalcBaseIfNeeded() {
        if (contentBox == null) return;
        contentBox.post(() -> {
            View parent = (View) contentBox.getParent();
            baseTY = (parent.getHeight() - contentBox.getHeight()) / 2f - contentBox.getTop();
            if (!imeVisible && !isUp) contentBox.setTranslationY(baseTY);
            baseReady = true;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (contentBox != null) contentBox.animate().cancel();
        if (img != null) img.animate().cancel();
        if (createBtn != null) createBtn.animate().cancel();
    }

    private void clearFocusAndHideIme() {
        if (editText == null || getContext() == null) return;

        editText.clearFocus();
        View root = getView();
        if (root != null) root.requestFocus();

        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        View target = (root != null && root.getWindowToken() != null) ? root : editText;
        IBinder token = target.getWindowToken();
        if (imm != null && token != null) {
            imm.hideSoftInputFromWindow(token, 0);
        }
    }

    private void setupOutsideTapToHideIme(View target) {
        target.setClickable(true);
        target.setFocusableInTouchMode(true);

        target.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                if (imeVisible && !isTouchInsideView(editText, ev)) {
                    clearFocusAndHideIme();
                    v.performClick();
                    return true;
                }
            }
            return false;
        });
    }

    private boolean isTouchInsideView(View view, MotionEvent ev) {
        if (view == null) return false;
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        float x = ev.getRawX();
        float y = ev.getRawY();
        return x >= loc[0] && x <= loc[0] + view.getWidth()
                && y >= loc[1] && y <= loc[1] + view.getHeight();
    }
}