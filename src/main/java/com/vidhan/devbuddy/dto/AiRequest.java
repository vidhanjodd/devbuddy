package com.vidhan.devbuddy.dto;

public class AiRequest {
    private Long   snippetId;
    private String action;
    private String prompt;
    private String language;

    public Long   getSnippetId()            { return snippetId; }
    public void   setSnippetId(Long id)     { this.snippetId = id; }

    public String getAction()               { return action; }
    public void   setAction(String action)  { this.action = action; }

    public String getPrompt()               { return prompt; }
    public void   setPrompt(String prompt)  { this.prompt = prompt; }

    public String getLanguage()             { return language; }
    public void   setLanguage(String lang)  { this.language = lang; }
}