# 5. Availability and Availability Patterns

> A system that's fast and scalable but *down* is worth nothing. Availability is the art of staying up even when individual parts inevitably fail — because at scale, something is *always* failing.

---

## What is it?

**Availability is the percentage of time a system is up and able to do its job.**

That's the simple definition. But the mindset behind it is what matters, and it's a mindset shift many engineers struggle with at first:

> **At scale, failure is not an exception — it is the normal, constant state of the world.** Disks die every day. Networks drop packets. A whole data center loses power. Software has bugs. If you have thousands of machines, several of them are broken *right now.*

So availability is **not** about *preventing* failures (impossible). It's about **designing a system that keeps working *despite* failures.** The goal is for the *system* to stay up even though its *parts* are constantly dying. This property has a name: **fault tolerance** — the ability to tolerate faults without going down.

### How availability is measured: "the nines"

Availability is expressed as a percentage of uptime, and engineers talk about it in terms of how many **nines** it has. Each extra nine is dramatically harder and more expensive to achieve:

| Availability | Nickname | Downtime per year | Downtime per day |
|---|---|---|---|
| 99% | "two nines" | ~3.65 days | ~14 minutes |
| 99.9% | "three nines" | ~8.8 hours | ~1.4 minutes |
| 99.99% | "four nines" | ~52 minutes | ~8.6 seconds |
| 99.999% | "five nines" | ~5.3 minutes | ~0.86 seconds |

> Look closely at the jump: going from 99.9% to 99.99% means cutting yearly downtime from ~9 hours to under an hour. Each nine roughly **10×'s the engineering effort and cost.** This is why you *negotiate* the target — "five nines" sounds great until you see the price tag. You buy only as many nines as the business genuinely needs.

### Two related terms you'll hear: SLA, SLO, SLI
- **SLI (Indicator):** the actual measured number, e.g., "we were up 99.95% last month."
- **SLO (Objective):** the internal target you aim for, e.g., "we want 99.99%."
- **SLA (Agreement):** the *contractual promise* to customers, often with penalties (refunds) if you miss it. SLAs are usually set *below* SLOs to leave a safety buffer.

### Availability vs. Reliability (a subtle but important distinction)
- **Availability** = "Is it up *right now*?"
- **Reliability** = "Does it work *correctly* and consistently over time, without failing?"

A system can be *available* but *unreliable* (it's up, but it keeps giving wrong answers). The ideal is both. For fintech, reliability is arguably even more sacred than availability — being up but corrupting balances is far worse than being briefly down.

---

## How it Works Under the Hood

Achieving high availability comes down to a few core techniques, all variations on one theme: **eliminate single points of failure by having spares ready.**

### The enemy: the Single Point of Failure (SPOF)

A **single point of failure** is any one component whose death takes down the whole system. One database, one load balancer, one server in a critical path — if there's only *one* of something essential, it's a SPOF. The entire practice of availability engineering is, fundamentally, **hunting down every SPOF and giving it a backup.**

### Technique 1: Redundancy (have spare copies)

Redundancy means **running more than one of everything important**, so if one dies, another takes over. There are two flavors, and the difference is critical:

**Active-Passive (failover) redundancy.**
You have a primary that does all the work, and a standby that sits idle, ready to take over. A health-check constantly pings the primary. When the primary dies, the system **fails over** — promotes the standby to be the new primary.

```
   [ Active (Primary) ] <--- handles all traffic
          |
   (health check / heartbeat)
          |
   [ Passive (Standby) ] <--- idle, waiting to take over
```

- **Pro:** simpler; the standby is a clean, ready copy.
- **Con:** the standby's capacity is "wasted" while idle, and there's a brief gap during failover (seconds to minutes) where you might drop requests or lose the most recent un-replicated data.

**Active-Active redundancy.**
*All* copies handle traffic simultaneously, behind a load balancer. If one dies, the others simply absorb its share.

```
        [ Load Balancer ]
         /            \
   [ Active ]      [ Active ]   <- both serving traffic right now
```

- **Pro:** no wasted capacity, near-instant failover (the others are already serving), and you get scaling *and* availability at once.
- **Con:** harder — all copies must stay in sync and handle concurrent updates, which raises consistency challenges.

### Technique 2: Failover (detect death, switch automatically)

Redundancy is useless if nobody notices the primary died. **Failover** is the automatic process of: (1) **detecting** failure via health checks/heartbeats, then (2) **redirecting** traffic to a healthy spare. The key design questions are *how fast you detect* (shorter = better availability, but too aggressive risks false alarms) and *whether failover loses any in-flight data* (depends on how up-to-date the standby is — which is a *replication* question, covered next chapter).

### Technique 3: Eliminate SPOFs at every layer

True high availability requires redundancy at *every* layer, because the system is only as available as its weakest link:
- **Multiple app servers** behind a load balancer (Chapter 2's horizontal scaling gives you this for free).
- **Multiple load balancers** (otherwise the load balancer itself is a SPOF!).
- **Database replicas** with automatic failover (next chapter).
- **Multiple Availability Zones (AZs)** — physically separate data centers in a region, so a fire/flood/power-loss in one building doesn't take you down.
- **Multiple regions** — for the highest tier, run in entirely different geographic regions, so even a whole-region outage is survivable.

### Technique 4: Graceful degradation (partial failure ≠ total failure)

A subtle but powerful pattern: when a *non-essential* component fails, the system should **lose that feature, not collapse entirely.** If the "recommended for you" service is down, the app should still let you check out — just without recommendations. This requires deliberately designing components to fail in isolation (closely tied to **circuit breakers**, which get their own chapter).

### How availability multiplies (the math that bites you)

Here's a counterintuitive trap. If your request must pass through several components *in series*, and each is 99.9% available, the *combined* availability is the **product**, not the minimum:

```
99.9% × 99.9% × 99.9% = ~99.7%
```

Three reliable components chained together are *less* available than any one of them alone. This is why **microservices can hurt availability**: a request touching 10 services has 10 chances to fail. The fix is redundancy (which *adds* availability — parallel paths) and graceful degradation (so non-critical hops don't count against you).

---

## Why do we need it?

We need availability because **downtime translates directly into lost money, lost trust, and sometimes legal/regulatory consequences** — and nowhere is this sharper than in fintech.

1. **Downtime is expensive, immediately.** For a payments company, every minute down is transactions not happening — direct, measurable revenue loss, plus merchants and users who can't operate.

2. **Trust is fragile and slow to rebuild.** People forgive a slow app occasionally. They do *not* forgive a banking app that's down when they need to pay rent or that loses their money. A few high-profile outages can permanently damage a fintech brand.

3. **Regulation demands it.** Financial systems are often *legally required* to meet uptime and reliability standards. Outages can trigger fines and audits.

4. **Failures are guaranteed at scale.** This isn't pessimism; it's statistics. With enough machines, hardware *will* fail today. Without availability design, every one of those failures is a user-facing outage.

**When do you invest in higher availability?** Proportionally to the cost of being down. An internal analytics dashboard can tolerate "three nines" (a few hours down a year is fine). A payment-authorization path needs "four or five nines" because being down even briefly is catastrophic. The skill is matching the (expensive) availability target to the real business stakes — not blindly chasing five nines everywhere.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** needs the payment path to hit **99.99% availability** (≤ ~52 minutes down per year). Here's how the patterns combine to get there.

**Killing SPOFs layer by layer:**
- **App servers:** already horizontally scaled to ~40 stateless Spring Boot instances behind a load balancer. If three crash, the load balancer routes around them and the other 37 carry on. ✅ No SPOF.
- **Load balancer:** we don't run just one (that'd be a SPOF!). We run redundant load balancers in **active-active**, with DNS failover in front. ✅
- **Database:** one primary handling writes, with **replicas** in standby (active-passive). A heartbeat monitors the primary; if it dies, an automated **failover** promotes a replica to primary within seconds. ✅ (The "did we lose the last few transactions?" question depends on replication mode — exactly the topic of the next chapter.)
- **Data centers:** everything runs across **3 Availability Zones**. If an entire AZ loses power (it happens), the other two keep serving. ✅

**Graceful degradation in action.** During a partial outage, the `fraud-scoring` service becomes unreachable. Do we block *all* payments? No — that would tank availability. Instead, the system **degrades gracefully**: small, low-risk payments are auto-approved with a lighter rule set, while the system flags them for review once fraud-scoring recovers. The core feature (paying) stays up; only the *enhancement* (deep fraud checks) is temporarily reduced. Critically, this is a *business decision* about acceptable risk — in fintech you balance availability against safety very deliberately.

**The availability-math lesson.** The team notices that chaining Payment → Fraud → Notification services in series *lowers* combined availability. So they re-architect: only the truly essential ledger write is synchronous, and Fraud/Notification happen **asynchronously over Kafka**. Now a Notification outage *cannot* take down a payment — it's off the critical path entirely. This is availability and latency design working hand in hand (recall Chapter 3's async lesson).

**The honest trade-off they accept:** hitting 99.99% costs real money — redundant everything, multi-AZ infrastructure, on-call rotations, chaos testing. They decided the payment path is worth it, but the internal *reporting dashboard* only gets 99.9% (single-AZ, fewer replicas), because nobody loses money if a report is briefly unavailable. **Right-sizing availability per component is the mature move.**

---

## Trade-offs (Pros & Cons)

### Pursuing high availability (in general)
**Pros**
- **The system survives the failures that *will* happen** — individual deaths don't become outages.
- **Protects revenue and trust**, which for fintech is existential.
- **Meets regulatory/contractual (SLA) obligations.**
- Redundancy often **doubles as scaling** (active-active gives you both at once).

**Cons**
- **Cost rises steeply** — each additional "nine" can roughly 10× the cost (more machines, more data centers, more engineering).
- **Complexity rises** — failover logic, health checks, multi-AZ/region setups, and chaos testing all add moving parts (and each part can itself fail).
- **Tension with consistency** — keeping redundant copies in sync raises the CAP-theorem trade-offs (upcoming chapters). Active-active especially forces hard consistency choices.
- **Diminishing returns** — chasing five nines for a system that doesn't need it wastes money you could spend elsewhere.

### Active-Passive vs. Active-Active (quick comparison)
| | Active-Passive | Active-Active |
|---|---|---|
| Spare capacity | Idle (wasted) until failover | Fully used all the time |
| Failover speed | Slower (promote the standby) | Near-instant (others already serving) |
| Complexity | Simpler | Harder (all nodes sync + concurrent writes) |
| Bonus | — | Also provides horizontal scaling |

> **Staff-engineer takeaway:** Availability = *staying up despite inevitable failures*, achieved by hunting down every single point of failure and giving it redundancy + automatic failover. Measure it in "nines," and **buy only the nines the business actually needs** — each one costs ~10× more. Use graceful degradation and async paths so non-critical failures never become total outages. And never forget the multiplication trap: chaining components in series *reduces* availability, so add parallel redundancy where it counts.

---

➡️ Next: [06-Replication.md](06-Replication.md) — the mechanism that makes redundant data copies possible, and the heart of both availability *and* the consistency trade-offs to come.
