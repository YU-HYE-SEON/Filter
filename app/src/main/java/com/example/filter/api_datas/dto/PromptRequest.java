package com.example.filter.api_datas.dto;

public class PromptRequest {
    private String prompt;

    public PromptRequest(String promptKo) {
        this.prompt = promptKo;
    }

    public String getPrompt() { return prompt; }
}