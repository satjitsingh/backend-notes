# DynamoDB & Caching

---

## 1. DynamoDB

### What is it?
DynamoDB is a fully managed **NoSQL** key-value and document database.
It's serverless — no servers to provision, patch, or manage. You create a table and go.
Single-digit millisecond response at any scale. Handles millions of requests per second.

### Why it matters in production
- DynamoDB is the go-to for high-scale, low-latency workloads on AWS.
- It's used for session stores, gaming leaderboards, IoT, e-commerce carts, and metadata.
- It's serverless and scales automatically — perfect for DevOps (no DB ops needed).

### Core Ideas
- **Table** — The core resource. Has a name and a primary key.
- **Item** — A single record (like a row in SQL). Max 400 KB.
- **Attribute** — A field within an item (like a column, but flexible — no fixed schema).
- **Primary Key** — Uniquely identifies each item. Two types:
  - **Partition Key (PK)** — Single attribute (e.g., `userId`).
  - **Composite Key** — Partition Key + Sort Key (e.g., `userId` + `orderDate`).

**Capacity modes:**

| Mode | How It Works | Best For |
|------|-------------|----------|
| **On-Demand** | Pay per request. No planning. | Unpredictable traffic, new apps |
| **Provisioned** | You set RCUs and WCUs. Cheaper if predictable. | Steady, predictable traffic |

- **RCU** (Read Capacity Unit) = 1 strongly consistent read of 4 KB/s.
- **WCU** (Write Capacity Unit) = 1 write of 1 KB/s.
- **Eventually consistent reads** = 2x throughput (but data may be slightly stale).
- **Strongly consistent reads** = Latest data, but 1x throughput.

**Key features:**
- **Auto Scaling** (provisioned mode) — Adjusts RCUs/WCUs based on usage.
- **DynamoDB Streams** — Capture item-level changes (like a changelog). Trigger Lambda.
- **TTL (Time to Live)** — Auto-delete items after a timestamp. Free.
- **Global Tables** — Multi-Region, multi-active replication.
- **DAX (DynamoDB Accelerator)** — In-memory cache. Microsecond reads.
- **Transactions** — ACID transactions across multiple items/tables.
- **Point-in-Time Recovery** — Restore table to any second in last 35 days.

### Quick Analogy
DynamoDB = A massive hash map in the cloud.
Give it a key → Get a value back instantly. It doesn't care if you have 10 items or 10 billion.
No schema — every item can have different attributes.

### Architecture View
```
DynamoDB Table: Orders
┌────────────────────────────────────────────────────┐
│ Partition Key: userId    Sort Key: orderDate        │
├────────────────┬──────────────┬─────────────────────┤
│ userId (PK)    │ orderDate(SK)│ Attributes          │
├────────────────┼──────────────┼─────────────────────┤
│ user-001       │ 2024-01-15   │ total: $99, items: 3│
│ user-001       │ 2024-02-20   │ total: $45, items: 1│
│ user-002       │ 2024-01-10   │ total: $200, items: 5│
│ user-003       │ 2024-03-01   │ total: $30, items: 2│
└────────────────┴──────────────┴─────────────────────┘

Query: Get all orders for user-001 → Returns 2 items (fast!)
Query: Get user-001's order on 2024-01-15 → Returns 1 item (fastest!)

Under the Hood:
┌────────────┐ ┌────────────┐ ┌────────────┐
│ Partition 1│ │ Partition 2│ │ Partition 3│
│ user-001   │ │ user-002   │ │ user-003   │
│ (sorted by │ │ (sorted by │ │ (sorted by │
│  orderDate)│ │  orderDate)│ │  orderDate)│
└────────────┘ └────────────┘ └────────────┘
   10 GB max per partition key value
```

### Hands-On (Step-by-step Lab)
1. Go to **DynamoDB → Create Table**.
2. Table name: `Orders`. Partition key: `userId` (String). Sort key: `orderDate` (String).
3. Capacity: **On-Demand** (for testing).
4. Create → Table is ready in seconds.
5. Go to **Items → Create Item** → Add: `userId = user-001`, `orderDate = 2024-01-15`, `total = 99`.
6. Add more items with different users and dates.
7. **Query:** Partition key = `user-001` → Returns all orders for that user.
8. **Scan:** Returns ALL items in the table (expensive — avoid in production).

### Common Mistakes
- ❌ Using **Scan** instead of **Query** — Scan reads the ENTIRE table. Very expensive.
- ❌ Bad partition key design — Using a low-cardinality key (like `status`) creates hot partitions.
- ❌ Exceeding 10 GB per partition key value — data gets throttled.
- ❌ Not enabling Point-in-Time Recovery — accidental deletes are unrecoverable.
- ❌ Using DynamoDB for relational queries (JOINs, complex aggregations) — wrong tool.

### Pro Tips
- ✅ **Design your access patterns FIRST, then model the table.** DynamoDB is query-driven, not entity-driven.
- ✅ Use **On-Demand** for new apps. Switch to **Provisioned + Auto Scaling** once patterns are known.
- ✅ Use **TTL** to auto-clean expired sessions, old logs, temporary data.
- ✅ Use **DynamoDB Streams + Lambda** for event-driven architectures (e.g., send email on new order).
- ✅ In interviews: "We use DynamoDB for high-throughput, low-latency workloads like session management and real-time analytics. We design tables around access patterns, use composite keys for efficient queries, and enable DynamoDB Streams for event-driven processing."

---

## 2. Partition Key (Primary Key Design)

### What is it?
The Partition Key determines **which partition** your data is stored on in DynamoDB.
A good partition key spreads data evenly. A bad one creates hot spots and throttling.
This is the single most important design decision in DynamoDB.

### Why it matters in production
- Bad partition key = uneven data distribution = hot partitions = throttling = downtime.
- DynamoDB scales by adding partitions. If all data goes to one partition, adding more doesn't help.
- Interviewers test this to see if you understand DynamoDB internals.

### Core Ideas
- **High cardinality** — The key should have many unique values (e.g., `userId`, `orderId`).
- **Even distribution** — Traffic should spread evenly across key values.
- **Composite key** — Partition Key + Sort Key enables range queries within a partition.

**Good partition keys:**

| Key | Why Good |
|-----|----------|
| `userId` | Millions of unique users — even spread |
| `orderId` | Every order is unique — perfect distribution |
| `deviceId` | IoT — each device is distinct |

**Bad partition keys:**

| Key | Why Bad |
|-----|---------|
| `status` | Only "active"/"inactive" — 2 partitions, massive hot spot |
| `date` | Today's date gets all traffic — one hot partition |
| `country` | Few values, uneven distribution (90% might be "US") |

**Composite key pattern:**
```
PK: userId    SK: orderDate
──────────────────────────────
user-001      2024-01-15      ← Query: PK=user-001 → All orders
user-001      2024-02-20      ← Query: PK=user-001, SK>2024-02 → Feb+ orders
user-002      2024-01-10
```

### Quick Analogy
Partition key = A filing system.
If you file everything under "A" (bad key), that drawer overflows while B-Z are empty.
If you file by customer name (good key), drawers fill evenly.

### Architecture View
```
GOOD DESIGN (userId as PK):
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│ Partition A│ │ Partition B│ │ Partition C│ │ Partition D│
│ user-001   │ │ user-002   │ │ user-003   │ │ user-004   │
│ 25% traffic│ │ 25% traffic│ │ 25% traffic│ │ 25% traffic│
└────────────┘ └────────────┘ └────────────┘ └────────────┘
✅ Even distribution. Each partition handles 25% load.

BAD DESIGN (status as PK):
┌──────────────────────────────────────────┐ ┌────────────┐
│              Partition A                 │ │ Partition B│
│              status=active               │ │status=done │
│              95% traffic 🔥               │ │ 5% traffic │
└──────────────────────────────────────────┘ └────────────┘
❌ Hot partition. Partition A is overwhelmed. Throttling occurs.
```

### Hands-On (Step-by-step Lab)
1. Create table `BadDesign` with PK: `status` (String).
2. Insert 1000 items with `status = "active"` and 10 with `status = "done"`.
3. Notice: All active items are on one partition — queries on "active" are slow at scale.
4. Create table `GoodDesign` with PK: `userId` (String), SK: `createdAt` (String).
5. Insert 1000 items with diverse `userId` values.
6. Query by `userId` → Fast. Data is spread evenly.
7. Check **CloudWatch → ConsumedReadCapacityUnits** per partition to see distribution.

### Common Mistakes
- ❌ Using low-cardinality attributes as partition key (status, boolean, country).
- ❌ Not thinking about access patterns before choosing the key.
- ❌ Using only a partition key when a composite key would enable more queries.
- ❌ Assuming DynamoDB auto-balances poorly designed keys — it doesn't.

### Pro Tips
- ✅ **Rule:** If in doubt, use a UUID or a natural high-cardinality ID as the partition key.
- ✅ For time-series data, use `deviceId` as PK and `timestamp` as SK — not date as PK.
- ✅ Use **write sharding** for extremely hot keys: append a random suffix (e.g., `userId#3`).
- ✅ Use **DynamoDB Contributor Insights** to identify hot keys.
- ✅ In interviews: "We choose high-cardinality partition keys like userId or orderId to ensure even data distribution. For time-series, we use deviceId as PK and timestamp as SK. We monitor hot keys with Contributor Insights."

---

## 3. Global Secondary Index (GSI)

### What is it?
A GSI lets you query DynamoDB on a **different key** than the table's primary key.
It's like creating an alternative "view" of your data with a different sort order.
The GSI has its own partition key and optional sort key — completely independent of the table's key.

### Why it matters in production
- Your table's primary key supports one access pattern. GSIs unlock additional patterns.
- Without GSIs, you'd need to Scan the entire table for alternative queries — very expensive.
- Real-world tables often have 2-5 GSIs for different access patterns.

### Core Ideas
- **GSI has its own PK and SK** — Can be any attributes from the table.
- **Eventually consistent** — GSIs replicate asynchronously. Slight lag from the base table.
- **Projected attributes** — You choose which attributes to copy into the GSI (ALL, KEYS_ONLY, or specific).
- **Separate capacity** — GSIs have their own RCUs/WCUs (or share On-Demand capacity).
- **Max 20 GSIs per table.**
- GSIs can be created **after** the table exists (unlike LSIs which must be created at table creation).

**LSI vs GSI:**

| Feature | GSI | LSI (Local Secondary Index) |
|---------|-----|---------------------------|
| **Key** | Different PK + different SK | Same PK + different SK |
| **Created** | Anytime | At table creation only |
| **Consistency** | Eventually consistent | Strongly or eventually |
| **Limit** | 20 per table | 5 per table |
| **Size** | Unlimited | 10 GB per PK value |
| **Recommendation** | Preferred | Rarely needed |

### Quick Analogy
Base table = A phone book sorted by last name.
GSI = A second phone book of the same data, sorted by city.
Same people, different index. You choose which book to search depending on your question.

### Architecture View
```
Base Table: Orders
PK: userId    SK: orderDate
┌───────────┬──────────────┬────────┬──────────┐
│ userId    │ orderDate    │ status │ total    │
├───────────┼──────────────┼────────┼──────────┤
│ user-001  │ 2024-01-15   │ shipped│ $99      │
│ user-001  │ 2024-02-20   │ pending│ $45      │
│ user-002  │ 2024-01-10   │ shipped│ $200     │
└───────────┴──────────────┴────────┴──────────┘

Access Pattern 1: "Get all orders for a user" → Query base table by userId ✅

GSI: StatusIndex
PK: status    SK: orderDate
┌──────────┬──────────────┬───────────┬──────────┐
│ status   │ orderDate    │ userId    │ total    │
├──────────┼──────────────┼───────────┼──────────┤
│ pending  │ 2024-02-20   │ user-001  │ $45      │
│ shipped  │ 2024-01-10   │ user-002  │ $200     │
│ shipped  │ 2024-01-15   │ user-001  │ $99      │
└──────────┴──────────────┴───────────┴──────────┘

Access Pattern 2: "Get all shipped orders" → Query GSI by status ✅
Access Pattern 3: "Get pending orders in date range" → Query GSI by status + orderDate range ✅
```

### Hands-On (Step-by-step Lab)
1. Using your existing `Orders` table → Go to **Indexes → Create Index**.
2. Partition Key: `status` (String). Sort Key: `orderDate` (String).
3. Index name: `StatusDateIndex`.
4. Projected attributes: ALL.
5. Create → Wait for "Active" status.
6. Go to **Items → Query → Select index: StatusDateIndex**.
7. Query: PK = `shipped` → Returns all shipped orders, sorted by date.
8. Compare: Querying the base table by `status` would require a full Scan.

### Common Mistakes
- ❌ Creating GSIs with low-cardinality PK (same hot partition problem as base table).
- ❌ Projecting ALL attributes when you only need a few — wastes storage and WCUs.
- ❌ Not provisioning enough WCUs for the GSI — writes to base table throttle if GSI can't keep up.
- ❌ Expecting strong consistency from GSIs — they're always eventually consistent.
- ❌ Creating too many GSIs — each one duplicates data and costs money.

### Pro Tips
- ✅ **Design all GSIs at the beginning** — based on your access patterns.
- ✅ Use **sparse indexes** — If only some items have a GSI attribute, only those items appear in the GSI (saves space).
- ✅ Use **overloaded GSIs** — One GSI with generic PK/SK names supporting multiple access patterns (advanced single-table design).
- ✅ Monitor GSI `ThrottleEvents` in CloudWatch.
- ✅ In interviews: "We design DynamoDB tables with access patterns in mind and create GSIs for secondary query patterns. We use sparse indexes to keep GSIs lean and monitor throttle events to ensure GSIs have sufficient capacity."

---

## 4. ElastiCache

### What is it?
ElastiCache is a fully managed **in-memory cache** service.
It supports two engines: **Redis** and **Memcached**.
It sits between your app and database, caching frequent queries for microsecond response times.

### Why it matters in production
- Database queries take milliseconds. Cache hits take microseconds. 100-1000x faster.
- Caching reduces database load, prevents timeouts, and saves money.
- ElastiCache is essential for any high-traffic application.

### Core Ideas

**Redis vs Memcached:**

| Feature | Redis | Memcached |
|---------|-------|-----------|
| **Data structures** | Strings, lists, sets, hashes, sorted sets | Strings only |
| **Persistence** | Yes (snapshots, AOF) | No |
| **Replication** | Yes (primary + replicas) | No |
| **Multi-AZ failover** | Yes (automatic) | No |
| **Clustering** | Yes (up to 500 nodes) | Yes (multithreaded) |
| **Use case** | Most use cases, sessions, leaderboards | Simple caching, multithreaded |
| **Recommendation** | **Default choice** | Only if you need multithreaded simplicity |

**Caching strategies:**

| Strategy | How It Works | Pros | Cons |
|----------|-------------|------|------|
| **Lazy Loading** | Load into cache only when requested (cache miss → DB → cache) | Only caches what's needed | First request is slow (cache miss) |
| **Write-Through** | Write to cache AND DB simultaneously | Cache always up to date | Writes are slower; caches unused data |
| **TTL (Time to Live)** | Data expires after X seconds | Prevents stale data | Expired data causes cache miss |

### Quick Analogy
ElastiCache = A sticky note on your desk.
Instead of walking to the filing cabinet (database) every time, you write the answer on a sticky note (cache). Next time someone asks, you just look at the note — instant response. But the note gets old, so you refresh it periodically (TTL).

### Architecture View
```
Application Flow with Cache:
┌──────────────┐
│  Application │
└──────┬───────┘
       │
       ▼
  ┌─────────────┐
  │ Check Cache │
  └──────┬──────┘
         │
    ┌────▼────┐
    │Cache HIT│──▶ Return data (microseconds ⚡)
    └─────────┘
         │
    ┌────▼─────┐
    │Cache MISS│──▶ Query Database ──▶ Store in Cache ──▶ Return data
    └──────────┘

Architecture:
┌───────────────────────────────────────────────────┐
│ VPC (Private Subnets)                             │
│                                                   │
│ ┌──────────┐    ┌──────────────────┐    ┌───────┐│
│ │ EC2/ECS  │◄──►│ ElastiCache      │    │  RDS  ││
│ │ App      │    │ Redis Cluster    │    │  DB   ││
│ │          │    │                  │    │       ││
│ │          │    │ Primary (AZ-1a)  │    │       ││
│ │          │◄──►│ Replica (AZ-1b)  │    │       ││
│ │          │    │ Replica (AZ-1c)  │    │       ││
│ └──────────┘    └──────────────────┘    └───────┘│
│                                                   │
│ Cache HIT (99% of reads) → ~0.5ms                │
│ Cache MISS (1% of reads) → Query RDS → ~5ms      │
└───────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **ElastiCache → Create Cluster → Redis**.
2. Node type: `cache.t3.micro`. Replicas: 1.
3. Place in **private subnets** (same VPC as your app).
4. Security Group: Allow port 6379 from `app-sg`.
5. Create → Wait for "Available".
6. SSH into your EC2 app server → Install `redis-cli`:
```bash
sudo amazon-linux-extras install redis6
redis-cli -h <elasticache-endpoint> -p 6379
SET mykey "hello"
GET mykey  → "hello"
```
7. Implement lazy loading in your app: Check Redis first → If miss, query DB → Store result in Redis with TTL.

### Common Mistakes
- ❌ Not setting TTL — stale data stays in cache forever.
- ❌ Caching everything — only cache frequently accessed, expensive queries.
- ❌ Putting ElastiCache in a public subnet — it should be private.
- ❌ Not handling cache failures gracefully — app should fall back to DB.
- ❌ Choosing Memcached when you need persistence or failover (use Redis).

### Pro Tips
- ✅ **Use Redis** as the default choice — it's more feature-rich than Memcached.
- ✅ Use **Lazy Loading + TTL** as the default caching strategy.
- ✅ Use ElastiCache for **session storage** — faster and more scalable than DB-backed sessions.
- ✅ Enable **Multi-AZ** with automatic failover for Redis in production.
- ✅ Monitor **CacheHitRate** in CloudWatch — should be > 80%. If not, review your caching logic.
- ✅ In interviews: "We use ElastiCache Redis in front of our database with lazy loading and 5-minute TTL. Cache hit rates are above 95%, reducing database load by 90%. Redis runs in private subnets with Multi-AZ failover."

---

## 5. When to Choose Which Database

### What is it?
AWS offers 15+ database services. Choosing the right one depends on your data model, access patterns, scale, and latency requirements.
There is no single "best" database — each is optimized for a specific use case.
This is a very common interview question: "Which database would you choose and why?"

### Why it matters in production
- Wrong database choice = poor performance, high costs, painful migration later.
- Migrating databases after launch is one of the hardest things in engineering.
- Understanding trade-offs is a senior DevOps/architect skill.

### Core Ideas

**The Decision Matrix:**

| Database | Type | Best For | Avoid When |
|----------|------|----------|------------|
| **RDS (MySQL/PostgreSQL)** | Relational | CRUD apps, traditional queries, JOINs | Massive scale, schema-less data |
| **Aurora** | Relational (cloud-native) | High-perf relational, 5x MySQL speed | Small workloads (overkill) |
| **DynamoDB** | NoSQL (key-value) | High scale, low latency, serverless | Complex queries, JOINs, aggregations |
| **ElastiCache (Redis)** | In-memory cache | Session store, caching, leaderboards | Persistent primary storage |
| **DocumentDB** | Document (MongoDB-compatible) | JSON document queries, MongoDB migration | Already using DynamoDB well |
| **Neptune** | Graph | Social networks, fraud detection, recommendations | Simple key-value or relational data |
| **Redshift** | Data warehouse | Analytics, OLAP, BI dashboards | OLTP (transactional workloads) |
| **Keyspaces** | Wide column (Cassandra) | Cassandra migration, time-series at scale | New projects (use DynamoDB) |
| **Timestream** | Time-series | IoT metrics, monitoring, DevOps metrics | Non-time-series data |
| **QLDB** | Ledger | Immutable audit logs, financial transactions | General purpose data |
| **MemoryDB** | Redis-compatible (durable) | Ultra-fast with durability needs | Just caching (use ElastiCache) |

**Quick Decision Flow:**

```
Need JOINs / complex queries?
├── YES → Need high performance?
│         ├── YES → Aurora
│         └── NO  → RDS (MySQL/PostgreSQL)
└── NO → Need millisecond latency at massive scale?
          ├── YES → DynamoDB
          └── NO → Need full-text search?
                    ├── YES → OpenSearch
                    └── NO → Need analytics on huge datasets?
                              ├── YES → Redshift
                              └── NO → Need caching?
                                        ├── YES → ElastiCache Redis
                                        └── NO → DynamoDB (default NoSQL)
```

### Quick Analogy
Choosing a database = Choosing a vehicle.
- Need to carry passengers? → Sedan (RDS).
- Need a sports car? → Aurora.
- Need to deliver packages fast at scale? → Delivery van (DynamoDB).
- Need a quick ride across the block? → Bicycle (ElastiCache).
- Need to analyze traffic patterns? → Traffic control center (Redshift).

### Architecture View
```
Typical Production System — Multiple Databases:

┌────────────────────────────────────────────────────────┐
│                                                        │
│  User Request ──▶ ALB ──▶ App Server                   │
│                              │                         │
│                    ┌─────────┼──────────────┐          │
│                    │         │              │          │
│                    ▼         ▼              ▼          │
│              ElastiCache   Aurora       DynamoDB       │
│              (Redis)       (Primary DB)  (Sessions,    │
│              - Caching     - Users       Metadata)     │
│              - Sessions    - Orders                    │
│              - Rate limit  - Products                  │
│                                                        │
│                              │                         │
│                              ▼                         │
│                          Redshift                      │
│                          (Analytics, BI)               │
│                          Loaded nightly via ETL        │
│                                                        │
│                              │                         │
│                              ▼                         │
│                          OpenSearch                    │
│                          (Full-text search)            │
│                          Synced from Aurora            │
└────────────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. **Scenario:** Build a simple e-commerce backend.
2. **User profiles + Orders + Products** → Aurora (relational, needs JOINs).
3. **Shopping cart** → DynamoDB (high throughput, key-value, TTL for expiry).
4. **Session store** → ElastiCache Redis (microsecond reads, auto-expiry).
5. **Product search** → OpenSearch (full-text search).
6. **Sales analytics** → Redshift (weekly aggregation reports).
7. Map each to the right database → Justify your choice in 1 sentence.

### Common Mistakes
- ❌ Using RDS for everything — hitting scaling limits for high-throughput workloads.
- ❌ Using DynamoDB for complex JOIN queries — it doesn't support JOINs.
- ❌ Using a database for caching — use ElastiCache instead.
- ❌ Using Redshift for transactional (OLTP) workloads — it's for analytics (OLAP).
- ❌ Choosing a database based on familiarity, not requirements.

### Pro Tips
- ✅ **Default to Aurora** for relational. **Default to DynamoDB** for NoSQL.
- ✅ **Always add ElastiCache** in front of your primary database for reads.
- ✅ Use **purpose-built databases** — AWS recommends using the right DB for each workload.
- ✅ Consider **DynamoDB first** for new serverless applications — fully managed, scales infinitely.
- ✅ In interviews: "We use a purpose-built database strategy. Aurora for relational workloads with JOINs, DynamoDB for high-throughput key-value access, ElastiCache Redis for caching and sessions, and Redshift for analytics. Each database is chosen based on the specific access patterns and performance requirements."
