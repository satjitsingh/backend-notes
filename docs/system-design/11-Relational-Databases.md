# 11. Relational Databases

> The "boring" technology that runs the world's money. Before reaching for anything trendy, understand why relational databases — tables, SQL, and ACID transactions — are still the default right answer for the vast majority of systems, especially in fintech.

---

## What is it?

A **relational database** organizes data into **tables** (rows and columns), where tables can be **related** to each other through shared keys, and you query it using **SQL** (Structured Query Language). The most popular ones are **PostgreSQL, MySQL, Oracle, SQL Server**, and they're collectively called **RDBMS** (Relational Database Management Systems).

The "relational" part is the key idea, and it's elegant. Instead of stuffing all data into one giant blob, you split it into focused tables and *link* them:

```
   accounts                      transactions
   ┌──────────┬─────────┐        ┌──────┬────────────┬────────┐
   │ account  │ balance │        │ txn  │ account_id │ amount │
   │   _id    │         │        │ _id  │ (foreign   │        │
   ├──────────┼─────────┤        │      │   key)     │        │
   │  A1      │  ₹500   │◄───────┤  T1  │    A1      │  ₹500  │
   │  A2      │  ₹200   │   links │  T2  │    A2      │  ₹200  │
   └──────────┴─────────┘        └──────┴────────────┴────────┘
```

Each `transaction` points to an `account` via a **foreign key** (`account_id`). This avoids duplicating account details in every transaction row, and guarantees a transaction can't reference an account that doesn't exist. That principle — *store each fact once, and link* — is called **normalization**.

But the single most important thing relational databases give you, and the reason fintech is built on them, is **ACID transactions**:

> **A transaction is a group of operations that succeed or fail *together*, as one indivisible unit.** "Debit Alice ₹500 AND credit Bob ₹500" is one transaction — either *both* happen, or *neither* does. There's no universe where Alice loses money but Bob doesn't receive it.

**ACID** is the four guarantees that make this trustworthy:
- **A — Atomicity:** all-or-nothing. The whole transaction commits, or it's fully rolled back. No half-done states.
- **C — Consistency:** the transaction takes the database from one *valid* state to another, never violating its rules (constraints, foreign keys). *(This is the ACID-C from Chapter 7 — correctness of invariants, not the distributed CAP-C.)*
- **I — Isolation:** concurrent transactions don't trip over each other; each behaves as if it ran alone. *(How much isolation is the entire next chapter.)*
- **D — Durability:** once committed, it survives crashes forever — guaranteed by the WAL from Chapter 10.

---

## How it Works Under the Hood

### The relational model and SQL

You define a **schema** upfront — the exact tables, columns, and types ("a `balance` is a non-negative decimal", "an `account_id` is required"). This **schema-on-write** approach means the database *enforces structure*: it rejects data that doesn't fit. That rigidity is a feature for financial data — you literally cannot insert a malformed transaction.

You then use **SQL**, a declarative language where you describe *what* you want, not *how* to get it:
```sql
SELECT a.account_id, SUM(t.amount) AS total_spent
FROM accounts a
JOIN transactions t ON a.account_id = t.account_id
WHERE t.timestamp > '2026-06-01'
GROUP BY a.account_id;
```
You didn't tell it which indexes to use or how to combine the tables — you described the result, and the database's **query planner** figures out the optimal execution (using the B-Tree indexes from Chapter 10). **JOINs** — combining related tables on the fly — are the relational superpower; they let you keep data normalized (stored once) yet still answer questions that span many tables.

### How ACID is actually implemented

This is where Chapter 10's machinery pays off. Each ACID property maps to a concrete mechanism:

**Atomicity (all-or-nothing)** is achieved with the **WAL + undo information**. As a transaction runs, the database records enough to *undo* its changes. If the transaction fails or you call `ROLLBACK`, the DB uses that info to erase every change as if it never happened. `COMMIT` makes them permanent.

**Durability (survives crashes)** is the **WAL** itself (Chapter 10): the commit isn't acknowledged until the change is safely in the on-disk log, so a crash can't lose it — on restart, the WAL is replayed.

**Isolation (concurrent safety)** is achieved with **locking** and/or **MVCC (Multi-Version Concurrency Control)**:
- **Locking:** a transaction takes a lock on rows it touches; others must wait. Safe but can cause contention and waiting.
- **MVCC** (used by PostgreSQL, MySQL/InnoDB, Oracle): instead of blocking readers, the DB keeps **multiple versions** of a row. Readers see a consistent *snapshot* of the data as it was when their transaction began, while a writer creates a new version. This is the magic that lets **"readers don't block writers, and writers don't block readers."** It's why a long analytics query can run without freezing live payments. (The degree of isolation MVCC gives you is the next chapter.)

**Consistency (valid states)** is enforced by **constraints** the database checks on every write: `NOT NULL`, `UNIQUE`, `CHECK (balance >= 0)`, foreign keys. If a transaction would violate one, the DB rejects it and rolls back.

### A worked transaction (the canonical money transfer)

```sql
BEGIN;                                                  -- start transaction
  UPDATE accounts SET balance = balance - 500 WHERE account_id = 'A1';  -- debit Alice
  UPDATE accounts SET balance = balance + 500 WHERE account_id = 'A2';  -- credit Bob
COMMIT;                                                 -- both succeed together
```
- If the server crashes *between* the two UPDATEs, **atomicity** ensures that on restart the half-done debit is rolled back — Alice's money returns. Money is never created or destroyed.
- A `CHECK (balance >= 0)` constraint (**consistency**) prevents Alice from going negative — the transaction fails and rolls back if she lacks funds.
- **Isolation** ensures another transaction reading Alice's balance concurrently doesn't see the spooky intermediate state where ₹500 has vanished from Alice but not yet arrived at Bob.
- **Durability** ensures that once `COMMIT` returns, the transfer survives any subsequent crash.

> In a Spring Boot app, this entire block is what a single `@Transactional` method gives you: the framework issues `BEGIN` when the method starts and `COMMIT` when it returns normally (or `ROLLBACK` if it throws). That one annotation is sitting on top of decades of WAL/lock/MVCC engineering.

### Scaling characteristics (the honest limitation)

Relational databases were *designed* for a single powerful server. They scale **reads** beautifully via replicas (Chapter 6), but scaling **writes** is hard — there's traditionally one leader accepting writes. JOINs, transactions, and strong consistency all assume the data is together on one machine; once you split it across many (sharding), those features get much harder. **This write-scaling limitation is the single biggest reason teams ever leave relational databases** — and it's the bridge to the NoSQL and sharding chapters coming up.

---

## Why do we need it?

We need relational databases because **they provide correctness guarantees that are extraordinarily hard to build yourself, and for most data those guarantees are exactly what you want.**

1. **ACID transactions make correctness easy.** "These operations happen together or not at all" is the foundation of any system handling money, orders, or inventory. Without it, you'd hand-write fragile, bug-prone logic to clean up half-finished operations after every crash.

2. **Strong consistency by default.** A relational DB on one server gives you the strong consistency of Chapter 7 *for free* — every read sees the latest committed write. No eventual-consistency surprises.

3. **Flexible querying without anticipating every question.** Because data is normalized and you have SQL + JOINs, you can answer questions you didn't plan for at design time. NoSQL often forces you to know your queries upfront.

4. **Data integrity via the schema and constraints.** The database itself refuses bad data, acting as a last line of defense no matter how buggy the application code is.

5. **Maturity.** Decades of hardening, tooling, expertise, and reliability. "Boring" is a compliment for a system holding your money.

**When to use it (the default):**
> Start with a relational database unless you have a *specific, proven* reason not to. It's the right default for the overwhelming majority of applications, and *especially* anything transactional (payments, orders, bookings, user accounts).

**When to look elsewhere:** when you genuinely need massive *write* scale beyond one machine, or you're storing huge volumes of loosely-structured / schemaless data, or you need a specialized access pattern (graph traversals, full-text search, time-series) — the NoSQL chapter covers these.

---

## Real-World / Fintech Example

For our **digital wallet / payments app**, the relational database is the **non-negotiable heart of the system** — and ACID is the reason.

**Why the ledger *must* be relational.** Consider the money transfer again. The entire business depends on the guarantee that "debit Alice, credit Bob" is **atomic** — there is no acceptable scenario where one happens without the other. That is precisely what an ACID transaction provides, and it's exactly what eventually-consistent NoSQL stores historically *don't* (without a lot of extra work). So the ledger lives in PostgreSQL, with the transfer wrapped in a single transaction:

- **Atomicity** guarantees money is never lost or duplicated mid-transfer.
- **A `CHECK (balance >= 0)` constraint** (Consistency) makes it *impossible* to overdraw, enforced by the database itself — even if a bug slips past the application code.
- **Isolation** stops two simultaneous payments from Alice from both reading her ₹500 balance and each approving a ₹500 spend (a double-spend). The next chapter shows exactly which isolation level prevents this.
- **Durability** ensures a confirmed payment never vanishes, even if the server dies a millisecond later.

**Relationships and integrity.** Accounts, transactions, KYC records, and statements are all separate, normalized tables linked by foreign keys. A foreign key from `transactions.account_id` to `accounts.account_id` makes it *structurally impossible* to record a transaction against a non-existent account — the database rejects it. For a regulated financial system, this built-in integrity is invaluable.

**Rich reporting via SQL.** Compliance asks: "all transactions over ₹50,000 by users in Mumbai last quarter." With SQL JOINs across `transactions`, `accounts`, and `users`, that's one query — no need to have anticipated it when designing the schema. This ad-hoc query power is hard to match in NoSQL.

**The honest limit, foreshadowing what's next.** At 80,000 payments/second, a *single* PostgreSQL leader eventually can't keep up with the **write** load — even with read replicas soaking up all the balance checks. This is the relational write-scaling wall. The team's options (covered soon) are to **shard** the relational database (split accounts across many PostgreSQL instances, accepting that cross-shard transactions get hard) or move *select* high-volume, non-transactional data to NoSQL. Notice they don't abandon relational for the ledger — they scale it — because nothing else gives them ACID as cleanly.

---

## Trade-offs (Pros & Cons)

**Pros**
- **ACID transactions** — all-or-nothing correctness; the gold standard for money/orders/inventory.
- **Strong consistency** by default (single-server) — no stale-read surprises.
- **Powerful, flexible querying** — SQL + JOINs answer questions you didn't plan for.
- **Data integrity** — schema and constraints reject bad data at the database level.
- **Mature and battle-tested** — decades of reliability, tooling, and expertise.

**Cons**
- **Hard to scale writes horizontally** — built around a single leader; sharding sacrifices the easy JOINs/transactions that made it great.
- **Rigid schema** — `schema-on-write` means structural changes (migrations) take planning; less friendly to rapidly-changing or loosely-structured data.
- **JOINs get expensive at huge scale** and don't work cleanly across shards.
- **Object-relational impedance mismatch** — mapping objects (e.g., Java entities) to tables adds an ORM layer (Hibernate/JPA) with its own pitfalls.

> **Staff-engineer takeaway:** Relational databases give you **ACID transactions and strong consistency** — guarantees that are priceless for money and brutally hard to rebuild yourself. Make them your **default**, *especially* for transactional data; the burden of proof is on anyone who wants to use something else. Their one real weakness is **horizontal write scaling**, which you address with replicas (for reads), sharding (for writes), and selectively offloading non-transactional data to NoSQL — *without* giving up ACID for the ledger itself.

---

➡️ Next: [12-Database-Isolation-Levels.md](12-Database-Isolation-Levels.md) — the "I" in ACID, in depth: the precise knob controlling how concurrent transactions see each other, and the subtle bugs each level prevents (or allows).
