package com.example.filter.api_datas.request_dto;

public class StickerCreateRequest {
    private String imageUrl;
    private String type;

    public StickerCreateRequest(String imageUrl, String type) {
        this.imageUrl = imageUrl;
        this.type = type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getType() {
        return type;
    }
}
