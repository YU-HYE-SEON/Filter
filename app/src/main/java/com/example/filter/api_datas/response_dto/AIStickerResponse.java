package com.example.filter.api_datas.response_dto;

public class AIStickerResponse {
    private boolean fromAIServer;
    private String imageUrl;
    private int id;

    public boolean isFromAIServer() { return fromAIServer; }
    public String getImageUrl() { return imageUrl; }
    public int getId() { return id; }
}