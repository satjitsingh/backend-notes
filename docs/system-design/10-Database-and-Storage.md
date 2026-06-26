# 10. Database and Storage

> All the theory so far — replication, consistency, CAP, PACELC — was building toward the place where data actually *lives*. This chapter is the foundation of storage: how data physically gets written, found, and protected, and the building blocks every database is made of.

---

## What is it?

**Storage is the broad problem of *putting data somewhere and getting it back reliably*; a database is a specialized piece of software that solves that problem with structure, safety, and speed.**

Let's build the idea from the ground up, because "just save the data" hides enormous depth.

At the simplest level, you could store data in a plain file. Want to save a user? Append a line to `users.txt`. This actually works — for a while. But it falls apart fast:
- **Finding data is slow.** To find one user, you scan the *entire* file line by line.
- **Concurrent access corrupts it.** Two requests writing at once garble each other.
- **Crashes lose or corrupt data.** A crash mid-write leaves a half-written, broken file.
- **No relationships, no validation, no querying.** It's just text.

A **database** is the software that solves *all* of these problems properly. It gives you:
1. **Efficient retrieval** via indexes (find one record among billions in milliseconds).
2. **Safe concurrency** so thousands of users can read/write at once without corruption (transactions, locking — covered in the isolation-levels chapter).
3. **Durability** so committed data survives crashes and power loss.
4. **Structure and querying** so you can ask rich questions ("all payments over ₹10,000 last month").

It's also worth separating two layers of the storage world:
- **The storage engine** — the low-level component that actually reads/writes bytes to disk and manages indexes (e.g., InnoDB in MySQL, the B-tree/LSM machinery). This is the "engine under the hood."
- **The database** — the full system wrapped around the engine: query language (SQL), transactions, networking, users/permissions, replication.

This chapter focuses on the *universal fundamentals* shared by **every** database — relational or NoSQL — before later chapters specialize. The big ideas: **how data is stored on disk, how indexes make lookups fast, and the two great families of storage engines (B-Tree vs LSM-Tree).**

---

## How it Works Under the Hood

### The memory hierarchy: why storage design is all about avoiding the disk

Recall the latency numbers from Chapter 3. Reading from **memory (RAM)** takes nanoseconds; reading from **disk** takes milliseconds — roughly **100,000× slower**. But RAM is volatile (it's wiped on power loss) and expensive, while disk is durable and cheap.

This single tension drives *all* storage design:

> Databases must keep the **durable truth on disk** (so it survives crashes), but disk is painfully slow, so they work tirelessly to **avoid touching disk** — caching hot data in RAM, organizing disk data so few reads are needed, and writing in efficient patterns. Almost every clever trick in a storage engine is, at heart, "how do we touch the slow disk as little as possible?"

A second physical fact matters: **sequential disk access is far faster than random access.** Reading a contiguous chunk of disk (sequential) can be 100×+ faster than jumping around to scattered locations (random) — especially on spinning disks, but meaningfully even on SSDs. So storage engines prize *sequential* writes and reads. (This is the key to why LSM-Trees, below, are so fast at writing.)

### The problem of finding data fast: Indexes

Without help, finding a specific record means a **full table scan** — reading every row. For a billion-row payments table, that's hopeless. An **index** is a separate data structure that lets the database jump straight to the data, like the index at the back of a textbook lets you find a topic without reading every page.

The trade-off of indexes, stated once and true everywhere:

> **An index makes reads faster but makes writes slower and uses extra space.** Every time you insert/update a row, every index on that table must *also* be updated. So indexes are not free — you add them deliberately for the queries that need them, not on everything.

Most indexes are built on one of two structures, which define the two great families of storage engines.

### Storage engine family #1: The B-Tree (read-optimized, the classic default)

A **B-Tree** (specifically B+Tree in most databases) is a balanced tree that keeps data **sorted** and allows lookups, insertions, and range scans in **O(log n)** time. It's the workhorse behind relational databases (MySQL/InnoDB, PostgreSQL) and many others.

How it works, conceptually:
```
                 [ 50 | 100 ]                 <- root: pointers to ranges
                /     |       \
        [10|30]   [60|80]   [120|150]         <- branch nodes
        /  |  \    ...         ...
     leaves hold the actual sorted keys + row pointers
```
- Data is organized into fixed-size **pages** (commonly 4–16 KB), the unit the DB reads from disk.
- To find a key, you start at the root and follow pointers down a few levels to the right leaf — touching only a handful of pages even in a billion-row table (a tree of depth ~4 can index *millions* of rows).
- **Writes update the tree in place**, which may require finding the right page, modifying it, and occasionally **splitting** a full page into two. These are *random* disk writes — the B-Tree's main weakness.
- **Range queries are excellent** ("all payments between two dates") because data is kept sorted — you find the start and scan sequentially.

**B-Tree summary:** great, predictable reads and range scans; writes are slower because they're in-place and random.

### Storage engine family #2: The LSM-Tree (write-optimized, the NoSQL favorite)

The **Log-Structured Merge-Tree** powers write-heavy stores like **Cassandra, RocksDB, LevelDB, HBase**, and the engines behind many time-series and big-data systems. It flips the strategy: optimize for *fast writes* by only ever writing *sequentially*.

How it works:
1. **Writes go to memory first.** A new write lands in an in-memory sorted structure called a **memtable** (and is also appended to a **write-ahead log** on disk for durability — more below). This is blazing fast because it's just a memory operation + a sequential log append.
2. **Memtable flushes to disk as an SSTable.** When the memtable fills up, it's written to disk in one go as an immutable, sorted file called an **SSTable** (Sorted String Table). This write is **purely sequential** — fast.
3. **Files accumulate, then compact.** Over time many SSTables pile up. A background process called **compaction** merges them, discarding deleted/overwritten values and keeping things tidy.
4. **Reads check multiple places.** A read may have to look in the memtable *and* several SSTables (newest first). To avoid slow disk checks, LSM engines use **Bloom filters** (a compact structure that can quickly say "this key is *definitely not* in this file") to skip files that don't contain the key.

**LSM-Tree summary:** outstanding write throughput (everything is sequential), at the cost of slower/more-complex reads (must check several files) and background compaction work (uses CPU and disk I/O).

### B-Tree vs LSM-Tree — the core comparison
| | B-Tree | LSM-Tree |
|---|---|---|
| Write pattern | In-place, random | Append-only, sequential |
| Write speed | Slower | **Faster** |
| Read speed | **Faster, predictable** | Slower (check several files) |
| Range scans | Excellent | Good (sorted SSTables) |
| Used by | PostgreSQL, MySQL/InnoDB | Cassandra, RocksDB, HBase |
| Best for | Read-heavy, transactional | Write-heavy, high-ingest |

### Durability: the Write-Ahead Log (WAL)

Here's a question: if writes go to memory first (true for *both* families to varying degrees), what happens if the server crashes before that data reaches disk? You'd lose it. The answer — used by virtually every serious database — is the **Write-Ahead Log (WAL)**:

> **Before** applying any change, the database first **appends a record of that change to a log file on disk** (a fast *sequential* write). Only after the change is safely in the log does the DB acknowledge "committed." If the server crashes, on restart the DB **replays the WAL** to redo any changes that hadn't yet made it into the main data files.

The WAL is the unsung hero of durability (the **D in ACID**) and is also exactly the **replication log** from Chapter 6 — followers replay the leader's WAL to stay in sync. It's the same mechanism serving both durability and replication.

---

## Why do we need it?

We need purpose-built databases and storage engines because **doing storage correctly is genuinely hard, and getting it wrong is catastrophic** — especially for data that represents money or identity.

1. **Speed at scale.** Without indexes and smart engines, finding data among billions of records is impossibly slow. Storage engineering is what makes "instant" search over huge datasets possible.

2. **Durability — never losing committed data.** The WAL and careful disk management mean that once the system says "saved," it stays saved, even through crashes and power loss. For a payment record, this is non-negotiable.

3. **Safe concurrency.** Thousands of simultaneous users must not corrupt each other's writes. Databases provide transactions and locking to make concurrent access safe (the next two chapters).

4. **Matching the engine to the workload.** Understanding B-Tree vs LSM lets you choose the right tool: a read-heavy transactional ledger wants a B-Tree relational DB; a write-heavy event/metrics firehose wants an LSM-based store. Picking wrong means fighting your database forever.

**When the choice matters most:** the moment you know your **read/write ratio** (recall: estimate it early, Chapter 1). Read-heavy → lean B-Tree/relational. Write-heavy ingestion (logs, metrics, IoT, event streams) → lean LSM. This single number quietly decides a lot of your storage architecture.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** actually needs *both* engine families, for *different* jobs — a great illustration of "match the engine to the workload."

**The core ledger → B-Tree (relational).**
Account balances and the transaction ledger are **read-heavy** (remember ~10:1 reads:writes — constant balance checks) and demand **strong consistency and transactions**. This is the B-Tree's home turf: PostgreSQL/MySQL with B-Tree indexes. We add a B-Tree index on `account_id` so "fetch Alice's balance" is an O(log n) jump, not a scan, and a composite index on `(account_id, timestamp)` so "Alice's transactions last month" is a fast *range scan* over sorted data. The WAL guarantees that once a payment commits, it survives any crash — which is the whole point for money.

**The fraud / events firehose → LSM-Tree.**
Every tap, login, device change, and transaction emits **events** for fraud analysis — a massive **write-heavy** stream, perhaps millions of events per minute, where each individual event is rarely read back. Forcing this through a B-Tree would thrash the disk with random in-place writes. So we route it to an **LSM-based store** (e.g., Cassandra), whose append-only sequential writes swallow the firehose easily. We accept slower individual reads because we rarely look up single events — we mostly batch-process them.

**Why the WAL matters here, concretely.** When Alice's ₹500 payment commits, the database writes it to the WAL on disk *before* confirming success. One second later the server's power fails. On reboot, the database replays the WAL and Alice's payment is intact — never lost. That same WAL stream is shipped to the follower replicas (Chapter 6), keeping them in sync. One mechanism, two critical guarantees: **durability and replication.**

**Indexing discipline.** A junior engineer might index every column "to be safe." But on the high-write ledger, each extra index slows every payment write (every index must update on insert). The staff-engineer move is to index *only* the columns the real queries filter/sort on (`account_id`, `timestamp`) and resist the rest — trading read speed for write speed *deliberately*, guided by the actual query patterns.

In Spring Boot terms: the ledger is a JPA/Hibernate-mapped relational datasource with carefully chosen `@Index` definitions and `@Transactional` writes, while the event firehose is written via a separate Cassandra/Kafka path entirely — physically different storage engines for physically different workloads.

---

## Trade-offs (Pros & Cons)

### Using a real database (vs. plain files / naive storage)
**Pros**
- **Fast retrieval** via indexes, even over billions of records.
- **Durability** via WAL — committed data survives crashes.
- **Safe concurrency** — transactions and locking prevent corruption.
- **Rich querying, validation, and relationships.**

**Cons**
- **Operational complexity** — a database is a system to run, tune, back up, and monitor.
- **Resource cost** — memory, disk, CPU for indexes, caching, compaction.

### B-Tree storage engines
**Pros**
- **Fast, predictable reads** and **excellent range scans**.
- Mature, well-understood, great for transactional/relational workloads.

**Cons**
- **Slower writes** (in-place, random disk I/O).
- Write amplification from page splits under heavy write load.

### LSM-Tree storage engines
**Pros**
- **Exceptional write throughput** (sequential, append-only).
- Great for high-ingest, write-heavy workloads (logs, metrics, events).

**Cons**
- **Slower/more complex reads** (must check memtable + multiple SSTables; mitigated by Bloom filters).
- **Compaction overhead** — background CPU/disk cost, and occasional latency spikes.

### Indexes (universal trade-off)
**Pros**
- Turn slow full scans into fast lookups and range queries.

**Cons**
- **Slow down writes** (every index updates on each write) and **consume extra space** — so add them deliberately.

> **Staff-engineer takeaway:** Every database is, under the hood, a clever scheme to *avoid touching the slow disk* while never losing committed data. Indexes buy read speed at the cost of write speed. **B-Trees are read-optimized** (your transactional ledger's friend); **LSM-Trees are write-optimized** (your event firehose's friend). The **WAL** is what makes "committed" mean *durable*, and it doubles as the replication stream. Match the engine to your read/write ratio, and index only what your real queries need.

---

➡️ Next: [11-Relational-Databases.md](11-Relational-Databases.md) — the most important storage family in fintech: tables, SQL, ACID transactions, and why "boring" relational databases are usually the right default.
