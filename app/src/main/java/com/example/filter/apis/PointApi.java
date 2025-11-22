package com.example.filter.apis;

import com.example.filter.api_datas.response_dto.CashTransactionListResponse;
import com.example.filter.api_datas.response_dto.FilterTransactionListResponse;
import com.example.filter.api_datas.response_dto.PageResponse;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PointApi {

    // 포인트 충전
    @POST("/api/v1/points/charge")
    Call<Map<String, Integer>> chargePoint(@Query("cash") int cash);

    // 충전 내역 조회
    @GET("/api/v1/points/charge/history")
    Call<PageResponse<CashTransactionListResponse>> getChargeHistory(
            @Query("page") int page,
            @Query("size") int size
    );

    // 사용 내역 조회
    @GET("/api/v1/points/usage")
    Call<PageResponse<FilterTransactionListResponse>> getUsageHistory(
            @Query("page") int page,
            @Query("size") int size
    );
}
