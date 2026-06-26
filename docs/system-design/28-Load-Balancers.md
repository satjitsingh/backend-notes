# 28. Load Balancers

> We've name-dropped the load balancer in nearly every chapter — it's the traffic cop that makes horizontal scaling possible. Now we open it up. A load balancer is deceptively simple in concept ("spread requests across servers") but rich in mechanics: algorithms, health checks, layers, and session handling that quietly determine whether your scaled-out system actually works.

---

## What is it?

**A load balancer is a component that sits in front of a group of servers and distributes incoming requests across them, so no single server is overwhelmed and the system can scale and stay available.**

Recall from Chapter 2 that horizontal scaling means running many copies of your (stateless) app. But that raises an immediate question: *when a request arrives, which copy handles it?* The load balancer answers this. It's the single entry point that receives all traffic and intelligently spreads it:

```
                    ┌──────────────────┐
[Clients] ─────────►│  Load Balancer    │
                    └────────┬──────────┘
              ┌──────────────┼──────────────┐
              ▼              ▼               ▼
         [Server 1]     [Server 2]     [Server 3]
         (healthy)      (healthy)      (unhealthy ✗ — LB skips it)
```

It does two jobs that are both essential, and people often only think of the first:

1. **Distribute load** — spread requests evenly so resources are used efficiently and no server is a hotspot. *(This is the scaling job.)*
2. **Provide availability** — continuously check server health and **route around dead/unhealthy servers**, so a server crash is invisible to users. *(This is the resiliency job — Chapters 5, 26.)*

> **The mental model:** a load balancer is both a *scaling enabler* (lets you add servers and use them) and an *availability enabler* (detects failures and reroutes). It's the component that turns "a pile of servers" into "one reliable, scalable service."

A critical subtlety: the load balancer itself must not become a **single point of failure** (Chapter 5). So in production you run **redundant load balancers** (active-active or active-passive with failover), often with DNS or a floating IP in front.

---

## How it Works Under the Hood

### Layer 4 vs Layer 7 (the two kinds)

Load balancers operate at different levels of the network stack, and the distinction matters:

**Layer 4 (Transport layer) load balancing** — routes based on **IP address and port**, *without* looking at the actual content of the request. It just forwards TCP/UDP packets to a chosen server.
- **Pro:** very fast and efficient (minimal processing); doesn't decrypt or inspect anything.
- **Con:** "dumb" — can't make decisions based on request content (URL, headers, cookies).

**Layer 7 (Application layer) load balancing** — routes based on the **content** of the request: the URL path, HTTP headers, cookies, etc.
- **Pro:** "smart" — can route `/api/payments` to one pool and `/api/images` to another, do SSL termination, content-based routing, and sticky sessions via cookies. *(The API Gateway from Chapter 22 is essentially a sophisticated L7 load balancer + more.)*
- **Con:** more processing per request (must parse the request, often decrypt SSL), so slightly slower than L4.

> Rule of thumb: **L4 for raw speed and simple TCP distribution; L7 for smart, content-aware routing** (which most web apps want). Many systems use both — L4 at the edge for throughput, L7 deeper for routing logic.

### Load balancing algorithms (how it picks a server)

The algorithm decides *which* server gets the next request. The common ones:

**Round Robin** — cycle through servers in order (1, 2, 3, 1, 2, 3...). Simple and fair when all servers and requests are equal.
- *Weakness:* ignores that some requests are heavier or some servers slower.

**Weighted Round Robin** — give more powerful servers a higher weight (more requests). Useful when servers aren't identical.

**Least Connections** — send the next request to the server with the *fewest active connections*. Smarter than round robin because it accounts for the fact that some requests take longer — it naturally avoids piling onto a server that's bogged down with long-running requests.
- Often the best default for variable request durations.

**Least Response Time** — route to the server with the lowest combination of active connections and fastest recent response time. Even more adaptive.

**IP Hash** — hash the client's IP to consistently route the same client to the same server. Useful for *session affinity* (below) without cookies.

**Consistent Hashing** — a special hashing approach (its own chapter later) that maps requests to servers such that adding/removing a server reshuffles *minimal* traffic. Crucial for distributed caches and stateful routing.

### Health checks (the availability mechanism)

This is what makes a load balancer an availability tool, not just a distributor. The LB continuously probes each server:
- **Active health checks:** periodically hit a health endpoint (e.g., `GET /health`) and expect a 200 OK. If a server fails N checks in a row, the LB marks it **unhealthy** and stops sending it traffic. When it recovers (passes checks again), traffic resumes.
- **Passive health checks:** watch real traffic — if a server starts returning errors or timing out, flag it.

This is the failover mechanism from Chapter 5 in action: a crashed server is detected within seconds and removed from rotation, so users transparently hit healthy servers. *(In Spring Boot, `/actuator/health` is the standard endpoint LBs and Kubernetes probe.)*

### The stateful problem: sticky sessions

Here's where the statelessness lesson from Chapter 2 returns. If your app is **stateless**, any server can handle any request — the LB is free to use any algorithm. But if a server stores state locally (e.g., a user's session in memory), then a user's requests *must* keep going to the *same* server, or their session is lost.

**Sticky sessions (session affinity)** solve this: the LB pins a given client to one server (via a cookie or IP hash). But this is a *workaround for a design flaw* — it undermines even load distribution (one server can get overloaded with sticky users) and breaks resiliency (if that server dies, those users lose their sessions).

> **The right fix is statelessness, not sticky sessions.** Store session state in a shared store (Redis, Chapter 2/17) so any server can serve any request, and the LB can balance freely. Sticky sessions are a last resort when you can't make the app stateless.

### DNS load balancing (the layer before the LB)
At the largest scale, even *before* the load balancer, **DNS** can distribute traffic — returning different server/LB IPs to different users (often based on geography, routing users to the nearest data center for lower latency, Chapter 3). This is how global systems spread load across regions, with the regional load balancers handling distribution within each region.

---

## Why do we need them?

We need load balancers because **they are the component that makes horizontal scaling and high availability actually work** — without one, a pile of servers is just a pile of servers:

1. **They enable horizontal scaling.** Adding servers (Chapter 2) is pointless if traffic can't reach them evenly. The LB is what distributes load across the fleet, so each added server actually shares the work. No LB, no effective scale-out.

2. **They provide high availability.** Via health checks, the LB detects failed servers and reroutes around them in seconds — turning a server crash from an outage into a non-event (Chapter 5's failover). This is automatic, fast, and invisible to users.

3. **They optimize resource use.** Smart algorithms (least connections, least response time) keep load balanced so no server is overwhelmed while others idle — maximizing the capacity you're paying for and keeping latency low (avoiding the queueing effect of Chapter 3).

4. **They enable zero-downtime operations.** Because the LB can drain traffic from a server gracefully, you can take servers out of rotation for deploys/maintenance (rolling updates, canary releases from Chapter 27) without downtime.

5. **They're a control point.** L7 LBs add SSL termination, content routing, rate limiting, and security filtering at the edge (overlapping with the API Gateway, Chapter 22).

**When to use:** essentially *any* system with more than one server. It's foundational infrastructure. The choices are *which type* (L4 vs L7), *which algorithm*, and *how to make the LB itself redundant* — not *whether* to have one.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** relies on load balancers at multiple layers — they're load-bearing infrastructure (pun intended) for both scale and uptime.

**Distributing payment traffic.** The ~40 stateless Spring Boot payment instances (Chapter 2) sit behind a **Layer 7 load balancer**. When 80,000 payment requests/sec arrive at peak, the LB spreads them across all 40 instances using **least connections** — so an instance bogged down with a few slow requests (e.g., waiting on the bank API) isn't handed even more work, while idle instances pick up the slack. This keeps every instance in its healthy, flat-latency zone (Chapter 3) and avoids hotspots.

**Availability via health checks.** Each instance exposes `/actuator/health`. The LB probes it every few seconds. When instance #17 crashes (OOM error) at peak, the LB notices within ~5 seconds, marks it unhealthy, and stops routing to it — the other 39 absorb its share. **Users never see an error**; the crash is invisible. This is the failover promise of Chapter 5, delivered by the LB.

**Statelessness enables free balancing.** Early on, the app stored sessions in instance memory, forcing **sticky sessions** — which caused uneven load (some instances overloaded with active users) and lost sessions on crashes. The team fixed the root cause: moved sessions to **Redis** (Chapter 2/17), making instances fully stateless. Now the LB balances *freely* with no stickiness, and a crashed instance loses *nothing* (sessions live in Redis). *The right fix was statelessness, not better sticky-session config.*

**L7 content routing.** The L7 LB (and API Gateway, Chapter 22) routes by path: `/payments/*` → payment pool, `/kyc/*` → KYC pool, `/reports/*` → reporting pool. Each pool scales independently per its load (Chapter 20's independent scaling). It also does **SSL termination** at the edge.

**Redundant LBs + global DNS.** The load balancer itself isn't a SPOF — the team runs **redundant LBs** (active-active) per region. And **DNS-level geographic routing** sends Indian users to the Mumbai region and US users to the Virginia region — each region's LBs then distribute locally. This cuts latency (users hit the nearest data center, Chapter 3) and provides regional redundancy (Chapter 27).

**Zero-downtime deploys.** To release a new version, the team uses the LB to **drain** traffic from instances one at a time (rolling update / canary, Chapter 27), deploy, health-check, and return them to rotation — shipping multiple times a day with no downtime, critical for a money app that can't take maintenance windows.

In Spring Boot/cloud terms: this is typically an AWS ALB (L7) / NLB (L4), or NGINX/HAProxy, or Kubernetes Services + Ingress, with `/actuator/health` readiness/liveness probes driving the health-check-based routing.

---

## Trade-offs (Pros & Cons)

**Pros**
- **Enables horizontal scaling** — distributes load so added servers actually share work.
- **High availability** — health checks detect and route around failed servers automatically.
- **Optimizes resource use & latency** — smart algorithms prevent hotspots and queueing.
- **Zero-downtime operations** — drain/rolling deploys, canary releases.
- **Edge control point (L7)** — SSL termination, content routing, security, rate limiting.

**Cons**
- **Potential SPOF** — the LB itself must be made redundant, or it becomes the single weak link.
- **Added hop / latency** — every request passes through it (L7 adds parsing/decryption overhead).
- **Configuration complexity** — choosing layer, algorithm, health-check tuning, and timeouts correctly.
- **Sticky sessions are a trap** — needed for stateful apps but undermine balancing and resiliency; the real fix is statelessness.
- **Cost** — managed LBs and the bandwidth through them add expense at scale.

### L4 vs L7 quick compare
| | Layer 4 | Layer 7 |
|---|---|---|
| Routes on | IP + port | URL, headers, cookies (content) |
| Speed | Faster (no inspection) | Slower (parses/decrypts) |
| Smarts | Dumb forwarding | Content-aware routing, SSL termination |
| Best for | Raw TCP throughput | Web apps, microservice routing |

> **Staff-engineer takeaway:** A load balancer is the traffic cop that makes horizontal scaling *and* high availability real — it **distributes load** (via algorithms like least-connections) and **provides availability** (via health checks that route around dead servers). Choose **L7 for smart content-aware routing** (most web apps), **L4 for raw speed**. Make the LB itself **redundant** so it's not a SPOF, and use **DNS geo-routing** to spread across regions. Above all: keep your app **stateless** so the LB can balance freely — sticky sessions are a workaround for a design flaw, not a feature.

---

➡️ Next: [29-Circuit-Breakers.md](29-Circuit-Breakers.md) — a deep dive into the single most important resiliency pattern for stopping cascading failures, which we introduced in Chapter 26.
