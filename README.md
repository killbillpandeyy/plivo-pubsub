# Plivo In-Memory Pub/Sub System

A simplified in-memory Pub/Sub messaging system built with Dropwizard, featuring WebSocket-based real-time messaging and HTTP REST APIs for management.

## Overview

This system provides:
- **WebSocket endpoint** (`/ws`) for real-time publish/subscribe operations
- **REST APIs** for topic management and observability
- **Thread-safe** in-memory message handling
- **Backpressure management** for slow consumers
- **Message replay** capability (last N messages)
- **No external dependencies** - all state maintained in JVM memory

## Features

- ✅ Multi-module architecture (models, core, server)
- ✅ WebSocket support for real-time messaging
- ✅ Topic-based pub/sub pattern
- ✅ Fan-out message delivery to all subscribers
- ✅ Ring buffer for message history with replay
- ✅ Bounded queues with backpressure handling
- ✅ Graceful shutdown with connection cleanup
- ✅ Health checks and statistics
- ✅ Docker support

## Project Structure

```
plivo/
├── models/                 # Domain models and DTOs
│   └── src/main/java/com/plivo/models/
├── core/                   # Business logic and thread-safe operations
│   └── src/main/java/com/plivo/core/
└── server/                 # HTTP/WebSocket endpoints
    ├── src/main/java/com/plivo/server/
    └── config.yml
```

## Documentation

- [DESIGN.md](DESIGN.md) - Detailed architecture and design decisions
- [IMPLEMENTATION_CHECKLIST.md](IMPLEMENTATION_CHECKLIST.md) - Step-by-step implementation guide

## Requirements

- Java 11 or higher
- Maven 3.6+
- Docker (optional)

## Building the Application

```bash
mvn clean install
```

## Running the Application

```bash
java -jar server/target/server-1.0-SNAPSHOT.jar server server/config.yml
```

Or with full Java path (if needed):
```bash
/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home/bin/java -jar server/target/server-1.0-SNAPSHOT.jar server server/config.yml
```

## Running with Docker

Build the Docker image:

```bash
docker build -t plivo-pubsub .
```

Run the container:

```bash
docker run -p 8080:8080 -p 8081:8081 plivo-pubsub
```

## API Documentation

### WebSocket Protocol

Connect to: `ws://localhost:8080/ws`

#### Client → Server Messages

**Subscribe to topic:**
```json
{
  "type": "subscribe",
  "topic": "orders",
  "client_id": "subscriber-1",
  "last_n": 5,
  "request_id": "req-123"
}
```

**Publish message:**
```json
{
  "type": "publish",
  "topic": "orders",
  "message": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "payload": {
      "order_id": "ORD-123",
      "amount": 99.5,
      "currency": "USD"
    }
  },
  "request_id": "req-456"
}
```

**Unsubscribe:**
```json
{
  "type": "unsubscribe",
  "topic": "orders",
  "client_id": "subscriber-1",
  "request_id": "req-789"
}
```

**Ping:**
```json
{
  "type": "ping",
  "request_id": "req-ping"
}
```

#### Server → Client Messages

**Acknowledgment:**
```json
{
  "type": "ack",
  "request_id": "req-123",
  "topic": "orders",
  "status": "ok",
  "ts": "2025-08-25T10:00:00Z"
}
```

**Event (message delivery):**
```json
{
  "type": "event",
  "topic": "orders",
  "message": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "payload": {...}
  },
  "ts": "2025-08-25T10:01:00Z"
}
```

**Error:**
```json
{
  "type": "error",
  "request_id": "req-456",
  "error": {
    "code": "TOPIC_NOT_FOUND",
    "message": "Topic 'orders' does not exist"
  },
  "ts": "2025-08-25T10:02:00Z"
}
```

### REST API Endpoints

**Application:** http://localhost:8080  
**Admin:** http://localhost:8081

#### Topic Management

**Create topic:**
```bash
curl -X POST http://localhost:8080/topics \
  -H "Content-Type: application/json" \
  -d '{"name":"orders"}'

# Response: 201 Created
{
  "status": "created",
  "topic": "orders"
}
```

**Delete topic:**
```bash
curl -X DELETE http://localhost:8080/topics/orders

# Response: 200 OK
{
  "status": "deleted",
  "topic": "orders"
}
```

**List topics:**
```bash
curl http://localhost:8080/topics

# Response: 200 OK
{
  "topics": [
    {
      "name": "orders",
      "subscribers": 3
    }
  ]
}
```

#### Observability

**Health check:**
```bash
curl http://localhost:8081/health

# Response: 200 OK
{
  "uptime_sec": 123,
  "topics": 2,
  "subscribers": 4
}
```

**Statistics:**
```bash
curl http://localhost:8080/stats

# Response: 200 OK
{
  "topics": {
    "orders": {
      "messages": 42,
      "subscribers": 3
    }
  }
}
```

## Configuration

Edit `server/config.yml`:

```yaml
pubsub:
  subscriberQueueSize: 1000      # Messages per subscriber
  historySize: 100                # Ring buffer size per topic
  slowConsumerThreshold: 100      # Warn after N drops
  slowConsumerAction: "drop"      # "drop" or "disconnect"
  heartbeatInterval: 30           # Seconds
  websocketIdleTimeout: 300       # Seconds
```

## Design Decisions

### Concurrency & Thread Safety
- `ConcurrentHashMap` for topic registry and subscribers
- `ArrayBlockingQueue` for per-subscriber message queues
- Dedicated thread pool for async message publishing
- ReentrantReadWriteLock for ring buffer operations

### Backpressure Policy
When a subscriber's queue is full:
1. Remove oldest message
2. Add new message
3. Increment dropped counter
4. Log warning periodically
5. Optionally disconnect (configurable)

### Message History
- Ring buffer per topic (configurable size, default 100)
- Supports replay with `last_n` parameter on subscribe
- Thread-safe read/write operations

## Testing

### Using wscat
```bash
npm install -g wscat
wscat -c ws://localhost:8080/ws

# Send subscribe
> {"type":"subscribe","topic":"orders","client_id":"s1","last_n":0}

# Send publish
> {"type":"publish","topic":"orders","message":{"id":"550e8400-e29b-41d4-a716-446655440000","payload":{"test":"data"}}}
```

### Using curl
```bash
# Create topic first
curl -X POST http://localhost:8080/topics -H "Content-Type: application/json" -d '{"name":"test"}'

# Check health
curl http://localhost:8081/health

# List topics
curl http://localhost:8080/topics
```

## Development Timeline

- **Day 1-2**: Core infrastructure (models, data structures, PubSubManager)
- **Day 3-4**: WebSocket integration (handlers, message processing, delivery)
- **Day 5**: REST API (topic management, observability)
- **Day 6**: Testing (unit, integration, load tests)
- **Day 7**: Polish, documentation, Docker

## Implementation Status

- ✅ Project structure
- ✅ Multi-module Maven setup
- ✅ Design document
- ⏳ Core implementation (see [IMPLEMENTATION_CHECKLIST.md](IMPLEMENTATION_CHECKLIST.md))

## License

MIT
