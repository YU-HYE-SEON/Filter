package com.example.filter.apis;

import com.example.filter.api_datas.response_dto.AIStickerResponse;
import com.example.filter.api_datas.request_dto.PromptRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AIStickerApi {
    @POST("api/v1/stickers/ai")
    @Headers("Content-Type: application/json")
    Call<AIStickerResponse> generateSticker(
            @Header("Authorization") String token,
            @Body PromptRequest body
    );
}