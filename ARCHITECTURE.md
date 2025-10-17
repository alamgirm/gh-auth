# Architecture Documentation

## System Overview

This application implements the OAuth 2.0 Device Authorization Grant (Device Flow) for GitHub authentication using a modern full-stack architecture.

## Technology Stack

### Frontend
- **Framework**: Nuxt 3 (Vue 3)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **HTTP Client**: Built-in `$fetch`
- **State Management**: Vue Composition API + `useState`
- **Storage**: Browser localStorage

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **HTTP Client**: Spring WebFlux (WebClient)
- **Build Tool**: Gradle
- **Architecture**: Stateless REST API

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Browser                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │           Nuxt 3 Frontend (Port 3000)              │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │  Pages          Components      Composables  │  │    │
│  │  │  - index.vue    - LoginFlow     - useAuth    │  │    │
│  │  │                 - UserProfile                 │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │                       │                             │    │
│  │                       │ REST API                    │    │
│  │                       ▼                             │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          │ HTTP/HTTPS
                          │
┌─────────────────────────▼───────────────────────────────────┐
│          Spring Boot Backend (Port 8080)                     │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Controllers        Services          Config       │    │
│  │  - AuthController   - DeviceFlow   - GitHubOAuth  │    │
│  │                                     - WebConfig     │    │
│  │                     Models                          │    │
│  │                     - DeviceCode                    │    │
│  │                     - AccessToken                   │    │
│  │                     - GitHubUser                    │    │
│  └────────────────────────────────────────────────────┘    │
│                       │                                      │
│                       │ HTTPS                                │
│                       ▼                                      │
└──────────────────────────────────────────────────────────────┘
                       │
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                  GitHub OAuth API                            │
│  - Device Code Endpoint                                      │
│  - Token Endpoint                                            │
│  - User API                                                  │
└──────────────────────────────────────────────────────────────┘
```

## Device Flow Sequence

```
User        Frontend       Backend       GitHub
 │             │              │             │
 │  Click Login│              │             │
 │────────────>│              │             │
 │             │              │             │
 │             │ POST /device/code         │
 │             │─────────────>│             │
 │             │              │             │
 │             │              │ POST /login/device/code
 │             │              │────────────>│
 │             │              │             │
 │             │              │<────────────│
 │             │              │ device_code │
 │             │              │ user_code   │
 │             │<─────────────│             │
 │             │ device_code  │             │
 │             │ user_code    │             │
 │<────────────│              │             │
 │ Display Code│              │             │
 │             │              │             │
 │  Enter Code │              │             │
 │─────────────────────────────────────────>│
 │  Authorize  │              │             │
 │─────────────────────────────────────────>│
 │             │              │             │
 │             │ GET /device/poll          │
 │             │─────────────>│             │
 │             │              │             │
 │             │ (polling)    │ POST /oauth/access_token
 │             │              │────────────>│
 │             │              │             │
 │             │              │<────────────│
 │             │              │ access_token│
 │             │              │             │
 │             │              │ GET /user   │
 │             │              │────────────>│
 │             │              │             │
 │             │              │<────────────│
 │             │              │ user_info   │
 │             │<─────────────│             │
 │             │ access_token │             │
 │             │ user_info    │             │
 │<────────────│              │             │
 │ Logged In!  │              │             │
```

## Data Flow

### 1. Authentication Initiation

**Frontend → Backend**
```http
POST /api/auth/device/code
Content-Type: application/json
```

**Backend → GitHub**
```http
POST https://github.com/login/device/code
Content-Type: application/x-www-form-urlencoded

client_id=xxx&scope=user:email read:user
```

**GitHub → Backend → Frontend**
```json
{
  "deviceCode": "xxx",
  "userCode": "WDJB-MJHT",
  "verificationUri": "https://github.com/login/device",
  "expiresIn": 900,
  "interval": 5
}
```

### 2. Authorization Polling

**Frontend → Backend** (every 5 seconds)
```http
GET /api/auth/device/poll?device_code=xxx
```

**Backend → GitHub** (when frontend polls)
```http
POST https://github.com/login/oauth/access_token
Content-Type: application/x-www-form-urlencoded

client_id=xxx&device_code=xxx&grant_type=urn:ietf:params:oauth:grant-type:device_code
```

**Responses**:

**Pending:**
```json
{
  "status": "pending",
  "message": "Waiting for user authorization"
}
```

**Authorized:**
```json
{
  "status": "authorized",
  "accessToken": "gho_xxx",
  "user": { ... },
  "message": "Authorization successful"
}
```

### 3. Token Verification

**Frontend → Backend**
```http
GET /api/auth/verify
Authorization: Bearer gho_xxx
```

**Backend → GitHub**
```http
GET https://api.github.com/user
Authorization: Bearer gho_xxx
Accept: application/json
```

## Security Architecture

### Backend Security

1. **Client Secret Protection**
   - Secret stored as environment variable
   - Never exposed to frontend
   - Used only in backend → GitHub communication

2. **CORS Configuration**
   - Whitelist specific origins
   - Validate request headers
   - Support credentials

3. **Stateless Design**
   - No session storage
   - Device codes stored temporarily in memory
   - Backend doesn't store access tokens

4. **Input Validation**
   - Request parameter validation
   - Error handling
   - Rate limiting (recommended for production)

### Frontend Security

1. **No Secrets**
   - No client secret
   - Only public client ID
   - Backend handles sensitive operations

2. **Token Storage**
   - localStorage for persistence
   - XSS protection via CSP (recommended)
   - HTTPS only in production

3. **API Communication**
   - HTTPS in production
   - CORS-compliant requests
   - Bearer token authentication

## Component Responsibilities

### Frontend Components

#### `useAuth` Composable
- **Responsibilities**:
  - Manage authentication state
  - Handle localStorage persistence
  - API communication
  - Token verification
- **State**:
  - `user`: Current user object
  - `accessToken`: GitHub access token
  - `isAuthenticated`: Computed boolean

#### `LoginFlow` Component
- **Responsibilities**:
  - Display login UI
  - Show device code
  - Poll for authorization
  - Handle errors
- **States**:
  - `start`: Initial state
  - `show-code`: Display code
  - `success`: Authorized
  - `error`: Failed

#### `UserProfile` Component
- **Responsibilities**:
  - Display user information
  - Format data
  - Show statistics

### Backend Components

#### `AuthController`
- **Responsibilities**:
  - Expose REST endpoints
  - Handle HTTP requests/responses
  - Input validation
  - Error responses

#### `GitHubDeviceFlowService`
- **Responsibilities**:
  - Initiate device flow
  - Poll GitHub for authorization
  - Fetch user information
  - Manage device codes
  - Token verification

#### `GitHubOAuthConfig`
- **Responsibilities**:
  - Load OAuth configuration
  - Provide GitHub endpoints
  - Manage credentials

#### `WebConfig`
- **Responsibilities**:
  - Configure CORS
  - Set allowed origins/methods

## Data Models

### DeviceCodeResponse
```typescript
{
  deviceCode: string;      // Unique device identifier
  userCode: string;        // User-visible code
  verificationUri: string; // GitHub URL
  expiresIn: number;       // Seconds until expiration
  interval: number;        // Polling interval in seconds
}
```

### GitHubUser
```typescript
{
  login: string;           // Username
  id: number;              // GitHub user ID
  avatarUrl: string;       // Profile picture
  name: string;            // Display name
  email: string;           // Email address
  bio: string;             // Biography
  location: string;        // Location
  publicRepos: number;     // Repository count
  followers: number;       // Follower count
  following: number;       // Following count
  createdAt: string;       // Account creation date
}
```

### PollStatusResponse
```typescript
{
  status: 'pending' | 'authorized' | 'expired' | 'error';
  accessToken?: string;    // Only when authorized
  user?: GitHubUser;       // Only when authorized
  message: string;         // Status message
}
```

## Deployment Architecture

### Development

```
Frontend: http://localhost:3000
Backend:  http://localhost:8080
GitHub:   https://github.com
```

### Production

```
Frontend: https://app.yourdomain.com
Backend:  https://api.yourdomain.com
GitHub:   https://github.com

- HTTPS required
- Environment variables
- CORS configured for production domains
- Rate limiting enabled
- Monitoring and logging
```

## Scalability Considerations

### Current Implementation
- In-memory device code storage (ConcurrentHashMap)
- Suitable for single-instance deployment
- Stateless backend design

### Production Recommendations

1. **Distributed Storage**
   - Use Redis for device codes
   - Share state across instances
   - Enable horizontal scaling

2. **Load Balancing**
   - Deploy multiple backend instances
   - Use load balancer (Nginx, AWS ALB)
   - Session affinity not required (stateless)

3. **Rate Limiting**
   - Protect against abuse
   - Per-IP rate limits
   - Token bucket algorithm

4. **Monitoring**
   - Log all authentication attempts
   - Monitor polling frequency
   - Alert on failures

5. **Caching**
   - Cache user profiles
   - Reduce GitHub API calls
   - TTL-based invalidation

## Error Handling

### Frontend Errors
- Network failures → Retry with exponential backoff
- Invalid device code → Restart flow
- Expired code → Show error, allow retry
- User denial → Show message, allow retry

### Backend Errors
- GitHub API down → Return error status
- Invalid credentials → Log error, return 500
- Rate limit exceeded → Return 429
- Token validation failure → Return 401

## Performance Characteristics

### Latency
- Device code request: ~200-500ms
- Authorization poll: ~100-300ms (when pending)
- User info fetch: ~200-400ms

### Polling
- Default interval: 5 seconds
- Adjustable based on GitHub response
- Automatic cleanup on success/expiration

### Storage
- Device codes: Temporary (15 minutes max)
- Access tokens: Frontend localStorage
- User data: Frontend localStorage

---

Built with security and scalability in mind! 🔒

