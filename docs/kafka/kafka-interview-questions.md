# Kafka Interview Questions

!!! tip "How to Use This Page"
    Each answer has multiple layers. Start with the **one-liner** for quick revision. Read the **full answer** before your interview. Use **"How to frame your answer"** to structure your response. Watch out for **red flags**—things that make you sound inexperienced.

---

## Basic Questions (Q1–Q8)

### Q1: What is Apache Kafka?

**One-liner:** Kafka is a distributed event streaming platform for publishing, storing, and processing streams of events in real time.

**What the interviewer wants to hear:** You understand it's not just a message queue. It's a *streaming platform* with persistence, replay, and high throughput. You can distinguish it from traditional queues.

**Detailed explanation:** Kafka acts as a highly scalable message broker. Producers send messages (events) to topics. Consumers read from those topics. Unlike traditional queues like RabbitMQ, Kafka **retains messages** for a configurable duration (days or weeks). Multiple consumer groups can read the same stream independently. You can replay from the beginning. It's built for high throughput—millions of messages per second on modest hardware.

**Example:** A ride-sharing app publishes "trip started" events to a topic. The billing service, the analytics service, and the driver app all consume the same stream. Each reads at its own pace. If analytics is slow, billing isn't blocked. That's the power of decoupled consumers with log retention.

**How to frame your answer:** "Kafka is a distributed event streaming platform. Producers publish events to topics, and consumers subscribe. The key differentiator is that Kafka retains messages—it's a log, not a queue. So you can replay, have multiple independent consumers, and handle very high throughput."

!!! warning "Red flags in your answer"
    - Calling it "just a message queue" (it's a log-based streaming platform)
    - Saying "messages are deleted after consumption" (they're retained)
    - Not mentioning distributed or scalability

!!! tip "Bonus points if you mention..."
    "It was originally built at LinkedIn for log aggregation and activity tracking, and it's now used by thousands of companies for event-driven architectures."

---

### Q2: What are the main components of Kafka?

**One-liner:** Producer, Consumer, Broker, Topic, Partition, and ZooKeeper/KRaft for coordination.

**What the interviewer wants to hear:** You know the core building blocks and how they fit together.

**Detailed explanation:**

| Component | Role |
|-----------|------|
| **Producer** | Sends messages to topics. Doesn't know who consumes. |
| **Consumer** | Reads messages from topics. Belongs to a consumer group. |
| **Broker** | Kafka server. Stores partitions. A cluster has multiple brokers. |
| **Topic** | Named category/stream. Like a channel or log name. |
| **Partition** | Sub-division of a topic. Enables parallelism. Messages in a partition are ordered. |
| **ZooKeeper/KRaft** | Cluster coordination—broker registry, leader election, metadata. KRaft is the newer built-in alternative to ZooKeeper. |

**Example:** Topic "orders" has 3 partitions on 3 brokers. Producer sends with key=orderId. Consumer group "payment-service" has 3 consumers—each reads one partition. That's the basic topology.

**How to frame your answer:** "The main components are producers that publish to topics, consumers that subscribe, brokers that store the data, and topics which are divided into partitions for parallelism. Coordination used to be ZooKeeper, but Kafka is moving to KRaft."

---

### Q3: What is a Topic? What is a Partition?

**One-liner:** A topic is a named stream of messages. A partition is a sub-division of a topic that enables parallel processing and ordering within that sub-division.

**What the interviewer wants to hear:** You understand that ordering is *within* a partition, not across the whole topic. You know why partitions matter for scaling.

**Detailed explanation:** A topic is like a TV channel—a named stream. A partition is like a lane on a highway. One topic can have many partitions. Messages within a partition are **ordered** (FIFO). Across partitions, there is **no ordering guarantee**. Partitions enable parallelism: you can have one consumer per partition. More partitions = more parallelism, but also more overhead.

**Example:** Topic "orders" with 3 partitions. Order A (key=user1) goes to partition 0. Order B (key=user2) goes to partition 1. Order C (key=user1) goes to partition 0. So A and C are ordered (same partition). A and B have no ordering guarantee (different partitions).

**How to frame your answer:** "A topic is a named stream. Partitions are sub-divisions for parallelism. Ordering is guaranteed within a partition, not across. So if you need ordering for related messages—like all events for one order—you use the same key so they go to the same partition."

!!! warning "Red flags in your answer"
    - Saying "messages are ordered across the whole topic" (only within partition)
    - Not connecting partitions to parallelism/consumers

!!! tip "Bonus points if you mention..."
    "The number of partitions is a key design decision—you can't decrease it easily, and it limits max consumers in a group. So you plan for future scale."

---

### Q4: What is an Offset?

**One-liner:** An offset is the unique position (ID) of a message within a partition. It's how consumers track where they are.

**What the interviewer wants to hear:** You understand that offsets are per-partition, sequential, and used for consumer progress tracking.

**Detailed explanation:** Each message in a partition has an offset—a monotonically increasing integer. Partition 0: [0, 1, 2, 3, ...]. Partition 1: [0, 1, 2, ...]. Offsets are independent per partition. Consumers commit their offset ("I've processed up to offset 42"). When they restart, they resume from 43. Offsets are stored in Kafka (internal topic `__consumer_offsets`) or externally if you use manual commit.

**Example:** Consumer reads partition 0. Processes messages 0, 1, 2. Commits offset 3. Crashes. Restarts. Resumes from offset 3. No reprocessing of 0, 1, 2.

**How to frame your answer:** "An offset is the position of a message within a partition. It's like a page number. Consumers commit offsets to track progress. On restart, they resume from the last committed offset."

---

### Q5: What is a Consumer Group?

**One-liner:** A set of consumers that share the work of reading from a topic's partitions. Each partition is read by exactly one consumer in the group.

**What the interviewer wants to hear:** You understand the load-sharing model and the relationship between partitions and consumers.

**Detailed explanation:** Rules:
- Each partition → only ONE consumer in the group
- One consumer → can read multiple partitions
- Max useful consumers = number of partitions (extra consumers sit idle)
- Different consumer groups read ALL messages independently (each group gets a full copy)

**Example:** Topic has 3 partitions. Consumer group "notifications" has 2 consumers. Consumer 1 gets partitions 0 and 1. Consumer 2 gets partition 2. Add a third consumer? Rebalance. Each gets one partition. Add a fourth? One sits idle.

**How to frame your answer:** "A consumer group shares the partitions. One partition, one consumer in that group. So with 3 partitions, you get at most 3 parallel consumers. Different groups—like 'notifications' and 'analytics'—each get all messages. Same topic, independent consumption."

!!! warning "Red flags in your answer"
    - Saying "each consumer gets a copy of each message" (only true for *different* groups; within a group, they share)
    - Not knowing that extra consumers beyond partition count are idle

---

### Q6: How does Kafka ensure fault tolerance?

**One-liner:** Through **replication**—each partition has copies (replicas) on multiple brokers. If a broker dies, a replica becomes the new leader.

**What the interviewer wants to hear:** You understand replication, leaders, followers, and leader election. You know about ISR.

**Detailed explanation:** Each partition has a leader and N replicas (followers). The leader handles reads and writes. Followers replicate. If the leader dies, a follower in the ISR (In-Sync Replicas) is elected as the new leader. ISR = replicas that are caught up. Producer can use `acks=all` to wait for all ISR replicas before considering a write successful. That prevents data loss.

**Example:** Partition 0: Leader on Broker 1, Follower on Broker 2, Follower on Broker 3. Broker 1 dies. Broker 2 (in ISR) becomes leader. Producers and consumers reconnect to Broker 2. No data loss if replication factor >= 2.

**How to frame your answer:** "Replication. Each partition has replicas on multiple brokers. The leader handles traffic; followers replicate. On leader failure, a follower from the ISR is elected. For critical data, we use acks=all so all in-sync replicas acknowledge before we consider the write done."

!!! tip "Bonus points if you mention..."
    "min.insync.replicas=2 ensures we never accept a write unless at least 2 replicas have it. So we can lose one broker and still have a copy."

---

### Q7: What is the role of ZooKeeper? What is KRaft?

**One-liner:** ZooKeeper manages Kafka cluster metadata (brokers, topics, leader election). KRaft is Kafka's new built-in consensus that replaces ZooKeeper.

**What the interviewer wants to hear:** You know Kafka is moving away from ZooKeeper. You understand the operational simplification.

**Detailed explanation:** ZooKeeper is an external distributed coordination service. Kafka used it for: broker registration, topic configuration, partition assignment, leader election. The downside: you run two systems (Kafka + ZooKeeper). KRaft (Kafka Raft) is built into Kafka 3.3+. It uses the Raft consensus algorithm for metadata. No ZooKeeper. Simpler ops, better scalability. Kafka is deprecating ZooKeeper.

**How to frame your answer:** "ZooKeeper used to handle cluster metadata and leader election. Kafka is moving to KRaft mode, which builds that into Kafka itself. Simpler to operate, no external dependency."

---

### Q8: How does Kafka handle message ordering?

**One-liner:** Messages are ordered **within a partition**, not across partitions. Use the same key for related messages to guarantee ordering.

**What the interviewer wants to hear:** You know the ordering guarantee is partition-scoped. You know the key determines partition.

**Detailed explanation:** Kafka guarantees order within a partition. Partition assignment: `hash(key) % numPartitions`. Same key → same partition → ordered. Different keys → possibly different partitions → no ordering. So for "all events for order 123," use key="order-123." They all go to one partition. Processed in order.

**Example:** key="order-123" → Partition 2. key="order-123" again → Partition 2. Both processed in order by the same consumer.

**How to frame your answer:** "Ordering is within a partition. To order related messages, use the same key—they'll hash to the same partition. So for order processing, we use orderId as the key."

!!! warning "Red flags in your answer"
    - Saying "Kafka guarantees global ordering" (only per partition)
    - Not knowing that key determines partition

---

## Intermediate Questions (Q9–Q16)

### Q9: What is the difference between Kafka and RabbitMQ?

**One-liner:** Kafka is a log-based streaming platform with retention and replay. RabbitMQ is a traditional message queue with routing flexibility. Different use cases.

**What the interviewer wants to hear:** You can articulate when to use each. You don't say "Kafka is better"—you say "it depends."

**Detailed explanation:**

| Feature | Kafka | RabbitMQ |
|---------|-------|----------|
| Model | Pub/Sub with log retention | Message queue with exchanges |
| Retention | Keeps messages (configurable) | Deletes after consumption |
| Throughput | Very high (millions/sec) | Moderate (thousands/sec) |
| Replay | Yes | No |
| Consumer model | Pull-based | Push-based |
| Routing | Topic-based (partitions) | Flexible (exchanges, routing keys) |
| Best for | Event streaming, logs, analytics | Task queues, RPC, complex routing |

**Example:** Use Kafka for event streaming—order events, click streams, logs. Use RabbitMQ for task distribution—send this email, process this PDF. If you need to replay, use Kafka. If you need complex routing (route by header, etc.), RabbitMQ might be better.

**How to frame your answer:** "Kafka is for high-throughput event streaming with retention and replay. RabbitMQ is for task queues with flexible routing. Kafka when you need multiple consumers, replay, or huge scale. RabbitMQ when you need complex routing or request-reply patterns."

!!! tip "Bonus points if you mention..."
    "You can use both in the same system—Kafka for event streaming, RabbitMQ for task distribution. They're complementary."

---

### Q10: What is ISR? Why is it important?

**One-liner:** ISR (In-Sync Replicas) is the set of replicas that are fully caught up with the leader. Leader election only happens from ISR.

**What the interviewer wants to hear:** You understand that only in-sync replicas can become leader. You know the data loss scenario.

**Detailed explanation:** Not all replicas are always caught up. A slow replica might lag. ISR = replicas that have replicated all committed messages. When the leader fails, only ISR members can be elected. If you have replication factor 3 but only the leader is in ISR (others are lagging), and the leader dies—you could lose data. `min.insync.replicas=2` means: don't accept a write unless at least 2 replicas (including leader) have it. So you always have a backup.

**How to frame your answer:** "ISR is the set of replicas that are caught up. Leader election only considers ISR. If acks=all, the producer waits for all ISR. min.insync.replicas ensures we never shrink to just the leader—so we always have a replica to promote."

!!! warning "Red flags in your answer"
    - Not knowing that a lagging replica can be removed from ISR
    - Not connecting ISR to data loss scenarios

---

### Q11: What are acks in Kafka? Which should I use?

**One-liner:** acks controls how many replicas must acknowledge a write before the producer considers it successful. 0 = none, 1 = leader, all = all ISR.

**What the interviewer wants to hear:** You know the trade-off between speed and durability. You can recommend based on use case.

**Detailed explanation:**

| acks | Behavior | Speed | Safety |
|------|----------|-------|--------|
| `0` | No confirmation | Fastest | Data loss possible (network failure, broker crash) |
| `1` | Leader confirms | Fast | Loss if leader dies before replicating |
| `all` | All ISR confirm | Slowest | No data loss (with min.insync.replicas) |

**Example:** Payments: acks=all. Logs/metrics: acks=1. Fire-and-forget metrics: acks=0 (rare).

**How to frame your answer:** "For critical data like payments, acks=all with min.insync.replicas=2. For logs or metrics where some loss is OK, acks=1. I'd avoid acks=0 unless it's truly best-effort."

!!! tip "Bonus points if you mention..."
    "acks=all doesn't mean slow if your cluster is healthy. The latency is one round-trip to the replicas. The safety is worth it for critical data."

---

### Q12: What is an Idempotent Producer?

**One-liner:** A producer that guarantees each message is written exactly once, even with retries. No duplicates from network retries.

**What the interviewer wants to hear:** You understand the duplicate problem and how idempotence solves it.

**Detailed explanation:** Without idempotence: Producer sends message. Broker receives it but the ack is lost (network). Producer retries. Broker might receive the same message twice. Duplicate! With `enable.idempotence=true`, Kafka assigns a PID (Producer ID) and sequence number. On retry, Kafka deduplicates. Same PID + sequence = already written, ignore. Result: exactly-once from producer's perspective (at-least-once delivery with dedup).

**Example:** Producer sends "order-123 created." Network glitch. Producer retries. Broker: "I already have PID 5, seq 10. Ignore." No duplicate in the log.

**How to frame your answer:** "Idempotent producer uses sequence numbers. On retry, Kafka deduplicates. So we get exactly-once semantics from the producer side. I always enable it for production—there's no downside."

---

### Q13: How does the producer decide which partition to send to?

**One-liner:** If key is null → round-robin. If key is present → hash(key) % numPartitions. Or use a custom partitioner.

**What the interviewer wants to hear:** You know the default behavior and when to use keys.

**Detailed explanation:**
1. **Key is null:** Round-robin across partitions. Good for load balancing. No ordering.
2. **Key is present:** `hash(key) % numPartitions`. Same key → same partition. Good for ordering.
3. **Custom partitioner:** Implement `Partitioner` interface. Your logic (e.g., partition by region).

**Example:** key="user-123" with 3 partitions → always partition 1 (hypothetically). All events for user-123 go to partition 1. Ordered.

**How to frame your answer:** "Default: hash of key modulo partitions. Same key, same partition—that's how we get ordering. Null key means round-robin. We can also implement a custom partitioner for things like geographic routing."

---

### Q14: What is auto.offset.reset?

**One-liner:** Tells the consumer where to start when it has no committed offset (e.g., first run or offset was invalidated).

**What the interviewer wants to hear:** You know the three options and when to use each.

**Detailed explanation:**

| Value | Behavior |
|-------|----------|
| `earliest` | Read from the beginning of the partition |
| `latest` | Read only new messages (from now) |
| `none` | Throw error if no offset exists |

**Example:** New consumer group, topic has 1M messages. earliest = process all 1M. latest = skip them, only new. none = fail (use when you want to catch misconfiguration).

**How to frame your answer:** "It's for when there's no valid offset. earliest for 'process everything'—like a new analytics consumer. latest for 'ignore old stuff'—like a real-time dashboard. none to fail fast if something's wrong."

---

### Q15: What is consumer rebalancing?

**One-liner:** When consumers join or leave a group, Kafka reassigns partitions. Consumption pauses during the rebalance.

**What the interviewer wants to hear:** You understand the triggers and the impact. You know it causes a brief pause.

**Detailed explanation:** Triggers:
- Consumer joins the group
- Consumer leaves (crash or graceful shutdown)
- New partitions added to the topic

During rebalance, all consumers stop consuming. Partitions are reassigned. Then consumption resumes. For a few seconds, nothing is processed. Frequent rebalances (e.g., consumers crashing) = problem. Long processing times can also cause rebalances (session timeout).

**How to frame your answer:** "Rebalancing happens when the group membership changes. Partitions get reassigned. There's a brief pause—usually seconds. If rebalances are frequent, we'd look at consumer stability and session timeouts."

!!! tip "Bonus points if you mention..."
    "Incremental cooperative rebalancing (Kafka 2.4+) reduces the pause—consumers don't give up all partitions at once."

---

### Q16: What are the delivery guarantees?

**One-liner:** At-most-once (may lose), at-least-once (may duplicate), exactly-once (complex). Most systems use at-least-once with idempotent consumers.

**What the interviewer wants to hear:** You know the trade-offs. You have a practical recommendation.

**Detailed explanation:**

| Guarantee | How | Trade-off |
|-----------|-----|-----------|
| At-most-once | Commit before processing | Fast, but may lose messages |
| At-least-once | Commit after processing | May get duplicates on crash |
| Exactly-once | Transactions + idempotence | Complex, some overhead |

**Practical approach:** At-least-once + idempotent consumer. Consumer processes, then commits. If crash before commit, reprocess. So duplicate possible. Make consumer idempotent: check "already processed?" before doing work. E.g., `INSERT ... ON CONFLICT DO NOTHING` or check DB for orderId.

**How to frame your answer:** "We typically use at-least-once and make consumers idempotent. Commit after processing. If we crash, we reprocess—might get a duplicate. So we design for it: check if order already exists before processing. Exactly-once is possible with transactions but adds complexity."

!!! warning "Red flags in your answer"
    - Claiming "Kafka guarantees exactly-once" without mentioning transactions and idempotence
    - Not having a strategy for duplicates

---

## Advanced Questions (Q17–Q23)

### Q17: What is Kafka Streams?

**One-liner:** A Java library for stream processing. Read from Kafka, transform, write back. No separate cluster. Runs in your app.

**What the interviewer wants to hear:** You know it's embedded, not a separate service. You can give a simple example.

**Detailed explanation:** Kafka Streams is a library. You write a Java app. It reads from Kafka topics, does transformations (map, filter, aggregate, join), and writes to output topics. State is stored in RocksDB (local) or Kafka. No Spark/Flink cluster. Just your JVM. Good for real-time aggregations, filtering, joins.

**Example:** Input topic "orders." Filter amount > 1000. Write to "high-value-orders." Or: aggregate clicks by user per hour. Output "user-hourly-clicks."

**How to frame your answer:** "Kafka Streams is a Java library for stream processing. It runs in-process—no separate cluster. You read from topics, transform, write to topics. Great for real-time aggregations and simple stream processing."

---

### Q18: What is Kafka Connect?

**One-liner:** A framework for moving data in and out of Kafka without writing application code. Source and sink connectors.

**What the interviewer wants to hear:** You know it's for data integration. You can name connectors.

**Detailed explanation:** Source connectors: pull from external systems into Kafka (e.g., MySQL, PostgreSQL, S3). Sink connectors: push from Kafka to external systems (Elasticsearch, Snowflake, S3). You configure connectors. No code. Popular: JDBC, Debezium (CDC), Elasticsearch, S3, MongoDB.

**Example:** Debezium MySQL source → Kafka. Every INSERT/UPDATE/DELETE becomes an event. Sink to Elasticsearch for search. Sink to data warehouse for analytics.

**How to frame your answer:** "Kafka Connect is for data integration. Source connectors ingest from DBs, files, etc. Sink connectors write to warehouses, search engines. We use it for CDC and data pipeline—no custom code."

---

### Q19: What is Schema Registry?

**One-liner:** A service that stores and validates schemas (Avro, JSON Schema, Protobuf) for Kafka messages. Ensures compatibility between producers and consumers.

**What the interviewer wants to hear:** You understand the compatibility problem. You know it prevents breaking changes.

**Detailed explanation:** Producers and consumers need to agree on message format. Without a registry: Producer adds a field. Old consumer breaks (can't deserialize). Schema Registry stores schemas. Producer registers schema before send. Consumer fetches schema to deserialize. You can enforce compatibility: backward, forward, full. Prevents accidental breaking changes.

**Example:** Order schema v1: orderId, product. v2: orderId, product, quantity. Backward compatible: old consumer ignores quantity. Forward compatible: new consumer handles missing quantity. Registry enforces this.

**How to frame your answer:** "Schema Registry stores schemas for Kafka messages. Producers and consumers use it for serialization. It enforces compatibility—so we don't break consumers when we add fields. We use Avro with Schema Registry in production."

---

### Q20: How do you monitor Kafka?

**One-liner:** Key metrics: consumer lag, broker disk, under-replicated partitions, request latency. Tools: Prometheus, Grafana, Confluent Control Center.

**What the interviewer wants to hear:** You know the critical metrics. Consumer lag is #1.

**Detailed explanation:**

| Metric | What It Tells You |
|--------|-------------------|
| **Consumer lag** | How far behind consumers are. High lag = consumers can't keep up |
| **Broker disk usage** | Running out of space? Retention will fail |
| **Under-replicated partitions** | Replicas falling behind. Risk of data loss on broker failure |
| **Request latency** | Producer/consumer performance. P99 matters |
| **Active controller count** | Should be exactly 1. Two = split brain |

**How to frame your answer:** "Consumer lag is the top metric—if it grows, we have a problem. Under-replicated partitions mean we're at risk. We use Prometheus and Grafana, with alerts on lag and replication."

!!! tip "Bonus points if you mention..."
    "We also monitor request rate per broker for capacity planning, and we track consumer group stability—frequent rebalances indicate issues."

---

### Q21: How do you handle duplicate messages?

**One-liner:** Make consumers **idempotent**. Use unique IDs, upserts, or deduplication logic.

**What the interviewer wants to hear:** You have concrete strategies. You don't say "Kafka guarantees no duplicates" (it doesn't, with at-least-once).

**Detailed explanation:** Strategies:
1. **Unique ID check:** Store processed message IDs. Skip if exists.
2. **Database upsert:** `INSERT ... ON CONFLICT DO NOTHING` or `UPDATE` with idempotent key.
3. **Idempotency key:** Use orderId. "If order already processed, return success." Same effect as processing.

**Example:**
```java
if (orderRepository.existsById(event.getOrderId())) {
    log.info("Already processed, skipping");
    return;
}
orderRepository.save(process(event));
```

**How to frame your answer:** "We design for at-least-once. Consumers are idempotent. We check if we've already processed—by orderId or a processed-ids table. Or we use database upserts. Duplicates become no-ops."

!!! warning "Red flags in your answer"
    - "We don't get duplicates" (with at-least-once, you can)
    - No concrete strategy

---

### Q22: What happens when a Kafka broker goes down?

**One-liner:** Controller detects failure. Leader election from ISR. Producers/consumers reconnect to new leader. No data loss if replication is sufficient.

**What the interviewer wants to hear:** You can walk through the sequence. You know when data loss can occur.

**Detailed explanation:**
1. Controller (or KRaft) detects broker down (heartbeat timeout).
2. For each partition with leader on dead broker: elect new leader from ISR.
3. Producers and consumers get metadata update. Reconnect to new leader.
4. When broker comes back: rejoins as follower, catches up, reenters ISR.

Data loss only if: the dead broker was the only ISR member, or replication factor was 1. With replication factor 2+ and min.insync.replicas=2, no loss.

**How to frame your answer:** "Controller detects the failure. New leader is elected from ISR. Clients get metadata refresh and reconnect. Brief unavailability—seconds. When the broker recovers, it catches up as a follower. No data loss with proper replication."

---

### Q23: What is the difference between Kafka and traditional message queues?

**One-liner:** Kafka is a log; traditional queues delete after consumption. Kafka retains, allows replay, supports multiple independent consumers. Queues are for task distribution.

**What the interviewer wants to hear:** You understand the fundamental model difference. Log vs. queue.

**Detailed explanation:** Traditional queue (e.g., RabbitMQ): Message is delivered to one consumer. Deleted after ack. No replay. Kafka: Message stays in the log. Multiple consumer groups each read independently. Replay from any offset. Different mental model: queue = "work to be done"; log = "events that happened."

**How to frame your answer:** "A queue is for distributing work—one consumer gets it, it's gone. Kafka is a log—events persist. Multiple consumers can read. You can replay. It's a different paradigm: event streaming vs. task queue."

---

## Scenario and Design Questions (Q24–Q28)

### Q24: Design an order processing system with Kafka.

**What the interviewer wants to hear:** You can design a realistic system. You make explicit decisions and justify them.

**Step-by-step design:**

**Step 1: Identify events and flows.**
- Order placed → validate → charge → inventory → ship → notify
- Each step can be a consumer. Or: order placed → multiple parallel consumers (payment, inventory, notification).

**Step 2: Topic design.**
- Topic "orders" for raw orders.
- Optional: "validated-orders," "payment-results" for multi-stage pipeline.
- Use orderId as key for ordering.

**Step 3: Consumer groups.**
- payment-service, inventory-service, notification-service. Each gets all orders. Independent scaling.

**Step 4: Critical decisions.**
- acks=all for order events (critical data).
- orderId as key (ordering guarantee).
- Dead Letter Topic for failed processing.
- Idempotent consumers (check if order already processed).

**Step 5: Draw the diagram.**

```
User places order
    │
    ▼
REST API → Kafka Topic: "orders" (key=orderId)
                │
                ├──▶ Payment Service (group: "payments") → Topic: "payment-results"
                ├──▶ Inventory Service (group: "inventory")
                └──▶ Notification Service (group: "notifications")
```

**How to frame your answer:** "I'd have an orders topic with orderId as key. Each downstream service is a consumer group. Payment, inventory, notifications—each gets all orders. We use acks=all, idempotent consumers, and a DLT for failures. I'd consider a separate validated-orders topic if validation is complex."

!!! tip "Bonus points if you mention..."
    "We'd monitor consumer lag for each group. Payment is critical—we'd alert if lag grows. We might use a saga pattern if we need distributed transactions across payment and inventory."

---

### Q25: How would you design a real-time analytics pipeline for user clicks?

**What the interviewer wants to hear:** You think about volume, retention, consumers, and aggregation.

**Step-by-step design:**

**Step 1: Volume.** Millions of clicks per minute. Kafka handles it. Topic "user-clicks" with many partitions (e.g., 20). Partition by userId for ordering per user if needed, or null key for load balance.

**Step 2: Consumers.**
- Real-time dashboard: Kafka Streams or Flink. Aggregate clicks per minute, per page. Push to dashboard (WebSocket or polling).
- Batch analytics: Consumer writes to data lake (S3) or warehouse. Runs hourly/daily jobs.
- ML pipeline: Consumer feeds feature store or training pipeline.

**Step 3: Retention.** Analytics might need 30 days. Configure retention accordingly.

**Step 4: Schema.** Use Schema Registry. Clicks have: userId, pageId, timestamp, sessionId, etc.

**How to frame your answer:** "Topic user-clicks, partitioned for throughput. Kafka Streams for real-time aggregates—clicks per minute, top pages. Separate consumer for batch—S3 or warehouse. Schema Registry for schema evolution. Retention based on analytics needs."

---

### Q26: Your consumer lag is growing. How do you debug and fix it?

**What the interviewer wants to hear:** You have a systematic approach. You don't just say "add more consumers."

**Step-by-step approach:**

**Step 1: Confirm the problem.** Check consumer lag. Which partition? Which consumer group?

**Step 2: Is the consumer slow or stuck?**
- Check consumer logs. Exceptions? Long processing time?
- Check if consumer is doing heavy work (DB calls, external API). Optimize or async.

**Step 3: Is the producer too fast?** Sudden traffic spike? Maybe temporary. Scale consumers.

**Step 4: Scaling options.**
- Add more consumers (up to partition count). More partitions = more parallelism (requires careful migration).
- Increase consumer instances (if partitions allow).
- Optimize consumer: batch processing, connection pooling, caching.

**Step 5: Temporary measures.** Increase retention so you don't lose data while fixing. Consider a separate "catch-up" consumer group that processes in batch.

**How to frame your answer:** "First I'd identify which partitions and which group. Check if consumers are erroring or slow. If processing is the bottleneck, optimize—maybe batch DB writes. If we need more throughput, add consumers—but we're limited by partitions. We might add partitions, but that's a bigger change. I'd also increase retention as a safety net."

!!! warning "Red flags in your answer"
    - "Just add more consumers" without checking if you're partition-limited
    - Not considering consumer-side optimization first

---

### Q27: How would you implement exactly-once processing in a Kafka consumer that writes to a database?

**What the interviewer wants to hear:** You know the options: transactional outbox, consumer transactions, or idempotent design.

**Step-by-step approach:**

**Option 1: Kafka consumer transactions (read-process-write).**
- Consumer reads from Kafka, processes, writes to DB.
- Use Kafka's transactional API: consume offsets and DB writes in one transaction.
- Complex. Supported in Kafka Streams and with some client libraries.

**Option 2: Idempotent consumer (practical).**
- At-least-once delivery.
- Consumer: check if record exists (by business key). If yes, skip. If no, process and insert.
- Duplicates become no-ops. "Exactly-once" from business perspective.

**Option 3: Transactional outbox (producer side).**
- Producer writes to DB (outbox table) and Kafka in one transaction. Or: CDC (Debezium) reads DB and publishes to Kafka. Consumer reads from Kafka. Single source of truth.

**How to frame your answer:** "For exactly-once, we'd make the consumer idempotent—use a unique key, check before insert, use upsert. That's the most practical. Kafka has transactional consume-process-produce for Streams, but for a DB sink, idempotency is simpler and robust."

---

### Q28: Design a notification system that sends email, SMS, and push when an event occurs. How does Kafka fit in?

**What the interviewer wants to hear:** One topic, multiple consumers. Each channel is independent.

**Step-by-step design:**

**Step 1: Event flow.** Event occurs (e.g., order placed). Publish to "notifications" topic. Payload: userId, type, data, preferences.

**Step 2: Consumers.** One consumer per channel:
- Email consumer: if user prefers email, send email.
- SMS consumer: if user prefers SMS or critical, send SMS.
- Push consumer: if user has app, send push.

**Step 3: Each consumer is a separate group.** So each gets all events. They filter by user preferences. Or: one "router" consumer that publishes to channel-specific topics. Simpler: one topic, three consumers, each filters.

**Step 4: Failure handling.** One channel failing (e.g., SMS gateway down) doesn't affect others. Retry and DLT per consumer.

**How to frame your answer:** "Single topic 'notifications.' Three consumer groups—email, SMS, push. Each gets all events, filters by user preference. Independent. Add WhatsApp? New consumer group. No change to producer. Retry and DLT per consumer for resilience."

---

## Top 5 Mistakes in Kafka Interviews

### 1. Confusing ordering guarantees
**Mistake:** Saying "Kafka guarantees ordering" without qualifying "within a partition."
**Fix:** Always say "ordering within a partition" and "use same key for related messages."

### 2. Ignoring consumer lag
**Mistake:** Not mentioning monitoring or consumer lag when asked about production Kafka.
**Fix:** "We monitor consumer lag. It's our top metric. High lag means we have a problem."

### 3. Over-promising exactly-once
**Mistake:** "Kafka guarantees exactly-once." It's possible but requires specific setup.
**Fix:** "We use at-least-once with idempotent consumers. Exactly-once is possible with transactions but we find idempotency simpler."

### 4. Not knowing partition-consumer relationship
**Mistake:** "We'll add 10 consumers to scale." (With 3 partitions, 7 sit idle.)
**Fix:** "Max parallel consumers = number of partitions. We'd add partitions if we need more parallelism."

### 5. Treating Kafka like a simple queue
**Mistake:** Describing it as "send message, consumer gets it, done."
**Fix:** Emphasize log retention, replay, multiple consumer groups, and event streaming.

---

## 30-Second Revision Cheat Sheet

| Topic | One-Liner |
|-------|-----------|
| **Kafka** | Distributed event streaming platform. Log retention. Replay. High throughput. |
| **Topic** | Named stream. Partition = sub-division. Order within partition. |
| **Key** | Same key → same partition → ordering. |
| **Consumer group** | Share partitions. One partition, one consumer per group. |
| **Replication** | Leader + followers. ISR. Leader election on failure. |
| **acks** | 0=none, 1=leader, all=ISR. Use all for critical. |
| **Idempotent producer** | No duplicates on retry. Enable always. |
| **Delivery** | At-least-once + idempotent consumer (most common). |
| **Consumer lag** | #1 metric. High = problem. |
| **Kafka vs RabbitMQ** | Kafka = streaming, retention, replay. RabbitMQ = task queue, routing. |

---

**Done with Kafka! Next:** [Projects](../projects/redis-api-caching.md) - Build real projects.
