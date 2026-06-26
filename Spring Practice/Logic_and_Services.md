# Logic and Services - journalApp

A deep dive into every controller, service, and how the business logic flows from HTTP request to database.

---

## The Big Picture: Request Flow

Every request in this app follows the same path:

```
HTTP Request → Controller → Service → Repository → MongoDB
                                                       │
HTTP Response ← Controller ← Service ← Repository ←───┘
```

Let's walk through each piece.

---

## Part 1: Controllers (The Waiters)

Controllers are the front door of your application. They receive HTTP requests, call the appropriate service, and return HTTP responses.

---

### 1.1 HealthCheck.java -- The Simplest Controller

**Purpose:** A quick way to check if the server is alive and responding.

```java
@RestController
public class HealthCheck {
    @GetMapping("/health-check")
    public String healthCheck() {
        return "OK";
    }
}
```

**What happens when you call `GET /health-check`:**

1. Spring sees the `@GetMapping("/health-check")` annotation
2. It calls the `healthCheck()` method
3. The method returns the string `"OK"`
4. Spring sends `"OK"` back to the client with HTTP status `200 OK`

**Why this is useful:** In production, monitoring tools (like AWS health checks or Kubernetes probes) regularly ping this endpoint. If it returns `"OK"`, the server is healthy. If it doesn't respond, something is wrong.

---

### 1.2 JournalEntryController.java -- The V1 (In-Memory) Controller

**Purpose:** An early version of the journal controller that stores data **in memory** (a `HashMap`), NOT in the database.

> **Important:** This controller is mapped to `/_journal` (with an underscore). The "real" controller is `JournalEntryControllerV2` mapped to `/journal`. This V1 version exists as a learning reference to show how things work without a database.

**How it stores data:**
```java
private Map<Long, JournalEntry> journalEntries = new HashMap<>();
```

This `HashMap` acts as a temporary "database" that lives only in memory. When the server restarts, all data is **lost**.

**Endpoints:**

| Method | URL | What It Does |
|---|---|---|
| `GET /_journal` | Returns all journal entries from the HashMap |
| `POST /_journal` | Creates a new entry (currently returns `true` but does nothing -- the save line is commented out) |
| `GET /_journal/id/{myId}` | Gets one entry by its ID |
| `DELETE /_journal/id/{myId}` | Deletes one entry by its ID |
| `PUT /_journal/id/{id}` | Updates an entry by its ID |

**Key Learning:** This controller demonstrates **basic REST principles** without the complexity of a database. Notice how it doesn't use `@Autowired` or any service -- it manages data directly. This is fine for learning but NOT suitable for real apps because:
- Data is lost on restart
- No validation or business logic
- No separation of concerns

---

### 1.3 JournalEntryControllerV2.java -- The Real Controller

**Purpose:** The production-ready journal controller that uses MongoDB through the service layer.

**Dependencies (injected via `@Autowired`):**
```java
@Autowired
private JournalEntryService journalEntryService;

@Autowired
private UserService userService;
```

This controller doesn't touch the database directly -- it delegates everything to the services.

---

#### Endpoint: Get All Journal Entries for a User

```java
@GetMapping("{username}")
public ResponseEntity<?> getAllJournalEntriesOfUser(@PathVariable String username) {
    User user = userService.findByUserName(username);
    List<JournalEntry> all = user.getJournalEntries();
    if (all != null && !all.isEmpty()) {
        return new ResponseEntity<>(all, HttpStatus.OK);
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
}
```

**Step-by-step:**
1. Extract `username` from the URL (e.g., `/journal/john` → `username = "john"`)
2. Find the user in the database
3. Get their list of journal entries
4. If entries exist → return them with `200 OK`
5. If no entries → return `404 NOT FOUND`

**What is `ResponseEntity<?>`?**
It's a wrapper that lets you control both the response body AND the HTTP status code. The `<?>` means the body can be any type.

| Without ResponseEntity | With ResponseEntity |
|---|---|
| Returns `200 OK` always | You choose the status code |
| No control over headers | Full control over headers |
| Simple but limited | Flexible and professional |

---

#### Endpoint: Create a Journal Entry

```java
@PostMapping("{username}")
public ResponseEntity<JournalEntry> createEntry(@RequestBody JournalEntry myEntry,
                                                  @PathVariable String username) {
    try {
        journalEntryService.saveEntry(myEntry, username);
        return new ResponseEntity<>(myEntry, HttpStatus.CREATED);
    } catch (Exception e) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}
```

**Step-by-step:**
1. Read the journal entry from the request body (JSON → Java object)
2. Extract the username from the URL
3. Call the service to save the entry AND link it to the user
4. If successful → return the entry with `201 CREATED`
5. If anything fails → return `400 BAD REQUEST`

**Why `try-catch`?** The service method might throw an exception (e.g., if the user doesn't exist). The `try-catch` ensures the API returns a clean error response instead of a raw server error.

---

#### Endpoint: Get a Journal Entry by ID

```java
@GetMapping("id/{myId}")
public ResponseEntity<JournalEntry> getJournalEntryById(@PathVariable ObjectId myId) {
    Optional<JournalEntry> journalEntry = journalEntryService.findById(myId);
    if (journalEntry.isPresent()) {
        return new ResponseEntity<>(journalEntry.get(), HttpStatus.OK);
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
}
```

**What is `Optional<JournalEntry>`?**
An `Optional` is a container that may or may not hold a value. It's Java's way of handling "this might be null" safely.

| Old way | With Optional |
|---|---|
| `JournalEntry entry = repo.find(id);` | `Optional<JournalEntry> entry = repo.findById(id);` |
| `if (entry != null) { ... }` | `if (entry.isPresent()) { ... }` |
| Risk of NullPointerException | Forces you to check before using |

---

#### Endpoint: Delete a Journal Entry

```java
@DeleteMapping("id/{username}/{myId}")
public ResponseEntity<?> deleteJournalEntryById(@PathVariable ObjectId myId,
                                                  @PathVariable String username) {
    journalEntryService.deleteByid(myId, username);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}
```

**Step-by-step:**
1. Extract the entry ID and username from the URL
2. Call the service to delete the entry (from both the `journal_entries` collection AND the user's reference list)
3. Return `204 NO CONTENT` -- the standard response for successful deletion (no data to return)

---

#### Endpoint: Update a Journal Entry

```java
@PutMapping("id/{username}/{id}")
public ResponseEntity<?> updateJournalEntryById(@PathVariable ObjectId id,
                                                  @RequestBody JournalEntry newEntry,
                                                  @PathVariable String username) {
    JournalEntry oldEntry = journalEntryService.findById(id).orElse(null);
    if (oldEntry != null) {
        oldEntry.setTitle(
            (newEntry.getTitle() != null && !newEntry.getTitle().equals(""))
                ? newEntry.getTitle() : oldEntry.getTitle()
        );
        oldEntry.setContent(
            (newEntry.getContent() != null && !newEntry.getContent().equals(""))
                ? newEntry.getContent() : oldEntry.getContent()
        );
        journalEntryService.saveEntry(oldEntry);
        return new ResponseEntity<>(oldEntry, HttpStatus.OK);
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
}
```

**Step-by-step:**
1. Find the existing entry by ID
2. If it exists:
   - Update the title ONLY if the new title is not null and not empty (otherwise keep the old one)
   - Update the content ONLY if the new content is not null and not empty
   - Save the updated entry
   - Return the updated entry with `200 OK`
3. If it doesn't exist → return `404 NOT FOUND`

**Smart Update Logic:** This is a **partial update** (PATCH-like behavior). If you only send `{"title": "New Title"}` in the request body, only the title changes -- the content stays the same. This is user-friendly because users don't have to resend fields they don't want to change.

---

### 1.4 UserController.java -- Managing Users

**Purpose:** Handles user registration and updates.

**Endpoints:**

#### Get All Users
```java
@GetMapping
public List<User> getAllUsers() {
    return userService.getAll();
}
```
Returns a list of all users in the database. Simple and direct.

#### Create a User
```java
@PostMapping
public void createUser(@RequestBody User user) {
    userService.saveEntry(user);
}
```
Takes a JSON body with `username` and `password`, creates a new user. Note: this returns `void` (no response body), which means the client gets a `200 OK` with an empty body. A better practice would be to return `201 CREATED`.

#### Update a User
```java
@PutMapping("/{userName}")
public ResponseEntity<?> updateUser(@RequestBody User user, @PathVariable String userName) {
    User userInDB = userService.findByUserName(userName);
    if (userInDB != null) {
        userInDB.setUsername(user.getUsername());
        userInDB.setPassword(user.getPassword());
        userService.saveEntry(userInDB);
    }
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}
```
Finds the user by their current username, then updates their username and password.

---

## Part 2: Services (The Chefs)

Services contain the **business logic** -- the actual rules and processes of your application. They sit between the controller (which handles HTTP) and the repository (which handles the database).

---

### 2.1 JournalEntryService.java

**Purpose:** Manages all journal entry operations with proper business logic.

#### Save a New Entry (with Transaction)

```java
@Transactional
public void saveEntry(JournalEntry journalEntry, String username) {
    try {
        User user = userService.findByUserName(username);
        journalEntry.setDate(LocalDateTime.now());
        JournalEntry saved = journalEntryRepository.save(journalEntry);
        user.getJournalEntries().add(saved);
        userService.saveEntry(user);
    } catch (Exception e) {
        System.out.println(e);
        throw new RuntimeException("An error has occurred while saving this entry", e);
    }
}
```

**This is the most important method in the project.** Here's what happens:

```
Step 1: Find the user → "Does john exist?"
Step 2: Set the current date/time on the entry
Step 3: Save the entry to journal_entries collection → MongoDB assigns it an _id
Step 4: Add the saved entry (with its new _id) to the user's journalEntries list
Step 5: Save the user (updating their list of references)
```

**Why `@Transactional` is critical here:**

Imagine this scenario WITHOUT `@Transactional`:
1. The journal entry gets saved to `journal_entries` ✓
2. Adding it to the user's list FAILS ✗

Now you have an "orphan" entry in the database that no user can see or manage. With `@Transactional`, if step 2 fails, step 1 is automatically **rolled back** (the entry is removed from `journal_entries`).

**Why re-throw the exception?**
```java
throw new RuntimeException("An error has occurred...", e);
```
The `@Transactional` annotation only triggers a rollback when an **unchecked exception** (like `RuntimeException`) is thrown. If the exception is caught and swallowed (not re-thrown), Spring thinks everything went fine and commits the transaction. That's why the `catch` block re-throws it.

---

#### Save an Entry (Simple -- for updates)

```java
public void saveEntry(JournalEntry journalEntry) {
    journalEntryRepository.save(journalEntry);
}
```

This overloaded version is used when **updating** an existing entry. Since the entry is already linked to a user, we don't need to modify the user's list -- just save the updated entry directly.

---

#### Delete an Entry

```java
public void deleteByid(ObjectId id, String username) {
    User user = userService.findByUserName(username);
    user.getJournalEntries().removeIf(x -> x.getId().equals(id));
    userService.saveEntry(user);
    journalEntryRepository.deleteById(id);
}
```

**Two-step deletion:**
1. **Remove the reference** from the user's `journalEntries` list (using a lambda that filters out the matching ID)
2. **Delete the actual document** from the `journal_entries` collection

**Why both steps?** Because of `@DBRef`, the journal entry exists in two places:
- As a document in `journal_entries`
- As a reference in the user's `journalEntries` list

If you only delete the document, the user's list would have a "broken link" (a reference to a document that no longer exists).

**Understanding the Lambda:**
```java
user.getJournalEntries().removeIf(x -> x.getId().equals(id));
```
This reads as: "From the user's journal entries list, remove any entry `x` where `x`'s ID equals the given `id`."

---

#### Other Methods

```java
public List<JournalEntry> getAll() {
    return journalEntryRepository.findAll();
}

public Optional<JournalEntry> findById(ObjectId id) {
    return journalEntryRepository.findById(id);
}
```

These are **pass-through** methods -- they simply call the repository. In a more complex app, you'd add validation, authorization checks, or data transformation here.

---

### 2.2 UserService.java

**Purpose:** Manages all user operations.

```java
@Component
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public void saveEntry(User user) {
        try {
            userRepository.save(user);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(ObjectId id) {
        return userRepository.findById(id);
    }

    public void deleteByid(ObjectId id) {
        userRepository.deleteById(id);
    }

    public User findByUserName(String username) {
        return userRepository.findByUsername(username);
    }
}
```

**Key Methods:**

| Method | What It Does |
|---|---|
| `saveEntry(user)` | Saves a new user or updates an existing one (MongoDB's `save()` does both) |
| `getAll()` | Returns all users |
| `findById(id)` | Finds a user by their MongoDB ObjectId |
| `deleteByid(id)` | Deletes a user by their ObjectId |
| `findByUserName(username)` | Finds a user by their username (uses a custom repository method) |

**Note about `saveEntry`:** The `try-catch` here silently catches exceptions (just prints them). This means if a user save fails, the caller doesn't know. In production, you'd want to either re-throw or return a meaningful error.

---

## Part 3: Key Concepts Used in the Logic

### 3.1 ResponseEntity -- Controlling HTTP Responses

`ResponseEntity` lets you set the **status code**, **headers**, and **body** of the HTTP response.

**Common Status Codes Used:**

| Code | Constant | Meaning |
|---|---|---|
| 200 | `HttpStatus.OK` | Request succeeded, here's the data |
| 201 | `HttpStatus.CREATED` | New resource was created successfully |
| 204 | `HttpStatus.NO_CONTENT` | Success, but nothing to return |
| 400 | `HttpStatus.BAD_REQUEST` | The request was invalid |
| 404 | `HttpStatus.NOT_FOUND` | The requested resource doesn't exist |

**Usage patterns in this project:**
```java
// Return data with a status code
return new ResponseEntity<>(data, HttpStatus.OK);

// Return just a status code (no body)
return new ResponseEntity<>(HttpStatus.NOT_FOUND);
```

---

### 3.2 The "save" Method -- Create vs Update

MongoDB's `save()` method is smart:
- If the document has **no `_id`** (or `_id` is null) → it **creates** a new document
- If the document has **an existing `_id`** → it **updates** that document

That's why the same `save()` method is used for both creating and updating in this project.

---

### 3.3 Overloaded Methods

`JournalEntryService` has two `saveEntry` methods:

```java
public void saveEntry(JournalEntry journalEntry, String username) { ... }  // For NEW entries
public void saveEntry(JournalEntry journalEntry) { ... }                   // For UPDATES
```

Java allows methods with the same name if they have **different parameters** (this is called "method overloading"). Spring and the compiler know which one to call based on the arguments you pass.

---

### 3.4 The V1 vs V2 Controller Pattern

This project shows an evolution pattern:

| | V1 (JournalEntryController) | V2 (JournalEntryControllerV2) |
|---|---|---|
| **URL** | `/_journal` | `/journal` |
| **Storage** | In-memory HashMap | MongoDB |
| **Services** | None | JournalEntryService, UserService |
| **Error Handling** | None | try-catch with ResponseEntity |
| **User Support** | No | Yes -- entries belong to users |

This is a common real-world pattern: you build a simple version first, then iterate to a better one. The V1 stays in the codebase as a reference (mapped to a different URL so it doesn't conflict).

---

## Flow Diagrams for Key Operations

### Creating a Journal Entry

```
Client sends POST /journal/john with body: {"title": "My Day", "content": "Great day!"}
    │
    ▼
JournalEntryControllerV2.createEntry()
    │  reads @RequestBody → JournalEntry object
    │  reads @PathVariable → username = "john"
    │
    ▼
JournalEntryService.saveEntry(entry, "john")    ← @Transactional starts
    │
    ├── userService.findByUserName("john")       → finds User from DB
    ├── entry.setDate(LocalDateTime.now())        → stamps the current time
    ├── journalEntryRepository.save(entry)        → saves to journal_entries collection
    ├── user.getJournalEntries().add(savedEntry)  → adds reference to user's list
    └── userService.saveEntry(user)               → saves updated user
    │
    ▼                                             ← @Transactional commits (or rolls back)
Controller returns ResponseEntity with 201 CREATED
    │
    ▼
Client receives: {"title": "My Day", "content": "Great day!", "date": "2026-03-16T..."}
```

### Deleting a Journal Entry

```
Client sends DELETE /journal/id/john/507f1f77bcf86cd799439011
    │
    ▼
JournalEntryControllerV2.deleteJournalEntryById()
    │  reads @PathVariable → myId, username
    │
    ▼
JournalEntryService.deleteByid(myId, "john")
    │
    ├── userService.findByUserName("john")        → finds User
    ├── user.getJournalEntries().removeIf(...)     → removes reference from user's list
    ├── userService.saveEntry(user)                → saves updated user
    └── journalEntryRepository.deleteById(myId)    → deletes from journal_entries
    │
    ▼
Controller returns ResponseEntity with 204 NO CONTENT
```
