package com.example.filter.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class AIStickerFailFragment extends Fragment {
    ImageButton retryBtn;
    private TextView retryTxt;
    private int retryTextColorDefault;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_aisticker_fail, container, false);

        retryBtn = view.findViewById(R.id.retryBtn);
        retryTxt = view.findViewById(R.id.retryTxt);

        retryTextColorDefault = retryTxt.getCurrentTextColor();

        retryBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            if (!isAdded()) return;
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.aiStickerView, new AIStickerCreateFragment())
                    .commit();
        });

        setupRetryButtonPressEffect();

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupRetryButtonPressEffect() {
        retryBtn.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    retryTxt.setTextColor(Color.WHITE);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    retryTxt.setTextColor(retryTextColorDefault);
                    break;
            }

            return false;
        });
    }
}