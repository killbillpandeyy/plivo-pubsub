package com.plivo.models.ws.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plivo.models.ws.enums.MessageType;

public class ErrorResponse extends ServerMessage {
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("details")
    private String details;
    
    public ErrorResponse() {
        setType(MessageType.ERROR);
    }
    
    public ErrorResponse(String code, String message, String requestId) {
        setType(MessageType.ERROR);
        setRequestId(requestId);
        this.code = code;
        this.message = message;
    }
    
    public ErrorResponse(String code, String message, String details, String requestId) {
        setType(MessageType.ERROR);
        setRequestId(requestId);
        this.code = code;
        this.message = message;
        this.details = details;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
}
