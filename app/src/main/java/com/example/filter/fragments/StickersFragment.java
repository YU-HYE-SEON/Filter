package com.example.filter.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class StickersFragment extends Fragment {
    private ImageView AIStickerIcon;
    private FrameLayout fullScreenFragmentContainer;
    private ConstraintLayout filterActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stickers, container, false);

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
                        .commit();
            }
        });



        return view;
    }
}
