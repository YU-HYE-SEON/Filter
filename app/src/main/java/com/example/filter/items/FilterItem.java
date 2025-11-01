package com.example.filter.items;

import com.example.filter.apis.FilterDtoCreateRequest;

import java.util.List;

public class FilterItem {
    public final String id;
    public final String nickname, originalPath, filterImageUrl, filterTitle, tags, price;
    public final String brushPath, stickerPath;
    public final int count;
    public final boolean isMockData;
    public final FilterDtoCreateRequest.ColorAdjustments colorAdjustments;

    public FilterItem(String id, String nickname, String originalPath, String filterImageUrl, String filterTitle, String tags, String price, int count,
                      boolean isMockData, FilterDtoCreateRequest.ColorAdjustments colorAdjustments,
                      String brushPath, String stickerPath) {
        this.id = id;
        this.nickname = nickname;
        this.originalPath = originalPath;
        this.filterImageUrl = filterImageUrl;
        this.filterTitle = filterTitle;
        this.tags = tags;
        this.price = price;
        this.count = count;
        this.isMockData = isMockData;
        this.colorAdjustments = colorAdjustments;
        this.brushPath = brushPath;
        this.stickerPath = stickerPath;
    }
}