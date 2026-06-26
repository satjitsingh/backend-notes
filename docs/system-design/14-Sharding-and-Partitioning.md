# 14. Sharding and Partitioning

> The nuclear option for scaling. When one machine can't hold all your data or absorb all your writes, you split the data across many machines. It's the only way to scale writes beyond a single server — and it permanently changes how you think about *every* query. Handle with respect.

---

## What is it?

**Partitioning is splitting one large dataset into smaller pieces. Sharding is partitioning where those pieces (shards) live on *different machines*.** The goal: no single machine has to hold all the data or handle all the load.

Let's untangle the vocabulary, because it's used loosely:

- **Partitioning** is the general idea of breaking a big table/dataset into smaller chunks. Those chunks might still live on the same server.
- **Sharding** specifically means **horizontal partitioning across multiple machines** — each shard is a separate database on its own server. (In practice, when people say "sharding" they mean this distributed version.)

And there are two *directions* you can split a table:

**Vertical partitioning — split by *columns*.** Put some columns in one place, others elsewhere. E.g., keep frequently-accessed account columns (`account_id`, `balance`) in a hot table, and rarely-accessed columns (`kyc_documents`, `profile_blob`) in another. You're slicing the table top-to-bottom.

**Horizontal partitioning / sharding — split by *rows*.** Put some rows on one machine, other rows on another. E.g., accounts A–M on Shard 1, accounts N–Z on Shard 2. *Each shard has the full table structure but only a subset of the rows.* This is what matters for scaling writes, and what the rest of this chapter is about.

```
   Before (one DB):              After sharding by user (3 shards):
   ┌────────────────┐           ┌─────────┐ ┌─────────┐ ┌─────────┐
   │  all accounts  │           │ Shard 1 │ │ Shard 2 │ │ Shard 3 │
   │   A through Z  │    ──►     │  A – I  │ │  J – R  │ │  S – Z  │
   │  (one machine) │           │(machine)│ │(machine)│ │(machine)│
   └────────────────┘           └─────────┘ └─────────┘ └─────────┘
```

> **Why sharding is the *only* tool that scales writes:** read replicas (Chapter 13) give you many copies of *all* the data, so every write still must go to the one leader. Sharding gives each machine *different* data, so each can accept writes *independently and in parallel*. Three shards = roughly 3× the write capacity. This is the fundamental reason sharding exists — and why nothing cheaper can replace it for write-bound systems.

---

## How it Works Under the Hood

### The shard key — the most important decision you'll make

Everything hinges on the **shard key** (or partition key): the column whose value decides *which shard a row lives on*. Choose it wisely and the system scales smoothly; choose it badly and you get hot spots, painful queries, and a re-sharding nightmare that can take a team months. The shard key is a near-irreversible decision, so it deserves serious thought.

A good shard key has two properties:
1. **High cardinality and even distribution** — it spreads data and load evenly across shards (no shard gets overloaded).
2. **Matches your access pattern** — the queries you run most should be answerable from a *single* shard (so you don't have to query all of them).

### The three main sharding strategies

**1. Range-based sharding.** Assign rows to shards by ranges of the key (accounts A–I → Shard 1, J–R → Shard 2, ...).
- **Pro:** range queries are efficient ("all users J–M" hit one shard); simple to understand.
- **Con:** prone to **hot spots** and uneven load. If you shard transactions by *date*, today's shard gets *all* the write traffic while old shards sit idle. Bad distribution.

**2. Hash-based sharding.** Run the shard key through a **hash function**, and the hash decides the shard (e.g., `shard = hash(account_id) % number_of_shards`).
- **Pro:** distributes data **evenly** and avoids hot spots — hashing scatters even sequential IDs uniformly.
- **Con:** **range queries become impossible** on a single shard (related rows are scattered everywhere), and — critically — `% number_of_shards` means **adding a shard re-maps almost *all* the data**, forcing a massive, disruptive reshuffle. (This exact problem is what **Consistent Hashing**, a later chapter, was invented to solve.)

**3. Directory-based (lookup) sharding.** Keep a separate **lookup table** that maps each key to its shard ("account A1 → Shard 3").
- **Pro:** maximum flexibility — you can move data between shards and rebalance freely by just updating the directory.
- **Con:** the directory itself becomes a component to maintain and a potential single point of failure / bottleneck (it must be highly available and fast).

### How a sharded query actually works

The application (or a middleware/proxy layer) needs **routing logic**: given a query, figure out which shard(s) hold the relevant data.

- **Single-shard query (the good case):** the query includes the shard key, so routing sends it to *one* shard. Fast, efficient, scales perfectly. *Example:* "get account A1" → `hash(A1)` → Shard 3 → done.
- **Scatter-gather query (the expensive case):** the query *doesn't* include the shard key (or spans many), so it must be sent to **all shards**, and the results combined. *Example:* "sum of all balances across the whole system" → ask every shard, add up the answers. This is slow, scales poorly, and gets worse as you add shards. **Minimizing scatter-gather is a primary design goal** — and it's why your shard key must match your access pattern.

### The hard problems sharding introduces (the price you pay)

This is why sharding is the last resort. It breaks things that were trivial on a single database:

**1. Cross-shard transactions are brutal.** On one database, "debit Alice, credit Bob" is one ACID transaction (Chapter 11). But if Alice is on Shard 1 and Bob on Shard 2, there's **no single database to give you atomicity** across both. You now need either:
- **Distributed transactions (Two-Phase Commit / 2PC):** a coordinator asks all shards to "prepare," then "commit." Correct but *slow* and fragile (if the coordinator dies mid-commit, you're stuck).
- **Sagas (the common modern choice):** break the operation into a sequence of local transactions with **compensating actions** to undo on failure (debit Alice; if crediting Bob fails, *refund* Alice). This gives up atomicity for eventual consistency and is more complex application logic.

**2. Cross-shard JOINs don't work.** SQL JOINs (Chapter 11) assume data is together. Across shards, you can't JOIN directly — you fetch from each shard and join in application code, or you **denormalize** (duplicate data to keep related things on the same shard).

**3. Rebalancing & re-sharding.** When a shard gets too big/hot, or you add capacity, you must move data between shards — a delicate, high-risk operation, especially with hash-based modulo (see Consistent Hashing chapter for the fix).

**4. Operational complexity.** More machines to monitor, back up, and fail over; harder debugging; uneven shards ("celebrity problem" — one hugely active account overloads its shard).

> **The mental model:** Sharding trades the *simplicity of one database* (ACID, JOINs, easy queries) for *unlimited write/data scale*. You don't shard until that trade is forced on you — and when you do, you design the shard key to keep the *common* operations single-shard, accepting pain only on the rare cross-shard ones.

---

## Why do we need it?

We need sharding for exactly two situations that **no other tool can solve**:

1. **Write throughput exceeds one machine.** Read replicas and caching scale reads but not writes (Chapter 13). When the write rate is more than a single leader can durably commit, sharding is the *only* way to add write capacity — by spreading writes across independent shards.

2. **Data is too big for one machine.** When the dataset itself exceeds the disk/RAM of any single server, you *must* split it. There's no machine large enough; partitioning across many is the only option.

A bonus benefit: **smaller datasets per shard** mean smaller indexes and faster queries on each shard, and **fault isolation** — one shard down affects only its slice of users, not everyone.

**When to use it (and the strong warning):**
> Shard **only after exhausting every tool in Chapter 13** (scale up, optimize, cache, read replicas). Sharding is permanent, invasive, and complex. The most common sharding mistake is doing it *too early* — inheriting cross-shard transaction and JOIN pain to solve a scale problem you don't actually have yet. Earn your way to sharding with real, measured write/data pressure.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** has hit the **write wall** from Chapter 13: 80,000 payments/sec overwhelms the single leader even with all reads offloaded. Sharding is now justified.

**Choosing the shard key.** The team shards the accounts/ledger by **`account_id`, using hash-based sharding.** Why this choice:
- **Even distribution:** hashing `account_id` scatters accounts uniformly, so no shard becomes a hot spot (unlike sharding by date or region, which would overload "today" or "Mumbai").
- **Matches access pattern:** the most common operations — "check Alice's balance," "Alice's transaction history," "debit Alice" — all key on `account_id`, so they're **single-shard queries.** Fast and scalable.

```
hash('Alice') % 4 = Shard 2   → all Alice's data + writes live on Shard 2
hash('Bob')   % 4 = Shard 0   → all Bob's data lives on Shard 0
```
Each of the 4 shards now handles ~20,000 payments/sec — within a single machine's reach. Write scaling achieved.

**The cross-shard transaction problem — front and center.** Now the core money transfer breaks. Alice (Shard 2) pays Bob (Shard 0). There's no single database to make "debit Alice + credit Bob" atomic. The team's options:
- **2PC:** correct but slow and fragile — bad for 80k/sec.
- **Saga (their choice):** model the transfer as a sequence — (1) debit Alice on Shard 2; (2) publish a "transfer" event; (3) credit Bob on Shard 0. If step 3 fails, a **compensating transaction** refunds Alice. This is eventually consistent and more complex, but it scales. They lean on idempotency and an event log (Kafka) to make it reliable. (This is precisely the kind of distributed-coordination problem that Event-Driven Architecture and message queues, later chapters, are built to handle.)

Notice the profound shift: a one-line `@Transactional` money transfer (Chapter 11) became a multi-step distributed saga with compensation logic — *that* is the true cost of sharding, and why they delayed it until truly necessary.

**The scatter-gather they tolerate.** A compliance query — "total money held across the entire system" — has no `account_id`, so it must **scatter-gather** across all 4 shards and sum the results. It's slow, but it runs rarely (nightly batch), so they accept it. The design principle held: *common* operations stay single-shard; only *rare* operations pay the scatter-gather tax.

**What they deliberately did NOT shard.** Reference data (list of supported banks, fee schedules) is small and fits on one machine — sharding it would add pain for no benefit. They shard only what's forced: the huge, write-heavy accounts/ledger data. **Shard the minimum.**

---

## Trade-offs (Pros & Cons)

**Pros**
- **The only way to scale writes** beyond a single machine — independent parallel writes per shard.
- **Handles datasets too large for one machine** — split until each piece fits.
- **Smaller per-shard data** → smaller indexes, faster local queries.
- **Fault isolation** — one shard's outage affects only its slice of users.

**Cons**
- **Cross-shard transactions are hard** — lose easy ACID; need 2PC (slow/fragile) or sagas (complex, eventually consistent).
- **Cross-shard JOINs don't work** — forces app-side joins or denormalization.
- **Shard key is near-irreversible** — a bad choice causes hot spots and painful re-sharding.
- **Scatter-gather queries are slow** and scale poorly — anything not keyed on the shard key hits every shard.
- **Rebalancing is risky** — moving data between shards is delicate (and modulo hashing makes adding shards a mass reshuffle → see Consistent Hashing).
- **Major operational complexity** — more machines, harder debugging, the "celebrity/hot-key" problem.

### Quick comparison of sharding strategies
| Strategy | Distribution | Range queries | Adding shards | Main risk |
|---|---|---|---|---|
| **Range** | Can be uneven | Efficient | Easy-ish | Hot spots |
| **Hash** | Even | Impossible (scattered) | Hard (mass reshuffle) | Re-sharding pain |
| **Directory** | Flexible | Depends | Easy (update directory) | Directory is a SPOF/bottleneck |

> **Staff-engineer takeaway:** Sharding splits *rows across machines* and is the **only** tool that scales writes and oversized datasets — at the cost of ACID transactions, JOINs, and operational simplicity. The **shard key is everything**: choose it for even distribution *and* to keep common queries single-shard (hash on `account_id` for a wallet). Expect cross-shard operations to become **sagas**, not transactions. And the cardinal rule: **shard last, shard the minimum, and only when real write/data pressure forces your hand.**

---

➡️ Next: [15-Non-Relational-Databases.md](15-Non-Relational-Databases.md) — the other escape hatch from the single-relational-database wall: NoSQL, what its four families are good at, and when to choose it over (or alongside) relational.
