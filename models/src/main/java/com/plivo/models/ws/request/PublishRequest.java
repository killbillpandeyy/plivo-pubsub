package com.plivo.models.ws.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plivo.models.ws.enums.MessageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class PublishRequest extends ClientMessage {
    
    @JsonProperty("topic")
    @NotEmpty(message = "Topic name cannot be empty")
    private String topic;
    
    @JsonProperty("message")
    @NotNull(message = "Message cannot be null")
    @Valid
    private Message message;
    
    public PublishRequest() {
        setType(MessageType.PUBLISH);
    }
    
    public PublishRequest(String topic, Message message, String requestId) {
        setType(MessageType.PUBLISH);
        setRequestId(requestId);
        this.topic = topic;
        this.message = message;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public Message getMessage() {
        return message;
    }
    
    public void setMessage(Message message) {
        this.message = message;
    }
    
    @Override
    public void accept(ClientMessageVisitor visitor) {
        visitor.visit(this);
    }
    
    public static class Message {
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("payload")
        @NotNull(message = "Payload cannot be null")
        private Object payload;
        
        public Message() {}
        
        public Message(String id, Object payload) {
            this.id = id;
            this.payload = payload;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public Object getPayload() {
            return payload;
        }
        
        public void setPayload(Object payload) {
            this.payload = payload;
        }
    }
}
