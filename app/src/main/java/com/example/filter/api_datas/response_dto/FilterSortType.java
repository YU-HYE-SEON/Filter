package com.example.filter.api_datas.response_dto;

public enum FilterSortType {
    ACCURACY,       // 추천순 (AI 전용) - 태그에서 사용하면 안됩니다!!
    POPULARITY,     // 인기순
    LATEST,         // 최신순
    LOW_PRICE,      // 낮은 가격순
    REVIEW_COUNT    // 리뷰 많은 순
}