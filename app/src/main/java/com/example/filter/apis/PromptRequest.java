package com.example.filter.apis;

public class PromptRequest {
    private String prompt_ko;

    public PromptRequest(String promptKo) {
        this.prompt_ko = promptKo;
    }

    public String getPrompt_ko() { return prompt_ko; }
}