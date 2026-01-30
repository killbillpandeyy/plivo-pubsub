package com.plivo.models.ws.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plivo.models.ws.enums.MessageType;
import jakarta.validation.constraints.NotEmpty;

public class SubscribeRequest extends ClientMessage {
    
    @JsonProperty("topic")
    @NotEmpty(message = "Topic name cannot be empty")
    private String topic;
    
    @JsonProperty("client_id")
    @NotEmpty(message = "Client ID cannot be empty")
    private String clientId;
    
    @JsonProperty("last_n")
    private Integer lastN;
    
    public SubscribeRequest() {
        setType(MessageType.SUBSCRIBE);
    }
    
    public SubscribeRequest(String topic, String clientId, String requestId) {
        setType(MessageType.SUBSCRIBE);
        setRequestId(requestId);
        this.topic = topic;
        this.clientId = clientId;
    }
    
    public SubscribeRequest(String topic, String clientId, Integer lastN, String requestId) {
        setType(MessageType.SUBSCRIBE);
        setRequestId(requestId);
        this.topic = topic;
        this.clientId = clientId;
        this.lastN = lastN;
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
    
    public Integer getLastN() {
        return lastN;
    }
    
    public void setLastN(Integer lastN) {
        this.lastN = lastN;
    }
    
    @Override
    public void accept(ClientMessageVisitor visitor) {
        visitor.visit(this);
    }
}

