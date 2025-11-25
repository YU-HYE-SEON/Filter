package com.example.filter.api_datas.response_dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MyReviewResponse implements Serializable {
    @SerializedName("isMine")
    public boolean isMine;

    @SerializedName("id")
    public Long id; // 리뷰 ID

    @SerializedName("imageUrl")
    public String imageUrl;

    @SerializedName("reviewerNickname")
    public String reviewerNickname;

    @SerializedName("socialType")
    public String socialType;   // "NONE", "INSTAGRAM", "X"

    @SerializedName("socialValue")
    public String socialValue;  // 인스타그램 ID 또는 X ID

    @SerializedName("createdAt")
    public String createdAt;    // "2025-11-22T..." 형식의 문자열로 받음

    // my에 필요한 정보 추가
    private Long filterId;
    private String filterName;
    private String priceDisplayType; // 가격 표시 유형: NONE, PURCHASED, NUMBER
    private int pricePoint; // 필터 가격
}
