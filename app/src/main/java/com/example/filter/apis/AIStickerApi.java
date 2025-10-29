package com.example.filter.apis;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

public interface AIStickerApi {
    @POST("api/v1/stickers/ai")
    @Headers("Content-Type: application/json")
    Call<AIStickerResponse> generateSticker(
            @Header("Authorization") String token,
            @Body PromptRequest body
    );
}