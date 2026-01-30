package com.plivo.models.ws.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plivo.models.ws.enums.MessageType;

public class PongResponse extends ServerMessage {
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    public PongResponse() {
        setType(MessageType.PONG);
    }
    
    public PongResponse(long timestamp, String requestId) {
        setType(MessageType.PONG);
        setRequestId(requestId);
        this.timestamp = timestamp;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
