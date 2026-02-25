# Kafka Basics

## What is Apache Kafka?

**Apache Kafka** is a distributed event streaming platform. That's a mouthful. Let's break it down.

Think of it as a super-powered messaging system that can handle millions of messages per second. But it's not just a simple message queue. Kafka stores messages on disk. It keeps them for days or weeks. Multiple applications can read the same messages independently, at their own pace. And when you're done, you can go back and read old messages again. That's the "streaming" part.

Why does this matter? In the old days, when you had an application that needed to send data to another application, they'd talk directly to each other. Like a phone call. If the other guy wasn't ready to pick up, the call failed. Kafka changes that. It's like leaving a voicemail. Except the voicemail never gets deleted. And anyone who needs to listen can listen whenever they want.

Kafka was created at LinkedIn in 2011. They had a problem: hundreds of systems needed to share data. User clicks, page views, search queries, profile updates. Connecting everything directly would have been a mess. So they built Kafka as a central nervous system. A single place where all events flow. Today, Kafka powers the data pipelines of Uber, Netflix, Airbnb, and thousands of other companies.

---

## The Newspaper Analogy: Understanding Kafka Through a Familiar World

Let's use a real-world analogy that will help everything click. Imagine a **massive newspaper company**.

### Reporters Write Stories (Producers)

Reporters are out in the field. They witness events. A football match. A political debate. A stock market crash. They write their stories and send them to the newspaper office. They don't care who reads them or when. They just deliver the news. In Kafka, these reporters are **producers**. Applications that create events and send them to Kafka.

### The Newspaper Office Receives and Stores (Kafka)

The newspaper office receives stories from every reporter. It doesn't throw them away after printing once. It organizes them by section: Sports, Politics, Business, Entertainment. Each section has its own filing cabinet. When a story arrives, it goes into the right cabinet. The office keeps copies for weeks. If someone wants to read yesterday's sports section, they can. In Kafka, the office is the **Kafka cluster**. The sections are **topics**. The filing cabinets are **partitions**.

### Subscribers Read the News (Consumers)

People subscribe to the newspaper. Some read only Sports. Some read only Business. Some read everything. They read at their own pace. One person might read the morning edition at 7 AM. Another at 10 AM. The newspaper doesn't care. It's already printed. It's waiting. In Kafka, these readers are **consumers**. Applications that read messages from topics. They can read at any time. They can read old messages. Multiple consumers can read the same news independently.

!!! info "The Key Insight"
    The newspaper doesn't deliver the paper to your door and then burn the original. It keeps the master copy. Anyone who subscribes can get a copy. That's the fundamental difference between Kafka and traditional message queues.

---

## The Problem Kafka Solves: A Before/After Story

Let's walk through a real scenario. Imagine you run an **e-commerce company** called ShopFast.

### The Before: A Tangled Mess

You have an Order Service. When someone places an order, it needs to:
1. Charge the customer (Payment Service)
2. Reserve inventory (Inventory Service)
3. Send confirmation email (Email Service)
4. Update analytics dashboard (Analytics Service)
5. Trigger loyalty points (Loyalty Service)

So your Order Service makes five API calls. Direct. One after another.

```
Order Service → Payment Service (wait for response)
Order Service → Inventory Service (wait for response)
Order Service → Email Service (wait for response)
Order Service → Analytics Service (wait for response)
Order Service → Loyalty Service (wait for response)
```

**What happens when Email Service is down?** The entire order fails. Even though payment went through and inventory was reserved. Customer sees an error. You lose the sale.

**What happens when you add a new service?** Say you want to add a Fraud Detection Service. You have to modify the Order Service. Add another API call. Deploy it. Every time you add a consumer of order data, you touch the producer. That's fragile.

**What happens when Analytics Service is slow?** It blocks the whole flow. Order Service waits. Customer waits. Timeout.

### The After: Kafka as the Central Hub

Now you introduce Kafka. The Order Service does one thing: it publishes an event. "Order #12345 was placed." That's it. It doesn't care who reads it.

```
Order Service → Kafka (topic: "orders") → Done!

Kafka holds the message. Meanwhile:
  Payment Service subscribes to "orders" → reads → charges customer
  Inventory Service subscribes to "orders" → reads → reserves stock
  Email Service subscribes to "orders" → reads → sends confirmation
  Analytics Service subscribes to "orders" → reads → updates dashboard
  Loyalty Service subscribes to "orders" → reads → adds points
```

**Email Service is down?** No problem. The message sits in Kafka. When Email Service comes back online, it reads from where it left off. It catches up. No data lost.

**Want to add Fraud Detection?** Just build a new consumer. Subscribe to "orders". No changes to Order Service. No redeployment of existing services. That's **decoupling**.

**Analytics is slow?** Doesn't matter. It reads at its own pace. Order Service doesn't wait. Payment doesn't wait. Everyone is independent.

!!! tip "Why Should You Care?"
    Decoupling means your system can evolve. Add new features without breaking old ones. Scale services independently. Fail in isolation. Kafka is the backbone that makes this possible.

---

## Key Concepts Explained

### Event (Message)

An **event** is a record of something that happened. It's immutable. It's a fact. "User X placed order Y at time Z." That's it. You can't change it. You can only add new events.

Think about it this way: when you check your bank statement, you see a list of transactions. Each transaction is an event. "On Feb 25, you spent $50 at Coffee Shop." That happened. It's recorded. You can't edit it. You can only add new transactions.

In Kafka, an event is typically a key-value pair with optional metadata. Here's a real example:

```json
{
  "eventType": "ORDER_PLACED",
  "orderId": "12345",
  "userId": "67890",
  "amount": 2500,
  "timestamp": "2026-02-25T10:00:00"
}
```

### Producer

A **producer** is any application that sends messages to Kafka. It creates events and publishes them to topics.

Real-world example: Your mobile app's backend. When a user adds an item to cart, it produces a "cart_updated" event. When they checkout, it produces an "order_placed" event. The producer doesn't wait for anyone to process it. It fires and forgets (well, it gets an acknowledgment from Kafka, but it doesn't wait for consumers).

### Consumer

A **consumer** is any application that reads messages from Kafka. It subscribes to one or more topics and processes messages.

Real-world example: Your email service. It subscribes to the "orders" topic. Every time a new order event is published, it reads it and sends a confirmation email. It might be slow. It might process 10 messages per second while the producer sends 1000 per second. It doesn't matter. Kafka buffers. The consumer catches up eventually.

### Topic

A **topic** is a category or channel for messages. Think of it as a named stream. All order events go to the "orders" topic. All payment events go to the "payments" topic.

```
Topic: "orders"        → Order placed, order cancelled, order shipped
Topic: "payments"      → Payment received, payment failed, refund issued
Topic: "notifications" → Email sent, SMS sent, push notification sent
```

Producers write to topics. Consumers read from topics. You create topics before you use them. You can have as many as you need.

### Broker

A **broker** is a Kafka server. One physical (or virtual) machine running Kafka. A Kafka cluster typically has multiple brokers—three, five, ten. That's for reliability. If one broker dies, the others keep running.

Think of brokers as the actual filing cabinets in our newspaper office. Each broker holds some of the data. Together, they form the complete storage system.

---

## How Kafka is Different from a Database

This confuses beginners. "Why not just use a database? Can't I insert a row and have other services read it?"

**Databases are for storing state.** You query them. "What's the current balance of user 123?" You get the latest value. You update. You delete. Databases are optimized for random access and updates.

**Kafka is for streaming events.** You append. You never update or delete (well, Kafka has log compaction, but that's advanced). You read sequentially. Kafka is optimized for high-throughput writes and sequential reads. Millions of messages per second.

Think about it this way: A database is like a filing cabinet where you can pull out any file, edit it, and put it back. Kafka is like a conveyor belt. Things move in one direction. You can read from any point on the belt, but you don't go back and change what's already passed.

!!! example "When to Use Which"
    Use a database when you need to answer "What is the current state?" Use Kafka when you need to answer "What happened?" and "What happened next?" Event sourcing, real-time analytics, and log aggregation are Kafka territory. User profiles, order history lookups, and inventory counts are database territory.

---

## How Kafka is Different from an API Call

When Service A calls Service B via REST API, it's a **synchronous request-response**. A sends a request. B processes it. B sends a response. A waits. If B is slow, A waits. If B is down, A fails.

When Service A produces to Kafka and Service B consumes, it's **asynchronous**. A sends a message. Kafka acknowledges. A moves on. B reads whenever it's ready. A and B never talk directly. They're decoupled.

**API calls are like phone calls.** You need both parties to be available at the same time. **Kafka is like email.** You send. The recipient reads when they can. You don't need to be online at the same time.

---

## Where is Kafka Used? Real Company Examples

### Uber

Uber has millions of rides. Every ride generates events: ride requested, driver assigned, ride started, ride ended, payment processed. These events flow through Kafka. The ride-matching service uses them. The pricing service uses them. The fraud detection system uses them. The analytics team uses them. Kafka is the central pipeline. Without it, connecting every service to every other service would be impossible.

### Netflix

Netflix uses Kafka for real-time activity streams. Every play, pause, and rewind. Every recommendation click. Every search. This data flows through Kafka to multiple systems. The recommendation engine uses it to improve suggestions. The content team uses it to understand what's popular. The billing system uses it for usage-based plans. One event stream, many consumers.

### LinkedIn

LinkedIn invented Kafka. They use it for activity tracking. Every profile view, connection request, job application, and content feed update. They process billions of events per day. Kafka handles the ingestion. Downstream systems process at their own pace.

!!! info "The Pattern"
    Kafka shines when you have one source of truth (the event stream) and many consumers (different services with different needs). Write once, read many times.

---

## Installing Kafka

### Using Docker Compose (Recommended)

The easiest way to run Kafka locally is with Docker. Create a `docker-compose.yml` file:

```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

Run it:

```bash
docker-compose up -d
```

### Troubleshooting Tips

**"Connection refused" when connecting to localhost:9092**

- Wait 30-60 seconds after starting. Kafka takes time to start.
- Check Kafka is running: `docker ps` (both zookeeper and kafka containers should be "Up")
- On Windows, ensure Docker Desktop has enough memory (4GB minimum)

**"Topic does not exist"**

- Kafka doesn't auto-create topics by default in some configs. Create the topic manually first (see commands below).

**Port 9092 already in use**

- Another Kafka instance might be running. Stop it or change the port in docker-compose: `"9093:9092"` and use 9093 in your commands.

**Zookeeper connection errors**

- Ensure Zookeeper starts before Kafka. The `depends_on` helps, but Kafka might start before Zookeeper is ready. Use `docker-compose up -d` and wait a minute before using Kafka.

---

## Your First Kafka Experience: A Guided Tutorial

Let's walk through your first Kafka interaction step by step. You'll create a topic, send messages, and read them.

### Step 1: Find Your Kafka Container

```bash
docker ps
```

Look for the container running `cp-kafka`. Note the container ID or name (e.g., `backend-kafka-1`). **Replace `<kafka-container>` in the commands below with your actual container name.**

### Step 2: Create a Topic

```bash
docker exec -it <kafka-container> kafka-topics --create \
  --topic my-first-topic \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1
```

**Expected output:** `Created topic my-first-topic.`

**What this does:** You're telling Kafka to create a new category called "my-first-topic". One partition (we'll learn about partitions later). One replica (fine for local dev).

### Step 3: List Topics (Verify)

```bash
docker exec -it <kafka-container> kafka-topics --list \
  --bootstrap-server localhost:9092
```

**Expected output:** You should see `my-first-topic` in the list.

### Step 4: Start a Consumer (Terminal 1)

Open a terminal. Run:

```bash
docker exec -it <kafka-container> kafka-console-consumer \
  --topic my-first-topic \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

**What this does:** This starts a consumer that reads from the beginning of the topic. It will block and wait for messages. Leave it running.

### Step 5: Send Messages (Terminal 2)

Open a **second** terminal. Run:

```bash
docker exec -it <kafka-container> kafka-console-producer \
  --topic my-first-topic \
  --bootstrap-server localhost:9092
```

You'll see a `>` prompt. Type a message and press Enter:

```
> Hello Kafka!
> This is my first message
> Learning event streaming
```

**Expected output:** In Terminal 1 (the consumer), you should see each message appear as you type it in Terminal 2. That's real-time streaming!

### Step 6: The Retention Test

Stop the consumer (Ctrl+C in Terminal 1). Send 3 more messages in Terminal 2:

```
> Message after consumer stopped
> Kafka still stores these
> Consumer will see them when it restarts
```

Now restart the consumer with `--from-beginning`:

```bash
docker exec -it <kafka-container> kafka-console-consumer \
  --topic my-first-topic \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

**Expected output:** You should see ALL messages. The old ones and the new ones. Kafka didn't delete them when the consumer stopped. That's retention. That's the power of Kafka.

---

## Basic Kafka Commands Reference

### Create a topic

```bash
kafka-topics --create \
  --topic orders \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

### List topics

```bash
kafka-topics --list --bootstrap-server localhost:9092
```

### Describe a topic (see partitions, config)

```bash
kafka-topics --describe --topic orders --bootstrap-server localhost:9092
```

### Produce messages (send)

```bash
kafka-console-producer \
  --topic orders \
  --bootstrap-server localhost:9092
> Order 1 placed
> Order 2 placed
```

### Consume messages (read)

```bash
kafka-console-consumer \
  --topic orders \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

---

## Common Mistakes Beginners Make

**Mistake 1: Expecting Kafka to work like a queue**

Kafka is not a traditional queue. Messages aren't deleted after consumption. Multiple consumers can read the same message. If you want "one consumer reads, message gone" behavior, use a single consumer group and understand that Kafka still retains the data.

**Mistake 2: Creating too many topics**

Topics are cheap, but each topic has overhead. Don't create a topic per user or per order. Create topics for logical event types: orders, payments, user-events. Use keys to distinguish within a topic.

**Mistake 3: Not setting retention**

By default, Kafka keeps messages for 7 days. For a development topic with test data, you might want 1 hour. For audit logs, you might want 90 days. Set `retention.ms` when creating topics.

**Mistake 4: Trying to use Kafka for request-response**

Kafka is for fire-and-forget event streaming. If you need to send a request and get a response (e.g., "Is this user valid?"), use an API. Kafka is for "something happened, whoever cares can react."

**Mistake 5: Starting the consumer before the producer**

This is fine! Actually, it's the opposite. Beginners often think the producer must run first. In Kafka, you can start the consumer first. It will wait. When the producer sends, the consumer will receive. Order doesn't matter.

---

## Kafka vs Traditional Message Queue

| Feature | Kafka | RabbitMQ / ActiveMQ |
|---------|-------|---------------------|
| Message retention | Keeps messages (configurable days) | Deletes after consumption |
| Consumer model | Pull (consumer asks for messages) | Push (broker sends to consumer) |
| Throughput | Very high (millions/sec) | Moderate (thousands/sec) |
| Replay | Can re-read old messages | Cannot (already deleted) |
| Ordering | Ordered within a partition | Ordered within a queue |
| Use case | Event streaming, logs, analytics | Task queues, RPC |

!!! tip "Interview Insight"
    "Kafka is designed for **high-throughput event streaming** with replay capability. Traditional queues are better for **task distribution** with guaranteed delivery."

---

## Quick Summary

| Concept | Explanation |
|---------|-------------|
| Kafka | Distributed event streaming platform |
| Producer | Sends messages to Kafka |
| Consumer | Reads messages from Kafka |
| Topic | A named stream of messages |
| Broker | A Kafka server instance |
| Event | A record of something that happened |
| Key Difference | Kafka retains messages; queues delete after reading |
| Default Port | 9092 |
| Why Kafka | Decoupling, scalability, fault tolerance, replay |

---

## Exercise

1. Run Kafka using Docker Compose.
2. Create a topic called `my-events`.
3. Open two terminals: one for producer, one for consumer.
4. Send 5 messages and watch them appear in the consumer.
5. Stop the consumer, send 3 more messages, restart the consumer. Do you see all messages?

---

**Next up:** [Kafka Architecture](kafka-architecture.md) - Understand how Kafka is built internally.
