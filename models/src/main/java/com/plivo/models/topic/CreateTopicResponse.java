package com.plivo.models.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateTopicResponse {
    
    @JsonProperty
    private String status;
    
    @JsonProperty
    private String topic;
    
    public CreateTopicResponse() {}
    
    public CreateTopicResponse(String status, String topic) {
        this.status = status;
        this.topic = topic;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
}

