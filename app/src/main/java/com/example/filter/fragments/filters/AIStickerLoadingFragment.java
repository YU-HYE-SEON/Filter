package com.example.filter.fragments.filters;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.apis.service.AIStickerApi;
import com.example.filter.apis.dto.AIStickerResponse;
import com.example.filter.apis.dto.PromptRequest;
import com.example.filter.apis.client.AIStickreRetrofitClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIStickerLoadingFragment extends Fragment {
    private static final String BASE_URL = "base_url";
    private static final String PROMPT = "prompt";
    private String baseUrl;
    private String prompt;
    private Call<AIStickerResponse> inflight;
    private static final String IMAGE_PATH = "image_path";
    private View logo;
    private ImageView F, e1, e2, l, star, e3, m;
    private LinearLayout charRow;
    private final List<ValueAnimator> anims = new ArrayList<>();
    private static final long STEP_MS = 350;
    private static final float AMPLITUDE_DP = 100;

    public static AIStickerLoadingFragment newInstance(String baseUrl, String prompt) {
        AIStickerLoadingFragment f = new AIStickerLoadingFragment();
        Bundle b = new Bundle();
        b.putString(BASE_URL, baseUrl);
        b.putString(PROMPT, prompt);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_aisticker_loading, container, false);
        logo = view.findViewById(R.id.logo);
        F = view.findViewById(R.id.F);
        e1 = view.findViewById(R.id.e1);
        e2 = view.findViewById(R.id.e2);
        l = view.findViewById(R.id.l);
        star = view.findViewById(R.id.star);
        e3 = view.findViewById(R.id.e3);
        m = view.findViewById(R.id.m);

        float scale = 0.75f;
        logo.setScaleX(scale);
        logo.setScaleY(scale);

        baseUrl = requireArguments().getString(BASE_URL);
        prompt = requireArguments().getString(PROMPT, "");

        buildAnimatedLoadingText();

        requestSticker(baseUrl, prompt);

        return view;
    }

    private void requestSticker(String baseUrl, String prompt) {
        SharedPreferences prefs = requireContext().getSharedPreferences("Auth", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("accessToken", null);

        if (accessToken == null) {
            Log.e("AISticker", "❌ accessToken이 SharedPreferences에 없습니다");
        } else {
            Log.d("AISticker", "✅ Authorization 헤더에 토큰 추가: " + accessToken);
        }

        AIStickerApi api = AIStickreRetrofitClient.create(baseUrl).create(AIStickerApi.class);
        inflight = api.generateSticker("Bearer " + accessToken, new PromptRequest(prompt));

        inflight.enqueue(new Callback<AIStickerResponse>() {
            @Override
            public void onResponse(Call<AIStickerResponse> call, Response<AIStickerResponse> resp) {
                if (!isAdded()) return;
                Log.d("AISticker", "HTTP Code: " + resp.code());
                Log.d("AISticker", "Response success: " + resp.isSuccessful());

                if (resp.isSuccessful() && resp.body() != null) {
                    String imageUrl = resp.body().getImageUrl();
                    Log.d("AISticker", "✅ 서버에서 받은 imageUrl: " + imageUrl);

                    // 👉 이미지 다운로드 및 캐시 저장
                    new Thread(() -> {
                        try {
                            java.net.URL url = new java.net.URL(imageUrl);
                            java.io.InputStream is = url.openStream();
                            File out = new File(requireContext().getCacheDir(),
                                    "sticker_" + System.currentTimeMillis() + ".png");
                            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
                            byte[] buf = new byte[8 * 1024];
                            int n;
                            while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                            fos.close();
                            is.close();

                            Log.d("AISticker", "✅ 이미지 다운로드 완료: " + out.getAbsolutePath());

                            requireActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                getParentFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.aiStickerView,
                                                AIStickerSuccessFragment.newWithImagePath(out.getAbsolutePath(), baseUrl, prompt))
                                        .commit();
                            });
                        } catch (Exception e) {
                            Log.e("AISticker", "❌ 이미지 다운로드 실패", e);
                            goToFailDelayed();
                        }
                    }).start();
                } else {
                    try {
                        String errorBody = resp.errorBody() != null ? resp.errorBody().string() : "null";
                        Log.e("AISticker", "❌ 스티커 생성 실패 - code: " + resp.code());
                        Log.e("AISticker", "❌ 에러 본문: " + errorBody);
                    } catch (Exception e) {
                        Log.e("AISticker", "❌ 에러 본문 파싱 실패", e);
                    }
                    goToFailDelayed();
                }
            }

            @Override
            public void onFailure(Call<AIStickerResponse> call, Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                Log.e("AISticker", "❌ 네트워크 오류: " + t.getMessage(), t);
                goToFailDelayed();
            }
        });
    }

    private void buildAnimatedLoadingText() {
        List<View> letters = new ArrayList<>();
        letters.add(F);
        letters.add(e1);
        letters.add(e2);
        letters.add(l);
        letters.add(star);
        letters.add(e3);
        letters.add(m);

        startWaveAnimation(letters);
    }

    private void startWaveAnimation(List<View> views) {
        float ampPx = dp(AMPLITUDE_DP);
        long step = STEP_MS;
        long total = (long) (STEP_MS * views.size() * 0.7f);

        for (int i = 0; i < views.size(); i++) {
            View v = views.get(i);
            long startDelay = i * step / 2;

            Keyframe k0 = Keyframe.ofFloat(0f, 0f);
            Keyframe k1 = Keyframe.ofFloat(0.35f, -ampPx);
            Keyframe k2 = Keyframe.ofFloat(0.65f, 0f);
            Keyframe k3 = Keyframe.ofFloat(1f, 0f);

            PropertyValuesHolder ty = PropertyValuesHolder.ofKeyframe("translationY", k0, k1, k2, k3);

            ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v, ty);
            a.setDuration(total);
            a.setStartDelay(startDelay);
            a.setInterpolator(new AccelerateDecelerateInterpolator());
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.RESTART);
            a.start();
            anims.add(a);
        }
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private void goToFailDelayed() {
        if (!isAdded()) return;
        requireView().postDelayed(() -> {
            if (!isAdded()) return;
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.aiStickerView, new AIStickerFailFragment())
                    .commit();
        }, 5000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (inflight != null) inflight.cancel();

        for (ValueAnimator a : anims) a.cancel();
        anims.clear();
    }
}
