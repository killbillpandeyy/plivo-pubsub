# In-Memory Pub/Sub System - Detailed Design Approach

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Module Structure](#module-structure)
3. [Core Components Design](#core-components-design)
4. [Implementation Steps](#implementation-steps)
5. [Concurrency & Thread Safety](#concurrency--thread-safety)
6. [Backpressure Strategy](#backpressure-strategy)
7. [WebSocket Protocol Implementation](#websocket-protocol-implementation)
8. [REST API Implementation](#rest-api-implementation)
9. [Testing Strategy](#testing-strategy)
10. [Deployment & Operations](#deployment--operations)

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Dropwizard Application                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌────────────────┐        ┌─────────────────┐             │
│  │  HTTP/REST     │        │   WebSocket     │             │
│  │   Endpoint     │        │    Endpoint     │             │
│  │   (/topics)    │        │      (/ws)      │             │
│  └───────┬────────┘        └────────┬────────┘             │
│          │                          │                        │
│          └──────────┬───────────────┘                        │
│                     │                                        │
│          ┌──────────▼──────────┐                            │
│          │   PubSubManager     │                            │
│          │  (Thread-Safe)      │                            │
│          └──────────┬──────────┘                            │
│                     │                                        │
│          ┌──────────▼──────────┐                            │
│          │    Topic Registry    │                            │
│          │ ConcurrentHashMap    │                            │
│          └──────────┬──────────┘                            │
│                     │                                        │
│       ┌─────────────┼─────────────┐                         │
│       │             │             │                         │
│  ┌────▼────┐   ┌───▼────┐   ┌───▼────┐                    │
│  │ Topic A │   │Topic B │   │Topic C │                    │
│  │         │   │        │   │        │                    │
│  │ ┌─────┐ │   │┌─────┐ │   │┌─────┐ │                    │
│  │ │Sub 1│ │   ││Sub 3│ │   ││Sub 5│ │                    │
│  │ │Sub 2│ │   ││Sub 4│ │   ││Sub 6│ │                    │
│  │ └─────┘ │   │└─────┘ │   │└─────┘ │                    │
│  │         │   │        │   │        │                    │
│  │ Ring    │   │ Ring   │   │ Ring   │                    │
│  │ Buffer  │   │ Buffer │   │ Buffer │                    │
│  └─────────┘   └────────┘   └────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **In-Memory Storage**: All state maintained in JVM memory using concurrent data structures
2. **Thread Safety**: ConcurrentHashMap for topics, synchronized blocks for critical sections
3. **WebSocket**: Jetty WebSocket API (comes with Dropwizard)
4. **Backpressure**: Bounded ArrayBlockingQueue per subscriber (configurable size)
5. **Message History**: Ring buffer (circular array) per topic for replay functionality
6. **Graceful Shutdown**: Lifecycle hooks to flush queues and close connections

---

## Module Structure

### 1. Models Module (`models/`)

Contains domain entities and data transfer objects:

```
models/src/main/java/com/plivo/models/
├── Message.java              # Published message with ID and payload
├── MessageEnvelope.java      # Message wrapper with timestamp
├── Subscription.java         # Subscriber metadata
├── Topic.java               # Topic with subscribers and ring buffer
├── ws/
│   ├── ClientMessage.java   # Incoming WebSocket messages
│   ├── ServerMessage.java   # Outgoing WebSocket messages
│   ├── MessageType.java     # Enum: subscribe, publish, etc.
│   └── ErrorCode.java       # Enum: BAD_REQUEST, TOPIC_NOT_FOUND, etc.
└── http/
    ├── CreateTopicRequest.java
    ├── CreateTopicResponse.java
    ├── TopicInfo.java
    ├── HealthResponse.java
    └── StatsResponse.java
```

### 2. Core Module (`core/`)

Business logic and thread-safe operations:

```
core/src/main/java/com/plivo/core/
├── pubsub/
│   ├── PubSubManager.java           # Main orchestrator (singleton)
│   ├── TopicRegistry.java           # Thread-safe topic management
│   ├── SubscriptionManager.java     # Per-topic subscriber handling
│   ├── MessagePublisher.java        # Publishing logic
│   └── MessageRouter.java           # Fan-out delivery
├── storage/
│   ├── RingBuffer.java              # Circular buffer for message history
│   └── SubscriberQueue.java         # Bounded queue with backpressure
├── websocket/
│   ├── WebSocketHandler.java        # Connection lifecycle
│   ├── MessageProcessor.java        # Process client messages
│   └── ConnectionRegistry.java      # Track active WS connections
├── health/
│   └── PubSubHealthCheck.java       # Dropwizard health check
└── exceptions/
    ├── TopicNotFoundException.java
    ├── SlowConsumerException.java
    └── ValidationException.java
```

### 3. Server Module (`server/`)

HTTP/WebSocket endpoints and configuration:

```
server/src/main/java/com/plivo/server/
├── PlivoApplication.java            # Main Dropwizard app
├── PlivoConfiguration.java          # App config (queue sizes, etc.)
├── resources/
│   ├── TopicResource.java          # REST: POST/DELETE/GET /topics
│   ├── HealthResource.java         # REST: GET /health
│   └── StatsResource.java          # REST: GET /stats
├── websocket/
│   ├── PubSubWebSocketServlet.java # WebSocket servlet
│   └── PubSubWebSocket.java        # WebSocket endpoint impl
└── health/
    └── ApplicationHealthCheck.java
```

---

## Core Components Design

### 1. Topic Class

```java
public class Topic {
    private final String name;
    private final RingBuffer<MessageEnvelope> messageHistory;
    private final ConcurrentHashMap<String, Subscription> subscribers;
    private final AtomicLong messageCount;
    private final ReentrantReadWriteLock lock;
    
    public Topic(String name, int historySize) {
        this.name = name;
        this.messageHistory = new RingBuffer<>(historySize);
        this.subscribers = new ConcurrentHashMap<>();
        this.messageCount = new AtomicLong(0);
        this.lock = new ReentrantReadWriteLock();
    }
    
    public void addSubscriber(Subscription sub) { /* ... */ }
    public void removeSubscriber(String clientId) { /* ... */ }
    public void publish(MessageEnvelope message) { /* ... */ }
    public List<MessageEnvelope> getLastN(int n) { /* ... */ }
}
```

### 2. Subscription Class

```java
public class Subscription {
    private final String clientId;
    private final String topicName;
    private final Session session; // WebSocket session
    private final SubscriberQueue queue;
    private final ScheduledExecutorService deliveryExecutor;
    
    public Subscription(String clientId, String topicName, 
                       Session session, int queueSize) {
        this.clientId = clientId;
        this.topicName = topicName;
        this.session = session;
        this.queue = new SubscriberQueue(queueSize);
        this.deliveryExecutor = Executors.newSingleThreadScheduledExecutor();
        startDeliveryLoop();
    }
    
    public boolean enqueue(MessageEnvelope msg) { /* ... */ }
    private void startDeliveryLoop() { /* ... */ }
    public void close() { /* ... */ }
}
```

### 3. PubSubManager (Singleton)

```java
@Singleton
public class PubSubManager {
    private final ConcurrentHashMap<String, Topic> topics;
    private final ConnectionRegistry connectionRegistry;
    private final ExecutorService publisherPool;
    private final AtomicBoolean isShuttingDown;
    private final int queueSize;
    private final int historySize;
    
    public PubSubManager(int queueSize, int historySize) {
        this.topics = new ConcurrentHashMap<>();
        this.connectionRegistry = new ConnectionRegistry();
        this.publisherPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
        );
        this.isShuttingDown = new AtomicBoolean(false);
        this.queueSize = queueSize;
        this.historySize = historySize;
    }
    
    // Topic management
    public void createTopic(String name) { /* ... */ }
    public void deleteTopic(String name) { /* ... */ }
    public List<Topic> getAllTopics() { /* ... */ }
    
    // Subscription management
    public void subscribe(String topicName, String clientId, 
                         Session session, int lastN) { /* ... */ }
    public void unsubscribe(String topicName, String clientId) { /* ... */ }
    
    // Publishing
    public void publish(String topicName, Message message) { /* ... */ }
    
    // Lifecycle
    public void shutdown() { /* ... */ }
}
```

### 4. RingBuffer (Thread-Safe)

```java
public class RingBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private final AtomicInteger writePos;
    private final AtomicInteger size;
    private final ReentrantReadWriteLock lock;
    
    public RingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
        this.writePos = new AtomicInteger(0);
        this.size = new AtomicInteger(0);
        this.lock = new ReentrantReadWriteLock();
    }
    
    public void add(T item) {
        lock.writeLock().lock();
        try {
            int pos = writePos.getAndUpdate(i -> (i + 1) % capacity);
            buffer[pos] = item;
            size.updateAndGet(s -> Math.min(s + 1, capacity));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public List<T> getLastN(int n) { /* ... */ }
}
```

### 5. SubscriberQueue

```java
public class SubscriberQueue {
    private final ArrayBlockingQueue<MessageEnvelope> queue;
    private final AtomicLong droppedMessages;
    
    public SubscriberQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.droppedMessages = new AtomicLong(0);
    }
    
    public boolean offer(MessageEnvelope msg) {
        boolean added = queue.offer(msg);
        if (!added) {
            droppedMessages.incrementAndGet();
            // Optionally: remove oldest and add new
            queue.poll();
            queue.offer(msg);
        }
        return added;
    }
    
    public MessageEnvelope poll() {
        return queue.poll();
    }
}
```

---

## Implementation Steps

### Phase 1: Core Infrastructure (Day 1-2)

1. **Setup Models Module**
   - Define all message classes with Jackson annotations
   - Implement validation (UUID format, required fields)
   - Add unit tests for serialization/deserialization

2. **Implement Core Data Structures**
   - RingBuffer with thread-safe operations
   - SubscriberQueue with backpressure handling
   - Topic class with subscriber management
   - Unit tests for concurrent access

3. **Build PubSubManager**
   - Topic lifecycle operations
   - Thread-safe registry
   - Basic health metrics collection

### Phase 2: WebSocket Integration (Day 3-4)

1. **Configure Jetty WebSocket**
   - Add dependencies to server/pom.xml
   - Create WebSocket servlet and register in Dropwizard
   - Configure connection timeouts and buffer sizes

2. **Implement WebSocket Handler**
   - Connection establishment and tracking
   - Message parsing and validation
   - Error handling and error responses

3. **Implement Message Processors**
   - Subscribe handler (with last_n replay)
   - Unsubscribe handler
   - Publish handler (fan-out to all subscribers)
   - Ping/Pong handler

4. **Subscription Delivery Loop**
   - Async message delivery per subscriber
   - Handle slow consumers (drop or disconnect)
   - Send error messages on failures

### Phase 3: REST API (Day 5)

1. **Topic Management Endpoints**
   - POST /topics - Create topic
   - DELETE /topics/{name} - Delete topic (disconnect subscribers)
   - GET /topics - List all topics with subscriber counts

2. **Observability Endpoints**
   - GET /health - Uptime, topic count, subscriber count
   - GET /stats - Per-topic statistics

3. **Add JAX-RS Exception Mappers**
   - TopicNotFoundException → 404
   - ValidationException → 400
   - Generic Exception → 500

### Phase 4: Testing (Day 6)

1. **Unit Tests**
   - RingBuffer concurrent access
   - SubscriberQueue overflow behavior
   - Topic subscriber management
   - Message serialization

2. **Integration Tests**
   - WebSocket connect/disconnect
   - Subscribe/unsubscribe flows
   - Publish and receive messages
   - Multiple subscribers per topic

3. **Load Tests**
   - Multiple concurrent publishers
   - Many subscribers (100+)
   - High message throughput
   - Slow consumer scenarios

### Phase 5: Polish & Documentation (Day 7)

1. **Configuration**
   - Externalize queue sizes, history size
   - WebSocket timeouts
   - Thread pool sizes

2. **Logging**
   - Structured logging (Logback)
   - Log levels for debugging
   - Connection/disconnection events
   - Error scenarios

3. **Documentation**
   - README with setup instructions
   - API documentation with examples
   - Architecture diagrams
   - Design decisions and trade-offs

4. **Docker**
   - Update Dockerfile
   - Docker Compose for easy testing
   - Health check integration

---

## Concurrency & Thread Safety

### Thread Safety Guarantees

1. **Topic Registry**: `ConcurrentHashMap<String, Topic>`
   - Concurrent reads/writes for topic creation/deletion
   - No external synchronization needed

2. **Subscriber Management**: `ConcurrentHashMap<String, Subscription>`
   - Per-topic subscriber map
   - Add/remove operations are atomic

3. **Message Publishing**:
   - Use dedicated thread pool (ExecutorService)
   - Each publish spawns async task for fan-out
   - No blocking on slow subscribers

4. **Ring Buffer**:
   - ReentrantReadWriteLock for read/write separation
   - Multiple readers, single writer pattern
   - AtomicInteger for positions

5. **Subscriber Queue**:
   - ArrayBlockingQueue (thread-safe)
   - Single consumer thread per subscriber
   - Producer (publisher) never blocks

### Synchronization Points

```java
// Publishing workflow
public void publish(String topicName, Message message) {
    Topic topic = topics.get(topicName);
    if (topic == null) {
        throw new TopicNotFoundException(topicName);
    }
    
    MessageEnvelope envelope = new MessageEnvelope(
        message, Instant.now()
    );
    
    // Add to history (write lock)
    topic.addToHistory(envelope);
    
    // Fan-out (no lock needed, fire-and-forget)
    publisherPool.submit(() -> {
        topic.getSubscribers().values().forEach(sub -> {
            boolean enqueued = sub.enqueue(envelope);
            if (!enqueued) {
                handleSlowConsumer(sub);
            }
        });
    });
}
```

---

## Backpressure Strategy

### Chosen Policy: **Drop Oldest + Warning**

When a subscriber's queue is full:
1. Remove oldest message from queue
2. Add new message
3. Increment dropped message counter
4. Log warning (every N drops to avoid log spam)
5. Send INFO message to client periodically

### Configuration

```yaml
pubsub:
  subscriberQueueSize: 1000      # Messages per subscriber
  historySize: 100                # Ring buffer size per topic
  slowConsumerThreshold: 100      # Warn after N drops
  slowConsumerAction: "drop"      # "drop" or "disconnect"
```

### Alternative: Disconnect on Overflow

```java
if (!sub.enqueue(envelope)) {
    sendError(sub.getSession(), ErrorCode.SLOW_CONSUMER,
             "Queue overflow, disconnecting");
    sub.close();
    topic.removeSubscriber(sub.getClientId());
}
```

---

## WebSocket Protocol Implementation

### Message Routing

```java
@OnWebSocketMessage
public void onMessage(Session session, String message) {
    try {
        ClientMessage clientMsg = objectMapper.readValue(
            message, ClientMessage.class
        );
        
        // Validate
        validateMessage(clientMsg);
        
        // Route based on type
        switch (clientMsg.getType()) {
            case SUBSCRIBE:
                handleSubscribe(session, clientMsg);
                break;
            case UNSUBSCRIBE:
                handleUnsubscribe(session, clientMsg);
                break;
            case PUBLISH:
                handlePublish(session, clientMsg);
                break;
            case PING:
                handlePing(session, clientMsg);
                break;
        }
    } catch (ValidationException e) {
        sendError(session, ErrorCode.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
        sendError(session, ErrorCode.INTERNAL, 
                 "Unexpected error");
        log.error("Error processing message", e);
    }
}
```

### Subscribe Flow

```java
private void handleSubscribe(Session session, ClientMessage msg) {
    String topic = msg.getTopic();
    String clientId = msg.getClientId();
    int lastN = msg.getLastN() != null ? msg.getLastN() : 0;
    
    // Subscribe
    pubSubManager.subscribe(topic, clientId, session, lastN);
    
    // Send ACK
    sendAck(session, msg.getRequestId(), topic);
    
    // Replay last N messages if requested
    if (lastN > 0) {
        List<MessageEnvelope> history = 
            pubSubManager.getHistory(topic, lastN);
        history.forEach(envelope -> 
            sendEvent(session, topic, envelope)
        );
    }
}
```

### Publish Flow

```java
private void handlePublish(Session session, ClientMessage msg) {
    String topic = msg.getTopic();
    Message message = msg.getMessage();
    
    // Validate message
    if (message.getId() == null || !isValidUUID(message.getId())) {
        throw new ValidationException(
            "message.id must be a valid UUID"
        );
    }
    
    // Publish (async)
    pubSubManager.publish(topic, message);
    
    // Send ACK (immediate)
    sendAck(session, msg.getRequestId(), topic);
}
```

---

## REST API Implementation

### Topic Resource

```java
@Path("/topics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TopicResource {
    
    private final PubSubManager pubSubManager;
    
    @POST
    public Response createTopic(@Valid CreateTopicRequest req) {
        try {
            pubSubManager.createTopic(req.getName());
            return Response.status(201).entity(
                new CreateTopicResponse("created", req.getName())
            ).build();
        } catch (TopicAlreadyExistsException e) {
            return Response.status(409).entity(
                Map.of("error", "Topic already exists")
            ).build();
        }
    }
    
    @DELETE
    @Path("/{name}")
    public Response deleteTopic(@PathParam("name") String name) {
        pubSubManager.deleteTopic(name);
        // Broadcast INFO to all subscribers
        broadcastTopicDeleted(name);
        return Response.ok(
            Map.of("status", "deleted", "topic", name)
        ).build();
    }
    
    @GET
    public Response listTopics() {
        List<TopicInfo> topics = pubSubManager.getAllTopics()
            .stream()
            .map(t -> new TopicInfo(
                t.getName(), 
                t.getSubscriberCount()
            ))
            .collect(Collectors.toList());
        return Response.ok(Map.of("topics", topics)).build();
    }
}
```

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testRingBufferConcurrency() throws Exception {
    RingBuffer<String> buffer = new RingBuffer<>(10);
    int threads = 10;
    int itemsPerThread = 100;
    
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    List<Future<?>> futures = new ArrayList<>();
    
    for (int i = 0; i < threads; i++) {
        int threadId = i;
        futures.add(executor.submit(() -> {
            for (int j = 0; j < itemsPerThread; j++) {
                buffer.add("T" + threadId + "-" + j);
            }
        }));
    }
    
    futures.forEach(f -> f.get());
    
    List<String> last10 = buffer.getLastN(10);
    assertEquals(10, last10.size());
}
```

### Integration Tests

```java
@Test
public void testPubSubFlow() {
    // Create topic
    Response resp = createTopic("orders");
    assertEquals(201, resp.getStatus());
    
    // Connect WebSocket subscriber
    WebSocketClient ws1 = connectWebSocket();
    ws1.send(subscribeMessage("orders", "sub1", 0));
    assertAck(ws1);
    
    // Publish message
    WebSocketClient ws2 = connectWebSocket();
    ws2.send(publishMessage("orders", testMessage()));
    assertAck(ws2);
    
    // Verify subscriber received it
    ServerMessage event = ws1.receive();
    assertEquals("event", event.getType());
    assertEquals("orders", event.getTopic());
}
```

---

## Deployment & Operations

### Configuration (config.yml)

```yaml
applicationName: Plivo PubSub
version: 1.0.0

server:
  applicationConnectors:
    - type: http
      port: 8080
  adminConnectors:
    - type: http
      port: 8081

pubsub:
  subscriberQueueSize: 1000
  historySize: 100
  slowConsumerThreshold: 100
  slowConsumerAction: drop
  heartbeatInterval: 30
  websocketIdleTimeout: 300

logging:
  level: INFO
  appenders:
    - type: console
```

### Dockerfile

```dockerfile
FROM maven:3.8-openjdk-11 AS builder
WORKDIR /app
COPY pom.xml .
COPY models/pom.xml models/
COPY core/pom.xml core/
COPY server/pom.xml server/
RUN mvn dependency:go-offline -B
COPY models/src models/src
COPY core/src core/src
COPY server/src server/src
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder /app/server/target/server-1.0-SNAPSHOT.jar app.jar
COPY server/config.yml config.yml
EXPOSE 8080 8081
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8081/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar", "server", "config.yml"]
```

### Monitoring Metrics

Key metrics to expose (via Dropwizard Metrics):
- Active WebSocket connections
- Messages published per second
- Messages delivered per second
- Subscriber queue depths
- Dropped message count per subscriber
- Topic count
- Publish latency (p50, p95, p99)

---

## Summary

This design provides:
- ✅ Thread-safe in-memory Pub/Sub
- ✅ WebSocket for real-time messaging
- ✅ REST API for management
- ✅ Backpressure handling
- ✅ Message replay (last_n)
- ✅ Graceful shutdown
- ✅ Comprehensive testing
- ✅ Production-ready Docker deployment

**Estimated Timeline**: 7 days for full implementation with testing and documentation.
