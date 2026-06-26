# Chapter 12: IoC & Dependency Injection Deep Dive

[← Back to Index](00-README.md) | [Previous: Spring Introduction](11-Spring-Introduction.md) | [Next: Spring vs Spring Boot →](13-Spring-vs-SpringBoot.md)

---

## Why This Chapter Matters

**Inversion of Control (IoC)** and **Dependency Injection (DI)** are not just features — they are the design philosophy that makes Spring work. Every annotation you use (`@Service`, `@Autowired`, `@Component`) exists because of IoC and DI.

If you don't deeply understand this chapter, you'll never truly understand Spring. You'll copy code that works without knowing *why* it works. And when it breaks, you'll be lost.

**What this chapter covers:**
- The problem that IoC solves (and why manual wiring becomes a nightmare)
- The IoC container concept — how Spring acts like a "restaurant manager"
- How Spring creates, wires, and manages beans
- Stereotype annotations (`@Component`, `@Service`, `@Repository`, `@Controller`)
- Injection types: constructor (preferred), setter, field (discouraged)
- Resolving ambiguity: `@Qualifier`, `@Primary`, `@Lazy`
- Circular dependencies and how to fix them
- Bean scopes: singleton, prototype, request, session
- Interface-based design with dependency injection

**Without understanding IoC/DI deeply:**
- You'll struggle with "NoSuchBeanDefinitionException" and "UnsatisfiedDependencyException"
- You'll create circular dependencies and not know how to fix them
- You'll use `@Autowired` on fields (bad practice) instead of constructors (good practice)
- You'll fail interview questions about Spring internals

**With proper IoC/DI understanding:**
- You'll design clean, testable, maintainable architectures
- You'll debug any Spring wiring issue confidently
- You'll understand WHY Spring promotes loose coupling
- You'll know exactly what happens when Spring sees `@Autowired`
- You'll excel in Spring interviews at any level

---

## Layer 1 — Intuition Builder

### Part 1: Imagine You're Building a Restaurant (The Problem)

Imagine you're opening a restaurant. You need:

- **A chef** who cooks the food
- **A waiter** who takes orders and serves food
- **A dishwasher** who cleans plates
- **Ingredients** that come from different farms

**The Old Way (The Chef Does Everything):**

The chef wakes up at 3 AM, drives to the farm to get tomatoes, buys flour from the mill, raises chickens in the backyard, and makes plates from clay. Only *then* can the chef start cooking.

- Want different tomatoes? The chef must find a new farm, learn new routes, and change the whole routine.
- Want to test a new recipe? The chef must grow new ingredients first.
- Want to open a second restaurant? The chef must repeat *everything* from scratch.

**The New Way (A Manager Handles All That):**

The chef arrives at the restaurant. The chef's station already has tomatoes, flour, and chicken — delivered by the manager. The chef doesn't know *where* they came from. The chef just cooks.

- Want different tomatoes? The manager switches the supplier. The chef's job stays the same.
- Want to test a new recipe? The manager brings test ingredients. The chef doesn't know the difference.
- Want to open a second restaurant? The manager hires staff and sets up supplies there too.

**Spring is that manager.** Your code is the chef. Spring brings what you need (dependencies) to you, so you can focus on your job (business logic).

---

### Part 2: What "Dependency" Means (In Plain English)

A **dependency** is simply: *something you need to do your job*.

- A chef **depends on** ingredients.
- A waiter **depends on** the chef (to get food to serve).
- A calculator **depends on** numbers to add.

In programming:
- `OrderService` depends on `PaymentService` (to charge the customer).
- `PaymentService` might depend on `DatabaseConnection` (to save the payment record).
- `UserController` depends on `UserService` (to get user data).

**The problem without Spring:**
You (the developer) create every object. When `OrderService` needs `PaymentService`, you write:

```text
"Create a PaymentService.
 But PaymentService needs a DatabaseConnection.
 So create that first.
 DatabaseConnection needs a password — get it from a config file.
 Now create PaymentService with that connection.
 Finally, create OrderService and give it the PaymentService."
```

If you have 50 classes, each needing 3–5 other classes, you end up with hundreds of lines just to "wire" things together. And if one thing changes (like the database password), you might have to change it in many places.

---

### Part 3: "Inversion of Control" — Who's in Charge?

**Without Spring (You're in control):**
- You create objects.
- You decide when they are created.
- You pass them to each other.
- You manage their lifecycle (when they start, when they end).

**With Spring (Spring is in control):**
- Spring creates objects.
- Spring decides when they are created.
- Spring passes them to each other.
- Spring manages their lifecycle.

Control has been **inverted** — it went from you to the framework. That's **Inversion of Control (IoC)**.

---

### Part 4: "Dependency Injection" — Someone Gives You What You Need

**Dependency Injection** means: *someone from the outside gives you the things you need, instead of you going to get them yourself*.

- Without DI: The chef drives to the farm. (You create and fetch dependencies.)
- With DI: The manager delivers ingredients to the chef. (Spring gives you dependencies.)

In code:
- Without DI: `OrderService` creates its own `PaymentService` inside its constructor.
- With DI: `OrderService` receives `PaymentService` as a parameter — Spring "injects" it.

---

### Part 5: The Container — Spring's "Back Office"

Spring uses something called a **container** (officially: IoC container or ApplicationContext). Think of it as the restaurant's back office:

1. **It knows who works there** — It reads your classes marked with `@Service`, `@Component`, etc.
2. **It hires everyone** — It creates one instance of each service/component (or more, depending on rules).
3. **It assigns who works with whom** — It figures out that `OrderService` needs `PaymentService` and connects them.
4. **It manages the schedule** — It decides when things are created and when they are destroyed.

Here's a simple picture of the flow:

```text
    ┌─────────────────────────────────────────────────────────┐
    │                  SPRING CONTAINER                       │
    │              (The "Back Office" / Manager)              │
    └─────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
    ┌──────────┐        ┌──────────┐        ┌──────────┐
    │  Bean A  │◄──────►│  Bean B  │◄──────►│  Bean C  │
    │(Service) │        │(Service) │        │(Repository)│
    └──────────┘        └──────────┘        └──────────┘
          │                   │                   │
          └───────────────────┼───────────────────┘
                              │
                    Spring wires them together
                    (Dependency Injection)
```

---

### Part 6: What Is a "Bean"? (Simple Definition)

A **bean** is any object that Spring creates and manages. That's it.

- You don't write `new OrderService()`.
- Spring creates `OrderService` and keeps it.
- When another class needs `OrderService`, Spring gives that same instance (or a new one, depending on scope).

**Bean vs regular object:**
- Regular object: You create it with `new`, you own it, you manage it.
- Bean: Spring creates it, Spring owns it, Spring gives it to you when you need it.

---

### Part 7: Manual Wiring Nightmare vs Spring DI

**Manual wiring (the nightmare):**

```text
To create OrderService, you must:
  1. Create ConnectionPool
  2. Create DataSource with ConnectionPool
  3. Create UserRepository with DataSource
  4. Create UserService with UserRepository
  5. Create PaymentService (which also needs... 5 more things)
  6. Create OrderService with UserService and PaymentService

Change the database URL? Update 12 different files.
Add a new service? Touch 8 existing classes.
Test one class? You must create 20 fake dependencies first.
```

**With Spring DI:**

```text
OrderService says: "I need UserService and PaymentService."
Spring says: "Here they are." 
Done.

Change the database URL? Update one config.
Add a new service? Just add a new class with @Service.
Test one class? Pass fake UserService and PaymentService. Easy.
```

---

### Part 8: The Big Idea — Interface-Based Design

Here's a powerful idea: *program to an interface, not to a concrete class*.

- Instead of: "I need StripePaymentService" (one specific payment company)
- Say: "I need something that can process payments" (any implementation)

Why?  
If you switch from Stripe to PayPal, you only change *which* implementation Spring injects. Your `OrderService` code stays the same. Spring can give you a real `PaymentService` in production and a fake one in tests — your code doesn't care.

That's what **interface-based design with DI** means: design around contracts (interfaces), and let Spring plug in the right implementation.

---

## Layer 2 — Professional Developer

### The IoC Container in Practice

Spring's IoC container (`ApplicationContext`) performs these steps:

1. **Read configuration** — Scans for `@Component`, `@Service`, `@Repository`, `@Controller`, and `@Configuration` classes.
2. **Create beans** — Instantiates classes and manages creation order.
3. **Wire dependencies** — Injects dependencies via constructor, setter, or field.
4. **Manage lifecycle** — Calls `@PostConstruct` and `@PreDestroy`.
5. **Provide beans** — Returns singleton or prototype instances when requested.

---

### Creating Beans: Stereotype Annotations

Spring uses **stereotypes** to classify and register components. All are specializations of `@Component`.

| Annotation      | Layer           | Use Case                                      |
|-----------------|-----------------|-----------------------------------------------|
| `@Component`    | Generic         | Any Spring-managed component                  |
| `@Service`      | Business logic  | Services, transaction boundaries             |
| `@Repository`   | Data access     | Repositories, exception translation           |
| `@Controller`   | Web (MVC)       | Controllers that return views                 |
| `@RestController` | Web (REST)    | Controllers that return JSON/XML              |

**Example with comments:**

```java
// Generic component — use when no other stereotype fits
@Component
public class EmailValidator { }

// Business logic — typically used with @Transactional
@Service
public class OrderService { }

// Data access — translates DB exceptions to DataAccessException
@Repository
public class UserRepository { }

// Web layer — handles HTTP and returns views
@Controller
public class HomeController { }

// REST API — returns JSON, not HTML views
@RestController
public class UserRestController { }
```

**Component scanning:** `@SpringBootApplication` includes `@ComponentScan`, so Spring scans the package and sub-packages for these annotations.

---

### Creating Beans: @Bean for Third-Party and Complex Cases

Use `@Bean` in a `@Configuration` class when:

- Configuring third-party classes (you cannot add `@Component`)
- Creating beans conditionally
- Multiple beans of the same type
- Complex creation logic

```java
@Configuration
public class DatabaseConfig {

    // Third-party class: we can't add @Component to HikariDataSource
    // So we use @Bean to create and register it
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost/mydb");
        config.setUsername("user");
        config.setPassword("password");
        return new HikariDataSource(config);
    }

    // Multiple RestTemplate configs with different settings
    @Bean("internalRestTemplate")
    public RestTemplate internalRestTemplate() {
        RestTemplate template = new RestTemplate();
        template.setConnectTimeout(2000);
        return template;
    }

    @Bean("externalRestTemplate")
    public RestTemplate externalRestTemplate() {
        RestTemplate template = new RestTemplate();
        template.setConnectTimeout(5000);
        return template;
    }
}
```

---

### Dependency Injection: Constructor vs Setter vs Field

| Type             | Preferred? | Use When                                   |
|------------------|------------|--------------------------------------------|
| Constructor      | Yes        | Required dependencies, most beans         |
| Setter          | Sometimes  | Optional or changeable dependencies       |
| Field            | No         | Avoid in production; demos only           |

#### 1. Constructor Injection (Preferred)

```java
@Service
public class OrderService {
    // final = immutable, set once in constructor
    private final UserService userService;
    private final PaymentService paymentService;

    // Spring injects these automatically (no @Autowired needed since Spring 4.3+)
    public OrderService(UserService userService, PaymentService paymentService) {
        this.userService = userService;
        this.paymentService = paymentService;
    }
}
```

**Why prefer constructor injection:**
- Dependencies can be `final` → immutability.
- All required dependencies are explicit; object cannot exist without them.
- Easy to test: pass mocks in the constructor.
- Fail-fast: missing dependency causes startup failure.

#### 2. Setter Injection

```java
@Service
public class NotificationService {
    private final EmailService emailService;  // Required
    private SmsService smsService;             // Optional

    // Required dependency via constructor
    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    // Optional dependency — required = false means Spring won't fail if no SmsService bean
    @Autowired(required = false)
    public void setSmsService(SmsService smsService) {
        this.smsService = smsService;
    }

    public void send(Notification n) {
        emailService.send(n.getEmail());
        if (smsService != null) {
            smsService.send(n.getPhone());
        }
    }
}
```

Use setter injection for optional or changeable dependencies.

#### 3. Field Injection (Avoid)

```java
@Service
public class BadService {
    @Autowired
    private UserService userService;  // Don't do this in production
}
```

**Problems:**
- Cannot make the field `final` → mutable, not thread-safe by design.
- Hidden dependencies: constructor is empty.
- Hard to test without Spring context or reflection.
- Violates encapsulation (Spring uses reflection to set the field).

Prefer constructor injection instead.

---

### @Autowired: How Spring Resolves Dependencies

Spring resolves dependencies in this order:

1. **By type** — Find beans of the required type.
2. **By @Qualifier** — If specified, match by bean name.
3. **By @Primary** — If multiple candidates, use the one marked `@Primary`.
4. **By parameter name** — Match constructor parameter name to bean name.
5. **Error** — If still ambiguous → `NoUniqueBeanDefinitionException`.

#### Multiple Implementations: @Primary

```java
public interface PaymentService {
    void processPayment(double amount);
}

@Service
@Primary  // Default when someone asks for PaymentService
public class StripePaymentService implements PaymentService {
    @Override
    public void processPayment(double amount) { /* Stripe logic */ }
}

@Service
public class PayPalPaymentService implements PaymentService {
    @Override
    public void processPayment(double amount) { /* PayPal logic */ }
}

@Service
public class OrderService {
    private final PaymentService paymentService;

    // Gets StripePaymentService because of @Primary
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

#### Multiple Implementations: @Qualifier

```java
@Service("stripe")
public class StripePaymentService implements PaymentService { }

@Service("paypal")
public class PayPalPaymentService implements PaymentService { }

@Service
public class OrderService {
    private final PaymentService paymentService;

    // Explicitly request "stripe" bean
    public OrderService(@Qualifier("stripe") PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

---

### @Lazy: Delayed Bean Creation

```java
@Service
public class HeavyService {
    // Created only when first used, not at startup
}

@Service
public class OrderService {
    // HeavyService is created only when OrderService first uses it
    public OrderService(@Lazy HeavyService heavyService) {
        this.heavyService = heavyService;
    }
    private final HeavyService heavyService;
}
```

Use `@Lazy` to defer creation of expensive or rarely used beans.

---

### Bean Scopes

| Scope       | Instances        | Typical use                              |
|-------------|------------------|------------------------------------------|
| `singleton` | One per container| Services, repositories, utilities       |
| `prototype` | New each request | Mutable, non-shared objects              |
| `request`   | One per HTTP req | Request-specific data                    |
| `session`   | One per session  | User preferences, cart                  |
| `application` | One per app    | Application-wide config                 |

#### Singleton (Default)

```java
@Service  // Default = singleton
public class OrderService {
    // One shared instance for the whole application
}
```

#### Prototype

```java
@Component
@Scope("prototype")
public class OrderProcessor {
    // New instance every time it's requested
}

// Get a new instance when needed
@Service
public class OrderService {
    private final ObjectProvider<OrderProcessor> processorProvider;

    public OrderService(ObjectProvider<OrderProcessor> processorProvider) {
        this.processorProvider = processorProvider;
    }

    public void process() {
        OrderProcessor p = processorProvider.getObject(); // New instance each time
        p.process();
    }
}
```

#### Request and Session (Web)

```java
@Component
@Scope("request")
public class RequestData {
    // New instance per HTTP request
}

@Component
@Scope("session")
public class UserPreferences {
    // One instance per user session (e.g. shopping cart)
}
```

---

### Interface-Based Design with DI

Design around interfaces so Spring can inject different implementations:

```java
// Interface = contract
public interface PaymentService {
    void processPayment(double amount);
}

// Production implementation
@Service
@Primary
public class StripePaymentService implements PaymentService {
    @Override
    public void processPayment(double amount) {
        // Real Stripe API call
    }
}

// Consumer depends on interface, not implementation
@Service
public class OrderService {
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;  // Could be Stripe, PayPal, or mock
    }
}
```

**Benefits:**
- Easy to swap implementations (e.g. Stripe ↔ PayPal).
- Simple testing: inject a mock `PaymentService`.
- Loose coupling between `OrderService` and concrete payment providers.

---

### Manual Wiring vs Spring DI — Code Comparison

**Manual wiring (problematic):**

```java
public class OrderService {
    private final UserService userService;
    private final PaymentService paymentService;

    public OrderService() {
        // We own the entire dependency chain — brittle and hard to test
        DataSource ds = new HikariDataSource(/* ... */);
        UserRepository repo = new UserRepository(ds);
        this.userService = new UserService(repo);
        this.paymentService = new StripePaymentService(/* ... */);
    }
}
```

**Spring DI (clean):**

```java
@Service
public class OrderService {
    private final UserService userService;
    private final PaymentService paymentService;

    public OrderService(UserService userService, PaymentService paymentService) {
        this.userService = userService;      // Injected by Spring
        this.paymentService = paymentService;
    }
}
```

---

## Layer 3 — Advanced Engineering Depth

### How the Container Works Internally

#### BeanDefinition

Spring does not create beans directly from your classes. It first builds **BeanDefinition** objects: metadata that describes how to create and configure each bean.

A `BeanDefinition` typically includes:
- Class name
- Scope (singleton, prototype, etc.)
- Constructor arguments
- Property values
- Initialization/destruction methods
- Dependencies

For `@Component` classes, Spring's component scanner produces a `BeanDefinition` for each discovered component.

#### BeanFactory vs ApplicationContext

```
BeanFactory (basic)
    ├── getBean(name)
    ├── getBean(Class)
    ├── containsBean(name)
    └── Lazy bean creation

ApplicationContext (extends BeanFactory)
    ├── All BeanFactory methods
    ├── Eager singleton creation
    ├── Event publishing
    ├── Message resolution (i18n)
    ├── AOP integration
    └── ApplicationContextAware
```

**ApplicationContext** is the usual choice; `BeanFactory` is rarely used directly.

---

### Bean Lifecycle

```
1. Instantiation (constructor)
2. Populate properties (DI)
3. BeanNameAware.setBeanName()
4. BeanFactoryAware.setBeanFactory()
5. ApplicationContextAware.setApplicationContext()
6. BeanPostProcessor.postProcessBeforeInitialization()
7. @PostConstruct
8. InitializingBean.afterPropertiesSet()
9. Custom init-method
10. BeanPostProcessor.postProcessAfterInitialization()  ← AOP proxies created here
    ... Bean ready ...
11. @PreDestroy
12. DisposableBean.destroy()
13. Custom destroy-method
```

**Example lifecycle usage:**

```java
@Component
public class OrderService implements BeanNameAware, InitializingBean, DisposableBean {
    private final PaymentService paymentService;
    private String beanName;

    public OrderService(PaymentService paymentService) {
        System.out.println("1. Constructor");
        this.paymentService = paymentService;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("7. @PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("8. InitializingBean.afterPropertiesSet()");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("11. @PreDestroy");
    }

    @Override
    public void destroy() {
        System.out.println("12. DisposableBean.destroy()");
    }
}
```

---

### Circular Dependencies

**Problem:**
- `ServiceA` needs `ServiceB`
- `ServiceB` needs `ServiceA`
- Neither can be fully constructed before the other

#### Constructor injection (fails)

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
// BeanCurrentlyInCreationException — Spring cannot resolve this
```

Spring cannot inject a partially-constructed bean into a constructor. Constructor cycles are not supported.

#### How Spring resolves (setter/field injection)

Spring uses a **three-level cache** for singletons:

1. **Singleton objects** — Fully initialized beans
2. **Early reference cache** — Partially constructed beans
3. **Singleton factories** — Factories that create beans

Process:
1. Start creating `ServiceA`
2. `ServiceA` needs `ServiceB` → start creating `ServiceB`
3. `ServiceB` needs `ServiceA` → Spring puts partially-constructed `ServiceA` in early cache and injects it
4. `ServiceB` finishes → `ServiceA` finishes

**Setter injection allows this:**

```java
@Service
public class ServiceA {
    private ServiceB serviceB;

    @Autowired
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

@Service
public class ServiceB {
    private ServiceA serviceA;

    @Autowired
    public void setServiceA(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}
```

#### Solutions

**1. Refactor (preferred)**  
Break the cycle by extracting shared logic into a third component:

```java
@Service
public class CommonService { }

@Service
public class ServiceA {
    private final CommonService commonService;
    public ServiceA(CommonService commonService) { this.commonService = commonService; }
}

@Service
public class ServiceB {
    private final CommonService commonService;
    public ServiceB(CommonService commonService) { this.commonService = commonService; }
}
```

**2. @Lazy**

```java
@Service
public class ServiceA {
    private final ServiceB serviceB;

    public ServiceA(@Lazy ServiceB serviceB) {
        this.serviceB = serviceB;  // Proxy injected; real bean created when first used
    }
}
```

**3. Setter injection**  
Accept mutable dependencies and use setter injection as shown above (less ideal than refactoring).

---

### Proxy-Based DI and AOP

Spring uses proxies for:
- `@Transactional`
- `@Cacheable`
- Other AOP aspects
- `@Lazy` resolution

**JDK dynamic proxy (interfaces):**  
Spring creates a proxy that implements the same interface and wraps the target.

**CGLIB proxy (classes):**  
Spring creates a subclass and overrides methods. Fails for `final` classes or `final` methods.

**Self-invocation and proxies:**

```java
@Service
public class OrderService {
    public void processOrder() {
        sendEmail();  // Direct call — NO proxy — @Transactional on sendEmail() is ignored!
    }

    @Transactional
    public void sendEmail() { }
}
```

**Fix:** Call through the proxy (inject self or use `ApplicationContext.getBean()`):

```java
@Service
public class OrderService {
    private final OrderService self;

    public OrderService(OrderService self) {
        this.self = self;  // Spring injects the proxy
    }

    public void processOrder() {
        self.sendEmail();  // Goes through proxy — @Transactional works
    }

    @Transactional
    public void sendEmail() { }
}
```

---

### Edge Cases and Performance

- **Prototype scope**: More instances → more memory. Use only when necessary.
- **@Lazy**: Reduces startup time but defers first-use cost.
- **Circular dependencies**: Prefer refactoring; setter/`@Lazy` are workarounds.
- **Large dependency graphs**: Constructor injection and good layering keep startup predictable.

---

## Layer 4 — Interview Mastery

### Q1. What is IoC and DI?

**IoC (Inversion of Control):** Control of object creation and wiring moves from application code to the framework. You don't create and connect objects; the framework does.

**DI (Dependency Injection):** A way to implement IoC. Dependencies are provided from outside (injected) instead of being created inside the class.

---

### Q2. What is the Spring IoC container?

The IoC container (`ApplicationContext`) is responsible for:
- Creating beans from configuration and stereotypes
- Resolving and injecting dependencies
- Managing bean lifecycle (creation, init, destruction)
- Providing beans when requested

---

### Q3. BeanFactory vs ApplicationContext?

| Aspect            | BeanFactory        | ApplicationContext   |
|-------------------|--------------------|----------------------|
| Bean creation     | Lazy               | Eager (singletons)   |
| Events            | No                 | Yes                  |
| i18n              | No                 | Yes                  |
| AOP               | No                 | Yes                  |

Use **ApplicationContext** in almost all cases.

---

### Q4. Why prefer constructor injection?

1. Enables `final` fields → immutability
2. Makes required dependencies explicit
3. Easy to test with mocks
4. Fail-fast if a dependency is missing
5. Naturally thread-safe when dependencies are immutable

---

### Q5. When to use @Component vs @Bean?

**@Component** (and stereotypes): Your own classes, simple creation.

**@Bean**: Third-party classes, conditional beans, multiple beans of same type, complex creation logic.

---

### Q6. How does @Autowired resolve dependencies?

1. By type
2. By `@Qualifier` (bean name)
3. By `@Primary`
4. By parameter name
5. Error if still ambiguous

---

### Q7. How do you fix circular dependencies?

1. **Refactor** — Extract shared logic; remove the cycle
2. **@Lazy** — Inject a proxy; resolve later
3. **Setter injection** — Works for singletons due to three-level cache (less preferred)

Constructor-only cycles cannot be resolved.

---

### Q8. Explain bean scopes.

| Scope       | Behavior                          | Use case              |
|-------------|-----------------------------------|------------------------|
| singleton   | One instance per container        | Services, repositories |
| prototype   | New instance each request         | Mutable objects       |
| request     | One per HTTP request              | Request-scoped data   |
| session     | One per HTTP session              | User-specific data    |
| application | One per ServletContext            | App-wide config       |

---

### Q9. What is BeanDefinition?

`BeanDefinition` is metadata describing how to create a bean: class, scope, constructor args, properties, init/destroy methods. Spring builds these during component scanning and configuration processing, then uses them to instantiate and configure beans.

---

### Q10. How does Spring create proxies?

- **Interfaces:** JDK dynamic proxy implementing the interface
- **Classes:** CGLIB subclass
- Proxies are created in `BeanPostProcessor.postProcessAfterInitialization()`

Cannot proxy: `final` classes, `final` methods, `private` methods.

---

## Summary

- **IoC** = Framework controls object creation and wiring.
- **DI** = Dependencies are injected from outside instead of created inside.
- **Container** = Creates beans, wires dependencies, manages lifecycle.
- **Beans** = Objects managed by Spring.
- **Constructor injection** = Preferred for required dependencies.
- **@Component/@Service/@Repository/@Controller** = Stereotypes for different layers.
- **@Qualifier** = Choose a specific bean; **@Primary** = Default when multiple exist.
- **@Lazy** = Defer bean creation.
- **Circular dependencies** = Prefer refactoring; setter and `@Lazy` are fallbacks.
- **Scopes** = singleton (default), prototype, request, session.
- **Interface-based design** = Depend on interfaces; let Spring inject implementations.

---

[← Back to Index](00-README.md) | [Previous: Spring Introduction](11-Spring-Introduction.md) | [Next: Spring vs Spring Boot →](13-Spring-vs-SpringBoot.md)
