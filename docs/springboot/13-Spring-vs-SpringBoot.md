# Chapter 13: Spring vs Spring Boot

[← Back to Index](00-README.md) | [Previous: IoC & DI](12-IoC-DI-DeepDive.md) | [Next: Spring Boot Architecture →](14-SpringBoot-Architecture.md)

---

## Why This Chapter Matters

Imagine someone asks you: "Do you know the difference between Spring and Spring Boot?" If you fumble or say "Spring Boot is newer," you've lost credibility. This chapter gives you rock-solid clarity.

**Without understanding the difference:**
- You'll struggle to explain Spring Boot's value in interviews
- You'll treat them as competitors instead of teammates
- You'll misuse features or fight against auto-configuration
- You'll make wrong choices when starting new projects

**With proper understanding:**
- You'll explain that Spring Boot *is* Spring—just with less setup
- You'll debug "why did Spring Boot do that?" confidently
- You'll know when to override defaults and when to trust them
- You'll ace Spring Boot interview questions

---

## Layer 1 — Intuition Builder

### Part 1: The Car Analogy

Picture you want a car.

**Spring Framework** = A huge warehouse full of car parts: engines, wheels, seats, steering wheels, wiring. You get to build your own car. You decide exactly which engine, which seats, how to connect everything. You have *total control*—but you spend weeks putting it together. You also need to read manuals, test that parts fit, and fix mistakes.

**Spring Boot** = You go to a dealership and buy a car that's already built. The engine, seats, and wiring are all connected. You turn the key and drive. Maybe you'd prefer different seats or a different radio—you can still change those later—but you get going *today*.

**Same engine underneath.** Spring Boot doesn't replace Spring. It uses Spring's engine and adds the "already assembled" part.

---

### Part 2: What Problem Did People Have?

Before Spring Boot, building a web app with Spring meant:

1. **Writing lots of setup code** — Telling the computer exactly how to connect database, web server, and your code. Like reading a long recipe every time you want to cook.
2. **Managing version numbers** — Making sure Library A and Library B work together. If you pick wrong versions, things break.
3. **Deploying to a separate server** — Your app was a package you had to put on another program (a "server") before anyone could use it.

Spring Boot said: *"What if we gave you smart defaults so most of this setup disappears? You can still change anything you want."*

That idea—sensible defaults you can override—is called **opinionated defaults**.

---

### Part 3: What Is Spring Framework? (Plain English)

**Spring Framework** is a set of tools that help you build programs by:

- **Dependency Injection (DI)** — Instead of your code creating its own helpers, Spring gives them to you. Like a chef getting ingredients delivered instead of growing them.
- **Inversion of Control (IoC)** — Spring decides *when* things run and *how* they're connected. You focus on "what" your app does, not "how" the wiring works.

Spring is powerful but flexible. It doesn't assume much—you configure almost everything yourself.

---

### Part 4: What Is Spring Boot? (Plain English)

**Spring Boot** is Spring with a friendly layer on top. It adds:

1. **Auto-configuration** — Spring Boot looks at what libraries you added and guesses sensible settings. If you add a database library, it sets up a database connection for you.
2. **Starter dependencies** — One small piece of config pulls in many related pieces. Like ordering a "web app kit" instead of buying 20 separate items.
3. **Embedded server** — A server comes *inside* your app. You run one file, and your app is live. No separate server to install.
4. **Opinionated defaults** — It chooses how things work unless you say otherwise. Fewer decisions, faster progress.

**Key idea:** Spring Boot *is* Spring. It's not a different product. It's Spring plus convenience.

---

### Part 5: The Cooking Analogy

**Spring Framework** = Raw ingredients and recipes. You buy flour, eggs, sugar. You follow recipes. You measure, mix, bake. Full control—but it takes time.

**Spring Boot** = A meal kit. Ingredients are pre-selected. Recipes are written. You mostly follow steps. Less control, but dinner is ready in 30 minutes.

---

### Part 6: Common Misconceptions

| Misconception | The Truth |
|---------------|-----------|
| "Spring Boot replaces Spring" | Spring Boot uses Spring. It adds helpers on top. |
| "They're competitors" | Spring Boot is part of the Spring family. |
| "Spring Boot is only for beginners" | Professionals use it for production apps and microservices. |
| "Spring Boot locks you in" | You can override almost any default. |
| "You have to choose one" | When you use Spring Boot, you're using Spring. |

---

### Part 7: The "Before and After" Feeling

**Without Spring Boot (Traditional Spring):**

- Create XML or Java config files
- Set up a web server (Tomcat) separately
- List many dependencies and their versions
- Package your app as a WAR (special file for servers)
- Deploy that WAR to the server
- Hope versions don't conflict

**With Spring Boot:**

- Add one dependency (e.g., `spring-boot-starter-web`)
- Write one main class with `@SpringBootApplication`
- Run it. Done. A server starts, and your app is live.

Same capabilities. Less paperwork.

---

## Layer 2 — Professional Developer

### Why Spring Boot Exists: The Real Pain Points

Before Spring Boot, developers spent significant time on:

1. **Boilerplate configuration** — Repetitive setup for databases, web layers, security.
2. **Version management** — Ensuring Spring, Jackson, Hibernate, etc. work together.
3. **Deployment complexity** — WAR files, external Tomcat, server.xml, and deployment descriptors.
4. **Inconsistent project structure** — Every team did it slightly differently.

Spring Boot addresses these with **convention over configuration**: agree on sensible conventions, configure only when you need to differ.

---

### Key Differences: At a Glance

| Aspect | Spring Framework | Spring Boot |
|--------|-----------------|-------------|
| **Configuration** | Manual (XML or Java config) | Auto-configured by default |
| **Server** | External (e.g., Tomcat you install) | Embedded (Tomcat/Jetty/Undertow inside your JAR) |
| **Dependencies** | You manage versions | Starters + Bill of Materials (BOM) manage versions |
| **Setup time** | Hours to days | Minutes |
| **Deployment** | WAR → external server | JAR → `java -jar app.jar` |
| **Config files** | Multiple XML files | `application.properties` or `application.yml` |
| **Monitoring** | You add and wire it | Actuator included and pre-wired |

---

### What Spring Framework Is (Technical)

Spring Framework provides:

- **Core Container** — Holds and manages your objects (beans). Handles creation, lifecycle, and wiring.
- **Dependency Injection** — Objects receive their dependencies instead of creating them.
- **AOP (Aspect-Oriented Programming)** — Cross-cutting concerns like logging, security, transactions.
- **Data access** — Templates and integration for JDBC, JPA, transactions.
- **Web support** — Spring MVC for web and REST APIs.

You configure all of this explicitly (XML or Java config). Spring Boot builds on this and adds automation.

---

### What Spring Boot Adds

#### 1. Auto-Configuration

**Why it matters:** Most apps need similar setups (database, JSON, web layer). Auto-configuration provides those setups when it detects the right libraries.

Spring Boot decides what to configure based on:
- What’s on the **classpath** (which libraries you added)
- What **beans** already exist (so it doesn’t override yours)
- What **properties** you set (so you can tune behavior)

**Example: database connection**

```java
// WITHOUT Spring Boot — You wire everything manually
@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() {
        // You must create and configure the connection pool yourself
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost/mydb");
        config.setUsername("user");
        config.setPassword("password");
        config.setDriverClassName("org.postgresql.Driver");
        return new HikariDataSource(config);
    }
}
```

```properties
# WITH Spring Boot — Add dependency + properties, done
# spring-boot-starter-data-jpa brings database support
# Spring Boot creates DataSource bean automatically

# application.properties
spring.datasource.url=jdbc:postgresql://localhost/mydb
spring.datasource.username=user
spring.datasource.password=password
# No Java config needed — auto-configuration does it
```

---

#### 2. Starter Dependencies

**Why they matter:** A web app needs many libraries (Spring MVC, Jackson, embedded server, validation, etc.). Starters group them and ensure compatible versions.

**Without Spring Boot — you list many dependencies and versions:**

```xml
<!-- Traditional Spring: many dependencies, you pick versions -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>6.0.0</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>6.0.0</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.0</version>
</dependency>
<!-- ... many more, versions must match ... -->
```

**With Spring Boot — one starter, versions managed:**

```xml
<!-- One starter pulls in web stack with compatible versions -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- No version — Spring Boot BOM manages it -->
</dependency>
```

**Common starters:**

| Starter | Purpose |
|---------|---------|
| `spring-boot-starter-web` | REST APIs, Spring MVC, embedded Tomcat, Jackson |
| `spring-boot-starter-data-jpa` | JPA, Hibernate, database access |
| `spring-boot-starter-security` | Spring Security |
| `spring-boot-starter-test` | JUnit, Mockito, AssertJ, test support |
| `spring-boot-starter-actuator` | Health, metrics, monitoring endpoints |

---

#### 3. Embedded Servers

**Why it matters:** You used to build a WAR, install Tomcat, and deploy. With an embedded server, the server is inside your app. Run the JAR and it serves HTTP.

**Traditional Spring:**

```text
1. Build WAR file
2. Install Tomcat (or similar) on a server
3. Configure server.xml, ports, context path
4. Deploy WAR to Tomcat
5. Start Tomcat
```

**Spring Boot:**

```java
@SpringBootApplication  // Combines config, auto-config, component scanning
public class Application {
    public static void main(String[] args) {
        // Embedded Tomcat starts automatically when context refreshes
        SpringApplication.run(Application.class, args);
    }
}
// Run: java -jar app.jar — app is live
```

**Available embedded servers:**
- **Tomcat** (default)
- **Jetty**
- **Undertow**

---

#### 4. Simplified Configuration: XML vs Properties

**Traditional Spring (XML):**

```xml
<!-- Multiple XML files, verbose setup -->
<beans>
    <context:component-scan base-package="com.example"/>
    <mvc:annotation-driven/>
    <bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
        <property name="jdbcUrl" value="jdbc:postgresql://localhost/mydb"/>
        <property name="username" value="user"/>
        <property name="password" value="password"/>
    </bean>
</beans>
```

**Spring Boot (properties):**

```properties
# application.properties — one place, clear and overridable
spring.datasource.url=jdbc:postgresql://localhost/mydb
spring.datasource.username=user
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
```

**Spring Boot (YAML):**

```yaml
# application.yml — hierarchical, readable
spring:
  datasource:
    url: jdbc:postgresql://localhost/mydb
    username: user
    password: password
  jpa:
    hibernate:
      ddl-auto: update
```

---

#### 5. Production-Ready Features (Actuator)

**Why it matters:** Production apps need health checks, metrics, and basic info. Actuator provides endpoints for these.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Example endpoints:**
- `GET /actuator/health` — Is the app up? Is the DB reachable?
- `GET /actuator/metrics` — Memory, request counts, etc.
- `GET /actuator/info` — App name, version, custom info

---

### When to Use Spring Boot vs Raw Spring

| Use Spring Boot when … | Use raw Spring when … |
|------------------------|------------------------|
| Starting a new project | Working on legacy Spring apps |
| Building microservices | Very unusual or non-standard setups |
| You want fast setup | You need maximal control over wiring |
| Convention-over-configuration fits your needs | You're learning Spring internals |
| You want embedded server + standalone JAR | You must deploy to an existing app server |

**Remember:** Spring Boot apps *are* Spring apps. You can use any Spring feature inside Spring Boot.

---

## Layer 3 — Advanced Engineering Depth

### How @SpringBootApplication Works

`@SpringBootApplication` is a **composite annotation** — it groups several annotations:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration   // Same as @Configuration
@EnableAutoConfiguration   // Turns on auto-configuration
@ComponentScan             // Scans for @Component, @Service, @Repository, etc.
public @interface SpringBootApplication {
    // exclude, excludeName for skipping auto-configurations
}
```

**Meaning of each:**
- `@SpringBootConfiguration` — Marks the class as a configuration source (alias for `@Configuration`).
- `@EnableAutoConfiguration` — Enables loading and applying auto-configuration classes from `META-INF/spring.factories`.
- `@ComponentScan` — Scans the package of the annotated class (and sub-packages) for components.

---

### SpringApplication.run() — What Happens Under the Hood

High-level flow:

```text
1. Create SpringApplication instance, infer application type (Servlet/Reactive/None)
2. Load ApplicationContextInitializers and ApplicationListeners
3. Identify main class
4. Run ApplicationContextInitializers
5. Create ApplicationContext (e.g., AnnotationConfigServletWebServerApplicationContext)
6. Prepare context (sources, environment)
7. refresh() — CRITICAL step
   ├── Load bean definitions
   ├── Process @ComponentScan
   ├── Process @EnableAutoConfiguration → load spring.factories
   ├── Evaluate @Conditional* on each auto-config
   ├── Register and create beans
   └── Wire dependencies
8. onRefresh() — Start embedded web server (if applicable)
9. Run ApplicationRunners and CommandLineRunners
10. Publish ApplicationReadyEvent
```

The heavy lifting happens in `refresh()`, especially in `invokeBeanFactoryPostProcessors()`, where auto-configuration is processed.

---

### Auto-Configuration Mechanism in Detail

#### Step 1: spring.factories

Auto-configuration classes are listed in `META-INF/spring.factories` inside `spring-boot-autoconfigure.jar`:

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration,\
org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration,\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
...
```

`AutoConfigurationImportSelector` reads this and proposes which `@Configuration` classes to load.

---

#### Step 2: Conditional Evaluation

Each auto-configuration class is guarded by **conditions**. It is applied only if conditions are met:

```java
@Configuration
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
@ConditionalOnMissingBean(DataSource.class)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties properties) {
        // Created only if:
        // 1. DataSource is on classpath
        // 2. No DataSource bean exists
        // 3. spring.datasource.url is set
        return properties.initializeDataSourceBuilder().build();
    }
}
```

**Common conditions:**

| Condition | Meaning |
|-----------|---------|
| `@ConditionalOnClass` | Class(es) must be on classpath |
| `@ConditionalOnMissingBean` | Bean must not already exist |
| `@ConditionalOnProperty` | Property must be present (and optionally match a value) |
| `@ConditionalOnWebApplication` | Must be a web application |
| `@ConditionalOnMissingClass` | Class must NOT be on classpath |

---

#### Step 3: Ordering

Some auto-configurations depend on others. Order is controlled with:

- `@AutoConfigureOrder` — Global ordering (e.g., `Ordered.HIGHEST_PRECEDENCE`)
- `@AutoConfigureAfter` / `@AutoConfigureBefore` — Relative ordering

```java
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class HibernateJpaAutoConfiguration {
    // Runs after DataSource is configured
}
```

---

#### Step 4: User Beans Override Auto-Configuration

If you define a `DataSource` bean, Spring Boot’s auto-configured `DataSource` is skipped because of `@ConditionalOnMissingBean`. **User definitions win over auto-configuration.**

---

### Inside spring-boot-starter-web

`spring-boot-starter-web` is a small POM that pulls in:

- `spring-boot-starter` (core, context, logging)
- `spring-webmvc`
- `spring-boot-starter-tomcat` (embedded Tomcat)
- `spring-web`
- `spring-boot-starter-json` (Jackson)
- `spring-boot-starter-validation`

That pulls in 50+ transitive dependencies. Versions come from `spring-boot-dependencies` (BOM).

---

### Version Management: BOM

Spring Boot uses a Bill of Materials (BOM):

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

`spring-boot-dependencies` defines versions for Spring, Hibernate, Jackson, Tomcat, etc. You usually don’t specify versions for these — the BOM does.

---

### Debugging Auto-Configuration

Enable the conditions evaluation report:

```properties
# application.properties
debug=true
```

On startup you’ll see something like:

```text
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required class 'javax.sql.DataSource'
      - @ConditionalOnProperty (spring.datasource.url) matched

Negative matches:
-----------------
   HibernateJpaAutoConfiguration:
      - @ConditionalOnClass did not find required class 'org.hibernate.Session'
```

Use this to see why a given auto-configuration was or wasn’t applied.

---

### Custom Auto-Configuration and Starters

**Example auto-configuration:**

```java
@Configuration
@ConditionalOnClass(MyService.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class MyServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MyService myService() {
        return new MyService();
    }
}
```

**Registration in `META-INF/spring.factories`:**

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.MyServiceAutoConfiguration
```

Use for internal libraries, shared services, or company-wide defaults.

---

### Excluding Auto-Configuration

**1. On the main class:**

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Application { }
```

**2. Via properties:**

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

**3. Via YAML:**

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

**4. Per test:**

```java
@SpringBootTest(properties = "spring.autoconfigure.exclude=...")
```

---

### Performance and Edge Cases

**Startup cost:** Auto-configuration evaluates many conditions. For very constrained environments, exclude unneeded auto-configurations to reduce startup time.

**Overriding:** When you define your own bean, it replaces the auto-configured one. Be aware of beans created by `@Configuration` with `@Bean` — they can have different behavior than the default.

**Classpath pollution:** Starters pull in many dependencies. Use `spring-boot-starter-web` with exclusions if you need a smaller footprint (e.g., exclude Tomcat and use Jetty or Undertow).

---

## Layer 4 — Interview Mastery

### 1. What is the difference between Spring and Spring Boot?

**Answer:** Spring Framework is the core: IoC, DI, AOP, web, data access. You configure almost everything manually. Spring Boot sits on top of Spring and adds auto-configuration, starter dependencies, embedded servers, and opinionated defaults. Spring Boot is Spring with less setup—not a replacement.

---

### 2. What does @SpringBootApplication do?

**Answer:** It’s a composite of `@SpringBootConfiguration` (alias for `@Configuration`), `@EnableAutoConfiguration` (loads auto-configurations from `spring.factories`), and `@ComponentScan` (finds components in the package and below). It marks the main application class and enables the default Spring Boot behavior.

---

### 3. How does auto-configuration work?

**Answer:** Spring Boot reads `META-INF/spring.factories` for `EnableAutoConfiguration` entries. For each listed class it evaluates `@Conditional*` annotations (e.g. `@ConditionalOnClass`, `@ConditionalOnProperty`, `@ConditionalOnMissingBean`). If conditions match, the configuration is applied and beans are created. User-defined beans take precedence over auto-configured ones.

---

### 4. Can you use Spring without Spring Boot?

**Answer:** Yes. Use `ClassPathXmlApplicationContext` or `AnnotationConfigApplicationContext` with XML or Java configuration. No Spring Boot dependency required. Common in legacy apps or when you need full control.

---

### 5. What are Spring Boot starters?

**Answer:** Starters are dependency descriptors that bundle related libraries with compatible versions. For example, `spring-boot-starter-web` pulls Spring MVC, Tomcat, Jackson, validation, etc. They simplify dependency management and version alignment via the Spring Boot BOM.

---

### 6. How does Spring Boot choose which auto-configurations to apply?

**Answer:** Through `@Conditional*` annotations. Examples: `@ConditionalOnClass` (class on classpath), `@ConditionalOnProperty` (property set), `@ConditionalOnMissingBean` (no existing bean). Only configurations whose conditions are satisfied are applied. Set `debug=true` to see the report.

---

### 7. Difference between @SpringBootApplication and @EnableAutoConfiguration?

**Answer:** `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`. `@EnableAutoConfiguration` only turns on auto-configuration. Use `@EnableAutoConfiguration` when you compose the pieces yourself or need to exclude specific auto-configurations.

---

### 8. How does Spring Boot manage dependency versions?

**Answer:** Via the `spring-boot-dependencies` BOM in `dependencyManagement`. It defines versions for Spring, Jackson, Hibernate, Tomcat, etc. Starters and your dependencies inherit these versions, reducing conflicts. You can override versions in your POM’s `<properties>` if needed.

---

### 9. What happens when a Spring Boot application starts?

**Answer:** `SpringApplication.run()` creates the application context, runs `refresh()`, loads bean definitions, processes `@ComponentScan` and `@EnableAutoConfiguration`, evaluates conditions, creates beans, wires dependencies, starts the embedded server (if applicable), runs `ApplicationRunner`/`CommandLineRunner`, and publishes `ApplicationReadyEvent`.

---

### 10. How do you exclude auto-configuration?

**Answer:** (1) `@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)`, (2) `@EnableAutoConfiguration(exclude = ...)`, (3) `spring.autoconfigure.exclude` in `application.properties`/`application.yml`, (4) `excludeAutoConfiguration` in `@SpringBootTest`.

---

## Summary

| Concept | One-Liner |
|---------|-----------|
| **Spring Framework** | Core framework for DI, IoC, web, data—you configure it yourself. |
| **Spring Boot** | Spring plus auto-configuration, starters, embedded server, and defaults. |
| **Auto-configuration** | Beans and settings created automatically based on classpath and conditions. |
| **Starters** | Bundles of dependencies with managed versions. |
| **Embedded server** | Web server packaged inside your app; run `java -jar` to serve. |
| **Opinionated defaults** | Sensible defaults you can override when needed. |
| **Key relationship** | Spring Boot is built on Spring; it adds convenience, not a different engine. |

---

[← Back to Index](00-README.md) | [Previous: IoC & DI](12-IoC-DI-DeepDive.md) | [Next: Spring Boot Architecture →](14-SpringBoot-Architecture.md)
