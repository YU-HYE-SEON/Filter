package com.example.filter.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class AiStickerSuccessFragment extends Fragment {
    private ImageButton checkBtn;
    private ImageView aiStickerImage;   //서버에서 받아온 AI 이미지
    private FrameLayout fullScreenFragmentContainer;
    private ConstraintLayout filterActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frgment_aisticker_success, container, false);
        checkBtn = requireActivity().findViewById(R.id.checkBtn);
        checkBtn.setVisibility(View.VISIBLE);

        aiStickerImage = view.findViewById(R.id.aiStickerImage);

        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                fullScreenFragmentContainer = requireActivity().findViewById(R.id.fullScreenFragmentContainer);
                filterActivity = requireActivity().findViewById(R.id.filterActivity);
                ConstraintLayout main = requireActivity().findViewById(R.id.main);

                fullScreenFragmentContainer.setVisibility(View.GONE);
                filterActivity.setVisibility(View.VISIBLE);
                main.setBackgroundColor(Color.BLACK);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.bottomArea, new StickersFragment())
                        .remove(AiStickerSuccessFragment.this)
                        .commit();
            }
        });
        return view;
    }
}
