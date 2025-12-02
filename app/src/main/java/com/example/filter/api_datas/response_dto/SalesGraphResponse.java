package com.example.filter.api_datas.response_dto;

import java.util.Map;

public class SalesGraphResponse {
    private long totalSalesPoints; // 총 판매 금액 (포인트)
    private long totalSalesCount; // 총 판매 수량
    private Map<String, Long> salesGraphData; // 날짜(String) : 판매포인트(Long)

    public long getTotalSalesPoints() { return totalSalesPoints; }
    public long getTotalSalesCount() { return totalSalesCount; }
    public Map<String, Long> getSalesGraphData() { return salesGraphData; }
}
