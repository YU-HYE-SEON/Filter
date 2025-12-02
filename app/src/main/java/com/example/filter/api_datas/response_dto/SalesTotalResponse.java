package com.example.filter.api_datas.response_dto;

public class SalesTotalResponse {
    private long settlementAmount; // 정산 예정 금액 (원)
    private long totalSales;       // 총 판매 수량

    public long getSettlementAmount() { return settlementAmount; }
    public long getTotalSales() { return totalSales; }
}