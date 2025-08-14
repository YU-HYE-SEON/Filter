package com.example.filter.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.File;

public class AiStickerSuccessFragment extends Fragment {
    private static final String IMAGE_PATH = "image_path";
    private static final String BASE_URL = "base_url";
    private static final String PROMPT = "prompt";
    //private static final String BASE_URL = "base_url";
    //private static final String PROMPT = "prompt";
    private ImageView aiStickerImage;   //서버에서 받아온 AI 이미지
    private ImageButton retryBtn;
    private String imagePath;
    private String baseUrl;
    private String prompt;
    //private Call<ResponseBody> inflight;

    public static AiStickerSuccessFragment newWithImagePath(String imagePath, String baseUrl, String prompt) {
        AiStickerSuccessFragment f = new AiStickerSuccessFragment();
        Bundle b = new Bundle();
        b.putString(IMAGE_PATH, imagePath);
        b.putString(BASE_URL, baseUrl);
        b.putString(PROMPT, prompt);
        f.setArguments(b);
        return f;
    }

    /*public static AiStickerSuccessFragment newInstance(String baseUrl, String prompt) {
        AiStickerSuccessFragment f = new AiStickerSuccessFragment();
        Bundle b = new Bundle();
        b.putString(BASE_URL, baseUrl);
        b.putString(PROMPT, prompt);
        f.setArguments(b);
        return f;
    }*/

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frgment_aisticker_success, container, false);
        aiStickerImage = view.findViewById(R.id.aiStickerImage);
        retryBtn = view.findViewById(R.id.retryBtn);

        Bundle args = requireArguments();
        imagePath = requireArguments().getString(IMAGE_PATH, null);
        baseUrl   = args.getString(BASE_URL, "");
        prompt    = args.getString(PROMPT, "");

        if (imagePath != null) {
            File f = new File(imagePath);
            if (f.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                aiStickerImage.setImageBitmap(bmp);
            }
            // 로딩은 이미 끝났으므로 retry 버튼 동작을 "다시 생성" 등으로 재정의하거나 숨겨도 됨.
            // retryBtn.setVisibility(View.GONE);
        }


        /*baseUrl = requireArguments().getString(BASE_URL);
        prompt  = requireArguments().getString(PROMPT, "");

        requestSticker(baseUrl, prompt);*/

        retryBtn.setOnClickListener(v -> {
            /*if (inflight != null && !inflight.isCanceled()) inflight.cancel();
            requestSticker(baseUrl, prompt);*/
            if (!isAdded()) return;
            getParentFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.aiStickerView,
                            AiStickerLoadingFragment.newInstance(baseUrl, prompt))
                    .commit();
        });

        return view;
    }

    /*private void requestSticker(String baseUrl, String prompt) {
        ApiService api = RetrofitClient.create(baseUrl).create(ApiService.class);

        inflight = api.generateSticker(new PromptRequest(prompt));

        inflight.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> resp) {
                if (!isAdded()) return;
                if (resp.isSuccessful() && resp.body() != null) {
                    try (InputStream is = resp.body().byteStream()) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        aiStickerImage.setImageBitmap(bmp);
                    } catch (Exception e) { e.printStackTrace(); }
                } else {
                    // TODO: 오류 토스트/로그
                }
            }

            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (!isAdded()) return;
                if (call.isCanceled()) return;
                t.printStackTrace();
                // TODO: 네트워크 오류 안내
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (inflight != null) inflight.cancel();
    }*/
}
