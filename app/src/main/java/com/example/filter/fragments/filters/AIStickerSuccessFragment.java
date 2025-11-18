package com.example.filter.fragments.filters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

import java.io.File;

public class AIStickerSuccessFragment extends Fragment {
    private static final String IMAGE_PATH = "image_path";
    private static final String BASE_URL = "base_url";
    private static final String PROMPT = "prompt";
    private ImageView aiStickerImage;   //서버에서 받아온 AI 이미지
    private ImageButton retryBtn;
    private String imagePath;
    private String baseUrl;
    private String prompt;
    private TextView retryTxt;
    private int retryTextColorDefault;

    public static AIStickerSuccessFragment newWithImagePath(String imagePath, String baseUrl, String prompt) {
        AIStickerSuccessFragment f = new AIStickerSuccessFragment();
        Bundle b = new Bundle();
        b.putString(IMAGE_PATH, imagePath);
        b.putString(BASE_URL, baseUrl);
        b.putString(PROMPT, prompt);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_aisticker_success, container, false);
        aiStickerImage = view.findViewById(R.id.aiStickerImage);
        retryBtn = view.findViewById(R.id.retryBtn);
        retryTxt = view.findViewById(R.id.retryTxt);

        retryTextColorDefault = retryTxt.getCurrentTextColor();

        Bundle args = requireArguments();
        imagePath = requireArguments().getString(IMAGE_PATH, null);
        baseUrl   = args.getString(BASE_URL, "");
        prompt    = args.getString(PROMPT, "");

        if (imagePath != null) {
            if (imagePath.startsWith("http")) {
                // ✅ S3 URL 로드
                Glide.with(this)
                        .load(imagePath)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        /*.placeholder(R.drawable.loading_placeholder) // 로딩 중 이미지 (optional)*/
                        /*.error(R.drawable.error_placeholder)         // 실패 시 이미지 (optional)*/
                        .into(aiStickerImage);
            } else {
                // ✅ 로컬 파일 경로 로드 (기존 방식 유지)
                File f = new File(imagePath);
                if (f.exists()) {
                    Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                    aiStickerImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    aiStickerImage.setImageBitmap(bmp);
                }
            }
        }

        ClickUtils.clickDim(retryBtn);
        retryBtn.setOnClickListener(v -> {
            if (!isAdded()) return;
            getParentFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.aiStickerView,
                            AIStickerLoadingFragment.newInstance(baseUrl, prompt))
                    .commit();
        });

        return view;
    }

    public @Nullable Bitmap getCurrentBitmap() {
        if (aiStickerImage != null && aiStickerImage.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
            return ((android.graphics.drawable.BitmapDrawable) aiStickerImage.getDrawable()).getBitmap();
        }
        if (imagePath != null) {
            return BitmapFactory.decodeFile(imagePath);
        }
        return null;
    }

    public @Nullable String getCurrentImagePath() {
        return imagePath;
    }
}
