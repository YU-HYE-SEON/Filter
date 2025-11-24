package com.example.filter.apis;

import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.api_datas.response_dto.ReviewResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ArchiveApi {

    // 내가 작성한 리뷰 목록 조회 20개씩 페이징
    // 서버: @GetMapping("/my") -> URL: /api/v1/reviews/my
    @GET("/api/v1/reviews/my")
    Call<PageResponse<ReviewResponse>> getMyReviews(
            @Query("page") int page,
            @Query("size") int size
    );
}
