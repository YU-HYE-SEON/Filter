package com.example.filter.etc;

public class FilterItem {
    public final String id;
    public final String nickname, filterImageUrl, filterTitle, tags, price;
    public final int count;
    public final boolean isMockData;

    public FilterItem(String id, String nickname, String filterImageUrl, String filterTitle, String tags, String price, int count, boolean isMockData) {
        this.id = id;
        this.nickname = nickname;
        this.filterImageUrl = filterImageUrl;
        this.filterTitle = filterTitle;
        this.tags = tags;
        this.price = price;
        this.count = count;
        this.isMockData = isMockData;
    }
}