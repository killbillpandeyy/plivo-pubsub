package com.plivo.models.ws.request;

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
    @JsonSubTypes.Type(value = SubscribeRequest.class, name = "subscribe"),
    @JsonSubTypes.Type(value = UnsubscribeRequest.class, name = "unsubscribe"),
    @JsonSubTypes.Type(value = PublishRequest.class, name = "publish"),
    @JsonSubTypes.Type(value = PingRequest.class, name = "ping")
})
public abstract class ClientMessage {
    
    @JsonProperty("type")
    private MessageType type;
    
    @JsonProperty("request_id")
    private String requestId;
    
    protected ClientMessage() {
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
    
    /**
     * Accept method for visitor pattern.
     * Subclasses must implement this to call the appropriate visit method.
     * 
     * @param visitor the visitor to accept
     */
    public abstract void accept(ClientMessageVisitor visitor);
}
