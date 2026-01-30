package com.plivo.models.ws.request;

import com.plivo.models.ws.enums.MessageType;

public class PingRequest extends ClientMessage {
    
    public PingRequest() {
        setType(MessageType.PING);
    }
    
    public PingRequest(String requestId) {
        setType(MessageType.PING);
        setRequestId(requestId);
    }
    
    @Override
    public void accept(ClientMessageVisitor visitor) {
        visitor.visit(this);
    }
}
