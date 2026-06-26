# 26. Resiliency

> Availability (Chapter 5) asked "is the system up?" Resiliency asks the deeper question: "when things break — and they *will* — how gracefully does the system bend instead of shatter?" In a distributed system, failure isn't an edge case; it's the steady state. Resiliency is the discipline of staying useful in spite of it.

---

## What is it?

**Resiliency is a system's ability to handle failures gracefully and recover from them — to keep providing service (even if degraded) when its components, dependencies, or infrastructure fail.**

It's closely related to availability but subtly different, and the difference matters:
- **Availability** (Chapter 5) is the *measurable outcome* — the percentage of time the system is up.
- **Resiliency** is the *set of properties and techniques* that *produce* that availability when failures occur. Resiliency is *how* you achieve availability in a hostile, failure-prone world.

The mindset, hammered home since Chapter 5 and now central:

> **In a distributed system, failure is constant and normal — not exceptional.** Networks drop packets, services crash, disks die, dependencies slow to a crawl, and entire data centers lose power. A resilient system is designed with the *assumption* that everything it depends on will fail eventually. The question is never "what if X fails?" but "*when* X fails, what happens?"

The opposite of resilient is **brittle** — a system where one small failure cascades into total collapse. The most feared form of this is the **cascading failure**: one slow/failed component drags down everything that depends on it, which drags down *their* dependents, until the whole system falls like dominoes. Resiliency is fundamentally about *stopping cascades* and *containing blast radius*.

Key related concepts:
- **Fault tolerance** — continuing to operate correctly even when components fail (often via redundancy, Chapter 5).
- **Graceful degradation** — losing *non-essential* functionality instead of failing entirely (Chapter 5).
- **Fail fast vs. fail safe** — failing quickly and cleanly (so callers aren't left hanging) vs. failing into a safe default state.
- **Recovery** — automatically returning to normal after a failure passes.

---

## How it Works Under the Hood

### Why failures cascade (the problem resiliency solves)

To understand resiliency, first understand *how* a small failure becomes a catastrophe. Consider service A calling slow service B:

```
1. Service B becomes slow (overloaded, GC pause, network issue).
2. Service A's calls to B start taking 30 seconds instead of 50ms.
3. Each of A's threads making a call to B is now BLOCKED for 30 seconds.
4. A's limited thread pool fills up with threads waiting on B.
5. A has NO threads left to handle ANY requests — even ones not needing B.
6. Service A is now effectively DOWN, taking down everything that depends on A.
7. The failure cascades upward until the whole system collapses.
```

The villain here is **resource exhaustion** — A ran out of threads because they were all stuck waiting on B. Notice the cruel irony: B didn't even fail completely; it just got *slow*. **Slowness is often more dangerous than outright failure**, because a fast failure frees resources immediately, while slowness holds them hostage. Most resiliency techniques are about *not letting a slow/failed dependency consume your resources*.

### The core resiliency patterns (the toolbox)

These are the standard techniques; several get deeper treatment in their own chapters (Circuit Breakers, Load Balancers).

**1. Timeouts.** Never wait forever for a dependency. Set an aggressive timeout so a slow call fails fast instead of holding a thread for 30 seconds.
> *The most fundamental resiliency rule:* **every network call must have a timeout.** A missing timeout is the #1 cause of cascading failures. It directly addresses the cascade above — A gives up on B in 1 second, freeing its thread.

**2. Retries (with backoff + jitter).** Transient failures (a brief network blip) often succeed if you just try again. So retry — but carefully:
- **Exponential backoff:** wait longer between each retry (1s, 2s, 4s...) so you don't hammer a struggling service.
- **Jitter:** randomize the wait so all clients don't retry *simultaneously* (a "retry storm" that re-overloads the recovering service — the avalanche problem from Chapter 17).
- **Only retry idempotent operations!** Retrying a non-idempotent "send money" could double-charge (Chapter 18's idempotency, again critical).

**3. Circuit Breaker.** If a dependency is clearly failing, *stop calling it* for a while — "trip the breaker" — and fail fast instead, giving the dependency time to recover. (Full chapter coming.) This is the primary defense against cascading failure.

**4. Bulkheads.** Isolate resources so a failure in one area can't consume *all* resources. Named after ship compartments: a hull breach floods one compartment, not the whole ship.
> *Mechanic:* give each dependency its *own* thread pool / connection pool. If B exhausts *its* pool, calls to C and D are unaffected because they have separate pools. The failure is *contained* — directly preventing the cascade above.

**5. Rate Limiting / Throttling.** Cap incoming load so a traffic spike (or abuse) can't overwhelm the system (Chapter 22's gateway). Shed excess load rather than collapsing under it.

**6. Fallbacks / Graceful Degradation.** When a dependency fails, return a sensible default instead of an error — cached data, a simplified response, or a "try later" message. The user gets *something* useful (Chapter 5).

**7. Load Shedding.** Under extreme overload, deliberately *reject* some requests (fail fast) to protect the system's ability to serve the rest. Better to serve 80% well than to serve 0% by collapsing.

### How they combine (the layered defense)

These aren't alternatives — they stack into a layered defense around *every* risky call:
```
A call to dependency B should be wrapped in:
  [ Bulkhead (own thread pool) ]
    [ Circuit Breaker (stop if B is failing) ]
      [ Timeout (don't wait > 1s) ]
        [ Retry w/ backoff (for transient blips, if idempotent) ]
          → actual call to B
      [ Fallback (if all else fails, return safe default) ]
```
This is exactly what resilience libraries (Resilience4j in Java, formerly Hystrix) provide — composable wrappers around each call.

---

## Why do we need it?

We need resiliency because **distributed systems fail constantly, and without resiliency a single small failure becomes a total outage** — which, for fintech, is unacceptable:

1. **Failure is inevitable at scale.** With many services, machines, and network links, *something* is always failing. A system that assumes everything works is guaranteed to go down. Resiliency is the only way to stay up in reality.

2. **It prevents cascading failures.** This is the big one. Without resiliency patterns, one slow dependency exhausts resources and topples the entire system (the domino effect above). Resiliency *contains* failures to their blast radius. Many of history's biggest outages were cascades that resiliency patterns would have stopped.

3. **It protects the user experience and the business.** A resilient system degrades gracefully (lose a feature) instead of failing totally (lose everything). For a payments app, "fraud scoring is temporarily simplified" is vastly better than "the app is down."

4. **Microservices make it mandatory.** The moment you split into services communicating over the network (Chapter 20), every call can fail. Resiliency isn't optional in a microservices world — it's the cost of admission.

**When to invest:** proportionally to the cost of failure and the number of dependencies. A critical payment path with many service dependencies needs the full toolbox (timeouts, circuit breakers, bulkheads, fallbacks). A simple internal batch job needs less. As always, match the investment to the stakes (Chapter 1).

---

## Real-World / Fintech Example

Our **digital wallet / payments app** is now a web of microservices (Chapter 20) communicating over the network — so resiliency is life-or-death. Here's the cascade it must prevent, and how.

**The cascade that almost happened.** The Payment service calls the Fraud service synchronously for scoring. One day, the Fraud service slows to a crawl (a bad ML model deploy). Without resiliency:
1. Fraud calls take 30s instead of 50ms.
2. Payment service threads block waiting on Fraud.
3. Payment's thread pool fills with stuck threads.
4. Payment can't handle *any* requests — even ones unrelated to fraud.
5. **The entire payments platform goes down** because *one* dependency got slow. Catastrophe during peak.

**How resiliency stops it — the layered defense:**
- **Timeout:** Payment waits at most **1 second** for Fraud. A slow Fraud call fails fast, freeing the thread immediately instead of holding it 30s. (The single most important fix.)
- **Circuit Breaker:** after Fraud fails repeatedly, the breaker **trips** — Payment stops calling Fraud entirely for 30 seconds and fails fast, giving Fraud room to recover instead of being hammered while down. (Next chapter.)
- **Bulkhead:** Fraud calls use their *own* thread pool. Even if it's fully exhausted, calls to the Ledger and Accounts services use *separate* pools and keep working. The failure is *contained* to fraud-related functionality.
- **Fallback / graceful degradation:** when the breaker is open, Payment falls back to a **lightweight rule-based fraud check** for low-risk amounts (approve small payments, flag for later review) instead of blocking all payments. **Payments keep flowing**; only the *deep* fraud scoring is temporarily degraded — a deliberate, pre-decided business trade-off (Chapter 5).

**Result:** a Fraud-service meltdown causes a *minor, contained degradation* (lighter fraud checks for a few minutes) instead of a *total payments outage*. That difference — bend vs. shatter — is the entire value of resiliency.

**Retries done safely.** When calling the external bank API to settle a transfer, transient network blips are common. Payment **retries with exponential backoff + jitter** — but *only* because each settlement call carries an **idempotency key** (Chapter 18), so a retry can never double-settle. They explicitly do **not** blindly retry the money-debit step without idempotency, because that would risk double-charging Alice.

**Load shedding at peak.** During an extreme festival spike beyond capacity, the gateway **sheds load** — rejecting a small fraction of low-priority requests (e.g., analytics refreshes) with a "try again" — to protect the core payment path's ability to function. Better to serve payments well and drop dashboard refreshes than to let everything collapse.

In Spring Boot, all of this is typically implemented with **Resilience4j**: `@CircuitBreaker`, `@Retry`, `@Bulkhead`, `@TimeLimiter`, and `@RateLimiter` annotations wrap the risky calls, with `fallbackMethod` providing graceful degradation — composable layers around every cross-service call.

---

## Trade-offs (Pros & Cons)

**Pros**
- **Prevents cascading failures** — contains a failure to its blast radius instead of toppling the whole system.
- **Higher availability in practice** — the system stays useful through the failures that *will* happen.
- **Graceful degradation** — lose a feature, not the whole product; far better UX.
- **Protects against slow dependencies** — timeouts/bulkheads stop resource exhaustion (the real killer).
- **Mandatory enabler of microservices** — makes network-based architectures survivable.

**Cons**
- **Added complexity** — timeouts, retries, breakers, bulkheads, and fallbacks are more code and more configuration to get right.
- **Hard to tune** — bad timeout/retry/breaker settings can *cause* problems (too-aggressive retries → retry storms; too-tight timeouts → false failures).
- **Requires idempotency** — safe retries depend on idempotent operations (extra design work).
- **Fallbacks can mask problems** — graceful degradation might hide a failing dependency unless you monitor and alert on it.
- **Testing failure is hard** — you must deliberately inject failures (chaos engineering) to verify resiliency actually works — covered next chapter.

> **Staff-engineer takeaway:** Resiliency is designing for the reality that **everything fails, constantly** — and ensuring the system *bends* (degrades gracefully) instead of *shattering* (cascading collapse). The deadliest failure mode is a **slow** dependency exhausting your resources, so the toolbox centers on *not letting that happen*: **timeouts** (every network call, no exceptions), **circuit breakers** (stop calling what's failing), **bulkheads** (isolate resource pools), **retries with backoff+jitter** (only for idempotent ops), and **fallbacks** (degrade, don't die). Layer them around every risky call (Resilience4j in Spring), and tune carefully — bad settings cause the very problems you're preventing.

---

➡️ Next: [27-Designing-for-Resiliency.md](27-Designing-for-Resiliency.md) — from individual techniques to the *holistic practice*: redundancy, isolation, chaos engineering, and the principles for architecting a system that survives the real world.
