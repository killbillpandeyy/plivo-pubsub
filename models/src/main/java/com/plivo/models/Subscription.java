package com.plivo.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Subscription {
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("topic")
    private String topic;
    
    @JsonProperty("subscribed_at")
    private long subscribedAt;
    
    private transient Object session;
    
    public Subscription() {}
    
    public Subscription(String clientId, String topic, Object session) {
        this.clientId = clientId;
        this.topic = topic;
        this.session = session;
        this.subscribedAt = System.currentTimeMillis();
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public long getSubscribedAt() {
        return subscribedAt;
    }
    
    public void setSubscribedAt(long subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
    
    public Object getSession() {
        return session;
    }
    
    public void setSession(Object session) {
        this.session = session;
    }
}
