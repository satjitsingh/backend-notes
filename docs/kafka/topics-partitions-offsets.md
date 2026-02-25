# Topics, Partitions & Offsets

## Why This Page Matters

These three concepts — Topics, Partitions, and Offsets — are the foundation of Kafka. If you don't understand them deeply, everything else (producers, consumers, scaling, replication) will feel confusing. This page takes it slow and makes each concept crystal clear.

---

## Topics: Where Messages Live

### What Is a Topic?

A **topic** is a named category for messages. Think of it as a **TV channel** or a **folder** in your email.

- The "Sports" channel only shows sports news.
- The "orders" topic only holds order events.
- The "payments" topic only holds payment events.

You decide what topics to create based on what kinds of events your system produces.

```bash
kafka-topics --create --topic orders --bootstrap-server localhost:9092
kafka-topics --create --topic payments --bootstrap-server localhost:9092
kafka-topics --create --topic notifications --bootstrap-server localhost:9092
```

### How Topics Work in Practice

Imagine you're building Swiggy. When different things happen in your system, events go to different topics:

| Event | Topic |
|-------|-------|
| User places an order | `orders` |
| Payment is processed | `payments` |
| Driver gets assigned | `driver-assignments` |
| User gets a notification | `notifications` |
| User clicks on a restaurant | `user-activity` |

Each topic is **independent**. Producing to the `orders` topic doesn't affect the `payments` topic. Consumers subscribe to the topics they care about.

### Topic Properties

When you create a topic, you configure:

```bash
kafka-topics --create \
  --topic orders \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --bootstrap-server localhost:9092
```

| Property | What It Means |
|----------|-------------|
| `partitions` | How many pieces to split the topic into (parallelism) |
| `replication-factor` | How many copies of each partition (durability) |
| `retention.ms` | How long to keep messages (604800000 ms = 7 days) |

!!! info "Key Insight"
    Topics are logical. You don't create them on a specific server. Kafka distributes the topic's partitions across all brokers in the cluster automatically.

---

## Partitions: The Secret to Kafka's Speed

### What Is a Partition?

A partition is a **sub-division** of a topic. When you create a topic with 3 partitions, Kafka splits the topic into 3 independent logs.

### Why Do Partitions Exist?

Think about a **highway**. If a highway has 1 lane, only 1 car can pass at a time. Traffic jam.

Now add 6 lanes. 6 cars can pass in parallel. Much faster.

Partitions are Kafka's lanes. More partitions = more messages processed in parallel = higher throughput.

```
Topic "orders" with 1 partition:
┌──────────────────────────────────────────┐
│ Partition 0: [msg1][msg2][msg3][msg4]... │  ← Everything in one lane. Bottleneck.
└──────────────────────────────────────────┘

Topic "orders" with 3 partitions:
┌──────────────────────────┐
│ Partition 0: [msg1][msg4][msg7]...  │  ← 3 lanes.
├──────────────────────────┤
│ Partition 1: [msg2][msg5][msg8]...  │  ← 3 consumers can read
├──────────────────────────┤             in parallel!
│ Partition 2: [msg3][msg6][msg9]...  │
└──────────────────────────┘
```

### How Messages Get Assigned to Partitions

When a producer sends a message, Kafka must decide which partition to put it in. There are two scenarios:

**Scenario 1: No key provided** → Round-robin

Messages are distributed evenly across partitions. Message 1 goes to Partition 0, Message 2 to Partition 1, Message 3 to Partition 2, Message 4 back to Partition 0, and so on.

```
Producer sends: msg1, msg2, msg3, msg4, msg5, msg6

Partition 0: [msg1] [msg4]
Partition 1: [msg2] [msg5]
Partition 2: [msg3] [msg6]
```

**Scenario 2: Key provided** → Hash-based

Kafka hashes the key and maps it to a partition: `hash(key) % numPartitions`. The same key ALWAYS goes to the same partition.

```
Producer sends messages with keys:

key="order-100" → hash("order-100") % 3 = 1 → Partition 1
key="order-200" → hash("order-200") % 3 = 0 → Partition 0
key="order-100" → hash("order-100") % 3 = 1 → Partition 1  (same as before!)
key="order-300" → hash("order-300") % 3 = 2 → Partition 2
key="order-100" → hash("order-100") % 3 = 1 → Partition 1  (always Partition 1!)
```

### Why Keys Matter: The Ordering Problem

This is critical. Within a single partition, messages are **strictly ordered**. Across partitions, there is **no ordering guarantee**.

**Without keys (round-robin):**

```
Events for order #100:
  1. ORDER_CREATED   → Partition 0
  2. PAYMENT_SUCCESS → Partition 2
  3. ORDER_SHIPPED   → Partition 1

Consumer might process: PAYMENT_SUCCESS → ORDER_SHIPPED → ORDER_CREATED
That's out of order! Payment before creation? 😱
```

**With keys (key = orderId):**

```
Events for order #100 (key = "order-100"):
  1. ORDER_CREATED   → Partition 1
  2. PAYMENT_SUCCESS → Partition 1
  3. ORDER_SHIPPED   → Partition 1

All in the same partition → Processed in order ✅
```

!!! tip "Interview Insight"
    "Kafka guarantees ordering within a partition, not across partitions. To maintain order for related events, use a message key. For example, using orderId as the key ensures all events for the same order go to the same partition and are processed in sequence."

### How Many Partitions Should I Create?

This is a common question. Here's a practical framework:

| Throughput Needed | Partitions | Why |
|------------------|-----------|-----|
| Low (< 1,000 msg/sec) | 3-6 | Enough for basic workloads |
| Medium (1,000-10,000 msg/sec) | 6-12 | Room to add consumers |
| High (10,000-100,000 msg/sec) | 12-30 | Parallel processing power |
| Very High (> 100,000 msg/sec) | 30-100+ | Enterprise scale |

**Rule of thumb:** Start with `max(number of consumers you expect, 6)`. You can always add more partitions later, but you **cannot reduce** them.

!!! warning "Caution"
    More partitions = more resources (file handles, memory, replication traffic). Don't set 100 partitions for a topic that gets 10 messages a day. Start small, scale as needed.

### Throughput Calculation Example

Let's say you need to process 50,000 messages per second. Each message is 1 KB. Your consumer can process 500 messages per second per partition.

**Throughput:** 50,000 msg/sec  
**Consumer capacity:** 500 msg/sec per partition  
**Partitions needed:** 50,000 / 500 = **100 partitions**

If you have 10 consumers, each can handle 10 partitions. 10 × 500 × 10 = 50,000. You're good.

If you only have 20 partitions: 20 × 500 = 10,000 msg/sec max. You're short. Add more partitions. Or optimize your consumer to process faster.

**Reverse calculation:** You have 100 partitions. Each consumer handles 1,000 msg/sec. 100 × 1,000 = 100,000 msg/sec theoretical max. Your producer can't exceed that without creating backpressure.

---

## Offsets: Your Bookmark in the Stream

### What Is an Offset?

An **offset** is a unique number assigned to each message within a partition. It's like a **page number** in a book. Offset 0 is the first message. Offset 1 is the second. And so on.

```
Partition 0:
┌─────────┬─────────┬─────────┬─────────┬─────────┐
│Offset 0 │Offset 1 │Offset 2 │Offset 3 │Offset 4 │
│ "Order  │ "Order  │ "Order  │ "Order  │ "Order  │
│  placed"│  paid"  │  shipped│  placed"│  paid"  │
└─────────┴─────────┴─────────┴─────────┴─────────┘
```

Key facts about offsets:

- Offsets are **unique per partition**. Partition 0 has its own offset 0, and Partition 1 has its own offset 0. They're independent.
- Offsets are **sequential and immutable**. They only go up. You can't insert a message at offset 2 after offset 4 already exists.
- Offsets are **never reused**. Even if old messages are deleted (retention), the numbering continues. If offset 0-99 are deleted, the next message is still offset 100, not offset 0.

### The Bookmark Analogy

Think of reading a long book over several days.

**Day 1:** You read pages 1-50. You put a bookmark at page 50.

**Day 2:** You pick up the book, see the bookmark, and start at page 51. You read to page 120. Move the bookmark.

**Day 3:** You start at page 121.

That bookmark is the **consumer offset**. It tracks "where did I stop reading?" If you close the book (consumer crashes) and open it later (consumer restarts), you find the bookmark and continue from there. You don't re-read the whole book.

### How Consumers Use Offsets

```
Partition 0: [Off 0] [Off 1] [Off 2] [Off 3] [Off 4] [Off 5] ...
                                        ↑
                              Consumer's committed offset = 3
                              "I've processed messages 0, 1, 2, 3"
                              Next poll returns messages 4, 5, ...
```

When a consumer starts up:

1. **First time ever?** → Start from `auto.offset.reset` setting (`earliest` = beginning, `latest` = now)
2. **Has a committed offset?** → Resume from where it left off

### What Happens During a Consumer Crash

Let's trace through a scenario:

```
Step 1: Consumer starts, reads offset 0, 1, 2
Step 2: Consumer commits offset 2 to Kafka ("I'm done up to offset 2")
Step 3: Consumer reads offset 3, 4
Step 4: Consumer CRASHES before committing
Step 5: Consumer restarts
Step 6: Kafka says "Your last committed offset was 2"
Step 7: Consumer re-reads offset 3, 4 (messages processed again!)
```

This is why offset management matters. If you commit too early, you might skip messages. If you commit too late (or crash before committing), you might process messages twice.

!!! tip "Interview Insight"
    "If a consumer crashes before committing its offset, it will re-read messages from the last committed offset when it restarts. This is the at-least-once delivery guarantee. To handle this, I make my consumers idempotent — they check if a message was already processed before processing it again."

### Offset Reset Policies

| Policy | When | What Happens |
|--------|------|-------------|
| `earliest` | New consumer group, no prior offset | Reads ALL messages from the very beginning |
| `latest` | New consumer group, no prior offset | Reads only NEW messages (from now onward) |
| `none` | No prior offset | Throws an error (forces explicit handling) |

```properties
# In application.yml or consumer config:
auto.offset.reset=earliest
```

Choose `earliest` when you can't afford to miss any messages (order processing). Choose `latest` when you only care about new events (live dashboard).

---

## Putting It All Together: The Full Picture

Let's trace one complete scenario to see Topics, Partitions, and Offsets working together.

**Setup:** Topic `orders` with 3 partitions.

```
Step 1: Producer sends order event
        key = "order-100", value = '{"action":"CREATED","amount":5000}'

Step 2: Kafka computes partition
        hash("order-100") % 3 = Partition 1

Step 3: Message appended to Partition 1
        Partition 1: [...existing messages...] [Offset 47: ORDER_CREATED]

Step 4: Consumer (in group "order-processors") assigned to Partition 1
        Consumer polls → gets message at Offset 47

Step 5: Consumer processes the message
        "Order 100 created. Save to database."

Step 6: Consumer commits offset 47
        Kafka records: "Group 'order-processors', Partition 1, offset = 47"

Step 7: Producer sends another event for the same order
        key = "order-100" → Partition 1 again → Offset 48: PAYMENT_SUCCESS

Step 8: Same consumer reads Offset 48 (same partition!)
        "Order 100 paid. Update status."
        Ordering guaranteed because both events are in Partition 1 ✅
```

---

## Viewing Topics, Partitions, and Offsets

### List all topics

```bash
kafka-topics --list --bootstrap-server localhost:9092
```

### Describe a topic (shows partitions, replicas, leaders)

```bash
kafka-topics --describe --topic orders --bootstrap-server localhost:9092
```

Output:

```
Topic: orders   PartitionCount: 3   ReplicationFactor: 2
  Topic: orders   Partition: 0   Leader: 1   Replicas: 1,2   ISR: 1,2
  Topic: orders   Partition: 1   Leader: 2   Replicas: 2,3   ISR: 2,3
  Topic: orders   Partition: 2   Leader: 3   Replicas: 3,1   ISR: 3,1
```

### Check consumer offsets (how far along consumers are)

```bash
kafka-consumer-groups --describe --group order-processors --bootstrap-server localhost:9092
```

Output:

```
GROUP             TOPIC   PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
order-processors  orders  0          450             450             0
order-processors  orders  1          380             385             5
order-processors  orders  2          420             420             0
```

`LAG` = how many unread messages. Partition 1 has 5 messages the consumer hasn't processed yet.

---

## Log Compaction

### What Is Log Compaction?

By default, Kafka keeps messages for a time period (retention) or until the log reaches a size limit. Then it deletes old messages. **Log compaction** is different. It keeps the **latest value per key**. Old values for the same key are deleted. The log becomes a compacted "changelog" — one message per key, with the most recent value.

### How It Works

Imagine a topic with user profile updates:

```
Offset 0: key="user-42", value={"name":"Alice","age":25}
Offset 1: key="user-99", value={"name":"Bob","age":30}
Offset 2: key="user-42", value={"name":"Alice Smith","age":26}  ← Updated
Offset 3: key="user-42", value={"name":"Alice Smith","age":27}  ← Updated again
```

With **delete** retention (default): All four messages stay until retention expires.

With **compact** retention: Kafka keeps only the latest value per key. After compaction, you might have:

```
key="user-42" → {"name":"Alice Smith","age":27}  (latest)
key="user-99" → {"name":"Bob","age":30}
```

Old values for user-42 are gone. The log is compacted. You still have the full history until compaction runs. Compaction runs periodically in the background.

### When to Use

- **Changelog topics:** User profiles, product catalog, configuration. You want the current state per key.

- **State management:** Kafka Streams uses compacted topics for state stores. You need "latest value per key" to rebuild state.

- **Deduplication:** If you have multiple updates for the same entity, compaction keeps only the latest.

### Configuration

```bash
# Create a compacted topic
kafka-topics --create --topic user-profiles \
  --config cleanup.policy=compact \
  --config min.cleanable.dirty.ratio=0.5 \
  --bootstrap-server localhost:9092
```

| Config | Meaning |
|--------|---------|
| `cleanup.policy=compact` | Use compaction instead of delete |
| `min.cleanable.dirty.ratio` | When 50% of the log is "dirty" (has old values), run compaction |

!!! info "Keys Are Required"
    Log compaction only works when messages have keys. Without a key, Kafka can't determine "latest per key." Always use keys for compacted topics.

!!! warning "Not for Event Streams"
    Don't use compaction for order events, payment events, or click streams. You need the full history. Compaction is for "current state" topics.

---

## Partition Reassignment

### What Happens When You Add Brokers

You start with 3 brokers. Your topic has 6 partitions. Each broker leads 2 partitions. You add 2 more brokers. Now you have 5 brokers. Kafka does **not** automatically move partitions to the new brokers. The new brokers sit idle. The old brokers still have all the load.

To balance the load, you must **reassign** partitions. Move some partitions from the old brokers to the new ones.

### How to Reassign Partitions

Use the `kafka-reassign-partitions` tool. You need a reassignment plan (JSON file) that specifies which partition goes to which broker.

**Step 1:** Create a reassignment plan. You can generate one automatically:

```bash
# Generate a plan to move all partitions of topic "orders" across brokers 1,2,3,4,5
kafka-reassign-partitions --bootstrap-server localhost:9092 \
  --generate \
  --broker-list 1,2,3,4,5 \
  --topics-to-move-json-file topics-to-move.json
```

The `topics-to-move.json` file:

```json
{"topics": [{"topic": "orders"}], "version": 1}
```

This outputs a proposed reassignment. Save it to `reassignment.json`.

**Step 2:** Execute the reassignment:

```bash
kafka-reassign-partitions --bootstrap-server localhost:9092 \
  --execute \
  --reassignment-json-file reassignment.json
```

**Step 3:** Verify progress:

```bash
kafka-reassign-partitions --bootstrap-server localhost:9092 \
  --verify \
  --reassignment-json-file reassignment.json
```

When it says "Reassignment of partition orders-0 is complete," you're done.

### What Happens During Reassignment

1. The new broker (leader) starts replicating the partition from the old leader.
2. Data is copied. This can take a while for large topics.
3. Once the new replica is in sync, the leader changes. The old broker stops serving that partition.
4. Consumer groups continue reading. They might see a brief pause during the leader switch.

!!! warning "Plan for Downtime"
    Reassignment can take hours for large topics. Monitor progress. Ensure you have enough disk and network capacity on the new brokers.

---

## Topic Configuration Deep Dive

When you create a topic, you configure how it behaves. Here are the most important settings.

### Retention Settings

| Config | Default | Meaning |
|--------|---------|---------|
| `retention.ms` | 168 (7 days) | How long to keep messages before deleting |
| `retention.bytes` | -1 (unlimited) | Max total size of the log before deleting old segments |

Use one or the other. Kafka deletes when either limit is hit. For a 7-day retention: `retention.ms=604800000`.

### Segment Settings

Kafka splits the log into segments (files). Each segment is a file on disk.

| Config | Default | Meaning |
|--------|---------|---------|
| `segment.ms` | 604800000 (7 days) | Rotate to a new segment after this many ms |
| `segment.bytes` | 1 GB | Rotate when segment reaches this size |

Smaller segments = compaction and deletion can run more often. Larger segments = fewer files, but slower cleanup.

### Message Size

| Config | Default | Meaning |
|--------|---------|---------|
| `max.message.bytes` | 1048588 (~1 MB) | Max size of a single message |

If your producer sends a 2 MB message, the broker rejects it. Increase this if you need larger messages. But large messages hurt throughput. Prefer smaller messages when possible.

### Compression

| Config | Default | Meaning |
|--------|---------|---------|
| `compression.type` | producer | Use producer's compression. Or set `gzip`, `snappy`, `lz4`, `zstd` |

Compression saves disk and network. `lz4` and `zstd` are fast and efficient. `gzip` is slower but compresses more.

### Example: Creating a Production Topic

```bash
kafka-topics --create --topic orders \
  --partitions 12 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config segment.bytes=1073741824 \
  --config max.message.bytes=2097152 \
  --config compression.type=lz4 \
  --bootstrap-server localhost:9092
```

---

## Common Questions (Mini-FAQ)

### Can I Delete a Topic?

Yes. But it's not immediate. Kafka marks the topic for deletion. The actual deletion happens in the background. You can also set `delete.topic.enable=true` on the broker (it's true by default in recent versions).

```bash
kafka-topics --delete --topic old-topic --bootstrap-server localhost:9092
```

!!! warning "Permanent"
    Deleting a topic removes all data. There is no undo. Make sure you really want to delete it.

### Can I Reduce Partitions?

**No.** Kafka does not support reducing the number of partitions. Once you create a topic with 10 partitions, it stays at 10. You can only add more. Plan ahead.

### What Happens to Offsets When I Add Partitions?

When you add partitions, existing offsets are unchanged. New partitions start at offset 0. Consumer groups get reassigned. Some consumers might get new partitions. They'll start from `auto.offset.reset` (earliest or latest) for those new partitions. Existing partitions keep their committed offsets.

### Can I Change the Key of a Message?

No. Once a message is in the log, it's immutable. You can't change the key, value, or partition. The only way to "fix" data is to produce a new message (e.g., a correction event) and have consumers handle both.

### What If a Consumer Reads from a Partition That Gets Reassigned?

During reassignment, the consumer might be told to stop reading from partition X. It fetches from the new partition. It uses the committed offset. Kafka handles this. The consumer might see a brief pause during the rebalance.

### How Do I Know What Offset to Start From?

For a new consumer group: Use `auto.offset.reset=earliest` to read from the beginning. Use `latest` to read only new messages. For an existing group: Kafka uses the committed offset. The consumer resumes from where it left off.

---

## Quick Summary

| Concept | What It Is | Analogy |
|---------|-----------|---------|
| Topic | Named category for messages | TV channel |
| Partition | Sub-division of a topic | Lane on a highway |
| Offset | Position number of a message in a partition | Page number in a book |
| Message Key | Determines which partition a message goes to | Last name determining which queue at the DMV |
| No key | Round-robin distribution | Taking turns |
| With key | Hash-based, same key = same partition | Same key = same lane, always |
| Ordering | Guaranteed within a partition only | Pages in a book are ordered. Books on different shelves are not. |

---

## Exercise

1. Create a topic `test-events` with 4 partitions.
2. Send 10 messages without keys. Use `kafka-console-consumer` to see which partition each goes to.
3. Send 10 messages with key `"user-1"`. Verify they all go to the same partition.
4. Use `kafka-consumer-groups --describe` to see your consumer's current offset and lag.
5. Stop the consumer, send 5 more messages, restart. Verify it picks up from where it left off.

---

**Next up:** [Producers & Consumers](producers-consumers.md) — How to send and receive messages.
