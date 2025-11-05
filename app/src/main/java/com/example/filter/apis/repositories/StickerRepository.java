package com.example.filter.apis.repositories;

import android.content.Context;
import android.util.Log;

import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.apis.dto.StickerResponseDto;
import com.example.filter.apis.service.StickerApi;
import com.example.filter.etc.StickerStore;
import com.example.filter.items.StickerItem;
import com.example.filter.items.StickerType;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ✅ StickerRepository
 * StickerUploader를 구현하여 StickerStore가 자동으로 서버 업로드를 위임하도록 함.
 * 서버 업로드 성공 시 StickerItem의 id를 갱신하고, StickerStore에도 반영.
 */
public class StickerRepository implements StickerUploader {

    private static final String TAG = "StickerRepository";
    private final StickerApi stickerApi;
    private final Context context;
    private final StickerStore stickerStore;

    public StickerRepository(Context context) {
        this.context = context.getApplicationContext();
        this.stickerApi = AppRetrofitClient.getInstance(context).create(StickerApi.class);
        this.stickerStore = StickerStore.get();
    }

    @Override
    public void uploadToServer(StickerItem item) {
        Log.d(TAG, "🚀 서버로 업로드 시작: " + item.getImageUrl());

        try {
            // ① 로컬 파일 경로 확인
            File file = new File(item.getImageUrl());
            if (!file.exists()) {
                Log.e(TAG, "❌ 파일이 존재하지 않습니다: " + file.getAbsolutePath());
                return;
            }

            // ② Multipart Request 생성
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/png"), file);
            MultipartBody.Part filePart =
                    MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            RequestBody typePart =
                    RequestBody.create(MediaType.parse("text/plain"), item.getType());

            // ③ 서버 업로드 요청
            stickerApi.createSticker(filePart, typePart).enqueue(new Callback<StickerResponseDto>() {
                @Override
                public void onResponse(Call<StickerResponseDto> call, Response<StickerResponseDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        StickerResponseDto body = response.body();

                        Log.d(TAG, "✅ 서버 업로드 성공: id=" + body.getId() + ", url=" + body.getImageUrl());

                        // ④ 서버에서 반환한 id로 새 StickerItem 생성
                        StickerItem uploaded = new StickerItem(
                                body.getId(),
                                null,
                                body.getImageUrl(),
                                StickerType.valueOf(item.getType())
                        );

                        // ⑤ 로컬 StickerStore에 반영 (id 업데이트된 버전)
                        stickerStore.addToAllFront(uploaded);
                        Log.d(TAG, "📦 StickerStore 업데이트 완료 (id=" + uploaded.getId() + ")");
                    } else {
                        Log.e(TAG, "❌ 서버 업로드 실패 (code=" + response.code() + ")");
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
