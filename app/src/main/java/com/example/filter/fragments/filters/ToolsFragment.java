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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.filter.FilterActivity;
import com.example.filter.etc.ClickUtils;

public class ToolsFragment extends Fragment {
    private ImageButton closeBtn;
    private LinearLayout rotationBtn, cropBtn, nextBtn;
    private ImageView rotationIcon, cropIcon;
    private TextView rotationTxt, cropTxt;
    private ConstraintLayout bottomArea1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_tools, container, false);

        rotationBtn = view.findViewById(R.id.rotationBtn);
        cropBtn = view.findViewById(R.id.cropBtn);
        rotationIcon = view.findViewById(R.id.rotationIcon);
        cropIcon = view.findViewById(R.id.cropIcon);
        rotationTxt = view.findViewById(R.id.rotationTxt);
        cropTxt = view.findViewById(R.id.cropTxt);

        FilterActivity activity = (FilterActivity) requireActivity();

        bottomArea1 = activity.findViewById(R.id.bottomArea1);
        bottomArea1.setVisibility(View.INVISIBLE);

        activity.updateSaveButtonState();

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


        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeBtn.performClick();
            }
        });


        rotationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new RotationFragment())

                        .addToBackStack(null)

                        .commit();
            }
        });

        cropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                if (!activity.isCropEdited()) {
                    activity.resetCropState();
                }

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new CropFragment())

                        .addToBackStack(null)

                        .commit();
            }
        });

        nextBtn = view.findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new ColorsFragment())

                        .addToBackStack(null)

                        .commit();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        closeBtn = requireActivity().findViewById(R.id.closeBtn);
        if (closeBtn != null) {
            closeBtn.setEnabled(true);
            closeBtn.setAlpha(1.0f);
        }
    }
}
