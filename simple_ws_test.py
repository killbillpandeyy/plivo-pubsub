#!/usr/bin/env python3
"""
Simple WebSocket test to verify the system works
"""

import asyncio
import websockets
import json

WS_URL = "ws://localhost:8080/ws"

async def test_basic():
    """Test basic subscribe and publish flow"""
    print("Testing basic pub/sub...")
    
    try:
        # Create connections manually, don't use async with to avoid premature closure
        ws_sub = await websockets.connect(WS_URL)
        ws_pub = await websockets.connect(WS_URL)
        
        try:
            # Subscribe
            await ws_sub.send(json.dumps({
                "type": "subscribe",
                "topic": "orders",
                "client_id": "sub1"
            }))
            
            # Wait for ACK
            ack = await asyncio.wait_for(ws_sub.recv(), timeout=2)
            print(f"Subscribe ACK: {ack}")
            ack_data = json.loads(ack)
            
            # Publish
            await ws_pub.send(json.dumps({
                "type": "publish",
                "topic": "orders",
                "message": {"id": "test1", "payload": "Hello!"}
            }))
            
            # Get publish ACK
            pub_ack = await asyncio.wait_for(ws_pub.recv(), timeout=2)
            print(f"Publish ACK: {pub_ack}")
            
            # Get event on subscriber
            event = await asyncio.wait_for(ws_sub.recv(), timeout=2)
            print(f"Event received: {event}")
            event_data = json.loads(event)
            
            if event_data["type"] == "event" and event_data["message"]["payload"] == "Hello!":
                print("✅ SUCCESS: Message delivered correctly!")
            else:
                print(f"❌ FAILED: Unexpected event: {event_data}")
                
        finally:
            await ws_sub.close()
            await ws_pub.close()
            
    except Exception as e:
        print(f"❌ FAILED: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test_basic())
