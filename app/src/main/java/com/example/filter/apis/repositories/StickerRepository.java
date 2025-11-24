package com.example.filter.apis.repositories;

import android.content.Context;
import android.util.Log;

import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.api_datas.request_dto.StickerCreateRequest;
import com.example.filter.api_datas.response_dto.StickerResponseDto;
import com.example.filter.apis.StickerApi;
import com.example.filter.fragments.filters.EditStickerFragment;
import com.example.filter.items.StickerItem;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * StickerRepository: 스티커 업로드를 담당하며, 업로드 완료 시 Fragment에 콜백을 전달합니다.
 */
public class StickerRepository implements StickerUploader { // StickerUploader 인터페이스를 구현한다고 가정

    private static final String TAG = "StickerRepository";
    private final StickerApi stickerApi;
    private final EditStickerFragment.StickerUploadListener uploadListener; // ✅ 리스너 필드

    // ✅ Constructor: Fragment에서 리스너를 받도록 수정
    public StickerRepository(Context context, EditStickerFragment.StickerUploadListener listener) {
        this.stickerApi = AppRetrofitClient.getInstance(context).create(StickerApi.class);
        this.uploadListener = listener;
    }

    @Override
    public void uploadToServer(StickerItem item) {
        try {
            // ✅ [핵심 로직] ID가 -1일 때 (새로운 스티커)만 업로드를 시도합니다.
            if (item.getId() != -1) {
                Log.w(TAG, "⚠️ 이미 등록된 스티커입니다 (ID=" + item.getId() + "). 업로드를 건너뜁니다.");

                // ★ 업로드를 건너뛰더라도 완료 카운트 감소를 위해 리스너 호출 필수
                if (uploadListener != null) {
                    uploadListener.onUploadFinished();
                }
                return;
            }

            if ("AI".equalsIgnoreCase(item.getType())) {
                uploadAiSticker(item);
            } else {
                uploadNormalSticker(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 업로드 중 예외 발생", e);
            if (uploadListener != null) {
                uploadListener.onUploadFinished(); // 예외 발생 시에도 완료 알림
            }
        }
    }

    /** ✅ 일반 이미지/브러시 스티커 업로드 (Multipart) */
    private void uploadNormalSticker(StickerItem item) {
        try {
            File file = new File(item.getImageUrl());
            if (!file.exists()) {
                Log.e(TAG, "❌ 파일이 존재하지 않습니다: " + file.getAbsolutePath());
                if (uploadListener != null) {
                    uploadListener.onUploadFinished(); // 파일 없으면 완료 알림
                }
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
                    if (uploadListener != null) {
                        uploadListener.onUploadFinished(); // ✅ 성공/실패와 관계없이 완료 알림
                    }
                }

                @Override
                public void onFailure(Call<StickerResponseDto> call, Throwable t) {
                    Log.e(TAG, "❌ 일반 스티커 업로드 네트워크 오류", t);
                    if (uploadListener != null) {
                        uploadListener.onUploadFinished(); // ✅ 네트워크 오류 시 완료 알림
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ 일반 스티커 업로드 중 예외", e);
            if (uploadListener != null) {
                uploadListener.onUploadFinished(); // 예외 발생 시 완료 알림
            }
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
                    if (uploadListener != null) {
                        uploadListener.onUploadFinished(); // ✅ 성공/실패와 관계없이 완료 알림
                    }
                }

                @Override
                public void onFailure(Call<StickerResponseDto> call, Throwable t) {
                    Log.e(TAG, "❌ AI 스티커 등록 네트워크 오류", t);
                    if (uploadListener != null) {
                        uploadListener.onUploadFinished(); // ✅ 네트워크 오류 시 완료 알림
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ AI 스티커 업로드 중 예외", e);
            if (uploadListener != null) {
                uploadListener.onUploadFinished(); // 예외 발생 시 완료 알림
            }
        }
    }
}