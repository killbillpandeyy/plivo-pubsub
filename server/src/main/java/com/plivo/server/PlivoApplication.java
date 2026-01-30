package com.plivo.server;

import com.plivo.core.repository.TopicRepository;
import com.plivo.core.service.PubSubService;
import com.plivo.server.health.ApplicationHealthCheck;
import com.plivo.server.resources.HealthResource;
import com.plivo.server.resources.StatsResource;
import com.plivo.server.resources.TopicResource;
import com.plivo.server.websocket.PubSubWebSocketCreator;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class PlivoApplication extends Application<PlivoConfiguration> {
    
    private static final Logger log = LoggerFactory.getLogger(PlivoApplication.class);

    public static void main(String[] args) throws Exception {
        new PlivoApplication().run(args);
    }

    @Override
    public String getName() {
        return "plivo-pubsub-application";
    }

    @Override
    public void initialize(Bootstrap<PlivoConfiguration> bootstrap) {
        // Add bundles, commands, etc.
        // WebSocket configuration will be added here later
    }

    @Override
    public void run(PlivoConfiguration configuration, Environment environment) {
        log.info("Initializing Plivo PubSub Application");
        
        // Initialize in-memory repository
        final TopicRepository topicRepository = new TopicRepository();
        log.info("Topic repository initialized");
        
        // Initialize PubSub service
        final PubSubService pubSubService = new PubSubService(topicRepository);
        log.info("PubSub service initialized");
        
        // Register REST resources
        final TopicResource topicResource = new TopicResource(topicRepository);
        environment.jersey().register(topicResource);
        log.info("TopicResource registered");
        
        final HealthResource healthResource = new HealthResource(topicRepository);
        environment.jersey().register(healthResource);
        log.info("HealthResource registered");
        
        final StatsResource statsResource = new StatsResource(topicRepository);
        environment.jersey().register(statsResource);
        log.info("StatsResource registered");
        
        // Register health checks
        final ApplicationHealthCheck healthCheck = new ApplicationHealthCheck();
        environment.healthChecks().register("application", healthCheck);
        log.info("Health checks registered");
        
        // Configure WebSocket
        configureWebSocket(environment, pubSubService);
        
        log.info("Plivo PubSub Application initialization complete");
    }
    
    private void configureWebSocket(Environment environment, PubSubService pubSubService) {
        try {
            Server server = environment.getApplicationContext().getServer();
            ServletContextHandler context = environment.getApplicationContext();
            
            // Configure WebSocket
            JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
                wsContainer.setMaxTextMessageSize(65536);
                wsContainer.setIdleTimeout(Duration.ofMinutes(5));
                
                PubSubWebSocketCreator creator = new PubSubWebSocketCreator(
                    pubSubService,
                    environment.getObjectMapper()
                );
                
                wsContainer.addMapping("/ws", creator);
                log.info("WebSocket endpoint configured at /ws");
            });
            
        } catch (Exception e) {
            log.error("Failed to configure WebSocket", e);
            throw new RuntimeException("Failed to configure WebSocket", e);
        }
    }
}
