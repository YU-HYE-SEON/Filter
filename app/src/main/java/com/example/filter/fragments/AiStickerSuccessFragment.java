package com.example.filter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;

public class AiStickerSuccessFragment extends Fragment {
    private ImageButton checkBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frgment_aisticker_success, container, false);
        checkBtn = requireActivity().findViewById(R.id.checkBtn);
        checkBtn.setVisibility(View.VISIBLE);


        return view;
    }
}
