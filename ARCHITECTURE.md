# Architecture Documentation

## System Overview

This application implements **GitHub App OAuth with popup-based authentication** for secure user login without full-page redirects.

## Technology Stack

### Frontend
- **Framework**: Nuxt 3 (Vue 3)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **HTTP Client**: Built-in `$fetch`
- **State Management**: Vue Composition API + `useState`
- **Storage**: Browser localStorage
- **Popup Communication**: window.postMessage API

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
│  │  │  - callback.vue - UserProfile                │  │    │
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
│  │  - AuthController   - AuthService   - GitHubOAuth │    │
│  │                                     - WebConfig     │    │
│  │                     Models                          │    │
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
│  - Authorization Endpoint                                    │
│  - Token Exchange Endpoint                                   │
│  - User API                                                  │
└──────────────────────────────────────────────────────────────┘
```

## Popup OAuth Flow Sequence

```
User        Main Window    Popup          Backend       GitHub
 │             │              │              │             │
 │  Click      │              │              │             │
 │  Login      │              │              │             │
 │────────────>│              │              │             │
 │             │              │              │             │
 │             │ GET /authorize-url          │             │
 │             │─────────────────────────────>│             │
 │             │              │              │             │
 │             │<─────────────────────────────│             │
 │             │ authUrl + state             │             │
 │             │              │              │             │
 │             │ Open Popup   │              │             │
 │             │─────────────>│              │             │
 │             │              │              │             │
 │             │              │ Navigate to GitHub         │
 │             │              │──────────────────────────>│
 │             │              │              │             │
 │             │              │ User Authorizes            │
 │             │              │<──────────────────────────│
 │             │              │ Redirect w/ code           │
 │             │              │              │             │
 │             │              │ /callback?code=xxx&state=yyy
 │             │              │              │             │
 │             │              │ Extract code │             │
 │             │              │ & state      │             │
 │             │              │              │             │
 │             │ postMessage  │              │             │
 │             │<─────────────│              │             │
 │             │ {code, state}│              │             │
 │             │              │              │             │
 │             │              │ Close popup  │             │
 │             │              │──────────X   │             │
 │             │              │              │             │
 │             │ POST /exchange-code         │             │
 │             │ {code, state}               │             │
 │             │─────────────────────────────>│             │
 │             │              │              │             │
 │             │              │   POST /oauth/access_token │
 │             │              │              │────────────>│
 │             │              │              │             │
 │             │              │              │<────────────│
 │             │              │              │ access_token│
 │             │              │              │             │
 │             │              │   GET /user  │             │
 │             │              │              │────────────>│
 │             │              │              │             │
 │             │              │              │<────────────│
 │             │              │              │ user_info   │
 │             │              │              │             │
 │             │<─────────────────────────────│             │
 │             │ {token, user}               │             │
 │             │              │              │             │
 │<────────────│              │              │             │
 │ Logged In!  │              │              │             │
```

## Data Flow

### 1. Get Authorization URL

**Frontend → Backend**
```http
GET /api/auth/authorize-url
```

**Backend Response**
```json
{
  "url": "https://github.com/login/oauth/authorize?client_id=xxx&redirect_uri=http://localhost:3000/auth/callback&scope=user:email%20read:user&state=abc123",
  "state": "abc123"
}
```

### 2. User Authorization (in Popup)

**Popup → GitHub**
```
https://github.com/login/oauth/authorize?
  client_id=xxx&
  redirect_uri=http://localhost:3000/auth/callback&
  scope=user:email read:user&
  state=abc123
```

**GitHub → Popup (redirect)**
```
http://localhost:3000/auth/callback?
  code=authorization_code_here&
  state=abc123
```

### 3. Code Exchange

**Popup → Main Window (postMessage)**
```javascript
{
  type: 'github-auth-success',
  code: 'authorization_code_here',
  receivedState: 'abc123'
}
```

**Main Window → Backend**
```http
POST /api/auth/exchange-code
Content-Type: application/json

{
  "code": "authorization_code_here",
  "state": "abc123"
}
```

**Backend → GitHub**
```http
POST https://github.com/login/oauth/access_token
Content-Type: application/x-www-form-urlencoded

client_id=xxx&
client_secret=yyy&
code=authorization_code_here&
redirect_uri=http://localhost:3000/auth/callback
```

**Backend Response to Frontend**
```json
{
  "accessToken": "gho_xxx",
  "tokenType": "bearer",
  "scope": "user:email,read:user",
  "user": {
    "login": "username",
    "id": 12345,
    "avatarUrl": "https://avatars.githubusercontent.com/u/12345",
    "name": "User Name",
    "email": "user@example.com",
    ...
  }
}
```

### 4. Token Verification

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
   - Support credentials for popup communication

3. **Stateless Design**
   - No session storage required
   - No persistent state between requests
   - Backend doesn't store access tokens

4. **State Parameter Validation**
   - Random state generated for each auth request
   - Prevents CSRF attacks
   - Validated on code exchange

### Frontend Security

1. **No Secrets**
   - No client secret
   - Only public client ID
   - Backend handles sensitive operations

2. **Token Storage**
   - localStorage for persistence
   - Tokens managed client-side
   - HTTPS only in production

3. **Popup Communication**
   - window.postMessage for secure IPC
   - Origin validation
   - State parameter verification

4. **API Communication**
   - HTTPS in production
   - CORS-compliant requests
   - Bearer token authentication

## Component Responsibilities

### Frontend Components

#### `useAuth` Composable
- **Responsibilities**:
  - Manage authentication state
  - Handle localStorage persistence
  - Open popup for OAuth
  - Handle postMessage communication
  - Exchange code for token
  - Token verification
- **State**:
  - `user`: Current user object
  - `accessToken`: GitHub access token
  - `isAuthenticated`: Computed boolean
- **Methods**:
  - `loginWithPopup()`: Opens popup and manages OAuth flow
  - `getAuthorizationUrl()`: Fetches OAuth URL from backend
  - `exchangeCodeForToken()`: Exchanges auth code for token
  - `verifyToken()`: Validates stored token
  - `logout()`: Clears auth state

#### `LoginFlow` Component
- **Responsibilities**:
  - Display login button
  - Handle popup OAuth flow
  - Show loading states
  - Handle errors
- **States**:
  - Default: Login button
  - Loading: Authenticating message
  - Error: Error message with retry

#### `auth/callback.vue` Page
- **Responsibilities**:
  - Extract code and state from URL
  - Send to parent via postMessage
  - Display status (processing/success/error)
  - Auto-close popup
- **Security**:
  - Validates window.opener exists
  - Checks origin on postMessage
  - Handles error cases

#### `UserProfile` Component
- **Responsibilities**:
  - Display user information
  - Format data
  - Show statistics

### Backend Components

#### `AuthController`
- **Responsibilities**:
  - Expose REST endpoints
  - Generate authorization URLs with state
  - Exchange authorization code for token
  - Verify access tokens
  - Input validation
  - Error responses

#### `GitHubAuthService`
- **Responsibilities**:
  - Generate GitHub authorization URL
  - Exchange code for access token
  - Fetch user information from GitHub
  - Token verification
  - Handle GitHub API errors

#### `GitHubOAuthConfig`
- **Responsibilities**:
  - Load OAuth configuration
  - Provide GitHub endpoints
  - Manage credentials
  - Configure redirect URI

#### `WebConfig`
- **Responsibilities**:
  - Configure CORS for popup communication
  - Set allowed origins/methods
  - Enable credentials support

## Data Models

### AccessTokenResponse
```typescript
{
  accessToken: string;     // GitHub access token
  tokenType: string;       // "bearer"
  scope: string;           // Granted scopes
  error?: string;          // Error code if failed
  errorDescription?: string; // Error details
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

### Authorization URL Response
```typescript
{
  url: string;             // Full GitHub OAuth URL
  state: string;           // CSRF protection state
}
```

## Deployment Architecture

### Development

```
Frontend: http://localhost:3000
Backend:  http://localhost:8080
GitHub:   https://github.com
Callback: http://localhost:3000/auth/callback
```

### Production

```
Frontend: https://app.yourdomain.com
Backend:  https://api.yourdomain.com
GitHub:   https://github.com
Callback: https://app.yourdomain.com/auth/callback

- HTTPS required
- Environment variables
- CORS configured for production domains
- Rate limiting enabled
- Monitoring and logging
```

## Scalability Considerations

### Current Implementation
- Completely stateless backend
- No server-side session storage
- Suitable for horizontal scaling
- No shared state between instances

### Production Recommendations

1. **Load Balancing**
   - Deploy multiple backend instances
   - Use load balancer (Nginx, AWS ALB)
   - No session affinity required (stateless)

2. **Rate Limiting**
   - Protect against abuse
   - Per-IP rate limits
   - Token bucket algorithm

3. **Monitoring**
   - Log all authentication attempts
   - Monitor OAuth failures
   - Alert on suspicious activity

4. **Caching**
   - Cache user profiles (optional)
   - Reduce GitHub API calls
   - TTL-based invalidation

5. **CDN**
   - Serve frontend from CDN
   - Edge caching for static assets
   - Global distribution

## Error Handling

### Frontend Errors
- Popup blocked → Show instructions to allow popups
- Network failures → Retry with user action
- Invalid state → Restart auth flow
- User closes popup → Show retry option
- postMessage failures → Handle gracefully

### Backend Errors
- GitHub API down → Return error status
- Invalid code → Return 401
- Rate limit exceeded → Return 429
- Token validation failure → Return 401
- CORS errors → Log and investigate

## Performance Characteristics

### Latency
- Authorization URL generation: ~50-100ms
- Code exchange: ~200-500ms
- User info fetch: ~200-400ms
- **Total login time: <3 seconds** ⚡

### No Polling Required
- Instant feedback after authorization
- No backend polling
- No wasted API calls
- Efficient resource usage

### Storage
- Access tokens: Frontend localStorage only
- User data: Frontend localStorage only
- No backend storage required

## Advantages Over Device Flow

| Feature | Device Flow | Popup OAuth |
|---------|-------------|-------------|
| **Speed** | 15+ seconds | <3 seconds |
| **User Steps** | 5 steps | 2 steps |
| **Manual Entry** | Yes | No |
| **Backend Polling** | Yes (every 10s) | No |
| **Complexity** | High | Low |
| **Server Load** | High | Low |
| **UX** | Poor | Excellent |
| **Mobile Support** | Good | Excellent |

## Browser Compatibility

### Supported Browsers
- ✅ Chrome/Edge (latest)
- ✅ Firefox (latest)
- ✅ Safari (latest)
- ✅ Mobile browsers (with fallback)

### Requirements
- JavaScript enabled
- Popups allowed
- localStorage available
- postMessage API support

### Popup Blocker Handling
- Detect blocked popups
- Show user-friendly message
- Provide instructions
- Allow retry

---

Built with security, speed, and user experience in mind! 🚀
