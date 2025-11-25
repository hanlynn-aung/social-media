# Authorization Best Practices

## Principles Applied

### 1. Principle of Least Privilege
- Users get minimum required permissions
- Resources are protected by default
- Explicit grants required for access
- Admin role needed for system-wide operations

### 2. Defense in Depth
- **Layer 1**: Spring Security authentication
- **Layer 2**: Method-level annotations (@RequireAdminRole, etc.)
- **Layer 3**: Aspect-based ownership checks
- **Layer 4**: Input validation

### 3. Fail-Secure
- Deny by default
- Explicit allow required
- Clear error messages for debugging
- Proper HTTP status codes for clients

## Usage Guidelines for Developers

### Adding Authorization to New Endpoints

#### Step 1: Determine Authorization Level
```java
// Public endpoint (no authorization needed)
@GetMapping("/public-data")
public ResponseEntity<?> getPublicData() { ... }

// User endpoint (any authenticated user)
@GetMapping("/user-data")
@RequireUserRole
public ResponseEntity<?> getUserData() { ... }

// Shop admin endpoint
@PostMapping("/shop-data")
@RequireShopAdminRole
public ResponseEntity<?> createShopData() { ... }

// Admin endpoint
@DeleteMapping("/{id}")
@RequireAdminRole
public ResponseEntity<?> deleteData(@PathVariable Long id) { ... }
```

#### Step 2: Add Ownership Checks if Needed
```java
@PutMapping("/{id}")
@RequireUserRole
@RequireResourceOwner("id")  // Validates user owns this resource
public ResponseEntity<?> updateMyData(@PathVariable Long id, @RequestBody Data data) {
    // Implementation here
}
```

#### Step 3: Validate Input
```java
@PostMapping
@RequireUserRole
public ResponseEntity<?> createData(@Valid @RequestBody Data data) {
    // Validation handled by @Valid
    
    // Additional business logic validation
    if (!isValidBusinessRule(data)) {
        return ResponseEntity.badRequest()
                .body(ResponseUtil.buildErrorResponse("Invalid data"));
    }
    
    // Proceed with creation
}
```

### Accessing Current User Information

```java
// In any controller with AuthorizationHelper injected
@Autowired
private AuthorizationHelper authorizationHelper;

public ResponseEntity<?> someMethod() {
    // Get current user
    Optional<User> currentUser = authorizationHelper.getCurrentUser();
    
    // Get current user ID
    Optional<Long> userId = authorizationHelper.getCurrentUserId();
    
    // Get current username
    Optional<String> username = authorizationHelper.getCurrentUsername();
    
    // Check roles
    boolean isAdmin = authorizationHelper.isAdmin();
    boolean isShopAdmin = authorizationHelper.isShopAdmin();
    
    // Check ownership
    if (authorizationHelper.canModifyResource(userId)) {
        // User owns resource or is admin
    }
}
```

## Common Patterns

### Pattern 1: User Self-Service with Admin Override
```java
@GetMapping("/user/{userId}/data")
@RequireUserRole
public ResponseEntity<?> getUserData(@PathVariable Long userId) {
    // Users can view their own data, admins can view any
    if (!authorizationHelper.canModifyResource(userId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseUtil.buildErrorResponse("Access Denied"));
    }
    
    // Retrieve and return data
    return ResponseEntity.ok(getData(userId));
}
```

### Pattern 2: Resource Owner Enforcement
```java
@PutMapping("/{id}")
@RequireUserRole
@RequireResourceOwner("id")  // Automatic check
public ResponseEntity<?> updateResource(@PathVariable Long id, @RequestBody Data data) {
    // By the time we reach here, ownership is guaranteed
    // (unless user is admin, in which case check passes)
    
    return ResponseEntity.ok(updateData(id, data));
}
```

### Pattern 3: Shop Admin Operations
```java
@PostMapping("/shop/{shopId}/data")
@RequireShopAdminRole
public ResponseEntity<?> createShopData(@PathVariable Long shopId, @RequestBody Data data) {
    try {
        // Verify shop exists and user has permission
        Shop shop = getShop(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        
        // Optional: verify user is shop owner
        // Only admins can skip this check
        if (!authorizationHelper.isAdmin()) {
            if (!isShopOwner(shop)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseUtil.buildErrorResponse("You can only manage your own shop"));
            }
        }
        
        return ResponseEntity.ok(createData(shopId, data));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
                .body(ResponseUtil.buildErrorResponse(e.getMessage()));
    }
}
```

### Pattern 4: Sensitive Operations
```java
@PostMapping("/sensitive-action")
@RequireAdminRole  // Only admins can perform
public ResponseEntity<?> performSensitiveAction(@RequestBody SensitiveRequest request) {
    try {
        // Log the action for audit trail
        logAudit("SENSITIVE_ACTION", authorizationHelper.getCurrentUsername().orElse("unknown"));
        
        // Perform action
        Result result = executeSensitiveAction(request);
        
        return ResponseEntity.ok(ResponseUtil.buildSuccessResponse("Action performed"));
    } catch (Exception e) {
        // Log error for investigation
        logError("SENSITIVE_ACTION_FAILED", e);
        return ResponseEntity.internalServerError()
                .body(ResponseUtil.buildErrorResponse("Action failed"));
    }
}
```

## Security Checklist for New Features

- [ ] Authorization annotation added (@RequireAdminRole, @RequireUserRole, @RequireShopAdminRole)
- [ ] Ownership checks added where applicable (@RequireResourceOwner)
- [ ] Input validation implemented (@Valid, manual checks)
- [ ] Error responses use ResponseUtil
- [ ] Proper HTTP status codes used
- [ ] Sensitive operations logged
- [ ] Database constraints enforced
- [ ] SQL injection prevented (use repository methods)
- [ ] XSS prevention (input validation)
- [ ] CSRF tokens handled by Spring Security

## Common Security Mistakes to Avoid

### ❌ Mistake 1: Checking Authorization in Business Logic
```java
// DON'T DO THIS
public ResponseEntity<?> updateUser(Long userId, User data) {
    // Authorization check mixed with business logic
    if (!getCurrentUserId().equals(userId)) {
        return ResponseEntity.status(403).build();
    }
    // ...
}
```

### ✅ Correct Approach
```java
// DO THIS
@PutMapping("/{id}")
@RequireUserRole
@RequireResourceOwner("id")  // Authorization declarative
public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User data) {
    // Authorization already checked by annotation/aspect
    // Just implement business logic
    return ResponseEntity.ok(userService.updateUser(id, data));
}
```

### ❌ Mistake 2: Insufficient Input Validation
```java
// DON'T DO THIS
@PostMapping
public ResponseEntity<?> createData(@RequestBody Data data) {
    // No validation - could accept null/invalid values
    return ResponseEntity.ok(dataService.create(data));
}
```

### ✅ Correct Approach
```java
// DO THIS
@PostMapping
@RequireUserRole
public ResponseEntity<?> createData(@Valid @RequestBody Data data) {
    // @Valid ensures data conforms to constraints
    
    // Additional business validation
    if (!isValidForBusiness(data)) {
        return ResponseEntity.badRequest()
                .body(ResponseUtil.buildErrorResponse("Invalid data for business rules"));
    }
    
    return ResponseEntity.ok(dataService.create(data));
}
```

### ❌ Mistake 3: Trusting User Input for Authorization
```java
// DON'T DO THIS
@PutMapping
public ResponseEntity<?> updateData(@RequestBody UpdateRequest request) {
    // User provides userId - what if they change it?
    Long userId = request.getUserId();
    
    return ResponseEntity.ok(dataService.update(userId, request.getData()));
}
```

### ✅ Correct Approach
```java
// DO THIS
@PutMapping("/{id}")
@RequireUserRole
@RequireResourceOwner("id")
public ResponseEntity<?> updateData(@PathVariable Long id, @RequestBody UpdateRequest request) {
    // Path variable comes from URL, can't be modified by user
    // @RequireResourceOwner ensures ownership
    
    return ResponseEntity.ok(dataService.update(id, request.getData()));
}
```

### ❌ Mistake 4: Inadequate Error Messages
```java
// DON'T DO THIS
return ResponseEntity.status(403).body("Error");  // Vague error
```

### ✅ Correct Approach
```java
// DO THIS
return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ResponseUtil.buildErrorResponse("You can only modify your own profile"));
```

## Testing Authorization

### Unit Tests
```java
@Test
public void testUserCanOnlyViewOwnProfile() {
    // Given: User with ID 1 trying to view profile of user 2
    Long currentUserId = 1L;
    Long targetUserId = 2L;
    
    // When/Then: Should fail
    assertThrows(AccessDeniedException.class, 
        () -> userController.getUserById(targetUserId));
}

@Test
public void testAdminCanViewAnyProfile() {
    // Given: Admin user trying to view any profile
    // Setup: Mock authentication with ADMIN role
    
    // When: Admin views user profile
    ResponseEntity<?> response = userController.getUserById(2L);
    
    // Then: Should succeed
    assertEquals(HttpStatus.OK, response.getStatusCode());
}
```

### Integration Tests
```java
@Test
public void testUnauthorizedAccessReturns403() {
    RestAssuredMockMvc
        .given()
            .auth().oauth2(userToken)
        .when()
            .put("/api/users/999")  // Different user's ID
        .then()
            .statusCode(403);
}

@Test
public void testAdminBypassOwnershipChecks() {
    RestAssuredMockMvc
        .given()
            .auth().oauth2(adminToken)
        .when()
            .put("/api/users/999")  // Any user's ID
        .then()
            .statusCode(200);
}
```

## Monitoring and Logging

### Log Authorization Failures
```java
@Component
@Aspect
public class AuthorizationAuditAspect {
    
    @Around("@annotation(com.example.socialmedia.annotation.RequireAdminRole)")
    public Object auditAdminAccess(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (AccessDeniedException e) {
            logger.warn("Unauthorized admin access attempt by: {}", 
                    authorizationHelper.getCurrentUsername());
            throw e;
        }
    }
}
```

### Monitor for Suspicious Activity
- Multiple failed authorization attempts
- Attempts to access high-privilege endpoints
- Bulk data access patterns
- Unusual API usage patterns

## Performance Considerations

1. **Caching**: Cache role/permission lookups if frequently checked
2. **Database**: Use indexes on user_id, role fields
3. **AspectJ**: Minimize aspect overhead by using precise pointcuts
4. **Security Context**: Avoid repeated context lookups

## Regular Security Audits

1. **Code Review**: Authorization logic changes
2. **Penetration Testing**: Authorization bypass attempts
3. **Compliance Check**: Ensure role assignments proper
4. **Access Logs**: Review admin activities
5. **Permission Creep**: Identify unnecessary privileges

## References

- OWASP Authorization Cheat Sheet
- Spring Security Documentation
- AspectJ Programming Guide
- JWT Best Practices

