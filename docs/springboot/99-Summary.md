# Summary & Cheat Sheet

[← Back to Index](00-README.md) | [Previous: Docker & Deployment](37-Docker-Deployment.md)

---

## Quick Reference for Interviews

### Core Spring Concepts

**IoC/DI**
- IoC: Framework controls object lifecycle
- DI: Dependencies injected, not created
- Constructor injection preferred

**Bean Scopes**
- singleton (default): One instance
- prototype: New instance each time
- request/session: Web scopes

**Bean Lifecycle**
1. Instantiation
2. Dependency Injection
3. @PostConstruct
4. Ready
5. @PreDestroy

### Key Annotations

**Component Annotations**
- @Component - Generic
- @Service - Business logic
- @Repository - Data access
- @Controller - Web MVC
- @RestController - REST API
- @Configuration - Config class

**Injection**
- @Autowired - Auto-wire dependencies
- @Qualifier - Specify which bean
- @Value - Inject property values
- @Primary - Default bean

**Web**
- @GetMapping, @PostMapping, @PutMapping, @DeleteMapping
- @PathVariable - URL path segment
- @RequestParam - Query parameter
- @RequestBody - Request body
- @ResponseStatus - HTTP status

**Data**
- @Entity - JPA entity
- @Id, @GeneratedValue - Primary key
- @Transactional - Transaction boundary
- @Query - Custom query

**Validation**
- @Valid, @Validated - Enable validation
- @NotNull, @NotBlank, @Size, @Email

### Common Interview Questions

**1. How does @Transactional work?**
Spring creates a proxy that wraps the method with transaction begin/commit/rollback.
Self-invocation bypasses proxy!

**2. N+1 Problem**
Loading N entities triggers N additional queries.
Solutions: JOIN FETCH, @EntityGraph, @BatchSize

**3. @Controller vs @RestController**
@RestController = @Controller + @ResponseBody
Returns data directly, not view names.

**4. Lazy vs Eager Loading**
LAZY: Load on access (default for collections)
EAGER: Load immediately
Prefer LAZY, fetch explicitly when needed.

**5. How does auto-configuration work?**
@EnableAutoConfiguration reads spring.factories, evaluates @Conditional annotations, creates beans automatically.

### Configuration Priority (Highest First)
1. Command line args
2. System properties
3. Environment variables
4. application-{profile}.properties
5. application.properties

### Status Codes to Know
- 200 OK
- 201 Created
- 204 No Content
- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 500 Internal Server Error

### Production Essentials
- Use @Transactional(readOnly = true) for queries
- Configure connection pooling (HikariCP)
- Enable compression
- Use DTOs (not entities in API)
- Centralize exception handling
- Log with correlation IDs
- Health checks with Actuator

### Testing Quick Reference
- @SpringBootTest - Full context
- @WebMvcTest - Web layer only
- @DataJpaTest - Data layer only
- @MockBean - Mock Spring bean
- Testcontainers for real database

---

## Study Checklist

### Must-Know for Interviews
- [ ] IoC and DI explanation
- [ ] Bean lifecycle
- [ ] @Transactional (including pitfalls)
- [ ] N+1 problem and solutions
- [ ] Hibernate entity states
- [ ] Spring Security filter chain
- [ ] REST best practices
- [ ] Exception handling patterns

### Good to Know
- [ ] Auto-configuration internals
- [ ] Bean scopes and when to use
- [ ] Caching strategies
- [ ] Microservices patterns
- [ ] Docker basics

---

*Good luck with your interviews!*

---

[← Back to Index](00-README.md) | [Previous: Docker & Deployment](37-Docker-Deployment.md)
