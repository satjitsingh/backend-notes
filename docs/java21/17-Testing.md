# Chapter 17: Testing Strategies

[← Back to Index](00-README.md) | [Previous: Performance](16-Performance.md) | [Next: Security →](18-Security.md)

---

## Why This Chapter Matters

Testing is not just a best practice—it's a fundamental skill that separates professional developers from hobbyists. In interviews, you'll be asked about testing strategies, mocking frameworks, test coverage, and how you approach quality assurance. Understanding testing demonstrates:

- **Professional maturity**: You care about code quality and maintainability
- **Risk awareness**: You understand the cost of bugs in production
- **Design thinking**: Good tests often reveal design flaws before they become problems
- **Collaboration skills**: Tests serve as living documentation and enable safe refactoring

Testing is often the difference between code that works today and code that works reliably for years. This chapter will take you from writing your first test to designing comprehensive testing strategies for enterprise applications.

---

## Layer 1 — Beginner Foundation

### Why Testing Matters

Testing is the practice of verifying that your code behaves as expected. Without tests, you're flying blind—every change could break existing functionality, and you won't know until a user reports it.

**Benefits of Testing:**
- **Confidence**: Make changes without fear of breaking existing features
- **Documentation**: Tests show how code is supposed to be used
- **Design feedback**: Hard-to-test code often indicates design problems
- **Regression prevention**: Catch bugs before they reach production
- **Refactoring safety**: Tests enable safe code improvements

### The Test Pyramid

The test pyramid is a visual guide for balancing different types of tests:

```
        /\
       /  \     E2E Tests (Few, Slow, Expensive)
      /____\
     /      \   Integration Tests (Some, Medium Speed)
    /________\
   /          \  Unit Tests (Many, Fast, Cheap)
  /____________\
```

**Unit Tests** (70-80%):
- Test individual components in isolation
- Fast execution (milliseconds)
- Easy to write and maintain
- Example: Testing a single method's logic

**Integration Tests** (15-20%):
- Test how components work together
- Medium speed (seconds)
- More complex setup
- Example: Testing database interactions

**End-to-End Tests** (5-10%):
- Test complete user workflows
- Slow execution (minutes)
- Expensive to maintain
- Example: Testing a full user registration flow

### JUnit 5 Basics

JUnit 5 is the modern testing framework for Java. It consists of:
- **JUnit Platform**: Foundation for running tests
- **JUnit Jupiter**: Programming and extension model
- **JUnit Vintage**: Support for JUnit 3/4 tests

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>
```

### Writing Your First Test

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    
    @Test
    void testAddition() {
        Calculator calculator = new Calculator();
        int result = calculator.add(2, 3);
        assertEquals(5, result);
    }
}
```

**Key Points:**
- `@Test` marks a method as a test
- Test methods don't need to be `public` (package-private is fine)
- Test methods can return `void` and take no parameters
- Use descriptive test method names

### Assertions

Assertions verify expected behavior. JUnit 5 provides static assertion methods:

```java
import static org.junit.jupiter.api.Assertions.*;

class AssertionExamplesTest {
    
    @Test
    void testBasicAssertions() {
        // Equality
        assertEquals(5, 2 + 3);
        assertEquals(5, 2 + 3, "Custom failure message");
        
        // Boolean conditions
        assertTrue(10 > 5);
        assertFalse(5 > 10);
        
        // Null checks
        String value = null;
        assertNull(value);
        assertNotNull("Hello");
        
        // Object equality (uses equals())
        String str1 = "Hello";
        String str2 = "Hello";
        assertEquals(str1, str2);
        
        // Reference equality
        assertSame(str1, str1);
        assertNotSame(str1, new String("Hello"));
    }
    
    @Test
    void testArrayAssertions() {
        int[] expected = {1, 2, 3};
        int[] actual = {1, 2, 3};
        assertArrayEquals(expected, actual);
    }
    
    @Test
    void testExceptionAssertions() {
        // Old way - using assertThrows
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> {
                throw new IllegalArgumentException("Invalid input");
            }
        );
        assertEquals("Invalid input", exception.getMessage());
    }
    
    @Test
    void testAllAssertions() {
        // All assertions run, even if some fail
        assertAll(
            () -> assertEquals(2, 1 + 1),
            () -> assertTrue(5 > 3),
            () -> assertNotNull("Hello")
        );
    }
}
```

### Test Lifecycle

JUnit 5 provides annotations to control test execution:

```java
import org.junit.jupiter.api.*;

class LifecycleTest {
    
    @BeforeAll
    static void setUpOnce() {
        // Runs once before all tests in the class
        // Must be static
        System.out.println("Setting up test class");
    }
    
    @AfterAll
    static void tearDownOnce() {
        // Runs once after all tests in the class
        // Must be static
        System.out.println("Cleaning up test class");
    }
    
    @BeforeEach
    void setUp() {
        // Runs before each test method
        System.out.println("Setting up for test");
    }
    
    @AfterEach
    void tearDown() {
        // Runs after each test method
        System.out.println("Cleaning up after test");
    }
    
    @Test
    void testOne() {
        System.out.println("Running test one");
    }
    
    @Test
    void testTwo() {
        System.out.println("Running test two");
    }
}
```

**Execution Order:**
1. `@BeforeAll` (once)
2. `@BeforeEach` → `@Test` → `@AfterEach` (for each test)
3. `@AfterAll` (once)

### Complete Example: Testing a Bank Account

```java
class BankAccount {
    private double balance;
    
    public BankAccount(double initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        this.balance = initialBalance;
    }
    
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balance += amount;
    }
    
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (amount > balance) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        balance -= amount;
    }
    
    public double getBalance() {
        return balance;
    }
}

class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

// Test class
class BankAccountTest {
    
    private BankAccount account;
    
    @BeforeEach
    void setUp() {
        account = new BankAccount(100.0);
    }
    
    @Test
    void testInitialBalance() {
        assertEquals(100.0, account.getBalance());
    }
    
    @Test
    void testDeposit() {
        account.deposit(50.0);
        assertEquals(150.0, account.getBalance());
    }
    
    @Test
    void testWithdraw() {
        account.withdraw(30.0);
        assertEquals(70.0, account.getBalance());
    }
    
    @Test
    void testDepositNegativeAmountThrowsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> account.deposit(-10.0));
    }
    
    @Test
    void testWithdrawMoreThanBalanceThrowsException() {
        assertThrows(InsufficientFundsException.class, 
            () -> account.withdraw(200.0));
    }
    
    @Test
    void testMultipleTransactions() {
        account.deposit(50.0);
        account.withdraw(30.0);
        account.deposit(20.0);
        assertEquals(140.0, account.getBalance());
    }
}
```

---

## Layer 2 — Working Developer Level

### Parameterized Tests

Parameterized tests allow running the same test with different inputs:

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class ParameterizedTestExample {
    
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 6, 8, 10})
    void testEvenNumbers(int number) {
        assertTrue(number % 2 == 0);
    }
    
    @ParameterizedTest
    @CsvSource({
        "2, 3, 5",
        "10, 20, 30",
        "0, 0, 0"
    })
    void testAddition(int a, int b, int expected) {
        Calculator calc = new Calculator();
        assertEquals(expected, calc.add(a, b));
    }
    
    @ParameterizedTest
    @MethodSource("provideTestData")
    void testWithMethodSource(int a, int b, int expected) {
        Calculator calc = new Calculator();
        assertEquals(expected, calc.add(a, b));
    }
    
    static Stream<Arguments> provideTestData() {
        return Stream.of(
            Arguments.of(1, 1, 2),
            Arguments.of(5, 5, 10),
            Arguments.of(100, 200, 300)
        );
    }
}
```

### Test Naming Conventions

Good test names communicate intent:

```java
class NamingConventionExamples {
    
    // Pattern: methodName_scenario_expectedBehavior
    @Test
    void withdraw_whenAmountExceedsBalance_throwsInsufficientFundsException() {
        // Test implementation
    }
    
    @Test
    void calculateDiscount_whenCustomerIsPremium_returns20PercentDiscount() {
        // Test implementation
    }
    
    @Test
    void validateEmail_whenEmailIsInvalid_returnsFalse() {
        // Test implementation
    }
    
    // Alternative: Given-When-Then style
    @Test
    void givenValidEmail_whenValidating_thenReturnsTrue() {
        // Test implementation
    }
}
```

### Testing Exceptions

Multiple ways to test exceptions:

```java
class ExceptionTestingExamples {
    
    @Test
    void testExceptionWithAssertThrows() {
        BankAccount account = new BankAccount(100.0);
        
        InsufficientFundsException exception = assertThrows(
            InsufficientFundsException.class,
            () -> account.withdraw(200.0)
        );
        
        assertEquals("Insufficient funds", exception.getMessage());
    }
    
    @Test
    void testExceptionWithAssertThrowsLambda() {
        assertThrows(IllegalArgumentException.class, () -> {
            new BankAccount(-100.0);
        });
    }
    
    @Test
    void testNoExceptionThrown() {
        BankAccount account = new BankAccount(100.0);
        assertDoesNotThrow(() -> account.deposit(50.0));
    }
}
```

### Mocking with Mockito

Mockito is the most popular mocking framework for Java. It allows you to create mock objects that simulate real dependencies.

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
```

**Basic Mocking:**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockitoBasicsTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Test
    void testMockBehavior() {
        // Arrange
        User user = new User("John", "john@example.com");
        when(userRepository.findById(1L)).thenReturn(user);
        
        // Act
        User found = userRepository.findById(1L);
        
        // Assert
        assertEquals("John", found.getName());
        verify(userRepository).findById(1L);
    }
    
    @Test
    void testMockVoidMethods() {
        @Mock
        EmailService emailService;
        
        doNothing().when(emailService).sendEmail(anyString());
        
        emailService.sendEmail("test@example.com");
        
        verify(emailService).sendEmail("test@example.com");
    }
    
    @Test
    void testMockExceptions() {
        when(userRepository.findById(999L))
            .thenThrow(new UserNotFoundException("User not found"));
        
        assertThrows(UserNotFoundException.class, 
            () -> userRepository.findById(999L));
    }
    
    @Test
    void testArgumentMatchers() {
        when(userRepository.findByEmail(anyString())).thenReturn(new User());
        when(userRepository.findById(eq(1L))).thenReturn(new User());
        when(userRepository.findByAge(gt(18))).thenReturn(new ArrayList<>());
        
        userRepository.findByEmail("any@email.com");
        userRepository.findById(1L);
        
        verify(userRepository).findByEmail(anyString());
        verify(userRepository).findById(eq(1L));
    }
}
```

### When to Mock vs When to Use Real Objects

**Mock When:**
- External dependencies (databases, APIs, file systems)
- Slow operations (network calls, complex calculations)
- Unpredictable behavior (random number generators, timestamps)
- Dependencies that don't exist yet
- Isolating the unit under test

**Use Real Objects When:**
- Simple value objects (POJOs, DTOs)
- Pure functions (no side effects)
- Fast, deterministic operations
- Testing integration between closely related components

**Example:**

```java
// Good: Mock external dependency
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private PaymentGateway paymentGateway; // External service - mock it
    
    @Mock
    private EmailService emailService; // External service - mock it
    
    @InjectMocks
    private OrderService orderService; // Real object - test this
    
    @Test
    void testProcessOrder() {
        when(paymentGateway.processPayment(any())).thenReturn(true);
        
        Order order = new Order(100.0); // Simple value object - use real
        orderService.processOrder(order);
        
        verify(paymentGateway).processPayment(any());
        verify(emailService).sendConfirmation(anyString());
    }
}

// Bad: Don't mock simple value objects
class BadExample {
    @Mock
    private Order order; // Don't mock simple POJOs!
    
    @Test
    void badTest() {
        when(order.getTotal()).thenReturn(100.0); // Unnecessary complexity
    }
}
```

### Test Doubles: Mock, Stub, Spy, Fake

Understanding different types of test doubles:

**1. Mock:**
- Records interactions
- Used to verify behavior
- Returns configured values

```java
@Mock
UserRepository userRepository; // Mock - verifies interactions

when(userRepository.findById(1L)).thenReturn(user);
verify(userRepository).findById(1L);
```

**2. Stub:**
- Returns predefined responses
- Doesn't verify interactions
- Focuses on state, not behavior

```java
// Stub - just returns values, no verification
UserRepository stub = mock(UserRepository.class);
when(stub.findById(1L)).thenReturn(user);
// No verify() calls - just using it for return values
```

**3. Spy:**
- Wraps real object
- Can verify real method calls
- Can stub specific methods

```java
List<String> realList = new ArrayList<>();
List<String> spy = spy(realList); // Spy on real object

spy.add("one");
spy.add("two");

verify(spy).add("one");
verify(spy).add("two");
assertEquals(2, spy.size()); // Real method still works
```

**4. Fake:**
- Working implementation with simplified behavior
- In-memory database, simple file system, etc.

```java
class FakeUserRepository implements UserRepository {
    private Map<Long, User> users = new HashMap<>();
    
    @Override
    public User findById(Long id) {
        return users.get(id); // Simple in-memory implementation
    }
    
    @Override
    public void save(User user) {
        users.put(user.getId(), user);
    }
}

// Use fake in tests
UserRepository fakeRepo = new FakeUserRepository();
UserService service = new UserService(fakeRepo);
```

### Code Coverage with JaCoCo

JaCoCo (Java Code Coverage) measures how much of your code is executed by tests.

**Maven Configuration:**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Running Coverage:**
```bash
mvn clean test jacoco:report
```

**Coverage Metrics:**
- **Line Coverage**: Percentage of lines executed
- **Branch Coverage**: Percentage of branches (if/else) executed
- **Method Coverage**: Percentage of methods called
- **Class Coverage**: Percentage of classes instantiated

**Interpreting Coverage:**
- 80%+ is generally good
- 100% doesn't mean perfect tests
- Focus on critical paths, not just numbers
- Low coverage indicates untested code

### TDD Basics (Test-Driven Development)

TDD follows a red-green-refactor cycle:

**1. Red**: Write a failing test
```java
@Test
void testCalculateDiscount() {
    DiscountCalculator calc = new DiscountCalculator();
    double discount = calc.calculate(100.0, "PREMIUM");
    assertEquals(20.0, discount); // Test fails - method doesn't exist
}
```

**2. Green**: Write minimal code to pass
```java
class DiscountCalculator {
    public double calculate(double amount, String customerType) {
        return 20.0; // Minimal implementation
    }
}
```

**3. Refactor**: Improve code while keeping tests green
```java
class DiscountCalculator {
    public double calculate(double amount, String customerType) {
        if ("PREMIUM".equals(customerType)) {
            return amount * 0.20;
        }
        return 0.0;
    }
}
```

**Benefits of TDD:**
- Forces you to think about design before implementation
- Ensures testability from the start
- Creates comprehensive test suite
- Enables confident refactoring

**Example: TDD for a Stack**

```java
// Step 1: Red - Write failing test
@Test
void testPushAndPop() {
    Stack<String> stack = new Stack<>();
    stack.push("first");
    assertEquals("first", stack.pop());
}

// Step 2: Green - Minimal implementation
class Stack<T> {
    private List<T> elements = new ArrayList<>();
    
    public void push(T item) {
        elements.add(item);
    }
    
    public T pop() {
        return elements.remove(elements.size() - 1);
    }
}

// Step 3: Refactor - Add more tests and improve
@Test
void testEmptyStackThrowsException() {
    Stack<String> stack = new Stack<>();
    assertThrows(EmptyStackException.class, () -> stack.pop());
}
```

---

## Layer 3 — Advanced Engineering Depth

### Test Architecture and Organization

**Package Structure:**
```
src/
  main/java/
    com/example/service/
      UserService.java
  test/java/
    com/example/service/
      UserServiceTest.java          # Unit tests
      UserServiceIntegrationTest.java # Integration tests
```

**Test Class Organization:**

```java
class WellOrganizedTest {
    
    // 1. Constants and test data
    private static final String VALID_EMAIL = "test@example.com";
    private static final Long USER_ID = 1L;
    
    // 2. Mocks and dependencies
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    // 3. Object under test
    @InjectMocks
    private UserService userService;
    
    // 4. Setup methods
    @BeforeEach
    void setUp() {
        // Common setup
    }
    
    // 5. Test methods grouped by feature
    @Nested
    @DisplayName("User Registration")
    class UserRegistrationTests {
        @Test
        void testSuccessfulRegistration() { }
        
        @Test
        void testRegistrationWithInvalidEmail() { }
    }
    
    @Nested
    @DisplayName("User Authentication")
    class UserAuthenticationTests {
        @Test
        void testSuccessfulLogin() { }
        
        @Test
        void testLoginWithInvalidCredentials() { }
    }
}
```

**Using @Nested for Organization:**

```java
@Nested
class UserServiceTest {
    
    @Nested
    class WhenCreatingUser {
        @Test
        void shouldCreateUserWithValidData() { }
        
        @Test
        void shouldThrowExceptionWhenEmailExists() { }
    }
    
    @Nested
    class WhenUpdatingUser {
        @Test
        void shouldUpdateExistingUser() { }
        
        @Test
        void shouldThrowExceptionWhenUserNotFound() { }
    }
}
```

### Testing Asynchronous Code

Testing code that uses `CompletableFuture`, reactive streams, or callbacks:

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;

class AsyncCodeTest {
    
    @Test
    void testCompletableFuture() {
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> "Hello")
            .thenApply(s -> s + " World");
        
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> future.isDone());
        
        assertEquals("Hello World", future.join());
    }
    
    @Test
    void testAsyncService() {
        AsyncService service = new AsyncService();
        CompletableFuture<String> result = service.processAsync("input");
        
        // Wait for completion
        String output = result.get(5, TimeUnit.SECONDS);
        
        assertEquals("processed: input", output);
    }
}

// Using Awaitility library for better async testing
// Maven: org.awaitility:awaitility:4.2.0

@Test
void testAsyncOperationCompletes() {
    CompletableFuture<String> future = asyncService.doSomething();
    
    await()
        .atMost(5, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .until(() -> future.isDone());
    
    assertTrue(future.isDone());
    assertEquals("expected", future.join());
}
```

### Testing Concurrent Code

Testing multi-threaded code is challenging but essential:

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class ConcurrentCodeTest {
    
    @Test
    void testThreadSafety() throws InterruptedException {
        Counter counter = new Counter();
        int numberOfThreads = 10;
        int incrementsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(numberOfThreads * incrementsPerThread, counter.getCount());
    }
    
    @Test
    void testConcurrentAccess() throws InterruptedException {
        ThreadSafeList<String> list = new ThreadSafeList<>();
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        list.add("Thread-" + threadId + "-Item-" + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(numberOfThreads * 100, list.size());
    }
}
```

### Integration Testing Strategies

**Database Integration Tests:**

```java
@SpringBootTest
@Transactional
@Rollback
class UserRepositoryIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testSaveAndFind() {
        User user = new User("John", "john@example.com");
        User saved = userRepository.save(user);
        
        Optional<User> found = userRepository.findById(saved.getId());
        
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getName());
    }
    
    @Test
    void testFindByEmail() {
        userRepository.save(new User("John", "john@example.com"));
        
        Optional<User> found = userRepository.findByEmail("john@example.com");
        
        assertTrue(found.isPresent());
    }
}
```

**REST API Integration Tests:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testCreateUser() throws Exception {
        UserDto userDto = new UserDto("John", "john@example.com");
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }
    
    @Test
    void testGetUser() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }
}
```

### Testcontainers for Database Testing

Testcontainers provides lightweight, throwaway instances of databases, message brokers, and more.

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

**Example:**

```java
@Testcontainers
class UserRepositoryTestcontainersTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testWithRealDatabase() {
        User user = new User("John", "john@example.com");
        User saved = userRepository.save(user);
        
        assertNotNull(saved.getId());
        assertEquals("John", saved.getName());
    }
}
```

### Contract Testing Concepts

Contract testing ensures that services communicate correctly:

**Consumer-Driven Contracts (CDC):**
- Consumer defines expected API contract
- Provider must satisfy the contract
- Tools: Pact, Spring Cloud Contract

**Example with Pact:**

```java
// Consumer side
@ExtendWith(PactConsumerTestExt.class)
class UserServiceContractTest {
    
    @Pact(consumer = "UserService", provider = "UserApi")
    public RequestResponsePact getUserPact(PactDslWithProvider builder) {
        return builder
            .given("user exists")
            .uponReceiving("a request for user")
            .path("/api/users/1")
            .method("GET")
            .willRespondWith()
            .status(200)
            .body(new PactDslJsonBody()
                .stringType("name", "John")
                .stringType("email", "john@example.com"))
            .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "getUserPact")
    void testGetUser(MockServer mockServer) {
        UserClient client = new UserClient(mockServer.getUrl());
        User user = client.getUser(1L);
        
        assertEquals("John", user.getName());
    }
}
```

### Mutation Testing

Mutation testing evaluates test quality by introducing small changes (mutations) to code and checking if tests catch them.

**Tool: PIT (Pitest)**

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.0</version>
</plugin>
```

**Mutation Operators:**
- Change `+` to `-`
- Change `>` to `>=`
- Negate conditions
- Remove method calls

**Example:**
```java
// Original code
public int add(int a, int b) {
    return a + b;
}

// Mutation: Changed + to -
public int add(int a, int b) {
    return a - b; // Mutation
}

// Good test catches this:
@Test
void testAdd() {
    assertEquals(5, calculator.add(2, 3)); // Fails with mutation
}
```

### Property-Based Testing

Property-based testing generates random inputs and verifies properties hold:

**Tool: jqwik**

```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.0</version>
    <scope>test</scope>
</dependency>
```

**Example:**

```java
import net.jqwik.api.*;

class PropertyBasedTest {
    
    @Property
    boolean additionIsCommutative(@ForAll int a, @ForAll int b) {
        return add(a, b) == add(b, a);
    }
    
    @Property
    boolean additionIsAssociative(@ForAll int a, @ForAll int b, @ForAll int c) {
        return add(add(a, b), c) == add(a, add(b, c));
    }
    
    @Property
    boolean stringReversal(@ForAll String s) {
        return reverse(reverse(s)).equals(s);
    }
    
    @Property
    @Report(Reporting.GENERATED)
    void listOperations(@ForAll List<Integer> list) {
        Assume.that(!list.isEmpty());
        
        int size = list.size();
        list.add(1);
        
        Assertions.assertThat(list.size()).isEqualTo(size + 1);
    }
}
```

### Testing Anti-Patterns

**1. Testing Implementation Details:**
```java
// Bad: Testing internal state
@Test
void testInternalState() {
    Service service = new Service();
    assertEquals(0, service.getInternalCounter()); // Don't test internals!
}

// Good: Testing behavior
@Test
void testServiceBehavior() {
    Service service = new Service();
    String result = service.process("input");
    assertEquals("expected", result);
}
```

**2. Over-Mocking:**
```java
// Bad: Mocking everything
@Mock User user;
@Mock Order order;
@Mock Product product;
// Too many mocks = design problem

// Good: Mock external dependencies only
@Mock PaymentGateway paymentGateway; // External - mock it
User user = new User("John"); // Simple object - use real
```

**3. Brittle Tests:**
```java
// Bad: Testing exact string formatting
@Test
void testMessage() {
    String message = service.getMessage();
    assertEquals("Hello, John! Today is Monday, January 1, 2024", message);
    // Breaks if date format changes
}

// Good: Testing important parts
@Test
void testMessage() {
    String message = service.getMessage();
    assertTrue(message.contains("Hello, John"));
    assertTrue(message.contains("Today is"));
}
```

**4. Test Interdependence:**
```java
// Bad: Tests depend on each other
private static User sharedUser;

@Test
void testCreateUser() {
    sharedUser = userService.createUser("John");
}

@Test
void testUpdateUser() {
    userService.updateUser(sharedUser.getId(), "Jane"); // Depends on previous test!
}

// Good: Each test is independent
@Test
void testCreateUser() {
    User user = userService.createUser("John");
    assertEquals("John", user.getName());
}

@Test
void testUpdateUser() {
    User user = userService.createUser("John");
    userService.updateUser(user.getId(), "Jane");
    // Test is self-contained
}
```

**5. Ignoring Test Failures:**
```java
// Bad: Ignoring failures
@Test
@Disabled("This test is flaky")
void testSomething() { }

// Good: Fix the test or remove it
```

### Test Performance Optimization

**Parallel Test Execution:**

```java
// junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
```

**Optimizing Slow Tests:**

```java
// Use @DirtiesContext sparingly
@SpringBootTest
// @DirtiesContext // Only if absolutely necessary - it's slow!

// Use @MockBean instead of real beans when possible
@MockBean
private ExternalService externalService; // Faster than real service

// Use in-memory databases
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// Better: Use H2 for tests
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
```

**Test Categorization:**

```java
// Tag slow tests
@Test
@Tag("slow")
void testSlowIntegration() { }

// Run only fast tests during development
// mvn test -Dgroups=!slow

// Run only slow tests in CI
// mvn test -Dgroups=slow
```

---

## Layer 4 — Interview Mastery

### Mock vs Stub vs Spy Differences

**Mock:**
- **Purpose**: Verify interactions (behavior verification)
- **Default behavior**: Returns null/empty/zero
- **Use case**: "Did this method get called with these arguments?"
- **Example**: Verifying that `emailService.send()` was called

```java
@Mock
EmailService emailService;

@Test
void testMock() {
    when(emailService.send(anyString())).thenReturn(true);
    
    orderService.processOrder(order);
    
    verify(emailService).send("order-confirmation@example.com");
    // Mock verifies the interaction happened
}
```

**Stub:**
- **Purpose**: Return predefined values (state verification)
- **Default behavior**: Configured return values
- **Use case**: "When I call this, return that value"
- **Example**: Stubbing a repository to return a user

```java
@Test
void testStub() {
    UserRepository stub = mock(UserRepository.class);
    when(stub.findById(1L)).thenReturn(new User("John"));
    
    UserService service = new UserService(stub);
    User user = service.getUser(1L);
    
    assertEquals("John", user.getName());
    // Stub just provides data, no verification of interactions
}
```

**Spy:**
- **Purpose**: Partial mocking of real objects
- **Default behavior**: Calls real methods unless stubbed
- **Use case**: "Use real implementation, but stub this one method"
- **Example**: Spying on a list, stubbing one method

```java
@Test
void testSpy() {
    List<String> realList = new ArrayList<>();
    List<String> spy = spy(realList);
    
    // Real method works
    spy.add("one");
    assertEquals(1, spy.size());
    
    // Can stub specific methods
    doReturn(100).when(spy).size();
    assertEquals(100, spy.size()); // Stubbed
    
    // Other methods still real
    spy.add("two");
    assertEquals(2, realList.size()); // Real list updated
}
```

**Summary Table:**

| Type | Purpose | Verifies | Default Behavior |
|------|---------|----------|------------------|
| Mock | Behavior | Interactions | Returns null/empty |
| Stub | State | Return values | Configured values |
| Spy | Partial | Real + stubbed | Real methods |

### How to Test Private Methods (And Why You Shouldn't)

**The Problem:**
Private methods are implementation details. Testing them directly creates brittle tests that break when refactoring.

**Why You Shouldn't:**
1. **Violates encapsulation**: Tests shouldn't know about internals
2. **Brittle tests**: Break when implementation changes
3. **Wrong focus**: Should test public behavior, not implementation
4. **Refactoring resistance**: Makes code changes harder

**Bad Approach:**
```java
// Don't do this!
@Test
void testPrivateMethod() throws Exception {
    Service service = new Service();
    Method method = Service.class.getDeclaredMethod("privateHelper", String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(service, "input");
    assertEquals("expected", result);
}
```

**Good Approach - Test Through Public Interface:**
```java
// Private method
class Calculator {
    public int calculate(int a, int b) {
        return add(a, multiply(b, 2)); // Calls private methods
    }
    
    private int add(int a, int b) {
        return a + b;
    }
    
    private int multiply(int a, int b) {
        return a * b;
    }
}

// Test public behavior, not private methods
@Test
void testCalculate() {
    Calculator calc = new Calculator();
    assertEquals(7, calc.calculate(3, 2)); // Tests private methods indirectly
}
```

**When It Might Be Acceptable:**
- Extremely complex private logic that's hard to test through public API
- Legacy code where refactoring isn't possible
- Using package-private instead of private for testability

**Better Solution - Extract to Separate Class:**
```java
// Extract complex logic to testable class
class CalculationEngine {
    public int add(int a, int b) {
        return a + b;
    }
    
    public int multiply(int a, int b) {
        return a * b;
    }
}

class Calculator {
    private CalculationEngine engine = new CalculationEngine();
    
    public int calculate(int a, int b) {
        return engine.add(a, engine.multiply(b, 2));
    }
}

// Now you can test CalculationEngine independently
class CalculationEngineTest {
    @Test
    void testAdd() {
        CalculationEngine engine = new CalculationEngine();
        assertEquals(5, engine.add(2, 3));
    }
}
```

### Testing Strategies for Legacy Code

Legacy code often lacks tests and is hard to test. Here's how to approach it:

**1. Characterization Tests (Golden Master):**
```java
// Capture current behavior, then refactor safely
@Test
void testLegacyMethodBehavior() {
    LegacyService service = new LegacyService();
    
    // Record current output (may be wrong, but it's the baseline)
    String result = service.complexLegacyMethod("input");
    
    // This test documents current behavior
    // Now you can refactor and ensure behavior doesn't change
    assertEquals("some-complex-output", result);
}
```

**2. Strangler Pattern:**
- Wrap legacy code with tests
- Gradually replace with new implementation
- Keep tests passing throughout

```java
class LegacyWrapper {
    private LegacyService legacy = new LegacyService();
    
    public String process(String input) {
        // Add validation, logging, etc.
        return legacy.process(input);
    }
}

// Test the wrapper
@Test
void testLegacyWrapper() {
    LegacyWrapper wrapper = new LegacyWrapper();
    String result = wrapper.process("input");
    assertNotNull(result);
}
```

**3. Dependency Injection for Testability:**
```java
// Before: Hard to test
class LegacyService {
    public void process() {
        Database db = new Database(); // Hard dependency
        db.save(data);
    }
}

// After: Testable
class LegacyService {
    private Database database;
    
    public LegacyService(Database database) {
        this.database = database;
    }
    
    public void process() {
        database.save(data);
    }
}

// Now testable
@Test
void testLegacyService() {
    Database mockDb = mock(Database.class);
    LegacyService service = new LegacyService(mockDb);
    service.process();
    verify(mockDb).save(any());
}
```

**4. Seam Identification:**
Find points where you can insert tests without changing code:
- Extract methods
- Add interfaces
- Use dependency injection

### What Makes a Good Unit Test?

**FIRST Principles:**

**F - Fast:**
- Runs in milliseconds
- No I/O, network, or database calls
- Can run thousands of tests quickly

**I - Independent:**
- No dependencies on other tests
- Can run in any order
- No shared state

**R - Repeatable:**
- Same result every time
- No randomness (unless testing randomness)
- No external dependencies

**S - Self-Validating:**
- Pass or fail clearly
- No manual inspection needed
- Boolean result

**T - Timely:**
- Written before or alongside code
- TDD or test-first approach

**Additional Qualities:**

**1. Clear and Readable:**
```java
// Bad: Unclear what's being tested
@Test
void test1() {
    Service s = new Service();
    String r = s.doSomething("x", 5);
    assertTrue(r != null && r.length() > 0);
}

// Good: Clear intent
@Test
void processOrder_whenValidOrder_returnsConfirmationNumber() {
    OrderService service = new OrderService();
    Order order = new Order(100.0);
    
    String confirmation = service.processOrder(order);
    
    assertNotNull(confirmation);
    assertTrue(confirmation.matches("ORD-\\d+"));
}
```

**2. Tests One Thing:**
```java
// Bad: Tests multiple things
@Test
void testUserOperations() {
    userService.createUser("John");
    userService.updateUser(1L, "Jane");
    userService.deleteUser(1L);
    // Too many assertions, unclear what failed
}

// Good: One test, one assertion (or related assertions)
@Test
void createUser_whenValidData_returnsUserWithId() {
    User user = userService.createUser("John", "john@example.com");
    
    assertNotNull(user.getId());
    assertEquals("John", user.getName());
}
```

**3. Arrange-Act-Assert Pattern:**
```java
@Test
void testWithAAA() {
    // Arrange: Set up test data and dependencies
    UserRepository mockRepo = mock(UserRepository.class);
    when(mockRepo.findById(1L)).thenReturn(new User("John"));
    UserService service = new UserService(mockRepo);
    
    // Act: Execute the code under test
    User result = service.getUser(1L);
    
    // Assert: Verify the results
    assertEquals("John", result.getName());
    verify(mockRepo).findById(1L);
}
```

**4. Meaningful Assertions:**
```java
// Bad: Weak assertion
assertTrue(result != null);

// Good: Specific assertion
assertEquals("expected-value", result);
assertThat(result).isEqualTo("expected-value");
```

### Test Coverage: The 100% Coverage Myth

**The Myth:**
"100% code coverage means perfect tests"

**The Reality:**
- Coverage measures lines executed, not test quality
- 100% coverage can exist with terrible tests
- Some code paths are hard or impossible to test
- Diminishing returns after ~80% coverage

**Why 100% Coverage is Problematic:**

```java
// Example: 100% coverage with useless test
class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
    
    public int subtract(int a, int b) {
        return a - b;
    }
}

// Test with 100% coverage but poor quality
@Test
void testAdd() {
    Calculator calc = new Calculator();
    calc.add(1, 1); // Executes line, but doesn't verify result!
}

@Test
void testSubtract() {
    Calculator calc = new Calculator();
    calc.subtract(1, 1); // Executes line, but doesn't verify result!
}
// 100% coverage, 0% confidence
```

**What Coverage Actually Measures:**
- **Line Coverage**: Lines executed
- **Branch Coverage**: If/else paths taken
- **Path Coverage**: All possible execution paths (usually impractical)

**Realistic Coverage Goals:**
- **Critical paths**: 90%+ (payment, authentication, data integrity)
- **Business logic**: 80%+ (core functionality)
- **Utilities/helpers**: 70%+ (less critical)
- **Getters/setters**: Don't worry about it
- **Exception handlers**: Test important ones

**Focus on Quality, Not Quantity:**

```java
// Better: 70% coverage with excellent tests
@Test
void calculateDiscount_premiumCustomer_returns20Percent() {
    DiscountCalculator calc = new DiscountCalculator();
    double discount = calc.calculate(100.0, CustomerType.PREMIUM);
    assertEquals(20.0, discount);
}

@Test
void calculateDiscount_regularCustomer_returns10Percent() {
    DiscountCalculator calc = new DiscountCalculator();
    double discount = calc.calculate(100.0, CustomerType.REGULAR);
    assertEquals(10.0, discount);
}

// Better than 100% coverage with weak tests
```

**Coverage as a Tool, Not a Goal:**
- Use coverage to find untested code
- Don't write tests just to increase coverage
- Focus on testing behavior, not achieving numbers
- Review coverage reports to identify gaps

### Design a Testing Strategy for a Microservice

**Scenario:** E-commerce order processing microservice

**Architecture:**
- REST API (Spring Boot)
- PostgreSQL database
- Message queue (RabbitMQ)
- External payment gateway
- Email service

**Testing Strategy:**

**1. Unit Tests (70% of tests):**

```java
// Test business logic in isolation
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private PaymentGateway paymentGateway;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void processOrder_whenPaymentSucceeds_createsOrder() {
        OrderRequest request = new OrderRequest(100.0);
        when(paymentGateway.process(any())).thenReturn(PaymentResult.success());
        
        Order order = orderService.processOrder(request);
        
        assertNotNull(order.getId());
        verify(orderRepository).save(any(Order.class));
    }
}
```

**2. Integration Tests (20% of tests):**

```java
// Test database interactions
@SpringBootTest
@Transactional
class OrderRepositoryIntegrationTest {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void testSaveAndFindOrder() {
        Order order = new Order(100.0);
        Order saved = orderRepository.save(order);
        
        Optional<Order> found = orderRepository.findById(saved.getId());
        assertTrue(found.isPresent());
    }
}

// Test REST API
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PaymentGateway paymentGateway; // Mock external service
    
    @Test
    void testCreateOrderEndpoint() throws Exception {
        when(paymentGateway.process(any())).thenReturn(PaymentResult.success());
        
        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content("{\"amount\": 100.0}"))
            .andExpect(status().isCreated());
    }
}
```

**3. Contract Tests (5% of tests):**

```java
// Test API contracts
@ExtendWith(PactConsumerTestExt.class)
class OrderServiceContractTest {
    
    @Pact(consumer = "OrderService", provider = "PaymentGateway")
    public RequestResponsePact paymentPact(PactDslWithProvider builder) {
        return builder
            .given("payment can be processed")
            .uponReceiving("a payment request")
            .path("/api/payments")
            .method("POST")
            .willRespondWith()
            .status(200)
            .body(new PactDslJsonBody()
                .stringType("status", "SUCCESS")
                .stringType("transactionId", "txn-123"))
            .toPact();
    }
}
```

**4. End-to-End Tests (5% of tests):**

```java
// Test complete workflows
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class OrderE2ETest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testCompleteOrderFlow() {
        // Create order
        OrderRequest request = new OrderRequest(100.0);
        ResponseEntity<Order> response = restTemplate.postForEntity(
            "/api/orders", request, Order.class);
        
        assertEquals(201, response.getStatusCodeValue());
        assertNotNull(response.getBody().getId());
        
        // Verify order in database
        // Verify message sent to queue
        // Verify email sent
    }
}
```

**Test Pyramid for Microservice:**

```
        /\
       /  \     E2E: Complete workflows (5%)
      /____\
     /      \   Integration: API, DB, MQ (20%)
    /________\
   /          \  Unit: Business logic (70%)
  /____________\  Contract: API contracts (5%)
```

**Key Testing Principles:**

1. **Test in Isolation**: Unit tests mock external dependencies
2. **Test Boundaries**: Integration tests verify interactions
3. **Test Contracts**: Ensure service contracts are maintained
4. **Test Critical Paths**: Focus on business-critical flows
5. **Fast Feedback**: Unit tests run in CI on every commit
6. **Comprehensive Coverage**: Integration/E2E run in staging

**Test Execution Strategy:**

- **Local Development**: Run unit tests (fast feedback)
- **Pre-commit Hook**: Run unit tests
- **CI Pipeline**: 
  - Unit tests (every commit)
  - Integration tests (on PR)
  - Contract tests (on PR)
  - E2E tests (on merge to main)
- **Staging Environment**: Full test suite before production

**Tools and Technologies:**

- **Unit Testing**: JUnit 5, Mockito
- **Integration Testing**: Spring Boot Test, Testcontainers
- **Contract Testing**: Pact, Spring Cloud Contract
- **E2E Testing**: Testcontainers, WireMock
- **Coverage**: JaCoCo
- **Test Data**: Testcontainers, H2 (in-memory DB)

**Metrics to Track:**

- Test execution time (keep unit tests < 1 second each)
- Code coverage (aim for 80%+ on critical paths)
- Test flakiness rate (should be < 1%)
- Bug escape rate (bugs found in production)

---

[← Back to Index](00-README.md) | [Previous: Performance](16-Performance.md) | [Next: Security →](18-Security.md)
