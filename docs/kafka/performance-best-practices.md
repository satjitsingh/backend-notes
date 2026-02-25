# Performance & Best Practices

## Why This Page Matters

Knowing Kafka's concepts is one thing. Running Kafka well in production is another. This page covers the practices that keep Kafka fast, reliable, and manageable at scale. These are the things that separate a junior developer from a senior one in interviews and on the job.

---

## Producer Best Practices

### 1. Always Set acks Intentionally

Don't rely on the default. Choose based on your needs:

```properties
# For critical data (orders, payments):
acks=all

# For non-critical data (logs, metrics):
acks=1
```

### 2. Enable Idempotence

Prevents duplicate messages on retries. No downside. Always enable it.

```properties
enable.idempotence=true
```

### 3. Use Batching Wisely

Kafka producers batch messages before sending. Larger batches = fewer network calls = higher throughput. But larger batches also mean higher latency for individual messages.

```properties
batch.size=32768          # 32 KB per batch (default: 16 KB)
linger.ms=10              # Wait up to 10ms to fill a batch
```

Think of it like a bus. You can send one passenger per bus (low latency, wasteful) or wait a few minutes to fill the bus (higher latency per passenger, but more efficient).

| Setting | Low Latency | High Throughput |
|---------|------------|----------------|
| `linger.ms` | 0-1 ms | 10-100 ms |
| `batch.size` | 16384 | 65536-131072 |

### 4. Use Compression

Compress messages before sending. Less data over the network. Less disk on brokers.

```properties
compression.type=lz4
```

| Algorithm | Speed | Compression Ratio |
|-----------|-------|------------------|
| `none` | N/A | 1x (no compression) |
| `lz4` | Fastest | Good |
| `snappy` | Fast | Good |
| `zstd` | Medium | Best |
| `gzip` | Slow | Great |

**Recommendation:** `lz4` for most workloads. Best balance of speed and compression.

### 5. Use Meaningful Keys

Messages with the same key go to the same partition. This guarantees ordering for related events.

```java
// Good: orderId as key — all events for same order are ordered
producer.send(new ProducerRecord<>("orders", orderId, event));

// Bad: no key — events for same order might go to different partitions
producer.send(new ProducerRecord<>("orders", event));
```

### 6. Handle Send Failures

Always use callbacks or check the future. Don't fire-and-forget critical messages.

```java
producer.send(record, (metadata, exception) -> {
    if (exception != null) {
        log.error("Failed to send: {}", exception.getMessage());
        // Retry, save to dead letter queue, or alert
    }
});
```

---

## Consumer Best Practices

### 1. Use Manual Offset Commits for Critical Data

Auto-commit is simple but can cause data loss or duplicates.

```properties
enable.auto.commit=false
```

```java
for (ConsumerRecord<String, String> record : records) {
    process(record);
}
consumer.commitSync();
```

### 2. Make Consumers Idempotent

Since at-least-once delivery is the norm, your consumer might process the same message twice. Design for it.

```java
if (repository.existsById(event.getId())) {
    return; // Already processed, skip
}
repository.save(event);
```

### 3. Tune poll() and Processing

If your processing takes too long, the consumer might be kicked from the group (exceeds `max.poll.interval.ms`).

```properties
max.poll.records=100              # Fewer records per poll = faster processing per batch
max.poll.interval.ms=600000       # 10 minutes max between polls
```

### 4. Use the CooperativeStickyAssignor

Reduces rebalancing disruption. Only the affected partitions are reassigned — others keep reading.

```properties
partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

### 5. Monitor Consumer Lag

Consumer lag tells you how far behind your consumer is. If it keeps growing, you're not processing fast enough.

```bash
kafka-consumer-groups --describe --group my-group --bootstrap-server localhost:9092
```

If lag grows: add more consumers (up to partition count) or optimize processing logic.

---

## Topic & Partition Best Practices

### 1. Start with a Reasonable Partition Count

```
Rule of thumb:
  Partitions = max(expected consumer count, target throughput / throughput per partition)

  A single partition can handle ~10 MB/sec writes.
  If you need 50 MB/sec, start with at least 6 partitions.
```

### 2. You Can Add Partitions, But Never Remove Them

```bash
# This works:
kafka-topics --alter --topic orders --partitions 12

# This does NOT work:
kafka-topics --alter --topic orders --partitions 3  # ERROR!
```

!!! warning "Adding partitions breaks key-based ordering"
    If you add partitions, `hash(key) % numPartitions` changes. A key that used to go to Partition 2 might now go to Partition 5. Existing consumers won't see this key's new messages. Plan your partition count upfront.

### 3. Use Replication Factor 3

The industry standard. Tolerates 2 broker failures.

```bash
kafka-topics --create --topic orders --partitions 6 --replication-factor 3
```

### 4. Set Appropriate Retention

Don't keep messages forever unless you need to. Storage costs money.

```properties
# Keep for 7 days (default):
retention.ms=604800000

# Keep for 3 days (if consumers catch up quickly):
retention.ms=259200000

# Compacted topics (keep latest value per key forever):
cleanup.policy=compact
```

---

## Broker Best Practices

### 1. Use Dedicated Disks

Kafka is disk-heavy. Don't share disks with other applications. Use SSDs for better performance.

### 2. Separate Kafka Log Dirs from OS Disk

```properties
log.dirs=/data/kafka-logs
```

Don't put Kafka data on the OS partition. If Kafka fills the disk, it won't crash the OS.

### 3. Monitor Key Broker Metrics

| Metric | What It Means | Alert If |
|--------|-------------|----------|
| Under-replicated partitions | Replicas falling behind | > 0 for extended time |
| Active controller count | Exactly 1 broker should be controller | != 1 |
| Request handler idle % | How busy the broker is | < 20% |
| Log flush latency | Disk write performance | Spikes consistently |
| Network throughput | Bytes in/out per second | Approaching NIC limit |

### 4. Set min.insync.replicas

```properties
min.insync.replicas=2
```

With `acks=all`, this ensures at least 2 replicas confirm each write. Prevents data loss if one broker fails.

---

## Naming Conventions

Consistent topic naming makes your Kafka cluster manageable.

```
# Good naming:
orders.created
orders.payment-completed
users.profile-updated
notifications.email
notifications.sms

# Bad naming:
topic1
myTopic
ORDER_EVENTS
user-updates-v2-final-FINAL
```

**Pattern:** `<domain>.<event-type>` using lowercase and hyphens.

---

## Error Handling Patterns

### Dead Letter Topic (DLT)

Messages that fail after all retries go to a special topic for manual review.

```
Normal flow:   orders → Consumer → Process ✅
Retry flow:    orders → Consumer → Fail → Retry 1 → Retry 2 → Fail
                                                                 ↓
                                                          orders-dlt (Dead Letter Topic)
```

In Spring Boot:

```java
@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000))
@KafkaListener(topics = "orders", groupId = "order-service")
public void process(OrderEvent event) {
    // If this throws, retried 3 times. Then goes to DLT.
}

@DltHandler
public void handleDlt(OrderEvent event) {
    log.error("Failed after 3 retries: {}", event);
    failedEventRepository.save(event);
}
```

### Circuit Breaker

If a downstream service (database, API) is down, stop processing messages and wait instead of failing every message.

Use Resilience4j or similar library to wrap your processing logic.

### Retry Strategies in Detail

**Exponential backoff:** Wait longer between each retry. First retry: 1 second. Second: 2 seconds. Third: 4 seconds. Prevents hammering a failing service.

```java
@RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2))
@KafkaListener(topics = "orders", groupId = "order-service")
public void process(OrderEvent event) {
    // Retries at 1s, 2s, 4s, 8s
}
```

**Max retries:** Decide how many times to retry before giving up. Too few: transient failures cause DLT overload. Too many: slow consumers block the partition. 3–5 retries is common.

**DLT routing:** After max retries, send to Dead Letter Topic. A separate consumer or manual process handles DLT messages. Alert on DLT growth.

```java
@DltHandler
public void handleDlt(OrderEvent event, ConsumerRecord<String, OrderEvent> record) {
    failedEventRepository.save(event);
    alertingService.notify("Message failed after retries", record);
}
```

!!! tip "Retry best practice"
    Use exponential backoff with a cap (e.g., max 30 seconds). Always route to DLT after max retries. Monitor DLT size.

---

## Kafka Connect Best Practices

### What Is Kafka Connect?

Kafka Connect is a framework for moving data in and out of Kafka. You configure connectors. No custom consumer code. Connectors handle the plumbing.

### When to Use Instead of Custom Consumers

Use Kafka Connect when:

- You need to sync a database table to Kafka (or vice versa).
- You need to move data to Elasticsearch, S3, or a data warehouse.
- The integration is standard (JDBC, file, HTTP). Don't reinvent the wheel.

Use custom consumers when:

- You need complex transformation logic.
- You need to call external APIs or services.
- The connector doesn't exist or doesn't fit.

### Source vs Sink Connectors

| Type | Direction | Example |
|------|-----------|---------|
| **Source** | External system → Kafka | JDBC Source (MySQL table → topic), File Source |
| **Sink** | Kafka → External system | Elasticsearch Sink, JDBC Sink, S3 Sink |

Source connectors pull data from a system and produce to Kafka. Sink connectors consume from Kafka and write to a system.

!!! info "Connector ecosystem"
    Confluent Hub and the Kafka ecosystem offer hundreds of connectors. Check before building your own.

---

## Schema Management

### Why Schemas Matter

Without schemas, producers and consumers can drift. Producer sends `{"price": 99.99}`. Consumer expects `{"amount": 99.99}`. Breaks. Schemas enforce a contract.

### Avro vs JSON vs Protobuf

| Format | Pros | Cons |
|-------|------|------|
| **Avro** | Compact, Schema Registry support, good for Kafka | Requires schema files, learning curve |
| **JSON** | Human-readable, no schema needed | Larger payload, no built-in validation |
| **Protobuf** | Fast, compact, strong typing | Binary, less Kafka-native tooling |

For production Kafka, Avro with Schema Registry is common. It gives you evolution (add fields, remove fields) with compatibility checks.

### Schema Registry Basics

Schema Registry stores schemas. Producers and consumers fetch schemas by ID. When you produce a message, you send schema ID + payload. Consumer fetches schema and deserializes.

### Backward and Forward Compatibility

- **Backward compatible:** New consumer can read old producer data. Add optional fields. Don't remove required fields.
- **Forward compatible:** Old consumer can read new producer data. Add optional fields. Don't change types.

!!! warning "Breaking changes"
    Changing a field type or removing a required field breaks compatibility. Plan schema evolution carefully.

---

## Monitoring & Alerting

### Key Metrics to Watch

| Metric | What It Means | Alert If |
|--------|---------------|----------|
| Under-replicated partitions | Replicas falling behind leader | > 0 for more than 5 minutes |
| Active controller count | Exactly 1 broker should be controller | != 1 |
| Request latency (p99) | How long brokers take to respond | Spikes above baseline |
| Consumer lag | How far behind consumers are | Growing over time, or > threshold |

### Tools

- **Prometheus + Grafana:** Scrape Kafka metrics. Build dashboards. Set alerts.
- **Confluent Control Center:** Commercial UI for Kafka. Topic management, consumer lag, broker health.
- **Kafka built-in:** `kafka-consumer-groups --describe` for lag. JMX for broker metrics.

!!! tip "Start simple"
    At minimum, monitor consumer lag and under-replicated partitions. Add more as you scale.

---

## Testing Kafka Applications

### Embedded Kafka for Unit Tests

Use `@EmbeddedKafka` (spring-kafka-test) to run a real Kafka broker in-process. Fast. No Docker needed.

```java
@EmbeddedKafka(partitions = 1, topics = {"orders"})
@SpringBootTest
class OrderConsumerTest {
    @Autowired
    private KafkaTemplate<String, String> template;

    @Test
    void shouldProcessOrder() {
        template.send("orders", "order-1", "{\"id\":\"order-1\"}");
        // Assert consumer processed it
    }
}
```

### Testcontainers for Integration Tests

For full integration (Kafka + DB + app), use Testcontainers. Spins up real Kafka in Docker. Slower but more realistic.

```java
@Testcontainers
@SpringBootTest
class OrderServiceIntegrationTest {
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
    // ...
}
```

### spring-kafka-test

Provides `@EmbeddedKafka`, `KafkaTestUtils`, and mock producers/consumers. Add dependency: `spring-kafka-test`.

---

## Operational Runbook

### Common Issues and Fixes

| Issue | Symptom | Fix |
|-------|---------|-----|
| **Consumer lag growing** | Lag increases, never catches up | Add consumers (up to partition count), optimize processing, scale horizontally |
| **Broker disk full** | Brokers fail, "No space left" | Reduce retention, add disks, delete old topics |
| **Rebalancing storms** | Constant consumer group rebalances | Increase `max.poll.interval.ms`, reduce `max.poll.records`, use CooperativeStickyAssignor |
| **Producer timeouts** | `TimeoutException` on send | Increase `request.timeout.ms`, check broker health, reduce batch size |

### Consumer Lag Growing

1. Check `kafka-consumer-groups --describe`. Identify which partitions have lag.
2. Add more consumer instances (up to number of partitions).
3. Profile your consumer. Is it slow? Optimize DB queries, add caching.
4. Consider increasing partitions (plan for key ordering impact).

### Broker Disk Full

1. Reduce `retention.ms` on topics. Delete old data.
2. Add more disk or expand volumes.
3. Move low-priority topics to separate clusters.

### Rebalancing Storms

1. Increase `max.poll.interval.ms` so slow processors don't get kicked.
2. Reduce `max.poll.records` so each poll is faster.
3. Use `CooperativeStickyAssignor` to minimize partition churn during rebalance.

---

## Production Checklist

```
✅ Producers
   ✅ acks=all for critical data
   ✅ enable.idempotence=true
   ✅ Compression enabled (lz4 or snappy)
   ✅ Send failures handled (callbacks or futures)
   ✅ Meaningful message keys for ordering

✅ Consumers
   ✅ Manual offset commit for critical data
   ✅ Idempotent processing logic
   ✅ max.poll.records tuned
   ✅ CooperativeStickyAssignor
   ✅ Consumer lag monitored

✅ Topics
   ✅ Partition count planned (start with 6+)
   ✅ Replication factor = 3
   ✅ Retention configured
   ✅ Consistent naming convention

✅ Brokers
   ✅ Dedicated disks (SSDs preferred)
   ✅ min.insync.replicas = 2
   ✅ Monitoring in place (Prometheus + Grafana)
   ✅ Alerting on under-replicated partitions
```

---

## Quick Summary

| Area | Best Practice |
|------|--------------|
| Producer acks | `all` for critical, `1` for logs |
| Idempotence | Always enable |
| Compression | `lz4` for most workloads |
| Consumer commits | Manual for critical data |
| Consumer idempotency | Always implement |
| Partitions | Start with 6+, can't reduce later |
| Replication | Factor of 3 is standard |
| Monitoring | Consumer lag is metric #1 |
| Error handling | Dead Letter Topics for failed messages |

---

## Exercise

1. Configure a producer with `lz4` compression and `acks=all`. Send 1000 messages. Check broker disk usage vs no compression.
2. Set `max.poll.records=5` on a consumer. Observe how it processes smaller batches.
3. Check consumer lag for your consumer group. Is it growing or stable?
4. Create a Dead Letter Topic handler. Make your consumer throw an exception and watch the message go to DLT.

---

**Next up:** [Kafka with Spring Boot](kafka-with-springboot.md) — Build real applications with Kafka.
