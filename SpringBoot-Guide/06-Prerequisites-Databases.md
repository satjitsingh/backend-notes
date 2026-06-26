# Chapter 6: Database Fundamentals (Prerequisites)

[← Back to Index](00-README.md) | [Previous: Web Fundamentals](05-Prerequisites-Web-Fundamentals.md) | [Next: Dependency Injection →](07-Prerequisites-DI.md)

---

## Why This Chapter Matters

Imagine building a house without understanding how bricks work. You could stack them, but when something breaks, you wouldn't know why. **90% of Spring Boot applications talk to databases.** If you use JPA or Hibernate without understanding databases, you're in that exact situation—things will break, and you won't know how to fix them.

### The Reality Check

Many developers have **shallow database knowledge**. They know:
- How to write basic SELECT, INSERT, UPDATE, DELETE
- That indexes make queries faster
- That transactions exist

But they **don't understand**:
- Why using an ORM without understanding SQL is dangerous
- How transaction isolation levels affect their app
- When indexes help vs. hurt performance
- How to debug slow queries
- What causes deadlocks and how to prevent them

### Why ORM Knowledge Without SQL Knowledge Is Dangerous

**The Trap:**
```
Developer learns JPA/Hibernate
↓
Writes code: userRepository.findAll()
↓
Doesn't understand what SQL is generated
↓
Production: Query takes 10 seconds, locks entire table
↓
Cannot debug because they don't understand SQL
```

**Real Production Issues:**

1. **N+1 Problem**: One query for users, then 1000 more for their orders—instead of one smart query.
2. **Missing Indexes**: Login checks every row in the table because there's no index on email.
3. **Transaction Deadlocks**: Too many `@Transactional` methods without understanding isolation—deadlocks under load.
4. **Poor Query Performance**: `findAll()` then filter in Java—the database scans millions of rows instead of using WHERE.
5. **Connection Pool Exhaustion**: Not understanding connection pooling leads to "too many connections" errors.

### Interview Expectations

For **backend roles**, interviewers expect you to:
- Explain ACID properties with examples
- Choose appropriate transaction isolation levels
- Design database schemas
- Debug slow queries using EXPLAIN
- Understand when to use indexes
- Explain optimistic vs. pessimistic locking
- Understand the N+1 problem

---

## Layer 1 — Intuition Builder

Think of this section like a story: we start with simple ideas and build up. Every idea starts with something you already know from everyday life.

---

### Part 1: What Is a Database?

**The Filing Cabinet Analogy**

Imagine a big filing cabinet in an office. Instead of papers stuffed randomly in drawers, you have:
- **Folders** (like tables) for different types of things
- **Sheets inside folders** (like rows) for each item
- **Fields on each sheet** (like columns) for each piece of information

A **database** is like that organized filing cabinet—but for a computer. It stores information in a structured way so you can find it, change it, and keep it safe.

```
┌─────────────────────────────────────────────────────────┐
│  📁 DATABASE (The Whole Filing Cabinet)                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐        │
│  │ 📂 users    │ │ 📂 orders   │ │ 📂 products │        │
│  │ (folder)    │ │ (folder)    │ │ (folder)    │        │
│  └─────────────┘ └─────────────┘ └─────────────┘        │
└─────────────────────────────────────────────────────────┘
```

**Why do we need databases?**
- **Permanent storage**: Data stays even when the app shuts down
- **Fast lookups**: Like using an index in a book instead of reading every page
- **Shared access**: Many users can read and write at the same time
- **Safety**: Protects against crashes and bad data

---

### Part 2: Relational vs. NoSQL—Two Ways to Organize

**Relational (SQL): Like a Spreadsheet Family**

Imagine a school office with several spreadsheets:
- One spreadsheet for **students**
- One for **classes**
- One for **grades**

They're **related** because the grades spreadsheet has a "student ID" that points to the students spreadsheet. Everything connects in a neat, structured way.

**NoSQL: Like a Collection of Notebooks**

Now imagine each student has their own notebook. In one place, you write their name, their classes, their grades, their favorite color, their pet's name—whatever you want. Each notebook can look different. Flexible, but harder to compare across notebooks in a standardized way.

| Aspect | Relational (SQL) | NoSQL |
|--------|------------------|-------|
| **Structure** | Fixed columns (like spreadsheets) | Flexible (can add any field) |
| **Relationships** | Tables linked by IDs | Often stored together or by key |
| **Best for** | Orders, users, payments—structured stuff | Logs, comments, flexible documents |
| **Example** | MySQL, PostgreSQL | MongoDB, Redis |

Most Spring Boot apps use **relational databases** because they handle structured business data well. This chapter focuses on those.

---

### Part 3: Tables, Rows, and Columns

**The Lunch Menu Analogy**

Think of a restaurant menu:

- **Table** = The whole menu (e.g., "Drinks" table)
- **Row** = One item on the menu (e.g., one specific drink)
- **Column** = One type of information (e.g., "name", "price", "size")

```
MENU TABLE (Drinks)
┌────┬─────────────┬───────┬──────┐
│ id │ name        │ price │ size │  ← Columns (what we track)
├────┼─────────────┼───────┼──────┤
│  1 │ Coca-Cola   │ 2.50  │ M    │  ← Row 1 (one drink)
│  2 │ Orange Juice │ 3.00  │ L    │  ← Row 2
│  3 │ Water       │ 1.50  │ S    │  ← Row 3
└────┴─────────────┴───────┴──────┘
```

- **Table**: A collection of related data (like one spreadsheet)
- **Row (record)**: One complete entry (one drink, one user, one order)
- **Column (field)**: One piece of info we track (name, price, email)

---

### Part 4: Primary Keys—Each Row’s Unique ID

**The Locker Number Analogy**

In a school, every locker has a number. No two lockers share the same number. When someone says "Locker 47," you know exactly which one they mean.

A **primary key** is like that locker number—a unique ID for each row. No two rows can have the same primary key.

```
users table
┌────┬─────────────┬────────────────────┐
│ id │ name        │ email              │  ← id is the primary key
├────┼─────────────┼────────────────────┤
│  1 │ John Doe    │ john@example.com   │  ← id=1 is unique
│  2 │ Jane Smith  │ jane@example.com   │  ← id=2 is unique
│  3 │ Bob Wilson  │ bob@example.com    │  ← id=3 is unique
└────┴─────────────┴────────────────────┘
```

**Rules for primary keys:**
- ✅ **Unique**: No duplicates
- ✅ **Not empty**: Every row must have one
- ✅ **Stable**: Usually we don't change it (like a social security number)

---

### Part 5: Foreign Keys—Connecting Tables

**The Receipt Analogy**

When you buy something, the receipt says "Customer: John Doe" or "Customer ID: 1." That links the receipt (order) to the customer (user). The receipt doesn't store all of John's details—just enough to know which customer it belongs to.

A **foreign key** is a column that points to the primary key of another table. It creates a **relationship** between tables.

```
users table                    orders table
┌────┬────────────┐            ┌────┬──────────┬───────┐
│ id │ name       │            │ id │ user_id  │ total │
├────┼────────────┤            ├────┼──────────┼───────┤
│  1 │ John       │◄───────────│  1 │ 1        │ 100   │  user_id points to users.id
│  2 │ Jane       │◄───────────│  2 │ 1        │  50   │
└────┴────────────┘            │  3 │ 2        │ 200   │
                               └────┴──────────┴───────┘
```

**Why foreign keys matter:**
- **Referential integrity**: You can't create an order for "user 999" if user 999 doesn't exist
- **Clear relationships**: You can see that Order 1 belongs to John (user 1)

---

### Part 6: Relationship Types (1:1, 1:N, N:M)

**1:1 (One-to-One)** — One person, one passport

```
Person          Passport
┌────┬──────┐   ┌────┬──────────┬─────────┐
│ id │ name │   │ id │ person_id│ number  │
├────┼──────┤   ├────┼──────────┼─────────┤
│  1 │ John │◄──│  1 │ 1        │ AB123   │
│  2 │ Jane │◄──│  2 │ 2        │ CD456   │
└────┴──────┘   └────┴──────────┴─────────┘
Each person has exactly one passport.
```

**1:N (One-to-Many)** — One parent, many kids

```
User            Orders
┌────┬──────┐   ┌────┬──────────┬───────┐
│ id │ name │   │ id │ user_id  │ total │
├────┼──────┤   ├────┼──────────┼───────┤
│  1 │ John │◄──│  1 │ 1        │ 100   │
│    │      │◄──│  2 │ 1        │  50   │
│    │      │◄──│  3 │ 1        │  75   │
└────┴──────┘   └────┴──────────┴───────┘
One user can have many orders.
```

**N:M (Many-to-Many)** — Students and courses

A student takes many courses. A course has many students. We need a **middle table** (junction table) to connect them.

```
students          student_courses          courses
┌────┬──────┐    ┌────────────┬───────────┐    ┌────┬───────┐
│ id │ name │    │ student_id │ course_id │    │ id │ name  │
├────┼──────┤    ├────────────┼───────────┤    ├────┼───────┤
│  1 │ Alice│    │ 1          │ 1         │    │  1 │ Math  │
│  2 │ Bob  │    │ 1          │ 2         │    │  2 │ Science│
└────┴──────┘    │ 2          │ 1         │    └────┴───────┘
                 │ 2          │ 2         │
                 └────────────┴───────────┘
Alice takes Math and Science. Bob takes Math and Science.
```

---

### Part 7: SQL—The Language to Talk to the Database

**The Waiter Analogy**

SQL (Structured Query Language) is like giving instructions to a waiter:
- "Bring me all the items from the dessert menu" (SELECT)
- "Add a new order for table 5" (INSERT)
- "Change the price of Coke to $2.75" (UPDATE)
- "Remove the sold-out item from the menu" (DELETE)

You're not reaching into the kitchen yourself—you're giving clear, structured commands.

---

### Part 8: CRUD—The Four Main Operations

**CRUD** = Create, Read, Update, Delete

**1. CREATE (INSERT)** — Add new data

```sql
-- Add one new user (like adding a new contact to your phone)
INSERT INTO users (name, email) 
VALUES ('John Doe', 'john@example.com');

-- Add multiple users at once
INSERT INTO users (name, email) VALUES
    ('Jane Smith', 'jane@example.com'),
    ('Bob Wilson', 'bob@example.com');
```

**2. READ (SELECT)** — Get data

```sql
-- Get ALL users (like "show me everyone in my contacts")
SELECT * FROM users;

-- Get only certain columns
SELECT id, name, email FROM users;

-- Get ONE specific user by ID
SELECT * FROM users WHERE id = 1;
```

**3. UPDATE** — Change existing data

```sql
-- Change John's email (like editing a contact)
UPDATE users 
SET email = 'newemail@example.com' 
WHERE id = 1;
```

⚠️ **Always use WHERE!** Without it, you update every row—like accidentally changing everyone's email.

**4. DELETE** — Remove data

```sql
-- Delete one user
DELETE FROM users WHERE id = 1;
```

⚠️ **Always use WHERE!** `DELETE FROM users;` with no WHERE deletes everything.

---

### Part 9: Filtering, Sorting, and Limiting

**Filtering with WHERE** — "Only show me what matches"

```sql
-- Users with a specific email
SELECT * FROM users WHERE email = 'john@example.com';

-- Users created after January 1, 2026
SELECT * FROM users WHERE created_at > '2026-01-01';

-- Multiple conditions (AND)
SELECT * FROM users 
WHERE email = 'john@example.com' AND created_at > '2026-01-01';

-- Either condition (OR)
SELECT * FROM users 
WHERE email = 'john@example.com' OR email = 'jane@example.com';

-- Pattern matching (LIKE) - % means "anything"
SELECT * FROM users WHERE email LIKE '%@example.com';
```

**Sorting with ORDER BY** — "Put them in order"

```sql
-- Alphabetical by name (A to Z)
SELECT * FROM users ORDER BY name;

-- Newest first (Z to A for dates)
SELECT * FROM users ORDER BY created_at DESC;

-- Multiple sort columns
SELECT * FROM users ORDER BY created_at DESC, name ASC;
```

**Limiting Results** — "Just give me the first few"

```sql
-- First 10 users
SELECT * FROM users LIMIT 10;

-- Skip first 5, then give me 10 (pagination)
SELECT * FROM users LIMIT 10 OFFSET 5;
```

---

### Part 10: JOINs—Combining Tables

**The Invitation List Analogy**

Imagine you have a list of guests and a list of their plus-ones. A JOIN combines them so you see "John + his partner Jane" on one line.

**Example data:**
```
users:                 orders:
id | name              id | user_id | total
1  | John              1  | 1       | 100
2  | Jane              2  | 1       |  50
                       3  | 99      | 200   (user 99 doesn't exist!)
```

**INNER JOIN** — "Only show me matches"

Like a strict guest list: only people who have a plus-one get a line. User 99's order doesn't appear because there's no user 99.

```sql
SELECT users.name, orders.total
FROM users
INNER JOIN orders ON users.id = orders.user_id;

-- Result: John | 100
--         John | 50
-- (Jane has no orders, so she's not shown. Order 3 has no valid user, so it's not shown.)
```

**LEFT JOIN** — "Show everyone from the left table, matches or not"

Like including everyone from the main list, and adding their plus-one info if they have one. Jane appears with NULL for total.

```sql
SELECT users.name, orders.total
FROM users
LEFT JOIN orders ON users.id = orders.user_id;

-- Result: John | 100
--         John | 50
--         Jane | NULL   (included even with no orders)
```

**RIGHT JOIN** — "Show everyone from the right table"

Same idea as LEFT, but we keep all orders and show users (or NULL if no user).

**FULL OUTER JOIN** — "Show everyone from both tables"

Everyone gets a line—users with no orders, orders with no users.

**Quick reference:**

| JOIN Type      | Meaning                                          |
|----------------|--------------------------------------------------|
| INNER JOIN     | Only rows that match in both tables              |
| LEFT JOIN      | All from left table + matches from right         |
| RIGHT JOIN     | All from right table + matches from left         |
| FULL OUTER JOIN| All from both tables                             |

---

### Part 11: GROUP BY and Aggregations

**The Class Report Card Analogy**

Instead of listing every test score, you might say: "Alice: average 92, Bob: average 88." You're **grouping** by student and **aggregating** (averaging) the scores.

**Common aggregations:**
- `COUNT()` — How many?
- `SUM()` — Total
- `AVG()` — Average
- `MIN()` / `MAX()` — Smallest / largest

```sql
-- How many orders does each user have?
SELECT user_id, COUNT(*) as order_count
FROM orders
GROUP BY user_id;

-- How much has each user spent in total?
SELECT users.name, SUM(orders.total) as total_spent
FROM users
INNER JOIN orders ON users.id = orders.user_id
GROUP BY users.id, users.name;

-- Only show users with more than 1 order (HAVING filters groups)
SELECT user_id, COUNT(*) as order_count
FROM orders
GROUP BY user_id
HAVING COUNT(*) > 1;
```

---

### Part 12: Indexes—The Book Index Analogy

**Without an index:** To find "Spring Boot" in a 500-page book, you read every page. Slow.

**With an index:** You look at the index, see "Spring Boot → page 347," and go straight there. Fast.

A **database index** does the same thing. It helps the database find rows without scanning the whole table.

```
Without index:  SELECT * FROM users WHERE email = 'john@example.com';
                → Scan 1,000,000 rows. Slow.

With index:     CREATE INDEX idx_email ON users(email);
                SELECT * FROM users WHERE email = 'john@example.com';
                → Jump straight to that row. Fast.
```

**When indexes help:**
- Columns in WHERE
- Columns in JOINs
- Columns in ORDER BY

**When they hurt:**
- Too many indexes (slows down INSERT/UPDATE/DELETE)
- Indexes on columns with few unique values (e.g., yes/no)
- Very small tables (full scan is often faster)

---

### Part 13: Transactions—All or Nothing

**The Lemonade Stand Analogy**

You sell lemonade for $1. The customer gives you $1, you give them lemonade. If you drop the dollar before they get the lemonade, you don't just keep the dollar—you either complete the whole deal or you don't.

A **transaction** is a group of database operations that either **all succeed** or **all fail**. No half-completed changes.

```
Transfer $100 from Account A to Account B:

WITHOUT transaction:
  - Deduct $100 from A ✓
  - [Server crashes]
  - Add $100 to B ✗  → Money disappears!

WITH transaction:
  - BEGIN
  - Deduct $100 from A
  - Add $100 to B
  - COMMIT  → If anything fails, everything is undone (ROLLBACK)
```

---

### Part 14: ACID—What Makes Transactions Safe

**ACID** is four rules that make transactions reliable:

| Letter | Name        | Plain English                                      |
|--------|-------------|----------------------------------------------------|
| **A**  | Atomicity   | All or nothing—no half-completed transactions       |
| **C**  | Consistency | The database stays in a valid state before and after|
| **I**  | Isolation   | One transaction doesn't see another's half-done work|
| **D**  | Durability  | Once committed, changes survive crashes             |

**Simple examples:**
- **Atomicity**: Either both "deduct from A" and "add to B" happen, or neither does.
- **Consistency**: You can't have an order total that doesn't match the sum of its items.
- **Isolation**: While you're updating a row, someone else reading it sees the old value until you're done.
- **Durability**: After COMMIT, the database writes to disk so a power failure doesn't lose your data.

---

## Layer 2 — Professional Developer

Now we add the technical depth you need as a developer. Every concept still has a clear reason before we show the mechanics.

---

### SQL Deep Dive

#### Complex JOINs

**Multiple tables in one query:**
```sql
-- Get order details with user and product names
SELECT 
    users.name AS user_name,      -- Customer name
    orders.id AS order_id,        -- Order number
    orders.total,                 -- Order total
    products.name AS product_name,-- Product name
    order_items.quantity         -- How many
FROM orders
INNER JOIN users ON orders.user_id = users.id
INNER JOIN order_items ON orders.id = order_items.order_id
INNER JOIN products ON order_items.product_id = products.id
WHERE orders.id = 1;
```

**Self-JOIN (table joined to itself):**
```sql
-- Employees and their managers (both in same table)
SELECT 
    e.name AS employee_name,
    m.name AS manager_name
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
-- "e" and "m" are aliases for the same table
```

#### Performance of Different JOINs

| JOIN Type       | Speed      | Use When                          |
|-----------------|-----------|-----------------------------------|
| INNER JOIN      | Fastest   | You only want matching rows       |
| LEFT JOIN       | Slightly slower | You need all left-side rows  |
| RIGHT JOIN      | Similar to LEFT | Less common                   |
| FULL OUTER JOIN | Slowest   | Rare—when you need everything     |

**Tip:** Prefer INNER JOIN when you don't need rows without matches.

#### Window Functions

Window functions compute values across a set of rows related to the current row, without grouping rows together.

```sql
-- Running total of order amounts
SELECT 
    id,
    total,
    SUM(total) OVER (ORDER BY id) AS running_total
FROM orders;

-- Rank orders by total (highest first)
SELECT 
    id,
    total,
    RANK() OVER (ORDER BY total DESC) AS rank
FROM orders;
```

**Common window functions:** `ROW_NUMBER()`, `RANK()`, `DENSE_RANK()`, `SUM() OVER()`, `LAG()`, `LEAD()`.

#### EXISTS vs IN

**IN:** Builds a list from the subquery and checks membership.
```sql
SELECT * FROM users WHERE id IN (SELECT user_id FROM orders);
```

**EXISTS:** Stops as soon as it finds one matching row. Often faster for "does this exist?" checks.
```sql
SELECT * FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);
```

**When to use:**
- **IN:** Subquery returns a small list
- **EXISTS:** Checking existence; can short-circuit

#### UNION vs UNION ALL

| Type      | Duplicates | Speed   |
|-----------|------------|---------|
| UNION     | Removed    | Slower  |
| UNION ALL | Kept       | Faster  |

Use UNION when you need unique rows; use UNION ALL when duplicates are fine or impossible.

---

### Indexing in Depth

#### B-Tree Index Structure

Most indexes use a **B-Tree** (balanced tree):

```
                    [50]
                   /    \
              [25]      [75]
             /   \      /   \
        [10] [30]  [60] [90]
```

**Search:** Start at root, go left if value is smaller, right if larger. Time is O(log n) instead of O(n).

#### When to Create Indexes

**Create indexes on:**
1. Primary keys (usually automatic)
2. Foreign keys (for JOINs)
3. Columns in WHERE clauses
4. Columns in ORDER BY

```sql
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_created_at ON orders(created_at);
```

#### When Indexes Hurt

1. **Too many indexes** — Every INSERT/UPDATE/DELETE must update them
2. **Low selectivity** — Boolean or status with few values
3. **Frequently updated columns** — Index maintenance cost
4. **Very small tables** — Full scan is cheap

#### Composite Indexes

Index on multiple columns. Column order matters.

```sql
CREATE INDEX idx_users_email_status ON users(email, status);

-- ✅ Fast: WHERE email = 'x' AND status = 'y'
-- ✅ Fast: WHERE email = 'x'
-- ❌ Slow: WHERE status = 'y' (can't use index efficiently)
```

Put the most selective column first.

#### Covering Indexes

If the index contains all columns the query needs, the database may not need to read the table.

```sql
-- Query: SELECT id, email FROM users WHERE email = 'john@example.com';
CREATE INDEX idx_email_covering ON users(email, id);
-- Both email and id are in the index → no table lookup
```

---

### Transactions (Critical for Spring)

#### What Is a Transaction?

A **transaction** is a set of operations treated as one unit: either all succeed or all are rolled back.

```sql
BEGIN TRANSACTION;
    UPDATE accounts SET balance = balance - 100 WHERE id = 1;
    UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;  -- Both changes become permanent
-- If anything fails: ROLLBACK;  -- Both are undone
```

#### ACID in More Detail

| Property   | Meaning | Example |
|-----------|---------|---------|
| **Atomicity** | All or nothing | Transfer: deduct and add, or neither |
| **Consistency** | Valid state only | Order total = sum of items |
| **Isolation** | Concurrent transactions don't see each other's partial work | A's UPDATE invisible to B until COMMIT |
| **Durability** | Survives crashes | Data written to disk / WAL |

#### Commit and Rollback

- **COMMIT** — Make changes permanent
- **ROLLBACK** — Undo all changes since BEGIN

#### Why @Transactional Requires This Understanding

Spring's `@Transactional` wraps your method in a transaction. You need to know:

1. **What belongs in one transaction** — e.g., creating an order and its items together
2. **When rollback happens** — On unchecked exceptions by default
3. **How long transactions hold locks** — Keep them short; avoid slow work (email, HTTP calls) inside `@Transactional`

```java
// Good: One transaction for related work
@Transactional
public void createOrderWithItems(Order order, List<OrderItem> items) {
    orderRepository.save(order);
    items.forEach(orderItemRepository::save);
}

// Bad: Email inside transaction holds DB connection
@Transactional
public void processOrder(Order order) {
    orderRepository.save(order);
    emailService.send(order);  // 5 seconds! Connection held
}
```

---

### Transaction Isolation Levels

**Isolation level** controls how much one transaction sees of another's uncommitted or in-progress changes.

| Level            | Dirty reads | Non-repeatable | Phantom reads |
|------------------|-------------|----------------|---------------|
| Read Uncommitted | Yes         | Yes            | Yes           |
| Read Committed   | No          | Yes            | Yes           |
| Repeatable Read  | No          | No             | Yes           |
| Serializable     | No          | No             | No            |

**Quick definitions:**
- **Dirty read** — Reading another transaction's uncommitted change (can be rolled back)
- **Non-repeatable read** — Same row, different value on second read
- **Phantom read** — New rows appear between reads

**Typical defaults:** PostgreSQL, Oracle: Read Committed. MySQL (InnoDB): Repeatable Read.

**Spring configuration:**
```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void readData() { ... }

@Transactional(isolation = Isolation.REPEATABLE_READ)
public void calculateTotal() { ... }
```

---

## Layer 3 — Advanced Engineering Depth

---

### Query Optimization

#### EXPLAIN and EXPLAIN ANALYZE

**EXPLAIN** shows the plan without running the query. **EXPLAIN ANALYZE** runs it and adds real timing.

```sql
-- Without index: Full table scan
EXPLAIN SELECT * FROM users WHERE email = 'john@example.com';
-- Seq Scan on users  (cost=0.00..25000.00 rows=1 width=64)

-- With index: Index scan
CREATE INDEX idx_email ON users(email);
EXPLAIN SELECT * FROM users WHERE email = 'john@example.com';
-- Index Scan using idx_email on users  (cost=0.42..8.44 rows=1 width=64)
```

**Important plan terms:**
- **Seq Scan** — Full table scan (often slow on large tables)
- **Index Scan** — Uses index
- **Index Only Scan** — Uses covering index, no table access
- **Hash Join / Merge Join** — How tables are combined

#### Why Indexes Might Not Be Used

1. **Function on column:** `WHERE UPPER(email) = 'JOHN@EXAMPLE.COM'` — can't use normal index
2. **Leading wildcard:** `WHERE name LIKE '%John%'` — can't use index
3. **Low selectivity** — Planner may choose full scan
4. **OR on different columns** — May need UNION for efficiency

#### Query Rewriting Tips

- Avoid `SELECT *`; choose only needed columns
- Use range conditions instead of functions: `created_at >= '2026-01-01' AND created_at < '2027-01-01'`
- Add LIMIT when you only need a few rows
- Prefer EXISTS over IN for large subqueries when checking existence

---

### Database Locking

#### Row-Level vs Table-Level

| Type        | Granularity | Concurrency |
|-------------|-------------|-------------|
| Row-level   | Per row     | High        |
| Table-level | Entire table| Low         |

Modern databases (PostgreSQL, MySQL InnoDB) use row-level locking by default.

#### Shared vs Exclusive

- **Shared (read) lock** — Multiple readers OK, writers block
- **Exclusive (write) lock** — Only one holder; others wait

#### Deadlocks

**Deadlock:** Two transactions waiting on each other's locks.

**Example:**
```
Transaction A: Lock users.id=1, then wait for orders.id=1
Transaction B: Lock orders.id=1, then wait for users.id=1
→ Circular wait = deadlock
```

**Prevention:**
1. **Consistent lock order** — Always lock users before orders (or vice versa) everywhere
2. **Short transactions** — Don't hold locks during slow operations
3. **Lower isolation when safe** — Avoid Serializable unless needed
4. **Retry logic** — Catch deadlock exceptions and retry

#### Optimistic vs Pessimistic Locking

| Type        | Assumption       | Locking      | Best for              |
|-------------|------------------|--------------|------------------------|
| Optimistic  | Conflicts rare   | Version check| Read-heavy, low contention |
| Pessimistic | Conflicts common| Lock immediately | Write-heavy, high contention |

**Optimistic (JPA):**
```java
@Version
private Long version;
// UPDATE ... WHERE id = ? AND version = ?
// If version changed → OptimisticLockException
```

**Pessimistic:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
User findByIdForUpdate(Long id);
// SELECT ... FOR UPDATE
```

---

### Connection Management

#### Connection Overhead

Creating a connection is expensive: network, auth, memory. Reusing connections (pooling) avoids that cost.

#### Connection Pooling

A **pool** keeps a set of connections ready. The app borrows one, uses it, and returns it instead of closing.

```
App → Pool (get connection) → Use → Pool (return connection)
```

**HikariCP (Spring Boot default):**
```properties
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.max-lifetime=1800000
```

**Sizing:** Don't exceed database limits. A rough starting point: `(cores * 2) + disk_count`, then tune by monitoring.

---

### Normalization vs Denormalization

#### Normal Forms (1NF, 2NF, 3NF)

**1NF** — Atomic values, no repeating groups  
**2NF** — No partial dependency on a composite key  
**3NF** — No transitive dependency (non-key depends only on key)

#### When to Denormalize

| Scenario       | Prefer                |
|----------------|-----------------------|
| OLTP, many writes | Normalized         |
| OLAP, analytics  | Denormalized       |
| Need consistency | Normalized         |
| Need read speed  | Denormalized       |

Often: normalized OLTP for source of truth, denormalized copy for reporting.

---

### SQL vs NoSQL

| Use relational (SQL) when      | Consider NoSQL when          |
|-------------------------------|------------------------------|
| Structured, well-known schema | Unstructured / flexible      |
| Need ACID                     | High write throughput        |
| Complex JOINs                 | Simple lookups               |
| Strong consistency            | Eventual consistency OK      |

**CAP theorem:** In a distributed system you can typically guarantee only 2 of: Consistency, Availability, Partition tolerance. SQL systems often favor CP; many NoSQL systems favor AP.

---

## Layer 4 — Interview Mastery

Concise Q&A format for quick revision.

---

### Q1. Explain ACID with examples.

**Answer:** ACID ensures reliable transactions.

- **Atomicity:** All operations in a transaction succeed or all fail. Example: transfer money—deduct from A and add to B, or neither.
- **Consistency:** Database stays in a valid state. Example: order total must equal sum of line items.
- **Isolation:** One transaction doesn't see another's uncommitted changes.
- **Durability:** Committed data survives crashes (e.g., via WAL).

---

### Q2. What are transaction isolation levels? When use each?

**Answer:**

| Level           | Dirty | Non-repeatable | Phantom | Typical use      |
|-----------------|-------|----------------|---------|------------------|
| Read Uncommitted| Yes   | Yes            | Yes     | Rarely           |
| Read Committed  | No    | Yes            | Yes     | **Default**      |
| Repeatable Read | No    | No             | Yes     | Consistent reads |
| Serializable    | No    | No             | No      | Absolute consistency |

Use READ_COMMITTED by default. Use REPEATABLE_READ for multi-step calculations that must see a stable snapshot. Use SERIALIZABLE only when strict serialization is required.

---

### Q3. How do indexes work? When not to use them?

**Answer:** Indexes (usually B-Trees) allow O(log n) lookups instead of O(n) scans. Create them on columns used in WHERE, JOIN, and ORDER BY. Avoid them on: low-selectivity columns, very small tables, columns never used in queries, and when too many indexes would slow writes.

---

### Q4. Optimistic vs pessimistic locking?

**Answer:** Optimistic locking uses a version/timestamp and checks before update; no lock until write. Good for read-heavy, low contention. Pessimistic locking uses `SELECT ... FOR UPDATE`; row is locked immediately. Good for write-heavy, high contention.

---

### Q5. How would you debug a slow query?

**Answer:** 1) Identify the query (logs, APM). 2) Run EXPLAIN ANALYZE. 3) Look for Seq Scan, high cost, missing index. 4) Add indexes on filtered/joined columns. 5) Rewrite: avoid SELECT *, functions in WHERE, unnecessary JOINs. 6) Use LIMIT. 7) Run ANALYZE on tables.

---

### Q6. What causes deadlocks? How prevent them?

**Answer:** Circular wait: A holds lock 1, wants 2; B holds 2, wants 1. Prevention: lock resources in a consistent order (e.g., always users before orders), keep transactions short, use lower isolation when possible, avoid holding locks during slow I/O, implement retries on deadlock errors.

---

### Q7. Design a schema for e-commerce.

**Answer:** Tables: users (id, email, name), products (id, name, price, category_id), categories (id, name), orders (id, user_id, total, status), order_items (id, order_id, product_id, quantity, price), payments (id, order_id, amount, status). Use foreign keys, indexes on user_id, status, created_at, and normalize to 3NF.

---

### Q8. When would you denormalize?

**Answer:** When read performance matters more than write cost: reporting/OLAP, dashboards, search indexes. Trade-off: faster reads vs. more storage, slower writes, and harder consistency. Often use a normalized OLTP DB as source of truth and a denormalized reporting DB updated by ETL.

---

### Q9. Explain the N+1 problem.

**Answer:** One query loads parents (e.g., users), then N queries load children (e.g., orders). Fix with JOIN FETCH, EntityGraph, or batch fetching. Monitor via SQL logging; use DTOs or selective fetching when appropriate.

---

### Q10. How does connection pooling work?

**Answer:** A pool maintains a fixed set of connections. The app borrows one, uses it, and returns it. Benefits: avoid connection creation overhead, limit total connections, improve throughput. Configure min/max size, idle timeout, and max lifetime. Monitor active/idle/waiting counts.

---

## Summary

| Topic | Key Takeaway |
|-------|--------------|
| **Database** | Structured storage for permanent, shared data |
| **Tables/Rows/Columns** | Tables hold rows; columns define attributes |
| **Primary Key** | Unique ID for each row |
| **Foreign Key** | Links to another table's primary key |
| **SQL CRUD** | SELECT, INSERT, UPDATE, DELETE — always use WHERE for UPDATE/DELETE |
| **JOINs** | INNER = matches only; LEFT = all from left + matches |
| **Indexes** | Speed up lookups; use on WHERE/JOIN/ORDER BY; don't over-index |
| **Transactions** | All-or-nothing; ACID ensures reliability |
| **Isolation** | Controls visibility of concurrent changes |
| **Locking** | Optimistic (version) vs pessimistic (lock now) |
| **Connection pooling** | Reuse connections for performance and stability |

Understanding these concepts will make Spring Data JPA, Hibernate, and production debugging much easier.

---

[← Back to Index](00-README.md) | [Previous: Web Fundamentals](05-Prerequisites-Web-Fundamentals.md) | [Next: Dependency Injection →](07-Prerequisites-DI.md)
