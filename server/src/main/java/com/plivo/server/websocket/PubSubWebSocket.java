package com.plivo.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plivo.core.service.PubSubService;
import com.plivo.core.service.WebSocketErrorService;
import com.plivo.models.ws.request.ClientMessage;
import com.plivo.models.ws.request.ClientMessageVisitor;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class PubSubWebSocket {
    
    private static final Logger log = LoggerFactory.getLogger(PubSubWebSocket.class);
    
    private final PubSubService pubSubService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Session, String> sessionToClientId;
    
    public PubSubWebSocket(PubSubService pubSubService, ObjectMapper objectMapper) {
        this.pubSubService = pubSubService;
        this.objectMapper = objectMapper;
        this.sessionToClientId = new ConcurrentHashMap<>();
    }
    
    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("WebSocket connection established: {}", session.getRemoteAddress());
    }
    
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        log.info("WebSocket connection closed: {} - {}", statusCode, reason);
        
        // Remove all subscriptions for this client
        String clientId = sessionToClientId.remove(session);
        if (clientId != null) {
            pubSubService.removeAllSubscriptions(clientId);
            log.info("Removed all subscriptions for client: {}", clientId);
        }
    }
    
    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket error for session {}: {}", session.getRemoteAddress(), error.getMessage(), error);
    }
    
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        log.debug("Received message: {}", message);
        
        try {
            ClientMessage clientMsg = objectMapper.readValue(message, ClientMessage.class);
            
            if (clientMsg.getType() == null) {
                WebSocketErrorService.sendError(session, objectMapper, "INVALID_MESSAGE", "Message type is required", null);
                return;
            }
            
            // Using visitor pattern to handle message
            ClientMessageVisitor visitor = new WebSocketMessageHandler(
                session, 
                pubSubService, 
                objectMapper, 
                sessionToClientId
            );
            clientMsg.accept(visitor);
            
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            WebSocketErrorService.sendError(session, objectMapper, "INTERNAL_ERROR", "Failed to process message: " + e.getMessage(), null);
        }
    }
}
