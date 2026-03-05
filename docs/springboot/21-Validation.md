# Chapter 21: Validation Strategies

[← Back to Index](00-README.md) | [Previous: Controllers](20-Controllers.md) | [Next: Exception Handling →](22-Exception-Handling.md)

---

## Why This Chapter Matters

**Rule #1 of backend development: Never trust user input. EVER.**

Imagine someone sends your app a message saying "I am -5 years old" or "My email is banana." Without checking, your app would believe it and save it. That's like letting anyone walk through the door without asking who they are — dangerous and messy.

**Validation** is the art of checking data *before* it enters your system. It's your first line of defense against bad data, security attacks, and bugs. Every professional API validates input. Spring Boot makes this surprisingly easy with simple annotations — but understanding *when* and *how* to use them is critical.

**Without proper validation:**
- Invalid data corrupts your database (age = -5, email = garbage)
- Your application is vulnerable to attacks (SQL injection, XSS)
- Users see confusing errors ("500 Internal Server Error")
- Business rules break silently (negative prices, future birth dates)
- Debugging becomes a nightmare ("where did this bad data come from?")

**With proper validation:**
- Bad data is caught at the door — before it touches business logic or database
- Users get clear, specific messages: "Email must be a valid email address"
- Complex business rules enforced with custom validators
- Defense-in-depth: validate at multiple layers
- APIs that frontend developers love working with

---

## Layer 1 — Intuition Builder

### Part 1: What Is Validation? (The Bouncer at the Club)

Think of validation like a **bouncer at a club**:

```
Person walks up → Bouncer checks
    ├── Is your ID real? (Format check)
    ├── Are you 18 or older? (Business rule)
    ├── Are you on the dress code list? (Custom rule)
    └── Either: "Welcome in!" or "Sorry, you can't enter because..."
```

The bouncer doesn't let everyone in. He **checks** first. If something's wrong, he says *why* — "You need to be 18" or "Your ID looks fake."

**In your Spring Boot app:**
- **Validation** = the bouncer
- **User input** = the person trying to get in
- **Your database** = the club inside

You want to check data *before* it gets inside. If the data is bad, you say why — clearly — so the user can fix it.

---

### Part 2: What Happens Without Validation?

Picture a restaurant that doesn't check orders:

- A customer writes "I want 1000 pizzas" — the kitchen tries to make them all, everything breaks
- Someone writes "My name is <script>hack</script>" — that nasty code gets saved and could harm others
- A customer says "I was born in the year 2030" — impossible, but nobody stops it

**Same idea in apps:**
- Someone sends `"age": -5` → Your database gets nonsense
- Someone sends `"email": "not-an-email"` → You can't contact them
- Someone sends `"name": ""` (empty) → You have no name to display

**Without validation:** Bad stuff gets saved. Later, things crash. Users see generic errors. Debugging is painful.

**With validation:** Bad stuff is stopped at the door. You say exactly what's wrong. The user fixes it. Your system stays clean.

---

### Part 3: The Four Checkpoints (Layers of Validation)

Think of an **airport security checkpoint**. There are several checks before you board:

**Checkpoint 1 — At Home (Client-Side)**
- You check: "Do I have my ID? Ticket?"
- **In apps:** The browser (JavaScript) checks before sending
- **Strength:** Fast, good for user experience
- **Weakness:** Users can turn it off or bypass it — **never trust this alone**

**Checkpoint 2 — Airport Entrance (Controller-Level)**
- Security checks your ID and ticket at the door
- **In apps:** Your API checks the data when it arrives
- **Strength:** First real defense; catches bad data before it enters your system
- **This chapter focuses heavily on this layer**

**Checkpoint 3 — Gate (Service-Level)**
- Another check before boarding
- **In apps:** Your business logic checks rules (e.g., "Is this email already used?")
- **Strength:** Enforces business rules; can use database or other services

**Checkpoint 4 — Plane (Database-Level)**
- Final rules: seat belts, no smoking
- **In apps:** Database rules (NOT NULL, UNIQUE, CHECK)
- **Strength:** Last line of defense; works even if app code has bugs

**Takeaway:** Each checkpoint has a job. Don't skip any.

---

### Part 4: The Magic of Annotations (Sticky Notes for Your Data)

In Spring Boot, we use **annotations** — like little sticky notes on fields that say "check this!"

Think of a form at school:
- Name field has a note: "Must be 2–50 characters"
- Email field has a note: "Must look like an email"
- Age field has a note: "Must be 18 or more"

When data comes in, Spring reads those notes and checks each field. If something fails, it stops and reports the problem.

**Common "sticky notes" (we'll learn the real names later):**
- "Can't be empty"
- "Must be an email"
- "Must be at least 18"
- "Must be between 2 and 50 characters"
- "Must match this pattern"

---

### Part 5: What "Clean Error Messages" Means

**Bad (confusing):**
```
Error 500
```
User has no idea what went wrong.

**Good (clear):**
```json
{
  "email": "Please provide a valid email address",
  "age": "Age must be at least 18"
}
```

The user knows *exactly* what to fix. That's what we aim for with validation.

---

## Layer 2 — Professional Developer

### Why Validate? (The Professional Answer)

**Security:** Malicious input (SQL injection, XSS) can be blocked at the door.

**Data integrity:** Your database stays consistent. No nulls where they shouldn't be, no negative ages.

**User experience:** Clear, field-specific error messages instead of generic 500 errors.

**Business rules:** Enforce rules (e.g., "Price must be positive") before data reaches your logic.

---

### Bean Validation (JSR 380) — The Java Standard

**Bean Validation** is the official Java standard for checking objects. "Bean" here means "object with fields." JSR 380 is the version number. Spring Boot uses it out of the box.

**Why use a standard?**
- Same idea across projects and frameworks
- Lots of built-in checks (email, size, etc.)
- Easy to extend with your own rules

---

### Adding the Validation Dependency

**Why:** Spring Boot doesn't include validation by default. You need the starter.

```xml
<!-- Maven: Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

```groovy
// Gradle: Add to build.gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

---

### Common Validation Annotations — Reference Table

| Category | Annotation | What It Checks | Example Use |
|----------|------------|----------------|-------------|
| **Null/Empty** | `@NotNull` | Cannot be null | Any field that must exist |
| | `@NotBlank` | String not null, empty, or whitespace only | Name, email, title |
| | `@NotEmpty` | Collection/array not null and not empty | List of items |
| **Size** | `@Size(min, max)` | Length within range (String, Collection) | Name length |
| | `@Min(value)` | Number ≥ value | Age ≥ 18 |
| | `@Max(value)` | Number ≤ value | Age ≤ 120 |
| **Format** | `@Email` | Valid email format | Email field |
| | `@Pattern(regexp)` | Matches regex | Zip code, phone |
| **Date/Time** | `@Past` | Date in the past | Birth date |
| | `@Future` | Date in the future | Expiry date |
| **Numeric** | `@Positive` | Number > 0 | Price, quantity |
| | `@PositiveOrZero` | Number ≥ 0 | Discount |
| | `@Negative` | Number < 0 | Balance (debt) |
| | `@Digits(integer, fraction)` | Digit count (e.g., 3.2 = 3 whole, 2 decimal) | Money format |

---

### Null vs Empty vs Blank — Why It Matters

```
@NotNull   → Field exists (not null). "" is allowed.
@NotEmpty  → For collections: at least one element. For String: not "" and not null.
@NotBlank  → For String: not null, not "", and not "   " (whitespace only).
```

**Example:**
- `null` → Fails `@NotNull`, `@NotEmpty`, `@NotBlank`
- `""` → Passes `@NotNull`, fails `@NotEmpty` and `@NotBlank`
- `"   "` → Passes `@NotNull` and `@NotEmpty`, fails `@NotBlank`

**Rule of thumb:** For text fields like name or email, use `@NotBlank`.

---

### Validating in Controllers with @Valid

**Why use @Valid:** It tells Spring to run all validation annotations on the object *before* the method runs. If validation fails, the method never executes.

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // @Valid triggers validation on the entire UserDto
    // If ANY field fails, Spring throws MethodArgumentNotValidException
    // Your method body never runs with invalid data
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody UserDto dto) {
        // By the time we get here, data has passed all checks
        return userService.create(dto);
    }

    // @Valid works for PUT, PATCH, and other request body params too
    @PutMapping("/{id}")
    public UserDto update(
            @PathVariable Long id,
            @Valid @RequestBody UserDto dto
    ) {
        return userService.update(id, dto);
    }

    // @Valid also works with @ModelAttribute (form data, query params)
    @GetMapping("/search")
    public List<UserDto> search(@Valid @ModelAttribute UserSearchRequest request) {
        return userService.search(request);
    }
}
```

---

### Example DTO with Validation Annotations

```java
public class UserDto {

    // Name: required, 2-50 characters
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    // Email: required, valid format
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    // Age: between 18 and 120
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must be at most 120")
    private Integer age;

    // Code: exactly 2 uppercase letters + 4 digits (e.g., AB1234)
    @Pattern(regexp = "^[A-Z]{2}\\d{4}$", message = "Code must be 2 uppercase letters followed by 4 digits")
    private String code;

    // Birth date must be in the past
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    // Expiry date must be in the future
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    // Amount must be positive
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    // Tags list cannot be empty
    @NotEmpty(message = "At least one tag is required")
    private List<String> tags;

    // Getters and setters
}
```

---

### BindingResult — Inspecting Validation Errors Manually

**Why use BindingResult:** Sometimes you want to handle validation errors yourself instead of throwing an exception. `BindingResult` holds the list of errors.

**Order matters:** `BindingResult` must come *immediately after* the validated parameter.

```java
@PostMapping("/users")
public ResponseEntity<?> create(
        @Valid @RequestBody UserDto dto,
        BindingResult bindingResult  // Must be right after @Valid parameter
) {
    // Check if validation failed
    if (bindingResult.hasErrors()) {
        // Build a map: field name -> error message
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    // No errors — proceed
    UserDto created = userService.create(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

**When to use BindingResult vs global handler:**
- **BindingResult:** When one endpoint needs custom logic (e.g., different status code, logging)
- **Global handler:** When you want the same error format for all endpoints (usually preferred)

---

### Validation in Services

**Why validate in services:** Controller validation checks format and basic rules. Service validation handles business rules (e.g., "email must not already exist").

**How:** Put `@Validated` on the service class and `@Valid` on method parameters. This requires method validation to be enabled.

```java
// Enable method-level validation (Spring Boot does this automatically with starter-validation)
// You need @Validated on the class for method params to be validated

@Service
@Validated  // Enables validation on method parameters
public class UserService {

    public User create(@Valid UserDto dto) {
        // Validation runs before method executes
        return userRepository.save(convertToEntity(dto));
    }

    public User findById(@NotNull @Positive Long id) {
        // Validates that id is not null and > 0
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

---

### Returning Clean Error Messages to the Client

**Goal:** Return a structured JSON with field-level errors so the client can show them next to each input.

**1. Global exception handler:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex
    ) {
        // Extract field errors: field name -> message
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        ErrorResponse response = new ErrorResponse("Validation failed", errors);
        return ResponseEntity.badRequest().body(response);
    }
}
```

**2. Error response DTO:**

```java
public class ErrorResponse {
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ErrorResponse(String message, Map<String, String> errors) {
        this.message = message;
        this.errors = errors;
    }

    // Getters and setters
}
```

**3. Example JSON returned to client:**

```json
{
  "message": "Validation failed",
  "errors": {
    "email": "Please provide a valid email address",
    "age": "Age must be at least 18"
  },
  "timestamp": "2026-02-17T14:30:00"
}
```

---

### Validation Groups — Different Rules for Create vs Update

**Why:** Create and update often need different rules. Example: ID is required on update, but must be absent on create.

**How:** Define marker interfaces (groups) and assign constraints to groups.

```java
// Define groups (empty interfaces are markers)
public interface OnCreate {}
public interface OnUpdate {}

public class UserDto {

    // ID required only when updating
    @NotNull(groups = OnUpdate.class, message = "ID is required for update")
    private Long id;

    // Name required for both create and update
    @NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "Name is required")
    @Size(min = 2, max = 50, groups = {OnCreate.class, OnUpdate.class})
    private String name;

    // Email required and validated on create (and optionally on update)
    @NotBlank(groups = OnCreate.class, message = "Email is required")
    @Email(groups = {OnCreate.class, OnUpdate.class})
    private String email;

    // Getters and setters
}

// Controller uses @Validated with the group
@PostMapping
public UserDto create(@Validated(OnCreate.class) @RequestBody UserDto dto) {
    // Only OnCreate validations run
    return userService.create(dto);
}

@PutMapping("/{id}")
public UserDto update(
        @PathVariable Long id,
        @Validated(OnUpdate.class) @RequestBody UserDto dto
) {
    dto.setId(id);
    return userService.update(dto);
}
```

**Note:** Use `@Validated` (not `@Valid`) when specifying groups.

---

### Nested Object Validation

**Why:** DTOs can contain other objects (e.g., User has Address). Those nested objects need validation too.

**How:** Put `@Valid` on the nested field. Without it, nested objects are not validated.

```java
public class AddressDto {
    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @Pattern(regexp = "^\\d{5}$", message = "Zip code must be 5 digits")
    private String zipCode;

    // Getters and setters
}

public class UserDto {
    @NotBlank
    private String name;

    // @Valid is REQUIRED for nested validation — without it, AddressDto is skipped!
    @Valid
    @NotNull(message = "Address is required")
    private AddressDto address;

    // Also works for collections
    @Valid
    @NotEmpty(message = "At least one phone number is required")
    private List<PhoneDto> phones;

    // Getters and setters
}

// Controller — single @Valid triggers validation of UserDto AND nested AddressDto
@PostMapping("/users")
public UserDto create(@Valid @RequestBody UserDto dto) {
    return userService.create(dto);
}
```

---

### Best Practices Summary

| Practice | Why |
|----------|-----|
| Validate at controller level | First defense; invalid data never enters your system |
| Use DTOs for input | Don't validate entities directly; keep API contract separate |
| Custom messages on annotations | Users see clear, actionable errors |
| Use @Valid on nested objects | Nested validation is skipped without it |
| Use @Validated for groups | Enables create vs update and other conditional validation |
| Global exception handler | Consistent error format across all endpoints |
| Don't rely on client-side only | Always validate on the server |

---

## Layer 3 — Advanced Engineering Depth

### Custom Validators

When built-in annotations aren't enough (e.g., "email must not already exist"), you create a **custom validator**.

**1. Define the annotation:**

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
@Documented
public @interface UniqueEmail {
    String message() default "Email already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**2. Implement the validator (can inject Spring beans):**

```java
@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(UniqueEmail constraintAnnotation) {
        // Called once when validator is created
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null) {
            return true;  // Let @NotNull handle null
        }

        boolean exists = userRepository.existsByEmail(email);

        if (exists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Email '" + email + "' is already registered"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
```

**3. Use in DTO:**

```java
public class CreateUserRequest {
    @NotBlank
    @Email
    @UniqueEmail(message = "This email is already registered")
    private String email;
}
```

---

### Cross-Field Validation (e.g., Password Match)

When one field depends on another (e.g., password and confirm password), use a **class-level** annotation.

**1. Annotation (class-level):**

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {
    String message() default "Passwords do not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String password();
    String confirmPassword();
}
```

**2. Validator:**

```java
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        this.passwordField = constraintAnnotation.password();
        this.confirmPasswordField = constraintAnnotation.confirmPassword();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        BeanWrapper wrapper = new BeanWrapperImpl(obj);

        String password = (String) wrapper.getPropertyValue(passwordField);
        String confirmPassword = (String) wrapper.getPropertyValue(confirmPasswordField);

        if (password == null || !password.equals(confirmPassword)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Passwords do not match")
                    .addPropertyNode(confirmPasswordField)
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
```

**3. Use on DTO:**

```java
@PasswordMatch(password = "password", confirmPassword = "confirmPassword")
public class CreateUserRequest {
    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String confirmPassword;
}
```

---

### Error Message Customization

**1. Custom messages in annotations:**

```java
@NotBlank(message = "Name is required")
@Email(message = "Please provide a valid email address")
```

**2. Externalized messages (validation-messages.properties):**

```properties
# src/main/resources/validation-messages.properties
javax.validation.constraints.NotNull.message=This field is required
javax.validation.constraints.NotBlank.message=This field cannot be blank
javax.validation.constraints.Email.message=Please provide a valid email address
javax.validation.constraints.Size.message=Size must be between {min} and {max}
user.name.required=User name is required
user.age.minimum=You must be at least {value} years old
```

```java
@NotBlank(message = "{user.name.required}")
private String name;

@Min(value = 18, message = "{user.age.minimum}")
private Integer age;
```

**3. Internationalization (i18n):**

```properties
# messages_en.properties
user.name.required=Name is required

# messages_es.properties
user.name.required=El nombre es obligatorio
```

Configure `MessageSource` and wire it into the validator for locale-based messages.

---

### Group Sequences (Validation Order)

Control the order in which groups are validated:

```java
@GroupSequence({BasicInfo.class, ContactInfo.class, UserDto.class})
public class UserDto {

    @NotBlank(groups = BasicInfo.class)
    private String name;

    @Email(groups = ContactInfo.class)
    private String email;
}

// Validates: BasicInfo first, then ContactInfo, then default
@PostMapping("/users")
public UserDto create(@Validated(UserDto.class) @RequestBody UserDto dto) {
    return userService.create(dto);
}
```

---

### Programmatic Validation

Validate without annotations, e.g., in services:

```java
@Service
public class UserService {

    private final Validator validator;

    public UserService(Validator validator) {
        this.validator = validator;
    }

    public User createUser(UserDto dto) {
        Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            Map<String, String> errors = violations.stream()
                    .collect(Collectors.toMap(
                            v -> v.getPropertyPath().toString(),
                            ConstraintViolation::getMessage
                    ));
            throw new ValidationException("Validation failed", errors);
        }

        return userRepository.save(convertToEntity(dto));
    }
}
```

---

### Performance Considerations

**1. Custom validators that hit the database:**
- Cache results when appropriate (e.g., "email exists" checks)
- Avoid N+1 queries; use bulk checks if validating many items

**2. Validator instances:**
- Validators are typically cached and reused; no need to create new ones per request

**3. When to validate:**
- Validate at the boundary (controllers); avoid redundant validation deep in the call stack

---

### Validation in Different Contexts

| Context | How | Notes |
|---------|-----|-------|
| REST Controllers | `@Valid` / `@Validated` on `@RequestBody`, `@ModelAttribute` | Primary use case |
| Service methods | `@Validated` on class + `@Valid` on params | For business-layer checks |
| JPA entities | `@Valid` on entity fields | Enable via `javax.persistence.validation.mode` |
| Programmatic | `Validator.validate()` | For manual validation in services |

---

## Layer 4 — Interview Mastery

### Q1: How does Bean Validation work in Spring Boot?

**Answer:** Bean Validation (JSR 380) is the Java standard for validating objects. In Spring Boot you add `spring-boot-starter-validation`, annotate fields (e.g., `@NotNull`, `@Email`), and use `@Valid` or `@Validated` on method parameters. Spring triggers validation before the method runs; on failure it throws `MethodArgumentNotValidException`, which contains a `BindingResult` with field errors. Hibernate Validator is the default implementation.

---

### Q2: What's the difference between @Valid and @Validated?

**Answer:**
- **@Valid (JSR 380):** Standard annotation; triggers validation; no support for groups.
- **@Validated (Spring):** Extends behavior; supports validation groups; can be used at class level for method-level validation.

Use `@Valid` for straightforward validation. Use `@Validated` when you need groups or method-level validation on services.

---

### Q3: How do you create a custom validator?

**Answer:** (1) Define an annotation with `@Constraint(validatedBy = YourValidator.class)`. (2) Implement `ConstraintValidator<YourAnnotation, TargetType>`. (3) Override `isValid()` with your logic. Return `true` for valid, `false` for invalid. For null, return `true` and let `@NotNull` handle it. Use `ConstraintValidatorContext` to customize error messages. Validators can be Spring beans and inject other beans.

---

### Q4: How do you handle validation errors and return clean messages?

**Answer:** Implement a `@RestControllerAdvice` with `@ExceptionHandler(MethodArgumentNotValidException.class)`. From `ex.getBindingResult().getFieldErrors()` build a map of field → message. Return it in a structured DTO (e.g., `ErrorResponse`) with HTTP 400. Optionally use `MessageSource` for i18n.

---

### Q5: What are validation groups and when would you use them?

**Answer:** Validation groups let you run different sets of constraints in different scenarios. Define marker interfaces (e.g., `OnCreate`, `OnUpdate`) and assign constraints to groups. Use `@Validated(OnCreate.class)` or `@Validated(OnUpdate.class)` to trigger the right group. Common use: different rules for create vs update (e.g., ID required only on update).

---

### Q6: How do you validate nested objects?

**Answer:** Put `@Valid` on the nested field. Without `@Valid`, nested objects are not validated. Example: `@Valid @NotNull private AddressDto address;`

---

### Q7: What is BindingResult and when do you use it?

**Answer:** `BindingResult` holds validation errors when using `@Valid`. It must be the parameter immediately after the validated parameter. Use it when you need custom handling in a specific endpoint (e.g., different response format, extra logging). For consistent API behavior, a global exception handler is usually preferred.

---

### Q8: @NotNull vs @NotBlank vs @NotEmpty?

**Answer:**
- **@NotNull:** Value cannot be null. Empty string `""` is allowed.
- **@NotEmpty:** For String: not null and not `""`. For collections: not null and not empty.
- **@NotBlank:** For String only: not null, not empty, and not whitespace-only.

For text fields like name and email, use `@NotBlank`.

---

## Summary

**Key Takeaways:**
1. **Validation is multi-layered** — Client, Controller, Service, Database; each has a role.
2. **Bean Validation (JSR 380)** — Java standard; use annotations + `@Valid` / `@Validated`.
3. **@Valid vs @Validated** — Use `@Validated` for groups and method-level validation.
4. **Custom validators** — For rules like "email unique" or "passwords match".
5. **Validation groups** — Different rules for create vs update.
6. **Error handling** — Global handler + structured DTO for consistent, clear messages.
7. **Nested validation** — Use `@Valid` on nested fields.

**Best Practices:**
- Validate at controller level first.
- Use DTOs for input, not entities.
- Write clear, user-friendly error messages.
- Use `@Valid` on nested objects.
- Use validation groups for create/update.
- Never rely on client-side validation alone.

**Common Pitfalls:**
- Forgetting `@Valid` on nested objects.
- Not handling `MethodArgumentNotValidException` globally.
- Trusting only client-side validation.
- Unclear or technical error messages.

**Next Steps:**
- Exception handling (Chapter 22)
- DTO patterns (Chapter 23)
- Spring Security (Chapter 29)

---

[← Back to Index](00-README.md) | [Previous: Controllers](20-Controllers.md) | [Next: Exception Handling →](22-Exception-Handling.md)
