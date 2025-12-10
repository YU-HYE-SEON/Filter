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
import com.example.filter.dialogs.StickersDialog;
import com.example.filter.etc.ClickUtils;

public class ColorsFragment extends Fragment {
    private ImageButton closeBtn;
    private String filterType;
    private LinearLayout brightnessBtn, exposureBtn, contrastBtn,
            highlightBtn, shadowBtn, temperatureBtn, hueBtn,
            saturationBtn, sharpnessBtn, blurBtn, vignetteBtn, noiseBtn;
    private ImageView brightnessIcon, exposureIcon, contrastIcon,
            highlightIcon, shadowIcon, temperatureIcon, hueIcon,
            saturationIcon, sharpnessIcon, blurIcon, vignetteIcon, noiseIcon;
    private TextView brightnessTxt, exposureTxt, contrastTxt,
            highlightTxt, shadowTxt, temperatureTxt, hueTxt,
            saturationTxt, sharpnessTxt, blurTxt, vignetteTxt, noiseTxt;
    private LinearLayout prevBtn,nextBtn;
    private ConstraintLayout bottomArea1;
    private ImageButton undoColor, redoColor, originalColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_colors, container, false);

        brightnessBtn = view.findViewById(R.id.brightnessBtn);
        brightnessIcon = view.findViewById(R.id.brightnessIcon);
        brightnessTxt = view.findViewById(R.id.brightnessTxt);

        exposureBtn = view.findViewById(R.id.exposureBtn);
        exposureIcon = view.findViewById(R.id.exposureIcon);
        exposureTxt = view.findViewById(R.id.exposureTxt);

        contrastBtn = view.findViewById(R.id.contrastBtn);
        contrastIcon = view.findViewById(R.id.contrastIcon);
        contrastTxt = view.findViewById(R.id.contrastTxt);

        highlightBtn = view.findViewById(R.id.highlightBtn);
        highlightIcon = view.findViewById(R.id.highlightIcon);
        highlightTxt = view.findViewById(R.id.highlightTxt);

        shadowBtn = view.findViewById(R.id.shadowBtn);
        shadowIcon = view.findViewById(R.id.shadowIcon);
        shadowTxt = view.findViewById(R.id.shadowTxt);

        temperatureBtn = view.findViewById(R.id.temperatureBtn);
        temperatureIcon = view.findViewById(R.id.temperatureIcon);
        temperatureTxt = view.findViewById(R.id.temperatureTxt);

        hueBtn = view.findViewById(R.id.hueBtn);
        hueIcon = view.findViewById(R.id.hueIcon);
        hueTxt = view.findViewById(R.id.hueTxt);

        saturationBtn = view.findViewById(R.id.saturationBtn);
        saturationIcon = view.findViewById(R.id.saturationIcon);
        saturationTxt = view.findViewById(R.id.saturationTxt);

        sharpnessBtn = view.findViewById(R.id.sharpnessBtn);
        sharpnessIcon = view.findViewById(R.id.sharpnessIcon);
        sharpnessTxt = view.findViewById(R.id.sharpnessTxt);

        blurBtn = view.findViewById(R.id.blurBtn);
        blurIcon = view.findViewById(R.id.blurIcon);
        blurTxt = view.findViewById(R.id.blurTxt);

        vignetteBtn = view.findViewById(R.id.vignetteBtn);
        vignetteIcon = view.findViewById(R.id.vignetteIcon);
        vignetteTxt = view.findViewById(R.id.vignetteTxt);

        noiseBtn = view.findViewById(R.id.noiseBtn);
        noiseIcon = view.findViewById(R.id.noiseIcon);
        noiseTxt = view.findViewById(R.id.noiseTxt);

        nextBtn = view.findViewById(R.id.nextBtn);
        prevBtn = view.findViewById(R.id.prevBtn);

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
        if (activity != null) {
            activity.refreshOriginalColorButton();
        }

        undoColor.setOnClickListener(v -> {
            if (activity != null) {
                activity.previewOriginalColors(false);
                activity.undoColor();
                activity.updateSaveButtonState();
            }
            refreshColorButtons();
        });

        redoColor.setOnClickListener(v -> {
            if (activity != null) {
                activity.previewOriginalColors(false);
                activity.redoColor();
                activity.updateSaveButtonState();
            }
            refreshColorButtons();
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                filterType = "";
                int id = view.getId();

                switch (id) {
                    case R.id.brightnessBtn:
                        filterType = "밝기";
                        break;
                    case R.id.exposureBtn:
                        filterType = "노출";
                        break;
                    case R.id.contrastBtn:
                        filterType = "대비";
                        break;
                    case R.id.highlightBtn:
                        filterType = "하이라이트";
                        break;
                    case R.id.shadowBtn:
                        filterType = "그림자";
                        break;
                    case R.id.temperatureBtn:
                        filterType = "온도";
                        break;
                    case R.id.hueBtn:
                        filterType = "색조";
                        break;
                    case R.id.saturationBtn:
                        filterType = "채도";
                        break;
                    case R.id.sharpnessBtn:
                        filterType = "선명하게";
                        break;
                    case R.id.blurBtn:
                        filterType = "흐리게";
                        break;
                    case R.id.vignetteBtn:
                        filterType = "비네트";
                        break;
                    case R.id.noiseBtn:
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


        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                prevBtn.performClick();
            }
        });


        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new ToolsFragment())
                        .commit();

                ConstraintLayout bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);
                bottomArea1.setVisibility(View.INVISIBLE);
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(view, 400)) return;

                new StickersDialog(requireContext(), new StickersDialog.StickersDialogListener() {
                    @Override
                    public void onKeep() {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .setCustomAnimations(R.anim.slide_up, 0)
                                .replace(R.id.bottomArea2, new StickersFragment())

                                .addToBackStack(null)

                                .commit();

                        ConstraintLayout bottomArea1 = requireActivity().findViewById(R.id.bottomArea1);
                        bottomArea1.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onChange() {

                    }
                }).show();
            }
        });

        brightnessBtn.setOnClickListener(listener);
        exposureBtn.setOnClickListener(listener);
        contrastBtn.setOnClickListener(listener);
        highlightBtn.setOnClickListener(listener);
        shadowBtn.setOnClickListener(listener);
        temperatureBtn.setOnClickListener(listener);
        hueBtn.setOnClickListener(listener);
        saturationBtn.setOnClickListener(listener);
        sharpnessBtn.setOnClickListener(listener);
        blurBtn.setOnClickListener(listener);
        vignetteBtn.setOnClickListener(listener);
        noiseBtn.setOnClickListener(listener);

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

        closeBtn = requireActivity().findViewById(R.id.closeBtn);
        if (closeBtn != null) {
            closeBtn.setEnabled(true);
            closeBtn.setAlpha(1.0f);
        }

        refreshColorButtons();
        updateColorIcons();

        if (undoColor != null) undoColor.setVisibility(View.VISIBLE);
        if (redoColor != null) redoColor.setVisibility(View.VISIBLE);
        if (originalColor != null) originalColor.setVisibility(View.VISIBLE);

        FilterActivity activity = (FilterActivity) getActivity();
        if (activity != null) {
            activity.refreshOriginalColorButton();
            activity.updateSaveButtonState();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            FilterActivity activity = (FilterActivity) getActivity();
            if (activity != null) {
                refreshColorButtons();
                updateColorIcons();
                activity.updateSaveButtonState();
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
            brightnessTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("노출") != 0) {
            exposureIcon.setImageResource(R.drawable.icon_exposure_yes);
            exposureTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            exposureIcon.setImageResource(R.drawable.icon_exposure_no);
            exposureTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("대비") != 0) {
            contrastIcon.setImageResource(R.drawable.icon_contrast_yes);
            contrastTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            contrastIcon.setImageResource(R.drawable.icon_contrast_no);
            contrastTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("하이라이트") != 0) {
            highlightIcon.setImageResource(R.drawable.icon_highlight_yes);
            highlightTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            highlightIcon.setImageResource(R.drawable.icon_highlight_no);
            highlightTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("그림자") != 0) {
            shadowIcon.setImageResource(R.drawable.icon_shadow_yes);
            shadowTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            shadowIcon.setImageResource(R.drawable.icon_shadow_no);
            shadowTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("온도") != 0) {
            temperatureIcon.setImageResource(R.drawable.icon_temperature_yes);
            temperatureTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            temperatureIcon.setImageResource(R.drawable.icon_temperature_no);
            temperatureTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("색조") != 0) {
            hueIcon.setImageResource(R.drawable.icon_hue_yes);
            hueTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            hueIcon.setImageResource(R.drawable.icon_hue_no);
            hueTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("채도") != 0) {
            saturationIcon.setImageResource(R.drawable.icon_saturation_yes);
            saturationTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            saturationIcon.setImageResource(R.drawable.icon_saturation_no);
            saturationTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("선명하게") != 0) {
            sharpnessIcon.setImageResource(R.drawable.icon_sharpness_yes);
            sharpnessTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            sharpnessIcon.setImageResource(R.drawable.icon_sharpness_no);
            sharpnessTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("흐리게") != 0) {
            blurIcon.setImageResource(R.drawable.icon_blur_yes);
            blurTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            blurIcon.setImageResource(R.drawable.icon_blur_no);
            blurTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("비네트") != 0) {
            vignetteIcon.setImageResource(R.drawable.icon_vignette_yes);
            vignetteTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            vignetteIcon.setImageResource(R.drawable.icon_vignette_no);
            vignetteTxt.setTextColor(Color.parseColor("#90989F"));
        }

        if (a.getCurrentValue("노이즈") != 0) {
            noiseIcon.setImageResource(R.drawable.icon_noise_yes);
            noiseTxt.setTextColor(Color.parseColor("#C2FA7A"));
        } else {
            noiseIcon.setImageResource(R.drawable.icon_noise_no);
            noiseTxt.setTextColor(Color.parseColor("#90989F"));
        }
    }
}