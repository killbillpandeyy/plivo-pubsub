#!/usr/bin/env python3
"""
WebSocket Test Client for Plivo PubSub Application

This script demonstrates how to test the WebSocket endpoints.
Usage: python3 test_websocket.py
"""

import asyncio
import websockets
import json
import sys

WS_URL = "ws://localhost:8080/ws"

async def test_ping():
    """Test PING/PONG message"""
    print("\n=== Testing PING ===")
    async with websockets.connect(WS_URL) as websocket:
        ping_msg = {
            "type": "ping",
            "request_id": "ping-123"
        }
        print(f"Sending: {json.dumps(ping_msg, indent=2)}")
        await websocket.send(json.dumps(ping_msg))
        
        response = await websocket.recv()
        print(f"Received: {json.dumps(json.loads(response), indent=2)}")

async def test_subscribe_and_publish():
    
    # Create topic first via REST API
    import urllib.request
    req = urllib.request.Request(
        "http://localhost:8080/topics",
        data=json.dumps({"name": "test-topic"}).encode(),
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req) as response:
            print(f"Topic created: {response.read().decode()}")
    except Exception as e:
        print(f"Topic may already exist: {e}")
    
    # Open two WebSocket connections (subscriber and publisher)
    async with websockets.connect(WS_URL) as subscriber, \
               websockets.connect(WS_URL) as publisher:
        
        # Subscribe
        subscribe_msg = {
            "type": "subscribe",
            "request_id": "sub-123",
            "topic": "test-topic",
            "client_id": "client-1"
        }
        print(f"\nSubscriber sending: {json.dumps(subscribe_msg, indent=2)}")
        await subscriber.send(json.dumps(subscribe_msg))
        
        response = await subscriber.recv()
        print(f"Subscriber received ACK: {json.dumps(json.loads(response), indent=2)}")
        
        # Publish a message
        publish_msg = {
            "type": "publish",
            "request_id": "pub-123",
            "topic": "test-topic",
            "message": {
                "id": "msg-001",
                "payload": {"text": "Hello World!", "timestamp": 1234567890}
            }
        }
        print(f"\nPublisher sending: {json.dumps(publish_msg, indent=2)}")
        await publisher.send(json.dumps(publish_msg))
        
        pub_response = await publisher.recv()
        print(f"Publisher received ACK: {json.dumps(json.loads(pub_response), indent=2)}")
        
        # Subscriber receives EVENT
        event = await subscriber.recv()
        print(f"\nSubscriber received EVENT: {json.dumps(json.loads(event), indent=2)}")
        
        # Unsubscribe
        unsubscribe_msg = {
            "type": "unsubscribe",
            "request_id": "unsub-123",
            "topic": "test-topic",
            "client_id": "client-1"
        }
        print(f"\nSubscriber sending: {json.dumps(unsubscribe_msg, indent=2)}")
        await subscriber.send(json.dumps(unsubscribe_msg))
        
        unsub_response = await subscriber.recv()
        print(f"Subscriber received: {json.dumps(json.loads(unsub_response), indent=2)}")

async def test_last_n_messages():
    """Test subscribing with last_n parameter to get message history"""
    print("\n=== Testing SUBSCRIBE with last_n ===")
    
    # Ensure topic exists and publish some messages first
    async with websockets.connect(WS_URL) as ws:
        for i in range(5):
            publish_msg = {
                "type": "publish",
                "request_id": f"pub-{i}",
                "topic": "test-topic",
                "message": {
                    "id": f"msg-{i}",
                    "payload": {"number": i}
                }
            }
            await ws.send(json.dumps(publish_msg))
            await ws.recv()  # Wait for ACK
    
    # Subscribe with last_n=3
    async with websockets.connect(WS_URL) as ws:
        subscribe_msg = {
            "type": "subscribe",
            "request_id": "sub-history",
            "topic": "test-topic",
            "client_id": "client-history",
            "last_n": 3
        }
        print(f"Sending: {json.dumps(subscribe_msg, indent=2)}")
        await ws.send(json.dumps(subscribe_msg))
        
        # Receive ACK
        ack = await ws.recv()
        print(f"Received ACK: {json.dumps(json.loads(ack), indent=2)}")
        
        # Receive historical events
        print("\nReceiving historical messages:")
        try:
            for i in range(3):
                event = await asyncio.wait_for(ws.recv(), timeout=1.0)
                print(f"Event {i+1}: {json.dumps(json.loads(event), indent=2)}")
        except asyncio.TimeoutError:
            print("No more historical messages")

async def test_error_cases():
    """Test error scenarios"""
    print("\n=== Testing Error Cases ===")
    
    async with websockets.connect(WS_URL) as ws:
        # Try to subscribe to non-existent topic
        subscribe_msg = {
            "type": "subscribe",
            "request_id": "err-123",
            "topic": "non-existent-topic",
            "client_id": "client-err"
        }
        print(f"Sending: {json.dumps(subscribe_msg, indent=2)}")
        await ws.send(json.dumps(subscribe_msg))
        
        response = await ws.recv()
        print(f"Received ERROR: {json.dumps(json.loads(response), indent=2)}")

async def main():
    """Run all tests"""
    try:
        print("=" * 60)
        print("WebSocket Test Client for Plivo PubSub")
        print("=" * 60)
        
        await test_ping()
        await test_subscribe_and_publish()
        await test_last_n_messages()
        await test_error_cases()
        
        print("\n" + "=" * 60)
        print("All tests completed!")
        print("=" * 60)
        
    except Exception as e:
        print(f"\nError: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    asyncio.run(main())
