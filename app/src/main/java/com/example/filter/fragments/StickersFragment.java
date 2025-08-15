package com.example.filter.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class StickersFragment extends Fragment {
    private ImageView myStickerIcon;
    private ImageView AIStickerIcon;
    private FrameLayout fullScreenFragmentContainer;
    private ConstraintLayout filterActivity;
    private ImageButton undoColor, redoColor, originalColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_stickers, container, false);

        undoColor = requireActivity().findViewById(R.id.undoColor);
        redoColor = requireActivity().findViewById(R.id.redoColor);
        originalColor = requireActivity().findViewById(R.id.originalColor);

        undoColor.setVisibility(View.INVISIBLE);
        redoColor.setVisibility(View.INVISIBLE);
        originalColor.setVisibility(View.INVISIBLE);

        myStickerIcon = view.findViewById(R.id.myStickerIcon);
        myStickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new MyStickersFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        AIStickerIcon = view.findViewById(R.id.AIStickerIcon);
        AIStickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                fullScreenFragmentContainer = requireActivity().findViewById(R.id.fullScreenFragmentContainer);
                filterActivity = requireActivity().findViewById(R.id.filterActivity);
                ConstraintLayout main = requireActivity().findViewById(R.id.main);

                fullScreenFragmentContainer.setVisibility(View.VISIBLE);
                filterActivity.setVisibility(View.GONE);
                main.setBackgroundColor(Color.parseColor("#007AFF"));

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fullScreenFragmentContainer, new AIStickerViewFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        return view;
    }
}
