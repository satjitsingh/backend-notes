# Chapter 17: Bean Lifecycle Mastery

[← Back to Index](00-README.md) | [Previous: Starters & Context](16-Starters-Context.md) | [Next: Configuration & Profiles →](18-Configuration-Profiles.md)

---

## Why This Chapter Matters

When you add `@Service` or `@Component` to a class, Spring creates it for you. But **when** does that happen? What if your object needs to connect to a database first? What happens when the app stops — does Spring close that connection?

The **bean lifecycle** answers these questions. It’s the step‑by‑step sequence Spring uses to create, set up, use, and eventually destroy your objects (beans).

**Why beginners need this:**
- You’ll see `@PostConstruct` and `@PreDestroy` in real projects and need to understand them
- You’ll avoid bugs caused by beans not being ready when you expect
- You’ll fix startup issues related to bean creation order
- You’ll manage resources (connections, caches, files) that need startup setup and shutdown cleanup

**Without understanding bean lifecycle:**
- Dependencies may be null when used in constructors
- Resources may leak if not cleaned up on shutdown
- Startup ordering issues are harder to debug
- Lifecycle callbacks and scopes are used incorrectly
- Race conditions can occur with partially initialized beans

**With proper understanding:**
- You use the right lifecycle hooks (post‑construction, pre‑destruction)
- You manage connections, caches, and external resources safely
- You can debug startup problems using initialization order
- You choose the right scope (singleton vs prototype vs request) for each bean
- You can clearly explain the Spring bean lifecycle in interviews

---

## Layer 1 — Intuition Builder

### Part 1: What Is a Bean? (No Jargon First)

Imagine a factory that makes robots. Each robot is built from a blueprint (your class). The factory doesn’t give you the blueprint — it builds robots for you and hands them out when needed. Those robots are **beans**.

**Bean** = an object that Spring creates and manages for you. You don’t use `new`; Spring decides when and how to create it.

### Part 2: The Employee Analogy — From Hire to Retire

Bean lifecycle is like hiring an employee:

```
    ┌─────────────────────────────────────────────────────────────────┐
    │                    BEAN LIFECYCLE = EMPLOYEE LIFECYCLE            │
    └─────────────────────────────────────────────────────────────────┘

    1. RECRUIT         2. ORIENTATION      3. TRAINING       4. WORK      5. RETIRE
    (Create object)    (Inject helpers)   (Initialize)    (Use bean)   (Cleanup & destroy)

         📋                  📂                   📚              💼              👋
    "We hired you"    "Here's your team"   "Here's how to   "Now do your   "Thanks, clean
    (Constructor)     (Dependencies)       do the job"      work"         up & goodbye"
                                               (@PostConstruct)              (@PreDestroy)
```

1. **Recruit** — Spring “hires” the object (creates it with the constructor).
2. **Orientation** — Spring gives it its “coworkers” (injects dependencies like database, other services).
3. **Training** — Spring runs your “setup” code (e.g., open connection, load cache).
4. **Work** — The bean is ready and used by your application.
5. **Retire** — When the app shuts down, Spring runs cleanup (close connections, release resources) and removes the bean.

### Part 3: The Full Lifecycle — A Numbered Sequence

Here’s the complete sequence in simple terms:

```
    ┌─────────────────────────────────────────────────────────────────────────┐
    │                     COMPLETE BEAN LIFECYCLE SEQUENCE                      │
    └─────────────────────────────────────────────────────────────────────────┘

    ① INSTANTIATION
       └── Spring calls your constructor (new MyService())
       └── The object exists, but it's "empty"

    ② DEPENDENCY INJECTION
       └── Spring fills in all the @Autowired fields
       └── Now your bean has its helpers (other beans)

    ③ INITIALIZATION (Your Setup Code)
       └── @PostConstruct runs — "I'm ready, let me set up my stuff"
       └── Open connections, load caches, validate config

    ④ BEAN IS READY ★
       └── Other beans can use this one
       └── Your app does its work

    ⑤ DESTRUCTION (Your Cleanup Code)
       └── App is shutting down
       └── @PreDestroy runs — "Time to clean up"
       └── Close connections, release resources
       └── Bean is destroyed
```

**Summary:** Create → Inject → Initialize → Use → Destroy.

### Part 4: Why Order Matters

Think of building a house:

- You can’t hang pictures before the walls are built.
- You can’t paint before the walls exist.
- You can’t move in before the house is finished.

With beans:

- You can’t use another bean before it’s created and injected.
- You can’t connect to a database in the constructor if the connection config isn’t injected yet.
- You must clean up resources (close connections) before the app stops, or you risk leaks.

### Part 5: Bean Scopes — How Many Copies Exist?

**Scope** = How many copies of a bean Spring keeps around.

| Scope | Copies | Analogy | When Created | When Destroyed |
|-------|--------|---------|--------------|----------------|
| **singleton** | One | One company car everyone shares | At app startup | At app shutdown |
| **prototype** | New each time | New rental car each trip | Each time you ask for one | Never (JVM cleans up) |
| **request** | One per web request | One form per customer visit | When request starts | When request ends |
| **session** | One per user session | One shopping cart per user | When session starts | When session ends |

**Simple rule:**
- **singleton** — One shared copy for the whole app (default).
- **prototype** — Fresh copy every time you ask.
- **request** — One per HTTP request (web apps).
- **session** — One per user session (web apps).

### Part 6: The Restaurant Analogy (Putting It Together)

```
    Restaurant opens ──► Hire chef, buy ingredients ──► Turn on ovens, set up tables
         (new)              (dependency injection)           (@PostConstruct)

    ──► Serve customers ──► Close kitchen, clean up, lock doors
           (bean ready)              (@PreDestroy)
```

The restaurant is your bean. Spring builds it, gives it a chef and ingredients, runs setup, lets it serve customers, and cleans up when it’s done.

---

## Layer 2 — Professional Developer

### What Is a Bean? (Technical Definition)

A **bean** is an object managed by the Spring container. The container controls creation, wiring, configuration, and destruction.

**Why Spring manages beans:**
- Ensures dependencies are injected in the correct order
- Applies lifecycle callbacks at the right time
- Handles scopes (singleton, prototype, etc.)
- Enables features like AOP, transactions, and lazy loading

### Bean Creation Process — Step by Step

Spring follows this order:

1. **Instantiation** — Call the constructor.
2. **Populate properties** — Inject dependencies (setters, fields, constructor args).
3. **BeanNameAware, BeanFactoryAware, etc.** — Set Spring infrastructure if the bean implements `*Aware` interfaces.
4. **BeanPostProcessor.postProcessBeforeInitialization()** — Custom logic before init.
5. **Initialization callbacks** — `@PostConstruct`, `InitializingBean`, custom `init-method`.
6. **BeanPostProcessor.postProcessAfterInitialization()** — Custom logic after init (e.g., create proxies).
7. **Bean ready** — Bean can be used.
8. **Destruction callbacks** — `@PreDestroy`, `DisposableBean`, custom `destroy-method`.

### Lifecycle Callbacks — Comparison Table

| Mechanism | Type | When It Runs | Use Case |
|-----------|------|--------------|----------|
| `@PostConstruct` | Annotation | After dependency injection | Modern, simple setup |
| `InitializingBean.afterPropertiesSet()` | Interface | After `@PostConstruct` | Legacy or when you need an interface |
| `@Bean(initMethod = "setup")` | Config | After `afterPropertiesSet()` | External or third‑party classes |
| `@PreDestroy` | Annotation | Before destruction | Modern cleanup |
| `DisposableBean.destroy()` | Interface | After `@PreDestroy` | Legacy cleanup |
| `@Bean(destroyMethod = "teardown")` | Config | After `destroy()` | External classes |

### @PostConstruct — Practical Example

Use `@PostConstruct` when setup must run **after** all dependencies are injected.

```java
import jakarta.annotation.PostConstruct;

@Component
public class CacheService {
    // Dependencies are injected first
    private final ConfigProperties config;
    private Map<String, Object> cache;  // Not ready until init runs

    public CacheService(ConfigProperties config) {
        this.config = config;
        // DON'T init cache here — config might not be fully ready
    }

    @PostConstruct
    public void init() {
        // Runs AFTER constructor and dependency injection
        // Safe to use config and set up resources
        this.cache = new HashMap<>();
        if (config.isPreloadEnabled()) {
            loadInitialDataIntoCache();
        }
        System.out.println("Cache initialized with max size: " + config.getMaxSize());
    }

    private void loadInitialDataIntoCache() {
        // Load hot data at startup
    }
}
```

### @PreDestroy — Practical Example

Use `@PreDestroy` for cleanup before the bean is destroyed (connections, threads, files).

```java
import jakarta.annotation.PreDestroy;

@Component
public class DatabaseConnectionPool {
    private final DataSource dataSource;
    private ExecutorService executor;  // Must be shut down cleanly

    public DatabaseConnectionPool(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        // Start pool and background thread pool
        this.executor = Executors.newFixedThreadPool(10);
        System.out.println("Connection pool started");
    }

    @PreDestroy
    public void cleanup() {
        // Runs when application context is shutting down
        // MUST cleanup or threads/resources leak
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Connection pool shut down");
    }
}
```

### InitializingBean and DisposableBean (Legacy)

```java
@Component
public class FileService implements InitializingBean, DisposableBean {
    private FileWriter writer;

    @Override
    public void afterPropertiesSet() throws Exception {
        // Same idea as @PostConstruct — runs after DI
        this.writer = new FileWriter("output.txt");
    }

    @Override
    public void destroy() throws Exception {
        // Same idea as @PreDestroy — runs before destruction
        if (writer != null) {
            writer.close();
        }
    }
}
```

**Recommendation:** Prefer `@PostConstruct` and `@PreDestroy` for new code; use these interfaces only when required (e.g., legacy or framework code).

### @Bean initMethod and destroyMethod

Useful when the class is not Spring‑managed (e.g., third‑party) or you want explicit method names.

```java
@Configuration
public class AppConfig {

    @Bean(initMethod = "connect", destroyMethod = "disconnect")
    public ExternalApiClient externalApiClient() {
        // Spring will call connect() after construction
        // Spring will call disconnect() before destruction
        return new ExternalApiClient("https://api.example.com");
    }
}

// External class — no Spring annotations
public class ExternalApiClient {
    public void connect() {
        System.out.println("Connecting to API...");
    }

    public void disconnect() {
        System.out.println("Disconnecting from API...");
    }
}
```

**Note:** Spring auto‑detects `close()` and `shutdown()` for many types. To disable:

```java
@Bean(destroyMethod = "")
public DataSource dataSource() {
    return new HikariDataSource();  // HikariCP manages its own shutdown
}
```

### Bean Scopes — Summary Table

| Scope | Instances | Creation | Destruction | Typical Use |
|-------|-----------|----------|-------------|-------------|
| **singleton** | One | Startup | Shutdown | Services, shared resources |
| **prototype** | Many | On each `getBean()` | Never (GC) | Stateful, per‑request handlers |
| **request** | One per request | Per HTTP request | End of request | Request‑scoped data |
| **session** | One per session | Per HTTP session | Session end | User session, cart |
| **application** | One per ServletContext | Web app startup | Web app shutdown | Web‑wide shared state |

### @Lazy — Delay Bean Creation

By default, singleton beans are created at startup. `@Lazy` defers creation until first use.

```java
@Lazy
@Component
public class HeavyReportService {
    public HeavyReportService() {
        // This runs on FIRST use, not at startup
        System.out.println("HeavyReportService created — expensive!");
    }
}
```

**Why use it:**
- Faster startup when some beans are rarely used
- Lower initial memory usage

**Trade‑offs:**
- Configuration errors show up at first use, not at startup
- First request that needs the bean can be slower

**Application‑level setting:**

```yaml
spring:
  main:
    lazy-initialization: true
```

---

## Layer 3 — Advanced Engineering Depth

### Complete Lifecycle — All Phases (ASCII Diagram)

```
    ┌──────────────────────────────────────────────────────────────────────────┐
    │                    SPRING BEAN LIFECYCLE — FULL SEQUENCE                   │
    └──────────────────────────────────────────────────────────────────────────┘

    PHASE 1: INSTANTIATION & AWARENESS
    ═══════════════════════════════════
    [1]  BeanFactoryPostProcessor.postProcessBeanFactory()  (if any)
    [2]  Instantiation (constructor)
    [3]  Populate properties (dependency injection)
    [4]  BeanNameAware.setBeanName()
    [5]  BeanClassLoaderAware.setBeanClassLoader()
    [6]  BeanFactoryAware.setBeanFactory()
    [7]  ... (other *Aware interfaces)
    [8]  ApplicationContextAware.setApplicationContext()

    PHASE 2: INITIALIZATION
    ════════════════════════
    [9]  BeanPostProcessor.postProcessBeforeInitialization()
    [10] @PostConstruct
    [11] InitializingBean.afterPropertiesSet()
    [12] @Bean(initMethod = "...")
    [13] BeanPostProcessor.postProcessAfterInitialization()

    PHASE 3: READY
    ═════════════════
    [14] ★ BEAN READY FOR USE ★

    PHASE 4: DESTRUCTION (singleton only; prototype never)
    ═══════════════════════════════════════════════════════
    [15] @PreDestroy
    [16] DisposableBean.destroy()
    [17] @Bean(destroyMethod = "...")
```

### BeanPostProcessor — Modify Beans During Creation

A `BeanPostProcessor` lets you run logic **before** and **after** initialization for every bean.

**Spring itself uses this for:**
- `@Autowired` / `@Value`
- `@PostConstruct` / `@PreDestroy`
- AOP proxies
- Custom annotations

**Example — Logging processor:**

```java
@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        // Runs BEFORE @PostConstruct
        System.out.println("[Before Init] " + beanName);
        return bean;  // Return original or wrap with proxy
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        // Runs AFTER @PostConstruct, init-method, etc.
        System.out.println("[After Init] " + beanName);
        return bean;
    }
}
```

**Ordering:**

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HighPriorityPostProcessor implements BeanPostProcessor { }

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class LowPriorityPostProcessor implements BeanPostProcessor { }
```

**Performance:** Runs for every bean. Avoid heavy work here.

### BeanFactoryPostProcessor — Modify Bean Definitions

Runs **before** any bean is created. It works on `BeanDefinition`s (metadata), not bean instances.

**Typical uses:**
- Change bean definition properties
- Add or change `BeanDefinition`s
- Read/modify configuration before beans exist

```java
@Component
public class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        // Runs ONCE, before any bean is created
        // Can modify BeanDefinitions
        BeanDefinition bd = beanFactory.getBeanDefinition("myService");
        bd.setLazyInit(true);  // Make bean lazy
    }
}
```

**Difference from BeanPostProcessor:**

| Aspect | BeanFactoryPostProcessor | BeanPostProcessor |
|--------|--------------------------|-------------------|
| When | Before any bean created | During each bean’s creation |
| Operates on | `BeanDefinition` | Bean instance |
| Frequency | Once | Per bean |
| Use | Change definitions, config | Modify instances, proxies |

### Prototype Scope — Important Behaviors

**1. No destruction callbacks**

Spring does not manage prototype beans after creation. `@PreDestroy` and `DisposableBean.destroy()` are never called.

```java
@Component
@Scope("prototype")
public class PrototypeBean implements DisposableBean {
    @PreDestroy
    public void cleanup() {
        // ❌ NEVER CALLED
    }

    @Override
    public void destroy() {
        // ❌ NEVER CALLED
    }
}
```

**2. Prototype inside singleton = effectively singleton**

When a prototype bean is injected into a singleton, it is created once and reused.

```java
@Component
public class SingletonService {
    @Autowired
    private PrototypeBean prototypeBean;  // Created ONCE, reused!
}
```

**Fix — `ObjectProvider` or `ApplicationContext`:**

```java
@Component
public class SingletonService {
    private final ObjectProvider<PrototypeBean> prototypeProvider;

    public SingletonService(ObjectProvider<PrototypeBean> prototypeProvider) {
        this.prototypeProvider = prototypeProvider;
    }

    public void use() {
        PrototypeBean bean = prototypeProvider.getObject();  // New instance each time
    }
}
```

**3. Memory and cleanup**

Prototypes are not tracked. If they hold resources (files, connections), you must clean them yourself, e.g. via `AutoCloseable` and `try-with-resources`.

### Lifecycle Order — Multiple Callbacks

If a bean uses several init mechanisms:

1. `@PostConstruct`
2. `InitializingBean.afterPropertiesSet()`
3. `@Bean(initMethod = "...")`

Destruction (reverse order):

1. `@PreDestroy`
2. `DisposableBean.destroy()`
3. `@Bean(destroyMethod = "...")`

**Caution:** Multiple `@PostConstruct` or `@PreDestroy` methods — order is not guaranteed. Prefer a single method per phase.

---

## Layer 4 — Interview Mastery

### Q1: Explain the Spring bean lifecycle.

**Answer:**

1. **Instantiation** — Constructor is called.
2. **Dependency injection** — Properties and dependencies are set.
3. **Aware callbacks** — `BeanNameAware`, `ApplicationContextAware`, etc. (if implemented).
4. **BeanPostProcessor.postProcessBeforeInitialization()**
5. **@PostConstruct**
6. **InitializingBean.afterPropertiesSet()**
7. **Custom init method** (`@Bean(initMethod = "...")`)
8. **BeanPostProcessor.postProcessAfterInitialization()**
9. **Bean ready for use**
10. **@PreDestroy** (on shutdown)
11. **DisposableBean.destroy()**
12. **Custom destroy method** (`@Bean(destroyMethod = "...")`)

Prototype beans do not get destruction callbacks.

---

### Q2: @PostConstruct vs InitializingBean?

| Aspect | @PostConstruct | InitializingBean |
|--------|----------------|------------------|
| Type | Annotation | Interface |
| Coupling | Low | High |
| Method name | Any | `afterPropertiesSet()` |
| Order | Before `afterPropertiesSet()` | After `@PostConstruct` |
| Preference | Preferred for new code | Legacy/framework |

---

### Q3: What is BeanPostProcessor and when to use it?

**Answer:** A `BeanPostProcessor` intercepts bean creation and can modify beans before and after initialization.

**When:** Cross‑cutting concerns (logging, metrics), proxy creation, custom annotation handling.

**Warning:** It runs for every bean; keep it lightweight.

---

### Q4: What is BeanFactoryPostProcessor?

**Answer:** It runs before any bean is created and works on `BeanDefinition`s, not instances. Use it to change bean metadata (e.g. lazy init, add properties) or bean definitions before instantiation.

---

### Q5: Why don’t prototype beans get destroy callbacks?

**Answer:** After creation, Spring hands the prototype to the caller and no longer tracks it. Spring has no lifecycle to manage, so it never calls `@PreDestroy` or `DisposableBean.destroy()`. Use manual cleanup (e.g. `AutoCloseable` + `try-with-resources`).

---

### Q6: How does @Lazy work?

**Answer:** `@Lazy` delays creation until first use. Startup is faster and memory use is lower initially, but failures appear at runtime instead of startup.

---

### Q7: Order of initialization callbacks?

**Answer:**  
1. `BeanPostProcessor.postProcessBeforeInitialization()`  
2. `@PostConstruct`  
3. `InitializingBean.afterPropertiesSet()`  
4. Custom init method  
5. `BeanPostProcessor.postProcessAfterInitialization()`

---

### Q8: Prototype bean injected into singleton — what happens?

**Answer:** The prototype is effectively a singleton: it is created once during DI and reused. To get a new instance each time, use `ObjectProvider<PrototypeBean>` or `ApplicationContext.getBean(PrototypeBean.class)`.

---

### Q9: Singleton vs prototype scope?

| Aspect | Singleton | Prototype |
|--------|-----------|-----------|
| Instances | One | New each request |
| Created | At startup | On demand |
| Destroy callbacks | Yes | No |
| Typical use | Stateless services | Stateful, per-request |

---

### Q10: How to clean up prototype beans?

**Answer:** Spring does not call destroy methods. Options: implement `AutoCloseable` and use `try-with-resources`, call a cleanup method explicitly, or use an object pool with explicit release.

---

## Summary

- **Bean** = object created and managed by the Spring container.
- **Lifecycle phases:** instantiation → dependency injection → initialization → ready → destruction.
- **Init options:** `@PostConstruct` (preferred), `InitializingBean`, `@Bean(initMethod)`.
- **Destroy options:** `@PreDestroy` (preferred), `DisposableBean`, `@Bean(destroyMethod)`.
- **Scopes:** singleton (default), prototype, request, session, application.
- **BeanPostProcessor:** modify bean instances during creation (e.g. proxies, cross‑cutting logic).
- **BeanFactoryPostProcessor:** modify bean definitions before any bean is created.
- **@Lazy:** delays singleton creation until first use.
- **Prototype:** no destroy callbacks; inject via `ObjectProvider` from singleton when you need fresh instances.

---

[← Back to Index](00-README.md) | [Previous: Starters & Context](16-Starters-Context.md) | [Next: Configuration & Profiles →](18-Configuration-Profiles.md)
