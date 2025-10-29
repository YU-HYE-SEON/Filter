package com.example.filter.etc;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface UserApi {
    @GET("api/v1/users/exists")
    Call<ResponseBody> checkNickname(@Query("nickname") String nickname);

    @POST("/api/v1/users/nickname")
    Call<ResponseBody> setNickname(@Body JsonObject nicknameBody);
}
