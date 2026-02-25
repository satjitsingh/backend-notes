# Redis vs Kafka: The Definitive Guide

## The Confusion: Why Do Beginners Mix Them Up?

You're learning backend development. You hear "Redis" and "Kafka" in the same breath. Both are used in distributed systems. Both can move data between services. Both show up in architecture diagrams. So they must be similar, right?

**Wrong.** They solve fundamentally different problems. Redis is a **data store** that happens to have messaging features. Kafka is a **messaging and event streaming platform** that happens to store data. Confusing them is like confusing a refrigerator (stores food, keeps it cold) with a conveyor belt (moves items from one place to another). Both are in a kitchen, but they do different jobs.

This page will clear that up. By the end, you'll know exactly when to reach for Redis, when to reach for Kafka, and when to use both together.

---

## Quick Answer (For the Impatient)

| | Redis | Kafka |
|---|-------|-------|
| **What is it?** | In-memory data store | Distributed event streaming platform |
| **Primary use** | Caching, sessions, real-time data | Event streaming, messaging, data pipelines |
| **Speed** | Sub-millisecond (RAM) | Low-latency (disk, but optimized) |
| **Data storage** | RAM (limited by memory) | Disk (can store terabytes) |
| **Message retention** | Optional (TTL-based) | Configurable (days/weeks/forever) |
| **Best analogy** | A sticky note on your desk | A post office that keeps copies of all letters |

---

## Building a Food Delivery App: Where Redis Fits and Where Kafka Fits

Let's build **QuickBite**, a food delivery app. We'll see Redis and Kafka in the same system, doing different jobs.

### The User Journey

1. **User opens the app** → Sees restaurant list, menu, prices
2. **User browses** → Checks restaurant ratings, reviews, delivery time estimates
3. **User places order** → Order goes to kitchen, payment processes, rider gets assigned
4. **User tracks order** → Real-time updates: "Preparing", "Out for delivery", "2 min away"
5. **Order delivered** → User gets notification, can rate the experience

### Where Redis Fits

**Restaurant list and menu** — These change rarely. Thousands of users browse the same restaurants. Every request hitting the database would kill it. **Redis caches** the restaurant list, menu items, and prices. First request: DB. Next 10,000 requests: Redis. Sub-millisecond response.

**User session** — User logs in. They're browsing, adding to cart. Which server handles their next request? Doesn't matter. **Redis stores the session** so any server can serve them. Cart survives server restarts.

**Real-time delivery tracking** — "Where is my rider?" The rider's location updates every few seconds. **Redis pub/sub or Redis Streams** can broadcast location updates to the user's app. Or: store the latest location in Redis (key = riderId, value = lat/long). App polls or uses WebSocket. Redis is fast enough for real-time.

**Rate limiting** — "Max 10 requests per minute per user." **Redis INCR** with a TTL. Simple, atomic, fast.

**Leaderboard** — "Top 10 restaurants by rating this week." **Redis Sorted Sets**. Add score, get range. O(log N). Perfect for rankings.

### Where Kafka Fits

**Order placed** — User clicks "Place Order." This event triggers: charge payment, notify restaurant, assign rider, update inventory, send confirmation. **Kafka**: One event "ORDER_PLACED" published. Payment service consumes it. Kitchen service consumes it. Rider service consumes it. Notification service consumes it. Each does its job. Decoupled. If payment is slow, the order is still recorded. Payment will catch up.

**Order status updates** — "Preparing" → "Out for delivery" → "Delivered." Each status change is an event. **Kafka** stores the full history. Audit trail. Analytics. "How long does it take from order to delivery?" Replay the events.

**Delivery assignment** — "Rider R123 assigned to order O456." Event goes to Kafka. Rider app consumes it. User app consumes it. Analytics consumes it. One event, many consumers.

**Payment failure** — Payment service publishes "PAYMENT_FAILED." Order service consumes it, updates order status. Notification service consumes it, alerts user. No tight coupling.

### The Architecture Together

```
    User App                    API Gateway
        │                            │
        │  GET /restaurants          │
        │ ──────────────────────────▶│
        │                            │  Check Redis cache
        │                            │ ──────────────────▶ Redis (cache)
        │  JSON response             │ ◀──────────────────
        │ ◀─────────────────────────│
        │                            │
        │  POST /orders              │
        │ ──────────────────────────▶│
        │                            │  Save to DB
        │                            │  Publish to Kafka
        │                            │ ──────────────────▶ Kafka (orders)
        │  Order created             │
        │ ◀─────────────────────────│
        │                            │
        │  GET /session/cart         │
        │ ──────────────────────────▶│
        │                            │  Read session from Redis
        │                            │ ──────────────────▶ Redis (sessions)
        │  Cart data                 │
        │ ◀─────────────────────────│
```

**Summary:** Redis = fast data access (cache, session, real-time state). Kafka = reliable event flow (order lifecycle, payment, notifications).

---

## Redis Pub/Sub vs Kafka: The Chat Application Example

Both Redis and Kafka can send messages from one publisher to many subscribers. But they work very differently. Let's see with a **chat application**.

### Scenario: Group Chat

Users A, B, C are in a chat room. User A sends a message. B and C should receive it.

### Redis Pub/Sub

```
    User A (Publisher)          Redis Channel "room:123"          User B (Subscriber)
         │                              │                              │
         │  PUBLISH "room:123" "Hello"  │                              │
         │ ───────────────────────────▶│                              │
         │                              │  Message delivered           │
         │                              │ ────────────────────────────▶│
         │                              │                              │
         │                              │  User C (Subscriber)          │
         │                              │ ────────────────────────────▶│
         │                              │                              │
```

**What happens if User B is offline?** The message is gone. Redis Pub/Sub is **fire-and-forget**. No storage. When B comes back, they never see "Hello."

**What happens if B is slow?** Redis doesn't wait. It pushes the message. If B's connection is slow, Redis might drop it. No backpressure.

**When Redis Pub/Sub is fine:** Live scores, live dashboards, presence notifications ("User X is typing"). If you miss a message, it's okay. The next one will have the current state.

### Kafka

```
    User A (Producer)           Kafka Topic "chat-messages"       User B (Consumer)
         │                              │                              │
         │  Produce "Hello"              │                              │
         │ ────────────────────────────▶│                              │
         │                              │  Message stored (persisted)  │
         │                              │  B consumes when ready       │
         │                              │ ────────────────────────────▶│
         │                              │                              │
         │                              │  User B was offline?          │
         │                              │  B comes back, reads from     │
         │                              │  last offset. Gets "Hello"!   │
```

**What happens if User B is offline?** The message sits in Kafka. When B comes back, they consume from their last offset. They get "Hello" and every message they missed.

**What happens if B is slow?** Kafka keeps the messages. B can catch up. No loss.

**When Kafka is right:** Chat history, order events, audit logs. You need guaranteed delivery and replay.

### Side-by-Side

| Feature | Redis Pub/Sub | Kafka |
|---------|---------------|-------|
| Message storage | No. Fire and forget. | Yes. Persisted to disk. |
| Offline consumer | Misses messages. | Catches up when back. |
| Replay | No. | Yes. Reset offset, re-read. |
| Throughput | High (in-memory). | Very high (distributed). |
| Ordering | Per channel. | Per partition. |
| Use case | Live updates, presence. | Events, audit, reliability. |

!!! tip "Interview Insight"
    "Redis Pub/Sub is fire-and-forget. Kafka is a durable log. If I need guaranteed delivery—like order processing or chat history—I choose Kafka. If I need real-time broadcasting where missing a message is acceptable—like live scores or 'user is typing'—Redis Pub/Sub is fine."

---

## Can I Use Redis AS a Message Queue?

Yes. **Redis Streams** (added in Redis 5.0) give you a Kafka-like log. Messages are stored. Consumers track their position (like Kafka offsets). Multiple consumer groups. Replay.

### Redis Streams vs Kafka: Detailed Comparison

This is a common interview question. Both support consumer groups, message acknowledgment, and replay. But they differ in scale, persistence, and operational complexity.

| Feature | Redis Streams | Kafka |
|---------|---------------|-------|
| **Storage** | RAM (optional AOF/RDB persistence) | Disk (always persisted) |
| **Scale** | Single node or Redis Cluster (sharded) | Distributed, many brokers |
| **Throughput** | ~100K msg/sec (single node) | Millions msg/sec (cluster) |
| **Retention** | MAXLEN (trim by count) or time-based | retention.ms, retention.bytes (days/weeks) |
| **Consumer groups** | Yes (XREADGROUP) | Yes |
| **Message ID** | Auto-generated (timestamp-seq) | Offset (per partition) |
| **Replay** | Yes (read from ID) | Yes (reset offset) |
| **Ordering** | Per stream (single partition) | Per partition |
| **Persistence guarantee** | Optional. Default: in-memory only | Always. Writes go to disk |
| **Operations** | Simpler. One process (or Cluster) | More complex. Brokers, ZooKeeper/KRaft |
| **Best for** | Task queues, low-volume events, already have Redis | High volume, multi-team, enterprise |

**When to use Redis Streams:** You need message queue semantics but your volume is modest (thousands, not millions per second). You already have Redis. You want simpler operations than Kafka. Good for: task queues, notification fan-out, event sourcing in smaller systems. Single team. Single region.

**When to use Kafka:** High throughput. Multiple teams. Long retention (days/weeks). Complex pipelines. Event sourcing at scale. Cross-datacenter. Enterprise compliance. When you need durability and can't afford to lose messages.

!!! tip "Interview Answer"
    "Redis Streams and Kafka both support consumer groups and replay. Redis Streams is in-memory first, simpler to run, good for thousands of messages per second. Kafka is disk-based, scales to millions, and is built for enterprise event streaming. I'd use Redis Streams when we already have Redis and need a lightweight queue. Kafka when we need durability, high throughput, or multi-team event pipelines."

---

## The Interview Question: When Would You Use Redis vs Kafka?

**Question:** "In what scenarios would you choose Redis over Kafka, and vice versa?"

**Model answer:**

"I'd use **Redis** when I need fast, low-latency access to data. Caching API responses, storing user sessions, rate limiting with atomic counters, leaderboards with sorted sets, or real-time presence. Redis lives in RAM, so reads and writes are sub-millisecond. It's perfect for state that needs to be read quickly and doesn't need to be persisted forever—or when persistence is optional.

"I'd use **Kafka** when I need reliable, ordered event streaming between services. Order processing, payment events, audit logs, notification fan-out, or data pipelines. Kafka persists messages to disk and supports consumer groups, so multiple services can process the same events independently. If a consumer is down, messages queue up. No loss. I can replay events for debugging or new consumers. Kafka scales horizontally with partitions and brokers.

"They're complementary. In an e-commerce app, I might use Redis for caching product data and session storage, and Kafka for order events, payment events, and inventory updates. Redis handles the 'read path' fast. Kafka handles the 'write path' and event flow reliably."

---

## Decision Framework: Yes/No Questions to Choose

Work through these questions. They'll point you to the right tool.

```
Do I need to store data temporarily for fast access?
  → YES → Redis (cache, session, rate limit)

Do I need to send events between microservices?
  → YES → Kafka

Do I need message replay or an audit trail?
  → YES → Kafka (or Redis Streams for smaller scale)

Do I need sub-millisecond response times?
  → YES → Redis

Do I need to process millions of events per second?
  → YES → Kafka

Do I need a leaderboard, ranked data, or sorted sets?
  → YES → Redis

Do I need to decouple producers and consumers?
  → YES → Kafka

Do I need both caching AND event streaming?
  → YES → Use both! Redis + Kafka together.
```

### More Scenarios: Expand Your Decision Tree

| Scenario | Redis? | Kafka? | Why |
|----------|--------|--------|-----|
| User session for 10K users | Yes | No | Fast lookup, TTL. Kafka is overkill. |
| Order events for audit | No | Yes | Need replay, durability. Redis doesn't persist like Kafka. |
| Real-time leaderboard | Yes | No | Sorted Sets. Sub-ms reads. Kafka isn't a query engine. |
| CDC from database to data lake | No | Yes | Event log, replay, high throughput. |
| Rate limit per user | Yes | No | INCR + TTL. Atomic. Fast. |
| Chat history (must not lose messages) | No | Yes | Kafka persists. Redis Pub/Sub loses messages. |
| "User is typing" indicator | Yes (Pub/Sub) | No | Fire-and-forget is fine. Low latency. |
| Inventory updates across 50 services | No | Yes | Fan-out, durability, replay. |
| Feature flags | Yes | No | Fast reads. Simple key-value. |
| Log aggregation from 1000 servers | No | Yes | High throughput, retention, multiple consumers. |

!!! info "Gray Areas"
    Some use cases could use either. Example: task queue. Redis Streams works for small scale. Kafka works for large scale. The decision depends on volume, team size, and existing infrastructure.

---

## Real-World Architecture Examples: Both Together

### Example 1: E-Commerce

```
1. User views product page → Redis cache serves product data (fast)
2. User places order → Kafka event: "ORDER_PLACED"
3. Payment service reads from Kafka → processes payment
4. After payment → Kafka event: "PAYMENT_SUCCESS"
5. Inventory service reads event → updates stock in DB
6. Cache service reads event → invalidates Redis cache for product
7. Next user sees updated stock → served from refreshed Redis cache
```

### Example 2: Rate Limiting + Event Logging

```
API Request arrives
    │
    ├──▶ Check Redis: "Has this user exceeded 100 requests/min?"
    │    ├── Yes → Return 429 Too Many Requests
    │    └── No  → INCR counter, process request
    │
    └──▶ Send to Kafka: Log the API request for analytics
```

### Example 3: Social Feed

```
- User posts: Event to Kafka → Fan-out service writes to followers' Redis feeds (sorted sets)
- User reads feed: Redis (fast, pre-computed)
- New follower: Kafka event → Fan-out service updates Redis feeds
```

---

## Architecture Patterns: Using Both Together

Here are three full architecture patterns. Each shows where Redis and Kafka fit. Use these as templates for design discussions.

### Pattern 1: E-Commerce Platform

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Web App   │────▶│  API GW     │────▶│   Redis     │  Cache: products, categories
└─────────────┘     └──────┬──────┘     └─────────────┘  Session: user cart, login
                           │
                           │  POST /order
                           ▼
                    ┌─────────────┐     ┌─────────────┐
                    │ Order Svc   │────▶│   Kafka     │  Topic: orders
                    └─────────────┘     └──────┬─────┘
                           │                    │
                           │                    ├──▶ Payment Svc (consume)
                           │                    ├──▶ Inventory Svc (consume)
                           │                    └──▶ Notification Svc (consume)
                           ▼
                    ┌─────────────┐
                    │  Database   │  Source of truth
                    └─────────────┘
```

**Redis:** Product cache, category cache, user session, cart. Rate limiting on API.
**Kafka:** Order placed, payment completed, inventory updated. Each service consumes independently. Audit trail. Replay for debugging.

### Pattern 2: Social Media Feed

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Mobile App │────▶│  Feed API   │────▶│   Redis     │  feed:user:{id} (Sorted Set)
└─────────────┘     └──────┬──────┘     └─────────────┘  Online users (Set)
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
   ┌──────────┐     ┌──────────┐     ┌──────────┐
   │  Kafka   │     │  Kafka   │     │  Kafka   │  Topics: posts, likes, follows
   │  posts   │     │  likes   │     │  follows │
   └────┬─────┘     └────┬─────┘     └────┬─────┘
        │                │                │
        └────────────────┼────────────────┘
                         ▼
                  ┌─────────────┐
                  │ Fan-out Svc │  Consumes events, writes to Redis feeds
                  └─────────────┘
```

**Redis:** Pre-computed feeds per user (Sorted Set by timestamp). "Who's online" (Set). Real-time "user typing" (Pub/Sub).
**Kafka:** New post, like, follow, unfollow. Fan-out service consumes and updates Redis. Search indexer consumes for Elasticsearch. Analytics consumes for dashboards.

### Pattern 3: Fintech / Payments

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Banking App│────▶│  Payment GW │────▶│   Redis     │  Rate limit, idempotency keys
└─────────────┘     └──────┬──────┘     └─────────────┘  Lock: prevent double-spend
                           │
                           │  Initiate payment
                           ▼
                    ┌─────────────┐     ┌─────────────┐
                    │ Payment Svc │────▶│   Kafka     │  Topic: payments (ordered by user)
                    └─────────────┘     └──────┬─────┘
                           │                    │
                           │                    ├──▶ Fraud detection (consume)
                           │                    ├──▶ Ledger service (consume)
                           │                    └──▶ Notification (consume)
                           ▼
                    ┌─────────────┐
                    │  Database   │  Transactions, balances
                    └─────────────┘
```

**Redis:** Rate limit per user. Idempotency key store (prevent duplicate payments). Distributed lock for balance updates. Cache for account summary.
**Kafka:** Payment initiated, payment completed, payment failed. Ordered by user ID (partition key) for consistency. Audit. Replay for reconciliation.

---

## Mock Interview Scenario

**Question:** "Design a ride-sharing app like Uber. Where does Redis fit? Where does Kafka fit? Walk me through a ride from request to completion."

### Model Answer

**Redis fits in these places:**

1. **Driver location cache:** When a rider requests a ride, we need nearby drivers. We store driver locations in Redis (Geospatial or Sorted Set by lat/long). Fast lookup. Updated every few seconds by driver app.

2. **Session and auth:** Rider and driver sessions. JWT or session ID in Redis. TTL for expiry.

3. **Rate limiting:** Prevent abuse. INCR per user with TTL. "Max 10 ride requests per minute."

4. **Real-time ETA:** Store current ETA in Redis. Rider app polls or uses WebSocket. Redis is fast enough for sub-second updates.

5. **Surge pricing state:** Current surge multiplier per zone. Stored in Redis. API reads it for price calculation.

**Kafka fits in these places:**

1. **Ride requested:** Event "RIDE_REQUESTED" with rider ID, pickup, dropoff. Dispatch service consumes. Finds drivers. Sends to Kafka "RIDE_ASSIGNED". Driver app consumes. Decoupled. If dispatch is slow, the event waits. No loss.

2. **Ride lifecycle:** RIDE_ASSIGNED → DRIVER_ARRIVED → RIDE_STARTED → RIDE_ENDED. Each event to Kafka. Multiple consumers: billing (for payment), analytics (for metrics), notification (for rider/driver alerts). Audit trail. "Why was this ride $25?" Replay the events.

3. **Payment events:** Payment initiated, payment completed. Kafka. Billing service consumes. Ledger updates. Idempotency via Redis, but the event flow is Kafka.

4. **Driver availability:** Driver goes online/offline. Event to Kafka. Dispatch service updates its view. Location updates could go to Kafka too for analytics (every 10 seconds), but real-time location is Redis for speed.

**Flow summary:**
- Rider opens app → Redis: session, cached zones.
- Rider requests ride → Kafka: RIDE_REQUESTED. Redis: rate limit check.
- Dispatch finds driver → Kafka: RIDE_ASSIGNED. Redis: update driver availability.
- Driver en route → Redis: driver location (real-time). Kafka: DRIVER_ARRIVED when close.
- Ride ends → Kafka: RIDE_ENDED. Billing consumes. Payment. Redis: clear session data if needed.

**One-liner:** "Redis for fast state—locations, sessions, rate limits. Kafka for events—ride lifecycle, payments, analytics. Redis is the read path. Kafka is the event backbone."

---

## Detailed Comparison

### Data Storage

```
Redis:
  ┌───────────┐
  │    RAM    │  ← Fast but limited and expensive
  └───────────┘

Kafka:
  ┌───────────┐
  │   Disk    │  ← Slower but huge capacity and cheap
  └───────────┘
```

| Aspect | Redis | Kafka |
|--------|-------|-------|
| Storage medium | RAM | Disk (sequential I/O) |
| Capacity | Limited by RAM (GBs) | Limited by disk (TBs) |
| Cost | Expensive (RAM costs more) | Cheaper (disk is cheap) |
| Persistence | Optional (RDB/AOF) | Always persisted |

---

### Message/Data Model

| Aspect | Redis | Kafka |
|--------|-------|-------|
| Data model | Key-value (+ Lists, Sets, Hashes, Streams) | Log of events (topic → partitions) |
| Message retention | Until TTL expires or manually deleted | Retained for configured duration |
| Replay | Not possible (data gone after read/expire) | Yes (re-read any past message) |
| Consumer tracking | N/A (Pub/Sub) or Streams (consumer groups) | Offset-based |

---

### Performance

| Metric | Redis | Kafka |
|--------|-------|-------|
| Read latency | < 1ms | 5-50ms |
| Write latency | < 1ms | 5-20ms |
| Throughput | ~100K ops/sec (single thread) | Millions of msgs/sec (distributed) |
| Best for | Low-latency reads/writes | High-throughput streaming |

---

### Scaling

| Aspect | Redis | Kafka |
|--------|-------|-------|
| Horizontal scaling | Redis Cluster (hash slots) | Add brokers + partitions |
| Replication | Master-Replica | Leader-Follower per partition |
| High availability | Redis Sentinel | Built-in (ISR, leader election) |
| Consumer scaling | N/A (Pub/Sub) | Add consumers to group (up to partition count) |

---

## When to Use What?

### Use Redis When:

| Scenario | Why Redis? |
|----------|------------|
| **Caching API responses** | Sub-millisecond reads from RAM |
| **Session storage** | Fast access, TTL auto-expiry |
| **Rate limiting** | Atomic counters (INCR) |
| **Leaderboards** | Sorted Sets |
| **Real-time counters** | Atomic operations |
| **Temporary data** | TTL-based auto-cleanup |
| **Distributed locks** | SET NX with expiry |

### Use Kafka When:

| Scenario | Why Kafka? |
|----------|------------|
| **Microservice communication** | Decouples services |
| **Event sourcing** | Complete event log |
| **Log aggregation** | Collects logs from many servers |
| **Data pipelines** | Moves data between systems |
| **Order processing** | Reliable, ordered event processing |
| **Real-time analytics** | Stream processing |
| **Audit trail** | Immutable event log |

---

## When NOT to Use Redis

Redis is great. But it's not for everything. Avoid Redis when:

| Scenario | Why Not Redis? |
|----------|----------------|
| **Large datasets (100s of GB)** | Redis is in-memory. RAM is expensive. Use a database or Kafka for bulk data. |
| **Complex queries (JOINs, aggregations)** | Redis has no SQL. No JOINs. For analytics, use a database or OLAP store. |
| **Primary database for critical data** | Redis can persist, but it's not ACID. Use PostgreSQL/MySQL for source of truth. |
| **Full-text search** | Redis has limited search. Use Elasticsearch. |
| **Guaranteed message delivery** | Redis Pub/Sub is fire-and-forget. Redis Streams can persist, but Kafka is built for this. |
| **Multi-datacenter replication** | Redis Cluster is single-cluster. Cross-DC is complex. Kafka has mirroring. |
| **Storing files or blobs** | Redis is for small values. Use object storage (S3) for large files. |

!!! warning "Redis Is Not a Database"
    Don't use Redis as your only data store for business-critical data. Use it as a cache, session store, or real-time layer. Your database is the source of truth.

---

## When NOT to Use Kafka

Kafka is powerful. But it adds complexity. Avoid Kafka when:

| Scenario | Why Not Kafka? |
|----------|----------------|
| **Simple request-response** | User clicks, API returns. No need for events. Use HTTP + database. |
| **Small system (single app, low traffic)** | Kafka is overkill. Redis, or even in-memory queues, might suffice. |
| **Low throughput (< 100 msg/sec)** | Kafka's strength is scale. For tiny volumes, a database or Redis is simpler. |
| **Need sub-millisecond latency** | Kafka writes to disk. Typically 5–20ms. For sub-ms, use Redis. |
| **Tight request-response coupling** | "Send order, wait for confirmation." Kafka is async. Use sync APIs or RPC. |
| **No ops team for Kafka** | Kafka needs tuning, monitoring, brokers. If you can't operate it, use managed Kafka or a simpler queue. |
| **Simple task queue (one producer, one consumer)** | Redis List or Streams is simpler. Kafka shines with multiple consumers and replay. |

!!! tip "Start Simple"
    If you're building an MVP or a small app, start with Redis or a database. Add Kafka when you have multiple services, event-driven needs, or scale that justifies it.

---

## Interview Cheat Sheet

| Question | Answer |
|----------|--------|
| "Redis or Kafka for caching?" | **Redis** — it's designed for caching |
| "Redis or Kafka for messaging?" | **Kafka** — durable, replayable, scalable |
| "Can you use both?" | **Yes** — Redis for caching, Kafka for events |
| "Redis Pub/Sub vs Kafka?" | Redis = fire-and-forget; Kafka = durable log |
| "Which is faster?" | Redis for reads; Kafka for throughput |
| "Which scales better?" | Kafka for data volume; Redis for read latency |

---

## Quick Summary

```
┌────────────────────────────────────────────────────┐
│              Redis vs Kafka Summary                │
├──────────────────┬─────────────────────────────────┤
│                  │                                 │
│     REDIS        │          KAFKA                  │
│                  │                                 │
│  • In-memory     │  • Disk-based                   │
│  • Key-value     │  • Event log                    │
│  • Cache/Session │  • Messaging/Streaming          │
│  • Sub-ms reads  │  • High throughput              │
│  • TTL expiry    │  • Configurable retention       │
│  • No replay     │  • Replay possible               │
│  • Pub/Sub (basic)│  • Pub/Sub (durable)           │
│                  │                                 │
│  Use together for best results!                    │
└──────────────────┴─────────────────────────────────┘
```

---

**Congratulations!** You've completed the entire learning path. Go back to the [Interview Questions](../redis/redis-interview-questions.md) pages for a quick revision before your interview. Good luck!
