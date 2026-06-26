# 32. Networking and Communication

> Every single thing in this roadmap — load balancers, replication, microservice calls, message queues, caches — ultimately comes down to *bytes traveling between machines over a network*. This chapter is the foundation underneath everything: how computers actually talk, the protocols involved, and why "the network is unreliable" is the most important sentence in distributed systems.

---

## What is it?

**Networking is how machines communicate by sending data across physical and logical connections; the communication protocols are the agreed-upon rules for how that data is formatted, addressed, transmitted, and received.**

We've treated the network as a given throughout — "Service A calls Service B," "the leader replicates to followers," "the client requests data." This chapter opens up *what's actually happening* when those calls cross the wire. It matters because the network's properties (latency, unreliability, bandwidth) *constrain* every design decision we've made:
- Why is a network call ~100,000× slower than a memory read? (Chapter 3's latency numbers.)
- Why must every network call have a timeout? (Chapter 26's resiliency.)
- Why do we put servers near users and use CDNs? (Latency.)
- Why is "exactly-once" delivery so hard? (Network unreliability.)

The single most important truth, which underlies all of distributed systems:

> **The network is unreliable.** Messages get lost, delayed, duplicated, reordered, or arrive corrupted. Connections drop. Latency varies wildly. A request that gets no response might mean the server never got it, *or* got it and the response was lost — **you cannot tell the difference.** This fundamental uncertainty is *why* we need idempotency (Chapter 30), retries with care (Chapter 26), timeouts, and eventual consistency. Almost every hard problem in distributed systems traces back to network unreliability.

---

## How it Works Under the Hood

### The layered model (how a message actually travels)

Networking is organized in **layers**, each handling one concern and building on the one below. The practical model (TCP/IP) has these layers:

```
┌─────────────────────────────────────────────┐
│ Application  (HTTP, gRPC, WebSocket, DNS...)  │ ← what your app speaks
├─────────────────────────────────────────────┤
│ Transport    (TCP, UDP)                       │ ← reliable vs fast delivery
├─────────────────────────────────────────────┤
│ Network      (IP)                             │ ← addressing & routing
├─────────────────────────────────────────────┤
│ Link         (Ethernet, WiFi)                 │ ← physical transmission
└─────────────────────────────────────────────┘
```
When your Spring Boot app sends an HTTP request, it travels *down* this stack on the sender (HTTP → TCP → IP → wire), across the network, and *up* the stack on the receiver. Each layer wraps the data with its own header (encapsulation). You mostly work at the **Application layer**, but understanding **Transport** (TCP/UDP) and **IP/DNS** explains a lot of system behavior.

### IP and DNS (addressing — finding the machine)

- **IP (Internet Protocol)** gives every machine an **address** (e.g., `142.250.x.x`) and handles **routing** — getting packets from source to destination across many intermediate routers. IP itself is *best-effort*: it doesn't guarantee delivery or order (that's TCP's job).
- **DNS (Domain Name System)** is the internet's phone book: it translates human names (`api.wallet.com`) into IP addresses. Your request first does a DNS lookup to find *where* to connect. DNS is also a tool for **load balancing and geo-routing** (Chapter 28) — returning different IPs to route users to the nearest data center (Chapter 3's latency).

### TCP vs UDP (the transport-layer choice)

This is the key communication decision at the transport layer:

**TCP (Transmission Control Protocol) — reliable, ordered, connection-based.**
- Establishes a connection via a **3-way handshake** (SYN → SYN-ACK → ACK) before sending data.
- Guarantees **reliable, in-order delivery**: lost packets are detected and retransmitted; out-of-order packets are reassembled; corrupted packets are rejected.
- Provides **flow control** and **congestion control** (slows down when the network is congested).
- **Cost:** the handshake adds a round trip of latency, and the reliability machinery adds overhead.
- **Used by:** HTTP/REST, gRPC, WebSockets, database connections — basically anything where correctness matters (most of what we've discussed).

**UDP (User Datagram Protocol) — fast, connectionless, best-effort.**
- No handshake, no guaranteed delivery, no ordering — just fire packets and hope.
- **Much faster and lower overhead** — no waiting for acknowledgments or retransmissions.
- **Cost:** packets can be lost, duplicated, or reordered, and the app must tolerate that.
- **Used by:** video/voice streaming, online gaming, DNS lookups, metrics — cases where speed matters more than perfect delivery and a lost packet is no big deal (a dropped video frame is better than a stalled stream).

| | TCP | UDP |
|---|---|---|
| Reliability | Guaranteed, ordered | Best-effort, unordered |
| Connection | Yes (handshake) | No |
| Speed | Slower (overhead) | **Faster** |
| Use for | Web, APIs, DB, money | Streaming, gaming, DNS, metrics |

> Rule of thumb: **TCP when every byte must arrive correctly (the default for business/financial data); UDP when speed beats perfection** and occasional loss is acceptable.

### HTTP and its evolution (the application layer you live in)

Most backend communication rides on **HTTP** (over TCP). Its versions matter for performance:
- **HTTP/1.1:** one request-response per connection at a time (head-of-line blocking); browsers open multiple connections to parallelize. Verbose text headers.
- **HTTP/2:** **multiplexing** — many requests share one connection simultaneously; binary framing; header compression. Big efficiency win (this is what gRPC uses, Chapter 23).
- **HTTP/3:** runs over **QUIC** (built on UDP!) — eliminates TCP's head-of-line blocking and speeds up connection setup, especially on lossy/mobile networks.

Also essential: **TLS/SSL** encrypts the connection (HTTPS), adding a handshake but providing confidentiality and integrity — mandatory for any data in transit, *especially* money (and often terminated at the load balancer/gateway, Chapters 22, 28).

### The fallacies of distributed computing (why this all matters)

A famous list every distributed-systems engineer should know — the *false* assumptions that cause outages:
1. The network is reliable. *(It isn't.)*
2. Latency is zero. *(It isn't — Chapter 3.)*
3. Bandwidth is infinite. *(It isn't.)*
4. The network is secure. *(It isn't — encrypt everything.)*
5. Topology doesn't change. *(It does — nodes come and go, Chapter 31.)*
6. There is one administrator. 7. Transport cost is zero. 8. The network is homogeneous.

> Every one of these fallacies, when assumed true, leads to a real-world failure. Good distributed-systems design is largely about *not* believing these comforting lies — designing for unreliable, slow, finite, insecure, changing networks. This list is the philosophical core of why we need timeouts, retries, idempotency, encryption, and consistent hashing.

---

## Why do we need to understand it?

We need networking fundamentals because **the network's properties dictate the behavior and constraints of every distributed system — and misunderstanding them causes the most common and severe failures:**

1. **It explains performance.** Latency (Chapter 3), the cost of extra service hops (Chapter 20), why CDNs and geo-routing exist, why HTTP/2/gRPC are faster — all are consequences of network physics. You can't reason about performance without understanding the network underneath.

2. **It explains why resiliency is mandatory.** Because the network is unreliable, calls fail, hang, and duplicate. This is *why* we need timeouts, circuit breakers, retries, and idempotency (Chapters 26, 29, 30). Network unreliability is the root cause those patterns address.

3. **It informs protocol choices.** TCP vs UDP, HTTP version, REST vs gRPC (Chapter 23) — these are networking decisions with real performance and reliability consequences. Knowing the trade-offs lets you choose well.

4. **It's the basis of security.** Understanding that the network is *not* secure (fallacy #4) is why we encrypt everything in transit (TLS) — non-negotiable for fintech.

5. **It grounds the theory.** CAP's "partition" (Chapter 8) is literally a network failure. Eventual consistency exists because propagation over the network takes time. The network is where the abstract trade-offs become concrete.

**When it matters most:** any time you're debugging latency, designing cross-service or cross-region communication, choosing protocols, or reasoning about failure modes. Which is to say: constantly.

---

## Real-World / Fintech Example

Networking decisions shape our **digital wallet / payments app** at every level — here's where they surface concretely.

**TCP everywhere for money; the reliability is the point.** Every payment-related call — client→gateway (HTTPS), gateway→services, service→service (gRPC), service→database — runs over **TCP**, because financial data demands guaranteed, in-order, uncorrupted delivery. A dropped or reordered byte in a payment instruction is unacceptable. The team accepts TCP's handshake/overhead cost as the price of correctness. (They'd only consider UDP for something like real-time metrics streaming, where loss is tolerable.)

**TLS on every hop — the network is not secure.** Recognizing fallacy #4, *all* traffic is encrypted with **TLS** — client-to-gateway and even service-to-service (mutual TLS) inside the data center, because "internal" networks can still be compromised. For a system moving money and holding PII/KYC data, unencrypted traffic would be a compliance and security catastrophe. TLS is terminated/re-established at the gateway and between services (Chapters 22, 28).

**HTTP/2 and gRPC for internal efficiency.** High-volume internal calls (Payment→Fraud, Chapter 23) use **gRPC over HTTP/2**, whose multiplexing lets many concurrent calls share one connection — cutting connection overhead and latency versus opening a new HTTP/1.1 connection per call. At 80,000 payments/sec, this efficiency is material (Chapter 3's throughput).

**DNS geo-routing for latency.** **DNS** returns the nearest region's IP to each user — Indian users resolve to Mumbai, US users to Virginia (Chapters 3, 28) — slashing the cross-continent round-trip latency (~100+ ms, Chapter 3) that would otherwise make every payment feel sluggish.

**The network-unreliability problem, made concrete.** Alice taps "Pay," the request reaches the server, the payment succeeds — but the *response* is lost on her flaky mobile network. Alice's app sees no response. **Did the payment happen or not? The network can't tell her.** This is the fundamental ambiguity above. The app handles it with **idempotency keys** (Chapter 30): when Alice's app retries, the same key ensures the payment isn't processed twice. *This single fintech scenario is the entire reason idempotency exists* — and it's a direct consequence of network unreliability.

**Timeouts because latency is not zero/bounded.** Every cross-service and external call has an aggressive **timeout** (Chapter 26), because the network can make any call hang indefinitely. Without timeouts, a slow network path to the bank API would exhaust threads and cascade (Chapter 26). The team *designed against* fallacies #1 and #2.

> The throughline: nearly every resiliency and consistency decision in the app — idempotency, timeouts, TLS, geo-routing, eventual consistency — exists *because of* the realities of networking. The network isn't a detail beneath the architecture; it's the bedrock that *shapes* the architecture.

---

## Trade-offs (Pros & Cons)

Since this is foundational knowledge, the "trade-offs" are the key communication choices:

### TCP vs UDP
**TCP Pros:** reliable, ordered, error-checked delivery; flow/congestion control; the safe default for business data.
**TCP Cons:** handshake + reliability overhead = higher latency; connection state to manage.
**UDP Pros:** fast, low overhead, no handshake; great for streaming/gaming/metrics.
**UDP Cons:** no delivery/order guarantees; the app must tolerate loss and reordering.

### Synchronous (HTTP/gRPC) vs Asynchronous (messaging) communication
**Synchronous Pros:** simple request-response; immediate result; easy to reason about.
**Synchronous Cons:** caller blocks; tight coupling; failure propagates directly (needs resiliency patterns).
**Asynchronous Pros:** decoupling, buffering, resilience (Chapters 18–21).
**Asynchronous Cons:** eventual consistency; harder to debug.

### Encryption (TLS)
**Pros:** confidentiality + integrity in transit; mandatory for sensitive/financial data.
**Cons:** handshake adds latency; CPU cost for encryption (mitigated by termination at gateway/LB and modern hardware).

> **Staff-engineer takeaway:** Networking is the **bedrock beneath every distributed-systems concept** — and its defining truth is that **the network is unreliable** (messages lost, delayed, duplicated; you can't tell a lost request from a lost response). Internalize the **layered model**, the **TCP-vs-UDP** choice (reliable default vs fast best-effort), **HTTP/2-3** efficiency, and **DNS** for addressing/geo-routing — and *always* encrypt with **TLS**. Above all, **don't believe the fallacies of distributed computing**: design for a slow, lossy, insecure, changing network. Nearly every pattern in this roadmap — idempotency, timeouts, retries, eventual consistency, consistent hashing — exists *because* of network reality.

---

➡️ Next: [33-Real-World-Architectures-and-Engineering-Blogs.md](33-Real-World-Architectures-and-Engineering-Blogs.md) — the capstone: how all these concepts combine in real famous systems, and how to keep learning from the engineers who build them.
