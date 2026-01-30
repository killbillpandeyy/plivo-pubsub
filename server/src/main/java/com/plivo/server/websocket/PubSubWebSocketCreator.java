package com.plivo.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plivo.core.service.PubSubService;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;

public class PubSubWebSocketCreator implements JettyWebSocketCreator {
    
    private final PubSubService pubSubService;
    private final ObjectMapper objectMapper;
    
    public PubSubWebSocketCreator(PubSubService pubSubService, ObjectMapper objectMapper) {
        this.pubSubService = pubSubService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Object createWebSocket(org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest req,
                                   org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse resp) {
        return new PubSubWebSocket(pubSubService, objectMapper);
    }
}
