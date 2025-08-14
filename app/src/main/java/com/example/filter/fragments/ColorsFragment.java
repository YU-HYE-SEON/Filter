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
import com.example.filter.dialogs.StickersDialog;
import com.example.filter.etc.ClickUtils;

public class ColorsFragment extends Fragment {
    private String filterType;
    private ImageView brightnessIcon, exposureIcon, contrastIcon,
            highlightIcon, shadowIcon, temperatureIcon, tintIcon,
            saturationIcon, sharpnessIcon, blurIcon, vignetteIcon, noiseIcon;
    private ImageView nextBtn;
    private ImageButton undoColor, redoColor, originalColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_colors, container, false);

        brightnessIcon = view.findViewById(R.id.brightnessIcon);
        exposureIcon = view.findViewById(R.id.exposureIcon);
        contrastIcon = view.findViewById(R.id.contrastIcon);
        highlightIcon = view.findViewById(R.id.highlightIcon);
        shadowIcon = view.findViewById(R.id.shadowIcon);
        temperatureIcon = view.findViewById(R.id.temperatureIcon);
        tintIcon = view.findViewById(R.id.tintIcon);
        saturationIcon = view.findViewById(R.id.saturationIcon);
        sharpnessIcon = view.findViewById(R.id.sharpnessIcon);
        blurIcon = view.findViewById(R.id.blurIcon);
        vignetteIcon = view.findViewById(R.id.vignetteIcon);
        noiseIcon = view.findViewById(R.id.noiseIcon);
        nextBtn = view.findViewById(R.id.nextBtn);

        undoColor = requireActivity().findViewById(R.id.undoColor);
        redoColor = requireActivity().findViewById(R.id.redoColor);
        originalColor = requireActivity().findViewById(R.id.originalColor);

        undoColor.setEnabled(false);
        redoColor.setEnabled(false);
        undoColor.setVisibility(View.VISIBLE);
        redoColor.setVisibility(View.VISIBLE);
        originalColor.setVisibility(View.VISIBLE);

        FilterActivity activity = (FilterActivity) getActivity();
        if (activity != null) activity.refreshOriginalColorButton();

        undoColor.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            if (activity != null) {
                activity.previewOriginalColors(false);
                activity.undoColor();
            }
            refreshColorButtons();
        });

        redoColor.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            if (activity != null) {
                activity.previewOriginalColors(false);
                activity.redoColor();
            }
            refreshColorButtons();
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                filterType = "";
                int id = view.getId();

                switch (id) {
                    case R.id.brightnessIcon:
                        filterType = "밝기";
                        break;
                    case R.id.exposureIcon:
                        filterType = "노출";
                        break;
                    case R.id.contrastIcon:
                        filterType = "대비";
                        break;
                    case R.id.highlightIcon:
                        filterType = "하이라이트";
                        break;
                    case R.id.shadowIcon:
                        filterType = "그림자";
                        break;
                    case R.id.temperatureIcon:
                        filterType = "온도";
                        break;
                    case R.id.tintIcon:
                        filterType = "색조";
                        break;
                    case R.id.saturationIcon:
                        filterType = "채도";
                        break;
                    case R.id.sharpnessIcon:
                        filterType = "선명하게";
                        break;
                    case R.id.blurIcon:
                        filterType = "흐리게";
                        break;
                    case R.id.vignetteIcon:
                        filterType = "비네트";
                        break;
                    case R.id.noiseIcon:
                        filterType = "노이즈";
                        break;
                }

                if (!filterType.isEmpty()) {
                    CustomseekbarFragment csf = new CustomseekbarFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("filterType", filterType);
                    csf.setArguments(bundle);
                    csf.setPreviousFragment(ColorsFragment.this);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .hide(ColorsFragment.this)
                            .add(R.id.bottomArea2, csf)
                            .addToBackStack(null)
                            .commit();
                }
            }
        };

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                new StickersDialog(requireContext(), new StickersDialog.StickersDialogListener() {
                    @Override
                    public void onKeep() {

                    }

                    @Override
                    public void onChange() {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .setCustomAnimations(R.anim.slide_up, 0)
                                .replace(R.id.bottomArea2, new StickersFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                }).show();
            }
        });

        brightnessIcon.setOnClickListener(listener);
        exposureIcon.setOnClickListener(listener);
        contrastIcon.setOnClickListener(listener);
        highlightIcon.setOnClickListener(listener);
        shadowIcon.setOnClickListener(listener);
        temperatureIcon.setOnClickListener(listener);
        tintIcon.setOnClickListener(listener);
        saturationIcon.setOnClickListener(listener);
        sharpnessIcon.setOnClickListener(listener);
        blurIcon.setOnClickListener(listener);
        vignetteIcon.setOnClickListener(listener);
        noiseIcon.setOnClickListener(listener);

        return view;
    }

    private void refreshColorButtons() {
        FilterActivity activity = (FilterActivity) getActivity();
        if (activity == null) return;

        undoColor.setVisibility(View.VISIBLE);
        redoColor.setVisibility(View.VISIBLE);

        boolean canUndo = activity.canUndoColor();
        boolean canRedo = activity.canRedoColor();

        undoColor.setEnabled(canUndo);
        redoColor.setEnabled(canRedo);

        undoColor.setAlpha(canUndo ? 1f : 0.4f);
        redoColor.setAlpha(canRedo ? 1f : 0.4f);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshColorButtons();

        FilterActivity activity = (FilterActivity) getActivity();
        if (activity != null) activity.refreshOriginalColorButton();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshColorButtons();
        }
    }
}
