package com.plivo.core.service;

import com.plivo.core.repository.TopicRepository;
import com.plivo.core.exceptions.TopicNotFoundException;
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
}
