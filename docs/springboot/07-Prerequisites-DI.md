# Chapter 7: Dependency Injection Concepts (Prerequisites)

[← Back to Index](00-README.md) | [Previous: Databases](06-Prerequisites-Databases.md) | [Next: Build Tools →](08-Prerequisites-Build-Tools.md)

---

## Why This Chapter Matters

Dependency Injection (DI) is **the heart of Spring Framework**. Everything Spring does—from managing your objects to making them work together—is built on this one idea. If you understand DI deeply, Spring Boot will feel natural. If you don't, you'll copy-paste annotations without knowing why and struggle when things go wrong.

**Without DI understanding:**
- You'll use `@Autowired` without knowing what it really does
- Testing will feel painful and confusing
- You'll create code that fights against Spring instead of flowing with it
- Interview questions about DI will stump you

**With solid DI understanding:**
- You'll write clean, testable code that Spring loves
- You'll know *why* constructor injection is preferred
- You'll design systems that are easy to change and extend
- You'll ace Spring interviews and code reviews

---

## Layer 1 — Intuition Builder

### Part 1: What Is a "Dependency"?

Imagine you're making a pizza.

To make a pizza, you need:
- Dough
- Sauce
- Cheese
- Toppings

**A dependency is something you need to do your job.** You can't make a pizza without dough. The dough is a *dependency* of pizza-making.

In code, it's the same idea. A class that processes orders needs:
- A way to take payments (payment gateway)
- A way to send emails (email service)
- A way to check inventory (inventory service)

Those are its **dependencies**—things it needs to work.

---

### Part 2: The Problem—When You Do Everything Yourself

Imagine a restaurant where the chef doesn't just cook. The chef also:
- Grows the vegetables in the backyard
- Raises the chickens
- Makes the plates from clay
- Delivers the food to tables

What happens when you want to change something?

- "Let's use organic vegetables from Farm A instead!"
  → The chef must stop cooking, dig up the garden, plant new seeds...
- "Let's use plastic plates instead of clay!"
  → The chef must learn pottery, buy a kiln...

**That's exhausting and ridiculous.** The chef should focus on *cooking*. Someone else should bring the ingredients.

**In code, this is called "tight coupling."** It means your class is glued to specific ways of doing things. It creates them itself. It can't easily switch to something else.

```
TIGHT COUPLING (Bad)
====================

    ┌─────────────────────────────────────┐
    │         OrderService                │
    │                                     │
    │  "I'll create my OWN payment!"      │
    │  payment = new StripePayment()      │
    │                                     │
    │  "I'll create my OWN email!"        │
    │  email = new SmtpEmailService()     │
    │                                     │
    │  Stuck. Can't change without        │
    │  rewriting THIS class.             │
    └─────────────────────────────────────┘
              │
              ▼
    ═══════════════════════════════════
    Glued together. Hard to separate.
    ═══════════════════════════════════
```

---

### Part 3: The Solution—Someone Delivers What You Need

Now imagine a *normal* restaurant.

The chef arrives. The refrigerator is already stocked. The suppliers delivered:
- Vegetables from Farm A (or B, or C—the chef doesn't care)
- Chicken from the butcher
- Plates from the manufacturer

The chef just *uses* what's there. The chef doesn't know (or care) which farm grew the tomatoes. The chef focuses on cooking.

**That's "loose coupling."** Your class receives what it needs. It doesn't create it. It doesn't know where it came from. It just uses it.

```
LOOSE COUPLING (Good)
====================

    ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
    │   Stripe     │     │  SmtpEmail   │     │  Inventory   │
    │   Payment   │     │   Service    │     │   Service    │
    └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
           │                   │                   │
           │   "Here you go!"   │   "Delivered!"   │
           └───────────┬───────┴─────────┬─────────┘
                       │                 │
                       ▼                 ▼
              ┌─────────────────────────────────┐
              │       OrderService              │
              │                                 │
              │  "I received my dependencies.   │
              │   I'll use whatever you gave    │
              │   me. Easy to swap!"            │
              └─────────────────────────────────┘
                       │
                       ▼
        ═══════════════════════════════════════
        Flexible. Can switch suppliers easily.
        ═══════════════════════════════════════
```

---

### Part 4: What Is "Injection"?

**Injection** means "delivering" or "giving" something to someone.

When you order pizza, you don't make the dough. The pizza place *injects* (delivers) a finished pizza to you. You receive it. You use it.

**Dependency Injection** means: *someone from the outside gives your class the things it needs.* Your class doesn't go out and get them. They are *injected*—handed in—through the constructor, a setter method, or (in some frameworks) directly into a field.

---

### Part 5: The Three Ways to Receive Dependencies

Think of your class as a house. Dependencies need to get inside. There are three doors:

**Door 1: Constructor (the front door when you move in)**  
Someone hands you everything as you enter. Once you're in, the doors are locked. You have what you need, and it never changes. *This is the best door.*

**Door 2: Setter (a side door you can open later)**  
You move in first. Later, someone knocks and hands you one thing at a time. You might get payment today, email tomorrow. Flexible, but you might forget something. *Use when things are optional.*

**Door 3: Field (someone drops things through the window)**  
Things just appear in your house. You didn't ask at the door. They're just... there. Convenient, but strange. Hard to see who gave you what. *Avoid in real projects.*

---

### Part 6: Why Interfaces Matter

Imagine you need "something that can deliver food."

You don't say: "I need Joe's Pizza Truck specifically, license plate XYZ123."

You say: "I need *a food delivery service*." It could be Joe's Pizza, Uber Eats, DoorDash—anyone who can deliver food.

**An interface is a promise.** It says: "I can do X." You don't care *who* does it, as long as they keep the promise.

In code:
- `PaymentGateway` = "something that can process payments"
- `EmailService` = "something that can send emails"

Your `OrderService` depends on *those promises* (interfaces), not on "Stripe" or "PayPal" specifically. That way, you can switch from Stripe to PayPal without changing `OrderService` at all.

---

### Part 7: Why This Helps Testing

Imagine you're testing a recipe. You don't want to wait for real tomatoes to grow. You use fake tomatoes (or toy tomatoes) to see if the recipe steps are correct.

**In testing, we use "mocks"—fake versions of dependencies.**

With tight coupling: Your class creates `new RealStripePayment()`. To test, you'd have to call the real Stripe API. Slow, costly, fragile.

With DI: Your class receives a `PaymentGateway`. In tests, you give it a *fake* `PaymentGateway` that just says "yes, I processed it." No real API. Fast, free, reliable.

---

### Part 8: What Is a "DI Container"?

A **container** is like a warehouse manager.

When you need an `OrderService`, you don't build it yourself. You ask the warehouse: "Give me an OrderService." The manager:
1. Checks what OrderService needs (payment, email, inventory)
2. Gets those things (or creates them if needed)
3. Puts them together
4. Hands you a ready-to-use OrderService

**A DI container does this automatically.** Spring's container is a smart warehouse that reads your code, figures out dependencies, creates objects in the right order, and injects them. You just say "I need OrderService" and it appears, fully wired.

---

## Layer 2 — Professional Developer

### What Is a Dependency? (Technical Definition)

A **dependency** is any object or service that a class needs to perform its responsibilities. If Class A uses Class B to do its job, B is a dependency of A.

### Tight Coupling vs. Loose Coupling

**Tight coupling:** A class directly creates or knows about concrete implementations. Changing one part forces changes in many places.

**Loose coupling:** A class depends on abstractions (interfaces) and receives implementations from outside. Changing implementations rarely requires changing the dependent class.

#### Tightly Coupled Code

```java
// BAD: OrderService creates its own dependencies
// Problem: Can't swap Stripe for PayPal without editing this file
public class OrderService {
    // Hard-coded: always Stripe, always SMTP, always Database
    private PaymentGateway payment = new StripePaymentGateway();
    private EmailService email = new SmtpEmailService();
    private InventoryService inventory = new DatabaseInventoryService();

    public void processOrder(Order order) {
        payment.processPayment(order.getAmount());
        email.sendConfirmation(order.getCustomerEmail());
        inventory.updateStock(order.getItems());
    }
}
```

#### Loosely Coupled Code

```java
// GOOD: Dependencies are injected from outside
// Benefit: Swap implementations without touching this class
public class OrderService {
    // Depends on abstractions (interfaces), not concrete classes
    private final PaymentGateway payment;
    private final EmailService email;
    private final InventoryService inventory;

    // Constructor: dependencies are INJECTED
    public OrderService(PaymentGateway payment, EmailService email, InventoryService inventory) {
        this.payment = payment;
        this.email = email;
        this.inventory = inventory;
    }

    public void processOrder(Order order) {
        payment.processPayment(order.getAmount());
        email.sendConfirmation(order.getCustomerEmail());
        inventory.updateStock(order.getItems());
    }
}
```

### Types of Dependency Injection

Spring supports three injection styles. Each has trade-offs.

#### DI Types Comparison

| Aspect | Constructor | Setter | Field |
|--------|-------------|--------|-------|
| **Immutability** | ✅ Can use `final` | ❌ No `final` | ❌ No `final` |
| **Required deps obvious** | ✅ Yes | ❌ No | ❌ No |
| **Testability** | ✅ Easy—pass mocks | ✅ Easy | ❌ Needs reflection/Spring |
| **Thread-safety** | ✅ Immutable = safe | ⚠️ Mutable | ⚠️ Mutable |
| **Spring support** | ✅ Preferred | ✅ Supported | ⚠️ Allowed but discouraged |
| **Optional deps** | ❌ Awkward | ✅ Natural | ✅ Possible |
| **Production use** | ✅ Preferred | ⚠️ For optional only | ❌ Avoid |

#### 1. Constructor Injection (Preferred)

**Why it's best:** Dependencies are required and immutable. The object is valid as soon as it's created.

```java
public class OrderService {
    // final = immutable; cannot be changed after construction
    private final PaymentGateway payment;
    private final EmailService email;

    // Dependencies MUST be provided at construction time
    public OrderService(PaymentGateway payment, EmailService email) {
        // Fail-fast: if null, object creation fails immediately
        this.payment = Objects.requireNonNull(payment, "PaymentGateway required");
        this.email = Objects.requireNonNull(email, "EmailService required");
    }

    public void processOrder(Order order) {
        payment.processPayment(order.getAmount());
        email.sendConfirmation(order.getCustomerEmail());
    }
}
```

**In tests:** `new OrderService(mockPayment, mockEmail)`—no framework needed.

#### 2. Setter Injection

**When to use:** Optional dependencies, or dependencies that may change during the object's lifetime.

```java
public class OrderService {
    // Not final—can be set later or changed
    private PaymentGateway payment;
    private EmailService email;

    // Setters allow injection after construction
    public void setPaymentGateway(PaymentGateway payment) {
        this.payment = payment;
    }

    public void setEmailService(EmailService email) {
        this.email = email;
    }

    public void processOrder(Order order) {
        if (payment != null) {
            payment.processPayment(order.getAmount());
        }
        if (email != null) {
            email.sendConfirmation(order.getCustomerEmail());
        }
    }
}
```

**Drawback:** Object can exist with null dependencies; you must remember to call setters.

#### 3. Field Injection

**Why Spring allows it:** Convenience for quick prototypes. **Why to avoid it:** Hidden dependencies, no immutability, testing pain.

```java
@Component
public class OrderService {
    // Framework injects via reflection—no constructor/setter
    @Autowired
    private PaymentGateway payment;

    @Autowired
    private EmailService email;

    // Problem: how do you create this in a unit test without Spring?
}
```

### Interface-Based Design

Interfaces define *contracts*. Your service depends on the contract, not the implementation.

```java
// Contract: "I can process payments"
public interface PaymentGateway {
    PaymentResult processPayment(double amount);
}

// Implementation 1
public class StripePaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult processPayment(double amount) {
        // Stripe-specific API calls
        return stripeApi.charge(amount);
    }
}

// Implementation 2
public class PayPalPaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult processPayment(double amount) {
        // PayPal-specific API calls
        return paypalApi.charge(amount);
    }
}

// Service depends on the interface—doesn't care which implementation
public class OrderService {
    private final PaymentGateway payment;

    public OrderService(PaymentGateway payment) {
        this.payment = payment; // Could be Stripe, PayPal, or a mock
    }
}
```

### Why DI Matters for Testing

Without DI, testing requires real external services. With DI, you inject mocks.

```java
// With DI—easy to test
@Test
void processOrder_shouldChargeCorrectAmount() {
    // Create a fake payment gateway that records calls
    PaymentGateway mockPayment = mock(PaymentGateway.class);
    EmailService mockEmail = mock(EmailService.class);

    OrderService service = new OrderService(mockPayment, mockEmail);
    Order order = new Order(100.0, "user@example.com");

    service.processOrder(order);

    // Verify behavior without calling real Stripe or sending real emails
    verify(mockPayment).processPayment(100.0);
    verify(mockEmail).sendConfirmation("user@example.com");
}
```

### Manual DI vs. Container

**Manual DI:** You create and wire everything yourself.

```java
public static void main(String[] args) {
    PaymentGateway payment = new StripePaymentGateway();
    EmailService email = new SmtpEmailService();
    InventoryService inventory = new DatabaseInventoryService();

    OrderService orderService = new OrderService(payment, email, inventory);
    orderService.processOrder(someOrder);
}
```

**Problem with manual DI:** As the app grows, dependency graphs become complex. You must create objects in the right order. A DI container (like Spring) does this automatically.

---

## Layer 3 — Advanced Engineering Depth

### The DI Container Concept

A **DI container** (also called an IoC container) inverts control: instead of your code creating dependencies, the container creates and wires them.

#### Inversion of Control

**Traditional (you control creation):**
```java
public class OrderService {
    private PaymentGateway payment = new StripePaymentGateway(); // You decide
}
```

**Inverted (container controls creation):**
```java
public class OrderService {
    private final PaymentGateway payment;

    public OrderService(PaymentGateway payment) {
        this.payment = payment; // Container provides this
    }
}
```

#### What a Container Does

1. **Scans** for beans (`@Component`, `@Service`, etc.)
2. **Builds a dependency graph** (who needs whom)
3. **Creates instances** in dependency order (dependencies first)
4. **Injects** dependencies via constructor/setter/field
5. **Manages lifecycle** (init, destroy, scopes)

#### Simplified DI Container (Conceptual)

```java
import java.util.*;

public class SimpleDIContainer {
    private Map<Class<?>, Object> singletons = new HashMap<>();
    private Map<Class<?>, Class<?>> typeMappings = new HashMap<>();

    public <T> void registerSingleton(Class<T> type, T instance) {
        singletons.put(type, instance);
    }

    public <T> void registerType(Class<T> interfaceType, Class<? extends T> implType) {
        typeMappings.put(interfaceType, implType);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        if (singletons.containsKey(type)) {
            return (T) singletons.get(type);
        }

        Class<?> impl = typeMappings.get(type);
        if (impl != null) {
            return createInstance(impl);
        }

        return createInstance(type);
    }

    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<T> clazz) {
        try {
            var constructors = clazz.getConstructors();
            var constructor = Arrays.stream(constructors)
                .max(Comparator.comparingInt(java.lang.reflect.Constructor::getParameterCount))
                .orElseThrow();

            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = resolve(paramTypes[i]); // Recursive resolution
            }

            return (T) constructor.newInstance(params);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create " + clazz.getName(), e);
        }
    }
}
```

Spring's `ApplicationContext` is a production-grade version with annotations, scopes, AOP, profiles, and more.

### Circular Dependencies

**Problem:** A needs B, B needs A. Neither can be created first.

```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(ServiceB serviceB) { this.serviceB = serviceB; }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    public ServiceB(ServiceA serviceA) { this.serviceA = serviceA; }
}
```

**Solutions:**
1. **Refactor**—extract shared logic into a third service (best)
2. **Setter or field injection**—Spring can break the cycle with proxies
3. **@Lazy**—inject a lazy proxy so real object is created later

### Scopes and Performance

- **Singleton (default):** One instance per container. Memory-efficient, shared. Must be thread-safe.
- **Prototype:** New instance per request. Heavier on memory, but no shared state.
- **Request/Session (web):** Lifecycle tied to HTTP request/session.

**Edge case:** Injecting a prototype bean into a singleton gives you the same prototype instance for that singleton's lifetime unless you use `ObjectFactory` or `Provider` to get a fresh instance each time.

---

## Layer 4 — Interview Mastery

### Q1: What is Dependency Injection?

**Answer:** DI is a pattern where a class receives its dependencies from outside instead of creating them itself. Dependencies are *injected* via constructor, setter, or field. This enables loose coupling, testability, and easier swapping of implementations.

---

### Q2: What is the difference between tight and loose coupling?

**Answer:** **Tight coupling:** A class directly creates or depends on concrete implementations. Changing one forces changes elsewhere. **Loose coupling:** A class depends on abstractions (interfaces) and receives implementations from outside. Implementations can change without modifying the dependent class.

---

### Q3: Why is constructor injection preferred?

**Answer:** (1) Immutability via `final`, (2) required dependencies are explicit, (3) easy to test—`new MyService(mock1, mock2)`, (4) thread-safe when immutable, (5) fail-fast if a dependency is missing.

---

### Q4: What are the problems with field injection?

**Answer:** (1) Cannot use `final`, (2) hidden dependencies, (3) harder to unit test without Spring/reflection, (4) violates encapsulation, (5) object cannot be constructed manually.

---

### Q5: How does DI improve testability?

**Answer:** You inject mock or stub implementations instead of real ones. No real database, API calls, or email. Tests are fast, reliable, and isolated.

---

### Q6: What is a DI container?

**Answer:** A framework that creates objects, resolves their dependencies, and injects them. It manages lifecycle (creation, destruction, scopes). Spring's `ApplicationContext` is a DI container.

---

### Q7: What is Inversion of Control (IoC)?

**Answer:** IoC means control of object creation shifts from your code to a framework. Your code doesn't `new` dependencies; the framework provides them. DI is one way to implement IoC.

---

### Q8: How do you resolve circular dependencies?

**Answer:** (1) Refactor to remove the cycle (e.g., extract a shared service). (2) Use setter/field injection so Spring can use proxies. (3) Use `@Lazy` on one of the dependencies to break the cycle.

---

### Q9: Why use interfaces with DI?

**Answer:** Interfaces define contracts. A class that depends on an interface can work with any implementation. This enables swapping implementations (Stripe vs. PayPal), using mocks in tests, and following the Dependency Inversion Principle.

---

### Q10: Can you implement DI without Spring?

**Answer:** Yes. Manual DI: create dependencies and pass them via constructor. Or build a simple container that registers types, resolves constructor parameters recursively, and creates instances. Spring is a production-ready version of this.

---

## Summary

| Concept | Key Takeaway |
|---------|--------------|
| **Dependency** | Something a class needs to do its job |
| **Tight coupling** | Class creates its own dependencies; hard to change |
| **Loose coupling** | Class receives dependencies; easy to swap |
| **Dependency Injection** | Providing dependencies from outside (constructor, setter, field) |
| **Constructor injection** | Preferred: immutable, explicit, testable |
| **Interface-based design** | Depend on abstractions; swap implementations easily |
| **Testing** | DI lets you inject mocks—fast, isolated tests |
| **DI container** | Creates objects, resolves dependencies, manages lifecycle |

Master these ideas and Spring Boot will feel natural. DI is not a Spring trick—it's a fundamental design approach that makes code flexible, testable, and maintainable.

---

[← Back to Index](00-README.md) | [Previous: Databases](06-Prerequisites-Databases.md) | [Next: Build Tools →](08-Prerequisites-Build-Tools.md)
