package com.plivo.models.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public class CreateTopicRequest {
    
    @NotEmpty(message = "Topic name is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Topic name must contain only alphanumeric characters, hyphens, and underscores")
    @JsonProperty
    private String name;
    
    public CreateTopicRequest() {}
    
    public CreateTopicRequest(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
