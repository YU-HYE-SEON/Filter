package com.example.filter.apis;

import com.example.filter.api_datas.response_dto.ReviewResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

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
}
