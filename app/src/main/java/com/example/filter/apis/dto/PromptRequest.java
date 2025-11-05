package com.example.filter.apis.dto;

public class PromptRequest {
    private String prompt;

    public PromptRequest(String promptKo) {
        this.prompt = promptKo;
    }

    public String getPrompt() { return prompt; }
}