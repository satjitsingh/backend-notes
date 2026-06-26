# Chapter 5: How The Web Works (Prerequisites)

[← Back to Index](00-README.md) | [Previous: HTTP & REST](04-Prerequisites-HTTP-REST.md) | [Next: Databases →](06-Prerequisites-Databases.md)

---

## Why This Chapter Matters

Think of building a Spring Boot app like building a toy robot. You can make the robot's arms and legs move (that's your code), but if you don't understand how electricity flows through the wires (how the web works), your robot might work on your desk but fail when you plug it in somewhere else.

**This chapter teaches you how the web actually works**—from the moment someone types a website address to the moment they see a response. As a Spring Boot developer, you'll build applications that live on this web. Understanding it makes you a better debugger, a smarter architect, and a developer who can fix problems when things break in production.

---

## Layer 1 — Intuition Builder

### Part 1: The Restaurant Analogy (Client-Server)

Imagine a **restaurant**:

- **You (the customer)** = **Client** (browser, phone app, or any program that asks for stuff)
- **The kitchen** = **Server** (a computer that has the stuff and sends it back)
- **Your order** = **Request** ("I'd like a pizza with cheese")
- **The food** = **Response** (the server gives you what you asked for)

The **client** asks. The **server** responds. That's it! That's the whole idea of the web.

```
    YOU                    RESTAURANT
   (Client)                 (Server)
      |                         |
      |  "I want pizza"         |
      |----------------------->|
      |                         |
      |   [Kitchen makes it]    |
      |                         |
      |   "Here's your pizza"   |
      |<-----------------------|
      |                         |
```

### Part 2: Addresses and Phone Books (DNS)

**Problem:** You want to visit "google.com" but computers only understand numbers like `142.250.80.46`. How does your computer find the right number?

**Analogy:** It's like a **phone book**. You know your friend's name ("Sarah") but not her number. So you look up "Sarah" in the phone book and find her number. **DNS is the internet's phone book.**

- **Domain name** (like google.com) = Your friend's name  
- **IP address** (like 142.250.80.46) = Their phone number  
- **DNS** = The book that translates names to numbers

**How it works (simplified):**

```
Your computer: "Where is google.com?"
      |
      v
[Check your own memory] → "I don't remember"
      |
      v
[Ask the phone company] → "We don't know, but ask the .com office"
      |
      v
[Ask the .com office] → "We don't know, but ask Google's office"
      |
      v
[Ask Google's office] → "google.com = 142.250.80.46"
      |
      v
Your computer: "Got it! I'll remember this for a while."
```

### Part 3: Apartments in a Building (IP Addresses and Ports)

Imagine a **big apartment building**:

- **IP address** = The building's street address (e.g., "123 Main Street")
- **Port** = The apartment number (e.g., "Apartment 443")

When a letter (your request) arrives, the address tells the mail carrier which building. The apartment number tells which door to deliver it to. Different "apartments" (ports) do different jobs:

| Port | What Lives There | Like... |
|------|------------------|---------|
| 80   | Regular web pages (HTTP) | The main lobby |
| 443  | Secure web pages (HTTPS) | The secure vault |
| 8080 | Your Spring Boot app! | A developer's office |
| 3306 | MySQL database | The filing room |
| 6379 | Redis cache | The quick-access shelf |

**Localhost** = "This building." When you run Spring Boot on your own computer, `localhost` means "my own computer." It's like saying "my house" instead of a real address—you're talking to yourself!

```
http://localhost:8080
       ^         ^
       |         |
   "My computer"  "Use apartment 8080"
```

**Two kinds of addresses (optional detail):**
- **Private IP** (e.g., 192.168.1.1) = Like your home address—only people inside your house/network know it
- **Public IP** (e.g., 203.0.113.1) = Like a business address—everyone on the internet can reach it

### Part 4: The Full Journey (What Happens When You Click a Link)

Let's follow a request from start to finish, like following a letter from your mailbox to its destination:

```
1. YOU type "https://api.example.com/users/123"
   └─> Your browser says: "I need to find api.example.com!"

2. DNS LOOKUP (phone book)
   └─> "api.example.com? That's at 203.0.113.1"

3. CONNECTION (knocking on the door)
   └─> Browser: "Hello, 203.0.113.1, can we talk?"
   └─> Server: "Sure! Here's a secure connection."

4. REQUEST (placing your order)
   └─> Browser sends: "GET /users/123" (I want user #123)

5. THE SERVER (the kitchen)
   └─> Server finds user 123
   └─> Prepares the response (like making your food)

6. RESPONSE (getting your order)
   └─> Server sends back: "Here's the user data!"

7. BROWSER (you receive it)
   └─> Browser shows you the result
```

**Simple analogies for the extra steps:**
- **Load balancer** = Receptionist who directs visitors to available staff
- **Reverse proxy** = Security guard who checks IDs, handles paperwork (SSL), and directs you to the right office
- **Application server** = The office worker who does the real work
- **Database** = The filing cabinet where records are stored

**In a big production system**, the journey has even more steps (like passing through a receptionist, then a guard, then the actual office):

```
┌─────────────────────────────────────────────────────────────────────┐
│              THE FULL REQUEST JOURNEY (Production)                   │
├─────────────────────────────────────────────────────────────────────┤
│  1. User types URL                                                  │
│  2. Browser checks cache → DNS lookup if needed                      │
│  3. DNS: Browser → OS → ISP → Root → TLD → Authoritative → IP       │
│  4. TCP handshake (connect to IP:443)                                │
│  5. TLS handshake (establish secure connection)                      │
│  6. HTTP request: GET /users/123                                     │
│  7. Load balancer → picks a healthy server, adds X-Forwarded-For    │
│  8. Reverse proxy (Nginx) → SSL offload, cache check, forwards       │
│  9. Spring Boot (Tomcat) → Controller → Service → Database           │
│ 10. Database → returns data                                          │
│ 11. Response travels back: DB → App → Nginx → LB → Browser           │
│ 12. Browser renders/caches result                                    │
└─────────────────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────────────────┐
│              THE REQUEST JOURNEY (Simplified)                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   YOU                    DNS                    SERVER               │
│    |                      |                        |                 │
│    |  "I want example.com"|                        |                 │
│    |--------------------->|                        |                 │
│    |  "It's at 203.x.x.1"|                        |                 │
│    |<---------------------|                        |                 │
│    |                      |                        |                 │
│    |  "Hello, I want /users/123"                    |                 │
│    |---------------------------------------------->|                 │
│    |                      |     [Server works]     |                 │
│    |                      |                        |                 │
│    |  "Here's the data!"   |                        |                 │
│    |<----------------------------------------------|                 │
│    |                      |                        |                 │
└─────────────────────────────────────────────────────────────────────┘
```

### Part 5: Cookies (The "Remember Me" Sticker)

**Analogy:** Imagine a **theme park** that gives you a wristband when you enter. The next time a worker sees you, they know you've already paid—they don't need to check your ticket again. **Cookies are like wristbands for websites.**

- You log in → Website gives you a cookie (a small piece of data)
- Your browser **saves** it
- Next time you visit → Browser sends the cookie back
- Website sees the cookie → "Oh, I know this person! They're logged in."

```
First visit:
  You  →  "Hi, I want to log in"  →  Server
  You  ←  "OK! Here's your cookie: abc123"  ←  Server
  (Browser saves "abc123")

Next visit:
  You  →  "Hi! I have cookie: abc123"  →  Server
  You  ←  "Welcome back! You're logged in!"  ←  Server
```

### Part 6: Sessions (Your Personal Locker)

**Analogy:** At a **water park**, you get a locker to keep your stuff. The locker has a number. Your stuff is safe inside. **Sessions work like lockers.**

- You log in → Server creates a "locker" (session) with your info
- Server gives you a "key" (session ID, often in a cookie)
- Every time you come back with the key → Server opens your locker and knows who you are

**Difference from cookies:**
- **Cookie** = Can store data on your computer (like a sticky note)
- **Session** = Stores data on the server (like a locker); you only get the key (session ID)

### Part 7: CORS (The "No Trespassing" Rule)

**Analogy:** Imagine **two different schools**. School A has a website. School B's website wants to get data from School A. But School A says: "I don't know you! I only talk to my own students." That rule—who can ask for my data—is **CORS** (Cross-Origin Resource Sharing).

- **Same origin** = Same school (same website)
- **Cross origin** = Different school (different website)
- **CORS** = The rule that says which "schools" (websites) can talk to each other

When your Spring Boot API runs on `api.mysite.com` and a website on `othersite.com` tries to fetch data, the browser asks: "Can othersite.com talk to you?" Your server must say "Yes" or the browser blocks it.

### Part 8: HTTPS (The Sealed Envelope)

**Analogy:** Sending a postcard vs. sending a **letter in a sealed envelope**.

- **HTTP** = Postcard. Anyone who sees it can read it.
- **HTTPS** = Sealed envelope. Only you and the recipient can read it.

HTTPS **encrypts** your data so that if someone intercepts it (like someone snatching your mail), they can't read it. That's why we use HTTPS for passwords, credit cards, and important data.

### Part 9: WebSockets (The Walkie-Talkie)

**Analogy:** Normal web requests are like **writing letters**: you send one, wait for a reply, send another.

**WebSockets** are like **walkie-talkies**: once connected, you can talk back and forth instantly without "sending a new letter" each time.

- **HTTP** = Send letter → Wait → Get reply → Send another letter
- **WebSocket** = Pick up walkie-talkie → Keep talking as long as you want

Used for: live chat, real-time games, live sports scores, notifications that pop up instantly.

---

## Layer 2 — Professional Developer

### Client-Server Model Deep Dive

The web runs on a **request-response** pattern. The client always initiates; the server always responds.

```
┌─────────────┐                    ┌─────────────┐
│   CLIENT    │                    │   SERVER    │
│  (Browser,  │   ──── Request ───>│ (Spring     │
│   App,      │                    │  Boot)      │
│   etc.)     │   <──── Response ──│             │
└─────────────┘                    └─────────────┘
```

**Key characteristics:**
- **Stateless by default**: Each request is independent (HTTP doesn't "remember" previous requests)
- **Server doesn't initiate**: Servers wait; clients ask
- **Multiple clients, one (or more) servers**: Many users can hit the same API

### DNS Resolution (Technical Flow)

```
                    DNS Resolution Chain
                    ═══════════════════

Browser                OS                ISP DNS            Root DNS
   |                    |                    |                  |
   | Check cache        |                    |                  |
   |─── miss ──────────>|                    |                  |
   |                    | Check cache        |                  |
   |                    |─── miss ──────────>|                  |
   |                    |                    | Query .com      |
   |                    |                    |────────────────>|
   |                    |                    |                  | "Ask TLD"
   |                    |                    |<────────────────|
   |                    |                    |                  |
   |                    |                    | Query TLD        |
   |                    |                    |────────────────>|
   |                    |                    |                  | "Ask example.com"
   |                    |                    |<────────────────|
   |                    |                    |                  |
   |                    |                    | Query Authoritative
   |                    |                    |────────────────>|
   |                    |                    |                  | "203.0.113.1"
   |                    |                    |<────────────────|
   |                    |                    |                  |
   |                    |<─── 203.0.113.1 ───|                  |
   |<─── 203.0.113.1 ───|                    |                  |
```

**TTL (Time To Live):** How long each step can "remember" the answer before asking again.

| Cache Level     | Typical TTL  | Purpose                        |
|-----------------|-------------|--------------------------------|
| Browser         | 5–60 min    | Fast repeat visits             |
| OS              | 1–24 hours  | System-wide reuse              |
| ISP DNS         | Per record  | Reduces upstream queries       |
| Authoritative   | Set by you  | Balance speed vs. change speed |

**Spring Boot tip:** Before changing server IPs (e.g., migration), lower TTL a day or two earlier so the new IP spreads faster.

### TCP/IP Basics

**TCP (Transmission Control Protocol):** Ensures data arrives correctly. Like registered mail—you get confirmation it arrived.

**IP (Internet Protocol):** Delivers packets to the right address. Like the address on an envelope.

**The handshake (simplified):**
```
Client: "I want to connect"     (SYN)
Server: "OK, let's connect"     (SYN-ACK)
Client: "Got it, connected!"    (ACK)
```

After this, data flows both ways reliably.

### Ports and localhost

| Port   | Service      | Notes                              |
|--------|--------------|------------------------------------|
| 80     | HTTP         | Standard web; often needs root     |
| 443    | HTTPS        | Secure web; needs SSL cert         |
| 8080   | Alt HTTP     | Spring Boot default; no root need  |
| 22     | SSH          | Remote login                       |
| 3306   | MySQL        | Database                           |
| 5432   | PostgreSQL   | Database                           |
| 6379   | Redis        | Cache/sessions                     |

**Why Spring Boot uses 8080:**
- Ports 80 and 443 often require admin/root
- 8080 allows running as normal user during development
- In production, a reverse proxy (Nginx, etc.) usually listens on 443 and forwards to 8080

```properties
# application.properties
# Change port if needed
server.port=8080
```

### Request Lifecycle (Step by Step)

```
1. DNS       → Resolve domain to IP
2. TCP       → 3-way handshake
3. TLS       → HTTPS handshake (if HTTPS)
4. HTTP      → Send request (method, path, headers)
5. Server    → Process (controller → service → database)
6. HTTP      → Send response (status, headers, body)
7. Browser   → Parse and display
```

**Example HTTP request:**
```http
GET /users/123 HTTP/1.1
Host: api.example.com
Accept: application/json
Cookie: sessionId=abc123
```

**Example response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{"id":123,"name":"Alice"}
```

### Cookies (Technical)

Cookies are name-value pairs sent in HTTP headers.

**Server sets a cookie:**
```http
Set-Cookie: sessionId=abc123; Path=/; HttpOnly; Secure
```

**Client sends it back:**
```http
Cookie: sessionId=abc123
```

**Important flags:**
- **HttpOnly** — Not readable by JavaScript (helps against XSS)
- **Secure** — Sent only over HTTPS
- **SameSite** — Limits when cookie is sent (helps against CSRF)

### Sessions (Technical)

Sessions store server-side state keyed by a session ID (often in a cookie).

```java
@GetMapping("/dashboard")
public String dashboard(HttpSession session) {
    // Spring injects HttpSession automatically
    // session.getAttribute retrieves data stored earlier (e.g., at login)
    String user = (String) session.getAttribute("user");
    return "Hello, " + user;
}
```

**Default:** Spring Boot stores sessions in memory (fine for one server, not for multiple).

**Production:** Use Redis or a database so all servers share the same sessions.

### CORS (Technical)

When a page at `https://app.example.com` calls `https://api.example.com`, that's cross-origin. The browser sends a preflight request and checks CORS headers.

**Spring Boot CORS configuration:**
```java
@Configuration  // Tells Spring this class holds configuration
public class CorsConfig {
    @Bean  // Exposes the returned object as a Spring bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Apply CORS rules to all paths under /api
                registry.addMapping("/api/**")
                    // Only allow requests from this specific frontend origin
                    .allowedOrigins("https://app.example.com")
                    // Which HTTP methods are allowed (blocks others)
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    // Allow cookies/credentials in cross-origin requests
                    .allowCredentials(true);
            }
        };
    }
}
```

### HTTPS / TLS Basics

**TLS** provides:
1. **Encryption** — Data is unreadable in transit
2. **Integrity** — Tampering can be detected
3. **Authentication** — Server identity via certificates

**Certificate chain:**
```
Root CA (trusted by browsers)
    └─> Intermediate CA
           └─> Your server certificate (api.example.com)
```

**SSL termination:** HTTPS is decrypted at a reverse proxy; the proxy forwards plain HTTP to your app. Spring Boot then only needs to handle HTTP (e.g., on 8080).

### WebSockets (Technical)

HTTP: request → response, connection closed.  
WebSocket: open connection, bidirectional messages over the same connection.

**Spring Boot WebSocket support:**
```java
@Configuration        // Configuration class for Spring
@EnableWebSocket      // Turns on WebSocket support
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register handler for path /ws - clients connect via ws://host/ws
        registry.addHandler(myHandler(), "/ws");
    }
}
```

Use for chat, notifications, live updates, etc.

### Web Server vs Application Server

| Type              | Examples     | Role                                  |
|-------------------|-------------|---------------------------------------|
| Web server        | Nginx, Apache| Static files, reverse proxy, SSL      |
| Application server| Tomcat, Jetty| Runs Java/Servlet/Spring code         |

**Typical production setup:**
```
Internet → Nginx (static files, SSL, reverse proxy)
              → Tomcat (runs your Spring Boot app)
```

Spring Boot embeds Tomcat by default, so you get an app server inside your JAR.

---

## Layer 3 — Advanced Engineering Depth

### Load Balancers

**Purpose:** Distribute traffic across many servers for performance and reliability.

```
                    ┌─────────────┐
                    │   Clients   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │Load Balancer│
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
   │Server 1 │       │Server 2 │       │Server 3 │
   └─────────┘       └─────────┘       └─────────┘
```

**Layer 4 vs Layer 7:**
- **L4:** Routes by IP/port only; faster, less flexible
- **L7:** Inspects HTTP (path, headers); can do SSL termination and content-based routing

**Algorithms:** Round-robin, least connections, IP hash (for affinity), weighted variants.

**Health checks:** LB periodically calls something like `/health`. If it fails or times out, the server is removed from the pool.

**Session affinity (sticky sessions):** Same client always sent to same server. Simplifies in-memory sessions but hurts scaling. Prefer stateless design + shared session store (e.g., Redis).

### Reverse Proxy

Sits in front of your app. Common tasks:
- **SSL termination** — Decrypt HTTPS, forward HTTP
- **Caching** — Cache static or API responses
- **Compression** — gzip responses
- **Load balancing** — Spread traffic to backend instances

**Proxy headers** (so the app knows the real client):

| Header             | Purpose              | Example                    |
|--------------------|----------------------|----------------------------|
| X-Forwarded-For    | Original client IP    | `203.0.113.45`             |
| X-Real-IP          | Client IP (Nginx)    | `203.0.113.45`             |
| X-Forwarded-Proto  | Original protocol    | `https`                    |
| X-Forwarded-Host   | Original host        | `api.example.com`          |

**Nginx example** (forwards to Spring Boot on 8080):
```nginx
server {
    listen 443 ssl;
    server_name api.example.com;
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:8080;  # Forward to Spring Boot
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**Spring Boot behind a proxy:**
```properties
# Trust proxy headers so redirects/URLs use correct scheme and host
server.forward-headers-strategy=framework
```

**Getting real client IP in a controller:**
```java
@GetMapping("/client-info")
public Map<String, String> getClientInfo(HttpServletRequest request) {
    // Prefer proxy headers; fallback to direct connection IP
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null) ip = request.getHeader("X-Real-IP");
    if (ip == null) ip = request.getRemoteAddr();
    // X-Forwarded-For can be "client, proxy1, proxy2" - first is client
    if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();

    return Map.of("ip", ip != null ? ip : "unknown");
}
```

### Connection Pooling

Creating new TCP/DB connections is expensive. **Connection pooling** reuses connections.

**Without pooling:**
```
Request 1: [New connection ~100ms] + [Query 10ms] = 110ms
Request 2: [New connection ~100ms] + [Query 10ms] = 110ms
```

**With pooling:**
```
Request 1: [Get from pool ~1ms] + [Query 10ms] = 11ms
Request 2: [Get from pool ~1ms] + [Query 10ms] = 11ms
```

Spring Boot uses **HikariCP** for database connections. Configure pool size to match your load and DB limits.

### CDN (Content Delivery Network)

CDNs cache content at many locations (edge servers) so users get it from a nearby server.

```
User (Tokyo) → Tokyo edge → Cache HIT  → Fast (e.g. 20ms)
User (Tokyo) → Tokyo edge → Cache MISS → Origin (e.g. NY) → Cache → Next time: HIT
```

Good for static assets (images, JS, CSS). Dynamic APIs benefit less unless you cache specific endpoints.

### Stateless vs Stateful Design

**Stateful (hard to scale):**
```java
// BAD: State lives in server memory
private Map<Long, Cart> carts = new HashMap<>();
// User hits Server 1 → cart stored there. Next request → Server 2 → cart empty!
```

**Stateless (scalable):**
```java
// GOOD: State in database or Redis
@Autowired CartRepository cartRepository;
// Any server can serve any request; state lives externally
```

**Scaling tip:** Prefer stateless design, external sessions (Redis), and JWT for APIs.

### Production Architecture (Typical Stack)

```
                    ┌─────────────┐
                    │    Users    │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │     DNS     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │     CDN     │  (static assets)
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │Load Balancer│
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Nginx     │  SSL termination, cache
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
   │Spring   │       │Spring   │       │Spring   │
   │Boot     │       │Boot     │       │Boot     │
   └────┬────┘       └────┬────┘       └────┬────┘
        └─────────────────┼─────────────────┘
                          │
                    ┌──────▼──────┐
                    │ Database   │
                    │  + Redis   │
                    └────────────┘
```

### Request Lifecycle Timing (Typical)

| Step            | Time   |
|-----------------|--------|
| DNS             | ~20ms  |
| TCP handshake   | ~30ms  |
| TLS handshake   | ~100ms |
| Load balancer   | ~1ms   |
| Reverse proxy   | ~2ms   |
| Application     | ~50ms  |
| Database        | ~20ms  |
| **Total**       | ~223ms |

### Spring Boot Behind a Proxy

```properties
# Trust X-Forwarded-* headers from proxy
server.forward-headers-strategy=framework
```

```java
// Get real client IP (not proxy IP)
String ip = request.getHeader("X-Forwarded-For");
if (ip == null) ip = request.getRemoteAddr();
```

---

## Layer 4 — Interview Mastery

### Q: What happens when you type a URL and press Enter?

**Answer:**  
1. **Parse URL** — Protocol, host, path.  
2. **DNS** — Resolve host to IP (cache → OS → resolver → root → TLD → authoritative).  
3. **TCP** — 3-way handshake.  
4. **TLS** — If HTTPS, handshake and certificate check.  
5. **HTTP** — Send request (e.g. GET /path).  
6. **Server** — Process (load balancer → reverse proxy → app → DB).  
7. **Response** — Status, headers, body.  
8. **Browser** — Parse, render, possibly cache.

### Q: What’s the difference between a load balancer and a reverse proxy?

**Answer:**  
- **Load balancer:** Distributes requests across multiple backend servers (high availability, scaling).  
- **Reverse proxy:** Single entry point; can do SSL termination, caching, compression.  
Many systems (e.g. Nginx, HAProxy) act as both.

### Q: What is SSL termination and where should it happen?

**Answer:**  
SSL termination = decrypt HTTPS before it reaches the app. It should happen at the reverse proxy or load balancer, not on the app server. That keeps certificates and CPU-intensive TLS work at the edge; the app runs plain HTTP (e.g. on 8080).

### Q: How do you handle sessions with multiple servers?

**Answer:**  
Avoid in-memory sessions. Use a shared store (Redis or DB). Spring Boot: `spring.session.store-type=redis`. For APIs, stateless JWT tokens avoid server-side sessions entirely.

### Q: What is CORS?

**Answer:**  
CORS (Cross-Origin Resource Sharing) controls which origins (scheme + host + port) can call your API. Browsers enforce it. Configure `Access-Control-Allow-Origin` and related headers on your API; Spring Boot can do this via `addCorsMappings` or `@CrossOrigin`.

### Q: HTTP vs WebSocket?

**Answer:**  
HTTP is request–response; each request opens (or reuses) a connection, gets a response, then it’s done. WebSocket opens one long-lived connection for two-way messaging. Use WebSocket for real-time features (chat, live updates); use HTTP for typical CRUD and API calls.

### Q: Why is stateless design important for scaling?

**Answer:**  
Stateless apps store no important data in process memory. Any instance can serve any request. You can add or remove instances without session affinity. State lives in DB, cache, or tokens (e.g. JWT), so scaling is simpler and more resilient.

### Q: How would you scale a Spring Boot application?

**Answer:**  
1. **Make it stateless** — No in-memory sessions; use Redis or DB.  
2. **Externalize sessions** — `spring.session.store-type=redis`.  
3. **Put a load balancer in front** — Health checks on `/actuator/health`.  
4. **Use connection pooling** — HikariCP (default) for DB connections.  
5. **Horizontal scaling** — Add more instances behind the LB.  
6. **Consider read replicas** — For read-heavy workloads.

### Q: What headers does a load balancer add?

**Answer:**  
- **X-Forwarded-For** — Original client IP (comma-separated if multiple proxies).  
- **X-Real-IP** — Original client IP (Nginx).  
- **X-Forwarded-Proto** — Original protocol (http/https).  
- **X-Forwarded-Host** — Original Host header.  

Spring Boot: `server.forward-headers-strategy=framework` to trust them.

### Q: How would you debug intermittent 502 errors?

**Answer:**  
1. Trace the request: DNS → LB → proxy → app → DB.  
2. Check LB health checks (endpoint, timeout).  
3. Check proxy timeouts (`proxy_read_timeout`).  
4. Check app logs and DB connection pool.  
5. Add request IDs (MDC/Sleuth) for correlation.  
6. Load-test to reproduce; verify timeouts at each layer.

---

## Summary

| Topic        | Simple idea                          | For Spring Boot                            |
|-------------|--------------------------------------|--------------------------------------------|
| Client-server | Client asks, server answers         | Your controllers are the server            |
| DNS         | Domain name → IP address             | Affects deployment, TTL for migrations     |
| IP & ports  | Address + “apartment” number         | Default port 8080; proxy often on 443       |
| localhost   | “This machine”                       | Where your dev server runs                  |
| Request flow| DNS → TCP → TLS → HTTP → server      | Same path your requests follow              |
| Cookies     | Small data the browser stores/sends | Auth, preferences, tracking                |
| Sessions    | Server-side state per user           | Prefer Redis/DB in production               |
| CORS        | Who can call your API from a browser | Configure `allowedOrigins` etc.             |
| HTTPS/TLS   | Encrypted transport                  | Termination usually at proxy               |
| WebSockets  | Long-lived, two-way connection       | For real-time features                      |
| Load balancer| Spreads traffic across servers       | Health checks, stateless design             |
| Reverse proxy| SSL, cache, compression in front     | `server.forward-headers-strategy`           |
| Stateless   | No state in app memory               | Use Redis/JWT for sessions                  |

Understanding these ideas helps you design, debug, and scale Spring Boot applications in real environments.

---

[← Back to Index](00-README.md) | [Previous: HTTP & REST](04-Prerequisites-HTTP-REST.md) | [Next: Databases →](06-Prerequisites-Databases.md)
