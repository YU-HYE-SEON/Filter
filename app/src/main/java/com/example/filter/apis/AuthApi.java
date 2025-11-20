package com.example.filter.apis;

import com.example.filter.api_datas.dto.TokenRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("auth/google")
    Call<ResponseBody> verifyGoogleToken(@Body TokenRequest tokenRequest);

    @POST("auth/reissue")
    Call<ResponseBody> reissueToken(@Body TokenRequest tokenRequest);
}