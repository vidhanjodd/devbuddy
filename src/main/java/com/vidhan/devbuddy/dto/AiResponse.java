package com.vidhan.devbuddy.dto;

public class AiResponse {
    private boolean success;
    private String result;
    private String error;

    public AiResponse(boolean success, String result, String error) {
        this.success = success;
        this.result  = result;
        this.error   = error;
    }

    public static AiResponse ok(String result)    { return new AiResponse(true,  result, null); }
    public static AiResponse fail(String error)   { return new AiResponse(false, null,   error); }

    public boolean isSuccess() { return success; }
    public String  getResult() { return result;  }
    public String  getError()  { return error;   }
}