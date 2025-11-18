package com.example.filter.apis.service;

import com.example.filter.apis.dto.FilterDtoCreateRequest;
import com.example.filter.apis.dto.FilterResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface FilterApi {
    @POST("/api/v1/filters")
    Call<FilterResponse> uploadFilter(@Body FilterDtoCreateRequest request);
}
