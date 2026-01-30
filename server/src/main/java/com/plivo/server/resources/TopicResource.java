package com.plivo.server.resources;

import com.plivo.core.exceptions.TopicAlreadyExistsException;
import com.plivo.core.exceptions.TopicNotFoundException;
import com.plivo.core.repository.TopicRepository;
import com.plivo.models.Topic;
import com.plivo.models.http.CreateTopicRequest;
import com.plivo.models.http.CreateTopicResponse;
import com.plivo.models.http.TopicInfo;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/topics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TopicResource {
    
    private static final Logger log = LoggerFactory.getLogger(TopicResource.class);
    private final TopicRepository topicRepository;
    
    public TopicResource(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }
    
    //Create a new topic
    @POST
    public Response createTopic(@Valid CreateTopicRequest request) {
        try {
            String topicName = request.getName();
            log.info("Creating topic: {}", topicName);
            
            Topic topic = topicRepository.createTopic(topicName);
            
            CreateTopicResponse response = new CreateTopicResponse("created", topicName);
            log.info("Topic created successfully: {}", topicName);
            
            return Response.status(Response.Status.CREATED).entity(response).build();
            
        } catch (TopicAlreadyExistsException e) {
            log.warn("Topic already exists: {}", e.getTopicName());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Topic already exists");
            error.put("topic", e.getTopicName());
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }
    }
    
    
    // Delete a topic
    @DELETE
    @Path("/{name}")
    public Response deleteTopic(@PathParam("name") String name) {
        try {
            log.info("Deleting topic: {}", name);
            topicRepository.deleteTopic(name);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "deleted");
            response.put("topic", name);
            
            log.info("Topic deleted successfully: {}", name);
            return Response.ok(response).build();
            
        } catch (TopicNotFoundException e) {
            log.warn("Topic not found for deletion: {}", name);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Topic not found");
            error.put("topic", name);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
    }
    
    
    //List all topics
    @GET
    public Response listTopics() {
        log.debug("Listing all topics");
        
        List<TopicInfo> topicInfos = topicRepository.getAllTopics().stream()
                .map(topic -> new TopicInfo(topic.getName(), topic.getSubscriberCount()))
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("topics", topicInfos);
        
        log.debug("Returning {} topics", topicInfos.size());
        return Response.ok(response).build();
    }
}
