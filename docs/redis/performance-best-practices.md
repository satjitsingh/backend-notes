# Performance & Best Practices

## Why This Page Matters

Knowing Redis commands is not enough. Using Redis *well* in production is what separates a junior developer from a senior one. This page covers the practices that keep Redis fast, reliable, and cost-effective at scale.

---

## Key Naming Conventions

Your Redis key names matter more than you think. With millions of keys, bad naming makes debugging a nightmare.

### The Standard: Use colons as separators

```bash
# Good: clear, consistent, readable
user:42:profile
user:42:session
product:101:details
order:ORD-A1B2:status
cache:api:/products?page=1

# Bad: no structure, inconsistent
user42profile
User_42_Profile
userProfile42
```

### Rules for good key names

| Rule | Good | Bad |
|------|------|-----|
| Use colons `:` as separators | `user:42:name` | `user_42_name` |
| Use lowercase | `order:status` | `Order:Status` |
| Be specific | `cache:product:101` | `p101` |
| Include the entity type | `session:token:abc` | `abc` |
| Don't make keys too long | `u:42:prof` (if millions of keys) | `user:profile:data:information:42` |

!!! tip "The trade-off"
    Longer keys are more readable but use more memory. For most apps, readability wins. If you have 100 million+ keys, shorter names save significant memory.

---

## Slow Commands to Avoid

Some Redis commands are dangerous at scale. They block Redis (remember: single-threaded) and can freeze your entire app.

| Command | Why It's Slow | What to Use Instead |
|---------|---------------|---------------------|
| `KEYS *` or `KEYS user:*` | Scans every key. O(N). Blocks for seconds with millions of keys. | `SCAN` (iterative, non-blocking) |
| `SMEMBERS` on large sets | Returns all members. O(N). A set with 1M members = 1M items over the network. | `SSCAN` for iteration, or `SRANDMEMBER` for a sample |
| `LRANGE` on large lists | Returns a range. O(S+N) where S is start offset. `LRANGE mylist 0 999999` = disaster | Use smaller ranges. Or `LTRIM` to cap list size |
| `SORT` | Sorts in Redis. Can be slow on large datasets. | Sort in application if possible |
| `FLUSHALL` / `FLUSHDB` | Deletes everything. Blocks until done. | Avoid in production. Use with extreme care |
| `SAVE` | Blocks Redis while saving snapshot to disk | Use `BGSAVE` (background) instead |
| `HGETALL` on huge hashes | Returns all fields. O(N). | Use `HSCAN` or `HGET` for specific fields |

!!! warning "The KEYS command"
    `KEYS *` is the #1 cause of Redis outages. Never use it in production. Use `SCAN` instead.

---

## Avoid the KEYS Command in Production

The `KEYS` command scans ALL keys in Redis. If you have 10 million keys, it blocks Redis for seconds. During those seconds, no other command can run (Redis is single-threaded). Your entire app freezes.

```bash
# NEVER do this in production:
KEYS user:*           # Scans ALL keys. Blocks Redis.

# Instead, use SCAN (non-blocking, iterative):
SCAN 0 MATCH user:* COUNT 100
```

`SCAN` returns a small batch of keys and a cursor. You call it repeatedly to iterate through all keys without blocking.

```bash
SCAN 0 MATCH user:* COUNT 100
# Returns: cursor=1750, keys=[user:1, user:2, ...]

SCAN 1750 MATCH user:* COUNT 100
# Returns: cursor=3200, keys=[user:42, user:43, ...]

# Continue until cursor returns 0 (done)
```

!!! warning "Critical Production Rule"
    Never use `KEYS *` in production. It's the single most common cause of Redis outages. Use `SCAN` instead.

---

## Use Pipelines for Batch Operations

Every Redis command requires a network round trip: send command → wait → receive response. If you need to run 100 commands, that's 100 round trips.

**Pipelining** sends multiple commands at once and gets all responses back in one round trip.

```
Without pipeline (100 commands):
  App → Redis: SET key1     → wait → OK
  App → Redis: SET key2     → wait → OK
  ...
  App → Redis: SET key100   → wait → OK
  Total: 100 round trips. ~100ms if each trip takes 1ms.

With pipeline (100 commands):
  App → Redis: SET key1, SET key2, ... SET key100   → wait → OK, OK, ... OK
  Total: 1 round trip. ~1ms.
```

### Java Example

```java
// Without pipeline: 100 round trips
for (int i = 0; i < 100; i++) {
    redis.set("key:" + i, "value:" + i);
}

// With pipeline: 1 round trip
Pipeline pipe = redis.pipelined();
for (int i = 0; i < 100; i++) {
    pipe.set("key:" + i, "value:" + i);
}
pipe.sync();  // Execute all at once
```

**When to use pipelines:**
- Loading bulk data
- Warming cache on startup
- Any scenario where you run 10+ commands that don't depend on each other's results

---

## Use the Right Data Structure

Choosing the wrong data structure wastes memory and makes operations slower.

| Scenario | Wrong Choice | Right Choice | Why |
|----------|-------------|-------------|-----|
| Storing a user profile (name, age, city) | 3 separate String keys | 1 Hash | Hash is more memory-efficient and logically grouped |
| Storing a list of unique tags | List | Set | Set prevents duplicates automatically |
| Ranking 1 million players | Sorted list in app memory | Sorted Set | O(log N) operations vs O(N) sorting |
| Counting unique visitors | Set with all user IDs | HyperLogLog | HyperLogLog uses 12 KB vs potentially GBs for a Set |
| Caching a simple API response | Hash | String | String is simpler for single-value cache |

---

## Connection Pooling

Opening a new connection to Redis for every request is expensive. Use a **connection pool** — a set of pre-opened connections that your app reuses.

### Spring Boot (auto-configured)

```properties
# Lettuce connection pool (Spring Boot default)
spring.data.redis.lettuce.pool.max-active=16
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2
spring.data.redis.lettuce.pool.max-wait=2000ms
```

| Setting | Meaning | Recommended |
|---------|---------|-------------|
| `max-active` | Max connections open at once | 16-32 for most apps |
| `max-idle` | Max idle connections kept alive | 8-16 |
| `min-idle` | Min connections kept alive even when idle | 2-4 |
| `max-wait` | How long to wait if all connections are busy | 2000ms |

!!! tip "Sizing"
    Start with `max-active=16`. Monitor. If you see "Cannot get connection" errors, increase it. If you see idle connections wasting resources, decrease it.

---

## Persistence: RDB vs AOF

Redis is in-memory, but you can configure it to save data to disk for durability.

### RDB (Redis Database Backup)

Takes a **snapshot** of all data at intervals.

```
# redis.conf
save 900 1        # Snapshot if at least 1 key changed in 900 seconds
save 300 10       # Snapshot if at least 10 keys changed in 300 seconds
save 60 10000     # Snapshot if at least 10000 keys changed in 60 seconds
```

**Pros:** Compact files (binary, compressed). Fast restart—just load the file. Good for backups. Minimal CPU overhead during normal operation.  
**Cons:** You lose everything since the last snapshot. If Redis crashes 5 minutes after the last save, 5 minutes of data is gone. And `SAVE` blocks Redis (use `BGSAVE` for background saves).

### AOF (Append-Only File)

Logs **every write command** to a file. On restart, Redis replays the log.

```
# redis.conf
appendonly yes
appendfsync everysec     # Flush to disk every second (default)
# appendfsync always     # Flush after every write (safest, slowest)
# appendfsync no         # Let OS decide (fastest, most data loss risk)
```

**Pros:** Much less data loss. With `everysec`, you lose at most 1 second. With `always`, you lose nothing (but it's slow).  
**Cons:** AOF files grow large over time. Redis supports AOF rewrite (compaction), but it adds complexity. Slightly more disk I/O and CPU.

### RDB vs AOF: Trade-off Summary

| Aspect | RDB | AOF |
|--------|-----|-----|
| Data loss on crash | Minutes (since last snapshot) | Seconds (with everysec) or zero (with always) |
| File size | Smaller (binary snapshot) | Larger (text log of commands) |
| Restart speed | Fast (load one file) | Slower (replay many commands) |
| Write performance | Minimal impact | Some overhead (disk sync) |
| Complexity | Simple | More config options |

### What to use?

| Scenario | Recommendation |
|----------|---------------|
| Pure cache (data can be re-fetched) | No persistence (or RDB only) |
| Session store (some loss is OK) | RDB |
| Important data (minimal loss) | AOF with `everysec` |
| Critical data (zero loss) | AOF with `always` + replication |
| Best of both | RDB + AOF together (Redis 4+) |

---

## Monitoring Redis

### Key Metrics to Watch

| Metric | What It Tells You | Warning Sign |
|--------|------------------|-------------|
| `used_memory` | Current RAM usage | Approaching `maxmemory` |
| `hit_rate` | Cache hit ratio | Below 80% |
| `connected_clients` | Number of active connections | Sudden spike or drop |
| `evicted_keys` | Keys removed due to memory pressure | Growing rapidly |
| `blocked_clients` | Clients waiting on blocking operations | Any number > 0 for extended time |
| `instantaneous_ops_per_sec` | Operations per second | Sudden drop = problem |
| `latency` | Command response time | Above 1ms consistently |

### Useful Commands

```bash
INFO                          # Everything about Redis
INFO memory                   # Memory stats
INFO stats                    # Hit/miss ratio, ops/sec
INFO clients                  # Connected clients
INFO keyspace                 # Number of keys per database

SLOWLOG GET 10                # Last 10 slow commands
LATENCY LATEST                # Latest latency events
CLIENT LIST                   # All connected clients

MONITOR                       # Real-time stream of ALL commands (use briefly!)
```

!!! warning "MONITOR"
    The `MONITOR` command shows every command in real time. Very useful for debugging, but it **reduces Redis performance by ~50%**. Never leave it running in production.

---

## Latency Troubleshooting

Redis is fast. When it's slow, something is wrong. Here's how to find the cause.

### Common causes of high latency

1. **Big keys:** A single key with millions of elements. `SMEMBERS` on a 1M-member set blocks Redis for seconds.
2. **Slow commands:** `KEYS *`, `FLUSHALL`, `SORT` on large data, `HGETALL` on huge hashes.
3. **Persistence:** `appendfsync always` causes every write to wait for disk. Or a `BGSAVE` snapshot can cause latency spikes.
4. **Network:** High latency between app and Redis. Or too many connections causing contention.
5. **Memory pressure:** When Redis is near maxmemory, eviction adds overhead.

### How to diagnose

**Step 1: Check slow commands**

```bash
SLOWLOG GET 10
```

Shows the last 10 slow commands. Each entry shows: command, duration (microseconds), timestamp. If you see `KEYS` or `SMEMBERS` taking 500ms, that's your culprit.

**Step 2: Check latency history**

```bash
LATENCY LATEST
LATENCY HISTORY command 10
```

Shows recent latency spikes. Helps correlate slowness with specific commands or time periods.

**Step 3: Find big keys**

```bash
redis-cli --bigkeys
```

Large keys often cause slow operations. Cap their size or split them.

**Step 4: Check persistence**

```bash
INFO persistence
```

If `rdb_last_save_time` or AOF writes coincide with latency spikes, persistence might be the cause. Consider `appendfsync everysec` instead of `always`, or schedule `BGSAVE` during low-traffic windows.

!!! tip "SLOWLOG configuration"
    Set `slowlog-log-slower-than 10000` (10ms) to log commands slower than 10ms. Adjust based on your latency requirements.

---

## Redis Cluster Basics

When a single Redis instance isn't enough—you need more memory or more throughput—**Redis Cluster** scales out.

### What is Redis Cluster?

Redis Cluster shards your data across multiple Redis nodes. Instead of one server holding everything, you have 6, 12, or more nodes. Data is distributed automatically.

### When do you need it?

- **More memory than one server can hold.** Single Redis instance has a practical limit (e.g., 64–128 GB). Cluster spreads data across nodes.
- **More throughput.** More nodes = more CPU = more commands per second.
- **High availability.** Cluster can survive node failures (with replicas).

### How data is distributed: Hash slots

Redis Cluster divides the key space into **16,384 hash slots**. Every key belongs to exactly one slot. The slot is computed as:

```
slot = CRC16(key) mod 16384
```

Each node is responsible for a subset of slots. When you `SET user:42 "data"`, Redis computes the slot, finds which node owns it, and routes the command there.

### How client routing works

Your Redis client (e.g., Lettuce, Jedis) talks to any cluster node. When you send a command:

1. Client sends to node A.
2. If the key's slot is on node A, node A processes it.
3. If the key's slot is on node B, node A returns **MOVED** with the correct node. Client updates its routing table and retries to node B.
4. For some operations (e.g., during resharding), node A might return **ASK**—temporary redirect. Client sends ASKING and retries.

Smart clients cache the slot-to-node mapping. After the first MOVED, they route directly. No extra round trips.

### MOVED vs ASK (brief intro)

- **MOVED:** The key permanently lives on another node. Update your routing table.
- **ASK:** Temporary. The key is being migrated. Send ASKING, then the command. Don't update the routing table.

!!! info "Cluster complexity"
    Redis Cluster adds operational complexity. You need at least 6 nodes (3 masters + 3 replicas) for a proper setup. Start with a single instance or Sentinel. Move to Cluster when you outgrow it.

---

## Lua Scripting

Sometimes you need to run multiple Redis commands as one atomic operation. **Lua scripting** lets you do that.

### What is it?

Redis can execute Lua scripts. The script runs on the Redis server. While it runs, no other command is processed. So it's atomic—all or nothing.

### Why it's useful

**Problem:** You want to "increment a counter and set TTL only if the key is new." That's two commands. Between them, another client could run. Not atomic.

**Solution:** Put both in a Lua script. Redis runs the script as a single unit.

### Simple example

```lua
-- Increment and set 60-second TTL if key is new
if redis.call('EXISTS', KEYS[1]) == 0 then
    redis.call('SETEX', KEYS[1], 60, 1)
    return 1
else
    return redis.call('INCR', KEYS[1])
end
```

Run it:

```bash
EVAL "if redis.call('EXISTS', KEYS[1]) == 0 then redis.call('SETEX', KEYS[1], 60, 1); return 1 else return redis.call('INCR', KEYS[1]) end" 1 rate_limit:user:42
```

**Real-world use:** Rate limiting (increment + expire atomically), atomic "check-and-set" for distributed locks, multi-step updates that must succeed or fail together.

!!! warning "Script blocking"
    Lua scripts block Redis. Keep them short and fast. A slow script blocks all other clients.

---

## Security Best Practices

Redis is fast and simple. Out of the box, it has **no authentication** and binds to all interfaces. In production, that's dangerous.

### 1. Set a password (requirepass)

```bash
# redis.conf
requirepass your_strong_password_here
```

Clients must send `AUTH your_strong_password_here` before running commands. Without it, Redis rejects requests.

### 2. Use ACL (Redis 6+)

ACL (Access Control List) lets you create users with specific permissions.

```bash
# Create a read-only user for reporting
ACL SETUSER reporter on >secret123 +get +mget +keys +scan

# Create a cache user (no dangerous commands)
ACL SETUSER cacheapp on >secret456 +get +set +del +exists -@dangerous
```

### 3. Rename dangerous commands

```bash
# redis.conf
rename-command FLUSHALL ""
rename-command FLUSHDB ""
rename-command KEYS ""
rename-command CONFIG "config_placeholder_xyz"
```

This disables or obfuscates commands that could take down your Redis. Use empty string `""` to fully disable.

### 4. Bind to localhost (or private IP only)

```bash
# redis.conf
bind 127.0.0.1
# Or for remote: bind 10.0.0.5  (your private IP only)
```

Don't expose Redis to the public internet. If your app and Redis are on the same server, bind to 127.0.0.1. If they're on different servers, use a private network and bind to that IP.

### 5. Use TLS for connections (Redis 6+)

If Redis is on a different network segment, encrypt traffic:

```bash
# redis.conf
tls-port 6379
tls-cert-file /path/to/redis.crt
tls-key-file /path/to/redis.key
tls-ca-cert-file /path/to/ca.crt
```

!!! warning "Default Redis is insecure"
    A Redis instance with no password, bound to 0.0.0.0, is an open door. Attackers scan for such instances and use them for crypto mining or data theft. Always secure Redis in production.

---

## Redis Sentinel for High Availability

In production, you never run a single Redis instance. If it goes down, your app is dead.

**Redis Sentinel** provides automatic failover.

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│ Sentinel │     │ Sentinel │     │ Sentinel │
│    1     │     │    2     │     │    3     │
└────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │                │
     │         Monitoring              │
     ▼                ▼                ▼
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Master  │────▶│ Replica 1│     │ Replica 2│
│ (writes) │     │ (reads)  │     │ (reads)  │
└──────────┘     └──────────┘     └──────────┘
```

- Sentinels **monitor** the master.
- If the master goes down, sentinels **vote** to elect a replica as the new master.
- Your app is notified of the switch. Automatic. No manual intervention.

---

## Production Checklist

Before going to production with Redis, check these:

```
✅ maxmemory is set (don't let Redis eat all RAM)
✅ maxmemory-policy is set (allkeys-lru for caching)
✅ Persistence is configured (RDB, AOF, or both)
✅ Sentinel or Cluster is set up (high availability)
✅ Connection pooling is configured
✅ KEYS command is not used (use SCAN)
✅ Slow commands avoided (no SMEMBERS on huge sets, no LRANGE 0 -1 on big lists)
✅ All cache keys have TTL
✅ Key names follow a consistent convention
✅ Monitoring is in place (INFO, SLOWLOG, LATENCY, alerts)
✅ Backups are configured (RDB snapshots)
✅ Redis version is up to date
✅ Security: requirepass or ACL, bind to private IP, rename dangerous commands
✅ Network security (Redis not exposed to the internet)
```

---

## Quick Summary

| Practice | Why |
|----------|-----|
| Consistent key naming (`entity:id:field`) | Debugging and maintenance |
| Never use `KEYS` in production | Blocks Redis completely |
| Avoid slow commands (SMEMBERS, LRANGE on big data) | Prevents latency spikes |
| Use pipelines for bulk operations | 100x faster than individual commands |
| Choose the right data structure | Memory and performance efficiency |
| Connection pooling | Avoid connection overhead |
| Set `maxmemory` + eviction policy | Prevent OOM crashes |
| Configure persistence (RDB/AOF) | Data durability |
| Use Sentinel for HA, Cluster for scale | High availability and throughput |
| Lua scripts for atomic multi-step ops | Consistency without race conditions |
| Secure Redis (password, ACL, bind) | Prevent unauthorized access |
| Monitor metrics (SLOWLOG, LATENCY) | Catch problems early |

---

## Exercise

1. Run `INFO memory` and note your Redis memory usage.
2. Use `SCAN` to find all keys matching `user:*` (don't use `KEYS`!).
3. Pipeline 50 `SET` commands. Compare the time vs running them individually.
4. Run `SLOWLOG GET 5` and see if any commands are slow.
5. Set `maxmemory 10mb` and fill Redis. Observe eviction behavior.

---

**Next up:** [Redis with Spring Boot](redis-with-springboot.md) — Build real applications with Redis.
