# Chapter 30: JWT Authentication

[← Back to Index](00-README.md) | [Previous: Spring Security](29-Spring-Security.md) | [Next: Caching →](31-Caching.md)

---

## Why This Chapter Matters

If you're building any modern API — the backend for a mobile app, a single-page web application, or a system made of many small services — you WILL use JWT. It's the **industry standard** for stateless authentication.

Every Spring Boot REST API tutorial, every job interview about APIs, and every production application you'll work on uses some form of token-based authentication. JWT is by far the most popular choice.

**Without JWT understanding:**
- You can't build a secure REST API (the #1 thing backend developers are hired to do)
- You won't understand how "Login with Google" or mobile app authentication works
- You'll struggle in security-focused interviews (JWT questions appear in 90% of backend interviews)
- You'll build insecure systems that leak user data or allow unauthorized access

**With JWT mastery:**
- You can build authentication for ANY application (web, mobile, microservices)
- You understand the security trade-offs and can explain them to your team
- You implement refresh tokens, token rotation, and blacklisting correctly
- You handle edge cases (token expiration, stolen tokens, logout)
- You excel in security architecture discussions and interviews

---

## Layer 1 — Intuition Builder

### Part 1: The Big Question — How Does a Website Remember You?

Imagine you go to a theme park. You show your ticket at the gate once. After that, how do the ride operators know you're allowed to be there? They don't call the main office every time. They need a quick way to know you belong there.

Websites have the same problem. When you type your password and log in, the website checks it. Great! But then you click to another page. How does the website know you're still you? The internet doesn't "remember" things automatically. Every time you ask for a new page, it's like a brand-new conversation. The website has to have a clever way to recognize you.

---

### Part 2: The Old Way — The "Room Key" Method (Sessions)

**Plain English First:** Some websites use something called a **session**. Think of it like a hotel.

1. You check in at the front desk (that's **logging in**)
2. The hotel gives you a **room key card** — a small plastic card (that's your **session ID**)
3. Every time you want to use the pool, gym, or restaurant, the staff **calls the front desk** to ask: "Is this person a real guest?" (That's the server **looking you up** in its memory.)
4. The front desk checks its big book of guests and says yes or no

**The problem:** If a million people check in, the front desk gets VERY busy. Every single request means another phone call to the front desk. And if the hotel has many buildings (many servers), they all need to share the same giant guest book. That's hard to do!

---

### Part 3: The New Way — The "Concert Wristband" Method (JWT)

**Plain English First:** JWT works differently. Imagine a concert:

1. You buy your ticket and go through the entrance (that's **logging in**)
2. They give you a **wristband** (that's the **JWT token**)
3. The wristband has your seat number, your name, and a special **hologram** or **stamp** that's hard to fake (that's the **signature**)
4. When you go to the food stand or the bathroom, the staff **looks at your wristband**. They don't call anyone. They just check: "Does the hologram look real? Is the wristband still valid?" (That's **stateless verification** — no one has to "remember" you.)

**The benefit:** No one needs a giant list of everyone at the concert. The wristband itself tells the story. Any staff member can check it. That's why JWT works so well when you have many servers — each one can check the token on its own, without talking to a central database.

---

### Part 4: What Exactly Is a JWT? (The "Passport" Analogy)

A **JWT** (which stands for **JSON Web Token** — a fancy name for "a small piece of text that carries information and proof it's real") is like a **mini passport**:

- **Passport = JWT token**
- **Your photo and name inside = the data (payload)**
- **The government's official stamp = the signature** — it proves no one tampered with it
- **When it expires = the expiration date** — after that, you need a new one

Anyone can *see* what's written on your passport (the data isn't secret). But they can't *forge* the stamp. That's exactly how JWT works: the information inside can be read, but the signature proves it came from the real server and wasn't changed.

---

### Part 5: The Three Parts of a JWT (Visual)

A JWT is one long string of characters with two dots in it. Those dots separate three parts:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
│──────── Part 1 ─────────│────────────── Part 2 ───────────────────│──────────── Part 3 ──────────────────│
      HEADER                         PAYLOAD                                SIGNATURE
  (What kind of token?)           (Who are you? When does it expire?)    (Anti-tamper seal)
```

**Part 1 — Header (The Label):**  
"This is a JWT token. We used method X to sign it."  
Like the label on a medicine bottle that says what's inside.

**Part 2 — Payload (The Actual Info):**  
"This token belongs to user@example.com. It was created at 3:00 PM. It expires at 4:00 PM."  
Like the form you fill out — your name, your permissions, when things happen.

**⚠️ Important:** Parts 1 and 2 are NOT secret! They're encoded (scrambled in a way that's easy to decode) but not encrypted. Anyone with the token can read them. **Never put passwords or credit card numbers in the payload!**

**Part 3 — Signature (The Seal):**  
A special code that only the server can create, using a **secret key** (like a password only the server knows). If someone changes Part 1 or Part 2 — even one letter — the signature won't match anymore, and the server will reject the token.

---

### Part 6: Session vs JWT — Side by Side

| **Question** | **Session (Hotel Model)** | **JWT (Concert Wristband Model)** |
|-------------|---------------------------|-----------------------------------|
| Where does the server keep your info? | In a database or memory (like the front desk's guest book) | Inside the token itself — the server doesn't store it |
| Can the server "kick you out" immediately? | Yes — delete your session from the book | No — the token keeps working until it expires (unless you use a blacklist) |
| If you have 100 servers, do they all need to share data? | Yes — they all need the same guest book | No — each server can verify the token by itself |
| Does it work well for phone apps? | Not really — cookies (the usual way) are awkward on phones | Yes — you just send the token with each request |
| Does it work across many small services? | Hard — everyone needs the central book | Easy — any service can check the token |

---

### Part 7: How Does the Whole Flow Work? (Step by Step)

1. **You log in** — You type your username and password and send them to the server.
2. **The server checks you** — It compares your password with what it has stored. If it matches, you're in!
3. **The server creates a token** — It puts your username, roles, and an expiration time into the token, then signs it with its secret key.
4. **You get the token** — The server sends it back to you. Your browser or app saves it (in memory, or in storage).
5. **You ask for something** — "Give me my profile" or "Show me my orders." You send the token along with the request, usually in a header like: `Authorization: Bearer <your-token>`.
6. **The server checks the token** — It verifies the signature (is it real?) and the expiration (is it still valid?). If both pass, it processes your request.
7. **When the token expires** — You use a special "refresh token" (like a backup pass) to get a new token without typing your password again.
8. **When you log out** — Your app deletes the token. For extra security, the server can also put it on a "blacklist" so it can't be used anymore.

---

### Part 8: Key Words You Need to Know (Plain English)

- **Claims** — The pieces of information inside the token. Like fields on a form: "subject" (who), "expiration" (when it dies), "issued at" (when it was created), and your custom stuff like "roles" or "department."

- **Stateless** — The server doesn't remember you between requests. The token carries everything it needs. No giant guest book.

- **Self-contained** — The token *is* the data. The server doesn't need to look you up in a database. Faster! But the trade-off: you can't "cancel" the token until it expires (unless you add a blacklist).

- **Bearer** — A fancy word that means "whoever has this token can use it." When you send `Authorization: Bearer <token>`, you're saying: "Here's my token; the person holding it (bearer) is me."

---

### Part 9: Common JWT Claims (What Goes in the Payload)

| **Claim** | **Full Name** | **What It Means** | **Example** |
|-----------|---------------|-------------------|-------------|
| `sub` | Subject | Who this token is about (usually username or user ID) | `"user@example.com"` |
| `iat` | Issued At | When the token was created (timestamp) | `1609459200` |
| `exp` | Expiration | When the token becomes invalid (timestamp) | `1609462800` |
| `roles` | (Custom) | What the user is allowed to do | `["USER", "ADMIN"]` |

---

## Layer 2 — Professional Developer

### Why JWT Matters for Professional Development

Before writing code, understand **why** we use JWT:

1. **Stateless scaling** — Microservices and load-balanced APIs don't share session storage. JWT lets any instance validate the token.
2. **Mobile and SPA support** — Mobile apps and single-page apps (React, Vue, Angular) don't handle cookies well. JWT travels in headers.
3. **Cross-domain auth** — APIs on different domains can validate the same token without sharing a session store.

Now let's build it.

---

### Dependencies

Add the JJWT library to your `pom.xml`. This library creates and validates JWTs.

```xml
<!-- JJWT API - the main interface we use in our code -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<!-- JJWT implementation - the actual crypto and parsing logic (loaded at runtime) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<!-- Jackson for JSON serialization of JWT payload -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

---

### JWT Service Implementation

This service is responsible for **creating** tokens (when the user logs in) and **validating** them (when the user makes API requests).

```java
package com.example.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Secret key from config - MUST be at least 32 chars for HS256
    @Value("${jwt.secret}")
    private String secretKey;

    // How long the token lasts (default 1 hour in milliseconds)
    @Value("${jwt.expiration:3600000}")
    private Long expiration;

    /**
     * Create the cryptographic key used to sign tokens.
     * For HS256, we need at least 256 bits (32 bytes).
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT for a logged-in user.
     * Includes username (subject) and authorities (roles) as claims.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities());
        return generateToken(claims, userDetails.getUsername());
    }

    /**
     * Internal method to build the token with claims and sign it.
     * Sets: claims, subject, issued-at, expiration, and signature.
     */
    private String generateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
            .claims(claims)
            .subject(subject)  // Usually username or email
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Extract the username (subject) from the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract the expiration date from the token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract any claim using a resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse the token and return all claims (payload).
     * Also verifies the signature - throws if invalid or tampered.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Check if the token has passed its expiration time.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Full validation: username matches AND token is not expired.
     * Used when we've loaded UserDetails from DB.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Basic validation: token is valid format and not expired.
     * Use when we only need to check token integrity.
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

### JWT Authentication Filter

This filter runs **before** every request. It looks for the token in the `Authorization` header, validates it, and tells Spring Security who the user is.

```java
package com.example.auth.filter;

import com.example.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Standard header for JWT: "Authorization: Bearer <token>"
        final String authHeader = request.getHeader("Authorization");

        // No token? Let the request continue - Spring Security will block if endpoint requires auth
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Remove "Bearer " prefix to get raw token
            final String jwt = authHeader.substring(7);

            // Get username from token (no DB call yet)
            final String username = jwtService.extractUsername(jwt);

            // Only authenticate if we have a username AND no one is already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user from DB to get roles and validate token
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.validateToken(jwt, userDetails)) {
                    // Create Spring Security authentication object
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                    // Attach request details (IP, session ID) for auditing
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Tell Spring Security: "This request is authenticated as userDetails"
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }
}
```

---

### Security Configuration

Configure Spring Security to use **stateless** sessions and our JWT filter.

```java
package com.example.auth.config;

import com.example.auth.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless APIs (no cookies = no CSRF risk)
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            // STATELESS: Don't create server-side sessions - we use JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            // Run our JWT filter before the default username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### Authentication Controller

Exposes login, register, refresh, and logout endpoints.

```java
package com.example.auth.controller;

import com.example.auth.dto.AuthRequest;
import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}
```

---

### Authentication Service

Handles the business logic: validate credentials, create users, generate tokens, refresh tokens, and logout.

```java
package com.example.auth.service;

import com.example.auth.dto.AuthRequest;
import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.model.User;
import com.example.auth.model.Role;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .role(Role.USER)
            .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
            .token(jwtToken)
            .build();
    }

    public AuthResponse login(AuthRequest request) {
        // Validate username + password via Spring Security
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
            .token(jwtToken)
            .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String newAccessToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
            .token(newAccessToken)
            .build();
    }

    public void logout(String token) {
        // Client deletes token. For immediate revocation, use blacklist (see Layer 3)
    }
}
```

---

### DTOs

```java
// AuthRequest.java - Login payload
public record AuthRequest(
    @NotBlank String email,
    @NotBlank String password
) {}

// RegisterRequest.java - Registration payload
public record RegisterRequest(
    @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String firstName,
    @NotBlank String lastName
) {}

// AuthResponse.java - What we return after login/register
public record AuthResponse(
    String token,
    String refreshToken  // Optional, for refresh token pattern
) {}

// RefreshTokenRequest.java - For /refresh endpoint
public record RefreshTokenRequest(String refreshToken) {}
```

---

### Application Properties

```properties
# JWT Configuration
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=3600000
jwt.refresh-expiration=604800000
```

⚠️ **Never commit real secrets to Git.** Use environment variables or a secrets manager in production.

---

### Testing JWT Authentication

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLogin() throws Exception {
        AuthRequest request = new AuthRequest("user@example.com", "password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void testProtectedEndpoint() throws Exception {
        String token = "your-jwt-token-here";

        mockMvc.perform(get("/api/protected")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
```

---

## Layer 3 — Advanced Engineering Depth

### Refresh Token Pattern

**Problem:** Access tokens should expire quickly (e.g., 15 minutes) for security. But users shouldn't have to log in every 15 minutes.

**Solution:** Two-token system:
- **Access Token:** Short-lived (15 min), used for API calls
- **Refresh Token:** Long-lived (7 days), used only to get new access tokens

```java
@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration:604800000}") // 7 days
    private Long refreshExpiration;

    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .claim("type", "refresh")  // Distinguish from access token
            .compact();
    }

    public AuthResponse generateTokenPair(UserDetails userDetails) {
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = generateRefreshToken(userDetails);

        storeRefreshToken(userDetails.getUsername(), refreshToken);

        return AuthResponse.builder()
            .token(accessToken)
            .refreshToken(refreshToken)
            .build();
    }
}
```

---

### Token Storage Strategies

| **Strategy** | **Where** | **Pros** | **Cons** |
|-------------|-----------|----------|----------|
| **HttpOnly Cookie** | Cookie (server sets it) | XSS-safe (JS can't read it) | CSRF risk (use SameSite) |
| **localStorage** | Browser storage | Easy, survives refresh | XSS can steal it |
| **Memory** | JS variable | Most secure (gone on refresh) | User re-logs on refresh |

**HttpOnly Cookie Example (Web):**

```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
    AuthResponse response = authService.login(request);

    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .maxAge(Duration.ofDays(7))
        .path("/api/auth/refresh")
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(response);
}
```

**localStorage (SPA):**

```javascript
localStorage.setItem('accessToken', response.token);

fetch('/api/protected', {
    headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` }
});
```

---

### Token Blacklisting for Logout

**Problem:** JWT is stateless — we can't "delete" it. It's valid until `exp`.

**Solution:** Store revoked tokens in Redis (or DB) until they expire. Check every request.

```java
@Service
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    public void blacklistToken(String token, Date expiration) {
        long ttl = expiration.getTime() - System.currentTimeMillis();
        redisTemplate.opsForValue().set(
            "blacklist:" + token,
            "revoked",
            Duration.ofMillis(ttl)
        );
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }
}
```

Update the JWT filter to check the blacklist before proceeding.

---

### HS256 vs RS256

| **Aspect** | **HS256 (Symmetric)** | **RS256 (Asymmetric)** |
|-----------|------------------------|-------------------------|
| Keys | One secret (sign + verify) | Private key signs, public key verifies |
| Performance | Faster | Slower |
| Secret sharing | All verifiers need the secret | Only issuer needs private key |
| If key leaks | All tokens compromised | Only signing key matters; public key is safe to share |

**When to use:**  
- **HS256:** Single service, internal APIs  
- **RS256:** Multi-service, third-party integrations, when verifiers can't be trusted with the signing secret

```java
// RS256: Load private key and sign
public String generateTokenRS256(UserDetails userDetails) {
    return Jwts.builder()
        .subject(userDetails.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
        .compact();
}
```

---

### Common Security Pitfalls

1. **Weak secret** — Use at least 256 bits (32 chars) for HS256.
2. **Long expiration** — Access tokens: 15–60 min. Refresh: 7–30 days.
3. **Sensitive data in payload** — No passwords, SSN, credit cards.
4. **Skipping signature check** — Always verify. Never trust unsigned or `alg: none`.
5. **Storing in localStorage with XSS risk** — Prefer HttpOnly cookies or memory.
6. **No HTTPS** — Tokens can be intercepted over HTTP.
7. **No refresh token** — Long-lived access tokens increase theft impact.

---

### Performance Considerations

- **Keep payload small** — Don't stuff large objects. Use IDs; fetch from DB if needed.
- **Cache UserDetails** — `loadUserByUsername` hits the DB every request. Cache by username with short TTL.
- **Blacklist lookup** — Use Redis for O(1) blacklist checks.

```java
@Cacheable(value = "userDetails", key = "#username")
public UserDetails loadUserByUsername(String username) {
    // ...
}
```

---

## Layer 4 — Interview Mastery

### Q1: How does JWT authentication work?

**Answer:**  
1. User logs in with credentials.  
2. Server validates and generates a JWT (signed with a secret).  
3. Client stores the token and sends it with each request: `Authorization: Bearer <token>`.  
4. Server validates signature and expiration, extracts user info, and sets the security context.  
5. Request proceeds with the authenticated user.  

**Key:** Stateless, self-contained, signature ensures integrity.

---

### Q2: Session vs JWT — pros and cons?

**Session:**  
✅ Immediate revocation  
❌ Server storage, horizontal scaling harder, cookie-based, CSRF concerns  

**JWT:**  
✅ Stateless, scales horizontally, works across domains, mobile-friendly  
❌ No immediate revocation (need blacklist), payload size limit, more complex  

**Use sessions** for traditional web apps where you need instant revocation. **Use JWT** for APIs, SPAs, mobile, microservices.

---

### Q3: How do you handle token expiration?

- Short-lived access tokens (15–60 min)  
- Long-lived refresh tokens (7–30 days)  
- Client uses refresh token to get new access token  
- Proactive refresh: refresh before expiration when < 5 min left  

---

### Q4: What is a refresh token?

A long-lived token used to obtain new access tokens without re-login. Stored securely (e.g., HttpOnly cookie). Rotated on each use. Revocable.

---

### Q5: JWT security best practices?

- Strong secret (256+ bits for HS256)  
- Short expiration  
- HTTPS only  
- HttpOnly cookies or memory (avoid localStorage with XSS risk)  
- No sensitive data in payload  
- Validate signature and expiration on every request  
- Token blacklist for logout  
- Refresh token rotation  

---

### Q6: How do you handle logout with JWT?

- **Client-side:** Delete token. Simple, but token valid until expiry.  
- **Blacklist:** Add token to Redis until `exp`. Immediate revocation.  
- **Short exp + refresh revocation:** Revoke refresh token; access token expires soon.  

---

### Q7: What if a JWT is stolen?

- Revoke refresh token immediately  
- Blacklist the stolen token  
- Force password reset  
- Prevention: short expiration, token rotation, HTTPS, device fingerprinting  

---

### Q8: Can you revoke a JWT before expiration?

**Standard JWT:** No (stateless).  
**Options:**  
1. Blacklist — store revoked tokens until exp  
2. Short expiration + refresh revocation  
3. Token versioning — increment user version on logout; reject old version  
4. DB lookup — defeats statelessness  

---

### Q9: How do you test JWT authentication?

- **Unit:** Mock UserDetails, generate token, validate extraction and expiration  
- **Integration:** Login → get token → call protected endpoint with header  
- **Security:** Test invalid, expired, missing token → expect 401  

---

### Q10: JWT vs OAuth2 — when to use what?

- **JWT:** Token format. Use for your own APIs, internal services.  
- **OAuth2:** Authorization framework (flows, grant types). Use for third-party access, delegated auth.  
- **Together:** OAuth2 can use JWT as the token format (e.g., OpenID Connect).  

---

## Summary

**Key Takeaways:**

1. **JWT is stateless** — No server-side session storage; any server can verify.
2. **Three parts** — Header (metadata), Payload (claims), Signature (anti-tamper).
3. **Self-contained** — Token carries identity; no DB lookup needed (unless you want fresh roles).
4. **Security** — Strong secret, short expiration, secure storage, HTTPS.
5. **Refresh tokens** — Bridge security (short access token) and UX (no constant re-login).
6. **Blacklisting** — Enables logout and immediate revocation.
7. **Best practices** — Validate always, rotate refresh tokens, monitor, rate limit.

**Common Pitfalls:**

- ❌ Sensitive data in payload  
- ❌ Weak secrets  
- ❌ Long expiration  
- ❌ Skipping signature validation  
- ❌ Insecure storage (localStorage with XSS)  
- ❌ No refresh token flow  

**Production Checklist:**

- ✅ Strong secret (256+ bits)  
- ✅ Short access token (15–60 min)  
- ✅ Refresh token + rotation  
- ✅ Token blacklist for logout  
- ✅ HTTPS only  
- ✅ Secure token storage  
- ✅ Proper error handling  
- ✅ Monitoring and logging  

---

**Next Steps:**
- Implement JWT in your project  
- Add refresh token flow  
- Set up token blacklisting  
- Test security scenarios (invalid, expired, stolen token)  
- Monitor token usage and failures  

[← Back to Index](00-README.md) | [Previous: Spring Security](29-Spring-Security.md) | [Next: Caching →](31-Caching.md)
