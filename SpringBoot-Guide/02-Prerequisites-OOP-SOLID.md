# Chapter 2: OOP & SOLID Principles (Prerequisites)

[← Back to Index](00-README.md) | [Previous: Java Foundations](01-Prerequisites-Java.md) | [Next: Design Patterns →](03-Prerequisites-Design-Patterns.md)

---

## Why This Chapter Matters

Imagine playing with LEGO blocks. You don't need to know how each brick is molded in a factory. You just know: *"This piece connects to that piece."* The blocks hide their complexity—you use simple rules to build amazing things.

**Spring Boot works the same way.** It's a toolbox built on simple rules called **OOP** (Object-Oriented Programming) and **SOLID**. These aren't fancy words—they're the *"how LEGO pieces connect"* of software. When you use Spring's `@Service`, `@Repository`, or `@Autowired`, you're following rules that were designed decades ago to make code understandable, changeable, and safe.

**Why does this matter for you?**
- **Without this**: You copy Spring code without knowing *why* it's structured that way. When something breaks, you guess. In interviews, you struggle to explain your design choices.
- **With this**: You *design* Spring applications. You know why layers exist, why interfaces matter, and why dependency injection isn't just "a Spring thing."

**What you'll learn**: We'll start with everyday ideas (like a restaurant kitchen, a toy box, or a school backpack), then connect each idea to real Java code, and finally to how Spring uses it. By the end, you'll see Spring not as magic—but as careful application of these principles.

---

## Layer 1 — Intuition Builder

*Think of this section as explaining to a curious 10-year-old. No jargon until we've built the picture.*

---

### Part 1: What is "Object-Oriented"? (The Toy Box Analogy)

Imagine you have a toy box. Inside, you have:

- A **car toy** (it can roll, has wheels)
- A **robot toy** (it can walk, has arms)
- A **ball** (it can bounce)

Each toy is **different**, but you can *organize* them. You might put all "things that roll" in one pile, and "things that move on legs" in another. That's **organization**—grouping things by what they *do* or what they *are*.

**In programming**, we do the same thing. Instead of toys, we have **objects**. An object is like a little box that holds:
1. **Data** (information—like a car's color or a robot's battery level)
2. **Actions** (things it can do—like "roll" or "walk")

```
┌─────────────────────┐     ┌─────────────────────┐
│   CAR TOY           │     │   ROBOT TOY          │
│   Data: color, size │     │   Data: battery, name │
│   Action: roll()    │     │   Action: walk()      │
└─────────────────────┘     └─────────────────────┘
```

**Key idea**: We put related data and actions *together* in one place. That's the heart of **Object-Oriented Programming**—everything is organized into objects.

---

### Part 2: Encapsulation (Your Secret Diary)

You have a diary. You don't want your little sibling to read it. So you:
- **Hide** the pages inside a locked cover
- Let them use only the **lock** (you have the key)

The diary **encapsulates** your secrets. "Encapsulate" means *to wrap something and control who can touch it*.

**In code**, we do the same. Some data is *private* (like the diary pages). We only allow access through specific actions (like "write a page" or "read a page")—and we can add rules. For example: "You can only withdraw money if you have enough."

```
┌──────────────────────────────────────────┐
│  BANK ACCOUNT (the diary)                │
│  ┌────────────────────────────────────┐  │
│  │ balance = 100 (HIDDEN - private)   │  │
│  └────────────────────────────────────┘  │
│  Allowed actions:                         │
│  • deposit(amount)  ← adds money          │
│  • withdraw(amount) ← checks rules first  │
│  • getBalance()    ← just looks, no change│
└──────────────────────────────────────────┘
```

**Why it matters**: If everyone could reach in and change the balance directly, someone could set it to a million! Encapsulation protects our data and ensures only *valid* changes happen.

---

### Part 3: Inheritance (Family Resemblance)

A **dog** is a kind of **animal**. A **goldfish** is also a kind of **animal**. They're different, but they share something: animals eat, breathe, and move.

So we say: Dog **inherits** from Animal. The dog gets "eating" and "breathing" for free—it doesn't need to define them again. But the dog adds its own thing: **barking**.

```
        ANIMAL
       (eat, breathe)
          /    \
         /      \
       DOG     GOLDFISH
    (bark)    (swim)
```

**Key idea**: We describe the *general* (Animal) once. Then we describe the *specific* (Dog) by saying "it's an animal, plus barking." That's **inheritance**—children get traits from parents.

---

### Part 4: Polymorphism (One Word, Many Meanings)

When your mom says "**Draw!**" to you, you might grab a pencil and draw on paper. When she says "**Draw!**" to your little brother, he might draw on a whiteboard. Same word—**different actions**. That's **polymorphism** (many forms).

**In code**: We might have a method called `draw()`. A **Circle** object draws a circle. A **Square** object draws a square. We call `draw()` on different objects—and each one does its own thing. We don't need a separate command for "draw circle" and "draw square."

```
  draw() on Circle  →  ● (draws circle)
  draw() on Square  →  ■ (draws square)
  Same command, different result!
```

---

### Part 5: Abstraction (The TV Remote)

You press "Volume Up" on the remote. You don't know—or care—whether the TV uses cables, infrared, or Bluetooth. The remote **hides** the complicated stuff. You only see: *buttons that do things*.

**Abstraction** = hiding the complicated details and showing only what matters.

In programming, we use **interfaces** (a list of "what something can do") without saying *how* it does it. A "payment" button might use a credit card, PayPal, or crypto—we don't care. We just say "pay." The **abstraction** is "pay"; the **details** are hidden.

---

### Part 6: The Five SOLID Rules (Like Kitchen Rules)

Imagine a busy restaurant kitchen. Good chefs follow rules so the kitchen doesn't become chaos:

| Rule | Kitchen Analogy | In One Sentence |
|------|-----------------|-----------------|
| **S**ingle job | One person chops vegetables, another grills. Nobody does *everything*. | One job per person (or class). |
| **O**pen/Closed | Want to add a new dish? Add a new recipe. Don't rewrite the old ones. | Add new things without changing old things. |
| **L**iskov | If a trainee replaces the head chef, the food should still be good. No surprises. | Substitutes must work the same way. |
| **I**nterface Segregation | Don't hand a salad chef a steak recipe. Give each person only what they need. | Don't force people to learn stuff they won't use. |
| **D**ependency Inversion | The head chef doesn't care *which farm* the vegetables come from. He just needs vegetables. | Depend on "what you need," not "where it comes from." |

**Why "SOLID"?** It's an acronym. Each letter stands for one principle. Together, they help us write code that's easy to change, easy to understand, and easy to extend.

---

### Part 7: How Spring Uses These Ideas (The Big Picture)

Spring is like a *smart restaurant manager*. It doesn't cook—it *organizes*:

- **Encapsulation**: Spring manages complicated stuff (database connections, transactions) behind simple annotations. You don't see the internals.
- **Inheritance**: `@Controller`, `@Service`, `@Repository` are all special types of "components." They inherit the idea of "being managed by Spring."
- **Polymorphism**: You write `UserRepository repository`—Spring gives you the *right* implementation (JPA, JDBC, etc.) without you caring which one.
- **Abstraction**: You depend on *interfaces*, not concrete classes. Spring provides the implementation.
- **SOLID**: Spring's layers (Controller → Service → Repository) follow Single Responsibility. Its design lets you extend without modifying (Open/Closed). Dependency Injection is pure Dependency Inversion.

```
┌─────────────────────────────────────────────────────────┐
│  YOU WRITE:  "I need a UserRepository"                  │
│  SPRING:     "Here's the right one." (injects it)       │
│  YOU NEVER:  say "give me MySQL version" or "JPA one"   │
└─────────────────────────────────────────────────────────┘
```

---

## Layer 2 — Professional Developer

*Now we connect intuition to real code. Every concept has WHY before HOW, and code is commented.*

---

### Encapsulation (Data Hiding) — Technical Deep Dive

**Why it matters**: Without encapsulation, any part of your program can corrupt data. With it, you control access, validate changes, and can refactor internals safely.

| Aspect | Without Encapsulation | With Encapsulation |
|--------|------------------------|---------------------|
| Data access | Direct (anyone can change) | Through methods only |
| Validation | Scattered, easy to forget | Centralized in setters |
| Refactoring | Risky (many places touch data) | Safe (change internals once) |

**Bad code** (no encapsulation):
```java
// ❌ BAD: Anyone can set balance to anything
public class BankAccount {
    public double balance;  // Exposed! No rules!
}

// Usage - disaster waiting to happen
account.balance = -1000;      // Invalid state!
account.balance = 999999999;   // No validation!
```

**Good code** (encapsulation):
```java
// ✅ GOOD: Controlled access with rules
public class BankAccount {
    private double balance;  // Hidden - cannot access directly
    
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        balance += amount;  // Only valid changes allowed
    }
    
    public void withdraw(double amount) {
        if (amount > balance) {
            throw new IllegalStateException("Insufficient funds");
        }
        balance -= amount;
    }
    
    public double getBalance() {
        return balance;  // Read-only access
    }
}
```

**Spring example**: `@Transactional` encapsulates transaction logic. You don't see connection handling, commit/rollback—it's all hidden behind the annotation.

---

### Inheritance (Is-A Relationship) — Technical Deep Dive

**When to use**: Clear "is-a" relationship, shared behavior, polymorphism needed.

**When NOT to use**: Just for code reuse (prefer composition), "has-a" relationships, forcing awkward hierarchies (e.g., Square extending Rectangle).

```java
// Base class - shared behavior
public class Animal {
    protected String name;  // protected = visible to subclasses
    
    public void eat() {
        System.out.println(name + " is eating");
    }
}

// Derived class - adds specific behavior
public class Dog extends Animal {
    public void bark() {
        System.out.println(name + " says Woof!");
    }
}

// Usage
Dog dog = new Dog();
dog.name = "Buddy";
dog.eat();   // Inherited from Animal
dog.bark();  // Own method
```

**Spring example**: `@RestController` extends `@Controller` extends `@Component`. All are "components" managed by Spring, with increasing specialization.

---

### Polymorphism — Technical Deep Dive

**Two types**:

1. **Compile-time (Method Overloading)**: Same method name, different parameters.
2. **Runtime (Method Overriding)**: Same method signature, different implementation—chosen at runtime.

```java
// Runtime polymorphism
public class Animal {
    public void makeSound() {
        System.out.println("Some sound");
    }
}

public class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Woof!");
    }
}

// The magic: reference type vs object type
Animal myPet = new Dog();  // Reference: Animal, Object: Dog
myPet.makeSound();        // Prints "Woof!" - runtime decides
```

**Spring example**: `List<UserRepository>` can hold any implementation. Your service depends on the interface; Spring injects the concrete implementation. Polymorphism makes this possible.

---

### Abstraction — Technical Deep Dive

**Abstract class**: Can have both abstract methods (must be implemented) and concrete methods (shared code).

**Interface**: Pure contract—"what" without "how." (Java 8+ allows default methods.)

```java
// Interface = pure abstraction
public interface PaymentProcessor {
    void processPayment(double amount);
    void refund(String transactionId);
}

// Implementation = the "how"
public class CreditCardProcessor implements PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        // Credit card specific logic
    }
    
    @Override
    public void refund(String transactionId) {
        // Refund logic
    }
}
```

**Spring example**: `UserRepository` interface abstracts database access. Your service depends on it; Spring Data provides the implementation.

---

### S — Single Responsibility Principle (SRP)

**Definition**: A class should have only **one reason to change**. One job.

**Why**: Multiple responsibilities → multiple reasons to change → fragile, hard-to-test code.

**Bad code**:
```java
// ❌ BAD: User class does too much
public class User {
    private String name;
    private String email;
    
    public void save() { /* Database logic */ }      // Responsibility 1
    public void sendEmail() { /* Email logic */ }    // Responsibility 2
    public boolean validate() { /* Validation */ }  // Responsibility 3
}
// If email service changes, we modify User. If DB schema changes, we modify User.
```

**Good code**:
```java
// ✅ GOOD: One responsibility per class

public class User {
    private String name;
    private String email;
    // Just data - getters/setters only
}

public class UserRepository {
    public void save(User user) { /* DB only */ }
}

public class EmailService {
    public void sendEmail(User user) { /* Email only */ }
}

public class UserValidator {
    public boolean validate(User user) { /* Validation only */ }
}
```

**How Spring enforces SRP**:

| Layer | Annotation | Responsibility |
|-------|------------|----------------|
| Web | `@Controller` / `@RestController` | HTTP requests/responses only |
| Business | `@Service` | Business logic only |
| Data | `@Repository` | Data access only |

```java
// Spring's layered design = SRP in action
@RestController
public class UserController {
    private UserService userService;
    // ONLY handles HTTP - delegates to service
}

@Service
public class UserService {
    private UserRepository userRepository;
    private EmailService emailService;
    // ONLY orchestrates business logic
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // ONLY data access
}
```

---

### O — Open/Closed Principle (OCP)

**Definition**: Software should be **open for extension, closed for modification**. Add new behavior by *extending* (new classes, implementations), not by *changing* existing code.

**Why**: Modifying existing code risks breaking what already works. Extending is safer.

**Bad code**:
```java
// ❌ BAD: Adding new payment type forces modification
public class PaymentProcessor {
    public void process(Payment payment) {
        if (payment.getType().equals("CREDIT_CARD")) {
            // credit card logic
        } else if (payment.getType().equals("PAYPAL")) {
            // PayPal logic
        }
        // Adding CRYPTO? Must add another else-if here - MODIFICATION!
    }
}
```

**Good code**:
```java
// ✅ GOOD: New payment type = new class, no modification

public interface PaymentProcessor {
    void process(double amount);
}

public class CreditCardProcessor implements PaymentProcessor {
    @Override
    public void process(double amount) { /* credit card */ }
}

public class PayPalProcessor implements PaymentProcessor {
    @Override
    public void process(double amount) { /* PayPal */ }
}

// NEW: Add Crypto - just implement interface. No changes to existing code!
public class CryptoProcessor implements PaymentProcessor {
    @Override
    public void process(double amount) { /* crypto */ }
}
```

**How Spring uses OCP**: `HandlerInterceptor`, `BeanPostProcessor`, `ApplicationListener`—you *implement* them to add behavior. Spring's code stays unchanged.

---

### L — Liskov Substitution Principle (LSP)

**Definition**: Subtypes must be **substitutable** for their base types. Code that works with the parent must work with the child—no surprises.

**Why**: Violating LSP breaks polymorphism. You think you have a "Rectangle" but get unexpected behavior.

**Classic violation (Rectangle/Square)**:
```java
// ❌ BAD: Square "is-a" Rectangle? Breaks expectations!
public class Rectangle {
    protected int width, height;
    public void setWidth(int w) { width = w; }
    public void setHeight(int h) { height = h; }
    public int getArea() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) {
        width = w;
        height = w;  // Square: both sides equal - but breaks Rectangle contract!
    }
    @Override
    public void setHeight(int h) {
        height = h;
        width = h;
    }
}

// Code expecting Rectangle fails with Square
Rectangle r = new Square();
r.setWidth(5);
r.setHeight(4);
r.getArea();  // Expect 20, get 16! LSP violated.
```

**Fix**: Don't force inheritance. Use separate types or composition.

```java
// ✅ GOOD: Both extend Shape, no substitution violation
public abstract class Shape {
    public abstract int getArea();
}

public class Rectangle extends Shape {
    private int width, height;
    public Rectangle(int w, int h) { width = w; height = h; }
    @Override
    public int getArea() { return width * height; }
}

public class Square extends Shape {
    private int side;
    public Square(int s) { side = s; }
    @Override
    public int getArea() { return side * side; }
}
```

**Spring and LSP**: When you extend a service or repository, the subclass must honor the parent's contract. Spring injects based on the type—LSP ensures substitution works.

---

### I — Interface Segregation Principle (ISP)

**Definition**: Clients should not be forced to depend on interfaces they don't use. **Small, focused interfaces** beat one big "fat" interface.

**Why**: Fat interfaces force classes to implement methods they don't need—leading to empty methods or thrown exceptions.

**Bad code**:
```java
// ❌ BAD: Fat interface - Developer doesn't design, Designer doesn't code
public interface Worker {
    void work();
    void code();
    void design();
    void test();
    void deploy();
}

public class Developer implements Worker {
    @Override
    public void design() {
        throw new UnsupportedOperationException();  // Forced to implement!
    }
}
```

**Good code**:
```java
// ✅ GOOD: Segregated interfaces
public interface Codable {
    void code();
}

public interface Designable {
    void design();
}

public class Developer implements Codable {
    @Override
    public void code() { /* ... */ }
}

public class Designer implements Designable {
    @Override
    public void design() { /* ... */ }
}
```

**Spring example**: `CrudRepository` → `PagingAndSortingRepository` → `JpaRepository`. Each adds a focused set of methods. You extend only what you need.

---

### D — Dependency Inversion Principle (DIP)

**Definition**:
1. High-level modules should not depend on low-level modules. Both should depend on **abstractions**.
2. Abstractions should not depend on details. Details depend on abstractions.

**Translation**: Depend on **interfaces**, not concrete classes. This is the **foundation of Spring Dependency Injection**.

**Bad code**:
```java
// ❌ BAD: UserService (high-level) depends on MySQLDatabase (low-level)
public class UserService {
    private MySQLDatabase database = new MySQLDatabase();  // Tight coupling!
    
    public void createUser(String name) {
        database.save(name);  // Stuck with MySQL forever
    }
}
```

**Good code**:
```java
// ✅ GOOD: Both depend on abstraction

public interface Database {
    void save(String data);
}

public class MySQLDatabase implements Database {
    @Override
    public void save(String data) { /* MySQL specific */ }
}

public class UserService {
    private Database database;  // Interface, not concrete class!
    
    public UserService(Database database) {
        this.database = database;  // Injected - can be any implementation
    }
    
    public void createUser(String name) {
        database.save(name);
    }
}
```

**Spring DI = DIP in action**:

```
┌─────────────────────────────────────────────────────────┐
│              DEPENDENCY INVERSION IN SPRING             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  UserService (high-level)                               │
│       │                                                  │
│       │ depends on                                      │
│       ▼                                                  │
│  UserRepository (interface - abstraction)                │
│       ▲                                                  │
│       │ implemented by                                   │
│       │                                                  │
│  JpaUserRepository (low-level)                          │
│                                                         │
│  Spring: Finds implementations, injects into UserService │
└─────────────────────────────────────────────────────────┘
```

---

## Layer 3 — Advanced Engineering Depth

*Internals, trade-offs, composition vs inheritance, clean architecture.*

---

### Composition vs Inheritance

| Use inheritance when | Use composition when |
|----------------------|------------------------|
| Clear "is-a" relationship | "has-a" relationship |
| Need polymorphism (treat as base type) | Want to swap behavior at runtime |
| Sharing implementation | Reusing without tight coupling |
| Subclasses are specializations | Avoiding deep hierarchies |

**Favor composition over inheritance**: Inheritance creates tight coupling; composition allows flexibility.

```java
// Composition: Car HAS-A Engine
public class Car {
    private Engine engine;  // Can swap engines
    private Radio radio;   // Can swap radios
    
    public Car(Engine engine, Radio radio) {
        this.engine = engine;
        this.radio = radio;
    }
}
```

**Spring example**: `UserService` composes `UserRepository`, `EmailService`—it doesn't inherit from them. Composition is the default in Spring's layered design.

#### Decorator Pattern (Composition in Action)

The Decorator pattern uses composition to add behavior without modifying original classes:

```java
// Base interface
public interface Coffee {
    double getCost();
    String getDescription();
}

// Concrete component
public class SimpleCoffee implements Coffee {
    public double getCost() { return 2.0; }
    public String getDescription() { return "Simple coffee"; }
}

// Decorator - wraps another Coffee (composition!)
public class MilkDecorator implements Coffee {
    private Coffee coffee;  // Composed, not extended
    
    public MilkDecorator(Coffee coffee) {
        this.coffee = coffee;
    }
    
    public double getCost() {
        return coffee.getCost() + 0.5;
    }
    
    public String getDescription() {
        return coffee.getDescription() + ", Milk";
    }
}

// Usage: add behavior by wrapping
Coffee drink = new MilkDecorator(new SimpleCoffee());
```

**Spring connection**: `HttpServletRequest` wrappers, `@Transactional` proxy—all use composition-like decoration.

---

### Layered Architecture and Dependency Direction

```
┌─────────────────────────────────────┐
│     Presentation (Controllers)     │  ← HTTP in/out
├─────────────────────────────────────┤
│     Business Logic (Services)      │  ← Core logic
├─────────────────────────────────────┤
│     Data Access (Repositories)     │  ← Database
└─────────────────────────────────────┘

Dependencies point INWARD: Controller → Service → Repository
```

**Rule**: Lower layers don't know about higher layers. Business logic never imports controllers.

---

### Interface-Based Design and Spring

**Why Spring favors interfaces**:

1. **Proxy support**: JDK dynamic proxies require interfaces. Spring uses them for `@Transactional`, AOP.
2. **Testing**: Easy to mock interfaces.
3. **Flexibility**: Swap implementations (e.g., `@Profile("dev")` vs `@Profile("prod")`).

```java
// Program to interface
@Service
public class OrderService {
    private PaymentProcessor processor;  // Interface
    
    public OrderService(PaymentProcessor processor) {
        this.processor = processor;  // Any implementation works
    }
}
```

---

### Edge Cases: LSP Violations to Avoid

| Violation type | Example | Fix |
|----------------|---------|-----|
| Throwing unexpected exceptions | `Penguin.fly()` throws | Don't inherit; use interfaces (e.g., `Flyable`) |
| Weakening preconditions | Subclass allows invalid input | Keep same or stricter validation |
| Strengthening postconditions | Subclass guarantees more than parent | Document clearly; ensure compatibility |
| Changing invariants | Square changes Rectangle's "width/height independent" invariant | Don't use inheritance |

---

### Package Structure (Clean Architecture)

```
com.example.app
├── domain/           # Core business logic
│   ├── model/
│   └── repository/   # Interfaces only
├── application/      # Use cases, orchestration
│   └── service/
├── infrastructure/   # External concerns
│   ├── persistence/  # JPA, JDBC implementations
│   ├── web/          # Controllers
│   └── config/
└── Application.java
```

---

## Layer 4 — Interview Mastery

*Concise Q&A format for interview preparation.*

---

### Q1: Explain all five SOLID principles with one example each.

| Principle | One-Line Definition | Example |
|-----------|---------------------|---------|
| **SRP** | One class, one reason to change | `UserService` = business logic only; `UserRepository` = data only |
| **OCP** | Open for extension, closed for modification | New payment type = new `PaymentProcessor` implementation; no changes to existing code |
| **LSP** | Subtypes substitutable for base types | Any `List` implementation works where `List` is expected |
| **ISP** | Small, focused interfaces | `Readable`, `Writable` instead of fat `FileOperations` |
| **DIP** | Depend on abstractions, not concretions | `UserService` depends on `UserRepository` interface; Spring injects implementation |

---

### Q2: How does Spring enforce Single Responsibility?

**Answer**: Through layered architecture and annotations. `@Controller` = HTTP only; `@Service` = business logic only; `@Repository` = data access only. Each layer has one job. Cross-layer mixing is discouraged by convention and tooling.

---

### Q3: What is Dependency Inversion, and how does Spring implement it?

**Answer**: High-level modules depend on abstractions (interfaces), not low-level implementations. Spring's DI container finds implementations of those interfaces and injects them. `UserService` depends on `UserRepository`; Spring provides `JpaUserRepository`. The inversion: both depend on the interface; Spring wires the details.

---

### Q4: Describe the Rectangle/Square LSP violation.

**Answer**: Square extends Rectangle. Rectangle has independent `setWidth` and `setHeight`. Square overrides both to keep width = height. Code that sets width=5, height=4 and expects area=20 gets 16 with Square. Square breaks the Rectangle contract—it's not substitutable. Fix: Don't use inheritance; use separate `Shape` subclasses or composition.

---

### Q5: What's wrong with fat interfaces?

**Answer**: Fat interfaces force implementers to add methods they don't need. Result: empty implementations or `UnsupportedOperationException`. Violates ISP. Fix: Split into smaller interfaces (`Codable`, `Designable`) so classes implement only what they use.

---

### Q6: When do you use inheritance vs composition?

**Answer**: **Inheritance** when there's a clear "is-a" relationship and you need polymorphism (Dog IS-A Animal). **Composition** when there's "has-a" or you want flexibility to swap behavior. Default to composition; use inheritance sparingly to avoid tight coupling.

---

### Q7: How does Spring use the Open/Closed Principle?

**Answer**: `HandlerInterceptor`, `BeanPostProcessor`, `ApplicationListener`—you *implement* these to add behavior. Spring's code is closed for modification; you extend by adding new implementations. Same for payment processors, report generators, etc.—add new implementations without touching existing ones.

---

### Q8: Design a notification system following SOLID.

**Answer**: 
- **SRP**: `EmailSender`, `SmsSender`, `NotificationService` (orchestrator) each have one job.
- **OCP**: New channel = new class implementing `NotificationSender`; no modification to existing code.
- **LSP**: Any `NotificationSender` implementation is substitutable.
- **ISP**: `NotificationSender` has only `send(Notification)` and maybe `supports(NotificationType)`.
- **DIP**: `NotificationService` depends on `NotificationSender` interface; Spring injects implementations.

**Code sketch**:
```java
public interface NotificationSender {
    void send(Notification n);
    boolean supports(NotificationType type);
}

@Component
public class EmailNotificationSender implements NotificationSender { /* ... */ }

@Component
public class SmsNotificationSender implements NotificationSender { /* ... */ }

@Service
public class NotificationService {
    private List<NotificationSender> senders;  // DIP: injected by Spring
    // findSender(type) -> sender.send(n)
}
```

---

### Q9: How do you identify SRP violations?

**Answer**: (1) Class has multiple reasons to change (DB + email + validation). (2) Can't describe responsibility in one sentence. (3) Class name contains "And" (e.g., `UserServiceAndRepository`). (4) Too many dependencies (5+ injections). (5) Methods group into unrelated concerns. Refactor: Extract each concern into its own class; orchestrate via composition.

---

### Q10: Refactor this DIP violation.

```java
// Before
public class OrderService {
    private FileLogger logger = new FileLogger();
    public void process(Order o) { logger.log("Processing " + o.getId()); }
}
```

**Answer**:
```java
public interface Logger { void log(String msg); }

public class FileLogger implements Logger { /* ... */ }

@Service
public class OrderService {
    private Logger logger;
    public OrderService(Logger logger) { this.logger = logger; }
    public void process(Order o) { logger.log("Processing " + o.getId()); }
}
```

---

## Summary

### OOP Pillars

| Concept | Plain English | Spring Connection |
|---------|---------------|-------------------|
| **Encapsulation** | Hide internals; control access | `@Transactional`, bean internals |
| **Inheritance** | Children get parent traits | `@RestController` extends `@Controller` |
| **Polymorphism** | Same interface, different behavior | `UserRepository` → multiple implementations |
| **Abstraction** | Hide complexity; show contract | Interfaces everywhere |

### SOLID Quick Reference

| Letter | Principle | Spring Example |
|--------|------------|----------------|
| **S** | Single Responsibility | Layered architecture: Controller, Service, Repository |
| **O** | Open/Closed | `HandlerInterceptor`, new implementations without modification |
| **L** | Liskov Substitution | Subclasses must honor parent contract for injection |
| **I** | Interface Segregation | `CrudRepository` → `JpaRepository` hierarchy |
| **D** | Dependency Inversion | **Core of Spring DI**—depend on interfaces |

### Why This Matters for Spring Boot

Spring Framework is an implementation of OOP and SOLID. Dependency Injection is DIP. Layered architecture is SRP. Interface-based design enables OCP and polymorphism. Understanding these principles lets you *design* with Spring, not just *use* it.

**Next**: [Design Patterns](03-Prerequisites-Design-Patterns.md) build on these foundations—Factory, Singleton, Strategy, Proxy, Observer—and show how Spring applies them.

---

[← Back to Index](00-README.md) | [Previous: Java Foundations](01-Prerequisites-Java.md) | [Next: Design Patterns →](03-Prerequisites-Design-Patterns.md)
