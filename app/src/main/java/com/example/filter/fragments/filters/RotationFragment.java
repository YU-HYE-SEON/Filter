package com.example.filter.fragments.filters;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.filter.FilterActivity;
import com.example.filter.etc.ClickUtils;

public class RotationFragment extends Fragment {
    private AppCompatButton saveBtn;
    private LinearLayout leftRotationBtn, rightRotationBtn, horizontalFlipBtn, verticalFlipBtn;
    private ImageView leftRotationIcon, rightRotationIcon, horizontalFlip, verticalFlip;
    private TextView leftRotationTxt, rightRotationTxt, horizontalFlipTxt, verticalFlipTxt;
    private ImageButton cancelBtn, checkBtn;
    private int currentRotationDeg = 0;
    private boolean flippedH = false;
    private boolean flippedV = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_rotation, container, false);

        leftRotationBtn = view.findViewById(R.id.leftRotationBtn);
        leftRotationIcon = view.findViewById(R.id.leftRotationIcon);
        leftRotationTxt = view.findViewById(R.id.leftRotationTxt);

        rightRotationBtn = view.findViewById(R.id.rightRotationBtn);
        rightRotationIcon = view.findViewById(R.id.rightRotationIcon);
        rightRotationTxt = view.findViewById(R.id.rightRotationTxt);

        horizontalFlipBtn = view.findViewById(R.id.horizontalFlipBtn);
        horizontalFlip = view.findViewById(R.id.horizontalFlip);
        horizontalFlipTxt = view.findViewById(R.id.horizontalFlipTxt);

        verticalFlipBtn = view.findViewById(R.id.verticalFlipBtn);
        verticalFlip = view.findViewById(R.id.verticalFlip);
        verticalFlipTxt = view.findViewById(R.id.verticalFlipTxt);

        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        FilterActivity activity = (FilterActivity) requireActivity();

        currentRotationDeg = activity.getAccumRotationDeg();
        flippedH = activity.isAccumFlipH();
        flippedV = activity.isAccumFlipV();

        updateIcon(activity);

        leftRotationBtn.setOnClickListener(v -> {
            activity.rotatePhoto(-90);
            currentRotationDeg = (currentRotationDeg - 90 + 360) % 360;
            activity.setLastRotationDirection(true, false);
            updateIcon(activity);
        });

        rightRotationBtn.setOnClickListener(v -> {
            activity.rotatePhoto(90);
            currentRotationDeg = (currentRotationDeg + 90) % 360;
            activity.setLastRotationDirection(false, true);
            updateIcon(activity);
        });

        horizontalFlipBtn.setOnClickListener(v -> {
            activity.flipPhoto(true);
            flippedH = !flippedH;
            updateIcon(activity);
        });

        verticalFlipBtn.setOnClickListener(v -> {
            activity.flipPhoto(false);
            flippedV = !flippedV;
            updateIcon(activity);
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                FilterActivity activity = (FilterActivity) getActivity();

                if (activity != null) {
                    activity.restoreOriginalPhoto();

                    flippedH = false;
                    flippedV = false;
                    updateIcon(activity);

                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, new ToolsFragment())
                            .commit();
                }
            }
        });

        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity != null) {
                    activity.commitTransformations(true);
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, new ToolsFragment())
                            .commit();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        saveBtn = requireActivity().findViewById(R.id.saveBtn);
        if (saveBtn != null) {
            saveBtn.setEnabled(false);
            saveBtn.setAlpha(0.4f);
        }
    }

    private void updateIcon(FilterActivity activity) {
        boolean lastPressedLeft = activity.isLastRotationLeft();
        boolean lastPressedRight = activity.isLastRotationRight();

        if (currentRotationDeg == 0) {
            leftRotationIcon.setImageResource(R.drawable.icon_rotation_left_no);
            leftRotationTxt.setTextColor(Color.parseColor("#90989F"));

            rightRotationIcon.setImageResource(R.drawable.icon_rotation_right_no);
            rightRotationTxt.setTextColor(Color.parseColor("#90989F"));
        } else if (lastPressedLeft) {
            leftRotationIcon.setImageResource(R.drawable.icon_rotation_left_yes);
            leftRotationTxt.setTextColor(Color.parseColor("#C2FA7A"));

            rightRotationIcon.setImageResource(R.drawable.icon_rotation_right_no);
            rightRotationTxt.setTextColor(Color.parseColor("#90989F"));
        } else if (lastPressedRight) {
            rightRotationIcon.setImageResource(R.drawable.icon_rotation_right_yes);
            rightRotationTxt.setTextColor(Color.parseColor("#C2FA7A"));

            leftRotationIcon.setImageResource(R.drawable.icon_rotation_left_no);
            leftRotationTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (flippedH) {
            horizontalFlip.setImageResource(R.drawable.icon_flip_horizontal_yes);
            horizontalFlipTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            horizontalFlip.setImageResource(R.drawable.icon_flip_horizontal_no);
            horizontalFlipTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (flippedV) {
            verticalFlip.setImageResource(R.drawable.icon_flip_vertical_yes);
            verticalFlipTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            verticalFlip.setImageResource(R.drawable.icon_flip_vertical_no);
            verticalFlipTxt.setTextColor(Color.parseColor("#90989F"));
        }

        boolean anyYes = (flippedH || flippedV);
        activity.setRotationEdited(anyYes);
    }
}
