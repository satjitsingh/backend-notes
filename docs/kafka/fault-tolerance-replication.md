# Fault Tolerance & Replication

## Why Fault Tolerance Matters

Servers fail. It's not a question of "if" but "when." Hard drives die. Networks glitch. Data centers lose power. In a distributed system running on dozens or hundreds of machines, something is always breaking.

Kafka is designed to keep working even when things go wrong. It does this through **replication** — keeping multiple copies of your data on different servers. If one server dies, another has the data. No loss. No downtime.

---

## Replication: The Core Concept

### The Analogy: Important Documents

Imagine you have a critical contract. You don't keep just one copy. You make 3 copies. One in your desk drawer. One in your safe at home. One in a bank locker. If your house burns down, the bank still has a copy.

Kafka does the same thing with your messages. Each partition can have multiple **replicas** — copies on different brokers.

```
Topic: "orders", Partition 0, Replication Factor = 3

Broker 1: Partition 0 (copy)
Broker 2: Partition 0 (copy)
Broker 3: Partition 0 (copy)

If Broker 1's hard drive dies → Broker 2 and 3 still have the data ✅
```

### Replication Factor

The **replication factor** is the number of copies of each partition.

| Replication Factor | What It Means | Tolerance |
|-------------------|--------------|-----------|
| 1 | Only 1 copy. No backup. | 0 failures. Data lost if broker dies. |
| 2 | 2 copies on different brokers. | 1 broker can die. |
| 3 | 3 copies on different brokers. | 2 brokers can die. |

!!! warning "Replication Factor 1"
    Never use replication factor 1 in production. It means zero fault tolerance. One broker failure = permanent data loss for those partitions.

**Industry standard:** Replication factor of **3**. This tolerates 2 simultaneous broker failures while keeping your data safe.

```bash
kafka-topics --create --topic orders \
  --partitions 6 \
  --replication-factor 3 \
  --bootstrap-server localhost:9092
```

---

## Leader and Followers

Not all replicas are equal. For each partition, one replica is the **leader** and the rest are **followers**.

### What the Leader Does

- **Handles ALL reads and writes** for that partition
- Producers send messages to the leader
- Consumers read from the leader
- The leader is the "source of truth"

### What Followers Do

- **Continuously copy data** from the leader
- They don't serve any client requests (by default)
- They exist purely as backups
- If the leader dies, one of them becomes the new leader

```
Topic: "orders", Partition 0

┌────────────────────────────────────────────────┐
│                                                │
│  Broker 1: Partition 0 ★ LEADER                │
│    ↑ Producers write here                      │
│    ↓ Consumers read here                       │
│                                                │
│  Broker 2: Partition 0   FOLLOWER              │
│    (copies from leader, ready to take over)     │
│                                                │
│  Broker 3: Partition 0   FOLLOWER              │
│    (copies from leader, ready to take over)     │
│                                                │
└────────────────────────────────────────────────┘
```

### Leader Distribution

Kafka distributes partition leaders across brokers to spread the load. Not all leaders sit on one broker.

```
Topic: "orders" (3 partitions, replication factor 3)

Partition 0: Leader=Broker 1, Followers=Broker 2, 3
Partition 1: Leader=Broker 2, Followers=Broker 3, 1
Partition 2: Leader=Broker 3, Followers=Broker 1, 2

Each broker is the leader for some partitions and a follower for others.
Load is balanced across the cluster.
```

---

## ISR: In-Sync Replicas

### What Is ISR?

The **ISR (In-Sync Replicas)** is the set of replicas that are fully caught up with the leader. They have copied all the leader's messages.

```
Leader has messages: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]

Follower A has:       [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]  ← In sync ✅
Follower B has:       [0, 1, 2, 3, 4, 5, 6, 7]          ← Behind (not in ISR) ❌
```

### Why ISR Matters

When the leader dies, Kafka must choose a new leader. It can ONLY choose from the ISR — replicas that have all the data. If it chose a replica that's behind, you'd lose messages.

```
ISR = [Broker 1 (leader), Broker 2]
Not in ISR = [Broker 3]   ← Broker 3 is behind, it's missing some messages

Broker 1 dies:
  New leader = Broker 2 (from ISR) ✅
  NOT Broker 3 (it would lose the messages it hasn't copied yet)
```

### min.insync.replicas

This setting defines the minimum number of replicas that must be in-sync before a producer can write.

```
min.insync.replicas=2
```

With `acks=all` and `min.insync.replicas=2`:

- A write succeeds only if at least 2 replicas (leader + 1 follower) acknowledge it.
- If only 1 replica is alive, writes are rejected. This prevents data loss.

```
Scenario: Replication Factor=3, min.insync.replicas=2

3 replicas alive → Write succeeds (3 >= 2) ✅
2 replicas alive → Write succeeds (2 >= 2) ✅
1 replica alive  → Write FAILS (1 < 2) ❌
                   "NotEnoughReplicasException"
```

!!! tip "Interview Insight"
    "For critical data, I configure `acks=all` with `min.insync.replicas=2` and `replication-factor=3`. This ensures writes are acknowledged by at least 2 replicas. Even if one broker fails, data is safe on the remaining two, and writes continue. If two brokers fail, writes stop to prevent data loss — a trade-off of availability for consistency."

---

## What Happens When a Broker Dies

Let's walk through a complete failure scenario.

### Before the Failure

```
Cluster: 3 Brokers
Topic: "orders" (3 partitions, replication factor 3)

Partition 0: Leader=Broker 1, Followers=Broker 2, 3    ISR=[1,2,3]
Partition 1: Leader=Broker 2, Followers=Broker 3, 1    ISR=[2,3,1]
Partition 2: Leader=Broker 3, Followers=Broker 1, 2    ISR=[3,1,2]
```

### Broker 2 Dies

```
Step 1: Kafka's controller detects Broker 2 is unresponsive (no heartbeat)

Step 2: For Partition 0: Broker 2 was a follower. ISR shrinks to [1, 3]. No leadership change needed.

Step 3: For Partition 1: Broker 2 was the LEADER. Election needed!
        New leader elected from ISR: Broker 3 becomes leader.
        ISR for Partition 1 = [3, 1]

Step 4: For Partition 2: Broker 2 was a follower. ISR shrinks to [3, 1].

Step 5: Producers and consumers update their metadata.
        Writes to Partition 1 now go to Broker 3 instead of Broker 2.

Result:
  Partition 0: Leader=Broker 1, ISR=[1, 3]    ← Still working ✅
  Partition 1: Leader=Broker 3, ISR=[3, 1]    ← New leader ✅
  Partition 2: Leader=Broker 3, ISR=[3, 1]    ← Still working ✅

  Zero data loss. Zero downtime (brief pause during election).
```

### Broker 2 Comes Back

```
Step 1: Broker 2 starts up and joins the cluster
Step 2: For each partition, Broker 2 was a replica, it starts catching up
        (copying messages it missed while it was down)
Step 3: Once fully caught up, Broker 2 is added back to the ISR
Step 4: Kafka may trigger a "preferred replica election" to rebalance
        leadership back to the original assignment

Result: Everything is back to normal.
```

### How Catch-Up Works in Detail

When a broker comes back, it must copy all messages it missed. How long does that take?

**Factors that affect catch-up time:**

- **How long was the broker down?** More downtime = more messages to copy.
- **Throughput during downtime:** High-volume topics generate more backlog.
- **Network bandwidth:** Replication is network-bound. A 1 Gbps link copies ~100 MB/s.
- **Disk I/O:** Both leader and follower read/write to disk. Slow disks slow replication.

**Example:** Broker was down for 10 minutes. Topic produces 10,000 msg/sec, 1 KB each. That's 10 min × 60 sec × 10,000 × 1 KB = ~6 GB to replicate. At 100 MB/s, catch-up takes about 1 minute. In practice, replication is often faster because Kafka batches and compresses.

**What happens during catch-up:** The follower sends fetch requests to the leader. The leader streams log segments. The follower writes to disk. Once the follower's log matches the leader's high-water mark, it rejoins the ISR. Until then, it's an out-of-sync replica — it cannot become leader.

!!! info "Replication throttle"
    In older Kafka versions, you could throttle replication to avoid saturating the network. Modern Kafka handles this more gracefully. Monitor `under-replicated-partitions` — if it stays high for a long time, check network or disk.

---

## Rack Awareness

### What Is Rack Awareness?

**Rack awareness** means Kafka knows which brokers are in which physical location (rack, availability zone, data center). It uses this to spread replicas across different racks. If one rack loses power or has a network outage, your data is still safe on other racks.

### Why Putting All Replicas on the Same Rack Is Dangerous

Without rack awareness, Kafka might place all 3 replicas of a partition on brokers in the same rack. That rack has a single point of failure:

```
Same rack (BAD):
  Rack A: Broker 1, Broker 2, Broker 3  ← All replicas here
  Rack B: (empty)
  
  Rack A loses power → All 3 replicas gone → Data lost!
```

### How To Enable Rack Awareness

Set `broker.rack` on each broker to identify its location:

```properties
# broker-1 in us-east-1a
broker.rack=us-east-1a

# broker-2 in us-east-1b
broker.rack=us-east-1b

# broker-3 in us-east-1c
broker.rack=us-east-1c
```

Kafka's partition assignment will spread replicas across racks when possible:

```
With rack awareness (GOOD):
  Partition 0: Leader in us-east-1a, Followers in us-east-1b, us-east-1c
  Partition 1: Leader in us-east-1b, Followers in us-east-1a, us-east-1c
  Partition 2: Leader in us-east-1c, Followers in us-east-1a, us-east-1b

  One rack goes down → 2 replicas still alive → No data loss ✅
```

!!! tip "Cloud deployments"
    In AWS, use availability zone as rack (e.g., `us-east-1a`). In GCP, use zone. In on-prem, use physical rack or data center. The goal is to survive a single location failure.

---

## Network Partitions (Split Brain)

### What Happens When Brokers Can't Talk to Each Other

A **network partition** (or "split brain") occurs when the network splits. Some brokers can talk to each other, but not to others. For example, Broker 1 and 2 can communicate, but neither can reach Broker 3.

### How Kafka Handles It

Kafka uses **ZooKeeper** (or KRaft in newer versions) to elect a **controller** — one broker that manages partition assignments and leader elections. The controller must have a quorum to make decisions.

**With ZooKeeper:** ZooKeeper itself uses a quorum. If the network splits, only one side has a ZooKeeper quorum. That side continues. The other side cannot elect a controller. Brokers on the "wrong" side stop accepting new writes for partitions they can't reach.

**Key point:** Kafka does NOT allow two leaders for the same partition. The controller ensures only one leader exists. During a partition, one side may be unable to elect a leader for some partitions. Those partitions become unavailable until the network heals. This is a deliberate trade-off: **consistency over availability** during a split.

```
Network partition: Brokers 1+2 can talk, Broker 3 isolated

Broker 3 thinks it's alone. It cannot form a quorum.
Broker 3's partitions go offline (no leader election without controller).

Brokers 1+2 continue. Their partitions stay available.
When network heals, Broker 3 rejoins, catches up, ISR restored.
```

!!! warning "Avoid split brain in application logic"
    Your application should not assume it can always write to Kafka. During a network partition, some partitions may be unavailable. Implement retries, circuit breakers, and fallback logic (e.g., queue to a local store and replay when Kafka is back).

---

## Data Recovery: What To Do When Things Go Really Wrong

### Full Outage Recovery Steps

If multiple brokers fail or you lose data, here's a structured approach:

1. **Assess the damage:** Which partitions are under-replicated? Which have no in-sync replicas? Use `kafka-topics --describe --under-replicated-partitions`.

2. **Restore brokers:** Bring failed brokers back online. Let them rejoin the cluster and catch up. This often fixes everything.

3. **If brokers are gone forever:** If you've lost 2 out of 3 replicas, you have one replica left. That replica becomes the leader. No data loss if that replica was in the ISR. If you've lost ALL replicas (e.g., replication factor 1, or all 3 brokers died), data is gone. Restore from backup.

4. **Unclean leader election (last resort):** If the only surviving replica is out-of-sync, you have two choices: (a) Accept unavailability until you restore a replica from backup, or (b) Enable `unclean.leader.election.enable=true` temporarily — you will lose data, but the partition comes back. Only for non-critical data.

### Topic Backup Strategies

**MirrorMaker 2:** Replicate topics to another cluster in a different region. If the primary cluster is destroyed, fail over to the secondary.

**Export to object storage:** Use Kafka Connect or a custom consumer to export topics to S3/GCS. Restore by replaying into a new cluster.

**Snapshot + offset tracking:** Periodically snapshot your consumer's state (database, etc.) and record the offset. To recover, restore the snapshot and reset the consumer to that offset.

!!! info "Disaster recovery plan"
    Document your RTO (recovery time objective) and RPO (recovery point objective). How much data loss is acceptable? How long can you be down? Your backup strategy should match these numbers.

---

## Monitoring Replication Health

### Key Metrics to Watch

| Metric | What It Means | Alert When |
|--------|---------------|------------|
| `UnderReplicatedPartitions` | Partitions with fewer in-sync replicas than expected | > 0 for more than 5 minutes |
| `OfflinePartitionsCount` | Partitions with no leader | > 0 |
| `ISR shrink rate` | Replicas falling out of ISR | Sudden spike |
| `ISR expand rate` | Replicas rejoining ISR | Monitor after broker recovery |

### How To Set Up Alerts

**Using Kafka's built-in metrics:** Expose JMX metrics to Prometheus (or your monitoring system). Key metrics:

```
kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions
kafka.controller:type=KafkaController,name=OfflinePartitionsCount
```

**CLI check:** Run periodically:

```bash
kafka-topics --bootstrap-server localhost:9092 --describe --under-replicated-partitions
```

If this returns any partitions, investigate. A broker might be down, or replication might be slow.

**ISR shrink/expand events:** These appear in broker logs. In production, aggregate logs and alert on "Shrunk ISR" or "Expanded ISR" to track replication health over time.

!!! tip "Under-replicated is normal briefly"
    When a broker dies, partitions it led will show as under-replicated until a new leader is elected. That's expected. Alert when under-replicated persists for more than a few minutes — it might indicate a stuck broker or network issue.

---

## Real Production Story: Broker Death During Peak Traffic

Here's a fictional but realistic scenario.

**Setup:** 3-broker cluster. Topic "orders" with 6 partitions, replication factor 3. Peak traffic: 5,000 messages/second. Three consumer groups processing orders, payments, and notifications.

**2:00 PM:** Broker 2's disk controller fails. The broker becomes unresponsive. Heartbeats to the controller stop.

**2:00:05 PM:** Kafka's controller detects Broker 2 is dead. Leader election for Partition 1 and 4 (Broker 2 was leader). Broker 1 takes Partition 1, Broker 3 takes Partition 4. ISR for affected partitions shrinks to [1, 3].

**2:00:06 PM:** Producers get brief "NotLeaderForPartition" errors. They retry. Metadata refresh. New leaders are found. Writes resume. A few hundred messages see 1-2 retries. No data loss.

**2:00:07 PM:** Consumers in all three groups get "NotLeaderForPartition" on their poll. They refresh metadata, reconnect to the new leaders, and resume. Consumer lag spikes by about 5,000 messages (one second of traffic). Within 30 seconds, lag is back to zero.

**2:05 PM:** Ops team gets an alert: "UnderReplicatedPartitions = 4" (partitions that had replicas on Broker 2). They check. Broker 2 is down. They start the broker recovery process.

**2:30 PM:** Broker 2 is back. It begins catching up. Replication fetches from Broker 1 and 3. With 5,000 msg/sec and 10 minutes of downtime, that's ~3 million messages. At ~50 MB/s replication, catch-up takes about 6 minutes.

**2:36 PM:** Broker 2 is fully caught up. All partitions rejoin ISR. UnderReplicatedPartitions drops to 0. Preferred replica election runs (optional). Leadership rebalances. Cluster is fully healthy.

**Lesson:** With replication factor 3 and `acks=all`, a single broker failure caused ~5 seconds of retries and a brief lag spike. No data loss. No manual intervention for the pipeline to keep running. The key was proper configuration and monitoring.

---

## Unclean Leader Election

What happens when the leader dies and NO replica in the ISR is available?

### The dilemma:

- Leader is dead.
- All ISR replicas are also dead.
- There's a follower alive, but it's NOT in the ISR (it's behind).

### Two options:

| Setting | Behavior | Risk |
|---------|----------|------|
| `unclean.leader.election.enable=false` (default) | Partition goes offline. No reads or writes until an ISR replica comes back. | No data loss, but partition is unavailable. |
| `unclean.leader.election.enable=true` | The out-of-sync follower becomes leader. Partition stays online. | **Data loss** — messages the follower hadn't copied are gone forever. |

!!! warning "Production recommendation"
    Keep `unclean.leader.election.enable=false` for critical data. It's better to have a partition temporarily unavailable than to lose data silently.

---

## Kafka's Durability Guarantees

| Configuration | Guarantee | Trade-off |
|--------------|-----------|-----------|
| `acks=0` | No guarantee. Message might be lost. | Fastest writes |
| `acks=1` | Leader wrote it. If leader dies before replicating, message lost. | Fast writes |
| `acks=all` + `min.insync.replicas=1` | Leader confirms + at least leader is in ISR. | Safe, but single point of failure |
| `acks=all` + `min.insync.replicas=2` | Leader + at least 1 follower confirmed. **Most durable.** | Slightly slower writes |

**The gold standard for production:**

```properties
acks=all
min.insync.replicas=2
replication.factor=3
```

This means: every write is confirmed by at least 2 out of 3 replicas. You can lose 1 broker and still have zero data loss.

---

## Quick Summary

| Concept | Explanation |
|---------|-------------|
| Replication | Multiple copies of each partition on different brokers |
| Replication Factor | Number of copies (3 is standard) |
| Leader | The replica that handles all reads and writes |
| Follower | Backup replicas that copy from the leader |
| ISR | Set of replicas that are fully caught up |
| Leader Election | When leader dies, new leader chosen from ISR |
| min.insync.replicas | Minimum ISR size to accept writes |
| Unclean Election | Allowing out-of-sync replica to become leader (risky) |
| Gold standard | `acks=all` + `min.insync.replicas=2` + `replication-factor=3` |

---

## Exercise

1. Create a topic with replication factor 3 on a 3-broker cluster. Use `--describe` to see leader and ISR.
2. Stop one broker. Re-describe the topic. Watch ISR shrink and leader change.
3. Restart the broker. Watch it rejoin the ISR.
4. Configure a producer with `acks=all`. Send messages while one broker is down. Do they succeed?

---

**Next up:** [Message Delivery Semantics](delivery-semantics.md) — At-most-once, at-least-once, exactly-once.
