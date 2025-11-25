# Authorization Enhancement Guide

## Overview
This document describes the enhanced authorization system implemented across all controllers in the Social Media API.

## Authorization Components

### 1. **AuthorizationHelper** (`com.example.socialmedia.security.AuthorizationHelper`)
Central utility component for authorization checks with methods:

- `getCurrentUser()` - Get current authenticated user
- `getCurrentUserId()` - Get current user ID
- `getCurrentUsername()` - Get current username
- `hasRole(String role)` - Check if user has specific role
- `isAdmin()` - Check if user is admin
- `isShopAdmin()` - Check if user is shop admin or admin
- `canModifyResource(Long resourceUserId)` - Check if user owns resource or is admin
- `isResourceOwner(Long resourceUserId)` - Check if user is resource owner
- `isAuthenticated()` - Check if user is authenticated

### 2. **Role-Based Annotations**

#### `@RequireUserRole`
- Allows: USER, SHOP_ADMIN, ADMIN roles
- Use for: General user operations

```java
@GetMapping("/{id}")
@RequireUserRole
public ResponseEntity<?> getUserById(@PathVariable Long id) { ... }
```

#### `@RequireShopAdminRole`
- Allows: SHOP_ADMIN, ADMIN roles
- Use for: Shop management operations

```java
@PostMapping("/shop/{shopId}")
@RequireShopAdminRole
public ResponseEntity<?> createPost(@PathVariable Long shopId, @RequestBody Post post) { ... }
```

#### `@RequireAdminRole`
- Allows: ADMIN role only
- Use for: System administration operations

```java
@GetMapping
@RequireAdminRole
public List<User> getAllUsers() { ... }
```

#### `@RequireResourceOwner` (NEW)
- Validates resource ownership
- Prevents users from modifying other users' resources
- Allows admins to bypass ownership checks

```java
@PutMapping("/{id}")
@RequireUserRole
@RequireResourceOwner("id")
public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) { ... }
```

### 3. **ResourceOwnershipAspect**
Automatically enforces `@RequireResourceOwner` annotations:
- Extracts path variable value
- Compares with current user ID
- Returns 403 Forbidden if unauthorized
- Allows admins to bypass

## Controller Authorization Enhancements

### UserController
| Endpoint | Method | Authorization | Notes |
|----------|--------|---------------|-------|
| `/api/users` | GET | @RequireAdminRole | Only admins can list all users |
| `/api/users/{id}` | GET | @RequireUserRole + Ownership | Users see own profile, admins see all |
| `/api/users` | POST | @RequireAdminRole | Only admins can create users |
| `/api/users/{id}` | PUT | @RequireUserRole + Ownership | Users modify own, admins modify any |
| `/api/users/{id}/change-password` | PUT | @RequireUserRole + Ownership | Users change own, admins change any |
| `/api/users/{id}` | DELETE | @RequireAdminRole | Only admins can delete |

### MessageController
| Endpoint | Method | Authorization |
|----------|--------|---------------|
| `/api/messages/shop/{shopId}` | GET | @RequireUserRole |
| `/api/messages/user/{userId}/shop/{shopId}` | POST | @RequireUserRole + Ownership |
| `/api/messages/{id}` | DELETE | @RequireUserRole |
| `/api/messages/{id}/hide` | PUT | @RequireUserRole |

### FileUploadController
| Endpoint | Method | Authorization |
|----------|--------|---------------|
| `/api/uploads` | POST | @RequireUserRole |
| `/api/uploads/files/{fileName}` | GET | Public |

**Security Features:**
- File size validation (10MB max)
- File type whitelist (.jpg, .jpeg, .png, .gif, .pdf, .doc, .docx, .xls, .xlsx)
- Path traversal prevention
- Empty filename validation

### NotificationController
| Endpoint | Method | Authorization |
|----------|--------|---------------|
| `/api/notifications/user/{userId}` | GET | @RequireUserRole + Ownership |
| `/api/notifications/{id}/read` | PUT | @RequireUserRole |
| `/api/notifications/broadcast` | POST | @RequireAdminRole |

### ReviewController
| Endpoint | Method | Authorization |
|----------|--------|---------------|
| `/api/reviews/shop/{shopId}` | GET | Public |
| `/api/reviews/user/{userId}/shop/{shopId}` | POST | @RequireUserRole + Ownership |

### ReservationController
| Endpoint | Method | Authorization |
|----------|--------|---------------|
| `/api/reservations/user/{userId}` | GET | @RequireUserRole + Ownership |
| `/api/reservations/shop/{shopId}` | GET | @RequireShopAdminRole |
| `/api/reservations/user/{userId}/shop/{shopId}` | POST | @RequireUserRole + Ownership |
| `/api/reservations/{id}/status` | PUT | @RequireShopAdminRole |
| `/api/reservations/{id}/payment` | PUT | @RequireUserRole |

### PostController
| Endpoint | Method | Authorization |
|----------|--------|---------------|
| `/api/posts` | GET | Public |
| `/api/posts/shop/{shopId}` | GET | Public |
| `/api/posts/shop/{shopId}` | POST | @RequireShopAdminRole |
| `/api/posts/{id}` | DELETE | @RequireShopAdminRole |

### ShopController
| Endpoint | Method | Authorization | Notes |
|----------|--------|---------------|-------|
| `/api/shops` | GET | Public | |
| `/api/shops/{id}` | GET | Public | |
| `/api/shops/search` | GET | Public | Query validation added |
| `/api/shops/nearby` | GET | Public | Coordinate validation added |
| `/api/shops/user/{userId}` | POST | @RequireShopAdminRole + Ownership | Creators manage own shops |
| `/api/shops/{id}` | PUT | @RequireShopAdminRole + Ownership | Owners update own shops |
| `/api/shops/{id}` | DELETE | @RequireAdminRole | Only admins can delete |

### AuthController
| Endpoint | Method | Authorization | Notes |
|----------|--------|---------------|-------|
| `/api/auth/signin` | POST | Public | |
| `/api/auth/signup` | POST | Public | Only admins can assign admin roles |
| `/api/auth/social-login` | POST | Public | |
| `/api/auth/logout` | POST | Public | |
| `/api/auth/reset-password-request` | POST | Public | Email enumeration prevented |

## Input Validation Enhancements

### File Upload
- Maximum file size: 10MB
- Allowed extensions: .jpg, .jpeg, .png, .gif, .pdf, .doc, .docx, .xls, .xlsx
- Path traversal prevention
- Empty file rejection

### Shop Endpoints
- Search term validation (non-empty)
- Coordinate validation (latitude: -90 to 90, longitude: -180 to 180)
- Radius validation (must be positive)

### Authentication
- Password strength: minimum 6 characters
- Email format validation
- Prevents admin role assignment by non-admins

## Security Best Practices Implemented

1. **Resource Ownership Validation**
   - Users can only modify their own resources
   - Admins can override ownership checks
   - Aspect-based enforcement for consistency

2. **Email Enumeration Prevention**
   - Password reset endpoint doesn't reveal email existence
   - Returns same response for existing and non-existing emails

3. **Input Validation**
   - File upload: size, type, name validation
   - Geographic queries: coordinate range validation
   - Search queries: non-empty validation
   - Password: minimum length enforcement

4. **Authorization Layering**
   - Spring Security: Authentication and basic authorization
   - Annotations: Role-based access control
   - Aspect: Resource ownership enforcement

5. **Consistent Error Responses**
   - 401 Unauthorized: Invalid credentials
   - 403 Forbidden: Insufficient permissions
   - 404 Not Found: Resource doesn't exist
   - 409 Conflict: Resource already exists
   - 400 Bad Request: Invalid input

## Usage Examples

### Check Current User
```java
authorizationHelper.getCurrentUser().ifPresent(user -> {
    System.out.println("Current user: " + user.getUsername());
});
```

### Verify Admin Status
```java
if (authorizationHelper.isAdmin()) {
    // Perform admin-only operation
}
```

### Check Resource Ownership
```java
if (!authorizationHelper.canModifyResource(userId)) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ResponseUtil.buildErrorResponse("Access Denied"));
}
```

### Automatic Ownership Check
```java
@PutMapping("/{id}")
@RequireUserRole
@RequireResourceOwner("id")  // Automatically checks if current user owns resource with this ID
public ResponseEntity<?> updateResource(@PathVariable Long id, @RequestBody Data data) {
    // If we reach here, ownership is guaranteed
}
```

## Testing Authorization

### Sample Requests

#### Admin List All Users
```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

#### User View Own Profile
```bash
curl -X GET http://localhost:8080/api/users/123 \
  -H "Authorization: Bearer <USER_TOKEN>"
```

#### User Cannot View Others' Profiles
```bash
# This will return 403 Forbidden
curl -X GET http://localhost:8080/api/users/456 \
  -H "Authorization: Bearer <USER_123_TOKEN>"
```

#### Admin Can View Any Profile
```bash
# Admin can view any user's profile
curl -X GET http://localhost:8080/api/users/456 \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

#### Unauthorized File Upload
```bash
# This will return 401 Unauthorized
curl -X POST http://localhost:8080/api/uploads \
  -F "file=@document.pdf"
```

## Migration Notes

- All controllers now have consistent authorization
- Resource ownership checks are enforced via aspect
- Error responses follow consistent format
- Input validation is comprehensive
- No breaking changes to API contracts

## Future Enhancements

1. Rate limiting per user/role
2. Audit logging for sensitive operations
3. Fine-grained permissions (resource-level ACL)
4. Token-based request signing
5. IP whitelisting for admin endpoints
