package com.example.filter.api_datas.response_dto;

import java.io.Serializable;

public class SalesListResponse implements Serializable {
    private Long filterId;
    private String filterName;
    private String filterImageUrl;
    private String filterCreatedAt;
    private int price; // 필터 1개 가격
    private long salesCount; // 판매수
    private long salesAmount; // 총 판매 금액 (포인트)
    private long saveCount; // 북마크수
    private boolean isDeleted; // 삭제된 필터?

    // Getters
    public Long getFilterId() {
        return filterId;
    }

    public String getFilterName() {
        return filterName;
    }

    public String getFilterImageUrl() {
        return filterImageUrl;
    }

    public String getFilterCreatedAt() {
        return filterCreatedAt;
    }

    public int getPrice() {
        return price;
    }

    public long getSalesCount() {
        return salesCount;
    }

    public long getSalesAmount() {
        return salesAmount;
    }

    public long getSaveCount() {
        return saveCount;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

}
