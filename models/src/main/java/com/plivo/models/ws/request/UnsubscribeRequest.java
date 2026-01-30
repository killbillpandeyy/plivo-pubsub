package com.plivo.models.ws.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plivo.models.ws.enums.MessageType;
import jakarta.validation.constraints.NotEmpty;

public class UnsubscribeRequest extends ClientMessage {
    
    @JsonProperty("topic")
    @NotEmpty(message = "Topic name cannot be empty")
    private String topic;
    
    @JsonProperty("client_id")
    @NotEmpty(message = "Client ID cannot be empty")
    private String clientId;
    
    public UnsubscribeRequest() {
        setType(MessageType.UNSUBSCRIBE);
    }
    
    public UnsubscribeRequest(String topic, String clientId, String requestId) {
        setType(MessageType.UNSUBSCRIBE);
        setRequestId(requestId);
        this.topic = topic;
        this.clientId = clientId;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    @Override
    public void accept(ClientMessageVisitor visitor) {
        visitor.visit(this);
    }
}

