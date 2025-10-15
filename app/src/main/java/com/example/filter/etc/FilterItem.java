package com.example.filter.etc;

public class FilterItem {
    public final String filterImageUrl, filterTitle, nickname, price;
    public final int count;

    public FilterItem(String filterImageUrl, String filterTitle, String nickname, String price, int count) {
        this.filterImageUrl = filterImageUrl;
        this.filterTitle = filterTitle;
        this.nickname = nickname;
        this.price = price;
        this.count = count;
    }
}