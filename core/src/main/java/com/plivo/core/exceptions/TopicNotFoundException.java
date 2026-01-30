package com.plivo.core.exceptions;

public class TopicNotFoundException extends RuntimeException {
    
    private final String topicName;
    
    public TopicNotFoundException(String topicName) {
        super("Topic not found: " + topicName);
        this.topicName = topicName;
    }
    
    public String getTopicName() {
        return topicName;
    }
}
