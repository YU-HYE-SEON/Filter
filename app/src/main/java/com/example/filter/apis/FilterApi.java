package com.example.filter.apis;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface FilterApi {
    @POST("api/v1/filters")
    Call<ResponseBody> uploadFilter(
            @Header("Authorization") String token,
            @Body FilterDtoCreateRequest request
    );
}
