# 6. Replication

> Redundancy needs *copies*, and copies need to be *kept in sync*. Replication is the machinery that does this — and it's where availability, performance, and consistency all collide. Understanding it is the key that unlocks the CAP theorem coming up.

---

## What is it?

**Replication is keeping multiple copies of the same data on different machines, and keeping those copies in sync.**

In the last chapter we said high availability needs redundancy — spare copies ready to take over. Replication is *how those spare copies of your data come to exist and stay current.* Without replication, a "backup database" would be a frozen snapshot from last night, useless for live failover. With replication, every change to the main database flows to the copies, so they're (nearly) up to date and ready.

But replication isn't only for availability. It serves **three distinct purposes**, and it's worth being clear about all three because they pull in slightly different directions:

1. **High availability** — if one copy dies, another has the data and can take over.
2. **Read scalability** — spread read queries across many copies so no single machine is overwhelmed (recall the read-heavy wallet from Chapter 2).
3. **Lower latency / geo-proximity** — put copies physically near users (a copy in India for Indian users, one in the US for US users) so reads are fast.

The vocabulary you'll see everywhere:
- **Leader** (also called **primary** or **master**) — the copy that accepts **writes**.
- **Follower** (also called **replica**, **secondary**, or **slave**) — a copy that receives changes from the leader and typically serves **reads**.

> **The one sentence that captures the whole topic:** Replication is easy when nobody's writing; the entire difficulty is *propagating writes to all the copies fast enough and reliably enough* — and that propagation delay is the source of every interesting trade-off in distributed data.

---

## How it Works Under the Hood

### The dominant model: Leader-Follower (Primary-Replica) replication

This is by far the most common setup, and the one you must understand deeply.

```
                        WRITES
                          |
                          v
                   ┌─────────────┐
                   │   LEADER     │  <- the only node that accepts writes
                   └─────────────┘
                    /     |     \
            (replication stream of changes)
                  /       |       \
                 v        v        v
          ┌─────────┐┌─────────┐┌─────────┐
          │FOLLOWER ││FOLLOWER ││FOLLOWER │  <- serve READS, stay in sync
          └─────────┘└─────────┘└─────────┘
                 ^        ^        ^
                          |
                        READS
```

The mechanics, step by step:
1. A client sends a **write** (e.g., "set Alice's balance to ₹500"). It goes to the **leader** only.
2. The leader applies the change to its own data and **records it in a replication log** (a list of all changes, in order — in databases this is the WAL / binlog).
3. The leader **streams that log** to every follower.
4. Each follower **replays** the changes in the same order, so its copy ends up matching the leader.
5. **Reads** can be served by the leader *or* any follower. Since reads usually vastly outnumber writes, sending reads to followers takes enormous load off the leader — that's your read scaling.

Everything hinges on **one question**: does the leader wait for followers to confirm before telling the client "done"? That choice splits replication into two modes.

### Synchronous vs. Asynchronous replication (the central trade-off)

**Synchronous replication** — the leader waits for follower(s) to confirm the write *before* acknowledging success to the client.

```
Client → Leader: "write X"
Leader → Follower: "write X"
Follower → Leader: "got it, saved!"   (leader WAITS for this)
Leader → Client: "success"            (only now)
```

- **Pro:** the follower is guaranteed to have the data. If the leader dies *right after*, no committed data is lost (**durability**), and a reader hitting the follower sees the latest value (**consistency**).
- **Con:** **slower** — every write pays the cost of a network round trip to the follower. And it's **fragile**: if the follower is slow or down, the write *blocks* or fails. A purely synchronous setup can hurt availability.

**Asynchronous replication** — the leader acknowledges success to the client *immediately*, and streams the change to followers *afterward*, in the background.

```
Client → Leader: "write X"
Leader → Client: "success"            (immediately!)
Leader → Follower: "write X"          (happens a moment later, in background)
```

- **Pro:** **fast** (no waiting) and **resilient** (a slow/dead follower doesn't block writes).
- **Con:** there's a window of **replication lag** — followers are slightly behind the leader. If the leader dies during that window, **the most recent writes are lost forever** (they never reached a follower). And a reader hitting a lagging follower may see **stale (old) data.**

> **This single choice — sync vs. async — is the seed of the CAP theorem (next chapters).** Synchronous favors *consistency and durability* at the cost of *latency and availability*. Asynchronous favors *latency and availability* at the cost of *possibly stale data and possible data loss on failure*. There is no free lunch; you're choosing what to sacrifice.

Most real systems use a pragmatic middle ground: **semi-synchronous** — replicate synchronously to *one* follower (so at least one copy is guaranteed current) and asynchronously to the rest (for speed). You get durability without waiting for everyone.

### The pain points replication creates

**1. Replication lag → stale reads.** With async replication, if you write to the leader and *immediately* read from a follower, you might get the *old* value (your write hasn't arrived yet). This breaks intuitive expectations — "I just updated my profile, why does it show the old one?"

Common fixes (these are **consistency patterns**, covered in depth soon):
- **Read-your-own-writes:** route a user's reads to the leader (or a guaranteed-fresh replica) for a short time after they write, so *they* always see their own changes.
- **Monotonic reads:** ensure a user doesn't see time "go backwards" by pinning them to one replica.

**2. Failover complexity.** When the leader dies, a follower must be promoted to leader. With async replication, that follower might be missing the last few writes → **data loss**. The system must also ensure *exactly one* leader is chosen, or you get the nightmare scenario below.

**3. Split-brain.** If a network glitch makes two nodes *both* think they're the leader, they accept conflicting writes and the data diverges — a **split-brain**. Systems prevent this with consensus/quorum mechanisms (e.g., requiring a majority of nodes to agree on who the leader is).

### Other replication topologies (briefly)

- **Multi-leader replication:** more than one node accepts writes (e.g., one leader per region). Great for write latency and multi-region, but introduces **write conflicts** — two leaders edit the same record at once, and you must resolve the conflict (last-write-wins, merge logic, etc.). Complex.
- **Leaderless replication (Dynamo-style, e.g., Cassandra, DynamoDB):** any node accepts writes/reads, and consistency is achieved by writing to and reading from a **quorum** (a majority of copies). Uses tunable read/write quorums (the famous "R + W > N" rule) to balance consistency vs. availability. Very highly available, but consistency is "eventual" and conflict handling is on you.

For most systems — and most of fintech's core — **single-leader replication is the default**, because having one place that accepts writes makes correctness and transactions far easier to reason about.

---

## Why do we need it?

Replication is foundational; without it, you can't have a serious distributed system. Concretely, it solves:

1. **Durability and availability** — copies on different machines mean a single machine's death doesn't lose your data or take you down. This is the mechanism behind Chapter 5's redundancy.

2. **Read scaling** — for read-heavy systems (almost all of them), followers absorb the read load so the leader isn't crushed. This is *the* standard first step when a single database can't keep up with reads.

3. **Lower latency via geography** — copies near users mean fast local reads instead of slow cross-continent round trips (recall Chapter 3's latency numbers — a local read is ~100× faster than a transcontinental one).

4. **Backups and analytics without hurting production** — you can run heavy analytics queries or take backups against a follower, so they don't slow down the live leader serving users.

**When to use which mode:**
- **Synchronous (or semi-sync):** when **losing data is unacceptable** — financial ledgers, order records, anything where "we lost your last transaction" is catastrophic.
- **Asynchronous:** when **speed and availability matter more than perfect freshness** — social feeds, view counts, caches, analytics, where a few seconds of staleness is harmless.

---

## Real-World / Fintech Example

Back to our **digital wallet / payments app**, where replication choices have real money on the line.

**Read scaling for balances & history.** Reads dominate (~10:1). So we run one **leader** for writes and several **followers**. Balance checks and transaction-history views — pure reads — are served by followers, taking that huge load off the leader. The leader is reserved for the precious write capacity. This is replication-for-scaling straight out of Chapter 2.

**The consistency choice for the money ledger.** Here's where it gets serious. The core ledger uses **synchronous (or semi-synchronous) replication.** Why? Because when we tell Alice "₹500 sent successfully," that fact must **survive the leader instantly dying.** If we used pure async and the leader crashed in the lag window, the transaction would vanish — Alice thinks she paid, Bob never got it, and the money's whereabouts are unknown. Unacceptable. So we pay the latency cost of waiting for at least one follower to confirm, guaranteeing the write is safely on ≥2 machines before we say "success." **Durability of money beats raw speed.**

**The stale-read bug, and its fix.** The team ships a feature and hits a classic bug: Alice tops up her wallet (write → leader), the app *immediately* refreshes her balance (read → a follower), and because of **replication lag**, she briefly sees her *old* balance. She panics. The fix is **read-your-own-writes consistency**: for a few seconds after a user makes a write, route *that user's* balance reads to the leader (or a guaranteed-fresh replica). Other users can keep reading slightly-stale followers — that's fine — but *you* always see *your own* latest change.

**Where async is perfectly fine.** Not everything needs the strict treatment. The "total transactions processed today" counter on an internal dashboard? Served from an **async** follower that's a few seconds behind — nobody cares if it lags. The team *deliberately* uses strong/synchronous replication only for the money ledger and relaxed/async replication for everything cosmetic. **Matching the replication mode to the data's importance is the craft.**

**Failover, handled safely.** The leader's host fails. A monitoring system detects the dead heartbeat and promotes the synchronously-updated follower to new leader. Because that follower was kept *in sync*, **no committed transaction is lost.** A consensus mechanism ensures only one new leader is elected, preventing split-brain. Users experience a few seconds of hiccup, then service resumes — exactly the high availability Chapter 5 promised, made real by replication.

---

## Trade-offs (Pros & Cons)

### Replication in general
**Pros**
- **Durability & availability** — surviving machine death without losing data.
- **Read scalability** — followers absorb read load, the standard fix for read-heavy systems.
- **Lower latency** — copies near users serve fast local reads.
- **Offload heavy work** — run backups/analytics on followers without hurting production.

**Cons**
- **Consistency headaches** — replication lag causes stale reads; you need consistency patterns to manage it.
- **Operational complexity** — failover, leader election, and split-brain prevention are genuinely hard.
- **Write throughput isn't solved** — replication scales *reads*, not *writes* (every write still goes to the single leader). Scaling writes needs **sharding** (later chapter).
- **Cost** — more machines storing full copies of the data.

### Synchronous vs. Asynchronous (the core decision)
| | Synchronous | Asynchronous |
|---|---|---|
| Write speed | Slower (waits for follower) | Fast (no waiting) |
| Data loss on leader crash | None (follower has it) | Possible (lag window lost) |
| Read freshness | Followers current | Followers may be stale |
| Resilience to slow/dead follower | Poor (writes block) | Good (doesn't block) |
| Best for | Money, orders, critical data | Feeds, counters, caches, analytics |

> **Staff-engineer takeaway:** Replication = keeping synced copies of data for *availability, read-scaling, and low-latency geo-reads*. The whole game is **how you propagate writes**: synchronous buys durability and consistency at the cost of latency and availability; asynchronous buys speed and resilience at the cost of stale reads and possible data loss. Use **single-leader** as your default, reach for **sync/semi-sync for money** and **async for cosmetic data**, and remember replication scales reads but **not writes** — that's what sharding is for. This sync-vs-async tension is *exactly* what the CAP theorem formalizes next.

---

➡️ **End of Batch 2.** Next batch dives into the theory this has been building toward: **Consistency & Consistency Patterns**, the **CAP Theorem**, and the **PACELC Theorem** — the formal rules governing the trade-offs you've just seen in action.
