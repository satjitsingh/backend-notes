# Chapter 10: Testing Fundamentals (Prerequisites)

[← Back to Index](00-README.md) | [Previous: JSON & Serialization](09-Prerequisites-JSON.md) | [Next: What is Spring →](11-Spring-Introduction.md)

---

## Why This Chapter Matters

Imagine you built a robot that makes sandwiches. Would you send it to a sandwich shop without first checking that it actually works? Of course not! You'd test it in your kitchen first—make sure it spreads peanut butter, slices the bread, and doesn't drop everything on the floor.

**Writing software is the same.** Professional developers don't just write code and hope it works. They write **tests**—small programs that automatically check whether their code does what it's supposed to do. This chapter teaches you the fundamentals of testing in Java, so you can:

- **Catch bugs before users do** — Like checking your homework before handing it in
- **Change code without fear** — Tests act as a safety net when you improve or refactor code
- **Work like a professional** — Every serious Java job expects you to write tests
- **Save time** — Automated tests run in seconds; manual testing takes forever

---

## Layer 1 — Intuition Builder

*In this section, we explain testing using everyday ideas—no jargon, no confusing words. Think of it as a conversation with someone who has never written code.*

---

### Part 1: Why Do We Test? (The Homework Analogy)

When you finish your math homework, what do you do before handing it in? You **check your answers**. You look at problem 1 and think: "Did I get 42?" You look at problem 2: "Does 15 + 27 really equal 42?" If something looks wrong, you fix it *before* the teacher sees it.

**Testing is the same idea for computer programs.** Instead of you manually checking each "answer," you write a small program that checks it for you. That way:

- You can run hundreds of checks in one second
- You can run them every time you change something (to make sure you didn't break anything)
- You never forget to check (the computer never gets tired)

**The big idea:** Testing = automatically checking that your code gives the right answers.

---

### Part 2: What Is a "Unit" and Why Test One at a Time?

Imagine you're building a toy car. The car has: wheels, an engine, a steering wheel, and headlights. If the car doesn't drive properly, is it the wheels? The engine? The steering? It's hard to know when everything is connected.

**The smart way:** Test each part by itself first. Check that each wheel spins. Check that the engine makes noise. Check that the steering turns. When each part works alone, you have a better chance they'll work together.

**A "unit" in programming** = one small piece of your code—usually one class or one method. Like one wheel, or one lightbulb.

**A "unit test"** = a check that runs on just one unit, in isolation. We don't connect it to the database, the internet, or other big systems. We just ask: "When I give this method the number 5, does it return the right answer?"

**Why test one unit at a time?**
- If the test fails, you know *exactly* what's broken (not "something somewhere")
- Unit tests run super fast (thousands in seconds)
- You can run them constantly while you code

---

### Part 3: The Test Pyramid (Cupcakes vs. Birthday Cakes)

Think about desserts:

- **Cupcakes** — Small, quick to make, cheap. You can bake 24 in 20 minutes. If one is bad, you throw away one.
- **Layer cakes** — Bigger, take longer, cost more. You make a few for special occasions.
- **Wedding cakes** — Huge, expensive, take days. You make maybe one per event.

**The Test Pyramid works the same way:**

| Layer | What It Tests | How Many | Speed | Analogy |
|-------|---------------|----------|-------|---------|
| **Bottom (wide)** | One small piece of code | MANY (hundreds) | Very fast | Cupcakes |
| **Middle** | Several pieces working together | SOME (dozens) | Slower | Layer cakes |
| **Top (narrow)** | The whole app like a real user | FEW (a handful) | Very slow | Wedding cake |

**The rule:** Most of your tests should be "cupcakes" (unit tests). A few should be "layer cakes" (integration tests). Very few should be "wedding cakes" (end-to-end tests). Why? Because cupcakes are fast, cheap, and easy to fix when something goes wrong!

---

### Part 4: Arrange, Act, Assert (Making a Sandwich in Three Steps)

Every good test follows the same pattern, like making a sandwich:

1. **Arrange** — Get your ingredients ready. Bread on the plate. Peanut butter jar open. Knife in hand.
2. **Act** — Do the thing. Spread the peanut butter. Put the bread together.
3. **Assert** — Check the result. Does the sandwich look right? Can I take a bite?

In a test:
- **Arrange** = Set up the data and objects you need
- **Act** = Run the one line of code you're testing
- **Assert** = Check that the result is what you expected

**Example in plain English:**
- Arrange: I have a calculator and two numbers: 2 and 3
- Act: I tell the calculator to add them
- Assert: The answer should be 5

If the answer is 5, the test passes. If it's 6, the test fails—and I know something is wrong with the add function!

---

### Part 5: What Is Mocking? (The Stunt Double Analogy)

In action movies, when the hero has to jump off a building or get in a car crash, does the famous actor do it? Usually not! They use a **stunt double**—someone who looks similar and does the dangerous part. The movie still works. The audience can't tell the difference.

**Mocking in programming** = using a "stunt double" instead of the real thing.

**Why would we do that?** Sometimes the "real thing" is:
- **Slow** — Like a real database (takes seconds to start and run)
- **Unpredictable** — Like a real weather API (sometimes it's down, sometimes it returns different data)
- **Dangerous** — Like a real payment system (we don't want to charge real credit cards in a test!)
- **Hard to set up** — Like a real email server (needs configuration, accounts, etc.)

**The solution:** We create a fake version—a "stunt double"—that pretends to be the database or the API. We tell it: "When someone asks for user #1, pretend you found them and say their name is John." Our code runs. It thinks it's talking to the real database. But we're in control. Fast. Safe. Predictable.

**That fake version is called a "mock."** Mock = pretend version we control for testing.

---

### Part 6: FIRST — What Makes a Good Test?

Think of a good multiple-choice quiz vs. a bad one:

**A good quiz:**
- Takes 2 minutes (not 2 hours) — **Fast**
- Each question stands alone (you don't need answer 1 to understand question 2) — **Independent**
- Same answers every time you take it — **Repeatable**
- You know immediately if you passed — **Self-validating**
- Given right after the lesson (not 6 months later) — **Timely**

**A bad quiz:**
- Takes 3 hours — You never want to run it
- Question 3 depends on question 1's answer — Confusing and fragile
- Sometimes says "B" is right, sometimes "C" — Frustrating!
- Teacher has to grade by hand — Slow feedback
- Given years after you learned the topic — You forgot everything

**Tests should be like the good quiz.** The letters **FIRST** help us remember: **F**ast, **I**ndependent, **R**epeatable, **S**elf-validating, **T**imely.

---

### Part 7: Naming Tests (Tell a Story)

Bad test name: `test1` or `testCalculator`

Good test name: `shouldAddTwoNumbersCorrectly` or `shouldReturnErrorWhenUserNotFound`

**Why?** When a test fails, the first thing you see is its name. If the name is `test1`, you learn nothing. If the name is `shouldReturnErrorWhenUserNotFound`, you immediately know: "Something broke in the 'user not found' case."

**The pattern:** `should[ExpectedResult]When[Condition]`

Examples:
- `shouldReturnFiveWhenAddingTwoAndThree` — When I add 2 and 3, I should get 5
- `shouldThrowExceptionWhenPasswordIsEmpty` — When the password is empty, we should get an error

---

### Part 8: Code Coverage (Did I Check Everything?)

Imagine a teacher who only ever gives you problems 1, 3, and 5 on a test. You practice those and get 100% on them. But you never practiced problem 2 or 4. On the real test, you might fail those!

**Code coverage** = a report that says: "Which parts of your code did your tests actually run?" If you have 80% coverage, it means 80% of your code was executed during tests. The other 20% was never touched—those are the "problem 2 and 4" that might have bugs hiding.

**Is 100% coverage the goal?** Not really! Some code (like simple getters and setters) doesn't need tests. And you can have 100% coverage with weak tests—like practicing only easy problems. **Quality of tests matters more than the percentage.** Aim for good coverage of the important, tricky parts.

---

### Part 9: TDD — Tests Before Code (Red, Green, Refactor)

**TDD** = Test-Driven Development. It means: write the test *first*, then write the code to make it pass.

**The cycle (like a traffic light):**

1. **Red** — Write a test that fails (because the feature doesn't exist yet). The test is like a wish list: "I want the calculator to add numbers."
2. **Green** — Write the simplest code to make the test pass. Maybe you hardcode `return 5` at first. That's OK! The light turns green.
3. **Refactor** — Now improve the code. Replace the hardcoded 5 with real addition. The test still passes—you didn't break anything.

**Why do it this way?** It forces you to think about *what* you want before *how* you'll build it. It also guarantees you always have tests, because you wrote them first!

---

## Layer 2 — Professional Developer

*Now we get technical. Every concept is explained with WHY before HOW, and all code includes comments.*

---

### JUnit 5 Basics

**JUnit** is the standard library in Java for writing and running tests. Think of it as the "testing toolbox" that every Java developer uses. **JUnit 5** is the current version (also called JUnit Jupiter).

#### Why JUnit?

Without a framework, you'd write tests as random `main` methods and manually look at the output. JUnit gives you:
- **Annotations** — Mark methods as tests with `@Test`
- **Assertions** — Built-in ways to check results (`assertEquals`, `assertTrue`, etc.)
- **Lifecycle hooks** — Run setup code before/after each test
- **Test runners** — Run all tests, see pass/fail, get reports

#### @Test — The Heart of Every Test

The `@Test` annotation tells JUnit: "This method is a test. Run it and report pass or fail."

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    @Test
    void shouldAddTwoNumbers() {
        // Arrange: Create the object we're testing
        Calculator calculator = new Calculator();

        // Act: Call the method we're testing
        int result = calculator.add(2, 3);

        // Assert: Check that the result matches our expectation
        assertEquals(5, result, "Adding 2 and 3 should give 5");
    }
}
```

**Key points:**
- Test methods can be `void` and `package-private` (no `public` required in JUnit 5)
- Use `assertEquals(expected, actual)` — expected first, actual second
- The third parameter (message) is optional but helpful when a test fails

#### @BeforeEach and @AfterEach — Setup and Cleanup

When you have many tests that need the same setup (e.g., a fresh `Calculator` for each test), use `@BeforeEach` so you don't repeat yourself. Use `@AfterEach` for cleanup (closing files, resetting state).

```java
import org.junit.jupiter.api.*;

class CalculatorTest {

    // This field will be shared across tests, but reset in setUp
    private Calculator calculator;

    @BeforeEach
    void setUp() {
        // Runs BEFORE each @Test method — gives us a fresh calculator every time
        calculator = new Calculator();
    }

    @AfterEach
    void tearDown() {
        // Runs AFTER each @Test method — use for cleanup (e.g., closing connections)
        calculator = null; // Not always needed for simple objects
    }

    @Test
    void shouldAddTwoNumbers() {
        int result = calculator.add(2, 3);
        assertEquals(5, result);
    }

    @Test
    void shouldSubtractTwoNumbers() {
        int result = calculator.subtract(10, 3);
        assertEquals(7, result);
    }
}
```

**Why @BeforeEach and not @BeforeAll?** `@BeforeEach` runs before *every* test, so each test gets a clean object. That keeps tests **Independent** (the I in FIRST). `@BeforeAll` runs once for the whole class—use it only for expensive setup (e.g., loading a config file) that doesn't change between tests.

---

### Test Structure: Arrange-Act-Assert and Given-When-Then

**Arrange-Act-Assert (AAA)** and **Given-When-Then** are the same idea with different names. Both enforce a clear, three-part structure.

| AAA | Given-When-Then | Purpose |
|-----|-----------------|---------|
| Arrange | Given | Set up the initial state and inputs |
| Act | When | Execute the behavior being tested |
| Assert | Then | Verify the outcome |

**Example with comments:**

```java
@Test
void shouldApplyDiscountWhenOrderTotalExceeds100() {
    // ARRANGE (Given): We have an order with total 150 and a discount service
    Order order = new Order();
    order.addItem(new Item("Book", 150.0));
    DiscountService discountService = new DiscountService();

    // ACT (When): We ask for the discount
    double discount = discountService.calculateDiscount(order);

    // ASSERT (Then): We expect 10% discount
    assertEquals(15.0, discount, 0.01);
}
```

---

### Assertions Reference

Assertions are the "check" part of your test. JUnit 5 provides several:

| Assertion | Purpose | Example |
|-----------|---------|---------|
| `assertEquals(expected, actual)` | Values must be equal | `assertEquals(5, result)` |
| `assertNotEquals(unexpected, actual)` | Values must differ | `assertNotEquals(0, result)` |
| `assertTrue(condition)` | Condition must be true | `assertTrue(user.isActive())` |
| `assertFalse(condition)` | Condition must be false | `assertFalse(order.isEmpty())` |
| `assertNull(value)` | Value must be null | `assertNull(optional.getValue())` |
| `assertNotNull(value)` | Value must not be null | `assertNotNull(user.getId())` |
| `assertThrows(ExceptionClass, executable)` | Code must throw this exception | `assertThrows(IllegalArgumentException.class, () -> service.process(null))` |
| `assertAll(assertions...)` | Run all assertions; report all failures | `assertAll(() -> assertEquals("John", user.getName()), () -> assertTrue(user.isActive()))` |

**Floating-point comparison:** Use a delta for doubles: `assertEquals(10.5, result, 0.01)` — allows small rounding errors.

**assertAll** — Use when testing one behavior with multiple aspects. If the first assertion fails, later ones still run, so you see the full picture:

```java
@Test
void shouldCreateUserWithAllFieldsCorrect() {
    User user = userService.createUser("John", "Doe", "john@example.com");

    assertAll(
        () -> assertNotNull(user.getId(), "ID should be assigned"),
        () -> assertEquals("John", user.getFirstName()),
        () -> assertEquals("Doe", user.getLastName()),
        () -> assertEquals("john@example.com", user.getEmail())
    );
}
```

---

### Test Naming Conventions

**Pattern:** `should[ExpectedBehavior]When[Condition]`

| Good Name | Bad Name |
|-----------|----------|
| `shouldReturnEmptyListWhenNoUsersExist()` | `testGetUsers()` |
| `shouldThrowExceptionWhenEmailIsInvalid()` | `test1()` |
| `shouldCalculateTotalWithMultipleItems()` | `testOrder()` |

---

### Mockito Basics

**Mockito** is the standard mocking library for Java. It lets you create "stunt doubles" (mocks) and control their behavior.

#### Why Mock?

Your `UserService` might depend on `UserRepository` (which talks to a database). In a unit test, we don't want to hit a real database—it's slow and requires setup. So we **mock** the repository: we create a fake one that returns whatever we tell it to.

#### Creating a Mock

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // Enables Mockito for this test class
class UserServiceTest {

    @Mock
    private UserRepository userRepository;  // Mockito creates a fake UserRepository

    @Test
    void shouldFindUserById() {
        // ...
    }
}
```

#### Stubbing: Defining What the Mock Returns

**Stubbing** = telling the mock: "When this method is called with these arguments, return this value."

```java
@Test
void shouldFindUserById() {
    // Arrange: Tell the mock — "When findById(1L) is called, return this user"
    User expectedUser = new User(1L, "John");
    when(userRepository.findById(1L)).thenReturn(Optional.of(expectedUser));

    // Act: Our service calls userRepository.findById(1L) internally
    UserService service = new UserService(userRepository);
    User result = service.findUserById(1L);

    // Assert: We got back the user we told the mock to return
    assertEquals("John", result.getName());
}
```

**Other stubbing patterns:**
```java
// Throw an exception
when(userRepository.findById(999L)).thenThrow(new UserNotFoundException("Not found"));

// Return different values on successive calls
when(repository.next()).thenReturn(1).thenReturn(2).thenReturn(3);

// Match any argument of a given type
when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
    User u = invocation.getArgument(0);
    u.setId(1L);
    return u;
});
```

#### Verification: Did the Mock Get Called?

Sometimes the result isn't enough—you want to check that a method was *called* (e.g., that `save` was invoked). That's **verification**:

```java
@Test
void shouldSaveUserWhenCreating() {
    UserService service = new UserService(userRepository);
    User newUser = new User("John", "john@example.com");

    when(userRepository.save(any(User.class))).thenAnswer(inv -> {
        User u = inv.getArgument(0);
        u.setId(1L);
        return u;
    });

    User created = service.createUser(newUser);

    // Verify: save was called exactly once with a User
    verify(userRepository).save(any(User.class));

    // Verify: save was never called with null
    verify(userRepository, never()).save(null);

    // Verify: save was called at least once
    verify(userRepository, atLeastOnce()).save(any(User.class));
}
```

#### Common Mockito Methods

| Method | Purpose |
|--------|---------|
| `when(mock.method(args)).thenReturn(value)` | Define return value |
| `when(mock.method(args)).thenThrow(exception)` | Define thrown exception |
| `verify(mock).method(args)` | Verify method was called |
| `verify(mock, times(n)).method(args)` | Verify called exactly n times |
| `verify(mock, never()).method(args)` | Verify never called |
| `any()`, `anyLong()`, `anyString()` | Argument matchers (any value of that type) |

---

### Complete JUnit + Mockito Example

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        // Create the real service, but inject our mocks
        userService = new UserService(userRepository, emailService);
    }

    @Test
    void shouldCreateUserWhenEmailDoesNotExist() {
        // Arrange: Email doesn't exist yet
        User newUser = new User("John", "john@example.com");
        User savedUser = new User(1L, "John", "john@example.com");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act: Create the user
        User result = userService.createUser(newUser);

        // Assert: User was created with ID
        assertNotNull(result.getId());
        assertEquals("John", result.getName());

        // Verify: Repository and email service were used correctly
        verify(userRepository).findByEmail("john@example.com");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(savedUser);
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Arrange: Email already exists
        User newUser = new User("John", "john@example.com");
        User existingUser = new User(1L, "Jane", "john@example.com");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));

        // Act & Assert: Should throw
        assertThrows(EmailAlreadyExistsException.class, () -> {
            userService.createUser(newUser);
        });

        // Verify: Save was never called (we rejected early)
        verify(userRepository, never()).save(any());
    }
}
```

---

### Test Doubles: Stub vs. Mock vs. Spy

| Type | Returns values? | Verifies calls? | Real behavior? |
|------|-----------------|-----------------|----------------|
| **Stub** | Yes (predefined) | No | No |
| **Mock** | Yes (optional) | Yes | No |
| **Spy** | Yes (selective) | Yes | Yes (selective) |

- **Stub** — You only care about controlling what gets returned. "When they ask for user 1, say John."
- **Mock** — You also care that a method was called. "Did you actually call `save`?"
- **Spy** — A real object where you override specific methods. Use sparingly.

---

### TDD Basics (Red-Green-Refactor)

```java
// 1. RED: Write the failing test first
@Test
void shouldAddTwoNumbers() {
    Calculator calc = new Calculator();
    assertEquals(5, calc.add(2, 3));  // Fails — add() doesn't exist
}

// 2. GREEN: Minimal code to pass
public int add(int a, int b) {
    return 5;  // Cheating, but test passes
}

// 3. REFACTOR: Make it correct
public int add(int a, int b) {
    return a + b;
}
```

---

## Layer 3 — Advanced Engineering Depth

*Deep internals, performance considerations, and edge cases.*

---

### One Assertion Per Test vs. assertAll

**One assertion per test** — Keeps each test focused. When it fails, you know exactly what broke. Downsides: more test methods, sometimes duplicated setup.

**assertAll for related checks** — When one behavior has multiple observable effects (e.g., "user is created" means ID is set, name is set, email is set), `assertAll` runs all checks and reports every failure. Acceptable and often preferable.

**Guideline:** Prefer one logical behavior per test. Use `assertAll` when that behavior has several aspects to verify.

---

### Test Behavior, Not Implementation

**Bad:** Testing that `userRepository.save()` was called. That's *how* you did it.

**Good:** Testing that after `createUser("John")`, you can find a user named "John." That's *what* the system does.

Implementation can change (e.g., switch from repository to a queue). Behavior tests still pass. Implementation-coupled tests break unnecessarily.

---

### Avoiding Private Method Testing

Don't test private methods directly. Test them indirectly through public methods. If a private method is complex enough to need its own tests, consider extracting it to a separate class or making it package-private for testing.

---

### Edge Cases and Boundaries

| Category | Examples |
|----------|----------|
| Happy path | Normal inputs, expected flow |
| Empty/null | `null`, empty list, empty string |
| Boundaries | First/last element, zero, max value |
| Error conditions | Invalid input, not found, server error |

Always consider: What if the input is null? Empty? At the boundary? Negative?

---

### Code Coverage: What It Measures

- **Line coverage** — % of lines executed
- **Branch coverage** — % of branches (if/else paths) executed
- **Method coverage** — % of methods called

**100% coverage is not a goal.** Focus coverage on business logic, error handling, and complex algorithms. Ignore trivial getters/setters and generated code.

**JaCoCo** — Common tool for Java. Add the Maven plugin, run `mvn test`, then open `target/site/jacoco/index.html`.

---

### Test Anti-Patterns

| Anti-Pattern | Problem | Fix |
|--------------|---------|-----|
| Slow tests | Real DB, network, files | Mock external deps |
| Flaky tests | Time, order, shared state | Deterministic data, fresh state per test |
| Test interdependence | Test B relies on Test A's data | Each test sets up its own data |
| Over-mocking | Mocking everything | Mock only external boundaries |
| Testing implementation | Checking internal calls | Assert on observable behavior |

---

### Flaky Tests: Causes and Fixes

1. **Time-dependent** — Use fixed clocks or accept a range, not exact `now()`
2. **Non-deterministic order** — Don't assert on `get(0)`; use `contains` or sort first
3. **Shared mutable state** — Use `@BeforeEach` to reset state
4. **Race conditions** — Use `CompletableFuture.get(timeout)` instead of `Thread.sleep`

---

## Layer 4 — Interview Mastery

*Concise Q&A for interview preparation.*

---

### Q1: What is the Test Pyramid?

**Answer:** A model for organizing tests: many fast unit tests at the base, fewer integration tests in the middle, and very few slow end-to-end tests at the top.

- **Base:** Unit tests — fast, cheap, test one unit in isolation
- **Middle:** Integration tests — test components working together
- **Top:** E2E tests — test full user flows

Most tests should be unit tests for speed and maintainability.

---

### Q2: Unit Test vs. Integration Test — When to Use Each?

**Unit test:** Tests a single class/method in isolation with mocked dependencies. Fast. Use for business logic.

**Integration test:** Tests multiple components together (e.g., service + repository + DB). Slower. Use for critical paths and component interaction.

**Rule of thumb:** ~80% unit tests, ~20% integration tests.

---

### Q3: What is Mocking? When to Use It?

**Answer:** Mocking replaces real dependencies with fake objects you control. Use it when the real dependency is slow (database), unpredictable (external API), or risky (payment gateway).

Mock to isolate the unit under test and keep tests fast and deterministic.

---

### Q4: Mock vs. Stub vs. Spy?

**Stub** — Returns predefined values. Does not verify calls.

**Mock** — Can return values and verifies that methods were called.

**Spy** — Wraps a real object; some methods are real, some are stubbed/mocked.

---

### Q5: How Do You Test Private Methods?

**Answer:** You don't. Test them through public methods. If a private method is complex enough to need its own tests, extract it (e.g., to another class or package-private helper).

---

### Q6: What is Code Coverage? Is 100% Good?

**Answer:** Coverage measures how much of your code is executed by tests (lines, branches, methods).

100% is not the goal. Focus on meaningful coverage of business logic and edge cases. Trivial code (getters, DTOs) doesn't need tests. Aim for ~70–80% overall with higher coverage on critical paths.

---

### Q7: What Makes a Test Flaky?

**Answer:** A flaky test passes and fails inconsistently with the same code. Common causes: time dependence, non-deterministic order, shared mutable state, and race conditions. Fix by making tests deterministic and independent.

---

### Q8: Explain TDD and Its Benefits

**Answer:** Test-Driven Development means writing tests before implementation.

**Red-Green-Refactor:**
1. Red — Write a failing test
2. Green — Write minimal code to pass
3. Refactor — Improve code while keeping tests green

**Benefits:** Better design (testable code), confidence when refactoring, living documentation, earlier bug detection.

---

### Q9: How Do You Test Code That Calls External APIs?

**Answer:** Mock the HTTP client or API client in unit tests. For integration tests, use WireMock or similar to stub the external service. Never call real external APIs in unit tests—they are slow, unreliable, and may incur cost.

---

### Q10: What Are Test Anti-Patterns?

**Answer:** Common anti-patterns: testing implementation instead of behavior, slow tests (real I/O), flaky tests (time/order/state), test interdependence, over-mocking, testing private methods directly, unclear test names, and "mystery guests" (unclear test data).

---

## Summary

| Concept | Key Takeaway |
|---------|--------------|
| **Why test** | Catch bugs early, refactor safely, work like a professional |
| **Unit test** | Test one unit in isolation; fast and focused |
| **Test pyramid** | Many unit tests, fewer integration, few E2E |
| **AAA / Given-When-Then** | Arrange/given → Act/when → Assert/then |
| **JUnit 5** | `@Test`, `@BeforeEach`, `@AfterEach`, assertions |
| **Mockito** | `when().thenReturn()`, `verify()` — control and verify dependencies |
| **Naming** | `should[Behavior]When[Condition]` |
| **Coverage** | Aim for meaningful coverage, not 100% |
| **TDD** | Red → Green → Refactor |
| **FIRST** | Fast, Independent, Repeatable, Self-validating, Timely |

---

[← Back to Index](00-README.md) | [Previous: JSON & Serialization](09-Prerequisites-JSON.md) | [Next: What is Spring →](11-Spring-Introduction.md)
