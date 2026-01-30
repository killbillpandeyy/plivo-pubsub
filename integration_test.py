#!/usr/bin/env python3
"""
Complete Integration Test for Plivo Pub/Sub System

Tests:
- Multiple topics simultaneously
- Multiple publishers per topic
- Multiple subscribers per topic
- REST API endpoints (topics, health, stats)
- Message routing and isolation between topics
- Concurrent operations
"""

import asyncio
import websockets
import json
import requests
import time
from typing import List, Dict
from datetime import datetime

# Configuration
BASE_URL = "http://localhost:8080"
WS_URL = "ws://localhost:8080/ws"

# ANSI color codes for better output
class Colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_header(text):
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'=' * 80}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{text:^80}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{'=' * 80}{Colors.ENDC}\n")

def print_test(test_name):
    print(f"{Colors.OKCYAN}▶ {test_name}{Colors.ENDC}")

def print_success(message):
    print(f"{Colors.OKGREEN}✓ {message}{Colors.ENDC}")

def print_error(message):
    print(f"{Colors.FAIL}✗ {message}{Colors.ENDC}")

def print_info(message):
    print(f"{Colors.OKBLUE}ℹ {message}{Colors.ENDC}")


class IntegrationTest:
    def __init__(self):
        self.test_topics = ["orders", "notifications"]
        self.results = []
        
    def run_all_tests(self):
        """Run all integration tests"""
        print_header("PLIVO PUB/SUB INTEGRATION TEST SUITE")
        
        # Phase 1: REST API Tests
        print_test("PHASE 1: REST API Tests")
        self.test_health_endpoint()
        self.test_create_topics()
        self.test_list_topics()
        self.test_stats_endpoint()
        
        # Phase 2: WebSocket Tests
        print_test("\nPHASE 2: WebSocket Tests")
        asyncio.run(self.run_websocket_tests())
        
        # Phase 3: Cleanup and Final Stats
        print_test("\nPHASE 3: Cleanup and Final Stats")
        self.test_final_stats()
        self.test_delete_topics()
        
        # Summary
        self.print_summary()
    
    def test_health_endpoint(self):
        """Test GET /health endpoint"""
        print_info("Testing GET /health...")
        try:
            response = requests.get(f"{BASE_URL}/health")
            assert response.status_code == 200
            data = response.json()
            assert "uptime_sec" in data
            assert "topics" in data
            assert "subscribers" in data
            print_success(f"Health check passed - Uptime: {data['uptime_sec']}s, Topics: {data['topics']}, Subscribers: {data['subscribers']}")
            self.results.append(("Health Endpoint", True))
        except Exception as e:
            print_error(f"Health check failed: {e}")
            self.results.append(("Health Endpoint", False))
    
    def test_create_topics(self):
        """Test POST /topics for multiple topics"""
        print_info(f"Creating topics: {self.test_topics}...")
        for topic in self.test_topics:
            try:
                response = requests.post(
                    f"{BASE_URL}/topics",
                    json={"name": topic},
                    headers={"Content-Type": "application/json"}
                )
                # Accept both 201 (created) and 409 (already exists)
                if response.status_code == 201:
                    data = response.json()
                    assert data["status"] == "created"
                    assert data["topic"] == topic
                    print_success(f"Topic '{topic}' created successfully")
                elif response.status_code == 409:
                    print_success(f"Topic '{topic}' already exists (skipping)")
                else:
                    raise Exception(f"Unexpected status code: {response.status_code}")
                self.results.append((f"Create Topic '{topic}'", True))
            except Exception as e:
                print_error(f"Failed to create topic '{topic}': {e}")
                self.results.append((f"Create Topic '{topic}'", False))
    
    def test_list_topics(self):
        """Test GET /topics endpoint"""
        print_info("Listing all topics...")
        try:
            response = requests.get(f"{BASE_URL}/topics")
            assert response.status_code == 200
            data = response.json()
            topics_list = data.get("topics", data) if isinstance(data, dict) else data
            assert isinstance(topics_list, list)
            topic_names = [t["name"] for t in topics_list]
            for topic in self.test_topics:
                assert topic in topic_names, f"Topic '{topic}' not found in list"
            print_success(f"Found {len(topics_list)} topics: {topic_names}")
            self.results.append(("List Topics", True))
        except Exception as e:
            print_error(f"Failed to list topics: {e}")
            self.results.append(("List Topics", False))
    
    def test_stats_endpoint(self):
        """Test GET /stats endpoint"""
        print_info("Checking topic statistics...")
        try:
            response = requests.get(f"{BASE_URL}/stats")
            assert response.status_code == 200
            data = response.json()
            topics_data = data.get("topics", data)
            for topic in self.test_topics:
                assert topic in topics_data, f"Topic '{topic}' not found in stats"
                assert "messages" in topics_data[topic]
                assert "subscribers" in topics_data[topic]
            print_success(f"Stats retrieved for {len(topics_data)} topics")
            self.results.append(("Stats Endpoint", True))
        except Exception as e:
            print_error(f"Failed to get stats: {e}")
            self.results.append(("Stats Endpoint", False))
    
    async def run_websocket_tests(self):
        """Run all WebSocket-based tests"""
        await self.test_multiple_topics_concurrent()
        await self.test_topic_isolation()
        await self.test_concurrent_publishers()
        await self.test_message_history()
    
    async def test_multiple_topics_concurrent(self):
        """Test multiple topics with multiple subscribers each running concurrently"""
        print_info("Testing multiple topics with concurrent subscribers...")
        
        ws_orders_1 = None
        ws_orders_2 = None
        ws_notif_1 = None
        ws_notif_2 = None
        ws_publisher = None
        
        try:
            # Create connections manually to avoid premature closure
            ws_orders_1 = await websockets.connect(WS_URL)
            ws_orders_2 = await websockets.connect(WS_URL)
            ws_notif_1 = await websockets.connect(WS_URL)
            ws_notif_2 = await websockets.connect(WS_URL)
            ws_publisher = await websockets.connect(WS_URL)
            
            # Subscribe to orders topic (2 subscribers)
            await ws_orders_1.send(json.dumps({
                "type": "subscribe",
                "topic": "orders",
                "client_id": "orders_sub_1"
            }))
            ack1 = json.loads(await asyncio.wait_for(ws_orders_1.recv(), timeout=2))
            if ack1.get("type") not in ["ack", "ACK"]:
                raise Exception(f"Expected ACK for orders_sub_1, got: {ack1}")
            
            await ws_orders_2.send(json.dumps({
                "type": "subscribe",
                "topic": "orders",
                "client_id": "orders_sub_2"
            }))
            ack2 = json.loads(await asyncio.wait_for(ws_orders_2.recv(), timeout=2))
            if ack2.get("type") not in ["ack", "ACK"]:
                raise Exception(f"Expected ACK for orders_sub_2, got: {ack2}")
            
            # Subscribe to notifications topic (2 subscribers)
            await ws_notif_1.send(json.dumps({
                "type": "subscribe",
                "topic": "notifications",
                "client_id": "notif_sub_1"
            }))
            ack3 = json.loads(await asyncio.wait_for(ws_notif_1.recv(), timeout=2))
            if ack3.get("type") not in ["ack", "ACK"]:
                raise Exception(f"Expected ACK for notif_sub_1, got: {ack3}")
            
            await ws_notif_2.send(json.dumps({
                "type": "subscribe",
                "topic": "notifications",
                "client_id": "notif_sub_2"
            }))
            ack4 = json.loads(await asyncio.wait_for(ws_notif_2.recv(), timeout=2))
            if ack4.get("type") not in ["ack", "ACK"]:
                raise Exception(f"Expected ACK for notif_sub_2, got: {ack4}")
            
            print_success("All 4 subscribers connected (2 per topic)")
            
            # Publish to orders topic
            await ws_publisher.send(json.dumps({
                "type": "publish",
                "topic": "orders",
                "message": {
                    "id": "order_001",
                    "payload": "New order received: $150"
                }
            }))
            pub_ack = json.loads(await asyncio.wait_for(ws_publisher.recv(), timeout=2))
            if pub_ack.get("type") not in ["ack", "ACK"]:
                raise Exception(f"Expected ACK from publish, got: {pub_ack}")
            
            # Both orders subscribers should receive the event
            event1 = json.loads(await asyncio.wait_for(ws_orders_1.recv(), timeout=2))
            event2 = json.loads(await asyncio.wait_for(ws_orders_2.recv(), timeout=2))
            assert event1.get("type") in ["event", "EVENT"] and event1.get("topic") == "orders"
            assert event2.get("type") in ["event", "EVENT"] and event2.get("topic") == "orders"
            assert event1["message"]["payload"] == "New order received: $150"
            print_success(f"Orders topic: Both subscribers received message")
            
            # Publish to notifications topic
            await ws_publisher.send(json.dumps({
                "type": "publish",
                "topic": "notifications",
                "message": {
                    "id": "notif_001",
                    "payload": "System maintenance scheduled"
                }
            }))
            pub_ack2 = json.loads(await asyncio.wait_for(ws_publisher.recv(), timeout=2))
            if pub_ack2.get("type") not in ["ack", "ACK"]:
                raise Exception(f"Expected ACK from publish, got: {pub_ack2}")
            
            # Both notification subscribers should receive the event
            notif_event1 = json.loads(await asyncio.wait_for(ws_notif_1.recv(), timeout=2))
            notif_event2 = json.loads(await asyncio.wait_for(ws_notif_2.recv(), timeout=2))
            assert notif_event1.get("type") in ["event", "EVENT"] and notif_event1.get("topic") == "notifications"
            assert notif_event2.get("type") in ["event", "EVENT"] and notif_event2.get("topic") == "notifications"
            assert notif_event1["message"]["payload"] == "System maintenance scheduled"
            print_success(f"Notifications topic: Both subscribers received message")
            
            self.results.append(("Multiple Topics Concurrent", True))
            
        except Exception as e:
            print_error(f"Multiple topics test failed: {e}")
            self.results.append(("Multiple Topics Concurrent", False))
        finally:
            # Clean up connections
            for ws in [ws_orders_1, ws_orders_2, ws_notif_1, ws_notif_2, ws_publisher]:
                if ws:
                    try:
                        await ws.close()
                    except:
                        pass
    
    async def test_topic_isolation(self):
        """Verify messages don't leak between topics"""
        print_info("Testing topic isolation (no cross-topic messages)...")
        
        ws_orders_sub = None
        ws_notif_sub = None
        ws_publisher = None
        
        try:
            ws_orders_sub = await websockets.connect(WS_URL)
            ws_notif_sub = await websockets.connect(WS_URL)
            ws_publisher = await websockets.connect(WS_URL)
            
            # Subscribe to different topics
            await ws_orders_sub.send(json.dumps({
                "type": "subscribe",
                "topic": "orders",
                "client_id": "isolation_orders"
            }))
            await asyncio.wait_for(ws_orders_sub.recv(), timeout=2)  # ACK
            
            await ws_notif_sub.send(json.dumps({
                "type": "subscribe",
                "topic": "notifications",
                "client_id": "isolation_notif"
            }))
            await asyncio.wait_for(ws_notif_sub.recv(), timeout=2)  # ACK
            
            # Publish ONLY to orders
            await ws_publisher.send(json.dumps({
                "type": "publish",
                "topic": "orders",
                "message": {
                    "id": "iso_test_001",
                    "payload": "Orders-only message"
                }
            }))
            await asyncio.wait_for(ws_publisher.recv(), timeout=2)  # ACK
            
            # Orders subscriber should receive it
            orders_event = json.loads(await asyncio.wait_for(ws_orders_sub.recv(), timeout=2))
            assert orders_event.get("type") in ["event", "EVENT"]
            assert orders_event.get("topic") == "orders"
            print_success("Orders subscriber received correct message")
            
            # Notifications subscriber should NOT receive anything
            try:
                notif_msg = await asyncio.wait_for(ws_notif_sub.recv(), timeout=1)
                print_error(f"Topic isolation VIOLATED: notifications subscriber received: {notif_msg}")
                self.results.append(("Topic Isolation", False))
            except asyncio.TimeoutError:
                print_success("Topic isolation verified: No cross-topic messages")
                self.results.append(("Topic Isolation", True))
                    
        except asyncio.TimeoutError as e:
            # If this is the timeout for notif subscriber, it's actually success
            if "Topic isolation verified" in str(self.results):
                pass
            else:
                print_error(f"Topic isolation test failed: Timeout - {e}")
                self.results.append(("Topic Isolation", False))
        except Exception as e:
            print_error(f"Topic isolation test failed: {e}")
            self.results.append(("Topic Isolation", False))
        finally:
            for ws in [ws_orders_sub, ws_notif_sub, ws_publisher]:
                if ws:
                    try:
                        await ws.close()
                    except:
                        pass
    
    async def test_concurrent_publishers(self):
        """Test multiple publishers publishing to same topic simultaneously"""
        print_info("Testing concurrent publishers on same topic...")
        
        ws_subscriber = None
        ws_pub1 = None
        ws_pub2 = None
        ws_pub3 = None
        
        try:
            ws_subscriber = await websockets.connect(WS_URL)
            ws_pub1 = await websockets.connect(WS_URL)
            ws_pub2 = await websockets.connect(WS_URL)
            ws_pub3 = await websockets.connect(WS_URL)
            
            # Subscribe
            await ws_subscriber.send(json.dumps({
                "type": "subscribe",
                "topic": "orders",
                "client_id": "multi_pub_sub"
            }))
            await asyncio.wait_for(ws_subscriber.recv(), timeout=2)  # ACK
            
            # Three publishers send messages concurrently
            publish_tasks = [
                ws_pub1.send(json.dumps({
                    "type": "publish",
                    "topic": "orders",
                    "message": {"id": "pub1_msg", "payload": "Message from publisher 1"}
                })),
                ws_pub2.send(json.dumps({
                    "type": "publish",
                    "topic": "orders",
                    "message": {"id": "pub2_msg", "payload": "Message from publisher 2"}
                })),
                ws_pub3.send(json.dumps({
                    "type": "publish",
                    "topic": "orders",
                    "message": {"id": "pub3_msg", "payload": "Message from publisher 3"}
                }))
            ]
            await asyncio.gather(*publish_tasks)
            
            # Collect ACKs from publishers
            await asyncio.wait_for(ws_pub1.recv(), timeout=2)
            await asyncio.wait_for(ws_pub2.recv(), timeout=2)
            await asyncio.wait_for(ws_pub3.recv(), timeout=2)
            
            # Subscriber should receive all 3 messages
            received_messages = []
            for i in range(3):
                event = json.loads(await asyncio.wait_for(ws_subscriber.recv(), timeout=2))
                assert event.get("type") in ["event", "EVENT"]
                received_messages.append(event["message"]["id"])
            
            assert "pub1_msg" in received_messages
            assert "pub2_msg" in received_messages
            assert "pub3_msg" in received_messages
            print_success(f"Received all 3 messages from concurrent publishers: {received_messages}")
            self.results.append(("Concurrent Publishers", True))
            
        except Exception as e:
            print_error(f"Concurrent publishers test failed: {e}")
            self.results.append(("Concurrent Publishers", False))
        finally:
            for ws in [ws_subscriber, ws_pub1, ws_pub2, ws_pub3]:
                if ws:
                    try:
                        await ws.close()
                    except:
                        pass
    
    async def test_message_history(self):
        """Test message history retrieval with last_n parameter"""
        print_info("Testing message history (last_n parameter)...")
        
        ws_publisher = None
        ws_subscriber = None
        
        try:
            ws_publisher = await websockets.connect(WS_URL)
            ws_subscriber = await websockets.connect(WS_URL)
            
            # Publish 5 messages first
            for i in range(1, 6):
                await ws_publisher.send(json.dumps({
                    "type": "publish",
                    "topic": "notifications",
                    "message": {
                        "id": f"hist_msg_{i}",
                        "payload": f"Historical message #{i}"
                    }
                }))
                await asyncio.wait_for(ws_publisher.recv(), timeout=2)  # ACK
            
            print_success("Published 5 messages to notifications topic")
            
            # Subscribe with last_n=3 to get recent history
            await ws_subscriber.send(json.dumps({
                "type": "subscribe",
                "topic": "notifications",
                "client_id": "history_tester",
                "last_n": 3
            }))
            ack = json.loads(await asyncio.wait_for(ws_subscriber.recv(), timeout=2))
            if ack.get("type") not in ["ack", "ACK"]:
                raise Exception(f"Expected ACK, got: {ack}")
            
            # Should receive 3 historical messages
            historical_messages = []
            for i in range(3):
                event = json.loads(await asyncio.wait_for(ws_subscriber.recv(), timeout=2))
                assert event.get("type") in ["event", "EVENT"]
                historical_messages.append(event["message"]["id"])
            
            # Should be the last 3 messages (3, 4, 5)
            assert "hist_msg_3" in historical_messages
            assert "hist_msg_4" in historical_messages
            assert "hist_msg_5" in historical_messages
            print_success(f"Retrieved last 3 messages correctly: {historical_messages}")
            self.results.append(("Message History", True))
            
        except Exception as e:
            print_error(f"Message history test failed: {e}")
            self.results.append(("Message History", False))
        finally:
            for ws in [ws_publisher, ws_subscriber]:
                if ws:
                    try:
                        await ws.close()
                    except:
                        pass
    
    def test_final_stats(self):
        """Check final statistics after all WebSocket operations"""
        print_info("Checking final statistics...")
        try:
            response = requests.get(f"{BASE_URL}/stats")
            data = response.json()
            topics_data = data.get("topics", data)
            
            print_info("Final Topic Statistics:")
            for topic, stats in topics_data.items():
                print(f"  - {topic}: {stats['messages']} messages, {stats['subscribers']} active subscribers")
            
            # Verify we have messages on both topics
            assert topics_data["orders"]["messages"] > 0, "Orders topic should have messages"
            assert topics_data["notifications"]["messages"] > 0, "Notifications topic should have messages"
            print_success("Final stats verified")
            self.results.append(("Final Stats Check", True))
        except Exception as e:
            print_error(f"Final stats check failed: {e}")
            self.results.append(("Final Stats Check", False))
    
    def test_delete_topics(self):
        """Test DELETE /topics/{name} endpoint"""
        print_info("Cleaning up - deleting test topics...")
        for topic in self.test_topics:
            try:
                response = requests.delete(f"{BASE_URL}/topics/{topic}")
                assert response.status_code == 200
                print_success(f"Topic '{topic}' deleted successfully")
                self.results.append((f"Delete Topic '{topic}'", True))
            except Exception as e:
                print_error(f"Failed to delete topic '{topic}': {e}")
                self.results.append((f"Delete Topic '{topic}'", False))
    
    def print_summary(self):
        """Print test execution summary"""
        print_header("TEST EXECUTION SUMMARY")
        
        passed = sum(1 for _, result in self.results if result)
        total = len(self.results)
        
        print(f"\n{Colors.BOLD}Test Results:{Colors.ENDC}")
        for test_name, result in self.results:
            status = f"{Colors.OKGREEN}PASSED{Colors.ENDC}" if result else f"{Colors.FAIL}FAILED{Colors.ENDC}"
            print(f"  {status} - {test_name}")
        
        percentage = (passed / total * 100) if total > 0 else 0
        print(f"\n{Colors.BOLD}Summary: {passed}/{total} tests passed ({percentage:.1f}%){Colors.ENDC}")
        
        if passed == total:
            print(f"\n{Colors.OKGREEN}{Colors.BOLD}🎉 ALL TESTS PASSED! 🎉{Colors.ENDC}")
        else:
            print(f"\n{Colors.WARNING}{Colors.BOLD}⚠ Some tests failed. Please review the output above.{Colors.ENDC}")


if __name__ == "__main__":
    print(f"{Colors.BOLD}Starting Plivo Pub/Sub Integration Tests...{Colors.ENDC}")
    print(f"{Colors.BOLD}Make sure the server is running at {BASE_URL}{Colors.ENDC}\n")
    
    try:
        # Quick connectivity check
        response = requests.get(f"{BASE_URL}/health", timeout=2)
        if response.status_code != 200:
            print_error("Server health check failed. Is the server running?")
            exit(1)
    except requests.exceptions.RequestException as e:
        print_error(f"Cannot connect to server at {BASE_URL}. Please start the server first.")
        print_error(f"Error: {e}")
        exit(1)
    
    test = IntegrationTest()
    test.run_all_tests()
