package com.example.filter.items;

public class ReviewItem {
    public final String imageUrl, nickname, snsId;
    public Long reviewId;

    public ReviewItem(String imageUrl, String nickname, String snsId, Long id) {
        this.imageUrl = imageUrl;
        this.nickname = nickname;
        this.snsId = snsId;
        this.reviewId = id;
    }
}