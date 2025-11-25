# Advanced Security Features

## Overview

This document describes the advanced security features implemented for the Social Media API, including rate limiting, audit logging, fine-grained permissions, request signing, and IP whitelisting.

## 1. Rate Limiting

### Overview
Implements per-user and per-role rate limiting using Bucket4j library to prevent abuse and ensure fair resource usage.

### Configuration

Enable in `application.properties`:
```properties
# Rate limiting is enabled by default
# No configuration needed - uses sensible defaults
```

### Rate Limits by Role

| Role | Requests/Minute | Purpose |
|------|-----------------|---------|
| Anonymous | 10 | Unauthenticated users |
| USER | 60 | Regular authenticated users |
| SHOP_ADMIN | 100 | Shop administrators |
| ADMIN | 500 | System administrators |

### Endpoint-Specific Limits

| Endpoint | Requests/Minute |
|----------|-----------------|
| `/api/uploads` | 5 |
| `/api/auth/*` | 5 |
| `/api/messages` | 30 |

### Usage

Rate limiting is automatic and transparent. Clients receive rate limit headers in responses:

```bash
curl -X GET http://localhost:9090/api/shops \
  -H "Authorization: Bearer $TOKEN"
```

Response headers:
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 55
```

### Error Response (Rate Limit Exceeded)

```json
{
  "error": "Rate limit exceeded. Maximum 60 requests per minute."
}
```

HTTP Status: `429 Too Many Requests`

### Implementation Details

**Class**: `RateLimitingConfig`
- Creates and manages token buckets per user/role
- Uses Bucket4j for efficient rate limiting

**Filter**: `RateLimitingFilter`
- Intercepts all requests
- Checks rate limit before processing
- Adds headers to response

### Testing Rate Limiting

```bash
# This will work
for i in {1..60}; do
  curl -X GET http://localhost:9090/api/shops \
    -H "Authorization: Bearer $TOKEN"
done

# This will fail with 429
curl -X GET http://localhost:9090/api/shops \
  -H "Authorization: Bearer $TOKEN"
```

---

## 2. Audit Logging

### Overview
Captures all sensitive operations in MongoDB for compliance, security investigation, and forensic analysis.

### Audited Events

**User Management**
- User creation
- User update
- User deletion
- Password changes

**Shop Management**
- Shop creation
- Shop update
- Shop deletion

**Security Events**
- Access denied (403)
- Unauthorized attempts (401)
- Failed operations

### Audit Log Structure

```json
{
  "id": "507f1f77bcf86cd799439011",
  "userId": 123,
  "username": "john_doe",
  "action": "USER_MANAGEMENT_UPDATE",
  "resource": "UserController",
  "resourceId": "456",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "status": "SUCCESS",
  "details": "User profile updated",
  "timestamp": "2025-11-25T10:40:00+06:30"
}
```

### Accessing Audit Logs

Using the AuditLogRepository:

```java
@Autowired
private AuditLogRepository auditLogRepository;

// Find logs by user
List<AuditLog> userLogs = auditLogRepository.findByUserId(123L);

// Find failed operations
List<AuditLog> failures = auditLogRepository.findByStatus("FAILURE");

// Find denied access attempts
List<AuditLog> deniedAccess = auditLogRepository.findDeniedAccessByUser(123L);

// Find logs in date range
List<AuditLog> recentLogs = auditLogRepository.findByTimestampBetween(
    ZonedDateTime.now().minusHours(24),
    ZonedDateTime.now()
);
```

### Implementation Details

**Entity**: `AuditLog`
- MongoDB document in `audit_logs` collection
- Stores all audit information with timestamp

**Aspect**: `AuditAspect`
- Intercepts sensitive method calls
- Logs before/after operation
- Captures exceptions

**Repository**: `AuditLogRepository`
- Query interface for audit logs
- Various search methods

### Usage Examples

```bash
# Trigger an audited event
curl -X PUT http://localhost:9090/api/users/123 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"newname","email":"new@example.com"}'

# Check logs in MongoDB
db.audit_logs.find({"userId": 123})
```

---

## 3. Fine-Grained Permissions (Resource-Level ACL)

### Overview
Extends the role-based authorization with resource-level access control for more granular permission management.

### Permission Model

**Resources**
- USER
- SHOP
- POST
- MESSAGE
- RESERVATION
- REVIEW
- NOTIFICATION

**Actions**
- READ
- CREATE
- UPDATE
- DELETE
- SHARE
- EXECUTE

### Default Role Permissions

#### ADMIN
- All permissions on all resources

#### SHOP_ADMIN
- SHOP: READ, UPDATE
- POST: CREATE, UPDATE, DELETE
- RESERVATION: READ, UPDATE

#### USER
- USER: READ (own), UPDATE (own)
- POST: READ, CREATE
- MESSAGE: CREATE, READ
- RESERVATION: CREATE
- REVIEW: CREATE

#### ANONYMOUS
- USER: READ (none)
- SHOP: READ
- POST: READ
- REVIEW: READ

### Usage

Inject `PermissionChecker` in controller:

```java
@Autowired
private PermissionChecker permissionChecker;

@GetMapping("/{id}")
public ResponseEntity<?> getPost(@PathVariable Long id) {
    if (!permissionChecker.canRead("POST")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    // Proceed with retrieval
}

@DeleteMapping("/{id}")
public ResponseEntity<?> deletePost(@PathVariable Long id) {
    if (!permissionChecker.canDelete("POST")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    // Proceed with deletion
}
```

### Get User Permissions

```java
// Get all permissions for a resource
Map<String, Boolean> postPerms = permissionChecker.getResourcePermissions("POST");
// Result: {READ: true, CREATE: true, UPDATE: false, DELETE: false, ...}

// Check specific permission
boolean canCreate = permissionChecker.canCreate("SHOP");

// Check multiple permissions
boolean hasReadOrWrite = permissionChecker.hasAnyPermission("SHOP", "READ", "CREATE");
boolean hasFullAccess = permissionChecker.hasAllPermissions("SHOP", "READ", "UPDATE", "DELETE");
```

### Implementation Details

**Class**: `Permission`
- Defines resource types and actions
- Provides default permission matrix per role

**Class**: `PermissionChecker`
- Evaluates permissions for current user
- Integrates with Spring Security context
- Query-based (extensible to database)

---

## 4. Request Signing (HMAC)

### Overview
Optional cryptographic signing of requests to ensure integrity and prevent tampering on high-security endpoints.

### Configuration

Enable in `application.properties`:
```properties
# Enable request signing for protected endpoints
security.request-signing.enabled=true

# Secret key for HMAC (should be environment variable)
security.request-signing.secret=your-secret-key-here

# Endpoints requiring signature
# (default: /api/users/delete, /api/shops/delete, /api/admin/*, /api/auth/signup)
```

### Protected Endpoints

By default, these endpoints require signatures:
- `DELETE /api/users/*`
- `DELETE /api/shops/*`
- `/api/admin/*`
- `POST /api/auth/signup`

### Request Signing Process

**Step 1: Generate Signature**

```java
String method = "POST";
String path = "/api/users";
String timestamp = String.valueOf(System.currentTimeMillis());
String secret = "your-secret-key";

String signature = RequestSigningFilter.generateClientSignature(
    method, path, timestamp, secret
);
```

**Step 2: Send Signed Request**

```bash
TIMESTAMP=$(date +%s)000
METHOD="POST"
PATH="/api/users"
SECRET="your-secret-key"

# Generate signature (client-side)
SIGNATURE=$(echo -n "$METHOD|$PATH|$TIMESTAMP" | \
    openssl dgst -sha256 -hmac "$SECRET" -binary | \
    base64)

# Send request with signature headers
curl -X POST http://localhost:9090/api/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Signature: $SIGNATURE" \
  -H "X-Timestamp: $TIMESTAMP" \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","email":"new@example.com","password":"pass123"}'
```

### Signature Validation

Server validates:
1. Signature is present
2. Timestamp is within 5 minutes
3. HMAC matches expected value

### Error Response

```json
{
  "error": "Invalid request signature"
}
```

HTTP Status: `401 Unauthorized`

### Implementation Details

**Filter**: `RequestSigningFilter`
- Validates HMAC-SHA256 signatures
- Enforces timestamp freshness
- Protects against replay attacks

---

## 5. IP Whitelisting

### Overview
Restricts access to sensitive endpoints to specific IP addresses.

### Configuration

Enable in `application.properties`:
```properties
# Enable IP whitelisting
security.ip-whitelist.enabled=true

# Comma-separated list of allowed IPs
security.ip-whitelist.ips=127.0.0.1,192.168.1.0/24,10.0.0.5

# Endpoints requiring IP whitelisting
security.ip-whitelist.paths=/api/admin/,/api/users/
```

### Protected Endpoints

By default:
- `/api/admin/**` - All admin endpoints
- `/api/users/**` - User management (except GET)

### IP Format Support

- Single IP: `192.168.1.100`
- CIDR: `192.168.1.0/24`
- Hostname: `localhost`, `admin.example.com`
- Localhost variants: `127.0.0.1`, `::1`

### Client IP Detection

The filter checks headers in order:
1. `X-Forwarded-For` (first IP in list)
2. `X-Real-IP`
3. `RemoteAddr` (request source)

### Error Response

```json
{
  "error": "Access denied. Your IP is not whitelisted."
}
```

HTTP Status: `403 Forbidden`

### Dynamic Whitelisting

Programmatically manage whitelist:

```java
@Autowired
private IPWhitelistFilter ipWhitelistFilter;

// Add IP
ipWhitelistFilter.addIPToWhitelist("203.0.113.42");

// Remove IP
ipWhitelistFilter.removeIPFromWhitelist("203.0.113.42");

// Get current whitelist
Set<String> ips = ipWhitelistFilter.getWhitelist();

// Reload from config
ipWhitelistFilter.reloadWhitelist();
```

### Configuration Examples

**Development**
```properties
security.ip-whitelist.ips=127.0.0.1,localhost
```

**Production**
```properties
security.ip-whitelist.ips=10.0.0.0/8,203.0.113.0/24,office.example.com
```

**With VPN**
```properties
security.ip-whitelist.ips=vpn.example.com,203.0.113.42,10.20.30.0/24
```

### Implementation Details

**Filter**: `IPWhitelistFilter`
- Checks client IP against whitelist
- Handles proxied requests via headers
- Supports reload without restart

---

## Security Layers Summary

```
Request
   ↓
[Rate Limiting Filter] ← Check request count per user/role
   ↓
[IP Whitelist Filter] ← Check client IP against whitelist
   ↓
[Request Signing Filter] ← Validate HMAC signature (optional)
   ↓
[JWT Auth Filter] ← Validate JWT token
   ↓
[Authorization Filter] ← Check roles and resource ownership
   ↓
[Controller] → [Audit Aspect] ← Log operation
   ↓
Response
```

---

## Best Practices

### 1. Rate Limiting
- Monitor rate limit violations for DDoS detection
- Adjust limits based on usage patterns
- Use separate limits for different endpoints

### 2. Audit Logging
- Review audit logs regularly
- Set up alerts for failed access attempts
- Archive logs for compliance
- Monitor for suspicious patterns

### 3. Fine-Grained Permissions
- Define permissions at deployment time
- Document permission requirements
- Test permission enforcement
- Audit permission usage

### 4. Request Signing
- Use strong, randomly-generated secrets
- Rotate secrets periodically
- Transmit secrets securely (environment variables)
- Log signature validation failures

### 5. IP Whitelisting
- Start restrictive, expand as needed
- Support dynamic IP allocation (VPN, cloud)
- Test with all client locations
- Document whitelist rules

---

## Monitoring & Alerts

### Recommended Monitoring

1. **Rate Limiting Violations**
   - Alert on >10 violations per hour from single IP
   - Investigate for potential DoS

2. **Audit Log Anomalies**
   - Alert on failed login attempts (>3 in 5 min)
   - Alert on access denied events
   - Monitor for mass deletions

3. **IP Whitelisting Blocks**
   - Log blocked IPs
   - Investigate unexpected locations
   - Update whitelist if legitimate

4. **Request Signing Failures**
   - Log all signature validation failures
   - Investigate for tampering
   - Check for replay attacks

---

## Troubleshooting

### Rate Limit Issues

**Problem**: Getting "Rate limit exceeded" too quickly
```properties
# Solution: Adjust rate limits in RateLimitingConfig.java
# USER_RATE_LIMIT = 120  # Increase from 60 to 120
```

**Problem**: Anonymous users blocked
```properties
# Solution: Increase anonymous limit or exclude endpoint
# In RateLimitingFilter, add path to EXCLUDED_PATHS
```

### Audit Log Issues

**Problem**: Logs not appearing in MongoDB
```java
// Check if MongoDB is running
// Check if spring.data.mongodb.uri is correct
// Verify AuditLog collection exists
```

### IP Whitelist Issues

**Problem**: Legitimate IP blocked
```java
// Check client IP detection
// Add debugging to see actual IP
// Update whitelist with correct IP
```

### Request Signing Issues

**Problem**: Signature validation failing
```bash
# Verify timestamp is current
# Verify secret key matches
# Check method/path/timestamp format
```

---

## Migration Guide

### Enabling Rate Limiting

No configuration needed - enabled by default. To customize:

```java
// In RateLimitingConfig.java
private static final int USER_RATE_LIMIT = 120; // Modify as needed
```

### Enabling Audit Logging

Audit logging is automatic. To query:

```java
@Autowired
private AuditLogRepository auditLogRepository;

List<AuditLog> logs = auditLogRepository.findByAction("USER_MANAGEMENT_CREATE");
```

### Enabling Request Signing

In `application.properties`:
```properties
security.request-signing.enabled=true
security.request-signing.secret=${SIGNING_SECRET}
```

### Enabling IP Whitelisting

In `application.properties`:
```properties
security.ip-whitelist.enabled=true
security.ip-whitelist.ips=127.0.0.1,office.example.com
```

---

## References

- Bucket4j Documentation: https://github.com/vladimir-bukhtoyarov/bucket4j
- OWASP Rate Limiting: https://owasp.org/www-community/attacks/Rate-Limit-Cheat-Sheet
- HMAC Security: https://tools.ietf.org/html/rfc4868
- IP Whitelisting Best Practices: https://owasp.org/www-community/attacks/IP_Whitelisting
