// Sticker Uploader를 구현해서 Sticker Store가 자동으로 서버 업로드를 위임할 수 있게 함

package com.example.filter.apis.repositories;

import android.content.Context;
import android.util.Log;

import com.example.filter.apis.client.AppRetrofitClient;
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

public class StickerRepository implements StickerUploader {

    private static final String TAG = "StickerRepository";
    private final StickerApi stickerApi;
    private final Context context;

    public StickerRepository(Context context) {
        this.context = context;
        this.stickerApi = AppRetrofitClient.getInstance(context).create(StickerApi.class);
    }

    @Override
    public void uploadToServer(StickerItem item) {
        Log.d(TAG, "🚀 서버로 업로드 중: " + item.getImageUrl());
        try {
            // 파일로 다시 로드 (imageUrl이 로컬 경로일 수 있음)
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

            stickerApi.createSticker(filePart, typePart).enqueue(new Callback<StickerResponseDto>() {
                @Override
                public void onResponse(Call<StickerResponseDto> call, Response<StickerResponseDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "✅ 서버 업로드 성공: " + response.body().getImageUrl());
                    } else {
                        Log.e(TAG, "❌ 서버 업로드 실패: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<StickerResponseDto> call, Throwable t) {
                    Log.e(TAG, "❌ 서버 업로드 네트워크 오류", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ 서버 업로드 중 예외 발생", e);
        }
    }
}
