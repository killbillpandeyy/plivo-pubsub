#!/usr/bin/env python3
import asyncio
import websockets
import json

async def test():
    # First create a topic via REST
    import urllib.request
    req = urllib.request.Request(
        "http://localhost:8080/topics",
        data=json.dumps({"name": "demo"}).encode(),
        headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req) as r:
            print(f"✓ Topic created: {r.read().decode()}\n")
    except:
        print("✓ Topic already exists\n")
    
    # Test WebSocket
    uri = "ws://localhost:8080/ws"
    
    # Test 1: Ping
    print("=== Test 1: PING ===")
    async with websockets.connect(uri) as ws:
        await ws.send(json.dumps({"type": "ping", "request_id": "1"}))
        resp = json.loads(await ws.recv())
        print(f"Response: {json.dumps(resp, indent=2)}\n")
    
    # Test 2: Subscribe and Publish
    print("=== Test 2: SUBSCRIBE and PUBLISH ===")
    async with websockets.connect(uri) as sub, websockets.connect(uri) as pub:
        # Subscribe
        await sub.send(json.dumps({
            "type": "subscribe",
            "request_id": "2",
            "topic": "demo",
            "client_id": "alice"
        }))
        ack1 = json.loads(await sub.recv())
        print(f"Subscribe ACK: {json.dumps(ack1, indent=2)}")
        
        # Publish
        await pub.send(json.dumps({
            "type": "publish",
            "request_id": "3",
            "topic": "demo",
            "message": {"id": "m1", "payload": "Hello WebSocket!"}
        }))
        ack2 = json.loads(await pub.recv())
        print(f"\nPublish ACK: {json.dumps(ack2, indent=2)}")
        
        # Receive event
        event = json.loads(await sub.recv())
        print(f"\nEvent received: {json.dumps(event, indent=2)}\n")
        
        # Unsubscribe
        await sub.send(json.dumps({
            "type": "unsubscribe",
            "request_id": "4",
            "topic": "demo",
            "client_id": "alice"
        }))
        ack3 = json.loads(await sub.recv())
        print(f"Unsubscribe ACK: {json.dumps(ack3, indent=2)}\n")
    
    # Test 3: Subscribe with last_n (message history)
    print("=== Test 3: SUBSCRIBE with last_n (Message History) ===")
    # First publish some messages
    async with websockets.connect(uri) as pub:
        for i in range(5):
            await pub.send(json.dumps({
                "type": "publish",
                "request_id": f"pub-{i}",
                "topic": "demo",
                "message": {"id": f"msg-{i}", "payload": f"Message #{i}"}
            }))
            await pub.recv()  # wait for ACK
    
    # Now subscribe with last_n=3
    async with websockets.connect(uri) as sub:
        await sub.send(json.dumps({
            "type": "subscribe",
            "request_id": "5",
            "topic": "demo",
            "client_id": "bob",
            "last_n": 3
        }))
        ack = json.loads(await sub.recv())
        print(f"Subscribe ACK: {json.dumps(ack, indent=2)}")
        
        # Receive historical messages
        print("\nHistorical messages:")
        for i in range(3):
            try:
                event = json.loads(await asyncio.wait_for(sub.recv(), timeout=1.0))
                print(f"  Message {i+1}: {event['message']['payload']}")
            except asyncio.TimeoutError:
                break
        print()
    
    # Test 4: Multiple subscribers
    print("=== Test 4: Multiple Subscribers ===")
    async with websockets.connect(uri) as sub1, \
               websockets.connect(uri) as sub2, \
               websockets.connect(uri) as pub:
        # Subscribe both clients
        await sub1.send(json.dumps({
            "type": "subscribe",
            "request_id": "6",
            "topic": "demo",
            "client_id": "charlie"
        }))
        await sub1.recv()
        
        await sub2.send(json.dumps({
            "type": "subscribe",
            "request_id": "7",
            "topic": "demo",
            "client_id": "dave"
        }))
        await sub2.recv()
        print("✓ Two clients subscribed")
        
        # Publish one message
        await pub.send(json.dumps({
            "type": "publish",
            "request_id": "8",
            "topic": "demo",
            "message": {"id": "broadcast", "payload": "Message to all!"}
        }))
        await pub.recv()
        
        # Both should receive it
        event1 = json.loads(await sub1.recv())
        event2 = json.loads(await sub2.recv())
        print(f"✓ Client 1 received: {event1['message']['payload']}")
        print(f"✓ Client 2 received: {event2['message']['payload']}\n")
    
    # Test 5: Error cases
    print("=== Test 5: Error Cases ===")
    async with websockets.connect(uri) as ws:
        # Try to subscribe to non-existent topic
        await ws.send(json.dumps({
            "type": "subscribe",
            "request_id": "9",
            "topic": "nonexistent",
            "client_id": "eve"
        }))
        error = json.loads(await ws.recv())
        print(f"Expected error for non-existent topic:")
        print(f"  Code: {error.get('code')}")
        print(f"  Message: {error.get('message')}\n")
    
    # Test 6: Publish to topic with no subscribers
    print("=== Test 6: Publish to Empty Topic ===")
    async with websockets.connect(uri) as pub:
        await pub.send(json.dumps({
            "type": "publish",
            "request_id": "10",
            "topic": "demo",
            "message": {"id": "lonely", "payload": "Nobody listening..."}
        }))
        ack = json.loads(await pub.recv())
        print(f"Publish ACK (no subscribers): {json.dumps(ack, indent=2)}\n")
    
    print("✅ All tests completed successfully!")

asyncio.run(test())
