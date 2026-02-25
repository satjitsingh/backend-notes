# Redis Data Structures

## Overview

Redis is not just a simple key-value store. It supports **rich data structures**. This is what makes Redis powerful—you're not limited to storing flat strings. You can store lists, sets, hashes, and sorted sets, each optimized for different problems.

Think of it like this:

- **String** = a single sticky note
- **List** = a stack of papers (ordered, can have duplicates)
- **Set** = a bag of unique marbles (no duplicates, unordered)
- **Hash** = a mini dictionary (key-value pairs inside one key)
- **Sorted Set** = a scoreboard (unique items, sorted by score)
- **Bitmap** = a row of light switches (each position is 0 or 1)
- **HyperLogLog** = a smart counter (estimates unique count in tiny space)
- **Stream** = a log or message queue (append-only, consumer groups)

```
┌──────────────────────────────────────────────┐
│              Redis Data Types                │
│                                              │
│  String    "hello"                           │
│  List      ["a", "b", "c"]                  │
│  Set       {"x", "y", "z"}  (unique)        │
│  Hash      {name: "Raj", age: "25"}         │
│  Sorted    {("Raj",100), ("Anu",200)}       │
│  Set       (unique + sorted by score)        │
│  Bitmap    [0,1,0,1,1,...]  (bits)          │
│  HyperLogLog  ~5.2M unique (approx)         │
│  Stream    msg1 → msg2 → msg3 (log)         │
└──────────────────────────────────────────────┘
```

**Why should you care?** Picking the right data structure makes your code simpler and faster. Use a List when you need order. Use a Set when you need uniqueness. Use a Sorted Set when you need both uniqueness and ranking. Let's walk through each one with real stories.

---

## 1. Strings

The **simplest** type. A key holds a single value—a string, a number, or even JSON. When you need to store one thing under one name, use a String.

### Why Strings Exist

Strings are the foundation. Every other type is built on top of this idea: a key maps to a value. Use Strings when you're caching a single API response, storing an OTP, or counting something.

**Time complexity:** `SET`, `GET`, `INCR`, `DECR` are all **O(1)**—constant time. `APPEND` and `STRLEN` are also O(1).

### Commands Explained

```bash
SET greeting "Hello World"
GET greeting                    # "Hello World"

# Numbers work too (Redis stores them as strings but treats them as numbers for INCR)
SET counter 10
INCR counter                    # 11
DECR counter                    # 10
INCRBY counter 5                # 15
```

**SET** stores a value. **GET** retrieves it. **INCR** increments a numeric value by 1—and here's the magic: it's **atomic**. Even if 1000 users hit `INCR page:views` at the same time, the count will be correct. No race conditions. No locks. Redis handles it.

### Real-World Walkthrough: Building a Page View Counter

Let's build a page view counter step by step. You have a blog, and you want to know how many people viewed the homepage.

**Step 1:** When the page loads, your app runs `INCR page:home:views`.

**What happens behind the scenes?** Redis looks up the key `page:home:views`. If it doesn't exist, Redis treats it as 0, increments it to 1, and stores it. If it exists, Redis adds 1 to the current value.

**Step 2:** User 2 visits. `INCR page:home:views` → 2. User 3 visits. → 3. And so on.

**Step 3:** To display the count, you run `GET page:home:views`. You get `"12345"` (or whatever the count is).

**Why use a String with INCR instead of storing a JSON object?** Because INCR is atomic. If you stored `{"views": 12345}` and tried to increment it in your application code, two requests could read 12345 at the same time, both add 1, and both write 12346. You'd lose a count. With INCR, Redis does it in one atomic operation.

!!! tip "Interview Insight"
    `INCR` is **atomic**. Even if 1000 users hit it at the same time, the count will be correct. No race condition.

### More String Use Cases

- **Cache an API response**: `SET "api:/users/1" "{name: Raj}" EX 3600`
- **OTP storage**: `SETEX "otp:9876543210" 300 "481923"`
- **Feature flags**: `SET "feature:dark_mode" "true"` — check this before rendering the UI

### When NOT to use Strings

- **Don't use Strings for objects with many fields.** Use a Hash instead. Updating one field in a JSON string requires GET + parse + modify + SET. With a Hash, you just HSET one field.
- **Don't store large blobs.** Images, videos, or huge JSON (>100 KB) waste RAM. Use object storage and store only a reference in Redis.
- **Don't use Strings when you need uniqueness.** For "unique visitors," a Set or HyperLogLog is better. A String can't enforce uniqueness.

---

## 2. Lists

A **List** is an ordered collection of strings. You can push items to the left or right, and pop from the left or right. Think of it as a queue (FIFO—first in, first out) or a stack (LIFO—last in, first out).

### Why Lists Exist

Sometimes you need order. A task queue: the first task added should be processed first. A chat history: messages should appear in the order they were sent. An activity feed: newest first. Lists give you that.

**Time complexity:** `LPUSH`, `RPUSH`, `LPOP`, `RPOP` are **O(1)**. `LRANGE start end` is **O(S+N)** where S is the start offset and N is the number of elements returned. So `LRANGE mylist 0 99` is fine. `LRANGE mylist 0 999999` on a million-item list is slow—avoid it.

### Commands Explained

```bash
# Push to the left (front)
LPUSH tasks "send email"
LPUSH tasks "process payment"

# Push to the right (back)
RPUSH tasks "generate report"

# See all items
LRANGE tasks 0 -1
# 1) "process payment"
# 2) "send email"
# 3) "generate report"

# Pop from left
LPOP tasks                      # "process payment"

# Pop from right
RPOP tasks                      # "generate report"

# Length
LLEN tasks                      # 1
```

**LPUSH** adds to the front. **RPUSH** adds to the back. **LRANGE 0 -1** means "give me items from index 0 to the last" (i.e., everything). **LPOP** removes and returns the first item. **RPOP** removes and returns the last item.

### How It Looks

```
LPUSH ──▶ [ "process payment", "send email", "generate report" ] ◀── RPUSH
LPOP  ◀──                                                        ──▶ RPOP
```

### Real-World Walkthrough: Building a Task Queue

Imagine you're building a background job system. You have workers that process tasks. Tasks are added by users (e.g., "send this email", "generate this report"). Workers pick tasks one by one.

**Step 1:** A user submits "send email". Your app runs `LPUSH task_queue "send_email:user_42"`. The task goes to the front of the list.

**Step 2:** Another user submits "process payment". `LPUSH task_queue "process_payment:order_101"`. Now the list has two items: `["process_payment:order_101", "send_email:user_42"]`.

**Step 3:** A worker wakes up. It runs `RPOP task_queue`. It gets `"send_email:user_42"` (the oldest task, since we're popping from the right). The worker processes it.

**Step 4:** Another worker runs `RPOP task_queue`. It gets `"process_payment:order_101"`. Now the queue is empty.

**What happens behind the scenes?** Redis maintains a doubly-linked list. LPUSH and RPUSH are O(1)—constant time. LPOP and RPOP are also O(1). You can process millions of tasks efficiently.

**Why use a List and not a database table?** Speed. Popping from a Redis List is microseconds. Querying a database, locking a row, and updating it is milliseconds. For high-throughput queues, Redis Lists are the go-to choice.

### More List Use Cases

- **Activity feed**: `LPUSH user:42:activity "Liked post 101"` — newest first
- **Chat messages**: `RPUSH chat:room_5 "message_json"` — store last N messages with `LRANGE chat:room_5 0 99`
- **Recent searches**: `LPUSH user:42:searches "laptop"` — trim to last 10 with `LTRIM`

### When NOT to use Lists

- **Don't use Lists when you need uniqueness.** Lists allow duplicates. If you need "unique online users," use a Set. If you add the same user twice to a List, you get two entries.
- **Don't use Lists for large unbounded data.** Always use `LTRIM` to cap size. A list that grows forever will eventually cause memory issues and slow `LRANGE` operations.
- **Don't use Lists for complex message queues.** For reliable queues with consumer groups, use Redis Streams instead. Lists are simple but lack features like acknowledgment and replay.

---

## 3. Sets

A **Set** is a collection of **unique** items. No duplicates allowed. And it's **unordered**—Redis doesn't guarantee order when you retrieve members.

### Why Sets Exist

Sometimes you need uniqueness. Tags on a blog post: "java", "redis", "tutorial"—each tag once. Online users: user IDs, no duplicates. Who liked a post: a set of user IDs. Set operations (union, intersection, difference) are also powerful—e.g., "find mutual friends."

**Time complexity:** `SADD`, `SREM`, `SISMEMBER` are **O(1)**. `SMEMBERS` is **O(N)**—it returns every member. For a set with 1 million members, SMEMBERS blocks Redis and sends 1 million items. Use `SSCAN` for iteration, or `SRANDMEMBER` for a sample.

### Commands Explained

```bash
# Add members
SADD skills "Java"
SADD skills "Python"
SADD skills "Java"              # Ignored! Already exists

# Get all members
SMEMBERS skills
# 1) "Java"
# 2) "Python"

# Check if member exists
SISMEMBER skills "Java"         # 1 (yes)
SISMEMBER skills "Go"           # 0 (no)

# Remove
SREM skills "Python"

# Count
SCARD skills                    # 1
```

**SADD** adds members. If you add a duplicate, Redis silently ignores it. **SMEMBERS** returns all members (order not guaranteed). **SISMEMBER** checks membership—returns 1 if yes, 0 if no. **SREM** removes. **SCARD** gives the count.

### Set Operations

```bash
SADD team1 "Alice" "Bob" "Charlie"
SADD team2 "Bob" "Diana" "Eve"

# Union (all people)
SUNION team1 team2
# Alice, Bob, Charlie, Diana, Eve

# Intersection (common people)
SINTER team1 team2
# Bob

# Difference (in team1 but not team2)
SDIFF team1 team2
# Alice, Charlie
```

### Real-World Walkthrough: Building an "Online Users" Tracker

Imagine you're building a chat app. You want to show "Who's online right now?" in the sidebar.

**Step 1:** When user Alice logs in or sends a message, you run `SADD online_users "alice_42"`. When user Bob comes online, `SADD online_users "bob_17"`.

**Step 2:** To get the list of online users, you run `SMEMBERS online_users`. You get `["alice_42", "bob_17"]`. You display these in the UI.

**Step 3:** When Alice logs out or goes idle (e.g., after 5 minutes of no activity), you run `SREM online_users "alice_42"`. She's gone from the set.

**Step 4:** To check if a specific user is online: `SISMEMBER online_users "alice_42"`. Returns 1 if yes, 0 if no. Useful for showing a green dot next to their name.

**What happens behind the scenes?** Redis uses a hash table internally. SADD, SREM, SISMEMBER are O(1). SMEMBERS is O(N) where N is the set size—so for millions of online users, you might want to use a different approach (e.g., only store user IDs for the current "room" or "channel").

**Why use a Set and not a List?** Uniqueness. If Alice logs in from two devices, you don't want her listed twice. Sets handle that automatically.

### More Set Use Cases

- **Tags**: `SADD post:101:tags "java" "redis" "tutorial"`
- **Mutual friends**: `SINTER user:42:friends user:17:friends` — find friends shared by user 42 and user 17
- **Unique visitors**: `SADD page:home:visitors "ip_1.2.3.4"` — count unique IPs with `SCARD`

### When NOT to use Sets

- **Don't use Sets for large unique counts.** If you need "how many unique visitors today?" and you have millions, a Set stores every ID—gigabytes of RAM. Use HyperLogLog instead—12 KB for millions of items, with ~0.81% error.
- **Don't use SMEMBERS on large sets.** It's O(N) and blocks. Use SSCAN to iterate, or SRANDMEMBER for a sample.
- **Don't use Sets when you need order.** Sets are unordered. For ranking or "top N," use a Sorted Set.

---

## 4. Hashes

A **Hash** is a mini dictionary inside a single key. Instead of storing one value, you store multiple field-value pairs. Perfect for objects like a user profile or a product.

### Why Hashes Exist

Imagine storing a user object. You could do `SET user:1 '{"name":"Satjit","age":25,"city":"Delhi"}'`. But what if you only want to update the city? You'd have to GET the whole thing, parse JSON, modify, serialize, and SET again. With a Hash, you can update one field: `HSET user:1 city "Mumbai"`. No need to touch the whole object.

**Time complexity:** `HSET`, `HGET`, `HDEL` are **O(1)**. `HGETALL` is **O(N)** where N is the number of fields. For a hash with 1000 fields, HGETALL returns all 1000. Use `HSCAN` or `HGET` for specific fields when the hash is large.

### Commands Explained

```bash
# Set fields
HSET user:1 name "Satjit"
HSET user:1 age "25"
HSET user:1 city "Delhi"

# Get one field
HGET user:1 name                # "Satjit"

# Get all fields and values
HGETALL user:1
# 1) "name"
# 2) "Satjit"
# 3) "age"
# 4) "25"
# 5) "city"
# 6) "Delhi"

# Set multiple fields at once
HMSET user:2 name "Rahul" age "30" city "Mumbai"

# Check if field exists
HEXISTS user:1 email            # 0 (no)

# Delete a field
HDEL user:1 city

# Increment a numeric field
HINCRBY user:1 age 1            # 26
```

**HSET** sets one field. **HGET** gets one field. **HGETALL** returns all fields and values (alternating: field1, value1, field2, value2). **HMSET** sets multiple fields (or use **HSET** with multiple args in Redis 4+). **HDEL** removes a field. **HINCRBY** increments a numeric field.

### How It Looks

```
Key: "user:1"
┌──────────┬──────────┐
│  Field   │  Value   │
├──────────┼──────────┤
│  name    │  Satjit  │
│  age     │  25      │
│  city    │  Delhi   │
└──────────┴──────────┘
```

### Real-World Walkthrough: Storing a User Profile (and Why It's Better Than JSON String)

**Scenario:** You want to cache user profile data. Option A: store as JSON string. Option B: store as Hash.

**Option A (JSON string):**
```bash
SET user:1 '{"name":"Satjit","age":25,"city":"Delhi"}'
```
User updates their city. You must: GET user:1, parse JSON, change city, serialize, SET user:1. Plus, you're sending the whole object over the network twice.

**Option B (Hash):**
```bash
HSET user:1 name "Satjit" age "25" city "Delhi"
```
User updates their city. You do: `HSET user:1 city "Mumbai"`. Done. One command. One field update. No parsing, no serialization.

**Why should you care?** In a high-traffic app, updating one field without touching the rest saves CPU and network. Hashes also support `HINCRBY`—e.g., incrementing a "view_count" field without reading the whole object.

!!! tip "Interview Insight"
    Use Hashes instead of storing a JSON string. Why? You can **update one field** without reading the whole object.

### More Hash Use Cases

- **Product details**: `HSET product:101 name "Laptop" price "50000" category "Electronics"`
- **Shopping cart**: User 42's cart: `HSET cart:42 "item_101" 2 "item_205" 1` — item ID as field, quantity as value
- **Counters per category**: `HINCRBY stats:page:42 views 1` — multiple counters in one key

### When NOT to use Hashes

- **Don't use Hashes for single values.** If you only need one field, a String is simpler. Hashes have overhead for the structure.
- **Don't use HGETALL on huge hashes.** O(N) and blocks. Prefer HGET for specific fields or HSCAN for iteration.
- **Don't use Hashes when you need to query by field value.** For "find all users where city=Delhi," use a database. Redis Hashes don't support secondary indexes.

---

## 5. Sorted Sets (ZSets)

Like a Set, but every member has a **score**. Members are sorted by score. You can get the top N, the bottom N, or members in a score range.

### Why Sorted Sets Exist

Leaderboards. Trending posts. Priority queues. Anything where you need "ranked by a number" and "get top N" or "get by score range." Sorted Sets are built for this.

**Time complexity:** `ZADD`, `ZINCRBY`, `ZREM`, `ZRANK`, `ZSCORE` are **O(log N)**. `ZRANGE` and `ZREVRANGE` are **O(log N + M)** where M is the number of elements returned. So getting top 10 from a million-member leaderboard is still fast—O(log 1M + 10) ≈ 20 + 10 operations.

### Commands Explained

```bash
# Add members with scores
ZADD leaderboard 100 "Alice"
ZADD leaderboard 250 "Bob"
ZADD leaderboard 180 "Charlie"

# Get all (sorted by score, low to high)
ZRANGE leaderboard 0 -1 WITHSCORES
# 1) "Alice"    - 100
# 2) "Charlie"  - 180
# 3) "Bob"      - 250

# Get all (high to low)
ZREVRANGE leaderboard 0 -1 WITHSCORES
# 1) "Bob"      - 250
# 2) "Charlie"  - 180
# 3) "Alice"    - 100

# Rank (0-based, lowest score = rank 0)
ZRANK leaderboard "Bob"        # 2

# Reverse rank (highest score = rank 0)
ZREVRANK leaderboard "Bob"     # 0

# Increment score
ZINCRBY leaderboard 50 "Alice" # Alice now has 150

# Count members in score range
ZCOUNT leaderboard 100 200     # 2 (Alice and Charlie)
```

**ZADD** adds a member with a score. **ZRANGE** gets members from lowest to highest score. **ZREVRANGE** gets from highest to lowest. **ZRANK** gives the rank (0-based, lowest first). **ZREVRANK** gives the rank with highest first. **ZINCRBY** adds to a member's score. **ZCOUNT** counts members in a score range.

### Real-World Walkthrough: Building a Gaming Leaderboard

**Scenario:** You're building a mobile game. Players earn points. You need a leaderboard: "Top 10 players this week."

**Step 1:** When Alice finishes a level with 100 points, you run `ZINCRBY leaderboard:week_42 100 "alice_123"`. Alice is added with score 100. Bob scores 250: `ZINCRBY leaderboard:week_42 250 "bob_456"`. Charlie scores 180: `ZINCRBY leaderboard:week_42 180 "charlie_789"`.

**Step 2:** To show the top 10: `ZREVRANGE leaderboard:week_42 0 9 WITHSCORES`. You get Bob (250), Charlie (180), Alice (100). Display in order.

**Step 3:** Alice plays again and earns 50 more points. `ZINCRBY leaderboard:week_42 50 "alice_123"`. Her score is now 150. The leaderboard updates automatically.

**Step 4:** To show Alice's rank: `ZREVRANK leaderboard:week_42 "alice_123"`. Returns 2 (0-based, so she's 3rd). To show "You're in the top 50%": `ZRANK leaderboard:week_42 "alice_123"` and compare to `ZCARD leaderboard:week_42`.

**What happens behind the scenes?** Redis uses a skip list + hash table. ZADD, ZINCRBY, ZRANK are O(log N). ZRANGE is O(log N + M) where M is the number of elements returned. Even with millions of players, leaderboard operations stay fast.

### More Sorted Set Use Cases

- **Trending posts**: Score = number of likes in last 24 hours. `ZREVRANGE trending:posts 0 9`
- **Priority queue**: Score = priority (lower = higher priority). Workers pop with `ZRANGE queue 0 0` then `ZREM`
- **Rate limiter (sliding window)**: Score = timestamp. Remove old entries with `ZREMRANGEBYSCORE`, count with `ZCARD`

### When NOT to use Sorted Sets

- **Don't use Sorted Sets when you don't need order.** If you just need uniqueness, a Set is simpler and uses less memory.
- **Don't use Sorted Sets for huge score ranges with few members.** The skip list structure has overhead. For simple "top 10" with a small set, a List might be fine.
- **Don't use Sorted Sets when scores change frequently and you need exact consistency.** ZADD updates are O(log N). For millions of members with constant updates, this adds up. But for most leaderboards, it's fine.

---

## 6. Bitmaps

A **Bitmap** is a special String that stores bits (0 or 1). You can set, get, and count bits at specific positions. Perfect for tracking binary flags.

### Why Bitmaps Exist

Imagine tracking "which users were active today?" You have 10 million user IDs. A Set would store 10 million strings—huge. A Bitmap uses one bit per user: user ID 42 = bit 42. 10 million users = ~1.25 MB. Massive savings.

### Commands Explained

```bash
# Set bit at position (user ID 42 was active)
SETBIT activity:2024-02-25 42 1

# Get bit (was user 42 active?)
GETBIT activity:2024-02-25 42
# (integer) 1

# Count users active today
BITCOUNT activity:2024-02-25
# (integer) 15000

# Check if user 100 was active
GETBIT activity:2024-02-25 100
# (integer) 0
```

### Real-World Walkthrough: Daily Active Users

**Scenario:** You want to know "how many unique users logged in today?" and "was user 42 active today?"

**Step 1:** Use the date as the key. User ID as the bit position. When user 42 logs in: `SETBIT dau:2024-02-25 42 1`.

**Step 2:** To get the count: `BITCOUNT dau:2024-02-25`. Returns the number of unique users.

**Step 3:** To check if user 42 was active: `GETBIT dau:2024-02-25 42`. Returns 1 or 0.

**Memory:** 10 million users = 10 million bits = 1.25 MB. A Set would be hundreds of MB.

!!! tip "Bitmap limitations"
    Bitmaps work best when user IDs are small integers (0 to a few million). If your IDs are UUIDs or large numbers, you need a hash function to map them to bit positions. Still efficient for "presence" tracking.

---

## 7. HyperLogLog

**HyperLogLog** is a probabilistic data structure. It estimates the **cardinality** (number of unique elements) of a set using only ~12 KB of memory. The trade-off: it's approximate (~0.81% standard error), not exact.

### Why HyperLogLog Exists

You want to count "unique visitors to our site today." You have 100 million visitors. A Set would store 100 million strings—gigabytes. HyperLogLog stores an estimate in 12 KB. You can't list the members or check "was user X here?"—you can only get the count.

### Commands Explained

```bash
# Add members (duplicates are automatically ignored)
PFADD visitors:2024-02-25 "user_1" "user_2" "user_3"
PFADD visitors:2024-02-25 "user_1"  # Ignored, already counted

# Get estimated count
PFCOUNT visitors:2024-02-25
# (integer) 3

# Merge multiple HyperLogLogs (e.g., combine daily counts)
PFMERGE visitors:week "visitors:mon" "visitors:tue" "visitors:wed"
PFCOUNT visitors:week
```

### Real-World Walkthrough: Unique Visitors

**Scenario:** Your blog gets 5 million unique visitors per day. You want to count them without storing 5 million IDs.

**Step 1:** When a user visits, add their ID (or IP, or session ID): `PFADD visitors:2024-02-25 "user_42"`.

**Step 2:** At end of day: `PFCOUNT visitors:2024-02-25`. You get something like 4,987,234 (approximate).

**Step 3:** To get weekly unique: `PFMERGE visitors:week "visitors:mon" "visitors:tue" ... "visitors:sun"` then `PFCOUNT visitors:week`. Note: this counts unique across the week (no double-counting if someone visited on Monday and Tuesday).

**Memory:** 12 KB per HyperLogLog. 5 million visitors in a Set would be hundreds of MB.

!!! warning "HyperLogLog is approximate"
    You get ~0.81% error. For 5 million, that's ±40,000. If you need exact counts (e.g., billing), use a Set or a database. For analytics and dashboards, HyperLogLog is perfect.

---

## 8. Redis Streams

**Redis Streams** are a log-like data structure. Producers append messages. Consumers read them. Think of it as a modern alternative to Lists for message queues.

### Why Streams Exist

Lists are simple: LPUSH/RPOP. But they lack features. What if a consumer crashes before processing? The message is gone. What if you need multiple consumer groups (e.g., "email service" and "analytics" both read the same stream)? Lists don't support that.

Streams add: **consumer groups**, **acknowledgment**, **pending message tracking**, **range queries by ID**. They're the right choice for reliable message queues.

### Quick Commands

```bash
# Add a message
XADD orders * user "42" product "101" quantity 2

# Read messages (consumer)
XREAD COUNT 10 STREAMS orders 0

# Create consumer group
XGROUP CREATE orders order-processors 0

# Read as consumer in group
XREADGROUP GROUP order-processors consumer1 COUNT 10 STREAMS orders >
```

### When to use Streams vs Lists

| Use Case | List | Stream |
|----------|------|--------|
| Simple task queue, best-effort | ✅ | Overkill |
| Reliable queue, no message loss | ❌ | ✅ |
| Multiple consumers, same messages | ❌ | ✅ |
| Need acknowledgment | ❌ | ✅ |
| Chat history | ✅ | ✅ (both work) |

!!! info "Streams are powerful"
    Redis Streams support consumer groups, exactly-once processing, and replay. For production message queues, prefer Streams over Lists when you need reliability.

---

## Choosing the Right Data Structure: Decision Flowchart

```
Do you need to store a single value?
  YES → String

Do you need ordering (queue, stack, feed)?
  YES → List (simple) or Stream (reliable queue)

Do you need uniqueness (no duplicates)?
  YES → Do you need ranking by score?
    YES → Sorted Set
    NO  → Do you need exact count or just approximate?
      Exact, small set → Set
      Approximate, huge set → HyperLogLog

Do you need to store an object with multiple fields?
  YES → Hash

Do you need to track binary flags (active/not) for many IDs?
  YES → Bitmap

Do you need to count unique items in millions?
  YES → HyperLogLog
```

### Detailed Scenarios

| Scenario | Best Structure | Why |
|----------|----------------|-----|
| Cache API response | String | Single blob of data |
| Page view counter | String + INCR | Atomic increment |
| Task queue | List | FIFO order, LPUSH/RPOP |
| Reliable message queue | Stream | Consumer groups, acknowledgment |
| Online users | Set | Uniqueness, fast membership check |
| User profile | Hash | Update individual fields |
| Leaderboard | Sorted Set | Ranked by score, get top N |
| Unique visitors per day (small) | Set | Uniqueness, SCARD for count |
| Unique visitors per day (millions) | HyperLogLog | ~12 KB, approximate count |
| Daily active users (presence) | Bitmap | One bit per user, minimal memory |
| Shopping cart | Hash | Item ID → quantity mapping |
| Recent activity | List | Order matters, LPUSH + LTRIM |
| Mutual friends | Set (SINTER) | Set intersection |
| Tags on a post | Set | Uniqueness, no duplicates |

### Comparison: When to Use What

| Need | String | List | Set | Hash | Sorted Set | Bitmap | HyperLogLog | Stream |
|------|--------|------|-----|------|------------|--------|-------------|--------|
| Single value | ✅ | | | | | | | |
| Order | | ✅ | | | | | | ✅ |
| Uniqueness | | | ✅ | | ✅ | | | |
| Multiple fields | | | | ✅ | | | | |
| Ranking | | | | | ✅ | | | |
| Presence (0/1) | | | | | | ✅ | | |
| Unique count (huge) | | | | | | | ✅ | |
| Reliable queue | | | | | | | | ✅ |
| Atomic counter | ✅ | | | | | | | |

---

## Quick Summary

| Type | Like | Example Use | Complexity (key ops) |
|------|------|-------------|----------------------|
| String | Sticky note | Cache, counter, OTP | GET/SET O(1) |
| List | Queue/Stack | Task queue, chat history | LPUSH/RPOP O(1), LRANGE O(S+N) |
| Set | Bag of unique items | Tags, online users | SADD/SISMEMBER O(1), SMEMBERS O(N) |
| Hash | Mini dictionary | User profile, cart | HSET/HGET O(1), HGETALL O(N) |
| Sorted Set | Scoreboard | Leaderboard, trending | ZADD/ZRANGE O(log N) |
| Bitmap | Binary flags | Daily active users | SETBIT/GETBIT O(1) |
| HyperLogLog | Approximate counter | Unique visitors (millions) | PFADD/PFCOUNT O(1) |
| Stream | Log/Queue | Reliable message queue | XADD/XREAD O(1) |

---

## Exercise

1. **Strings:** Create a page view counter for a blog post. Use `INCR` and `GET`. What happens if you INCR a key that doesn't exist?

2. **Hashes:** Create a Hash for your profile with name, age, and city. Update just your city. Use `HGETALL` to verify.

3. **Sorted Sets:** Create a leaderboard with 5 players and scores. Find the top 3 using `ZREVRANGE`. Add 50 points to the last-place player. What's the new order?

4. **Sets:** Create two Sets: your friends and your sibling's friends. Use `SINTER` to find mutual friends. Use `SUNION` to find all unique friends.

5. **Lists:** Simulate a task queue. Add 3 tasks with `LPUSH`. Pop them one by one with `RPOP`. In what order do they come out?

6. **Guided:** You're building a "recently viewed products" feature. Each user can see the last 10 products they viewed. Which data structure? Write the Redis commands to add a product and to retrieve the last 10.

---

**Next up:** [Caching Explained](caching-explained.md) - Understand why caching is the #1 use case for Redis.
