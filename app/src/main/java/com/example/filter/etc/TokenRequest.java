package com.example.filter.etc;

public class TokenRequest {
    private String idToken;
    public TokenRequest(String idToken) { this.idToken = idToken; }
    public String getToken() { return idToken; }
}
