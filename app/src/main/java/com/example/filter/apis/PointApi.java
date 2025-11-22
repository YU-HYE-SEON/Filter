package com.example.filter.apis;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PointApi {

    // 포인트 충전
    @POST("/api/v1/points/charge")
    Call<Map<String, Integer>> chargePoint(@Query("cash") int cash);
}
