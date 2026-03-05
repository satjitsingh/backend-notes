# Chapter 18: Configuration & Profiles

[← Back to Index](00-README.md) | [Previous: Bean Lifecycle](17-Bean-Lifecycle.md) | [Next: REST API Design →](19-REST-Design.md)

---

## Why This Chapter Matters

Imagine you build a toy robot that only works in your bedroom. When you take it to your friend's house, it breaks because the floor is different. **That's what happens when you put settings (like database addresses and passwords) directly inside your code.** It works on your computer, but when you move the app somewhere else—like a real company's server—everything breaks.

**The golden rule:** Settings should **never** be baked into your code. They should live *outside* the code, like the brightness and volume settings on your phone. You can change them without changing the app itself.

**Profiles** solve another problem: your app needs to behave differently in different places. At home, you might want lots of debug messages and a simple test database. In production (the real world), you want few logs and a powerful database. Profiles let you define all these "modes" and switch between them with one switch—like choosing "School Mode" vs "Party Mode" on your phone.

**Without understanding configuration:**
- You'll hide passwords and secret keys in your code (and sometimes accidentally share them)
- You'll say "it works on my laptop" but it fails everywhere else
- You'll need separate versions of your app for each environment
- Configuration will become messy and hard to manage

**With proper configuration understanding:**
- All settings (database addresses, secrets, feature switches) live outside your code
- Profiles let you manage development, testing, and production environments cleanly
- Secrets stay out of your code and out of version control
- One app runs everywhere, with zero code changes

---

## Layer 1 — Intuition Builder

### Part 1: Configuration Is Like Phone Settings

Think of your phone. It has settings: brightness, volume, WiFi on or off, what language it uses. You can change these **without changing how the phone was built**. The engineers didn't hardcode "brightness = 5" into the phone's circuits; they made it so brightness comes from a *setting* you can adjust.

**Configuration** in a Spring Boot app is exactly like that. Instead of the app saying "connect to database at address X," the app says "connect to wherever the *setting* says." That setting can be different on your laptop, your friend's computer, or a company server—without touching the code.

### Part 2: Profiles Are Like Different Outfits

You don't wear the same clothes to school, to soccer practice, and to a birthday party. Each occasion needs a different outfit.

**Profiles** in Spring Boot are like that. They're different "outfits" your app can wear for different occasions:

- **Dev (development) profile** — Like casual home clothes: a simple in-memory database, lots of debug messages, relaxed rules. Perfect for building and trying things.
- **Test profile** — Like gym clothes: fast, isolated, fake services so tests don't touch real data. Perfect for automated checks.
- **Prod (production) profile** — Like formal clothes: a real database, few logs, strict security. Perfect for real users.

Same app, different configuration for each "occasion."

### Part 3: Where Do Settings Live? (Property Files)

Remember phone settings? They're stored somewhere—in a file or in memory. In Spring Boot, we store settings in **property files**. These are simple files where each line says: "This setting = that value."

Example (in plain English):
```
The server's port number = 8080
The database address = localhost
The app's name = My Cool App
```

We'll see the exact format soon. The idea: **property files are lists of key = value**, stored outside the code.

### Part 4: Who Wins When Settings Conflict?

Imagine three people tell you what time to wake up: Mom says 7 AM, Dad says 6:30 AM, and your alarm says 8 AM. Who wins? You need a rule. In Spring Boot, we have a strict order: **the last (highest priority) source wins**.

Roughly, from highest to lowest priority:

1. **Command line** — Things you type when starting the app (like "use port 9000")
2. **Environment variables** — System-wide variables (like `DATABASE_URL=...`)
3. **Profile-specific files** — Settings for the active profile (e.g., dev, prod)
4. **Main config file** — Default settings for the app
5. **Built-in defaults** — What Spring Boot uses if nobody overrides

So: command line overrides everything. Environment variables override files. Profile files override the main file. **Higher wins over lower.**

### Part 5: Two Ways to Write Settings (Properties vs YAML)

You can write settings in two common formats:

**Format 1: Properties** — Flat list, one setting per line:
```
server.port=8080
app.name=My App
```

**Format 2: YAML** — Indented, like an outline. Grouped by topic:
```yaml
server:
  port: 8080
app:
  name: My App
```

Both do the same thing. Properties are simpler and very common. YAML is nicer when you have lots of nested settings (settings inside settings).

### Part 6: How Does the App Know Which "Outfit" to Wear? (Activating Profiles)

The app needs to know: "Am I in dev, test, or prod?" You tell it by **activating** a profile. You can do that by:

1. **Environment variable** — Set `SPRING_PROFILES_ACTIVE=dev` before starting the app
2. **Command line** — Add `--spring.profiles.active=prod` when you run the app
3. **Config file** — Put `spring.profiles.active=dev` in your main config (but be careful: this might not be ideal for shared code)

Once a profile is active, Spring Boot loads that profile's extra settings (e.g., `application-dev.properties` for the "dev" profile).

### Part 7: Secret Stuff (Passwords and Keys)

You would never write your house key number on a poster for everyone to see. **Secrets** (passwords, API keys) should never live in your code or in files you share. Instead, you use **environment variables**: the system holds the secret, and your config says "use the value from `DB_PASSWORD`." The secret is set on the machine (or in Docker/Kubernetes) and never goes into your project.

### Part 8: Summary in One Sentence

**Configuration** = settings stored outside your code; **profiles** = different sets of settings for different occasions (dev, test, prod). Keep secrets out of code; use environment variables. Higher-priority sources override lower ones.

---

## Layer 2 — Professional Developer

### application.properties vs application.yml

**Why two formats?** Different teams and tools prefer different styles. Spring Boot supports both equally. Choose based on readability and team convention.

#### Properties Format

```properties
# application.properties
# Each line: key=value
# Use dots (.) for hierarchy

# Server settings
server.port=8080

# Database connection (Spring Boot auto-configures from these)
spring.datasource.url=jdbc:mysql://localhost/mydb
spring.datasource.username=admin
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Custom app settings (our own prefix)
app.name=My Application
app.version=1.0.0
app.timeout=5000
```

**Pros:** Simple, flat, widely used. **Cons:** Repetitive for nested settings (e.g., many `spring.datasource.*` lines).

#### YAML Format

```yaml
# application.yml
# Indentation matters! Use spaces, not tabs
# Hierarchy is expressed by indentation

server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost/mydb
    username: admin
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

app:
  name: My Application
  version: 1.0.0
  timeout: 5000
```

**Pros:** Clean hierarchy, less repetition, comments supported. **Cons:** Indentation errors can cause subtle bugs.

#### Side-by-Side Comparison

| Aspect | application.properties | application.yml |
|--------|------------------------|-----------------|
| Structure | Flat (key=value) | Hierarchical (indentation) |
| Nested config | `spring.datasource.url=...` | `spring:\n  datasource:\n    url: ...` |
| Repetition | More (repeated prefix) | Less (shared prefix once) |
| Readability | Good for simple config | Better for complex config |
| Indentation | Not used | Critical (2 spaces typical) |

**Recommendation:** Use YAML for apps with many nested settings; use properties for small, flat configs.

### Property Source Precedence

**Why precedence matters:** Different environments (dev, prod, Docker, Kubernetes) inject values in different ways. You need a predictable order so overrides work correctly.

| Priority | Source | Example |
|----------|--------|---------|
| 1 (highest) | Command line arguments | `--server.port=9000` |
| 2 | JNDI attributes | From application server |
| 3 | Java system properties | `-Dserver.port=8080` |
| 4 | OS environment variables | `SERVER_PORT=8080` |
| 5 | Profile-specific files | `application-prod.properties` |
| 6 | Main config file | `application.properties` |
| 7 | `@PropertySource` | Custom annotated sources |
| 8 (lowest) | Spring Boot defaults | Built-in default values |

**Rule:** Higher priority overrides lower. Command line always wins for the same property.

### Accessing Configuration in Code

#### Method 1: @Value — Simple, Single Values

**When to use:** One-off values, quick prototyping, values that aren't grouped.

```java
@Component
public class AppConfig {

    // Injects the value of server.port (e.g., 8080)
    @Value("${server.port}")
    private int serverPort;

    // Custom property from our config
    @Value("${app.name}")
    private String appName;

    // With default: if app.timeout is missing, use 5000
    @Value("${app.timeout:5000}")
    private int timeout;

    // Boolean with default false
    @Value("${app.feature.enabled:false}")
    private boolean featureEnabled;
}
```

**Pros:** Simple, inline. **Cons:** No validation, no type-safety for complex objects, scattered across classes. Prefer `@ConfigurationProperties` for grouped, production config.

#### Method 2: @ConfigurationProperties — Type-Safe, Validated (Recommended)

**Why use it:** Groups related properties, gives IDE support, supports validation and nested objects. Best for real applications.

```java
// Binds all properties with prefix "app" to this class
@ConfigurationProperties(prefix = "app")
@Component
@Validated  // Enables validation (see Layer 3)
public class AppProperties {

    private String name;
    private String version;
    private int timeout = 5000;  // Default if not in config
    private Feature feature = new Feature();

    // Nested object: maps to app.feature.* in config
    public static class Feature {
        private boolean enabled = false;
        private String apiKey;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    // Getters and setters (required for binding)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public Feature getFeature() { return feature; }
    public void setFeature(Feature feature) { this.feature = feature; }
}
```

**Config (properties or YAML):**
```properties
# application.properties
app.name=My Application
app.version=1.0.0
app.timeout=10000
app.feature.enabled=true
app.feature.api-key=secret-key-123
```

**Usage in a service:**
```java
@Service
public class MyService {
    private final AppProperties properties;

    public MyService(AppProperties properties) {
        this.properties = properties;
    }

    public void doSomething() {
        System.out.println("App: " + properties.getName());
        if (properties.getFeature().isEnabled()) {
            // Use feature with properties.getFeature().getApiKey()
        }
    }
}
```

**Benefits:** Type-safe, IDE autocomplete, validation, relaxed binding (e.g., `api-key` ↔ `apiKey`), nested support.

#### Method 3: Environment Interface — Programmatic Access

**When to use:** Dynamic lookups, conditional reads, or checking if a property exists.

```java
@Component
public class ConfigReader {
    private final Environment environment;

    public ConfigReader(Environment environment) {
        this.environment = environment;
    }

    public void readConfig() {
        // Get string (may be null)
        String port = environment.getProperty("server.port");

        // Get with type and default
        int timeout = environment.getProperty("app.timeout", Integer.class, 5000);
        boolean enabled = environment.getProperty("app.feature.enabled", Boolean.class, false);

        // Check existence before using
        if (environment.containsProperty("app.custom.property")) {
            String value = environment.getProperty("app.custom.property");
        }
    }
}
```

### Externalized Configuration

**Why externalize:** So the same JAR runs everywhere; config changes without recompiling; secrets stay out of the codebase.

**Config file search order** (first match wins for `application.properties` / `application.yml`):

1. `file:./config/` — Directory `config/` next to the JAR
2. `file:./` — Current directory
3. `classpath:/config/` — Inside JAR under `config/`
4. `classpath:/` — Root of classpath (e.g., `src/main/resources/`)

**Override search path:**
```bash
# Point to external config directory
java -jar app.jar --spring.config.location=file:/etc/myapp/

# Multiple locations (comma-separated)
java -jar app.jar --spring.config.location=classpath:/,file:./config/
```

### Spring Profiles (dev, test, prod)

**Why profiles:** One codebase, many environments. Profiles load different config and beans per environment.

#### Profile-Specific Config Files

Naming: `application-{profile}.properties` or `application-{profile}.yml`.

```
src/main/resources/
├── application.properties      # Base (always loaded)
├── application-dev.properties  # When profile "dev" is active
├── application-test.properties # When profile "test" is active
└── application-prod.properties # When profile "prod" is active
```

**Example: application-dev.properties**
```properties
# Development: in-memory DB, verbose logging
spring.datasource.url=jdbc:h2:mem:devdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

logging.level.root=DEBUG
logging.level.com.example=DEBUG

app.environment=development
```

**Example: application-prod.properties**
```properties
# Production: real DB, minimal logging, secrets from env vars
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

logging.level.root=INFO
logging.level.com.example=WARN

app.environment=production
```

**Example: application-test.properties**
```properties
# Test: isolated, fast
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

logging.level.root=WARN
app.environment=test
```

#### Activating Profiles

| Method | Example |
|--------|---------|
| Command line | `java -jar app.jar --spring.profiles.active=prod` |
| Environment variable | `SPRING_PROFILES_ACTIVE=prod` |
| System property | `-Dspring.profiles.active=prod` |
| In config file | `spring.profiles.active=dev` (avoid for env-specific default) |
| Programmatic | `app.setAdditionalProfiles("prod");` |

**Multiple profiles:**
```bash
java -jar app.jar --spring.profiles.active=prod,monitoring
```

#### Profile-Specific Beans: @Profile

**Why:** Some beans (e.g., DataSource, mail sender) must differ per environment. Use `@Profile` so Spring creates only the bean for the active profile.

```java
@Configuration
public class DatabaseConfig {

    @Profile("dev")
    @Bean
    public DataSource devDataSource() {
        // In-memory H2 for development
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }

    @Profile("prod")
    @Bean
    public DataSource prodDataSource() {
        // Real connection pool for production
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(env.getProperty("spring.datasource.url"));
        return ds;
    }

    @Profile("test")
    @Bean
    public DataSource testDataSource() {
        // Isolated test DB
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .build();
    }
}
```

**Profile expressions:**
```java
@Profile("dev")              // Only when "dev" is active
@Profile("!prod")            // When "prod" is NOT active
@Profile({"dev", "test"})    // "dev" OR "test"
@Profile("prod & monitoring") // "prod" AND "monitoring"
```

#### Profile-Specific YAML (document separators)

```yaml
# application.yml — default section
server:
  port: 8080

spring:
  profiles:
    active: dev

---
# Document for dev profile (Spring Boot 2.4+)
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:devdb
    driver-class-name: org.h2.Driver

logging:
  level:
    root: DEBUG

---
# Document for prod profile
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

logging:
  level:
    root: INFO
```

### Environment Variables and Command Line Args

**Why use them:** Containers (Docker, Kubernetes) and CI/CD inject values via env vars and args. No secrets in config files.

**Mapping:** Spring Boot maps `UPPER_SNAKE_CASE` env vars to `lower.kebab-case` properties:
- `SERVER_PORT` → `server.port`
- `SPRING_DATASOURCE_URL` → `spring.datasource.url`

**Placeholders with defaults:**
```properties
# If DB_URL is set, use it; otherwise use the default
spring.datasource.url=${DB_URL:jdbc:h2:mem:defaultdb}
spring.datasource.username=${DB_USERNAME:sa}
spring.datasource.password=${DB_PASSWORD:}
```

### Secrets Management Basics

**Rule:** Never commit secrets. Use external mechanisms.

1. **Environment variables:** `spring.datasource.password=${DB_PASSWORD}` — set `DB_PASSWORD` on the host/container.
2. **External config:** Keep `application-secrets.properties` outside the repo; load via `--spring.config.location`.
3. **`.gitignore`:** Add `application-local.properties`, `*-secrets.properties`, etc.
4. **Secret managers:** Tools like HashiCorp Vault, AWS Secrets Manager, or Kubernetes Secrets inject values (typically via env vars or files).

**Example (safe pattern):**
```properties
# Committed — no secrets, only placeholders
spring.datasource.url=${DB_URL:jdbc:h2:mem:defaultdb}
spring.datasource.username=${DB_USERNAME:sa}
spring.datasource.password=${DB_PASSWORD}
```

### Practical Multi-Environment Example

**Directory layout:**
```
project/
├── src/main/resources/
│   ├── application.properties      # Shared defaults
│   ├── application-dev.properties
│   ├── application-test.properties
│   └── application-prod.properties
└── config/                          # Optional external (near JAR)
    └── application-prod.properties  # Override for production
```

**application.properties (committed, no secrets):**
```properties
app.name=My Application
spring.profiles.active=dev
# Dev is default for local runs; prod set by env/args in deployment
```

**Running:**
```bash
# Local dev (uses dev profile by default)
./mvnw spring-boot:run

# Production (profile + env vars)
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://prod-db:3306/mydb
export DB_USERNAME=appuser
export DB_PASSWORD=secret
java -jar app.jar
```

---

## Layer 3 — Advanced Engineering Depth

### @ConfigurationProperties Advanced Features

#### Constructor Binding (Immutable Properties)

**Why:** Immutable config objects are safer in concurrent code and align with functional style. Spring Boot 2.2+ supports constructor binding.

```java
@ConfigurationProperties(prefix = "app")
@ConstructorBinding  // Spring Boot 2.2–2.3; in 2.4+ use @ConfigurationProperties without it
public record AppProperties(
        String name,
        int timeout,
        Feature feature
) {
    public record Feature(boolean enabled, String apiKey) {}
}
```

For non-record classes, use a constructor and omit setters. Requires `@EnableConfigurationProperties(AppProperties.class)` or `@ConfigurationPropertiesScan`.

#### Relaxed Binding

**Behavior:** Spring accepts multiple property name formats and maps them to the same Java property.

| Property file | Java field |
|---------------|------------|
| `app.feature.apiKey` | `apiKey` |
| `app.feature.api-key` | `apiKey` |
| `app.feature.api_key` | `apiKey` |
| `app.feature.API_KEY` | `apiKey` |

**Best practice:** Use kebab-case in config (`api-key`); use camelCase in Java (`apiKey`).

#### Map and List Properties

**Properties:**
```properties
app.servers[0]=server1.example.com
app.servers[1]=server2.example.com
app.config.database.url=jdbc:mysql://localhost/db
app.config.cache.ttl=3600
```

**YAML (clearer):**
```yaml
app:
  servers:
    - server1.example.com
    - server2.example.com
  config:
    database:
      url: jdbc:mysql://localhost/db
    cache:
      ttl: 3600
      max-size: 1000
```

```java
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private List<String> servers = new ArrayList<>();
    private Map<String, Map<String, Object>> config = new HashMap<>();
    // getters/setters
}
```

#### Custom Converters

```java
@ConfigurationPropertiesBinding
@Component
public class StringToDurationConverter implements Converter<String, Duration> {
    @Override
    public Duration convert(String source) {
        return Duration.parse(source);  // e.g., "PT30S" = 30 seconds
    }
}
```

```properties
app.timeout=PT30S
```

### Profile Groups

**Why:** Avoid repeating `--spring.profiles.active=prod,monitoring,security` everywhere. Define a group once.

```properties
# application.properties
spring.profiles.group.production=prod,monitoring,security
spring.profiles.group.development=dev,debug
```

```bash
# Activates prod, monitoring, security
java -jar app.jar --spring.profiles.active=production
```

### Config Server and Secrets (Spring Cloud / Vault)

**Spring Cloud Config:** Central server holds config files; apps fetch config at startup (and optionally refresh). Good for many services sharing config.

**HashiCorp Vault:** Stores secrets; Spring Cloud Vault can inject them as properties. Provides rotation and audit logs.

**Kubernetes:** ConfigMaps for non-sensitive config; Secrets for passwords/keys, often mounted as env vars or files.

### Property Encryption (Jasypt)

**Use case:** Config files are in version control, but some values must be encrypted.

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

```properties
jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD}
spring.datasource.password=ENC(encrypted-value-here)
```

**Edge case:** The Jasypt password must be provided securely (env var), not in config. If it's compromised, encrypted values can be decrypted.

### Custom Property Sources

**EnvironmentPostProcessor** — Add property sources before the app context is fully initialized:

```java
public class CustomEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                      SpringApplication application) {
        // Load from DB, API, or custom file format
        Properties props = loadFromExternalSource();
        environment.getPropertySources()
                .addFirst(new PropertiesPropertySource("custom", props));
    }
}
```

Register in `META-INF/spring.factories`:
```
org.springframework.boot.env.EnvironmentPostProcessor=com.example.CustomEnvironmentPostProcessor
```

### Configuration Best Practices

1. **Externalize all config** — No hardcoded URLs, ports, or secrets.
2. **Prefer @ConfigurationProperties** — Over `@Value` for grouped, production config.
3. **Validate** — Use `@Validated` and Bean Validation so invalid config fails fast.
4. **Use profiles** — Separate dev, test, prod (and staging if needed).
5. **Never commit secrets** — Use `${VAR}` placeholders and set vars in the environment.
6. **Document properties** — Javadoc and optional `spring-configuration-metadata.json` for IDE support.

---

## Layer 4 — Interview Mastery

### Q1: What is the order of property source precedence in Spring Boot?

**Answer:** From highest to lowest: (1) command line arguments, (2) JNDI, (3) Java system properties, (4) OS environment variables, (5) profile-specific files, (6) main application config, (7) `@PropertySource`, (8) Spring Boot defaults. Higher overrides lower.

### Q2: How do you activate a profile?

**Answer:** (1) `--spring.profiles.active=prod` on the command line, (2) `SPRING_PROFILES_ACTIVE=prod` environment variable, (3) `spring.profiles.active=prod` in config, or (4) `app.setAdditionalProfiles("prod")` in code. For production, use command line or environment variable.

### Q3: @Value vs @ConfigurationProperties — when to use which?

**Answer:** `@Value` for single, unrelated properties and quick experiments. `@ConfigurationProperties` for grouped config, validation, nested objects, and relaxed binding. Prefer `@ConfigurationProperties` in production.

### Q4: How do you handle secrets in Spring Boot?

**Answer:** Never put secrets in code or committed config. Use `spring.datasource.password=${DB_PASSWORD}` and set `DB_PASSWORD` via environment variable. For more control, use external config, secret managers (Vault, AWS Secrets Manager), or Kubernetes Secrets.

### Q5: What is relaxed binding?

**Answer:** Spring allows multiple property name styles (camelCase, kebab-case, snake_case) and maps them to the same Java property. Example: `api-key`, `api_key`, and `apiKey` all bind to `apiKey`.

### Q6: How do you validate configuration properties?

**Answer:** Add `@Validated` to the class and use Bean Validation (`@NotBlank`, `@Min`, `@Max`, `@Email`, etc.). Include `spring-boot-starter-validation`. Invalid config causes startup failure with clear errors.

### Q7: What are profile groups?

**Answer:** Named groups of profiles. Define `spring.profiles.group.production=prod,monitoring,security`. Activating `production` activates all three. Reduces repetition when using multiple profiles.

### Q8: How do you debug configuration issues?

**Answer:** (1) Run with `--debug` to see auto-config and conditions, (2) Expose `env` and `configprops` Actuator endpoints and inspect values, (3) Log `Environment` property sources and active profiles, (4) Use `@Validated` to catch invalid config at startup.

### Q9: Where does Spring Boot look for application.properties?

**Answer:** In order: `file:./config/`, `file:./`, `classpath:/config/`, `classpath:/`. Can override with `--spring.config.location` or `--spring.config.additional-location`.

### Q10: What is externalized configuration?

**Answer:** Keeping configuration outside the code—in files, env vars, or other sources. Same JAR runs in dev, test, prod by changing only config; no recompilation. Essential for 12-factor apps and cloud deployments.

---

## Summary

| Concept | One-Line Explanation |
|---------|------------------------|
| **Configuration** | Settings outside code; change behavior without recompiling |
| **Profiles** | Named sets of config/beans for dev, test, prod |
| **Property precedence** | Command line > env vars > profile files > main config > defaults |
| **@Value** | Simple injection for single properties |
| **@ConfigurationProperties** | Type-safe, validated binding for grouped config |
| **Externalized config** | Config in files/env, not in code |
| **Secrets** | Never in code/config; use env vars or secret managers |
| **Relaxed binding** | Multiple property name formats map to same Java property |
| **Profile groups** | Shorthand for activating multiple profiles |

**Golden rules:** (1) Externalize all config. (2) Use profiles for environments. (3) Never commit secrets. (4) Prefer `@ConfigurationProperties` over `@Value` for production config.

---

[← Back to Index](00-README.md) | [Previous: Bean Lifecycle](17-Bean-Lifecycle.md) | [Next: REST API Design →](19-REST-Design.md)
