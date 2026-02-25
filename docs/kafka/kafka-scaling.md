# Scaling Kafka

## Why Scaling Matters: A Food Delivery Story

Imagine you're building a food delivery app. On day one, you have 10 orders. Your system handles it easily. One server. One Kafka broker. One consumer. Everything works.

Six months later, you're doing 10,000 orders per day. That's roughly 7 orders per minute. Still manageable. But your single consumer is starting to sweat. Messages pile up during lunch rush. Lag grows.

A year later, you're at 100,000 orders per day. Lunch rush is 500 orders per minute. Your single consumer is drowning. Lag is in the thousands. Orders take minutes to process. Customers complain. Restaurants complain. You need to scale. Fast.

This is the reality of growing systems. What worked at 10 orders doesn't work at 10,000. And what works at 10,000 might not work at 1 million. Kafka is designed to scale. But you need to know how. This page teaches you exactly that.

---

## The Three Dimensions of Scaling

Kafka scaling happens in three places:

1. **Producers** — Can you send messages fast enough?
2. **Consumers** — Can you read and process messages fast enough?
3. **Brokers** — Can Kafka store and serve the data?

We'll cover each. We'll use analogies. We'll walk through real scenarios. By the end, you'll know how to scale a Kafka system from zero to millions of messages.

---

# Producer Scaling

## The Problem: Sending One Message at a Time

Every message you send has overhead. A network round-trip. Serialization. Connection setup. Sending 1000 messages as 1000 separate requests is slow. The network becomes the bottleneck.

Think about it this way: If you're moving 100 boxes from your house to a warehouse, do you make 100 trips with one box each? Or do you rent a truck and make one trip with 100 boxes? The truck is batching. One trip. Much faster.

---

## Batching: The Delivery Truck Analogy

### Without Batching

You send message 1. Wait for ack. Send message 2. Wait for ack. Send message 3. Wait for ack. Each message is a separate network request. If each round-trip takes 1ms, 1000 messages take 1 second. And you're not even using the network efficiently. Small packets. Lots of overhead.

### With Batching

You collect messages. Wait a few milliseconds (`linger.ms`). Or until you have enough bytes (`batch.size`). Then you send them all in one request. One round-trip. 100 messages in one go. Maybe 10 round-trips for 1000 messages instead of 1000. 100x fewer network calls.

**Key settings:**

| Setting | What It Does | Recommended |
|---------|--------------|-------------|
| `batch.size` | Max bytes per batch before sending | 16384 (16KB) to 65536 (64KB) |
| `linger.ms` | How long to wait for more messages | 5-20 ms |

```
linger.ms=0:  Send immediately. Low latency. Good for real-time.
linger.ms=10: Wait 10ms. Batch fills. Higher throughput. Slight latency cost.
linger.ms=100: Wait 100ms. Big batches. Max throughput. Higher latency.
```

!!! tip "Trade-off"
    Low linger = low latency, lower throughput. High linger = higher throughput, higher latency. For event streaming (orders, clicks), 5-20ms is usually a good balance.

---

## Compression: Fitting More in the Truck

Even with batching, you're sending a lot of data. Text compresses well. JSON compresses very well. Enabling compression means you send fewer bytes over the network. Less bandwidth. Faster transfer. Less disk usage on the broker.

### The Analogy

Your truck can hold 100 boxes. But if you compress the boxes (vacuum seal them), you can fit 200 in the same truck. One trip. Double the cargo. That's compression.

| Algorithm | Speed | Compression Ratio | When to Use |
|-----------|-------|-------------------|-------------|
| none | Fastest | 1x | Don't use |
| gzip | Slow | Best | When bandwidth is very limited |
| snappy | Fast | Good | General purpose, balanced |
| lz4 | Fastest | Good | Low latency, good compression |
| zstd | Fast | Great | Modern choice, best balance |

```properties
compression.type=snappy
```

### Compression Benchmarks (Approximate)

Real numbers vary by data. JSON, log text, and repetitive data compress well. Binary or already-compressed data does not. These are typical for JSON-like payloads:

| Algorithm | Compression Ratio | Throughput (relative) | CPU Overhead |
|-----------|-------------------|------------------------|--------------|
| none | 1.0x | 100% (baseline) | None |
| lz4 | 2.5–3.5x | 80–90% | Low |
| snappy | 2.0–3.0x | 70–80% | Medium |
| zstd | 3.0–4.0x | 60–75% | Medium |
| gzip | 3.5–5.0x | 40–50% | High |

**Example:** 1 MB of JSON. Uncompressed: 1 MB. With snappy: ~350 KB. With gzip: ~200 KB. Snappy gives you 65% size reduction with modest CPU cost. Gzip gives 80% but uses more CPU. For most workloads, snappy or lz4 is the sweet spot. zstd often beats both—better ratio than snappy, faster than gzip.

!!! tip "Interview Insight"
    "I'd use `snappy` or `lz4` for a good balance of speed and compression. `zstd` is newer and often better. Avoid `gzip` unless you're bandwidth-bound. In benchmarks, snappy typically gives 2–3x compression on JSON with minimal latency impact."

---

## Multiple Producer Instances

Your application might run on multiple servers. Each server has its own producer. They all write to the same topic. Kafka handles it. No coordination needed. Just run more instances.

```
App Instance 1 → Producer → Kafka
App Instance 2 → Producer → Kafka
App Instance 3 → Producer → Kafka
```

Throughput scales linearly. Add more app instances. Get more producers. More messages per second.

---

# Consumer Scaling

## The Rule: One Partition Per Consumer (Max)

Remember this. It's the most important rule for consumer scaling: **The maximum number of useful consumers in a group equals the number of partitions.** Add a 4th consumer to a 3-partition topic? One consumer sits idle. Forever.

### Why?

Each partition is assigned to exactly one consumer in the group. Partitions can't be split. So 3 partitions = max 3 busy consumers. The 4th has nothing to do.

---

## Adding Consumers Step by Step

Let's walk through scaling consumers. Topic "orders" has 3 partitions.

### Step 1: One Consumer

```
Consumer 1 → Partition 0, Partition 1, Partition 2
```

One consumer reads everything. It's the bottleneck. If it processes 100 msg/sec and you produce 300 msg/sec, lag grows.

### Step 2: Add a Second Consumer

```
Consumer 1 → Partition 0, Partition 1
Consumer 2 → Partition 2
```

Rebalance happens. Partitions redistributed. Now two consumers. Better. Maybe you can handle 200 msg/sec. If you still produce 300, lag grows. Add more.

### Step 3: Add a Third Consumer

```
Consumer 1 → Partition 0
Consumer 2 → Partition 1
Consumer 3 → Partition 2
```

Perfect balance. Each consumer has one partition. Max parallelism for 3 partitions. If each does 100 msg/sec, you handle 300. You're keeping up.

### Step 4: Need More? Add Partitions First

You're producing 600 msg/sec now. 3 consumers can't keep up. You need 6 consumers. But you only have 3 partitions. So first, add partitions:

```bash
kafka-topics --alter --topic orders \
  --partitions 6 \
  --bootstrap-server localhost:9092
```

!!! warning "Caution"
    Increasing partitions **cannot be undone**. Also, existing keys may map to different partitions. Ordering for a key is still guaranteed (same key, same partition), but the partition number might change. Plan carefully.

Now add 3 more consumers. Rebalance. Each gets one partition. 6 consumers, 6 partitions. You handle 600 msg/sec.

---

## Consumer Lag: The Factory Assembly Line Analogy

**Consumer lag** = how far behind the consumer is from the latest message in the partition.

Imagine a factory assembly line. Boxes move down the line. Workers take boxes and process them. If workers are fast, they keep up. The line stays clear. If workers are slow, boxes pile up. The line backs up. Lag.

**In Kafka:** Messages arrive in the partition. The consumer reads and processes. If the consumer is slow, messages pile up. Lag = (latest offset) - (consumer's current offset). Lag of 0 means "fully caught up." Lag of 10,000 means "10,000 messages behind."

### Why Lag Matters

- **Lag growing:** Your consumer can't keep up. You're falling further behind. Eventually, you're processing data from hours ago. Real-time? Forget it.
- **Lag stable but high:** You're keeping up with new messages, but you have a backlog. You'll eventually catch up. Or not, if produce rate equals consume rate.
- **Lag near zero:** Healthy. Consumer is keeping up.

### How to Monitor Lag

```bash
kafka-consumer-groups --describe \
  --group order-processor \
  --bootstrap-server localhost:9092
```

Output:

```
GROUP           TOPIC   PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
order-processor orders  0          950             1000            50
order-processor orders  1          1100            1100            0
order-processor orders  2          800             900             100
```

LAG = LOG-END-OFFSET - CURRENT-OFFSET. Partition 0 has lag 50. Partition 2 has lag 100. Partition 1 is caught up.

### Fixing High Lag

1. **Add more consumers** (up to partition count)
2. **Add more partitions** (then add consumers)
3. **Optimize processing** (faster code, better algorithms)
4. **Increase max.poll.records** (process more per batch)
5. **Parallelize within consumer** (multiple threads processing from the same partition—advanced, use carefully)

---

# Broker Scaling

## The Warehouse Expansion Analogy

Your warehouse is full. Shelves are packed. Workers are overwhelmed. You have two options: optimize the current warehouse (bigger shelves, faster workers) or build a new warehouse (add a broker).

**Adding a broker** = adding a new server to the cluster. It can hold new partitions. It can take over replicas from overloaded brokers. The cluster gets more capacity.

### How It Works

When you add Broker 4 to a 3-broker cluster, the new broker is empty. Existing partitions don't automatically move. You have to **reassign** them. Kafka has a tool for that: `kafka-reassign-partitions`.

```bash
# Create a reassignment plan (simplified - real usage needs a JSON file)
kafka-reassign-partitions --execute \
  --reassignment-json-file reassignment.json \
  --bootstrap-server localhost:9092
```

The reassignment moves partition replicas to the new broker. Data is copied. Eventually, the load is balanced. Broker 1 had 8 partitions. Now it has 6. Broker 4 has 2. Better balance.

### When to Add Brokers

- Disk space running low on existing brokers
- CPU or network saturated
- Need higher replication (more copies for durability)
- Planning for growth

---

## Horizontal vs Vertical Scaling

You have two ways to add capacity: **horizontal** (more machines) and **vertical** (bigger machines). Kafka favors horizontal. Here's when to use each.

### Horizontal Scaling: Add More Brokers

**What it means:** Add Broker 4, 5, 6 to the cluster. Spread partitions across them. More disk. More CPU. More network capacity.

**When to use:**
- Disk is full on existing brokers
- You've hit the limit of useful consumers (partition count)
- You need more replication capacity
- Cluster is I/O bound (disk or network)

**Pros:** Linear scaling. No single point of failure. Add capacity incrementally.
**Cons:** More machines to operate. Rebalancing takes time. Network overhead between brokers.

### Vertical Scaling: Upgrade Existing Brokers

**What it means:** Give each broker more CPU, more RAM, bigger/faster disks. Same number of brokers, more power each.

**When to use:**
- You have few brokers (1–3) and can't add more yet
- Single broker is the bottleneck (e.g., controller + data on same node)
- Disk I/O is slow—upgrade to SSDs
- CPU is maxed—upgrade to more cores

**Pros:** Simpler. No rebalancing. Fewer machines.
**Cons:** There's a ceiling. You can't infinitely upgrade one machine. Cost per additional unit of capacity goes up. Single machine failure still affects multiple partitions.

### The Kafka Way

Kafka is designed for horizontal scaling. Add brokers. Add partitions. Add consumers. Vertical scaling helps in the short term (faster disks, more RAM for page cache). But long-term growth is horizontal. Plan for it.

!!! tip "Interview Insight"
    "I'd scale horizontally first—add brokers and partitions. Vertical scaling is useful when we're I/O bound and can upgrade disks, or when we have a small cluster and need a quick win. But Kafka's model is horizontal. That's how we scale to millions of messages."

---

## Scaling Story: How Swiggy Handles 1 Million Orders

Let's walk through a fictional but realistic scaling story. Swiggy (or any food delivery platform) at scale.

### Phase 1: Startup (1,000 orders/day)

- 1 broker
- 1 topic "orders" with 3 partitions
- 1 consumer in group "order-processor"
- Producer: Single app server, no batching, acks=1
- Works fine. Lag near zero.

### Phase 2: Growth (50,000 orders/day)

- Lag starts growing during lunch rush (12-2 PM)
- **Fix 1:** Add 2 more consumers. Now 3 consumers for 3 partitions. Perfect.
- **Fix 2:** Enable producer batching. linger.ms=10, batch.size=32KB. Throughput improves.
- **Fix 3:** Enable compression. snappy. Network load drops.
- 3 brokers for redundancy. Replication factor 2.

### Phase 3: Scale (500,000 orders/day)

- 6 partitions. 6 consumers. Still good.
- But brokers are sweating. Disk I/O high. **Fix:** Add 2 more brokers. Reassign partitions. Spread load.
- Consumer lag spikes during dinner rush. **Fix:** Optimize order processing. Cache restaurant data. Reduce DB calls. Processing time per message drops from 50ms to 10ms. Lag drops.

### Phase 4: Hypergrowth (1,000,000+ orders/day)

- 12 partitions. 12 consumers. 6 brokers.
- Producer: 10 app server instances. Each produces. Batching + compression. Idempotence enabled.
- Consumer: 12 consumer instances. Each processes one partition. Async processing where possible. Batch DB writes.
- Monitoring: Lag alerts. If lag > 1000 for 5 minutes, page on-call. Broker disk alerts. Consumer group health checks.
- Capacity planning: Peak is 3x average. System designed for 3x current load. Headroom for growth.

!!! example "Key Takeaway"
    Scaling is iterative. You don't build for 1 million on day one. You start small. You monitor. You add consumers when lag grows. You add partitions when you need more parallelism. You add brokers when disk or CPU is saturated. You tune as you go.

---

## Capacity Planning: How Many Partitions Do I Need?

This is the most common question. There's no perfect formula. But here are guidelines.

### Throughput-Based

- Estimate messages per second per topic.
- Each partition can handle roughly 10-50 MB/sec (depends on message size, batching, etc.).
- For 1000 msg/sec at 1KB each = 1 MB/sec. One partition could handle it. But you want parallelism. Start with at least 3-6 for a production topic.

### Consumer-Based

- How many consumers do you want to run? That's your minimum partition count.
- Want 10 consumers? Need at least 10 partitions.
- Add some headroom. Maybe 12 or 15. So you can add a few more consumers without altering the topic.

### Retention-Based

- More partitions = more files on disk. More overhead. For small topics, don't over-partition.
- For large topics (millions of messages), more partitions help with parallelism and distribution.

### Practical Rule of Thumb

- **Small topic (< 10 msg/sec):** 3 partitions. Simple.
- **Medium topic (10-1000 msg/sec):** 6-12 partitions. Room to scale.
- **Large topic (> 1000 msg/sec):** 12-50+ partitions. Match your consumer count. Plan for 2-3x growth.

!!! warning "Remember"
    You can increase partitions later. You cannot decrease. Start conservative. Add when needed.

### Capacity Planning: Worked Example

Let's size a system for a food delivery app. **Requirements:** 10,000 orders per day peak, 1 KB per message, 7-day retention, 6 consumers.

**Step 1: Throughput**
- 10,000 orders/day ≈ 0.12 msg/sec average. Peak might be 3x = 0.36 msg/sec. Round up: 1 msg/sec.
- 1 msg/sec × 1 KB = 1 KB/sec. One partition handles 10-50 MB/sec. Throughput is not the bottleneck.

**Step 2: Partitions**
- Need 6 consumers. Minimum 6 partitions. Add headroom: 9 or 12 partitions. Choose 12.

**Step 3: Storage**
- 10,000 msg/day × 1 KB × 7 days = 70 MB per partition. 12 partitions = 840 MB. With replication factor 2 = 1.68 GB total. Tiny. One broker is enough for storage.

**Step 4: Brokers**
- Replication factor 2 → need 2 brokers minimum. For production, use 3 for better availability and balance.

**Result:** 3 brokers, 12 partitions, replication 2. Handles 10K orders/day with room to grow.

**Scale up:** 100,000 orders/day. 100K × 1 KB × 7 = 700 MB per partition. 12 partitions = 8.4 GB. Replication 2 = 16.8 GB. Still fine. 1M orders/day = 84 GB. Plan for more disk or more brokers.

---

## Performance Tuning Checklist

### Producer Side

| Item | What to Do | Why |
|------|------------|-----|
| acks | Use acks=1 for speed, acks=all for safety | Trade latency for durability |
| compression | Enable snappy or lz4 | Less network, less disk |
| batch.size | 16KB-64KB | Bigger batches = more throughput |
| linger.ms | 5-20 ms | Allow batching |
| idempotence | Always true in production | Prevent duplicates |
| retries | 3 or more | Handle transient failures |

### Consumer Side

| Item | What to Do | Why |
|------|------------|-----|
| Partitions | Match consumer count | Max parallelism |
| enable.auto.commit | false for critical apps | Control when you commit |
| max.poll.records | 500-1000 | Balance batch size and processing time |
| session.timeout.ms | 30-45 seconds | Allow slow processing |
| max.poll.interval.ms | 5 minutes or more | If processing is slow, increase this |

### Broker Side

| Item | What to Do | Why |
|------|------------|-----|
| Brokers | Add when disk/CPU saturated | More capacity |
| Replication | 2 or 3 for production | Durability |
| Disk | Use SSDs for better I/O | Kafka is disk-bound |
| num.network.threads | Increase if needed | Handle more connections |
| num.io.threads | Increase if needed | Handle more requests |

---

## Monitoring: Key Metrics Explained

### Producer Metrics

**record-send-rate:** Messages per second sent. Is your producer keeping up with your app's needs?

**record-error-rate:** Failed sends. Should be 0 or near 0. If high, check broker health, network, serialization.

**request-latency-avg:** How long for the broker to respond. High latency = network issues or broker overload.

### Consumer Metrics

**records-consumed-rate:** Messages per second consumed. Compare to produce rate. Should be close.

**records-lag:** The most important metric. How far behind. Alert if lag > threshold for too long.

**fetch-latency-avg:** How long to fetch from broker. High = broker or network issues.

### Broker Metrics

**under-replicated-partitions:** Partitions with replicas that are lagging. Should be 0. If > 0, replicas are unhealthy.

**offline-partitions-count:** Partitions with no leader. Should be 0. If > 0, data loss risk.

**request-handler-avg-idle-percent:** How busy the broker is. Low = overloaded. Consider adding brokers.

**disk-usage:** Disk space per broker. Alert when > 80%. Plan for expansion.

### How to Monitor

- **JMX:** Kafka exposes metrics via JMX. Use Prometheus + JMX exporter, or Grafana + Kafka dashboards.
- **kafka-consumer-groups:** CLI for lag. Script it. Alert on lag.
- **Confluent Control Center / Kafka UI:** Web UIs that show broker health, consumer lag, topic config.

!!! info "Alerting Checklist"
    Set up alerts for: consumer lag > 1000, under-replicated partitions > 0, broker disk > 80%, consumer group rebalance frequency. These catch most production issues.

---

## Scaling Decision Flowchart

```
Is your producer too slow?
├── Yes → Enable batching (batch.size, linger.ms)
│         Enable compression (snappy/lz4)
│         Run multiple producer instances
└── No

Is your consumer too slow? (high lag)
├── Yes → Are there idle consumers?
│         ├── No → Add more partitions, then add consumers
│         └── Yes → Optimize processing logic
│                   Increase max.poll.records
└── No

Is your broker overloaded? (high CPU, disk, request latency)
├── Yes → Add more brokers
│         Reassign partitions to spread load
│         Use SSDs
└── No → You're good! 🎉
```

---

## Throughput Numbers (Approximate)

Real-world Kafka performance varies. Hardware, network, message size, batching—all matter. These are rough guidelines:

| Setup | Approximate Throughput |
|-------|------------------------|
| Single broker, single partition | ~100K messages/sec |
| 3 brokers, 12 partitions | ~500K messages/sec |
| 10 brokers, 100 partitions | ~2M messages/sec |
| LinkedIn (production) | ~7 trillion messages/day |

---

## Quick Summary

| What to Scale | How | Limit |
|---------------|-----|-------|
| Producers | Batching, compression, multiple instances | No hard limit |
| Consumers | Add consumers to group | Max = partition count |
| Partitions | Increase partition count | Cannot decrease later |
| Brokers | Add more servers | No hard limit |
| Consumer Lag | Add consumers, optimize processing | Monitor constantly |

---

## Exercise

1. Create a topic with 3 partitions. Start 1 consumer. Check lag.
2. Send 10,000 messages rapidly. Watch lag increase.
3. Add 2 more consumers to the group. Watch lag decrease.
4. Experiment: What happens when you add a 4th consumer (more than partitions)?

---

**Next up:** [Use Cases](kafka-usecases.md) - Where Kafka is used in the real world.
