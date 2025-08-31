package com.example.filter.fragments;

import android.os.Bundle;
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
    private FrameLayout brushPanel;

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

        brushPanel = requireActivity().findViewById(R.id.brushPanel);

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
        if (brushPanel == null) return;

        View panel = inflater.inflate(R.layout.v_pen, brushPanel, false);

        brushPanel.setVisibility(View.VISIBLE);
        brushPanel.removeAllViews();
        brushPanel.addView(panel);

        currentToolPanel = panel;

        View penCancel   = panel.findViewById(R.id.cancelBtn);
        View penComplete = panel.findViewById(R.id.completeBtn);
        if (penCancel != null)   penCancel.setOnClickListener(v -> hideToolPanel());
        if (penComplete != null) penComplete.setOnClickListener(v -> hideToolPanel());
    }

    private void hideToolPanel() {
        if (brushPanel == null) return;
        if (currentToolPanel == null) { brushPanel.setVisibility(View.GONE); return; }

        View panel = currentToolPanel;
        panel.animate().alpha(0f).translationY(dp(16)).setDuration(120)
                .withEndAction(() -> {
                    brushPanel.removeAllViews();
                    brushPanel.setVisibility(View.GONE);
                    currentToolPanel = null;
                }).start();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
