package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
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
    private ConstraintLayout bottomArea1;
    private ImageButton undoColor, redoColor, originalColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_colors, container, false);

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

        bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);
        undoColor = requireActivity().findViewById(R.id.undoColor);
        redoColor = requireActivity().findViewById(R.id.redoColor);
        originalColor = requireActivity().findViewById(R.id.originalColor);

        bottomArea1.setVisibility(View.VISIBLE);
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
                                .commit();

                        ConstraintLayout bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);
                        bottomArea1.setVisibility(View.INVISIBLE);
                    }
                }).withMessage("스티커를 추가하면 사진 비율이 고정됩니다.\n현재 비율을 유지하시겠습니까?")
                        .withButton1Text("변경")
                        .withButton2Text("유지")
                        .show();
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

        updateColorIcons();

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

        updateColorIcons();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshColorButtons();
        updateColorIcons();

        FilterActivity activity = (FilterActivity) getActivity();

        if (activity != null) {
            activity.refreshOriginalColorButton();
            activity.requestUpdateBackGate();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshColorButtons();
            updateColorIcons();
            FilterActivity act = (FilterActivity) getActivity();
            if (act != null) act.requestUpdateBackGate();
        }
    }

    private void updateColorIcons() {
        FilterActivity a = (FilterActivity) getActivity();
        if (a == null) return;

        setIcon(brightnessIcon,  a.getCurrentValue("밝기")       != 0, R.drawable.brightness_icon_yes,  R.drawable.brightness_icon_no);
        setIcon(exposureIcon,    a.getCurrentValue("노출")       != 0, R.drawable.exposure_icon_yes,    R.drawable.exposure_icon_no);
        setIcon(contrastIcon,    a.getCurrentValue("대비")       != 0, R.drawable.contrast_icon_yes,    R.drawable.contrast_icon_no);
        setIcon(highlightIcon,   a.getCurrentValue("하이라이트") != 0, R.drawable.highlight_icon_yes,   R.drawable.highlight_icon_no);
        setIcon(shadowIcon,      a.getCurrentValue("그림자")     != 0, R.drawable.shadow_icon_yes,      R.drawable.shadow_icon_no);
        setIcon(temperatureIcon, a.getCurrentValue("온도")       != 0, R.drawable.temperature_icon_yes, R.drawable.temperature_icon_no);
        setIcon(tintIcon,        a.getCurrentValue("색조")       != 0, R.drawable.hue_icon_yes,         R.drawable.hue_icon_no);
        setIcon(saturationIcon,  a.getCurrentValue("채도")       != 0, R.drawable.saturation_icon_yes,  R.drawable.saturation_icon_no);
        setIcon(sharpnessIcon,   a.getCurrentValue("선명하게")   != 0, R.drawable.sharpness_icon_yes,   R.drawable.sharpness_icon_no);
        setIcon(blurIcon,        a.getCurrentValue("흐리게")     != 0, R.drawable.blur_icon_yes,        R.drawable.blur_icon_no);
        setIcon(vignetteIcon,    a.getCurrentValue("비네트")     != 0, R.drawable.vignette_icon_yes,    R.drawable.vignette_icon_no);
        setIcon(noiseIcon,       a.getCurrentValue("노이즈")     != 0, R.drawable.noise_icon_no,       R.drawable.noise_icon_no);
    }

    private void setIcon(ImageView iv, boolean on, int yesRes, int noRes) {
        if (iv != null) iv.setImageResource(on ? yesRes : noRes);
    }
}