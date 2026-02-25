# Project: Redis Session Storage

## Why Default Sessions Are a Problem

When you build a web application with Spring Boot, sessions work out of the box. A user logs in, and the server stores their session data (cart, preferences, etc.) in memory. Simple. But here's the catch: **that memory lives inside your application server**.

### The Server Restart Problem

You deploy your app. Users log in, add items to their cart, and browse. Then you need to deploy a new version. You restart the server. **Poof.** Every session is gone. Every user is logged out. Every shopping cart is empty. Your users are confused and frustrated.

!!! warning "Real Impact"
    Imagine a user spent 20 minutes building a cart with 15 items. You push a hotfix. Server restarts. Their cart is empty. They might abandon the purchase entirely.

### The Load Balancing Problem

Now imagine you have **two** servers behind a load balancer to handle traffic. User logs in on Server 1. Their session is stored in Server 1's memory. Next request? The load balancer might send them to Server 2. Server 2 has no idea who this user is—it never saw the login. **Session not found.** User appears logged out. This is called the "sticky session" problem—you'd need to configure the load balancer to always send the same user to the same server, which is fragile and doesn't scale well.

### The Memory Problem

Sessions in memory consume RAM. 10,000 active users with 5KB of session data each = 50MB. 100,000 users = 500MB. Your application server is now competing with your actual application logic for memory. And if your server runs out of memory, it crashes—taking all sessions with it.

**The solution:** Store sessions in Redis. Redis is built for this. It's fast. It persists. And every server instance can read from the same Redis—so it doesn't matter which server handles the request.

---

## The Problem Visualized: In-Memory Sessions Across Multiple Servers

```
                    WITHOUT REDIS (In-Memory Sessions)
                    ═══════════════════════════════════

    User (Browser)              Load Balancer              Server 1              Server 2
         │                            │                        │                      │
         │  Request 1 (Login)         │                        │                      │
         │ ─────────────────────────▶│ ──────────────────────▶│                      │
         │                            │                        │ Session A created     │
         │                            │                        │ (in Server 1 RAM)     │
         │  Response + Cookie         │                        │                      │
         │ ◀─────────────────────────│ ◀──────────────────────│                      │
         │                            │                        │                      │
         │  Request 2 (Add to cart)   │                        │                      │
         │ ─────────────────────────▶│ ─────────────────────────────────────────────▶│
         │                            │                        │                      │ Session?
         │                            │                        │                      │ NOT FOUND!
         │                            │                        │                      │ User appears
         │  401 Unauthorized          │                        │                      │ logged out! ❌
         │ ◀─────────────────────────│ ◀────────────────────────────────────────────│
```

**What went wrong?** Request 2 went to Server 2. Server 2 never saw the login. It has no session for this user.

```
                    WITH REDIS (Shared Session Store)
                    ═══════════════════════════════════

    User (Browser)              Load Balancer              Server 1              Server 2              Redis
         │                            │                        │                      │                    │
         │  Request 1 (Login)         │                        │                      │                    │
         │ ─────────────────────────▶│ ──────────────────────▶│                      │                    │
         │                            │                        │ Create session       │                    │
         │                            │                        │ ────────────────────────────────────────▶│
         │                            │                        │                      │                    │ Store
         │  Response + Cookie         │                        │                      │                    │
         │ ◀─────────────────────────│ ◀──────────────────────│                      │                    │
         │                            │                        │                      │                    │
         │  Request 2 (Add to cart)   │                        │                      │                    │
         │ ─────────────────────────▶│ ─────────────────────────────────────────────▶│                    │
         │                            │                        │                      │ Read session       │
         │                            │                        │                      │ ──────────────────▶│
         │                            │                        │                      │ ◀─────────────────│
         │                            │                        │                      │ Session found! ✅  │
         │  Cart updated              │                        │                      │                    │
         │ ◀─────────────────────────│ ◀────────────────────────────────────────────│                    │
```

**What's different?** Both servers read from Redis. The session lives in Redis, not in any server's memory. Request 2 hits Server 2, Server 2 asks Redis "do you have session XYZ?", Redis says yes, and the user's cart is there.

---

## Spring Session + Redis: How It Integrates

Spring Session is a clever piece of middleware. It sits between your application and the default session storage. Instead of storing sessions in a `HttpSession` backed by server memory, it stores them in Redis.

**The magic:** Your code doesn't change. You still use `HttpSession session` in your controllers. You still call `session.setAttribute("cart", cart)`. Spring Session intercepts these calls and redirects them to Redis. You get the same API with a different backend.

### Integration Steps (High Level)

1. **Add dependencies** — `spring-session-data-redis` and `spring-boot-starter-data-redis`
2. **Configure Redis** — host, port in `application.properties`
3. **Enable Redis HTTP Session** — `@EnableRedisHttpSession` on your main class
4. **That's it** — All `HttpSession` operations now go to Redis automatically

---

## Step 1: Dependencies (pom.xml)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.session</groupId>
        <artifactId>spring-session-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**What's happening here?**

- `spring-session-data-redis` — The bridge. It replaces the default session store with Redis.
- `spring-boot-starter-data-redis` — Redis client. Spring Session uses it to talk to Redis.
- `spring-boot-starter-security` — We need authentication for our demo. In a real app, you'd use proper login (e.g., form login, OAuth).

---

## Step 2: application.properties

```properties
# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Session stored in Redis
spring.session.store-type=redis
spring.session.redis.namespace=myapp:session
server.servlet.session.timeout=30m

# Basic security user (for demo)
spring.security.user.name=admin
spring.security.user.password=password123
```

**What's happening here?**

- `spring.session.store-type=redis` — Explicitly tell Spring to use Redis for sessions. (Spring Boot might auto-detect this when both dependencies are present.)
- `spring.session.redis.namespace=myapp:session` — All session keys in Redis will be prefixed with `myapp:session:`. This lets you run multiple apps against the same Redis without key collisions.
- `server.servlet.session.timeout=30m` — Session expires after 30 minutes of inactivity. Redis will automatically delete the key when TTL expires.

---

## Step 3: Enable Redis Session

```java
@SpringBootApplication
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionApp {
    public static void main(String[] args) {
        SpringApplication.run(SessionApp.class, args);
    }
}
```

**What's happening here?**

- `@EnableRedisHttpSession` — Swaps the default session store for Redis. All `HttpSession` operations now go to Redis.
- `maxInactiveIntervalInSeconds = 1800` — 1800 seconds = 30 minutes. If the user doesn't make a request for 30 minutes, the session expires. Redis TTL is set accordingly.

---

## Step 4: Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

**What's happening here?**

- `/api/public/**` — No auth required. Good for health checks.
- `anyRequest().authenticated()` — Everything else needs login.
- `httpBasic` — We use HTTP Basic Auth for the demo (username/password in header). In production, you'd use form login or JWT.

---

## Step 5: Session Controller

```java
@RestController
@RequestMapping("/api/session")
public class SessionController {

    @GetMapping("/info")
    public Map<String, Object> sessionInfo(HttpSession session) {
        Map<String, Object> info = new HashMap<>();
        info.put("sessionId", session.getId());
        info.put("creationTime",
            new Date(session.getCreationTime()));
        info.put("lastAccessedTime",
            new Date(session.getLastAccessedTime()));
        info.put("maxInactiveInterval",
            session.getMaxInactiveInterval());
        return info;
    }

    @PostMapping("/set")
    public String setAttribute(
            HttpSession session,
            @RequestParam String key,
            @RequestParam String value) {

        session.setAttribute(key, value);
        return "Stored: " + key + " = " + value;
    }

    @GetMapping("/get")
    public String getAttribute(
            HttpSession session,
            @RequestParam String key) {

        Object value = session.getAttribute(key);
        return value != null ? value.toString() : "Not found";
    }

    @PostMapping("/cart/add")
    public Map<String, Object> addToCart(
            HttpSession session,
            @RequestParam String product,
            @RequestParam int quantity) {

        @SuppressWarnings("unchecked")
        Map<String, Integer> cart = (Map<String, Integer>)
            session.getAttribute("cart");

        if (cart == null) {
            cart = new HashMap<>();
        }

        cart.put(product, cart.getOrDefault(product, 0) + quantity);
        session.setAttribute("cart", (Serializable) cart);

        return Map.of(
            "message", "Added to cart",
            "cart", cart,
            "sessionId", session.getId()
        );
    }

    @GetMapping("/cart")
    public Map<String, Object> getCart(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> cart = (Map<String, Integer>)
            session.getAttribute("cart");

        return Map.of(
            "cart", cart != null ? cart : Map.of(),
            "sessionId", session.getId()
        );
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "Session destroyed!";
    }
}
```

**What's happening here?**

- `session.setAttribute()` / `getAttribute()` — Standard servlet API. Spring Session intercepts these and stores/retrieves from Redis.
- Cart is stored as a `Map<String, Integer>`. It must be `Serializable` for Redis storage.
- `session.invalidate()` — Deletes the session from Redis. User is logged out.

---

## Walk Through the Login Flow

Let's trace what happens when a user logs in and adds items to their cart.

### Step 1: User Sends First Authenticated Request

```
curl -u admin:password123 http://localhost:8080/api/session/info
```

**What happens:**

1. Spring Security validates the credentials (admin/password123).
2. Security creates a new session (or reuses one if it exists).
3. Spring Session creates a session in Redis with a unique ID (e.g., `a1b2c3d4-e5f6-7890-abcd-ef1234567890`).
4. Response includes a `Set-Cookie` header: `SESSION=a1b2c3d4-e5f6-7890-abcd-ef1234567890`.
5. The browser stores this cookie for future requests.

### Step 2: What the Session Looks Like in Redis

Spring Session stores the session as a Redis Hash. The key format is:

```
myapp:session:sessions:<session-id>
```

For example: `myapp:session:sessions:a1b2c3d4-e5f6-7890-abcd-ef1234567890`

The hash contains serialized session attributes. When you add to the cart, the `cart` attribute is stored in this hash.

### Step 3: Subsequent Requests

```
curl -u admin:password123 -X POST \
  "http://localhost:8080/api/session/cart/add?product=Laptop&quantity=1"
```

**What happens:**

1. Browser sends the request with the `SESSION` cookie.
2. Spring Session reads the cookie, extracts the session ID.
3. Spring Session looks up `myapp:session:sessions:a1b2c3d4-...` in Redis.
4. Session found! Attributes (including cart) are loaded.
5. Controller adds Laptop to cart, calls `session.setAttribute("cart", cart)`.
6. Spring Session writes the updated cart back to Redis.
7. Response returned. Cart now has `{"Laptop": 1}`.

---

## The Big Test: Restart the Server

This is the moment of truth. With in-memory sessions, a restart would wipe everything. With Redis, the session survives.

### Step-by-Step

1. **Start Redis and the app:**
   ```bash
   docker run --name redis -p 6379:6379 -d redis
   mvn spring-boot:run
   ```

2. **Login and add to cart** (use the same curl commands with `-u admin:password123`):
   ```bash
   curl -u admin:password123 http://localhost:8080/api/session/info
   curl -u admin:password123 -X POST "http://localhost:8080/api/session/cart/add?product=Laptop&quantity=1"
   curl -u admin:password123 -X POST "http://localhost:8080/api/session/cart/add?product=Mouse&quantity=2"
   curl -u admin:password123 http://localhost:8080/api/session/cart
   ```
   You should see: `{"cart":{"Laptop":1,"Mouse":2}, "sessionId":"..."}`

3. **Copy the session ID** from the response (or keep the cookie). You'll need it for the next step.

4. **Stop the Spring Boot app** (Ctrl+C in the terminal).

5. **Start it again:**
   ```bash
   mvn spring-boot:run
   ```

6. **Make the same cart request again** (with the same credentials—the session cookie is tied to the session ID stored in Redis):
   ```bash
   curl -u admin:password123 -b "SESSION=<your-session-id>" http://localhost:8080/api/session/cart
   ```
   Or simply use the `-u` flag again—with HTTP Basic Auth, each request creates/uses a session. If you're using a browser, the cookie persists automatically. Just refresh the cart endpoint.

!!! example "Expected Result"
    **Your cart is still there!** `{"Laptop":1,"Mouse":2}`. The session survived the server restart because it lived in Redis, not in the server's memory.

---

## How to Inspect Sessions in Redis

Connect to Redis and explore what Spring Session stores:

```bash
redis-cli

# List all session keys (your namespace is myapp:session)
KEYS myapp:session:*

# Example output:
# 1) "myapp:session:sessions:expires:a1b2c3d4-e5f6-7890-abcd-ef1234567890"
# 2) "myapp:session:sessions:a1b2c3d4-e5f6-7890-abcd-ef1234567890"

# View the session data (it's a hash)
HGETALL "myapp:session:sessions:a1b2c3d4-e5f6-7890-abcd-ef1234567890"

# Example output (simplified):
# 1) "sessionAttr:cart"
# 2) "\xac\xed\x00\x05sr\x00\x0ejava.util.HashMap..."  (serialized Java object)
# 3) "creationTime"
# 4) "1730000000000"
# 5) "lastAccessedTime"
# 6) "1730000100000"
# 7) "maxInactiveInterval"
# 8) "1800"

# Check TTL (time to live) - how long until session expires
TTL "myapp:session:sessions:a1b2c3d4-e5f6-7890-abcd-ef1234567890"
# Returns: 1742  (seconds remaining)
```

!!! tip "Session Expiry Keys"
    Spring Session uses a separate key `myapp:session:sessions:expires:<id>` for expiry. Redis expires this key, and Spring Session uses that to trigger session cleanup. The main session hash may stay a bit longer; the expires key is what Redis actually deletes.

---

## Session Security Considerations

| Concern | Mitigation |
|---------|------------|
| **Session fixation** | Spring Security rotates the session ID on login. Always call `session.invalidate()` on logout. |
| **Session hijacking** | Use HTTPS. The session cookie should have `Secure` and `HttpOnly` flags. |
| **Session ID predictability** | Spring generates random UUIDs. Don't use custom predictable IDs. |
| **Sensitive data in session** | Avoid storing passwords or tokens. Store minimal data (e.g., user ID, cart). |
| **Redis network exposure** | Don't expose Redis to the public internet. Use a private network or VPN. |

!!! warning "Production Checklist"
    - Enable HTTPS and set `server.servlet.session.cookie.secure=true`
    - Set `server.servlet.session.cookie.http-only=true` to prevent XSS from reading the cookie
    - Use a strong `server.servlet.session.cookie.same-site=strict` to mitigate CSRF
    - Consider Redis password protection: `spring.data.redis.password=yourpassword`

---

## Scaling: How This Works with Multiple Server Instances

```
                    ┌─────────────────────────────────────────────────────────┐
                    │           MULTIPLE SERVERS, ONE REDIS                    │
                    └─────────────────────────────────────────────────────────┘

                         ┌─────────────┐
                         │   Redis     │
                         │  (Sessions) │
                         └──────┬──────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                 │
              ▼                 ▼                 ▼
        ┌──────────┐      ┌──────────┐      ┌──────────┐
        │ Server 1 │      │ Server 2 │      │ Server 3 │
        └────┬─────┘      └────┬─────┘      └────┬─────┘
             │                 │                 │
             └─────────────────┼─────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Load Balancer     │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Users (Browsers)   │
                    └─────────────────────┘
```

**How it works:**

- User A's request goes to Server 1. Server 1 reads session from Redis. Session found.
- User A's next request goes to Server 3 (load balancer round-robins). Server 3 reads the same session from Redis. Session found.
- No sticky sessions needed. Any server can serve any user. Redis is the single source of truth.

**Considerations:**

- Redis becomes a critical dependency. If Redis goes down, no one can log in or access sessions. Use Redis Sentinel or Redis Cluster for HA.
- Network latency: Each request does at least one Redis read. Keep Redis close to your app servers (same data center).

---

## Step 6: Public Endpoint (No Auth)

```java
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/health")
    public String health() {
        return "Service is running!";
    }
}
```

---

## Step 7: Test It

### Start Redis

```bash
docker run --name redis -p 6379:6379 -d redis
```

### Run the App

```bash
mvn spring-boot:run
```

### Test Commands

```bash
# Public endpoint (no auth needed)
curl http://localhost:8080/api/public/health

# Session info (with basic auth)
curl -u admin:password123 http://localhost:8080/api/session/info

# Store data in session
curl -u admin:password123 -X POST \
  "http://localhost:8080/api/session/set?key=username&value=Satjit"

# Retrieve data from session
curl -u admin:password123 \
  "http://localhost:8080/api/session/get?key=username"

# Add to cart
curl -u admin:password123 -X POST \
  "http://localhost:8080/api/session/cart/add?product=Laptop&quantity=1"

curl -u admin:password123 -X POST \
  "http://localhost:8080/api/session/cart/add?product=Mouse&quantity=2"

# View cart
curl -u admin:password123 http://localhost:8080/api/session/cart

# Logout (destroy session)
curl -u admin:password123 -X POST http://localhost:8080/api/session/logout
```

---

## Quick Summary

| Concept | Implementation |
|---------|-----------------|
| Store sessions in Redis | `spring-session-data-redis` |
| Session auto-expiry | `maxInactiveIntervalInSeconds` |
| Session data persistence | Survives server restarts |
| Shared sessions | Works across multiple server instances |
| Session attributes | `session.setAttribute()` / `getAttribute()` |

| Problem | Redis Solution |
|---------|----------------|
| Server restart = sessions lost | Redis persists sessions |
| Multiple servers = session not found | All servers share Redis |
| Too many sessions = out of memory | Redis handles millions of sessions |
| Session expiry | Redis TTL auto-expires sessions |

---

**Next project:** [Kafka Order Processing](kafka-order-processing.md)
</think>
Fixing a typo in the Session Controller.
<｜tool▁calls▁begin｜><｜tool▁call▁begin｜>
Read