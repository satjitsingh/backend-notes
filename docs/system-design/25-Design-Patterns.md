# 25. Design Patterns

> We've spent eight batches on *system-level* architecture — the big boxes. This chapter zooms into the *code* inside those boxes. Design patterns are battle-tested solutions to problems that recur in object-oriented code. They're the shared vocabulary that lets one engineer say "use a Strategy here" and another instantly understand the whole approach.

---

## What is it?

**Design patterns are reusable, named solutions to commonly-occurring problems in software design — at the level of classes and objects, *within* a program.**

Recall the distinction from Chapter 4: **architectural patterns** organize the *whole system* (monolith, microservices); **design patterns** organize the *code inside* one program (how classes are structured and collaborate). This chapter is about the latter.

A design pattern is *not* a finished piece of code you copy-paste. It's a **template** — a description of *how* to solve a problem that you adapt to your situation. The value is twofold:
1. **Proven solutions:** smart people already solved these problems well; you don't reinvent (often badly).
2. **Shared vocabulary:** saying "this is a Factory" communicates an entire design in one word. Patterns are the *language* senior engineers use to discuss code.

The classic patterns come from the **"Gang of Four" (GoF)** book and fall into three families:

**Creational** — *how objects are created.* (Singleton, Factory, Builder, Prototype, Abstract Factory.)
**Structural** — *how objects are composed into larger structures.* (Adapter, Decorator, Facade, Proxy, Composite.)
**Behavioral** — *how objects communicate and divide responsibility.* (Strategy, Observer, Command, Template Method, State, Iterator.)

> **A vital caution up front:** patterns are tools, not goals. Forcing patterns where they aren't needed creates *more* complexity than it removes — this is "pattern-itis." The mark of seniority is knowing *when* a pattern earns its keep, not memorizing all 23. Use a pattern when it genuinely simplifies; otherwise prefer simple code.

---

## How it Works Under the Hood

Let's go deep on the handful you'll *actually* use most, organized by family.

### Creational patterns

**Singleton** — ensures a class has *exactly one* instance, with a global access point.
- *Problem it solves:* some things should exist only once (a configuration manager, a connection pool, a logger). 
- *Mechanic:* private constructor + a static method returning the single shared instance.
- *Caution:* often overused; it's essentially a global variable, which makes testing hard and hides dependencies. In Spring, beans are singletons by default (managed by the container) — which is the *good* version, because the framework injects them rather than code reaching for a global.

**Factory (Factory Method / Simple Factory)** — delegates object *creation* to a separate method/class, so callers don't hard-code which concrete class they instantiate.
- *Problem it solves:* you need to create one of several related types based on some input, without scattering `new ConcreteType()` everywhere.
- *Mechanic:* a factory method takes a parameter and returns the right subtype behind a common interface.
- *Why it matters:* adding a new type means changing one factory, not hunting down every creation site (Open/Closed Principle below).

**Builder** — constructs a complex object step by step, avoiding "telescoping constructors" with dozens of parameters.
- *Problem it solves:* objects with many optional fields. `new Payment(a, null, b, null, null, c)` is unreadable and error-prone.
- *Mechanic:* a fluent builder: `Payment.builder().amount(500).currency("INR").idempotencyKey(k).build()`.
- Common in Java (Lombok's `@Builder` generates it).

### Structural patterns

**Adapter** — converts one interface into another that a client expects (a "translator").
- *Problem:* you have a class with the wrong interface (e.g., a third-party payment SDK) and code that expects a different one.
- *Mechanic:* wrap the incompatible class in an adapter that exposes the expected interface and translates calls.

**Decorator** — adds behavior to an object *dynamically*, by wrapping it, without changing its class.
- *Problem:* you want to add features (logging, caching, encryption) to an object without subclass explosion.
- *Mechanic:* wrap the object in a decorator that implements the same interface, adds its behavior, and delegates to the wrapped object. You can stack decorators.

**Facade** — a simple, unified interface in front of a complex subsystem.
- *Problem:* a subsystem with many moving parts is hard for clients to use.
- *Mechanic:* one class exposing a few easy methods that orchestrate the messy internals. *(The API Gateway from Chapter 22 is a Facade at the system level!)*

**Proxy** — a stand-in that controls access to another object (for lazy loading, access control, caching, remote calls).
- *Mechanic:* the proxy implements the same interface as the real object and intercepts calls (e.g., to check permissions or cache results before delegating). *(Spring AOP, `@Transactional`, and `@Cacheable` are implemented with dynamic proxies!)*

### Behavioral patterns

**Strategy** — defines a family of interchangeable algorithms and lets you swap them at runtime.
- *Problem:* you have multiple ways to do something (different payment methods, different pricing rules) and want to choose dynamically without giant if/else chains.
- *Mechanic:* define a common interface (`PaymentStrategy`), implement each algorithm as a class, inject/select the right one at runtime.
- One of the *most useful, least over-used* patterns — it replaces sprawling conditionals with clean polymorphism.

**Observer** — lets objects subscribe to and be notified of another object's changes (one-to-many).
- *Problem:* when one object changes, many others need to react, without tight coupling.
- *Mechanic:* the subject keeps a list of observers and notifies them on change. *(This is the in-process cousin of the Event-Driven Architecture from Chapter 21 — pub/sub is Observer at system scale.)*

**Command** — encapsulates a request as an object, so you can queue, log, or undo it.
- *Problem:* you want to parameterize, queue, schedule, or undo operations.
- *Mechanic:* wrap an action and its parameters in a command object with an `execute()` (and maybe `undo()`). *(The "task" dropped on a message queue in Chapter 18/19 is essentially a Command.)*

### The SOLID principles (the foundation patterns rest on)

Patterns are applications of deeper design principles. The **SOLID** principles are the bedrock of maintainable OO code:
- **S — Single Responsibility:** a class should have one reason to change (one job).
- **O — Open/Closed:** open for extension, closed for modification (add new behavior without editing existing code — Strategy/Factory enable this).
- **L — Liskov Substitution:** subtypes must be usable wherever their base type is expected.
- **I — Interface Segregation:** many small, focused interfaces beat one fat one.
- **D — Dependency Inversion:** depend on abstractions (interfaces), not concrete classes. *(This is the principle behind Spring's Dependency Injection — the single most important pattern in the Spring ecosystem.)*

> **Mental model:** SOLID tells you *what good design looks like*; design patterns are *recurring ways to achieve it*. Dependency Injection (Spring's core) is itself the practical embodiment of Dependency Inversion.

---

## Why do we need them?

We need design patterns because **they make code maintainable, extensible, and communicable — the qualities that determine whether a codebase stays healthy or rots over years:**

1. **Proven solutions to recurring problems.** You don't reinvent the wheel (and reinvent its bugs). Patterns encode decades of collective experience about what works.

2. **Shared vocabulary.** "Use a Strategy" or "wrap it in a Decorator" conveys a complete design instantly. This dramatically speeds up design discussions and code reviews — patterns are a *communication* tool as much as a coding one.

3. **Maintainability and extensibility.** Patterns (especially with SOLID) let you add features by *adding* code rather than *modifying* working code (Open/Closed) — reducing the risk of breaking what already works. A new payment method becomes a new Strategy class, not a risky edit to a 500-line method.

4. **Testability.** Dependency Inversion / DI lets you swap real dependencies for test doubles (mocks), making code unit-testable — critical for fintech where correctness is paramount.

**When to use them (and the warning):**
- Use a pattern when you recognize the *problem* it solves recurring in your code (multiple algorithms → Strategy; complex object creation → Builder/Factory; many reactors → Observer).
- **Don't** apply patterns prophylactically. Adding a Factory and three interfaces "in case we need flexibility later" for something with one implementation is over-engineering (Chapter 1's trap, at code level). Introduce the pattern *when the second case appears*, not before. Simple code first; patterns when the problem is real.

---

## Real-World / Fintech Example

In our **digital wallet / payments app**'s Spring Boot codebase, patterns appear constantly — many supplied *by the framework itself*. Here's where they earn their keep:

**Strategy — payment methods.** The app supports paying via wallet balance, linked bank account (UPI), and credit card. Instead of a giant `if (method == WALLET) {...} else if (method == UPI) {...}` block, the team defines a `PaymentStrategy` interface with implementations `WalletPaymentStrategy`, `UpiPaymentStrategy`, `CardPaymentStrategy`. At runtime the right strategy is selected by payment type. Adding "pay with credit line" later is a *new class* — no edits to existing, tested payment code (Open/Closed). This is the single cleanest win, replacing brittle conditionals with polymorphism.

**Factory — creating the right strategy.** A `PaymentStrategyFactory` takes the payment type and returns the correct strategy instance, so the controller doesn't hard-code which to build. (In Spring, this is often done by injecting a `Map<String, PaymentStrategy>` of all implementations — DI + Factory together.)

**Builder — constructing payment requests.** A payment has many fields (amount, currency, source, destination, idempotency key, metadata). The team uses a **Builder** (`@Builder`) so callers write readable, safe `Payment.builder()...build()` instead of an error-prone 8-argument constructor.

**Adapter — third-party gateways.** They integrate external providers (Stripe, a bank's API) whose SDKs have different interfaces. Each is wrapped in an **Adapter** exposing the app's internal `ExternalGateway` interface, so the rest of the code is shielded from provider-specific quirks (and swapping providers is localized).

**Proxy & Observer — handed to them by Spring.** They barely write these explicitly because the framework does:
- `@Transactional` and `@Cacheable` (Chapters 11, 17) work via **dynamic Proxy** — Spring wraps the bean and intercepts calls to start transactions / check the cache.
- `ApplicationEventPublisher`/`@EventListener` (the in-process events of Chapter 21) are the **Observer** pattern.
- Every `@Service`/`@Repository` injected via constructor is **Dependency Inversion** in action — the heart of testability (they mock the repository in unit tests).

**Facade — at two levels.** A `PaymentFacade` service exposes one `makePayment()` method that orchestrates the messy internals (validate → strategy → ledger transaction → publish event), giving controllers a simple entry point. And at the *system* level, the **API Gateway (Chapter 22) is a Facade** over all the microservices.

**Where they resisted patterns.** For a one-off internal admin tool with a single code path, the team deliberately wrote plain, simple code — *no* Strategy, *no* Factory — because there was only one case. Adding patterns there would have been pattern-itis. **Restraint is part of the skill.**

---

## Trade-offs (Pros & Cons)

**Pros**
- **Proven, reliable solutions** — avoid reinventing (and re-breaking) common designs.
- **Shared vocabulary** — communicate complex designs in a word; faster reviews and discussions.
- **Maintainability & extensibility** — add features by adding code, not editing working code (Open/Closed).
- **Testability** — DI/Dependency Inversion makes mocking and unit testing straightforward.
- **Decoupling** — patterns reduce tight coupling between components.

**Cons**
- **Over-engineering risk ("pattern-itis")** — applying patterns where simple code would do adds needless complexity and indirection.
- **Indirection can obscure** — heavy pattern use can make a simple flow hard to follow (jumping through many small classes).
- **Learning curve** — requires understanding the catalog and, crucially, *when* each applies.
- **Not a substitute for good design** — patterns help *express* good design but don't *create* it; misused, they make bad design more elaborate.

> **Staff-engineer takeaway:** Design patterns are **named, proven solutions** to recurring code-level problems and a **shared vocabulary** for discussing design. Master the high-value few — **Strategy** (swap algorithms, kill if/else chains), **Factory/Builder** (clean creation), **Adapter/Facade/Proxy** (structure and shielding), **Observer/Command** (the in-process cousins of events and queues) — and the **SOLID** principles beneath them (especially **Dependency Inversion**, which *is* Spring's DI). But patterns are tools, not trophies: apply one **when its problem actually recurs**, and prefer simple code otherwise. Notice that frameworks like Spring already hand you Proxy, Observer, and DI for free.

---

➡️ Next: [26-Resiliency.md](26-Resiliency.md) — zooming back out to a system property that's become non-negotiable: the ability to keep working (or fail gracefully) when things inevitably break.
