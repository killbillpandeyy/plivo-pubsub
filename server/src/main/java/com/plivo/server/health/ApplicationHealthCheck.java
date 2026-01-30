package com.plivo.server.health;

import com.codahale.metrics.health.HealthCheck;

public class ApplicationHealthCheck extends HealthCheck {

    public ApplicationHealthCheck() {
    }

    @Override
    protected Result check() throws Exception {
        // Add your health check logic here
        return Result.healthy("Application is running");
    }
}
