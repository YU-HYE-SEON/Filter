package com.example.filter.api_datas.response_dto;

import java.io.Serializable;

public class FilterTransactionListResponse implements Serializable {
    public Long id;
    public int amount;        // 사용 포인트
    public int balance;       // 사용 후 잔액
    public String filterName; // 구매한 필터 이름
    public String createdAt;  // 날짜
}
