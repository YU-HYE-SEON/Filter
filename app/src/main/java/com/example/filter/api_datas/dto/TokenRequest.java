package com.example.filter.api_datas.dto;

public class TokenRequest {
    private String idToken;
    private String refreshToken;

    public TokenRequest(String idToken) {
        this.idToken = idToken;
    }

    public TokenRequest(String idToken, String refreshToken) {
        this.idToken = idToken;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
