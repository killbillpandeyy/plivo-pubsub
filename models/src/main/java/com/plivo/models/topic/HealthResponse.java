package com.plivo.models.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthResponse {
    
    @JsonProperty("uptime_sec")
    private long uptimeSec;
    
    @JsonProperty
    private int topics;
    
    @JsonProperty
    private long subscribers;
    
    public HealthResponse() {}
    
    public HealthResponse(long uptimeSec, int topics, long subscribers) {
        this.uptimeSec = uptimeSec;
        this.topics = topics;
        this.subscribers = subscribers;
    }
    
    public long getUptimeSec() {
        return uptimeSec;
    }
    
    public void setUptimeSec(long uptimeSec) {
        this.uptimeSec = uptimeSec;
    }
    
    public int getTopics() {
        return topics;
    }
    
    public void setTopics(int topics) {
        this.topics = topics;
    }
    
    public long getSubscribers() {
        return subscribers;
    }
    
    public void setSubscribers(long subscribers) {
        this.subscribers = subscribers;
    }
}

