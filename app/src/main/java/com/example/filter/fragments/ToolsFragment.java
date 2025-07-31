package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class ToolsFragment extends Fragment {
    private ImageView rotationIcon;
    private ImageView cropIcon;
    private ImageButton nextBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);

        rotationIcon=view.findViewById(R.id.rotationIcon);
        cropIcon = view.findViewById(R.id.cropIcon);

        rotationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea, new RotationFragment())
                        .commit();
            }
        });

        cropIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea, new CropFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        nextBtn = view.findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea, new ColorsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        return view;
    }
}
