package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;

public class FilterColorsFragment extends Fragment {
    private String filterType;

    private LinearLayout brightnessIcon;
    private LinearLayout contrastIcon;
    private LinearLayout sharpnessIcon;
    private LinearLayout saturationIcon;
    private ImageButton nextBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.filter_colors_fragment, container, false);

        brightnessIcon = view.findViewById(R.id.brightnessIcon);
        contrastIcon = view.findViewById(R.id.contrastIcon);
        sharpnessIcon = view.findViewById(R.id.sharpnessIcon);
        saturationIcon = view.findViewById(R.id.saturationIcon);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filterType = "";
                int id = view.getId();

                switch (id) {
                    case R.id.brightnessIcon:
                        filterType = "밝기";
                        break;
                    case R.id.contrastIcon:
                        filterType = "대비";
                        break;
                    case R.id.sharpnessIcon:
                        filterType = "선명하게";
                        break;
                    case R.id.saturationIcon:
                        filterType = "채도";
                        break;
                }

                if (!filterType.isEmpty()) {
                    ColorsSeekbarFragment csf = new ColorsSeekbarFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("filterType", filterType);
                    csf.setArguments(bundle);
                    csf.setPreviousFragment(FilterColorsFragment.this);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .hide(FilterColorsFragment.this)
                            .add(R.id.bottomArea, csf)
                            .commit();
                }
            }
        };

        nextBtn = view.findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea, new FilterColorsFragment())
                        .commit();
            }
        });

        brightnessIcon.setOnClickListener(listener);
        contrastIcon.setOnClickListener(listener);
        sharpnessIcon.setOnClickListener(listener);
        saturationIcon.setOnClickListener(listener);

        return view;
    }
}
