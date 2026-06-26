# Chapter 24: Spring Data JPA Deep Dive

[← Back to Index](00-README.md) | [Previous: DTO Patterns](23-DTO-Patterns.md) | [Next: Hibernate Internals →](25-Hibernate-Internals.md)

---

## Why This Chapter Matters

Almost every application you'll build talks to a database. Users, orders, products, messages — they all live in a database. **Spring Data JPA** is how most Spring Boot applications interact with that database.

Without Spring Data JPA, you'd write dozens (or hundreds) of lines of repetitive boilerplate code for every table: create, read, update, delete, find by name, find by email, paginate, sort... With Spring Data JPA, you write an **interface** (no implementation!) and Spring generates all that code for you.

It's honestly one of the most impressive features of Spring — and understanding it well separates junior developers from intermediate ones.

**Without understanding Spring Data JPA:**
- You'll write 50+ lines of boilerplate code for simple CRUD operations
- You'll struggle with complex queries and resort to raw SQL everywhere
- You'll miss performance optimization opportunities (N+1 problems, lazy loading issues)
- You'll fail to leverage Spring's powerful abstractions that save hours of work

**With proper Spring Data JPA understanding:**
- You'll write clean, maintainable data access code with almost zero boilerplate
- You'll leverage method-name-based queries that read like English
- You'll use `@Query`, projections, and specifications for complex cases
- You'll understand how Spring generates implementations at runtime
- You'll excel in data layer architecture discussions and interviews

---

## Layer 1 — Intuition Builder

### Part 1: What Is a Database, and Why Do We Need to "Talk" to It?

Imagine a giant filing cabinet where you store information. You have drawers for "Users," "Orders," "Products," and so on. Each drawer has folders. Each folder has papers with information written on them.

A **database** is that filing cabinet — but on a computer. It stores your data safely and lets you find it quickly when you need it. When someone signs up on your website, you add a new "folder" (row) to the Users drawer (table). When they place an order, you add a folder to the Orders drawer.

Your Java application needs to put data *into* this cabinet and get data *out* of it. That's what "talking to the database" means.

---

### Part 2: The Language Problem — Java vs. Database

Here's the tricky part: Your Java program speaks "Java." The database speaks "SQL" (a special language for databases — think of it like a different human language). If you're English and your friend speaks Spanish, you need a translator to understand each other.

**JPA (Java Persistence API)** is that translator. It sits between your Java objects and the database. You tell it in Java: "Save this User object." JPA translates that into SQL: "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')." When you ask for data back, JPA translates the database rows into Java objects.

Think of it this way:
- **Without JPA:** You write every SQL sentence yourself, by hand, in a foreign language.
- **With JPA:** You speak Java. JPA does the translation for you.

---

### Part 3: What Does Spring Data JPA Add on Top of JPA?

JPA is the translator. But you still have to tell the translator what to say. For every "find user by email" or "find all active users," you'd write Java code that builds the right JPA query. That's a lot of repetitive work.

**Spring Data JPA** is like having a super-smart assistant who knows exactly what you want. You say: "I want a way to find a user by their email." Spring Data JPA says: "Got it. Just write a method name — `findByEmail(String email)` — and I'll figure out the rest."

You describe *what* you want. Spring Data JPA figures out *how* to do it. No translation manual. No writing SQL. No writing query-building code. Just method names.

---

### Part 4: The Librarian Analogy — The Repository Pattern

Imagine a library. The library has millions of books. You don't walk into the storage room and search every shelf yourself. You go to the front desk and ask the **librarian**: "Do you have any books about dinosaurs?" or "Find me the book with ID 42."

The librarian knows where everything is. They go and get it for you. You don't care how they organize the shelves or what system they use. You just ask, and they deliver.

A **Repository** in Spring Data JPA is that librarian. It's the "front desk" for your data. You say: "Find the user with this email." The repository goes to the database, runs the right query, and gives you back a User object. You never touch the database directly. You talk to the repository.

---

### Part 5: Before vs. After — Why Spring Data JPA Is Magic

**Before (without Spring Data JPA):** You're the librarian, the translator, and the shelf-organizer all at once. For *every single* operation, you write code like this:

```java
// WITHOUT Spring Data JPA — 20+ lines for ONE simple "find by email" query
public User findByEmail(String email) {
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
        conn = dataSource.getConnection();  // Get a connection to the database
        stmt = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
        stmt.setString(1, email);  // Put the email in the right place
        rs = stmt.executeQuery();  // Run the query
        
        if (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));      // Copy each column by hand
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setActive(rs.getBoolean("active"));
            // ... for every single column ...
            return user;
        }
        return null;
    } catch (SQLException e) {
        // Handle errors
    } finally {
        // Close everything (conn, stmt, rs) — don't forget!
    }
}
// Now repeat this for findByName, findByAge, findActive, findAll, save, delete...
// It's HUNDREDS of lines of boring, error-prone code.
```

**After (with Spring Data JPA):**

```java
// ONE line. That's it. Spring generates the implementation.
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
// Spring reads the method name "findByEmail" and automatically generates
// the SQL, handles the connection, maps the results, and returns a User.
// You write one line. Spring does everything else.
```

Same result. Hundreds of lines → one line. That's the magic.

---

### Part 6: Method Names Turn Into Queries — The Core Trick

Spring Data JPA is really good at reading method names. It breaks them into parts and turns each part into a piece of a query.

| What You Write          | What Spring Understands                  |
|-------------------------|------------------------------------------|
| `findByEmail`           | Find rows where `email` equals the value  |
| `findByNameAndActive`   | Find rows where `name` AND `active` match |
| `findByNameContaining`  | Find rows where `name` contains the text  |
| `findByAgeGreaterThan`  | Find rows where `age` is greater than     |
| `findFirst10ByOrderByNameAsc` | Get first 10, sort by name A→Z      |

You're not writing SQL. You're writing something that reads almost like English. Spring fills in the rest.

---

### Part 7: The Repository Family Tree

Repositories come in a family. The "parent" has basic features. The "child" adds more. The "grandchild" has the most.

| Repository Type              | What It Gives You                                   |
|-----------------------------|------------------------------------------------------|
| **Repository**              | Just a marker — says "this is a repository"          |
| **CrudRepository**          | Create, Read, Update, Delete (the basics)            |
| **PagingAndSortingRepository** | Crud + pagination (page 1, page 2...) + sorting   |
| **JpaRepository**           | Everything above + flush, batch deletes, JPA extras  |

For most projects, you use **JpaRepository** — it has everything you need.

---

### Part 8: One Small Example — User Entity and Repository

**Entity** = a Java class that represents one row in a database table. Like a form: each field (name, email) is a column.

```java
@Entity  // "This class is a database table"
public class User {
    @Id  // "This is the unique ID"
    @GeneratedValue  // "The database assigns it automatically"
    private Long id;
    
    private String name;
    private String email;
    private boolean active;
    // getters and setters
}
```

**Repository** = the librarian. You extend `JpaRepository<User, Long>` — meaning "this repository works with User objects, and their IDs are Long numbers."

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByNameContaining(String name);
}
```

**Usage** — in your service, you just call the repository:

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public User createUser(User user) {
        return userRepository.save(user);  // Save = create or update
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);  // One line does it all
    }
}
```

**Key insight:** You define *what* you want. Spring figures out *how* to do it.

---

### Part 9: When Things Get Complicated — Custom Queries

Sometimes the method name would get too long or weird. Like: `findByEmailAddressAndActiveStatusIsTrueAndCreatedAtAfter`. Yuck.

For those cases, you write the query yourself using `@Query`. It's like giving the librarian a specific note: "Here's exactly what I want. Go get it." Spring still runs it for you; you just write the query in a language called JPQL (very similar to SQL, but uses Java class names instead of table names).

---

### Part 10: Relationships — When Data Is Connected

In the real world, things are connected. A user has many orders. An order has many items. A student attends many courses; a course has many students.

**Relationships** in JPA let you express these connections. You annotate your entity with things like `@OneToMany` (one user, many orders) or `@ManyToMany` (many students, many courses). JPA knows how to load related data and save it correctly when you save the main entity.

**Cascade** means "when I save/delete this, do the same to its related things." Example: When you delete a user, should all their orders be deleted too? You configure that with cascade types.

**Orphan removal** means "if I remove a child from its parent's list, delete that child from the database." Example: You remove an order from a user's order list — that order becomes an "orphan." With orphan removal, it gets deleted automatically.

---

## Layer 2 — Professional Developer

### What Is JPA? (Plain English First)

**JPA (Java Persistence API)** is a specification — a set of rules — for how Java applications should talk to databases. It defines:

1. **Object-Relational Mapping (ORM):** How Java objects map to database rows. A `User` object with `id`, `name`, `email` maps to a `users` table with columns `id`, `name`, `email`.

2. **Entity Lifecycle:** How entities are created, managed, and removed. JPA tracks whether an entity is "new," "managed," or "detached."

3. **Query Language (JPQL):** A query language that looks like SQL but works with Java class and property names instead of table and column names.

**Hibernate** is the most popular implementation of JPA. When you use Spring Data JPA, you're usually using Hibernate under the hood. Hibernate does the actual translation and database work.

---

### What Spring Data JPA Adds

Spring Data JPA sits on top of JPA/Hibernate and adds:

| Feature                | What It Does                                                                 |
|------------------------|-------------------------------------------------------------------------------|
| **Repository abstraction** | You define interfaces; Spring provides implementations. No boilerplate.   |
| **Query derivation**       | Method names like `findByEmail` generate queries automatically.            |
| **@Query**                 | Custom JPQL or native SQL when method names aren't enough.                  |
| **Pagination & sorting**   | Built-in `Pageable`, `Sort`, `Page`, `Slice`.                               |
| **Specifications**        | Build dynamic queries programmatically (optional filters).                   |
| **Auditing**               | Auto-fill `createdAt`, `updatedAt`, `createdBy`, `lastModifiedBy`.         |
| **Reduced boilerplate**    | No DAO classes, no manual `EntityManager` wiring.                           |

---

### Repository Hierarchy — Full Picture

```
Repository<T, ID>                    ← Marker interface (no methods)
    │
    └── CrudRepository<T, ID>        ← save, findById, findAll, delete, count, existsById
            │
            └── PagingAndSortingRepository<T, ID>  ← findAll(Sort), findAll(Pageable)
                    │
                    └── JpaRepository<T, ID>         ← flush, saveAndFlush, deleteInBatch,
                                                    getById, List<T> instead of Iterable<T>
```

**When to use what:**
- **CrudRepository:** Minimal needs, no pagination.
- **JpaRepository:** Default choice for almost all Spring Data JPA projects (includes everything).

---

### CrudRepository vs. JpaRepository — Side by Side

| Aspect           | CrudRepository              | JpaRepository                      |
|-----------------|-----------------------------|------------------------------------|
| **CRUD**        | ✅ save, findById, delete    | ✅ Same                            |
| **Pagination**  | ❌ No                        | ✅ findAll(Pageable)               |
| **Sorting**     | ❌ No                        | ✅ findAll(Sort)                   |
| **Return types**| Iterable<T>                 | List<T> (more convenient)          |
| **Flush**       | ❌ No                        | ✅ flush(), saveAndFlush()         |
| **Batch delete**| ❌ No                        | ✅ deleteInBatch(), deleteAllInBatch() |
| **Recommendation** | Rare                        | Use for most projects              |

---

### Entity, Repository, and Service — Complete Example

```java
// ═══════════════════════════════════════════════════════════════
// ENTITY: Maps to "users" table
// ═══════════════════════════════════════════════════════════════
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private boolean active = true;
    
    private LocalDateTime createdAt;
    
    // Constructors, getters, setters
}

// ═══════════════════════════════════════════════════════════════
// REPOSITORY: Data access layer — Spring provides implementation
// ═══════════════════════════════════════════════════════════════
public interface UserRepository extends JpaRepository<User, Long> {
    // Method name → query: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);
    
    // Method name → query: SELECT * FROM users WHERE name LIKE %?% AND active = true
    List<User> findByNameContainingAndActiveTrue(String name);
    
    // Method name → query: SELECT * FROM users ORDER BY createdAt DESC LIMIT 1
    Optional<User> findFirstByOrderByCreatedAtDesc();
    
    // Custom JPQL when method name would be too long
    @Query("SELECT u FROM User u WHERE u.active = true ORDER BY u.name ASC")
    List<User> findActiveUsersSortedByName();
}

// ═══════════════════════════════════════════════════════════════
// SERVICE: Business logic — uses repository for data access
// ═══════════════════════════════════════════════════════════════
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public User createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> searchActiveUsers(String name) {
        return userRepository.findByNameContainingAndActiveTrue(name);
    }
}
```

---

### Query Derivation from Method Names

Spring Data JPA parses method names and derives queries. The pattern is:

`[action][By/OrderBy][property][keyword][And/Or][property][keyword]...`

**Examples:**

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Find by single property
    List<User> findByName(String name);
    Optional<User> findByEmail(String email);
    
    // AND
    List<User> findByNameAndEmail(String name, String email);
    
    // OR
    List<User> findByNameOrEmail(String name, String email);
    
    // Comparison
    List<User> findByAgeGreaterThan(int age);
    List<User> findByAgeLessThan(int age);
    List<User> findByAgeBetween(int min, int max);
    
    // Null checks
    List<User> findByEmailIsNull();
    List<User> findByEmailIsNotNull();
    
    // String matching
    List<User> findByNameLike(String pattern);       // You provide % (e.g., "%john%")
    List<User> findByNameContaining(String text);    // Spring adds % (e.g., "%john%")
    List<User> findByNameStartingWith(String prefix);
    List<User> findByNameEndingWith(String suffix);
    
    // Case insensitive
    List<User> findByNameIgnoreCase(String name);
    
    // Ordering
    List<User> findByActiveTrueOrderByNameAsc();
    List<User> findByActiveTrueOrderByCreatedAtDesc();
    
    // Limiting
    User findFirstByOrderByCreatedAtDesc();
    List<User> findTop10ByOrderByCreatedAtDesc();
    
    // Count / Exists
    long countByName(String name);
    boolean existsByEmail(String email);
    
    // Delete
    void deleteByName(String name);
    long deleteByActiveFalse();
}
```

**Query method keywords (reference table):**

| Keyword           | SQL Equivalent | Example                  |
|-------------------|----------------|--------------------------|
| `And`             | AND            | findByNameAndEmail       |
| `Or`              | OR             | findByNameOrEmail        |
| `Is`, `Equals`    | =              | findByEmailIs            |
| `Between`         | BETWEEN        | findByAgeBetween         |
| `LessThan`        | <              | findByAgeLessThan        |
| `LessThanEqual`   | <=             | findByAgeLessThanEqual   |
| `GreaterThan`     | >              | findByAgeGreaterThan     |
| `GreaterThanEqual`| >=             | findByAgeGreaterThanEqual|
| `After`           | > (dates)      | findByCreatedAtAfter     |
| `Before`          | < (dates)      | findByCreatedAtBefore    |
| `IsNull`          | IS NULL        | findByEmailIsNull        |
| `IsNotNull`       | IS NOT NULL    | findByEmailIsNotNull     |
| `Like`            | LIKE           | findByNameLike           |
| `NotLike`         | NOT LIKE       | findByNameNotLike        |
| `Containing`      | LIKE %value%   | findByNameContaining     |
| `StartingWith`    | LIKE value%    | findByNameStartingWith   |
| `EndingWith`      | LIKE %value    | findByNameEndingWith     |
| `IgnoreCase`      | UPPER/LOWER    | findByNameIgnoreCase     |
| `OrderBy`         | ORDER BY       | findByOrderByNameAsc     |
| `First`, `Top`    | LIMIT          | findFirstBy, findTop10By |
| `Distinct`        | DISTINCT       | findDistinctByName       |

**Best practice:** If a method name exceeds ~50 characters, use `@Query` instead.

---

### @Query for Custom JPQL and SQL

When method-name derivation isn't enough:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // JPQL — works with entity/attribute names
    @Query("SELECT u FROM User u WHERE u.email = ?1")
    Optional<User> findByEmailAddress(String email);
    
    // JPQL with named parameters
    @Query("SELECT u FROM User u WHERE u.name = :name AND u.active = :active")
    List<User> findByNameAndActive(@Param("name") String name, 
                                    @Param("active") boolean active);
    
    // Native SQL (use when JPQL can't do what you need)
    @Query(value = "SELECT * FROM users WHERE email LIKE %?1%", nativeQuery = true)
    List<User> searchByEmailPattern(String pattern);
    
    // Modifying query (UPDATE) — requires @Modifying and @Transactional
    @Modifying
    @Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
    int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
    
    // Modifying query (DELETE)
    @Modifying
    @Query("DELETE FROM User u WHERE u.active = false")
    int deleteInactiveUsers();
    
    // Pagination with custom query
    @Query("SELECT u FROM User u WHERE u.active = true")
    Page<User> findActiveUsers(Pageable pageable);
}
```

**Rules for @Modifying:**
- Use `@Modifying` for UPDATE/DELETE.
- Call the method from a `@Transactional` context (e.g., service).
- Return type can be `int` (affected rows) or `void`.
- `clearAutomatically = true` clears the persistence context after execution to avoid stale entities.

---

### Pagination and Sorting

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByStatus(Status status, Pageable pageable);
    List<User> findByStatus(Status status, Sort sort);
    Slice<User> findByStatus(Status status, Pageable pageable);  // No count query
}

// Usage
@Service
public class UserService {
    public Page<User> getUsers(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return userRepository.findByStatus(ACTIVE, pageable);
    }
    
    public Page<User> getUsersMultiSort() {
        Sort sort = Sort.by("name").ascending()
                       .and(Sort.by("createdAt").descending());
        Pageable pageable = PageRequest.of(0, 10, sort);
        return userRepository.findAll(pageable);
    }
    
    // Slice — no count query (better for infinite scroll)
    public Slice<User> getUsersSlice(int page, int size) {
        return userRepository.findByStatus(ACTIVE, PageRequest.of(page, size));
    }
}

// Page gives you:
// - page.getContent()       → current page data
// - page.getTotalElements() → total count
// - page.getTotalPages()    → total pages
// - page.hasNext()          → has next page

// Slice gives you:
// - slice.getContent()      → current page data
// - slice.hasNext()        → based on content size (no count)
// - NO totalElements or totalPages
```

| Type   | Count Query? | Use Case                    |
|--------|--------------|-----------------------------|
| Page   | Yes          | When you need total count   |
| Slice  | No           | Infinite scroll, performance|

---

### Auditing — @CreatedDate, @LastModifiedDate

Automatic tracking of when and by whom entities were created/updated:

```java
// 1. Base auditable entity
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class AuditableEntity {
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @CreatedBy
    @Column(nullable = false, updatable = false)
    private String createdBy;
    
    @LastModifiedBy
    @Column(nullable = false)
    private String lastModifiedBy;
    
    // getters, setters
}

// 2. Enable auditing and provide auditor
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaConfig {
    @Bean
    public AuditorAware<String> auditorAwareImpl() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.of(auth.getName());
        };
    }
}

// 3. Entity extends auditable base
@Entity
public class User extends AuditableEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String email;
    // createdAt, updatedAt, createdBy, lastModifiedBy inherited
}
```

---

### Entity Relationships — @OneToMany, @ManyToOne, @ManyToMany

**@ManyToOne / @OneToMany**

One user has many orders. One order belongs to one user.

```java
// Order (many side) — has a reference to User
@Entity
public class Order {
    @Id
    @GeneratedValue
    private Long id;
    private BigDecimal amount;
    
    @ManyToOne(fetch = FetchType.LAZY)  // Many orders → one user
    @JoinColumn(name = "user_id")
    private User user;
    
    // getters, setters
}

// User (one side) — has a collection of Orders
@Entity
public class User {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
    
    // getters, setters
}
```

**@ManyToMany**

Students and courses: many students, many courses.

```java
@Entity
public class Student {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}

@Entity
public class Course {
    @Id
    @GeneratedValue
    private Long id;
    private String title;
    
    @ManyToMany(mappedBy = "courses")  // Owning side is Student
    private Set<Student> students = new HashSet<>();
}
```

---

### Cascade Types — What Happens to Related Entities

When you save, merge, or delete an entity, cascade controls what happens to its associations.

| Cascade Type   | save() | merge() | delete() | detach() | refresh() |
|----------------|--------|---------|----------|----------|-----------|
| **PERSIST**    | ✅     | ❌      | ❌       | ❌       | ❌        |
| **MERGE**      | ❌     | ✅      | ❌       | ❌       | ❌        |
| **REMOVE**     | ❌     | ❌      | ✅       | ❌       | ❌        |
| **ALL**        | ✅     | ✅      | ✅       | ✅       | ✅        |
| **DETACH**     | ❌     | ❌      | ❌       | ✅       | ❌        |
| **REFRESH**    | ❌     | ❌      | ❌       | ❌       | ✅        |
| **None**       | ❌     | ❌      | ❌       | ❌       | ❌        |

**Typical usage:**
- `CascadeType.ALL` on parent→child when child cannot exist without parent (e.g., Order→OrderItem).
- `CascadeType.PERSIST` when you want new children to be saved with the parent.
- `CascadeType.REMOVE` when deleting the parent should delete children (often combined with `orphanRemoval`).

---

### Orphan Removal

When a child is removed from the parent's collection, should it be deleted from the database?

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> orders = new ArrayList<>();
```

- **orphanRemoval = true:** Removing an order from `user.getOrders()` and saving the user deletes that order from the database.
- **orphanRemoval = false:** The order remains in the database; its `user_id` may become null (if allowed).

Use orphan removal when the child has no meaning without the parent (e.g., order items without their order).

---

### Specifications for Dynamic Queries

When filters are optional (e.g., search form with name, email, active status, date range), Specifications build the query dynamically:

```java
// 1. Repository extends JpaSpecificationExecutor
public interface UserRepository extends JpaRepository<User, Long>, 
                                       JpaSpecificationExecutor<User> {
}

// 2. Specification helpers
public class UserSpecifications {
    
    public static Specification<User> hasName(String name) {
        return (root, query, cb) -> 
            name == null ? cb.conjunction() : cb.equal(root.get("name"), name);
    }
    
    public static Specification<User> hasEmailContaining(String email) {
        return (root, query, cb) -> {
            if (email == null) return cb.conjunction();
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }
    
    public static Specification<User> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
    
    public static Specification<User> createdAfter(LocalDateTime date) {
        return (root, query, cb) -> 
            date == null ? cb.conjunction() : cb.greaterThan(root.get("createdAt"), date);
    }
}

// 3. Use in service
@Service
public class UserService {
    public Page<User> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        Specification<User> spec = Specification
            .where(UserSpecifications.hasName(criteria.getName()))
            .and(UserSpecifications.hasEmailContaining(criteria.getEmail()))
            .and(criteria.getActive() != null ? 
                 UserSpecifications.isActive(criteria.getActive()) : null)
            .and(UserSpecifications.createdAfter(criteria.getCreatedAfter()));
        
        return userRepository.findAll(spec, pageable);
    }
}
```

---

### Custom Repository Implementation

For logic that doesn't fit method names or `@Query`:

```java
// 1. Custom interface
public interface CustomUserRepository {
    List<User> complexSearch(UserSearchCriteria criteria);
}

// 2. Implementation (name must end with Impl)
public class CustomUserRepositoryImpl implements CustomUserRepository {
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<User> complexSearch(UserSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> root = query.from(User.class);
        // Build dynamic query...
        return entityManager.createQuery(query).getResultList();
    }
}

// 3. Extend main repository
public interface UserRepository extends JpaRepository<User, Long>, 
                                       CustomUserRepository {
}
```

---

## Layer 3 — Advanced Engineering Depth

### N+1 Problem and Fetch Joins

Loading a user and then accessing their orders triggers one query per order (N+1: 1 for users + N for orders). Use fetch joins to load everything in one query:

```java
@Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.id = :id")
Optional<User> findByIdWithOrders(@Param("id") Long id);
```

### Entity Graphs

Control which associations are loaded eagerly:

```java
@Entity
@NamedEntityGraph(name = "User.withOrders", attributeNodes = @NamedAttributeNode("orders"))
public class User { ... }

@Query("SELECT u FROM User u WHERE u.id = :id")
@EntityGraph("User.withOrders")
Optional<User> findByIdWithOrders(@Param("id") Long id);
```

### Batch Operations

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```

### Projections for Performance

Return only needed fields:

```java
public interface UserSummary {
    String getName();
    String getEmail();
}

List<UserSummary> findByActiveTrue();  // No full entity load
```

### Query Hints

```java
@QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
List<User> findByActiveTrue();
```

---

## Layer 4 — Interview Mastery

### Q1: What is JPA, and what does Spring Data JPA add?

**Answer:** JPA (Java Persistence API) is a specification for mapping Java objects to database tables. It defines how to persist, query, and manage entity lifecycles. Hibernate is a common JPA implementation.

Spring Data JPA adds a repository abstraction on top of JPA: you define interfaces with method names (e.g., `findByEmail`) and Spring generates implementations. It also provides pagination, sorting, `@Query`, Specifications, auditing, and less boilerplate.

---

### Q2: CrudRepository vs. JpaRepository — when to use what?

**Answer:** JpaRepository extends PagingAndSortingRepository, which extends CrudRepository. JpaRepository adds pagination, sorting, flush operations, batch deletes, and returns `List<T>` instead of `Iterable<T>`.

Use **JpaRepository** for almost all Spring Data JPA applications. Use **CrudRepository** only when you need minimal features and no pagination.

---

### Q3: How does query derivation from method names work?

**Answer:** Spring parses the method name using a `PartTree`: subject (find, count, delete), predicate (By…), property names, and keywords (And, Or, Like, GreaterThan, etc.). It then builds a query (e.g., Criteria API or JPQL) and executes it. Method names map directly to query structure.

---

### Q4: When would you use @Query instead of method names?

**Answer:** Use @Query when:
- Method names would be too long
- You need joins, subqueries, or aggregates
- You need native SQL
- You need UPDATE/DELETE (with @Modifying)
- Method-name derivation can't express the logic

---

### Q5: What are Specifications, and when are they useful?

**Answer:** Specifications let you build dynamic queries programmatically using the JPA Criteria API. You combine predicates (AND/OR) based on optional filters. Use them for search screens with many optional criteria instead of dozens of repository methods.

---

### Q6: Page vs. Slice for pagination?

**Answer:** Page runs a data query and a count query, so you get total elements and total pages. Slice runs only the data query (with limit+1 to check hasNext). Use Page when you need total count; use Slice for infinite scroll or when count is expensive.

---

### Q7: What do @CreatedDate and @LastModifiedDate do?

**Answer:** With `@EnableJpaAuditing` and `AuditingEntityListener`, JPA automatically sets `createdAt` when an entity is first persisted and `updatedAt` on each update. `@CreatedBy` and `@LastModifiedBy` require an `AuditorAware` bean (e.g., current user).

---

### Q8: Explain cascade types. When would you use CascadeType.ALL?

**Answer:** Cascade controls whether operations on a parent propagate to associated entities. PERSIST, MERGE, REMOVE, REFRESH, DETACH can be combined or used via ALL.

Use `CascadeType.ALL` when the child has no independent lifecycle (e.g., OrderItem cannot exist without Order). Be careful with REMOVE on large collections.

---

### Q9: What is orphan removal?

**Answer:** When `orphanRemoval = true` on a `@OneToMany` (or `@OneToOne`), removing a child from the parent’s collection and flushing causes the child to be deleted from the database. Use it when the child is owned by the parent and has no meaning alone.

---

### Q10: How does Spring Data JPA implement repositories at runtime?

**Answer:** Spring creates JDK proxies for repository interfaces. When a method is called, the proxy checks for a custom implementation, then for a declared `@Query`, then falls back to method-name derivation. It builds the query (e.g., via Criteria API) and executes it through the `EntityManager`.

---

## Summary

**Key Takeaways:**

1. **JPA** translates between Java objects and the database; **Spring Data JPA** adds repositories and query derivation.
2. **Repository pattern** — define interfaces; Spring provides implementations.
3. **Use JpaRepository** for most projects.
4. **Method names** like `findByEmail` generate queries automatically.
5. **@Query** for custom JPQL or native SQL when method names aren’t enough.
6. **Specifications** for dynamic, optional filters.
7. **Pagination:** Page for total count, Slice for performance.
8. **Auditing** for automatic `createdAt`, `updatedAt`, `createdBy`, `lastModifiedBy`.
9. **Relationships:** @OneToMany, @ManyToOne, @ManyToMany.
10. **Cascade** controls propagation of operations; **orphan removal** deletes children removed from collections.

**Best Practices:**
- Use JpaRepository for most repositories
- Prefer method-name derivation; use @Query when names get long or logic is complex
- Use Optional for single-result methods
- Use projections when you don’t need full entities
- Use Slice when total count isn’t needed
- Use Specifications for dynamic search
- Use @Transactional with @Modifying queries

**Common Pitfalls:**
- Forgetting @Transactional on @Modifying
- N+1 queries — use fetch joins or entity graphs
- Using Page when Slice would suffice
- Overly long method names — switch to @Query
- Not handling null in Specifications

---

[← Back to Index](00-README.md) | [Previous: DTO Patterns](23-DTO-Patterns.md) | [Next: Hibernate Internals →](25-Hibernate-Internals.md)
