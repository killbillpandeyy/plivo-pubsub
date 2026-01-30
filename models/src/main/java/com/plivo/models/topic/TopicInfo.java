package com.plivo.models.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TopicInfo {
    
    @JsonProperty
    private String name;
    
    @JsonProperty
    private long subscribers;
    
    public TopicInfo() {}
    
    public TopicInfo(String name, long subscribers) {
        this.name = name;
        this.subscribers = subscribers;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getSubscribers() {
        return subscribers;
    }
    
    public void setSubscribers(long subscribers) {
        this.subscribers = subscribers;
    }
}
