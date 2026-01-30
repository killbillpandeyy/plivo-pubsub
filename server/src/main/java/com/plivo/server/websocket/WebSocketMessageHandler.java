package com.plivo.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plivo.core.exceptions.TopicNotFoundException;
import com.plivo.core.exceptions.QueueOverflowException;
import com.plivo.core.service.PubSubService;
import com.plivo.core.service.WebSocketErrorService;
import com.plivo.models.MessageEnvelope;
import com.plivo.models.Subscription;
import com.plivo.models.ws.request.*;
import com.plivo.models.ws.response.*;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class WebSocketMessageHandler implements ClientMessageVisitor {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageHandler.class);
    
    private final Session session;
    private final PubSubService pubSubService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Session, String> sessionToClientId;
    private final WebSocketErrorService errorService;
    
    public WebSocketMessageHandler(
            Session session,
            PubSubService pubSubService,
            ObjectMapper objectMapper,
            ConcurrentHashMap<Session, String> sessionToClientId) {
        this.session = session;
        this.pubSubService = pubSubService;
        this.objectMapper = objectMapper;
        this.sessionToClientId = sessionToClientId;
        this.errorService = new WebSocketErrorService(session, objectMapper);
    }
    
    @Override
    public void visit(SubscribeRequest request) {
        try {
            String topic = request.getTopic();
            String clientId = request.getClientId();
            Integer lastN = request.getLastN();
            String requestId = request.getRequestId();
            
            if (topic == null || topic.isEmpty()) {
                sendError("INVALID_REQUEST", "Topic name is required", requestId);
                return;
            }
            
            if (clientId == null || clientId.isEmpty()) {
                sendError("INVALID_REQUEST", "Client ID is required", requestId);
                return;
            }
            
            // Track client ID for this session
            sessionToClientId.put(session, clientId);
            
            // Subscribe
            pubSubService.subscribe(topic, clientId, session, lastN);
            
            // Send ACK
            AckResponse ack = new AckResponse("success", "Subscribed to topic: " + topic, requestId);
            sendMessage(ack);
            
            // Send message history if requested
            if (lastN != null && lastN > 0) {
                List<MessageEnvelope> history = pubSubService.getMessageHistory(topic, lastN);
                for (MessageEnvelope envelope : history) {
                    EventResponse event = new EventResponse(
                        topic,
                        new EventResponse.MessageData(
                            envelope.getId(),
                            envelope.getPayload(),
                            envelope.getPublishedAt()
                        )
                    );
                    sendMessage(event);
                }
            }
            
        } catch (TopicNotFoundException e) {
            sendError("TOPIC_NOT_FOUND", e.getMessage(), request.getRequestId());
        } catch (Exception e) {
            log.error("Error handling subscribe: {}", e.getMessage(), e);
            sendError("INTERNAL_ERROR", "Failed to subscribe: " + e.getMessage(), request.getRequestId());
        }
    }
    
    @Override
    public void visit(UnsubscribeRequest request) {
        try {
            String topic = request.getTopic();
            String clientId = request.getClientId();
            String requestId = request.getRequestId();
            
            if (topic == null || topic.isEmpty()) {
                sendError("INVALID_REQUEST", "Topic name is required", requestId);
                return;
            }
            
            if (clientId == null || clientId.isEmpty()) {
                sendError("INVALID_REQUEST", "Client ID is required", requestId);
                return;
            }
            
            pubSubService.unsubscribe(topic, clientId);
            
            AckResponse ack = new AckResponse("success", "Unsubscribed from topic: " + topic, requestId);
            sendMessage(ack);
            
        } catch (TopicNotFoundException e) {
            sendError("TOPIC_NOT_FOUND", e.getMessage(), request.getRequestId());
        } catch (Exception e) {
            log.error("Error handling unsubscribe: {}", e.getMessage(), e);
            sendError("INTERNAL_ERROR", "Failed to unsubscribe: " + e.getMessage(), request.getRequestId());
        }
    }
    
    @Override
    public void visit(PublishRequest request) {
        try {
            String topic = request.getTopic();
            PublishRequest.Message msg = request.getMessage();
            String requestId = request.getRequestId();
            
            if (topic == null || topic.isEmpty()) {
                sendError("INVALID_REQUEST", "Topic name is required", requestId);
                return;
            }
            
            if (msg == null || msg.getPayload() == null) {
                sendError("INVALID_REQUEST", "Message payload is required", requestId);
                return;
            }
            
            // Publish message
            MessageEnvelope envelope = pubSubService.publish(topic, msg.getId(), msg.getPayload());
            
            // Send ACK to publisher
            AckResponse ack = new AckResponse("success", "Message published to topic: " + topic, requestId);
            sendMessage(ack);
            
            // Broadcast to all subscribers
            ConcurrentHashMap<String, Subscription> subscribers = pubSubService.getTopicSubscribers(topic);
            if (subscribers != null) {
                EventResponse event = new EventResponse(
                    topic,
                    new EventResponse.MessageData(
                        envelope.getId(),
                        envelope.getPayload(),
                        envelope.getPublishedAt()
                    )
                );
                
                String eventJson = objectMapper.writeValueAsString(event);
                
                subscribers.values().forEach(sub -> {
                    Session subscriberSession = (Session) sub.getSession();
                    if (subscriberSession != null && subscriberSession.isOpen()) {
                        try {
                            subscriberSession.getRemote().sendString(eventJson);
                            log.debug("Sent event to subscriber: {}", sub.getClientId());
                        } catch (IOException e) {
                            log.error("Failed to send message to subscriber {}: {}", sub.getClientId(), e.getMessage());
                        }
                    }
                });
            }
            
        } catch (QueueOverflowException e) {
            // Handle backpressure - queue is full, consumers are slow
            log.warn("Queue overflow for topic {}: {}/{}", e.getTopicName(), e.getQueueSize(), e.getQueueCapacity());
            String errorMsg = String.format("Topic queue is full (%d/%d messages). Consumers are slow. Topic has stopped accepting new messages.",
                    e.getQueueSize(), e.getQueueCapacity());
            sendError("CONSUMER_IS_SLOW", errorMsg, request.getRequestId());
            
            // Initiate graceful shutdown for this topic
            try {
                pubSubService.initiateGracefulShutdown(e.getTopicName());
                log.warn("Initiated graceful shutdown for topic {}", e.getTopicName());
            } catch (Exception ex) {
                log.error("Failed to initiate graceful shutdown: {}", ex.getMessage());
            }
        } catch (TopicNotFoundException e) {
            sendError("TOPIC_NOT_FOUND", e.getMessage(), request.getRequestId());
        } catch (Exception e) {
            log.error("Error handling publish: {}", e.getMessage(), e);
            sendError("INTERNAL_ERROR", "Failed to publish message: " + e.getMessage(), request.getRequestId());
        }
    }
    
    @Override
    public void visit(PingRequest request) {
        try {
            PongResponse pong = new PongResponse(System.currentTimeMillis(), request.getRequestId());
            sendMessage(pong);
        } catch (Exception e) {
            log.error("Error handling ping: {}", e.getMessage(), e);
        }
    }
    
    private void sendMessage(ServerMessage message) {
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.getRemote().sendString(json);
                log.debug("Sent message: {}", json);
            } catch (IOException e) {
                log.error("Failed to send message: {}", e.getMessage(), e);
            }
        }
    }
    
    private void sendError(String code, String message, String requestId) {
        errorService.sendError(code, message, requestId);
    }
}
