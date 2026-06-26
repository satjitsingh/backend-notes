# 3. Latency and Throughput

> Two numbers describe almost everything about how a system performs. Confuse them and you'll optimize the wrong thing. Understand them and you can read any architecture like a doctor reads a heartbeat.

---

## What is it?

These are the two fundamental measurements of system performance. They are *not* the same thing, and the difference matters enormously.

**Latency is how long *one* operation takes — the *delay* or *wait time*.**
It's measured in time units: milliseconds (ms), microseconds (µs), sometimes seconds. When you tap "Pay" and wait 800 ms for the confirmation, that 800 ms is latency. Lower latency = feels snappier. Latency answers: *"How long does a single request take to finish?"*

**Throughput is how *much* work the system completes per unit of time — the *volume* or *rate*.**
It's measured in operations per second: requests per second (RPS), transactions per second (TPS), queries per second (QPS), or bytes per second for data. When your payment system handles 80,000 payments every second, that 80,000 TPS is throughput. Higher throughput = handles more total load. Throughput answers: *"How many requests can the system get through in a second?"*

The classic analogy is a **highway**:

- **Latency** = how long it takes *one car* to drive from the start of the highway to the end.
- **Throughput** = how many cars *pass through* the highway per minute.

Here's the part that surprises people:

> **Latency and throughput are related, but you can't assume improving one improves the other. Sometimes they even fight each other.**

You can have **low latency but low throughput**: a narrow, empty country road — one car crosses quickly, but only a few cars per minute can use it. You can have **high latency but high throughput**: a 10-lane highway during rush hour — each individual car crawls (high latency), but a *huge* number of cars pass per minute (high throughput). And the dream is **low latency *and* high throughput**: a wide, fast, free-flowing highway.

> **Quick mental check:** Latency is about the *individual's wait*. Throughput is about the *system's total volume*. A user cares about latency ("why is *my* page slow?"). The business cares about throughput ("how many users can we serve at once?"). A great system serves both.

---

## How it Works Under the Hood

### What latency is actually made of

When you click a button in an app, the "latency" you feel is really the **sum of many smaller delays** stacked end to end. Understanding this stack is how you debug slowness:

```
Total latency a user feels =
    Network time (request travels to server)
  + Queueing time (request waits its turn on the server)
  + Processing time (CPU does the actual work)
  + Database / I/O time (reading or writing data — often the biggest chunk)
  + Network time (response travels back)
```

Each layer adds delay. The biggest culprits are usually:
- **Network distance** — data physically cannot travel faster than light. A round trip from India to a US server is *hundreds of ms* just in travel time, no matter how fast the server is. This is why we put servers (and CDNs) *close to users*.
- **Disk and database I/O** — reading from a spinning disk or even an SSD is far slower than reading from memory. This is the single biggest reason caches exist (next chapters).
- **Waiting in queues** — under heavy load, requests pile up waiting for a free thread or connection, and this wait often *dominates* total latency. This is the key link between throughput and latency (more below).

### The latency numbers every engineer should roughly know

These orders of magnitude shape every design decision. You don't need exact figures, but you must feel the *relative scale*:

| Operation | Rough time | Intuition |
|---|---|---|
| Read from CPU cache / main memory | ~100 nanoseconds | basically instant |
| Read 1 MB sequentially from memory | ~3 microseconds | very fast |
| Read from SSD | ~100 microseconds | fast |
| Round trip within the same data center | ~0.5 milliseconds | quick |
| Read 1 MB from SSD | ~1 millisecond | noticeable |
| Read from spinning hard disk | ~10 milliseconds | slow |
| Network round trip across continents | ~100+ milliseconds | very slow |

> The headline takeaway: **memory is roughly 100,000× faster than a network call across the world.** This single fact is *why* caching, putting servers near users, and avoiding unnecessary network hops are such powerful tools. When you "make a system faster," you're almost always moving work from a slow layer (network, disk) to a fast layer (memory).

### Why averages lie: the importance of percentiles (p50, p95, p99)

Here's a trap that catches even experienced engineers. Suppose you measure latency and the **average is 100 ms**. Sounds great, right? But the average *hides the pain*. What if 95% of requests take 50 ms, but 5% take 3,000 ms? The average looks fine, yet 1 in 20 users is having a miserable, possibly transaction-failing experience.

So instead of averages, we use **percentiles**:

- **p50 (median):** half of requests are faster than this. The "typical" experience.
- **p95:** 95% of requests are faster than this; the slowest 5% are worse. The "annoyed user" line.
- **p99:** 99% are faster; the worst 1%. The "something is wrong" line.
- **p99.9:** the worst 1 in 1,000 — matters a lot at huge scale.

> **Why we obsess over the "tail" (p99, p99.9):** At massive scale, the rare slow request isn't rare in absolute terms. If you serve 1 billion requests a day, your p99.9 affects **1 million requests**. And in systems where one user action triggers *many* internal calls, the odds that *at least one* of them hits the slow tail get high — so the tail latency becomes the experience most users actually feel. Senior engineers design for p99, not the average.

### How latency and throughput interact (the queueing effect)

This is the deep connection. Imagine a server that can process one request in 100 ms (its latency when idle). How many can it handle per second? If it works on one at a time, throughput is 10/sec. To get higher throughput, you process requests **in parallel** (many threads, many servers).

But here's the catch: **as you push a system toward its maximum throughput, latency gets *worse*.** When the system is near capacity, new requests have to *wait in a queue* for a free worker — and that queue wait piles directly onto the latency each user feels. Push it past capacity and latency spikes toward infinity as the queue grows without end.

```
Latency
  ^
  |                                        .
  |                                      .
  |                                    .         <- near max capacity, latency
  |                              .                   shoots up (queueing)
  |____________________......
  |        (flat, healthy zone)
  +--------------------------------------------> Throughput (load)
                                        ^
                                  max safe capacity
```

> This is why you **never run a system at 100% capacity.** You leave headroom (often targeting ~70-80% max load) so that latency stays in the flat, healthy zone. Squeezing out the last bit of throughput destroys latency — and therefore user experience.

---

## Why do we need it?

We need to measure and reason about these two numbers because **they are how you translate business requirements into engineering targets**, and because they pull in different directions.

- **You need latency targets** because humans are impatient and, in fintech, slow responses cause real harm. A user who waits too long for a payment confirmation assumes it failed and retries — risking double charges, support tickets, and lost trust. Studies repeatedly show that even small latency increases measurably reduce conversions and revenue.

- **You need throughput targets** because they determine *how much hardware you need and whether you survive peak load*. If your system maxes out at 10,000 TPS but Black Friday brings 80,000 TPS, you *will* go down — regardless of how fast each individual request is.

- **You need to understand their interaction** because optimizing blindly backfires. Chasing maximum throughput by running servers hot will wreck your p99 latency. Chasing minimum latency by massively over-provisioning wastes money. The job is to hit *both* targets *together* at the required scale.

In practice, your design choices map directly onto these two dials:
- To **lower latency:** add caching, put servers/CDNs near users, use faster storage, reduce network hops, precompute results.
- To **raise throughput:** scale horizontally (more servers), process work asynchronously via queues, batch operations, and use connection pooling.

---

## Real-World / Fintech Example

Back to our **digital wallet / payments app**, now viewed through the latency/throughput lens.

**The two requirements, stated as numbers:**
- **Latency:** "A payment confirmation must return in under **500 ms at p99**." (Not average — p99, because we care about the unlucky 1%.)
- **Throughput:** "The system must sustain **80,000 transactions per second** at festival peak."

**Tackling latency.** When Alice taps "Pay ₹500 to Bob," several things must happen: check Alice's balance, verify she has funds, debit her, credit Bob, record the transaction. Some of these are *essential* and must finish before we tell Alice "success" — these go on the **fast, synchronous path**. Others are *not* essential to confirm the payment — sending Bob a push notification, updating analytics dashboards, running deeper fraud scoring. If we did *all* of that before responding, latency would balloon past 2 seconds.

So we split the work:
- **Synchronous (must finish now):** the actual debit/credit in the ledger, wrapped in a database transaction. We make this fast by reading Alice's balance from a **cache** (memory, ~microseconds) instead of always hitting disk, and by keeping the transaction tight.
- **Asynchronous (can happen after):** we publish a "PaymentCompleted" event to **Kafka** and return success to Alice immediately. Notifications, analytics, and fraud-scoring services consume that event *later*, in the background. Alice gets her sub-500 ms confirmation; the slow work still happens, just off the critical path.

This is the single most important latency technique in fintech backends: **do the minimum required work synchronously, and push everything else to asynchronous processing.** (Async processing and message queues get full chapters later.)

**Tackling throughput.** One Spring Boot instance might handle, say, 2,000 TPS. To reach 80,000 TPS we **scale horizontally** to ~40+ stateless instances behind a load balancer (Chapter 2's lesson). We also use **connection pooling** so app servers reuse database connections instead of paying the setup cost each time, and we offload reads to **caches and replicas** so the precious database write-capacity is reserved for actual money movement.

**The interaction in action.** Suppose during peak we notice p99 latency creeping from 300 ms to 1,200 ms even though no single component is "broken." The likely cause is the **queueing effect**: servers are running too hot, and requests are waiting in line. The fix isn't to make the code faster — it's to *add more servers* to bring each one back into its healthy, flat-latency zone with headroom to spare. Recognizing that "latency spiked because we're near max throughput" — rather than blaming the code — is exactly the kind of insight these two concepts give you.

---

## Trade-offs (Pros & Cons)

Rather than pros/cons of a single tool, here the trade-offs are about **the tension between latency and throughput** and the techniques used to balance them.

**Optimizing for low latency**
- ✅ Users feel a fast, responsive product; fewer retries, higher conversion, more trust.
- ✅ Critical for real-time and financial actions where waiting causes real harm.
- ❌ Often requires *over-provisioning* (extra idle capacity for headroom) → costs more money.
- ❌ Techniques like aggressive caching add complexity and risk of serving stale data.

**Optimizing for high throughput**
- ✅ Handles huge peak load; serves more users with the same architecture pattern.
- ✅ Async/batching/horizontal scaling make the system economical at scale.
- ❌ Pushing utilization high to maximize throughput *degrades p99 latency* (the queueing effect).
- ❌ Async processing improves throughput and felt-latency but adds **eventual consistency** — the notification or analytics update lags behind the actual payment.

**The core trade-off to remember**
- ⚖️ **You usually can't max out both at once.** Running servers near 100% gives great throughput-per-dollar but terrible tail latency. Running them cool gives great latency but wastes capacity. The design target is *"hit both SLAs with sensible headroom,"* not *"maximize one."*
- ⚖️ **Averages will betray you.** Always design and measure against **percentiles (p95/p99)**, never the mean — the tail is where users actually suffer and where money is lost.

> **Staff-engineer takeaway:** Latency is the *individual's wait*; throughput is the *system's volume*. They trade off through queueing, so leave headroom and never run hot. Cut latency by moving work to faster layers (cache, nearby servers) and off the critical path (async). Raise throughput by scaling out and batching. And always, always reason in **p99**, not averages.

---

➡️ **End of Batch 1.** Next batch covers **Architectural Patterns**, **Availability & Availability Patterns**, and **Replication** — building on the scaling and performance foundations you now have.
