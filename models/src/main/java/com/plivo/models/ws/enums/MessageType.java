package com.plivo.models.ws.enums;

public enum MessageType {
    SUBSCRIBE("subscribe"),
    UNSUBSCRIBE("unsubscribe"),
    PUBLISH("publish"),
    PING("ping"),
    ACK("ack"),
    EVENT("event"),
    ERROR("error"),
    PONG("pong"),
    INFO("info");
    
    private final String value;
    
    MessageType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
