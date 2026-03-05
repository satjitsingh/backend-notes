# Chapter 3: Core Design Patterns (Prerequisites)

[← Back to Index](00-README.md) | [Previous: OOP & SOLID](02-Prerequisites-OOP-SOLID.md) | [Next: HTTP & REST →](04-Prerequisites-HTTP-REST.md)

---

## Why This Chapter Matters

Imagine you're learning to cook. Before you can make fancy dishes, you need to know basic techniques: chopping, stirring, boiling. Design patterns are exactly that for writing software—they're proven ways to solve common problems that great programmers have figured out over decades.

**Why does this matter for Spring Boot?** Spring is like a kitchen that's *already built* using these patterns. Every time you write `@Service` or `@Transactional`, you're using code that someone designed using these patterns. If you don't understand the patterns, Spring will feel like magic. If you do understand them, you'll see exactly what's going on—and you'll write better code.

This chapter will take you from "patterns sound scary" to "I can see patterns everywhere." We'll cover ten essential patterns, starting with simple everyday analogies, then building up to how professional developers use them, and finally how Spring uses them under the hood.

---

## Layer 1 — Intuition Builder

Think of this section as a storybook. Each pattern is a character that solves a specific problem. We'll meet them one by one.

---

### Part 1: Singleton — "There Can Only Be One"

**The Everyday Story**

Your school has one principal. No matter which classroom you're in, when someone says "the principal," everyone knows who that is. You don't have Principal #1, Principal #2, Principal #3. There's just one.

In programming, sometimes we need the same idea: **exactly one copy of something**, and everyone uses that same copy.

**The Problem It Solves**

Imagine you're building a connection to a database (a place where your app stores information). Creating a connection is slow and expensive. What if every part of your program created its own connection? You'd have hundreds of connections, your database would get confused, and things would run slowly.

**The Solution**

Singleton says: "Create it once. When anyone needs it, give them that same one."

```
    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
    │  User Page  │     │ Order Page  │     │  Cart Page  │
    └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
           │                   │                   │
           │  "Give me the     │  "Give me the    │  "Give me the
           │   connection"     │   connection"    │   connection"
           └───────────────────┼──────────────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   ONE Database      │
                    │   Connection        │
                    │   (Shared by all)   │
                    └─────────────────────┘
```

**When NOT to Use It**

Don't use Singleton when different parts of your program need their own separate copy—like when each person shopping needs their own shopping cart!

---

### Part 2: Factory — "Don't Build It Yourself, Ask the Factory"

**The Everyday Story**

You want a toy car. You don't go to a factory, get metal, plastic, and wheels, and build it yourself. You go to a toy store and say "I want a red sedan." The store (or the factory behind it) knows how to make it. You get your car. You're happy.

A **factory** in programming is a place that *creates* things for you. You say what you want; it figures out how to make it.

**The Problem It Solves**

Creating objects can be complicated. Imagine: "If they want to pay with a credit card, create a CreditCardProcessor. If they want PayPal, create a PayPalProcessor. If they want Bitcoin..." Soon your code is full of `if` statements and creation logic everywhere.

**The Solution**

A Factory says: "Tell me what you want. I'll create the right thing and give it to you."

```
    You: "I want to pay with PayPal"
    
    ┌─────────────┐         ┌──────────────────┐         ┌─────────────────┐
    │ Payment     │  ───►   │  Payment Factory  │  ───►   │ PayPalProcessor │
    │ Page       │         │  "Creates the     │         │ (Ready to use!) │
    └─────────────┘         │   right thing"    │         └─────────────────┘
                            └──────────────────┘
```

**The Key Idea**

You don't need to know *how* a PayPal processor is built. You just ask for it. The factory handles the messy part.

---

### Part 3: Builder — "Build Your Pizza One Topping at a Time"

**The Everyday Story**

You're ordering a custom pizza. The worker doesn't say "Tell me everything at once: size, crust, sauce, cheese, and all 20 toppings in one breath!" Instead, you go step by step: "Large. Thin crust. Extra cheese. Add pepperoni. Add mushrooms. No olives." Each step is simple. You can stop whenever you're done.

**The Problem It Solves**

Imagine creating a user for a website. You need: first name, last name, email, phone, address, age, birthday, profile picture... That could be 15 different pieces of information! A normal constructor would look like:

```
createUser("John", "Doe", "john@email.com", null, null, 25, null, null, null, ...)
```

Which one is which? What if some are optional? It's a mess.

**The Solution**

Builder says: "Add one piece at a time. Only add what you need."

```
User = start building
  .addFirstName("John")
  .addLastName("Doe")
  .addEmail("john@email.com")
  .addAge(25)
  .finish()
```

Each step is clear. You can skip optional things. No confusion.

---

### Part 4: Strategy — "Different Ways to Do the Same Job"

**The Everyday Story**

You need to get to school. You could walk, bike, take the bus, or get a ride. The *goal* is the same: arrive at school. The *method* changes. On rainy days you might take the bus. On sunny days you might bike. Your mom doesn't need to change the school—she just picks a different way to get you there.

**The Problem It Solves**

Your app needs to send a notification. Should it send an email? A text message? A push notification? The app doesn't want 100 `if` statements: "if email then... if SMS then... if push then..."

**The Solution**

Strategy says: "Pick which way you want to do it. The rest of the code stays the same."

```
                    ┌─────────────────┐
                    │ "Send           │
                    │  notification"  │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
    ┌─────────┐         ┌─────────┐         ┌─────────┐
    │ Email   │         │  SMS    │         │  Push   │
    │Strategy │         │Strategy │         │Strategy │
    └─────────┘         └─────────┘         └─────────┘
```

You can swap strategies without changing the code that *uses* them. Add a new strategy? Just plug it in.

---

### Part 5: Template Method — "Same Recipe, Different Ingredients"

**The Everyday Story**

Making hot chocolate and making tea are almost the same:
1. Boil water
2. Add your main ingredient (cocoa powder OR tea bag)
3. Pour into a cup
4. Add milk if you want

The *steps* are the same. Only step 2 is different. A template method is like a recipe card that says "Do steps 1, 2, 3, 4" but leaves step 2 blank for you to fill in.

**The Problem It Solves**

Imagine processing data from a file. Whether it's an XML file or a JSON file, you always: open the file, read it, process the data (this part is different!), and close the file. Without a template, you'd copy the "open, read, close" code everywhere.

**The Solution**

Template Method says: "Here's the skeleton. You just fill in the part that changes."

```
    Template (Same for everyone):
    
    [1] Open file          ─── Same
    [2] Read file          ─── Same  
    [3] Process data       ─── YOU FILL THIS IN (different for XML vs JSON)
    [4] Close file         ─── Same
```

**Spring Connection**

When you use `JdbcTemplate` in Spring, it handles "connect to database, run your SQL, handle errors, close connection." You only write the SQL and say how to read the results. The boring parts are already done!

---

### Part 6: Proxy — "The Guard at the Door"

**The Everyday Story**

You want to enter a building. There's a security guard at the door. You don't walk straight in. The guard checks your ID first. If you're allowed in, the guard lets you through. The guard doesn't *replace* the building—they *control who gets in*.

A **proxy** is like that guard. It stands in front of something (like a service or an object) and controls access to it.

**The Problem It Solves**

You have a service that saves user data. You want to:
- Start a database transaction before saving
- Actually save the data
- Commit the transaction when done (or undo everything if something goes wrong)

You *could* write that transaction code in every single method. Or... you could have a proxy that wraps your service and adds that behavior automatically.

**The Solution**

Proxy says: "Every time someone calls this method, I'll run first. I'll do my stuff (start transaction), then call the real method, then do more stuff (commit)."

```
    Someone calls: saveUser()
    
         │
         ▼
    ┌─────────────┐
    │   PROXY     │  "I'll check permissions, start transaction..."
    │   (Guard)   │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │ REAL USER   │  "Actually saves the user"
    │   SERVICE   │
    └─────────────┘
           │
           ▼
    ┌─────────────┐
    │   PROXY     │  "...commit transaction, log the action"
    └─────────────┘
```

**Spring Connection**

When you put `@Transactional` on a method, Spring doesn't change your code. It creates a proxy around your class. When anyone calls that method, the proxy runs first—starting the transaction, then calling your real method, then committing. That's why `@Transactional` "just works" without you writing transaction code!

---

### Part 7: Decorator — "Stack the Toppings"

**The Everyday Story**

You have a plain ice cream cone. You add chocolate sauce. Now it's cone + chocolate. You add sprinkles. Now it's cone + chocolate + sprinkles. Each addition *wraps around* what you already have. The cone is still there. You're just adding layers.

**The Problem It Solves**

You have a "send email" function. You want to add logging (record that an email was sent). You also want to add retry logic (if it fails, try again). You could create: EmailSender, LoggingEmailSender, RetryEmailSender, LoggingAndRetryEmailSender... That's too many combinations!

**The Solution**

Decorator says: "Wrap one thing with another. Each wrapper adds one behavior. You can stack them."

```
    Basic Email Sender
           │
           │  wrap with
           ▼
    [Logging Decorator]  →  "Logs every email sent"
           │
           │  wrap with  
           ▼
    [Retry Decorator]    →  "Tries again if it fails"
           │
           ▼
    Final object: Sends email + Logs + Retries
```

**Decorator vs Proxy**

- **Proxy**: Controls *access* (like a guard—who gets in?)
- **Decorator**: Adds *behavior* (like toppings—adds new abilities)
- You can have *multiple* decorators stacked. Usually one proxy.

---

### Part 8: Observer — "Subscribe and Get Notified"

**The Everyday Story**

You subscribe to a YouTube channel. You don't constantly check "Did they upload?" You just wait. When they upload, you get notified. Same with a newspaper—you subscribe, and when there's news, they deliver it. You're an **observer**; the channel/newspaper is the **subject**. When the subject changes, observers get notified.

**The Problem It Solves**

When a user signs up, you need to: send a welcome email, add them to the newsletter, create their profile, update the analytics dashboard. The signup code would need to know about and call all of these. What if you add "send a SMS" later? You'd have to change the signup code again.

**The Solution**

Observer says: "When something happens, announce it. Whoever's listening will react. The thing that happened doesn't need to know who's listening."

```
    User signs up
           │
           ▼
    ┌─────────────────────┐
    │  EVENT: "User       │
    │  Created!"          │
    └──────────┬──────────┘
               │
     ┌─────────┼─────────┬─────────────┐
     ▼         ▼         ▼             ▼
  Email    SMS      Analytics    Newsletter
  Service  Service  Service      Service
  (each reacts on its own—signup code doesn't call them!)
```

**Spring Connection**

Spring has `ApplicationEventPublisher` and `@EventListener`. You publish an event when something happens. Any number of listeners can react. Add a new listener? Just create it—no need to change the code that publishes the event.

---

### Part 9: Adapter — "The Power Adapter for Your Trip"

**The Everyday Story**

Your laptop charger has a US plug. You're in Europe. The wall has a European socket. They don't match! You need an adapter—a small thing that converts one shape to another. The adapter doesn't change your laptop or the wall. It just makes them work together.

**The Problem It Solves**

You have old code that expects interface A. You have new code (or a library) that provides interface B. They're incompatible. You could rewrite everything... or you could create an adapter that makes B look like A.

**The Solution**

Adapter says: "Wrap the incompatible thing and translate its interface to what the client expects."

```
    Your Code expects:        Old Library provides:
    ┌─────────────────┐       ┌─────────────────┐
    │  save(data)     │       │  storeRecord(r)  │
    │  load(id)       │       │  fetchRecord(id) │
    └────────┬────────┘       └────────┬────────┘
             │                         │
             │    ┌─────────────┐      │
             └───►│  ADAPTER    │◄─────┘
                  │ save→store  │
                  │ load→fetch  │
                  └─────────────┘
```

---

### Part 10: Facade — "The Restaurant Waiter"

**The Everyday Story**

A restaurant has: a chef, a cashier, a manager, a dishwasher, someone who orders supplies. It's complex! When you go to a restaurant, you don't talk to each person. You talk to the **waiter**. The waiter talks to the chef for your order, to the cashier when you pay, and so on. The waiter is a **facade**—a simple front that hides the complex system behind it.

**The Problem It Solves**

Using a database directly means: get a connection, create a statement, run the query, read the results, handle errors, close everything. That's a lot of steps! And it's easy to forget one (like closing the connection) and cause bugs.

**The Solution**

Facade says: "Here's one simple thing you can call. I'll handle all the complexity behind the scenes."

```
    You want to: "Get user by ID"
    
    ┌─────────────────────────────────────────────────────┐
    │  FACADE: JdbcTemplate.query("SELECT...", id)         │
    └─────────────────────────────────────────────────────┘
                              │
         Behind the scenes (you don't see):
         • Get connection from pool
         • Create PreparedStatement
         • Execute query
         • Map results to User object
         • Handle exceptions
         • Return connection to pool
```

**Spring Connection**

`JdbcTemplate` is a facade over raw JDBC. You write one simple line; it handles connection management, error handling, and resource cleanup. Same idea with `RestTemplate` for HTTP calls.

---

## Layer 2 — Professional Developer

Now we translate these intuitions into real code. For each pattern, we'll see **why** it matters, **how** to implement it, and **how Spring uses it**.

---

### Singleton Pattern

#### Why It Matters

- **Global access**: One instance, reachable from anywhere
- **Expensive creation**: Database connections, thread pools—create once, reuse
- **Shared state**: Configuration, caches—everyone reads the same data

#### Without Pattern (The Messy Way)

```java
// Every class creates its own connection—wasteful and dangerous!
public class UserRepository {
    private Connection conn = DriverManager.getConnection(...);  // New connection!
}
public class OrderRepository {
    private Connection conn = DriverManager.getConnection(...);  // Another one!
}
// Soon you have 50 connections. Database says "no more!"
```

#### With Pattern (The Clean Way)

```java
public class DatabaseConnection {
    // Static = one copy for the whole program
    private static final DatabaseConnection instance = new DatabaseConnection();
    
    // Private = no one else can say "new DatabaseConnection()"
    private DatabaseConnection() {
        // Initialize connection once
    }
    
    // The only way to get it
    public static DatabaseConnection getInstance() {
        return instance;
    }
}

// Usage everywhere:
Connection conn = DatabaseConnection.getInstance().getConnection();
```

#### Thread-Safe Variants

**Lazy (created when first needed)** — but NOT thread-safe:

```java
public static DatabaseConnection getInstance() {
    if (instance == null) {           // Thread 1 and 2 both see null
        instance = new DatabaseConnection();  // Both create! Two instances!
    }
    return instance;
}
```

**Double-Checked Locking** — thread-safe and efficient:

```java
private static volatile DatabaseConnection instance;  // volatile = visible to all threads

public static DatabaseConnection getInstance() {
    if (instance == null) {                    // Fast path: already created
        synchronized (DatabaseConnection.class) {  // Only one thread at a time
            if (instance == null) {            // Check again inside lock
                instance = new DatabaseConnection();
            }
        }
    }
    return instance;
}
```

**Enum Singleton** — recommended in Java (thread-safe, serialization-safe, reflection-safe):

```java
public enum DatabaseConnection {
    INSTANCE;
    
    public void connect() {
        // Connection logic
    }
}
// Usage: DatabaseConnection.INSTANCE.connect();
```

#### How Spring Uses Singleton

```java
@Service  // Default scope = singleton. Spring creates ONE instance.
public class UserService {
    // This same instance is injected everywhere UserService is needed
}

// Explicit (same as default):
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ConfigService { }
```

**Key difference**: Java Singleton = one per JVM. Spring Singleton = one per Spring container (usually one per application).

---

### Factory Pattern

#### Why It Matters

- **Decouples creation from use**: Caller doesn't know how objects are constructed
- **Centralizes creation logic**: Change how objects are created in one place
- **Supports multiple types**: Same interface, different implementations

#### Without Pattern (Conditional Chaos)

```java
public void processPayment(String type, double amount) {
    PaymentProcessor processor;
    if (type.equals("CREDIT_CARD")) {
        processor = new CreditCardProcessor();  // Creation logic mixed with business logic
    } else if (type.equals("PAYPAL")) {
        processor = new PayPalProcessor();
    } else if (type.equals("CRYPTO")) {
        processor = new CryptoProcessor();
    }
    processor.process(amount);
}
```

#### With Pattern (Simple Factory)

```java
// Factory: knows how to create. Caller: just asks for what it needs.
public class PaymentFactory {
    public static PaymentProcessor create(String type) {
        return switch (type) {
            case "CREDIT_CARD" -> new CreditCardProcessor();
            case "PAYPAL" -> new PayPalProcessor();
            case "CRYPTO" -> new CryptoProcessor();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}

// Usage: clean!
PaymentProcessor p = PaymentFactory.create("PAYPAL");
p.process(amount);
```

#### How Spring Uses Factory

**BeanFactory** and **ApplicationContext** are the core. You declare beans; Spring creates them:

```java
@Configuration
public class AppConfig {
    @Bean  // Spring's factory creates this when needed
    public UserService userService() {
        return new UserService(userRepository());
    }
}

// Later, anywhere:
@Autowired
UserService userService;  // Spring's factory gave you the bean
```

---

### Builder Pattern

#### Why It Matters

- **Readability**: `User.builder().name("John").age(25).build()` vs constructor with 10 params
- **Optional parameters**: Skip what you don't need
- **Validation**: Validate in `build()` before creating object

#### Without Pattern (Constructor Nightmare)

```java
new User("John", "Doe", 25, "john@x.com", null, null, null, true, false);
// What is the 8th parameter? Nobody knows.
```

#### With Pattern

```java
public class User {
    private final String name;
    private final int age;
    private final String email;
    
    private User(Builder b) {
        this.name = b.name;
        this.age = b.age;
        this.email = b.email;
    }
    
    public static class Builder {
        private String name;
        private int age;
        private String email;
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder age(int age) { this.age = age; return this; }
        public Builder email(String email) { this.email = email; return this; }
        
        public User build() {
            if (name == null) throw new IllegalStateException("Name required");
            return new User(this);
        }
    }
}

// Usage: crystal clear
User u = new User.Builder()
    .name("John")
    .age(25)
    .email("john@x.com")
    .build();
```

#### How Spring Uses Builder

```java
// ResponseEntity - building HTTP responses
return ResponseEntity.ok()
    .header("X-Custom", "value")
    .body(user);

// UriComponentsBuilder - building URLs
UriComponentsBuilder.fromUriString("https://api.example.com")
    .path("/users")
    .queryParam("page", 1)
    .build();

// Lombok @Builder - generates builder automatically
@Builder
public class User { String name; int age; }
User u = User.builder().name("John").age(25).build();
```

---

### Strategy Pattern

#### Why It Matters

- **Eliminates if/else chains**: No more `if (type == A) ... else if (type == B)`
- **Open/Closed**: Add new strategies without modifying existing code
- **Runtime switching**: Change strategy without restarting

#### Without Pattern (Conditional Spaghetti)

```java
public void sendNotification(String type, String msg, String to) {
    if (type.equals("EMAIL")) {
        // 20 lines of email logic
    } else if (type.equals("SMS")) {
        // 20 lines of SMS logic
    } else if (type.equals("PUSH")) {
        // 20 lines of push logic
    }
}
```

#### With Pattern

```java
// Strategy interface
public interface NotificationStrategy {
    void send(String message, String recipient);
}

// Concrete strategies
public class EmailStrategy implements NotificationStrategy {
    @Override
    public void send(String message, String recipient) {
        // Email logic
    }
}

public class SmsStrategy implements NotificationStrategy {
    @Override
    public void send(String message, String recipient) {
        // SMS logic
    }
}

// Context - holds current strategy
public class NotificationService {
    private NotificationStrategy strategy;
    
    public void setStrategy(NotificationStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void notify(String message, String recipient) {
        strategy.send(message, recipient);  // Delegate to strategy
    }
}
```

#### How Spring Uses Strategy

- **ResourceLoader**: Different strategies for `classpath:`, `file:`, `http:` URLs
- **PlatformTransactionManager**: JDBC vs JPA vs Hibernate—same interface, different implementations
- **Your own services**: Inject `List<NotificationStrategy>`, pick by name/type at runtime

```java
@Service
public class NotificationService {
    private final Map<String, NotificationStrategy> strategies;
    
    public NotificationService(List<NotificationStrategy> strategies) {
        this.strategies = strategies.stream()
            .collect(Collectors.toMap(s -> s.getClass().getSimpleName(), Function.identity()));
    }
    
    public void notify(String type, String msg, String to) {
        strategies.get(type).send(msg, to);
    }
}
```

---

### Template Method Pattern

#### Why It Matters

- **Reuse common steps**: Connect, fetch, process, disconnect—write once
- **Customize only what varies**: Subclass fills in the unique part
- **Prevent duplication**: DRY principle

#### Structure

```java
public abstract class DataProcessor {
    // Template method - defines the skeleton. final = subclasses can't change order
    public final void process() {
        connect();      // Same for all
        fetchData();    // Same for all
        processData();  // SUBCLASS IMPLEMENTS - the varying part
        saveData();     // Same for all
    }
    
    private void connect() { /* common */ }
    private void fetchData() { /* common */ }
    protected abstract void processData();  // Hook for subclasses
    private void saveData() { /* common */ }
}

public class XmlProcessor extends DataProcessor {
    @Override
    protected void processData() {
        // XML-specific processing
    }
}
```

#### How Spring Uses Template Method

**JdbcTemplate**: You provide SQL and row mapping. Spring handles connection, statement, exception translation, cleanup:

```java
@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;
    
    public User findById(Long id) {
        return jdbc.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            (rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name")),
            id
        );
    }
}
```

**RestTemplate, TransactionTemplate, JmsTemplate**: Same idea—template handles boilerplate, you provide the custom part.

---

### Proxy Pattern

#### Why It Matters

- **Add behavior without changing code**: Logging, transactions, caching
- **Lazy loading**: Create expensive object only when first used
- **Access control**: Check permissions before allowing the call

#### Static Proxy (Manual)

```java
public interface UserService {
    void saveUser(User user);
}

public class UserServiceImpl implements UserService {
    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }
}

public class UserServiceProxy implements UserService {
    private final UserService real = new UserServiceImpl();
    
    @Override
    public void saveUser(User user) {
        System.out.println("Before save");
        if (hasPermission()) {
            real.saveUser(user);
        }
        System.out.println("After save");
    }
}
```

#### Dynamic Proxy (What Spring Uses)

Spring creates proxies at runtime—no manual proxy class:

```java
UserService proxy = (UserService) Proxy.newProxyInstance(
    UserService.class.getClassLoader(),
    new Class[]{UserService.class},
    (p, method, args) -> {
        System.out.println("Before " + method.getName());
        Object result = method.invoke(realService, args);
        System.out.println("After " + method.getName());
        return result;
    }
);
```

#### How Spring Uses Proxy — CRITICAL

**@Transactional**:

```java
@Service
public class UserService {
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
}
```

**What happens**: Spring creates a proxy. When you call `saveUser()`, the proxy:
1. Starts a transaction
2. Calls your real `saveUser()`
3. Commits (or rolls back on exception)

**Gotcha — self-invocation bypasses proxy**:

```java
@Transactional
public void methodA() {
    this.methodB();  // Bypasses proxy! methodB's @Transactional won't run!
}

@Transactional
public void methodB() { }
```

**Fix**: Self-inject or move `methodB` to another `@Service`.

---

### Decorator Pattern

#### Why It Matters

- **Add behavior dynamically**: Wrap objects with additional functionality
- **Avoid class explosion**: Coffee, CoffeeWithMilk, CoffeeWithSugar, CoffeeWithBoth... vs Coffee + MilkDecorator + SugarDecorator
- **Single responsibility**: Each decorator does one thing

#### Structure

```java
public interface Coffee {
    String getDescription();
    double getCost();
}

public class SimpleCoffee implements Coffee {
    public String getDescription() { return "Coffee"; }
    public double getCost() { return 2.0; }
}

public abstract class CoffeeDecorator implements Coffee {
    protected Coffee coffee;
    public CoffeeDecorator(Coffee c) { this.coffee = c; }
}

public class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee c) { super(c); }
    public String getDescription() { return coffee.getDescription() + ", Milk"; }
    public double getCost() { return coffee.getCost() + 0.5; }
}

// Usage: stack decorators
Coffee c = new MilkDecorator(new SugarDecorator(new SimpleCoffee()));
```

#### How Spring Uses Decorator

- **HttpServletRequestWrapper**: Wraps request to cache body for multiple reads
- **AOP**: Aspects decorate your beans with logging, security—add behavior without modifying original class

---

### Observer Pattern

#### Why It Matters

- **Loose coupling**: Publisher doesn't know who's listening
- **Extensibility**: Add new listeners without changing publisher
- **Event-driven**: React to things that happen

#### Basic Implementation

```java
public interface Observer {
    void update(Event event);
}

public class EventSource {
    private List<Observer> observers = new ArrayList<>();
    
    public void addObserver(Observer o) { observers.add(o); }
    
    public void notifyObservers(Event e) {
        observers.forEach(o -> o.update(e));
    }
}
```

#### How Spring Uses Observer

```java
// 1. Define event
public class UserCreatedEvent extends ApplicationEvent {
    private final User user;
    public UserCreatedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}

// 2. Publish
@Service
public class UserService {
    private final ApplicationEventPublisher publisher;
    
    public void createUser(User user) {
        userRepository.save(user);
        publisher.publishEvent(new UserCreatedEvent(this, user));
    }
}

// 3. Listen
@Component
public class EmailService {
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        sendWelcomeEmail(event.getUser());
    }
}
```

**Benefits**: Async support (`@Async`), ordering (`@Order`), conditional listeners.

---

### Adapter Pattern

#### Why It Matters

- **Integrate incompatible interfaces**: Make old code work with new code
- **Third-party libraries**: Their API doesn't match yours—wrap it
- **Can't modify source**: Adapter translates without changing original

#### Structure

```java
// What your code expects
public interface MediaPlayer {
    void play(String file);
}

// What you have (incompatible)
public class VlcPlayer {
    public void playVlc(String file) { }
}

// Adapter: makes VlcPlayer look like MediaPlayer
public class VlcAdapter implements MediaPlayer {
    private VlcPlayer vlc = new VlcPlayer();
    
    @Override
    public void play(String file) {
        vlc.playVlc(file);  // Translate the call
    }
}
```

#### How Spring Uses Adapter

- **HandlerAdapter** in Spring MVC: Different controller types (@Controller, HttpRequestHandler) → unified handling
- **Spring Security**: OAuth2, LDAP, etc. adapted to `Authentication` interface

---

### Facade Pattern

#### Why It Matters

- **Simplify complex subsystems**: One simple interface over many classes
- **Reduce dependencies**: Client depends on facade, not 20 internal classes
- **Easier to use**: Hide the complexity

#### Structure

```java
// Complex subsystem
class CPU { void start() { } }
class Memory { void load() { } }
class Disk { void read() { } }

// Facade
public class ComputerFacade {
    private CPU cpu = new CPU();
    private Memory memory = new Memory();
    private Disk disk = new Disk();
    
    public void start() {
        cpu.start();
        memory.load();
        disk.read();
    }
}

// Client: one call instead of three
computer.start();
```

#### How Spring Uses Facade

- **JdbcTemplate**: Facade over Connection, Statement, ResultSet, exception handling
- **RestTemplate**: Facade over HTTP client complexity
- **Spring Security**: Facade over authentication/authorization details

---

## Layer 3 — Advanced Engineering Depth

### Spring's Internal Pattern Usage

#### IoC Container = Factory + Registry + Singleton

```
┌─────────────────────────────────────────────────────────┐
│              ApplicationContext (IoC Container)         │
│                                                         │
│  Factory: Creates beans via reflection @Bean methods    │
│  Registry: Stores beans by name & type (Map)           │
│  Singleton: One instance per bean (default scope)      │
│  DI: Wires dependencies automatically                  │
└─────────────────────────────────────────────────────────┘
```

#### AOP = Proxy + (Conceptually) Decorator

```java
@Aspect
@Component
public class LoggingAspect {
    @Around("@annotation(Loggable)")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        // Proxy: intercepts the call
        // Decorator: adds logging behavior
        logBefore(pjp);
        Object result = pjp.proceed();
        logAfter(pjp);
        return result;
    }
}
```

#### Template Classes Structure

```
┌─────────────────────────────────────────────────┐
│  JdbcTemplate / RestTemplate / TransactionTemplate │
├─────────────────────────────────────────────────┤
│  1. Setup (connection, transaction, etc.)       │
│  2. Execute callback (YOUR code)                │
│  3. Exception translation                       │
│  4. Cleanup (close, commit/rollback)           │
└─────────────────────────────────────────────────┘
```

### JDK Dynamic Proxy vs CGLIB

| Aspect | JDK Dynamic Proxy | CGLIB |
|--------|-------------------|-------|
| **Requires** | Interface | Class (no interface) |
| **Mechanism** | Reflection at runtime | Bytecode generation |
| **Final methods** | N/A | Cannot proxy |
| **Performance** | Slower | Faster |
| **Spring uses when** | Bean implements interface | No interface, or forced |

**Spring's logic**: If your `@Service` implements an interface → JDK Proxy. If not → CGLIB (creates subclass).

### Pattern Combinations in Spring

1. **@Configuration**: Singleton + Factory (config class is singleton, produces beans)
2. **@Transactional service**: Singleton + Proxy (one instance, wrapped with transaction proxy)
3. **JdbcTemplate**: Facade + Template Method (simplifies JDBC, defines algorithm skeleton)
4. **Event system**: Observer + loose coupling (publish; multiple listeners react)

---

## Layer 4 — Interview Mastery

### Q1: What design patterns does Spring use?

**Answer**: Singleton (bean scope), Factory (BeanFactory, ApplicationContext), Proxy (AOP, @Transactional, @Cacheable), Template Method (JdbcTemplate, RestTemplate), Strategy (ResourceLoader, TransactionManager), Observer (ApplicationEventPublisher), Adapter (HandlerAdapter), Decorator (AOP aspects), Builder (ResponseEntity.builder()), Facade (JdbcTemplate). Most critical: **Proxy** (for AOP) and **Factory** (for IoC).

---

### Q2: How does @Transactional work?

**Answer**: Spring creates a proxy of your service. When a @Transactional method is called, the proxy intercepts: starts a transaction, calls your method, commits or rolls back. **Gotcha**: Calling `this.otherTransactionalMethod()` from within the same class bypasses the proxy—use self-injection or a separate service.

---

### Q3: JDK Proxy vs CGLIB?

**Answer**: JDK Proxy requires an interface; uses reflection. CGLIB proxies classes (no interface); uses bytecode generation; faster but can't proxy final methods. Spring uses JDK Proxy when the bean implements an interface, CGLIB otherwise.

---

### Q4: Thread-safe Singleton in Java?

**Answer**: Best: **Enum singleton** (`public enum Db { INSTANCE; }`)—thread-safe, serialization-safe, reflection-safe. Alternative: double-checked locking with `volatile` on the instance field.

---

### Q5: Strategy vs Template Method?

**Answer**: **Strategy**: Entire algorithm varies; swap implementations at runtime. **Template Method**: Algorithm structure is fixed; only specific steps vary; subclasses fill in the hooks.

---

### Q6: How does Spring's event system work?

**Answer**: Observer pattern. Define event (extends ApplicationEvent), publish via `ApplicationEventPublisher.publishEvent()`, listen with `@EventListener` on a method. Supports @Async, @Order, and conditional listeners.

---

### Q7: What is BeanFactory?

**Answer**: Factory + Registry + Singleton. Creates beans, stores them by name/type, returns single instance (default scope). ApplicationContext extends it with events, i18n, etc.

---

### Q8: When would you use Adapter vs Facade?

**Answer**: **Adapter**: Two incompatible interfaces—make one look like the other. **Facade**: Complex subsystem—provide one simple interface to simplify usage.

---

## Summary

| Pattern | One-Liner | Spring Usage |
|---------|-----------|--------------|
| **Singleton** | One instance, shared everywhere | Default bean scope |
| **Factory** | Creates objects; callers don't know how | BeanFactory, ApplicationContext |
| **Builder** | Build objects step-by-step | ResponseEntity.builder(), Lombok @Builder |
| **Strategy** | Swap algorithms at runtime | ResourceLoader, TransactionManager |
| **Template Method** | Same skeleton, customize steps | JdbcTemplate, RestTemplate |
| **Proxy** | Intercept calls, add behavior | @Transactional, @Cacheable, AOP |
| **Decorator** | Wrap to add behavior | AOP aspects, RequestWrapper |
| **Observer** | Notify listeners when something happens | ApplicationEventPublisher, @EventListener |
| **Adapter** | Make incompatible interfaces work together | HandlerAdapter in MVC |
| **Facade** | Simple interface over complex subsystem | JdbcTemplate, RestTemplate |

**Key insight**: Spring is not magic—it's design patterns applied consistently. Understanding patterns = understanding Spring.

---

[← Back to Index](00-README.md) | [Previous: OOP & SOLID](02-Prerequisites-OOP-SOLID.md) | [Next: HTTP & REST →](04-Prerequisites-HTTP-REST.md)
