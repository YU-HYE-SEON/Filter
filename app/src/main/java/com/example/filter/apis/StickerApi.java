package com.example.filter.apis;

import com.example.filter.api_datas.request_dto.StickerCreateRequest;
import com.example.filter.api_datas.response_dto.StickerResponseDto;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface StickerApi {

    // ✅ 일반 / 브러시 스티커 업로드 (Multipart)
    @Multipart
    @POST("/api/v2/stickers")
    Call<StickerResponseDto> createSticker(
            @Part MultipartBody.Part file,
            @Part("type") RequestBody type
    );

    // ✅ AI 스티커 업로드 (JSON)
    @POST("/api/v2/stickers")
    Call<StickerResponseDto> createStickerJson(@Body StickerCreateRequest body);

    // ✅ 내가 제작한 모든 스티커 조회
    @GET("/api/v2/stickers")
    Call<List<StickerResponseDto>> getMyStickers();
}
