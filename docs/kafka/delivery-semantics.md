# Message Delivery Semantics

## The Problem: What Could Go Wrong?

When a producer sends a message and a consumer processes it, several things can fail at different stages. Networks drop. Servers crash. Processes get killed mid-operation. The question is: after a failure, what happened to the message?

Was it delivered? Was it processed? Was it processed **twice**? Was it **lost**?

Kafka gives you three levels of guarantee, and understanding them is essential — both for building reliable systems and for interviews.

---

## The Three Guarantees

| Guarantee | What It Means | Risk |
|-----------|-------------|------|
| **At-most-once** | Message is delivered 0 or 1 time. It might be lost. | Data loss possible |
| **At-least-once** | Message is delivered 1 or more times. It might be duplicated. | Duplicates possible |
| **Exactly-once** | Message is delivered exactly 1 time. Never lost, never duplicated. | Complex to implement |

### The Analogy: Sending Important Mail

**At-most-once:** You drop a letter in a public mailbox. You hope it arrives. You never check. If it gets lost in transit, too bad. You don't send another.

**At-least-once:** You send a registered letter. If you don't get a delivery confirmation within 3 days, you send the letter again. The recipient might get 2 copies, but at least they get it.

**Exactly-once:** You send a registered letter with a unique tracking number. The post office checks: "Have we already delivered a letter with this tracking number?" If yes, they discard the duplicate. The recipient gets exactly one copy.

---

## Real-World Scenario: Payment Processing

Let's trace a payment through each delivery guarantee. A customer pays $100 for an order. The payment service produces a "PAYMENT_COMPLETE" event to Kafka. The order service consumes it and marks the order as paid. Here's the detailed story for each case.

### At-Most-Once: User Not Charged (Data Loss)

**Setup:** The order service uses "commit before process." It reads the event, commits the offset immediately, then starts processing.

**Step 1:** Customer completes payment. Payment service publishes `{orderId: 123, amount: 100, txId: "tx_abc"}` to topic "payments."

**Step 2:** Order service consumer reads the message at offset 42. It commits offset 42 right away. "I'm done with this message," it tells Kafka.

**Step 3:** Consumer starts processing. It calls the payment gateway to confirm the charge. It updates the order in the database. But before it finishes, the pod is killed. OOM. Deployment. Crash.

**Step 4:** Consumer restarts. It asks Kafka: "Where did I leave off?" Kafka says: "Offset 42 is committed. Start from 43."

**Step 5:** The message at offset 42 was never fully processed. The order is still "pending." The customer's $100 was deducted by the payment gateway, but the order service never recorded it. Money is in limbo. The customer calls support: "I paid but my order says unpaid." Manual refund or manual order update. At-most-once lost the event.

### At-Least-Once: Charged Twice (Duplicate)

**Setup:** The order service uses "process then commit." It processes first, commits after.

**Step 1:** Same event: `{orderId: 123, amount: 100, txId: "tx_abc"}` at offset 42.

**Step 2:** Consumer processes. It marks the order as paid. It records the transaction. No idempotency check.

**Step 3:** Before it can commit offset 42, the consumer crashes. Network partition. Pod eviction.

**Step 4:** Consumer restarts. Last committed offset was 41. It reads from 42 again.

**Step 5:** Consumer processes the same event again. It marks the order as paid again. It calls the payment gateway again. The customer gets charged twice. $100 becomes $200. Chargebacks. Angry customer. At-least-once without idempotency causes duplicates.

### Exactly-Once: Charged Once (Correct)

**Setup:** The order service uses idempotency. Before doing anything, it checks: "Have we already processed tx_abc?"

**Step 1:** Same event at offset 42.

**Step 2:** Consumer processes. It checks: `SELECT 1 FROM processed_transactions WHERE tx_id = 'tx_abc'`. No row. First time.

**Step 3:** It marks the order as paid. It inserts into `processed_transactions`. It commits offset 42.

**Step 4:** Crash. Restart. Re-read offset 42.

**Step 5:** Consumer processes again. It checks: `SELECT 1 FROM processed_transactions WHERE tx_id = 'tx_abc'`. Row exists. Skip. No duplicate charge. Commit offset 42 again (idempotent commit). Customer charged exactly once. Order marked paid. Clean.

---

## At-Most-Once Delivery

### How It Works

The consumer commits its offset **before** processing the message. If it crashes after committing but before finishing processing, the message is lost. When the consumer restarts, it skips the message (already committed).

```
Step 1: Consumer reads message at offset 5
Step 2: Consumer commits offset 5 → "I'm done with this message"
Step 3: Consumer starts processing the message
Step 4: CRASH! Processing never finishes
Step 5: Consumer restarts → "My committed offset is 5, start from 6"
Step 6: Message at offset 5 was never processed ❌ Lost!
```

### When To Use

Honestly, rarely. But some use cases tolerate data loss:

- **Logging:** If you lose a log line, it's usually not catastrophic.
- **Metrics sampling:** Missing one data point out of thousands doesn't ruin your graphs.
- **Real-time sensor readings:** If a temperature reading is lost, the next one comes in 5 seconds anyway.

### Real Scenario: Metrics Dashboard

A dashboard consumes CPU metrics from Kafka. At-most-once is fine. If one data point is lost, the graph has a tiny gap. The next second's data fills in. Nobody notices. Using at-least-once here would add complexity (deduplication) for no benefit. The team keeps it simple: commit before processing, accept occasional gaps.

### Producer Side: acks=0

```properties
acks=0
```

The producer doesn't wait for any acknowledgment. It sends the message and moves on. If the broker never received it, the producer doesn't know. Message lost.

---

## At-Least-Once Delivery

### How It Works

The consumer processes the message **first**, then commits the offset. If it crashes after processing but before committing, it will re-read and re-process the message when it restarts.

```
Step 1: Consumer reads message at offset 5
Step 2: Consumer processes the message → "Order #100 paid"
Step 3: CRASH! Before committing offset 5
Step 4: Consumer restarts → "My committed offset is 4, start from 5"
Step 5: Consumer reads offset 5 AGAIN → "Order #100 paid" (again!)
Step 6: Order #100 might get charged TWICE ❌ Duplicate!
```

### This Is the Most Common Guarantee

Most production Kafka systems use at-least-once. Why? Because it's simpler than exactly-once and doesn't lose data. You handle duplicates in your application logic.

### Real Scenario: Order Fulfillment

An order service consumes "ORDER_PLACED" events. It creates an order in the database and sends a confirmation email. At-least-once. During a deploy, a consumer processes an order, sends the email, then crashes before committing. On restart, it re-processes. Without idempotency: duplicate order in DB (or unique constraint violation), duplicate email to customer. With idempotency: check `orderRepository.existsById(orderId)` before creating. Skip if exists. Customer gets one email. Clean.

### How To Handle Duplicates: Idempotent Consumers

An **idempotent** operation is one that produces the same result no matter how many times you run it. `SET x = 5` is idempotent (doing it twice doesn't change x). `INCR x` is NOT idempotent (doing it twice gives a different result).

Make your consumers idempotent:

```java
public void processOrder(OrderEvent event) {
    // Check if we've already processed this message
    if (orderRepository.existsById(event.getOrderId())) {
        log.info("Already processed order {}, skipping",
            event.getOrderId());
        return;
    }

    // Process for the first time
    orderRepository.save(event);
    paymentService.charge(event.getAmount());
}
```

Other idempotency strategies:

| Strategy | How It Works |
|----------|------------|
| Database unique constraint | `INSERT ... ON CONFLICT DO NOTHING` — DB rejects duplicates |
| Processed message ID table | Store message IDs in a "processed" table, check before processing |
| Business key deduplication | Use orderId/transactionId to check if already done |
| Redis deduplication | `SET msg:<id> "processed" NX EX 3600` — returns false if already set |

### Idempotency Patterns in Detail

Here are four patterns with code. Pick based on your stack and latency needs.

**Pattern 1: Database Unique Constraint**

Use a unique constraint on the business key. Duplicate inserts fail. Your code handles the failure.

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "orderId"))
public class ProcessedOrder {
    @Id
    private String orderId;
    // ...
}

// In consumer:
try {
    repository.save(new ProcessedOrder(event.getOrderId()));
    doWork(event);
} catch (DataIntegrityViolationException e) {
    log.info("Already processed order {}", event.getOrderId());
    return;  // Duplicate, skip
}
```

**Pattern 2: Redis SET NX for Dedup**

Use Redis as a dedup store. `SET key NX` only succeeds if the key doesn't exist. Set TTL so old keys expire.

```java
String key = "processed:" + event.getTransactionId();
Boolean added = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24));
if (Boolean.FALSE.equals(added)) {
    log.info("Already processed tx {}", event.getTransactionId());
    return;
}
doWork(event);
```

**Pattern 3: Idempotency Key in HTTP Header**

When calling an external API, send an idempotency key. The API stores it and rejects duplicate requests with the same key.

```java
String idempotencyKey = event.getOrderId() + "-" + event.getTimestamp();
restTemplate.exchange(url, HttpMethod.POST, 
    new HttpEntity<>(body, headers.set("Idempotency-Key", idempotencyKey)),
    Response.class);
// If duplicate, API returns 409 or same response. Your consumer can treat as success.
```

**Pattern 4: Processed-Message-IDs Table**

Store Kafka message IDs (or topic+partition+offset) in a table. Check before processing.

```java
String msgId = record.topic() + "-" + record.partition() + "-" + record.offset();
if (processedRepo.existsByMessageId(msgId)) {
    return;
}
processedRepo.save(new ProcessedMessage(msgId));
doWork(record.value());
```

!!! tip "Choose wisely"
    Database unique constraint: simple, durable. Redis: fast, but Redis failure loses dedup state. Idempotency key: for external APIs. Processed-IDs table: flexible, works for any message source.

### Producer Side: acks=all + retries

```properties
acks=all
retries=3
enable.idempotence=true
```

With `acks=all`, the producer waits for all ISR replicas to confirm. With retries, if a network glitch occurs, the producer retries. With idempotence enabled, Kafka deduplicates retries on the broker side.

!!! tip "Interview Insight"
    "In most production systems, I use at-least-once delivery with idempotent consumers. The producer uses `acks=all` with `enable.idempotence=true`. On the consumer side, I check if the message was already processed using a unique business key like orderId before processing. This gives me durability without the complexity of exactly-once semantics."

---

## Exactly-Once Delivery

### The Holy Grail

Exactly-once means every message is processed exactly one time. No loss. No duplicates. Sounds perfect, right? So why doesn't everyone use it?

Because it's **hard** and **expensive**. It requires coordination between the producer, Kafka, and the consumer — all in a transaction.

### How Kafka Achieves Exactly-Once

Kafka supports exactly-once through two mechanisms:

**1. Idempotent Producer (producer → Kafka)**

```properties
enable.idempotence=true
```

The producer assigns a sequence number to each message. If a network retry causes Kafka to receive the same message twice, Kafka detects the duplicate sequence number and discards it.

```
Producer sends: msg (seq=42) → Kafka stores it at offset 100
Network glitch → Producer retries: msg (seq=42) → Kafka says "I already have seq 42, skip"

Result: Message stored exactly once in Kafka ✅
```

But this only covers producer-to-Kafka. What about the consumer?

**2. Transactional Processing (Kafka → consumer → Kafka)**

If your consumer reads from one topic, processes data, and writes to another topic, you can wrap it in a Kafka transaction:

```java
producer.initTransactions();

try {
    producer.beginTransaction();

    // Read from input topic
    ConsumerRecords<String, String> records = consumer.poll(...);

    for (ConsumerRecord<String, String> record : records) {
        // Process
        String result = process(record.value());

        // Write to output topic
        producer.send(new ProducerRecord<>("output-topic", result));
    }

    // Commit offset + output message atomically
    producer.sendOffsetsToTransaction(offsets, groupMetadata);
    producer.commitTransaction();

} catch (Exception e) {
    producer.abortTransaction();
}
```

Either BOTH the output write and the offset commit happen, or NEITHER happens. No partial state.

### Kafka Streams Exactly-Once (exactly_once_v2)

Kafka Streams supports exactly-once processing out of the box. Set the processing guarantee:

```properties
processing.guarantee=exactly_once_v2
```

With this, Kafka Streams uses transactional producers and consumer offsets are committed as part of the transaction. If the app crashes, it restarts and re-processes from the last committed offset. No duplicates. No data loss.

**How it works:** Streams uses a transactional producer. It reads from input topics, processes, writes to output topics and state stores. All in one transaction. Commit happens atomically. `exactly_once_v2` improves on the older `exactly_once` with better handling of producer fencing and recovery.

!!! info "When to use"
    Use `exactly_once_v2` for Kafka Streams apps that do aggregations, joins, or topic-to-topic ETL. The cost is slightly higher latency. For simple consumers, at-least-once + idempotency is often enough.

---

## Kafka Transactions Deep Dive

### transactional.id

When using transactions, you must set `transactional.id` on the producer. This ID is used to identify the producer across restarts. Kafka uses it to fence out old instances — if a new producer starts with the same ID, the old one can no longer write.

```properties
transactional.id=my-producer-1
```

!!! warning "One transactional ID per producer instance"
    Each producer instance (e.g., each pod in Kubernetes) should have a unique `transactional.id`. If two producers share the same ID, they will fence each other out. Use something like `my-producer-${pod-name}` or `my-producer-${uuid}`.

### isolation.level: read_committed vs read_uncommitted

Consumers can choose what to read:

| Level | Behavior |
|-------|----------|
| `read_uncommitted` (default) | Read all messages, including those in uncommitted transactions. Fastest. May see duplicates if a transaction aborts. |
| `read_committed` | Only read messages from committed transactions. Never see aborted or in-progress data. Slightly slower. |

For exactly-once processing, consumers of the output topic should use `read_committed`:

```properties
isolation.level=read_committed
```

### Transaction Coordinator

Kafka has a **transaction coordinator** — a special broker that manages transactions. When a producer calls `initTransactions()`, it finds the coordinator. The coordinator tracks the producer's state, handles commits and aborts, and writes to the internal `__transaction_state` topic.

**Flow:** Producer begins transaction → sends messages → sends offset commit → commits transaction. The coordinator writes a "commit" record. Only then do the messages become visible to `read_committed` consumers.

---

## Consumer Offset Storage

### Where Are Offsets Stored?

Kafka stores consumer group offsets in an internal topic: `__consumer_offsets`. Each consumer group has entries like:

```
Group "order-processors", Topic "orders", Partition 2 → Offset 15420
```

When a consumer commits, it writes to this topic. When it starts, it reads from this topic to know where to resume.

### How Long Are Offsets Retained?

By default, `__consumer_offsets` has a retention policy. Old offset commits are eventually deleted. The setting is `offsets.retention.minutes` (default: 10080 = 7 days).

If a consumer group doesn't commit for 7 days, its offsets may be expired. When the consumer comes back, it has no stored offset. It will use `auto.offset.reset` (earliest or latest) to decide where to start. You might re-process millions of messages or skip them — depending on the setting.

!!! tip "Long-running consumers"
    If you have consumer groups that might be inactive for weeks (e.g., a batch job that runs monthly), increase `offsets.retention.minutes` on the broker. Or use external offset storage (e.g., a database) for critical replay scenarios.

### Offset Commit Failure Scenarios

**Scenario 1:** Consumer commits, but the commit request fails (network error). The consumer thinks it committed. On restart, it re-reads from the old offset. Duplicate processing. Use `commitSync()` to ensure the commit succeeds before proceeding, or implement retry logic.

**Scenario 2:** Consumer commits offset 100. Before the commit is fully replicated, the coordinator (broker) dies. The new coordinator might not have the commit. Consumer restarts, reads from an older offset. Again, duplicates.

**Scenario 3:** Rebalance during commit. The consumer is removed from the group. Its commit might be ignored. When it rejoins and gets a new partition assignment, it might re-process. This is why idempotency is critical.

---

### When To Use Exactly-Once

| Use Case | Exactly-Once Needed? |
|----------|---------------------|
| Financial transactions | Yes — can't charge someone twice |
| Kafka Streams processing (topic-to-topic) | Yes — built-in support |
| Simple event logging | No — at-least-once is fine |
| Notifications | No — getting 2 emails is annoying but not catastrophic |
| Analytics counters | Depends — idempotent at-least-once is usually sufficient |

!!! warning "The cost"
    Exactly-once adds latency (transactions are slower) and complexity (more failure modes to handle). Use it only when the business requires it.

### Real Scenario: Kafka Streams Aggregation

A team uses Kafka Streams to aggregate sales by region. Input topic: "sales". Output topic: "sales-by-region". They use exactly-once. The Streams app reads, aggregates, and writes in a transaction. If it crashes mid-batch, the transaction aborts. On restart, it re-reads from the last committed offset. No partial writes. The output topic has consistent, deduplicated aggregates. Without exactly-once, a crash could produce duplicate counts (same sale counted twice) or missing counts. For analytics, that would corrupt reports.

---

## Choosing the Right Guarantee

### Decision Framework

```
Is losing a message acceptable?
├── Yes → At-most-once (acks=0, commit before processing)
└── No
    ├── Is receiving a duplicate acceptable?
    │   ├── Yes → At-least-once (acks=all, commit after processing)
    │   │         + Make consumers idempotent
    │   └── No → Exactly-once (transactions + idempotent producer)
    └── Usually, "at-least-once + idempotent consumer" is the sweet spot
```

### Comparison Table

| | At-Most-Once | At-Least-Once | Exactly-Once |
|---|------------|--------------|-------------|
| Data loss? | Possible | No | No |
| Duplicates? | No | Possible | No |
| Complexity | Simple | Moderate | Complex |
| Performance | Fastest | Fast | Slowest |
| Producer acks | `0` | `all` | `all` + transactions |
| Consumer offset | Commit before processing | Commit after processing | Transactional commit |
| When to use | Logs, metrics | **Most use cases** | Financial, streams |
| **Real-world example** | Dashboard metrics, log aggregation | Order processing, notifications, most microservices | Payment processing, Kafka Streams ETL |

---

## Offset Commit Strategies

How you commit offsets determines your delivery guarantee on the consumer side.

### Auto Commit (Default)

```properties
enable.auto.commit=true
auto.commit.interval.ms=5000
```

Kafka auto-commits every 5 seconds. Simple, but risky:

```
Consumer reads messages 1-10
Auto-commit happens at message 7 (5-second timer fires)
Consumer processes message 8, 9...
CRASH at message 9!
Restart: committed offset = 7, re-reads 8, 9, 10
Messages 8 and 9 processed twice. Message 10 not lost.
```

### Manual Sync Commit

```java
for (ConsumerRecord<String, String> record : records) {
    process(record);
}
consumer.commitSync();  // Block until commit succeeds
```

Safer. You commit only after all processing is done. But if you crash between processing and committing, you re-process.

### Manual Async Commit

```java
consumer.commitAsync((offsets, exception) -> {
    if (exception != null) {
        log.error("Commit failed", exception);
    }
});
```

Non-blocking. Faster. But if the commit fails, you don't retry (to avoid out-of-order commits). Use `commitSync()` in the `finally` block or on shutdown.

### Per-Message Commit

```java
for (ConsumerRecord<String, String> record : records) {
    process(record);
    consumer.commitSync(
        Map.of(new TopicPartition(record.topic(), record.partition()),
               new OffsetAndMetadata(record.offset() + 1)));
}
```

Most granular. Commits after each message. If you crash, at most 1 message is re-processed. But it's slow — one commit per message is expensive.

---

## Quick Summary

| Guarantee | Lost? | Duplicated? | How | Best For |
|-----------|-------|-------------|-----|----------|
| At-most-once | Yes | No | Commit before process | Logs, metrics |
| At-least-once | No | Yes | Process then commit + idempotent consumer | **Most apps** |
| Exactly-once | No | No | Kafka transactions | Financial, streams |

| Commit Strategy | Risk | Performance |
|----------------|------|------------|
| Auto commit | Unpredictable duplicates/loss | Fastest |
| Manual sync after batch | Batch re-processing on crash | Good |
| Manual per-message | At most 1 re-processed | Slowest |

---

## Exercise

1. Write a consumer with auto-commit enabled. Process 10 messages. Kill it mid-way. Restart. How many messages are re-processed?
2. Switch to manual commit (`commitSync()` after processing). Repeat the test. What's different?
3. Implement an idempotent consumer that checks a database before processing.
4. Explain to a friend (or a rubber duck): "Why is at-least-once with idempotent consumers the most common production pattern?"

---

**Next up:** [Real-World Use Cases](kafka-usecases.md) — Where Kafka is used in the real world.
