package com.plivo.core.service;

import com.plivo.core.repository.TopicRepository;
import com.plivo.core.exceptions.TopicNotFoundException;
import com.plivo.core.exceptions.QueueOverflowException;
import com.plivo.models.Topic;
import com.plivo.models.Subscription;
import com.plivo.models.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PubSubService {
    
    private static final Logger log = LoggerFactory.getLogger(PubSubService.class);
    
    private final TopicRepository topicRepository;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Subscription>> topicSubscriptions;
    
    public PubSubService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
        this.topicSubscriptions = new ConcurrentHashMap<>();
    }
    
    //Subscribe a client to a topic
    public void subscribe(String topicName, String clientId, Object session, Integer lastN) {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new TopicNotFoundException(topicName);
        }
        
        // Create subscription
        Subscription subscription = new Subscription(clientId, topicName, session);
        
        // Get or create topic subscriptions map
        ConcurrentHashMap<String, Subscription> subs = topicSubscriptions
                .computeIfAbsent(topicName, k -> new ConcurrentHashMap<>());
        
        // Add subscription
        subs.put(clientId, subscription);
        topic.getSubscribers().put(clientId, subscription);
        topic.incrementSubscriberCount();
        
        log.info("Client {} subscribed to topic {}", clientId, topicName);
        
        // Send message history if requested
        if (lastN != null && lastN > 0) {
            List<MessageEnvelope> history = topic.getLastNMessages(lastN);
            log.info("Sending {} historical messages to client {}", history.size(), clientId);
            // History will be sent by the caller
        }
    }
    
    //Unsubscribe a client from a topic
    public void unsubscribe(String topicName, String clientId) {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new TopicNotFoundException(topicName);
        }
        
        ConcurrentHashMap<String, Subscription> subs = topicSubscriptions.get(topicName);
        if (subs != null) {
            subs.remove(clientId);
            topic.getSubscribers().remove(clientId);
            topic.decrementSubscriberCount();
            log.info("Client {} unsubscribed from topic {}", clientId, topicName);
        }
    }
    
    //Publish a message to a topic
    public MessageEnvelope publish(String topicName, String messageId, Object payload) {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new TopicNotFoundException(topicName);
        }
        
        // Check if topic is accepting messages (backpressure check)
        if (!topic.isAcceptingMessages()) {
            log.warn("Topic {} is not accepting messages - queue is full", topicName);
            throw new QueueOverflowException(topicName, topic.getQueueSize(), topic.getQueueCapacity());
        }
        
        // Generate message ID if not provided
        if (messageId == null || messageId.isEmpty()) {
            messageId = UUID.randomUUID().toString();
        }
        
        // Create message envelope
        MessageEnvelope envelope = new MessageEnvelope(
            messageId,
            payload,
            System.currentTimeMillis()
        );
        
        // Try to add message to queue (backpressure handling)
        boolean added = topic.offerMessage(envelope);
        
        if (!added) {
            log.error("Failed to add message to queue for topic {} - queue overflow", topicName);
            throw new QueueOverflowException(topicName, topic.getQueueSize(), topic.getQueueCapacity());
        }
        
        // Add to topic history
        topic.addToHistory(envelope);
        topic.incrementMessageCount();
        
        log.info("Published message {} to topic {}", messageId, topicName);
        
        return envelope;
    }
    
    /**
     * Get all subscribers for a topic
     */
    public ConcurrentHashMap<String, Subscription> getTopicSubscribers(String topicName) {
        return topicSubscriptions.get(topicName);
    }
    
    /**
     * Get subscription for a specific client
     */
    public Subscription getSubscription(String topicName, String clientId) {
        ConcurrentHashMap<String, Subscription> subs = topicSubscriptions.get(topicName);
        return subs != null ? subs.get(clientId) : null;
    }
    
    /**
     * Remove all subscriptions for a client (on disconnect)
     */
    public void removeAllSubscriptions(String clientId) {
        topicSubscriptions.forEach((topicName, subs) -> {
            if (subs.remove(clientId) != null) {
                Topic topic = topicRepository.getTopic(topicName);
                if (topic != null) {
                    topic.getSubscribers().remove(clientId);
                    topic.decrementSubscriberCount();
                }
                log.info("Removed subscription for client {} from topic {}", clientId, topicName);
            }
        });
    }
    
    /**
     * Get message history for a topic
     */
    public List<MessageEnvelope> getMessageHistory(String topicName, int lastN) {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new TopicNotFoundException(topicName);
        }
        return topic.getLastNMessages(lastN);
    }
    
    /**
     * Get queue statistics for a topic
     */
    public QueueStats getQueueStats(String topicName) {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new TopicNotFoundException(topicName);
        }
        
        return new QueueStats(
            topic.getQueueSize(),
            topic.getQueueCapacity(),
            topic.isAcceptingMessages(),
            topic.isQueueFull()
        );
    }
    
    /**
     * Initiate graceful shutdown for a topic (stops accepting new messages)
     */
    public void initiateGracefulShutdown(String topicName) {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new TopicNotFoundException(topicName);
        }
        
        topic.stopAcceptingMessages();
        log.warn("Initiated graceful shutdown for topic {} - no longer accepting messages", topicName);
    }
    
    /**
     * Drain remaining messages from topic queue
     */
    public List<MessageEnvelope> drainTopicQueue(String topicName) {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new TopicNotFoundException(topicName);
        }
        
        List<MessageEnvelope> drained = topic.drainQueue();
        log.info("Drained {} messages from topic {} queue", drained.size(), topicName);
        return drained;
    }
    
    /**
     * Resume accepting messages for a topic (if queue has space)
     */
    public void resumeAcceptingMessages(String topicName) {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new TopicNotFoundException(topicName);
        }
        
        topic.resumeAcceptingMessages();
        
        if (topic.isAcceptingMessages()) {
            log.info("Resumed accepting messages for topic {}", topicName);
        } else {
            log.warn("Cannot resume accepting messages for topic {} - queue is still full", topicName);
        }
    }
    
    /**
     * Inner class to hold queue statistics
     */
    public static class QueueStats {
        private final int size;
        private final int capacity;
        private final boolean acceptingMessages;
        private final boolean full;
        
        public QueueStats(int size, int capacity, boolean acceptingMessages, boolean full) {
            this.size = size;
            this.capacity = capacity;
            this.acceptingMessages = acceptingMessages;
            this.full = full;
        }
        
        public int getSize() {
            return size;
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        public boolean isAcceptingMessages() {
            return acceptingMessages;
        }
        
        public boolean isFull() {
            return full;
        }
    }
}
