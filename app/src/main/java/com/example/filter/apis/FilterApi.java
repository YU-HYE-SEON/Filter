package com.example.filter.apis;

import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
import com.example.filter.api_datas.response_dto.FilterListResponse;
import com.example.filter.api_datas.response_dto.FilterResponse;
import com.example.filter.api_datas.response_dto.FilterSortType;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.api_datas.response_dto.SearchType;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FilterApi {
    @POST("/api/v1/filters")
    Call<FilterResponse> uploadFilter(@Body FilterDtoCreateRequest request);

    // ✔️ 필터 상세 조회 API (PathVariable 사용)
    @GET("/api/v1/filters/{filterId}")
    Call<FilterResponse> getFilter(@Path("filterId") Long filterId);

    // ✔️ 홈- 필터 최신순 페이징 조회
    @GET("/api/v1/filter-lists/recent")
    Call<PageResponse<FilterListResponse>> getRecentFilters(
            @Query("page") int page,
            @Query("size") int size
    );

    // ✔️ 홈- 인기순 조회
    @GET("/api/v1/filter-lists/hot")
    Call<PageResponse<FilterListResponse>> getHotFilters(
            @Query("page") int page,
            @Query("size") int size
    );

    // ✔️ 홈- 랜덤 조회
    @GET("/api/v1/filter-lists/random")
    Call<PageResponse<FilterListResponse>> getRandomFilters(
            @Query("page") int page,
            @Query("size") int size
    );

    // ✔️ AI 기반 홈 화면 추천
    @GET("/api/v1/filter-lists/recommend")
    Call<PageResponse<FilterListResponse>> getHomeRecommendations(
            @Query("page") int page,
            @Query("size") int size
    );

    // ✔️ 검색 (자연어/태그 + 정렬)
    @GET("/api/v1/filter-lists/search")
    Call<PageResponse<FilterListResponse>> searchFilters(
            @Query("query") String query,
            @Query("search-type") SearchType searchType, // "NL" or "TAG"
            @Query("sort") FilterSortType sortType,      // "ACCURACY", "LOW_PRICE"...
            @Query("page") int page,
            @Query("size") int size
    );

    // ✔️ 가격 수정
    @PUT("/api/v1/filters/{filterId}/price")
    Call<Void> updatePrice(
            @Path("filterId") Long filterId,
            @Body java.util.HashMap<String, Object> requestBody
    );

    // ✔️ 필터 구매
    @POST("/api/v1/filters/{filterId}/usage")
    Call<Void> useFilter(@Path("filterId") Long filterId);

    // ✔️ 북마크 토글
    @PUT("/api/v1/filters/{filterId}/bookmark")
    Call<Boolean> toggleBookmark(@Path("filterId") Long filterId);

    // 필터 삭제
    @DELETE("/api/v1/filters/{filterId}")
    Call<Void> deleteFilter(@Path("filterId") Long filterId);
}
