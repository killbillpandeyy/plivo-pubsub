package com.plivo.models.ws.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plivo.models.ws.enums.MessageType;

public class EventResponse extends ServerMessage {
    
    @JsonProperty("topic")
    private String topic;
    
    @JsonProperty("message")
    private MessageData message;
    
    public EventResponse() {
        setType(MessageType.EVENT);
    }
    
    public EventResponse(String topic, MessageData message) {
        setType(MessageType.EVENT);
        this.topic = topic;
        this.message = message;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public MessageData getMessage() {
        return message;
    }
    
    public void setMessage(MessageData message) {
        this.message = message;
    }
    
    public static class MessageData {
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("payload")
        private Object payload;
        
        @JsonProperty("published_at")
        private long publishedAt;
        
        public MessageData() {}
        
        public MessageData(String id, Object payload, long publishedAt) {
            this.id = id;
            this.payload = payload;
            this.publishedAt = publishedAt;
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
        
        public long getPublishedAt() {
            return publishedAt;
        }
        
        public void setPublishedAt(long publishedAt) {
            this.publishedAt = publishedAt;
        }
    }
}
