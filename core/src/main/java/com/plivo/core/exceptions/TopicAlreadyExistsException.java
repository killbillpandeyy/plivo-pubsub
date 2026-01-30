package com.plivo.core.exceptions;

public class TopicAlreadyExistsException extends RuntimeException {
    
    private final String topicName;
    
    public TopicAlreadyExistsException(String topicName) {
        super("Topic already exists: " + topicName);
        this.topicName = topicName;
    }
    
    public String getTopicName() {
        return topicName;
    }
}
