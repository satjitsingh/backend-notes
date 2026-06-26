# 29. Circuit Breakers

> The single most important pattern for stopping cascading failures. Borrowed straight from electrical engineering: when something downstream is failing dangerously, *cut the connection* before the damage spreads. We met it briefly in Chapter 26 — now we go deep on the mechanics that make it work.

---

## What is it?

**A circuit breaker is a resiliency pattern that monitors calls to a dependency and, when failures cross a threshold, "trips" to stop sending calls for a while — failing fast instead of repeatedly hammering a service that's already in trouble.**

The name comes directly from your home's electrical panel. An electrical circuit breaker detects a dangerous surge and *cuts the circuit* to prevent a fire. When the danger passes, you flip it back on. A software circuit breaker does exactly this for service-to-service calls:

> When a dependency starts failing, **continuing to call it is actively harmful** — it wastes resources, holds threads (the cascade from Chapter 26), and piles load onto a service that needs *less* load to recover. The circuit breaker recognizes "this dependency is sick" and *stops calling it*, failing fast (returning an error or fallback immediately) instead of waiting and retrying into the void. This gives the sick service breathing room to recover, and protects the caller from resource exhaustion.

It directly attacks the deadliest failure mode from Chapter 26: a **slow or failing dependency exhausting the caller's resources and cascading**. The circuit breaker is the primary defense.

The crucial behavioral insight:

> A circuit breaker **trades a few failed requests for the survival of the whole system.** When tripped, requests to the dependency fail *immediately* (fast) rather than slowly (timeout). Fast failure is *good* — it frees resources instantly and lets the caller respond (with a fallback) rather than hang. **A fast, clean failure is far better than a slow, resource-hogging one.**

---

## How it Works Under the Hood

### The three states (the heart of the pattern)

A circuit breaker is a state machine with three states. Understanding the transitions *is* understanding the pattern:

```
                  failure threshold exceeded
       ┌──────────────────────────────────────────┐
       │                                            ▼
  ┌─────────┐                                  ┌─────────┐
  │ CLOSED  │                                  │  OPEN   │
  │(normal) │                                  │(tripped)│
  └─────────┘                                  └─────────┘
       ▲                                            │
       │ success                          after timeout, try one request
       │                                            ▼
       │            success           ┌──────────────────┐
       └──────────────────────────────│    HALF-OPEN      │
                                       │ (testing recovery)│
                                       └──────────────────┘
                                          │ failure → back to OPEN
```

**1. CLOSED (normal operation).** Calls flow through to the dependency as usual. The breaker *counts failures* (and successes) in a rolling window. As long as the failure rate stays below the threshold, it stays closed. ("Closed" = circuit complete = electricity/requests flow — the electrical metaphor.)

**2. OPEN (tripped — failing fast).** When failures exceed the threshold (e.g., "50% of the last 20 calls failed"), the breaker **trips OPEN**. Now *all* calls to the dependency **fail immediately** without even attempting the call — returning an error or invoking a fallback instantly. No threads are held, no load hits the sick dependency. The breaker stays open for a configured **cool-down period** (e.g., 30 seconds), giving the dependency time to recover.

**3. HALF-OPEN (testing recovery).** After the cool-down, the breaker moves to HALF-OPEN and cautiously lets **a limited number of trial requests** through to test the waters:
- If they **succeed**, the dependency has recovered → the breaker closes (back to normal).
- If they **fail**, the dependency is still sick → the breaker re-opens for another cool-down period.

This half-open probing is what makes the breaker *self-healing* — it automatically detects recovery and resumes normal operation without human intervention, while avoiding a flood of requests at a still-fragile service.

### The configuration knobs (and why tuning matters)

A circuit breaker's behavior is governed by parameters you must tune for each dependency:
- **Failure threshold:** what fraction/count of failures trips it (e.g., 50% failure rate over a sliding window of 20 calls). Too sensitive → trips on normal blips (false positives). Too lenient → too slow to protect.
- **Sliding window:** count-based (last N calls) or time-based (last N seconds). Determines how failures are measured.
- **Wait duration in OPEN:** how long to stay tripped before testing (the cool-down). Too short → keeps hammering a recovering service; too long → unnecessary downtime after recovery.
- **Half-open trial count:** how many test calls to allow when probing recovery.
- **What counts as a "failure":** timeouts, exceptions, certain HTTP status codes (5xx but maybe not 4xx, since a 404 isn't the dependency's fault).

> **Tuning is genuinely hard** and a common source of incidents (recall Chapter 26's warning). Bad settings can *cause* problems — a too-sensitive breaker trips constantly and degrades a healthy system; a too-lenient one fails to protect. You tune based on the dependency's real behavior and validate with chaos testing (Chapter 27).

### How it composes with other patterns

A circuit breaker rarely works alone — it's part of the layered defense from Chapter 26:
- **+ Timeout:** the timeout defines what "too slow" means; a timed-out call counts as a failure toward tripping the breaker. *(Without timeouts, the breaker can't even detect slowness.)*
- **+ Fallback:** when the breaker is OPEN, you don't just error — you invoke a **fallback** (cached data, default value, degraded feature) so the user gets a graceful response. The breaker + fallback together *are* graceful degradation (Chapter 5).
- **+ Bulkhead:** isolate the breaker's dependency in its own resource pool so even pre-trip failures are contained.
- **+ Retry:** retries handle *transient* blips; the breaker handles *sustained* failure. They operate at different timescales (retry = milliseconds-seconds; breaker = seconds-minutes).

### Circuit breaker vs. retry (a common confusion)
| | Retry | Circuit Breaker |
|---|---|---|
| Handles | *Transient* failures (brief blip) | *Sustained* failures (dependency down) |
| Action | Try again (hoping it works) | Stop trying (it clearly won't) |
| Risk if misused | Retry storm (more load on sick service) | Tripping on healthy service |
| Together | Retry a few times; if failures persist, the breaker trips and stops the retries |

> They're complementary: retries give transient errors a second chance; the breaker steps in when retries would just be flogging a dead dependency. Used together (carefully), retries handle blips and the breaker prevents retry storms from becoming cascades.

---

## Why do we need it?

We need circuit breakers because **they are the primary mechanism that stops a single failing dependency from cascading into total system collapse** — the #1 resiliency concern in distributed systems:

1. **They stop cascading failures.** This is *the* reason. Without a breaker, a slow dependency holds the caller's threads until they're exhausted, and the failure climbs the dependency tree until everything is down (Chapter 26's domino effect). The breaker cuts the connection, freeing resources and halting the cascade at its source.

2. **They protect the failing service from being overwhelmed.** A struggling service that keeps getting hammered can *never* recover — the load keeps it down. By stopping calls, the breaker gives it the breathing room to come back. (Counterintuitively, *not calling* a service is sometimes the kindest thing you can do for it.)

3. **They enable fast failure + graceful degradation.** Failing fast (vs. slow timeouts) means the caller can immediately invoke a fallback and give the user a useful, degraded response instead of a hung request. Better UX under failure.

4. **They're self-healing.** The half-open state automatically detects recovery and resumes normal operation — no manual intervention, no 3 AM page to "turn it back on."

**When to use:** wrap *every* call to a dependency that could be slow or fail — especially **external services** (third-party APIs, payment gateways) and **inter-service calls** in a microservices architecture (Chapter 20). 

**When NOT to / cautions:** for purely local, in-process operations (no breaker needed — nothing to cascade through). And be careful tuning: an unnecessary or mis-tuned breaker on a reliable dependency adds complexity and can cause false trips.

---

## Real-World / Fintech Example

This is the resolution of the cascading-failure scenario we set up in Chapter 26, now seen through the circuit breaker's mechanics.

**The setup.** Our **payments app**'s Payment service calls the **Fraud service** synchronously for risk scoring on each transaction. The Fraud service depends on an ML model that occasionally degrades. We wrap the Payment → Fraud call in a **circuit breaker** (Resilience4j).

**Normal operation (CLOSED).** Fraud responds in ~50ms; calls flow through; the breaker counts mostly successes and stays closed. Business as usual.

**Fraud service degrades.** A bad model deploy makes Fraud slow (responses climbing past several seconds) and error-prone:
1. The **timeout** (1s) starts firing — slow calls are cut off and counted as failures.
2. Within the breaker's sliding window, the failure rate crosses **50%**.
3. The breaker **trips OPEN.** Now every Payment → Fraud call **fails instantly** — no thread is held waiting, no further load hits the struggling Fraud service.
4. Because the call fails fast, Payment immediately invokes its **fallback**: a lightweight rule-based fraud check (approve low-risk small payments, flag larger ones for later review). **Payments keep flowing.**

**The cascade is prevented.** Without the breaker, Payment's threads would have piled up waiting on slow Fraud calls, exhausted the pool, and taken down *all* payments (Chapter 26). Instead: Fraud's failure is *contained* to a minor, deliberate degradation (lighter fraud checks for a few minutes), and the core payment path stays healthy. **Bend, not shatter.**

**Self-healing (HALF-OPEN → CLOSED).** After the 30-second cool-down, the breaker goes **HALF-OPEN** and lets a few trial payments call Fraud. The ops team has rolled back the bad model, so Fraud now responds quickly — the trial calls **succeed**, and the breaker **closes**. Full fraud scoring resumes automatically, with zero manual intervention. Had Fraud still been sick, the trials would have failed and the breaker would re-open for another cool-down.

**Where they're especially vital: external dependencies.** The breaker matters most for the **external bank/settlement API** (outside the team's control). When that third party has an outage, the breaker trips and the app queues settlements for later (Chapter 18's async) rather than failing every payment synchronously — and stops bombarding the bank's struggling API. Combined with **idempotent retries** (Chapter 18) for transient blips, the app handles external instability gracefully.

**Tuned and tested.** The team tunes thresholds per dependency (the bank API gets more lenient settings than internal services, since cross-internet calls are noisier) and **validates with chaos engineering** (Chapter 27) — deliberately injecting Fraud-service latency in game days to confirm the breaker trips, the fallback works, and payments survive. *An untested breaker is just hope (Chapter 27).*

In Spring Boot: `@CircuitBreaker(name = "fraudService", fallbackMethod = "lightweightFraudCheck")` from **Resilience4j**, with thresholds/windows/wait-durations configured in `application.yml`, composed with `@TimeLimiter`, `@Retry`, and `@Bulkhead` for the full layered defense.

---

## Trade-offs (Pros & Cons)

**Pros**
- **Stops cascading failures** — the primary defense against one failure toppling the system.
- **Protects the failing dependency** — stops the hammering so it can recover.
- **Fast failure** — frees resources instantly and enables immediate fallback (vs. slow timeouts).
- **Self-healing** — half-open probing auto-detects recovery, no manual intervention.
- **Better UX under failure** — pairs with fallbacks for graceful degradation.

**Cons**
- **Tuning is hard** — wrong thresholds/cool-downs cause false trips (degrading healthy systems) or fail to protect.
- **Added complexity** — another stateful component per dependency to configure, monitor, and reason about.
- **Fallbacks can mask issues** — if a breaker is silently open, the underlying problem may go unnoticed without good monitoring/alerting.
- **Requires good observability** — you need to monitor breaker state and alert when breakers open.
- **Not a fix for the root cause** — it *contains* failure; the failing dependency still needs to be repaired.

> **Staff-engineer takeaway:** The circuit breaker is the **#1 pattern for stopping cascading failures** — borrowed from electrical engineering, it *cuts the connection* to a failing dependency so the failure can't spread and the dependency can recover. Master its **three states**: CLOSED (normal, counting failures) → OPEN (tripped, fail fast) → HALF-OPEN (probe for recovery) → CLOSED. Wrap **every external and inter-service call** in one, **compose it with timeouts, fallbacks, and bulkheads** (Chapter 26), **tune it per dependency**, monitor breaker state, and **prove it works with chaos testing**. Remember the core trade: a few fast failures now to prevent total collapse later.

---

➡️ Next: [30-System-Essentials.md](30-System-Essentials.md) — a consolidation of the essential cross-cutting concepts every system needs: security, observability, idempotency, rate limiting, and more.
