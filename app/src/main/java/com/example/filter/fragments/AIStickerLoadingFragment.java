package com.example.filter.fragments;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.apis.AIStickerApi;
import com.example.filter.apis.AIStickerResponse;
import com.example.filter.apis.PromptRequest;
import com.example.filter.apis.AIStickreRetrofitClient;

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
    private TextView loadingTxt;
    private LinearLayout charRow;
    private final List<ValueAnimator> anims = new ArrayList<>();
    private static final long STEP_MS = 400;
    private static final float AMPLITUDE_DP = 80;

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
        loadingTxt = view.findViewById(R.id.loadingTxt);

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
        CharSequence text = loadingTxt.getText();
        float sizePx = loadingTxt.getTextSize();
        int color = loadingTxt.getCurrentTextColor();
        Typeface tf = loadingTxt.getTypeface();

        ViewGroup parent = (ViewGroup) loadingTxt.getParent();
        parent.setClipChildren(false);
        parent.setClipToPadding(false);
        android.view.ViewParent p = parent.getParent();
        if (p instanceof ViewGroup) {
            ViewGroup gp = (ViewGroup) p;
            gp.setClipChildren(false);
            gp.setClipToPadding(false);
        }

        int idx = parent.indexOfChild(loadingTxt);
        parent.removeViewAt(idx);

        charRow = new LinearLayout(requireContext());
        charRow.setOrientation(LinearLayout.HORIZONTAL);

        charRow.setClipChildren(false);
        charRow.setClipToPadding(false);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        charRow.setLayoutParams(lp);

        int pad = dp(AMPLITUDE_DP);
        charRow.setPadding(0, pad, 0, pad);

        parent.addView(charRow, idx);

        for (int i = 0; i < text.length(); i++) {
            TextView tv = new TextView(requireContext());
            tv.setIncludeFontPadding(true);
            tv.setText(String.valueOf(text.charAt(i)));
            tv.setTextColor(color);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
            tv.setTypeface(tf);
            tv.setPadding(0, 0, dp(1), 0);
            charRow.addView(tv);
        }

        charRow.post(this::startWaveAnimation);
    }

    private void startWaveAnimation() {
        float ampPx = dp(AMPLITUDE_DP);
        int n = charRow.getChildCount();
        long step = STEP_MS;
        long total = (long) (STEP_MS * n * 0.7f);

        for (int i = 0; i < n; i++) {
            View ch = charRow.getChildAt(i);
            long startDelay = i * step / 2;

            Keyframe k0 = Keyframe.ofFloat(0f, 0f);
            Keyframe k1 = Keyframe.ofFloat(0.35f, -ampPx);
            Keyframe k2 = Keyframe.ofFloat(0.65f, 0f);
            Keyframe k3 = Keyframe.ofFloat(1f, 0f);

            PropertyValuesHolder ty = PropertyValuesHolder.ofKeyframe("translationY", k0, k1, k2, k3);

            ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(ch, ty);
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
