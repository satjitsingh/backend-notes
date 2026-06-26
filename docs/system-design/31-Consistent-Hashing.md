# 31. Consistent Hashing

> We flagged this twice — in sharding (Chapter 14) and load balancing (Chapter 28) — as the fix for a specific, painful problem: when you add or remove a server, naive hashing reshuffles *almost everything*. Consistent hashing is the elegant algorithm that makes adding/removing nodes cheap. It's the secret sauce behind distributed caches, databases like Cassandra and DynamoDB, and CDNs.

---

## What is it?

**Consistent hashing is a technique for distributing data (or requests) across a set of servers such that adding or removing a server only moves a *small fraction* of the data — instead of reshuffling almost all of it.**

To appreciate why it exists, you first need to feel the pain of the naive approach. Recall hash-based sharding from Chapter 14:
```
server = hash(key) % N      (N = number of servers)
```
This distributes keys evenly across N servers — great. But watch what happens when N changes. Say you go from 4 servers to 5:
```
hash(key) % 4  vs  hash(key) % 5   →  almost EVERY key maps to a different server now!
```
Because the modulus changed, the result changes for *nearly every key*. In a distributed cache, this means **almost the entire cache is suddenly invalid** — every key is now looked for on the wrong server, causing a storm of cache misses that all hit the database at once (a cache avalanche, Chapter 17) — potentially crashing it. Adding *one* server triggers a system-wide catastrophe.

> **The core problem:** `hash(key) % N` couples *every* key's location to the *total number* of servers. Change N, and everything moves. This makes scaling (adding nodes) or failure (removing nodes) extremely disruptive — the opposite of what you want in an elastic, fault-tolerant system.

**Consistent hashing breaks this coupling.** With it, adding or removing one server only affects the keys that were on (or adjacent to) *that* server — roughly **1/N of the keys** move, not ~all of them. This is what makes distributed systems able to scale and heal smoothly.

---

## How it Works Under the Hood

### The hash ring

The central idea is to map *both* servers *and* keys onto the same circular space — a **hash ring** — and assign each key to the server nearest it on the ring.

Step by step:

**1. Imagine a ring of hash values.** Take the output range of a hash function (say 0 to 2³²−1) and bend it into a circle, so the largest value wraps around to 0.

```
                0 / max
                  ·
          ·                 ·
       ·                       ·
      ·         (hash ring)      ·
       ·                       ·
          ·                 ·
                  ·
```

**2. Place servers on the ring.** Hash each *server's* identifier (name/IP) and place it at that position on the ring.
```
        [Server A]
       ·           ·
   ·                   [Server B]
   ·                   ·
       ·           ·
        [Server C]
```

**3. Place keys on the ring.** Hash each *key* and place it at its position too.

**4. Assign each key to a server by walking clockwise.** A key belongs to the **first server found going clockwise** from the key's position.
```
   key X (here) → walk clockwise → first server hit is [Server B] → X lives on B
```

### Why adding/removing a node is now cheap

This is the magic. Consider what happens on a change:

**Adding a server (D):** you hash D and place it on the ring. Now only the keys that fall in the arc *between D and the previous server (counter-clockwise)* get reassigned to D. **Every other key stays exactly where it was.** Only ~1/N of keys move — and they move from just *one* neighbor, not from everywhere.

```
Before: ...[C]----keys----[A]...      (these keys went to A)
After:  ...[C]--keys--[D]--keys--[A]  (only keys between C and D move to D; rest unchanged)
```

**Removing a server (B fails):** B's keys simply go to the *next server clockwise*. All other servers' keys are untouched. The failure's impact is contained to B's share — exactly the "contain the blast radius" goal from Chapter 27.

Compare to naive modulo where *every* key moved. Consistent hashing reduces the disruption from ~100% to ~1/N. That's the whole point.

### The problem of uneven distribution → Virtual Nodes

There's a catch. With few servers placed randomly on the ring, the arcs between them can be very **uneven** — one server might own a huge arc (and thus most keys) while another owns a tiny one. This creates **hotspots** (the very thing we're trying to avoid). Also, when a server is removed, *all* its load dumps onto a single neighbor, overloading it.

The fix is **virtual nodes (vnodes)**:

> Instead of placing each physical server on the ring *once*, place it at **many positions** (e.g., 100–200 virtual copies, each from hashing `serverA#1`, `serverA#2`, ...). Each physical server is now represented by many small arcs scattered around the ring.

Benefits:
- **Even distribution:** with many small arcs per server, the load evens out statistically — no giant arcs, no hotspots.
- **Smooth rebalancing:** when a server is added or removed, its many small arcs are spread across *many* other servers, so the load redistributes evenly rather than dumping onto one neighbor.
- **Heterogeneous servers:** a more powerful server can be given *more* vnodes (more arcs = more keys), naturally weighting load by capacity.

Virtual nodes are essential in practice — real systems (Cassandra, DynamoDB) always use them.

### Where it's used
- **Distributed caches** (Memcached/Redis clusters): so adding/removing a cache node doesn't invalidate the whole cache (the avalanche we started with).
- **Distributed databases** (Cassandra, DynamoDB, Riak): the leaderless, partition-by-hash systems from Chapter 15 use consistent hashing to assign data to nodes, enabling elastic scaling and node failure without mass data movement.
- **Load balancers** (Chapter 28): to route a given client/key consistently to the same server (session affinity, cache locality) while tolerating server changes.
- **CDNs:** to map content to edge servers consistently.

---

## Why do we need it?

We need consistent hashing because **it's what makes distributed systems *elastic and fault-tolerant* — able to add capacity and survive node failures without catastrophic data movement:**

1. **It enables smooth scaling.** Adding a server (to handle growth) should be a routine, low-impact operation. With naive hashing it's a system-wide reshuffle (and cache avalanche); with consistent hashing, only ~1/N of data moves. This is the difference between "scaling is scary" and "scaling is routine."

2. **It enables graceful failure handling.** When a node dies (which it will, Chapter 5/26), only *its* share of data needs to relocate — to its ring neighbors — while everything else keeps working. The blast radius is contained (Chapter 27).

3. **It prevents cache avalanches.** For distributed caches, it ensures a node change doesn't invalidate the entire cache and stampede the database (Chapter 17) — protecting the database behind it.

4. **It's foundational for leaderless distributed databases.** The entire elastic, highly-available design of Cassandra/DynamoDB (Chapter 15) depends on consistent hashing to place and move data as the cluster grows and shrinks.

**When to use:** whenever you're distributing data or requests across a *dynamic* set of nodes — nodes that will be added, removed, or fail over time. (If the node set were truly fixed forever, naive hashing would suffice — but it almost never is.)

---

## Real-World / Fintech Example

In our **digital wallet / payments app**, consistent hashing quietly powers the elastic, resilient pieces of infrastructure.

**The distributed balance cache (Redis cluster).** The app caches balances and sessions across a **Redis cluster** of several nodes (Chapters 2, 17). The cluster uses consistent hashing to decide which node holds which key.
- **The problem it avoids:** during a festival sale, the team scales the cache from 4 to 6 nodes to handle the read surge. With *naive* `hash(key) % N`, going from 4→6 would relocate ~almost every cached balance — every lookup would miss, and **800,000 reads/sec would stampede PostgreSQL all at once**, very likely crashing it at the worst possible moment (cache avalanche, Chapter 17).
- **With consistent hashing:** adding 2 nodes only moves ~1/3 of the keys to the new nodes; the other ~2/3 of cached balances stay exactly where they are and keep serving hits. The database sees only a modest, survivable bump in misses. **Scaling the cache mid-peak becomes safe** — exactly what an elastic money system needs.

**Node failure is contained.** When one Redis node crashes, only *its* keys are lost (and re-fetched from the database on the next read); the other nodes' cached data is untouched. The blast radius is one node's worth, not the whole cache.

**The event/fraud store (Cassandra).** The fraud-event firehose lives in **Cassandra** (Chapters 10, 15), which uses consistent hashing (with **virtual nodes**) internally to distribute data across its cluster. This is *why* the team can add Cassandra nodes as event volume grows — the cluster rebalances by moving only a fraction of data per new node, with no downtime. Virtual nodes keep the load even across nodes of differing sizes and ensure a failed node's data redistributes smoothly across many peers rather than crushing one neighbor.

**Why virtual nodes matter here.** Without vnodes, a crashed Cassandra node would dump its *entire* load onto a single neighbor, potentially overloading it and triggering a *secondary* failure (a mini-cascade, Chapter 26). With vnodes, that load scatters across many nodes — each absorbs a tiny slice. This is the difference between a contained failure and a cascading one.

> The lesson: consistent hashing is invisible when it works, but it's what lets the team **scale the cache during peak and survive node failures without catastrophe.** Naive hashing would make both operations dangerous. It's a small algorithm with outsized impact on elasticity and resilience.

In practice, the team doesn't implement consistent hashing by hand — it's built into **Redis Cluster** and **Cassandra**. But understanding it explains *why* those systems scale and heal gracefully, and informs choices like vnode counts and replication placement.

---

## Trade-offs (Pros & Cons)

**Pros**
- **Minimal data movement on change** — adding/removing a node relocates only ~1/N of keys, not ~all.
- **Smooth, safe scaling** — add capacity as a routine operation, even during peak.
- **Graceful failure handling** — a dead node's load goes only to neighbors; blast radius contained.
- **Prevents cache avalanches** — node changes don't invalidate the whole cache.
- **Foundational for elastic distributed databases/caches** (Cassandra, DynamoDB, Redis Cluster).
- **Virtual nodes** give even distribution and support heterogeneous server capacities.

**Cons**
- **More complex** than naive modulo hashing — the ring, clockwise lookup, and vnode bookkeeping.
- **Uneven distribution without virtual nodes** — naive placement creates hotspots; vnodes are essentially mandatory, adding metadata overhead.
- **Still needs replication for availability** — consistent hashing places data but doesn't itself make it durable; you replicate each key to the next few nodes on the ring (ties to Chapter 6).
- **Lookups need ring metadata** — clients/coordinators must know the current ring state (managed by the system, but it's state to maintain and propagate).

> **Staff-engineer takeaway:** Consistent hashing solves the disaster of naive `hash(key) % N`, where changing the node count reshuffles *almost everything* (causing cache avalanches and mass data movement). By mapping both nodes and keys onto a **hash ring** and assigning each key to the next node clockwise, **adding or removing a node moves only ~1/N of keys** — making distributed systems elastic and fault-tolerant. Always pair it with **virtual nodes** for even distribution and smooth rebalancing. You rarely implement it yourself (Redis Cluster, Cassandra, DynamoDB do it for you), but understanding it explains *why* those systems scale and heal gracefully — and it's the resolution to the re-sharding pain flagged back in Chapter 14.

---

➡️ Next: [32-Networking-and-Communication.md](32-Networking-and-Communication.md) — the physical and protocol foundation underneath *everything* we've discussed: how bytes actually travel between machines.
