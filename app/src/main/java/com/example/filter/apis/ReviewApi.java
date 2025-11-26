package com.example.filter.apis;

import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.api_datas.response_dto.ReviewResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ReviewApi {
    // ✔️ 리뷰 생성
    // 서버: @RequestParam("file"), @RequestParam("filterId"), @RequestParam("socialType")
    @Multipart
    @POST("/api/v1/reviews")
    Call<ReviewResponse> createReview(
            @Part MultipartBody.Part file,       // 이미지 파일
            @Part("filterId") RequestBody filterId,   // 필터 ID
            @Part("socialType") RequestBody socialType // 소셜 타입: NONE, INSTAGRAM, X
    );

    // ✔️ 리뷰 1개 상세 조회
    // URL: /api/v1/reviews/{reviewId}
    @GET("/api/v1/reviews/{reviewId}")
    Call<ReviewResponse> getReviewById(@Path("reviewId") Long reviewId);

    // ✔️ 리뷰 목록 조회 (페이징)
    @GET("/api/v1/review-lists/{filterId}")
    Call<PageResponse<ReviewResponse>> getReviewsByFilter(
            @Path("filterId") Long filterId,
            @Query("page") int page,
            @Query("size") int size
    );

    // ✔️ 리뷰 미리보기 (최신 5개 조회)
    // 서버 Controller 경로 확인 필요 (예: /api/v1/filters/{filterId}/reviews/preview)
    @GET("/api/v1/review-lists/{filterId}/preview")
    Call<List<ReviewResponse>> getReviewPreview(@Path("filterId") Long filterId);

    // ✔️ 리뷰 삭제
    // 서버 Controller 경로 확인 필요 (예: /api/v1/reviews/{reviewId})
    @DELETE("/api/v1/reviews/{reviewId}")
    Call<Void> deleteReview(@Path("reviewId") Long reviewId);
}
