#!/usr/bin/env python3
"""
Test script to verify backpressure handling and CONSUMER_IS_SLOW error
"""

import asyncio
import websockets
import json
import requests

BASE_URL = "http://localhost:8080"
WS_URL = "ws://localhost:8080/ws"

async def test_backpressure():
    """Test that queue overflow triggers CONSUMER_IS_SLOW error"""
    print("=" * 80)
    print("BACKPRESSURE TEST: Testing Queue Overflow Handling")
    print("=" * 80)
    
    # Create topic
    print("\n1. Creating test topic 'backpressure-test'...")
    try:
        response = requests.post(
            f"{BASE_URL}/topics",
            json={"name": "backpressure-test"},
            headers={"Content-Type": "application/json"}
        )
        if response.status_code == 201:
            print("✓ Topic created successfully")
        else:
            print(f"✗ Failed to create topic: {response.status_code}")
            return
    except Exception as e:
        print(f"✗ Error creating topic: {e}")
        return
    
    # Connect publisher
    print("\n2. Connecting publisher...")
    publisher = await websockets.connect(WS_URL)
    print("✓ Publisher connected")
    
    # Publish many messages to fill the queue (default capacity is 1000)
    print("\n3. Publishing 1100 messages to overflow the queue...")
    overflow_detected = False
    consumer_slow_error = None
    successful_publishes = 0
    
    for i in range(1, 1101):
        try:
            await publisher.send(json.dumps({
                "type": "publish",
                "topic": "backpressure-test",
                "message": {
                    "id": f"msg_{i}",
                    "payload": f"Test message {i}"
                }
            }))
            
            # Receive response
            response = await asyncio.wait_for(publisher.recv(), timeout=2)
            response_data = json.loads(response)
            
            if response_data.get("type") in ["ack", "ACK"]:
                successful_publishes += 1
                if i % 100 == 0:
                    print(f"  ✓ Published {i} messages successfully")
            elif response_data.get("type") in ["error", "ERROR"]:
                error_code = response_data.get("code")
                if error_code == "CONSUMER_IS_SLOW":
                    overflow_detected = True
                    consumer_slow_error = response_data
                    print(f"\n  ⚠ Queue overflow detected at message {i}!")
                    print(f"  Error: {response_data.get('message')}")
                    print(f"  Error Code: {error_code}")
                    break
                else:
                    print(f"  ✗ Unexpected error: {response_data}")
                    break
        except asyncio.TimeoutError:
            print(f"  ✗ Timeout waiting for response at message {i}")
            break
        except Exception as e:
            print(f"  ✗ Error publishing message {i}: {e}")
            break
    
    print(f"\n4. Test Results:")
    print(f"  - Successfully published: {successful_publishes} messages")
    print(f"  - Queue overflow detected: {'YES' if overflow_detected else 'NO'}")
    
    if overflow_detected:
        print(f"\n  ✓ CONSUMER_IS_SLOW error triggered correctly!")
        print(f"  - Error Code: {consumer_slow_error.get('code')}")
        print(f"  - Error Message: {consumer_slow_error.get('message')}")
        
        # Try to publish one more message to verify topic stopped accepting
        print(f"\n5. Verifying topic stopped accepting messages...")
        await publisher.send(json.dumps({
            "type": "publish",
            "topic": "backpressure-test",
            "message": {
                "id": "verification_msg",
                "payload": "This should fail"
            }
        }))
        
        response = await asyncio.wait_for(publisher.recv(), timeout=2)
        response_data = json.loads(response)
        
        if response_data.get("code") == "CONSUMER_IS_SLOW":
            print("  ✓ Topic correctly rejected new message (still in shutdown)")
        else:
            print(f"  ? Unexpected response: {response_data}")
    else:
        print(f"\n  ✗ Queue overflow NOT detected (expected around message 1000)")
        print(f"  Note: Queue capacity might be different than expected")
    
    # Cleanup
    await publisher.close()
    
    # Delete topic
    print(f"\n6. Cleaning up - deleting topic...")
    try:
        response = requests.delete(f"{BASE_URL}/topics/backpressure-test")
        if response.status_code == 200:
            print("✓ Topic deleted successfully")
    except Exception as e:
        print(f"✗ Error deleting topic: {e}")
    
    print("\n" + "=" * 80)
    if overflow_detected:
        print("✓ BACKPRESSURE TEST PASSED: CONSUMER_IS_SLOW error working correctly!")
    else:
        print("✗ BACKPRESSURE TEST FAILED: Overflow not detected")
    print("=" * 80)


async def test_with_slow_consumer():
    """Test with a slow consumer to simulate real backpressure"""
    print("\n\n" + "=" * 80)
    print("SLOW CONSUMER TEST: Testing with actual slow subscriber")
    print("=" * 80)
    
    # Create topic
    print("\n1. Creating test topic 'slow-consumer-test'...")
    try:
        response = requests.post(
            f"{BASE_URL}/topics",
            json={"name": "slow-consumer-test"},
            headers={"Content-Type": "application/json"}
        )
        print("✓ Topic created")
    except Exception as e:
        print(f"✗ Error: {e}")
        return
    
    # Connect slow subscriber (but don't read messages)
    print("\n2. Connecting slow subscriber (won't consume messages)...")
    slow_subscriber = await websockets.connect(WS_URL)
    
    # Subscribe
    await slow_subscriber.send(json.dumps({
        "type": "subscribe",
        "topic": "slow-consumer-test",
        "client_id": "slow_consumer"
    }))
    ack = await slow_subscriber.recv()
    print(f"✓ Subscriber connected: {json.loads(ack)}")
    
    # Connect fast publisher
    print("\n3. Connecting fast publisher...")
    publisher = await websockets.connect(WS_URL)
    
    # Publish many messages rapidly
    print("\n4. Publishing 1100 messages rapidly (subscriber is slow)...")
    overflow_detected = False
    successful = 0
    
    for i in range(1, 1101):
        try:
            await publisher.send(json.dumps({
                "type": "publish",
                "topic": "slow-consumer-test",
                "message": {"id": f"msg_{i}", "payload": f"Data {i}"}
            }))
            
            response = await asyncio.wait_for(publisher.recv(), timeout=1)
            response_data = json.loads(response)
            
            if response_data.get("code") == "CONSUMER_IS_SLOW":
                overflow_detected = True
                print(f"\n  ⚠ CONSUMER_IS_SLOW detected at message {i}")
                print(f"  Message: {response_data.get('message')}")
                break
            else:
                successful += 1
                if i % 200 == 0:
                    print(f"  Published {i} messages...")
        except asyncio.TimeoutError:
            break
        except Exception as e:
            print(f"  Error at message {i}: {e}")
            break
    
    print(f"\n5. Results:")
    print(f"  - Successful publishes: {successful}")
    print(f"  - Overflow detected: {'YES' if overflow_detected else 'NO'}")
    
    # Cleanup
    await slow_subscriber.close()
    await publisher.close()
    
    requests.delete(f"{BASE_URL}/topics/slow-consumer-test")
    
    print("\n" + "=" * 80)
    if overflow_detected:
        print("✓ SLOW CONSUMER TEST PASSED!")
    else:
        print("? Queue might be larger than test size")
    print("=" * 80)


if __name__ == "__main__":
    print("Starting backpressure tests...\n")
    print("Make sure the server is running at http://localhost:8080\n")
    
    try:
        # Quick connectivity check
        response = requests.get(f"{BASE_URL}/health", timeout=2)
        if response.status_code != 200:
            print("✗ Server health check failed!")
            exit(1)
        print("✓ Server is running\n")
    except Exception as e:
        print(f"✗ Cannot connect to server: {e}")
        exit(1)
    
    # Run tests
    asyncio.run(test_backpressure())
    # asyncio.run(test_with_slow_consumer())  # Optional second test
