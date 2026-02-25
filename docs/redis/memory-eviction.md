# Memory & Eviction Policies

## The Fundamental Problem

Redis stores everything in RAM. RAM is fast, but it's also **limited and expensive**. A typical server might have 16 GB, 32 GB, or 64 GB of RAM. What happens when Redis tries to store more data than your RAM can hold?

If you don't plan for this, Redis will either crash your server (by consuming all available memory) or start rejecting writes. Neither is good.

This page teaches you how Redis manages memory and what happens when it runs out.

---

## How Much Memory Does Redis Use?

Let's get a feel for the numbers.

### Memory per data type (approximate)

| What you store | Rough memory usage |
|---------------|-------------------|
| 1 simple string key-value (10-byte key, 10-byte value) | ~90 bytes |
| 1 million simple string key-values | ~90 MB |
| 1 Hash with 10 fields | ~200-500 bytes |
| 1 million Hashes with 10 fields each | ~400 MB |
| 1 Sorted Set with 100 members | ~5 KB |

!!! info "The overhead"
    Redis has overhead per key — internal data structures, pointers, metadata. A 10-byte value doesn't take 10 bytes in Redis. It takes about 50-90 bytes total. This is why Redis can fill memory faster than you'd expect.

### Checking memory usage

```bash
INFO memory
```

Key fields in the output:

```
used_memory: 1048576              # Bytes used by Redis (1 MB here)
used_memory_human: 1.00M          # Human-readable
used_memory_peak: 2097152         # Peak memory usage
maxmemory: 0                      # 0 = no limit (dangerous!)
maxmemory_policy: noeviction      # What happens when full
```

To check a specific key's memory:

```bash
MEMORY USAGE user:profile:42
# (integer) 180    ← 180 bytes
```

---

## Setting a Memory Limit

By default, Redis has **no memory limit** (`maxmemory = 0`). It will use as much RAM as available. This is dangerous in production because Redis can consume all your server's RAM, causing the OS to kill it (OOM killer) or making other processes crash.

### Set a limit

In `redis.conf`:

```
maxmemory 2gb
```

Or at runtime:

```bash
CONFIG SET maxmemory 2gb
```

This tells Redis: "You can use up to 2 GB of RAM. Don't go beyond."

### How much should you allocate?

A good rule of thumb:

```
Redis memory = Total server RAM - OS needs - Other processes

Example:
  Server has 16 GB RAM
  OS and other processes need ~4 GB
  Redis gets: 12 GB max
  But keep a buffer: set maxmemory to 10 GB
```

!!! warning "Always set maxmemory in production"
    Running Redis without `maxmemory` in production is like driving without brakes. Everything is fine until it isn't — and then it's a catastrophe.

---

## What Happens When Redis Hits the Memory Limit?

This is where **eviction policies** come in. When Redis can't allocate memory for a new write because `maxmemory` has been reached, it checks the eviction policy to decide what to do.

Think of it like a **bookshelf that's full**. You want to put a new book on it. What do you do?

- **Option 1:** Refuse to add the new book. ("Sorry, shelf is full.")
- **Option 2:** Remove the oldest book. ("This one hasn't been read in years.")
- **Option 3:** Remove the least popular book. ("Nobody reads this one.")
- **Option 4:** Remove a random book. ("Just grab any one.")
- **Option 5:** Remove the book with the soonest "return by" date. ("This one's leaving soon anyway.")

Redis has policies for each of these strategies.

---

## Eviction Policies Explained

### 1. `noeviction` (Default)

**What it does:** When memory is full, Redis returns an error for any write command. Reads still work.

**Analogy:** The bookshelf is full. You say "Sorry, no more books." The library refuses to accept new books but lets people read existing ones.

```
Client: SET new:key "value"
Redis: (error) OOM command not allowed when used memory > 'maxmemory'
```

**When to use:** When you cannot afford to lose ANY data. You'd rather fail loudly than silently delete something important. Good for data stores, bad for caches.

**When NOT to use:** For caching. If your cache is full and can't accept new entries, what's the point? You want old stuff evicted to make room for new stuff.

---

### 2. `allkeys-lru`

**What it does:** When memory is full, Redis evicts the **Least Recently Used** key across ALL keys. The key that hasn't been accessed for the longest time gets deleted.

**Analogy:** The bookshelf is full. You look at all the books and ask: "Which one hasn't been read in the longest time?" You remove that one and put the new book in its place.

```
Keys in Redis:
  user:1   (last accessed 2 seconds ago)
  user:2   (last accessed 5 minutes ago)
  user:3   (last accessed 3 hours ago)     ← This one gets evicted
  user:4   (last accessed 10 seconds ago)
```

**When to use:** This is the **best choice for caching**. Most popular data stays. Rarely accessed data gets evicted. This is what the majority of Redis caching deployments use.

!!! tip "Interview Insight"
    If an interviewer asks "What eviction policy would you use for Redis caching?", answer: "I'd use `allkeys-lru`. It evicts the least recently used key, which naturally keeps the most popular data in cache. It's the standard choice for caching workloads."

---

### 3. `allkeys-lfu` (Least Frequently Used)

**What it does:** Evicts the key that has been accessed the **fewest total times**. Not the oldest access, but the rarest access.

**Analogy:** The bookshelf is full. You look at how many times each book has been checked out in total. The book with the fewest checkouts gets removed.

**LRU vs LFU — what's the difference?**

```
Scenario: Key A was accessed 1000 times but the last access was 1 hour ago.
          Key B was accessed 2 times but the last access was 1 second ago.

LRU evicts: Key A (because it was accessed less recently)
LFU evicts: Key B (because it was accessed less frequently)
```

**Concrete scenario:** Imagine a news site. The homepage is accessed 10,000 times per hour. At 3 AM, traffic drops. Nobody visits for 30 minutes. With LRU, the homepage cache might get evicted (it wasn't accessed recently). When the morning rush hits at 8 AM, the first 1000 users get a cache miss—slow! With LFU, the homepage stays (it was accessed 10,000 times—high frequency). LFU protects your "hot" data even during quiet periods.

**When to use:** When some keys are consistently popular and you want to protect them. LFU is better than LRU when you have "hot" data that's always needed. LRU might accidentally evict a popular key during a brief quiet period.

---

### 4. `volatile-lru`

**What it does:** Same as `allkeys-lru`, but ONLY considers keys that have a TTL (expiry) set. Keys without TTL are never evicted.

**Analogy:** The bookshelf is full. You only consider removing books that have a "return by" sticker. Books without stickers are permanent residents — they never get removed.

**When to use:** When you have a mix of permanent data and temporary/cache data in the same Redis instance. Permanent data (no TTL) stays safe. Cache data (with TTL) can be evicted if memory runs low.

---

### 5. `volatile-lfu`

Same as `volatile-lru`, but uses frequency instead of recency. Only evicts keys with TTL set, and chooses the one accessed least frequently.

---

### 6. `volatile-ttl`

**What it does:** Evicts keys with the **shortest remaining TTL** first. The key closest to expiring gets deleted early.

**Analogy:** The bookshelf is full. You look at the "return by" dates and remove the book that's due back soonest. "You were leaving in 5 minutes anyway."

**When to use:** When you want to prioritize keeping data that was meant to last longer. Short-TTL data is considered less important.

---

### 7. `allkeys-random`

**What it does:** Picks a random key and deletes it. No logic. No preference.

**When to use:** When all keys are equally important (or unimportant). Rarely used in practice.

---

### 8. `volatile-random`

Same as `allkeys-random`, but only considers keys with TTL set.

---

## How to Choose Your Eviction Policy: Decision Flowchart

Not sure which policy to use? Follow this logic:

```
Do you need to NEVER lose any data?
  YES → noeviction (and ensure you have enough RAM!)

Is Redis used purely as a cache (data can be re-fetched)?
  YES → allkeys-lru (most common) or allkeys-lfu (if you have "hot" data)

Do you have BOTH permanent data AND cache data in the same Redis?
  YES → volatile-lru or volatile-lfu (only evict keys with TTL)

Are all keys equally important (or unimportant)?
  YES → allkeys-random (rare)

Do you want to evict short-TTL data first?
  YES → volatile-ttl
```

**Quick rule of thumb:** For 90% of caching workloads, use `allkeys-lru`. It's the industry standard. Only deviate if you have a specific reason.

---

## Policy Comparison Table

| Policy | Considers | Eviction Logic | Best For |
|--------|-----------|---------------|----------|
| `noeviction` | None | Rejects writes | Data that must not be lost |
| `allkeys-lru` | All keys | Least recently used | **Caching (most common)** |
| `allkeys-lfu` | All keys | Least frequently used | Caching with hot data |
| `volatile-lru` | Keys with TTL | Least recently used | Mixed data + cache |
| `volatile-lfu` | Keys with TTL | Least frequently used | Mixed data, protect hot keys |
| `volatile-ttl` | Keys with TTL | Shortest TTL first | Prioritize long-lived data |
| `allkeys-random` | All keys | Random | All keys equally important |
| `volatile-random` | Keys with TTL | Random | Rarely used |

---

## Setting the Eviction Policy

In `redis.conf`:

```
maxmemory-policy allkeys-lru
```

At runtime:

```bash
CONFIG SET maxmemory-policy allkeys-lru
```

Check the current policy:

```bash
CONFIG GET maxmemory-policy
# 1) "maxmemory-policy"
# 2) "allkeys-lru"
```

---

## How LRU Actually Works in Redis

Here's something that surprises people: Redis does **not** implement a true LRU. A true LRU would require tracking access time for every key, which uses a lot of memory. Instead, Redis uses an **approximated LRU**.

### How it works:

1. When Redis needs to evict, it picks **N random keys** (default: 5).
2. Among those N keys, it evicts the one with the oldest access time.
3. That's it. Not the globally oldest key — just the oldest among a random sample.

### Why approximation?

True LRU requires maintaining a linked list of all keys sorted by access time. For millions of keys, this is expensive (in memory and CPU). The approximation is close enough to real LRU for most use cases and uses almost no extra memory.

### Tuning the sample size:

```
maxmemory-samples 10
```

Higher = more accurate (closer to true LRU) but slightly slower. Default of 5 is good for most workloads.

!!! tip "Interview Insight"
    "Redis doesn't implement exact LRU — it uses an approximated LRU that samples a configurable number of keys and evicts the least recently used among the sample. This keeps memory overhead low while being accurate enough for caching."

---

## Memory Optimization Tips

### 1. Use the right data structure

Hashes with small fields are more memory-efficient than many individual string keys.

```bash
# Wasteful: 3 keys
SET user:42:name "Satjit"
SET user:42:age "25"
SET user:42:city "Delhi"
# = ~270 bytes (3 keys × 90 bytes overhead each)

# Better: 1 Hash
HSET user:42 name "Satjit" age "25" city "Delhi"
# = ~200 bytes (1 key overhead + compact field storage)
```

### 2. Use short key names in high-volume scenarios

```bash
# Readable but long:
SET user:profile:session:token:abc123 "data"

# Shorter (saves bytes × millions of keys):
SET u:s:abc123 "data"
```

In most cases, readability matters more. But if you have 100 million keys, those extra bytes add up.

### 3. Set TTL on everything that's temporary

Data without TTL stays forever. If you're using Redis as a cache, every key should have a TTL. Otherwise, your cache fills up and never makes room for new data.

### 4. Use compression for large values

If you're storing large JSON blobs, compress them before storing:

```java
byte[] compressed = compress(jsonString);
redis.set("data:large", compressed, Duration.ofMinutes(30));
```

### 5. Monitor regularly

```bash
INFO memory              # Overall memory stats
DBSIZE                   # Number of keys
MEMORY USAGE <key>       # Size of a specific key
MEMORY DOCTOR            # Redis memory health check
```

---

## Memory Fragmentation

Sometimes Redis uses more RAM than the actual data size. That's **memory fragmentation**.

### What is it?

When you add and delete keys over time, Redis allocates and frees memory. The memory allocator (jemalloc or libc) can leave "gaps"—small chunks of free memory that aren't big enough to reuse. Redis thinks it's using 4 GB, but only 3 GB of that is actual data. The rest is wasted.

**Analogy:** Imagine a parking lot. Cars leave and new cars arrive. Over time, you might have small gaps—one car space here, two there—but no space for a bus. The lot is "fragmented." You have free space, but it's not usable for large allocations.

### How to check

```bash
INFO memory
```

Look at these fields:

```
used_memory: 1048576000              # What Redis thinks it's using
used_memory_rss: 1572864000          # What the OS reports (actual RAM)
mem_fragmentation_ratio: 1.5         # used_memory_rss / used_memory
```

**mem_fragmentation_ratio** tells the story:

- **1.0** = Perfect. No fragmentation.
- **1.0–1.5** = Normal. Slight fragmentation acceptable.
- **> 1.5** = High fragmentation. Redis is using more RAM than needed.
- **< 1.0** = Redis is using swap (disk). Very bad for performance.

### What to do about it

1. **Restart Redis** (careful in production). A fresh start clears fragmentation.
2. **Set `activedefrag yes`** (Redis 4+). Redis will periodically defragment in the background.
3. **Avoid frequent small key churn.** If you're constantly creating and deleting tiny keys, fragmentation grows. Use Hashes to group small data instead.

!!! warning "Fragmentation ratio > 2"
    If you see mem_fragmentation_ratio above 2, Redis is wasting a lot of RAM. Consider restarting during a low-traffic window or enabling activedefrag.

---

## Redis Memory Optimization Techniques

Beyond the basics, here are more ways to squeeze more data into less RAM.

### Ziplist encoding

Redis internally uses "ziplists" for small data structures. A ziplist is a compact, serialized format. Less overhead than full data structures.

For **Hashes**, Redis uses ziplist when:
- The hash has few fields (default: ≤ 512)
- Each field value is small (default: ≤ 64 bytes)

```bash
# redis.conf
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
```

If you have many small hashes (e.g., user profiles with 5–10 fields), they'll use ziplist and save memory. If you increase these limits, Redis might switch to a full hash table—more memory per key.

### Intset encoding

For **Sets** with only integers, Redis uses a special "intset" format. Much smaller than a hash table.

```bash
# redis.conf
set-max-intset-entries 512
```

If your set has ≤ 512 integers, Redis uses intset. Above that, it switches to a hash table. For "user IDs who liked this post" (integer IDs), this saves a lot.

### List and Sorted Set ziplist

Similar for Lists and Sorted Sets:

```bash
list-max-ziplist-entries 512
list-max-ziplist-value 64
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
```

### Shared vs dedicated Redis instances

**Option A:** One Redis instance for cache, sessions, queues, and more.  
**Option B:** Separate Redis instances per use case.

**Shared:** Simpler. Fewer connections. But one noisy app can evict another app's cache. And you can't tune memory per workload.

**Dedicated:** More control. Cache gets its own eviction policy. Sessions get their own. But more connections, more instances to manage.

!!! tip "When to split"
    If you have distinct workloads (e.g., cache vs session vs real-time), consider separate instances. If you're small, one instance is fine.

---

## Real Scenario: Debugging Memory Issues

Your Redis is using 80% of maxmemory. Alerts are firing. How do you debug?

### Step 1: Check overall stats

```bash
INFO memory
DBSIZE
```

Note `used_memory`, `maxmemory`, `mem_fragmentation_ratio`, and total key count.

### Step 2: Find the biggest keys

```bash
redis-cli --bigkeys
```

This scans the database and reports the largest keys by type. Example output:

```
-------- summary -------
Sampled 100000 keys in the keyspace!
Total key length in bytes is 1000000 (avg len 10.00)

Biggest string found 'cache:product:12345' has 524288 bytes
Biggest hash found 'user:42' has 50 fields
Biggest list found 'task_queue' has 10000 items
Biggest set found 'online_users' has 5000 members
Biggest zset found 'leaderboard' has 100000 members
```

Now you know what's eating memory. Maybe one cache key is huge. Maybe a list is unbounded.

### Step 3: Check fragmentation

```bash
INFO memory
```

If `mem_fragmentation_ratio` > 1.5, fragmentation is part of the problem. Consider `activedefrag yes` or a restart.

### Step 4: Fix the culprits

- **Big string:** Maybe you're caching a huge JSON. Compress it or split it.
- **Big list:** Add `LTRIM` to cap size. Or use a TTL.
- **Big set:** Consider HyperLogLog if you only need approximate count.
- **Big hash:** Check if you're storing too many fields. Consider splitting.

### Step 5: Verify

After fixes, run `INFO memory` and `--bigkeys` again. Memory should drop.

!!! info "MEMORY DOCTOR"
    Run `MEMORY DOCTOR` for Redis's built-in health check. It suggests fixes based on your current state.

---

## Real-World Scenario: E-Commerce Cache

You're running an e-commerce site. Redis is your cache. Here's how you'd configure it:

```
maxmemory 4gb
maxmemory-policy allkeys-lru
```

**What happens during a normal day:**

- 50,000 product pages are cached. Redis uses 2 GB. Plenty of room.
- Popular products (top 500) are accessed thousands of times. They stay in cache.
- Unpopular products (long tail) are accessed rarely. They naturally sit at the bottom of the LRU list.

**What happens during a flash sale:**

- 200,000 products get cached as traffic spikes. Redis fills up to 4 GB.
- `allkeys-lru` kicks in. It evicts old, rarely-accessed product pages.
- The flash sale products (currently popular) stay in cache.
- After the sale, traffic normalizes. New data fills the cache. Old sale data gets evicted.

The system self-regulates. No manual intervention needed.

---

## Quick Summary

| Concept | Explanation |
|---------|-------------|
| `maxmemory` | Maximum RAM Redis can use |
| Eviction | Deleting old data to make room for new data |
| `noeviction` | Reject writes when full (default) |
| `allkeys-lru` | Evict least recently used. **Best for caching** |
| `allkeys-lfu` | Evict least frequently used |
| `volatile-*` | Only evicts keys that have TTL set |
| Approximate LRU | Redis samples N random keys, evicts oldest among them |
| `maxmemory-samples` | How many keys to sample (default: 5) |
| Best practice | Set `maxmemory` + `allkeys-lru` for caching workloads |

---

## Exercise

1. Set `maxmemory` to `10mb` on your local Redis. Fill it with data. What error do you get?
2. Change policy to `allkeys-lru`. Fill Redis again. What happens now?
3. Store 5 keys. Access only 2 of them repeatedly. Fill Redis to the limit. Which keys survive?
4. Check `INFO memory` and note `used_memory` vs `maxmemory`.

---

**Next up:** [Caching Explained](caching-explained.md) — The #1 reason Redis exists in most backends.
