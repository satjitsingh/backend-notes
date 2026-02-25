# TTL & Expiration

## Why Does Data Need to Expire?

Not all data should live forever. Think about it:

- An **OTP** sent to your phone is valid for 5 minutes. After that, it should disappear.
- A **session token** should expire after 30 minutes of inactivity. Otherwise, anyone who finds your laptop open can use your account forever.
- A **cached product listing** should refresh every 15 minutes so users see the latest prices.
- A **rate limit counter** ("user has made 8 of 10 allowed requests this minute") should reset every minute.

If you had to manually track all these timers and delete data yourself, your code would be a mess. Redis solves this with **TTL (Time To Live)** — you tell Redis "keep this data for X seconds, then delete it automatically." Redis handles the countdown for you.

---

## What is TTL?

**TTL = Time To Live.** It's a countdown timer attached to a key. When the timer hits zero, Redis automatically deletes the key. Gone. No trace. No cleanup code needed on your side.

### Real-Life Analogy: Parking Meter

Think of a parking meter. You insert a coin and get 60 minutes. The meter counts down. When it hits zero, your parking "expires" — you get a ticket. You don't have to remember to move your car at exactly the right time; the meter enforces the deadline.

Redis TTL works the same way. You set a key with a value and a timer. Redis counts down. When the timer hits zero, the key is deleted. You don't have to remember to delete it yourself.

```
SET parking:car42 "occupied"
EXPIRE parking:car42 3600     ← 60 minutes. After that, automatically deleted.

Time passes... 59 minutes later:
TTL parking:car42              ← Returns: 60 (seconds remaining)

Time passes... 1 more minute:
GET parking:car42              ← Returns: (nil) — it's gone!
```

---

## Core TTL Commands

### EXPIRE — Set a timer on an existing key

You already have a key. Now you want to add a countdown.

```bash
SET user:session:abc "user_data_here"
EXPIRE user:session:abc 1800
```

This says: "The key `user:session:abc` should be deleted in 1800 seconds (30 minutes)."

!!! info "When to use EXPIRE"
    Use `EXPIRE` when you've already stored a key (maybe with `SET`, `HSET`, `LPUSH`, etc.) and want to add a timer after the fact.

### TTL — Check how much time is left

```bash
TTL user:session:abc
# (integer) 1750    ← 1750 seconds remaining
```

What TTL returns:

| Return Value | Meaning |
|-------------|---------|
| Positive number | Seconds remaining before expiry |
| `-1` | Key exists but has **no expiry** (lives forever) |
| `-2` | Key does **not exist** (already expired or never created) |

This is useful for debugging. "Why is this cache returning nil? Oh, TTL says -2. It expired."

### PTTL — Same as TTL, but in milliseconds

```bash
PTTL user:session:abc
# (integer) 1750432    ← 1,750,432 milliseconds remaining
```

Use this when you need precision — for example, rate limiting where you care about exact millisecond timing.

### SETEX — Set value + expiry in one command

Instead of doing `SET` and then `EXPIRE` separately (two commands, two network round trips), combine them:

```bash
SETEX otp:9876543210 300 "482910"
```

This does three things at once:

1. Creates the key `otp:9876543210`
2. Sets its value to `"482910"`
3. Sets it to expire in 300 seconds (5 minutes)

!!! tip "Why SETEX is better than SET + EXPIRE"
    With `SET` followed by `EXPIRE`, there's a tiny window between the two commands where the key exists **without** an expiry. If your app crashes between `SET` and `EXPIRE`, the key lives forever. With `SETEX`, it's atomic — either both happen or neither happens. Always prefer `SETEX` for data that must expire.

### PSETEX — Same as SETEX, but milliseconds

```bash
PSETEX flash:sale:banner 30000 "50% OFF!"
```

Expires in 30,000 milliseconds (30 seconds). Good for very short-lived data.

### SET with EX/PX options — The modern way

In newer Redis versions, `SET` itself supports expiry flags:

```bash
SET otp:9876543210 "482910" EX 300         # Expires in 300 seconds
SET otp:9876543210 "482910" PX 300000      # Expires in 300,000 milliseconds
```

You can also use `NX` (only set if key doesn't exist) and `XX` (only set if key already exists):

```bash
SET lock:order:123 "server-1" EX 10 NX
# Sets the key ONLY if it doesn't exist. Expires in 10 seconds.
# This is how you build a distributed lock!
```

### PERSIST — Remove the expiry (make it live forever)

Changed your mind? Want to keep the key?

```bash
SET temp "data"
EXPIRE temp 60       # Will expire in 60 seconds

PERSIST temp         # Cancel the expiry! Now it lives forever.
TTL temp             # Returns -1 (no expiry)
```

### EXPIREAT — Set expiry at a specific Unix timestamp

Instead of "expire in 300 seconds," you say "expire at exactly this moment in time."

```bash
EXPIREAT event:ticket:99 1740500000
# Expires at Unix timestamp 1740500000 (a specific date/time)
```

This is useful for things like "this coupon is valid until midnight on March 1st."

---

## How Redis Handles Expiration Internally

This is important to understand — and it comes up in interviews.

Redis doesn't set a real "timer" for every single key. If you have 10 million keys with TTL, Redis doesn't maintain 10 million countdown timers. That would be too expensive. Instead, Redis uses two strategies:

### Strategy 1: Lazy Deletion (Passive)

When someone tries to access a key, Redis checks: "Has this key expired?" If yes, Redis deletes it right then and returns `(nil)`. If no one ever accesses the key again, it just sits there taking up memory — until Strategy 2 kicks in.

```
Client: GET expired:key
Redis thinks: "Let me check... this key expired 5 minutes ago."
Redis: *deletes key* → returns (nil) to client
```

### Strategy 2: Active Cleanup (Periodic)

Every 100 milliseconds, Redis runs a cleanup cycle:

1. Randomly pick 20 keys that have TTL set.
2. Delete the ones that have expired.
3. If more than 25% of the sampled keys were expired, repeat immediately.

This means expired keys might linger in memory for a short time after they expire. But Redis catches up quickly.

```
┌───────────────────────────────────────────────┐
│          How Redis Expires Keys               │
│                                               │
│  1. Lazy: Check on access → delete if expired │
│  2. Active: Every 100ms, sample & delete      │
│                                               │
│  Result: Most expired keys are cleaned up     │
│  within a few hundred milliseconds            │
└───────────────────────────────────────────────┘
```

!!! tip "Interview Insight"
    If an interviewer asks "How does Redis handle key expiration?", say: "Redis uses two strategies. Lazy deletion — it checks the TTL when a key is accessed and deletes it if expired. And active expiration — every 100ms, Redis randomly samples keys with TTL and deletes the expired ones. This combination keeps memory usage under control without the overhead of per-key timers."

---

## Real-World TTL Scenarios

### Scenario 1: OTP Verification

When a user requests a login OTP, you generate a random code and store it in Redis with a 5-minute TTL.

```bash
SETEX otp:9876543210 300 "482910"
```

When the user submits the OTP:

```java
String storedOtp = redis.get("otp:" + phoneNumber);
if (storedOtp == null) {
    return "OTP expired. Request a new one.";
}
if (!storedOtp.equals(submittedOtp)) {
    return "Wrong OTP.";
}
redis.del("otp:" + phoneNumber);
return "Verified!";
```

If the user doesn't enter the OTP within 5 minutes, it's gone. No cleanup code needed.

### Scenario 2: Rate Limiting (Sliding Window)

You want to allow 100 API requests per user per minute.

```bash
# When a request comes in:
INCR rate:user:42          # Increment counter
TTL rate:user:42           # Check if timer exists

# If TTL is -1 (no expiry), this is the first request:
EXPIRE rate:user:42 60     # Set 60-second window

# If counter > 100:
# Reject the request with 429 Too Many Requests
```

Better approach using `SETEX` pattern:

```java
String key = "rate:" + userId;
Long count = redis.incr(key);
if (count == 1) {
    redis.expire(key, 60);  // First request, start the window
}
if (count > 100) {
    throw new RateLimitExceededException();
}
```

After 60 seconds, the key auto-deletes. Counter resets. Fresh window starts.

### Scenario 3: User Session

```bash
SETEX session:token:abc123def 1800 '{"userId":42,"role":"admin"}'
```

- User logs in. Session stored with 30-minute TTL.
- Every time the user does something, you **extend** the session:

```bash
EXPIRE session:token:abc123def 1800
# Resets the 30-minute timer. Like feeding a parking meter.
```

- If the user goes inactive for 30 minutes, the session auto-expires. They have to log in again.

### Scenario 4: Distributed Lock

You need to ensure only one server processes a job at a time.

```bash
SET lock:email:job "server-1" EX 30 NX
# NX = only set if key doesn't exist
# EX 30 = expire in 30 seconds (safety net)
```

If `server-1` crashes, the lock automatically expires in 30 seconds. Another server can take over. Without TTL, the lock would be stuck forever (deadlock).

### Scenario 5: Flash Sale Banner

```bash
PSETEX banner:flash:sale 7200000 '{"text":"70% OFF!","color":"red"}'
# 7,200,000 ms = 2 hours
```

The banner shows for exactly 2 hours, then disappears. Marketing doesn't need to remember to remove it.

---

## TTL Recommendations by Data Type

| Data Type | Suggested TTL | Why |
|-----------|--------------|-----|
| OTP | 3-5 minutes | Security. Must expire quickly. |
| User session | 15-30 minutes | Standard session timeout |
| API cache (product list) | 5-15 minutes | Data changes occasionally |
| API cache (config) | 1-24 hours | Rarely changes |
| Rate limit counter | 1 minute | Reset window |
| Distributed lock | 10-60 seconds | Safety net for crashes |
| Real-time stock price | 5-10 seconds | Changes constantly |
| Static page content | 1-6 hours | Very rarely changes |
| Temporary calculation result | 5-30 minutes | Saves re-computation |

!!! warning "Don't set TTL too high"
    A TTL of 24 hours for frequently changing data means users might see stale data for up to 24 hours. Choose TTL based on how often the underlying data changes AND how much staleness your users can tolerate.

!!! warning "Don't set TTL too low"
    A TTL of 5 seconds for a cache that takes 200ms to rebuild means you're rebuilding it 12 times per minute. If the data doesn't change that fast, you're wasting resources. Find the sweet spot.

---

## Common TTL Pitfalls

### Pitfall 1: Forgetting to set TTL

You cache a user's profile but forget `EXPIRE`. The profile data changes, but the cache never expires. User sees stale data forever.

**Fix:** Always use `SETEX` or `SET ... EX` instead of plain `SET` for cache data.

### Pitfall 2: All keys expire at the same time

You load 10,000 products into cache with `TTL = 3600`. They were all loaded at the same time, so they all expire at the same time. Suddenly, 10,000 requests hit the database simultaneously.

**Fix:** Add random jitter: `TTL = 3600 + random(0, 300)`. Each key expires at a slightly different time.

### Pitfall 3: Extending TTL on every read

If you reset TTL every time someone reads a key, popular data never expires. Sounds good, right? But if the underlying data changes and you rely on TTL for freshness, the cache will be stale indefinitely.

**Fix:** Only extend TTL for session-like data (where continued activity should extend the lifetime). For cache data, let it expire naturally.

### Pitfall 4: Using TTL as the only invalidation strategy

TTL is a safety net, not a precision tool. If a product price changes from 500 to 400, waiting for TTL to expire means some users see 500 and some see 400.

**Fix:** Use event-based invalidation (delete cache on update) + TTL as a backup.

### Pitfall 5: TTL on complex data structures

You use `HSET` to add fields to a hash, then `EXPIRE` on the key. Later you use `HINCRBY` to increment a field. In Redis, some commands like `HINCRBY` do not reset the TTL. But `EXPIRE` on an existing key replaces the TTL. If you forget to set TTL after creating a new hash key, it lives forever.

**Fix:** Use a wrapper that always sets TTL when creating keys. Or use `SET` with `EX` for simple values. For hashes, call `EXPIRE` right after the initial `HSET`.

### Pitfall 6: Relying on exact expiry timing

Redis does not guarantee that a key is deleted at exactly the second TTL hits zero. Lazy deletion means it might linger until someone accesses it. Active cleanup runs every 100ms and samples randomly. For critical "expire at exactly midnight" use cases, you need application-level checks.

**Fix:** For time-sensitive expiry (e.g., coupon valid until date X), check the expiry in your application logic. Use Redis TTL as a hint, not the sole authority.

---

## TTL in Spring Boot

Spring Boot's caching abstraction works with Redis through `RedisCacheManager`. By default, cached entries do not expire. You must configure TTL explicitly.

### How @Cacheable and RedisCacheConfiguration Handle TTL

When you use `@Cacheable`, Spring stores the result in the configured cache. For Redis, that means a key-value pair. TTL is controlled by `RedisCacheConfiguration`.

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))  // Default TTL for all caches
        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

    Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
    cacheConfigs.put("products", defaultConfig.entryTtl(Duration.ofMinutes(5)));
    cacheConfigs.put("config", defaultConfig.entryTtl(Duration.ofHours(24)));

    return RedisCacheManager.builder(factory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigs)
        .build();
}
```

### Per-Cache TTL Configuration in Java

You can set different TTLs per cache name. Products might need 5 minutes. Config might need 24 hours.

```java
cacheConfigs.put("products", defaultConfig.entryTtl(Duration.ofMinutes(5)));
cacheConfigs.put("sessions", defaultConfig.entryTtl(Duration.ofMinutes(30)));
cacheConfigs.put("config", defaultConfig.entryTtl(Duration.ofHours(24)));
```

!!! tip "Spring Cache TTL"
    If you don't set `entryTtl`, Redis keys created by Spring Cache never expire. Always configure TTL for production caches.

---

## TTL Patterns

Different use cases need different TTL behavior. Here are three common patterns.

### Sliding Window TTL (Extend on Every Access)

The TTL resets every time someone reads or writes the key. Good for sessions. "If the user is active, keep the session alive."

```bash
# Every request: extend the session
EXPIRE session:token:abc123 1800
```

In code: on every authenticated request, call `EXPIRE` with the same duration. Inactive users lose their session. Active users stay logged in.

!!! info "Use case"
    User sessions, API rate limit windows (sliding), "last seen" timestamps.

### Fixed Window TTL (Don't Extend)

The TTL is set once and never changes. Good for OTPs, one-time codes, flash sale banners.

```bash
SETEX otp:9876543210 300 "482910"
# No EXPIRE on read. Key dies in 5 minutes no matter what.
```

!!! info "Use case"
    OTP verification, rate limits (fixed window), temporary tokens, flash sales.

### Lazy Refresh (Refresh in Background Before Expiry)

When a key is about to expire, refresh it in the background. The user gets the cached value immediately. A background thread or async task fetches fresh data and updates the cache. Reduces latency spikes when TTL expires.

```java
// Pseudocode: if TTL < 60 seconds, trigger async refresh
long ttl = redis.ttl("product:42");
if (ttl > 0 && ttl < 60) {
    executor.submit(() -> {
        Product fresh = database.getProduct(42);
        redis.setex("product:42", 3600, serialize(fresh));
    });
}
return redis.get("product:42");  // Return current (maybe stale) value
```

!!! tip "When to use"
    High-traffic caches where you want to avoid cache stampede when TTL expires. The user gets fast response; freshness improves in the background.

---

## Monitoring TTL

In production, you need to know: Do any keys have no TTL? Are TTLs too long or too short?

### Finding Keys Without TTL

Use `OBJECT IDLETIME` to see how long a key has been idle. Keys with no TTL have `TTL` returning `-1`. To find them:

```bash
# Scan keys and check TTL (use with caution on large datasets)
redis-cli --scan --pattern "cache:*" | while read key; do
  ttl=$(redis-cli TTL "$key")
  if [ "$ttl" -eq -1 ]; then
    echo "No TTL: $key"
  fi
done
```

!!! warning "SCAN is not instant"
    On large datasets, scanning all keys can be slow. Run during low traffic. Consider sampling.

### Auditing TTL Values in Production

Log TTL distribution for important key patterns. For example, track how many `session:*` keys have TTL < 600 (10 min) vs > 1800 (30 min). This helps you tune session timeouts.

```bash
# Example: count keys by TTL bucket
redis-cli --scan --pattern "session:*" | head -1000 | while read key; do
  redis-cli TTL "$key"
done | sort | uniq -c
```

---

## Key Expiry Events

Redis can notify you when a key expires. Useful for cleanup callbacks, analytics, or triggering downstream actions.

### Keyspace Notifications

Enable keyspace notifications in `redis.conf`:

```conf
notify-keyspace-events Ex
```

`Ex` means: notify on key **e**xpiry events. (Use `A` for all events, but that's noisy.)

### Subscribing to Expiry Events

Expiry events are published to a special channel:

```
__keyevent@0__:expired
```

The message is the key name that expired. You subscribe with a Redis client:

```java
// Subscribe to expiry events
redisTemplate.execute((RedisCallback<Void>) connection -> {
    connection.subscribe((message, pattern) -> {
        String expiredKey = new String(message.getBody());
        log.info("Key expired: {}", expiredKey);
        // e.g., session cleanup: remove from local session store
    }, "__keyevent@0__:expired".getBytes());
    return null;
});
```

### Use Case: Session Cleanup Callback

When a session key expires, you might need to clean up related state: invalidate local caches, update "active users" count, or notify other services. Subscribe to `__keyevent@0__:expired` and filter for `session:*` keys. Run your cleanup logic when you receive the event.

!!! warning "Events are best-effort"
    Redis sends expiry events asynchronously. If the subscriber is down, events can be lost. Don't use this as the only source of truth for critical logic.

---

## Quick Summary

| Concept | Explanation |
|---------|-------------|
| TTL | Countdown timer. Key auto-deletes when it hits zero |
| EXPIRE | Add a TTL to an existing key |
| SETEX | Set value + TTL in one atomic command |
| TTL command | Check remaining seconds (-1 = no expiry, -2 = key gone) |
| PERSIST | Remove TTL, make key permanent |
| Lazy deletion | Redis checks on access, deletes if expired |
| Active cleanup | Redis samples and deletes expired keys every 100ms |
| Best practice | Use SETEX, add TTL jitter, combine TTL with event invalidation |

---

## Exercise

1. Store an OTP with a 2-minute TTL. Check the remaining time every 30 seconds until it expires.
2. Build a rate limiter: Allow yourself 5 `GET` requests in 30 seconds. Use `INCR` and `EXPIRE`.
3. Create a key, set TTL to 60 seconds, then use `PERSIST` to cancel it. Verify with `TTL`.
4. Create 3 keys with the same TTL. Why is this a potential problem in production? How would you fix it?

---

**Next up:** [Memory & Eviction Policies](memory-eviction.md) — What happens when Redis runs out of memory?
