package com.example.filter.apis;

import com.example.filter.api_datas.dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.dto.FilterResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface FilterApi {
    @POST("/api/v1/filters")
    Call<FilterResponse> uploadFilter(@Body FilterDtoCreateRequest request);
}
