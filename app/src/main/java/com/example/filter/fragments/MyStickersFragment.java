package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.adapters.MyStickersAdapter;
import com.example.filter.etc.ClickUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyStickersFragment extends Fragment {
    private ImageButton cancelBtn, checkBtn;
    private ImageButton deleteStickerIcon;
    private RecyclerView myStickers;
    private FrameLayout stickerOverlay;
    private View currentStickerView = null;
    private String currentStickerName = null;
    private int currentSelectedPos = RecyclerView.NO_POSITION;
    private LayoutInflater cachedInflater;
    private MyStickersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.f_my_stickers, container, false);
        cachedInflater = inflater;

        myStickers = view.findViewById(R.id.myStickers);
        deleteStickerIcon = view.findViewById(R.id.deleteStickerIcon);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn  = view.findViewById(R.id.checkBtn);
        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);

        deleteStickerIcon.setEnabled(false);
        deleteStickerIcon.setAlpha(0.4f);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false);
        myStickers.setLayoutManager(lm);

        List<String> imageNames = new ArrayList<>(Arrays.asList(
                "sticker_hearts","sticker_blueheart","sticker_cyanheart"
        ));
        adapter = new MyStickersAdapter(imageNames);
        myStickers.setAdapter(adapter);

        myStickers.setItemAnimator(null);

        adapter.setOnStickerClickListener((position, stickerName) -> {
            currentSelectedPos = position;
            deleteStickerIcon.setEnabled(true);
            deleteStickerIcon.setAlpha(1.0f);
            showStickerCentered(stickerName);
        });

        deleteStickerIcon.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            if (!deleteStickerIcon.isEnabled()) return;
            if (currentSelectedPos != RecyclerView.NO_POSITION) {
                clearCurrentSticker();
                adapter.removeAt(currentSelectedPos);
                currentSelectedPos = RecyclerView.NO_POSITION;
                currentStickerName = null;

                deleteStickerIcon.setEnabled(false);
                deleteStickerIcon.setAlpha(0.4f);
            }
        });

        cancelBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            stickerOverlay.removeAllViews();
            currentStickerView = null; currentStickerName = null; currentSelectedPos = RecyclerView.NO_POSITION;

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        checkBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        return view;
    }

    private void showStickerCentered(String stickerName) {
        clearCurrentSticker();

        View stickerLayout = cachedInflater.inflate(R.layout.v_sticker_edit, stickerOverlay, false);

        ImageView stickerImage = stickerLayout.findViewById(R.id.stickerImage);
        int resId = getResources().getIdentifier(stickerName, "drawable", requireContext().getPackageName());
        if (resId != 0) stickerImage.setImageResource(resId);

        int sizePx = dpToPx(230);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx,sizePx);
        stickerLayout.setLayoutParams(lp);

        stickerLayout.setOnClickListener(v -> openEdit(stickerName, stickerLayout));

        stickerOverlay.post(() -> {
            float cx = (stickerOverlay.getWidth() - sizePx) / 2f;
            float cy = (stickerOverlay.getHeight()- sizePx) / 2f;
            stickerLayout.setX(cx);
            stickerLayout.setY(cy);
            stickerOverlay.addView(stickerLayout);

            currentStickerView = stickerLayout;
            currentStickerName = stickerName;
        });
    }

    private void clearCurrentSticker() {
        if (currentStickerView != null && currentStickerView.getParent() == stickerOverlay) {
            stickerOverlay.removeView(currentStickerView);
        }
        currentStickerView = null;
    }

    private void openEdit(String stickerName, View stickerLayout){
        Bundle args = new Bundle();
        args.putString("stickerName", stickerName);
        args.putFloat("x", stickerLayout.getX());
        args.putFloat("y", stickerLayout.getY());
        args.putInt("w", stickerLayout.getLayoutParams().width);
        args.putInt("h", stickerLayout.getLayoutParams().height);
        args.putFloat("rotation", stickerLayout.getRotation());

        stickerLayout.setTag("editingSticker");

        EditMyStickerFragment edit = new EditMyStickerFragment();
        edit.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_up, 0)
                .replace(R.id.bottomArea2, edit)
                .commit();

        ImageView editFrame = stickerLayout.findViewById(R.id.editFrame);
        ImageView sizeController = stickerLayout.findViewById(R.id.sizeController);
        ImageView rotateController= stickerLayout.findViewById(R.id.rotateController);
        editFrame.setVisibility(View.VISIBLE);
        sizeController.setVisibility(View.VISIBLE);
        rotateController.setVisibility(View.VISIBLE);
    }

    private int dpToPx(int dp){
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }
}