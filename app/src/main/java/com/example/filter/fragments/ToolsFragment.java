package com.example.filter.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.ClickUtils;

public class ToolsFragment extends Fragment {
    private ImageButton rotationIcon, cropIcon, nextBtn;
    private TextView rotationTxt, cropTxt;
    private ConstraintLayout bottomArea1;
    //private ImageButton undoColor, redoColor, originalColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_tools, container, false);

        rotationIcon = view.findViewById(R.id.rotationIcon);
        cropIcon = view.findViewById(R.id.cropIcon);
        rotationTxt = view.findViewById(R.id.rotationTxt);
        cropTxt = view.findViewById(R.id.cropTxt);

        bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);
        bottomArea1.setVisibility(View.INVISIBLE);

        FilterActivity activity = (FilterActivity) requireActivity();

        if (activity.isRotationEdited()) {
            rotationIcon.setImageResource(R.drawable.icon_rotation_yes);
            rotationTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            rotationIcon.setImageResource(R.drawable.icon_rotation_no);
            rotationTxt.setTextColor(Color.WHITE);
        }

        if (activity.isCropEdited()) {
            cropIcon.setImageResource(R.drawable.icon_crop_yes);
            cropTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            cropIcon.setImageResource(R.drawable.icon_crop_no);
            cropTxt.setTextColor(Color.WHITE);
        }

        rotationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new RotationFragment())
                        .commit();
            }
        });

        cropIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                if (!activity.isCropEdited()) {
                    activity.resetCropState();
                }

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new CropFragment())
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
                        .replace(R.id.bottomArea2, new ColorsFragment())
                        .commit();
            }
        });

        return view;
    }
}
