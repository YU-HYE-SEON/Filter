package com.example.filter.apis;

import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.response_dto.FilterListResponse;
import com.example.filter.api_datas.response_dto.FilterResponse;
import com.example.filter.api_datas.response_dto.PageResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FilterApi {
    @POST("/api/v1/filters")
    Call<FilterResponse> uploadFilter(@Body FilterDtoCreateRequest request);

    // ✔️ 필터 상세 조회 API (PathVariable 사용)
    @GET("/api/v1/filters/{filterId}")
    Call<FilterResponse> getFilter(@Path("filterId") Long filterId);

    // ✔️ 필터 최신순 페이징 조회
    @GET("/api/v1/filter-lists/recent")
    Call<PageResponse<FilterListResponse>> getRecentFilters(
            @Query("page") int page,
            @Query("size") int size
    );

    // ✔️ 인기순 조회
    @GET("/api/v1/filter-lists/hot")
    Call<PageResponse<FilterListResponse>> getHotFilters(
            @Query("page") int page,
            @Query("size") int size
    );

}
