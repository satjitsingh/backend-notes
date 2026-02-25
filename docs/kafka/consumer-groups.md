# Consumer Groups Explained

## Why Consumer Groups Exist

Imagine a pizza restaurant with one delivery person. Orders pile up. Customers wait. One person can't deliver fast enough.

Now hire 3 delivery people and divide the area: Person A delivers to Zone 1, Person B to Zone 2, Person C to Zone 3. Three times the throughput. No overlap. Each zone is covered by exactly one person.

That's exactly what Kafka consumer groups do. Multiple consumers share the work of reading from a topic's partitions.

---

## What Is a Consumer Group?

A **consumer group** is a set of consumers that work together to read from a topic. They share the same `group.id`. Kafka divides the topic's partitions among the consumers in the group so that each partition is read by exactly one consumer.

```java
// Consumer 1 and Consumer 2 are in the same group
props.put("group.id", "order-processors");
```

### The Core Rules

1. **Each partition is assigned to exactly ONE consumer in the group.** No two consumers in the same group read the same partition.
2. **One consumer can read from multiple partitions.** If there are 6 partitions and 2 consumers, each consumer gets 3 partitions.
3. **If consumers > partitions, extra consumers sit idle.** They're standby. If an active consumer dies, an idle one takes over.

---

## Consumer Group Coordinator

### Which Broker Is the Coordinator?

Each consumer group has a **coordinator** — one broker responsible for that group. The coordinator manages membership, partition assignment, and offset commits.

### How the Coordinator Is Chosen

Kafka uses a deterministic formula: hash the `group.id`, modulo the number of partitions in `__consumer_offsets`. The partition leader for that partition is the coordinator.

```
group.id = "order-processors"
__consumer_offsets has 50 partitions
hash("order-processors") % 50 = 23

Partition 23 of __consumer_offsets → Leader broker is the coordinator for "order-processors"
```

When a consumer joins a group, it sends a "FindCoordinator" request to any broker. That broker tells it which broker is the coordinator. The consumer then talks directly to the coordinator for joins, heartbeats, and commits.

### What the Coordinator Does

- **Tracks membership:** Who is in the group? Who sent a heartbeat recently?
- **Triggers rebalances:** When a consumer joins or leaves, the coordinator starts a rebalance.
- **Stores offsets:** When a consumer commits, the coordinator writes to `__consumer_offsets`.
- **Assigns partitions:** The coordinator (or the consumer, depending on protocol) assigns partitions to each member.

!!! info "Coordinator failure"
    If the coordinator broker dies, the group gets a new coordinator (the new leader of that partition). Consumers reconnect automatically. There may be a brief pause during the handover.

---

## Walkthrough: Every Possible Scenario

Let's use a topic with **4 partitions** and see what happens as we add and remove consumers.

### Scenario 1: 1 Consumer, 4 Partitions

```
Topic: "orders" (4 partitions)
Group: "order-processors"

Consumer 1 → Partition 0, Partition 1, Partition 2, Partition 3

One person doing all the work. It works, but slow.
```

### Scenario 2: 2 Consumers, 4 Partitions

```
Consumer 1 → Partition 0, Partition 1
Consumer 2 → Partition 2, Partition 3

Work is split. Twice the speed.
```

### Scenario 3: 4 Consumers, 4 Partitions (Sweet Spot)

```
Consumer 1 → Partition 0
Consumer 2 → Partition 1
Consumer 3 → Partition 2
Consumer 4 → Partition 3

Perfect distribution. Maximum parallelism.
```

### Scenario 4: 6 Consumers, 4 Partitions (Over-provisioned)

```
Consumer 1 → Partition 0
Consumer 2 → Partition 1
Consumer 3 → Partition 2
Consumer 4 → Partition 3
Consumer 5 → IDLE (no partition to read)
Consumer 6 → IDLE (no partition to read)

Two consumers have nothing to do. Wasted resources.
```

!!! warning "Key Rule"
    The maximum number of useful consumers in a group equals the number of partitions. Adding more consumers than partitions gives you standby capacity, not more throughput. If you need more consumers, add more partitions first.

### Scenario 5: Consumer 3 Crashes

```
Before crash:
  Consumer 1 → Partition 0
  Consumer 2 → Partition 1
  Consumer 3 → Partition 2  ← CRASHES
  Consumer 4 → Partition 3

Kafka detects Consumer 3 is dead (no heartbeat).
Rebalance happens.

After rebalance:
  Consumer 1 → Partition 0, Partition 2  ← Takes over P2
  Consumer 2 → Partition 1
  Consumer 4 → Partition 3

Messages in Partition 2 continue to be processed. No data lost.
Consumer 1 reads from P2's last committed offset.
```

### Scenario 6: New Consumer Joins

```
Before:
  Consumer 1 → Partition 0, Partition 1, Partition 2
  Consumer 2 → Partition 3

Consumer 3 joins the group.
Rebalance happens.

After:
  Consumer 1 → Partition 0, Partition 1
  Consumer 2 → Partition 3
  Consumer 3 → Partition 2

Work is redistributed more evenly.
```

---

## Rebalancing: How It Works

**Rebalancing** is the process where Kafka reassigns partitions to consumers. It happens when:

- A new consumer joins the group
- A consumer leaves the group (crash or graceful shutdown)
- New partitions are added to the topic
- A consumer is considered "dead" (no heartbeat within `session.timeout.ms`)

### What Happens During a Rebalance

```
Step 1: Kafka detects a change (consumer joined/left)
Step 2: ALL consumers in the group pause reading
Step 3: Kafka reassigns partitions (rebalance)
Step 4: Each consumer gets its new partition assignment
Step 5: Consumers resume reading from committed offsets
```

!!! warning "The Pause"
    During rebalancing, **no messages are consumed**. This is a brief pause (usually seconds), but in high-throughput systems, even a few seconds can cause lag. This is why you should avoid frequent rebalances.

### How Long Does Rebalancing Take?

Typical rebalance: **2-10 seconds**. It depends on:

- **Group size:** More consumers = more metadata to exchange. 100 consumers take longer than 5.
- **Protocol:** Eager rebalancing (default with Range/RoundRobin) pauses everyone. Cooperative rebalancing can be faster for incremental changes.
- **Network latency:** Consumers must talk to the coordinator. Slow networks add time.
- **Processing during join:** If consumers are in the middle of a long `poll()` → `process()` cycle, they must finish or hit `max.poll.interval.ms` before they can participate. A slow consumer can delay the whole rebalance.

**What affects it:** Large groups, slow networks, and consumers that take a long time to respond to the "revoke partitions" request all increase rebalance duration. In production, aim for rebalances under 30 seconds. If they're longer, investigate: are consumers slow to respond? Is the group too large?

### Causes of Unnecessary Rebalancing

| Problem | Why It Happens | How to Fix |
|---------|---------------|-----------|
| Consumer takes too long to process | No heartbeat sent, Kafka thinks it's dead | Increase `max.poll.interval.ms` |
| Slow startup | Consumer joins, then immediately seems unresponsive | Increase `session.timeout.ms` |
| Network glitch | Brief disconnect, Kafka triggers rebalance | Increase `session.timeout.ms` |
| Too many consumers starting at once | Each one triggers a rebalance | Use static group membership |

### Key Configuration

```properties
# How long before a consumer is considered dead (no heartbeat)
session.timeout.ms=45000

# How often the consumer sends heartbeats
heartbeat.interval.ms=3000

# Max time between poll() calls before consumer is kicked out
max.poll.interval.ms=300000

# Max messages returned per poll
max.poll.records=500
```

!!! tip "Think about it this way"
    `session.timeout.ms` is like a check-in deadline. The consumer must send a heartbeat before this time runs out, or Kafka considers it dead. `heartbeat.interval.ms` is how often it sends the "I'm alive" signal. Set heartbeat to about 1/3 of the session timeout.

---

## Multiple Consumer Groups: Independent Reading

Different consumer groups read the **same messages independently**. This is one of Kafka's most powerful features.

```
Topic: "orders" (3 partitions)

Group A: "payment-service" (2 consumers)
  Consumer A1 → Partition 0, Partition 1
  Consumer A2 → Partition 2

Group B: "notification-service" (1 consumer)
  Consumer B1 → Partition 0, Partition 1, Partition 2

Group C: "analytics-service" (3 consumers)
  Consumer C1 → Partition 0
  Consumer C2 → Partition 1
  Consumer C3 → Partition 2
```

**Key insight:** Each group has its own offset tracking. Group A might be at offset 500, Group B at offset 480 (slower), Group C at offset 500. They don't interfere with each other.

### Real-World Example

When a user places an order on Swiggy:

```
Order Service produces: "ORDER_PLACED" → Topic "orders"

Payment Service (group: "payments")    → Reads the event → Charges customer
Kitchen Service (group: "kitchens")    → Reads the event → Notifies restaurant
Rider Service (group: "riders")        → Reads the event → Assigns a delivery partner
Notification Service (group: "notifs") → Reads the event → Sends confirmation SMS
Analytics Service (group: "analytics") → Reads the event → Updates dashboards
```

One event. Five services. Each reads it independently. If the Analytics Service goes down for an hour, it catches up when it comes back — the messages are still in Kafka.

---

## Partition Assignment Strategies

Kafka has different strategies for how it assigns partitions to consumers within a group.

### Range Assignor (Default)

Assigns partitions in order, grouped per topic.

```
Topic A: P0, P1, P2
Topic B: P0, P1, P2
Consumers: C1, C2

C1 → Topic A: P0, P1  |  Topic B: P0, P1
C2 → Topic A: P2      |  Topic B: P2

C1 gets more load. Not perfectly even.
```

### Round Robin Assignor

Distributes partitions one at a time across consumers.

```
Topic A: P0, P1, P2
Topic B: P0, P1, P2
Consumers: C1, C2

C1 → A-P0, A-P2, B-P1
C2 → A-P1, B-P0, B-P2

More even distribution.
```

### Sticky Assignor

Tries to keep the same assignment as before. Minimizes partition movement during rebalancing. Less disruption.

### Cooperative Sticky (Recommended)

Like Sticky, but allows **incremental rebalancing**. Only the partitions that need to move are reassigned. Other consumers keep reading without pause.

```properties
partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

!!! tip "Interview Insight"
    "In production, I use the CooperativeStickyAssignor. It enables incremental rebalancing, which means consumers that aren't affected by the rebalance continue processing without pause. This reduces the impact of rebalances on throughput."

---

## Rebalancing Protocols Deep Dive

### Eager Rebalancing (Default with Range, RoundRobin, Sticky)

**How it works:** When a rebalance is triggered, ALL consumers in the group stop reading. They revoke ALL their partitions. The coordinator reassigns partitions from scratch. Every consumer gets a new assignment. Then everyone resumes.

**Problem:** The entire group pauses. Even consumers that didn't lose any partitions must stop and wait. If you have 10 consumers and 1 dies, 9 healthy consumers pause for no good reason. Throughput drops to zero for the duration.

**When it happens:** Every time you use Range, RoundRobin, or the old Sticky assignor. Any membership change triggers a full stop.

### Cooperative Rebalancing (CooperativeStickyAssignor)

**How it works:** Rebalancing happens in rounds. In round 1, only the consumer that needs to give up partitions (e.g., the one that's leaving) revokes them. Other consumers keep reading. The coordinator assigns the revoked partitions to the remaining consumers. In round 2, if a new consumer joined, it might get some partitions from existing consumers — but only those consumers revoke. Others keep going.

**Benefit:** Consumers that aren't affected never pause. If consumer 3 dies, consumers 1, 2, and 4 might keep processing their partitions while consumer 3's partitions are reassigned. Much less disruption.

**Protocol:** Set `partition.assignment.strategy=CooperativeStickyAssignor`. The consumer must support the "cooperative" rebalance protocol. Modern Kafka clients do.

### What Happens During Each

| Event | Eager | Cooperative |
|-------|-------|-------------|
| Consumer leaves | All pause, full reassign | Only affected partitions move |
| Consumer joins | All pause, full reassign | New consumer gets share, minimal revocation |
| New partition added | All pause, new partition assigned | New partition assigned, others keep reading |

!!! tip "Upgrade path"
    If you're on Range or RoundRobin, try CooperativeStickyAssignor. You'll see fewer "stop the world" rebalances. Especially helpful for large groups or high-throughput topics.

---

## Static Group Membership

By default, every time a consumer restarts, Kafka treats it as a "new" consumer and triggers a rebalance. If your consumers restart frequently (e.g., during deployments), this causes constant rebalancing.

**Static group membership** assigns a fixed identity to each consumer. If a consumer restarts with the same ID, Kafka knows it's the same consumer and skips the rebalance (as long as it comes back within `session.timeout.ms`).

```properties
group.instance.id=consumer-1
```

This is very useful for Kubernetes deployments where pods restart often.

---

## Monitoring Consumer Groups

### kafka-consumer-groups CLI Commands

**List all consumer groups:**

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

**Describe a specific group (see partitions, offsets, lag):**

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group order-processors
```

Output shows each partition, current offset, log end offset, and lag. Lag = how many messages behind.

**Check lag for all groups:**

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 --all-groups --describe
```

**Delete a consumer group (use with caution):**

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --delete --group old-test-group
```

This removes the group and its stored offsets. Consumers in that group will start fresh (or from `auto.offset.reset`).

### Checking Lag

Lag is the difference between the latest message in the partition (log end offset) and the consumer's current offset. High lag means the consumer is falling behind.

```
TOPIC    PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
orders   0          50000          50100            100   ← 100 messages behind
orders   1          48000          48000            0     ← Caught up
```

Set up alerts: if lag > threshold for N minutes, page the team. Common thresholds: 1,000 (warning), 10,000 (critical).

### Listing Groups and Resetting Offsets

See the "Resetting Consumer Offsets" section below for how to reset. To list groups and their state, use `--describe` with `--state` (in newer Kafka versions) to see if the group is stable, rebalancing, or dead.

---

## Resetting Consumer Offsets

### When You Need To Re-Process Messages

Sometimes you need to re-read messages. A bug in your consumer corrupted data. You fixed the bug. Now you need to reprocess from the beginning. Or you want to skip ahead and ignore old messages.

### How To Reset

**Important:** Stop all consumers in the group before resetting. Otherwise they'll overwrite your reset when they commit.

**Reset to earliest (reprocess everything):**

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group order-processors \
  --reset-offsets --to-earliest \
  --topic orders \
  --execute
```

**Reset to latest (skip all current messages, start from new ones):**

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group order-processors \
  --reset-offsets --to-latest \
  --topic orders \
  --execute
```

**Reset to a specific offset:**

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group order-processors \
  --reset-offsets --to-offset 1000 \
  --topic orders:0 \
  --execute
```

(Use `topic:partition` to target a specific partition.)

**Reset to a specific timestamp:**

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group order-processors \
  --reset-offsets --to-datetime 2024-01-15T00:00:00.000 \
  --topic orders \
  --execute
```

The consumer will start from the first message at or after that timestamp.

!!! warning "Dry run first"
    Use `--dry-run` instead of `--execute` to see what would happen without making changes. Verify the offsets look correct before running with `--execute`.

---

## Common Consumer Group Patterns

### Pattern 1: One Service = One Group

Each microservice has its own consumer group. All services process all messages independently.

```
Topic: "user-events"
  → group: "recommendation-service"  (builds recommendations)
  → group: "analytics-service"       (tracks metrics)
  → group: "search-indexer"          (updates search index)
```

### Pattern 2: Competing Consumers (Load Sharing)

Multiple instances of the same service share the load within one group.

```
Topic: "email-queue" (6 partitions)
Group: "email-sender"
  → Instance 1 → P0, P1
  → Instance 2 → P2, P3
  → Instance 3 → P4, P5

Each instance sends different emails. Work is distributed.
```

### Pattern 3: Broadcast (All Consumers Get Everything)

Each consumer has a UNIQUE group ID. Everyone gets all messages.

```
Consumer A: group.id = "cache-invalidator-server-1"  → All partitions
Consumer B: group.id = "cache-invalidator-server-2"  → All partitions
Consumer C: group.id = "cache-invalidator-server-3"  → All partitions

All three servers receive every cache invalidation event.
```

---

## Real Production Story: Over-Provisioned Consumers

Here's a fictional but realistic scenario.

**Setup:** A team runs an order processing service. Topic "orders" has 3 partitions. They deploy 5 consumer instances for high availability. Each instance has the same `group.id`: "order-processors".

**What they expected:** 5 consumers sharing the load. More consumers = more throughput.

**What actually happened:** Kafka assigned 3 partitions to 3 consumers. The other 2 consumers got nothing. They sat idle. Every poll() returned empty. They were using CPU and memory but doing no work.

**The waste:** 2 idle pods. Double the cost for zero benefit. During peak traffic, they couldn't scale — they were already at the limit (3 consumers for 3 partitions). Adding more consumers would just add more idle ones.

**The fix:** They had two choices. (1) Add more partitions to the topic (e.g., 6 or 9) so more consumers could work. (2) Reduce to 3 consumers and accept that as the max parallelism. They chose to add partitions to 6, then scaled to 6 consumers. Throughput doubled. No more idle consumers.

**Lesson:** Maximum useful consumers = number of partitions. Plan your partition count when creating the topic. If you need to scale consumers later, add partitions first (you can add, never remove). Monitor: if you have more consumers than partitions, you're wasting resources.

---

## Quick Summary

| Concept | Explanation |
|---------|-------------|
| Consumer Group | Set of consumers with the same `group.id` |
| Coordinator | One broker per group; manages membership, offsets, rebalances |
| Partition Assignment | Each partition → exactly 1 consumer per group |
| Max useful consumers | = Number of partitions |
| Extra consumers | Sit idle (standby) |
| Rebalancing | Partition reassignment when consumers join/leave; typically 2-10 seconds |
| Eager vs Cooperative | Eager: all pause. Cooperative: only affected consumers pause |
| Multiple groups | Each group reads all messages independently |
| Offset tracking | Per group, per partition; stored in `__consumer_offsets` |
| Static membership | Fixed consumer ID, avoids unnecessary rebalances |
| Best assignment strategy | CooperativeStickyAssignor |
| Lag | Messages behind; monitor and alert |
| Reset offsets | Use `kafka-consumer-groups --reset-offsets` when reprocessing needed |

---

## Exercise

1. Create a topic with 4 partitions. Start 1 consumer in group "test-group". Check which partitions it's assigned.
2. Start a 2nd consumer in the same group. Watch partitions get redistributed.
3. Stop one consumer. Watch rebalancing reassign its partitions to the remaining one.
4. Start a consumer in a DIFFERENT group. Verify it reads all messages independently.
5. Start 6 consumers in the same group (more than partitions). Verify 2 are idle.

---

**Next up:** [Scaling Kafka](kafka-scaling.md) — Handle millions of messages per second.
