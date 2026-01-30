package com.plivo.core.repository;

import com.plivo.models.Topic;
import com.plivo.core.exceptions.TopicAlreadyExistsException;
import com.plivo.core.exceptions.TopicNotFoundException;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class TopicRepository {
    
    private final ConcurrentHashMap<String, Topic> topics;
    private final Instant startTime;
    
    public TopicRepository() {
        this.topics = new ConcurrentHashMap<>();
        this.startTime = Instant.now();
    }
    
    //Create a new topic
    public Topic createTopic(String name) {
        Topic newTopic = new Topic(name);
        Topic existing = topics.putIfAbsent(name, newTopic);
        if (existing != null) {
            throw new TopicAlreadyExistsException(name);
        }
        return newTopic;
    }
    
    //Get a topic by name
    public Topic getTopic(String name) {
        Topic topic = topics.get(name);
        if (topic == null) {
            throw new TopicNotFoundException(name);
        }
        return topic;
    }
    
    //Get all topics
    public Collection<Topic> getAllTopics() {
        return topics.values();
    }
    
    //Delete a topic
    public void deleteTopic(String name) {
        Topic removed = topics.remove(name);
        if (removed == null) {
            throw new TopicNotFoundException(name);
        }
    }
    
    // Check if topic exists
    public boolean topicExists(String name) {
        return topics.containsKey(name);
    }
    
    //Get total number of topics
    public int getTopicCount() {
        return topics.size();
    }
    
    //Get total subscriber count across all topics
    public long getTotalSubscriberCount() {
        return topics.values().stream()
                .mapToLong(Topic::getSubscriberCount)
                .sum();
    }
    
    //Get uptime in seconds
    public long getUptimeSeconds() {
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }
    
    //Clear all topics (for testing)
    public void clear() {
        topics.clear();
    }
}
