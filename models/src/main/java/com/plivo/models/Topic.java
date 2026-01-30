package com.plivo.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Topic {
    
    // Default queue capacity for backpressure handling
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;
    private static final int DEFAULT_HISTORY_SIZE = 100;
    
    @JsonProperty
    private String name;
    
    @JsonProperty
    private Instant createdAt;
    
    private AtomicLong messageCount;
    
    private AtomicLong subscriberCount;
    
    private ConcurrentHashMap<String, Subscription> subscribers;
    
    private List<MessageEnvelope> messageHistory;
    
    // Bounded queue for backpressure handling
    private BlockingQueue<MessageEnvelope> messageQueue;
    
    private int queueCapacity;
    
    // Flag to indicate if topic is accepting messages
    private AtomicBoolean acceptingMessages;
    
    public Topic() {
        this.messageCount = new AtomicLong(0);
        this.subscriberCount = new AtomicLong(0);
        this.subscribers = new ConcurrentHashMap<>();
        this.messageHistory = new ArrayList<>();
        this.queueCapacity = DEFAULT_QUEUE_CAPACITY;
        this.messageQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.acceptingMessages = new AtomicBoolean(true);
    }
    
    public Topic(String name) {
        this.name = name;
        this.createdAt = Instant.now();
        this.messageCount = new AtomicLong(0);
        this.subscriberCount = new AtomicLong(0);
        this.subscribers = new ConcurrentHashMap<>();
        this.messageHistory = new ArrayList<>();
        this.queueCapacity = DEFAULT_QUEUE_CAPACITY;
        this.messageQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.acceptingMessages = new AtomicBoolean(true);
    }
    
    public Topic(String name, int queueCapacity) {
        this.name = name;
        this.createdAt = Instant.now();
        this.messageCount = new AtomicLong(0);
        this.subscriberCount = new AtomicLong(0);
        this.subscribers = new ConcurrentHashMap<>();
        this.messageHistory = new ArrayList<>();
        this.queueCapacity = queueCapacity > 0 ? queueCapacity : DEFAULT_QUEUE_CAPACITY;
        this.messageQueue = new LinkedBlockingQueue<>(this.queueCapacity);
        this.acceptingMessages = new AtomicBoolean(true);
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
        // Keep only last N messages
        if (messageHistory.size() > DEFAULT_HISTORY_SIZE) {
            messageHistory.remove(0);
        }
    }
    
    public synchronized List<MessageEnvelope> getLastNMessages(int n) {
        int size = messageHistory.size();
        int fromIndex = Math.max(0, size - n);
        return new ArrayList<>(messageHistory.subList(fromIndex, size));
    }
    
    /**
     * Offers a message to the queue (non-blocking).
     * Returns true if message was added, false if queue is full.
     */
    public boolean offerMessage(MessageEnvelope message) {
        if (!acceptingMessages.get()) {
            return false;
        }
        
        boolean added = messageQueue.offer(message);
        
        // If queue is full, stop accepting messages
        if (!added) {
            acceptingMessages.set(false);
        }
        
        return added;
    }
    
    /**
     * Polls a message from the queue (non-blocking).
     * Returns null if queue is empty.
     */
    public MessageEnvelope pollMessage() {
        return messageQueue.poll();
    }
    
    /**
     * Checks if the message queue is full.
     */
    public boolean isQueueFull() {
        return messageQueue.remainingCapacity() == 0;
    }
    
    /**
     * Gets the current queue size.
     */
    public int getQueueSize() {
        return messageQueue.size();
    }
    
    /**
     * Gets the queue capacity.
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }
    
    /**
     * Checks if topic is accepting new messages.
     */
    public boolean isAcceptingMessages() {
        return acceptingMessages.get();
    }
    
    /**
     * Stops accepting new messages (used for graceful shutdown).
     */
    public void stopAcceptingMessages() {
        acceptingMessages.set(false);
    }
    
    /**
     * Resumes accepting messages (if queue has space).
     */
    public void resumeAcceptingMessages() {
        if (!isQueueFull()) {
            acceptingMessages.set(true);
        }
    }
    
    /**
     * Drains messages from queue for graceful shutdown.
     */
    public List<MessageEnvelope> drainQueue() {
        List<MessageEnvelope> drained = new ArrayList<>();
        messageQueue.drainTo(drained);
        return drained;
    }
}
