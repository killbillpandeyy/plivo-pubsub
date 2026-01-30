package com.plivo.models.ws.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.plivo.models.ws.enums.MessageType;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AckResponse.class, name = "ack"),
    @JsonSubTypes.Type(value = EventResponse.class, name = "event"),
    @JsonSubTypes.Type(value = ErrorResponse.class, name = "error"),
    @JsonSubTypes.Type(value = PongResponse.class, name = "pong"),
    @JsonSubTypes.Type(value = InfoResponse.class, name = "info")
})
public abstract class ServerMessage {
    
    @JsonProperty("type")
    private MessageType type;
    
    @JsonProperty("request_id")
    private String requestId;
    
    protected ServerMessage() {
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
