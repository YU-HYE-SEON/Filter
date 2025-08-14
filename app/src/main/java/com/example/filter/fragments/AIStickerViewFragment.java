package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class AIStickerViewFragment extends Fragment {
    private FrameLayout aiStickerView;
    private ImageButton cancelBtn,checkBtn;
    private FrameLayout fullScreenFragmentContainer;
    private ConstraintLayout filterActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aisticker_view, container, false);
        aiStickerView = view.findViewById(R.id.aiStickerView);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        checkBtn.setVisibility(View.INVISIBLE);

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.aiStickerView, new AiStickerCreateFragment())
                .commit();

        getChildFragmentManager().addOnBackStackChangedListener(this::updateCheckBtnVisibility);
        view.post(this::updateCheckBtnVisibility);

        getChildFragmentManager().registerFragmentLifecycleCallbacks(
                new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull androidx.fragment.app.FragmentManager fm,
                                                      @NonNull Fragment f, @NonNull View v, @Nullable Bundle s) {
                        updateCheckBtnVisibility();
                    }
                    @Override
                    public void onFragmentResumed(@NonNull androidx.fragment.app.FragmentManager fm,
                                                  @NonNull Fragment f) {
                        updateCheckBtnVisibility();
                    }
                    @Override
                    public void onFragmentDestroyed(@NonNull androidx.fragment.app.FragmentManager fm,
                                                    @NonNull Fragment f) {
                        updateCheckBtnVisibility();
                    }
                }, true
        );

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                            getChildFragmentManager().popBackStack();
                        } else {
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    }
                });

        cancelBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        checkBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }

    private void updateCheckBtnVisibility() {
        Fragment current = getChildFragmentManager().findFragmentById(R.id.aiStickerView);
        if (current instanceof AiStickerSuccessFragment) {
            checkBtn.setVisibility(View.VISIBLE);
        } else {
            checkBtn.setVisibility(View.INVISIBLE);
        }
    }
}
