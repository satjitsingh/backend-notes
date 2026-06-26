# Chapter 29: Spring Security Fundamentals

[← Back to Index](00-README.md) | [Previous: Query Optimization](28-Query-Optimization.md) | [Next: JWT Auth →](30-JWT-Auth.md)

---

## Why This Chapter Matters

Imagine you build a beautiful house but forget to add locks on the doors. Anyone could walk in, take your things, or pretend to be you. That's what happens when an application has no security.

**Spring Security** is like a team of guards, locks, and rules that protect your application. It's not optional. It's not something you add later when you "have time." A single security mistake can leak millions of passwords, damage your reputation, and get you in legal trouble.

This chapter teaches you how security works—from the basics (like explaining it to a curious kid) all the way to advanced topics that senior developers and interviewers care about.

---

## Layer 1 — Intuition Builder

### Part 1: Security = A Nightclub With Bouncers

Picture a fancy nightclub. Before anyone gets inside, they must pass several checkpoints:

```
🏢 THE NIGHTCLUB (Your Web Application)
│
├── 🚪 Front Door (Where people arrive)
│   │
│   ├── 🧑‍✈️ Bouncer #1: "Show me your ID!"
│   │   └── This checks: "Are you really who you say you are?"
│   │       (We call this AUTHENTICATION — proving your identity)
│   │
│   ├── 🧑‍✈️ Bouncer #2: "Are you on the VIP list?"
│   │   └── This checks: "Are you allowed to be here?"
│   │       (We call this AUTHORIZATION — checking your permissions)
│   │
│   └── 🧑‍✈️ Bouncer #3: "Empty your pockets—no weapons allowed!"
│       └── This checks: "Is anything you're bringing in dangerous?"
│           (We call this INPUT VALIDATION — blocking harmful stuff)
│
├── 🎵 Dance Floor (Public area)
│   └── Anyone with a valid ID can enter
│
├── 🥂 VIP Section (Special area)
│   └── Only VIP members can enter
│
└── 🔒 Owner's Office (Secret area)
    └── Only the owner can enter
```

**The big idea:** Your web application works the same way. Every person (request) trying to get in must pass several "bouncers" (security checks). Spring Security is the system that sets up and runs these bouncers for you.

---

### Part 2: Authentication vs Authorization—Two Different Questions

Think of going to an airport:

| Question | What It Means | Real Example |
|----------|---------------|--------------|
| **"Who are you?"** | Authentication | Showing your passport—proving your identity |
| **"Where are you allowed to go?"** | Authorization | Showing your boarding pass—proving you're allowed on this specific flight |

**Authentication** = Prove who you are (username + password, fingerprint, etc.)  
**Authorization** = Prove you're allowed to do this specific thing (like entering the admin area)

**Important:** You must answer "Who are you?" BEFORE you can answer "Where are you allowed to go?" If you show up without a passport, they won't even look at your boarding pass.

---

### Part 3: The Pipeline of Checks (Filter Chain)

Every request to your app goes through a **pipeline**—like an assembly line of checks. If it fails ANY check, it never reaches your code.

```
       HTTP REQUEST (Someone knocking on your app's door)
                    │
                    ▼
┌───────────────────────────────────────────────────────────────────┐
│  CHECK 1: "Do we already know you from before?"                     │
│  (Did you log in earlier? Are you using a saved session?)           │
└───────────────────────────┬───────────────────────────────────────┘
                            │ Pass? Continue ▼
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│  CHECK 2: "Where are you coming from?"                              │
│  (Is your website allowed to talk to our server?)                    │
└───────────────────────────┬───────────────────────────────────────┘
                            │ Pass? Continue ▼
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│  CHECK 3: "Is this request real or fake?"                           │
│  (Are you a real user or a scammer trying to trick us?)             │
└───────────────────────────┬───────────────────────────────────────┘
                            │ Pass? Continue ▼
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│  CHECK 4: "Who are you? Prove it!"                                  │
│  (Show us your username and password, or your token)                │
└───────────────────────────┬───────────────────────────────────────┘
                            │ Pass? Continue ▼
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│  CHECK 5: "Are you allowed to do this?"                             │
│  (Do you have permission to access this page?)                      │
└───────────────────────────┬───────────────────────────────────────┘
                            │ Pass? Continue ▼
                            ▼
                    ✅ YOUR CODE RUNS!
                    (Finally, the request reaches your controller)
```

**Key idea:** One failed check = request stops. No exceptions. This is why security is so strong—there's no way to "skip" a step.

---

### Part 4: Bad Guys and How We Stop Them

Here are the main ways attackers try to break in, and what we do to stop them:

| Attack | What the Bad Guy Does | Simple Analogy | How We Stop It |
|--------|-----------------------|----------------|----------------|
| **CSRF** (Cross-Site Request Forgery) | Tricks your browser into sending a request you didn't mean to send | Someone steals your signed blank check and fills in their own amount | We give you a secret token. Fake websites don't have it, so their requests get rejected. |
| **XSS** (Cross-Site Scripting) | Injects sneaky code that runs in other people's browsers | Someone hides a tiny camera in a room—everyone gets recorded | We sanitize (clean) all user input so no scripts can sneak in. |
| **Session Fixation** | Forces you to use a session ID they already know | Someone makes a copy of a hotel key, lets you check in, then uses their copy | We create a NEW session ID after you log in, so the old one becomes useless. |
| **Brute Force** | Tries thousands of passwords until one works | Someone tries every key on a keyring until the door opens | We slow them down (rate limiting), lock accounts after too many tries, and use strong password hashing. |

---

### Part 5: Passwords—Why We Never Store the Real Thing

Imagine a bank that writes your PIN on the outside of your debit card. Anyone who sees the card can use your money. That's what happens when we store passwords as plain text (the actual words users type).

**Instead, we use something called "hashing":**

- When you sign up, we take your password and run it through a special one-way math recipe.
- The result is a long gibberish string like `$2a$10$N9qo8uLOickgx2ZMRZoMye...`
- We store ONLY that gibberish. We never store "mypassword123".
- When you log in, we run your typed password through the same recipe and compare the result. If they match, you're in!

**Why "one-way"?** You can't reverse it. If a hacker steals the gibberish from our database, they still can't figure out your real password. (Well, not easily—that's why we use strong algorithms like BCrypt.)

---

## Layer 2 — Professional Developer

### Step-by-Step Security Setup

#### Step 1: Add the Dependency

```xml
<!-- Add to your pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**What happens:** The moment you add this, Spring Boot turns on security. Every endpoint becomes protected. A default login page appears. A default user is created (username: `user`, password: printed in the console). This is "convention over configuration"—sensible defaults with zero code.

---

#### Step 2: Create a Security Configuration Class

We need to tell Spring Security:
- Which URLs are public (anyone can access)
- Which URLs require specific roles (only admins, etc.)
- How users log in (form or HTTP Basic)
- How to log out

```java
// Tells Spring: "I want to configure web security myself"
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // This bean defines the entire security pipeline
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // --- AUTHORIZATION: Who can access what? ---
            .authorizeHttpRequests(auth -> auth
                // Public: no login required
                .requestMatchers("/", "/home", "/about").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/register", "/login").permitAll()
                
                // Admin-only: must have ADMIN role
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Manager or Admin: either role works
                .requestMatchers("/management/**").hasAnyRole("ADMIN", "MANAGER")
                
                // Everything else: must be logged in (any authenticated user)
                .anyRequest().authenticated()
            )
            
            // --- LOGIN: How do users prove who they are? ---
            .formLogin(form -> form
                .loginPage("/login")              // URL of our login page
                .defaultSuccessUrl("/dashboard")  // Where to go after success
                .failureUrl("/login?error=true")  // Where to go after failure
                .permitAll()                      // Login page must be public!
            )
            
            // --- LOGOUT: How do users sign out? ---
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)     // Clear the session
                .deleteCookies("JSESSIONID")      // Remove session cookie
            )
            
            .build();
    }
    
    // --- USER STORAGE: Where do we look up usernames and passwords? ---
    // For development: in-memory users. For production: database (see below).
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails alice = User.builder()
            .username("alice")
            .password(passwordEncoder().encode("alice123"))  // MUST encode!
            .roles("USER")
            .build();
        
        UserDetails bob = User.builder()
            .username("bob")
            .password(passwordEncoder().encode("bob123"))
            .roles("USER", "ADMIN")  // Bob has two roles
            .build();
        
        return new InMemoryUserDetailsManager(alice, bob);
    }
    
    // --- PASSWORD ENCODING: NEVER store plain text. Always use BCrypt. ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Key annotations and concepts:**

| Term | Plain English | Technical |
|------|---------------|-----------|
| `@EnableWebSecurity` | "I'm taking control of security" | Enables Spring Security and disables defaults |
| `SecurityFilterChain` | The list of bouncers/pipeline steps | Ordered list of filters every request passes through |
| `UserDetailsService` | "The receptionist who looks up users" | Interface; we implement `loadUserByUsername()` |
| `PasswordEncoder` | "The password scrambler" | Hashes passwords so we never store plain text |

---

### Database-Backed Authentication (Production Setup)

In real apps, users live in a database. We implement `UserDetailsService` to look them up.

#### 1. User Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;  // Stores BCrypt hash, NEVER plain text
    
    @Column(nullable = false)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;        // USER, ADMIN, MANAGER, etc.
    
    private boolean enabled = true;
    private boolean accountNonLocked = true;
    
    // Getters, setters, constructors...
}

public enum Role {
    USER, MANAGER, ADMIN
}
```

#### 2. Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

#### 3. Custom UserDetailsService (The Bridge)

Spring Security calls `loadUserByUsername()` when someone tries to log in. We look up the user in the database and return Spring's `UserDetails` object.

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) 
            throws UsernameNotFoundException {
        
        // 1. Find user in database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found: " + username));
        
        // 2. Convert our User entity to Spring's UserDetails
        // Spring Security then compares the password from the login form
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())         // Already hashed in DB
            .roles(user.getRole().name())         // "USER", "ADMIN", etc.
            .disabled(!user.isEnabled())
            .accountLocked(!user.isAccountNonLocked())
            .build();
    }
}
```

#### 4. Registration Service (Always Encode Passwords!)

```java
@Service
public class UserRegistrationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // ...
    
    public User registerUser(String username, String rawPassword, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username taken: " + username);
        }
        
        User user = new User();
        user.setUsername(username);
        // CRITICAL: Encode before storing. NEVER store rawPassword!
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setRole(Role.USER);
        
        return userRepository.save(user);
    }
}
```

---

### Method-Level Security (@PreAuthorize, @Secured)

Sometimes URL rules aren't enough. You need rules like: "Only the owner of a post can edit it."

#### Enable Method Security

```java
@Configuration
@EnableMethodSecurity  // Enables @PreAuthorize, @Secured, etc.
public class MethodSecurityConfig {
}
```

#### Use It on Service Methods

```java
@Service
public class UserService {
    
    // Only ADMIN can call this
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    // ADMIN or MANAGER
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // User can only get THEIR OWN profile; admin can get anyone's
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow();
    }
    
    // Any logged-in user
    @PreAuthorize("isAuthenticated()")
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }
}
```

**Common expressions:**

| Expression | Meaning |
|------------|---------|
| `hasRole('ADMIN')` | User has ADMIN role |
| `hasAnyRole('ADMIN', 'MANAGER')` | User has at least one of these roles |
| `hasAuthority('DELETE_USER')` | User has specific permission |
| `isAuthenticated()` | User is logged in |
| `isAnonymous()` | User is not logged in |
| `#userId == authentication.principal.id` | Parameter matches current user's ID |

---

### HTTP Basic Auth (Simple API Protection)

For REST APIs, you can use HTTP Basic: the client sends username and password in the `Authorization` header.

```java
// In SecurityFilterChain:
.httpBasic(Customizer.withDefaults())
```

Then disable form login (since APIs typically don't use HTML forms):

```java
// No form login for API-only apps
.formLogin(Customizer.withDefaults())  // or omit for API-only
```

---

### CORS Configuration (When Frontend and Backend Are Separate)

If your React app runs on `http://localhost:3000` and your API on `http://localhost:8080`, the browser blocks requests by default (Same-Origin Policy). CORS tells the browser: "It's okay for this other site to talk to me."

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    
    // Which websites can call our API?
    config.setAllowedOrigins(List.of("http://localhost:3000", "https://myapp.com"));
    
    // Which HTTP methods? (GET, POST, etc.)
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    
    // Which headers can they send?
    config.setAllowedHeaders(List.of("*"));
    
    // Include cookies? (needed for session-based auth)
    config.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

---

### CSRF: When to Enable vs Disable

| App Type | CSRF | Why |
|----------|------|-----|
| **Session-based** (traditional web app with login form) | **Enable** | Browser auto-sends cookies; attackers can forge requests. CSRF token blocks them. |
| **Stateless API** (JWT, no sessions) | **Disable** | No session cookies = no CSRF risk. Token is in header, not auto-sent. |

```java
// Session-based app (forms):
// CSRF is enabled by default. No change needed.

// Stateless API (JWT):
.csrf(csrf -> csrf.disable())
```

---

### SecurityContextHolder—Where Spring Stores "Who Is Logged In"

When a user logs in, Spring puts their identity in `SecurityContextHolder`. You can read it anywhere in your code:

```java
// Get the currently logged-in user's name
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
Object principal = auth.getPrincipal();  // Often a UserDetails object
```

It's stored per-thread (each request has its own). So Alice's request sees Alice's auth, and Bob's request sees Bob's—they never mix.

---

## Layer 3 — Advanced Engineering Depth

### The Security Filter Chain—Exact Order

Spring Security uses a fixed order of filters. Understanding this helps with debugging and custom filters.

```
1. DisableEncodeUrlFilter           → Hides session ID from URLs
2. SecurityContextHolderFilter      → Loads auth from session into current thread
3. HeaderWriterFilter               → Adds security headers (X-Frame-Options, etc.)
4. CorsFilter                       → Handles CORS preflight and headers
5. CsrfFilter                       → Validates CSRF token on POST/PUT/DELETE
6. LogoutFilter                     → Handles /logout
7. UsernamePasswordAuthenticationFilter → Processes form login (POST /login)
8. BasicAuthenticationFilter        → Processes HTTP Basic header
9. RequestCacheAwareFilter          → Restores original request after login redirect
10. SecurityContextHolderAwareRequestFilter → Wraps request with security helpers
11. AnonymousAuthenticationFilter   → If no auth, creates anonymous user
12. ExceptionTranslationFilter      → Converts security exceptions → 401/403
13. AuthorizationFilter             → Final check: does user have permission?
```

---

### Custom Authentication: JWT Filter Example

For stateless JWT auth, you add a custom filter that runs *before* `UsernamePasswordAuthenticationFilter`:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) 
            throws ServletException, IOException {
        
        // 1. Get token from "Authorization: Bearer <token>"
        String token = extractToken(request);
        
        // 2. If valid, load user and set SecurityContext
        if (token != null && tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

**Register the filter:**

```java
.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
```

---

### Custom AuthenticationProvider

For complex auth (e.g., custom token validation, LDAP), implement `AuthenticationProvider`:

```java
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    
    @Override
    public Authentication authenticate(Authentication auth) 
            throws AuthenticationException {
        String username = auth.getName();
        String password = (String) auth.getCredentials();
        
        // Your custom validation logic here
        // ...
        
        return new UsernamePasswordAuthenticationToken(
            userDetails, password, userDetails.getAuthorities());
    }
    
    @Override
    public boolean supports(Class<?> authType) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authType);
    }
}
```

---

### SecurityContext and Async Code (Edge Case)

`SecurityContext` is stored in a `ThreadLocal`. Async code runs on a *different* thread, so it does NOT automatically see the logged-in user.

```java
@Async
public void sendAsyncNotification() {
    // auth is NULL here! Different thread.
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
}
```

**Fix:** Use `DelegatingSecurityContextAsyncTaskExecutor` or set strategy to `MODE_INHERITABLETHREADLOCAL` so the context is copied to child threads.

---

### Session Management Configuration

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
    .maximumSessions(1)                    // One session per user
    .maxSessionsPreventsLogin(false)     // New login expires old session
    .sessionFixation().migrateSession()   // New session ID after login (security)
)
```

| Policy | Meaning |
|--------|---------|
| `STATELESS` | Never create sessions (JWT APIs) |
| `IF_REQUIRED` | Create only when needed (default) |
| `ALWAYS` | Always create a session |

---

### Common Security Threats and Protections (Table)

| Threat | What Happens | Spring Security Protection |
|--------|--------------|----------------------------|
| CSRF | Attacker forges request using victim's session | `CsrfFilter` validates token |
| XSS | Malicious script injected into page | Input sanitization; secure headers |
| Session Fixation | Attacker reuses known session ID | `sessionFixation().migrateSession()` |
| Brute Force | Many password guesses | Rate limiting, lockout, strong hashing |
| SQL Injection | Malicious SQL in input | Parameterized queries (JPA) |
| Sensitive data in URLs | Passwords in query params | Use POST; avoid GET for secrets |

---

## Layer 4 — Interview Mastery

### Q1: What's the difference between Authentication and Authorization?

**Answer:**  
**Authentication** = "Who are you?" — verifying identity (e.g., username + password).  
**Authorization** = "What are you allowed to do?" — checking permissions (e.g., roles).  
Authentication happens first. If it fails → 401. If it succeeds but authorization fails → 403.

---

### Q2: What is the SecurityFilterChain?

**Answer:**  
An ordered list of servlet filters. Every HTTP request passes through them before reaching the controller. Filters handle CORS, CSRF, authentication, authorization, etc. If any filter rejects the request, the controller is never reached. You configure it via a `SecurityFilterChain` bean in Spring Security 6.x.

---

### Q3: What is UserDetailsService?

**Answer:**  
An interface with one method: `loadUserByUsername(String username)`. Spring Security calls it during login to fetch user data (username, password hash, roles). You implement it to load users from a database, LDAP, or any other source.

---

### Q4: Why use BCrypt for passwords?

**Answer:**  
- One-way hash: can't reverse to get the original password  
- Includes random salt: same password → different hashes  
- Configurable work factor: can make hashing slower to resist brute force  
- Industry standard, battle-tested  

Never store plain text passwords.

---

### Q5: When do you enable vs disable CSRF?

**Answer:**  
**Enable** for session-based web apps (form login, cookies). Browsers auto-send cookies, so attackers can forge requests.  
**Disable** for stateless REST APIs using JWT. No session cookies = no CSRF vector. Token is in the header and not auto-sent.

---

### Q6: How does method-level security work?

**Answer:**  
Use `@EnableMethodSecurity` and annotations like `@PreAuthorize("hasRole('ADMIN')")` or `@Secured("ROLE_ADMIN")` on service methods. Spring uses AOP to check the expression before the method runs. Supports SpEL (e.g., `#userId == authentication.principal.id`).

---

### Q7: What is SecurityContextHolder?

**Answer:**  
Holds the current user's `Authentication` in a `ThreadLocal`. Each request thread has its own. Use `SecurityContextHolder.getContext().getAuthentication()` to get the current user. It does NOT automatically propagate to async threads.

---

### Q8: How would you add custom authentication (e.g., JWT)?

**Answer:**  
1. Create a filter extending `OncePerRequestFilter`.  
2. Extract and validate the JWT from the `Authorization` header.  
3. Load user and create `UsernamePasswordAuthenticationToken`.  
4. Set it in `SecurityContextHolder.getContext().setAuthentication()`.  
5. Register with `http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`.  
6. Use `SessionCreationPolicy.STATELESS` and disable CSRF.

---

### Q9: What protects against session fixation?

**Answer:**  
`sessionFixation().migrateSession()` in session management config. After login, Spring creates a new session ID. The old one (which the attacker might know) becomes invalid.

---

### Q10: 401 vs 403—when does each happen?

**Answer:**  
**401 Unauthorized** = Authentication failed. We don't know who you are (wrong password, missing token, etc.).  
**403 Forbidden** = Authentication succeeded, but you're not allowed to do this (wrong role, not enough permissions).

---

## Summary

| Concept | Takeaway |
|---------|----------|
| **Authentication** | Proves identity ("Who are you?") |
| **Authorization** | Proves permission ("What can you do?") |
| **Filter Chain** | Pipeline of checks; request must pass all to reach the controller |
| **UserDetailsService** | Loads user from DB/other source for Spring Security |
| **PasswordEncoder** | Always use BCrypt; never store plain text |
| **Form vs JWT** | Form login → sessions + CSRF. JWT → stateless, no CSRF. |
| **Method Security** | `@PreAuthorize`, `@Secured` for fine-grained control |
| **SecurityContextHolder** | ThreadLocal; holds current user; doesn't propagate to async by default |
| **401** | Not authenticated |
| **403** | Authenticated but not authorized |

---

[← Back to Index](00-README.md) | [Previous: Query Optimization](28-Query-Optimization.md) | [Next: JWT Auth →](30-JWT-Auth.md)
