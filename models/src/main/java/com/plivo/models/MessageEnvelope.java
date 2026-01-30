package com.plivo.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageEnvelope {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("payload")
    private Object payload;
    
    @JsonProperty("published_at")
    private long publishedAt;
    
    public MessageEnvelope() {}
    
    public MessageEnvelope(String id, Object payload, long publishedAt) {
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

