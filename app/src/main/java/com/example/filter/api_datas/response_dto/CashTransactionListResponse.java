package com.example.filter.api_datas.response_dto;

import java.io.Serializable;

public class CashTransactionListResponse implements Serializable {
    public Long id;
    public double cash;       // 결제 금액
    public int point;         // 충전된 포인트
    public int balance;       // 충전 후 잔액
    public String type;
    public String createdAt;  // 날짜 (ISO String)
}