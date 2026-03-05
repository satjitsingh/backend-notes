# Chapter 22: Global Exception Handling

[← Back to Index](00-README.md) | [Previous: Validation](21-Validation.md) | [Next: DTO Patterns →](23-DTO-Patterns.md)

---

## Why This Chapter Matters

Imagine you run a pizza shop. A customer asks for "pizza number 999" — but you only have pizzas 1 through 10. What do you say? You don't shout random kitchen noises or show them your recipe book. You say something polite and helpful: *"Sorry, we don't have pizza 999. Would you like to see our menu?"*

Your API is like that pizza shop. When something goes wrong — a user doesn't exist, the data is invalid, the database is down — your API must respond in a helpful, consistent way. **Exception handling is how you turn messy, scary errors into clear, professional responses.**

Without proper exception handling, your API might "shout" a raw Java stack trace at your users:
```
java.lang.NullPointerException at com.example.service.UserService.findById(UserService.java:42)
at com.example.controller.UserController.getUser(UserController.java:28)
at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ...
```

That's like showing a customer your kitchen chaos instead of calmly explaining the problem.

**What good exception handling gives you:**
- A clean, consistent JSON response every frontend developer can parse
- No leaked internal details (security)
- One central place to manage all error responses
- Professional APIs that feel trustworthy

**Interview Reality:** Exception handling appears in **90%+ of Spring Boot interviews**. You'll be asked about `@ControllerAdvice`, error response design, and exception hierarchy.

---

## Layer 1 — Intuition Builder

### Part 1: When Things Go Wrong (The Mess)

Imagine you own a toy store. Every time a kid asks for a toy you don't have, you have to:
1. Run to the back
2. Search everywhere
3. Come back and explain — *differently each time*
4. Sometimes you just panic and say random things

That's what happens **without** global exception handling. Every part of your code (every controller method) has to catch errors and figure out what to say. And they all say it differently. Messy!

**What your API might accidentally do:**
- Show ugly technical gibberish (stack traces) to users
- Give different error shapes from different endpoints (frontend can't reliably parse)
- Repeat the same error-handling code in dozens of places
- Tell users "500 Internal Server Error" for everything — even when it's just "user not found"

---

### Part 2: The Customer Service Department Analogy

Think of a big company with many departments (Sales, Shipping, Returns). Each department gets complaints. Instead of each department handling complaints their own way, the company has **one Customer Service department**. No matter who has a problem, it goes there. One place, one style, one professional response.

**In Spring Boot:**
- Your **controllers** are the departments (they handle requests)
- When something goes wrong, they "escalate" — they throw an **exception** (raise a flag)
- The **exception handler** is the Customer Service department — it catches ALL complaints
- It gives a **consistent, friendly response** every time

```
Customer has problem → Department (Controller) escalates
    ↓
Complaint goes to Customer Service (@ControllerAdvice)
    ↓
Customer Service handles:
    ├── "Item not found" → Polite 404 message
    ├── "Invalid order" → 400, explain what’s wrong
    ├── "Duplicate order" → 409, suggest checking
    └── "Unknown issue" → 500, apologize, log for investigation
```

---

### Part 3: Without vs With Global Handling

**Without** global handling — every controller does its own thing. Repetitive. Inconsistent. Easy to forget.

```java
// Imagine every controller method looking like this — yuck!
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    try {
        return userService.findById(id);
    } catch (UserNotFoundException e) {
        return ResponseEntity.status(404).body(/* build error by hand */);
    } catch (Exception e) {
        log.error("Error", e);
        return ResponseEntity.status(500).body(/* build error by hand */);
    }
}
// Same messy try-catch repeated in 20 other methods!
```

**With** global handling — controllers stay clean. One place handles everything.

```java
// Controllers stay simple — no try-catch clutter!
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);  // Just throw if not found
}

// ONE class handles ALL errors from ALL controllers
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(UserNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }
}
```

---

### Part 4: Two Annotations — Location and Job

You need two things:

1. **Where** does the "Customer Service" live?  
   → `@ControllerAdvice` — "This class handles problems for ALL controllers."

2. **What** does it handle?  
   → `@ExceptionHandler` — "This method handles THIS specific type of problem."

Think of it like this:
- `@ControllerAdvice` = The complaint desk (the **place**)
- `@ExceptionHandler` = The person who handles "Item Not Found" complaints (the **specific job**)

---

### Part 5: The Ugly vs The Clean

**Ugly** (what you want to avoid):
```
java.lang.NullPointerException at com.example.service.UserService...
at com.example.controller.UserController...
```

**Clean** (what you want):
```json
{
  "code": "NOT_FOUND",
  "message": "User not found with id: 999",
  "timestamp": "2026-02-17T10:30:00Z"
}
```

The clean version is:
- Easy for frontend developers to read
- Safe (no internal code exposed)
- Consistent (same shape for every error)

---

### Part 6: Why Not Leak Stack Traces?

A stack trace tells attackers exactly where your code is, what methods you call, and how your app is built. That's like leaving your house blueprints on the front lawn. In production, you log the full stack trace **internally** (for debugging) but return only a polite, generic message to the client.

---

## Layer 2 — Professional Developer

### Why Handle Exceptions Globally?

**Before (the problem):**
- Duplicate try-catch in every controller method
- Inconsistent error formats across endpoints
- Easy to forget handling in new endpoints
- Stack traces exposed to clients (security risk)
- Hard to change error behavior (must edit many files)

**After (the solution):**
- Centralized logic in one `@ControllerAdvice` class
- Consistent `ErrorResponse` structure everywhere
- Controllers focus on business logic
- Stack traces only in logs, never in responses
- Single place to update error handling

---

### The ErrorResponse DTO — Consistent Structure

Every error should return the same shape. That way, frontend code can parse errors reliably.

```java
/**
 * Standard error response returned for ALL exceptions.
 * Consistent structure = predictable parsing by clients.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String code;              // Machine-readable: "NOT_FOUND", "VALIDATION_ERROR"
    private String message;           // Human-readable explanation
    private List<FieldErrorDetail> errors;  // For validation: which fields failed
    private Instant timestamp;        // When the error occurred
    private String path;              // Request path (e.g. "/api/users/999")
    private String traceId;           // For log correlation (optional)
}

/** Field-level error (e.g. "email" invalid, "age" must be positive) */
@Getter
@AllArgsConstructor
public class FieldErrorDetail {
    private String field;
    private String message;
    private Object rejectedValue;  // What the user sent (optional, be careful with PII)
}
```

**Example JSON responses:**

```json
// 404 Not Found
{
  "code": "NOT_FOUND",
  "message": "User not found with id: 999",
  "timestamp": "2026-02-17T10:30:00Z",
  "path": "/api/users/999"
}

// 400 Validation Error
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "email", "message": "must be a well-formed email address" },
    { "field": "age", "message": "must be greater than 0" }
  ],
  "timestamp": "2026-02-17T10:30:00Z",
  "path": "/api/users"
}
```

---

### Custom Exception Classes

Domain-specific exceptions make your code self-documenting. Instead of throwing generic `RuntimeException`, throw `ResourceNotFoundException` or `DuplicateResourceException`.

```java
/**
 * Base for all business-layer exceptions. Unchecked so controllers
 * don't need try-catch — the global handler catches them.
 */
public abstract class BusinessException extends RuntimeException {
    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

/** Thrown when a resource (User, Order, etc.) is not found */
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, Long id) {
        super("NOT_FOUND", resource + " not found with id: " + id);
    }

    public ResourceNotFoundException(String resource, String identifier) {
        super("NOT_FOUND", resource + " not found: " + identifier);
    }
}

/** Thrown when creating a duplicate (e.g. duplicate email) */
public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String resource, String field, Object value) {
        super("DUPLICATE_RESOURCE",
              resource + " already exists with " + field + ": " + value);
    }
}

/** Thrown when user lacks permission */
public class InsufficientPermissionsException extends BusinessException {
    public InsufficientPermissionsException(String action) {
        super("FORBIDDEN", "Insufficient permissions to " + action);
    }
}
```

**Usage in service layer:**

```java
@Service
public class UserService {

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User create(UserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User", "email", dto.getEmail());
        }
        return userRepository.save(toEntity(dto));
    }
}
```

---

### Complete GlobalExceptionHandler

A full `@ControllerAdvice` with multiple handlers:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400 — Invalid input (validation, bad arguments)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(e -> new FieldErrorDetail(
                e.getField(),
                e.getDefaultMessage(),
                e.getRejectedValue()))
            .toList();

        log.warn("Validation failed for {}: {}", request.getRequestURI(), errors);
        return ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Invalid input")
            .errors(errors)
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
    }

    // 400 — Constraint violations (e.g. @Valid on path variables)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldErrorDetail> errors = ex.getConstraintViolations()
            .stream()
            .map(v -> new FieldErrorDetail(
                v.getPropertyPath().toString(),
                v.getMessage(),
                v.getInvalidValue()))
            .toList();

        return ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Constraint violation")
            .errors(errors)
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
    }

    // 400 — Bad request (IllegalArgumentException, etc.)
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Bad request: {} - {}", request.getRequestURI(), ex.getMessage());
        return ErrorResponse.builder()
            .code("BAD_REQUEST")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
    }

    // 404 — Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} - {}", request.getRequestURI(), ex.getMessage());
        return ErrorResponse.builder()
            .code("NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
    }

    // 404 — No handler for URL (Spring's NoHandlerFoundException)
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoHandler(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler for: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ErrorResponse.builder()
            .code("NOT_FOUND")
            .message("No endpoint for " + ex.getHttpMethod() + " " + ex.getRequestURL())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
    }

    // 409 — Conflict (duplicate resource)
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Conflict: {} - {}", request.getRequestURI(), ex.getMessage());
        return ErrorResponse.builder()
            .code("CONFLICT")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
    }

    // 500 — Unexpected errors (catch-all)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error for {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ErrorResponse.builder()
            .code("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
    }
}
```

---

### Mapping Exceptions to HTTP Status Codes

| Exception Type | HTTP Status | When It Occurs |
|----------------|-------------|----------------|
| `ResourceNotFoundException` | 404 Not Found | User/order/etc. doesn't exist |
| `NoHandlerFoundException` | 404 Not Found | URL has no matching endpoint |
| `MethodArgumentNotValidException` | 400 Bad Request | `@Valid` fails on request body |
| `ConstraintViolationException` | 400 Bad Request | `@Validated` fails on path/params |
| `IllegalArgumentException` | 400 Bad Request | Invalid argument passed |
| `DuplicateResourceException` | 409 Conflict | Creating duplicate (e.g. same email) |
| `AccessDeniedException` | 403 Forbidden | User lacks permission |
| `AuthenticationException` | 401 Unauthorized | Not authenticated |
| `Exception` (catch-all) | 500 Internal Server Error | Unexpected errors |

---

### @ResponseStatus vs ResponseStatusException

**@ResponseStatus** — Annotate the exception class or the handler method:
```java
@ExceptionHandler(ResourceNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)  // Sets HTTP 404
public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
    return ErrorResponse.builder().code("NOT_FOUND").message(ex.getMessage()).build();
}
```

**ResponseStatusException** — Throw it when you need different statuses for the same exception type:
```java
// In controller or service — no custom exception needed
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
```

Handled by a generic handler or Spring's default. Use for one-off cases; prefer custom exceptions for reusable business rules.

---

### Handling 404 Explicitly

404 can come from:
1. **Your code** — `ResourceNotFoundException`
2. **Spring** — `NoHandlerFoundException` (wrong URL)

To get `NoHandlerFoundException`, enable:
```properties
# application.properties
spring.mvc.throw-exception-if-no-handler-found=true
spring.web.resources.add-mappings=false  # So static resources don't "handle" unknown paths
```

---

### Logging Strategy

- **Business errors** (404, 409, validation): `log.warn()` — expected, not bugs
- **Technical/unexpected errors** (500): `log.error(..., ex)` — include full stack
- Never return stack traces in the response; log them internally only

---

### Best Practices

| Do | Don't |
|----|-------|
| Use a consistent `ErrorResponse` structure | Return different shapes from different handlers |
| Log full stack trace for 500s (server-side) | Expose stack traces to clients |
| Use custom exceptions for domain errors | Throw raw `RuntimeException` with vague messages |
| Put specific handlers before generic ones | Catch `Exception` before `ResourceNotFoundException` |
| Include `path` and `timestamp` in errors | Omit context that helps debugging |

---

## Layer 3 — Advanced Engineering Depth

### Exception Hierarchy Design

A well-designed hierarchy lets you handle groups of exceptions together:

```java
public abstract class ApplicationException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details = new HashMap<>();

    protected ApplicationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected ApplicationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getDetails() { return details; }

    public ApplicationException withDetail(String key, Object value) {
        details.put(key, value);
        return this;
    }
}

public abstract class BusinessException extends ApplicationException {
    protected BusinessException(String errorCode, String message) { super(errorCode, message); }
}

public abstract class TechnicalException extends ApplicationException {
    protected TechnicalException(String errorCode, String message) { super(errorCode, message); }
    protected TechnicalException(String errorCode, String message, Throwable cause) { super(errorCode, message, cause); }
}

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with id: %s", resource, id));
        withDetail("resource", resource);
        withDetail("id", id);
    }
}

public class ExternalServiceException extends TechnicalException {
    public ExternalServiceException(String service, String message, Throwable cause) {
        super("EXTERNAL_SERVICE_ERROR", String.format("Error calling %s: %s", service, message), cause);
        withDetail("service", service);
    }
}
```

**Handler for hierarchy:**
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
    HttpStatus status = switch (ex.getErrorCode()) {
        case "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
        case "DUPLICATE_RESOURCE" -> HttpStatus.CONFLICT;
        default -> HttpStatus.BAD_REQUEST;
    };
    return ResponseEntity.status(status).body(buildErrorResponse(ex, request));
}

@ExceptionHandler(TechnicalException.class)
public ResponseEntity<ErrorResponse> handleTechnical(TechnicalException ex, HttpServletRequest request) {
    log.error("Technical error: {}", ex.getErrorCode(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildErrorResponse(ex, request));
}
```

---

### Exception Resolution Order

Spring picks the **most specific** handler first:

1. Controller-local `@ExceptionHandler`
2. `@ControllerAdvice` (scoped or global)
3. Most specific exception type wins

So define handlers in order: **specific first, generic last**.

```java
@ExceptionHandler(ResourceNotFoundException.class)  // More specific — checked first
public ErrorResponse handleNotFound(...) { }

@ExceptionHandler(BusinessException.class)          // Parent — catches others
public ErrorResponse handleBusiness(...) { }

@ExceptionHandler(Exception.class)                  // Catch-all — last
public ErrorResponse handleGeneral(...) { }
```

---

### RFC 7807 Problem Details

RFC 7807 defines a standard JSON structure for errors:

```java
@Builder
@Getter
public class ProblemDetail {
    private String type;       // URI: "https://api.example.com/problems/not-found"
    private String title;      // Short: "Resource Not Found"
    private int status;        // 404
    private String detail;     // "User not found with id: 123"
    private String instance;   // "/api/users/123"
    private Map<String, Object> extensions;  // Custom fields
}
```

Example:
```json
{
  "type": "https://api.example.com/problems/resource-not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "User not found with id: 123",
  "instance": "/api/users/123",
  "resource": "User",
  "id": 123
}
```

Use when you need a standard, interoperable error format.

---

### Environment-Specific Responses

In development, return more detail; in production, keep it generic:

```java
@Value("${spring.profiles.active:prod}")
private String activeProfile;

@ExceptionHandler(Exception.class)
public ErrorResponse handleException(Exception ex, HttpServletRequest request) {
    boolean isDev = "dev".equals(activeProfile) || "local".equals(activeProfile);

    var builder = ErrorResponse.builder()
        .code("INTERNAL_ERROR")
        .timestamp(Instant.now())
        .path(request.getRequestURI());

    if (isDev) {
        builder.message(ex.getMessage());
        // Optionally: builder.details(Map.of("exception", ex.getClass().getName()));
    } else {
        builder.message("An unexpected error occurred");
    }

    log.error("Error processing request", ex);
    return builder.build();
}
```

---

### Correlation IDs for Tracing

```java
@Component
public class CorrelationIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        String id = http.getHeader("X-Correlation-Id");
        if (id == null) id = UUID.randomUUID().toString();

        MDC.put("correlationId", id);
        ((HttpServletResponse) response).setHeader("X-Correlation-Id", id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

Include `correlationId` in `ErrorResponse` so clients can reference it when reporting issues.

---

### Checked vs Unchecked Exceptions

| Type | Use Case | Example |
|------|----------|---------|
| **Unchecked** (`RuntimeException`) | Business/technical errors where caller usually can't recover | `ResourceNotFoundException`, `DuplicateResourceException` |
| **Checked** (`Exception`) | When caller can meaningfully recover (retry, fallback) | `RetryableException` |

In Spring Boot, prefer **unchecked** for most cases. The global handler catches them; controllers stay clean.

---

## Layer 4 — Interview Mastery

### Q1: How do you handle exceptions globally in Spring Boot?

**Answer:** Use `@ControllerAdvice` (or `@RestControllerAdvice`) with `@ExceptionHandler` methods. When any controller throws an exception, Spring looks for a matching handler in controller-first, then in `@ControllerAdvice` classes. The handler returns a consistent error response (e.g. `ErrorResponse` DTO) with the right HTTP status.

---

### Q2: What's the difference between @ControllerAdvice and @ExceptionHandler?

**Answer:** `@ControllerAdvice` is a **class-level** annotation — it makes the class a global advisor for controllers (exception handling, model attributes, etc.). `@ExceptionHandler` is **method-level** — it marks a method that handles a specific exception type. `@ControllerAdvice` provides the scope; `@ExceptionHandler` provides the logic. `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody` for REST APIs.

---

### Q3: How do you design error responses?

**Answer:** Use a consistent DTO (e.g. `ErrorResponse`) with: `code`, `message`, `timestamp`, `path`, and optionally `errors` for validation. Use machine-readable codes (`NOT_FOUND`, `VALIDATION_ERROR`). In production, avoid exposing stack traces or internal details. Include correlation IDs for tracing. Consider RFC 7807 Problem Details for interoperability.

---

### Q4: How do you handle validation errors?

**Answer:** Catch `MethodArgumentNotValidException` (from `@Valid` on `@RequestBody`) and `ConstraintViolationException` (from `@Validated` on path/params). Extract errors from `BindingResult` or `ConstraintViolation`s and map them to a list of `FieldError` objects in the `ErrorResponse`. Return HTTP 400.

---

### Q5: When to use checked vs unchecked exceptions for business errors?

**Answer:** Use **unchecked** (`RuntimeException`) for business errors like "not found" or "duplicate." Controllers don't need try-catch; the global handler catches them. Use **checked** only when the caller can meaningfully recover (e.g. retry). Spring and most libraries favor unchecked exceptions.

---

### Q6: How do you avoid leaking stack traces?

**Answer:** Log the full exception (including stack trace) on the server with `log.error("...", ex)`. In the response, return only a generic or safe message. Use environment flags (e.g. `dev` vs `prod`) to optionally include more detail in dev.

---

### Q7: How do you handle 404 for unknown URLs?

**Answer:** Enable `spring.mvc.throw-exception-if-no-handler-found=true` and `spring.web.resources.add-mappings=false`. Handle `NoHandlerFoundException` in the global handler and return a consistent 404 `ErrorResponse`.

---

## Summary

- **Global handling:** `@ControllerAdvice` + `@ExceptionHandler` centralizes error handling so controllers stay clean.
- **ErrorResponse:** Use a consistent DTO (`code`, `message`, `timestamp`, `path`, optional `errors`) for all errors.
- **Custom exceptions:** Create domain exceptions (`ResourceNotFoundException`, `DuplicateResourceException`) for clearer code and mapping to HTTP status.
- **Mapping:** 404 for not found, 400 for validation/bad input, 409 for conflict, 500 for unexpected errors.
- **Validation:** Handle `MethodArgumentNotValidException` and `ConstraintViolationException`; extract field errors into the response.
- **Logging:** Warn for expected errors (404, 409, validation); error with full stack for 500s. Never expose stack traces to clients.
- **Production:** Use correlation IDs, avoid leaking internals, and optionally use different detail levels per environment.

**Next Steps:**
- DTO patterns (Chapter 23)
- Spring Data JPA (Chapter 24)
- Transaction management (Chapter 26)

---

[← Back to Index](00-README.md) | [Previous: Validation](21-Validation.md) | [Next: DTO Patterns →](23-DTO-Patterns.md)
