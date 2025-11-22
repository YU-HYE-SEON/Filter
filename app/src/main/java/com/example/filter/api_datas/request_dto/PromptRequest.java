package com.example.filter.api_datas.request_dto;

public class PromptRequest {
    private String prompt;

    public PromptRequest(String promptKo) {
        this.prompt = promptKo;
    }

    public String getPrompt() { return prompt; }
}