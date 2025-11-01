package com.example.filter.items;

import com.example.filter.apis.FilterDtoCreateRequest;

import java.util.List;

public class ReviewItem {
    public final String imageUrl, nickname, snsId;

    public ReviewItem(String imageUrl, String nickname, String snsId) {
        this.imageUrl = imageUrl;
        this.nickname = nickname;
        this.snsId = snsId;
    }
}