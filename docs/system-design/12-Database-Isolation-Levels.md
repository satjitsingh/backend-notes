# 12. Database Isolation Levels

> The "I" in ACID, opened up. This is the single most under-studied topic that separates engineers who *think* they understand databases from those who really do. Isolation levels are the precise dial controlling how concurrent transactions see each other — and the source of some of the nastiest, hardest-to-reproduce bugs in fintech.

---

## What is it?

**An isolation level is a setting that controls *how much* concurrent transactions are protected from interfering with each other.** It's the tuning knob behind the "I" (Isolation) of ACID.

Here's the fundamental tension. In an ideal world, every transaction would run as if it were the *only* one touching the database — perfect isolation, called **serializability** (the result is as if transactions ran one-at-a-time in some serial order). But enforcing that perfectly requires heavy locking or coordination, which **kills concurrency and performance**. So databases offer a *menu* of isolation levels, letting you trade safety for speed:

> **Higher isolation = more correctness, less concurrency (slower). Lower isolation = more concurrency (faster), but exposes you to specific concurrency bugs called "anomalies."**

The SQL standard defines four levels, from weakest to strongest:

| Level | Speed | Safety |
|---|---|---|
| **Read Uncommitted** | Fastest | Weakest — almost no protection |
| **Read Committed** | Fast | Decent — the common default |
| **Repeatable Read** | Slower | Strong — stable reads within a transaction |
| **Serializable** | Slowest | Strongest — as if transactions ran one at a time |

To understand the levels, you first need to understand the **anomalies** (the bad things) each one prevents. That's the heart of this topic.

### The concurrency anomalies (the bugs we're defending against)

**1. Dirty Read** — reading data another transaction wrote but *hasn't committed yet*. If that other transaction then rolls back, you read a value that **never officially existed.**
> *Example:* Transaction B reads Alice's balance as ₹0 because Transaction A is mid-transfer and temporarily set it there — but A then fails and rolls back. B made a decision based on a phantom value.

**2. Non-Repeatable Read** — reading the *same row twice* in one transaction and getting *different values*, because another transaction committed a change in between.
> *Example:* You read Alice's balance (₹500), do some work, read it again (₹0) — because another transaction modified and committed it mid-way. The "same" read gave two answers.

**3. Phantom Read** — re-running the same *query* (over a range/set of rows) and getting a *different set of rows*, because another transaction inserted or deleted rows matching your condition.
> *Example:* You count "transactions over ₹10,000" and get 5. A moment later in the same transaction you count again and get 6 — a new matching row (a "phantom") appeared.

**4. Lost Update** — two transactions read the same value, both modify it, and one overwrites the other's change — so one update is silently *lost*.
> *Example:* Two simultaneous ₹100 deposits to Alice both read ₹500, both compute ₹600, both write ₹600. She should have ₹700 but has ₹600. One deposit vanished. **This one is especially dangerous in fintech.**

Each isolation level is defined by *which of these anomalies it permits*:

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|---|---|---|---|
| **Read Uncommitted** | ✅ possible | ✅ possible | ✅ possible |
| **Read Committed** | ❌ prevented | ✅ possible | ✅ possible |
| **Repeatable Read** | ❌ prevented | ❌ prevented | ✅ possible* |
| **Serializable** | ❌ prevented | ❌ prevented | ❌ prevented |

*\*The SQL standard says Repeatable Read allows phantoms, but some databases (e.g., PostgreSQL, MySQL/InnoDB) prevent them at this level too via MVCC — real-world behavior varies, which is a crucial practical point below.*

---

## How it Works Under the Hood

### The two mechanisms: Locking vs. MVCC

Databases enforce isolation using one (or both) of two strategies, introduced in Chapter 11:

**Pessimistic — Locking.** A transaction takes **locks** on the rows (or ranges) it reads/writes; other transactions that conflict must **wait**. Higher isolation = more and longer-held locks = more waiting. "Pessimistic" because it assumes conflict and blocks preemptively.

**Optimistic — MVCC (Multi-Version Concurrency Control).** Instead of locking readers, the DB keeps **multiple versions** of each row, each tagged with the transaction that created it. A transaction reads a consistent **snapshot** — the version of the data as of a certain point in time — so readers never block writers and writers never block readers. Most modern databases (PostgreSQL, MySQL/InnoDB, Oracle) use MVCC heavily. "Optimistic" because it lets work proceed and only checks for conflicts at commit.

Now let's walk each level.

### Read Uncommitted (weakest)
Transactions can see **uncommitted** changes from others. Essentially no read protection — all anomalies including dirty reads are possible. Almost nobody uses this intentionally; the performance gain over Read Committed is usually negligible while the risk is severe. (PostgreSQL doesn't even truly implement it — it behaves as Read Committed.)

### Read Committed (the common default)
> A transaction only ever sees data that has been **committed**. No dirty reads. This is the **default in PostgreSQL, Oracle, and SQL Server.**

How (via MVCC): each *statement* reads the latest *committed* snapshot at the moment that statement runs. The catch: two statements in the *same* transaction run at different moments, so they can see *different* committed snapshots → **non-repeatable reads and phantoms are still possible.** It prevents the worst anomaly (dirty reads) while keeping high concurrency — a pragmatic sweet spot for most applications.

### Repeatable Read (stronger)
> The transaction sees a **consistent snapshot fixed at the moment it began.** Every read in the transaction returns the *same* value, regardless of others committing in the meantime. No dirty reads, no non-repeatable reads. (It's **MySQL/InnoDB's default.**)

How (via MVCC): the snapshot is taken at transaction start, not per-statement. So you get a stable, frozen view. Per the SQL standard, phantoms can still occur; but PostgreSQL's Repeatable Read and InnoDB (via next-key locking) actually prevent phantoms too — so in practice this level is often stronger than the standard requires.

**Important:** Repeatable Read does *not* automatically prevent **lost updates** in all cases. Two transactions can each take a snapshot, both update, and clash. MVCC databases typically *detect* this at commit and abort one transaction with a serialization error (which your app must catch and retry), or you prevent it explicitly with `SELECT ... FOR UPDATE` (which locks the rows you read).

### Serializable (strongest)
> Guarantees the end result is **as if all transactions ran one after another**, in *some* serial order. Prevents *every* anomaly. This is the gold standard for correctness.

How: either via strict **two-phase locking** (lots of locking, lots of waiting) or, in PostgreSQL, via **Serializable Snapshot Isolation (SSI)** — an optimistic approach that monitors for dangerous patterns among concurrent transactions and **aborts** one if a non-serializable outcome would occur. Either way, you pay: more blocking or more aborts-and-retries, and lower throughput.

### The critical real-world technique: explicit locking for lost updates

Because the default isolation level (Read Committed) does **not** prevent lost updates, the everyday fix in transactional apps is **explicit row locking**:

```sql
BEGIN;
  SELECT balance FROM accounts WHERE account_id = 'A1' FOR UPDATE;  -- lock the row
  -- now no other transaction can modify A1's row until we commit
  UPDATE accounts SET balance = balance - 500 WHERE account_id = 'A1';
COMMIT;
```
`SELECT ... FOR UPDATE` locks the row so concurrent transactions must wait — eliminating the lost-update and double-spend risk for that row, *without* paying the global cost of Serializable across the whole database. This targeted locking is one of the most important practical tools in transactional system design.

> **The mental model:** Pick the *lowest* isolation level that's still *correct* for the operation, and use **targeted locks (`FOR UPDATE`)** to protect the specific hot spots that need more — rather than cranking the whole database to Serializable and paying for it everywhere.

---

## Why do we need it?

We need isolation levels because **concurrency bugs are invisible in testing and catastrophic in production** — and the only defense is choosing the right level (and locks) deliberately.

1. **These bugs don't show up under light load.** With one user at a time, *no* anomaly ever occurs — your tests pass perfectly. The bugs only appear when many transactions hit the *same data simultaneously*, i.e., exactly during your busiest, highest-stakes moments (the festival-sale peak). You cannot stumble into correctness here; you must *design* it.

2. **In fintech, an anomaly is lost money.** A lost update means a deposit silently disappears. A dirty read means approving a payment based on a balance that never existed. These aren't cosmetic glitches — they're financial incidents, customer complaints, and regulatory problems.

3. **You can't just max out isolation.** Setting everything to Serializable "to be safe" can tank throughput and cause a storm of transaction aborts/retries or lock contention under load — turning a correctness solution into an availability problem. You must balance.

4. **Defaults differ across databases.** PostgreSQL defaults to Read Committed; MySQL/InnoDB to Repeatable Read. If you assume the wrong default, you may be exposed to anomalies you didn't know were possible. Knowing your DB's actual behavior is essential.

**When to use which:**
- **Read Committed:** the sensible default for most operations — fast, prevents dirty reads, fine when occasional non-repeatable reads don't break correctness.
- **Repeatable Read:** when a transaction reads the same data multiple times and needs a stable view (e.g., generating a financial report mid-transaction).
- **Serializable:** for the rare operations where correctness is paramount and you can't reason about every anomaly — accept the performance hit.
- **`FOR UPDATE` locking:** the targeted tool for preventing lost updates / double-spends on specific hot rows (like a balance), usable at lower isolation levels.

---

## Real-World / Fintech Example

The **double-spend bug** in our **digital wallet / payments app** is the perfect, concrete illustration — and it's a *lost update / write-skew* problem at heart.

**The bug.** Alice has ₹500. She taps "Pay ₹500 to Bob" twice in quick succession (impatient, thought the first tap didn't register). Two transactions run *concurrently*:

```
Transaction 1                         Transaction 2
read Alice.balance = ₹500
                                       read Alice.balance = ₹500   (also sees 500!)
check 500 >= 500 ✓                     check 500 >= 500 ✓
debit → balance = ₹0                   debit → balance = ₹0
commit                                 commit
```
Both read ₹500, both saw "enough funds," both approved. Alice spent ₹1,000 she didn't have. **At Read Committed (the default), this is entirely possible** — neither transaction saw the other's uncommitted change, so both believed the money was available. This is a *lost update*: one debit effectively overwrote the other's view.

**The fix — targeted locking (the usual, performant choice).** Lock Alice's row when you read it:
```sql
BEGIN;
  SELECT balance FROM accounts WHERE account_id = 'Alice' FOR UPDATE;  -- lock!
  -- Transaction 2 now BLOCKS here until Transaction 1 commits
  -- (after which it reads the *new* balance ₹0 and correctly rejects the payment)
  UPDATE accounts SET balance = balance - 500 WHERE account_id = 'Alice';
COMMIT;
```
Now Transaction 2 can't read Alice's balance until Transaction 1 finishes. When it finally does, it sees ₹0 and *correctly rejects* the second payment. The double-spend is impossible — and we only paid for a lock on *one row*, not Serializable across the entire database. This is the standard fintech pattern.

**The alternative — Serializable.** We *could* run both payments at Serializable isolation; the database would detect the conflict and abort one with a serialization error, which the app catches and retries (where it then sees ₹0 and rejects). Correct, but it costs more throughput and requires retry logic everywhere. So we reserve it for genuinely complex multi-row invariants, and use `FOR UPDATE` for the common single-balance case.

**Knowing the default saved them.** The team initially assumed "the database will protect us." Then a load test (many concurrent payments from one account) surfaced the double-spend — *because* their default was Read Committed. Recognizing that **the default isolation level does NOT prevent lost updates** is exactly the senior insight that prevents a real-money incident. (Had they been on MySQL's Repeatable Read default, they'd *still* need `FOR UPDATE` or conflict-abort handling — Repeatable Read alone doesn't guarantee no lost updates either.)

**Where they relax isolation.** Generating Alice's monthly statement reads thousands of rows; they run it at **Repeatable Read** so the whole report reflects one consistent snapshot (no figures shifting mid-report), but they deliberately *don't* use Serializable because a read-only report can't cause a lost update. **Right level for the job.**

In Spring Boot, all of this is controllable per method: `@Transactional(isolation = Isolation.REPEATABLE_READ)` sets the level, and a `@Lock(LockModeType.PESSIMISTIC_WRITE)` on a JPA query (or a `findByIdForUpdate`) issues the `SELECT ... FOR UPDATE`. The annotations are friendly, but you must know *which* to apply — and this chapter is why.

---

## Trade-offs (Pros & Cons)

### Lower isolation (Read Uncommitted / Read Committed)
**Pros**
- **High concurrency and throughput** — minimal locking/blocking.
- **Lower latency** — transactions rarely wait on each other.
- Fine for the *majority* of operations where the exposed anomalies don't affect correctness.

**Cons**
- **Exposes anomalies** — dirty reads (Read Uncommitted), non-repeatable reads, phantoms, and **lost updates**.
- **Subtle, load-dependent bugs** that pass tests and explode in production.

### Higher isolation (Repeatable Read / Serializable)
**Pros**
- **Strong correctness** — fewer or zero anomalies; easy to reason about.
- **Serializable is provably correct** — behaves as if transactions ran one at a time.

**Cons**
- **Lower throughput** — more locking/waiting, or more aborts-and-retries.
- **Serialization failures** — Serializable/Repeatable-Read transactions may abort and require **retry logic** in the app.
- **Risk of deadlocks** and lock contention under heavy load.

### Targeted locking (`SELECT ... FOR UPDATE`)
**Pros**
- **Prevents lost updates/double-spends** precisely where needed.
- **Cheaper than Serializable** — locks only the hot rows, not everything.

**Cons**
- **Reduces concurrency on those rows** (others wait), which can become a hot-spot bottleneck.
- **Deadlock risk** if transactions lock multiple rows in inconsistent orders.
- Requires you to *know* which rows need protecting (back to understanding anomalies).

> **Staff-engineer takeaway:** Isolation levels trade **correctness against concurrency**, and the dangerous bugs (especially **lost updates** → double-spends) are invisible until concurrent load hits your hottest data. **Know your database's default** (Postgres = Read Committed, MySQL = Repeatable Read), and remember neither default prevents lost updates on its own. The mature pattern: run most operations at a **low, fast isolation level** and protect specific hot spots with **`SELECT ... FOR UPDATE`**, reserving **Serializable** for genuinely complex invariants — buying exactly as much isolation as correctness demands, and no more.

---

➡️ **End of Batch 4.** You now understand where data lives (storage engines), the relational default and ACID, and the isolation knob that keeps concurrent transactions correct. Next batch tackles *growing* the database beyond one machine: **Scaling Databases**, **Sharding and Partitioning**, and **Non-Relational Databases** — confronting head-on the write-scaling wall we kept running into.
