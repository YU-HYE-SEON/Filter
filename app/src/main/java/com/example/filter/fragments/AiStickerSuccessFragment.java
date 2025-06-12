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

public class AiStickerSuccessFragment extends Fragment {
    private ImageButton checkBtn;
    private ImageView aiStickerImage;   //서버에서 받아온 AI 이미지

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frgment_aisticker_success, container, false);
        checkBtn = requireActivity().findViewById(R.id.checkBtn);
        checkBtn.setVisibility(View.VISIBLE);

        aiStickerImage=view.findViewById(R.id.aiStickerImage);


        return view;
    }
}
