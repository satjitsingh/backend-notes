# Chapter 16: Starters & Application Context

[← Back to Index](00-README.md) | [Previous: Auto-Configuration](15-Auto-Configuration.md) | [Next: Bean Lifecycle →](17-Bean-Lifecycle.md)

---

## Why This Chapter Matters

**Starters** and **ApplicationContext** are two of the most fundamental Spring Boot concepts. If you don't understand them, you're building on a foundation you can't see.

**Starters** are what makes Spring Boot "magic" — you add ONE dependency and suddenly database access, web servers, or security just works. But HOW? Understanding starters removes the mystery and gives you control.

**ApplicationContext** is the **heart of Spring**. It's the container that holds all your beans (objects) and manages their lifecycles. Every time Spring "injects" something into your class, it's the ApplicationContext doing the work.

**Without understanding Starters:**
- You'll manually manage dozens of dependency versions (and get conflicts)
- You won't know why adding `spring-boot-starter-web` suddenly gives you Tomcat
- You'll fail to create reusable starter modules for your team
- You'll struggle when you need to swap one library for another

**Without understanding ApplicationContext:**
- You'll be confused about WHERE your beans come from and HOW Spring finds them
- You'll struggle to debug "NoSuchBeanDefinitionException" (one of the most common errors)
- You won't know how to access beans programmatically when injection isn't available
- You'll misunderstand bean scopes, profiles, and context hierarchy

**With proper understanding:**
- You'll leverage starters to set up any functionality in minutes
- You'll create custom starters for your organization (a senior-level skill)
- You'll understand exactly how Spring creates, manages, and provides your beans
- You'll debug dependency and bean issues confidently

---

## Layer 1 — Intuition Builder

### Part 1: What Is a Dependency? (Plain English First)

Before we talk about starters, we need to understand a **dependency**.

Imagine you're making a chocolate cake. You need flour, eggs, sugar, cocoa, butter — lots of things. Each ingredient is like a **dependency**. Your cake *depends* on them to exist. In programming, when your app needs someone else's code to work (like a library that reads JSON or talks to a database), that's a dependency. Your app *depends* on that code.

**Technical term alert:** A **dependency** = a piece of code (a library) that your project needs to run. We download these from the internet (from places like Maven Central) and use them in our code.

---

### Part 2: The Problem — Dependency Hell (Why Starters Exist)

**The situation before Spring Boot:**

Imagine you're ordering lunch at a restaurant. You want a burger meal. But instead of ordering "the burger combo," the waiter says: "You must order each item separately. The burger. The bun. The fries. The ketchup packet. The napkin. The drink. The straw. The cup. The ice. And you must tell me exactly which brand of ketchup, which potato farm the fries came from, and the exact version of the cup manufacturer."

Sounds crazy, right? But that's exactly what developers had to do before Spring Boot!

To build a simple web app, you needed to add **10, 15, or even 20 separate dependencies** — and here's the nightmare part: **each one had a version number**. What if the web framework version 5.3 doesn't work with the JSON library version 2.14? What if the database driver needs an older version of something? Developers spent hours, sometimes days, fixing "version conflicts" — when two libraries demanded incompatible versions of a third library.

**We call this "dependency hell."**

---

### Part 3: Starters — The Meal Combo Solution

**The solution: meal combos!**

Back to the restaurant. Instead of ordering 15 items one by one, you say: **"I'll have the Web Combo, please."** The kitchen knows exactly what that means: burger + fries + drink + napkin + ketchup — all the right brands that work together, in the right amounts.

**Starters are the "meal combos" of Spring Boot.**

When you add **one** starter (like `spring-boot-starter-web`), you get **everything** you need for that feature — all the right libraries, all in versions that work together. The Spring team has already tested them. No more guessing. No more version conflicts.

**Before Starters (the old way):**
```
You manually add: spring-web, spring-webmvc, tomcat, jackson-databind, 
jackson-core, hibernate-validator, logback, slf4j... (10+ dependencies)
Each with its own version. Hope they all get along!
```

**After Starters (Spring Boot way):**
```
You add: spring-boot-starter-web
That's it. One line. Spring Boot brings in everything — and all versions match.
```

---

### Part 4: Common Starters — The Menu

Here's the "menu" of the most popular starters:

| Starter | What It Gives You | When You'd Use It |
|---------|-------------------|-------------------|
| `spring-boot-starter-web` | Web server + REST support + JSON | When you want to build a website or API |
| `spring-boot-starter-data-jpa` | Database access (saving/loading data) | When you want to store data in a database |
| `spring-boot-starter-security` | Login, passwords, who-can-do-what | When you need to protect your app |
| `spring-boot-starter-test` | Testing tools (JUnit, Mockito) | When you want to write tests |
| `spring-boot-starter-actuator` | Health checks, metrics (is the app okay?) | When you need to monitor your app |

**Simple rule:** Need web stuff? Add web starter. Need a database? Add data-jpa starter. Need security? Add security starter. One line each.

---

### Part 5: What Is ApplicationContext? (Plain English First)

Now let's talk about **ApplicationContext**. This is a fancy name for something simple.

**Imagine your app is a company.**

- You have employees: `OrderService`, `UserService`, `PaymentService`, etc.
- Employees need other employees to do their job. (OrderService needs UserService and PaymentService.)
- Someone has to: **hire** everyone, **assign** who works with whom, and **make sure** everyone has what they need.

That "someone" is the **ApplicationContext**. It's like the **brain** or **HR department** of your app.

- It **creates** all your objects (beans) — like hiring employees.
- It **wires** them together — like assigning teams.
- It **knows** who exists — you can ask it "give me OrderService" and it will.

**Technical term:** ApplicationContext is Spring's **container** — a place that holds and manages all your beans (objects). When Spring "injects" a dependency into your class, the ApplicationContext is the one doing the injection.

---

### Part 6: BeanFactory vs ApplicationContext — The Simple Version

Both are "containers" that hold beans. But:

- **BeanFactory** = basic container. "I create beans when you ask. That's it."
- **ApplicationContext** = BeanFactory + a lot more. "I create beans, publish events, load files, handle messages, and more."

**Rule of thumb:** We almost always use ApplicationContext. It's the full-featured version. BeanFactory is the minimal version — useful in rare cases when you need something lightweight.

---

### Part 7: What Does ApplicationContext Actually Do?

Think of ApplicationContext as having four main jobs:

| Job | Plain English | Example |
|-----|---------------|---------|
| **Bean Factory** | Creates and gives you objects | "Give me OrderService" → you get it |
| **Event Publisher** | Sends announcements ("something happened!") | "Order was created!" → other parts of the app can react |
| **Resource Loader** | Loads files (config, properties) | "Load config.properties from the classpath" |
| **Message Resolver** | Handles different languages (i18n) | "Get the 'welcome' message in French" |

---

### Part 8: How Do I Get the ApplicationContext?

In a Spring Boot app, when you run `SpringApplication.run(...)`, the ApplicationContext is created automatically. You can grab it and use it:

```java
// When your app starts, this line creates the ApplicationContext
ApplicationContext context = SpringApplication.run(MyApp.class, args);

// Now you can ask for any bean
OrderService service = context.getBean(OrderService.class);
```

**But here's the secret:** Most of the time you *don't* need to grab the context. You just tell Spring what you need in your constructor, and Spring injects it. Asking the context directly is for special cases (like when you need a bean whose name you don't know until runtime).

---

## Layer 2 — Professional Developer

### Starters: Before vs After (Real Code)

**Why this matters:** Seeing the "before" makes you appreciate the "after." Before starters, developers copied dozens of dependency blocks and hoped versions aligned.

**BEFORE Starters — Manual dependency management (10+ dependencies):**

```xml
<!-- Each dependency added manually with explicit versions -->
<!-- One wrong version = hours of debugging -->
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
        <version>5.3.21</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
        <version>5.3.21</version>
    </dependency>
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-core</artifactId>
        <version>9.0.65</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.13.3</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-core</artifactId>
        <version>2.13.3</version>
    </dependency>
    <dependency>
        <groupId>org.hibernate.validator</groupId>
        <artifactId>hibernate-validator</artifactId>
        <version>6.2.3</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.11</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.36</version>
    </dependency>
    <!-- ... and more. Versions must stay in sync across Spring ecosystem! -->
</dependencies>
```

**AFTER Starters — One dependency, versions managed by Spring Boot parent:**

```xml
<!-- One line. Spring Boot parent POM manages all versions. -->
<!-- All transitive dependencies included. All compatible. -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- No version needed — parent POM defines it -->
    </dependency>
</dependencies>
```

---

### How Starters Work Internally

**Why it matters:** Understanding the structure helps you debug "why did this dependency get pulled in?" and "how do I exclude something?"

**Anatomy of a starter:**

Starters are **POM files** — they contain no Java code. They are just dependency declarations. When you add a starter, Maven/Gradle resolves a tree of dependencies.

**What's inside `spring-boot-starter-web`?**

```
spring-boot-starter-web (the POM you add)
├── spring-boot-starter (base: core, autoconfigure, logging)
├── spring-web (Spring's web abstractions)
├── spring-webmvc (MVC framework for REST/controllers)
├── spring-boot-starter-tomcat (embedded Tomcat server)
└── spring-boot-starter-json (Jackson for JSON)
```

**Version management:** The Spring Boot **parent POM** declares versions for hundreds of libraries. Your starter dependencies don't specify versions — they inherit them. One place to upgrade (the parent version) → everything stays consistent.

```xml
<!-- In your pom.xml — parent provides version management -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<!-- Dependencies get versions from parent automatically -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- Version comes from parent's dependencyManagement -->
</dependency>
```

---

### Common Starters Reference Table

| Starter | Purpose | Key Transitive Dependencies |
|---------|---------|----------------------------|
| `spring-boot-starter` | Core (always transitively included) | spring-boot, spring-boot-autoconfigure, spring-boot-starter-logging |
| `spring-boot-starter-web` | Web apps, REST APIs | spring-webmvc, tomcat-embed, jackson |
| `spring-boot-starter-data-jpa` | Database with JPA | hibernate-core, spring-data-jpa |
| `spring-boot-starter-security` | Authentication & authorization | spring-security-web, spring-security-config |
| `spring-boot-starter-test` | Testing (JUnit, Mockito, etc.) | junit-jupiter, mockito, assertj |
| `spring-boot-starter-actuator` | Production monitoring | micrometer, health indicators |

---

### Accessing ApplicationContext — The Four Ways

**Why it matters:** Most of the time you inject beans directly. But sometimes you need the context itself — for dynamic lookup, optional beans, or event publishing.

#### Method 1: ApplicationContextAware (for framework-style code)

```java
@Component
public class MyBean implements ApplicationContextAware {
    private ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.context = ctx; // Spring calls this after creating the bean
    }
    
    public void doSomething() {
        // Use context when you need dynamic lookup
        OrderService service = context.getBean(OrderService.class);
        service.processOrder(order);
    }
}
```

**When to use:** You need the context in many methods, or you're building framework-level components.

---

#### Method 2: Inject ApplicationContext via Constructor (preferred over field injection)

```java
@Component
public class MyBean {
    private final ApplicationContext context;
    
    // Spring injects ApplicationContext automatically
    public MyBean(ApplicationContext context) {
        this.context = context;
    }
    
    public void doSomething() {
        OrderService service = context.getBean(OrderService.class);
        service.processOrder(order);
    }
}
```

**When to use:** Simple cases where you need the context. Constructor injection is testable and immutable.

---

#### Method 3: Inject the Bean Directly (best when you know the type at compile time)

```java
@Component
public class MyBean {
    private final OrderService orderService;
    
    // Prefer this! No need for ApplicationContext
    public MyBean(OrderService orderService) {
        this.orderService = orderService;
    }
    
    public void doSomething() {
        orderService.processOrder(order); // Direct use, no lookup
    }
}
```

**When to use:** Almost always. Use ApplicationContext only when you need dynamic lookup, optional beans, or multiple beans of the same type.

---

#### Method 4: ObjectProvider for Optional or Multiple Beans

```java
@Component
public class MyBean {
    private final ObjectProvider<OptionalService> optionalServiceProvider;
    
    public MyBean(ObjectProvider<OptionalService> optionalServiceProvider) {
        this.optionalServiceProvider = optionalServiceProvider;
    }
    
    public void doSomething() {
        // Get bean only if it exists — no exception if missing
        OptionalService service = optionalServiceProvider.getIfAvailable();
        if (service != null) {
            service.doSomething();
        }
    }
}
```

**When to use:** Optional dependencies, or when multiple beans of the same type exist and you need lazy/conditional access.

---

### ApplicationContext Capabilities Table

| Capability | Description | Code Example |
|------------|-------------|--------------|
| **Bean Factory** | Get beans by type or name | `context.getBean(OrderService.class)` |
| **Event Publishing** | Broadcast application events | `context.publishEvent(new OrderCreatedEvent(order))` |
| **Resource Loading** | Load files from classpath, file system, URL | `context.getResource("classpath:config.properties")` |
| **Message Resolution** | i18n (internationalization) | `context.getMessage("key", args, locale)` |
| **Environment** | Access profiles, properties | `context.getEnvironment().getActiveProfiles()` |
| **Parent Context** | Hierarchical contexts | `context.getParent()` |

---

### ApplicationContext Implementations (Context Types)

**Why it matters:** Different apps need different context types. Spring Boot uses `ServletWebServerApplicationContext` for web apps; tests might use `AnnotationConfigApplicationContext`.

| Implementation | Use Case |
|----------------|----------|
| `AnnotationConfigApplicationContext` | Java config, component scanning, modern apps |
| `GenericApplicationContext` | Programmatic registration, testing |
| `ServletWebServerApplicationContext` | Spring Boot web apps (created by `SpringApplication.run`) |
| `ClassPathXmlApplicationContext` | Legacy XML configuration (avoid in new projects) |

```java
// Spring Boot web app — context is created implicitly
ApplicationContext context = SpringApplication.run(Application.class, args);
// Internally: ServletWebServerApplicationContext

// Manual Java config (e.g., tests)
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
```

---

### Excluding Transitive Dependencies (Swap Tomcat for Jetty)

**Why:** You might want a different embedded server (e.g., Jetty instead of Tomcat) or need to remove an unwanted transitive dependency.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

Spring Boot auto-configuration detects Jetty on the classpath and configures it instead of Tomcat.

---

### Environment and Profiles

**Why it matters:** You often need different config for dev vs prod. The ApplicationContext's `Environment` holds active profiles and properties.

```java
@Component
public class ConfigChecker {
    private final ApplicationContext context;
    
    public ConfigChecker(ApplicationContext context) {
        this.context = context;
    }
    
    public void logEnvironment() {
        String[] profiles = context.getEnvironment().getActiveProfiles();
        // e.g., ["dev"] or ["prod"]
        
        String port = context.getEnvironment().getProperty("server.port");
        // Read from application.properties / application.yml
    }
}
```

---

## Layer 3 — Advanced Engineering Depth

### How Starters and Auto-Configuration Work Together

**Internal flow:**

1. You add `spring-boot-starter-web` → Maven pulls in `spring-boot-autoconfigure` (transitively).
2. `spring-boot-autoconfigure` contains `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` listing auto-configuration classes.
3. On startup, Spring Boot loads these classes and evaluates `@ConditionalOn*` conditions (e.g., "is Tomcat on classpath?").
4. If conditions pass, beans (e.g., `DispatcherServlet`, `TomcatServletWebServerFactory`) are registered.
5. Your app runs with a fully configured web server and MVC stack.

**Edge case:** If you exclude `spring-boot-starter-tomcat` but don't add Jetty/Undertow, the web server auto-configuration may back off, and you could get a non-web context (or a failure) unless you provide your own `ServletWebServerFactory`.

---

### Creating a Custom Starter

**When:** Reusable setup across many projects (e.g., company-wide "our standard Redis + metrics setup").

**Structure:** Two modules — *autoconfigure* (logic) and *starter* (POM that pulls autoconfigure + deps).

**Autoconfigure module:**

```
my-starter-autoconfigure/
├── src/main/java/com/example/mystarter/
│   ├── MyStarterAutoConfiguration.java
│   └── MyStarterProperties.java
└── src/main/resources/META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**AutoConfiguration class:**

```java
@Configuration
@ConditionalOnClass(MyService.class)  // Only when MyService is on classpath
@EnableConfigurationProperties(MyStarterProperties.class)
public class MyStarterAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean  // Don't override user's own bean
    public MyService myService(MyStarterProperties props) {
        return new MyService(props.getApiKey(), props.getTimeout());
    }
}
```

**Properties class:**

```java
@ConfigurationProperties(prefix = "my.starter")
public class MyStarterProperties {
    private String apiKey = "";
    private int timeout = 5000;
    // getters/setters
}
```

**Registration file:** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.example.mystarter.MyStarterAutoConfiguration
```

**Starter POM (no code):**

```xml
<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>my-starter-autoconfigure</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

---

### ApplicationContext Refresh Lifecycle (Deep Internals)

**Why it matters:** Helps debug startup failures, bean creation order, and "why isn't my bean there yet?"

High-level refresh phases:

```
1. prepareRefresh()           — Startup timestamp, validate env
2. obtainFreshBeanFactory()    — Create/obtain BeanFactory
3. prepareBeanFactory()       — Configure BeanFactory (classloader, resolvers)
4. postProcessBeanFactory()   — Allow subclasses to customize
5. invokeBeanFactoryPostProcessors() — Process @Configuration, @PropertySource
6. registerBeanPostProcessors()     — Register @Autowired, @PostConstruct processors
7. initMessageSource()        — i18n
8. initApplicationEventMulticaster() — Event infrastructure
9. onRefresh()                — Subclass hooks (e.g., create web server)
10. registerListeners()       — Register event listeners
11. finishBeanFactoryInitialization() — Create non-lazy singletons (YOUR BEANS)
12. finishRefresh()           — Publish ContextRefreshedEvent
```

**Performance note:** Step 11 is usually the slowest — that's when all singleton beans are instantiated. Slow `@PostConstruct` or heavyweight `@Bean` methods here delay startup.

---

### Context Hierarchy and Bean Resolution

**When:** Web apps (root + servlet contexts), multi-module apps, shared test context.

```
Parent (Root)
├── SharedService
├── DataSource
└── ...

Child (Servlet / Module)
├── Controller
└── ... (can see parent beans)
```

**Resolution order:** Child first, then parent. Beans in child can inject parent beans; parent cannot see child beans.

---

### BeanFactory vs ApplicationContext — Deep Comparison

| Aspect | BeanFactory | ApplicationContext |
|--------|-------------|---------------------|
| Bean creation | Lazy (on first `getBean`) | Eager for singletons (at refresh) |
| Event system | No | Yes (`ApplicationEventPublisher`) |
| Message source (i18n) | No | Yes |
| Resource loading | Basic | Enhanced (`ResourcePatternResolver`) |
| `ApplicationContextAware` | No | Yes |
| AOP integration | No | Yes (if AOP on classpath) |
| Typical use | Rare (low-memory, custom scenarios) | Default for almost all apps |

---

## Layer 4 — Interview Mastery

### Q1: What are Spring Boot starters?

**Answer:** Starters are curated dependency sets — POMs that declare transitive dependencies for a feature (web, data, security, etc.). One starter brings in many compatible libraries; versions are managed by the Spring Boot parent POM. They reduce dependency hell and work with auto-configuration for "just add one dependency and it works."

---

### Q2: What does spring-boot-starter-web include?

**Answer:** Web server (embedded Tomcat), Spring MVC, Jackson for JSON, validation (Hibernate Validator), and logging (via spring-boot-starter). It provides everything needed for REST APIs and web apps out of the box.

---

### Q3: How does Spring Boot manage starter versions?

**Answer:** Via the `spring-boot-starter-parent` POM's `dependencyManagement` section. It declares versions for hundreds of dependencies. Starters reference these without explicit versions, so upgrading the parent upgrades everything consistently. You can override with `<properties>` (e.g., `<jackson.version>2.16.0</jackson.version>`).

---

### Q4: What is ApplicationContext?

**Answer:** ApplicationContext is Spring's central IoC container. It extends BeanFactory and adds event publishing, resource loading, message resolution, and AOP. It creates beans, wires dependencies, and manages their lifecycle. In Spring Boot, `SpringApplication.run()` creates a `ServletWebServerApplicationContext` for web apps.

---

### Q5: BeanFactory vs ApplicationContext?

**Answer:** BeanFactory is the minimal IoC container (lazy bean creation). ApplicationContext extends it with eager singleton creation, events, i18n, resources, and AOP. Use ApplicationContext unless you have a specific need for the lighter BeanFactory.

---

### Q6: When would you access ApplicationContext directly?

**Answer:** When you need (1) dynamic bean lookup by name at runtime, (2) optional beans (`getIfAvailable`), (3) multiple beans of the same type, (4) event publishing, or (5) prototype bean creation. Prefer direct injection when the type is known at compile time.

---

### Q7: How do you create a custom starter?

**Answer:** Create two modules: (1) *autoconfigure* — `@Configuration` with `@ConditionalOn*`, `@ConfigurationProperties`, register in `AutoConfiguration.imports`; (2) *starter* — POM that depends on autoconfigure and required libraries. The starter is just a POM; all logic lives in autoconfigure.

---

### Q8: What happens when you exclude a dependency from a starter?

**Answer:** The excluded dependency is removed from the dependency tree. Useful to swap implementations (e.g., exclude Tomcat, add Jetty) or remove unused features. Auto-configuration may back off if a required class is missing (e.g., no web server if all server starters are excluded).

---

### Q9: What is ApplicationContext hierarchy?

**Answer:** Parent-child relationships between contexts. Child can access parent beans; parent cannot see child. Used in web apps (root vs servlet context) and for shared configuration in tests. Resolution: child first, then parent.

---

### Q10: How do you test a custom starter?

**Answer:** Use `@SpringBootTest` with `@EnableAutoConfiguration`, or `ApplicationContextRunner` for lightweight tests. Verify beans appear when conditions are met and are absent when disabled (e.g., `my.starter.enabled=false`). Test property binding with `@SpringBootTest(properties = {...})`.

---

## Summary

| Topic | Key Takeaway |
|-------|--------------|
| **Starters** | Curated dependency bundles; one line adds many compatible libraries. Versions managed by parent POM. |
| **Before starters** | Manual declaration of 10+ dependencies with version conflicts. |
| **After starters** | One starter, auto-configuration, minimal setup. |
| **ApplicationContext** | Spring's IoC container: creates beans, wires dependencies, publishes events, loads resources. |
| **BeanFactory** | Minimal container; ApplicationContext is the full-featured default. |
| **Accessing context** | Prefer direct injection. Use context for dynamic lookup, optional beans, events. |
| **Custom starters** | Autoconfigure module (logic) + starter POM (deps). Register in `AutoConfiguration.imports`. |

---

[← Back to Index](00-README.md) | [Previous: Auto-Configuration](15-Auto-Configuration.md) | [Next: Bean Lifecycle →](17-Bean-Lifecycle.md)
