# Kafka Use Cases

## Where is Kafka Used?

Kafka is everywhere in modern backend systems. But here's the thing—reading a list of use cases doesn't teach you much. You need to *feel* the problems that Kafka solves. You need to understand what life was like *before* Kafka, and why teams made the switch.

In this guide, we're going to tell stories. Real-world scenarios where developers faced real pain, and how Kafka became the solution. By the end, you won't just know *what* Kafka does—you'll understand *why* it exists and *when* to reach for it.

---

## 1. Event-Driven Architecture

### The Story: From Chaos to Calm at StartupXYZ

Imagine you're a backend developer at a fast-growing startup. Your company started simple: one monolithic application that did everything. Then the CTO said, "We need to scale. Let's break this into microservices."

So you did. You now have an Order Service, a Payment Service, an Inventory Service, and an Email Service. The Order Service is the star—when someone places an order, it needs to tell everyone else. So you wrote code like this:

```
Order placed? Call Payment Service via HTTP.
Payment done? Call Inventory Service via HTTP.
Inventory updated? Call Email Service via HTTP.
```

Seems logical, right? Here's what happened in production.

**Monday 2 AM:** The Email Service crashed. Maybe a bug, maybe the email provider was down. Your Order Service made an HTTP call to send the order confirmation. The call failed. Your Order Service threw an exception. The entire order flow stopped. Customers who had *already paid* never got a confirmation email. Support tickets flooded in.

**Tuesday:** You added retry logic. "If Email Service fails, retry 3 times." But now when Email Service was slow, your Order Service waited. And waited. Your API response time went from 200ms to 5 seconds. Users thought the site was broken.

**Wednesday:** The Product team said, "We need to add SMS notifications too." And "We need to send data to our analytics warehouse." And "We need to update the search index." Every new requirement meant modifying the Order Service. It became a tangled web of HTTP calls. One service down, and the whole chain broke.

**The breaking point:** Black Friday. Traffic spiked 10x. The Payment Service got overwhelmed. Every HTTP call from Order Service to Payment Service started timing out. Orders were failing. The CEO was on the war room call. Someone said: "What if Order Service could just *fire and forget*? What if it didn't have to wait for anyone?"

That's when you discovered Kafka.

### How Kafka Solved It

Think about it this way: Instead of Order Service *calling* other services, Order Service now *publishes an event* to a topic called "orders." It doesn't care who's listening. It doesn't wait. It just says, "Hey, an order happened. Here are the details." And it moves on.

```
Order Service ──▶ Kafka Topic: "orders"
                       │
                       ├──▶ Payment Service (reads when ready)
                       ├──▶ Inventory Service (reads when ready)
                       ├──▶ Email Service (reads when it's back up)
                       └──▶ Analytics Service (you added this without touching Order Service!)
```

If Email Service is down for an hour? The messages wait in Kafka. When Email Service comes back, it reads from where it left off. No lost emails. No blocked orders. No coupling.

**The magic:** Order Service doesn't know Payment Service exists. It doesn't know Email Service exists. It just knows: "I publish to Kafka." Want to add a new consumer that sends Slack notifications? Add it. No changes to Order Service. That's loose coupling.

!!! success "Real-World Scenario"
    **Uber's use case:** When a ride starts, dozens of systems need to know—driver app, rider app, billing, fraud detection, analytics, surge pricing. They don't call each other. They all subscribe to the same Kafka topics. When one system is slow, others keep moving.

!!! info "Companies Using This Pattern"
    - **Netflix:** Every user action (play, pause, search) becomes an event. Hundreds of services consume these events for recommendations, A/B testing, and analytics. No service directly calls another for these flows.
    - **Airbnb:** Search events, booking events, and messaging events flow through Kafka. The search ranking service, the notification service, and the analytics pipeline all consume independently.
    - **Spotify:** Playback events, skip events, and playlist changes go to Kafka. The recommendation engine, the royalty calculation system, and the analytics team all read from the same streams.

### Common Mistakes Beginners Make

**Mistake 1:** Treating Kafka like a queue. "I'll have one consumer that processes everything." That works for simple cases, but the power of event-driven architecture is *multiple independent consumers*. Each service gets its own copy of the event.

**Mistake 2:** Putting business logic in the producer. The producer should just publish "what happened." Don't make it decide who cares. Let consumers decide.

**Mistake 3:** Synchronous thinking. "But I need a response from Payment Service!" For that, you might use request-reply over Kafka, or keep a synchronous call for that specific flow. Event-driven doesn't mean *everything* is async.

---

## 2. Log Aggregation

### The Story: The DevOps Engineer Drowning in Logs

Meet Priya. She's a DevOps engineer at a company with 50 microservices running across 50 servers. Every service writes logs to its local disk. When something goes wrong at 3 AM, she gets paged. "The checkout is failing!"

She SSHes into Server 23. Checks the Order Service logs. Nothing obvious. Maybe it's the Payment Service? SSH to Server 41. Check those logs. Maybe the failure is in the API Gateway? SSH to Server 7. She's jumping between servers, grepping through log files, trying to trace a single user's request across 5 different services. It takes 45 minutes to find the issue. The user gave up 44 minutes ago.

**The daily struggle:** Developers ask, "Can you get me the logs for user ID 12345 from yesterday?" That request touched 8 services across 6 servers. Priya writes a script. It takes 20 minutes to run. The developer needs it in 5.

**The scaling nightmare:** The company grows. 50 servers become 200. 200 become 500. Logs are everywhere. Disk space runs out. Log rotation deletes old logs before anyone can analyze them. There's no single place to search.

**The solution:** Log aggregation. Every server runs a lightweight agent that ships logs to a central place. But here's the problem—what's that central place? If you send 500 servers' worth of logs directly to Elasticsearch, you'll overwhelm it. If you send them to a database, you'll need a massive one. And what happens when the central system is down? Logs get lost.

### How Kafka Solved It

Kafka sits in the middle. Think of it as a shock absorber.

```
Server 1  ──▶ ┐
Server 2  ──▶ ├──▶ Kafka Topic: "application-logs" ──▶ Logstash/Fluentd ──▶ Elasticsearch
Server 3  ──▶ │         (buffers millions of messages)                      (search when ready)
...
Server 500 ──▶ ┘
```

Each server produces logs to Kafka. Kafka holds them. It doesn't care if Elasticsearch is slow or restarting. The logs wait. A consumer (like Logstash or a custom service) reads from Kafka at its own pace and pushes to Elasticsearch. If Elasticsearch goes down for an hour, Kafka keeps the logs. No data loss.

**Why Kafka and not just "send to Elasticsearch"?** Because Kafka handles *bursts*. Black Friday? 10x log volume? Kafka absorbs it. Elasticsearch can't ingest that fast. Kafka buffers. The consumer catches up when traffic slows. That's the power of decoupling producers from consumers.

!!! tip "Think About It This Way"
    Kafka is like a post office during Christmas. Millions of packages arrive. The post office doesn't deliver them all the same day. It holds them. Delivery trucks (consumers) take what they can handle. No packages are lost. Everyone gets their mail—eventually.

!!! info "Companies Using This Pattern"
    - **LinkedIn:** Kafka was *literally created* for this. LinkedIn had thousands of servers generating logs and metrics. They needed a system that could handle the volume. Kafka was born. Today, LinkedIn ingests billions of events per day into Kafka before they flow to monitoring and analytics systems.
    - **Netflix:** Every microservice sends logs and metrics to Kafka. From there, they flow to their monitoring stack. When Netflix does a chaos engineering experiment (intentionally killing servers), Kafka ensures no log data is lost during the chaos.
    - **Twitter/X:** Tweet ingestion, engagement events, and system logs all flow through Kafka. Their data pipeline starts with Kafka as the central nervous system.

---

## 3. Real-Time Analytics

### The Story: The Dashboard That Couldn't Keep Up

Imagine you're building the operations dashboard for Uber. The product manager wants: "Show me how many drivers are currently active in San Francisco. Live. Not 5 minutes ago. Right now."

Your first attempt: a batch job. Every 5 minutes, you run a query against the database. Count active drivers. Store the result. The dashboard reads the stored result. It works, but "live" it is not. During a concert or a game, demand spikes in minutes. By the time your dashboard updates, the surge is over. Dispatchers are making decisions with stale data.

Your second attempt: query the database on every page load. "SELECT COUNT(*) FROM drivers WHERE status='active' AND city='SF'." With 10,000 concurrent dashboard users, your database melts. You add caching. Now you have a 30-second cache. Better, but still not real-time.

**The real requirement:** Every time a driver goes online, goes offline, or moves to a new city, the dashboard should reflect it within seconds. Not minutes. Seconds.

### How Kafka Solved It

Every state change becomes an event. Driver goes online? Event. Driver accepts a ride? Event. Driver completes a ride? Event. These events flow to Kafka. A stream processing application (Kafka Streams, Flink, or similar) consumes them and maintains a *real-time* aggregate. "Active drivers in SF right now." The dashboard queries this aggregate—or the aggregate pushes updates to the dashboard via WebSockets.

```
Driver App: "I'm online in SF"
    │
    ▼
Kafka Topic: "driver-events"
    │
    ▼
Stream Processor: Maintains "active_drivers_by_city" state
    │
    ▼
Dashboard: "SF has 847 active drivers" (updated every 2 seconds)
```

**The key insight:** You're not querying a database for "current state." You're *building* current state from a stream of events. The stream is the source of truth. The database (or in-memory store) is just a materialized view that gets updated in real time.

!!! example "Real-World Scenario"
    **Uber's driver tracking:** When you open the Uber app and see cars moving on the map, that's real-time. Driver location updates flow through Kafka. A geo-processing service consumes them, updates positions, and pushes to your phone. Millions of location updates per second. Kafka handles it.

!!! info "Companies Using This Pattern"
    - **Uber:** Trip events, driver location, surge pricing calculations—all real-time. The famous "heat map" showing demand? Kafka streams. Driver ETA estimates? Kafka streams.
    - **Netflix:** "Trending now" and "Because you watched X"—these use real-time viewing events. When millions of people start watching a new show, the system knows within seconds.
    - **LinkedIn:** The feed ranking algorithm uses real-time engagement. You like a post? That event flows through Kafka. The ranking model gets updated. Your feed reflects it.

---

## 4. Data Pipeline / ETL

### The Problem (Briefly)

Data lives in many places. Your transactional database (MySQL). Your data warehouse (Snowflake, BigQuery). Your search engine (Elasticsearch). Your cache (Redis). They all need to stay in sync. Or data needs to flow from source systems to analytics. Traditionally, you'd write ETL jobs—Extract, Transform, Load. Cron jobs that run every hour. Or custom scripts. They're brittle. They fail. They're hard to maintain.

### How Kafka Solved It

Kafka Connect is a framework that moves data in and out of Kafka *without writing application code*. You configure connectors. "Take every row from this MySQL table and put it in Kafka." "Take every message from this Kafka topic and put it in Elasticsearch." The connectors handle the heavy lifting.

```
MySQL (orders table) ──▶ Kafka Connect (JDBC Source) ──▶ Kafka Topic: "mysql.orders"
                                                              │
                                                              ├──▶ Kafka Connect (Snowflake Sink) ──▶ Data Warehouse
                                                              ├──▶ Kafka Connect (Elasticsearch Sink) ──▶ Search
                                                              └──▶ Kafka Connect (MongoDB Sink) ──▶ Analytics DB
```

**Why Kafka in the middle?** Because you might have 10 destinations. Without Kafka, you'd need 10 different pipelines from MySQL. With Kafka, you have one source connector. Ten sink connectors. Add a new destination? Add a connector. No changes to the source.

### Step-by-Step Flow

**Step 1:** Application inserts a row into MySQL `orders` table. MySQL writes to its transaction log (binlog).

**Step 2:** Kafka Connect JDBC Source (or Debezium) reads the binlog. Detects the new row.

**Step 3:** Source connector publishes change event to Kafka topic "mysql.orders": `{op: "c", after: {id: 1, customer_id: 42, total: 99.99}}`.

**Step 4:** Snowflake Sink connector consumes from "mysql.orders." Writes row to data warehouse. Analytics team can query.

**Step 5:** Elasticsearch Sink connector consumes the same event. Indexes the order for search. Customers can search orders.

**Step 6:** MongoDB Sink consumes. Syncs to analytics DB. One source. Many sinks. Each connector runs independently.

!!! info "Companies Using This Pattern"
    Almost every data team at scale uses Kafka in their pipeline. Confluent (founded by Kafka creators) reports that the majority of their enterprise customers use Kafka Connect for database replication, data lake ingestion, and cloud data warehouse loading.

---

## 5. Order Processing

### The Story: Black Friday Without Kafka

It's Black Friday at MegaShop, an e-commerce company. The architecture: Order Service receives an order. It makes synchronous HTTP calls to Payment Service, Inventory Service, and Shipping Service. If all succeed, it sends a confirmation email.

**10:00 AM:** Traffic is normal. Everything works.

**10:15 AM:** A viral tweet. Traffic doubles. Payment Service starts slowing down. Each HTTP call from Order Service takes 3 seconds instead of 200ms. Order Service threads are blocked. The thread pool fills up. New orders get "503 Service Unavailable." Customers see errors. They retry. Duplicate orders start appearing.

**10:30 AM:** Someone restarts Payment Service to "clear the load." All in-flight requests fail. Hundreds of orders were "charged" (money deducted) but Order Service never got the response. Those orders are stuck in limbo. Finance is panicking.

**10:45 AM:** Inventory Service runs out of connections to the database. It starts rejecting requests. Order Service gets errors. It doesn't know if the order was placed or not. Some orders went through. Some didn't. The data is inconsistent.

**The post-mortem:** "We need to decouple. We need to make our system resilient. We need to handle failures gracefully." The team decides to rebuild with Kafka.

### How Kafka Solved It

```
Customer places order
    │
    ▼
Order Service: Validates, publishes to Kafka Topic "orders"
    │
    ▼ (Order Service returns "Order received!" immediately. No waiting.)
    │
Kafka Topic: "orders"
    │
    ├──▶ Payment Service: Charges card, publishes to "payment-results"
    │         │
    │         └──▶ (If payment fails, publishes to "payment-failures" for retry)
    │
    ├──▶ Inventory Service: Reserves stock (reads when ready, processes at its own pace)
    │
    ├──▶ Shipping Service: Creates shipment (can batch for efficiency)
    │
    └──▶ Notification Service: Sends email (if this is slow, others aren't affected)
```

**What changed?** Order Service doesn't wait for anyone. It publishes and returns. Each downstream service consumes at its own speed. Payment Service slow? Messages queue up in Kafka. Payment Service catches up. No blocked threads. No cascading failures. Payment Service down? Messages wait. When it's back, it processes. No lost orders.

**For critical ordering:** Use the order ID as the Kafka message key. All events for order-123 go to the same partition. They're processed in order. Payment before shipping. Always.

### Step-by-Step Flow

**Step 1:** Customer clicks "Place Order" on MegaShop. Order Service validates cart, creates order record in DB with status "PENDING."

**Step 2:** Order Service publishes to Kafka topic "orders" with key=orderId. Message: `{orderId, customerId, items, total}`. Returns "Order received!" to customer. No waiting.

**Step 3:** Payment Service consumes the event. It charges the card. If success, publishes to "payment-results." If failure, publishes to "payment-failures" for retry or alert.

**Step 4:** Inventory Service consumes the same event. Reserves stock. Updates DB. Can batch multiple orders for efficiency.

**Step 5:** Shipping Service consumes. Creates shipment. May wait for payment-results first (depends on design).

**Step 6:** Notification Service consumes. Sends confirmation email. If Email Service is slow, others are not blocked. Messages wait in Kafka.

!!! warning "Common Mistake"
    Don't use Kafka for the *synchronous* part of checkout—"Validate card and reserve inventory before showing order confirmation." That still needs a fast, synchronous flow. Use Kafka for the *asynchronous* follow-ups: send email, update analytics, notify warehouse. Know the difference.

!!! info "Companies Using This Pattern"
    - **Walmart:** Order events, inventory updates, and fulfillment events flow through Kafka. During peak shopping seasons, the decoupled architecture handles 10x traffic without the order service becoming a bottleneck.
    - **Alibaba:** Single's Day (their Black Friday) generates billions in sales. Kafka handles the event stream for orders, inventory, and logistics. Each system scales independently.

---

## 6. Notification System

### The Story: Building WhatsApp-Like Notifications

You're building a social app. When someone likes your post, you need to notify the user. How? Email. Push notification. In-app badge. Maybe SMS for critical alerts. Maybe a Slack message for business users.

**First attempt:** Your "Like" service, when it receives a like, calls the Email Service API. Then calls the Push Service API. Then calls the In-App Service. Three HTTP calls. If Push Service is down, the whole flow fails. You add retries. Now the Like service is slow. And what about ordering? User gets a push before the in-app badge updates? Weird.

**Second attempt:** A "Notification Router" service. Like service calls the router. Router calls Email, Push, In-App. Same problem—router is a single point of failure. And adding a new channel (WhatsApp) means changing the router.

### How Kafka Solved It

```
User likes a post
    │
    ▼
Like Service: Publishes to Kafka Topic "notifications"
    │
    │  Message: { userId, type: "like", postId, actorId, timestamp }
    │
    ▼
Kafka Topic: "notifications"
    │
    ├──▶ Email Consumer: "User prefers email? Send email."
    ├──▶ Push Consumer: "User has app? Send push."
    ├──▶ In-App Consumer: "Update badge count, store in DB."
    ├──▶ SMS Consumer: "Critical alert? Send SMS."
    └──▶ (Next month) WhatsApp Consumer: "User opted in? Send WhatsApp."
```

**The beauty:** Adding WhatsApp notifications doesn't touch the Like service. Doesn't touch the Email consumer. You add a new consumer. It subscribes to "notifications." Done. Each channel is independent. Email is slow? Push and In-App still work. That's the power of the pub/sub model.

!!! tip "Think About It This Way"
    Kafka is like a radio broadcast. The radio station (producer) sends out a signal. You don't know who's listening. Could be 10 people. Could be 10 million. Could be someone with a radio, someone with a phone, someone with a car stereo. Each listener (consumer) gets the same content. Independently. The station doesn't wait for anyone to tune in.

---

## 7. Activity Tracking

### The Story: LinkedIn Tracking Your Profile Views

You visit someone's LinkedIn profile. They get a notification: "Someone viewed your profile." How does LinkedIn know? They're tracking every page view, every click, every search. Millions of users. Billions of events per day.

**The challenge:** This data feeds many systems. The "Who viewed your profile" feature. The analytics team (what content gets engagement?). The recommendation engine (suggest people similar to those you viewed). The ad targeting system (show relevant ads). The data science team (build ML models). If each of these systems had to be called synchronously when you viewed a profile, the page load would take seconds. And one slow system would block the others.

### How Kafka Solved It

```
You click "View Profile" on John's page
    │
    ▼
Frontend: Sends event to backend (async, fire-and-forget)
    │
    ▼
Backend: Publishes to Kafka Topic "user-activity"
    │  Message: { userId: you, action: "profile_view", targetUserId: John, timestamp }
    │
    ▼
Kafka Topic: "user-activity"
    │
    ├──▶ Profile Views Service: "John has a new viewer. Update his 'Who viewed you' list."
    ├──▶ Analytics Service: "Increment profile_view metric. Store for dashboards."
    ├──▶ Recommendation Engine: "User viewed John. Maybe suggest similar people."
    ├──▶ Data Lake Consumer: "Store in S3 for ML training."
    └──▶ Ad Service: "User interested in professionals like John. Refine ad targeting."
```

**Key point:** The frontend doesn't wait. The event is published. The page loads fast. Five different systems get the data. They process independently. Some might be real-time (profile views). Some might be batch (ML training). Kafka serves both.

### Step-by-Step Flow

**Step 1:** User clicks "View Profile" on John's LinkedIn page. Frontend sends async request to backend.

**Step 2:** Backend receives the request. It does not call Profile Views, Analytics, Recommendations, or Ad Service. It publishes one event to Kafka topic "user-activity": `{userId: you, action: "profile_view", targetUserId: John, timestamp}`.

**Step 3:** Backend returns 200 OK immediately. Page load is fast.

**Step 4:** Profile Views Service consumes the event. It updates John's "Who viewed you" list. Writes to DB.

**Step 5:** Analytics Service consumes the same event. Increments metrics. Stores for dashboards.

**Step 6:** Recommendation Engine consumes the same event. Updates model: "User viewed John. Maybe suggest similar people."

**Step 7:** Data Lake consumer stores the event in S3 for ML training. Batch job.

**Step 8:** Ad Service consumes the event. Refines ad targeting. All five systems get the same event. Independently. No blocking.

!!! info "Companies Using This Pattern"
    - **LinkedIn:** Every click, view, search, and connection flows through Kafka. This data powers the feed, "People you may know," analytics, and more. Kafka was built at LinkedIn for exactly this.
    - **Facebook/Meta:** The activity stream—likes, comments, shares—flows through Kafka-like systems. Multiple consumers power different features.
    - **Amazon:** "Customers who viewed this also viewed"—that recommendation uses activity data. So does "Frequently bought together." Event streams feed the recommendation engines.

---

## 8. Change Data Capture (CDC)

### What is CDC? (From Scratch)

Most beginners haven't heard of CDC. Let's fix that.

**The school register analogy:** Imagine a school with a paper register. Every time a student enrolls, drops, or changes their address, the office updates the register. Other departments need this info. The library needs the student list for library cards. The cafeteria needs it for meal plans. The bus service needs it for routes.

**Option 1:** Every time the register changes, the office calls the library, the cafeteria, and the bus service. "Hey, we have an update." That's a lot of work. And what if the library is closed? Missed update.

**Option 2:** Each department comes to the office every hour and copies the entire register. Wasteful. And they might miss changes that happened between visits.

**Option 3 (CDC):** The office keeps a *change log*. "9:00 - Student 101 enrolled. 9:15 - Student 102 address updated. 9:30 - Student 103 dropped." Anyone who needs to stay in sync reads this log. The library reads it and updates its records. The cafeteria reads it. They all get the same changes, in order. No one has to copy the whole register. They just apply the changes.

**CDC = Change Data Capture.** It's the idea of capturing every change (insert, update, delete) that happens in a database and streaming those changes to other systems. The database is the source of truth. The change log is the stream. Other systems consume the stream and stay in sync.

### How Kafka Fits In

Databases don't natively publish to Kafka. You need a tool. **Debezium** is the most popular. It reads the database's transaction log (MySQL binlog, PostgreSQL WAL, etc.) and publishes each change as an event to Kafka.

```
MySQL: INSERT into users (id, name) VALUES (1, 'Alice')
    │
    ▼ (Debezium reads the binlog)
Kafka Topic: "mysql.users"
    │  Message: { "op": "c", "after": { "id": 1, "name": "Alice" } }
    │
    ├──▶ Elasticsearch: Index the new user for search
    ├──▶ Redis: Add to cache
    ├──▶ Data Warehouse: Sync for analytics
    └──▶ Search Index: Update "Users named Alice" index
```

**Why is this powerful?** Your application writes to MySQL. Just MySQL. It doesn't know about Elasticsearch. It doesn't know about the cache. Debezium and Kafka handle the sync. Add a new consumer? Your application code doesn't change. The database doesn't change. You just add a consumer.

!!! info "Companies Using This Pattern"
    - **Netflix:** Database changes flow to Kafka via CDC. Search indices, caches, and data lakes stay in sync with the source of truth.
    - **Uber:** Trip data, driver data, rider data—changes in primary databases are captured and streamed. Multiple systems consume for different purposes.
    - **Financial services:** Banks use CDC to sync transactional databases with reporting systems, fraud detection, and regulatory reporting. Consistency is critical.

---

## 9. IoT Data Ingestion

### The Problem (Briefly)

Millions of sensors. Temperature sensors in warehouses. GPS trackers on trucks. Smart meters in homes. Heart rate monitors. They all send data. Constantly. A single smart city might have 10 million devices sending a reading every minute. That's 10 million messages per minute. Per topic. You need a system that can ingest this firehose, buffer it, and let downstream systems process at their own pace.

### How Kafka Solved It

Kafka was designed for this. Millions of messages per second. Persistent storage. Multiple consumers. IoT devices produce to Kafka. Stream processors consume for real-time alerts ("Temperature > 100° in Warehouse 3!"). Batch consumers store for historical analysis. Each use case gets the same data. Independently.

```
Sensor 1 (temperature) ──▶ ┐
Sensor 2 (humidity)    ──▶ ├──▶ Kafka ──▶ Real-time alerting ("Anomaly detected!")
Sensor 3 (GPS)         ──▶ │              Historical storage (data lake)
...                     ──▶ │              ML model training
Sensor 1,000,000       ──▶ ┘
```

---

## 10. Fraud Detection

### The Problem (Briefly)

A fraudulent transaction happens. You want to catch it *before* the money leaves. Or within seconds of it happening. Not in a batch job tomorrow. Real-time. The transaction flows through Kafka. A fraud detection service (often with an ML model) consumes it, scores it, and either allows or blocks. All in milliseconds.

```
Transaction event ──▶ Kafka ──▶ Fraud Detection Service
                                      │
                                      ├──▶ Score > 0.8 → Block, alert, DLT
                                      └──▶ Score < 0.8 → Allow, continue
```

Kafka Streams or Kafka with a fast consumer makes this possible. The key is low latency and the ability to maintain state (e.g., "This user has made 5 transactions in 2 minutes—suspicious").

---

## 11. Saga Pattern with Kafka

### What Is the Saga Pattern?

A **Saga** coordinates a multi-step business process across microservices. Each step can fail. If one step fails, you need to undo (compensate) previous steps. There's no distributed transaction. You use events and compensating actions.

### How It Handles Distributed Transactions

Instead of a 2PC (two-phase commit) across services, each service does its work and publishes an event. If a later step fails, it publishes a "compensating" event. Previous services listen and undo their work.

### Choreography vs Orchestration

| Style | How It Works |
|-------|--------------|
| **Choreography** | Each service listens to events and reacts. No central coordinator. Services react to "OrderPlaced," "PaymentCompleted," "PaymentFailed." |
| **Orchestration** | A central Saga orchestrator tells each service what to do. It tracks state. On failure, it sends compensating commands. |

### Simple Example: Order Saga

**Choreography style:**

1. Order Service publishes "OrderPlaced."
2. Payment Service consumes, charges card. Publishes "PaymentCompleted" or "PaymentFailed."
3. Inventory Service consumes "PaymentCompleted," reserves stock. Publishes "InventoryReserved" or "InventoryFailed."
4. If "InventoryFailed," Payment Service consumes it and issues a refund (compensating action).
5. Shipping Service consumes "InventoryReserved," creates shipment.

Kafka topics: `orders`, `payment-events`, `inventory-events`, `shipping-events`. Each service is a producer and consumer. Events drive the flow.

---

## 12. CQRS with Kafka

### What Is CQRS?

**CQRS = Command Query Responsibility Segregation.** Separate the write path (commands) from the read path (queries). Writes go to a command model. Reads come from a read model. They can be different shapes. Kafka enables this by carrying events from writes to read-model builders.

### How Kafka Enables It

1. **Write:** Application receives a command (e.g., "Place Order"). It writes to the database (or command store). It publishes an event to Kafka.
2. **Consume:** A consumer reads the event. It updates the read model (e.g., a denormalized table, Elasticsearch index, cache).
3. **Read:** Queries hit the read model. Fast. Optimized for reads. No joins on the write side.

### Flow: Command Topic → Consumer Updates Read Model

```
User: "Place Order" (command)
    │
    ▼
Order Service: Writes to DB, publishes to "order-events"
    │
    ▼
Kafka Topic: "order-events"
    │
    ▼
Read Model Consumer: Updates "orders_by_customer" table, search index, dashboard aggregates
    │
    ▼
User: "Show my orders" (query) → Reads from read model. Fast.
```

The write model stays normalized. The read model is denormalized, cached, or indexed. Kafka is the bridge.

---

## 13. Event Replay and Debugging

### How to Replay Events for Debugging

Kafka keeps messages. You can reset a consumer group's offset to an earlier point. The consumer will re-read messages. Useful for debugging: "What did we process at 2 AM when the bug happened?"

```bash
# Reset consumer group to beginning of topic
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group my-consumer --topic orders --reset-offsets --to-earliest --execute

# Reset to specific offset
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group my-consumer --topic orders --reset-offsets \
  --to-offset 1000:42 --execute
```

!!! warning "Stop consumers first"
    Stop the consumer group before resetting offsets. Otherwise, the consumer might overwrite your reset.

### Resetting Offsets to Investigate Production Issues

**Scenario:** A bug caused wrong data to be written. You fix the bug. Now you need to re-process events from last night.

1. Stop the consumer group.
2. Reset offsets to the timestamp before the bad data (e.g., `--to-datetime 2024-02-24T00:00:00`).
3. Restart consumers. They re-process. Ensure your consumer is idempotent so re-processing doesn't create duplicates.

!!! tip "Retention matters"
    If you need to replay, ensure topic retention is long enough. Default is 7 days. For critical topics, consider 30 days or more.

---

## Companies Using Kafka

| Company | Use Case | Specific Problem Solved |
|---------|----------|-------------------------|
| **LinkedIn** | Activity tracking, metrics, messaging | Had thousands of servers generating logs and metrics. Needed a system to ingest billions of events per day without losing data. Created Kafka to solve log aggregation and activity tracking at scale. |
| **Netflix** | Event streaming, recommendations | Needed to decouple hundreds of microservices. Playback events, A/B tests, and recommendations had to flow without blocking. Kafka ensures no data loss during chaos engineering experiments (intentionally killing servers). |
| **Uber** | Trip updates, driver matching, surge pricing | Needed real-time driver location and trip state across rider app, driver app, billing, and surge pricing. HTTP calls would not scale. Kafka streams location updates; each system consumes at its own pace. |
| **Airbnb** | Search ranking, event processing | Search and booking events needed to power the ranking algorithm in real time. Multiple consumers (search, notifications, analytics) without coupling. Kafka decouples producers from consumers. |
| **Spotify** | Log delivery, event-driven microservices | Needed to deliver playback and skip events to royalty calculation, recommendations, and analytics. One event, many consumers. Kafka replaced point-to-point integrations. |
| **Twitter/X** | Real-time tweet processing | Billions of tweets and engagement events. Needed a pipeline that could ingest, buffer, and fan out to timeline generation, analytics, and search. Kafka handles the firehose. |
| **Goldman Sachs** | Trade processing, risk calculation | Trade events must flow to risk systems in real time. Cannot lose a trade. Kafka provides durability and replay capability for regulatory compliance. |
| **Walmart** | Inventory tracking, order processing | Black Friday traffic spikes 10x. Order and inventory events had to flow without the order service blocking. Kafka decouples; each system scales independently during peak. |

---

## Quick Summary

| Use Case | What Kafka Does | Key Benefit |
|----------|-----------------|-------------|
| Event-Driven Architecture | Decouples microservices | Services don't block each other; add consumers without changing producers |
| Log Aggregation | Centralizes logs from many servers | One place to search; handles bursts; no lost logs |
| Real-Time Analytics | Enables live dashboards | Build current state from event stream; seconds, not minutes |
| Data Pipeline | Moves data between systems | Kafka Connect; one source, many destinations |
| Order Processing | Orchestrates multi-step workflows | Each step independent; no cascading failures |
| Activity Tracking | Records every user action | One event, many consumers; fast page loads |
| Notifications | Routes to email/SMS/push | Add channels without changing producers |
| CDC | Syncs database changes to other systems | Database is source of truth; Debezium captures changes |
| IoT | Ingests millions of device messages | Handles firehose; multiple consumers |
| Fraud Detection | Real-time transaction scoring | Millisecond latency; block before money leaves |

---

## Exercise

1. **For each use case above**, identify which one is most relevant to your current or target company. Write one paragraph explaining why.
2. **Pick one use case** and draw the Kafka architecture on paper. Include producers, topics, and at least 3 consumers.
3. **Think about:** What happens if the consumer is down for 1 hour? (Answer: Messages queue up in Kafka. The consumer stores its offset. When it comes back, it reads from where it left off. It catches up. No messages are lost—assuming retention is configured properly.)
4. **Challenge:** Your company has a monolith. It needs to send emails when orders are placed. Today it calls an Email API directly. How would you introduce Kafka? What's the migration path?

---

**Next up:** [Kafka with Spring Boot](kafka-with-springboot.md) - Write real Kafka code!
