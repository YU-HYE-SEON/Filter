package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.filter.R;

public class AIStickerFragment extends Fragment {
    private FrameLayout aiStickerView;
    private ImageButton cancelBtn;
    private FrameLayout fullScreenFragmentContainer;
    private ConstraintLayout filterActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aisticker_view, container, false);
        aiStickerView = view.findViewById(R.id.aiStickerView);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.aiStickerView, new AiStickerMakeFragment())
                .commit();

        cancelBtn = view.findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(v -> {
            fullScreenFragmentContainer = requireActivity().findViewById(R.id.fullScreenFragmentContainer);
            filterActivity = requireActivity().findViewById(R.id.filterActivity);
            fullScreenFragmentContainer.setVisibility(View.GONE);
            filterActivity.setVisibility(View.VISIBLE);

            FragmentManager fm = requireActivity().getSupportFragmentManager();
            Fragment aiStickerFragment = fm.findFragmentById(R.id.fullScreenFragmentContainer);
            if (aiStickerFragment != null) fm.beginTransaction().remove(aiStickerFragment).commit();
        });

        return view;
    }
}
