# Common Mistakes

## Why This Page Exists

These are the mistakes that every developer makes when starting with Kafka. Some cause subtle bugs that only show up under load. Others cause production outages. Learning them here saves you from learning them the hard way at 2 AM with your manager on the phone.

---

## Mistake 1: Not Using Message Keys When Ordering Matters

### The Mistake

You send order events without a key:

```java
producer.send(new ProducerRecord<>("orders", event.toJson()));
```

Events for the same order land in different partitions. The consumer processes "PAYMENT_SUCCESS" before "ORDER_CREATED."

### Why It's Wrong

Without a key, Kafka uses round-robin partitioning. Related events scatter across partitions. Since ordering is only guaranteed within a partition, events arrive out of order.

### What To Do

Use the entity ID as the key:

```java
producer.send(new ProducerRecord<>("orders",
    event.getOrderId(),    // Key: same order always → same partition
    event.toJson()));
```

Now all events for order #100 go to the same partition. ORDER_CREATED always comes before PAYMENT_SUCCESS.

### What Actually Happens in Production

A fintech startup shipped their order pipeline without keys. In dev, with low volume, everything seemed fine. In production during Black Friday, the payment service processed "PAYMENT_SUCCESS" for order #4521 before "ORDER_CREATED." The system tried to charge a customer for an order that didn't exist yet. Support got flooded with "ghost charge" complaints. They spent two days debugging before someone noticed the event order in the logs. Adding keys and redeploying fixed it — but the damage to customer trust was done.

---

## Mistake 2: Too Many or Too Few Partitions

### Too Few Partitions

You create a topic with 1 partition. You can only have 1 consumer. When traffic grows 10x, you can't scale consumption.

### Too Many Partitions

You create a topic with 200 partitions "just in case." Each partition uses file handles, memory, and replication bandwidth. The cluster is stressed for a topic that gets 100 messages a day.

### What To Do

Start with a sensible number. A good formula:

```
Partitions = max(expected consumer count, 6)
```

For most services, 6-12 partitions is a good starting point. You can always increase (but never decrease).

### What Actually Happens in Production

A team started with 1 partition for their "notifications" topic. When they launched a big marketing campaign, 50,000 users signed up in an hour. One consumer couldn't keep up. Lag hit 40,000 messages. Users waited 6 hours for their welcome email. Another team went the opposite way: 500 partitions for a topic that got 10 messages per minute. Broker memory usage spiked. Rebalancing took 30 seconds every time a consumer restarted. Both teams learned the hard way that partition count matters.

---

## Mistake 3: Using Auto-Commit for Critical Data

### The Mistake

You leave the default `enable.auto.commit=true`. The consumer auto-commits every 5 seconds. If it crashes between auto-commit and finishing processing, messages are either lost or duplicated in unpredictable ways.

### Why It's Wrong

Auto-commit is time-based, not processing-based. It has no idea whether your consumer actually finished processing the messages.

### What To Do

Disable auto-commit and commit manually after successful processing:

```properties
enable.auto.commit=false
```

```java
for (ConsumerRecord<String, String> record : records) {
    processMessage(record);
}
consumer.commitSync();  // Commit only after all messages are processed
```

### What Actually Happens in Production

A logistics company used auto-commit for their shipment tracking events. One night, a consumer processed 200 messages, then crashed at message 198 — right before the 5-second auto-commit timer fired. On restart, it re-read from offset 150 (the last auto-commit). It processed 48 messages again. Some shipments got duplicate "out for delivery" notifications. Others got "delivered" before "out for delivery." The tracking timeline was a mess. Customers called asking "Where is my package?" The ops team spent hours correlating logs to understand the duplicate events.

---

## Mistake 4: Not Making Consumers Idempotent

### The Mistake

Your consumer charges a payment every time it receives a "PAYMENT" event. Due to a rebalance, the same event is processed twice. Customer is charged twice.

### Why It's Wrong

With at-least-once delivery (the default in production), duplicates are expected. If your consumer can't handle being called twice with the same data, you will have bugs.

### What To Do

Before processing, check if the message was already handled:

```java
public void processPayment(PaymentEvent event) {
    if (paymentRepository.existsByTransactionId(event.getTransactionId())) {
        log.info("Already processed {}, skipping", event.getTransactionId());
        return;
    }
    paymentRepository.save(event);
    chargeCustomer(event.getAmount());
}
```

### What Actually Happens in Production

An e-commerce company had a consumer that applied loyalty points when a purchase event arrived. No idempotency check. During a deployment, a rebalance caused the same "PURCHASE_COMPLETE" event to be processed twice. Customers woke up to double points. Some cashed them in before the team noticed. The finance team had to manually reverse thousands of point transactions. The CTO sent a company-wide email: "We gave away $12,000 in duplicate loyalty points. From now on, every consumer checks before processing."

---

## Mistake 5: Ignoring Consumer Lag

### The Mistake

Your consumer is running. No errors in the logs. Everything looks fine. But consumer lag has been growing for 3 days. You have 500,000 unprocessed messages.

### Why It's Dangerous

Consumer lag means your consumer is falling behind. Users might see delayed notifications, stale data, or missing updates. By the time you notice, the backlog is so large it takes hours to catch up.

### What To Do

Monitor consumer lag and set alerts:

```bash
kafka-consumer-groups --describe --group my-group --bootstrap-server localhost:9092
```

Set alerts in your monitoring system:

- **Warning:** Lag > 1,000 messages for more than 5 minutes
- **Critical:** Lag > 10,000 messages for more than 15 minutes

When lag grows: add more consumers (up to partition count) or optimize processing speed.

### What Actually Happens in Production

A real-time dashboard team had no lag monitoring. Their consumer processed analytics events. One day, a bug caused each message to take 2 seconds instead of 50ms. Nobody noticed for 3 days. Lag grew to 2 million messages. When they finally caught it, catching up took 18 hours. During that time, the dashboard showed data from 3 days ago. The product manager thought the feature was broken. They added lag alerts the next week. Now they get paged if lag exceeds 1,000 for more than 5 minutes.

---

## Mistake 6: Replication Factor of 1 in Production

### The Mistake

You create topics with `--replication-factor 1` because it was quick during development. You deploy to production the same way.

### Why It's Dangerous

Replication factor 1 means zero fault tolerance. If the broker holding that partition dies, the data is gone. Permanently. No backup. No recovery.

### What To Do

Always use replication factor **3** in production:

```bash
kafka-topics --create --topic orders \
  --partitions 6 \
  --replication-factor 3 \
  --bootstrap-server localhost:9092
```

### What Actually Happens in Production

A startup ran their staging Kafka with replication factor 1. "It's just staging," they said. One Friday, a broker's SSD failed. All staging data — 2 weeks of test events, fixtures, and integration test state — vanished. They couldn't run end-to-end tests until they rebuilt everything. Worse: they had copied the same config to a "low-priority" production topic. When that broker died on a holiday weekend, they lost 3 days of audit logs. The compliance team was not happy. Replication factor 3 is now mandatory for every topic, no exceptions.

---

## Mistake 7: Not Handling Deserialization Errors

### The Mistake

A producer sends a malformed message (wrong format, missing fields, corrupt JSON). Your consumer tries to deserialize it, throws an exception, and crashes. It restarts, reads the same bad message, crashes again. Infinite loop.

### Why It's Dangerous

One bad message can permanently block an entire partition. Your consumer is stuck in a crash loop. Thousands of good messages behind the bad one never get processed.

### What To Do

Use an error-handling deserializer:

```properties
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer
```

Or catch and skip bad messages:

```java
try {
    OrderEvent event = deserialize(record.value());
    process(event);
} catch (DeserializationException e) {
    log.error("Bad message at offset {}, skipping: {}",
        record.offset(), e.getMessage());
    // Optionally: send to dead letter topic
}
```

### What Actually Happens in Production

A team had a consumer that expected JSON with a "userId" field. A buggy producer sent a message with "user_id" (snake_case) instead. The consumer threw a deserialization exception and crashed. It restarted, read the same bad message, crashed again. Infinite loop. The partition was blocked. 50,000 good messages piled up behind the one bad message. The team didn't notice until users reported missing notifications. They added an error-handling deserializer that logs and skips bad messages. Now bad messages go to a dead-letter topic for investigation instead of blocking the pipeline.

---

## Mistake 8: Treating Kafka Like a Database

### The Mistake

You store data in Kafka and query it by key: "Give me order #12345." You expect random-access reads like a database.

### Why It's Wrong

Kafka is a **log**, not a database. You can read sequentially (from an offset forward) but not randomly (by key). There are no indexes, no `SELECT WHERE`, no joins.

### What To Do

Use Kafka for event streaming. Use a database for querying.

```
Producer → Kafka (event log) → Consumer → Database (queryable store)

"Give me all orders" → Query the database
"Something just happened" → Read from Kafka
```

If you need key-based lookup from Kafka, consider **Kafka Streams** with a state store, or consume events into a database/cache.

### What Actually Happens in Production

A team built a "query service" that read from Kafka to answer "show me order #12345." They scanned the topic from the beginning every time. With 10 million messages, each query took 30 seconds. They added caching, but cache misses were brutal. Users complained about slow load times. The team eventually moved to a proper design: Kafka for the event stream, a database for the materialized view. Queries went from 30 seconds to 50ms. The lesson: use the right tool. Kafka is a log. Databases are for querying.

---

## Mistake 9: Not Setting Retention Properly

### The Mistake

You leave the default retention (7 days) for all topics. A high-throughput topic generates 50 GB/day. After a week, you have 350 GB of data on each broker for that one topic. Disk fills up.

### Why It's Dangerous

When brokers run out of disk, they crash. All partitions on that broker go offline.

### What To Do

Set retention based on your needs:

```bash
# Short-lived events (notifications, logs):
kafka-topics --alter --topic notifications --config retention.ms=86400000  # 1 day

# Important events (orders):
kafka-topics --alter --topic orders --config retention.ms=604800000  # 7 days

# Compacted topic (latest value per key, forever):
kafka-topics --alter --topic user-profiles --config cleanup.policy=compact
```

Monitor disk usage on each broker.

### What Actually Happens in Production

A company had a high-volume event topic with default 7-day retention. It produced 100 GB per day. After a week, each broker had 700 GB for that topic alone. They had 3 brokers. Disk usage hit 95%. One broker ran out of space and crashed. Its partitions went offline. Producers got "Leader Not Available" errors. The whole pipeline stopped. They had to add disks, reduce retention to 2 days, and set up disk usage alerts. A simple retention config would have prevented the outage.

---

## Mistake 10: Not Testing with Failures

### The Mistake

You test Kafka with everything running perfectly: 3 brokers up, network stable, consumers healthy. You go to production. First broker failure? Chaos. Consumer crash? Data loss. Network partition? Nobody knows what happens.

### What To Do

Test failure scenarios:

- Kill a broker. Do consumers recover?
- Kill a consumer. Does the group rebalance?
- Send malformed messages. Does the consumer handle them?
- Simulate network latency. Do producers time out gracefully?
- Fill the disk. Does the broker alert you before crashing?

### What Actually Happens in Production

A team had never killed a broker in their test environment. In production, a broker died during peak traffic. Producers started getting timeouts. Some retried. Some didn't. Consumers got "NotLeaderForPartition" and stalled. The team didn't know how long leader election would take. They didn't know their consumers would automatically reconnect. Panic. They restarted everything "just in case." That caused more rebalances. The outage lasted 45 minutes. A 15-minute runbook drill ("kill a broker, watch what happens") would have cut that to 5 minutes. Now they run chaos tests every sprint.

---

## Mistake 11: Producing Messages Synchronously in a Loop

### The Mistake

You send 10,000 messages in a loop, waiting for each one to complete before sending the next:

```java
for (OrderEvent event : events) {
    producer.send(record).get();  // Blocks until ack received!
}
```

Your producer crawls. What should take seconds takes minutes.

### Why It's Wrong

`send().get()` blocks the thread until Kafka acknowledges the message. You're doing everything one at a time. Network round-trip latency (often 1-5ms per message) adds up. 10,000 messages × 3ms = 30 seconds of pure waiting. You're not using Kafka's batching or async capabilities.

### What To Do

Use async send with callbacks. Let the producer batch and send in parallel:

```java
for (OrderEvent event : events) {
    producer.send(record, (metadata, exception) -> {
        if (exception != null) {
            log.error("Send failed for {}", event.getOrderId(), exception);
        }
    });
}
producer.flush();  // Optional: wait for all to complete before exiting
```

The producer batches messages internally. It sends multiple messages in one network request. Throughput can be 10-100x higher.

!!! tip "When to use sync send"
    Use `send().get()` only when you need to know the result immediately (e.g., the next line depends on it). For bulk ingestion, async with callbacks is almost always better.

---

## Mistake 12: Not Configuring Proper Timeouts

### The Mistake

You use default timeouts. Things work in dev. In production, under load, you get mysterious "timed out" errors. Or your consumer gets kicked out of the group during a slow database query.

### Why It's Dangerous

Kafka has many timeout settings. Wrong values cause false failures or hide real problems.

| Setting | What It Controls | Default | Production Tip |
|---------|-----------------|---------|----------------|
| `request.timeout.ms` | How long producer waits for broker response | 30000 | Increase if brokers are slow (60000) |
| `delivery.timeout.ms` | Total time for a message (includes retries) | 120000 | Must be > request.timeout.ms × (1 + retries) |
| `session.timeout.ms` | How long before consumer is considered dead | 10000 | 30000-45000 for slow processors |
| `max.poll.interval.ms` | Max time between poll() calls | 300000 | Increase if processing is slow (600000) |

### What To Do

```properties
# Producer: allow time for retries
request.timeout.ms=30000
delivery.timeout.ms=120000
retries=3

# Consumer: avoid false "dead consumer" during slow processing
session.timeout.ms=45000
max.poll.interval.ms=600000
```

!!! warning "The max.poll.interval.ms trap"
    If your consumer takes 10 minutes to process a batch (e.g., calling a slow API), and `max.poll.interval.ms` is 5 minutes, Kafka will kick the consumer out. It thinks the consumer is dead. Increase the interval or process in smaller batches.

---

## Mistake 13: Using Kafka for Request-Reply Pattern

### The Mistake

You want a synchronous request-response flow. You send a request to a Kafka topic, block waiting for a reply on another topic. "It's async under the hood, but we'll make it sync with a timeout."

### Why It's Wrong

Kafka is designed for **asynchronous, fire-and-forget** messaging. It's not a RPC system. Request-reply on Kafka means:

- You must correlate request and response (custom IDs, headers)
- You block a thread waiting for a response that might never come
- Timeouts are tricky (what if the consumer is slow?)
- You're fighting the system. Kafka has no built-in request-reply semantics.

### What To Do

Use HTTP or gRPC for request-reply. Use Kafka for event streaming.

```
Need sync response?  → HTTP/gRPC
Need async events?   → Kafka
```

If you truly need request-reply over Kafka (e.g., legacy integration), use a library like Spring Kafka's `ReplyingKafkaTemplate` — but know you're adding complexity. For most cases, keep sync and async separate.

---

## Mistake 14: Not Planning for Schema Evolution

### The Mistake

Your messages have a fixed format. One day you add a new field. You deploy the new producer. Old consumers crash. "Unknown field" or "Missing required field" errors everywhere.

### Why It's Dangerous

Message formats change. New features need new fields. If you don't plan for it, every change is a coordinated big-bang deployment. One service can't upgrade until all others do. That doesn't scale.

### What To Do

Use a schema registry (Confluent Schema Registry, AWS Glue Schema Registry) with Avro or Protobuf. Define backward- and forward-compatible rules:

- **Backward compatible:** Old consumers can read new messages (e.g., new optional field)
- **Forward compatible:** New consumers can read old messages (e.g., old messages lack new required field — make it optional)

```java
// With Schema Registry, adding optional field is safe
schema = {
  "name": "OrderEvent",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "discountCode", "type": ["null", "string"], "default": null}  // New, optional
  ]
}
```

!!! info "Breaking changes"
    Renaming a field, changing a type, or removing a field often breaks consumers. Use a new topic or versioned topics for breaking changes. Migrate consumers gradually.

---

## Summary: The 14 Commandments

| # | Commandment |
|---|------------|
| 1 | Use message keys when ordering matters |
| 2 | Start with 6-12 partitions, not 1 or 200 |
| 3 | Use manual offset commits for critical data |
| 4 | Always make consumers idempotent |
| 5 | Monitor consumer lag and set alerts |
| 6 | Use replication factor 3 in production |
| 7 | Handle deserialization errors gracefully |
| 8 | Kafka is a log, not a database |
| 9 | Set retention based on your actual needs |
| 10 | Test with failures before going to production |
| 11 | Use async send with callbacks for bulk producing, not sync in a loop |
| 12 | Configure timeouts (request, delivery, session, max.poll) for your workload |
| 13 | Use HTTP/gRPC for request-reply, not Kafka |
| 14 | Plan for schema evolution with Schema Registry |

---

## Quick Summary: Revision Table

| Mistake | Symptom | Fix |
|---------|---------|-----|
| No message keys | Events out of order, wrong sequence | Use entity ID as key |
| Wrong partition count | Can't scale, or wasted resources | 6-12 partitions to start |
| Auto-commit for critical data | Lost or duplicated messages | Manual commit after processing |
| Non-idempotent consumer | Duplicate charges, double processing | Check before process (DB, Redis, etc.) |
| Ignoring lag | Delayed data, backlog explosion | Monitor lag, set alerts |
| Replication factor 1 | Data loss when broker dies | Use replication factor 3 |
| No deserialization error handling | Consumer crash loop, blocked partition | ErrorHandlingDeserializer or try/catch |
| Kafka as database | Slow queries, wrong tool | Kafka for events, DB for queries |
| Wrong retention | Disk full, or data gone too soon | Set retention per topic needs |
| No failure testing | Panic during real outage | Chaos test: kill broker, consumer |
| Sync send in loop | Slow producer, low throughput | Async send with callbacks |
| Wrong timeouts | False failures, consumer kicked out | Tune request, delivery, session, max.poll |
| Kafka for request-reply | Complex, fragile, fighting the system | Use HTTP/gRPC for sync |
| No schema evolution plan | Breaking changes, big-bang deploys | Schema Registry, compatible changes |

---

## Exercise

1. Send 5 messages without a key to a 3-partition topic. Verify they go to different partitions.
2. Send 5 messages with the same key. Verify they all go to the same partition.
3. Set `enable.auto.commit=true`, process a message, and kill the consumer. What happens on restart?
4. Repeat with `enable.auto.commit=false` and manual commit. What's different?

---

**Next up:** [Kafka with Spring Boot](kafka-with-springboot.md) — Build real applications with Kafka.
