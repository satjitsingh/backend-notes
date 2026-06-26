# LTIMindtree — 1-Page Cheat Sheet (Last-Minute Revision)

> Glance through this 30 min before the call. One-liners + the "plain-terms" hook for each. Full detail is in `LTIMindtree_Interview_Prep.md`.

---

## YOUR 4 POWER STORIES (lead with these)
| Story | One-liner | Numbers |
|---|---|---|
| **Batch optimization** | Bulk DB ops + Java multithreading on a billing batch job | 45+ min → **<2 min (95%↓)** |
| **AOP refactor** | AspectJ → Spring AOP, fixed N+1 query patterns | **+60% ORM throughput** |
| **Monitoring framework** | Shell + cron proactive alerting | **MTTR −80%** |
| **Modernization** | Java 6/Hibernate 3 → Java 21/Jakarta migration | Killed tech debt |

**30-sec project pitch:** "Enterprise billing & charging platform at Amdocs — Java 8–21, Spring Boot, Hibernate, Oracle. I own billing components end-to-end: design, perf tuning, P1 support, data-integrity tooling."

---

## CORE JAVA
- **Stream pipeline** = source → intermediate (lazy: `map`/`filter`) → terminal (eager: `collect`). *Plain terms: a recipe; nothing cooks until the final "serve" step.*
- **`map` vs `flatMap`**: transform 1→1 vs flatten nested.
- **HashMap** = array of buckets; `hashCode`→bucket, `equals`→exact match; collision→linkedlist→**tree after 8**; resize at **0.75**. *Plain terms: labeled drawers; hashCode picks the drawer, equals finds the item.*
- **equals+hashCode**: always override together or map breaks.
- **`==` vs equals**: reference vs value.
- **final/finally/finalize**: constant / always-runs block / dead GC hook.
- **Checked vs unchecked**: compile-time recoverable vs runtime bug.
- **Java 21**: records, sealed, pattern matching, **virtual threads** (millions of cheap threads for I/O). *Plain terms: virtual threads = hire 10,000 cheap temp workers instead of 200 expensive full-timers.*

## CONCURRENCY
- **Runnable vs Callable**: no return vs returns value/throws.
- **ExecutorService > new Thread()**: reuse, bounded, managed.
- **Thread-safe**: immutability, `synchronized`/`ReentrantLock`, `Atomic*`, `ConcurrentHashMap`.
- **volatile** = visibility only (NOT atomic). **synchronized** = visibility + atomicity.
- **Deadlock** = circular lock wait; fix with lock ordering. *Plain terms: two people each holding the tool the other needs.*

## SPRING / SPRING BOOT
- **Boot** = Spring + auto-config + starters + embedded server. *Plain terms: furnished apartment vs empty flat.*
- **Auto-config**: classpath + `@ConditionalOn...` → beans created automatically.
- **DI/IoC**: container wires dependencies; **constructor injection** preferred. *Plain terms: don't build your own engine — it's delivered ready-fitted.*
- **@Component/@Service/@Repository/@Controller**: same thing, different layer label; `@Repository` translates DB exceptions.
- **@Controller vs @RestController**: view name vs JSON body (`@ResponseBody`).
- **@Transactional**: proxy begins/commits/rolls-back; rollback only on **unchecked** by default (`rollbackFor` for checked). Fails on **private/self-invocation**. *Plain terms: all-or-nothing wrapper around DB work.*
- **Propagation**: REQUIRED (join/create) vs REQUIRES_NEW (suspend+new).
- **AOP** = cross-cutting (logging/tx) via proxies; AspectJ = compile-time weaving. *Plain terms: a wrapper around methods that adds behavior without touching their code.*
- **Global exceptions**: `@RestControllerAdvice` + `@ExceptionHandler`.

## HIBERNATE / JPA / SQL
- **JPA** = spec, **Hibernate** = implementation.
- **N+1** = 1 query for parents + 1 per child. Fix: **JOIN FETCH**, `@EntityGraph`, `@BatchSize`. *Plain terms: grabbing 1 grocery per trip instead of one big trip.*
- **Lazy vs Eager**: load-on-access vs load-now. ToMany=lazy, ToOne=eager by default.
- **LazyInitializationException**: accessed lazy data after session closed.
- **L1 cache** = per-session (default); **L2** = shared across sessions.
- **save/persist/merge**: returns id / void-new / reattach detached.
- **Optimize SQL**: EXPLAIN PLAN, indexes, no SELECT *, avoid func on indexed col, bulk DML.
- **2nd highest salary**: `DENSE_RANK() OVER (ORDER BY salary DESC)` then `=2`.
- **WHERE vs HAVING**: before grouping vs after aggregation.
- **DELETE/TRUNCATE/DROP**: rows(rollback) / all-fast(no rollback) / table gone.
- **ACID**: Atomicity, Consistency, Isolation, Durability.
- **CDC** = capture row changes (insert/update/delete) for audit/sync.

## MICROSERVICES ⚠️ (be honest: "modular monolith experience, but I understand…")
- **Micro vs mono**: independent deploy/scale vs simple but coupled.
- **Comms**: REST/gRPC (sync) vs Kafka/RabbitMQ (async events).
- **Saga** = distributed tx via local txns + **compensating** undo; choreography (events) vs orchestration (coordinator). *Plain terms: no global undo button, so each step has its own "reverse" step.*
- **Circuit breaker** (Resilience4j): stop calling a failing service, fail fast, retry later. *Plain terms: a fuse that trips to protect the system.*
- **Distributed tracing**: correlation/trace ID across services (Zipkin/Jaeger).
- **API Gateway** = single entry (routing/auth); **Eureka** = service discovery.
- **Scalable REST API**: stateless + horizontal scale, caching, pagination, idempotency, versioning.
- **Kafka**: topics → partitions (ordering+parallelism) → consumer groups; order only within a partition.
- **JWT**: signed stateless token, validated per request (no server session).

## CODING (think aloud → brute force → optimize → complexity → edge cases)
- Reverse/palindrome: `new StringBuilder(s).reverse()`.
- Freq/first-unique: `groupingBy(c->c, LinkedHashMap::new, counting())`.
- Dedup keep order: `.distinct()`.
- Two-sum: HashSet, O(n).
- Sort map by value: `Map.Entry.comparingByValue(reverseOrder())`.

## HR (keep short, honest, positive)
- **Why leave Amdocs?** Growth + modern distributed/cloud exposure (never bad-mouth).
- **Notice period?** State honestly, mention negotiability.
- **Salary?** "Fair market-aligned hike, open to discussion."
- **Strength**: backend perf + ownership (quantified). **Weakness**: real + improving.
- **Ask them**: team's biggest challenge, success in 6 months, growth path.

---
**Mindset:** Real quantified wins + honesty about gaps. Lead with your stories. You've got this.
