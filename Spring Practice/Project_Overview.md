# Project Overview - journalApp

## What Is This Project?

This is a **Journal Application** built with **Spring Boot**. It lets users:

- Create an account (username + password)
- Write journal entries (title + content)
- View, update, and delete their journal entries
- Each user has their own private list of journal entries

Think of it like a **personal diary app** with a REST API backend -- there's no frontend (no web page), but you can interact with it using tools like **Postman** or **cURL**.

---

## Tech Stack at a Glance

| Technology | Version | What It Does |
|---|---|---|
| **Java** | 21 | The programming language everything is written in |
| **Spring Boot** | 3.5.10 | A framework that makes building Java web apps fast and easy |
| **MongoDB** | -- | A NoSQL database that stores data as JSON-like documents |
| **Maven** | 3.9.12 | A build tool that manages dependencies and compiles the project |
| **Lombok** | 1.18.42 | A library that auto-generates boilerplate code (getters, setters, etc.) |

---

## Why These Technologies?

### Spring Boot
> **Analogy:** Imagine building a house. You *could* lay every brick yourself, or you could use pre-built walls and just assemble them. Spring Boot gives you those pre-built walls.

Spring Boot removes the pain of configuring a Java web application from scratch. Without it, you'd need to write hundreds of lines of XML configuration, set up a server manually, and wire everything together. Spring Boot does all of that with just a few annotations.

### MongoDB (instead of MySQL/PostgreSQL)
> **Analogy:** A traditional SQL database is like a spreadsheet -- rigid rows and columns. MongoDB is like a filing cabinet where each folder can hold different kinds of documents.

MongoDB was chosen because:
- Journal entries and users map naturally to **documents** (JSON objects)
- No need to define strict table schemas upfront
- Easy to embed or reference related data (a user's journal entries)

### Maven
> **Analogy:** Maven is like a shopping list for your project. You write down what libraries you need, and Maven goes to the store (Maven Central Repository) and brings them back for you.

Maven handles:
- Downloading all the libraries (dependencies) your project needs
- Compiling your code
- Running tests
- Packaging the app into a runnable `.jar` file

### Lombok
> **Analogy:** Instead of writing the same "boilerplate" code over and over (getters, setters, constructors), Lombok is like having an assistant who writes it for you automatically.

Without Lombok, every entity class would need 20-30 extra lines of code. With Lombok's `@Data` annotation, all of that is auto-generated at compile time.

---

## Project Structure

```
journalApp/
├── src/main/java/com/journalApp/
│   ├── JournalApp.java                          # The MAIN class -- starts the app
│   ├── controller/                               # Handles incoming HTTP requests
│   │   ├── HealthCheck.java                      # Simple "is the server alive?" endpoint
│   │   ├── JournalEntryController.java           # V1 controller (in-memory, no database)
│   │   ├── JournalEntryControllerV2.java         # V2 controller (uses MongoDB)
│   │   └── UserController.java                   # Manages user accounts
│   ├── journalEntity/                            # Data models (what gets stored)
│   │   ├── JournalEntry.java                     # The journal entry document
│   │   └── User.java                             # The user document
│   ├── repository/                               # Talks directly to MongoDB
│   │   ├── JournalEntryRepository.java           # CRUD for journal entries
│   │   └── UserRepository.java                   # CRUD for users
│   └── service/                                  # Business logic layer
│       ├── JournalEntryService.java              # Logic for journal operations
│       └── UserService.java                      # Logic for user operations
├── src/main/resources/
│   └── application.properties                    # App configuration (DB connection, etc.)
├── src/test/java/
│   └── JournalAppApplicationTests.java           # Basic test file
└── pom.xml                                       # Maven dependencies and build config
```

---

## How the Layers Work Together

This project follows the **Controller → Service → Repository** pattern, which is the standard way to organize Spring Boot applications.

```
  Client (Postman/Browser)
        │
        ▼
  ┌─────────────┐
  │  Controller  │  ← Receives HTTP requests, sends HTTP responses
  └──────┬───────┘
         │ calls
         ▼
  ┌─────────────┐
  │   Service    │  ← Contains business logic (validation, data processing)
  └──────┬───────┘
         │ calls
         ▼
  ┌─────────────┐
  │  Repository  │  ← Talks directly to MongoDB (save, find, delete)
  └──────┬───────┘
         │
         ▼
  ┌─────────────┐
  │   MongoDB    │  ← The actual database storing your data
  └─────────────┘
```

> **Analogy:** Think of a restaurant:
> - **Controller** = The waiter (takes your order and brings your food)
> - **Service** = The chef (prepares the food using recipes/logic)
> - **Repository** = The pantry/fridge (where raw ingredients are stored and retrieved)
> - **MongoDB** = The farm that supplies the pantry

### Why separate layers?

1. **Separation of concerns** -- Each layer has one job. The controller doesn't know *how* data is saved; it just asks the service.
2. **Easy to test** -- You can test each layer independently.
3. **Easy to change** -- Want to switch from MongoDB to PostgreSQL? You only change the repository layer. Everything else stays the same.

---

## Dependencies Explained (pom.xml)

### `spring-boot-starter-web`
Gives you everything needed to build a REST API:
- An embedded **Tomcat** web server (no need to install one separately)
- Support for `@RestController`, `@GetMapping`, `@PostMapping`, etc.
- Automatic JSON conversion (Java objects ↔ JSON)

### `spring-boot-starter-data-mongodb`
Connects your app to MongoDB:
- Provides `MongoRepository` interface (ready-made CRUD operations)
- Auto-configures the MongoDB connection using `application.properties`
- Supports `@Document`, `@DBRef`, `@Indexed`, and other MongoDB annotations

### `spring-boot-starter-test`
Includes testing libraries (JUnit 5, Mockito, etc.) for writing unit and integration tests. The `<scope>test</scope>` tag means it's only available during testing, not in the final app.

### `lombok`
Auto-generates boilerplate code at compile time. The `<scope>provided</scope>` tag means Lombok is only needed during compilation -- it doesn't get bundled into the final `.jar` file because its job is done by then.

---

## Application Configuration

```properties
spring.application.name=journalApp

spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=journaldb
spring.data.mongodb.auto-index-creation=true
```

| Property | What It Does |
|---|---|
| `spring.application.name` | Names your app (shows up in logs and monitoring tools) |
| `spring.data.mongodb.host` | Where MongoDB is running (`localhost` = your own machine) |
| `spring.data.mongodb.port` | MongoDB's default port |
| `spring.data.mongodb.database` | The name of the database to use inside MongoDB |
| `spring.data.mongodb.auto-index-creation` | Tells Spring to automatically create database indexes defined by `@Indexed` |

---

## API Endpoints Summary

### Health Check
| Method | URL | Description |
|---|---|---|
| GET | `/health-check` | Returns `"OK"` if the server is running |

### Journal Entries (V2 -- uses MongoDB)
| Method | URL | Description |
|---|---|---|
| GET | `/journal/{username}` | Get all journal entries for a user |
| POST | `/journal/{username}` | Create a new journal entry for a user |
| GET | `/journal/id/{id}` | Get a specific journal entry by its ID |
| PUT | `/journal/id/{username}/{id}` | Update a journal entry |
| DELETE | `/journal/id/{username}/{id}` | Delete a journal entry |

### Users
| Method | URL | Description |
|---|---|---|
| GET | `/user` | Get all users |
| POST | `/user` | Create a new user |
| PUT | `/user/{userName}` | Update a user's details |

---

## How to Run This Project

1. **Make sure MongoDB is running** on `localhost:27017`
2. Open a terminal in the project root directory
3. Run: `./mvnw spring-boot:run` (Linux/Mac) or `mvnw.cmd spring-boot:run` (Windows)
4. The app starts on `http://localhost:8080`
5. Test with: `GET http://localhost:8080/health-check` -- should return `"OK"`
