package com.plivo.server.resources;

import com.plivo.core.repository.TopicRepository;
import com.plivo.models.Topic;
import com.plivo.models.http.TopicStats;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/stats")
@Produces(MediaType.APPLICATION_JSON)
public class StatsResource {
    
    private final TopicRepository topicRepository;
    
    public StatsResource(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }
    
    //Get system statistics
    @GET
    public Response getStats() {
        Map<String, TopicStats> topicStatsMap = new HashMap<>();
        
        for (Topic topic : topicRepository.getAllTopics()) {
            TopicStats stats = new TopicStats(
                topic.getMessageCount(),
                topic.getSubscriberCount()
            );
            topicStatsMap.put(topic.getName(), stats);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("topics", topicStatsMap);
        
        return Response.ok(response).build();
    }
}
