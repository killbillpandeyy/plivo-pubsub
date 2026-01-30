# Plivo Pub/Sub System

In-memory Pub/Sub with WebSocket real-time messaging, REST APIs, and backpressure handling.

## Quick Start

```bash
# Build
mvn clean install

# Run
java -jar server/target/server-1.0-SNAPSHOT.jar server server/config/config.yml
```

Server runs on `http://localhost:8080` (application) and `http://localhost:8081` (admin).

## REST API Testing

```bash
# Health check
curl http://localhost:8080/health

# Create topic
curl -X POST http://localhost:8080/topics \
  -H "Content-Type: application/json" \
  -d '{"name": "orders"}'

# List topics
curl http://localhost:8080/topics

# Get stats
curl http://localhost:8080/stats

# Delete topic
curl -X DELETE http://localhost:8080/topics/orders
```

## WebSocket Testing

Connect to `ws://localhost:8080/ws` and send JSON messages:

**Subscribe:**
```json
{"type": "subscribe", "topic": "orders", "client_id": "sub1", "last_n": 5}
```

**Publish:**
```json
{"type": "publish", "topic": "orders", "message": {"order_id": "123", "status": "confirmed"}}
```

**Unsubscribe:**
```json
{"type": "unsubscribe", "topic": "orders", "client_id": "sub1"}
```

**Ping:**
```json
{"type": "ping"}
```

## Integration Tests

```bash
# Install Python dependencies
pip3 install websockets requests

# Run tests
python3 integration_test.py

# Run backpressure test
python3 test_backpressure.py
```

## Design Patterns

### Visitor Pattern
WebSocket messages use the Visitor pattern for type-safe message handling. Each message type (`SubscribeRequest`, `PublishRequest`, etc.) accepts a `ClientMessageVisitor` that processes it. This eliminates type-checking and provides compile-time safety.

**Location:** `WebSocketMessageHandler` implements `ClientMessageVisitor` with `visit()` methods for each message type.

### Backpressure Policy
Each topic has a bounded queue (default: 1000 messages). When full:
1. New publishes return `CONSUMER_IS_SLOW` error
2. Topic stops accepting messages (graceful shutdown)
3. Publisher receives queue stats (size/capacity)
4. Admin can drain or resume topic manually

**Location:** `Topic.offerMessage()` uses `LinkedBlockingQueue` with non-blocking offer/poll operations.

## Project Structure

```
plivo/
├── models/          # Domain models, WebSocket DTOs
├── core/            # Business logic, repositories, services
├── server/          # REST/WebSocket endpoints, application entry
└── /server/config/   # Configuration files
```

## Technologies

- Dropwizard 4.0.0
- Jetty WebSocket 11.0.14
- Java 11
- Maven multi-module

## License

MIT
