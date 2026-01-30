package com.plivo.models.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TopicStats {
    
    @JsonProperty
    private long messages;
    
    @JsonProperty
    private long subscribers;
    
    public TopicStats() {}
    
    public TopicStats(long messages, long subscribers) {
        this.messages = messages;
        this.subscribers = subscribers;
    }
    
    public long getMessages() {
        return messages;
    }
    
    public void setMessages(long messages) {
        this.messages = messages;
    }
    
    public long getSubscribers() {
        return subscribers;
    }
    
    public void setSubscribers(long subscribers) {
        this.subscribers = subscribers;
    }
}

