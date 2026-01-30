# Implementation Checklist

## Phase 1: Core Infrastructure ✓ (Setup Complete)

### Models Module
- [ ] Create `Message.java` - Published message with ID and payload
- [ ] Create `MessageEnvelope.java` - Message wrapper with timestamp
- [ ] Create `Subscription.java` - Subscriber metadata  
- [ ] Create `Topic.java` - Topic with subscribers and ring buffer
- [ ] Create WebSocket message classes:
  - [ ] `ClientMessage.java` - Incoming messages
  - [ ] `ServerMessage.java` - Outgoing messages
  - [ ] `MessageType.java` - Enum for message types
  - [ ] `ErrorCode.java` - Error code enum
- [ ] Create HTTP DTO classes:
  - [ ] `CreateTopicRequest.java`
  - [ ] `CreateTopicResponse.java`
  - [ ] `TopicInfo.java`
  - [ ] `HealthResponse.java`
  - [ ] `StatsResponse.java`
- [ ] Add Jackson annotations for JSON serialization
- [ ] Add validation annotations (@NotNull, @Pattern for UUID)
- [ ] Write unit tests for model classes

### Core Module - Data Structures
- [ ] Implement `RingBuffer.java` with:
  - [ ] Thread-safe add operation
  - [ ] Thread-safe getLastN operation
  - [ ] ReentrantReadWriteLock for concurrency
  - [ ] Unit tests for concurrent access
- [ ] Implement `SubscriberQueue.java` with:
  - [ ] ArrayBlockingQueue backing
  - [ ] Backpressure handling (drop oldest)
  - [ ] Dropped message counter
  - [ ] Unit tests for overflow scenarios
- [ ] Implement `Topic.java` with:
  - [ ] ConcurrentHashMap for subscribers
  - [ ] RingBuffer for message history
  - [ ] Thread-safe addSubscriber/removeSubscriber
  - [ ] Publish method with fan-out
  - [ ] AtomicLong for message count
  - [ ] Unit tests

### Core Module - PubSubManager
- [ ] Create singleton `PubSubManager.java` with:
  - [ ] ConcurrentHashMap<String, Topic> registry
  - [ ] ExecutorService for async publishing
  - [ ] createTopic method
  - [ ] deleteTopic method with subscriber cleanup
  - [ ] getAllTopics method
  - [ ] subscribe method with replay support
  - [ ] unsubscribe method
  - [ ] publish method with fan-out
  - [ ] getHealth method (uptime, counts)
  - [ ] getStats method (per-topic metrics)
  - [ ] shutdown method for graceful cleanup
- [ ] Create `ConnectionRegistry.java` to track WebSocket connections
- [ ] Write unit tests for PubSubManager

## Phase 2: WebSocket Integration

### Dependencies
- [ ] Add Jetty WebSocket dependencies to server/pom.xml:
  ```xml
  <dependency>
    <groupId>org.eclipse.jetty.websocket</groupId>
    <artifactId>websocket-servlet</artifactId>
  </dependency>
  ```

### WebSocket Handler
- [ ] Create `PubSubWebSocketServlet.java`
- [ ] Create `PubSubWebSocket.java` with:
  - [ ] @OnWebSocketConnect handler
  - [ ] @OnWebSocketClose handler
  - [ ] @OnWebSocketMessage handler
  - [ ] @OnWebSocketError handler
  - [ ] Connection tracking
  - [ ] Message parsing and validation
- [ ] Create `MessageProcessor.java` with handlers for:
  - [ ] Subscribe (with last_n replay)
  - [ ] Unsubscribe
  - [ ] Publish
  - [ ] Ping/Pong
- [ ] Implement message validation:
  - [ ] UUID format validation
  - [ ] Required field checks
  - [ ] Topic existence checks
- [ ] Implement error response sending
- [ ] Register WebSocket servlet in PlivoApplication

### Subscription Delivery
- [ ] Implement async delivery loop in `Subscription.java`:
  - [ ] ScheduledExecutorService for delivery
  - [ ] Poll from SubscriberQueue
  - [ ] Send via WebSocket session
  - [ ] Handle send failures
  - [ ] Handle slow consumer scenario
- [ ] Implement server-to-client messages:
  - [ ] ACK messages
  - [ ] EVENT messages
  - [ ] ERROR messages
  - [ ] PONG messages
  - [ ] INFO messages (heartbeat, topic_deleted)

## Phase 3: REST API

### Topic Management Resource
- [ ] Create `TopicResource.java` with:
  - [ ] POST /topics - Create topic
  - [ ] DELETE /topics/{name} - Delete topic
  - [ ] GET /topics - List all topics
  - [ ] Proper HTTP status codes
  - [ ] Error handling
- [ ] Register TopicResource in PlivoApplication

### Observability Resources
- [ ] Create `HealthResource.java`:
  - [ ] GET /health endpoint
  - [ ] Return uptime, topic count, subscriber count
- [ ] Create `StatsResource.java`:
  - [ ] GET /stats endpoint
  - [ ] Return per-topic statistics
  - [ ] Message counts, subscriber counts

### Exception Mappers
- [ ] Create `TopicNotFoundException` → 404
- [ ] Create `TopicAlreadyExistsException` → 409
- [ ] Create `ValidationException` → 400
- [ ] Create generic exception mapper → 500

## Phase 4: Configuration

### PlivoConfiguration
- [ ] Add pubsub configuration class:
  ```java
  public class PubSubConfig {
      private int subscriberQueueSize = 1000;
      private int historySize = 100;
      private int slowConsumerThreshold = 100;
      private String slowConsumerAction = "drop";
      private int heartbeatInterval = 30;
      private int websocketIdleTimeout = 300;
      // getters and setters
  }
  ```
- [ ] Update config.yml with pubsub settings
- [ ] Inject configuration into PubSubManager

## Phase 5: Testing

### Unit Tests
- [ ] RingBuffer concurrent access tests
- [ ] SubscriberQueue overflow tests
- [ ] Topic subscriber management tests
- [ ] Message serialization tests
- [ ] PubSubManager tests

### Integration Tests
- [ ] WebSocket connection lifecycle
- [ ] Subscribe flow with last_n replay
- [ ] Unsubscribe flow
- [ ] Publish and fan-out delivery
- [ ] Multiple subscribers per topic
- [ ] Topic deletion with cleanup
- [ ] Slow consumer scenarios
- [ ] Error scenarios (invalid UUID, topic not found, etc.)

### Load Tests
- [ ] Multiple concurrent publishers
- [ ] Many subscribers (100+)
- [ ] High message throughput (1000+ msg/sec)
- [ ] Slow consumer stress test
- [ ] Memory leak checks

## Phase 6: Polish & Documentation

### Logging
- [ ] Add structured logging throughout
- [ ] Connection/disconnection events
- [ ] Message publish/delivery events
- [ ] Error scenarios with stack traces
- [ ] Slow consumer warnings

### Documentation
- [ ] Update README.md with:
  - [ ] Overview of the system
  - [ ] Setup instructions
  - [ ] API documentation with examples
  - [ ] WebSocket protocol documentation
  - [ ] Configuration options
  - [ ] Building and running
  - [ ] Docker instructions
- [ ] Add inline code documentation
- [ ] Add sequence diagrams for key flows

### Docker
- [ ] Update Dockerfile (already exists)
- [ ] Create docker-compose.yml for testing
- [ ] Add healthcheck to Docker
- [ ] Document Docker usage

### Graceful Shutdown
- [ ] Implement shutdown hook in PlivoApplication
- [ ] Stop accepting new connections
- [ ] Flush subscriber queues (best effort)
- [ ] Close all WebSocket connections gracefully
- [ ] Shutdown thread pools
- [ ] Log shutdown progress

## Phase 7: Optional Stretch Goals

- [ ] Implement basic authentication:
  - [ ] X-API-Key header validation for REST
  - [ ] Authorization header for WebSocket
- [ ] Add Dropwizard Metrics:
  - [ ] Messages published counter
  - [ ] Messages delivered counter
  - [ ] Active connections gauge
  - [ ] Publish latency histogram
  - [ ] Queue depth gauge per subscriber
- [ ] Add circuit breaker for slow consumers
- [ ] Implement message TTL (time to live)
- [ ] Add message filtering/routing (optional)

---

## Quick Start Commands

### Build
```bash
mvn clean install
```

### Run
```bash
java -jar server/target/server-1.0-SNAPSHOT.jar server server/config.yml
```

### Docker Build
```bash
docker build -t plivo-pubsub .
```

### Docker Run
```bash
docker run -p 8080:8080 -p 8081:8081 plivo-pubsub
```

### Test WebSocket (using wscat)
```bash
npm install -g wscat
wscat -c ws://localhost:8080/ws
```

### Test REST API
```bash
# Create topic
curl -X POST http://localhost:8080/topics \
  -H "Content-Type: application/json" \
  -d '{"name":"orders"}'

# List topics
curl http://localhost:8080/topics

# Health check
curl http://localhost:8081/health
```

---

## Current Status

- ✅ Project structure set up
- ✅ Multi-module Maven project created
- ✅ Dropwizard application skeleton ready
- ✅ Design document completed
- ⏳ Ready to start implementation
