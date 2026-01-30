package com.plivo.core.exceptions;

public class QueueOverflowException extends RuntimeException {
    
    private final String topicName;
    private final int queueSize;
    private final int queueCapacity;
    
    public QueueOverflowException(String topicName, int queueSize, int queueCapacity) {
        super(String.format("Queue overflow for topic '%s': %d/%d messages", 
                topicName, queueSize, queueCapacity));
        this.topicName = topicName;
        this.queueSize = queueSize;
        this.queueCapacity = queueCapacity;
    }
    
    public String getTopicName() {
        return topicName;
    }
    
    public int getQueueSize() {
        return queueSize;
    }
    
    public int getQueueCapacity() {
        return queueCapacity;
    }
}
