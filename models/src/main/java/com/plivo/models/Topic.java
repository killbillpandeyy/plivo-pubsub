package com.plivo.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;

public class Topic {
    
    @JsonProperty
    private String name;
    
    @JsonProperty
    private Instant createdAt;
    
    private AtomicLong messageCount;
    
    private AtomicLong subscriberCount;
    
    private ConcurrentHashMap<String, Subscription> subscribers;
    
    private List<MessageEnvelope> messageHistory;
    
    public Topic() {
        this.messageCount = new AtomicLong(0);
        this.subscriberCount = new AtomicLong(0);
        this.subscribers = new ConcurrentHashMap<>();
        this.messageHistory = new ArrayList<>();
    }
    
    public Topic(String name) {
        this.name = name;
        this.createdAt = Instant.now();
        this.messageCount = new AtomicLong(0);
        this.subscriberCount = new AtomicLong(0);
        this.subscribers = new ConcurrentHashMap<>();
        this.messageHistory = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    

    
    @JsonProperty("messageCount")
    public long getMessageCount() {
        return messageCount.get();
    }
    
    public void incrementMessageCount() {
        messageCount.incrementAndGet();
    }
    
    @JsonProperty("subscriberCount")
    public long getSubscriberCount() {
        return subscriberCount.get();
    }
    
    public void incrementSubscriberCount() {
        subscriberCount.incrementAndGet();
    }
    
    public void decrementSubscriberCount() {
        subscriberCount.decrementAndGet();
    }
    
    public ConcurrentHashMap<String, Subscription> getSubscribers() {
        return subscribers;
    }
    
    public synchronized void addToHistory(MessageEnvelope message) {
        messageHistory.add(message);
        // Keep only last 100 messages
        if (messageHistory.size() > 100) {
            messageHistory.remove(0);
        }
    }
    
    public synchronized List<MessageEnvelope> getLastNMessages(int n) {
        int size = messageHistory.size();
        int fromIndex = Math.max(0, size - n);
        return new ArrayList<>(messageHistory.subList(fromIndex, size));
    }
}
