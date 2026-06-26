# 9. PACELC Theorem

> CAP only tells you what happens when the network breaks. But networks are *fine* almost all the time — so what trade-off are you making the other 99.99% of the time? PACELC answers that. It's CAP, completed.

---

## What is it?

**PACELC is an extension of the CAP theorem that adds the trade-off you face during *normal* operation, when there's no partition.** It's pronounced "pass-elk," and the name is literally the rule spelled out:

> **If there is a Partition (P), choose between Availability (A) and Consistency (C); Else (E), choose between Latency (L) and Consistency (C).**

Read it as two halves:

- **PAC** — *"during a Partition, trade Availability vs Consistency."* This is just CAP. Nothing new.
- **ELC** — *"Else (normal operation), trade Latency vs Consistency."* **This is the new and arguably more important part.**

Here's the insight that makes PACELC so valuable. CAP obsesses over partitions — but partitions are **rare**. A healthy data center might have a partition a few times a year. So CAP describes how your system behaves during a tiny fraction of its life. **PACELC asks the question that matters every single millisecond: even when everything is healthy, do you favor speed or correctness?**

Why is there a trade-off even with no partition? Because of **replication** (Chapter 6). To keep copies consistent, the system must do coordination work — wait for replicas to confirm writes, read from a quorum, etc. That waiting is **latency**. So even on a perfect network:
- Want **strong consistency**? You pay **latency** (wait for replicas to agree).
- Want **low latency**? You accept **weaker consistency** (answer from the nearest copy without waiting for agreement).

> **The core realization:** consistency is never free. During a partition it costs you *availability* (CAP's point). During normal operation it costs you *latency* (PACELC's addition). PACELC captures the *whole* truth: consistency has a price you pay **all the time**, not just during failures.

### The four PACELC categories

Every distributed system gets a two-part label — what it does during a Partition, and what it does Else:

| Label | During Partition | During Normal Operation | Meaning |
|---|---|---|---|
| **PA/EL** | Availability | Latency | "Always fast and available; consistency comes last." |
| **PC/EC** | Consistency | Consistency | "Always correct, whatever the latency or availability cost." |
| **PA/EC** | Availability | Consistency | "Stay up during partitions, but otherwise prioritize correctness." |
| **PC/EL** | Consistency | Latency | "Correct during partitions, but fast in normal times." |

The two common, "pure" personalities are **PA/EL** (speed-first systems like Cassandra, DynamoDB in default mode) and **PC/EC** (correctness-first systems like traditional RDBMS clusters, HBase, ZooKeeper/etcd, Google Spanner).

---

## How it Works Under the Hood

### The "Else" trade-off, mechanically

Picture a healthy 3-replica system (leader + 2 followers), no partition. A write comes in. The system designer has a dial:

**Dial toward Consistency (EC):**
```
Write → Leader → wait for replicas to ACK (quorum) → THEN respond "done"
```
The client waits for the network round trips to the replicas. Every write (and often every read, if you require quorum reads) pays this coordination latency. The payoff: any subsequent read, from any replica, is correct.

**Dial toward Latency (EL):**
```
Write → Leader → respond "done" immediately → replicate in background
```
The client gets an instant answer. The payoff: speed. The cost: for a brief window, replicas are stale, so a read might return old data (eventual consistency).

This is the *exact same sync-vs-async replication trade-off* from Chapter 6 — PACELC just gives it a formal name and places it alongside CAP. The "ELC" choice is essentially "**synchronous replication (EC) vs asynchronous replication (EL)** during normal operation."

### Why PACELC describes reality better than CAP

CAP makes it sound like the only interesting moment is a partition. But consider two databases that are *both* "CP" under CAP — they look identical in CAP's eyes. PACELC distinguishes them:

- A traditional RDBMS cluster: **PC/EC** — correct during partitions *and* correct (at the cost of latency) normally.
- A different system might be **PC/EL** — correct during partitions, but in normal times it answers fast from the nearest replica without full coordination.

CAP can't tell these apart; PACELC can. That's why PACELC is considered the more complete and practically useful model — it captures the trade-off you're actually tuning the vast majority of the time.

### Walking through the famous systems

**Cassandra / DynamoDB (default) → PA/EL.** During a partition, stay available (AP). During normal operation, answer from the nearest replica without waiting for global agreement → low latency, eventual consistency. Speed and uptime above all. (Both are *tunable* — you can request stronger consistency per query, nudging specific operations toward EC.)

**Traditional RDBMS (single-leader cluster), HBase, etcd/ZooKeeper → PC/EC.** During a partition, refuse rather than serve wrong data (CP). During normal operation, coordinate to stay consistent even though it adds latency. Correctness above all.

**Google Spanner → effectively PC/EC** (a famous case): it provides strong global consistency, paying a latency cost (using synchronized atomic clocks / TrueTime to order transactions globally). It chooses consistency in *both* halves — and engineers its infrastructure (super-reliable network, atomic clocks) to keep the latency and partition costs acceptably low. It's the clearest real-world statement that "if you want consistency everywhere, you *will* pay latency — so invest to make that latency small."

> **The mental model:** PACELC = CAP + "and what about the other 99.99% of the time?" The answer is always the **latency ↔ consistency** dial. Every replicated system sits somewhere on it, and good engineers tune it **per operation**, just like CAP.

---

## Why do we need it?

We need PACELC because **CAP alone leads to incomplete designs and lazy database comparisons.** Specifically:

1. **It surfaces the trade-off you make constantly, not rarely.** Designing only around partitions (CAP) is like designing a car only for crashes and ignoring how it drives day to day. PACELC forces you to consciously decide your *normal-operation* behavior — which is what users experience nearly all the time.

2. **It explains real performance.** When someone asks "why is our globally-consistent database slow?", PACELC has the answer: you chose EC, so you're paying coordination latency on every operation by design. It connects an architectural choice directly to the latency numbers from Chapter 3.

3. **It makes database comparisons meaningful.** "Both are CP" hides huge differences. "One is PC/EC, the other PC/EL" tells you which will be faster in normal operation. PACELC is a sharper tool for choosing technology.

4. **It reinforces the per-operation mindset.** Just like CAP, the ELC choice should be made per data type — strong/EC for money, fast/EL for cosmetic data.

**When to lean which way (the ELC choice):**
- **Favor Consistency (EC)** when correctness matters more than a few milliseconds: financial writes, inventory, anything where stale reads cause real harm. Accept the latency.
- **Favor Latency (EL)** when speed and scale dominate and slight staleness is harmless: feeds, catalogs, counters, recommendations, caches. Accept eventual consistency.

---

## Real-World / Fintech Example

Our **digital wallet / payments app**, viewed through PACELC, finally gets a *complete* description of its behavior — not just "what happens during a partition" but "what trade-off it makes every millisecond."

**The money ledger → PC/EC.**
- **P (partition):** choose **Consistency** — refuse/delay payments rather than risk a double-spend (the CP choice from Chapter 8).
- **E (normal operation):** *also* choose **Consistency** — even on a perfectly healthy network, a write to the ledger waits for synchronous replication and quorum agreement before confirming "success." Yes, this adds a few milliseconds of latency to every payment. The business *gladly* pays that latency tax, because the alternative — a balance that's momentarily wrong — is unacceptable for money. This is the PC/EC personality: **correctness in both worlds, latency be damned.**

Like Google Spanner, the team then *invests* to make that consistency-latency as small as possible (co-locating the ledger's replicas in the same region, fast networking, semi-sync replication to just one nearby replica) — embracing the cost but engineering it down.

**Transaction history & balance *display* → PC/EL (or PA/EL).**
- **E (normal operation):** choose **Latency** — serve balance/history reads from the nearest replica or cache *immediately*, without waiting for global agreement. These reads are ~10× more frequent than writes, so paying coordination latency on each one would be slow and wasteful. A read that's a few milliseconds stale is fine here (and read-your-writes from Chapter 7 patches the one case — your *own* recent change — where staleness would confuse you).
- Result: snappy, fast app for the read-heavy parts.

**Trending merchants / spending insights → PA/EL.**
Pure speed and availability. Eventually consistent, always fast, never blocks. The most relaxed corner of the dial.

> The complete fintech picture: the **ledger is PC/EC** (correct always, pays latency), while the **read-heavy display and cosmetic features are EL** (fast, eventually consistent). CAP told us how each behaves *during a partition*; PACELC adds how each behaves *the rest of the time* — and that "rest of the time" is what defines the everyday feel and cost of the system. A staff engineer specifies *both* halves of the label for every critical data path.

In Spring Boot terms: the ledger's writes go through synchronous/quorum replication on the primary (EC — accept the latency), while balance/history endpoints read from replicas or a Redis cache and respond instantly (EL), with async Kafka consumers keeping the relaxed data fresh-enough in the background.

---

## Trade-offs (Pros & Cons)

### Favoring Consistency in normal operation (EC)
**Pros**
- **Always-correct reads** — no stale-data surprises, even momentarily.
- **Simplest to reason about** — behaves like a single truthful copy at all times.
- **Mandatory for money, inventory, uniqueness.**

**Cons**
- **Higher latency on every operation** — you pay the coordination cost constantly, not just during failures.
- **Lower throughput** — coordination limits how fast you can go.
- **Costlier to scale** geographically (cross-region consistency = cross-region latency).

### Favoring Latency in normal operation (EL)
**Pros**
- **Fast responses** — no waiting for replica agreement.
- **High throughput and great UX** for read-heavy, tolerant data.
- **Scales across regions cheaply** — answer locally, sync later.

**Cons**
- **Eventual consistency** — stale reads in the replication-lag window.
- **Conflict handling burden** when concurrent writes diverge.
- **Wrong for exact data** (money, limited inventory).

### PACELC as a concept
**Pros**
- **More complete than CAP** — covers normal operation, where systems spend ~all their time.
- **Sharper for comparing databases** (distinguishes systems CAP calls identical).
- **Directly connects architecture to everyday latency/throughput.**

**Cons / limitations**
- **Still a simplification** — treats consistency/latency as a binary dial when both are spectrums.
- **More nuanced to communicate** — the four-way label takes more explaining than CAP's catchy "pick two."

> **Staff-engineer takeaway:** PACELC completes CAP by adding the trade-off you make **all the time, not just during partitions**: *Else, Latency vs Consistency*. Consistency is **never free** — it costs availability during partitions and **latency during normal operation**. Label each critical data path with *both* halves: make your money ledger **PC/EC** (correct always, pay the latency and engineer it down), and make read-heavy/cosmetic data **PA/EL or EL** (fast and available, eventually consistent). This per-operation tuning across *both* failure and normal modes is the complete mental model for distributed data.

---

➡️ **End of Batch 3.** You now have the full theoretical core: consistency models, CAP (the partition trade-off), and PACELC (the everyday trade-off). The next batch grounds all this theory in concrete storage technology: **Database & Storage**, **Relational Databases**, and **Database Isolation Levels** — where ACID, transactions, and the practical tools for consistency live.
