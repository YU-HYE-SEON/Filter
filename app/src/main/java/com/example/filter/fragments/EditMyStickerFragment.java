package com.example.filter.fragments;

import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.adapters.MyStickersAdapter;
import com.example.filter.etc.ClickUtils;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.List;

public class EditMyStickerFragment extends Fragment {
    private ImageButton cancelBtn;
    private ImageButton checkBtn;
    private ImageView stickerEditFrame;
    private ImageView stickerImage;
    private ImageView stickerDeleteIcon;
    private ImageView stickerSizeController;
    private FrameLayout stickerOverlay;
    private TextView txt;
    private CheckBox checkBox;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_my_sticker, container, false);

        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);
        stickerEditFrame = requireActivity().findViewById(R.id.stickerEditFrame);
        stickerImage = requireActivity().findViewById(R.id.stickerImage);
        stickerDeleteIcon = requireActivity().findViewById(R.id.stickerDeleteIcon);
        stickerSizeController = requireActivity().findViewById(R.id.stickerSizeController);
        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        txt = view.findViewById(R.id.txt);
        checkBox = view.findViewById(R.id.checkBox);


        //드래그 기능 추가 필요

        //크기조절 기능 추가 필요

        stickerDeleteIcon.setOnClickListener(v -> {
            stickerOverlay.removeAllViews();
            stickerOverlay.setX(0);
            stickerOverlay.setY(0);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea, new MyStickersFragment())
                    .commit();
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ClickUtils.isFastClick(500)) return;

                stickerEditFrame.setVisibility(View.INVISIBLE);
                stickerDeleteIcon.setVisibility(View.INVISIBLE);
                stickerSizeController.setVisibility(View.INVISIBLE);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea, new MyStickersFragment())
                        .commit();
            }
        });

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                txt.setTextColor(Color.parseColor("#C2FA7A"));
                stickerEditFrame.setVisibility(View.GONE);
                stickerImage.setVisibility(View.GONE);
                stickerDeleteIcon.setVisibility(View.GONE);
                stickerSizeController.setVisibility(View.GONE);

                GLSurfaceView photoPreview = requireActivity().findViewById(R.id.photoPreview);
                ImageView photoPreviewImage = requireActivity().findViewById(R.id.photoPreviewImage);

                photoPreview.setVisibility(View.GONE);
                photoPreviewImage.setVisibility(View.VISIBLE);
                photoPreviewImage.setImageResource(R.drawable.photo_after);
            } else {
                txt.setTextColor(Color.WHITE);
                stickerEditFrame.setVisibility(View.VISIBLE);
                stickerImage.setVisibility(View.VISIBLE);
                stickerDeleteIcon.setVisibility(View.VISIBLE);
                stickerSizeController.setVisibility(View.VISIBLE);

                GLSurfaceView photoPreview = requireActivity().findViewById(R.id.photoPreview);
                ImageView photoPreviewImage = requireActivity().findViewById(R.id.photoPreviewImage);

                photoPreviewImage.setVisibility(View.GONE);
                photoPreview.setVisibility(View.VISIBLE);
            }
        });

        return view;
    }
}
