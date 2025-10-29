package com.example.filter.etc;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("api/v1/auth/google")
    Call<ResponseBody> verifyGoogleToken(@Body TokenRequest tokenRequest);
}