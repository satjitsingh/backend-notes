# Chapter 14: Spring Boot Architecture

[← Back to Index](00-README.md) | [Previous: Spring vs Spring Boot](13-Spring-vs-SpringBoot.md) | [Next: Auto-Configuration →](15-Auto-Configuration.md)

---

## Why This Chapter Matters

Every time you run a Spring Boot app, **a lot happens behind the scenes** — even before your first line of code runs. Understanding this process is the difference between a developer who can fix startup problems in minutes versus one who stares at confusing error messages for hours.

In interviews, "What happens when a Spring Boot application starts?" is one of the most common questions. A junior might say "it starts up." A senior walks through each step and explains why it matters.

**Without understanding Spring Boot architecture:**
- You'll struggle to debug startup failures (a "BeanNotFoundException" — but why?)
- You'll misuse lifecycle hooks and application events
- You'll create configurations that slow down startup
- You'll fail interview questions about internals
- You won't understand why your `@Bean` isn't being found

**With proper architecture understanding:**
- You'll debug any startup issue confidently (you know where to look)
- You'll customize Spring Boot effectively (events, hooks, initializers)
- You'll optimize application startup time
- You'll understand how auto-configuration "magically" works
- You'll excel in architecture discussions and interviews

---

## Layer 1 — Intuition Builder

*Imagine you've never written code. These parts explain Spring Boot using everyday ideas.*

### Part 1: What Is "Starting an App" Anyway?

Imagine you turn on a video game. You press the power button, the screen lights up, music plays, and after a few seconds you see the main menu. A lot happened in those few seconds: the game loaded its files, set up its world, and got ready for you to play.

**Starting a Spring Boot app is the same idea.** You run one line of code, and in one or two seconds, your app has prepared its "world" and is ready to accept requests. But what really happens during those seconds?

### Part 2: The Restaurant Analogy — Opening for the Day

Think of your Spring Boot app as a **restaurant**. When you "run" the app, you're like the manager opening the restaurant each morning. Here's what happens, step by step:

| Step | Restaurant World | Spring Boot World |
|------|------------------|-------------------|
| 1 | The manager arrives at the door | Your `main()` method runs |
| 2 | The manager reads today's schedule and special instructions | Spring reads your configuration files (like `application.properties`) |
| 3 | The manager unlocks the kitchen and turns on the lights | Spring creates a special "container" (a place to hold everything the app needs) |
| 4 | Staff arrive and go to their posts — chef to kitchen, waiter to dining room, cashier to the register | Spring creates and positions all your "components" — controllers, services, repositories |
| 5 | The manager opens the front door and flips the "Open" sign | Spring starts the built-in web server (like a tiny website waiting for visitors) |
| 6 | The manager announces "We're open!" | Spring fires a "ready" signal — your app is now accepting requests |

**If any step fails, the restaurant doesn't open.** If the chef doesn't show up (a bean can't be created), the manager cancels opening. Similarly, if something goes wrong during Spring Boot startup, the app stops and shows you an error message — usually pointing to which step failed.

### Part 3: What Is "Context"? (Plain English First)

You don't need to know the word "context" yet — think of it as **the restaurant building itself**: the kitchen, the dining room, the storage room. It's the physical space where everything lives and works together.

In Spring Boot, the **ApplicationContext** is that "space." It's a special container that holds all the objects your app needs (we call them "beans" later). When someone asks "where does Spring keep my UserService?" the answer is: inside the ApplicationContext.

*Now you can use the word: the ApplicationContext is the IoC container — the place where Spring creates, stores, and wires together all your application components.*

### Part 4: What Is a "Bean"? (Plain English First)

A **bean** is just an object that Spring creates and manages for you. If your app has a `UserService`, Spring creates one instance of it, keeps it in the Context, and gives it to any other part of the app that needs it.

Think of beans as the **staff members** in the restaurant analogy. The chef is a bean. The waiter is a bean. The cashier is a bean. Spring hires them (creates them), assigns them their stations (injects them where needed), and makes sure they're ready when the restaurant opens.

### Part 5: The Bootstrap Sequence — Seven Steps

When you call `SpringApplication.run()`, Spring Boot goes through a fixed sequence. You can think of it as a checklist:

```
┌─────────────────────────────────────────────────────────────────────┐
│  WHAT HAPPENS WHEN YOU RUN SPRING BOOT (The 7 Steps)                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Step 1: main() is called                                             │
│          → Like the manager arriving at the restaurant                │
│                                                                       │
│  Step 2: SpringApplication object is created                          │
│          → The "orchestrator" that will run the whole show            │
│                                                                       │
│  Step 3: Environment is prepared                                      │
│          → Read properties, profiles, environment variables           │
│          → Like reading today's schedule and special instructions     │
│                                                                       │
│  Step 4: ApplicationContext is created                                │
│          → The "kitchen" — the space where all beans will live        │
│                                                                       │
│  Step 5: Beans are loaded and auto-configuration runs                 │
│          → All your @Component, @Service, @Controller, @Repository     │
│            are created and wired together                             │
│          → Spring also auto-creates things like DataSource, etc.      │
│          → Like staff arriving and taking their positions             │
│                                                                       │
│  Step 6: Embedded server is started                                   │
│          → Tomcat (or Jetty/Undertow) starts listening on a port      │
│          → Like opening the front door                                │
│                                                                       │
│  Step 7: Application is ready!                                        │
│          → ApplicationReadyEvent is fired                             │
│          → "Started MyApplication in 2.3 seconds" appears in logs     │
│          → Like flipping the "Open" sign                              │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

**Why this helps you:** If your app fails at Step 5, you'll see errors like "BeanCreationException." If it fails at Step 6, you might see "port already in use." Knowing the steps tells you where to look.

### Part 6: Where Do I Put My Files? (Project Structure)

Spring Boot expects your project to follow a certain folder structure. Think of it like a house: the bedroom is for sleeping, the kitchen is for cooking. Similarly:

| Folder | What Goes Here | Plain English |
|--------|----------------|---------------|
| `src/main/java/` | Your Java code (controllers, services, repositories) | The "brain" of your app |
| `src/main/resources/` | Configuration files, static files (CSS, JS), templates | The "settings and blueprints" |
| `src/main/resources/application.properties` | App configuration (port, database URL, etc.) | The "instruction manual" |
| `src/test/java/` | Test code | The "quality check" code |
| Root (`pom.xml` or `build.gradle`) | Dependency list, build settings | The "shopping list" of libraries |

**Typical structure:**

```
my-app/
├── src/main/java/
│   └── com/example/myapp/
│       ├── MyApplication.java       ← The main class (the "manager")
│       ├── controller/              ← Handles HTTP requests (the "waiters")
│       │   └── UserController.java
│       ├── service/                  ← Business logic (the "chefs")
│       │   └── UserService.java
│       ├── repository/               ← Talks to the database (the "inventory staff")
│       │   └── UserRepository.java
│       └── config/                   ← Extra configuration
│           └── AppConfig.java
├── src/main/resources/
│   ├── application.properties       ← Main config file
│   ├── application-dev.properties   ← Dev-specific config
│   ├── application-prod.properties  ← Prod-specific config
│   ├── static/                       ← CSS, JS, images
│   └── templates/                    ← HTML templates (e.g., Thymeleaf)
├── src/test/java/                   ← Tests
└── pom.xml                           ← Maven dependencies
```

### Part 7: The Main Class — The One That Starts Everything

Every Spring Boot app has a **main class**. It's the one with `public static void main(String[] args)`. This is like the manager's key — it's the single entry point that kicks everything off.

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

**In plain English:**
- `@SpringBootApplication` — A label that tells Spring: "This is the main class; scan from here and configure everything."
- `SpringApplication.run(...)` — The one line that actually starts the whole restaurant-opening process.
- `MyApplication.class` — Tells Spring where to start looking for your components (from this class's package downward).
- `args` — Any command-line arguments you passed (like "use profile dev").

### Part 8: The Layered Architecture — Who Does What?

Imagine the restaurant again. The **waiter** takes orders from customers but doesn't cook. The **chef** cooks but doesn't serve. The **inventory staff** manages the pantry but doesn't cook or serve. Everyone has a clear job.

Spring Boot apps work the same way. We split code into **layers**:

```
        ┌─────────────────────────────────────┐
        │  CONTROLLER (The Waiters)             │  ← Handles HTTP: receives requests,
        │  "I'll take your order!"              │    returns responses
        └────────────────┬────────────────────┘
                         │ calls
                         ▼
        ┌─────────────────────────────────────┐
        │  SERVICE (The Chefs)                  │  ← Business logic: rules, calculations,
        │  "I'll prepare your meal!"            │    orchestrating work
        └────────────────┬────────────────────┘
                         │ calls
                         ▼
        ┌─────────────────────────────────────┐
        │  REPOSITORY (The Inventory Staff)     │  ← Data access: save, load,
        │  "I'll fetch from the pantry!"       │    delete from database
        └─────────────────────────────────────┘
```

- **Controller** — Talks to the outside world (HTTP). Doesn't contain business logic.
- **Service** — Contains the "thinking" — business rules, validation, orchestration. Doesn't know about HTTP.
- **Repository** — Talks to the database. Doesn't contain business logic.

Each layer has one job. That makes the app easier to understand, test, and change.

---

## Layer 2 — Professional Developer

*Now we add technical detail. Every concept explains WHY before HOW, and all code has comments.*

### @SpringBootApplication — What It Really Is

`@SpringBootApplication` is a **composite annotation**. It's shorthand for three annotations combined:

```java
// @SpringBootApplication is equivalent to:
@SpringBootConfiguration    // 1. Marks this as a configuration class
@EnableAutoConfiguration    // 2. Turns on auto-configuration magic
@ComponentScan              // 3. Scans for @Component, @Service, @Controller, @Repository
```

**Why they're combined:** Convention over configuration. Most apps need all three, so Spring Boot provides one annotation instead of three.

#### 1. @SpringBootConfiguration

```java
// What it does: Tells Spring this class can define @Bean methods
// It's essentially @Configuration with a Spring Boot–specific name
// WHY: Spring needs to know which classes are "config" classes
@SpringBootConfiguration
public class MyApplication {
    // This class can now define @Bean methods
}
```

#### 2. @EnableAutoConfiguration

```java
// What it does: Enables Spring Boot's auto-configuration mechanism
// It scans your classpath (dependencies) and conditionally creates beans
// WHY: So you don't have to manually configure DataSource, JPA, Security, etc.
//
// Examples:
// - If spring-boot-starter-web is on classpath → configures web server
// - If spring-boot-starter-data-jpa is on classpath → configures JPA
// - If spring-boot-starter-security is on classpath → configures security
@EnableAutoConfiguration
```

#### 3. @ComponentScan

```java
// What it does: Scans for classes annotated with @Component, @Service, @Repository, @Controller
// By default, scans from the package of the annotated class downward
// WHY: So Spring can find and create your beans automatically
@SpringBootApplication  // Default: scans com.example.myapp and all subpackages
public class MyApplication { }

// Custom scanning (if your components live in other packages):
@SpringBootApplication(scanBasePackages = {"com.example", "com.other"})
public class MyApplication { }
```

### SpringApplication.run() — The Bootstrap Process (Detailed)

When you call `SpringApplication.run(MyApplication.class, args)`, this is the flow:

```
main()
  │
  ▼
SpringApplication.run()
  │
  ├─► Create SpringApplication instance (determines app type: servlet/reactive/none)
  │
  ├─► Load ApplicationContextInitializers (run BEFORE context refresh)
  ├─► Load ApplicationListeners (react to startup events)
  │
  ├─► Create ApplicationContext (the IoC container)
  │     - Servlet app → AnnotationConfigServletWebServerApplicationContext
  │     - Reactive app → AnnotationConfigReactiveWebServerApplicationContext
  │     - None → AnnotationConfigApplicationContext
  │
  ├─► Prepare Environment
  │     - Load application.properties / application.yml
  │     - Process profiles (spring.profiles.active)
  │     - Merge property sources (CLI args > system props > env vars > application.properties)
  │
  ├─► Refresh Context  ← "The magic happens here"
  │     - Process @Configuration classes
  │     - Process @ComponentScan (find and register beans)
  │     - Process auto-configuration (conditional beans)
  │     - Create singleton beans
  │     - Wire dependencies (@Autowired, constructor injection)
  │     - Call @PostConstruct methods
  │
  ├─► Run ApplicationRunner and CommandLineRunner beans
  ├─► Start embedded server (Tomcat/Jetty/Undertow)
  │
  └─► Publish ApplicationReadyEvent
        └─► "Started MyApplication in X seconds"
```

### Project Structure Conventions — Table

| Path | Purpose | Conventions |
|------|---------|-------------|
| `src/main/java/<base-package>/` | Application code | Same package as main class or subpackages |
| `src/main/java/.../MyApplication.java` | Main class | Must have `@SpringBootApplication` |
| `src/main/java/.../controller/` | REST endpoints | `*Controller.java`, `@RestController` |
| `src/main/java/.../service/` | Business logic | `*Service.java`, `@Service` |
| `src/main/java/.../repository/` | Data access | `*Repository.java`, `@Repository` |
| `src/main/java/.../config/` | Configuration | `@Configuration`, `@Bean` methods |
| `src/main/resources/application.properties` | Default config | Key-value pairs |
| `src/main/resources/application-{profile}.properties` | Profile-specific | e.g., `application-dev.properties` |
| `src/main/resources/static/` | Static assets | CSS, JS, images |
| `src/main/resources/templates/` | View templates | Thymeleaf, etc. |

### Layered Architecture — Code Examples

#### Controller Layer

**Responsibility:** Handle HTTP requests and responses. Validate input. Delegate to service. Transform domain to DTO.

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // WHY constructor injection: Immutable, testable, makes dependencies explicit
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users/{id}
     * WHY ResponseEntity: Allows control over status code, headers, body
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return userService.getUserById(id)
            .map(ResponseEntity::ok)           // 200 OK with body
            .orElse(ResponseEntity.notFound().build());  // 404
    }
}
```

**❌ Avoid:** Controllers accessing repositories directly or containing business logic.

#### Service Layer

**Responsibility:** Business logic. Orchestration. Transactions. Transform domain ↔ DTO.

```java
@Service
@Transactional  // WHY: All public methods run in a transaction by default
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
            .map(this::toDTO);  // Transform entity to DTO
    }

    public UserDTO createUser(CreateUserRequest request) {
        User user = new User(request.getName(), request.getEmail());
        User saved = userRepository.save(user);
        emailService.sendWelcomeEmail(saved.getEmail());  // Orchestration
        return toDTO(saved);
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }
}
```

#### Repository Layer

**Responsibility:** Data access. Abstract database operations. Define queries.

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data derives query from method name
    Optional<User> findByEmail(String email);

    // Custom query
    @Query("SELECT u FROM User u WHERE u.active = true")
    List<User> findActiveUsers();
}
```

#### Complete Flow (ASCII)

```
HTTP GET /api/users/1
         │
         ▼
┌─────────────────────┐
│ UserController      │  Validates path, calls service
│ getUser(1)          │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ UserService         │  getById(1), maps to DTO
│ getUserById(1)      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ UserRepository      │  findById(1) → SQL
│ findById(1)         │
└──────────┬──────────┘
           │
           ▼
    [ Database ]
           │
           ▼
    Response flows back up: Repository → Service → Controller → HTTP 200 + JSON
```

### Component Scanning — How It Works

```java
// Component scanning finds classes with these annotations:
// @Component, @Service, @Repository, @Controller, @RestController, @Configuration

// Default: Scans from MyApplication's package (com.example.myapp) downward
@SpringBootApplication
public class MyApplication { }

// To include another package:
@SpringBootApplication(scanBasePackages = {"com.example.myapp", "com.shared.lib"})
public class MyApplication { }

// To exclude a specific auto-configuration:
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MyApplication { }
```

---

## Layer 3 — Advanced Engineering Depth

*Deep internals, performance, edge cases, and customization.*

### Application Events — Lifecycle Order

Spring Boot publishes events during startup. Knowing the order is critical for choosing the right hook:

```
1. ApplicationStartingEvent        ← Very first; no context yet
2. ApplicationEnvironmentPreparedEvent
3. ApplicationContextInitializedEvent
4. ApplicationPreparedEvent / ContextRefreshedEvent
5. ApplicationStartedEvent        ← Context refreshed; server may not be ready
6. ApplicationReadyEvent          ← Server is listening; safe for traffic
   (on failure)
7. ApplicationFailedEvent
```

**ApplicationStartedEvent vs ApplicationReadyEvent**

| Aspect | ApplicationStartedEvent | ApplicationReadyEvent |
|--------|-------------------------|------------------------|
| **When** | After context refresh, runners called | After embedded server is listening |
| **Server** | May still be starting | Fully ready |
| **Use for** | Internal setup | External readiness, service discovery, health checks |
| **Safe to send traffic?** | No | Yes |

### Context Hierarchy

Spring Boot can use parent-child contexts (e.g., Spring Boot Admin, multi-module apps):

```
Parent Context
  ├── Shared beans (DataSource, common config)
  └── Child Context(s)
        ├── Can access parent beans
        └── Can override with @Primary
```

**Bean resolution:** Child first, then parent. If not found, `NoSuchBeanDefinitionException`.

### Customizing Bootstrap

#### SpringApplicationBuilder — Fluent Configuration

```java
public static void main(String[] args) {
    new SpringApplicationBuilder()
        .sources(MyApplication.class)
        .profiles("dev", "local")
        .properties("custom.key=value")
        .bannerMode(Banner.Mode.OFF)
        .logStartupInfo(false)
        .web(WebApplicationType.SERVLET)
        .run(args);
}
```

#### ApplicationContextInitializer — Before Refresh

```java
public class CustomInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().setActiveProfiles("custom");
        // Add property sources, register bean definitions, etc.
    }
}

// Register via:
SpringApplication app = new SpringApplication(MyApplication.class);
app.addInitializers(new CustomInitializer());
app.run(args);
```

#### ApplicationListener — React to Events

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // Run before other listeners
public class CustomListener implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Application is fully ready; safe to register with load balancer, etc.
    }
}

// Modern approach: @EventListener
@Component
public class ModernListener {
    @EventListener
    @Order(1)
    public void handleReady(ApplicationReadyEvent event) {
        // Same as above
    }
}
```

### Startup Performance — Edge Cases

- **Lazy initialization:** `spring.main.lazy-initialization=true` defers bean creation until first use. Faster startup, slower first request.
- **Exclude unused auto-configurations:** Use `@SpringBootApplication(exclude = {...})` to skip configs you don't need.
- **Conditional beans:** Auto-configuration uses `@ConditionalOn*`; only matching beans are created, reducing overhead.
- **Context refresh:** Most startup time is in refresh (component scan + bean creation). Minimize component count and avoid heavy `@PostConstruct` work.

### Failure Analysis

Spring Boot's `FailureAnalyzer` provides friendlier error messages. You can add a custom one:

```java
@Component
public class CustomFailureAnalyzer implements FailureAnalyzer {
    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (failure instanceof PortInUseException) {
            return new FailureAnalysis(
                "Port 8080 is already in use.",
                "Stop the other process or set server.port to a different value.",
                failure
            );
        }
        return null;
    }
}
```

---

## Layer 4 — Interview Mastery

### Q1: What happens when a Spring Boot application starts?

**Short answer:**  
`main()` runs → `SpringApplication` is created → environment is prepared → `ApplicationContext` is created → context is refreshed (beans created, auto-configuration applied) → embedded server starts → `ApplicationReadyEvent` is published.

**Medium answer:**  
1. `main()` calls `SpringApplication.run(MyApplication.class, args)`.  
2. Spring determines application type (servlet/reactive/none).  
3. Environment is loaded (properties, profiles).  
4. `ApplicationContext` is created.  
5. Context refresh: `@Configuration` processed, component scan runs, auto-configuration applied, beans created and wired.  
6. `ApplicationRunner` and `CommandLineRunner` run.  
7. Embedded server starts.  
8. `ApplicationReadyEvent` is published.

**Advanced:**  
Mention events: `ApplicationStartingEvent` → `ApplicationEnvironmentPreparedEvent` → `ApplicationContextInitializedEvent` → `ContextRefreshedEvent` → `ApplicationStartedEvent` → `ApplicationReadyEvent`. Describe what happens during refresh (BeanFactory post-processing, bean instantiation, dependency injection).

---

### Q2: Explain @SpringBootApplication

**Short answer:**  
It combines `@SpringBootConfiguration`, `@EnableAutoConfiguration`, and `@ComponentScan`.

**Medium answer:**  
- **@SpringBootConfiguration:** Marks the class as configuration (like `@Configuration`).  
- **@EnableAutoConfiguration:** Enables auto-configuration; uses `AutoConfigurationImportSelector` to load configs from `META-INF/spring.factories` based on classpath.  
- **@ComponentScan:** Scans for `@Component`, `@Service`, `@Repository`, `@Controller` from the annotated class's package downward.

**Advanced:**  
Say it's a meta-annotation. Mention `@ConditionalOnClass`, `@ConditionalOnMissingBean`, and that you can override with `exclude` or `scanBasePackages`.

---

### Q3: ApplicationStartedEvent vs ApplicationReadyEvent?

**Short answer:**  
`ApplicationStartedEvent` fires after context refresh; the server may not be ready. `ApplicationReadyEvent` fires when the server is listening and the app is fully ready.

**Medium answer:**  
Use `ApplicationStartedEvent` for internal setup. Use `ApplicationReadyEvent` for external readiness (service discovery, health checks, sending traffic).

---

### Q4: How do you customize the startup process?

**Short answer:**  
`ApplicationContextInitializer` (before refresh), `ApplicationListener` / `@EventListener` (at events), `ApplicationRunner` / `CommandLineRunner` (after refresh), `SpringApplicationBuilder` (fluent config), `@PostConstruct` (bean init).

**Medium answer:**  
- **Initializer:** Modify context/environment before refresh.  
- **Listener:** React to specific events (e.g., `ApplicationReadyEvent`).  
- **Runner:** Run logic after refresh, with access to beans.  
- **Builder:** Set profiles, properties, web type, banner, etc.  
- **@PostConstruct:** Initialize a bean after creation and injection.

---

### Q5: Explain layered architecture in Spring Boot

**Short answer:**  
Controller (HTTP) → Service (business logic) → Repository (data access). Each layer has one responsibility.

**Medium answer:**  
- **Controller:** HTTP handling, validation, DTO mapping. No business logic.  
- **Service:** Business rules, transactions, orchestration. No HTTP or DB details.  
- **Repository:** Data access, queries. No business logic.

**Benefits:** Separation of concerns, easier testing, maintainability, flexibility (e.g., changing DB without touching services).

**Advanced:**  
Mention anti-patterns: controller accessing repository, business logic in controller, anemic domain model. Mention when to add layers (e.g., integration, caching).

---

## Summary

**Key points:**

1. **Bootstrap:** `main()` → environment → context creation → refresh (scan, auto-configure, create beans) → server start → ready.
2. **@SpringBootApplication:** Composite of `@SpringBootConfiguration`, `@EnableAutoConfiguration`, `@ComponentScan`.
3. **Events:** Know the order; use `ApplicationReadyEvent` when you need the server to be ready.
4. **Layered architecture:** Controller (HTTP) → Service (logic) → Repository (data). Clear responsibilities.
5. **Customization:** Initializers, listeners, runners, builder, `@PostConstruct` — each for a specific phase.

**Next steps:**
- Chapter 15: Auto-Configuration
- Chapter 16: Starters and Application Context
- Chapter 17: Bean Lifecycle

---

[← Back to Index](00-README.md) | [Previous: Spring vs Spring Boot](13-Spring-vs-SpringBoot.md) | [Next: Auto-Configuration →](15-Auto-Configuration.md)
