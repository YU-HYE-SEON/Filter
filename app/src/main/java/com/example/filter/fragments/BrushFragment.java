package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class BrushFragment extends Fragment {
    private ImageButton pen, glowPen, mosaic, eraser;
    private ImageButton cancelBtn, checkBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_brush, container, false);

        pen = view.findViewById(R.id.pen);
        glowPen = view.findViewById(R.id.glowPen);
        mosaic = view.findViewById(R.id.mosaic);
        eraser = view.findViewById(R.id.eraser);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        pen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClickUtils.isFastClick(500)) return;

            }});

        glowPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClickUtils.isFastClick(500)) return;

            }});

        return view;
    }
}
