# Chapter 23: DTO Patterns & Mapping

[← Back to Index](00-README.md) | [Previous: Exception Handling](22-Exception-Handling.md) | [Next: Spring Data JPA →](24-Spring-Data-JPA.md)

---

## Why This Chapter Matters

Imagine your `User` entity has a `password` field. If you return the entity directly from your API, every API response includes the user's password hash. Congratulations, you've created a security vulnerability.

This happens ALL the time when developers skip DTOs. The fix? **Never return entities directly from your API.** Use DTOs — separate objects designed specifically for data transfer.

DTOs vs Entities is one of the first things a code reviewer checks. In interviews, if you return entities from controllers, it's an immediate red flag. This chapter teaches you the professional way to handle data between layers.

**Without understanding DTOs:**
- You'll accidentally expose passwords, SSNs, and internal IDs in API responses
- You'll face `LazyInitializationException` when Jackson tries to serialize lazy-loaded associations
- You'll create tight coupling — changing a database column breaks your API contract
- You'll over-fetch data (sending 50 fields when the client needs 5)
- You'll struggle with API versioning (how do you add fields without breaking v1 clients?)

**With proper DTO understanding:**
- You'll build secure APIs that only expose what clients need
- You'll separate concerns cleanly (database structure != API structure)
- You'll handle API versioning gracefully (v1 and v2 DTOs, same entity)
- You'll optimize data transfer and eliminate lazy loading issues
- You'll follow the pattern used by every professional Spring Boot project

---

## Layer 1 — Intuition Builder

### Part 1: The Restaurant Analogy

**Think of a restaurant.**

When you go to a restaurant, do you walk into the kitchen and see:
- The secret recipe book?
- The inventory list?
- The chef's notes?
- Raw ingredients in the freezer?

**No!** You see a **menu**.

The menu shows only what you need to know: the dish name, a nice description, maybe a picture, and the price. The kitchen keeps its secret recipes, inventory systems, and internal processes hidden.

**In your app:**
- **Entity** = The kitchen's internal world. The full recipe, every ingredient, every step. Everything the database stores.
- **DTO** = The menu card. Only what the customer (your API client) should see. Nothing more.

**Why does this matter?** Because if you hand the customer the chef's recipe book instead of the menu, they'll see secrets they shouldn't know — like your secret sauce formula (or in code: passwords, internal IDs, and private data).

---

### Part 2: What Happens When You Don't Use a "Menu" (DTO)?

Imagine you run a lemonade stand. You keep a notebook with:
- How many lemons you have
- Your secret sugar ratio
- Your mom's phone number (for emergencies)
- How much money you've made today

A customer asks: "What lemonade do you sell and how much does it cost?"

**Wrong answer:** Hand them your entire notebook. They see your secret recipe, your mom's number, and your profits. That's bad!

**Right answer:** Give them a small card that says "Lemonade - $2" and nothing else. That's a DTO.

**In code, the same thing happens:**

| What You Have (Entity)         | What You Show (DTO)   |
|-------------------------------|-----------------------|
| User ID, name, email, **password**, SSN | User ID, name, email only |
| Order with 100 internal fields| Order number, total, status |
| Everything in the database    | Only what the client needs |

---

### Part 3: The Four Big Problems (Explained Simply)

**Problem 1: The Password Leak (Security)**

You have a `User` that stores a password. If you send the whole User object to the client, the password goes too. Oops! That's like accidentally showing your house key to everyone who walks by.

**Problem 2: The Lazy Loading Trap**

Imagine a book. The book has a table of contents that says "Chapter 2 is on page 45" — but when someone tries to read Chapter 2, the pages aren't actually loaded yet. They get an error: "Pages not loaded!"

In code, an `Order` might have a list of `OrderItem` objects that aren't loaded until you ask for them. When your API tries to send the Order, it tries to read those items — but they're not there. **LazyInitializationException** — crash!

**Problem 3: The Tight Coupling Problem**

You decide to rename a database column from `name` to `firstName` and `lastName`. If your API sends the entity directly, your API shape changes too. Every client that expected `name` breaks. That's tight coupling — changing one thing breaks everything.

**Problem 4: Over-Sending (Waste)**

You only need to show a user's name on a list screen. But if you send the whole entity, you're sending their bio, preferences, 50 other fields — like mailing someone a truck when they asked for a letter.

---

### Part 4: What Is a DTO? (Plain English)

**DTO** = **D**ata **T**ransfer **O**bject

In plain English: **A DTO is a simple box that holds only the data you want to send or receive. Nothing more.**

- When someone **sends** you data (like creating a new user), you put it in a **Request DTO**.
- When you **send back** data (like user info), you put it in a **Response DTO**.

You decide exactly what goes in the box. The entity (database) might have 20 fields; your DTO might have 3. You're in control.

---

### Part 5: The Flow — From Request to Response

```
Client sends a form (CreateUserRequest) 
    → You convert it to an Entity 
    → You save the Entity to the database 
    → You convert the saved Entity to a Response DTO 
    → You send the Response DTO back to the client
```

**Key idea:** The client never sees the Entity. They only see Request DTOs (what they send) and Response DTOs (what they get back).

---

### Part 6: Entity vs DTO — Side by Side

| Aspect       | Entity (Kitchen/Recipe)        | DTO (Menu)                    |
|-------------|--------------------------------|-------------------------------|
| Lives where | In the database (via JPA)      | In memory, for API only       |
| Contains    | All fields, relationships      | Only fields the client needs  |
| Has         | `@Entity`, `@Table`, etc.      | Plain class, no DB annotations|
| Purpose     | Represent database structure   | Represent API contract        |
| Sensitive?  | May have password, SSN, etc.   | Never — you choose what's in it |
| Changes     | When DB schema changes         | When API contract changes     |

---

### Part 7: The Security Example (Why DTOs Matter)

**Without a DTO (BAD):**
```text
API returns the whole User entity.
JSON looks like: { "id": 1, "name": "Alice", "email": "alice@mail.com", "password": "secret123" }
Anyone can see the password! 💀
```

**With a DTO (GOOD):**
```text
API returns UserResponse DTO.
JSON looks like: { "id": 1, "name": "Alice", "email": "alice@mail.com" }
Password is never included. ✅
```

---

### Part 8: Request vs Response DTOs

- **Request DTO**: What the client sends when they want to create or update something. Example: `CreateUserRequest` with name, email, password.
- **Response DTO**: What you send back. Example: `UserResponse` with id, name, email — no password.

Different jobs, different boxes. You don't use the same box for "what comes in" and "what goes out."

---

## Layer 2 — Professional Developer

### Why DTOs Before How

**Why first:** DTOs exist to:
1. **Secure** — Hide sensitive fields (passwords, SSN, internal IDs).
2. **Decouple** — Database structure can change without breaking the API.
3. **Control** — You decide exactly what goes over the wire.
4. **Avoid bugs** — No `LazyInitializationException` from lazy-loaded relationships.
5. **Optimize** — Send only what the client needs.

**How:** Define separate classes for requests and responses, then map between entities and DTOs.

---

### Entity vs DTO: Professional Comparison

| Criterion        | Entity                          | DTO                               |
|------------------|---------------------------------|-----------------------------------|
| Annotations      | `@Entity`, `@Table`, `@Column`  | None (or `@JsonProperty` for JSON) |
| Relationships    | `@OneToMany`, `@ManyToOne`      | Nested DTOs or IDs only           |
| Sensitive data   | May contain                     | Must never contain                |
| Lazy loading     | Yes (JPA/Hibernate)             | No (all data explicitly mapped)   |
| Versioning       | Tied to DB schema               | Independent (v1, v2 DTOs)         |
| Validation       | Usually minimal                 | `@Valid`, `@NotBlank`, etc.       |

---

### Separate Request and Response DTOs

**Request DTO (what the client sends):**
```java
// Input from client — only fields needed to create a user
public class CreateUserRequest {
    private String name;      // Required for new user
    private String email;     // Must be unique
    private String password;  // Will be hashed before storage
    // Getters and setters
}
```

**Response DTO (what you send back):**
```java
// Output to client — NEVER include password or other sensitive fields
public class UserResponse {
    private Long id;    // Exposed for client reference
    private String name;
    private String email;
    // Getters and setters — no password!
}
```

**Why separate?** Create and update often need different fields. Update might not include password; create usually does. Response must never expose sensitive data.

---

### Manual Mapping (Simple, Full Control)

**Entity:**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;  // Never expose
    // Getters and setters
}
```

**Mapper class (manual):**
```java
@Service
public class UserMapper {

    // Convert Entity → Response DTO (safe fields only)
    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        // password deliberately NOT copied
        return response;
    }

    // Convert CreateUserRequest → Entity (for saving)
    public User toEntity(CreateUserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());  // Will be hashed in service
        return user;
    }
}
```

**Controller using the mapper:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        // Request DTO → Entity
        User user = userMapper.toEntity(request);
        // Save to database
        User saved = userService.save(user);
        // Entity → Response DTO (password never leaves the server)
        return userMapper.toResponse(saved);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        User user = userService.findById(id);
        return userMapper.toResponse(user);
    }
}
```

---

### DTO Variants for Different Use Cases

| DTO Type            | Purpose                  | Typical Fields                          |
|---------------------|--------------------------|-----------------------------------------|
| `CreateUserRequest` | Creating a resource      | name, email, password                   |
| `UpdateUserRequest` | Updating (optional)      | name?, email? (no password by default)  |
| `UserResponse`      | Single resource          | id, name, email                         |
| `UserSummaryResponse` | List view              | id, name (minimal)                       |
| `UserDetailResponse`| Detail view              | All safe fields + nested data           |

---

### DTO Validation (Why and How)

**Why:** Invalid input (empty name, bad email, weak password) should be rejected before hitting the database.

**How:** Use Jakarta Bean Validation on **request** DTOs:

```java
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50)
    private String name;

    @NotBlank
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Getters and setters
}
```

**In the controller:** Add `@Valid` so validation runs automatically:
```java
@PostMapping
public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
    // If validation fails, MethodArgumentNotValidException is thrown
    // and handled by your exception handler (Chapter 22)
    User user = userMapper.toEntity(request);
    return userMapper.toResponse(userService.save(user));
}
```

---

### MapStruct: Auto-Mapping (Recommended for Production)

**Why MapStruct:** Manual mapping is verbose and error-prone. MapStruct generates mapping code at **compile time** — fast, type-safe, no reflection.

**1. Add dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```

**2. Configure annotation processor (in `maven-compiler-plugin` or `build`):**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.5.5.Final</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**3. Define mapper interface:**
```java
@Mapper(componentModel = "spring")  // Generates Spring @Component for injection
public interface UserMapper {

    // Entity → Response DTO (MapStruct auto-matches same field names)
    UserResponse toResponse(User user);

    // CreateUserRequest → Entity (for saving new records)
    User toEntity(CreateUserRequest request);

    // Update: merge request into existing entity; never overwrite id or password
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

    // Custom: combine firstName + lastName into fullName
    @Mapping(target = "fullName", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    UserResponse toResponseWithFullName(User user);

    // List mapping (MapStruct maps each element)
    List<UserResponse> toResponseList(List<User> users);
}
```

**4. Use in controller:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;  // MapStruct generates implementation

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        User user = userService.findById(id);
        return userMapper.toResponse(user);
    }

    @GetMapping
    public List<UserResponse> list() {
        return userMapper.toResponseList(userService.findAll());
    }
}
```

**Pros:** Compile-time safety, fast, no reflection. **Cons:** Requires annotation processor setup. **Use when:** Production applications, team projects.

---

### ModelMapper (Runtime Alternative)

**What it is:** A library that maps objects at **runtime** using reflection. Easier to set up than MapStruct, but slower and less type-safe.

**When to use:** Quick prototypes or when you need runtime flexibility. For production, prefer MapStruct.

```java
// Configuration
@Bean
public ModelMapper modelMapper() {
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STRICT)
        .setSkipNullEnabled(true);
    return mapper;
}

// Usage — maps by matching field names (reflection at runtime)
UserResponse response = modelMapper.map(user, UserResponse.class);
```

---

### Record Classes as DTOs (Java 16+)

Records are ideal for **immutable** response DTOs. They auto-generate constructor, getters, `equals`, `hashCode`, and `toString`.

**Before (class):**
```java
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    // Constructor, getters, setters, equals, hashCode...
}
```

**After (record):**
```java
// Immutable by default; no setters
public record UserResponse(Long id, String name, String email) {}
```

**Usage:**
```java
return new UserResponse(user.getId(), user.getName(), user.getEmail());
```

**With MapStruct:** MapStruct supports records; mapping works the same. Use records when the DTO is read-only; use a class when you need setters (e.g. some manual mapping scenarios).

---

### Nested DTOs

When an entity has relationships (e.g. `Order` has `User` and `List<OrderItem>`), the response DTO should use nested DTOs, not entities.

**Entity:**
```java
@Entity
public class Order {
    @Id
    private Long id;
    private String orderNumber;
    @ManyToOne
    private User user;
    @OneToMany
    private List<OrderItem> items;
}
```

**Response DTOs:**
```java
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private UserSummaryResponse user;   // Nested DTO, not User entity
    private List<OrderItemResponse> items;
}

public class UserSummaryResponse {
    private Long id;
    private String name;
}

public class OrderItemResponse {
    private Long id;
    private String productName;
    private Integer quantity;
}
```

**MapStruct with nested mappers:**
```java
@Mapper(componentModel = "spring", uses = {UserMapper.class, OrderItemMapper.class})
public interface OrderMapper {
    OrderResponse toResponse(Order order);
}
```

---

### When to Use DTOs vs When Entities Are Fine

| Situation                          | Use DTOs? | Reason                                           |
|-----------------------------------|-----------|--------------------------------------------------|
| Public REST API                   | ✅ Yes    | Security, control, avoid lazy loading           |
| Internal microservice (same trust)| ⚠️ Optional | Can expose entities if no sensitive data       |
| Simple CRUD, no relations         | ✅ Yes    | Still decouples API from DB                     |
| Prototyping / spike               | ⚠️ Can skip | Refactor to DTOs before production              |
| GraphQL (client picks fields)     | ⚠️ Depends | GraphQL reduces over-fetching; DTOs still help  |

**Default:** Use DTOs for any API that might be public or that has sensitive or relational data.

---

## Layer 3 — Advanced Engineering Depth

### Deep Dive: LazyInitializationException

**What happens:** With `FetchType.LAZY`, related entities (e.g. `Order.items`) are loaded only when accessed, inside an open Hibernate session. After the transaction/session closes, accessing them triggers `LazyInitializationException`.

**Why DTOs help:** You map entity → DTO **inside** the transaction, where lazy collections can be loaded. You then return only the DTO. The DTO has no lazy proxies; Jackson serializes plain data.

**Alternative:** Use `JOIN FETCH` or `@EntityGraph` to load relations in one query, then map to DTO. Never return entities with uninitialized lazy relations.

---

### N+1 and Projections

**N+1 problem:** Loading a list of `User` and then accessing `user.getOrders()` for each user triggers one query per user. With 100 users, that's 101 queries.

**Mitigations:**
1. **Fetch join:** `SELECT u FROM User u LEFT JOIN FETCH u.orders`
2. **`@BatchSize`:** Load related collections in batches.
3. **Projections:** Fetch only the columns you need.

**Projection example (interface):**
```java
public interface UserSummary {
    Long getId();
    String getName();
    String getEmail();
}

// Repository
@Query("SELECT u.id as id, u.name as name, u.email as email FROM User u")
List<UserSummary> findAllProjected();
```

Only `id`, `name`, `email` are selected — less data, fewer joins.

---

### Performance: MapStruct vs ModelMapper vs Manual

| Approach     | Mechanism        | Speed      | Type safety | When to use                |
|-------------|------------------|------------|-------------|----------------------------|
| MapStruct   | Compile-time gen | Very fast  | Yes         | Production, most cases     |
| ModelMapper | Reflection       | Slower     | No          | Prototypes, rare cases     |
| Manual      | Hand-written     | Fastest    | Yes         | Very simple, few mappings |

MapStruct generates code similar to manual mapping; ModelMapper uses reflection at runtime.

---

### Edge Cases: Circular References, Nulls, Optional Fields

**Circular references:** `User` → `Order` → `User`. If both DTOs reference each other, Jackson can loop. Break the cycle: use `UserSummaryResponse` inside `OrderResponse` (no back-reference to full `User`), or use `@JsonIgnore` / `@JsonManagedReference` / `@JsonBackReference`.

**Null handling in MapStruct:**
```java
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);
}
```
Null fields in the request are ignored; existing values remain.

---

### API Versioning with DTOs

Different API versions can use different DTOs while sharing the same entity:

```java
public class UserResponseV1 {
    private Long id;
    private String name;
    private String email;
}

public class UserResponseV2 {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;  // New in v2
}
```

Controllers return `UserResponseV1` or `UserResponseV2` based on path, header, or media type. Entity stays one; DTOs evolve with the API.

---

## Layer 4 — Interview Mastery

**Q1: Why use DTOs instead of returning entities?**

**A:** DTOs separate the database model from the API. Benefits: (1) **Security** — hide password, SSN, internal IDs; (2) **LazyInitializationException** — avoid serializing lazy proxies; (3) **Decoupling** — DB changes don’t break the API; (4) **Over-fetching** — send only needed fields; (5) **Versioning** — evolve the API with different DTOs.

---

**Q2: What’s the security risk of returning entities?**

**A:** Entities often contain sensitive fields (password, SSN, API keys). Returning them in JSON exposes that data. DTOs let you explicitly choose what is sent; sensitive fields are never included.

---

**Q3: MapStruct vs ModelMapper?**

**A:** MapStruct generates code at compile time — fast and type-safe. ModelMapper uses reflection at runtime — flexible but slower and less type-safe. Prefer MapStruct for production; ModelMapper mainly for quick prototypes.

---

**Q4: How do you handle nested DTOs?**

**A:** Use separate mappers for nested types and wire them with `uses` in MapStruct. For manual mapping, map each nested object explicitly. Break circular references by using summary DTOs (e.g. `UserSummaryResponse`) instead of full objects.

---

**Q5: When is it acceptable to return entities directly?**

**A:** Only in narrow cases: internal microservices within the same security boundary, read-only reference data with no sensitive fields, or prototyping (with plans to add DTOs). For public or production APIs, always use DTOs.

---

**Q6: Should request and response DTOs be the same?**

**A:** No. Create/update often need different fields; response must never include sensitive data. Use `CreateUserRequest`, `UpdateUserRequest`, and `UserResponse` (and summary/detail variants) as separate types.

---

**Q7: How do you prevent over-fetching with DTOs?**

**A:** (1) Use projections in queries (interface or constructor-based) to select only needed columns. (2) Use summary DTOs for lists and detail DTOs for single resources. (3) Load relationships only when needed (e.g. fetch join in detail queries).

---

**Q8: Can you use records as DTOs?**

**A:** Yes. Records are ideal for immutable response DTOs. MapStruct supports them. Use records when the DTO is read-only; use classes when you need setters.

---

## Summary

**Key ideas:**
1. **Entity** = database model; **DTO** = API contract. Never expose entities directly in public APIs.
2. Use **Request DTOs** for input (`CreateUserRequest`, `UpdateUserRequest`) and **Response DTOs** for output (`UserResponse`).
3. **MapStruct** is the preferred mapping tool for production (compile-time, type-safe, fast).
4. **Records** work well for immutable response DTOs.
5. **Validate** request DTOs with `@Valid` and Jakarta Bean Validation.
6. Use **projections** and **summary vs detail DTOs** to avoid over-fetching and N+1.

**Best practices:**
- Use DTOs for all production APIs.
- Separate Create, Update, Response, Summary, and Detail DTOs.
- Use MapStruct (or manual mapping for very simple cases).
- Validate all request DTOs.
- Handle nested objects and circular references carefully.

**Common patterns:**
- `CreateXxxRequest`, `UpdateXxxRequest` — input
- `XxxResponse`, `XxxSummaryResponse`, `XxxDetailResponse` — output

**Next steps:**
- Spring Data JPA (Chapter 24)
- Hibernate internals (Chapter 25)
- Transaction management (Chapter 26)

---

[← Back to Index](00-README.md) | [Previous: Exception Handling](22-Exception-Handling.md) | [Next: Spring Data JPA →](24-Spring-Data-JPA.md)
