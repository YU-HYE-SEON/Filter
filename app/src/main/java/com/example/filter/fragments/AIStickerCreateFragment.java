package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
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
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class AIStickerCreateFragment extends Fragment {
    private ConstraintLayout aiStickerCreate;
    private LinearLayout contentBox;
    private ImageView symbol;
    private TextView warningTxt;
    private EditText editText;
    private ImageButton createBtn;
    private TextView createTxt;
    private float upView = 280f;
    private int animDuration = 500;
    private boolean isUp = false;
    private boolean imeVisible = false;
    private float baseTY = 0f;
    private boolean baseReady = false;
    private float upTY = Float.NaN;
    private boolean upTYReady = false;
    private int lastParentH = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_aisticker_create, container, false);
        aiStickerCreate = view.findViewById(R.id.aiStickerCreate);
        contentBox = view.findViewById(R.id.contentBox);
        symbol = view.findViewById(R.id.symbol);
        warningTxt = view.findViewById(R.id.warningTxt);
        editText = view.findViewById(R.id.editText);
        createBtn = view.findViewById(R.id.createBtn);
        createTxt = view.findViewById(R.id.createTxt);

        isUp = false;
        imeVisible = false;
        baseReady = false;

        createBtn.setVisibility(View.INVISIBLE);
        createTxt.setVisibility(View.INVISIBLE);
        warningTxt.setVisibility(View.INVISIBLE);

        ClickUtils.clickDim(createBtn);
        createBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            String prompt = editText.getText() != null ? editText.getText().toString().trim() : "";
            if (prompt.isEmpty()) return;

            String baseUrl = "http://13.124.105.243/";

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.aiStickerView, AIStickerLoadingFragment.newInstance(baseUrl, prompt))
                    .addToBackStack("ai_create_to_loading")
                    .commit();
        });

        setupKeyboardVisibilityListener(view);
        setupEditTextBehavior();

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
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            root.getWindowVisibleDisplayFrame(r);
            int screenH = root.getRootView().getHeight();
            int keypadH = screenH - r.bottom;

            boolean nowVisible = keypadH > screenH * 0.15f;
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

    private void updateControlsByText(String text) {
        boolean overLimit = isOverLimit(text);
        boolean empty = text.trim().isEmpty();

        warningTxt.setVisibility(overLimit ? View.VISIBLE : View.INVISIBLE);

        boolean enableCreate = !empty && !overLimit;
        createBtn.setEnabled(enableCreate);
        createTxt.setEnabled(enableCreate);

        if (enableCreate) {
            createBtn.setBackgroundResource(R.drawable.btn_create_ai_sticker_yes);
            createTxt.setTextColor(Color.parseColor("#007AFF"));
        } else {
            createBtn.setBackgroundResource(R.drawable.btn_create_ai_sticker_no);
            createTxt.setTextColor(Color.parseColor("#80007AFF"));
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

        symbol.animate().alpha(0f).setDuration(animDuration).setInterpolator(interp).start();

        if (createBtn.getVisibility() != View.VISIBLE) {
            createBtn.setVisibility(View.VISIBLE);
            createTxt.setVisibility(View.VISIBLE);
            createBtn.setAlpha(0f);
            createTxt.setAlpha(0f);
        }

        createBtn.animate().alpha(1f).setDuration(animDuration).setInterpolator(interp).start();
        createTxt.animate().alpha(1f).setDuration(animDuration).setInterpolator(interp).start();

        String cur = editText.getText() == null ? "" : editText.getText().toString();

        if (!isOverLimit(cur) && !cur.trim().isEmpty()) {
            createBtn.setBackgroundResource(R.drawable.btn_create_ai_sticker_yes);
            createTxt.setTextColor(Color.parseColor("#007AFF"));
        } else {
            createBtn.setBackgroundResource(R.drawable.btn_create_ai_sticker_no);
            createTxt.setTextColor(Color.parseColor("#80007AFF"));
        }

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

        symbol.animate().alpha(1f).setDuration(animDuration).setInterpolator(interp).start();

        createBtn.animate().alpha(0f).setDuration(animDuration).setInterpolator(interp)
                .withEndAction(() -> {
                    createBtn.setVisibility(View.INVISIBLE);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (contentBox != null) contentBox.animate().cancel();
        if (symbol != null) symbol.animate().cancel();
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