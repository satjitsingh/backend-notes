# Kafka Architecture

## Why Architecture Matters

Kafka's architecture is what makes it powerful. Understanding it will help you design better systems, debug problems, and answer interview questions. But it can feel overwhelming. Topics, partitions, offsets, brokers, replication, consumer groups—it's a lot.

Don't worry. We'll use one analogy throughout this entire page: **a warehouse**. By the end, you'll see how every Kafka concept maps to something physical and familiar. Let's build that mental model.

---

## The Warehouse Analogy: Your Mental Model

Imagine a **massive distribution warehouse** for an online retailer.

### The Warehouse (Kafka Cluster)

The warehouse is the entire operation. It's not one building—it's a complex of buildings, each with its own staff and storage. In Kafka, the warehouse is the **cluster**. The sum of all brokers working together.

### Shelving Units (Brokers)

Each building in the warehouse complex has rows of shelving units. These units hold the actual boxes. In Kafka, each building is a **broker**. A broker is a server. It stores data on disk. It serves read and write requests. A cluster has multiple brokers (typically 3, 5, or more) for redundancy.

### Aisles (Topics)

Inside each building, products are organized by category. The "Electronics" aisle. The "Clothing" aisle. The "Groceries" aisle. In Kafka, an aisle is a **topic**. A topic is a named stream. All order events go in the "orders" aisle. All payment events go in the "payments" aisle.

### Shelf Sections (Partitions)

Each aisle is divided into sections. The Electronics aisle might have Section A (phones), Section B (laptops), Section C (accessories). Workers can stock different sections in parallel. In Kafka, a section is a **partition**. A topic is split into partitions. Each partition is an ordered, immutable log. Partitions enable parallelism.

### Box Positions (Offsets)

Within each shelf section, boxes are placed in order. Position 0, Position 1, Position 2. You can say "I've processed everything up to Position 47." In Kafka, each message has an **offset**—a sequential ID within its partition. Offset 0, 1, 2, 3... It's like a page number. Or a bookmark.

---

## Topics: The Named Streams

A **topic** is a category or channel for messages. Producers write to topics. Consumers read from topics. It's that simple at the high level.

### Why Should You Care?

Topics let you organize your events. Without topics, everything would be one giant stream. Imagine a warehouse with no aisles—just a pile of boxes. Chaos. Topics give you structure.

### Three Different Topic Examples

**Example 1: E-commerce**

```
Topic: "orders"
  - order_placed, order_cancelled, order_shipped
  - High volume, needs many partitions

Topic: "payments"
  - payment_received, payment_failed, refund_issued
  - Critical data, needs replication

Topic: "user-activity"
  - page_view, click, search
  - Huge volume, analytics consumers
```

**Example 2: IoT Sensors**

```
Topic: "temperature-sensors"
  - Readings from factory machines
  - Time-series data, high throughput

Topic: "alerts"
  - Critical alerts (overheating, failure)
  - Low volume, must not lose any
```

**Example 3: Social Media**

```
Topic: "posts"
  - New posts, edits, deletes
  - Feed generation, search indexing

Topic: "likes"
  - Like, unlike events
  - Real-time counters, analytics
```

You create topics with a name, number of partitions, and replication factor:

```bash
kafka-topics --create --topic orders \
  --partitions 6 \
  --replication-factor 2 \
  --bootstrap-server localhost:9092
```

---

## Partitions: The Highway to Parallelism

Partitions are where beginners get confused. Let's fix that.

### Why Do Partitions Exist?

Imagine a single-lane highway. All cars must go in order. One car at a time. Slow. Now imagine a 6-lane highway. Six cars can move in parallel. Six times the throughput. Partitions are the lanes.

**Without partitions:** One topic = one stream. One consumer reads it. Bottleneck.

**With partitions:** One topic = multiple streams (partitions). Multiple consumers can read different partitions simultaneously. Parallelism.

### The Traffic Analogy: Step by Step

Let's say you have a topic "orders" with 3 partitions. Messages arrive. How do they get distributed?

**Scenario: 10 messages, no keys (round-robin)**

```
Message 1 → Partition 0
Message 2 → Partition 1
Message 3 → Partition 2
Message 4 → Partition 0
Message 5 → Partition 1
Message 6 → Partition 2
Message 7 → Partition 0
Message 8 → Partition 1
Message 9 → Partition 2
Message 10 → Partition 0
```

Kafka distributes evenly. Load balancing. Simple.

**Scenario: 10 messages, WITH keys (same key = same partition)**

Say we have keys: order-101, order-102, order-103, order-101, order-102...

```
hash("order-101") % 3 = Partition 2
hash("order-102") % 3 = Partition 0
hash("order-103") % 3 = Partition 1
hash("order-101") % 3 = Partition 2  (same as first order-101!)
hash("order-102") % 3 = Partition 0  (same as first order-102!)
```

All messages for order-101 go to Partition 2. In order. That's how we guarantee ordering for a specific order. Critical for "order created → order paid → order shipped."

!!! tip "Key Rule"
    Same key always goes to the same partition. Use keys when you need ordering for a logical entity (user, order, session).

### What Problem Do Partitions Solve?

1. **Parallelism:** Multiple consumers can read different partitions at once.
2. **Scalability:** More partitions = more throughput (up to a point).
3. **Ordering:** Messages within ONE partition are strictly ordered. Across partitions, no guarantee.

---

## Offsets: Your Bookmark in the Stream

An **offset** is the position of a message within a partition. It's a sequential number. 0, 1, 2, 3, 4...

### The Book Analogy

You're reading a 500-page book. You put a bookmark at page 247. That's your offset. "I've read up to page 247." Tomorrow, you start from page 248. You don't re-read from page 1.

In Kafka, the consumer commits its offset. "I've processed up to offset 247 in Partition 0." If the consumer crashes and restarts, it asks Kafka: "What was my last offset?" Kafka says "247." The consumer resumes from 248. No re-processing of old messages (unless you want to).

### Walk-Through: Consumer Crashes and Restarts

Let's trace what happens:

**Initial state:** Partition 0 has messages at offsets 0-9. Consumer starts. No previous offset.

**Consumer reads:** Messages 0, 1, 2, 3, 4. Processes them. Commits offset 5. (Meaning: "I've processed up through offset 4, next is 5.")

**Consumer crashes.** Before it could process 5, 6, 7, 8, 9.

**Consumer restarts.** It asks Kafka: "What's my committed offset for Partition 0?" Kafka says "5."

**Consumer resumes.** It reads from offset 5. Gets messages 5, 6, 7, 8, 9. Processes them. No data lost. No duplicate processing of 0-4 (assuming commit worked).

!!! warning "Commit Timing"
    If the consumer processes messages 0-4 but crashes BEFORE committing, it never told Kafka "I'm at 5." On restart, it might get offset 0 again (or whatever auto.offset.reset says). It could process 0-4 again. That's at-least-once. Duplicates possible.

---

## Brokers: The Server Rooms

A **broker** is a single Kafka server. One process. One machine (or VM, or container). It runs Kafka.

### What Does Each Broker Actually Do?

Think of a broker as a server room in our warehouse. It has three main jobs:

**1. Store messages on disk**

Kafka writes messages to log files. Not in memory (well, it uses memory for caching, but durability is on disk). Each partition is a directory. Messages are appended to segment files. When the segment gets big enough, a new one starts. Old segments can be deleted based on retention.

**2. Serve producer requests**

When a producer sends a message, it goes to the broker that leads the target partition. The broker appends the message. Returns an acknowledgment. The broker handles batching, compression, and replication coordination.

**3. Serve consumer requests**

When a consumer asks for messages from offset X, the broker reads from disk (or cache) and returns them. The broker doesn't push. The consumer pulls. The broker just responds to fetch requests.

### The Controller Broker

One broker in the cluster is elected the **controller**. It's like the warehouse manager. It doesn't do the heavy lifting of storing data. It manages metadata:

- Which broker is the leader for each partition?
- Which brokers are alive?
- When a broker dies, the controller triggers leader election.
- When a new topic is created, the controller assigns partitions to brokers.

If the controller dies, another broker is elected. The cluster keeps running.

#### What Does the Controller Actually Do?

The controller is the "brain" of the cluster. It holds the authoritative view of cluster state. Every broker knows who the controller is. When something changes, they ask the controller.

**Key responsibilities:**

1. **Partition assignment:** When you create a topic, the controller decides which broker gets which partition. It balances load. Partition 0 leader on Broker 1, Partition 1 leader on Broker 2, and so on.

2. **Leader election:** When a broker dies, the controller detects it (via ZooKeeper/KRaft heartbeats). For each partition that had a leader on the dead broker, the controller promotes an in-sync replica to leader. It updates metadata. Producers and consumers get the new leader info.

3. **Replica assignment:** When you add a broker, the controller can reassign replicas. It manages the reassignment process.

4. **Preferred leader election:** Kafka can rebalance leaders to put them on "preferred" brokers (the original assignment). The controller orchestrates this.

#### How Is the Controller Elected?

**With ZooKeeper:** The first broker to start creates an ephemeral node in ZooKeeper called `/controller`. That broker becomes the controller. If it dies, the node disappears. Other brokers watch that node. When it's gone, they race to create it. The winner becomes the new controller.

**With KRaft:** Kafka uses an internal Raft consensus. Brokers vote. One becomes the "active" controller. The election is built into KRaft—no external ZooKeeper.

#### What Happens When the Controller Fails?

1. **Detection:** Other brokers (or KRaft) detect the controller is gone. Heartbeats stop. Timeout expires.

2. **Election:** A new controller is elected. Usually within a few seconds. With ZooKeeper, brokers race to create the ephemeral node. With KRaft, Raft elects a new leader.

3. **State recovery:** The new controller loads cluster state from ZooKeeper (or KRaft log). It knows all topics, partitions, leaders, ISR.

4. **Resume:** The cluster continues. In-flight requests might fail. Clients retry. No data loss. Brief unavailability (seconds) for metadata-dependent operations.

!!! info "Controller and Data"
    The controller does NOT store message data. It only manages metadata. Even if the controller is down for a few seconds, producers and consumers can still read/write to partition leaders. They might get stale metadata (wrong leader) and need to refresh.

---

## Kafka Networking Model: How Clients Discover Brokers

When you configure a Kafka client with `bootstrap.servers=broker1:9092,broker2:9092`, you're not giving it the full list of brokers. You're giving it a starting point. Here's how discovery works.

### Bootstrap Servers Explained

**Bootstrap servers** are the initial contact points. The client connects to one of them. It doesn't need to know every broker. It just needs to reach one.

**Step 1: Connect.** The producer or consumer connects to broker1 (or broker2 if broker1 is down).

**Step 2: Metadata request.** The client asks: "What topics exist? What are the partition leaders? Which broker leads partition 0 of topic 'orders'?"

**Step 3: Response.** The broker returns metadata. Topic "orders" has 6 partitions. Partition 0 leader: broker2. Partition 1 leader: broker3. And so on.

**Step 4: Direct connection.** The client now connects directly to the partition leaders. To produce to partition 0, it talks to broker2. To consume from partition 1, it talks to broker3. No need to go through the bootstrap broker for data.

### Why This Matters

- **Broker failure:** If broker1 (bootstrap) dies, the client tries broker2. It gets metadata. It discovers the new leaders. It continues.
- **Scaling:** Add broker4. The controller updates metadata. Next metadata refresh, clients learn about broker4. No client config change.
- **Partition movement:** Reassign partitions. Leaders change. Clients refresh metadata (on error or periodically). They get new leaders. Transparent.

### Metadata Refresh

Clients cache metadata. They don't ask every time. When do they refresh? (1) On connection. (2) When they get a "NotLeaderForPartition" error. (3) Periodically (e.g., every 5 minutes via `metadata.max.age.ms`). Stale metadata can cause temporary routing errors. Clients retry. They get fresh metadata. They succeed.

!!! tip "Production Tip"
    Always give at least 2–3 bootstrap servers. If one is down, the client can still connect. Use different hosts, not just different ports on the same host.

---

## Replication: Backup Copies of Important Documents

What if a broker dies? The data on that broker would be lost. Unless we have copies.

### The Analogy

Imagine you have an important contract. You keep the original in your office. You make two copies. One in a safe. One in a bank vault. If your office burns down, you still have the contract. Replication is the same idea.

### How Replication Works

When you create a topic with `replication-factor=3`, each partition has 3 copies. They live on 3 different brokers.

```
Topic: "orders", Partition 0, Replication Factor = 3

Broker 1: Partition 0 (LEADER)    ← Producers write here. Consumers read from here.
Broker 2: Partition 0 (FOLLOWER)  ← Copy. Syncs from leader.
Broker 3: Partition 0 (FOLLOWER)  ← Copy. Syncs from leader.
```

**Leader:** Handles all reads and writes. The source of truth.

**Followers:** Replicate from the leader. They don't serve reads (in normal Kafka). They're backups. If the leader dies, a follower is promoted.

### Broker Failure Scenario: Step by Step

**Initial state:** Broker 1 is leader for Partition 0. Broker 2 and 3 are followers. All in sync.

**Broker 1 dies.** (Power failure, disk crash, whatever.)

**What happens:**

1. ZooKeeper (or KRaft) detects Broker 1 is gone. Heartbeats stopped.
2. The controller initiates leader election.
3. Broker 2 or Broker 3 is chosen as the new leader. (Both have the data. Either can take over.)
4. Producers and consumers are notified. They now talk to the new leader.
5. The cluster continues. No data loss. Brief pause during failover (usually seconds).

**Broker 1 comes back.** It rejoins the cluster. It might become a follower again for Partition 0. It syncs from the new leader. Eventually it's back in sync.

---

## ISR: In-Sync Replicas

Not all replicas are equal. Some might be lagging. Maybe a follower had a network hiccup. Maybe it's on a slow disk.

### The Classroom Note-Taking Analogy

Imagine a teacher writes on the board. Students take notes. Some students are caught up. Some are three pages behind. The "in-sync" students have the same content as the teacher. The lagging students don't.

**ISR (In-Sync Replicas)** = the set of replicas that are caught up with the leader. They have all the same messages. When the leader dies, we can only promote someone from the ISR. We can't promote a lagging replica—it might be missing messages.

```
ISR = [Broker 1 (leader), Broker 2, Broker 3]

Broker 2 has network issues, falls behind:
ISR = [Broker 1 (leader), Broker 3]   // Broker 2 removed from ISR

Broker 1 dies:
New leader = Broker 3 (only in-sync option)
```

!!! info "Why It Matters"
    With `acks=all`, the producer waits for all ISR replicas to acknowledge. If one replica falls behind and is removed from ISR, you only need the remaining in-sync replicas. You're still safe as long as you have at least one replica (plus leader).

---

## Consumer Groups: The Pizza Delivery Team

Consumer groups confuse beginners the most. Let's fix that with a clear analogy.

### The Pizza Delivery Team Analogy

Imagine a pizza shop. Orders come in. You have a team of delivery drivers. Each order must be delivered by exactly one driver. You don't want two drivers delivering the same pizza. You also don't want orders piling up with no one to deliver.

**Orders = Partitions.** Each order is assigned to one driver.

**Drivers = Consumers.** Each driver handles their assigned orders.

**The team = Consumer Group.** All drivers share the work. No overlap. No gaps.

### Rule: One Partition, One Consumer (per group)

Within a consumer group, each partition is assigned to exactly one consumer. A consumer can have multiple partitions. But a partition has only one consumer in that group.

### Scenario 1: 2 Consumers, 3 Partitions

```
Topic "orders" has 3 partitions: P0, P1, P2
Consumer Group: "order-processors"

Consumer 1 → P0, P1   (one consumer gets 2 partitions)
Consumer 2 → P2
```

Consumer 1 does more work. That's okay. Kafka tries to balance, but with 3 partitions and 2 consumers, someone gets 2.

### Scenario 2: 3 Consumers, 3 Partitions (Perfect Balance)

```
Consumer 1 → P0
Consumer 2 → P1
Consumer 3 → P2
```

Each consumer has one partition. Ideal. Maximum parallelism.

### Scenario 3: 5 Consumers, 3 Partitions (Extra Consumers Idle)

```
Consumer 1 → P0
Consumer 2 → P1
Consumer 3 → P2
Consumer 4 → IDLE (no partition)
Consumer 5 → IDLE (no partition)
```

You have more consumers than partitions. Two consumers have nothing to do. They just sit there. **Maximum useful consumers = number of partitions.** Remember that.

### Scenario 4: Consumer Crashes

**Before:** C1→P0, C2→P1, C3→P2. All happy.

**C3 crashes.** Stops sending heartbeats. Kafka detects it (after session.timeout).

**Rebalance:** Partitions must be reassigned. C1 and C2 are still alive.

**After:** C1→P0, P2. C2→P1.

C1 takes over P2. It reads from the last committed offset for P2. Work continues. Brief pause during rebalance.

### Scenario 5: New Consumer Joins

**Before:** C1→P0, P1. C2→P2.

**C3 joins the group.** "Hey, I'm here to help!"

**Rebalance:** Kafka redistributes. Now we have 3 consumers for 3 partitions.

**After:** C1→P0. C2→P1. C3→P2.

C2 gave up P2 to C3. C1 gave up P1 to C2. Everyone has one partition. Better balance.

!!! warning "Key Rule"
    Maximum useful consumers in a group = Number of partitions. Want to scale consumers? Add more partitions first.

---

## ZooKeeper and KRaft: Who's the Boss?

A Kafka cluster needs to know: Who's the controller? Which brokers are alive? Where are the partition leaders? This is **metadata**. It needs to be consistent. All brokers must agree.

### ZooKeeper (Legacy)

ZooKeeper is a separate service. It's a distributed coordination system. Kafka used it to store cluster metadata. "Broker 1 is the controller." "Partition 0 leader is Broker 2." ZooKeeper is the "boss" that everyone consults.

**Problem:** You need to run ZooKeeper separately. Two systems to operate. ZooKeeper has scalability limits. More components to monitor and maintain.

### KRaft Mode (New, Kafka 3.3+)

Kafka now has built-in consensus. No ZooKeeper. Kafka brokers themselves elect a controller and replicate metadata using the Raft protocol. It's simpler. One less thing to run. Fewer moving parts.

| Feature | ZooKeeper | KRaft |
|---------|-----------|-------|
| External dependency | Yes (ZooKeeper) | No |
| Setup complexity | More complex | Simpler |
| Scalability | Limited | Better |
| Metadata storage | ZooKeeper znodes | Kafka's internal __cluster_metadata topic |
| Future | Being phased out | The future of Kafka |

### Migration from ZooKeeper to KRaft

Kafka supports migrating from ZooKeeper to KRaft without downtime. The process is called **ZooKeeper to KRaft migration (ZK to KRaft)**.

**Overview:**

1. **Dual-write phase:** Run a KRaft controller quorum alongside your existing ZooKeeper cluster. The KRaft controllers replicate metadata from ZooKeeper. Both systems are in sync.

2. **Switch:** Once KRaft has caught up, you switch brokers to use KRaft for metadata. Brokers are reconfigured. They stop talking to ZooKeeper. They talk to KRaft.

3. **Decommission ZooKeeper:** After all brokers use KRaft, you shut down ZooKeeper. The cluster runs on KRaft only.

**Key points:**

- **Kafka 3.6+** has production-ready migration tooling.
- **Plan carefully.** Test in staging first. Migration involves config changes and rolling restarts.
- **Confluent and other vendors** offer guided migration. Check their docs for step-by-step.
- **New deployments:** Use KRaft from day one. No need for ZooKeeper.

!!! info "KRaft Is the Future"
    Apache Kafka is moving to KRaft-only. ZooKeeper support will be deprecated and eventually removed. New projects should use KRaft. Existing ZooKeeper clusters should plan migration.

---

## End-to-End Message Journey: One Message, Every Step

Let's trace a single message from producer to consumer. Every detail.

**Message:** `{ "orderId": "12345", "amount": 99.99 }`  
**Topic:** orders  
**Key:** order-12345

### Step 1: Producer Creates the Message

The Order Service creates a ProducerRecord. Topic: "orders". Key: "order-12345". Value: the JSON.

### Step 2: Serialization

The producer serializes key and value to bytes. Kafka doesn't care about JSON. It stores bytes. StringSerializer converts "order-12345" to bytes. Same for the value.

### Step 3: Partition Selection

The producer (or the broker) determines the partition. `hash("order-12345") % 6` (assuming 6 partitions). Say the result is 3. Partition 3.

### Step 4: Batching (Optional)

The producer might batch this message with others. Wait up to `linger.ms` (e.g., 5ms). Send a batch to reduce network round-trips.

### Step 5: Send to Broker

The producer sends the batch to the broker that leads Partition 3. Say that's Broker 2. The request goes to Broker 2.

### Step 6: Broker Appends

Broker 2 appends the message to Partition 3's log. The message gets the next offset. Say it's offset 8472.

### Step 7: Replication

Broker 2 (leader) replicates to followers. Broker 1 and Broker 3 have Partition 3 as followers. They copy the message. With `acks=all`, the producer waits for all in-sync replicas to acknowledge.

### Step 8: Acknowledgment

Broker 2 sends an acknowledgment to the producer. "Message received. Partition 3, Offset 8472." Producer considers it done.

### Step 9: Consumer Polls

A consumer in group "order-processors" is assigned Partition 3. It polls. "Give me messages from offset 8470." Broker 2 returns messages 8470, 8471, 8472.

### Step 10: Consumer Processes

The consumer deserializes the bytes. Gets the JSON. Processes it (e.g., send confirmation email). Commits offset 8473. "I've processed through 8472. Next is 8473."

Done. The message was produced, stored, replicated, and consumed.

---

## What Happens When Things Go Wrong?

### Broker Dies

We covered this. Leader election. Follower promoted. Brief unavailability. No data loss (with replication factor >= 2).

### Consumer Crashes

The consumer stops sending heartbeats. After `session.timeout.ms`, it's considered dead. Rebalance. Its partitions are reassigned to other consumers. They resume from last committed offset. Messages between last commit and crash might be processed again (at-least-once) or lost (at-most-once if you committed before processing). Design for idempotency.

### Network Partition

A broker gets disconnected. Can't reach the rest of the cluster. The controller might think it's dead. Leader election for its partitions. When the network heals, the broker might rejoin as a follower. It syncs. Could have missed some messages while disconnected. Replication handles this—the in-sync replicas have the data.

### Disk Full

If a broker's disk fills up, it can't append new messages. Producers will fail. Alert on disk space. Add more disk or replace the broker.

---

## Message Storage: How Kafka Keeps Data

Kafka stores messages **on disk**. Each partition is a directory. Inside are segment files. Messages are appended. When a segment reaches its size limit (e.g., 1GB), a new segment starts. Old segments are deleted based on retention (e.g., 7 days) or compacted (for log compaction topics).

### Retention Policy

| Setting | Default | Meaning |
|---------|---------|---------|
| `retention.ms` | 7 days | How long to keep messages |
| `retention.bytes` | -1 (unlimited) | Max size of partition data |
| `cleanup.policy` | `delete` | Delete old messages, or `compact` (keep latest per key) |

---

## Quick Summary

| Component | Analogy | What It Does |
|-----------|---------|--------------|
| Cluster | Warehouse | The entire Kafka deployment |
| Broker | Shelving unit / Server room | Stores data, serves requests |
| Topic | Aisle | Named category for messages |
| Partition | Shelf section | Sub-division for parallelism |
| Offset | Box position / Bookmark | Message position in partition |
| Leader | Primary copy | Handles reads/writes |
| Follower | Backup copy | Replicates from leader |
| ISR | In-sync replicas | Replicas caught up with leader |
| Consumer Group | Delivery team | Consumers sharing partitions |
| ZooKeeper/KRaft | Boss / Coordinator | Cluster metadata management |

---

## Exercise

1. Create a topic with 3 partitions and replication factor 2.
2. Send messages with keys ("A", "B", "C") and observe which partition they go to.
3. Start 2 consumers in the same group. See how partitions are distributed.
4. Kill one consumer. Watch the other take over its partitions (rebalance).

---

**Next up:** [Producers & Consumers](producers-consumers.md) - Deep dive into sending and receiving messages.
