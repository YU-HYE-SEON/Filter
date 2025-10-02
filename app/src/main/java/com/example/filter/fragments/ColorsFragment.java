package com.example.filter.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

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
    private TextView brightnessTxt, exposureTxt, contrastTxt,
            highlightTxt, shadowTxt, temperatureTxt, tintTxt,
            saturationTxt, sharpnessTxt, blurTxt, vignetteTxt, noiseTxt;
    private ImageView nextBtn;
    private ConstraintLayout bottomArea1;
    private ImageButton undoColor, redoColor, originalColor;
    private ImageButton undoSticker, redoSticker, originalSticker;

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

        brightnessTxt = view.findViewById(R.id.brightnessTxt);
        exposureTxt = view.findViewById(R.id.exposureTxt);
        contrastTxt = view.findViewById(R.id.contrastTxt);
        highlightTxt = view.findViewById(R.id.highlightTxt);
        shadowTxt = view.findViewById(R.id.shadowTxt);
        temperatureTxt = view.findViewById(R.id.temperatureTxt);
        tintTxt = view.findViewById(R.id.tintTxt);
        saturationTxt = view.findViewById(R.id.saturationTxt);
        sharpnessTxt = view.findViewById(R.id.sharpnessTxt);
        blurTxt = view.findViewById(R.id.blurTxt);
        vignetteTxt = view.findViewById(R.id.vignetteTxt);
        noiseTxt = view.findViewById(R.id.noiseTxt);

        bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);
        undoColor = requireActivity().findViewById(R.id.undoColor);
        redoColor = requireActivity().findViewById(R.id.redoColor);
        originalColor = requireActivity().findViewById(R.id.originalColor);
        undoSticker = requireActivity().findViewById(R.id.undoSticker);
        redoSticker = requireActivity().findViewById(R.id.redoSticker);
        originalSticker = requireActivity().findViewById(R.id.originalSticker);


        bottomArea1.setVisibility(View.VISIBLE);
        undoSticker.setVisibility(View.INVISIBLE);
        redoSticker.setVisibility(View.INVISIBLE);
        originalSticker.setVisibility(View.INVISIBLE);
        undoColor.setEnabled(false);
        redoColor.setEnabled(false);
        undoColor.setVisibility(View.VISIBLE);
        redoColor.setVisibility(View.VISIBLE);
        originalColor.setVisibility(View.VISIBLE);

        FilterActivity activity = (FilterActivity) getActivity();
        if (activity != null) {
            activity.refreshOriginalColorButton();
        }

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

        if (undoColor != null) undoColor.setVisibility(View.VISIBLE);
        if (redoColor != null) redoColor.setVisibility(View.VISIBLE);
        if (originalColor != null) originalColor.setVisibility(View.VISIBLE);

        if (undoSticker != null) undoSticker.setVisibility(View.INVISIBLE);
        if (redoSticker != null) redoSticker.setVisibility(View.INVISIBLE);
        if (originalSticker != null) originalSticker.setVisibility(View.INVISIBLE);

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
            FilterActivity act = (FilterActivity) getActivity();
            if (act != null) {
                act.requestUpdateBackGate();
                refreshColorButtons();
                updateColorIcons();
            }
        }
    }

    private void updateColorIcons() {
        FilterActivity a = (FilterActivity) getActivity();
        if (a == null) return;

        if (a.getCurrentValue("밝기") != 0) {
            brightnessIcon.setImageResource(R.drawable.icon_brightness_yes);
            brightnessTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            brightnessIcon.setImageResource(R.drawable.icon_brightness_no);
            brightnessTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("노출") != 0) {
            exposureIcon.setImageResource(R.drawable.icon_exposure_yes);
            exposureTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            exposureIcon.setImageResource(R.drawable.icon_exposure_no);
            exposureTxt.setTextColor(Color.WHITE);
        }

        //
        if (a.getCurrentValue("대비") != 0) {
            contrastIcon.setImageResource(R.drawable.icon_contrast_yes);
            contrastTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            contrastIcon.setImageResource(R.drawable.icon_contrast_no);
            contrastTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("하이라이트") != 0) {
            highlightIcon.setImageResource(R.drawable.icon_highlight_yes);
            highlightTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            highlightIcon.setImageResource(R.drawable.icon_highlight_no);
            highlightTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("그림자") != 0) {
            shadowIcon.setImageResource(R.drawable.icon_shadow_yes);
            shadowTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            shadowIcon.setImageResource(R.drawable.icon_shadow_no);
            shadowTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("온도") != 0) {
            temperatureIcon.setImageResource(R.drawable.icon_temperature_yes);
            temperatureTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            temperatureIcon.setImageResource(R.drawable.icon_temperature_no);
            temperatureTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("색조") != 0) {
            tintIcon.setImageResource(R.drawable.icon_tint_yes);
            tintTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            tintIcon.setImageResource(R.drawable.icon_tint_no);
            tintTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("채도") != 0) {
            saturationIcon.setImageResource(R.drawable.icon_saturation_yes);
            saturationTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            saturationIcon.setImageResource(R.drawable.icon_saturation_no);
            saturationTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("선명하게") != 0) {
            sharpnessIcon.setImageResource(R.drawable.icon_sharpness_yes);
            sharpnessTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            sharpnessIcon.setImageResource(R.drawable.icon_sharpness_no);
            sharpnessTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("흐리게") != 0) {
            blurIcon.setImageResource(R.drawable.icon_blur_yes);
            blurTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            blurIcon.setImageResource(R.drawable.icon_blur_no);
            blurTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("비네트") != 0) {
            vignetteIcon.setImageResource(R.drawable.icon_vignette_yes);
            vignetteTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            vignetteIcon.setImageResource(R.drawable.icon_vignette_no);
            vignetteTxt.setTextColor(Color.WHITE);
        }

        if (a.getCurrentValue("노이즈") != 0) {
            noiseIcon.setImageResource(R.drawable.icon_noise_yes);
            noiseTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            noiseIcon.setImageResource(R.drawable.icon_noise_no);
            noiseTxt.setTextColor(Color.WHITE);
        }
    }
}