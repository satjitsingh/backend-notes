# Chapter 26: Transaction Management

[← Back to Index](00-README.md) | [Previous: Hibernate Internals](25-Hibernate-Internals.md) | [Next: N+1 Problem →](27-N+1-Problem.md)

---

## Why This Chapter Matters

Imagine a friend transfers $500 from their checking account to their savings account. The computer does two things: (1) takes $500 out of checking, (2) puts $500 into savings. What if the computer crashes right after step 1? Without the right protection, the $500 disappears into thin air — gone from checking, never arrived in savings.

That protection is called a **transaction**. With it, if step 2 fails, step 1 is undone automatically. The money goes back. Nothing is lost.

**`@Transactional`** ( pronounced "at transactional" ) is the most important annotation in Spring Boot — and the one developers misuse most often. People add it to methods without understanding how it works, then wonder why:
- Changes sometimes roll back when they shouldn't
- Changes sometimes don't roll back when they should
- Calling one method from another in the same class makes it seem like the annotation "does nothing"
- Read-only operations are slower than they need to be

This chapter makes all of that clear, from simple intuition to interview-level depth.

**Without understanding transactions:**
- Data can be corrupted (half-finished operations)
- Money, orders, or user data can vanish in production
- `@Transactional` will "not work" and you won't know why
- You'll misuse advanced options like propagation and isolation

**With proper understanding:**
- Data stays consistent — no partial updates, no lost records
- You'll know exactly where to put `@Transactional` and which settings to use
- You'll understand the proxy trap and avoid it
- You'll handle many users at once without data corruption
- You'll answer transaction questions confidently in interviews

---

## Layer 1 — Intuition Builder

### Part 1: What Is a Transaction? (All-or-Nothing Thinking)

A transaction is a **group of actions that must either all succeed together or all fail together**. There is no "half done."

**Everyday Analogy: Swapping Lunchboxes**

Imagine you and a friend swap lunchboxes:
1. You hand your lunchbox to your friend
2. Your friend hands their lunchbox to you

What if step 2 never happens (they drop it, or run away)? Without a transaction, you've already given away your lunch and have nothing in return. With a transaction, if anything goes wrong, both of you get your original lunchboxes back. Nobody loses.

**Bank Transfer Analogy:**

When you move money from one account to another:
1. The computer subtracts money from Account A
2. The computer adds money to Account B

If the power goes out between step 1 and step 2, you could lose money. A transaction says: "Either both steps succeed, or neither does. If something fails, undo everything." Like a big, invisible undo button.

**Plain English Summary:** A transaction is an "all-or-nothing" guarantee for a set of database changes.

---

### Part 2: The Four Safety Promises (ACID — But We'll Explain It Simply)

When databases handle transactions, they make four promises. People call these ACID (a technical acronym). Here's what each one means in plain words:

**1. Atomicity — "All or Nothing"**

Like a light switch: it's either on or off. There's no "half-on." Every action in the transaction either all happen, or none of them do. No in-between.

**2. Consistency — "Rules Stay True"**

The database always follows its own rules. For example, total money in the system shouldn't suddenly change. If $500 leaves one account, $500 must arrive in another. The database moves from one valid state to another.

**3. Isolation — "Nobody Peeks at Unfinished Work"**

Imagine you're writing a test and haven't finished. Your friend shouldn't see half-written answers. Same idea: while one transaction is still working, others shouldn't see its unfinished changes. Each transaction can feel like it's running alone.

**4. Durability — "Once Done, It Stays"**

When a transaction finishes successfully, the changes are permanent. Even if the computer crashes right after, the data is saved on disk. It won't disappear.

```
┌─────────────────────────────────────────────────────────────────────┐
│              THE FOUR SAFETY PROMISES (ACID)                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  A — ATOMICITY     "All or nothing — like a light switch"            │
│  C — CONSISTENCY   "Rules are always followed"                        │
│  I — ISOLATION     "Nobody sees my unfinished work"                  │
│  D — DURABILITY    "Once saved, it stays saved"                       │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Part 3: How Spring Helps You (The @Transactional Annotation)

You don't have to manually say "start transaction" and "commit" or "rollback" in your code. Spring does it for you when you use `@Transactional`.

**What @Transactional Does (Simple Version):**

Think of it as a wrapper:
1. Before the method runs: Spring tells the database "start a transaction"
2. The method runs (your code)
3. If everything goes well: Spring says "save it"
4. If something goes wrong: Spring says "undo everything"

**Plain English:** `@Transactional` tells Spring: "Wrap this method in a transaction. If anything fails, undo the whole thing."

```java
@Service
public class BankService {

    @Transactional  // This one line makes the whole method "all-or-nothing"
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {

        // Spring automatically says: "Begin transaction" (you don't see this)

        accountRepository.withdraw(fromId, amount);   // Step 1
        accountRepository.deposit(toId, amount);      // Step 2

        // If we reach here: Spring says "Commit" (save everything)
        // If ANY exception is thrown: Spring says "Rollback" (undo everything)
    }
}
```

**What "exception" means:** When your code hits an error (like a null pointer, or a failed payment), Java throws an exception. By default, Spring treats that as "something went wrong" and rolls back.

---

### Part 4: The Proxy — Spring's Secret Helper

Spring doesn't change your code. It wraps your class in a **proxy** — a fake version of your class that sits in front of the real one. When someone calls your method, they actually talk to the proxy first. The proxy starts the transaction, then calls your real method, then commits or rolls back.

**Analogy:** Think of a receptionist at a doctor's office. Patients don't walk straight into the doctor's room. They talk to the receptionist first. The receptionist (proxy) does setup work (start transaction), lets them in (call your method), then does cleanup (commit/rollback).

**Important rule:** The proxy only works when someone calls your method from *outside* your class. If you call your own method from inside the same class, you skip the receptionist. You go straight to the doctor. No setup, no cleanup — and no transaction.

---

### Part 5: The Self-Invocation Trap (When @Transactional Seems to Do Nothing)

This is the number one bug beginners hit.

**The Problem:**

You have two methods in the same class. One calls the other. The one being called has `@Transactional`. You expect a transaction, but it doesn't happen. Why?

Because when `methodA` calls `methodB` inside the same class, it's a direct call. The proxy is never used. No proxy = no transaction logic.

```
EXTERNAL CALL (works):
  Controller → Proxy → Transaction starts → Your method runs → Commit/Rollback

INTERNAL CALL (broken):
  methodA() → this.methodB()  ← Proxy is skipped! No transaction!
```

**Visual Diagram:**

```
┌──────────────────────────────────────────────────────────────┐
│                                                                │
│  External Call (WORKS):                                        │
│  Controller ──→ [PROXY] ──→ OrderService.createOrder()       │
│                  ↑                                            │
│          Transaction starts here ✅                           │
│                                                                │
│  Internal Call (BROKEN):                                      │
│  OrderService.processOrder() ──→ this.createOrder()          │
│                                   ↑                            │
│                          Proxy is SKIPPED! ❌                  │
│                          No transaction!                       │
│                                                                │
└──────────────────────────────────────────────────────────────┘
```

**Fixes (Simple Ideas):**

1. **Put @Transactional on the outer method** — The one that gets called from outside. Then the inner call runs inside that same transaction.

2. **Call yourself through the proxy** — Inject your own service and call it. That way you go through the proxy. (Sounds odd, but it works.)

3. **Split into two services** — Put the transactional method in a different class. Then when you call it, it's an "external" call and the proxy is used.

---

### Part 6: Read-Only Mode (A Free Speed Boost)

Some methods only read data. They never change anything. For those, you can mark the transaction as **read-only**. The database and Hibernate can then optimize: they don't need to watch for changes or prepare to save anything.

**Plain English:** `readOnly = true` means "I'm only looking, not changing anything." The system can work faster and use less memory.

**Rule of thumb:** If a method only fetches data (findById, findAll, search), add `readOnly = true`.

---

### Part 7: Rollback Rules — When Things Go Wrong

By default, Spring rolls back when an **unchecked exception** is thrown. In Java, unchecked exceptions are things like `NullPointerException`, `IllegalArgumentException`, or any `RuntimeException`. These usually mean "something went wrong in the program."

**Checked exceptions** (like `IOException`, `SQLException`) are different. By default, Spring does NOT roll back for those. Many people forget this and get surprised when data stays committed after an error.

**Fix:** Use `@Transactional(rollbackFor = Exception.class)` if you want *any* exception to trigger a rollback.

---

### Part 8: When Transactions Call Other Transactions (Propagation)

Sometimes a transactional method calls another transactional method. What happens? Do they share one transaction, or does the second one start its own?

**Propagation** controls this. Here's a simple table:

| Propagation   | Plain English Meaning                                              |
|---------------|--------------------------------------------------------------------|
| **REQUIRED**  | "Use an existing transaction if there is one, otherwise create one" (default) |
| **REQUIRES_NEW** | "Always start a brand new transaction, no matter what"          |
| **SUPPORTS**  | "Use a transaction if one exists, otherwise run without one"       |
| **NOT_SUPPORTED** | "Pause any current transaction and run without one"            |
| **MANDATORY** | "There MUST be a transaction already; otherwise throw an error"   |
| **NEVER**     | "There must NOT be a transaction; otherwise throw an error"       |
| **NESTED**    | "Create a savepoint inside the current transaction (like a sub-undo)" |

**You'll use REQUIRED almost all the time.** REQUIRES_NEW is useful when you need something to commit even if the main operation fails (e.g., audit logging).

---

### Part 9: Isolation — When Many Users Work at Once

**Isolation** controls how one transaction's changes are visible to other transactions while they're still in progress.

**Read phenomena (what can go wrong):**

1. **Dirty read** — You read data that another transaction hasn't committed yet. If they roll back, you've seen "ghost" data.

2. **Non-repeatable read** — You read a row twice. Between those two reads, another transaction changed it. You get different values each time.

3. **Phantom read** — You run the same query twice. Between those runs, another transaction adds or deletes rows. You see different numbers of rows.

**Isolation levels (from weakest to strongest):**

| Level              | Dirty Read | Non-Repeatable Read | Phantom Read |
|--------------------|------------|---------------------|--------------|
| READ_UNCOMMITTED   | Possible   | Possible            | Possible     |
| READ_COMMITTED     | No         | Possible            | Possible     |
| REPEATABLE_READ    | No         | No                  | Possible     |
| SERIALIZABLE       | No         | No                  | No           |

Stronger isolation = fewer weird cases, but slower and more locking. Most systems use **READ_COMMITTED** as the default, and that's fine for most applications.

---

## Layer 2 — Professional Developer

### Basic Usage

#### Where to Put @Transactional

**Best practice:** Put `@Transactional` on **service** methods that coordinate multiple operations, not on repository methods. Spring Data JPA already handles transactions for repository operations.

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    // WHY: This method does multiple things (order, payment, inventory)
    // All must succeed or all must fail — classic transaction use case
    @Transactional
    public Order createOrder(OrderDto dto) {
        // 1. Create order record
        Order order = new Order();
        order.setCustomerId(dto.getCustomerId());
        order.setItems(dto.getItems());
        order = orderRepository.save(order);

        // 2. Charge payment — if this fails, order above must roll back
        paymentService.processPayment(order.getId(), dto.getPaymentInfo());

        // 3. Update inventory — if this fails, everything above must roll back
        inventoryService.updateInventory(order.getItems());

        return order;
    }

    // WHY: This only reads data — use readOnly for performance
    @Transactional(readOnly = true)
    public Order findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
```

**Rules:**
- `@Transactional` works only on **public** methods (proxies intercept only public methods)
- Put it on the service layer, not the controller or repository
- Use `readOnly = true` for methods that only read

---

### Rollback Rules (Checked vs Unchecked Exceptions)

**Default behavior:**
- **RuntimeException** and **Error** → rollback
- **Checked exceptions** (e.g., `IOException`, custom `OrderException`) → commit (no rollback)

**Common mistake:** You throw a checked exception and expect a rollback, but the transaction commits.

**Fix:** Declare explicitly which exceptions should trigger rollback:

```java
@Service
public class OrderService {

    // WHY: OrderException is checked — without rollbackFor, it would NOT roll back
    @Transactional(rollbackFor = OrderException.class)
    public void createOrder(OrderDto dto) throws OrderException {
        // If OrderException is thrown, transaction will roll back
        Order order = saveOrder(dto);
        paymentService.charge(order.getId(), dto.getPaymentInfo());
    }

    // WHY: Sometimes you want to NOT roll back for certain exceptions
    @Transactional(noRollbackFor = ValidationException.class)
    public void validateAndSave(OrderDto dto) {
        // ValidationException → commit (e.g., you've logged it, it's expected)
        // Other exceptions → rollback
    }

    // WHY: Roll back on multiple exception types
    @Transactional(
        rollbackFor = {OrderException.class, PaymentException.class},
        noRollbackFor = ValidationException.class
    )
    public void complexOperation(OrderDto dto) {
        // Fine-grained control over rollback behavior
    }
}
```

---

### The Self-Invocation Trap — Fixes

**Fix 1: Move @Transactional to the outer method**

```java
@Service
public class OrderService {

    // Put @Transactional on the method that gets called from outside
    @Transactional
    public void processOrder(OrderDto dto) {
        createOrder(dto);  // Runs inside the same transaction
    }

    public void createOrder(OrderDto dto) {
        orderRepository.save(new Order(dto));
        paymentService.charge(dto.getPaymentInfo());
    }
}
```

**Fix 2: Self-injection (call through the proxy)**

```java
@Service
public class OrderService {

    @Autowired
    @Lazy  // Avoid circular dependency if createOrder calls something that calls back
    private OrderService self;  // Spring injects the PROXY

    public void processOrder(OrderDto dto) {
        self.createOrder(dto);  // Goes through proxy → transaction works
    }

    @Transactional
    public void createOrder(OrderDto dto) {
        orderRepository.save(new Order(dto));
        paymentService.charge(dto.getPaymentInfo());
    }
}
```

**Fix 3: Extract to a separate service**

```java
@Service
public class OrderOrchestrator {

    @Autowired
    private OrderService orderService;  // Different class = external call = proxy used

    public void processOrder(OrderDto dto) {
        orderService.createOrder(dto);  // Transaction works
    }
}

@Service
public class OrderService {

    @Transactional
    public void createOrder(OrderDto dto) {
        orderRepository.save(new Order(dto));
        paymentService.charge(dto.getPaymentInfo());
    }
}
```

---

### Propagation Types — In Practice

| Propagation   | When to Use |
|---------------|-------------|
| **REQUIRED**  | Default. Use for almost everything. |
| **REQUIRES_NEW** | Audit logging, notifications, error logs that must persist even if main operation fails |
| **SUPPORTS**  | Methods that work with or without a transaction |
| **NOT_SUPPORTED** | Pause transaction (e.g., for non-transactional external API calls) |
| **MANDATORY** | Method that must always run inside a transaction |
| **NEVER**     | Method that must never run inside a transaction (rare) |
| **NESTED**    | Partial rollback via savepoints (DB must support savepoints) |

**REQUIRED (default):**

```java
@Transactional(propagation = Propagation.REQUIRED)
public void createOrder(OrderDto dto) {
    Order order = saveOrder(dto);
    processPayment(order.getId());  // Same transaction; if payment fails, order rolls back too
}
```

**REQUIRES_NEW (audit log example):**

```java
@Transactional
public void createOrder(OrderDto dto) {
    Order order = saveOrder(dto);

    // Audit must be saved even if createOrder fails later
    auditService.logOrderCreated(order.getId());  // REQUIRES_NEW — commits independently

    processPayment(order.getId());  // If this fails, order rolls back, but audit stays
}

@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderCreated(Long orderId) {
        auditRepository.save(new AuditLog("Order created", orderId));
        // Commits immediately, separate from caller's transaction
    }
}
```

---

### Isolation Levels

| Level             | Use Case                                      |
|-------------------|-----------------------------------------------|
| READ_UNCOMMITTED  | Almost never — dirty reads possible           |
| READ_COMMITTED    | Default for most DBs; prevents dirty reads   |
| REPEATABLE_READ   | Need consistent reads within a transaction    |
| SERIALIZABLE      | Highest isolation; can cause deadlocks       |

**Practical guidance:** Stick with the default (usually READ_COMMITTED) unless you have a clear reason to change it.

```java
@Transactional(isolation = Isolation.READ_COMMITTED)  // Explicit default
public Account getAccount(Long id) {
    return accountRepository.findById(id);
}

@Transactional(isolation = Isolation.REPEATABLE_READ)
public BigDecimal calculateTotal(Long accountId) {
    // Multiple reads of same row must return same value
    Account a1 = accountRepository.findById(accountId);
    // ... do something ...
    Account a2 = accountRepository.findById(accountId);
    // a1 and a2 are guaranteed to have same data
    return a1.getBalance().add(a2.getBalance());
}
```

---

### Timeout

Prevent transactions from holding locks too long:

```java
@Transactional(timeout = 30)  // 30 seconds
public void longRunningOperation() {
    // If this takes more than 30 seconds, transaction is rolled back
}
```

---

### Complete Annotated Example

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED,
        timeout = 30,
        rollbackFor = {OrderException.class, PaymentException.class},
        readOnly = false
    )
    public Order createOrder(OrderDto dto) {
        // 1. Create order
        Order order = new Order();
        order.setCustomerId(dto.getCustomerId());
        order.setItems(dto.getItems());
        order = orderRepository.save(order);

        // 2. Process payment (same transaction)
        paymentService.processPayment(order.getId(), dto.getPaymentInfo());

        // 3. Update inventory (same transaction)
        inventoryService.updateInventory(order.getItems());

        // 4. Audit (REQUIRES_NEW — commits separately)
        auditService.logOrderCreated(order.getId());

        return order;
    }

    @Transactional(readOnly = true, timeout = 10)
    public Order findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
```

---

### Common Mistakes and Fixes

| Mistake | Problem | Fix |
|---------|---------|-----|
| Put @Transactional on private method | Proxy can't intercept private methods | Use public methods |
| Throw checked exception, expect rollback | Default: checked exceptions don't roll back | Add `rollbackFor = YourException.class` |
| Call @Transactional method from same class | Self-invocation bypasses proxy | Self-inject or extract to another service |
| No readOnly on read-only methods | Unnecessary overhead | Add `readOnly = true` |
| @Transactional on repository | Spring Data already does this | Put it on service methods |
| Long transaction with external API call | Holds DB connection and locks | Use REQUIRES_NEW or NOT_SUPPORTED for external calls |
| Use REQUIRES_NEW for related operations | Withdraw and deposit would commit independently | Use REQUIRED for logically atomic operations |

---

## Layer 3 — Advanced Engineering Depth

### How @Transactional Works (Proxy Mechanism)

Spring uses AOP (Aspect-Oriented Programming) with proxies. When you call a `@Transactional` method, the call is intercepted:

```
Client Code
    ↓
@Autowired OrderService  (actually gets a proxy)
    ↓
Spring Proxy (CGLIB or JDK Dynamic Proxy)
    ↓
Transaction Interceptor
    ↓
  1. Get transaction (or create new, per propagation)
  2. Call actual method
  3. On success: commit
  4. On exception: rollback (per rollback rules)
    ↓
Actual OrderService implementation
```

**Proxy creation:**
- If the bean implements an interface: JDK dynamic proxy (interface-based)
- Otherwise: CGLIB proxy (subclass)
- `@EnableTransactionManagement` (on by default in Spring Boot) enables this
- Transaction attributes come from `@Transactional` on the method/class

**Self-invocation:** `this.method()` does not go through the proxy, so no transaction logic runs. Only calls from other beans go through the proxy.

---

### Transaction Synchronization Callbacks

You can run code when a transaction commits or completes:

```java
@Transactional
public void createOrder(OrderDto dto) {
    Order order = saveOrder(dto);

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Runs only after successful commit
                sendEmail(order);
                publishEvent(order);
            }

            @Override
            public void afterCompletion(int status) {
                // Runs after commit OR rollback
                if (status == STATUS_COMMITTED) {
                    log.info("Committed: {}", order.getId());
                } else {
                    log.info("Rolled back: {}", order.getId());
                }
            }
        }
    );
}
```

Use cases: notifications after commit, event publishing, cleanup after rollback.

---

### Programmatic Transactions (TransactionTemplate)

For dynamic transaction boundaries or complex logic, use `TransactionTemplate`:

```java
@Service
public class OrderService {

    private final TransactionTemplate transactionTemplate;
    private final OrderRepository orderRepository;

    public Order createOrder(OrderDto dto) {
        return transactionTemplate.execute(status -> {
            Order order = new Order();
            order.setCustomerId(dto.getCustomerId());
            return orderRepository.save(order);
            // Commit on success, rollback on exception
        });
    }

    public Order createWithCustomSettings(OrderDto dto) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setTimeout(30);

        return template.execute(status -> {
            Order order = new Order();
            order.setCustomerId(dto.getCustomerId());
            return orderRepository.save(order);
        });
    }
}
```

Prefer `@Transactional` when possible; use programmatic control when you need it.

---

### Multiple Transaction Managers

If you have multiple data sources or transaction managers:

```java
@Configuration
@EnableTransactionManagement
public class TxConfig {

    @Bean
    @Primary
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public PlatformTransactionManager jdbcTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}

@Service
public class OrderService {

    @Transactional  // Uses primary (JPA)
    public void createOrder(OrderDto dto) { ... }

    @Transactional(transactionManager = "jdbcTransactionManager")
    public void customJdbcOperation() { ... }
}
```

---

### Transaction-Scoped EntityManager

The JPA `EntityManager` is typically transaction-scoped. Changes are tracked in the persistence context and flushed when the transaction commits:

```java
@Transactional
public void updateOrder(Long id) {
    Order order = entityManager.find(Order.class, id);
    order.setStatus(OrderStatus.COMPLETED);
    // No explicit save() — dirty checking flushes on commit
}
```

---

### Performance Considerations

**1. Batch operations in one transaction**

```java
// ❌ Each iteration gets its own transaction
@Transactional
public void processOrders(List<OrderDto> orders) {
    for (OrderDto dto : orders) {
        createOrder(dto);  // If createOrder is @Transactional, separate tx per call
    }
}

// ✅ One transaction for the whole batch
@Transactional
public void processOrders(List<OrderDto> orders) {
    for (OrderDto dto : orders) {
        Order order = new Order();
        order.setCustomerId(dto.getCustomerId());
        orderRepository.save(order);
    }
}
```

**2. readOnly for queries**

```java
@Transactional(readOnly = true)
public List<Order> findOrders() {
    // No dirty checking, no flush — less overhead
    return orderRepository.findAll();
}
```

**3. REQUIRES_NEW and connection pool**

Each `REQUIRES_NEW` needs a separate connection. Using it in a loop can exhaust the pool — use it sparingly.

**4. Timeout to avoid long locks**

```java
@Transactional(timeout = 10)
public void quickOperation() {
    // Rollback if it takes more than 10 seconds
}
```

---

### Phantom Reads Explained

**Phantom read:** You run `SELECT * FROM orders WHERE status = 'PENDING'` and get 5 rows. Another transaction inserts a new PENDING order and commits. You run the same query again and get 6 rows — a "phantom" row appeared.

**REPEATABLE_READ** prevents non-repeatable reads (same row changing) but in some databases can still allow phantom reads. **SERIALIZABLE** prevents both by serializing access.

---

## Layer 4 — Interview Mastery

### Q1: How does @Transactional work?

**Answer:** Spring uses AOP with proxies. At startup, it creates a proxy around beans that have `@Transactional` methods. When a method is called, the proxy intercepts, starts a transaction (or joins an existing one per propagation), calls the real method, then commits on success or rolls back on configured exceptions. The important detail: only calls that go through the proxy get transactional behavior. Self-invocation (`this.method()`) bypasses the proxy, so no transaction runs.

---

### Q2: Why doesn't @Transactional work when I call it from another method in the same class?

**Answer:** Because of the proxy. `@Transactional` is applied by a proxy that wraps your bean. When another class calls your method, it calls the proxy, which starts the transaction and then invokes your method. When you call `this.someMethod()` from inside the same class, you're calling the real object directly and skipping the proxy, so no transaction logic runs. Fix: move `@Transactional` to the outer method, self-inject and call through the proxy, or extract the transactional logic to a separate service.

---

### Q3: What is the default rollback behavior for checked exceptions?

**Answer:** By default, Spring rolls back only for unchecked exceptions (`RuntimeException` and `Error`). Checked exceptions do not trigger rollback. Use `@Transactional(rollbackFor = YourCheckedException.class)` if you want rollback for checked exceptions.

---

### Q4: When would you use REQUIRES_NEW vs REQUIRED?

**Answer:** Use **REQUIRED** (default) when the operation should participate in the caller's transaction — all succeed or all fail together. Use **REQUIRES_NEW** when the operation must commit independently, even if the caller rolls back — for example, audit logs, error logs, or notifications that must always be persisted.

---

### Q5: Explain dirty read, non-repeatable read, and phantom read.

**Answer:**
- **Dirty read:** Reading uncommitted data from another transaction. If that transaction rolls back, you've read invalid data.
- **Non-repeatable read:** Reading the same row twice and getting different values because another transaction committed a change in between.
- **Phantom read:** Running the same query twice and getting different numbers of rows because another transaction added or deleted matching rows.

READ_COMMITTED prevents dirty reads. REPEATABLE_READ prevents non-repeatable reads. SERIALIZABLE prevents phantom reads (and the others).

---

### Q6: What does readOnly = true do?

**Answer:** It tells Hibernate and the database that the transaction will not modify data. Hibernate can skip dirty checking and flushing. Some databases can route read-only transactions to replicas. Result: better performance and lower resource usage for query-only operations.

---

### Q7: What propagation types exist, and which is default?

**Answer:** REQUIRED (default), REQUIRES_NEW, SUPPORTS, NOT_SUPPORTED, MANDATORY, NEVER, NESTED. Default is REQUIRED: use an existing transaction or create a new one.

---

### Q8: How would you handle distributed transactions across multiple services?

**Answer:** Avoid traditional 2PC/XA when possible — it's complex and doesn't fit microservices well. Prefer patterns like **Saga** (choreography or orchestration) with compensating actions, or **event-driven** approaches with eventual consistency. Use an **outbox pattern** for reliable event publishing. Accept eventual consistency and design for it.

---

### Q9: What is the self-invocation trap, and how do you fix it?

**Answer:** Self-invocation is when a method in a class calls another `@Transactional` method in the same class via `this`. The proxy is bypassed, so no transaction runs. Fixes: (1) put `@Transactional` on the outer method, (2) inject the same bean (proxy) and call through it, or (3) move the transactional logic to a separate service and call that.

---

### Q10: Should @Transactional be on the controller, service, or repository?

**Answer:** On the **service** layer. Controllers should stay thin. Repositories are already transactional via Spring Data JPA. Service methods are where you coordinate multiple operations and need a single transactional boundary.

---

## Summary

**Core ideas:**
1. A transaction is an "all-or-nothing" unit of work.
2. ACID describes four guarantees: atomicity, consistency, isolation, durability.
3. `@Transactional` uses proxies to start/commit/rollback automatically.
4. Self-invocation bypasses the proxy — no transaction runs.
5. Use `readOnly = true` for read-only methods.
6. Checked exceptions do not roll back by default — use `rollbackFor`.
7. REQUIRED is the default propagation; REQUIRES_NEW is for independent commits.
8. READ_COMMITTED is the typical default isolation level.

**Best practices:**
- Put `@Transactional` on service methods.
- Use `readOnly = true` for queries.
- Keep transactions short.
- Explicitly configure `rollbackFor` for checked exceptions.
- Prefer self-injection or separate services over self-invocation of transactional methods.

**Common pitfalls:**
- Self-invocation bypassing the proxy
- Checked exceptions not rolling back
- Long transactions holding locks
- Missing `readOnly` on read-only methods
- Using REQUIRES_NEW for logically atomic operations

---

[← Back to Index](00-README.md) | [Previous: Hibernate Internals](25-Hibernate-Internals.md) | [Next: N+1 Problem →](27-N+1-Problem.md)
