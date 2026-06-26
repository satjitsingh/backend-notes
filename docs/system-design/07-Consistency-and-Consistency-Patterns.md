# 7. Consistency and Consistency Patterns

> Replication gave us many copies of our data. Now the hard question: when those copies disagree (and they *will*, briefly), what does a reader see? "Consistency" is the set of promises a system makes about that. Get this clear and CAP/PACELC become easy.

---

## What is it?

**Consistency is the guarantee a system makes about *which version of the data* a reader will see, especially when there are multiple copies.**

Here's the situation that makes consistency a topic at all. From Chapter 6, your data lives on a leader plus several followers, and writes take a moment to propagate. So at any instant, the copies might hold *slightly different values*. Consistency is the **promise the system makes about what you'll get when you read** during that messy in-between window.

A crucial warning right away, because this word is overloaded and causes endless confusion:

> **"Consistency" means two completely different things depending on context.**
> - The **C in ACID** (database transactions) means *"a transaction never violates the database's rules/invariants"* — e.g., a constraint or a balance never going negative. This is about *correctness within one database*.
> - The **C in CAP** (distributed systems) means *"every read sees the most recent write, no matter which copy it hits"* — i.e., all replicas agree. This is about *agreement across copies*.

This chapter is about the **CAP-style, distributed meaning**: how up-to-date and how *agreed-upon* the data you read is. Think of consistency as a **spectrum**, from "everyone always sees the latest truth instantly" (strong) to "copies will agree... eventually" (weak). Stronger consistency is easier to reason about but costs latency and availability; weaker consistency is fast and available but forces you to handle stale or surprising data.

The main points on that spectrum (we'll define each below):

| Model | Promise | Cost |
|---|---|---|
| **Strong consistency** | Every read sees the latest write, always | Slow, less available |
| **Eventual consistency** | Reads may be stale, but copies converge if writes stop | Fast, highly available |
| **Causal consistency** | Things that are causally related are seen in order | Middle ground |
| **Read-your-writes / Monotonic** | Per-user guarantees that feel intuitive | Targeted, practical |

---

## How it Works Under the Hood

### Strong consistency

**Promise:** the moment a write succeeds, *every* subsequent read — from *any* copy — returns that new value. The system behaves *as if there were only one copy of the data*, even though there are many.

**How it's achieved:** the system refuses to show you a value until all (or a majority of) relevant copies agree on it. Concretely:
- Writes use **synchronous replication** (from Chapter 6) — the write isn't "done" until replicas have it.
- Reads either go to the leader, or require a **quorum** (a majority of nodes must agree on the value before returning it).
- Often coordinated by **consensus algorithms** (Paxos, Raft) that make a cluster of machines agree on a single ordered history of writes.

**The cost:** all that coordination means waiting — every operation pays for network round trips and agreement. And if machines can't reach each other (a network partition), a strongly consistent system will **refuse to answer** rather than risk returning a wrong/old value. That's it choosing *correctness over availability* — the exact CAP trade-off in the next chapter.

### Eventual consistency

**Promise:** if you stop writing, then *eventually* (usually milliseconds, sometimes seconds) all copies will converge to the same value. But in the meantime, **different reads may return different, stale values.**

**How it's achieved:** writes are accepted quickly (often **asynchronous replication**, or any node accepts the write in leaderless systems) and propagated in the background. Nodes reconcile differences over time using techniques like:
- **Gossip protocols** — nodes chatter with each other to spread updates.
- **Conflict resolution** — when two copies were updated differently, a rule decides the winner: **Last-Write-Wins (LWW)** using timestamps, or smarter merging with **vector clocks** / **CRDTs** (data types designed to merge automatically without conflict).

**The cost:** the application must tolerate seeing **stale data** and occasionally **surprising orderings**. For a "likes" counter, who cares. For a bank balance, this is dangerous — which is why fintech uses eventual consistency only for the *cosmetic* parts.

### The practical "client-centric" patterns (the ones you'll actually implement)

Pure strong consistency is expensive; pure eventual consistency is confusing for users. So in practice we apply **targeted guarantees** that make the *experience* feel correct without paying full strong-consistency cost. These are the **consistency patterns** in the topic title:

**1. Read-Your-Own-Writes (Read-after-Write).**
Guarantee that *a user always sees their own updates*, even if others see them slightly later. (We met this in Chapter 6.) Implementation: after a user writes, route *that user's* reads to the leader (or a guaranteed-fresh replica) for a short window. Solves the "I edited my profile but it still shows the old one" problem.

**2. Monotonic Reads.**
Guarantee that a user *never sees time go backwards* — once you've seen a newer value, you won't later get an older one (which can happen if your first read hit an up-to-date replica and your second hit a lagging one). Implementation: **pin a given user to the same replica** (e.g., via a consistent hash of their user ID) so their reads come from one timeline.

**3. Monotonic Writes.**
Guarantee a single user's writes are applied **in the order they made them** (so write A always lands before write B). Implementation: route a user's writes through the same leader/path in order.

**4. Causal Consistency.**
Guarantee that operations with a **cause-and-effect relationship** are seen by everyone in the right order. Classic example: you should never see a *reply* to a comment before the *comment itself*. Causally-unrelated things can still appear in any order (that's fine and cheap). This is often the *sweet spot* — it preserves the orderings humans actually notice, without the full cost of strong consistency.

> **The key mental model:** You rarely need *global* strong consistency on *everything*. Instead, you apply the *minimum* consistency guarantee that makes each piece of data behave correctly for its purpose. Strong consistency for money; causal for comment threads; read-your-writes for profiles; eventual for view counts. **Consistency is chosen per-data-type, not once for the whole system.**

### How this connects to replication (Chapter 6)
The consistency model is essentially *a direct consequence of your replication choices*:
- Synchronous replication + reads from leader/quorum → **strong consistency**.
- Asynchronous replication + reads from any follower → **eventual consistency**.
- Routing tricks on top of async (pin user to leader/replica) → the **client-centric patterns**.

So consistency isn't a separate machine you bolt on — it's the *behavior that emerges* from how you replicate and route reads.

---

## Why do we need it?

We need to think explicitly about consistency because **the wrong consistency model either corrupts your data or needlessly destroys your performance.** It's a two-sided danger:

1. **Too weak → correctness bugs and angry users.** If you serve a bank balance from an eventually-consistent replica, a user might spend money they don't have, or see a payment vanish and reappear. In fintech, that's not a glitch — it's a financial and legal incident.

2. **Too strong → slow, fragile, expensive systems.** If you force global strong consistency on data that doesn't need it (like a "trending posts" list), you pay for coordination round trips on every read and become unavailable during network partitions — all to protect data nobody would mind being a few seconds stale.

3. **Surprising UX without the patterns.** Even when eventual consistency is *acceptable* for data integrity, raw staleness confuses users ("why did my own edit disappear?"). The client-centric patterns (read-your-writes, monotonic reads) exist to make an eventually-consistent system *feel* correct to each user, cheaply.

**When to use what:**
- **Strong consistency:** money, inventory counts, unique-username checks, anything where two readers disagreeing causes real harm.
- **Causal consistency:** messaging, comments, collaborative apps — anywhere ordering of related events matters.
- **Read-your-writes / monotonic:** user-facing reads of a user's own data (profiles, settings, their own balance display).
- **Eventual consistency:** likes, view counts, recommendation feeds, analytics, caches — high-volume data where staleness is harmless and availability/speed matter most.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** is the perfect showcase, because it deliberately uses *different consistency models for different data* — which is exactly the skill to internalize.

**Strong consistency — the money ledger.**
The actual balance and the debit/credit ledger demand **strong consistency.** When Alice pays Bob ₹500, *every* part of the system must agree instantly that Alice has ₹500 less. If a follower briefly thought Alice still had the money, she could open a second tab and spend it again — a **double-spend**. So the ledger uses synchronous replication and reads-from-leader/quorum: no read ever returns a stale balance for the purpose of *authorizing a payment*. Slower and less available during partitions — and the team accepts that, because correctness of money is non-negotiable.

**Read-your-own-writes — the balance display.**
Alice tops up ₹1,000. The app immediately shows her balance. Using read-your-writes, *her* balance reads are routed to the leader for a few seconds, so **she** always sees the ₹1,000 instantly — even though some far-off replica hasn't caught up yet. Other users don't need to see Alice's balance, so they're unaffected. Cheap, targeted, intuitive.

**Monotonic reads — transaction history.**
Alice refreshes her history and sees 10 transactions. She refreshes again — she must **never** see only 9 (time going backwards). Pinning her to a consistent replica guarantees her view only moves forward. This avoids a terrifying "did my transaction disappear?!" moment without needing full strong consistency.

**Causal consistency — support chat / notifications.**
In the in-app support chat, a reply must never appear before the message it answers. Causal consistency preserves that ordering for related messages, while unrelated messages can arrive in any order — far cheaper than making the entire chat strongly consistent.

**Eventual consistency — the cosmetic stuff.**
"You've spent ₹45,000 this month" spending-insights widget, the "trending merchants" list, the lifetime-transactions counter — all **eventually consistent**, served from lagging async replicas. If they're 5 seconds stale, nobody is harmed and the app stays fast and highly available.

> The lesson in one glance: **one app, five consistency models, each matched to what the data actually needs.** A junior engineer asks "is our system consistent?" A staff engineer answers "which data? The ledger is strong, the insights widget is eventual, and the balance display does read-your-writes." That nuance *is* the expertise.

In Spring Boot terms, the strong-consistency ledger lives behind `@Transactional` operations against the primary datasource, while the eventually-consistent widgets read from replica datasources (or a cache) and are updated by asynchronous Kafka consumers — the architecture physically separates the two consistency worlds.

---

## Trade-offs (Pros & Cons)

### Strong consistency
**Pros**
- **Simplest to reason about** — behaves like a single copy; no stale-data surprises.
- **Essential for correctness** of money, inventory, uniqueness.

**Cons**
- **Higher latency** — coordination/round trips on every operation.
- **Lower availability** — refuses to answer during network partitions (CAP, next chapter).
- **Harder to scale** — the coordination becomes a bottleneck.

### Eventual consistency
**Pros**
- **High performance & low latency** — no waiting for agreement.
- **High availability** — stays up and answers even during partitions.
- **Scales beautifully** — minimal coordination.

**Cons**
- **Stale reads** — readers may see old data temporarily.
- **Conflict handling burden** — you must resolve concurrent conflicting writes (LWW/CRDTs).
- **Confusing UX and correctness risk** if used on data that needs to be exact.

### Client-centric patterns (read-your-writes, monotonic, causal)
**Pros**
- **Make eventual consistency *feel* correct** to users, at low cost.
- **Targeted** — pay the cost only where the experience needs it.

**Cons**
- **Add routing complexity** (pinning users to replicas, tracking write timestamps).
- **Only partial guarantees** — they fix specific surprises, not global agreement.

> **Staff-engineer takeaway:** Consistency is a *spectrum*, and you choose a point on it **per data type**, not once for the whole system. Use **strong** for money, **causal** for ordered conversations, **read-your-writes / monotonic** to make user-facing reads feel right, and **eventual** for high-volume cosmetic data. Crucially, your consistency model is just the *visible behavior* of your replication and read-routing choices — and the unavoidable tension between consistency, availability, and partitions is exactly what the **CAP theorem** formalizes next.

---

➡️ Next: [08-CAP-Theorem.md](08-CAP-Theorem.md) — the famous rule that says, during a network failure, you must *choose* between consistency and availability.
