package com.example.filter.apis.repositories;

import com.example.filter.api_datas.response_dto.UserMypageResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface MyPageApi {
    // 마이페이지 정보 조회 (토큰은 AppRetrofitClient가 자동 주입)
    @GET("/api/v1/users/mypage")
    Call<UserMypageResponse> getMyPage();
}
