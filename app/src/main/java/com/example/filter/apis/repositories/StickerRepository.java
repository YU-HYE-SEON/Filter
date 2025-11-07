package com.example.filter.apis.repositories;

import android.content.Context;
import android.util.Log;

import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.apis.dto.StickerCreateRequest;
import com.example.filter.apis.dto.StickerResponseDto;
import com.example.filter.apis.service.StickerApi;
import com.example.filter.items.StickerItem;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ✅ StickerRepository
 * - StickerUploader 인터페이스 구현
 * - 일반 스티커는 Multipart
 * - AI 스티커는 JSON 전송
 */
public class StickerRepository implements StickerUploader {

    private static final String TAG = "StickerRepository";
    private final StickerApi stickerApi;

    public StickerRepository(Context context) {
        this.stickerApi = AppRetrofitClient.getInstance(context).create(StickerApi.class);
    }

    @Override
    public void uploadToServer(StickerItem item) {
        try {
            if ("AI".equalsIgnoreCase(item.getType())) {
                uploadAiSticker(item);
            } else {
                uploadNormalSticker(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 업로드 중 예외 발생", e);
        }
    }

    /** ✅ 일반 이미지/브러시 스티커 업로드 (Multipart) */
    private void uploadNormalSticker(StickerItem item) {
        try {
            File file = new File(item.getImageUrl());
            if (!file.exists()) {
                Log.e(TAG, "❌ 파일이 존재하지 않습니다: " + file.getAbsolutePath());
                return;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/png"), file);
            MultipartBody.Part filePart =
                    MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            RequestBody typePart =
                    RequestBody.create(MediaType.parse("text/plain"), item.getType());

            Log.d(TAG, "☁️ 일반 스티커 업로드 요청 (" + item.getType() + ")");
            stickerApi.createSticker(filePart, typePart).enqueue(new Callback<StickerResponseDto>() {
                @Override
                public void onResponse(Call<StickerResponseDto> call, Response<StickerResponseDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        StickerResponseDto dto = response.body();
                        Log.d(TAG, "✅ 서버 업로드 성공 (id=" + dto.getId() + ", url=" + dto.getImageUrl() + ")");
                    } else {
                        Log.e(TAG, "❌ 서버 업로드 실패: code=" + response.code());
                    }
                }

                @Override
                public void onFailure(Call<StickerResponseDto> call, Throwable t) {
                    Log.e(TAG, "❌ 일반 스티커 업로드 네트워크 오류", t);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ 일반 스티커 업로드 중 예외", e);
        }
    }

    /** ✅ AI 스티커 업로드 (JSON 전송) */
    private void uploadAiSticker(StickerItem item) {
        try {
            StickerCreateRequest request = new StickerCreateRequest(item.getImageUrl(), item.getType());
            Log.d(TAG, "🤖 AI 스티커 업로드 요청: " + item.getImageUrl());

            stickerApi.createStickerJson(request).enqueue(new Callback<StickerResponseDto>() {
                @Override
                public void onResponse(Call<StickerResponseDto> call, Response<StickerResponseDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        StickerResponseDto dto = response.body();
                        Log.d(TAG, "✅ AI 스티커 등록 성공 (id=" + dto.getId() + ", url=" + dto.getImageUrl() + ")");
                    } else {
                        Log.e(TAG, "❌ AI 스티커 등록 실패: code=" + response.code());
                    }
                }

                @Override
                public void onFailure(Call<StickerResponseDto> call, Throwable t) {
                    Log.e(TAG, "❌ AI 스티커 등록 네트워크 오류", t);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ AI 스티커 업로드 중 예외", e);
        }
    }
}
