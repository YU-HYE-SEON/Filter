package com.example.filter.apis.service;

import com.example.filter.apis.dto.StickerResponseDto;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface StickerApi {

    // ✅ 스티커 업로드 + 등록 (v2)
    @Multipart
    @POST("/api/v2/stickers")
    Call<StickerResponseDto> createSticker(
            @Part MultipartBody.Part file,           // 이미지 파일
            @Part("type") RequestBody type           // 스티커 타입 (예: "NORMAL", "AI")
    );
}
