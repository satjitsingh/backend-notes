# Producers & Consumers

## Introduction

Producers send messages. Consumers read them. It sounds simple. But the devil is in the details. How do you guarantee a message was received? How do you avoid duplicates? How do you ensure ordering? What happens when things fail?

This page walks you through every step. We'll use real-life analogies. We'll trace what happens behind the scenes. And we'll look at full code examples with line-by-line explanations.

---

# Producers: Sending Messages

## What is a Producer?

A **producer** is any application that sends messages to Kafka topics. When your Order Service creates an order, it produces an event. When your user-tracking service logs a click, it produces an event. The producer's job is simple: get the message to Kafka. That's it.

### Real-Life Analogy

You're sending postcards. You write the message. You put it in an envelope. You write the destination address (the topic). You drop it at the post office (Kafka). You might get a receipt (acknowledgment). Or you might not, depending on how you sent it. The producer doesn't wait for anyone to read the postcard. It just delivers.

---

## The Full Lifecycle of Sending a Message

Let's trace what happens from the moment you call `producer.send()` to the moment Kafka confirms receipt. Every step.

### Step 1: Serialize

Your message is a Java object. Or a Python dict. Or a JSON string. Kafka stores bytes. So the first step is **serialization**. Convert the key and value to byte arrays. Kafka provides serializers for String, Integer, and bytes. For custom objects, you use JSON or Avro serializers.

**What happens behind the scenes:** The producer calls your configured `Serializer`. For StringSerializer, it does `"hello".getBytes(UTF_8)`. The result is what gets sent over the network.

### Step 2: Partition

Kafka needs to know which partition to send the message to. If you provided a key, the partitioner hashes the key and does `hash % numPartitions`. Same key always goes to the same partition. If you didn't provide a key, the producer uses round-robin or a sticky partitioner (stays with a partition for a batch, then switches).

**What happens behind the scenes:** The producer has a `Partitioner` component. It runs before the message is sent. The result is a partition number. The producer now knows: "This message goes to Partition 3."

### Step 3: Batch

Sending one message at a time is inefficient. Network round-trips are expensive. So the producer **batches** messages. It collects messages for the same partition. Waits up to `linger.ms` (e.g., 5 milliseconds). Or until the batch is full (`batch.size`). Then it sends the whole batch in one network request.

**What happens behind the scenes:** Messages go into an in-memory buffer. Per partition. When the buffer has enough data or the linger time expires, the batch is sent. If you send 100 messages to Partition 0 in quick succession, they might all go in one or two batches. Much faster than 100 separate requests.

### Step 4: Send

The producer sends the batch to the broker that leads the target partition. Over the network. TCP. The broker receives it.

### Step 5: Acknowledge

The broker appends the messages to the log. Replicates to followers (if configured). Then sends back an acknowledgment. The producer receives it. The `send()` callback fires. Success.

If the broker returns an error (e.g., "Leader not available"), the producer retries. With idempotence enabled, retries don't create duplicates.

---

## Acknowledgments (acks): The Registered Mail Analogy

When you send a letter, you have options. Drop it in a postbox and hope (no confirmation). Hand it to the postman and get a nod (some confirmation). Or use registered mail with a signature (full confirmation). Kafka's `acks` setting is the same idea.

### acks=0: Dropping in a Postbox

You drop the letter in the postbox. You don't wait. You don't get a receipt. Maybe it gets delivered. Maybe it gets lost. You'll never know.

**In Kafka:** The producer sends the message and immediately considers it successful. It doesn't wait for the broker to respond. Fastest. But if the broker never receives it (network failure, broker down), you've lost the message. No retry. No way to know.

**Use case:** Metrics, logs. Data where losing a few messages is acceptable. Ultra-low latency.

### acks=1: Giving to the Postman

You hand the letter to the postman. He takes it. You see him take it. That's your confirmation. You don't know if it reached the sorting facility. Or the destination. But at least it left your hands.

**In Kafka:** The producer waits for the **leader** broker to acknowledge. The leader has appended the message to its log. Good. But the leader might crash before replicating to followers. If that happens, the message could be lost. Moderate safety.

**Use case:** General purpose. Good balance of speed and safety. Acceptable for many applications.

### acks=all (or acks=-1): Confirmed Delivery

You use registered mail. The recipient signs for it. You get a delivery confirmation. You know it arrived.

**In Kafka:** The producer waits for the leader AND all in-sync replicas to acknowledge. The message is written to multiple brokers. If the leader dies, a replica has the data. No loss.

**Use case:** Payments, orders, critical data. When you cannot afford to lose a message.

| Setting | Speed | Safety | When to Use |
|---------|-------|--------|-------------|
| acks=0 | Fastest | None | Logs, metrics |
| acks=1 | Fast | Moderate | General |
| acks=all | Slowest | Highest | Critical data |

!!! tip "Interview Insight"
    For critical data (payments, orders), use `acks=all`. For logs/metrics where some loss is okay, use `acks=1` or even `acks=0`.

---

## Message Keys: Why Order-123 Must Go to the Same Partition

### The Problem

Imagine an order processing system. You have three events for order #123:
1. Order created
2. Payment received
3. Order shipped

If these go to different partitions, they might be processed by different consumers. And in the wrong order. Consumer A might process "shipped" before "payment received." Chaos. You might ship before charging. Or update the wrong state.

### The Solution: Use the Order ID as the Key

Set `key = "order-123"` for all three messages. The hash of "order-123" always maps to the same partition. Say Partition 2. All three messages go to Partition 2. One consumer reads Partition 2. It gets them in order: created, paid, shipped. Correct.

```
Message 1: key="order-123", value="created"   → Partition 2
Message 2: key="order-123", value="paid"     → Partition 2
Message 3: key="order-123", value="shipped"  → Partition 2

Consumer assigned to Partition 2 reads in order. Perfect.
```

### When to Use Keys

- **Use keys** when you need ordering for a logical entity (user, order, session).
- **Don't use keys** (or use null) when you want even distribution and don't care about ordering. Round-robin gives you better load balancing.

---

## Batching: The Delivery Truck Analogy

Sending one message is like sending one item by truck. The truck drives to the warehouse. Drops off one box. Drives back. Expensive. Wasteful.

Batching is like filling the truck. You collect 100 boxes. Load the truck. One trip. Drop off 100 boxes. Much more efficient.

**In Kafka:** `batch.size` controls how many bytes to collect per partition before sending. `linger.ms` controls how long to wait for more messages. If you have low traffic, `linger.ms=5` means "wait up to 5ms to fill the batch." You trade a bit of latency for throughput.

```
linger.ms=0:  Send immediately. Low latency. Low throughput.
linger.ms=10: Wait 10ms. Batch might fill. Higher throughput. Slightly higher latency.
```

**How linger.ms and batch.size interact (visual):**

```
Time (ms)    0    1    2    3    4    5    6    7    8    9   10
            |----|----|----|----|----|----|----|----|----|----|
Messages:   m1   m2   m3        m4   m5        m6
            |________________________|
            Batch for Partition 0 (linger.ms=5, batch.size=16KB)

Scenario A: linger.ms=0
  m1 arrives → send immediately (batch of 1)
  m2 arrives → send immediately (batch of 1)
  Low latency. Many small batches. Low throughput.

Scenario B: linger.ms=5, batch.size=16KB
  m1 arrives at 0ms → wait
  m2 arrives at 1ms → wait
  m3 arrives at 2ms → wait
  5ms elapsed → send batch [m1, m2, m3]
  m4, m5, m6 arrive → wait until 10ms → send batch [m4, m5, m6]
  Fewer network round-trips. Higher throughput. Slightly higher latency.
```

The batch is sent when **either** condition is met: (1) `linger.ms` time has passed, or (2) `batch.size` bytes have been collected. Whichever comes first.

!!! info "Trade-off"
    For real-time systems (gaming, trading), use low linger. For bulk ingestion (logs, analytics), use higher linger and larger batches.

---

## Idempotent Producer: Preventing Duplicates on Retry

### The Problem

You send a message. Network glitch. The broker received it, but the acknowledgment got lost. The producer doesn't know. It retries. Sends the same message again. The broker receives it twice. Duplicate. For a payment, that could mean charging the customer twice.

### The Solution: enable.idempotence=true

With idempotence, the producer assigns a unique ID (PID) and a sequence number to each message. The broker tracks "I've already seen PID 5, sequence 42." If the producer retries, the broker says "Already have it." Stores only once. Returns success. No duplicate.

**Scenario:** Producer sends message. Network glitch. Producer retries. Broker: "Duplicate. Ignoring." Producer gets ack. Consumer sees the message once. Perfect.

!!! tip "Recommendation"
    Always use `enable.idempotence=true` for production. It's a no-brainer. Prevents duplicates from retries. Minimal overhead.

---

## Producer Error Handling

Not all errors are the same. Some are retriable. Some are not. Your error handler should treat them differently.

### Retriable vs Non-Retriable Errors

**Retriable:** The producer can retry. The message might succeed on the next attempt.

- `Leader Not Available` — partition leader is moving (rebalance). Wait and retry.
- `Not Leader For Partition` — same idea. Leader changed.
- `NetworkException` — temporary network glitch.
- `TimeoutException` — broker didn't respond in time. Message might have been received.

**Non-Retriable:** Retrying won't help. Fix the problem first.

- `RecordTooLargeException` — message exceeds `max.message.bytes`. Split or compress.
- `SerializationException` — your serializer failed. Bad data. Fix the data or serializer.
- `InvalidTopicException` — topic doesn't exist or has invalid name.
- `AuthenticationException` — wrong credentials. Won't fix by retrying.

### Custom Error Handler

```java
props.put("retries", 3);
props.put("retry.backoff.ms", 100);

// Custom callback: log non-retriable, retry retriable
producer.send(record, (metadata, exception) -> {
    if (exception == null) {
        log.info("Sent to partition {} offset {}", metadata.partition(), metadata.offset());
    } else {
        if (exception instanceof RetriableException) {
            log.warn("Retriable error, producer will retry: {}", exception.getMessage());
        } else {
            log.error("Non-retriable error, message lost: {}", exception.getMessage());
            // Maybe write to a dead-letter queue, alert, or persist for manual retry
            saveToDeadLetterQueue(record);
        }
    }
});
```

!!! tip "Best Practice"
    For critical data, use a custom callback. On non-retriable errors, write to a dead-letter topic or database. Don't silently drop messages.

---

## Producer Interceptors

Interceptors let you run code **before** a message is sent and **after** the broker acknowledges it. They're like middleware for the producer.

### What Are They?

An interceptor implements `ProducerInterceptor`. You can have multiple. They run in order.

```java
public class LoggingInterceptor implements ProducerInterceptor<String, String> {
    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        log.info("Sending to topic {} partition {} key {}", 
            record.topic(), record.partition(), record.key());
        return record;  // Return the record (possibly modified)
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception == null) {
            log.info("Acked: partition {} offset {}", metadata.partition(), metadata.offset());
        } else {
            log.error("Failed: {}", exception.getMessage());
        }
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
```

Register it:

```java
props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
    "com.example.LoggingInterceptor");
```

### Use Cases

- **Logging:** Log every message sent. Useful for debugging.
- **Metrics:** Count messages per topic. Send to Prometheus or StatsD.
- **Header injection:** Add tracing IDs (TraceId, SpanId) to every message for distributed tracing.
- **Encryption:** Encrypt the value in `onSend`, decrypt in consumer interceptor.
- **Validation:** Reject invalid messages before they're sent. Return null from `onSend` to skip sending.

!!! info "Order Matters"
    If you have multiple interceptors, they run in the order you list them. The first runs first on send, last on ack.

---

## What Can Go Wrong? Producer Edition

**Buffer full:** The producer has a buffer (`buffer.memory`). If you produce faster than the network can send, the buffer fills. `send()` blocks or throws. Slow down or increase buffer.

**Leader not available:** The partition leader might be moving (rebalance). Producer retries. With retries enabled, it usually succeeds eventually.

**Timeout:** Request times out. Broker might have received the message. Retry could duplicate. Use idempotence.

**Serialization error:** Your serializer throws. Message never sent. Handle the exception. Fix your data or serializer.

---

# Consumers: Reading Messages

## What is a Consumer?

A **consumer** is any application that reads messages from Kafka topics. It subscribes to topics. It polls for messages. It processes them. It commits offsets. Repeat.

### Real-Life Analogy

You go to your mailbox every morning. You check for new letters. You remember which ones you've already read (that's your offset). Tomorrow, you only read the new ones. You don't re-read yesterday's mail. The mailbox (Kafka) keeps everything. You decide where to start and how fast to read.

---

## The Poll Loop: What Does consumer.poll() Actually Do?

The consumer runs in a loop. `while (true) { records = consumer.poll(); process(records); commit(); }`

### What Happens When You Call poll()?

1. **Fetch:** The consumer sends a fetch request to the broker. "Give me messages from partition X, starting at offset Y." It can fetch from multiple partitions in one request.

2. **Broker responds:** The broker returns messages. Up to `max.partition.fetch.bytes` per partition. Up to `max.poll.records` total.

3. **Deserialize:** The consumer deserializes the bytes to objects. StringDeserializer, etc.

4. **Return:** You get a `ConsumerRecords` object. A collection of records. You iterate. Process. Done. Next poll.

**Important:** `poll()` is a pull. The consumer asks. The broker doesn't push. If there are no new messages, `poll()` returns empty. It doesn't block forever. It blocks up to the timeout you pass (e.g., 100ms). Then returns whatever it got (maybe nothing).

---

## Consumer Interceptors

Consumers have interceptors too. They run **before** you receive the records and **after** you process them (when you commit).

```java
public class MetricsInterceptor implements ConsumerInterceptor<String, String> {
    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
        // Runs before records are returned to your code
        log.info("Received {} records", records.count());
        return records;  // Can filter or modify
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // Runs when you commit
        log.info("Committed offsets: {}", offsets);
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
```

### Use Cases

- **Logging:** Log every batch received. Track consumption rate.
- **Metrics:** Measure lag, throughput, processing time. Send to monitoring.
- **Header extraction:** Read TraceId from headers. Set in MDC for logging.
- **Filtering:** Remove records you don't want to process (e.g., test data). Return a filtered `ConsumerRecords`.

---

## Serialization Deep Dive

Kafka stores bytes. Your app uses objects. Serialization converts between them. The choice matters for performance, compatibility, and schema evolution.

### String Serialization

**StringSerializer / StringDeserializer:** Simple. UTF-8 bytes. Human-readable in tools. No schema. Good for JSON strings, log lines.

```java
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
```

**When to use:** Prototypes, simple text, JSON stored as string. Cross-language (any client can read UTF-8).

### JSON Serialization

**Jackson, Gson:** Convert Java objects to JSON bytes. Flexible. No schema. But no backward compatibility guarantee. Add a field? Old consumers might break. Remove a field? New consumers might break.

```java
ObjectMapper mapper = new ObjectMapper();
byte[] bytes = mapper.writeValueAsBytes(order);
```

**When to use:** Internal services, rapid iteration. When you control producer and consumer and can deploy together.

### Avro Serialization

**Avro:** Schema-based. Binary format. Compact. Schema Registry stores the schema. Producer sends schema ID + bytes. Consumer fetches schema, deserializes. **Backward compatible.** Add optional field? Old consumer ignores it. Remove optional field? New consumer handles missing.

```java
// With Schema Registry
props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
props.put("schema.registry.url", "http://localhost:8081");
```

**When to use:** Event-driven systems, microservices, when producers and consumers evolve independently. Used by Confluent, LinkedIn, Uber.

### Protobuf Serialization

**Protobuf:** Like Avro. Schema-based. Binary. Very fast. Good tooling. Schema Registry supports it.

```java
props.put("value.serializer", "io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer");
```

**When to use:** High performance, strong typing, gRPC ecosystem. Google, many cloud-native apps.

### Comparison Table

| Format | Size | Speed | Schema | Cross-Language | Schema Evolution |
|-------|------|-------|--------|----------------|------------------|
| String | Large | Fast | No | Yes | N/A |
| JSON | Medium | Medium | No | Yes | Manual |
| Avro | Small | Fast | Yes (Registry) | Yes | Built-in |
| Protobuf | Small | Very fast | Yes (Registry) | Yes | Built-in |

!!! info "Schema Registry"
    Schema Registry (e.g., Confluent Schema Registry) stores schemas. Producers and consumers reference schemas by ID. Enables evolution: add optional fields, remove optional fields. Old and new clients work together.

---

## Backpressure: When the Consumer Can't Keep Up

### What Happens

The producer sends 10,000 messages per second. The consumer processes 1,000 per second. The consumer is 10x slower. Messages pile up in Kafka. **Consumer lag** grows. Lag = how many messages are waiting to be read.

```
Producer: 10,000 msg/sec ──▶ Kafka ──▶ Consumer: 1,000 msg/sec
                                    │
                                    └── Lag grows by 9,000/sec
```

If this continues, lag reaches millions. By the time the consumer catches up, the data might be stale. Or Kafka runs out of disk (if retention is long).

### How to Handle It

**1. Increase partitions and consumers.** If you have 1 partition and 1 consumer, that's your ceiling. Add 10 partitions. Run 10 consumers. 10 × 1,000 = 10,000 msg/sec. You keep up.

**2. Optimize processing.** Make the consumer faster. Batch database writes. Use async processing. Profile and remove bottlenecks.

**3. Batch processing.** Instead of processing one message at a time, fetch a batch. Process in parallel (thread pool). Commit after the batch. Higher throughput.

**4. Scale out.** Add more consumer instances. Kafka assigns partitions. More consumers = more parallelism. Up to the number of partitions.

**5. Accept the lag.** For non-real-time use cases (analytics, reporting), lag might be OK. The consumer eventually catches up. Monitor lag. Alert if it exceeds a threshold.

!!! warning "Partition Limit"
    You can't have more consumers than partitions. If you have 6 partitions, 6 consumers can run in parallel. A 7th consumer sits idle. To add more parallelism, add more partitions first.

---

## Offset Commit: The Reading-a-Book Analogy

You're reading a book. You put a bookmark at page 50. That's your offset. "I've read through page 50." Tomorrow you start at page 51. You don't re-read 1-50.

**In Kafka:** After processing messages, you commit the offset. "I've processed through offset 50." Kafka stores this. If you restart, you say "Give me messages from offset 51." You resume. No re-processing.

**Where is the offset stored?** In Kafka itself. There's an internal topic `__consumer_offsets`. Your consumer group's offsets live there. When you commit, you're writing to that topic.

---

## Auto-Commit vs Manual Commit: The Data Loss Risk

### Auto-Commit (enable.auto.commit=true)

Kafka commits for you. Every `auto.commit.interval.ms` (default 5 seconds), it commits the latest offset you've received. You don't control when.

**The risk:** You poll. You get messages 1-10. You start processing. Message 5. Message 6. Crash. You never finished. But auto-commit might have already run after the poll. It committed offset 11. "I've processed through 10." On restart, you start at 11. Messages 5-10? Never fully processed. But Kafka thinks you did. **Data loss.** You skipped them.

Or the opposite: You process 1-10. Before auto-commit runs, you crash. On restart, no commit. You get 1-10 again. **Duplicate processing.** You might charge someone twice.

### Manual Commit (enable.auto.commit=false)

You call `consumer.commitSync()` or `consumer.commitAsync()` after processing. You control exactly when.

**Safe pattern:** Process message. Then commit. If you crash after process but before commit, you'll reprocess. Duplicate. But at least you don't skip. That's at-least-once. Make your processing idempotent to handle duplicates.

**Dangerous pattern:** Commit before process. If you crash after commit but before process, you've lost the message. Never processed. At-most-once.

!!! warning "Common Mistake"
    Never commit before processing if you need to ensure the message is handled. Commit after. Accept that you might reprocess on crash. Design for idempotency.

---

## Consumer Rebalancing: Step by Step

When a consumer joins or leaves a group, Kafka **rebalances**. Partitions are reassigned. Let's walk through it.

### Consumer Leaves (Crashes)

**Before:** C1→P0, C2→P1, C3→P2.

**C3 crashes.** Stops sending heartbeats. Kafka waits `session.timeout.ms` (default 45 seconds). Still no heartbeat. C3 is dead.

**Rebalance starts.** The group coordinator (a broker) triggers a rebalance. All consumers are told to stop fetching. They revoke their partitions.

**New assignment:** C1 and C2 are left. Partitions P0, P1, P2 must be assigned to 2 consumers. C1 gets P0 and P2. C2 gets P1. (Or similar. The assignor decides.)

**Consumers resume.** C1 fetches from P0 and P2. C2 fetches from P1. They resume from last committed offsets. Work continues.

**Pause:** During rebalance, consumption pauses. Usually a few seconds. For high-throughput systems, this can cause a backlog.

### New Consumer Joins

**Before:** C1→P0, P1. C2→P2.

**C3 joins.** Sends a join group request.

**Rebalance.** Partitions redistributed. C1→P0. C2→P1. C3→P2. Better balance.

**Resume.** Everyone fetches. C2 and C1 gave up some partitions. They might need to seek to the correct offset if they had in-memory state.

---

## Delivery Semantics: Payment Processing Scenario

Imagine you're processing payments. A message says "Charge user $50." What happens if you process it twice? Or not at all?

### At-Most-Once

**Meaning:** Process 0 or 1 time. Never more. Might skip.

**How:** Commit before processing. If you crash after commit, you never process. Message lost.

**Payment scenario:** You commit. Then you charge. Crash before charge. User was never charged. You lost the payment. Bad.

**When to use:** When duplicates are worse than loss. Rare. Maybe for "decrement inventory" where double-decrement is terrible.

### At-Least-Once

**Meaning:** Process 1 or more times. Never skip. Might duplicate.

**How:** Process. Then commit. If you crash after process but before commit, you'll reprocess. Duplicate.

**Payment scenario:** You charge. Then commit. Crash before commit. Restart. You charge again. User charged twice. Bad. Unless you make it idempotent: "If payment ID X already processed, skip." Check before charging.

**When to use:** Most common. With idempotent consumers. Safe and practical.

### Exactly-Once

**Meaning:** Process exactly 1 time. No skip. No duplicate.

**How:** Kafka Transactions. Producer and consumer participate. Atomic. Complex.

**Payment scenario:** Charge and commit in one transaction. Crash? Rollback. No duplicate. No loss. Perfect. But harder to implement.

!!! tip "Interview Insight"
    "In most systems, I use **at-least-once** delivery with **idempotent consumers**. Making the consumer idempotent (e.g., checking if order already processed) is simpler than exactly-once semantics."

---

## Full Java Code Examples

### Producer: Line-by-Line Explanation

```java
// Step 1: Configure the producer
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");  // Where to find Kafka
props.put("key.serializer",
    "org.apache.kafka.common.serialization.StringSerializer");  // How to convert key to bytes
props.put("value.serializer",
    "org.apache.kafka.common.serialization.StringSerializer");  // How to convert value to bytes
props.put("acks", "all");  // Wait for all replicas. Safest.
props.put("enable.idempotence", "true");  // Prevent duplicates on retry

// Step 2: Create the producer
KafkaProducer<String, String> producer = new KafkaProducer<>(props);

// Step 3: Create a record (topic, key, value)
// Key "order-123" ensures all order-123 events go to same partition
ProducerRecord<String, String> record = new ProducerRecord<>(
    "orders",           // topic
    "order-123",        // key (use for ordering!)
    "Order placed"      // value
);

// Step 4: Send (async with callback)
producer.send(record, (metadata, exception) -> {
    if (exception == null) {
        // Success! Message made it to Kafka.
        System.out.println("Sent to partition " + metadata.partition() +
            " offset " + metadata.offset());
    } else {
        // Failure. Log it. Maybe retry.
        exception.printStackTrace();
    }
});

// Step 5: Flush and close (important: don't forget!)
producer.flush();  // Wait for in-flight messages
producer.close();
```

**Line 1-8:** Configuration. Bootstrap servers tell the producer where to find Kafka (it will discover the rest of the cluster). Serializers convert your objects to bytes. acks=all and idempotence for safety.

**Line 11:** Create the producer. It opens connections to Kafka.

**Line 14-18:** ProducerRecord is the message. Topic, key, value. The key is crucial for ordering. All "order-123" events go to the same partition.

**Line 21-29:** send() is asynchronous. It returns immediately. The callback runs when the broker responds. Check for exceptions. In production, you'd retry or alert.

**Line 32-33:** flush() waits for all buffered messages to be sent. close() releases resources. Without flush, you might exit before messages are actually delivered.

### Consumer: Line-by-Line Explanation

```java
// Step 1: Configure the consumer
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "order-processor");  // Consumer group. Required!
props.put("key.deserializer",
    "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer",
    "org.apache.kafka.common.serialization.StringDeserializer");
props.put("auto.offset.reset", "earliest");  // If no offset, start from beginning
props.put("enable.auto.commit", "false");    // We'll commit manually

// Step 2: Create consumer and subscribe
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Collections.singletonList("orders"));  // Subscribe to topic

// Step 3: The poll loop
while (true) {
    // Poll for messages. Blocks up to 100ms. Returns batch of records.
    ConsumerRecords<String, String> records =
        consumer.poll(Duration.ofMillis(100));

    // Process each record
    for (ConsumerRecord<String, String> record : records) {
        System.out.println("Key: " + record.key() +
            " Value: " + record.value() +
            " Partition: " + record.partition() +
            " Offset: " + record.offset());

        // Your business logic here. Process the order. Send email. etc.
        // processOrder(record.value());  // Implement your processing logic
    }

    // Commit after processing. Safe: we've handled all messages.
    if (!records.isEmpty()) {
        consumer.commitSync();  // Blocking. Waits for commit to complete.
    }
}
```

**Line 1-10:** Configuration. group.id is required. Consumers in the same group share partitions. enable.auto.commit=false means we commit manually. Safer.

**Line 13-14:** Create consumer. Subscribe to "orders". The consumer will be assigned partitions by the group coordinator.

**Line 17-18:** poll() fetches messages. Duration.ofMillis(100) means "block up to 100ms if no messages, then return." You might get 0 or 1000 records. Depends on what's available.

**Line 21-28:** Iterate over records. Each has key, value, partition, offset. Process them. Do your work.

**Line 31-33:** After processing, commit. commitSync() blocks until Kafka confirms. Now if we crash, we'll resume after the last committed offset. No skips. Might have duplicates if we crash between process and commit—design for idempotency.

---

## Common Production Issues

**Consumer lag growing:** Your consumers can't keep up. Add more consumers (up to partition count). Or optimize processing. Or add partitions and consumers.

**Rebalance storms:** Consumers constantly joining/leaving. Rebalances every few seconds. Check session.timeout. Increase if your processing is slow. Or use max.poll.interval.ms—if processing takes longer than this, the consumer is kicked out. Increase it if you have slow processing.

**Duplicate processing:** At-least-once means duplicates. Make consumers idempotent. Check "have I already processed this?" before doing work. Use a unique ID in the message. Store processed IDs in a DB.

**Lost messages:** At-most-once, or commit-before-process. Fix: commit after process. Use acks=all on producer.

**OOM (Out of Memory):** Fetching too many records. Reduce max.poll.records. Or process in smaller batches.

**Coordinator not available:** The broker that manages your consumer group is moving. Wait. Retry. Usually transient.

---

## Quick Summary

| Concept | Producer | Consumer |
|---------|----------|----------|
| Role | Sends messages | Reads messages |
| Key config | acks, retries, batch.size | group.id, auto.offset.reset |
| Ordering | Use keys for ordering | One partition per consumer in group |
| Safety | acks=all + idempotent | Manual commit + idempotent processing |
| Scaling | Add more partitions | Add more consumers (up to partition count) |

---

## Exercise

1. Write a Java producer that sends 10 messages with key = "user-1".
2. Verify all 10 messages go to the same partition.
3. Start 2 consumers in the same group on a 3-partition topic. Observe partition assignment.
4. Kill one consumer. Watch rebalancing happen.
5. Reset offset to `earliest` and re-read all messages.

---

**Next up:** [Scaling Kafka](kafka-scaling.md) - Handle millions of messages.
