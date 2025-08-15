package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import java.util.Arrays;
import java.util.List;

public class MyStickersFragment extends Fragment {
    private ImageButton cancelBtn;
    private ImageButton checkBtn;
    private RecyclerView myStickers;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_my_stickers, container, false);

        myStickers = view.findViewById(R.id.myStickers);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        myStickers.setLayoutManager(layoutManager);
        List<String> imageNames = Arrays.asList("sticker_hearts", "sticker_blueheart", "sticker_cyanheart");
        MyStickersAdapter adapter = new MyStickersAdapter(imageNames);
        myStickers.setAdapter(adapter);

        FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);

        adapter.setOnStickerClickListener(stickerName -> {
            View stickerLayout = inflater.inflate(R.layout.v_sticker_edit, stickerOverlay, false);

            ImageView stickerImageView = stickerLayout.findViewById(R.id.stickerImage);
            int resId = getResources().getIdentifier(stickerName, "drawable", requireContext().getPackageName());
            if (resId != 0) {
                stickerImageView.setImageResource(resId);
            }

            int sizePx = dpToPx(230);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizePx, sizePx);
            stickerLayout.setLayoutParams(params);

            stickerImageView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    EditMyStickerFragment editFragment = new EditMyStickerFragment();
                    Bundle args = new Bundle();
                    args.putString("stickerName", stickerName);
                    editFragment.setArguments(args);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_up, 0)
                            .replace(R.id.bottomArea2, new EditMyStickerFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                }
                return false;
            });

            stickerOverlay.post(() -> {
                int width = stickerOverlay.getWidth();
                int height = stickerOverlay.getHeight();
                float centerX = (width - sizePx) / 2f;
                float centerY = (height - sizePx) / 2f;
                stickerLayout.setX(centerX);
                stickerLayout.setY(centerY);
                stickerOverlay.addView(stickerLayout);
            });
        });

        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new StickersFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, new StickersFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        return view;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
