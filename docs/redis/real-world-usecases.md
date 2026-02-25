# Real-World Use Cases

## Redis Is Everywhere

Redis isn't just a toy for tutorials. It powers features you use every day — from the Instagram feed on your phone to the Uber driver tracking on your screen. This page walks through the most important real-world use cases with detailed stories and explanations.

---

## 1. Caching (The #1 Use Case)

### The Story

You work at Flipkart. The product page for "iPhone 15 Pro" gets 50,000 views per minute during a sale. Every view triggers a database query: product details, reviews, ratings, seller info. That's 50,000 queries per minute for the **same data**.

Your database can handle maybe 5,000 queries per second before it slows down. You're hitting 833 queries per second just for ONE product. You have thousands of products. The database is drowning.

### The Redis Solution

Cache the product page data in Redis.

```bash
SETEX product:iphone15pro 600 '{"name":"iPhone 15 Pro","price":134900,...}'
```

- 1st request: Cache miss → query database → store in Redis → return to user (45ms)
- 2nd through 50,000th request: Cache hit → return from Redis (0.3ms)

Database load: 1 query instead of 50,000. Response time: 150x faster.

### How It Works Step by Step

1. User requests product page for iPhone 15 Pro.
2. App checks Redis: `GET product:iphone15pro`
3. **Cache hit:** Redis returns the JSON. App sends it to the user. Done. No database.
4. **Cache miss:** Redis returns nil. App queries the database.
5. App gets product data from DB. App writes to Redis: `SETEX product:iphone15pro 600 '{"name":"iPhone 15 Pro",...}'`
6. App returns the data to the user.
7. Next 50,000 requests hit the cache. No database load.

### Anti-Patterns

- **Don't cache everything.** Cache only hot data.

- **Don't forget TTL.** Cache without expiry = stale data forever.

- **Don't cache user-specific data with a shared key.** If the key is the same for all users but the data differs per user, you'll serve wrong data.

### Who Uses This

- **Amazon**: Caches product details, recommendations, pricing
- **Netflix**: Caches movie metadata, thumbnails, user preferences
- **Flipkart/Myntra**: Caches product listings, search results, filter counts
- **Walmart**: Caches product pages, inventory counts, pricing for millions of SKUs

---

## 2. Session Storage

### The Story

You run a banking app. A user logs in on their phone. Their session (who they are, what permissions they have, their account details) is stored in your server's memory. Everything works.

Then your DevOps team deploys a new version. The server restarts. The session is gone. The user is logged out mid-transaction. They're furious.

Or worse: you have 3 servers behind a load balancer. Request 1 goes to Server A (which has the session). Request 2 goes to Server B (which doesn't). The user sees "Please log in again." Terrible experience.

### The Redis Solution

Store sessions in Redis instead of server memory.

```bash
SETEX session:token:abc123 1800 '{"userId":42,"role":"admin","cartItems":3}'
```

- All servers read from the same Redis. No session mismatch.
- Server restarts? Sessions are still in Redis.
- Session expires after 30 minutes of inactivity (TTL).

```
┌──────────┐       ┌──────────┐       ┌──────────┐
│ Server A │──────▶│          │◀──────│ Server C │
└──────────┘       │  Redis   │       └──────────┘
┌──────────┐──────▶│(sessions)│
│ Server B │       │          │
└──────────┘       └──────────┘

All servers share the same session store!
```

### How It Works Step by Step

1. User logs in. Server generates a session token (e.g., UUID).
2. Server stores session data in Redis: `SETEX session:token:abc123 1800 '{"userId":42,"role":"admin"}'`
3. Server sends the token to the client (cookie or response header).
4. Next request: User sends the token. Request goes to Server B (load balancer).
5. Server B: `GET session:token:abc123` from Redis. Gets the session. Knows user 42, role admin.
6. Server B processes the request. No "please log in again" — session is shared.
7. After 30 minutes of no activity, TTL expires. Key is deleted. User must log in again.

### Anti-Patterns

- **Don't store session in server memory.** It's lost on restart. Use Redis.

- **Don't store huge objects in session.** Keep it small. Session is read on every request.

- **Don't forget to set TTL.** Sessions without expiry = memory leak.

### Who Uses This

- **GitHub**: Stores user sessions in Redis for seamless experience across server restarts
- **Shopify**: Millions of merchant sessions stored in Redis
- **Twitter/X**: Session data for logged-in users
- Every major web app with multiple servers

---

## 3. Rate Limiting

### The Story

You run a public API. A malicious user writes a script that sends 10,000 requests per second to your `/api/search` endpoint. Your servers are overwhelmed. Other users can't search. Your database is hammered.

### The Redis Solution

Count requests per user per time window using Redis atomic counters.

```bash
# When a request comes from user 42:
INCR rate:user:42:minute
# If this is the first request, set a 60-second expiry:
EXPIRE rate:user:42:minute 60

# Check the count:
GET rate:user:42:minute
# If > 100, reject with HTTP 429 "Too Many Requests"
```

Why Redis? Because `INCR` is **atomic**. Even if 100 requests arrive at the exact same millisecond, the counter is accurate. No race conditions. No double-counting.

### How It Works Step by Step

1. Request arrives from user 42. App extracts user ID from token or IP.
2. App runs: `INCR rate:user:42:minute`
3. If the key was just created (Redis returns 1), app runs: `EXPIRE rate:user:42:minute 60`
4. App: `GET rate:user:42:minute` → returns count.
5. If count > 100: Return HTTP 429 "Too Many Requests". Reject.
6. If count <= 100: Process the request. Allow.
7. After 60 seconds, key expires. Next minute starts fresh.

### Anti-Patterns

- **Don't use a database for rate limiting.** DB updates are slow. Redis INCR is microseconds.

- **Don't forget TTL.** Without EXPIRE, the counter never resets. User gets blocked forever.

- **Don't rate limit by IP only.** Use user ID when possible. IPs can be shared (NAT).

### Who Uses This

- **GitHub API**: 60 requests per hour for unauthenticated, 5000 for authenticated
- **Twitter API**: Rate limits per 15-minute window
- **Stripe**: Rate limits API calls per account
- **Twilio**: Rate limits SMS and voice API calls per account

---

## 4. Leaderboards & Rankings

### The Story

You're building a gaming app. Players earn points. You need to show:
- Top 100 players globally
- A player's rank
- Players ranked 50-60

With a relational database, ranking millions of players requires sorting the entire table. That's slow.

### The Redis Solution

Redis **Sorted Sets** were literally designed for this.

```bash
# Add player scores
ZADD leaderboard 2500 "player:alice"
ZADD leaderboard 4200 "player:bob"
ZADD leaderboard 3100 "player:charlie"
ZADD leaderboard 1800 "player:diana"

# Top 3 players (highest score first)
ZREVRANGE leaderboard 0 2 WITHSCORES
# 1) "player:bob"      4200
# 2) "player:charlie"  3100
# 3) "player:alice"    2500

# Alice's rank (0-based, highest = 0)
ZREVRANK leaderboard "player:alice"
# (integer) 2   ← 3rd place

# Update score (player earns more points)
ZINCRBY leaderboard 2000 "player:alice"
# Alice now has 4500, she's #1!
```

All these operations are **O(log N)**. Even with 10 million players, ranking is instant.

### How It Works Step by Step

1. Player Alice finishes a game. She earns 2500 points.
2. Game server: `ZADD leaderboard 2500 "player:alice"`
3. Player Bob earns 4200. `ZADD leaderboard 4200 "player:bob"`
4. User opens leaderboard page. App: `ZREVRANGE leaderboard 0 99 WITHSCORES`
5. Redis returns top 100 players. App renders the list.
6. User clicks "My rank". App: `ZREVRANK leaderboard "player:alice"` → returns 2 (3rd place).
7. Alice plays again. `ZINCRBY leaderboard 2000 "player:alice"`. Her score updates. Leaderboard reflects it on next fetch.

### Anti-Patterns

- **Don't use a database with ORDER BY for real-time leaderboards.** It's slow at scale. Sorted Sets are built for this.

- **Don't store the entire player object in the member.** Store ID. Fetch details from DB when needed.

- **Don't forget to handle ties.** Same score = same rank. Use a secondary sort (e.g., timestamp) if needed.

### Who Uses This

- **PUBG/Free Fire**: Real-time leaderboards
- **Stack Overflow**: Reputation rankings
- **Duolingo**: Weekly leaderboards per league
- **Discord**: Server member activity rankings

---

## 5. Real-Time Analytics & Counters

### The Story

You're YouTube. You need to show "1,523,847 views" on a video page. This number increases by hundreds every second. You can't query the database for a `COUNT(*)` on every page view — that would be insanely slow.

### The Redis Solution

Use Redis as a real-time counter.

```bash
# Every time someone watches the video:
INCR video:views:abc123

# To display the count:
GET video:views:abc123
# "1523847"
```

`INCR` is atomic, single-threaded, and takes microseconds. It can handle thousands of increments per second without breaking a sweat.

For more complex analytics (unique visitors):

```bash
# Track unique visitors using HyperLogLog (probabilistic, very memory efficient)
PFADD visitors:today:page:home "user:42"
PFADD visitors:today:page:home "user:99"
PFADD visitors:today:page:home "user:42"   # Duplicate, not counted

PFCOUNT visitors:today:page:home
# (integer) 2
```

HyperLogLog can count **millions** of unique items using only **12 KB of memory**.

### Who Uses This

- **YouTube**: View counts
- **Twitter/X**: Like, retweet, and reply counts
- **Instagram**: Story view counts

---

## 6. Distributed Locking

### The Story

You run an e-commerce site. User clicks "Place Order." Due to a network glitch, the request is sent twice. Without a lock, two orders are created. User gets charged twice. Bad.

### The Redis Solution

Use Redis as a distributed lock.

```bash
# Try to acquire lock (NX = only if not exists, EX = expire in 10 seconds)
SET lock:order:user:42 "server-1" NX EX 10
```

If the command returns `OK`, you got the lock. Process the order. When done, delete the lock.

If it returns `nil`, someone else has the lock. Don't process. The duplicate request is blocked.

The `EX 10` is a safety net. If the server crashes while holding the lock, it auto-releases after 10 seconds.

```
Request 1: SET lock:order:user:42 ... NX → OK (got lock, processes order)
Request 2: SET lock:order:user:42 ... NX → nil (lock taken, skip)
```

### Who Uses This

- **Payment systems**: Prevent double charges
- **Inventory systems**: Prevent overselling (only 1 left, 2 people click "buy")
- **Scheduled jobs**: Ensure only one server runs a cron job

---

## 7. Message Queue (Lightweight)

### The Story

You need a simple task queue. Workers pick up tasks and process them. You don't need the full power of Kafka or RabbitMQ. Just a simple queue.

### The Redis Solution

Use Redis Lists as a queue.

```bash
# Producer: Push tasks to the queue
LPUSH task:queue '{"type":"send_email","to":"user@mail.com"}'
LPUSH task:queue '{"type":"resize_image","imageId":"img123"}'

# Consumer: Pop tasks from the other end
BRPOP task:queue 0
# Returns the oldest task (FIFO). Blocks until a task is available.
```

`BRPOP` is a **blocking pop**. The consumer waits until a task appears. No polling. No busy-waiting.

```
Producer ──LPUSH──▶ [task3, task2, task1] ──BRPOP──▶ Consumer
            (left)                          (right)
```

!!! warning "When to use Redis vs Kafka for queuing"
    Redis queues are great for simple, lightweight task queuing (< 10,000 tasks/sec). For high-throughput event streaming with replay, message retention, and consumer groups, use Kafka.

### How It Works Step by Step

1. API receives "send email" request. App: `LPUSH task:queue '{"type":"send_email","to":"user@mail.com"}'`
2. Task is at the left end of the list. Worker is blocking on: `BRPOP task:queue 0`
3. Redis delivers the task to the worker. Worker gets the JSON.
4. Worker parses it. Sends the email. Task done.
5. Worker calls BRPOP again. Blocks. Waits for next task.
6. Multiple workers can run BRPOP. Redis delivers each task to only one worker. Load balanced.

### Anti-Patterns

- **Don't use Redis for mission-critical queues with strict ordering.** If Redis restarts, in-flight tasks can be lost. Use Kafka for that.

- **Don't use LPOP without blocking.** Polling with LPOP wastes CPU. Use BRPOP to block until a task arrives.

- **Don't forget to handle task failures.** If the worker crashes mid-processing, the task is lost. Consider using Redis Streams for acknowledgment.

### Who Uses This

- **Celery**: Uses Redis as a broker for Python task queues
- **Sidekiq**: Uses Redis for Ruby background jobs
- **Laravel**: Redis queue driver for PHP
- **Bull**: Node.js job queue with Redis

---

## 8. Pub/Sub (Real-Time Broadcasting)

### The Story

You're building a live cricket score app. When India hits a six, 500,000 users need to see the updated score instantly.

### The Redis Solution

Redis Pub/Sub broadcasts messages to all subscribers in real time.

```bash
# Subscriber (each user's connection):
SUBSCRIBE cricket:live:IND_vs_AUS

# Publisher (score update service):
PUBLISH cricket:live:IND_vs_AUS '{"event":"SIX","score":"267/4","over":"42.3"}'
```

Every subscriber receives the message instantly. No polling. No database queries.

```
Publisher ──PUBLISH──▶ Redis Channel ──▶ Subscriber 1 (gets message instantly)
                                    ──▶ Subscriber 2 (gets message instantly)
                                    ──▶ Subscriber 3 (gets message instantly)
                                    ──▶ ... 500,000 subscribers
```

!!! warning "Limitation"
    Redis Pub/Sub is fire-and-forget. If a subscriber is offline, it misses the message. There's no message history. For durable messaging, use Kafka or Redis Streams.

### How It Works Step by Step

1. 500,000 users open the cricket app. Each connection subscribes: `SUBSCRIBE cricket:live:IND_vs_AUS`
2. Score update service receives "India hits a six" from the data feed.
3. Service: `PUBLISH cricket:live:IND_vs_AUS '{"event":"SIX","score":"267/4"}'`
4. Redis receives the message. Redis sends it to all 500,000 subscribers.
5. Each subscriber's connection receives the message. App pushes it to the user's browser via WebSocket.
6. All users see the update within milliseconds. No polling. No database.

### Anti-Patterns

- **Don't use Pub/Sub when you need message history.** If a subscriber reconnects, it gets nothing. Use Redis Streams or Kafka.

- **Don't use Pub/Sub for critical commands.** If the subscriber is down, the message is lost. Use a queue for "must process" messages.

- **Don't have one subscriber do heavy work.** Each message is sent to all subscribers. If one is slow, it doesn't block others, but that subscriber will fall behind.

### Who Uses This

- **Live score apps**: Cricbuzz, ESPN
- **Chat applications**: Real-time message delivery
- **Collaborative editing**: Google Docs-style presence
- **Sports betting platforms**: Live odds updates

---

## 9. Geospatial Data

### The Story

You're building Uber. User opens the app and asks: "Show me drivers within 2 km."

### The Redis Solution

Redis has built-in geospatial support.

```bash
# Add driver locations (longitude, latitude, name)
GEOADD drivers 77.2090 28.6139 "driver:101"
GEOADD drivers 77.2100 28.6150 "driver:102"
GEOADD drivers 77.2300 28.6400 "driver:103"

# Find drivers within 2 km of user's location
GEORADIUS drivers 77.2095 28.6145 2 km WITHCOORD WITHDIST COUNT 5
# Returns driver:101 (0.08 km), driver:102 (0.15 km)
# driver:103 is too far (3.2 km)
```

This is backed by Sorted Sets internally. Very fast, even with millions of entries.

### How It Works Step by Step

1. Driver 101 starts his shift. App: `GEOADD drivers 77.2090 28.6139 "driver:101"`
2. Driver moves. App updates: `GEOADD drivers 77.2100 28.6145 "driver:101"` (overwrites)
3. User at (77.2095, 28.6145) requests "Show nearby drivers."
4. App: `GEORADIUS drivers 77.2095 28.6145 2 km WITHCOORD WITHDIST COUNT 5`
5. Redis returns drivers within 2 km, sorted by distance. Driver 101 is 0.08 km away.
6. App displays them on the map. User selects driver 101. Assignment happens.

### Anti-Patterns

- **Don't update location on every GPS tick.** Batch updates. E.g., update every 10 seconds or when the driver moves 50 meters.

- **Don't store driver details in the member.** Store ID. Fetch name, photo, rating from DB when displaying.

- **Don't use GEORADIUS for very large radii.** For "drivers in the city," consider pre-filtering by region first.

### Who Uses This

- **Uber, Ola, Lyft**: Find nearby drivers
- **Swiggy, Zomato**: Find nearby restaurants, delivery partners
- **Pokemon GO**: Find nearby Pokestops
- **Dating apps**: Find nearby users

---

## 10. Feature Flags

### The Story

You want to roll out a new checkout UI to 10% of users. If it causes problems, you want to disable it instantly — without deploying new code.

### The Redis Solution

Store feature flags in Redis.

```bash
# Enable new checkout for 10% of users
SET feature:new_checkout '{"enabled":true,"percentage":10}'
```

Your app checks Redis on each request:

```java
String flag = redis.get("feature:new_checkout");
if (shouldShowFeature(flag, userId)) {
    showNewCheckout();
} else {
    showOldCheckout();
}
```

To disable instantly:

```bash
SET feature:new_checkout '{"enabled":false}'
```

All servers pick up the change immediately. No deployment. No restart.

### How It Works Step by Step

1. DevOps sets flag: `SET feature:new_checkout '{"enabled":true,"percentage":10}'`
2. User 42 requests checkout page. App: `GET feature:new_checkout`
3. App gets the JSON. Parses it. `hash(userId) % 100 < 10`? User 42 might be in the 10%.
4. If yes: Render new checkout UI. If no: Render old UI.
5. Bug found. DevOps: `SET feature:new_checkout '{"enabled":false}'`
6. Next request from any user: App fetches again. Gets disabled. All users see old UI. Instant rollback.

### Anti-Patterns

- **Don't cache feature flags in app memory for too long.** You want quick rollback. Cache for a few seconds, or fetch on each request for critical flags.

- **Don't use feature flags for security.** "Admin only" should be enforced in the backend, not by a flag that could be tampered with.

- **Don't have hundreds of flags with complex logic.** Keep it simple. Use a proper feature flag service (LaunchDarkly, etc.) if you need targeting rules.

### Who Uses This

- **Netflix**: A/B testing, gradual rollouts
- **Spotify**: Feature toggles for experiments
- **Every modern tech company**: Canary deployments, kill switches

---

## 11. Shopping Cart

### The Story

You run an e-commerce site. Users add items to their cart. They might leave and come back later. They might switch devices. The cart must persist. Storing the cart in the database on every add/remove is expensive. Sessions are lost when the server restarts. You need something fast and shared.

### The Redis Solution

Store the cart in a Redis Hash. One Hash per user. Field = product ID. Value = quantity.

```bash
# User 42 adds 2x product 101, 1x product 205
HSET cart:user:42 101 2
HSET cart:user:42 205 1

# Get entire cart
HGETALL cart:user:42
# 101 -> 2, 205 -> 1

# Update quantity
HSET cart:user:42 101 3

# Remove item
HDEL cart:user:42 205

# Set expiry (cart expires after 7 days of inactivity)
EXPIRE cart:user:42 604800
```

Why a Hash? You can add, update, or remove individual items without reading and rewriting the whole cart. Atomic operations. Fast.

### How It Works Step by Step

1. User adds "Laptop" (product 101) to cart. App: `HINCRBY cart:user:42 101 1`
2. User adds another laptop. `HINCRBY cart:user:42 101 1` → quantity is 2.
3. User opens cart page. App: `HGETALL cart:user:42` → gets {101: 2, 205: 1}
4. App fetches product details (name, price) from DB for 101 and 205. Renders cart.
5. User removes product 205. App: `HDEL cart:user:42 205`
6. User abandons cart. After 7 days, TTL expires. Key is deleted. Cart is gone.

### Why Redis Is Better Than Session or DB

- **vs Session:** Session is in server memory. Lost on restart. Not shared across servers. Redis is shared and persistent.
- **vs Database:** DB write on every add/remove is slow. Redis HSET/HDEL is microseconds. You can sync to DB at checkout if you need analytics.

### Anti-Patterns

- **Don't store full product data in the cart.** Store product ID and quantity. Fetch name, price, image from DB when displaying. Product data can change.

- **Don't forget TTL.** Carts should expire. Otherwise, abandoned carts fill memory forever.

- **Don't use a String with JSON for the cart.** A Hash lets you update one item without parsing and rewriting the whole thing. More efficient.

### Who Uses This

- **Amazon**: Cart in Redis for fast add/remove
- **Walmart**: Shopping cart with cross-device sync
- **Etsy**: Cart persistence for logged-in and guest users

---

## 12. API Response Caching with CDN Fallback

### The Story

You run a public API. Responses are expensive to compute. You want two layers: L1 = Redis (fast, in your datacenter). L2 = CDN (CloudFront, Cloudflare) at the edge. If Redis has it, serve from Redis. If not, compute, store in Redis, and cache at CDN. Next request from another region might hit the CDN first. Even faster.

### The Redis Solution

Redis is your L1 cache. CDN is L2. The flow:

1. Request arrives. Check CDN (via Cache-Control headers or CDN config).
2. CDN miss → Request hits your origin server.
3. Origin checks Redis: `GET api:products:list:v2`
4. Redis hit → Return from Redis. Set CDN cache headers (e.g., max-age=300).
5. Redis miss → Compute response (query DB, aggregate). Store in Redis: `SETEX api:products:list:v2 300 '{"data":[...]}'`
6. Return to client. CDN caches the response. Next request from same region hits CDN. No origin. No Redis.

```
Request from Delhi:
  CDN (Mumbai edge) → miss → Origin → Redis miss → DB → Store Redis → Return
  CDN caches it.

Next request from Delhi:
  CDN (Mumbai edge) → HIT → Return (no origin, no Redis)

Request from Bangalore:
  CDN (Bangalore edge) → miss → Origin → Redis HIT → Return
  CDN caches it.
```

### How It Works Step by Step

1. Client requests `GET /api/products?category=electronics`
2. Request goes to CDN. CDN has a cache key: `/api/products?category=electronics`
3. CDN miss. Request forwarded to origin (your API server).
4. API server: `GET cache:api:products:category:electronics` from Redis.
5. Redis hit: Return cached JSON. Add header `Cache-Control: public, max-age=300`
6. Redis miss: Query DB. Build response. `SETEX cache:api:products:category:electronics 300 '{"data":[...]}'`
7. Return response with Cache-Control. CDN stores it for 5 minutes.
8. Next request (same URL, within 5 min): CDN serves from edge. Origin never hit.

### Anti-Patterns

- **Don't cache user-specific responses at CDN.** CDN is shared. Cache only public, non-personalized data.

- **Don't forget to invalidate.** When products change, invalidate Redis and CDN. Use cache keys with version numbers or tags.

- **Don't set CDN TTL longer than Redis TTL.** If Redis expires first, origin will recompute. That's fine. But if CDN TTL > Redis TTL, you might serve stale data from CDN after Redis has refreshed.

### Who Uses This

- **Stripe**: API response caching with edge caching
- **GitHub API**: Cached responses at edge and origin
- **Content APIs**: News, weather, stock data with CDN + Redis

---

## Use Case Decision Matrix

| I need to... | Use Redis... |
|-------------|-------------|
| Speed up database reads | Caching (Strings/Hashes with TTL) |
| Store user login state | Sessions (Strings with TTL) |
| Limit API abuse | Rate limiting (INCR + EXPIRE) |
| Rank players / items | Leaderboards (Sorted Sets) |
| Count page views, likes | Counters (INCR) |
| Prevent duplicate actions | Distributed Locks (SET NX EX) |
| Simple task queue | Lists (LPUSH / BRPOP) |
| Broadcast real-time updates | Pub/Sub |
| Find nearby drivers/stores | Geospatial (GEO commands) |
| Toggle features on/off | Feature flags (Strings) |
| Store shopping cart | Hashes (HSET, HGETALL, HDEL) |
| Cache API responses with CDN | Strings + CDN as L2 cache |

---

## Quick Summary

| Use Case | Redis Feature | Companies |
|----------|--------------|-----------|
| Caching | Strings, Hashes, TTL | Amazon, Netflix, Flipkart, Walmart |
| Sessions | Strings with TTL | GitHub, Shopify, Twitter |
| Rate Limiting | INCR + EXPIRE | GitHub API, Twitter API, Stripe, Twilio |
| Leaderboards | Sorted Sets | PUBG, Stack Overflow, Duolingo, Discord |
| Counters | INCR, HyperLogLog | YouTube, Instagram, Twitter, Medium |
| Dist. Locks | SET NX EX, Redlock | Payment systems, inventory |
| Task Queues | Lists (LPUSH/BRPOP) | Celery, Sidekiq, Bull |
| Real-time Broadcast | Pub/Sub | Cricbuzz, chat apps, sports betting |
| Geospatial | GEO commands | Uber, Swiggy, Ola, Pokemon GO |
| Feature Flags | Strings | Netflix, Spotify, every modern company |
| Shopping Cart | Hashes | Amazon, Walmart, Etsy |
| API + CDN Cache | Strings + CDN L2 | Stripe, GitHub API |

---

## Exercise

1. Build a page view counter for 3 pages. Increment them randomly. Display the counts.
2. Implement rate limiting: Allow 5 operations in 30 seconds. Test by running 10 operations quickly.
3. Create a leaderboard with 5 players. Find the top 3. Find a specific player's rank.
4. Use `GEOADD` to add 3 locations and find which ones are within 1 km of a point.

---

**Next up:** [Performance & Best Practices](performance-best-practices.md) — Make your Redis usage production-grade.
