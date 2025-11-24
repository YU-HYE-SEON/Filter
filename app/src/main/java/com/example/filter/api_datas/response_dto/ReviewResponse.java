package com.example.filter.api_datas.response_dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ReviewResponse implements Serializable {

    @SerializedName("isMine")
    public boolean isMine;

    @SerializedName("id")
    public Long id;

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
}
