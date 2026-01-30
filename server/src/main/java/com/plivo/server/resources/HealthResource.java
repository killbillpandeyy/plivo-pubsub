package com.plivo.server.resources;

import com.plivo.core.repository.TopicRepository;
import com.plivo.models.http.HealthResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {
    
    private final TopicRepository topicRepository;
    
    public HealthResource(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }
    
    //Get system health
    @GET
    public Response getHealth() {
        long uptimeSec = topicRepository.getUptimeSeconds();
        int topicCount = topicRepository.getTopicCount();
        long subscriberCount = topicRepository.getTotalSubscriberCount();
        
        HealthResponse response = new HealthResponse(uptimeSec, topicCount, subscriberCount);
        
        return Response.ok(response).build();
    }
}
