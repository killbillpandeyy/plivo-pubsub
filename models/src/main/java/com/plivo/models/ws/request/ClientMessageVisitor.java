package com.plivo.models.ws.request;

public interface ClientMessageVisitor {
    
    void visit(SubscribeRequest request);
    
    void visit(UnsubscribeRequest request);
    
    void visit(PublishRequest request);
    
    
    void visit(PingRequest request);
}
