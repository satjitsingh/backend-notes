# Chapter 20: Controllers & Request Handling

[← Back to Index](00-README.md) | [Previous: REST Design](19-REST-Design.md) | [Next: Validation →](21-Validation.md)

---

## Why This Chapter Matters

Controllers are the **front door of your application**. Every HTTP request — every click a user makes, every API call from a mobile app, every request from another service — enters your application through a controller.

If your controllers are messy, your entire API is messy. If your controllers are well-designed, everything downstream benefits.

**Without understanding controllers:**
- You'll put business logic inside controllers (a common anti-pattern)
- You'll create inconsistent APIs that confuse frontend developers
- You'll struggle with "where does this code go?" decisions
- You'll write controllers that are impossible to test
- You'll return inconsistent error messages

**With proper controller understanding:**
- You'll build clean, predictable APIs that frontend developers love
- You'll follow the "thin controller" pattern (controllers delegate, not compute)
- You'll handle every type of request parameter (path, query, body, headers)
- You'll understand the full request lifecycle from HTTP request to JSON response
- You'll create testable code that's easy to maintain

---

## Layer 1 — Intuition Builder

### Part 1: What Is a Controller? — The Hotel Receptionist

Imagine you run a big hotel. Thousands of guests arrive every day. They all need different things: check-in, room service, housekeeping, check-out. How do you make sure everyone gets to the right place?

You hire a **receptionist** who sits at the front desk.

**The receptionist's job is simple:**
1. **Listen** to what each guest wants (receive the request)
2. **Figure out** who can help them (find the right department)
3. **Send them** to that department (delegate the work)
4. **Give them** the answer or confirmation (return the response)

The receptionist does **NOT** clean rooms. The receptionist does **NOT** cook food. The receptionist does **NOT** handle money. They **direct** people to the right place.

**A controller in your application does exactly the same thing.** It's the receptionist at the front desk of your software. When a request comes in — like "Give me user #123" or "Create a new order" — the controller:
1. Listens (receives the request)
2. Figures out what's needed (understands the request)
3. Sends the work to the right place (calls a service)
4. Gives back the answer (returns the response)

**The golden rule:** Just like the receptionist never mops the floor, the controller never does the "real work" — the business logic. It only directs traffic.

---

### Part 2: What's an HTTP Request? — Letters in the Mail

Before we go further, let's understand what a "request" actually is.

Think of the internet like a postal system. When you click "Submit" on a website or when an app fetches data, it's like sending a **letter**. That letter has:

- **An address** — Where is it going? (Like `example.com/api/users`)
- **A type of envelope** — Is it a "please send me something" letter (GET), or a "please take this and save it" letter (POST)?
- **Optional sticky notes** — Extra info like "I want the answer in English" (headers)
- **Optional stuff inside** — For "save this" letters, there might be data in the envelope (the body)

**A request** = one of these letters arriving at your server. **A controller** = the person at your server who opens the letter, reads it, and decides what to do.

---

### Part 3: Two Kinds of Controllers — Dine-In vs Takeout

Imagine two types of restaurants:

**Restaurant A (Dine-In):** You sit down, order food, and the waiter brings you a **complete plated meal** on a nice plate. You don't see the raw ingredients — you see the final dish.

**Restaurant B (Takeout/Meal Kit):** You order, and they give you **raw ingredients + a recipe**. You take it home and cook/display it yourself.

In web development, we have the same two approaches:

- **@Controller (Dine-In):** Your server prepares the **entire final page** (HTML) and sends it to the user. The user sees a complete web page, ready to view.
- **@RestController (Takeout/Meal Kit):** Your server sends **raw data** (like JSON — structured text that describes objects). The user's browser or app takes that data and decides how to show it (builds the page itself).

**Most modern apps use @RestController** — because we build phone apps and single-page websites that want the data and will display it themselves. In this guide, we focus on @RestController.

---

### Part 4: How Does Spring Know Which Method to Call? — The Mailbox System

Imagine your hotel has many mailboxes. Each mailbox has:
- A **number** (like "/api/users" or "/api/users/123")
- A **type** (only "GET" letters go in the GET mailbox, only "POST" letters go in the POST mailbox)

When a letter arrives, the receptionist looks at the address and the type, finds the right mailbox, and follows the instructions inside that mailbox.

**In Spring:**
- The "address" is the **URL path** (like `/api/users` or `/api/users/42`)
- The "type" is the **HTTP method** (GET, POST, PUT, DELETE)
- Each "mailbox" is a **method** in your controller

So when a request comes in: `GET /api/users/42`, Spring finds the controller method that handles "GET" requests to that path, and runs it. The `42` gets passed into the method as a number (the user ID we want).

---

### Part 5: Where Does the Data Come From? — Five Hiding Spots

When someone sends a request, the information you need can be in different places:

| Place | Plain English | Example |
|-------|---------------|---------|
| **In the URL path** | Part of the address itself | `/users/42` — the 42 is in the path |
| **In the query string** | After the `?` in the URL | `/users?name=John` — name is a "question" we're asking |
| **In the envelope body** | The actual content of a POST/PUT letter | JSON data like `{"name":"John","email":"john@example.com"}` |
| **In the headers** | Sticky notes on the envelope | `Authorization: Bearer abc123` — who sent this? |
| **In a cookie** | A small note your server gave them earlier | `sessionId=xyz` — we remember you! |

Spring has special "labels" (annotations) to grab data from each place:
- `@PathVariable` — from the path
- `@RequestParam` — from the query string
- `@RequestBody` — from the body
- `@RequestHeader` — from headers
- `@CookieValue` — from cookies

---

### Part 6: What Do We Send Back? — The Response

After the controller gets the work done (by calling a service), it needs to send something back. Two things matter:

1. **The status** — Did it work? (200 = yes, 404 = not found, 500 = server error)
2. **The body** — The actual data or message

**ResponseEntity** is like a "complete package" — you choose the status AND the body. When you don't need that much control, you can use simpler options like `@ResponseStatus` for just setting the status.

---

### Part 7: The One Rule to Rule Them All — Thin Controllers

Remember the receptionist? They never do the cleaning. **Thin controller** means: your controller methods should be **short**. They should:
1. Receive the request
2. Call one service method
3. Return the result

If your controller has lots of if/else logic, calculations, database calls, or business rules — **that's wrong**. Put that in a **service** instead. The controller is just the messenger.

---

## Layer 2 — Professional Developer

### @Controller vs @RestController — Technical Deep Dive

**Why the distinction exists:** Traditional web apps (1990s–2000s) rendered HTML on the server. Modern apps (APIs, SPAs, mobile) send data and let the client render. Spring supports both.

| Aspect | @Controller | @RestController |
|--------|-------------|-----------------|
| **Purpose** | Server-rendered views (HTML) | REST APIs (JSON/XML data) |
| **Return value** | View name (String) → resolved to HTML | Object → serialized to JSON |
| **Under the hood** | ViewResolver resolves "users/list" → template | HttpMessageConverter serializes User → JSON |
| **Use case** | Thymeleaf, JSP, server-side MVC | REST APIs, React/Angular backends, microservices |

**@RestController** is a shortcut: it's `@Controller` + `@ResponseBody` on every method. So every return value is automatically turned into the response body (usually JSON).

```java
// @RestController = @Controller + @ResponseBody everywhere
@RestController
@RequestMapping("/api/users")
public class UserController {
    // Return value automatically becomes JSON response body
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);  // User object → JSON
    }
}
```

---

### Request Mapping: @RequestMapping and HTTP Method Annotations

**Why we need mapping:** Spring must decide *which* controller method handles *which* request. Mapping annotations create that connection.

**@RequestMapping** — Base path and shared settings for a class. Can specify path, method, headers, etc.

**Convenience annotations** — Shortcuts for each HTTP method:
- `@GetMapping` — Handle GET (read/fetch)
- `@PostMapping` — Handle POST (create)
- `@PutMapping` — Handle PUT (replace entire resource)
- `@PatchMapping` — Handle PATCH (partial update)
- `@DeleteMapping` — Handle DELETE (remove)

| Annotation | HTTP Method | Typical Use |
|------------|-------------|-------------|
| @GetMapping | GET | Retrieve resource(s) |
| @PostMapping | POST | Create new resource |
| @PutMapping | PUT | Replace entire resource |
| @PatchMapping | PATCH | Update part of resource |
| @DeleteMapping | DELETE | Remove resource |

---

### Complete CRUD Controller Example

Below is a full controller for a "Book" resource with Create, Read, Update, Delete. Every line is commented to show *why* it's there.

```java
package com.example.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Handles all HTTP requests for the /api/books path.
 * This is a THIN controller: it only receives requests, delegates to the service,
 * and returns responses. No business logic here.
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    // Constructor injection: Spring passes BookService when creating this controller
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * GET /api/books — List all books.
     * Returns 200 OK with a JSON array of books.
     */
    @GetMapping
    public ResponseEntity<List<BookDto>> getAllBooks() {
        List<BookDto> books = bookService.findAll();
        return ResponseEntity.ok(books);  // 200 OK, body = books
    }

    /**
     * GET /api/books/42 — Get one book by ID.
     * The {id} in the path becomes the method parameter via @PathVariable.
     * Returns 200 OK if found, 404 Not Found if not.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBook(@PathVariable Long id) {
        return bookService.findById(id)
            .map(book -> ResponseEntity.ok(book))      // Found → 200 OK
            .orElse(ResponseEntity.notFound().build()); // Not found → 404
    }

    /**
     * POST /api/books — Create a new book.
     * The request body (JSON) is automatically converted to CreateBookRequest.
     * Returns 201 Created with the new book and Location header.
     */
    @PostMapping
    public ResponseEntity<BookDto> createBook(@RequestBody CreateBookRequest request) {
        BookDto created = bookService.create(request);
        // Build the URL of the new resource: /api/books/{newId}
        var location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
        return ResponseEntity.created(location).body(created);  // 201 Created
    }

    /**
     * PUT /api/books/42 — Replace entire book.
     * Path variable for ID + body for new content.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookDto> updateBook(
            @PathVariable Long id,
            @RequestBody UpdateBookRequest request) {
        return bookService.update(id, request)
            .map(book -> ResponseEntity.ok(book))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/books/42 — Remove a book.
     * Returns 204 No Content on success (no body needed).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        return bookService.delete(id)
            ? ResponseEntity.noContent().build()   // 204 No Content
            : ResponseEntity.notFound().build();   // 404 Not Found
    }
}
```

---

### Parameter Binding: Annotations Reference

**Why parameter binding matters:** You need data from the request (path, query, body, headers) as Java objects. Spring does this automatically when you use the right annotations.

| Annotation | Purpose | Where data comes from |
|------------|---------|------------------------|
| @PathVariable | Extract from URL path | `/users/{id}` → `id` |
| @RequestParam | Extract from query string | `?name=John` → `name` |
| @RequestBody | Deserialize JSON/XML body | Request body → DTO object |
| @RequestHeader | Extract HTTP header | `Authorization: Bearer xyz` |
| @CookieValue | Extract cookie | `sessionId=abc` |
| @ResponseBody | (Rare) Mark return as response body | Method return → response |

#### @PathVariable — Values from the Path

```java
// GET /api/users/123
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    // id = 123 (converted from string to Long automatically)
    return userService.findById(id);
}

// GET /api/users/5/orders/42 — multiple path variables
@GetMapping("/{userId}/orders/{orderId}")
public Order getOrder(
    @PathVariable("userId") Long userId,
    @PathVariable("orderId") Long orderId
) {
    return orderService.findByUserAndOrder(userId, orderId);
}

// Optional path variable — match both /users and /users/123
@GetMapping({"/users", "/users/{id}"})
public Object getUsers(@PathVariable(required = false) Long id) {
    return id == null ? userService.findAll() : userService.findById(id);
}
```

#### @RequestParam — Values from the Query String

```java
// GET /api/users?name=John&age=25
@GetMapping("/search")
public List<User> search(
    @RequestParam String name,                                    // Required
    @RequestParam(required = false) Integer age,                  // Optional
    @RequestParam(required = false, defaultValue = "0") int page  // Optional, default 0
) {
    return userService.search(name, age, page);
}

// Multiple values: ?tags=java&tags=spring
@GetMapping("/users")
public List<User> getUsers(@RequestParam List<String> tags) {
    return userService.findByTags(tags);
}

// Different param name than variable: ?user-name=John
@GetMapping("/users")
public User getUser(@RequestParam("user-name") String userName) {
    return userService.findByUsername(userName);
}
```

#### @RequestBody — JSON Body to Object

```java
// POST body: {"name":"John","email":"john@example.com"}
@PostMapping("/users")
public User create(@RequestBody CreateUserRequest request) {
    // Spring uses Jackson to convert JSON → CreateUserRequest
    return userService.create(request);
}

// With validation (Chapter 21)
@PostMapping("/users")
public User create(@Valid @RequestBody CreateUserRequest request) {
    return userService.create(request);
}
```

#### @RequestHeader — Values from Headers

```java
@GetMapping("/data")
public Data getData(
    @RequestHeader("Authorization") String auth,
    @RequestHeader(value = "X-Custom-Header", required = false) String custom,
    @RequestHeader Map<String, String> allHeaders
) {
    // Use auth for authentication, custom if provided
    return dataService.getData(auth);
}
```

#### @CookieValue — Values from Cookies

```java
@GetMapping("/profile")
public Profile getProfile(@CookieValue("sessionId") String sessionId) {
    return profileService.getProfileForSession(sessionId);
}

@GetMapping("/profile")
public Profile getProfile(
    @CookieValue(value = "sessionId", required = false) String sessionId
) {
    return sessionId != null
        ? profileService.getProfileForSession(sessionId)
        : profileService.getAnonymousProfile();
}
```

---

### Response Handling: ResponseEntity vs @ResponseStatus

**Why both exist:** Sometimes you only need to set a fixed status (e.g., "POST always returns 201"). Sometimes the status depends on the result (found → 200, not found → 404). Spring gives you both options.

| Approach | Use when | Can set headers? | Dynamic status? |
|----------|----------|------------------|-----------------|
| ResponseEntity | Full control needed | Yes | Yes |
| @ResponseStatus | Simple, fixed status | No | No |

```java
// ResponseEntity — dynamic status based on result
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return userService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}

// @ResponseStatus — fixed status
@PostMapping("/users")
@ResponseStatus(HttpStatus.CREATED)
public User create(@RequestBody UserDto dto) {
    return userService.create(dto);
}

@DeleteMapping("/users/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable Long id) {
    userService.delete(id);
}
```

**Important:** `@ResponseStatus` only affects *successful* returns. For errors (404, 500), use `ResponseEntity` or exception handlers (Chapter 22).

---

### HTTP Status Codes in Controllers

| Code | Constant | Meaning |
|------|----------|---------|
| 200 | OK | Success, body contains data |
| 201 | CREATED | Resource created, usually with Location header |
| 204 | NO_CONTENT | Success, no body (common for DELETE) |
| 400 | BAD_REQUEST | Invalid input |
| 401 | UNAUTHORIZED | Not authenticated |
| 403 | FORBIDDEN | Not authorized |
| 404 | NOT_FOUND | Resource not found |
| 500 | INTERNAL_SERVER_ERROR | Server error |

---

### Request/Response Flow Through a Controller

```
1. HTTP Request arrives (e.g., GET /api/users/123)
        ↓
2. DispatcherServlet receives it (Spring's front controller)
        ↓
3. HandlerMapping finds the matching controller method (GET + /api/users/{id})
        ↓
4. Spring extracts path variables, query params, reads body if present
        ↓
5. Spring converts request body (JSON) to Java object via HttpMessageConverter
        ↓
6. Controller method is invoked with the parameters
        ↓
7. Controller calls service, gets result
        ↓
8. Controller returns ResponseEntity or object
        ↓
9. Spring converts return value to JSON via HttpMessageConverter
        ↓
10. HTTP Response sent back (status + body)
```

---

### Thin vs Fat Controllers — Good vs Bad

**Why thin matters:** Controllers are the hardest to unit test when they contain logic. Business logic belongs in services, which are easy to test. Controllers should be glue code only.

```java
// ❌ BAD: Fat controller with business logic
@RestController
@RequestMapping("/api/orders")
public class BadOrderController {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;

    @PostMapping
    public Order create(@RequestBody CreateOrderRequest req) {
        // Business logic in controller!
        if (req.getQuantity() <= 0) throw new IllegalArgumentException("Invalid quantity");
        Product p = productRepository.findById(req.getProductId()).orElseThrow();
        double price = p.getPrice() * req.getQuantity();
        if (req.getCouponCode() != null) {
            price = price * 0.9;  // 10% off — business rule in controller!
        }
        Order order = new Order();
        order.setAmount(price);
        return orderRepository.save(order);
    }
}

// ✅ GOOD: Thin controller — delegates everything
@RestController
@RequestMapping("/api/orders")
public class GoodOrderController {
    private final OrderService orderService;

    public GoodOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.createOrder(req);  // All logic in service
    }
}
```

---

### Best Practices Summary

| Practice | Reason |
|----------|--------|
| Keep controllers thin | Testability, separation of concerns |
| Use @Valid with @RequestBody | Catch invalid input early |
| Return ResponseEntity when status varies | 404 vs 200, etc. |
| Use @ResponseStatus for fixed success status | Cleaner code for simple cases |
| Use consistent response format | Easier for API consumers |
| No business logic in controllers | Logic belongs in services |

---

## Layer 3 — Advanced Engineering Depth

### Request Processing Pipeline — Internal Flow

**How Spring routes a request to the right method and back:**

```
HTTP Request
    ↓
DispatcherServlet (front controller — receives all requests)
    ↓
HandlerMapping (builds registry of @RequestMapping methods at startup)
    ↓
Match: HTTP method + URL pattern + params/headers/consumes/produces
    ↓
HandlerAdapter (RequestMappingHandlerAdapter)
    ↓
Resolve method parameters (PathVariable, RequestParam, RequestBody, etc.)
    ↓
HttpMessageConverter converts request body (JSON → Object)
    ↓
Controller method invoked
    ↓
Return value processed
    ↓
HttpMessageConverter converts response (Object → JSON)
    ↓
HTTP Response
```

#### HandlerMapping — How Spring Finds the Method

At startup, `RequestMappingHandlerMapping` scans all `@RequestMapping` methods and builds a registry. For each request, it finds the best match by:

1. HTTP method (GET, POST, etc.)
2. URL pattern (with path variables)
3. Additional conditions (params, headers, consumes, produces)

The most specific match wins. Overlapping patterns can cause ambiguity — avoid them.

#### HandlerAdapter — Executing the Method

`RequestMappingHandlerAdapter`:

1. Resolves each method parameter using `HandlerMethodArgumentResolver`s
2. Converts the request body using `HttpMessageConverter`
3. Invokes the method
4. Handles the return value (wraps in ResponseEntity if needed, converts to bytes)

#### HttpMessageConverter — Body Conversion

- **Request:** `Content-Type: application/json` + body → Jackson → Java object
- **Response:** Java object → Jackson → `application/json` bytes

Default converters include `MappingJackson2HttpMessageConverter` (JSON), `StringHttpMessageConverter`, etc. You can add custom converters via `WebMvcConfigurer`.

---

### Content Negotiation

**How Spring chooses response format (JSON vs XML, etc.):**

1. **Accept header** — `Accept: application/json`
2. **URL extension** — `/users/1.json`
3. **Query parameter** — `/users/1?format=json` (if configured)

Configuration:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer config) {
        config
            .favorParameter(true)
            .parameterName("format")
            .ignoreAcceptHeader(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML);
    }
}
```

---

### Async Controllers

**When to use:** Long-running operations (external API calls, heavy computation) that would block a thread.

```java
// Callable — executed in a separate thread from the request thread
@GetMapping("/slow")
public Callable<User> slowEndpoint() {
    return () -> userService.findHeavyUser();  // Runs in async executor
}

// CompletableFuture
@GetMapping("/async")
public CompletableFuture<User> asyncEndpoint() {
    return CompletableFuture.supplyAsync(() -> userService.findHeavyUser());
}

// DeferredResult — manual completion
@GetMapping("/deferred")
public DeferredResult<User> deferredEndpoint() {
    DeferredResult<User> result = new DeferredResult<>();
    CompletableFuture.supplyAsync(() -> userService.findHeavyUser())
        .whenComplete((user, ex) -> {
            if (ex != null) result.setErrorResult(ex);
            else result.setResult(user);
        });
    return result;
}
```

**Caveat:** Default async executor is shared. For production, configure a dedicated `AsyncConfigurer` with appropriate pool size and queue capacity.

---

### Custom Argument Resolvers

**Use case:** Inject "current user" or other context that you derive from the request (e.g., JWT).

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}

@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter param) {
        return param.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter param, ModelAndViewContainer mav,
                                  NativeWebRequest request, WebDataBinderFactory factory) {
        String token = request.getHeader("Authorization");
        return userService.getUserFromToken(token);
    }
}

// Register in WebMvcConfigurer.addArgumentResolvers

// Use in controller
@GetMapping("/me")
public User getMe(@CurrentUser User user) {
    return user;
}
```

---

### Performance Considerations

| Concern | Approach |
|---------|----------|
| **Caching** | Use `@Cacheable` on service methods, not controllers |
| **Compression** | `server.compression.enabled=true` in `application.properties` |
| **Connection pooling** | Configure HikariCP for DB (separate concern) |
| **Blocking I/O** | Prefer async or reactive for I/O-bound work |
| **Large responses** | Use pagination; avoid loading entire collections |

---

### Edge Cases and Pitfalls

1. **Optional @PathVariable** — Use `required = false` and handle null. Match both patterns explicitly: `@GetMapping({"/users", "/users/{id}"})`.
2. **@RequestBody and read-once** — Request body stream can only be read once. Don't read it manually if you also use `@RequestBody`.
3. **Content-Type mismatch** — If client sends wrong `Content-Type`, converter may fail. Return 415 Unsupported Media Type via exception handler.
4. **Large request bodies** — Set `spring.servlet.multipart.max-file-size` and `max-request-size` if accepting uploads.
5. **Circular references in JSON** — Use `@JsonIgnore` or `@JsonManagedReference`/`@JsonBackReference` to avoid infinite loops when serializing entities.

---

## Layer 4 — Interview Mastery

### Q1: What is the difference between @Controller and @RestController?

**Short:** `@Controller` returns view names for server-rendered HTML. `@RestController` returns data (e.g., JSON) directly. `@RestController` = `@Controller` + `@ResponseBody` on every method.

**Deep:** `@Controller` uses ViewResolver to render templates. `@RestController` uses HttpMessageConverter to serialize return values. Use `@RestController` for REST APIs; use `@Controller` for traditional MVC with Thymeleaf/JSP.

---

### Q2: Explain @RequestMapping, @GetMapping, @PostMapping, etc.

**Short:** `@RequestMapping` defines base path and conditions. `@GetMapping`, `@PostMapping`, etc. are shortcuts for `@RequestMapping(method = GET)`, etc. Use them for clarity and RESTful semantics.

**Deep:** All are processed by `RequestMappingHandlerMapping`. The convenience annotations reduce boilerplate and make the HTTP method explicit.

---

### Q3: @PathVariable vs @RequestParam — when to use each?

**Short:** `@PathVariable` for resource identity in the path (`/users/123`). `@RequestParam` for filtering, pagination, optional data in the query string (`?page=1&size=20`).

**Deep:** Path variables are part of the resource URL and typically required. Query params modify behavior (filter, sort, page). RESTful design uses path for identity, query for options.

---

### Q4: What does @RequestBody do?

**Short:** Binds the HTTP request body to a method parameter. Spring uses HttpMessageConverter (usually Jackson) to convert JSON/XML to a Java object based on `Content-Type`.

**Deep:** `Content-Type` selects the converter. Default is `application/json` → Jackson. The body stream is read once; don't consume it manually when using `@RequestBody`.

---

### Q5: What is @ResponseBody and when do you need it?

**Short:** Marks a return value as the HTTP response body (no view resolution). With `@RestController`, every method is implicitly `@ResponseBody`, so you rarely use it explicitly.

**Deep:** Use `@ResponseBody` on individual methods when using `@Controller` but needing to return data. With `@RestController`, it's redundant.

---

### Q6: When to use ResponseEntity vs @ResponseStatus?

**Short:** Use `ResponseEntity` when status or headers depend on the result (e.g., 200 vs 404). Use `@ResponseStatus` when the status is always the same for success (e.g., 201 for create, 204 for delete).

**Deep:** `@ResponseStatus` only affects successful returns. For errors, use `ResponseEntity` or `@ExceptionHandler` with the right status.

---

### Q7: Explain the thin controller principle.

**Short:** Controllers should only receive requests, delegate to services, and return responses. No business logic, no direct repository access, no complex conditionals.

**Deep:** Improves testability (mock the service), separation of concerns, and reusability. Business rules live in services; controllers are thin adapters between HTTP and the domain layer.

---

### Q8: What are @RequestHeader and @CookieValue for?

**Short:** `@RequestHeader` binds an HTTP header to a parameter (e.g., `Authorization`). `@CookieValue` binds a cookie value (e.g., `sessionId`). Both support `required = false` and `defaultValue`.

**Deep:** Common uses: auth tokens in headers, session IDs in cookies. Use `Map<String, String>` to receive all headers when needed.

---

### Q9: How does the request flow through a controller?

**Short:** Request → DispatcherServlet → HandlerMapping (finds method) → HandlerAdapter (resolves params, invokes method) → Controller → Service → Return → HttpMessageConverter (object → bytes) → Response.

**Deep:** Path variables and conditions are matched first. Then argument resolvers fill parameters. `@RequestBody` goes through `HttpMessageConverter`. Return value is wrapped or converted, then written to the response.

---

### Q10: How do you return different HTTP status codes from a controller?

**Short:** Use `ResponseEntity.status(HttpStatus.XXX).body(data)` or `ResponseEntity.ok()`, `.created(uri)`, `.notFound()`, `.noContent()`, etc. For fixed success status, use `@ResponseStatus(HttpStatus.XXX)` on the method.

**Deep:** For errors, prefer `@ControllerAdvice` + `@ExceptionHandler` to return consistent error responses with the right status codes.

---

## Summary

**Core concepts:**
- Controllers are the entry point for HTTP requests (like a receptionist).
- `@RestController` = `@Controller` + `@ResponseBody` — use for REST APIs.
- `@RequestMapping` / `@GetMapping` / `@PostMapping` / etc. map URLs and HTTP methods to methods.
- Use `@PathVariable`, `@RequestParam`, `@RequestBody`, `@RequestHeader`, `@CookieValue` to bind request data.
- Use `ResponseEntity` for full control; use `@ResponseStatus` for fixed success status.
- Keep controllers thin — delegate to services; no business logic in controllers.

**Best practices:**
- Thin controllers that delegate to services
- Validate input with `@Valid` and DTOs
- Use consistent response formats and HTTP status codes
- Prefer `ResponseEntity` when status varies

**Next steps:**
- Chapter 21: Validation
- Chapter 22: Exception handling
- Chapter 23: DTOs and mapping

---

[← Back to Index](00-README.md) | [Previous: REST Design](19-REST-Design.md) | [Next: Validation →](21-Validation.md)
