package com.plivo.models.ws.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plivo.models.ws.enums.MessageType;

public class AckResponse extends ServerMessage {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    public AckResponse() {
        setType(MessageType.ACK);
    }
    
    public AckResponse(String status, String message, String requestId) {
        setType(MessageType.ACK);
        setRequestId(requestId);
        this.status = status;
        this.message = message;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
