# Quick Start Guide

## 1. Build the Project
```bash
cd d:\projects\social-media-api
mvn clean package -DskipTests
```

## 2. Run the Application
```bash
# Default port (8080 - may be in use)
java -jar target/social-media-api-0.0.1-SNAPSHOT.jar

# Alternative port (9090)
java -jar -Dserver.port=9090 target/social-media-api-0.0.1-SNAPSHOT.jar
```

## 3. Access the API

### Swagger UI (Documentation)
```
http://localhost:9090/swagger-ui/html
```

### H2 Console (Database)
```
http://localhost:9090/h2-console
```

## 4. Sample API Calls

### Register a User
```bash
curl -X POST http://localhost:9090/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "password123",
    "phoneNumber": "+95912345678"
  }'
```

### Login
```bash
curl -X POST http://localhost:9090/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "roles": ["USER"]
}
```

### Use JWT Token in Requests
```bash
# Extract token from login response and use it
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Get your own user profile (works)
curl -X GET http://localhost:9090/api/users/1 \
  -H "Authorization: Bearer $TOKEN"

# Try to get another user's profile (fails with 403)
curl -X GET http://localhost:9090/api/users/2 \
  -H "Authorization: Bearer $TOKEN"
```

### Public Endpoints (No Auth Required)
```bash
# Get all shops
curl -X GET http://localhost:9090/api/shops

# Search shops
curl -X GET "http://localhost:9090/api/shops/search?name=Restaurant"

# Find nearby shops
curl -X GET "http://localhost:9090/api/shops/nearby?lat=16.8661&lng=96.1951&radius=5"

# Get shop reviews
curl -X GET http://localhost:9090/api/reviews/shop/1
```

### Protected Endpoints (Require Auth)
```bash
TOKEN="your_jwt_token_here"

# Upload a file
curl -X POST http://localhost:9090/api/uploads \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@document.pdf"

# Create a shop (requires SHOP_ADMIN role)
curl -X POST http://localhost:9090/api/shops/user/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Shop",
    "description": "A great shop",
    "address": "123 Main St",
    "latitude": 16.8661,
    "longitude": 96.1951
  }'
```

## 5. Authorization Levels

### Public Endpoints
- `GET /api/shops` - List all shops
- `GET /api/shops/{id}` - Get shop details
- `GET /api/reviews/shop/{id}` - Get shop reviews
- `POST /api/auth/**` - All auth endpoints

### User Endpoints (@RequireUserRole)
- `GET /api/users/{id}` - View own profile (or admin can view any)
- `PUT /api/users/{id}` - Update own profile
- `POST /api/uploads` - Upload files
- `POST /api/messages` - Send messages
- `POST /api/reservations` - Create reservations
- `POST /api/reviews` - Post reviews

### Shop Admin Endpoints (@RequireShopAdminRole)
- `POST /api/shops/user/{userId}` - Create shop
- `PUT /api/shops/{id}` - Update shop
- `POST /api/posts/shop/{shopId}` - Create posts
- `GET /api/reservations/shop/{shopId}` - View shop reservations

### Admin Endpoints (@RequireAdminRole)
- `GET /api/users` - List all users
- `POST /api/users` - Create user
- `DELETE /api/users/{id}` - Delete user
- `DELETE /api/shops/{id}` - Delete shop
- `POST /api/notifications/broadcast` - Send broadcast notification

## 6. Common Errors & Solutions

### 401 Unauthorized
**Cause:** Missing or invalid JWT token
```bash
# Solution: Include valid token in header
curl -X GET http://localhost:9090/api/users/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 403 Forbidden
**Cause:** User lacks required role or doesn't own resource
```bash
# User cannot access another user's profile
curl -X GET http://localhost:9090/api/users/2 \
  -H "Authorization: Bearer $USER_TOKEN"
# Response: 403 Forbidden - "Access Denied: You can only modify your own resources"
```

### 404 Not Found
**Cause:** Resource doesn't exist
```bash
# Response when user/shop doesn't exist
curl -X GET http://localhost:9090/api/users/9999 \
  -H "Authorization: Bearer $TOKEN"
# Response: 404 Not Found
```

### 409 Conflict
**Cause:** Resource already exists (e.g., duplicate username)
```bash
# Response when trying to register with existing username
curl -X POST http://localhost:9090/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "existing_user",
    "email": "new@example.com",
    "password": "password123"
  }'
# Response: 409 Conflict - "Username is already taken"
```

## 7. Key Features Implemented

✅ **Role-Based Access Control**
- USER, SHOP_ADMIN, ADMIN roles
- Annotations for easy enforcement

✅ **Resource Ownership Validation**
- Users can only modify their own resources
- Admins can override

✅ **Input Validation**
- File size limits (10MB max)
- File type whitelist
- Email format validation
- Password strength requirements

✅ **Security Features**
- JWT token authentication
- CSRF protection
- CORS enabled
- Path traversal prevention
- Email enumeration prevention

## 8. Development Tips

### Adding Authorization to New Endpoints

```java
@PutMapping("/{id}")
@RequireUserRole  // Require authenticated user
@RequireResourceOwner("id")  // Validate ownership
public ResponseEntity<?> updateResource(
    @PathVariable Long id,
    @RequestBody UpdateRequest request) {
    // Code automatically checks ownership
    // Users can only modify own resources
    // Admins can modify any
}
```

### Checking Current User in Code

```java
@Autowired
private AuthorizationHelper authHelper;

public void someMethod() {
    // Get current user info
    authHelper.getCurrentUser().ifPresent(user -> {
        System.out.println("Current user: " + user.getUsername());
    });
    
    // Check roles
    if (authHelper.isAdmin()) {
        // Admin-only logic
    }
    
    // Check ownership
    if (authHelper.canModifyResource(userId)) {
        // User owns resource or is admin
    }
}
```

## 9. Testing Authorization

### Unit Testing Example
```java
@Test
public void testUserCanOnlyViewOwnProfile() {
    // User 1 trying to view user 2's profile
    assertThrows(AccessDeniedException.class, 
        () -> userController.getUserById(2L));
}

@Test
public void testAdminCanViewAnyProfile() {
    // Admin viewing any user
    ResponseEntity<?> response = userController.getUserById(2L);
    assertEquals(HttpStatus.OK, response.getStatusCode());
}
```

## 10. Documentation

For complete documentation, see:
- **AUTHORIZATION_GUIDE.md** - Full endpoint reference
- **AUTHORIZATION_BEST_PRACTICES.md** - Development guidelines
- **PROJECT_STATUS.md** - Project overview

---

Need help? Check the documentation files or examine the source code in:
- `src/main/java/com/example/socialmedia/security/AuthorizationHelper.java`
- `src/main/java/com/example/socialmedia/security/ResourceOwnershipAspect.java`
