# Database Models - journalApp

Everything about how data is structured, stored, and accessed in MongoDB.

---

## MongoDB Basics (Quick Primer)

If you're coming from a SQL background, here's a quick translation:

| SQL Concept | MongoDB Equivalent |
|---|---|
| Database | Database |
| Table | Collection |
| Row | Document |
| Column | Field |
| Primary Key | `_id` |
| Foreign Key | `@DBRef` (reference) |
| JOIN | `$lookup` or `@DBRef` auto-resolution |
| Schema (strict) | Schema (flexible -- documents can have different fields) |

In this project, there are **two collections** in the `journaldb` database:
- `journal_entries` -- stores all diary/journal entries
- `users` -- stores all user accounts

---

## Entity 1: JournalEntry

**File:** `src/main/java/com/journalApp/journalEntity/JournalEntry.java`

```java
@Document(collection = "journal_entries")
@Data
@NoArgsConstructor
public class JournalEntry {
    @Id
    private ObjectId id;

    @NonNull
    private String title;

    private String content;

    private LocalDateTime date;
}
```

### Field-by-Field Breakdown

| Field | Type | Annotation | Required? | Description |
|---|---|---|---|---|
| `id` | `ObjectId` | `@Id` | Auto-generated | The unique identifier for each journal entry |
| `title` | `String` | `@NonNull` | Yes | The title/heading of the entry |
| `content` | `String` | -- | No | The body text of the entry |
| `date` | `LocalDateTime` | -- | No (set by code) | When the entry was created |

### What a Document Looks Like in MongoDB

When you create a journal entry, this is what gets stored in the `journal_entries` collection:

```json
{
    "_id": ObjectId("507f1f77bcf86cd799439011"),
    "title": "My First Day",
    "content": "Today I started learning Spring Boot!",
    "date": ISODate("2026-03-16T14:30:00.000Z")
}
```

### Key Design Decisions

**Why `ObjectId` instead of `Long` or `Integer`?**

`ObjectId` is MongoDB's native ID type. It's a 12-byte value that encodes:
- A timestamp (when it was created)
- A machine identifier
- A process ID
- A random counter

This means IDs are **globally unique** without needing a central counter, and you can extract the creation timestamp from the ID itself.

**Why `LocalDateTime` for the date?**

`LocalDateTime` stores both date and time (e.g., `2026-03-16T14:30:00`) without timezone information. It's set programmatically in the service layer:

```java
journalEntry.setDate(LocalDateTime.now());
```

This ensures every entry gets an accurate timestamp, regardless of what the client sends.

**Why is `content` optional but `title` is required?**

The `@NonNull` annotation on `title` means you can't create a journal entry without a title. But `content` has no such restriction -- you might want to create a quick entry with just a title and fill in the content later.

---

## Entity 2: User

**File:** `src/main/java/com/journalApp/journalEntity/User.java`

```java
@Document(collection = "users")
@Data
public class User {
    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @NonNull
    private String username;

    @NonNull
    private String password;

    @DBRef
    private List<JournalEntry> journalEntries = new ArrayList<>();
}
```

### Field-by-Field Breakdown

| Field | Type | Annotation(s) | Required? | Description |
|---|---|---|---|---|
| `id` | `ObjectId` | `@Id` | Auto-generated | Unique identifier for the user |
| `username` | `String` | `@Indexed(unique=true)`, `@NonNull` | Yes | The user's login name (must be unique) |
| `password` | `String` | `@NonNull` | Yes | The user's password (stored as plain text -- see security note below) |
| `journalEntries` | `List<JournalEntry>` | `@DBRef` | No | List of references to the user's journal entries |

### What a Document Looks Like in MongoDB

```json
{
    "_id": ObjectId("507f1f77bcf86cd799439022"),
    "username": "john",
    "password": "mypassword123",
    "journalEntries": [
        {
            "$ref": "journal_entries",
            "$id": ObjectId("507f1f77bcf86cd799439011")
        },
        {
            "$ref": "journal_entries",
            "$id": ObjectId("507f1f77bcf86cd799439012")
        }
    ]
}
```

### Understanding `@DBRef` -- The Parent-Child Relationship

This is the most important concept in the data model. Let's break it down:

**Without `@DBRef` (Embedding):**
```json
{
    "_id": "...",
    "username": "john",
    "journalEntries": [
        { "title": "Day 1", "content": "Full text here..." },
        { "title": "Day 2", "content": "Full text here too..." }
    ]
}
```
The entire journal entry data is **duplicated** inside the user document. If an entry is updated, you'd have to update it in two places.

**With `@DBRef` (Referencing):**
```json
{
    "_id": "...",
    "username": "john",
    "journalEntries": [
        { "$ref": "journal_entries", "$id": "507f..." },
        { "$ref": "journal_entries", "$id": "508f..." }
    ]
}
```
Only **references** (pointers) are stored. The actual entry data lives in the `journal_entries` collection. When you access `user.getJournalEntries()`, Spring automatically resolves these references and fetches the full data.

> **Analogy:** Think of a library catalog. The catalog doesn't contain the full books -- it contains **call numbers** that tell you where to find each book on the shelves. `@DBRef` stores the call numbers, and Spring acts as the librarian who fetches the actual books for you.

### Understanding `@Indexed(unique = true)`

```java
@Indexed(unique = true)
private String username;
```

This does two things:

1. **Creates an index** -- Makes searching by username much faster (like an index at the back of a textbook)
2. **Enforces uniqueness** -- Prevents duplicate usernames

**What happens without the index:**
To find user "john", MongoDB would scan EVERY document in the collection one by one (called a "full collection scan"). With 1 million users, that's slow.

**What happens with the index:**
MongoDB maintains a sorted lookup table for usernames. Finding "john" is like looking up a word in a dictionary -- you jump straight to the "J" section instead of reading every page.

**The `auto-index-creation` requirement:**
In `application.properties`, this line is crucial:
```properties
spring.data.mongodb.auto-index-creation=true
```
By default, Spring Boot does NOT create indexes defined with `@Indexed`. This property tells Spring to actually create them at startup.

---

## Repositories (The Data Access Layer)

Repositories are interfaces that provide methods to interact with MongoDB. You define the interface, and **Spring generates the implementation automatically at runtime**.

> **Analogy:** A repository is like a **universal remote control** that comes pre-programmed with all the basic buttons (save, find, delete). For special functions, you just tell it what you want, and it figures out how to do it.

---

### JournalEntryRepository

**File:** `src/main/java/com/journalApp/repository/JournalEntryRepository.java`

```java
public interface JournalEntryRepository extends MongoRepository<JournalEntry, ObjectId> {
}
```

**That's it -- just one line of actual code.** But this single interface gives you all of these methods for free:

| Method | What It Does |
|---|---|
| `save(entity)` | Saves a new document or updates an existing one |
| `findById(id)` | Finds a document by its `_id` |
| `findAll()` | Returns all documents in the collection |
| `deleteById(id)` | Deletes a document by its `_id` |
| `count()` | Returns the total number of documents |
| `existsById(id)` | Checks if a document exists |
| ...and many more | |

**How `MongoRepository<JournalEntry, ObjectId>` works:**
- First type parameter (`JournalEntry`) -- the entity type this repository manages
- Second type parameter (`ObjectId`) -- the type of the entity's ID field

Spring reads these types and auto-generates the appropriate MongoDB queries.

---

### UserRepository

**File:** `src/main/java/com/journalApp/repository/UserRepository.java`

```java
public interface UserRepository extends MongoRepository<User, ObjectId> {
    User findByUsername(String username);
}
```

This repository has everything `JournalEntryRepository` has, PLUS one custom method:

#### `findByUsername(String username)` -- Query Derivation Magic

You didn't write any query code, yet this method works. How?

Spring uses **query derivation** -- it reads the method name and generates the MongoDB query automatically:

```
findByUsername(String username)
  │     │
  │     └── Field name in the User class: "username"
  └── Operation: "find" (search)
```

Spring translates this to the MongoDB query:
```javascript
db.users.findOne({ "username": "john" })
```

**More examples of query derivation (not in this project, but available):**

| Method Name | Generated Query |
|---|---|
| `findByUsername(String name)` | `{ "username": name }` |
| `findByPassword(String pw)` | `{ "password": pw }` |
| `findByUsernameAndPassword(String u, String p)` | `{ "username": u, "password": p }` |
| `findByUsernameContaining(String part)` | `{ "username": { $regex: part } }` |
| `countByUsername(String name)` | Counts documents where `username` matches |
| `deleteByUsername(String name)` | Deletes documents where `username` matches |

The naming convention is: `findBy` + `FieldName` + optional operator (`And`, `Or`, `Containing`, `GreaterThan`, etc.).

---

## How the Two Collections Relate

```
┌─────────────────────────────────────┐
│          users collection           │
│                                     │
│  ┌───────────────────────────────┐  │
│  │  _id: ObjectId("aaa...")      │  │
│  │  username: "john"             │  │
│  │  password: "pass123"          │  │
│  │  journalEntries: [            │  │
│  │    ──► ref to ObjectId("111") │──┼──┐
│  │    ──► ref to ObjectId("222") │──┼──┼──┐
│  │  ]                            │  │  │  │
│  └───────────────────────────────┘  │  │  │
│                                     │  │  │
│  ┌───────────────────────────────┐  │  │  │
│  │  _id: ObjectId("bbb...")      │  │  │  │
│  │  username: "jane"             │  │  │  │
│  │  password: "pass456"          │  │  │  │
│  │  journalEntries: [            │  │  │  │
│  │    ──► ref to ObjectId("333") │──┼──┼──┼──┐
│  │  ]                            │  │  │  │  │
│  └───────────────────────────────┘  │  │  │  │
└─────────────────────────────────────┘  │  │  │
                                         │  │  │
┌─────────────────────────────────────┐  │  │  │
│    journal_entries collection       │  │  │  │
│                                     │  │  │  │
│  ┌───────────────────────────────┐  │  │  │  │
│  │  _id: ObjectId("111...")   ◄──┼──┼──┘  │  │
│  │  title: "My First Day"       │  │     │  │
│  │  content: "Great day!"       │  │     │  │
│  │  date: 2026-03-16T10:00      │  │     │  │
│  └───────────────────────────────┘  │     │  │
│                                     │     │  │
│  ┌───────────────────────────────┐  │     │  │
│  │  _id: ObjectId("222...")   ◄──┼──┼─────┘  │
│  │  title: "Learning Spring"    │  │        │
│  │  content: "Annotations..."   │  │        │
│  │  date: 2026-03-16T14:00      │  │        │
│  └───────────────────────────────┘  │        │
│                                     │        │
│  ┌───────────────────────────────┐  │        │
│  │  _id: ObjectId("333...")   ◄──┼──┼────────┘
│  │  title: "Jane's Entry"       │  │
│  │  content: "Hello world!"     │  │
│  │  date: 2026-03-16T16:00      │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

**Key Points:**
- Each user **references** (not embeds) their journal entries
- A journal entry belongs to **one user** (one-to-many relationship)
- When a user is fetched with `@DBRef`, Spring automatically loads the referenced journal entries

---

## Lombok's Role in the Entities

Without Lombok, the `JournalEntry` class would look like this:

```java
@Document(collection = "journal_entries")
public class JournalEntry {
    @Id
    private ObjectId id;
    private String title;
    private String content;
    private LocalDateTime date;

    // No-args constructor
    public JournalEntry() {}

    // Getter and Setter for id
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    // Getter and Setter for title (with null check)
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) {
        if (title == null) throw new NullPointerException("title is marked @NonNull");
        this.title = title;
    }

    // Getter and Setter for content
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    // Getter and Setter for date
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    // toString
    @Override
    public String toString() {
        return "JournalEntry(id=" + id + ", title=" + title +
               ", content=" + content + ", date=" + date + ")";
    }

    // equals
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JournalEntry that = (JournalEntry) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(title, that.title) &&
               Objects.equals(content, that.content) &&
               Objects.equals(date, that.date);
    }

    // hashCode
    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, date);
    }
}
```

**With Lombok (`@Data` + `@NoArgsConstructor`), all of that is just:**

```java
@Document(collection = "journal_entries")
@Data
@NoArgsConstructor
public class JournalEntry {
    @Id
    private ObjectId id;
    @NonNull
    private String title;
    private String content;
    private LocalDateTime date;
}
```

That's **~50 lines reduced to ~10 lines** -- and Lombok generates the same code at compile time.

---

## Transaction Management in MongoDB

### The Problem

When saving a journal entry, two database operations happen:
1. Save the entry to `journal_entries`
2. Update the user's `journalEntries` reference list

If operation 2 fails, operation 1 has already completed. You now have an orphan entry.

### The Solution

In `JournalApp.java`:

```java
@EnableTransactionManagement
public class JournalApp {

    @Bean
    public PlatformTransactionManager add(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
```

And in `JournalEntryService.java`:

```java
@Transactional
public void saveEntry(JournalEntry journalEntry, String username) {
    // ... both operations happen here
    // If anything fails, BOTH are rolled back
}
```

**How it works:**

```
Without @Transactional:              With @Transactional:
┌───────────────────┐                ┌───────────────────────────┐
│ Op 1: Save entry ✓│                │ BEGIN TRANSACTION         │
│ Op 2: Update user ✗│               │   Op 1: Save entry ✓     │
│                    │                │   Op 2: Update user ✗    │
│ Result: Orphan     │                │ ROLLBACK (Op 1 undone)   │
│ entry in DB!       │                │                           │
└───────────────────┘                │ Result: Clean state,      │
                                     │ nothing changed           │
                                     └───────────────────────────┘
```

**Important MongoDB Requirement:** Transactions in MongoDB require a **replica set** (even a single-node replica set). If you're running a standalone MongoDB instance, transactions won't work and `@Transactional` will throw errors. For local development, you can set up a single-node replica set.

---

## Security Note

**The passwords in this project are stored as plain text.** This is acceptable for learning purposes, but in a real application, you should ALWAYS hash passwords using a library like **BCrypt** (provided by Spring Security).

```java
// What this project does (NOT safe for production):
user.setPassword("mypassword123");  // stored as "mypassword123"

// What a production app should do:
user.setPassword(passwordEncoder.encode("mypassword123"));
// stored as "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
```

---

## Summary: How Data Flows Through the Layers

```
JSON Request Body ──► @RequestBody ──► Java Object (JournalEntry / User)
                                            │
                                            ▼
                                    Service Layer
                                    (adds business logic:
                                     timestamps, validation)
                                            │
                                            ▼
                                    Repository.save()
                                            │
                                            ▼
                                    MongoDB Document
                                    (stored in collection)

MongoDB Document ──► Repository.find() ──► Java Object ──► JSON Response
```
