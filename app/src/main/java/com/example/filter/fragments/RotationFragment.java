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
import com.example.filter.activities.FilterActivity;
import com.example.filter.etc.ClickUtils;

public class RotationFragment extends Fragment {
    private ImageView leftRotationIcon;
    private ImageView rightRotationIcon;
    private ImageView horizontalFlip;
    private ImageView verticalFlip;
    private ImageButton cancelBtn;
    private ImageButton checkBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rotation, container, false);

        leftRotationIcon = view.findViewById(R.id.leftRotationIcon);
        rightRotationIcon = view.findViewById(R.id.rightRotationIcon);
        horizontalFlip = view.findViewById(R.id.horizontalFlip);
        verticalFlip = view.findViewById(R.id.verticalFlip);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        leftRotationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.rotatePhoto(-90);
                }
            }
        });

        rightRotationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.rotatePhoto(90);
                }
            }
        });

        horizontalFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.flipPhoto(true);
                }
            }
        });

        verticalFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.flipPhoto(false);
                }
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.restoreOriginalPhoto();
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, new ToolsFragment())
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.commitTransformations(true);
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, new ToolsFragment())
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        return view;
    }
}
