# Visitor Pattern Refactoring

## Overview
Refactored the WebSocket message handling from a switch statement to the Visitor design pattern for better extensibility and separation of concerns.

## Changes Made

### 1. Created Visitor Interface
**File**: `models/src/main/java/com/plivo/models/ws/ClientMessageVisitor.java`
- Defines visit methods for each message type:
  - `visit(SubscribeRequest)`
  - `visit(UnsubscribeRequest)`
  - `visit(PublishRequest)`
  - `visit(PingRequest)`

### 2. Updated ClientMessage Base Class
**File**: `models/src/main/java/com/plivo/models/ws/ClientMessage.java`
- Added abstract method: `accept(ClientMessageVisitor visitor)`
- Enforces all subclasses to implement visitor acceptance

### 3. Implemented accept() in All Message Classes
**Files Updated**:
- `SubscribeRequest.java` - calls `visitor.visit(this)`
- `UnsubscribeRequest.java` - calls `visitor.visit(this)`
- `PublishRequest.java` - calls `visitor.visit(this)`
- `PingRequest.java` - calls `visitor.visit(this)`

### 4. Created Concrete Visitor Implementation
**File**: `server/src/main/java/com/plivo/server/websocket/WebSocketMessageHandler.java`
- Implements `ClientMessageVisitor`
- Encapsulates all message handling logic
- Contains methods:
  - `visit(SubscribeRequest)` - handles subscriptions with history
  - `visit(UnsubscribeRequest)` - handles unsubscriptions
  - `visit(PublishRequest)` - handles publishing and broadcasting
  - `visit(PingRequest)` - handles ping/pong

### 5. Simplified PubSubWebSocket
**File**: `server/src/main/java/com/plivo/server/websocket/PubSubWebSocket.java`
- **Before**: 200+ line switch statement with inline handlers
- **After**: Clean delegation to visitor pattern:
  ```java
  ClientMessageVisitor visitor = new WebSocketMessageHandler(
      session, pubSubService, objectMapper, sessionToClientId
  );
  clientMsg.accept(visitor);
  ```

## Benefits

1. **Open/Closed Principle**: Easy to add new message types without modifying existing code
2. **Single Responsibility**: Each visitor method handles one message type
3. **Type Safety**: Compile-time checking ensures all message types are handled
4. **Maintainability**: Message handling logic is centralized in one class
5. **Testability**: Visitor can be easily mocked/tested independently

## Testing
✅ All WebSocket tests pass:
- PING/PONG
- Subscribe/Unsubscribe
- Publish with broadcasting
- Message history (last_n)
- Multiple subscribers
- Error handling

## Architecture

```
ClientMessage (abstract)
├── accept(visitor) [abstract]
├── SubscribeRequest.accept() → visitor.visit(this)
├── UnsubscribeRequest.accept() → visitor.visit(this)
├── PublishRequest.accept() → visitor.visit(this)
└── PingRequest.accept() → visitor.visit(this)

ClientMessageVisitor (interface)
├── visit(SubscribeRequest)
├── visit(UnsubscribeRequest)
├── visit(PublishRequest)
└── visit(PingRequest)

WebSocketMessageHandler (concrete visitor)
└── Implements all visit methods with actual logic

PubSubWebSocket
└── Creates visitor and delegates: clientMsg.accept(visitor)
```

## Future Extensibility
To add a new message type (e.g., `AckRequest`):
1. Add `visit(AckRequest)` to `ClientMessageVisitor` interface
2. Create `AckRequest` class extending `ClientMessage`
3. Implement `accept()` in `AckRequest`
4. Implement `visit(AckRequest)` in `WebSocketMessageHandler`
5. Compiler ensures nothing is missed!
