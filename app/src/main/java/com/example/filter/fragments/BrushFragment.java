package com.example.filter.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class BrushFragment extends Fragment {
    private ImageButton pen, glowPen, mosaic, eraser;
    private ImageButton cancelBtn, checkBtn;
    private View currentToolPanel;
    private LayoutInflater inflater;
    private FrameLayout fullScreenFragmentContainer;
    private View bottomArea2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_brush, container, false);
        this.inflater = inflater;

        pen = view.findViewById(R.id.pen);
        glowPen = view.findViewById(R.id.glowPen);
        mosaic = view.findViewById(R.id.mosaic);
        eraser = view.findViewById(R.id.eraser);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        fullScreenFragmentContainer = requireActivity().findViewById(R.id.fullScreenFragmentContainer);
        bottomArea2  = requireActivity().findViewById(R.id.bottomArea2);

        pen.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            showPenPanel();
        });

        glowPen.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            showPenPanel();
        });

        return view;
    }

    private void showPenPanel() {
        if (fullScreenFragmentContainer == null) return;

        if (currentToolPanel != null) {
            fullScreenFragmentContainer.removeAllViews();
            currentToolPanel = null;
        }

        View panel = inflater.inflate(R.layout.v_pen, fullScreenFragmentContainer, false);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
        );
        panel.setLayoutParams(lp);

        fullScreenFragmentContainer.setVisibility(View.VISIBLE);
        fullScreenFragmentContainer.addView(panel);
        fullScreenFragmentContainer.bringToFront();

        currentToolPanel = panel;

        Runnable applyBottomMargin = () -> {
            int h = (bottomArea2 != null) ? bottomArea2.getHeight() : dp(200);
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) panel.getLayoutParams();
            p.bottomMargin = h;
            panel.setLayoutParams(p);
            panel.requestLayout();
        };
        if (bottomArea2 != null && bottomArea2.getHeight() > 0) {
            applyBottomMargin.run();
        } else if (bottomArea2 != null) {
            bottomArea2.post(applyBottomMargin);
        } else {
            panel.post(applyBottomMargin);
        }

        View penCancel   = panel.findViewById(R.id.cancelBtn);
        View penComplete = panel.findViewById(R.id.completeBtn);
        if (penCancel != null)   penCancel.setOnClickListener(v -> hideToolPanel());
        if (penComplete != null) penComplete.setOnClickListener(v -> hideToolPanel());

        panel.setAlpha(0f);
        panel.setTranslationY(dp(24));
        panel.animate().alpha(1f).translationY(0f).setDuration(160).start();
    }

    private void hideToolPanel() {
        if (fullScreenFragmentContainer == null) return;
        if (currentToolPanel == null) { fullScreenFragmentContainer.setVisibility(View.GONE); return; }

        View panel = currentToolPanel;
        panel.animate().alpha(0f).translationY(dp(16)).setDuration(120)
                .withEndAction(() -> {
                    fullScreenFragmentContainer.removeAllViews();
                    fullScreenFragmentContainer.setVisibility(View.GONE);
                    currentToolPanel = null;
                }).start();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
