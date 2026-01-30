package com.plivo.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plivo.models.ws.response.ErrorResponse;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Common service for handling WebSocket error responses.
 * Centralizes error response creation and sending logic.
 */
public class WebSocketErrorService {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketErrorService.class);
    
    private final Session session;
    private final ObjectMapper objectMapper;
    
    public WebSocketErrorService(Session session, ObjectMapper objectMapper) {
        this.session = session;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Sends an error response to the WebSocket client.
     *
     * @param code The error code (e.g., "INVALID_REQUEST", "TOPIC_NOT_FOUND")
     * @param message The error message
     * @param requestId The original request ID for correlation (can be null)
     */
    public void sendError(String code, String message, String requestId) {
        ErrorResponse error = new ErrorResponse(code, message, requestId);
        sendErrorResponse(error);
    }
    
    /**
     * Sends an error response to the WebSocket client.
     *
     * @param error The ErrorResponse object to send
     */
    public void sendErrorResponse(ErrorResponse error) {
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(error);
                session.getRemote().sendString(json);
                log.debug("Sent error: code={}, message={}", error.getCode(), error.getMessage());
            } catch (IOException e) {
                log.error("Failed to send error message: {}", e.getMessage(), e);
            }
        } else {
            log.warn("Cannot send error - session is null or closed");
        }
    }
    
    /**
     * Static utility method for sending errors when you have a session and ObjectMapper.
     *
     * @param session The WebSocket session
     * @param objectMapper The ObjectMapper for JSON serialization
     * @param code The error code
     * @param message The error message
     * @param requestId The original request ID (can be null)
     */
    public static void sendError(Session session, ObjectMapper objectMapper, String code, String message, String requestId) {
        ErrorResponse error = new ErrorResponse(code, message, requestId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(error);
                session.getRemote().sendString(json);
            } catch (IOException e) {
                log.error("Failed to send error message: {}", e.getMessage(), e);
            }
        }
    }
}
