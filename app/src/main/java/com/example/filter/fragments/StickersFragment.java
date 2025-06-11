package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;

public class StickersFragment extends Fragment {
    private LinearLayout AIStickerIcon;
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
                fullScreenFragmentContainer = requireActivity().findViewById(R.id.fullScreenFragmentContainer);
                filterActivity = requireActivity().findViewById(R.id.filterActivity);
                fullScreenFragmentContainer.setVisibility(View.VISIBLE);
                filterActivity.setVisibility(View.GONE);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fullScreenFragmentContainer, new AIStickerFragment())
                        .commit();
            }
        });

        return view;
    }
}
