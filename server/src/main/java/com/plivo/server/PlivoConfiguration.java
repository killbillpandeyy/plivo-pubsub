package com.plivo.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.NotEmpty;

public class PlivoConfiguration extends Configuration {

    @NotEmpty
    @JsonProperty
    private String applicationName = "Plivo PubSub Application";

    @JsonProperty
    private String version = "1.0.0";
    
    public String getApplicationName() {
        return applicationName;
    }
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
}

