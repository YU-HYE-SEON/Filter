package com.example.filter.fragments;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
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
import com.example.filter.apis.PromptRequest;
import com.example.filter.apis.AIStickreRetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIStickerLoadingFragment extends Fragment {
    private static final String BASE_URL = "base_url";
    private static final String PROMPT = "prompt";
    private String baseUrl;
    private String prompt;
    private Call<ResponseBody> inflight;
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
        AIStickerApi api = AIStickreRetrofitClient.create(baseUrl).create(AIStickerApi.class);
        inflight = api.generateSticker(new PromptRequest(prompt));

        inflight.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> resp) {
                if (!isAdded()) return;
                Log.d("AISticker", "HTTP Code: " + resp.code());
                Log.d("AISticker", "Response success: " + resp.isSuccessful());

                if (resp.isSuccessful() && resp.body() != null) {
                    try (InputStream is = resp.body().byteStream()) {
                        File out = new File(requireContext().getCacheDir(),
                                "sticker_" + System.currentTimeMillis() + ".png");
                        try (FileOutputStream fos = new FileOutputStream(out)) {
                            byte[] buf = new byte[8 * 1024];
                            int n;
                            while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                            fos.flush();
                        }

                        Log.d("AISticker", "✅ 스티커 이미지 저장 완료: " + out.getAbsolutePath());

                        if (!isAdded()) return;
                        getParentFragmentManager()
                                .beginTransaction()
                                .replace(R.id.aiStickerView, AIStickerSuccessFragment.newWithImagePath(out.getAbsolutePath(), baseUrl, prompt))
                                .commit();
                    } catch (Exception e) {
                        e.printStackTrace();

                        Log.e("AISticker", "❌ 이미지 저장 중 오류", e);

                        goToFailDelayed();
                    }
                } else {
                    // ✅ 실패 이유 출력
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
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (!isAdded() || call.isCanceled()) return;

                Log.e("AISticker", "❌ 네트워크/통신 오류: " + t.getMessage(), t);

                t.printStackTrace();
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
