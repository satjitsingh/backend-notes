# Chapter 35: Testing Spring Applications

[← Back to Index](00-README.md) | [Previous: Event-Driven](34-Event-Driven.md) | [Next: Performance Internals →](36-Performance-Internals.md)

---

## Why This Chapter Matters

Here's a harsh truth: **code without tests is broken code that hasn't been caught yet.** Every professional development team requires tests. Every CI/CD pipeline runs tests. Every code review asks "where are the tests?"

If you can't write tests, you can't work on a professional team. Period.

**Why beginners struggle with testing:**
- "My code works when I run it, why do I need tests?"
- "Testing takes too long, I'd rather write features"
- "I don't know what to test or how to start"

**The reality:** The developer who writes good tests ships faster, breaks less, and gets promoted sooner than the one who skips tests and spends weekends debugging production fires.

**Without proper testing:**
- You deploy bugs to production (and discover them at 3 AM)
- Refactoring becomes terrifying ("if I change this, will something break?")
- New team members are afraid to touch old code
- Every deployment is a gamble
- You waste days debugging issues that a 10-line test would have caught

**With proper testing:**
- You catch bugs in 5 seconds (test run) instead of 5 hours (production debugging)
- You refactor fearlessly — tests tell you immediately if you broke something
- Tests serve as living documentation ("how is this feature supposed to work?")
- CI/CD deploys automatically because tests prove the code works
- You sleep well at night knowing production is solid

---

## Layer 1 — Intuition Builder

### Part 1: What Is Testing? — Like Checking Your Homework Before You Hand It In

**Imagine you're a student.** You just finished a math problem. Smart students **check their answer** before handing in their homework. They plug the answer back in: "Does 5 + 3 really equal 8? Let me count: 5, 6, 7, 8. Yes!"

That's testing. You're checking that your answer works, before someone else (like your teacher or a customer) finds out it's wrong.

**Another way to think about it:** Testing is like a **safety net** under a tightrope. You hope you don't fall, but if you do, the net catches you. Tests catch your mistakes before customers see them.

**In code, it looks like this:**

```java
// Your code (the "math problem"):
public int addNumbers(int a, int b) {
    return a + b;
}

// Your test (the "checking step"):
@Test
void checkThatAdditionWorks() {
    int answer = addNumbers(5, 3);
    assertThat(answer).isEqualTo(8);  // ✅ Correct!
}
```

Without the test, you might accidentally change the code to `a - b` someday, and nobody would notice until a customer gets a wrong total on their bill. The test catches it in one second.

---

### Part 2: The LEGO Analogy — Why We Have Different Kinds of Tests

**Think of building with LEGO.** There are three ways to check if everything works:

**1. Test ONE piece at a time (Unit Test)**  
You pick up a single red brick. You check: "Is it the right shape? Does it have the right bumps?" You're not building anything yet — you're just making sure each piece is good.  
- **Fast**: You check hundreds of pieces in minutes.  
- **Simple**: One piece, one check.  
- **Analogy in code**: Test one small piece of your program — like a calculator that adds numbers — without needing a whole app or database.

**2. Test ONE section of the model (Slice Test)**  
You've built the wheels section of a LEGO car. You spin the wheels: "Do they turn together? Are they attached correctly?" You're not testing the whole car, just the wheel section.  
- **Medium speed**: A few sections, a few checks.  
- **Focused**: One part of the bigger picture.  
- **Analogy in code**: Test just the part that handles web requests (like when someone clicks "Add to Cart") — without needing the database or the whole app.

**3. Test the FULL model (Integration Test)**  
The whole LEGO spaceship is built. You pick it up, press the button, and check: "Does the rocket light up? Do all the pieces work together?"  
- **Slower**: You need to assemble everything first.  
- **Most realistic**: You're testing the real thing.  
- **Analogy in code**: Test your whole app — website + database + business logic — as if a real user is using it.

**The rule:** Do LOTS of small tests (unit), some medium tests (slice), and FEW big tests (integration). That's called the **testing pyramid**. It looks like this:

```
        ▲
       / \         Few big tests (slow, expensive)
      /   \        "Test the whole spaceship"
     /─────\
    /       \      Some medium tests (slice)
   /         \     "Test the wheels section"
  /           \
 /             \   Many small tests (fast, cheap)
/_______________\  "Test each LEGO piece"
```

---

### Part 3: Given-When-Then — The Recipe for Any Good Test

Every test answers three questions. Think of it like a recipe card:

| Step | Question | Example (Real Life) | Example (Code) |
|------|----------|---------------------|----------------|
| **GIVEN** | What's the starting situation? | "I have $20 in my piggy bank" | "I have a user named John in the system" |
| **WHEN** | What action happens? | "I take out $5" | "I click 'delete user'" |
| **THEN** | What should the result be? | "I have $15 left" | "John is gone from the system" |

**Why this matters:** If you always follow this pattern, your tests become **easy to read**. Anyone can understand what you're checking. It's like writing instructions so clear that a friend could follow them.

```java
@Test
void piggyBankWithdrawal() {
    // GIVEN: I have $20 in my piggy bank
    PiggyBank bank = new PiggyBank(20);
    
    // WHEN: I take out $5
    bank.takeOut(5);
    
    // THEN: I have $15 left
    assertThat(bank.getMoney()).isEqualTo(15);
}
```

---

### Part 4: What Is a "Mock"? — The Pretend Friend That Helps You Practice

**Real-life analogy: Learning to drive.**

You want to practice what to do when the car runs out of gas. Do you:
- **Option A:** Actually run out of gas on a real highway? (Dangerous! Scary!)  
- **Option B:** Have a friend *pretend* to be the gas gauge and say "we're empty!"? (Safe! You can practice over and over!)

**Option B is mocking.** A **mock** is a pretend version of something. It doesn't do the real job — it just *acts* like it does, so you can test YOUR part without needing the real thing.

**In code:**
- Your code might need to talk to a **database** (stores user names, orders, etc.).
- You want to test your *logic* — "when a user signs up, do we save their name correctly?" — but you don't want to start a real database for every tiny test.
- So you use a **mock database**: a fake that says "when you ask me to save a user, I'll say 'OK, I did it!'" without actually saving anything.
- Your code doesn't know it's fake. It runs the same way. But the test is **fast** and **cheap** — no real database needed.

**The three things you'll do with mocks most often:**
1. **Teach it:** "When someone calls findUser(1), you say 'here is John.'"
2. **Ask it:** "Did anyone call save()? How many times?"
3. **Make it fail:** "When someone calls findUser(999), pretend there's an error."

---

### Part 5: What Makes Spring Testing Special? — The Glue Problem

In a normal app, your pieces connect to each other. Your "UserService" (handles user signups) needs a "UserRepository" (talks to the database). Spring is the **glue** that connects them: it creates these pieces and wires them together.

**The challenge:** When you want to test the UserService, you need *something* for it to talk to. You have two choices:

**Choice 1: Use a FAKE (mock)**  
- Give UserService a pretend UserRepository.  
- No real database. Super fast.  
- This is a **unit test** — testing one piece with fake friends.

**Choice 2: Use the REAL thing**  
- Give UserService a real UserRepository connected to a test database.  
- Slower, but you see how the pieces work together.  
- This is an **integration test**.

Spring gives you **annotations** (special labels in your code) to pick which approach you want. We'll learn those in Layer 2.

---

### Part 6: Quick Reference — Test Types in Spring (Plain English)

| Test Type | What It Tests | Speed | LEGO Analogy |
|-----------|---------------|-------|---------------|
| **Unit Test** | One class, all friends are fakes | Very fast (milliseconds) | Testing one brick |
| **Slice Test** | One section (e.g., web or database only) | Fast–medium | Testing the wheels section |
| **Integration Test** | Multiple pieces + real database | Slower (seconds) | Testing the full spaceship |

**Spring's special labels (we'll explain these properly in Layer 2):**

| Label | What it does (simple) |
|-------|------------------------|
| `@SpringBootTest` | "Start the whole app" (integration) |
| `@WebMvcTest` | "Start only the web part" (slice) |
| `@DataJpaTest` | "Start only the database part" (slice) |
| `@Mock` | "Make a fake version" (unit test) |
| `@MockBean` | "Make a fake and put it in Spring's box" (slice/integration) |
| `@SpyBean` | "Use the real thing, but I can make it lie sometimes" (advanced) |

---

## Layer 2 — Professional Developer

### Unit Testing with Mockito — @Mock and @InjectMocks

**Why unit tests first?** They're the fastest, most reliable, and pinpoint bugs to a single class. You test one class in isolation — all its dependencies are mocks. No Spring context is loaded.

**How it works:**
- `@Mock`: Creates a fake version of a dependency. All methods return empty/default values until you "teach" them.
- `@InjectMocks`: Creates the real class you're testing and automatically injects the mocks into it.
- `@ExtendWith(MockitoExtension.class)`: Tells JUnit 5 to use Mockito for creating and wiring mocks.

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // Fake repository — no real database
    @Mock
    private UserRepository userRepository;

    // Fake email sender — no real emails sent
    @Mock
    private EmailService emailService;

    // The REAL class we're testing — gets the mocks injected automatically
    @InjectMocks
    private UserService userService;

    @Test
    void shouldFindUserById() {
        // GIVEN: Repository returns John when asked for ID 1
        User expectedUser = new User(1L, "John Doe", "john@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(expectedUser));

        // WHEN: Service looks up user 1
        Optional<User> result = userService.findById(1L);

        // THEN: We get John back
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John Doe");
        verify(userRepository).findById(1L);  // Ensure we actually called the repo
    }

    @Test
    void shouldCreateUserAndSendWelcomeEmail() {
        // GIVEN: Save returns a user with an ID
        UserDto userDto = new UserDto("Jane Doe", "jane@example.com");
        User savedUser = new User(1L, "Jane Doe", "jane@example.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // WHEN: We create a new user
        User result = userService.create(userDto);

        // THEN: User is created and welcome email is sent
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Jane Doe");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail("jane@example.com");
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        // GIVEN: Email already in use
        UserDto userDto = new UserDto("John", "existing@example.com");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // WHEN & THEN: Creating user throws exception, save is never called
        assertThatThrownBy(() -> userService.create(userDto))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessage("Email already exists: existing@example.com");

        verify(userRepository, never()).save(any());
    }
}
```

**Test naming:** Use descriptive names that explain the scenario and expected behavior. Common patterns: `should[DoSomething]When[Condition]` or `should[ExpectedResult]`.

---

### Integration Testing with @SpringBootTest

**Why integration tests?** Unit tests use mocks — they don't catch bugs where components miscommunicate. Integration tests load the real Spring context, real beans, and (optionally) a real or in-memory database. They verify that pieces work together.

**Key annotations:**
- `@SpringBootTest`: Loads the full Spring Boot application context.
- `@ActiveProfiles("test")`: Uses the `test` profile (e.g., `application-test.yml`) so you don't touch production config.
- `@Transactional`: Each test runs in a transaction that is rolled back afterward — database stays clean between tests.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional  // Rollback after each test — no leftover data
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateAndFindUser() {
        // GIVEN: A new user DTO
        UserDto userDto = new UserDto("John Doe", "john@example.com");

        // WHEN: We create the user (real service + real repo + real DB)
        User created = userService.create(userDto);

        // THEN: User exists and can be found
        assertThat(created.getId()).isNotNull();
        Optional<User> found = userService.findById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
    }

    @Test
    void shouldUpdateUser() {
        // GIVEN: An existing user in the database
        User user = userRepository.save(new User("John", "john@example.com"));

        // WHEN: We update via the service
        UserDto updateDto = new UserDto("John Updated", "john@example.com");
        User updated = userService.update(user.getId(), updateDto);

        // THEN: Database has the new name
        assertThat(updated.getName()).isEqualTo("John Updated");
        User fromDb = userRepository.findById(user.getId()).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("John Updated");
    }
}
```

---

### Slice Tests — @WebMvcTest, @DataJpaTest, @JsonTest

**Why slice tests?** `@SpringBootTest` loads everything — slow. Slice tests load only one "slice" of your app (web, data, or JSON). Faster and focused.

| Slice | Annotation | What Loads | What You Mock |
|-------|-------------|------------|---------------|
| Web (controllers) | `@WebMvcTest` | Controllers, filters, WebMvc | Services |
| Data (repositories) | `@DataJpaTest` | Repositories, JPA, H2 in-memory DB | Nothing (uses real DB) |
| JSON (serialization) | `@JsonTest` | Jackson, JSON marshalling | Nothing |

---

### Web Layer: @WebMvcTest and MockMvc

**What MockMvc does:** Simulates HTTP requests to your controllers without starting a real server. You send GET/POST and assert on status, headers, and JSON body.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)  // Only web layer — no services, no DB
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Simulates HTTP requests

    @MockBean  // Replaces real UserService in Spring context with a mock
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;  // Converts objects to JSON

    @Test
    void shouldGetUser_WhenUserExists() throws Exception {
        // GIVEN: Service returns John for ID 1
        User user = new User(1L, "John Doe", "john@example.com");
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        // WHEN & THEN: GET /api/users/1 returns 200 and correct JSON
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void shouldCreateUser_WhenValidPayload() throws Exception {
        // GIVEN: Valid user DTO and service returns created user
        UserDto userDto = new UserDto("Jane Doe", "jane@example.com");
        User createdUser = new User(1L, "Jane Doe", "jane@example.com");
        when(userService.create(any(UserDto.class))).thenReturn(createdUser);

        // WHEN & THEN: POST creates user, returns 201
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    void shouldReturn404_WhenUserNotFound() throws Exception {
        // GIVEN: Service returns empty for ID 999
        when(userService.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN: GET returns 404
        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400_WhenValidationFails() throws Exception {
        // GIVEN: Invalid DTO (empty name/email)
        UserDto invalidDto = new UserDto("", "");

        // WHEN & THEN: POST returns 400 Bad Request
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
            .andExpect(status().isBadRequest());
    }
}
```

---

### Data Layer: @DataJpaTest

**What it does:** Loads only JPA repositories and entities, uses an in-memory H2 database by default, provides `TestEntityManager` for direct DB setup.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest  // Only data layer — H2 in-memory, auto-rollback
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;  // Direct DB access for setup (faster than repo)

    @Test
    void shouldFindByEmail_WhenUserExists() {
        // GIVEN: User in database
        User user = new User("John Doe", "john@example.com");
        entityManager.persistAndFlush(user);

        // WHEN: We search by email
        Optional<User> found = userRepository.findByEmail("john@example.com");

        // THEN: We find John
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
    }

    @Test
    void shouldFindActiveUsers_WhenMixedActiveAndInactive() {
        // GIVEN: One active, one inactive user
        User activeUser = new User("Active", "active@example.com");
        activeUser.setActive(true);
        User inactiveUser = new User("Inactive", "inactive@example.com");
        inactiveUser.setActive(false);
        entityManager.persist(activeUser);
        entityManager.persist(inactiveUser);
        entityManager.flush();

        // WHEN: Query for active users only
        List<User> activeUsers = userRepository.findByActiveTrue();

        // THEN: Only active user returned
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getName()).isEqualTo("Active");
    }

    @Test
    void shouldReturnEmpty_WhenEmailDoesNotExist() {
        // WHEN: Search for non-existent email
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // THEN: Empty result
        assertThat(found).isEmpty();
    }
}
```

---

### JSON Layer: @JsonTest

**What it does:** Tests that your objects serialize to JSON and deserialize from JSON correctly — without loading the rest of the app.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class UserDtoJsonTest {

    @Autowired
    private JacksonTester<UserDto> json;

    @Test
    void shouldSerializeUserDto_ToCorrectJson() throws Exception {
        // GIVEN: A user DTO
        UserDto userDto = new UserDto("John Doe", "john@example.com");

        // WHEN: We convert to JSON
        var jsonContent = json.write(userDto);

        // THEN: JSON has expected fields and values
        assertThat(jsonContent).extractingJsonPathStringValue("$.name").isEqualTo("John Doe");
        assertThat(jsonContent).extractingJsonPathStringValue("$.email").isEqualTo("john@example.com");
    }

    @Test
    void shouldDeserializeUserDto_FromJson() throws Exception {
        // GIVEN: A JSON string
        String jsonString = """
            {
                "name": "John Doe",
                "email": "john@example.com"
            }
            """;

        // WHEN: We parse it
        UserDto userDto = json.parse(jsonString).getObject();

        // THEN: DTO has correct values
        assertThat(userDto.getName()).isEqualTo("John Doe");
        assertThat(userDto.getEmail()).isEqualTo("john@example.com");
    }
}
```

---

### @MockBean vs @SpyBean

| Aspect | @MockBean | @SpyBean |
|--------|-----------|----------|
| Behavior | Fully fake — all methods return defaults unless stubbed | Real implementation; you can override specific methods |
| Use case | When you don't need real behavior | When you want real behavior but need to stub some methods |
| Default | Methods return null, 0, empty, etc. | Methods execute real code |

```java
@SpringBootTest
class UserServiceBeanTest {

    // MockBean: Complete fake — sendEmail() does nothing by default
    @MockBean
    private EmailService emailService;

    // SpyBean: Real repository, but we can stub count() to return 100
    @SpyBean
    private UserRepository userRepository;

    @Test
    void testWithMockBean() {
        // Must stub — otherwise returns null
        when(emailService.sendEmail(anyString())).thenReturn(true);
    }

    @Test
    void testWithSpyBean() {
        // Real save works — data goes to DB
        User user = userRepository.save(new User("John"));

        // But we can override count() for this test
        when(userRepository.count()).thenReturn(100L);
    }
}
```

---

### Testcontainers — Real Database in Tests

**Why?** H2 is fast but behaves differently from PostgreSQL/MySQL. Testcontainers runs a real database in a Docker container — same SQL, same constraints, same quirks.

```java
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserServiceRealDatabaseTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    // Injects DB URL/credentials into Spring before context starts
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserService userService;

    @Test
    void shouldWorkWithRealPostgreSQL() {
        // Real PostgreSQL — tests DB-specific behavior (JSONB, arrays, etc.)
        UserDto userDto = new UserDto("John", "john@example.com");
        User created = userService.create(userDto);
        assertThat(created.getId()).isNotNull();
    }
}
```

---

### Security Testing — @WithMockUser

**Why?** Controllers may require authentication or roles. You need to test as different users (admin, user, anonymous).

```java
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;

@WebMvcTest(UserController.class)
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowDelete_WhenUserIsAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403_WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails("admin@example.com")  // Loads real user from UserDetailsService
    void shouldAllowAccess_WhenUserDetailsLoaded() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());
    }
}
```

---

### Async Testing with Awaitility

**Problem:** Async code finishes "later." A plain assertion right after calling an async method may run before it completes.

**Solution:** Awaitility waits (with a timeout) until a condition is met — no brittle `Thread.sleep()`.

```java
import org.awaitility.Awaitility;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AsyncServiceTest {

    @Autowired
    private AsyncService asyncService;

    @Test
    void shouldProcessAsync_WithinTimeout() {
        // GIVEN: Async operation starts
        CompletableFuture<String> future = asyncService.processAsync("data");

        // WHEN & THEN: Wait up to 5 seconds for completion, then assert
        await().atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(future.isDone()).isTrue();
                assertThat(future.get()).isEqualTo("processed: data");
            });
    }
}
```

---

### Test Configuration — @TestConfiguration and @ActiveProfiles

**@TestConfiguration:** Define beans that exist only in tests (e.g., a no-op email service).

```java
@SpringBootTest
class UserServiceWithTestConfigTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public EmailService emailService() {
            return new MockEmailService();  // No real emails in tests
        }
    }

    @Autowired
    private UserService userService;

    @Test
    void shouldWorkWithMockEmailService() {
        // Uses MockEmailService instead of real EmailService
    }
}
```

**@ActiveProfiles:** Use `application-test.yml` so tests never touch production config.

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  mail:
    host: localhost
    port: 1025  # e.g. MailHog
```

```java
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {
    // Uses application-test.yml
}
```

---

### @Sql — Loading Test Data from Scripts

**When?** Complex setups (many tables, specific IDs) are easier in SQL than in Java.

```java
@SpringBootTest
@Sql(scripts = "/test-data/users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/test-data/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserServiceWithSqlTest {
    // users.sql runs before each test; cleanup.sql runs after
}
```

**test-data/users.sql:**
```sql
INSERT INTO users (id, name, email) VALUES
(1, 'John Doe', 'john@example.com'),
(2, 'Jane Smith', 'jane@example.com');
```

---

### @DirtiesContext — When to Use (Sparingly)

**What it does:** Tells Spring to reload the application context (e.g., after each test or class). Use when a test modifies context, configuration, or static state that would affect other tests.

**Downside:** Very slow — context startup is expensive.

```java
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceDirtyContextTest {
    // Context reloaded after each test — only when absolutely necessary
}
```

**Best practice:** Prefer `@Transactional` and isolated test data. Use `@DirtiesContext` only when you cannot avoid mutating shared state.

---

## Layer 3 — Advanced Engineering Depth

### Testing Pyramid for Spring Boot — Ratios and Rationale

| Layer | % of Tests | Speed | What to Test |
|-------|------------|-------|--------------|
| Unit | ~70% | &lt;10ms each | Business logic, calculations, edge cases |
| Slice | ~20% | 100ms–1s | Controllers, repositories, JSON |
| Integration | ~10% | 1–10s | Component wiring, transactions, DB behavior |

**Anti-pattern:** Inverted pyramid (many `@SpringBootTest`, few unit tests) → slow, flaky, hard-to-maintain suites.

---

### @Transactional Semantics in Tests

- By default, `@SpringBootTest` + `@Transactional` runs each test in a transaction that rolls back.
- `@Commit` disables rollback — use only when you need to inspect DB state after the test (e.g., debugging).
- `TestTransaction` (Spring Test) lets you explicitly start/end transactions inside a test.

---

### Parallel Test Execution — Risks and When to Use

```properties
# junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
```

**Risks:**
- Shared database (H2 file-mode, static data)
- Static variables
- Port conflicts (embedded servers)
- File system contention

**Recommendation:** Enable parallel for unit tests; keep integration tests sequential or use isolated DBs (e.g., Testcontainers with unique DB names).

---

### Advanced MockMvc — File Upload, Headers, Exception Handling

```java
@Test
void shouldUploadFile() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "test.txt", "text/plain", "file content".getBytes()
    );
    mockMvc.perform(multipart("/api/files/upload").file(file))
        .andExpect(status().isOk());
}

@Test
void shouldHandleCustomHeaders() throws Exception {
    mockMvc.perform(get("/api/users")
            .header("X-Request-ID", "12345")
            .header("Authorization", "Bearer token"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Response-Time", notNullValue()));
}

@Test
void shouldHandleGlobalException() throws Exception {
    when(userService.findById(1L)).thenThrow(new UserNotFoundException("User not found"));
    mockMvc.perform(get("/api/users/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("User not found"));
}
```

---

### @Timeout for Test Performance

```java
@Test
@Timeout(value = 1, unit = TimeUnit.SECONDS)
void shouldCompleteQuickly() {
    userService.processData();
}
```

If the test exceeds 1 second, it fails — helps catch performance regressions.

---

### Testcontainers Reuse and Optimization

- Use `@Container` with `static` for class-level reuse (one container per test class).
- For modular tests, consider `@Testcontainers(disabledWithoutDocker = true)` to skip when Docker is unavailable.
- Reuse containers across classes with Testcontainers' reuse feature (configure in `~/.testcontainers.properties`) for faster CI.

---

## Layer 4 — Interview Mastery

### Q1: What's the difference between unit tests and integration tests in Spring?

**Unit tests:** Test one class in isolation; all dependencies are mocks. No Spring context. Very fast (milliseconds). Use `@ExtendWith(MockitoExtension.class)` with `@Mock` and `@InjectMocks`.

**Integration tests:** Test multiple components together. Spring context loaded (`@SpringBootTest`). Real or test database. Slower (seconds). Verify wiring, transactions, and DB behavior.

**When to use:** Unit tests for business logic and edge cases; integration tests for component interactions and DB-specific behavior.

---

### Q2: What is @WebMvcTest and when should you use it?

**Answer:** `@WebMvcTest` is a slice test that loads only the web layer (controllers, filters, interceptors). It does not load services or the database. Use `MockMvc` to simulate HTTP and `@MockBean` for service dependencies. Use it when testing controller logic, request/response mapping, validation, and exception handling — faster than `@SpringBootTest`.

---

### Q3: How do you test with a real database in Spring Boot?

**Three options:**
1. **H2 in-memory** — `@DataJpaTest` or `@SpringBootTest` with H2. Fast, but not identical to production DB.
2. **Testcontainers** — Start PostgreSQL/MySQL in Docker; use `@DynamicPropertySource` to configure `spring.datasource.*`. Matches production DB behavior.
3. **Test profile** — `application-test.yml` pointing to a dedicated test DB. Good for CI when Docker isn't ideal.

---

### Q4: What's the difference between @Mock and @MockBean?

| | @Mock | @MockBean |
|---|-------|-----------|
| Framework | Mockito | Spring Boot Test |
| Spring context | None | Yes |
| Bean replacement | No | Replaces Spring bean |
| Typical use | Unit tests | Slice/integration tests |

`@Mock` is a plain Mockito mock. `@MockBean` is Spring-aware and replaces a bean in the context.

---

### Q5: How do you test Spring Security?

Use Spring Security Test:
- `@WithMockUser(roles = "ADMIN")` — simulated user with roles.
- `@WithUserDetails("admin@example.com")` — load real user from `UserDetailsService`.
- `@WithAnonymousUser` — unauthenticated access.

Test both success (authorized) and failure (403/401) paths.

---

### Q6: How do you test async operations?

Use Awaitility instead of `Thread.sleep()`:

```java
await().atMost(5, TimeUnit.SECONDS)
    .untilAsserted(() -> assertThat(future.isDone()).isTrue());
```

Awaitility polls until the condition holds or the timeout is reached — more reliable and readable.

---

### Q7: What is @Transactional in tests and why use it?

`@Transactional` on a test wraps the test method in a transaction that rolls back when the test ends. Keeps the database clean between tests and ensures isolation. Use `@Commit` only when you must inspect DB state after the test (e.g., debugging).

---

### Q8: What's the difference between @MockBean and @SpyBean?

**@MockBean:** Fully mocked — all methods return defaults unless stubbed. Use when you don't need real behavior.

**@SpyBean:** Wraps the real bean; real methods run unless you stub them. Use when you want real behavior but need to override specific methods (e.g., `count()`).

---

### Q9: How do you handle test data setup and cleanup?

1. **@Transactional** — Default for `@SpringBootTest`; auto-rollback after each test.
2. **@Sql** — Run SQL scripts before/after tests for complex data.
3. **@BeforeEach / @AfterEach** — Programmatic setup/cleanup; use sparingly.
4. **TestEntityManager** — In `@DataJpaTest`, use for direct persistence when faster than repository calls.

---

### Q10: What is @DirtiesContext and when should you use it?

`@DirtiesContext` forces Spring to reload the application context (e.g., after each test or class). Use when tests modify context, bean definitions, or static state that would affect other tests. It is expensive — prefer `@Transactional` and isolation instead.

---

## Summary

**Test types:**
- **Unit** — One class, mocked deps, no Spring. Fast. Use for logic.
- **Slice** — One layer (`@WebMvcTest`, `@DataJpaTest`, `@JsonTest`). Medium speed. Use for controllers, repos, JSON.
- **Integration** — Full context (`@SpringBootTest`). Slower. Use for wiring, transactions, real DB.

**Annotations:**
- `@SpringBootTest` — Full context
- `@WebMvcTest` — Web slice
- `@DataJpaTest` — Data slice
- `@JsonTest` — JSON slice
- `@Mock` / `@MockBean` — Mocks
- `@SpyBean` — Partial mock
- `@Transactional` — Auto-rollback
- `@ActiveProfiles("test")` — Test config
- `@Sql` — Script-based test data
- `@DirtiesContext` — Reload context (use sparingly)

**Best practices:**
- Follow the testing pyramid (many unit, some slice, few integration).
- Use Given-When-Then and descriptive test names.
- Prefer `@Transactional` for isolation; use `@Sql` for complex data.
- Use Testcontainers when you need production-like DB behavior.
- Test security with `@WithMockUser` and `@WithUserDetails`.
- Use Awaitility for async tests instead of `Thread.sleep()`.

**Common pitfalls:**
- Overusing `@SpringBootTest` → slow feedback.
- Skipping `@Transactional` → tests affecting each other.
- Testing implementation details → brittle tests.
- Ignoring test performance → slow CI.

**Next steps:**
- Add unit tests for services.
- Add slice tests for controllers and repositories.
- Set up Testcontainers for integration tests.
- Add security tests for protected endpoints.

---

[← Back to Index](00-README.md) | [Previous: Event-Driven](34-Event-Driven.md) | [Next: Performance Internals →](36-Performance-Internals.md)
