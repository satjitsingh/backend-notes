# Chapter 9: JSON & Serialization (Prerequisites)

[← Back to Index](00-README.md) | [Previous: Build Tools](08-Prerequisites-Build-Tools.md) | [Next: Testing Fundamentals →](10-Prerequisites-Testing.md)

---

## Why This Chapter Matters

When you build a Spring Boot app that talks to phones, browsers, or other apps, everyone needs to agree on how to send and receive data. JSON is that agreed-upon format, and serialization is how your Java objects get turned into that format (and back again).

Without understanding JSON and serialization:

- You'll struggle with mismatches between what your API sends and what clients expect
- Date and time values will break in confusing ways
- Circular references can crash your app
- You'll miss common interview questions about how JSON works in Java
- API changes may break existing clients

---

## Layer 1 — Intuition Builder

Think of everything here as if you're explaining to someone who has never heard of JSON or serialization. We'll use simple analogies first, then connect them to the real code.

---

### Part 1: What is JSON?

Imagine you send a letter to a friend. You could write it in your own secret code, but then your friend wouldn't understand it. Instead, you both agree to use plain text, maybe the same language, so anyone who can read can understand the letter.

**JSON (JavaScript Object Notation)** is like that agreed-upon letter format for computers. It's a simple, text-based way to describe data so that:

- Java programs can write it
- Python programs can read it
- Browsers can understand it
- Phones and tablets can use it

It's not tied to any one programming language. That's why you see JSON everywhere: websites, mobile apps, REST APIs, and configuration files.

**In plain English:** JSON is a standard way to write down information (names, numbers, lists, etc.) so different programs can exchange it easily.

---

### Part 2: What Does JSON Look Like?

JSON uses only a few symbols. Here's a simple example:

```json
{
  "name": "Alex",
  "age": 10,
  "likesPizza": true,
  "favoriteNumbers": [3, 7, 11]
}
```

- **Curly braces `{ }`** — wrap a collection of named things (like a person)
- **Square brackets `[ ]`** — wrap a list of things in order (like favorite numbers)
- **Colon `:`** — separates a label from its value (e.g. `"name"` : `"Alex"`)
- **Comma `,`** — separates items in a list or object

**Rules that matter (and trip up beginners):**

1. Labels (keys) must always be in **double quotes** — `"name"` is valid, `name` is not.
2. Strings must be in **double quotes** — `"hello"` is valid, `'hello'` is not.
3. No **trailing comma** after the last item.
4. No **comments** — you can't add notes like `// this is a comment` in JSON.

---

### Part 3: The Six Kinds of Data JSON Can Hold

JSON has six types of values. You can think of them as six different kinds of boxes you can put information in:

| JSON Type | Example | Plain-English Meaning |
|-----------|---------|------------------------|
| **String** | `"Hello"` | Text in quotes |
| **Number** | `42` or `3.14` | Whole or decimal numbers (no quotes) |
| **Boolean** | `true` or `false` | Yes or no (no quotes) |
| **Null** | `null` | "Nothing here" or "empty" |
| **Array** | `[1, 2, 3]` or `["a", "b"]` | An ordered list |
| **Object** | `{"key": "value"}` | A labeled bundle of values |

**Complete example mixing all types:**

```json
{
  "name": "Sam",
  "age": 25,
  "isStudent": false,
  "email": null,
  "grades": [88, 92, 95],
  "address": {
    "city": "Boston",
    "zip": "02101"
  }
}
```

---

### Part 4: JSON vs XML — Why JSON Won for APIs

Before JSON was popular, many systems used **XML** (another text format with tags like `<name>John</name>`). XML is fine for documents and complex structures, but for simple data exchange, it's wordy and harder to read.

Here's the same information in both formats:

**XML:**

```xml
<user>
  <name>John</name>
  <age>30</age>
  <address>
    <city>New York</city>
  </address>
</user>
```

**JSON (same data):**

```json
{
  "name": "John",
  "age": 30,
  "address": {
    "city": "New York"
  }
}
```

| Aspect | JSON | XML |
|--------|------|-----|
| **Readability** | Easier | More verbose |
| **Size** | Smaller | Larger |
| **Parsing** | Faster | Slower |
| **Modern APIs** | Used everywhere | Mostly legacy |
| **Browser support** | Built in (`JSON.parse`) | Needs extra libraries |

**Takeaway:** For REST APIs and modern apps, JSON is the standard. Understanding it is essential.

---

### Part 5: What is Serialization? (Packing a Suitcase)

**Serialization** = turning something in your program (like a Java object) into a format that can be stored or sent over a network (like JSON).  
**Deserialization** = turning that format back into an object your program can use.

**Analogy:** Packing a suitcase.

- **Serialization:** You pack your clothes (object) into a suitcase (JSON) so you can travel.
- **Deserialization:** You unpack (deserialize) when you arrive so you can use the clothes again.

**Why it matters for APIs:**

Without serialization, your Java object lives only in memory. You can't send it to a browser or another service. With serialization:

1. Your Java object gets converted to a JSON string (serialization).
2. That string is sent over HTTP.
3. The receiver converts the JSON back into their own object (deserialization).

```
Java Object (in memory)
        ↓
   Serialization
        ↓
   JSON string
        ↓
   HTTP request/response
        ↓
   Deserialization
        ↓
Object in another language (e.g. JavaScript)
```

---

### Part 6: Why Dates and Names Cause Trouble

**Dates:**  
If you store a date as a raw number (e.g. `1704567890123`), most developers and frontends don't know what it means. They expect something like `"2024-01-05T10:30:00Z"` (a readable, standard format). Understanding how to control date formats saves hours of debugging.

**Names:**  
Java likes `firstName` (camelCase). Some frontends or APIs expect `first_name` (snake_case). If the names don't match, data may not map correctly. You need a way to say "this Java field is actually this JSON key."

**Circular references:**  
If User has a list of Orders, and each Order points back to User, serializing can lead to an infinite loop and a crash. You must know how to break that loop.

---

## Layer 2 — Professional Developer

Here we get technical, but we still explain *why* before *how*. Every code example includes comments.

---

### Jackson: Spring Boot's Default JSON Engine

**What is Jackson?**  
Jackson is a Java library that converts between Java objects and JSON. Spring Boot uses it automatically whenever you return objects from a controller or accept JSON in request bodies.

**Why Jackson?** It's fast, widely used, and supports advanced cases (custom formats, inheritance, etc.).

**Core class: ObjectMapper**

`ObjectMapper` is the main class that does the conversion. You rarely use it directly in controllers (Spring does that for you), but understanding it helps when you need custom behavior.

```java
import com.fasterxml.jackson.databind.ObjectMapper;

// Create the converter (Spring usually provides this for you)
ObjectMapper mapper = new ObjectMapper();

// Serialization: Java object → JSON string
User user = new User("John", 30);
String json = mapper.writeValueAsString(user);
// Result: {"name":"John","age":30}

// Deserialization: JSON string → Java object
String json = "{\"name\":\"John\",\"age\":30}";
User user = mapper.readValue(json, User.class);
// Result: User object with name="John", age=30
```

---

### How Jackson Finds Properties (Why Getters/Setters Matter)

Jackson discovers what to serialize by looking at **getters** (like `getName()`) and **setters** (like `setName(String name)`). If a field has no getter, it won't be included in the JSON. If it has no setter, it won't be set during deserialization.

```java
public class User {
    private String name;
    private String email;

    // Jackson uses these to read values for serialization
    public String getName() { return name; }
    public String getEmail() { return email; }

    // Jackson uses these to write values during deserialization
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
}
```

---

### Essential Jackson Annotations

#### @JsonProperty — Map Java Names to JSON Names

**Use when:** The JSON key differs from the Java field name (e.g. snake_case vs camelCase).

```java
public class User {
    // Java uses camelCase
    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("user_age")
    private Integer age;
}
```

**Resulting JSON:**
```json
{
  "first_name": "John",
  "user_age": 30
}
```

---

#### @JsonIgnore — Hide a Field from JSON

**Use when:** You must never expose a field in the API (passwords, internal IDs, etc.).

```java
public class User {
    private String name;
    private String email;

    // Never include password in API responses or accept it from requests
    @JsonIgnore
    private String password;

    @JsonIgnore
    private String internalId;
}
```

---

#### @JsonInclude — Exclude Null or Empty Values

**Use when:** You want cleaner JSON by omitting nulls or empty collections.

```java
// Apply to the whole class
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    private String name;
    private String email;  // If null, this field is omitted from JSON
    private Integer age;   // If null, omitted
}
```

**Options:**

| Option | Meaning |
|--------|---------|
| `NON_NULL` | Skip fields that are null |
| `NON_EMPTY` | Skip null, empty strings, empty lists |
| `NON_DEFAULT` | Skip default values (0, false, etc.) |
| `ALWAYS` | Include everything (default) |

---

#### @JsonFormat — Control Date and Time Format

**Use when:** You need a specific date/time string format (e.g. ISO 8601 for APIs).

```java
public class Order {
    // Use ISO 8601 string instead of timestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdAt;

    // Custom format for date-only field
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    // Legacy Date type with custom format and timezone
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date updatedAt;
}
```

**Important:** Prefer ISO 8601 (e.g. `"2024-01-05T10:30:00Z"`) for APIs. It's a standard and works across languages.

---

### JSON Types Mapped to Java

| JSON Type | Java Type |
|-----------|-----------|
| String | `String` |
| Number | `int`, `Integer`, `long`, `Long`, `double`, `BigDecimal` |
| Boolean | `boolean`, `Boolean` |
| Null | `null` |
| Array | `List<T>`, `Set<T>`, `T[]` |
| Object | Any Java class (POJO) |

---

### Handling Dates in Spring Boot

**Problem:** By default, `java.util.Date` and older types often serialize as timestamps (numbers), which most frontends can't interpret well.

**Solution:** Use `LocalDateTime`, `LocalDate` and `@JsonFormat`, and register the Java Time module.

```java
// Register Java 8 date/time support ( Jackson does this when JavaTimeModule is present )
ObjectMapper mapper = JsonMapper.builder()
    .addModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build();

// On your class:
public class Order {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdAt;  // "2024-01-05T10:30:00"
}
```

**Via application.properties:**
```properties
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.time-zone=UTC
```

---

### Nested Objects and Arrays

**Nested object:**

```java
public class Address {
    private String street;
    private String city;
    // getters and setters
}

public class User {
    private String name;
    private Address address;  // Nested object
}
```

**JSON:**
```json
{
  "name": "John",
  "address": {
    "street": "123 Main St",
    "city": "Boston"
  }
}
```

**Array of objects:**

```java
public class User {
    private String name;
    private List<String> tags;       // ["java", "spring"]
    private List<Order> orders;       // Array of objects
}
```

**JSON:**
```json
{
  "name": "John",
  "tags": ["java", "spring"],
  "orders": [
    {"id": 1, "total": 99.99},
    {"id": 2, "total": 49.99}
  ]
}
```

---

### Common Jackson Annotations Reference

| Annotation | Purpose |
|------------|---------|
| `@JsonProperty("name")` | Use a different JSON key |
| `@JsonIgnore` | Exclude from serialization and deserialization |
| `@JsonInclude(NON_NULL)` | Exclude null (or empty) values |
| `@JsonFormat(...)` | Custom date format or shape |
| `@JsonManagedReference` | "Forward" side of circular reference |
| `@JsonBackReference` | "Back" side (omitted from JSON) |

---

### Common Errors and Fixes

**1. Date shows as number (e.g. 1704567890123)**  
- **Fix:** Use `@JsonFormat(shape = JsonFormat.Shape.STRING)` or `LocalDateTime` + `write-dates-as-timestamps=false`.

**2. Field name mismatch (Java `firstName` vs JSON `first_name`)**  
- **Fix:** Add `@JsonProperty("first_name")` on the Java field.

**3. Password or sensitive data in response**  
- **Fix:** Add `@JsonIgnore` on the field.

**4. null values cluttering JSON**  
- **Fix:** Add `@JsonInclude(JsonInclude.Include.NON_NULL)` on the class.

**5. Infinite loop / StackOverflowError**  
- **Fix:** Break circular references with `@JsonManagedReference` and `@JsonBackReference`, or use DTOs that don’t reference each other in a loop.

---

## Layer 3 — Advanced Engineering Depth

This section goes deeper: Jackson internals, performance, edge cases, and security.

---

### How Jackson Discovers Properties

Jackson uses reflection to find:

1. Getters (`getXxx()`, `isXxx()` for booleans)
2. Setters (`setXxx`)
3. Annotations like `@JsonProperty` to override names

You can change this behavior:

```java
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .build();
    }
}
```

---

### Property Naming Strategies

You can apply a naming strategy globally instead of annotating each field:

```java
@Bean
public ObjectMapper objectMapper() {
    return JsonMapper.builder()
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .build();
}
// Java: userName → JSON: "user_name"
```

Options: `SNAKE_CASE`, `KEBAB_CASE`, `LOWER_CASE`, etc.

---

### Circular References and Solutions

**Problem:** `User` has `List<Order>`, and `Order` has `User`. Serializing leads to recursion and `StackOverflowError`.

**Solution 1: @JsonManagedReference / @JsonBackReference**

```java
public class User {
    @OneToMany(mappedBy = "user")
    @JsonManagedReference  // Serialize this side
    private List<Order> orders;
}

public class Order {
    @ManyToOne
    @JsonBackReference    // Skip serializing this side to break the loop
    private User user;
}
```

**Solution 2: DTO pattern (often preferred)**  
Create separate DTO classes that don’t have circular references. Convert entities to DTOs before returning them in your API.

---

### Lazy-Loaded Collections (Hibernate)

**Problem:** With `FetchType.LAZY`, collections may not be loaded when Jackson serializes. Jackson might trigger lazy loading outside a transaction, causing `LazyInitializationException`.

**Solutions:**

- Use DTOs and explicitly load only what you need.
- Or annotate lazy fields with `@JsonIgnore` if you don’t want to expose them.
- Avoid exposing entities directly in REST responses; prefer DTOs.

---

### Custom Serializers and Deserializers

When annotations aren’t enough, implement custom logic:

```java
public class CustomDateSerializer extends JsonSerializer<Date> {
    private static final DateTimeFormatter FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneId.of("UTC"));

    @Override
    public void serialize(Date date, JsonGenerator gen,
                         SerializerProvider serializers) throws IOException {
        gen.writeString(Instant.ofEpochMilli(date.getTime()).toString());
    }
}

// Usage
public class Order {
    @JsonSerialize(using = CustomDateSerializer.class)
    private Date createdAt;
}
```

---

### Polymorphic Types (@JsonTypeInfo)

When you have inheritance and need to deserialize to the correct subclass:

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Dog.class, name = "dog"),
    @JsonSubTypes.Type(value = Cat.class, name = "cat")
})
public abstract class Animal {
    private String name;
}
```

**JSON:**
```json
{
  "type": "dog",
  "name": "Buddy",
  "breed": "Golden Retriever"
}
```

**Security:** Prefer `NAME` with an explicit whitelist of subtypes. Avoid `CLASS` or putting class names in JSON (security risk).

---

### Performance Tips

1. **Reuse ObjectMapper** — It’s thread-safe. Don’t create a new one per request.
2. **Inject Spring’s ObjectMapper** — Let Spring configure and manage it.
3. **Use streaming for very large payloads** — `JsonGenerator` instead of building the whole object in memory.
4. **Use `@JsonCreator` for immutable objects** — Reduces reflection overhead.

---

### Security Considerations

- **Polymorphic deserialization:** `@JsonTypeInfo` with `Id.CLASS` can be abused. Prefer `Id.NAME` and a fixed whitelist.
- **Large payloads:** Limit request size (e.g. `spring.servlet.multipart.max-request-size`) to avoid DoS.
- **Unknown properties:** `FAIL_ON_UNKNOWN_PROPERTIES` can help catch unexpected or malicious fields.
- **Use DTOs, not entities** — Don’t deserialize directly into JPA entities; validate and map to DTOs first.

---

## Layer 4 — Interview Mastery

Concise Q&A for common interview questions.

---

### Q1: What is JSON, and why is it used in REST APIs?

**Answer:** JSON (JavaScript Object Notation) is a text format for representing data (strings, numbers, booleans, arrays, objects). It’s used in REST APIs because it’s readable, compact, language-independent, and easy to parse in almost every language.

---

### Q2: What is serialization? What is deserialization?

**Answer:**  
- **Serialization:** Converting a Java object into a storable/transmittable format (e.g. JSON).  
- **Deserialization:** Converting that format back into a Java object.

Both are needed so data can cross process boundaries (HTTP, files, queues).

---

### Q3: Which library does Spring Boot use for JSON?

**Answer:** Jackson. It’s included in `spring-boot-starter-web` and configured by `JacksonAutoConfiguration`. `@RestController` and `@RequestBody` use Jackson for serialization and deserialization.

---

### Q4: What does ObjectMapper do?

**Answer:** `ObjectMapper` is Jackson’s main class. It converts Java objects to JSON (`writeValueAsString`) and JSON to Java objects (`readValue`). Spring Boot injects a preconfigured `ObjectMapper` bean.

---

### Q5: How do you change the JSON field name for a Java property?

**Answer:** Use `@JsonProperty("json_field_name")` on the Java field. For example: `@JsonProperty("first_name")` on `firstName`.

---

### Q6: How do you exclude a field from JSON (e.g. password)?

**Answer:** Add `@JsonIgnore` on the field. It will be omitted from both serialization and deserialization.

---

### Q7: How do you handle dates in JSON?

**Answer:** Use ISO 8601 strings. With Jackson:

- Add `JavaTimeModule` and disable `WRITE_DATES_AS_TIMESTAMPS`.
- Use `LocalDateTime`, `LocalDate` where possible.
- Use `@JsonFormat(shape = JsonFormat.Shape.STRING)` or a custom pattern if needed.
- Configure timezone (e.g. UTC) explicitly.

---

### Q8: What are circular references, and how do you fix them?

**Answer:** Circular references occur when A references B and B references A. Serializing can cause infinite recursion and `StackOverflowError`.

**Fixes:**

1. `@JsonManagedReference` on the “parent” collection.
2. `@JsonBackReference` on the “child” back-reference (omitted from JSON).
3. Or use DTOs that avoid circular references.

---

### Q9: What is @JsonInclude used for?

**Answer:** It controls when a property is included in the JSON output. For example, `@JsonInclude(JsonInclude.Include.NON_NULL)` excludes fields that are null.

---

### Q10: How do you make JSON serialization backward compatible?

**Answer:**

- Avoid removing or renaming fields; add new ones instead.
- Use `@JsonProperty` to keep old names where needed.
- Add new fields only; support both old and new clients during transition.
- For major changes, consider API versioning (e.g. `/api/v1/`, `/api/v2/`).

---

### Q11: Jackson vs Gson?

**Answer:** Both handle JSON in Java. Jackson is faster, more feature-rich, and is Spring Boot’s default. Gson is simpler and common on Android. For Spring Boot, use Jackson unless you have a specific reason for Gson.

---

### Q12: What are deserialization security risks?

**Answer:**

- Polymorphic deserialization can be exploited if arbitrary class names are allowed.
- Very large payloads can cause DoS.
- Deserializing untrusted input without validation is risky.

**Mitigations:** Use a whitelist of types, limit payload size, validate input, use DTOs, and avoid class-name-based polymorphism.

---

## Summary

- **JSON** is a standard text format for data exchange. It supports strings, numbers, booleans, null, arrays, and objects.
- **Serialization** turns Java objects into JSON; **deserialization** turns JSON back into objects.
- Spring Boot uses **Jackson** and its **ObjectMapper** for JSON. `@JsonProperty`, `@JsonIgnore`, `@JsonInclude`, and `@JsonFormat` control how objects are serialized and deserialized.
- **Dates** should use ISO 8601 format. Configure Jackson (e.g. `write-dates-as-timestamps=false`, `JavaTimeModule`) and `@JsonFormat` as needed.
- **Circular references** cause infinite recursion. Use `@JsonManagedReference` / `@JsonBackReference` or DTOs to avoid them.
- **Best practices:** Use DTOs for APIs, reuse `ObjectMapper`, configure naming and date formats globally when possible, and avoid exposing sensitive or internal fields.

---

[← Back to Index](00-README.md) | [Previous: Build Tools](08-Prerequisites-Build-Tools.md) | [Next: Testing Fundamentals →](10-Prerequisites-Testing.md)
