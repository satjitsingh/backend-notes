# Chapter 13: JDBC & Database Connectivity

[← Back to Index](00-README.md) | [Previous: Networking](12-Networking.md) | [Next: Design Patterns →](14-Design-Patterns.md)

---

## What You'll Learn

By the end of this chapter you will be able to:

- **Connect** to a database from Java and run SQL (SELECT, INSERT, UPDATE, DELETE).
- **Use PreparedStatement** so your code is safe from SQL injection and easy to maintain.
- **Handle transactions** (commit/rollback) for multi-step operations.
- **Manage resources** correctly (connections, statements, result sets) with try-with-resources.
- **Apply good practices**: connection pooling, DAO pattern, and when to use batch processing.
- **Explain** JDBC concepts clearly to a senior engineer (summaries and a self-check are included).

The chapter is ordered from basics to advanced: Layer 1 (foundation), Layer 2 (daily use), Layer 3 (pooling, stored procedures, BLOB/CLOB), and Layer 4 (interview-level depth).

---

## Why This Chapter Matters

In the real world, applications rarely exist in isolation. They need to persist data, retrieve information, and maintain state across sessions. Whether you're building an e-commerce platform storing product catalogs, a banking system managing transactions, or a social media app tracking user interactions, **database connectivity is the backbone of modern software**.

JDBC (Java Database Connectivity) is Java's standard API for connecting to relational databases. It provides a uniform interface to interact with different database systems (MySQL, PostgreSQL, Oracle, SQL Server) without changing your application code. Understanding JDBC is not just about writing SQL queries—it's about:

- **Data Persistence**: Storing and retrieving application data reliably
- **Transaction Management**: Ensuring data integrity through ACID properties
- **Performance Optimization**: Connection pooling, batch processing, and query optimization
- **Security**: Preventing SQL injection and managing database credentials safely
- **Scalability**: Building systems that can handle concurrent database operations efficiently

Mastering JDBC is essential because even when you use modern frameworks like Hibernate or JPA, understanding the underlying JDBC concepts helps you debug issues, optimize performance, and make informed architectural decisions.

---

## Before You Start: Prerequisites & Setup

### What You Should Know

- **Basic Java**: variables, classes, `try`/`catch`, interfaces.
- **Basic SQL**: `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and what a table/row/column is.
- **A database**: MySQL, PostgreSQL, H2, or any JDBC‑compatible database installed (or use an in-memory DB for practice).

If SQL is new to you, think of it as: *"Tell the database what data you want (SELECT), or what to change (INSERT/UPDATE/DELETE)."*

### Adding the JDBC Driver to Your Project

You need the **JDBC driver JAR** for your database. It’s the bridge between Java and the database.

**Using Maven** (add to `pom.xml`):

```xml
<!-- MySQL driver (choose the one for your database) -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.33</version>
</dependency>
```

**Using Gradle** (add to `build.gradle`):

```groovy
dependencies {
    implementation 'com.mysql:mysql-connector-j:8.0.33'
}
```

**Other common drivers:**

| Database   | GroupId           | ArtifactId           |
|-----------|--------------------|-----------------------|
| MySQL     | com.mysql          | mysql-connector-j    |
| PostgreSQL| org.postgresql     | postgresql           |
| H2        | com.h2database     | h2                   |
| Oracle    | com.oracle.database.jdbc | ojdbc11        |

### Sample Database Schema (Used in This Chapter)

All examples in this chapter use these tables. Create them in your database so you can run the code as-is.

**Table: `users`**

```sql
CREATE TABLE users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) UNIQUE,
    age         INT,
    phone       VARCHAR(50),
    biography   TEXT,
    profile_picture BLOB,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Table: `accounts`** (for transaction examples)

```sql
CREATE TABLE accounts (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0
);

-- Sample rows
INSERT INTO accounts (id, balance) VALUES (1, 1000.00), (2, 500.00);
```

**Table: `orders` and `order_items`** (for N+1 / batch examples)

```sql
CREATE TABLE orders (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL
);

CREATE TABLE order_items (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    order_id     INT NOT NULL,
    product_name VARCHAR(255),
    quantity     INT,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
```

Once this schema exists, every example in the chapter can run against the same data.

---

## Layer 1 — Beginner Foundation

### What is JDBC and Why It Exists

**JDBC** (Java Database Connectivity) is a Java API that provides a standard way to access relational databases. Before JDBC, each database vendor had proprietary APIs, making it difficult to switch databases or write portable code.

**Simple analogy:** JDBC is like a standard "plug and socket" for databases. Your Java code is the plug; the driver is the socket for MySQL, PostgreSQL, etc. You write the same plug (same Java code); you only change the socket (driver) when you switch databases.

JDBC solves this by:
- Providing a **vendor-neutral interface** for database operations
- Allowing **database independence**—switch databases by changing the driver
- Enabling **standardized SQL execution** across different database systems

### JDBC Architecture

JDBC follows a layered architecture:

```
Application Code
      ↓
JDBC API (java.sql.*)
      ↓
JDBC Driver Manager
      ↓
JDBC Driver (Database-specific)
      ↓
Database
```

**Key Components:**

| Component   | What it is in plain English |
|------------|------------------------------|
| **Driver** | Database-specific code (JAR) that turns JDBC calls into the protocol your database understands. |
| **Connection** | An open "session" to the database. Expensive to create; reuse via pooling in real apps. |
| **Statement** | A carrier for one SQL command. You create it from a `Connection`, set SQL, then execute. |
| **ResultSet** | A cursor over the rows returned by a `SELECT`. You call `next()` to move row-by-row and `getXxx()` to read column values. |

**Mental model:** You get a **Connection** (like a phone line). Over that line you send **Statements** (SQL text). For `SELECT`, the database sends back a **ResultSet** (a list of rows); for `INSERT`/`UPDATE`/`DELETE` you get a count of affected rows.

### Loading Drivers and Establishing Connections

**Traditional Approach (JDBC 4.0+ auto-loading):**

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class BasicConnection {
    public static void main(String[] args) {
        // JDBC 4.0+ automatically loads drivers from classpath
        // No need for Class.forName("com.mysql.cj.jdbc.Driver") anymore

        // 1. URL tells JDBC *which* database and *where* it is
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";

        // 2. try-with-resources ensures the connection is closed when the block ends
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("Connection successful!");
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }
}
```

**What happens step by step:**

1. **`DriverManager.getConnection(url, username, password)`** — The JDBC driver (from your JAR) is found on the classpath. It opens a TCP connection to the database, authenticates with `username`/`password`, and returns a `Connection` object.
2. **`try (Connection conn = ...)`** — When the `try` block finishes (normally or by exception), `conn.close()` is called automatically. Always close connections; otherwise you leak resources and can exhaust the database’s connection limit.

**Connection URL Formats:**

| Database   | URL pattern |
|-----------|-------------|
| MySQL     | `jdbc:mysql://hostname:port/database` e.g. `jdbc:mysql://localhost:3306/mydb` |
| PostgreSQL| `jdbc:postgresql://hostname:port/database` |
| H2 (file) | `jdbc:h2:file:./data/mydb` |
| H2 (memory) | `jdbc:h2:mem:testdb` |
| Oracle    | `jdbc:oracle:thin:@hostname:port:serviceName` |
| SQL Server| `jdbc:sqlserver://hostname:port;databaseName=database` |

Replace `hostname`, `port`, and `database` with your actual host, port, and database name.

**Run your first program:** Put `BasicConnection` in `src/main/java` and run it. From the project root:

- **Maven:** `mvn compile exec:java -Dexec.mainClass="BasicConnection"`
- **Gradle:** `gradle run` (with `application { mainClass = "BasicConnection" }` in `build.gradle`)
- **IDE:** Right‑click the class → Run.

If you see "Connection successful!", JDBC and your driver are set up correctly.

### Basic CRUD Operations

CRUD = **C**reate, **R**ead, **U**pdate, **D**elete. In JDBC you use two execution methods:

- **`executeUpdate(sql)`** — For `INSERT`, `UPDATE`, `DELETE`. Returns the number of rows affected (int).
- **`executeQuery(sql)`** — For `SELECT`. Returns a `ResultSet` (rows) that you read with `next()` and `getXxx()`.

**Create (INSERT):**

*Goal: Add one row to the `users` table.*

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            String sql = "INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com')";
            int rowsAffected = stmt.executeUpdate(sql);  // Use executeUpdate for INSERT
            System.out.println(rowsAffected + " row(s) inserted");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

**Read (SELECT):**

*Goal: Fetch all rows from `users` and print them. ResultSet is like an iterator over rows.*

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ReadExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, email FROM users")) {

            // ResultSet starts *before* the first row; next() moves to the next row and returns false when there are no more
            while (rs.next()) {
                int id = rs.getInt("id");           // by column name (recommended)
                String name = rs.getString("name");
                String email = rs.getString("email");
                // You can also use column index: rs.getInt(1), rs.getString(2), etc.
                System.out.println("ID: " + id + ", Name: " + name + ", Email: " + email);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

*Important:* Always use `executeQuery` for `SELECT`. The `ResultSet` must be read (or closed) before you can execute another statement on the same connection in most drivers.

**Update:**

*Goal: Change the email of the user with `id = 1`.*

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class UpdateExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            String sql = "UPDATE users SET email = 'newemail@example.com' WHERE id = 1";
            int rowsAffected = stmt.executeUpdate(sql);  // 1 if a row was updated, 0 if no row matched
            System.out.println(rowsAffected + " row(s) updated");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

**Delete:**

*Goal: Remove the user with `id = 1`.*

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DeleteExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            String sql = "DELETE FROM users WHERE id = 1";
            int rowsAffected = stmt.executeUpdate(sql);
            System.out.println(rowsAffected + " row(s) deleted");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### Understanding SQL Injection Risks

**Vulnerable Code (NEVER DO THIS):**

```java
// DANGEROUS - SQL Injection vulnerability
String userInput = request.getParameter("userId");
String sql = "SELECT * FROM users WHERE id = " + userInput;
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);
```

**Attack Example:**
If `userInput = "1 OR 1=1"`, the query becomes:
```sql
SELECT * FROM users WHERE id = 1 OR 1=1
```
This returns ALL users, potentially exposing sensitive data!

**Solution:** Always use `PreparedStatement` (covered in Layer 2).

### Common Beginner Mistakes and How to Avoid Them

| Mistake | What goes wrong | Fix |
|--------|------------------|-----|
| Concatenating user input into SQL | SQL injection; possible data breach. | Use `PreparedStatement` and `?` placeholders. |
| Forgetting to close `Connection` / `ResultSet` | Connection leaks; eventually "too many connections". | Use try-with-resources: `try (Connection c = ...; Statement s = ...) { }`. |
| Setting parameters after `executeQuery()` | Wrong or empty results; parameters are ignored. | Call `setXxx(1, value)` (and 2, 3, …) **before** `executeQuery()` or `executeUpdate()`. |
| Using `executeUpdate()` for SELECT | Throws or wrong behavior. | Use `executeUpdate()` for INSERT/UPDATE/DELETE; use `executeQuery()` for SELECT. |
| Using `executeQuery()` for INSERT/UPDATE/DELETE | Throws or wrong behavior. | Use `executeQuery()` only for SELECT. |
| Wrong URL or credentials | `SQLException` like "Access denied" or "Unknown database". | Check URL (host, port, database name), username, password, and that the DB is running. |
| Driver JAR not on classpath | "No suitable driver found for jdbc:mysql://...". | Add the JDBC driver dependency (e.g. MySQL Connector/J) to Maven/Gradle and rebuild. |
| Reading column by wrong type | `getInt()` on a string column, etc. | Use the correct `getXxx()` for the column type, or use `getObject()` and handle type. |

---

**Layer 1 summary — How to explain JDBC to a senior**

You can say: *"JDBC is the standard Java API for talking to relational databases. My code uses the `java.sql` interfaces; the actual network protocol is implemented by a driver JAR (e.g. MySQL Connector/J). I get a `Connection` from `DriverManager`, create `Statement` or `PreparedStatement` from it, and for SELECT I get a `ResultSet` and iterate with `next()` and `getXxx()`. I always use try-with-resources so connections and result sets are closed. For any user input in SQL I use `PreparedStatement` so I never concatenate input into the query string."*

---

## Layer 2 — Working Developer Level

### PreparedStatement vs Statement

**Why PreparedStatement is Mandatory:**

1. **Prevents SQL Injection**: Parameters are escaped automatically
2. **Performance**: Pre-compiled SQL is cached and reused
3. **Type Safety**: Compile-time checking of parameter types
4. **Readability**: Cleaner code with parameter placeholders

**Statement (Avoid in Production):**

```java
// Vulnerable to SQL injection
String sql = "SELECT * FROM users WHERE email = '" + email + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);
```

**PreparedStatement (Always Use):**

*Rule: Set every `?` with the right `setXxx(index, value)` **before** calling `executeQuery()` or `executeUpdate()`.*

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PreparedStatementExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // INSERT with PreparedStatement
            String insertSql = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, "Jane Doe");
                pstmt.setString(2, "jane@example.com");
                pstmt.setInt(3, 30);
                pstmt.executeUpdate();
            }
            
            // SELECT with PreparedStatement — set parameters BEFORE executeQuery()
            String selectSql = "SELECT * FROM users WHERE email = ? AND age > ?";
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setString(1, "jane@example.com");
                pstmt.setInt(2, 25);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("Name: " + rs.getString("name"));
                        System.out.println("Email: " + rs.getString("email"));
                    }
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

**PreparedStatement Methods:**
- `setString(int index, String value)`
- `setInt(int index, int value)`
- `setLong(int index, long value)`
- `setDouble(int index, double value)`
- `setDate(int index, Date value)`
- `setTimestamp(int index, Timestamp value)`
- `setNull(int index, int sqlType)`

### Batch Processing

Batch processing improves performance by executing multiple statements in a single database round-trip.

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BatchProcessingExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(false); // Start transaction

            try {
                String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= 1000; i++) {
                        pstmt.setString(1, "User " + i);
                        pstmt.setString(2, "user" + i + "@example.com");
                        pstmt.addBatch();
                        if (i % 100 == 0) {
                            pstmt.executeBatch();
                            pstmt.clearBatch();
                            System.out.println("Inserted batch of 100 records");
                        }
                    }
                    pstmt.executeBatch();  // remaining rows
                }
                conn.commit();
                System.out.println("All batches committed");
            } catch (SQLException e) {
                conn.rollback();  // Same connection that did the work
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### Transaction Management

Transactions ensure **ACID** properties:
- **Atomicity**: All or nothing
- **Consistency**: Database remains in valid state
- **Isolation**: Concurrent transactions don't interfere
- **Durability**: Committed changes persist

**Basic Transaction Example:**

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;

public class TransactionExample {
    public static void transferMoney(Connection conn, int fromAccount, 
                                     int toAccount, double amount) throws SQLException {
        try {
            conn.setAutoCommit(false); // Start transaction
            
            // Deduct from source account
            String deductSql = "UPDATE accounts SET balance = balance - ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deductSql)) {
                pstmt.setDouble(1, amount);
                pstmt.setInt(2, fromAccount);
                int rowsUpdated = pstmt.executeUpdate();
                
                if (rowsUpdated == 0) {
                    throw new SQLException("Source account not found");
                }
            }
            
            // Add to destination account
            String addSql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(addSql)) {
                pstmt.setDouble(1, amount);
                pstmt.setInt(2, toAccount);
                int rowsUpdated = pstmt.executeUpdate();
                
                if (rowsUpdated == 0) {
                    throw new SQLException("Destination account not found");
                }
            }
            
            conn.commit(); // Commit transaction
            System.out.println("Transfer successful");
            
        } catch (SQLException e) {
            conn.rollback(); // Rollback on error
            System.err.println("Transfer failed: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true); // Restore auto-commit
        }
    }
    
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            transferMoney(conn, 1, 2, 100.0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

**Savepoints (Partial Rollback):**

```java
public class SavepointExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(false);
            
            // First operation
            PreparedStatement pstmt1 = conn.prepareStatement("INSERT INTO users (name) VALUES (?)");
            pstmt1.setString(1, "User 1");
            pstmt1.executeUpdate();
            
            // Create savepoint
            Savepoint savepoint = conn.setSavepoint("after_user1");
            
            // Second operation
            PreparedStatement pstmt2 = conn.prepareStatement("INSERT INTO users (name) VALUES (?)");
            pstmt2.setString(1, "User 2");
            pstmt2.executeUpdate();
            
            // Rollback to savepoint (undoes second operation, keeps first)
            conn.rollback(savepoint);
            
            // Commit (only first operation is committed)
            conn.commit();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### Connection Management Best Practices

**1. Always Use Try-With-Resources:**

```java
// GOOD - Automatic resource cleanup
try (Connection conn = DriverManager.getConnection(url, username, password);
     PreparedStatement pstmt = conn.prepareStatement(sql);
     ResultSet rs = pstmt.executeQuery()) {
    // Use resources
} // All resources automatically closed
```

**2. Close Resources in Reverse Order:**

```java
// If not using try-with-resources
Connection conn = null;
PreparedStatement pstmt = null;
ResultSet rs = null;

try {
    conn = DriverManager.getConnection(url, username, password);
    pstmt = conn.prepareStatement(sql);
    rs = pstmt.executeQuery();
    // Use resources
} finally {
    // Close in reverse order
    if (rs != null) rs.close();
    if (pstmt != null) pstmt.close();
    if (conn != null) conn.close();
}
```

**3. Never Share Connections Across Threads:**

```java
// BAD - Connection is not thread-safe
public class BadConnectionSharing {
    private static Connection sharedConnection; // DON'T DO THIS

    public static Connection getConnection() {
        if (sharedConnection == null) {
            sharedConnection = DriverManager.getConnection(url, username, password);
        }
        return sharedConnection;
    }
}
```

**Layer 2 summary — How to explain to a senior**

*"For any SQL that includes user input I use PreparedStatement with `?` placeholders and set parameters with `setXxx()` before executing — that avoids SQL injection and lets the driver cache the prepared plan. For multi-step updates I use a transaction: setAutoCommit(false), run the statements, commit(), and on failure rollback(). I always close resources in reverse order or use try-with-resources. For production I’d use a DataSource with a connection pool like HikariCP instead of DriverManager.getConnection() every time."*

### ResultSet Types

**1. Forward-Only (Default):**

```java
// Can only move forward
Statement stmt = conn.createStatement(
    ResultSet.TYPE_FORWARD_ONLY,
    ResultSet.CONCUR_READ_ONLY
);
```

**2. Scrollable:**

```java
// Can move forward and backward
Statement stmt = conn.createStatement(
    ResultSet.TYPE_SCROLL_INSENSITIVE, // Changes in DB not visible
    ResultSet.CONCUR_READ_ONLY
);

ResultSet rs = stmt.executeQuery("SELECT * FROM users");
rs.next();        // Move forward
rs.previous();    // Move backward
rs.first();       // Move to first row
rs.last();        // Move to last row
rs.absolute(5);   // Move to row 5
rs.relative(-2);  // Move 2 rows back
```

**3. Updatable:**

```java
// Can update rows directly
Statement stmt = conn.createStatement(
    ResultSet.TYPE_SCROLL_SENSITIVE, // Changes in DB visible
    ResultSet.CONCUR_UPDATABLE
);

ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = 1");
if (rs.next()) {
    rs.updateString("name", "Updated Name");
    rs.updateRow(); // Commit the update
}
```

### Handling NULL Values

```java
public class NullHandlingExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";
        
        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement pstmt = conn.prepareStatement("SELECT name, email, phone FROM users");
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                String name = rs.getString("name");
                
                // Check for NULL before using
                String email = rs.getString("email");
                if (rs.wasNull()) {
                    email = "No email provided";
                }
                
                // Using getObject() and checking for null
                Object phoneObj = rs.getObject("phone");
                String phone = (phoneObj != null) ? phoneObj.toString() : "N/A";
                
                System.out.println("Name: " + name + ", Email: " + email + ", Phone: " + phone);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### DAO Pattern Basics

**Data Access Object (DAO)** pattern separates data access logic from business logic: one interface (e.g. `UserDAO`) defines operations like `findById`, `save`, `delete`; the implementation uses JDBC. The rest of the app depends on the interface, so you can test or swap implementations without touching business code.

```java
// User entity
public class User {
    private int id;
    private String name;
    private String email;
    
    // Constructors, getters, setters
    public User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    // Getters and setters...
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}

// UserDAO interface
public interface UserDAO {
    User findById(int id) throws SQLException;
    List<User> findAll() throws SQLException;
    void save(User user) throws SQLException;
    void update(User user) throws SQLException;
    void delete(int id) throws SQLException;
}

// UserDAO implementation
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {
    private final String url;
    private final String username;
    private final String password;
    
    public UserDAOImpl(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
    
    @Override
    public User findById(int id) throws SQLException {
        String sql = "SELECT id, name, email FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email")
                    );
                }
            }
        }
        return null;
    }
    
    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email FROM users";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email")
                ));
            }
        }
        return users;
    }
    
    @Override
    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.executeUpdate();
        }
    }
    
    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setInt(3, user.getId());
            pstmt.executeUpdate();
        }
    }
    
    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}
```

---

## Layer 3 — Advanced Engineering Depth

### Connection Pooling

**Why Connection Pooling is Critical:**

Creating database connections is **expensive** (network overhead, authentication, resource allocation). Connection pooling reuses existing connections, dramatically improving performance:

- **Without Pooling**: Create connection → Use → Close → Repeat (slow)
- **With Pooling**: Get connection from pool → Use → Return to pool (fast)

**Performance Impact:**
- Creating a connection: ~100-200ms
- Getting from pool: ~1-5ms
- **20-100x faster!**

### HikariCP (Industry Standard)

**Maven Dependency:**
```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>
```

**Basic Setup:**

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class HikariCPExample {
    private static HikariDataSource dataSource;
    
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("root");
        config.setPassword("password");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Pool configuration
        config.setMaximumPoolSize(10);        // Max connections in pool
        config.setMinimumIdle(5);            // Min idle connections
        config.setConnectionTimeout(30000);   // 30 seconds
        config.setIdleTimeout(600000);       // 10 minutes
        config.setMaxLifetime(1800000);      // 30 minutes
        config.setLeakDetectionThreshold(60000); // Detect leaks
        
        dataSource = new HikariDataSource(config);
    }
    
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
    
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Connection from pool: " + conn);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }
}
```

**Configuration Properties:**

```java
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(20);              // Maximum pool size
config.setMinimumIdle(5);                  // Minimum idle connections
config.setConnectionTimeout(30000);         // Wait time for connection (ms)
config.setIdleTimeout(600000);              // Max idle time before removal (ms)
config.setMaxLifetime(1800000);            // Max connection lifetime (ms)
config.setLeakDetectionThreshold(60000);   // Leak detection threshold (ms)
config.setPoolName("MyPool");               // Pool name for monitoring
config.setAutoCommit(true);                 // Auto-commit default
config.setReadOnly(false);                  // Read-only default
config.setCatalog("mydb");                  // Default catalog
config.setConnectionTestQuery("SELECT 1");  // Connection test query
```

### DataSource vs DriverManager

**DriverManager (Simple, but Limited):**

```java
// Direct connection - no pooling
Connection conn = DriverManager.getConnection(url, username, password);
```

**DataSource (Recommended for Production):**

```java
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceExample {
    private DataSource dataSource;
    
    public DataSourceExample(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void doWork() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // Use connection
        }
    }
}
```

**Benefits of DataSource:**
- Connection pooling support
- JNDI lookup capability
- Better for enterprise applications
- Standard interface for connection management

### Stored Procedures with CallableStatement

**Creating a Stored Procedure (MySQL):**

```sql
DELIMITER //
CREATE PROCEDURE GetUserById(IN userId INT, OUT userName VARCHAR(255))
BEGIN
    SELECT name INTO userName FROM users WHERE id = userId;
END //
DELIMITER ;
```

**Calling from Java:**

```java
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;

public class StoredProcedureExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // Call stored procedure
            String sql = "{CALL GetUserById(?, ?)}";
            try (CallableStatement cstmt = conn.prepareCall(sql)) {
                // Set IN parameter
                cstmt.setInt(1, 1);
                
                // Register OUT parameter
                cstmt.registerOutParameter(2, Types.VARCHAR);
                
                // Execute
                cstmt.execute();
                
                // Get OUT parameter value
                String userName = cstmt.getString(2);
                System.out.println("User name: " + userName);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

**Stored Procedure with ResultSet:**

```java
// Stored procedure that returns a result set
String sql = "{CALL GetUsersByAge(?)}";
try (CallableStatement cstmt = conn.prepareCall(sql)) {
    cstmt.setInt(1, 25);
    boolean hasResult = cstmt.execute();
    
    if (hasResult) {
        try (ResultSet rs = cstmt.getResultSet()) {
            while (rs.next()) {
                System.out.println("User: " + rs.getString("name"));
            }
        }
    }
}
```

### BLOB/CLOB Handling

**BLOB (Binary Large Object) - Images, PDFs, etc.:**

```java
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BlobExample {
    // Save BLOB
    public static void saveBlob(Connection conn, int userId, String filePath) throws SQLException {
        String sql = "UPDATE users SET profile_picture = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             FileInputStream fis = new FileInputStream(filePath)) {
            
            pstmt.setBinaryStream(1, fis);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }
    
    // Read BLOB
    public static void readBlob(Connection conn, int userId, String outputPath) throws SQLException {
        String sql = "SELECT profile_picture FROM users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    try (InputStream is = rs.getBinaryStream("profile_picture");
                         FileOutputStream fos = new FileOutputStream(outputPath)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
    }
}
```

**CLOB (Character Large Object) - Large Text:**

```java
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClobExample {
    // Save CLOB
    public static void saveClob(Connection conn, int userId, String largeText) throws SQLException {
        String sql = "UPDATE users SET biography = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             Reader reader = new StringReader(largeText)) {
            
            pstmt.setCharacterStream(1, reader, largeText.length());
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }
    
    // Read CLOB
    public static String readClob(Connection conn, int userId) throws SQLException {
        String sql = "SELECT biography FROM users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    try (Reader reader = rs.getCharacterStream("biography")) {
                        StringBuilder sb = new StringBuilder();
                        char[] buffer = new char[4096];
                        int charsRead;
                        while ((charsRead = reader.read(buffer)) != -1) {
                            sb.append(buffer, 0, charsRead);
                        }
                        return sb.toString();
                    }
                }
            }
        }
        return null;
    }
}
```

### Database Metadata

**Getting Database Information:**

```java
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MetadataExample {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Database information
            System.out.println("Database: " + metaData.getDatabaseProductName());
            System.out.println("Version: " + metaData.getDatabaseProductVersion());
            System.out.println("Driver: " + metaData.getDriverName());
            System.out.println("Driver Version: " + metaData.getDriverVersion());
            System.out.println("URL: " + metaData.getURL());
            System.out.println("Username: " + metaData.getUserName());
            
            // Supported features
            System.out.println("Supports transactions: " + metaData.supportsTransactions());
            System.out.println("Supports batch updates: " + metaData.supportsBatchUpdates());
            System.out.println("Supports stored procedures: " + metaData.supportsStoredProcedures());
            
            // List tables
            try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                System.out.println("\nTables:");
                while (tables.next()) {
                    System.out.println("  - " + tables.getString("TABLE_NAME"));
                }
            }
            
            // Get columns for a table
            try (ResultSet columns = metaData.getColumns(null, null, "users", null)) {
                System.out.println("\nColumns in 'users' table:");
                while (columns.next()) {
                    System.out.println("  - " + columns.getString("COLUMN_NAME") + 
                                     " (" + columns.getString("TYPE_NAME") + ")");
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### Distributed Transactions (XA)

**XA transactions** coordinate transactions across multiple databases/resources.

```java
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class XATransactionExample {
    // Simplified example - actual implementation requires JTA (Java Transaction API)
    public static void performDistributedTransaction(
            XADataSource ds1, XADataSource ds2) throws SQLException {
        
        XAConnection xaConn1 = ds1.getXAConnection();
        XAConnection xaConn2 = ds2.getXAConnection();
        
        XAResource xaRes1 = xaConn1.getXAResource();
        XAResource xaRes2 = xaConn2.getXAResource();
        
        // Generate transaction ID
        Xid xid = generateXid();
        
        try {
            // Start transaction on both resources
            xaRes1.start(xid, XAResource.TMNOFLAGS);
            xaRes2.start(xid, XAResource.TMNOFLAGS);
            
            // Perform operations
            Connection conn1 = xaConn1.getConnection();
            Connection conn2 = xaConn2.getConnection();
            
            // Execute SQL on both connections
            // ...
            
            // Prepare (two-phase commit)
            int prepare1 = xaRes1.prepare(xid);
            int prepare2 = xaRes2.prepare(xid);
            
            if (prepare1 == XAResource.XA_OK && prepare2 == XAResource.XA_OK) {
                // Commit both
                xaRes1.commit(xid, false);
                xaRes2.commit(xid, false);
            } else {
                // Rollback both
                xaRes1.rollback(xid);
                xaRes2.rollback(xid);
            }
            
        } finally {
            xaConn1.close();
            xaConn2.close();
        }
    }
    
    private static Xid generateXid() {
        // Implementation to generate unique XID
        return null; // Simplified
    }
}
```

**Note:** Real-world XA transactions typically use JTA (Java Transaction API) with application servers or frameworks like Spring.

### N+1 Query Problem

**The Problem:**

```java
// BAD - N+1 queries
public List<Order> getOrdersWithItems() {
    List<Order> orders = getOrders(); // 1 query
    
    for (Order order : orders) {
        List<OrderItem> items = getOrderItems(order.getId()); // N queries
        order.setItems(items);
    }
    // Total: 1 + N queries
}
```

**Solution 1: Eager Loading with JOIN**

```java
// GOOD - Single query with JOIN
public List<Order> getOrdersWithItems() {
    String sql = """
        SELECT o.id, o.date, oi.id as item_id, oi.product_name, oi.quantity
        FROM orders o
        LEFT JOIN order_items oi ON o.id = oi.order_id
        ORDER BY o.id
        """;
    
    // Process ResultSet to build Order objects with items
    // Single query instead of N+1
}
```

**Solution 2: Batch Loading**

```java
// GOOD - Batch query
public List<Order> getOrdersWithItems() {
    List<Order> orders = getOrders(); // 1 query
    
    List<Integer> orderIds = orders.stream()
        .map(Order::getId)
        .toList();
    
    Map<Integer, List<OrderItem>> itemsMap = getOrderItemsBatch(orderIds); // 1 query
    orders.forEach(order -> order.setItems(itemsMap.get(order.getId())));
    
    // Total: 2 queries instead of N+1
}
```

### ORM vs Raw JDBC Tradeoffs

**Raw JDBC Advantages:**
- Full control over SQL queries
- Better performance for complex queries
- No learning curve for ORM framework
- Lightweight (no dependencies)
- Direct database features access

**Raw JDBC Disadvantages:**
- More boilerplate code
- Manual object-relational mapping
- No automatic relationship handling
- Manual transaction management
- More error-prone

**ORM (Hibernate/JPA) Advantages:**
- Less boilerplate code
- Automatic object-relational mapping
- Relationship management
- Caching support
- Database independence

**ORM Disadvantages:**
- Learning curve
- Performance overhead for simple queries
- Less control over generated SQL
- Can generate inefficient queries
- Framework dependency

**When to Use Each:**
- **Raw JDBC**: Simple applications, performance-critical code, complex queries, learning
- **ORM**: Complex domain models, rapid development, team familiarity with ORM

---

## Layer 4 — Interview Mastery

### Statement vs PreparedStatement vs CallableStatement

**Statement:**
- Used for **static SQL** queries
- **Vulnerable to SQL injection**
- No pre-compilation
- Use case: DDL statements (CREATE, DROP, ALTER)

**PreparedStatement:**
- Used for **parameterized SQL** queries
- **Prevents SQL injection** (parameters are escaped)
- Pre-compiled and cached for performance
- Use case: DML statements (SELECT, INSERT, UPDATE, DELETE) with parameters

**CallableStatement:**
- Used for **stored procedures**
- Extends PreparedStatement
- Supports IN, OUT, and INOUT parameters
- Use case: Calling database stored procedures/functions

**Example Comparison:**

```java
// Statement - Static SQL only
Statement stmt = conn.createStatement();
stmt.executeUpdate("CREATE TABLE users (id INT, name VARCHAR(100))");

// PreparedStatement - Parameterized queries
PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
pstmt.setInt(1, userId);
ResultSet rs = pstmt.executeQuery();

// CallableStatement - Stored procedures
CallableStatement cstmt = conn.prepareCall("{CALL GetUserById(?, ?)}");
cstmt.setInt(1, userId);
cstmt.registerOutParameter(2, Types.VARCHAR);
cstmt.execute();
String userName = cstmt.getString(2);
```

### Explain Connection Pooling

**Connection Pooling** is a technique that maintains a cache of database connections that can be reused across multiple requests.

**How It Works:**
1. Pool is initialized with a minimum number of connections
2. When application needs a connection, it requests from pool
3. Pool provides an available connection or creates a new one (up to max)
4. After use, connection is returned to pool (not closed)
5. Idle connections are kept alive for reuse

**Benefits:**
- **Performance**: Reusing connections is 20-100x faster than creating new ones
- **Resource Management**: Limits total connections to database
- **Scalability**: Handles concurrent requests efficiently
- **Connection Lifecycle**: Manages connection health and cleanup

**Key Configuration Parameters:**
- `maximumPoolSize`: Maximum connections in pool
- `minimumIdle`: Minimum idle connections maintained
- `connectionTimeout`: Max wait time to get connection
- `idleTimeout`: Max time connection can be idle before removal
- `maxLifetime`: Max time before connection is retired

**Example Answer:**
> "Connection pooling maintains a pool of reusable database connections. Instead of creating a new connection for each database operation (which takes 100-200ms), we reuse existing connections from the pool (1-5ms). This dramatically improves performance and allows better resource management. The pool handles connection lifecycle, health checks, and ensures we don't exceed database connection limits."

### How to Prevent SQL Injection

**SQL Injection** occurs when user input is directly concatenated into SQL queries, allowing attackers to execute malicious SQL.

**Prevention Methods:**

1. **Always Use PreparedStatement:**
```java
// BAD
String sql = "SELECT * FROM users WHERE email = '" + email + "'";

// GOOD
PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
pstmt.setString(1, email);
```

2. **Input Validation:**
```java
// Validate and sanitize input
if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
    throw new IllegalArgumentException("Invalid email format");
}
```

3. **Use Parameterized Queries:**
```java
// Parameters are automatically escaped
pstmt.setString(1, userInput); // Safe even if userInput contains SQL
```

4. **Least Privilege Principle:**
```java
// Database user should have minimum required permissions
// Don't use admin/root account for application
```

5. **Avoid Dynamic SQL Construction:**
```java
// BAD - Building SQL dynamically
String sql = "SELECT * FROM " + tableName + " WHERE id = " + id;

// GOOD - Use whitelist for table names
if (!allowedTables.contains(tableName)) {
    throw new SecurityException("Invalid table name");
}
```

**Example Answer:**
> "SQL injection is prevented by using PreparedStatement with parameterized queries. Parameters are automatically escaped, preventing malicious SQL from being executed. Additionally, we validate and sanitize user input, use least-privilege database accounts, and avoid dynamic SQL construction. PreparedStatement is the primary defense because it separates SQL structure from data values."

### Transaction Isolation Levels

**Isolation Levels** control how transactions interact with each other:

1. **READ UNCOMMITTED** (Lowest isolation):
   - Can read uncommitted data from other transactions
   - **Dirty reads** possible
   - **Use case**: Rarely used, maximum performance

2. **READ COMMITTED** (Default in most databases):
   - Can only read committed data
   - Prevents dirty reads
   - **Non-repeatable reads** possible
   - **Use case**: General purpose, good balance

3. **REPEATABLE READ**:
   - Same query returns same results within transaction
   - Prevents dirty reads and non-repeatable reads
   - **Phantom reads** possible
   - **Use case**: When consistency is important

4. **SERIALIZABLE** (Highest isolation):
   - Transactions execute serially
   - Prevents all concurrency issues
   - **Lowest performance**
   - **Use case**: Critical data integrity requirements

**Setting Isolation Level:**

```java
conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
```

**Concurrency Issues:**

- **Dirty Read**: Reading uncommitted data that may be rolled back
- **Non-Repeatable Read**: Same query returns different results within transaction
- **Phantom Read**: New rows appear in result set between reads

**Example Answer:**
> "Transaction isolation levels control how transactions see each other's changes. READ UNCOMMITTED allows dirty reads but has best performance. READ COMMITTED (default) prevents dirty reads but allows non-repeatable reads. REPEATABLE READ ensures consistent reads within a transaction but allows phantom reads. SERIALIZABLE provides highest isolation but lowest performance. Choose based on consistency requirements vs performance needs."

### Design a Simple Connection Pool

**Basic Connection Pool Implementation:**

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SimpleConnectionPool {
    private final BlockingQueue<Connection> pool;
    private final String url;
    private final String username;
    private final String password;
    private final int maxSize;
    private int currentSize;
    
    public SimpleConnectionPool(String url, String username, String password, int maxSize) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxSize = maxSize;
        this.pool = new LinkedBlockingQueue<>(maxSize);
        this.currentSize = 0;
    }
    
    public Connection getConnection() throws SQLException {
        Connection conn = pool.poll();
        
        if (conn != null && !conn.isClosed() && conn.isValid(1)) {
            return conn;
        }
        
        // Create new connection if pool is empty and under limit
        synchronized (this) {
            if (currentSize < maxSize) {
                conn = DriverManager.getConnection(url, username, password);
                currentSize++;
                return conn;
            }
        }
        
        // Wait for available connection
        try {
            conn = pool.poll(30, TimeUnit.SECONDS);
            if (conn != null && !conn.isClosed() && conn.isValid(1)) {
                return conn;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        throw new SQLException("Unable to get connection from pool");
    }
    
    public void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(1)) {
                    conn.setAutoCommit(true); // Reset state
                    pool.offer(conn);
                } else {
                    synchronized (this) {
                        currentSize--;
                    }
                }
            } catch (SQLException e) {
                synchronized (this) {
                    currentSize--;
                }
            }
        }
    }
    
    public void close() throws SQLException {
        Connection conn;
        while ((conn = pool.poll()) != null) {
            conn.close();
        }
        synchronized (this) {
            currentSize = 0;
        }
    }
}
```

**Usage:**

```java
SimpleConnectionPool pool = new SimpleConnectionPool(
    "jdbc:mysql://localhost:3306/mydb", "root", "password", 10
);

try {
    Connection conn = pool.getConnection();
    // Use connection
    pool.releaseConnection(conn);
} finally {
    pool.close();
}
```

**Key Design Considerations:**
- Thread-safe queue for connection storage
- Connection validation before reuse
- Maximum pool size limit
- Timeout for getting connections
- Reset connection state on return
- Handle connection failures gracefully

### Common Performance Pitfalls

**1. Not Using Connection Pooling:**
```java
// BAD - Creates new connection every time
Connection conn = DriverManager.getConnection(url, username, password);
```

**2. Not Closing Resources:**
```java
// BAD - Connection leak
Connection conn = DriverManager.getConnection(url, username, password);
// Forgot to close!
```

**3. N+1 Query Problem:**
```java
// BAD - Multiple queries in loop
for (Order order : orders) {
    List<Item> items = getItems(order.getId()); // Query per iteration
}
```

**4. Fetching Too Much Data:**
```java
// BAD - Fetches all columns and rows
SELECT * FROM users

// GOOD - Fetch only what you need
SELECT id, name FROM users WHERE active = true LIMIT 100
```

**5. Not Using Batch Processing:**
```java
// BAD - Multiple round trips
for (User user : users) {
    insertUser(user); // One query per user
}

// GOOD - Batch insert
insertUsersBatch(users); // One query for all users
```

**6. Not Using Indexes:**
```java
// BAD - Full table scan
SELECT * FROM users WHERE email = 'user@example.com'
// If email column is not indexed

// GOOD - Ensure email column has index
CREATE INDEX idx_email ON users(email);
```

**7. Inefficient ResultSet Processing:**
```java
// BAD - Loading all results into memory
List<User> users = getAllUsers(); // Loads all users

// GOOD - Process in batches or use streaming
processUsersInBatches(1000);
```

**8. Not Using Transactions Appropriately:**
```java
// BAD - Too many small transactions
for (User user : users) {
    conn.setAutoCommit(false);
    updateUser(user);
    conn.commit(); // Commit per user
}

// GOOD - Batch transaction
conn.setAutoCommit(false);
for (User user : users) {
    updateUser(user);
}
conn.commit(); // Single commit
```

**9. Ignoring Connection Timeout:**
```java
// BAD - No timeout, can hang forever
Connection conn = pool.getConnection();

// GOOD - Set timeout
HikariConfig config = new HikariConfig();
config.setConnectionTimeout(30000); // 30 seconds
```

**10. Not Monitoring Pool Metrics:**
```java
// GOOD - Monitor pool health
HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
System.out.println("Active connections: " + poolBean.getActiveConnections());
System.out.println("Idle connections: " + poolBean.getIdleConnections());
System.out.println("Total connections: " + poolBean.getTotalConnections());
```

**Example Answer:**
> "Common JDBC performance pitfalls include: not using connection pooling (creating connections is expensive), N+1 query problems (multiple queries in loops), fetching unnecessary data (SELECT *), not using batch processing, missing database indexes, inefficient ResultSet processing, inappropriate transaction boundaries, ignoring connection timeouts, and not monitoring pool metrics. The biggest impact comes from connection pooling and proper query optimization."

---

## Complete CRUD Example

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CompleteCRUDExample {
    private final String url;
    private final String username;
    private final String password;
    
    public CompleteCRUDExample(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
    
    // CREATE
    public int createUser(String name, String email) throws SQLException {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }
    
    // READ
    public User readUser(int id) throws SQLException {
        String sql = "SELECT id, name, email FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email")
                    );
                }
            }
        }
        return null;
    }
    
    public List<User> readAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email FROM users";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email")
                ));
            }
        }
        return users;
    }
    
    // UPDATE
    public boolean updateUser(int id, String name, String email) throws SQLException {
        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setInt(3, id);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // DELETE
    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}

class User {
    private int id;
    private String name;
    private String email;
    
    public User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    // Getters and setters...
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
```

## Transaction Example

```java
public class TransactionExample {
    public static void transferFunds(String url, String username, String password,
                                      int fromAccount, int toAccount, double amount) {
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(false); // Start transaction
            
            try {
                // Check balance
                String checkBalanceSql = "SELECT balance FROM accounts WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(checkBalanceSql)) {
                    pstmt.setInt(1, fromAccount);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next() || rs.getDouble("balance") < amount) {
                            throw new SQLException("Insufficient funds");
                        }
                    }
                }
                
                // Deduct from source
                String deductSql = "UPDATE accounts SET balance = balance - ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deductSql)) {
                    pstmt.setDouble(1, amount);
                    pstmt.setInt(2, fromAccount);
                    if (pstmt.executeUpdate() == 0) {
                        throw new SQLException("Source account not found");
                    }
                }
                
                // Add to destination
                String addSql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(addSql)) {
                    pstmt.setDouble(1, amount);
                    pstmt.setInt(2, toAccount);
                    if (pstmt.executeUpdate() == 0) {
                        throw new SQLException("Destination account not found");
                    }
                }
                
                conn.commit(); // Commit transaction
                System.out.println("Transfer successful");
                
            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                System.err.println("Transfer failed: " + e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true); // Restore auto-commit
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

## Batch Processing Example

```java
public class BatchProcessingExample {
    public static void insertUsersBatch(String url, String username, String password,
                                        List<User> users) throws SQLException {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(false); // Disable auto-commit for batch
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int batchSize = 0;
                
                for (User user : users) {
                    pstmt.setString(1, user.getName());
                    pstmt.setString(2, user.getEmail());
                    pstmt.addBatch();
                    batchSize++;
                    
                    // Execute batch every 100 records
                    if (batchSize % 100 == 0) {
                        int[] results = pstmt.executeBatch();
                        pstmt.clearBatch();
                        System.out.println("Inserted batch of 100 records");
                    }
                }
                
                // Execute remaining batch
                if (batchSize % 100 != 0) {
                    int[] results = pstmt.executeBatch();
                    System.out.println("Inserted final batch");
                }
                
                conn.commit(); // Commit all batches
                System.out.println("All batches committed successfully");
                
            } catch (SQLException e) {
                conn.rollback(); // Rollback all on error
                System.err.println("Batch insert failed: " + e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true); // Restore auto-commit
            }
        }
    }
}
```

---

## Glossary (Quick Reference)

| Term | Meaning |
|------|--------|
| **JDBC** | Java Database Connectivity — the standard Java API for relational database access. |
| **Driver** | A JAR that implements JDBC for a specific database (e.g. MySQL Connector/J). |
| **Connection** | An open session to the database; obtained from `DriverManager` or a `DataSource`. |
| **Statement** | Object used to run a single SQL command; not safe for user input (SQL injection). |
| **PreparedStatement** | Statement with `?` placeholders; parameters set via `setXxx()`. Safe and cacheable. |
| **CallableStatement** | Used to call stored procedures; extends `PreparedStatement`. |
| **ResultSet** | Cursor over rows from a SELECT; use `next()` and `getXxx()` to read. |
| **executeQuery** | Run a SELECT; returns `ResultSet`. |
| **executeUpdate** | Run INSERT/UPDATE/DELETE; returns row count (int). |
| **DataSource** | Factory for connections; supports pooling and JNDI. |
| **Connection pool** | Set of reusable connections (e.g. HikariCP) to avoid creating new ones every time. |
| **Transaction** | Group of SQL operations that commit together or roll back together (ACID). |
| **Commit** | Make changes of the current transaction permanent. |
| **Rollback** | Undo changes of the current transaction. |
| **SQL injection** | Attack where user input is interpreted as SQL; prevented by using `PreparedStatement`. |
| **DAO** | Data Access Object — layer that encapsulates all JDBC code for one entity/table. |

---

## After Reading: Can You Explain This to a Senior?

Use this as a self-check. You should be able to explain in your own words:

1. **What JDBC is** — Standard Java API for DB access; driver does the DB-specific part.
2. **Connection → Statement → ResultSet** — How you get a connection, run SQL, and read SELECT results.
3. **Why PreparedStatement** — Safety from SQL injection and better performance via precompilation.
4. **executeQuery vs executeUpdate** — When to use which and what each returns.
5. **Transactions** — `setAutoCommit(false)`, `commit()`, `rollback()`, and why we use them for multi-step operations.
6. **Connection pooling** — Why we reuse connections (cost of creating) and that HikariCP is a common choice.
7. **Resource management** — Always close connections and result sets (try-with-resources).
8. **DAO** — Separating data access behind an interface so business logic stays clean and testable.

If you can explain these without looking at the chapter, you’re ready to discuss JDBC with a senior engineer.

---

[← Back to Index](00-README.md) | [Previous: Networking](12-Networking.md) | [Next: Design Patterns →](14-Design-Patterns.md)
