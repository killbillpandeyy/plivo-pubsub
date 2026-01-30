package com.plivo.models.ws.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plivo.models.ws.enums.MessageType;

public class InfoResponse extends ServerMessage {
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private Object data;
    
    public InfoResponse() {
        setType(MessageType.INFO);
    }
    
    public InfoResponse(String message, String requestId) {
        setType(MessageType.INFO);
        setRequestId(requestId);
        this.message = message;
    }
    
    public InfoResponse(String message, Object data, String requestId) {
        setType(MessageType.INFO);
        setRequestId(requestId);
        this.message = message;
        this.data = data;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
}
