package com.example.filter.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.LoadActivity;
import com.example.filter.etc.ClickUtils;

public class StickersFragment extends Fragment {
    private ImageView myStickerIcon;
    private ImageView loadStickerIcon;
    private ImageView brushIcon;
    private ImageView AIStickerIcon;
    private FrameLayout fullScreenFragmentContainer;
    private ConstraintLayout filterActivity;
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();

                    if (photoUri != null) {
                        Intent intent = new Intent(requireContext(), LoadActivity.class);
                        intent.setData(photoUri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } else {
                        Toast.makeText(requireContext(), "사진 선택 실패: URI 없음", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_stickers, container, false);

        myStickerIcon = view.findViewById(R.id.myStickerIcon);
        loadStickerIcon = view.findViewById(R.id.loadStickerIcon);
        brushIcon = view.findViewById(R.id.brushIcon);
        AIStickerIcon = view.findViewById(R.id.AIStickerIcon);

        myStickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FrameLayout overlay = requireActivity().findViewById(R.id.stickerOverlay);
                int sessionBaseline = (overlay != null) ? overlay.getChildCount() : 0;

                MyStickersFragment f = new MyStickersFragment();
                Bundle args = new Bundle();
                args.putInt("sessionBaseline", sessionBaseline);

                boolean skipDisableOnce = false;
                args.putBoolean("skipDisableOnce", skipDisableOnce);
                f.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, f)
                        .commit();
            }
        });

        loadStickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            }
        });

        brushIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                FrameLayout overlay = requireActivity().findViewById(R.id.brushOverlay);
                int sessionBaseline = (overlay != null) ? overlay.getChildCount() : 0;

                BrushFragment f = new BrushFragment();
                Bundle args = new Bundle();
                args.putInt("sessionBaseline", sessionBaseline);

                boolean skipDisableOnce = false;
                args.putBoolean("skipDisableOnce", skipDisableOnce);
                f.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, f)
                        .commit();
            }
        });

        AIStickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                fullScreenFragmentContainer = requireActivity().findViewById(R.id.fullScreenFragmentContainer);
                filterActivity = requireActivity().findViewById(R.id.filterActivity);
                ConstraintLayout main = requireActivity().findViewById(R.id.main);

                fullScreenFragmentContainer.setVisibility(View.VISIBLE);
                filterActivity.setVisibility(View.GONE);
                main.setBackgroundColor(Color.parseColor("#007AFF"));

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fullScreenFragmentContainer, new AIStickerViewFragment())
                        .addToBackStack("ai_sticker_view")
                        .commit();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        FrameLayout overlay = requireActivity().findViewById(R.id.stickerOverlay);
        if (overlay == null) return;
        for (int i = 0; i < overlay.getChildCount(); i++) {
            View child = overlay.getChildAt(i);
            MyStickersFragment.setStickerActive(child, true);
            MyStickersFragment.hideControllers(child);
            attachEditListenerForSticker(child, overlay);
        }
    }

    private void attachEditListenerForSticker(@NonNull View stickerView, @NonNull FrameLayout overlay) {
        stickerView.setOnClickListener(v -> {
            if (!stickerView.isEnabled()) return;

            Fragment cur = requireActivity()
                    .getSupportFragmentManager()
                    .findFragmentById(R.id.bottomArea2);
            if (cur instanceof EditMyStickerFragment) return;

            if (ClickUtils.isFastClick(500)) return;

            if (stickerView.findViewById(R.id.stickerImage) == null ||
                    stickerView.findViewById(R.id.editFrame) == null ||
                    stickerView.findViewById(R.id.rotateController) == null ||
                    stickerView.findViewById(R.id.sizeController) == null) {
                return;
            }

            for (int i = 0; i < overlay.getChildCount(); i++) {
                View child = overlay.getChildAt(i);
                MyStickersFragment.hideControllers(child);
                Object t = child.getTag();
                if ("editingSticker".equals(t)) {
                    child.setTag(null);
                }
            }

            for (int i = 0; i < overlay.getChildCount(); i++) {
                View child = overlay.getChildAt(i);
                child.setTag(R.id.tag_prev_enabled, child.isEnabled());
                MyStickersFragment.setStickerActive(child, child == stickerView);
            }

            setControllersVisible(stickerView, true);
            stickerView.setTag("editingSticker");

            Bundle args = new Bundle();
            args.putString("origin", "stickers");
            EditMyStickerFragment edit = new EditMyStickerFragment();
            edit.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, edit)
                    .addToBackStack("edit_from_stickers")
                    .commit();
        });
    }

    private void setControllersVisible(@NonNull View sticker, boolean visible) {
        View editFrame = sticker.findViewById(R.id.editFrame);
        View rotate = sticker.findViewById(R.id.rotateController);
        View size = sticker.findViewById(R.id.sizeController);

        int vis = visible ? View.VISIBLE : View.INVISIBLE;

        if (editFrame != null) editFrame.setVisibility(vis);
        if (rotate != null) rotate.setVisibility(vis);
        if (size != null) size.setVisibility(vis);
    }
}