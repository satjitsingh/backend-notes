# Chapter 6: Exception Handling

[← Back to Index](00-README.md) | [Previous: Generics](05-Generics.md) | [Next: Concurrency →](07-Concurrency.md)

---

## Why Exception Handling Matters

> **The Reality**: Poor exception handling is one of the most common causes of 
> production outages and hard-to-debug issues.

Exception handling is about writing robust code that fails gracefully, provides useful diagnostics, and doesn't hide bugs.

---

## Layer 1 — Beginner Foundation

### The Exception Hierarchy

```
                        ┌─────────────┐
                        │  Throwable  │
                        └──────┬──────┘
                               │
              ┌────────────────┴────────────────┐
              │                                 │
       ┌──────▼──────┐                   ┌──────▼──────┐
       │   Error     │                   │  Exception  │
       │ (Don't catch)│                   └──────┬──────┘
       └─────────────┘                          │
              │                   ┌─────────────┴─────────────┐
              │                   │                           │
       ┌──────▼──────┐     ┌──────▼──────┐           ┌───────▼───────┐
       │ OutOfMemory │     │RuntimeExcep │           │    Checked    │
       │StackOverflow│     │  (Unchecked)│           │  Exceptions   │
       │  VirtualMach│     └──────┬──────┘           │  (Must handle)│
       └─────────────┘            │                  └───────────────┘
                                  │                         │
                    ┌─────────────┼─────────────┐          │
                    │             │             │          │
              NullPointer   ArrayIndex    ClassCast    IOException
              Arithmetic    OutOfBounds   Illegal      SQLException
              ...           ...           Argument     FileNotFound
```

### Three Categories of Throwables

```java
/*
 * 1. ERRORS: Serious JVM problems (don't catch)
 *    - OutOfMemoryError
 *    - StackOverflowError
 *    - VirtualMachineError
 *    
 * 2. CHECKED EXCEPTIONS: Must handle or declare (extends Exception)
 *    - IOException
 *    - SQLException
 *    - FileNotFoundException
 *    
 * 3. UNCHECKED EXCEPTIONS: Programming bugs (extends RuntimeException)
 *    - NullPointerException
 *    - ArrayIndexOutOfBoundsException
 *    - IllegalArgumentException
 */

public class ExceptionCategories {
    
    // Checked: Compiler forces you to handle
    public void readFile() throws IOException {  // Must declare
        FileReader reader = new FileReader("file.txt");
    }
    
    // Unchecked: Compiler doesn't force handling
    public void accessArray(int[] arr, int index) {
        // Can throw ArrayIndexOutOfBoundsException
        // No need to declare or catch
        int value = arr[index];
    }
    
    // Error: JVM problem
    public void causeStackOverflow() {
        causeStackOverflow();  // StackOverflowError
    }
}
```

### Try-Catch-Finally Basics

```java
public class TryCatchFinally {
    public static void main(String[] args) {
        // Basic try-catch
        try {
            int result = 10 / 0;  // ArithmeticException
        } catch (ArithmeticException e) {
            System.out.println("Cannot divide by zero: " + e.getMessage());
        }
        
        // Multiple catch blocks (specific to general)
        try {
            String s = null;
            s.length();
        } catch (NullPointerException e) {
            System.out.println("Null pointer!");
        } catch (RuntimeException e) {
            System.out.println("Runtime error!");
        } catch (Exception e) {
            System.out.println("Some exception!");
        }
        
        // Multi-catch (Java 7+)
        try {
            // Some risky operation
        } catch (IOException | SQLException e) {
            // Handle both the same way
            System.out.println("IO or SQL error: " + e.getMessage());
        }
        
        // Finally: ALWAYS executes (even if exception thrown)
        FileReader reader = null;
        try {
            reader = new FileReader("file.txt");
            // Process file
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            // Cleanup - runs regardless of exception
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

### Try-With-Resources (Java 7+)

```java
/*
 * AUTOMATIC RESOURCE MANAGEMENT
 * Resources implementing AutoCloseable are automatically closed
 */

public class TryWithResources {
    
    // ❌ Old way: Verbose and error-prone
    public void oldWay() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("file.txt"));
            String line = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();  // Might hide original exception!
                }
            }
        }
    }
    
    // ✅ Modern way: Clean and correct
    public void modernWay() {
        try (BufferedReader reader = new BufferedReader(new FileReader("file.txt"))) {
            String line = reader.readLine();
            // reader is automatically closed, even if exception occurs
        } catch (IOException e) {
            e.printStackTrace();
        }
        // No finally needed for resource cleanup
    }
    
    // Multiple resources
    public void multipleResources() {
        try (FileInputStream fis = new FileInputStream("input.txt");
             FileOutputStream fos = new FileOutputStream("output.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            
            // Use resources
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        // All resources closed in reverse order of creation
    }
    
    // Effectively final variables (Java 9+)
    public void java9Enhancement() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("file.txt"));
        // reader is effectively final
        try (reader) {  // Can use existing variable
            String line = reader.readLine();
        }
    }
}
```

---

## Layer 2 — Working Developer Level

### Creating Custom Exceptions

```java
// Checked exception (extends Exception)
public class UserNotFoundException extends Exception {
    private final String userId;
    
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }
    
    public UserNotFoundException(String userId, Throwable cause) {
        super("User not found: " + userId, cause);
        this.userId = userId;
    }
    
    public String getUserId() {
        return userId;
    }
}

// Unchecked exception (extends RuntimeException)
public class InvalidConfigurationException extends RuntimeException {
    public InvalidConfigurationException(String message) {
        super(message);
    }
    
    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Usage
public class UserService {
    
    public User findUser(String userId) throws UserNotFoundException {
        User user = database.find(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        return user;
    }
    
    public void processUser(String userId) {
        try {
            User user = findUser(userId);
            // Process user
        } catch (UserNotFoundException e) {
            log.error("User not found: " + e.getUserId(), e);
            // Handle appropriately
        }
    }
}
```

### Exception Chaining

```java
/*
 * EXCEPTION CHAINING: Wrap low-level exceptions in higher-level ones
 * Preserves root cause while providing context
 */

public class ExceptionChaining {
    
    public void processOrder(String orderId) throws OrderProcessingException {
        try {
            // Database operation
            Order order = database.findOrder(orderId);
            
        } catch (SQLException e) {
            // Wrap low-level exception in domain exception
            throw new OrderProcessingException(
                "Failed to process order: " + orderId, 
                e  // Original exception preserved as cause
            );
        }
    }
    
    public static void main(String[] args) {
        try {
            service.processOrder("12345");
        } catch (OrderProcessingException e) {
            // Print full stack trace including causes
            e.printStackTrace();
            
            // Access root cause
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            System.out.println("Root cause: " + rootCause);
        }
    }
}
```

### Exception Handling Best Practices

```java
public class ExceptionBestPractices {
    
    // ❌ BAD: Catch and ignore
    public void bad1() {
        try {
            riskyOperation();
        } catch (Exception e) {
            // Swallowed! Bug hiding!
        }
    }
    
    // ❌ BAD: Catch generic Exception
    public void bad2() {
        try {
            riskyOperation();
        } catch (Exception e) {  // Too broad
            e.printStackTrace();
        }
    }
    
    // ❌ BAD: Use exceptions for flow control
    public boolean bad3(String[] array, String target) {
        try {
            for (int i = 0; ; i++) {
                if (array[i].equals(target)) return true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;  // Terrible!
        }
    }
    
    // ❌ BAD: Losing the stack trace
    public void bad4() throws ServiceException {
        try {
            riskyOperation();
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());  // Cause lost!
        }
    }
    
    // ✅ GOOD: Specific exception, proper logging, preserve cause
    public void good1() {
        try {
            riskyOperation();
        } catch (IOException e) {
            log.error("Failed to perform risky operation", e);
            throw new ServiceException("Operation failed", e);  // Cause preserved
        }
    }
    
    // ✅ GOOD: Use Optional instead of exception for expected cases
    public Optional<User> good2(String id) {
        return Optional.ofNullable(database.find(id));
    }
    
    // ✅ GOOD: Fail fast with validation
    public void good3(String name, int age) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Age must be between 0 and 150");
        }
        // Proceed with valid inputs
    }
    
    // ✅ GOOD: Document exceptions
    /**
     * Processes the given order.
     * 
     * @param orderId the order identifier
     * @throws OrderNotFoundException if order doesn't exist
     * @throws PaymentFailedException if payment processing fails
     * @throws IllegalArgumentException if orderId is null
     */
    public void good4(String orderId) 
            throws OrderNotFoundException, PaymentFailedException {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        // ...
    }
}
```

### Checked vs Unchecked: When to Use Which

```java
/*
 * USE CHECKED EXCEPTIONS when:
 * - Caller can reasonably recover
 * - It's a predictable failure condition
 * - You want to force handling
 * 
 * Examples:
 * - FileNotFoundException (file might not exist)
 * - SQLException (database might be down)
 * - IOException (network might fail)
 * 
 * USE UNCHECKED EXCEPTIONS when:
 * - It's a programming bug
 * - Caller cannot reasonably recover
 * - Exception indicates broken invariant
 * 
 * Examples:
 * - NullPointerException (bug: null where it shouldn't be)
 * - IllegalArgumentException (bug: invalid input)
 * - IllegalStateException (bug: wrong state)
 */

public class CheckedVsUnchecked {
    
    // Checked: Caller should handle file not existing
    public String readFile(String path) throws FileNotFoundException {
        FileReader reader = new FileReader(path);
        // ...
    }
    
    // Unchecked: Null path is a programming bug
    public String processPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        // ...
    }
    
    // Unchecked: Calling on closed object is a bug
    public void useResource() {
        if (isClosed) {
            throw new IllegalStateException("Resource already closed");
        }
        // ...
    }
}
```

---

## Layer 3 — Advanced Engineering Depth

### Suppressed Exceptions

```java
/*
 * SUPPRESSED EXCEPTIONS: When multiple exceptions occur
 * (e.g., original exception AND close() throws)
 * 
 * Try-with-resources automatically handles this
 */

public class SuppressedExceptions {
    
    public static void main(String[] args) {
        try {
            methodThatThrows();
        } catch (Exception e) {
            System.out.println("Primary: " + e.getMessage());
            
            // Get suppressed exceptions
            for (Throwable suppressed : e.getSuppressed()) {
                System.out.println("Suppressed: " + suppressed.getMessage());
            }
        }
    }
    
    static void methodThatThrows() throws Exception {
        try (ProblematicResource r = new ProblematicResource()) {
            throw new Exception("Primary exception");
        }
        // When close() also throws, its exception is suppressed
    }
}

class ProblematicResource implements AutoCloseable {
    @Override
    public void close() throws Exception {
        throw new Exception("Close exception");  // Will be suppressed
    }
}

// Manual suppression
public void manualSuppression() {
    Exception primary = null;
    Resource r = null;
    try {
        r = new Resource();
        r.use();
    } catch (Exception e) {
        primary = e;
    } finally {
        if (r != null) {
            try {
                r.close();
            } catch (Exception e) {
                if (primary != null) {
                    primary.addSuppressed(e);  // Add as suppressed
                } else {
                    throw e;
                }
            }
        }
        if (primary != null) {
            throw primary;
        }
    }
}
```

### Exception Performance

```java
/*
 * EXCEPTION PERFORMANCE:
 * 
 * Creating an exception is EXPENSIVE because:
 * 1. Stack trace capture (walks the call stack)
 * 2. Memory allocation
 * 
 * Throwing is relatively cheap.
 * 
 * Rule: Don't use exceptions for expected flow control
 */

public class ExceptionPerformance {
    
    // ❌ SLOW: Exception for expected case
    public boolean slowContains(int[] array, int value) {
        try {
            for (int i = 0; ; i++) {
                if (array[i] == value) return true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }
    
    // ✅ FAST: Normal control flow
    public boolean fastContains(int[] array, int value) {
        for (int element : array) {
            if (element == value) return true;
        }
        return false;
    }
    
    // Optimization: Skip stack trace for control-flow exceptions
    public class NoStackTraceException extends RuntimeException {
        public NoStackTraceException(String message) {
            super(message, null, true, false);  // writableStackTrace = false
        }
        
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;  // Don't fill stack trace
        }
    }
    
    // Pre-allocated exception for performance-critical code
    private static final RuntimeException CACHED_EXCEPTION = 
        new RuntimeException("Cached") {
            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        };
}
```

### Assertions

```java
/*
 * ASSERTIONS: Verify assumptions during development
 * 
 * Disabled by default at runtime!
 * Enable with: java -ea MyClass (or -enableassertions)
 * 
 * Use for:
 * - Internal invariants
 * - Control-flow assumptions
 * - Postconditions in private methods
 * 
 * Don't use for:
 * - Argument validation in public methods
 * - Anything required for correctness in production
 */

public class AssertionDemo {
    
    // Assert internal invariant
    private void sort(int[] array) {
        // Sorting logic...
        
        assert isSorted(array) : "Array should be sorted after sort()";
    }
    
    // Assert impossible control flow
    private String getQuarter(int month) {
        return switch (month) {
            case 1, 2, 3 -> "Q1";
            case 4, 5, 6 -> "Q2";
            case 7, 8, 9 -> "Q3";
            case 10, 11, 12 -> "Q4";
            default -> {
                assert false : "Invalid month: " + month;
                yield "";  // Unreachable
            }
        };
    }
    
    // Assert preconditions in private methods
    private void internalMethod(Object param) {
        assert param != null : "Internal method requires non-null param";
        // ...
    }
    
    // ❌ DON'T use for public API validation
    public void publicMethod(Object param) {
        // assert param != null;  // ❌ Might be disabled
        if (param == null) {
            throw new IllegalArgumentException("param cannot be null");
        }
    }
    
    private boolean isSorted(int[] array) {
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[i-1]) return false;
        }
        return true;
    }
}
```

---

## Layer 4 — Interview Mastery

### Frequently Asked Interview Questions

**Q1: "What's the difference between checked and unchecked exceptions?"**

| Aspect | Checked | Unchecked |
|--------|---------|-----------|
| Inherits from | Exception (not Runtime) | RuntimeException |
| Compile-time | Must declare/handle | Optional |
| Represents | Recoverable conditions | Programming bugs |
| Examples | IOException, SQLException | NullPointerException, IllegalArgumentException |

> "Checked exceptions represent conditions a well-written application should anticipate and recover from. The compiler forces handling to ensure reliability. Unchecked exceptions represent bugs or programming errors that shouldn't occur in correct code—catching them usually indicates a bug that should be fixed.
>
> Modern best practice leans toward unchecked exceptions for APIs because they're less invasive. Spring Framework, for example, converts all JDBC checked exceptions to unchecked ones."

---

**Q2: "Why shouldn't we catch Exception or Throwable?"**

> "Catching Exception catches both checked exceptions AND all RuntimeExceptions including bugs like NullPointerException. This:
>
> 1. **Hides bugs**: You might catch and 'handle' a NPE that should crash and be fixed
> 2. **Over-broad handling**: Different exceptions need different handling
> 3. **Catches InterruptedException**: Can mess up thread interruption
>
> Catching Throwable is even worse—it catches Errors like OutOfMemoryError. You usually can't recover from Errors.
>
> Only catch specific exceptions you can actually handle. Let unexpected ones propagate to reveal bugs."

---

**Q3: "Explain try-with-resources and suppressed exceptions."**

> "Try-with-resources automatically closes resources implementing AutoCloseable when the try block exits—whether normally or via exception. Resources close in reverse order of declaration.
>
> Suppressed exceptions occur when both the try block AND close() throw exceptions. The original exception is primary, and the close exception is added via `addSuppressed()`. This ensures the original problem is reported while not losing the cleanup failure.
>
> Before Java 7, you had to choose which exception to throw (usually losing one), or use complex try-finally nesting that was error-prone."

---

**Q4: "What output does this produce?"**

```java
public static int test() {
    try {
        return 1;
    } finally {
        return 2;
    }
}
```

> "It returns 2. The finally block always executes before the method returns. When finally contains a return statement, it overrides the try block's return. This is why you should never return from finally—it can also swallow exceptions if one was thrown.
>
> If try throws an exception and finally returns, the exception is lost! This is a code smell that IDE warnings will catch."

---

### Common Mistakes by Level

**Beginner Mistakes**:
- ❌ Catching and ignoring exceptions (empty catch blocks)
- ❌ Using `printStackTrace()` instead of proper logging
- ❌ Not closing resources (before try-with-resources)
- ❌ Catching Exception instead of specific types

**Intermediate Mistakes**:
- ❌ Losing exception cause when rethrowing
- ❌ Using exceptions for expected control flow
- ❌ Creating custom exceptions without constructors for cause
- ❌ Returning from finally blocks

**Advanced Mistakes**:
- ❌ Ignoring suppressed exceptions
- ❌ Creating many exception objects in hot paths
- ❌ Using assertions for public API validation
- ❌ Catching Throwable or Error

---

### Checkpoint: Exception Handling

✅ You should now understand:
- [ ] The exception hierarchy (Error, Exception, RuntimeException)
- [ ] Checked vs unchecked exceptions and when to use each
- [ ] Try-with-resources for automatic cleanup
- [ ] Exception chaining (preserving cause)
- [ ] Creating meaningful custom exceptions
- [ ] Suppressed exceptions mechanism
- [ ] Exception handling best practices

**Mental Model**:
> Think of exceptions as escalation procedures:
> - **Checked exceptions**: "Expected problems that need a plan" (like fire drills)
> - **Unchecked exceptions**: "Bugs that should never happen" (like finding a hole in the floor)
> - **Errors**: "Building on fire—evacuate" (JVM failing)
> - **Always preserve evidence** (stack trace, cause)

---

[← Back to Index](00-README.md) | [Previous: Generics](05-Generics.md) | [Next: Concurrency →](07-Concurrency.md)
