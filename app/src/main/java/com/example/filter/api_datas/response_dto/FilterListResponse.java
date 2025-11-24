package com.example.filter.api_datas.response_dto;

import java.io.Serializable;

public class FilterListResponse implements Serializable {
    public Long id;
    public String name;
    public String thumbnailUrl;
    public String creator;
    public int pricePoint;
    public Long useCount;
    // public boolean usage; // 사용 혹은 구매 여부
    public String priceDisplayType; // NONE, PURCHASED, NUMBER
    public boolean bookmark;
}
