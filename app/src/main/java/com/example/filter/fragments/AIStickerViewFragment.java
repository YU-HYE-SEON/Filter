package com.example.filter.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.dialogs.FilterEixtDialog;
import com.example.filter.etc.ClickUtils;
import com.example.filter.items.StickerItem;
import com.example.filter.etc.StickerStore;

import java.io.File;
import java.io.FileOutputStream;

public class AIStickerViewFragment extends Fragment {
    private FrameLayout aiStickerView;
    private ImageButton cancelBtn, checkBtn;
    private FrameLayout fullScreenFragmentContainer;
    private ConstraintLayout filterActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_aisticker_view, container, false);
        aiStickerView = view.findViewById(R.id.aiStickerView);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        Window window = requireActivity().getWindow();
        window.setNavigationBarColor(Color.parseColor("#007AFF"));

        checkBtn.setVisibility(View.INVISIBLE);

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.aiStickerView, new AIStickerCreateFragment())
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
                    @Override
                    public void handleOnBackPressed() {
                        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                            getChildFragmentManager().popBackStack();
                        } else {
                            new FilterEixtDialog(requireContext(), new FilterEixtDialog.FilterEixtDialogListener() {
                                @Override
                                public void onKeep() {
                                }

                                @Override
                                public void onExit() {
                                    requireActivity().finish();
                                }
                            }
                            ).withMessage("편집한 내용을 저장하지 않고\n종료하시겠습니까?")
                                    .withButton1Text("예")
                                    .withButton2Text("아니오")
                                    .show();
                        }
                    }
                });

        cancelBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                closeSelfSafely();
            }
        });

        checkBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            Fragment current = getChildFragmentManager().findFragmentById(R.id.aiStickerView);
            if (current instanceof AIStickerSuccessFragment) {
                AIStickerSuccessFragment f = (AIStickerSuccessFragment) current;

                Bitmap bmp = f.getCurrentBitmap();
                if (bmp != null) {
                    try {
                        File dir = new File(requireContext().getFilesDir(), "stickers");
                        if (!dir.exists()) dir.mkdirs();
                        File out = new File(dir, "ai_" + System.currentTimeMillis() + ".png");
                        try (FileOutputStream fos = new FileOutputStream(out)) {
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        }
                        StickerItem item = StickerItem.fromFile(out.getAbsolutePath());
                        StickerStore.get().enqueuePending(item);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String p = f.getCurrentImagePath();
                    if (p != null) {
                        StickerItem item = StickerItem.fromFile(p);
                        StickerStore.get().enqueuePending(item);
                    }
                }
            }

            if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                closeSelfSafely();
            }
        });

        return view;
    }

    private void updateCheckBtnVisibility() {
        Fragment current = getChildFragmentManager().findFragmentById(R.id.aiStickerView);
        if (current instanceof AIStickerSuccessFragment) {
            checkBtn.setVisibility(View.VISIBLE);
        } else {
            checkBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void closeSelfSafely() {
        View root = getView();
        if (root != null) {
            FrameLayout full = requireActivity().findViewById(R.id.fullScreenFragmentContainer);
            ConstraintLayout filter = requireActivity().findViewById(R.id.filterActivity);
            ConstraintLayout main = requireActivity().findViewById(R.id.main);

            if (full != null) full.setVisibility(View.GONE);
            if (filter != null) filter.setVisibility(View.VISIBLE);
            if (main != null) main.setBackgroundColor(Color.BLACK);
        }

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .remove(this)
                .commitAllowingStateLoss();
    }
}
