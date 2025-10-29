package com.example.filter.apis;

public class AIStickerResponse {
    private boolean fromAIServer;
    private String imageUrl;
    private int id;

    public boolean isFromAIServer() { return fromAIServer; }
    public String getImageUrl() { return imageUrl; }
    public int getId() { return id; }
}