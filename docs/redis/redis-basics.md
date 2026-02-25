# Redis Basics

## What is Redis?

**Redis** stands for **Re**mote **Di**ctionary **S**erver. But that technical name doesn't tell you much. Let me explain what Redis actually is in plain English.

Redis is a database that lives entirely in your computer's RAM (random access memory). Unlike MySQL or PostgreSQL, which store data on your hard drive or SSD, Redis keeps everything in memory. This makes it blazingly fast—we're talking microseconds instead of milliseconds. Think of it as the difference between grabbing a book from your desk versus walking to the library across town.

Redis is a **key-value store**. That means every piece of data has a name (the key) and a value. It's like a giant dictionary or a phone book: you look up "user:42" and you get back whatever data is stored there. The key is always a string. The value can be a string, a number, a list, or several other data types we'll explore later.

!!! info "One-liner"
    Redis is an **in-memory key-value store** used for caching, sessions, queues, and real-time data.

Redis is not meant to replace your main database. It works *alongside* your database. You use Redis when you need speed—caching frequently accessed data, storing user sessions, counting things in real-time, or building features like leaderboards. Your main database (MySQL, PostgreSQL) remains the source of truth for permanent storage.

---

## The Library Analogy: A Full Story

Let me walk you through a story that will make Redis click. Imagine you work at a busy university library.

**Meet the characters:** You're Priya, the librarian at the front desk. Students constantly ask for books. The books are stored on shelves in the back—that's your **database**. Walking to the shelf, finding the book, and coming back takes about 2 minutes per trip.

**The problem:** A student named Rahul asks for "Introduction to Algorithms" at 9:00 AM. You walk to the shelf, find it, bring it back. At 9:15 AM, *another* student asks for the same book. Do you walk to the shelf again? That would be wasteful! The book location hasn't changed.

**The solution:** You keep a **small notebook** on your desk. When you fetch a book, you write down: "Introduction to Algorithms → Shelf 12, Row 3." Next time someone asks for it, you check your notebook first. If it's there, you send a helper to get it—or you already know exactly where to go. No wandering. That notebook is **Redis**.

**The flow:**

```
Without Redis (every request):
  Student ──▶ Priya ──▶ Walk to shelf (2 min) ──▶ Return book

With Redis (repeat requests):
  Student ──▶ Priya ──▶ Check notebook (2 seconds) ──▶ Send helper with exact location
```

The notebook doesn't replace the shelves. The books still live on the shelves. The notebook just saves you from walking when you already know the answer. That's exactly what Redis does for your application and database.

---

## How Does Your Computer's Memory Work?

Before we go further, let's understand why "in-memory" matters so much. Your computer has two main places to store data: **RAM** and **Disk**.

**RAM (Random Access Memory)** is like your desk. It's where your computer keeps things it's actively working with. When you open a document, it gets loaded into RAM. When you switch between browser tabs, that data is in RAM. RAM is *fast*—your CPU can read from it in nanoseconds. But RAM is *volatile*: when you turn off your computer, everything in RAM disappears. And RAM is expensive. A typical laptop might have 8–16 GB of RAM.

**Disk (HDD or SSD)** is like a filing cabinet. It's where data lives permanently. Your photos, your documents, your database—they're all on disk. Disk is *slow* compared to RAM. Reading from an SSD might take 0.1–1 millisecond. Reading from a slow HDD can take 10+ milliseconds. But disk is *persistent*: turn off the computer, and your data is still there. And disk is cheap—you can have terabytes of storage.

**Why this matters for Redis:** Redis keeps data in RAM. So when your app asks Redis for a value, Redis doesn't have to wait for a disk spin or an SSD read. It just looks in memory. That's why Redis can respond in **microseconds** (0.001 milliseconds) while a database query might take **milliseconds** (1–50 ms or more).

---

## Why Redis Being In-Memory Matters: Real Numbers

Let's put some numbers on this. These are approximate, but they illustrate the gap:

| Operation | Typical Time | Human Analogy |
|------------|--------------|---------------|
| Read from Redis (RAM) | ~0.1 ms | Checking your pocket for your keys |
| Read from SSD (Database) | ~1–10 ms | Walking to the next room |
| Read from HDD | ~10–50 ms | Walking to the garage |
| Network call to another server | ~50–200 ms | Driving to the next city |
| External API call | ~200–1000 ms | Flying to another country |

Redis is often **100x to 1000x faster** than a database query. When you're serving thousands of requests per second, that difference is the difference between a snappy app and a sluggish one.

!!! tip "Think about it this way"
    If your database takes 10 ms per query and you get 1000 requests per second for the same data, that's 10 seconds of database time per second—your database would be overwhelmed. With Redis, those 1000 requests might take 0.1 seconds total. The database is only queried once; the rest are served from Redis.

---

## Where is Redis Used in Real Companies?

Redis isn't a toy. It powers some of the biggest apps you use every day.

**Instagram** uses Redis to store user sessions and feed data. When you scroll through your feed, a lot of that data comes from Redis—pre-computed and ready to serve. They also use Redis for real-time features like "who's online" and activity counters.

**Twitter (X)** uses Redis to cache timelines. When you load your feed, the system checks Redis first. If your timeline is cached (and it often is), you get it instantly. Redis also powers their real-time engagement counters—likes, retweets, and reply counts.

**GitHub** uses Redis for job queues (background tasks like sending emails, updating caches) and for caching repository metadata. When you view a repo's star count or fork count, that might be served from Redis.

**Stack Overflow** uses Redis for caching question and answer data. Their read-heavy workload benefits enormously from Redis—most questions are read many more times than they're written.

**Swiggy, Zomato, Uber** use Redis for session storage (so you stay logged in across requests), real-time order tracking, and caching restaurant menus and locations.

**Shopify** uses Redis for session storage, caching product catalogs, and powering their job queue system. When millions of merchants load their dashboards, Redis serves cached data so the database isn't hammered. Their real-time inventory updates and order processing rely on Redis for speed.

**Pinterest** uses Redis for caching pins, boards, and user feeds. When you scroll through your home feed, much of that content comes from Redis. They also use Redis for rate limiting API requests and storing real-time analytics data.

The pattern is the same everywhere: **frequently accessed data** goes in Redis. **Permanent, authoritative data** stays in the main database.

---

## What Redis is NOT

Before you get carried away, it's important to know what Redis is *not*. This saves you from making costly mistakes.

**Redis is NOT a replacement for your database.** Your main database (MySQL, PostgreSQL) is the source of truth. Redis is a helper. If Redis crashes and loses data, you should be able to rebuild from your database. Never store data in Redis that doesn't exist somewhere else (unless it's truly ephemeral, like sessions or OTPs).

**Redis is NOT for complex queries.** You can't run SQL-like joins, aggregations, or full-text search. Redis is key-value. You look up by key. If you need "find all users who bought product X in the last 30 days," that's a database job, not Redis.

**Redis is NOT for large binary data.** Storing images, videos, or huge JSON blobs in Redis wastes expensive RAM. Use object storage (S3, etc.) for large files. Redis is for small, frequently accessed data.

**Redis is NOT for data that must never be lost.** Redis can persist to disk, but it's optimized for speed, not durability. For critical financial transactions or audit logs, use a proper database with ACID guarantees.

!!! warning "Common mistake"
    Beginners often try to use Redis as their primary database. Don't. Redis is a cache and a speed layer. Your database is the foundation.

---

## Why Use Redis?

| Problem | How Redis Helps |
|---------|----------------|
| Database is slow | Redis serves data from memory in microseconds |
| Same query runs 1000 times | Cache the result. Query database only once |
| User sessions | Store session data in Redis, shared across servers |
| Rate limiting | Count API calls per user using Redis counters |
| Real-time leaderboard | Use Redis Sorted Sets |
| Message queue | Use Redis Lists or Streams |

---

## Key Features

- **In-Memory**: Data lives in RAM. Super fast.
- **Key-Value Store**: Every piece of data has a name (key) and value.
- **Persistence**: Can save data to disk (so it survives restarts).
- **TTL (Time-To-Live)**: Data can auto-expire. Set it and forget it.
- **Single-Threaded**: One command at a time. No race conditions.
- **Rich Data Structures**: Not just strings. Lists, Sets, Hashes, and more.

---

## Installing Redis

### Option 1: Docker (Recommended)

```bash
docker run --name my-redis -p 6379:6379 -d redis
```

This pulls the official Redis image and runs it in the background. Port 6379 is Redis's default port.

### Option 2: Windows (WSL)

```bash
sudo apt update
sudo apt install redis-server
sudo service redis-server start
```

### Option 3: Mac

```bash
brew install redis
brew services start redis
```

### Verify it works

```bash
redis-cli ping
# Output: PONG
```

If you see `PONG`, Redis is running! `PING` is Redis's way of saying "I'm here and ready."

---

## Basic Redis Commands

Open the Redis CLI:

```bash
redis-cli
```

You'll see a prompt like `127.0.0.1:6379>`. You're now talking directly to Redis.

---

### SET and GET - Store and Retrieve Data

```bash
SET name "Satjit"
# OK

GET name
# "Satjit"
```

**What happened?** You stored the value `"Satjit"` under the key `name`. Redis acknowledged with `OK`. When you ran `GET name`, Redis looked up the key and returned the value.

**Think of it like a dictionary:** `name` is the key, `"Satjit"` is the value. You can store strings, numbers (as strings), or even JSON—Redis doesn't care. It stores whatever you give it.

**Real-world use case:** Storing a user's display name. When someone logs in, you might do `SET user:42:name "Satjit"`. Later, when rendering a comment, you `GET user:42:name` instead of hitting the database.

---

### DEL - Delete a Key

```bash
DEL name
# (integer) 1

GET name
# (nil)
```

**What happened?** `DEL` removes the key from Redis. The `1` means one key was deleted. After that, `GET name` returns `(nil)`—Redis's way of saying "that key doesn't exist."

**Real-world use case:** When a user logs out, you `DEL session:abc123` to invalidate their session. When a product is updated, you might `DEL product:101` so the next request fetches fresh data from the database.

---

### EXISTS - Check if Key Exists

```bash
EXISTS name
# (integer) 0   (0 = no, 1 = yes)
```

**What happened?** We deleted `name` earlier, so `EXISTS` returns `0`. If the key existed, it would return `1`.

**Real-world use case:** Before doing expensive work, you might check `EXISTS cache:expensive-report`. If it's 1, you serve from cache. If it's 0, you generate the report and cache it.

---

### EXPIRE and TTL - Set Auto-Delete Timer

```bash
SET session "abc123"
EXPIRE session 60          # Expires in 60 seconds

TTL session                # Time remaining
# (integer) 57
```

**What happened?** You set a key and then told Redis to delete it automatically after 60 seconds. `TTL` (Time To Live) shows how many seconds are left. After 60 seconds, the key vanishes—no `DEL` needed.

**Real-world use case:** User sessions. You don't want sessions to live forever. `EXPIRE session:xyz 1800` means the session expires after 30 minutes of inactivity. OTPs: `EXPIRE otp:9876543210 300`—the OTP is valid for 5 minutes only.

---

### SETEX - Set + Expire in One Command

```bash
SETEX otp 300 "784512"     # Key "otp", expires in 300 seconds, value "784512"
```

**What happened?** `SETEX` combines `SET` and `EXPIRE` in one step. You set the value and the TTL atomically. No chance of forgetting to set the expiry.

**Real-world use case:** OTP storage. When you send an OTP, you do `SETEX otp:phone_number 300 "123456"`. The OTP is stored and will auto-delete in 5 minutes. One command, done.

---

### MSET and MGET - Set and Get Multiple Keys at Once

```bash
MSET user:1:name "Satjit" user:1:age "25" user:2:name "Rahul" user:2:age "30"

MGET user:1:name user:1:age user:2:name
# 1) "Satjit"
# 2) "25"
# 3) "Rahul"
```

**What happened?** `MSET` sets multiple key-value pairs in one command. `MGET` retrieves multiple keys in one round trip. Instead of 4 separate GET calls, you do 1 MGET.

**Real-world use case:** Loading a user's profile. You need name, email, and avatar URL. Instead of 3 round trips, you do `MGET user:42:name user:42:email user:42:avatar`. One network call. Much faster when you're loading several related values.

---

### APPEND and STRLEN - Append to a String, Get Length

```bash
SET log "Error: "
APPEND log "Connection failed"
GET log
# "Error: Connection failed"

STRLEN log
# (integer) 23
```

**What happened?** `APPEND` adds text to the end of an existing string. If the key doesn't exist, Redis creates it. `STRLEN` returns the length of the string in bytes.

**Real-world use case:** Building a simple log buffer. Each error appends to the same key. Or storing a growing JSON array as a string (though for complex cases, a List might be better).

---

### INCR, DECR, INCRBY - Atomic Counters

```bash
SET page_views 0
INCR page_views          # 1
INCR page_views          # 2
INCRBY page_views 10     # 12
DECR page_views          # 11
```

**What happened?** `INCR` adds 1. `DECR` subtracts 1. `INCRBY key N` adds N. If the key doesn't exist, Redis treats it as 0 first. These are **atomic**—safe even with thousands of concurrent requests.

**Real-world use case:** Rate limiting. Track API calls per user: `INCR api:calls:user:42` then `EXPIRE api:calls:user:42 60`. Or inventory: `DECR inventory:product:101` when someone buys. Or like count: `INCRBY post:42:likes 1`.

!!! tip "Why atomic matters"
    If 1000 users click "like" at the same time, 1000 INCR commands run. Without atomicity, you might get 50 instead of 1000. Redis handles it correctly.

---

## Key Naming Best Practices

How you name your keys matters. With thousands or millions of keys, good naming makes debugging easy and prevents collisions.

### The colon convention

Use colons `:` to create a hierarchy. Think of it like a file path: `entity:id:field`.

```bash
# Good: clear structure
user:42:profile
user:42:session
product:101:details
order:ORD-A1B2:status
cache:api:/products?page=1

# Bad: no structure, hard to debug
user42profile
User_42_Profile
userProfile42
```

### Why this works

- **Namespacing:** `user:42:name` and `product:42:name` don't collide. Different entities, same ID.
- **Scanning:** You can use `SCAN 0 MATCH user:*` to find all user keys. Or `user:42:*` for one user's data.
- **Readability:** Anyone can understand `session:abc123` or `otp:9876543210` at a glance.

### Examples by use case

| Use Case | Key Pattern | Example |
|----------|-------------|---------|
| User data | `user:{id}:{field}` | `user:42:name`, `user:42:email` |
| Session | `session:{token}` | `session:abc123xyz` |
| Cache | `cache:{resource}` | `cache:product:101`, `cache:api:/users` |
| OTP | `otp:{identifier}` | `otp:9876543210` |
| Rate limit | `ratelimit:{user}:{window}` | `ratelimit:user:42:60` |
| Lock | `lock:{resource}` | `lock:order:101` |

!!! info "Consistency is key"
    Pick a convention and stick to it. Mixing `user_42` and `user:42` in the same app makes debugging confusing.

---

## How Redis Processes Commands (Single-Threaded Model)

Redis is **single-threaded**. That means it processes one command at a time. No parallel execution. Why would anyone design a database that way? And how does it still handle millions of requests per second?

### The event loop

Think of Redis as a single worker at a counter. One customer at a time. When a customer (client) sends a command, Redis processes it. When done, it moves to the next. No multitasking. No threads.

```
Client 1: SET key1 "value"   → Redis processes → OK
Client 2: GET key2           → Redis processes → "value"
Client 3: INCR counter      → Redis processes → 42
```

Commands are processed in order. First come, first served.

### Why single-threaded is actually good

**No locks.** In a multi-threaded system, you need locks to prevent two threads from modifying the same data at once. Locks are expensive. They cause contention. Redis avoids all of that. One thread = no locks = no lock overhead.

**No race conditions.** Two clients can't corrupt data by updating the same key at the same time. Redis handles one command at a time. Atomic by design.

**Simpler code.** The Redis codebase doesn't need to worry about thread safety. Fewer bugs. Easier to reason about.

**CPU is rarely the bottleneck.** Redis is I/O bound. It waits for network data. It waits for disk (if persistence is on). The CPU is rarely maxed out. So one thread is often enough.

### I/O multiplexing in simple terms

Here's the magic: Redis doesn't block while waiting for clients. It uses **I/O multiplexing** (epoll on Linux, kqueue on Mac). Think of it as a receptionist who takes messages from many people at once.

When 1000 clients are connected, Redis doesn't process them one by one in a slow way. It:

1. Listens to all sockets at once (multiplexing).
2. When a client sends a command, Redis reads it.
3. Redis processes the command (fast—microseconds).
4. Redis sends the response.
5. Redis immediately moves to the next client that has data ready.

The "waiting" part is handled by the OS. Redis doesn't sit idle. It's always ready to accept the next command. So even with one thread, it can handle tens of thousands of connections and thousands of commands per second.

!!! tip "Interview insight"
    "Redis is single-threaded but uses I/O multiplexing. It processes one command at a time, which eliminates locks and race conditions. The OS handles waiting for network I/O, so Redis stays responsive even with many connections."

---

## Redis Persistence: RDB vs AOF (Introduction)

Redis keeps data in RAM. When Redis restarts, RAM is empty. So how do you keep data across restarts? Redis offers two persistence mechanisms. We'll cover them in depth on a dedicated page, but here's the teaser.

### RDB (Redis Database Backup)

Redis takes a **snapshot** of all data at configured intervals. It writes a single file (e.g., `dump.rdb`) to disk. On restart, Redis loads that file.

**Pros:** Compact. Fast to load. Good for backups.  
**Cons:** You lose everything since the last snapshot. If Redis crashes 5 minutes after the last save, 5 minutes of data is gone.

**When to use:** Caching. Some session data. When losing a few minutes of data is acceptable.

### AOF (Append Only File)

Redis logs **every write command** to a file. On restart, it replays those commands to rebuild the dataset.

**Pros:** Much less data loss. You can sync every second (or even every write) for near-zero loss.  
**Cons:** Files get large. Recovery can be slower. Slightly more overhead on writes.

**When to use:** When data matters. Sessions you can't afford to lose. Or combine RDB + AOF for the best of both.

!!! info "For beginners"
    If you're using Redis for caching, you often don't need persistence. The cache can be rebuilt from the database. If you're storing sessions or other critical data, enable AOF or RDB. See the dedicated persistence page for full details.

---

## How Redis Stores Data

```
┌─────────────────────────────────┐
│           Redis (RAM)           │
│                                 │
│   Key          │   Value        │
│   ─────────────┼───────────     │
│   "user:1"     │   "Satjit"    │
│   "user:2"     │   "Rahul"     │
│   "otp:9876"   │   "482910"    │
│   "session:x"  │   "{...json}" │
│                                 │
└─────────────────────────────────┘
```

Everything is a key-value pair. Keys are strings. Values can be different types (strings, lists, sets, etc.—covered in the next page).

---

## What Happens When Redis Restarts?

By default, Redis keeps data only in memory. When you restart Redis (or your server crashes), **all data is lost**. That's fine for caching—you can repopulate the cache from the database. But for sessions or other important data, you might want persistence.

Redis offers two persistence options:

**RDB (Redis Database):** Redis periodically takes a "snapshot" of all data and writes it to a file on disk. If Redis restarts, it loads from that file. You might lose data from the last few minutes (whatever changed since the last snapshot).

**AOF (Append Only File):** Redis logs every write command to a file. On restart, it replays those commands to reconstruct the state. You lose less data, but the file can get large and recovery can be slower.

!!! info "For beginners"
    If you're using Redis for caching, you often don't need persistence. The cache can be rebuilt. If you're storing sessions or other critical data, enable AOF or RDB. We'll cover this in more detail in the interview questions section.

---

## Redis vs Traditional Database

| Feature | Redis | MySQL/PostgreSQL |
|---------|-------|-----------------|
| Storage | RAM (Memory) | Disk |
| Speed | Microseconds | Milliseconds |
| Data Model | Key-Value | Tables with rows |
| Best For | Caching, sessions | Long-term storage |
| Data Size | Limited by RAM | Limited by disk |
| Persistence | Optional | Always |

!!! warning "Important"
    Redis is **not a replacement** for your database. It **works alongside** your database to make things faster.

---

## What's Next in the Redis Section?

Now that you understand what Redis is, how it stores data, and the basic commands, the next pages dive deeper into specific topics:

- **[Data Structures](data-structures.md)** — Strings, Lists, Sets, Hashes, and Sorted Sets with real-world walkthroughs
- **[TTL & Expiration](ttl-expiration.md)** — Deep dive into how data auto-expires (OTPs, sessions, rate limits)
- **[Memory & Eviction](memory-eviction.md)** — What happens when Redis runs out of memory
- **[Caching Explained](caching-explained.md)** — Why caching is Redis's #1 use case
- **[Caching Patterns](caching-patterns.md)** — Cache-Aside, Write-Through, and more
- **[Real-World Use Cases](real-world-usecases.md)** — Sessions, leaderboards, rate limiting, distributed locks
- **[Performance & Best Practices](performance-best-practices.md)** — Production-grade Redis usage
- **[Redis with Spring Boot](redis-with-springboot.md)** — Build real code
- **[Common Mistakes](common-mistakes.md)** — Avoid the pitfalls every beginner hits
- **[Interview Questions](redis-interview-questions.md)** — Get interview-ready

---

## Quick Summary

| Concept | Explanation |
|---------|-------------|
| Redis | In-memory key-value store |
| Why use it | Speed. Sub-millisecond reads |
| Key-Value | Every data has a name (key) and content (value) |
| TTL | Auto-expire data after N seconds |
| Single-Threaded | Processes one command at a time |
| Default Port | 6379 |
| Persistence | Optional (RDB, AOF). Often not needed for caching |

---

## Exercise

1. Install Redis using Docker.
2. Open `redis-cli` and run:
    - Store your name with key `myname`
    - Get it back
    - Set a TTL of 10 seconds
    - Wait 10 seconds and try to GET it again
3. What does `PONG` mean when you run `PING`?
4. **Bonus:** Store a session with `SETEX` that expires in 60 seconds. Use `TTL` to watch the countdown. What happens when you try to GET it after it expires?

---

**Next up:** [Data Structures](data-structures.md) - Learn the powerful data types Redis offers.
