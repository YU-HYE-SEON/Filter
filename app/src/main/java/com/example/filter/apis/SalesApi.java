package com.example.filter.apis;

import com.example.filter.api_datas.request_dto.SalesPeriod;
import com.example.filter.api_datas.request_dto.SalesSortType;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.api_datas.response_dto.SalesGraphResponse;
import com.example.filter.api_datas.response_dto.SalesListResponse;
import com.example.filter.api_datas.response_dto.SalesTotalResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SalesApi {
    // 1. 종합 판매 수치 조회
    @GET("api/v1/sales/total")
    Call<SalesTotalResponse> getTotalSales(
            @Query("period") SalesPeriod period
    );

    // 2. 판매중인 필터 목록 조회 (페이징 포함)
    @GET("api/v1/sales/lists")
    Call<PageResponse<SalesListResponse>> getSalesList(
            @Query("sortBy") SalesSortType sortBy,
            @Query("page") int page,
            @Query("size") int size
    );

    // 3. 개별 필터 상세 수치 및 그래프 조회
    @GET("api/v1/sales/{filterId}")
    Call<SalesGraphResponse> getFilterSalesDetail(
            @Path("filterId") Long filterId,
            @Query("period") SalesPeriod period
    );
}
