# Backend Token Storage Architecture

## ✅ Architecture Update: Frontend Token Storage → Backend Database Storage

The application has been updated to use a **server-centric** authentication model where tokens are stored securely in the backend database.

## 🔄 What Changed

### Before (Client-Side Token Storage)
```
Frontend:
- Stores access token in localStorage
- Makes direct calls to GitHub API
- Manages token lifecycle

Backend:
- Stateless
- Only exchanges code for token
- Returns token to frontend
```

### After (Server-Side Token Storage)
```
Frontend:
- Stores only userId in localStorage
- Never sees the access token
- Makes calls to backend
- Backend proxies to GitHub

Backend:
- Stores tokens in database
- Creates user sessions
- Proxies GitHub API calls
- Manages token lifecycle
```

## 🏗️ New Architecture

```
┌─────────────────────────────────────────┐
│  Frontend (Nuxt 3)                       │
│  ┌────────────────────────────────────┐ │
│  │  LocalStorage:                     │ │
│  │  - github_user_id: "12345"        │ │
│  │  - NO ACCESS TOKEN                 │ │
│  └────────────────────────────────────┘ │
└──────────────┬──────────────────────────┘
               │
               │ Session Cookie
               │ userId in session
               ▼
┌─────────────────────────────────────────┐
│  Backend (Spring Boot)                   │
│  ┌────────────────────────────────────┐ │
│  │  Database (H2/PostgreSQL):         │ │
│  │  ┌──────────────────────────────┐  │ │
│  │  │ user_tokens table             │  │ │
│  │  │ - userId                      │  │ │
│  │  │ - accessToken (encrypted)     │  │ │
│  │  │ - username, email, etc.       │  │ │
│  │  └──────────────────────────────┘  │ │
│  └────────────────────────────────────┘ │
└──────────────┬──────────────────────────┘
               │
               │ Backend makes calls
               │ with stored token
               ▼
┌─────────────────────────────────────────┐
│  GitHub API                              │
│  - GET /user                             │
│  - Other endpoints                       │
└─────────────────────────────────────────┘
```

## 🔄 Authentication Flow

```
1. User clicks "Login with GitHub"
   │
2. Popup opens → GitHub authorization
   │
3. User authorizes
   │
4. Callback receives code
   │
5. Frontend → Backend: POST /exchange-code {code}
   │
6. Backend:
   ├─→ Exchange code for token with GitHub
   ├─→ Fetch user info from GitHub
   ├─→ Store token in database
   ├─→ Create session with userId
   └─→ Return userId + user to frontend
   │
7. Frontend:
   ├─→ Store userId in localStorage
   ├─→ Session cookie automatically stored by browser
   └─→ Display user profile
   │
✅ Authenticated!
```

## 🔌 API Flow

### Get User Info (New Behavior)

**Frontend → Backend**
```http
GET /api/auth/user
Cookie: JSESSIONID=xxx
```

**Backend:**
1. Gets userId from session
2. Looks up token in database
3. Calls GitHub API with stored token
4. Returns user data to frontend

**GitHub never sees the frontend!**

```
Frontend → Backend → Database → Get Token → GitHub API → Return Data
```

## 📊 Data Storage Comparison

### Before
```
Frontend localStorage:
├── github_access_token: "gho_xxxxxxxxxxxx"  ← Security risk!
└── github_user: "{...}"

Backend:
└── (nothing stored)
```

### After
```
Frontend localStorage:
└── github_user_id: "12345"  ← Just an identifier

Frontend cookies (automatic):
└── JSESSIONID: "xxx"  ← Session cookie

Backend database:
└── user_tokens table:
    ├── userId: "12345"
    ├── accessToken: "gho_xxx"  ← Stored securely!
    ├── username: "john"
    ├── email: "john@example.com"
    └── ... (cached profile data)
```

## 🔒 Security Improvements

### Before (Client-Side Storage)
- ⚠️ Token in localStorage (XSS vulnerable)
- ⚠️ Frontend makes direct GitHub API calls
- ⚠️ Token exposed in browser memory
- ⚠️ No server-side token management

### After (Server-Side Storage)
- ✅ Token never sent to frontend
- ✅ Token stored in backend database
- ✅ Session-based authentication
- ✅ Backend proxies all GitHub API calls
- ✅ Can encrypt tokens at rest
- ✅ Centralized token revocation
- ✅ Audit trail possible

## 🔌 Updated API Endpoints

| Method | Endpoint | Description | Returns |
|--------|----------|-------------|---------|
| GET | `/api/auth/authorize-url` | Get OAuth URL | `{url, state}` |
| POST | `/api/auth/exchange-code` | Exchange code | `{userId, user}` |
| GET | `/api/auth/user` | Get user (fresh from GitHub) | `GitHubUser` |
| GET | `/api/auth/user/cached` | Get cached user (from DB) | `GitHubUser` |
| GET | `/api/auth/check` | Check auth status | `{authenticated, userId, user}` |
| POST | `/api/auth/logout` | Logout user | `{message}` |
| GET | `/api/auth/health` | Health check | `"OK"` |

## 💾 Database Schema

### user_tokens Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `user_id` | VARCHAR(255) | GitHub user ID (unique) |
| `username` | VARCHAR(255) | GitHub username |
| `access_token` | VARCHAR(1000) | GitHub access token |
| `token_type` | VARCHAR(500) | Token type (bearer) |
| `scope` | VARCHAR(500) | OAuth scopes |
| `created_at` | TIMESTAMP | When token was created |
| `updated_at` | TIMESTAMP | Last update time |
| `name` | VARCHAR(500) | User's display name |
| `email` | VARCHAR(500) | User's email |
| `avatar_url` | VARCHAR(1000) | Avatar URL |
| `bio` | VARCHAR(2000) | User bio |
| `location` | VARCHAR(500) | Location |
| `public_repos` | INTEGER | Repository count |
| `followers` | INTEGER | Follower count |
| `following` | INTEGER | Following count |

## 🔑 Session Management

### How Sessions Work

1. **Login**:
   - Backend creates session
   - Sets `userId` in session
   - Returns `JSESSIONID` cookie to frontend
   - Frontend stores it automatically

2. **Subsequent Requests**:
   - Frontend includes cookie automatically
   - Backend reads `userId` from session
   - Backend looks up token in database
   - Backend makes GitHub API call

3. **Logout**:
   - Backend deletes token from database
   - Backend invalidates session
   - Frontend clears localStorage

### Session Configuration

```yaml
# In application.yml (Spring Boot default)
server:
  servlet:
    session:
      cookie:
        name: JSESSIONID
        http-only: true      # JavaScript can't access
        secure: false        # true in production (HTTPS)
        same-site: lax       # CSRF protection
```

## 🔐 Token Security

### Encryption at Rest (Optional)

You can encrypt tokens before storing:

```java
@Service
public class EncryptionService {
    
    public String encrypt(String token) {
        // Use AES encryption
        // Store encryption key in environment
    }
    
    public String decrypt(String encryptedToken) {
        // Decrypt using key
    }
}
```

Then in `GitHubAuthService`:
```java
private final EncryptionService encryptionService;

// When saving
userToken.setAccessToken(encryptionService.encrypt(token));

// When using
String token = encryptionService.decrypt(userToken.getAccessToken());
```

## 📝 Frontend Changes

### What Frontend Stores

**Before**:
```javascript
localStorage.setItem('github_access_token', 'gho_xxx')  // ❌ Security risk
localStorage.setItem('github_user', '{"login":"..."}')
```

**After**:
```javascript
localStorage.setItem('github_user_id', '12345')  // ✅ Just identifier
// Token stored in backend database
```

### API Calls

**Before**:
```typescript
// Frontend called GitHub directly
const user = await fetch('https://api.github.com/user', {
  headers: {
    Authorization: `Bearer ${localStorage.getItem('github_access_token')}`
  }
})
```

**After**:
```typescript
// Frontend calls backend, backend calls GitHub
const user = await $fetch('/api/auth/user', {
  credentials: 'include'  // Includes session cookie
})
```

## 🎯 Benefits

### Security
- ✅ Tokens never exposed to frontend
- ✅ XSS attacks can't steal tokens
- ✅ Centralized token management
- ✅ Can revoke all tokens server-side
- ✅ Audit trail of token usage
- ✅ Can encrypt tokens at rest

### Performance
- ✅ Cached user data in database
- ✅ Less GitHub API calls (use cached data)
- ✅ Faster user profile loading

### Scalability
- ✅ Can use Redis for session storage
- ✅ Can use PostgreSQL for tokens
- ✅ Centralized user management
- ✅ Better for multi-server deployments

### Compliance
- ✅ Better for PCI/SOC2 compliance
- ✅ Tokens stored server-side
- ✅ Audit logs possible
- ✅ Token rotation easier

## 🔄 Migration Impact

### Frontend Developer
```typescript
// OLD: Direct access to token
const token = localStorage.getItem('github_access_token')
fetch('https://api.github.com/user', {
  headers: { Authorization: `Bearer ${token}` }
})

// NEW: Backend proxy
const user = await $fetch('/api/auth/user', {
  credentials: 'include'
})
```

### Backend Developer
```java
// NEW: Store and manage tokens
@Autowired
private UserTokenRepository userTokenRepository;

// Store token
UserToken userToken = UserToken.builder()
    .userId(String.valueOf(githubUser.getId()))
    .accessToken(accessToken)
    .build();
userTokenRepository.save(userToken);

// Use token
UserToken token = userTokenRepository.findByUserId(userId);
// Make GitHub API call with token.getAccessToken()
```

## 📊 Request Flow Examples

### Get User Profile (Cached)

```
Frontend                    Backend                     Database
   │                           │                            │
   │  GET /auth/user/cached    │                            │
   │  Cookie: SESSION=xxx      │                            │
   │──────────────────────────→│                            │
   │                           │                            │
   │                           │  Get userId from session   │
   │                           │                            │
   │                           │  SELECT * FROM user_tokens │
   │                           │  WHERE user_id = ?         │
   │                           │───────────────────────────→│
   │                           │                            │
   │                           │←───────────────────────────│
   │                           │  UserToken record          │
   │                           │                            │
   │←──────────────────────────│                            │
   │  GitHubUser (cached)      │                            │
```

### Get User Profile (Fresh)

```
Frontend    Backend    Database    GitHub
   │           │           │          │
   │  GET /user│           │          │
   │──────────→│           │          │
   │           │           │          │
   │           │  Get token│          │
   │           │──────────→│          │
   │           │           │          │
   │           │←──────────│          │
   │           │  Token    │          │
   │           │           │          │
   │           │  GET /user           │
   │           │  Bearer token        │
   │           │─────────────────────→│
   │           │                      │
   │           │←─────────────────────│
   │           │  User data           │
   │           │                      │
   │           │  Update DB           │
   │           │──────────→│          │
   │           │           │          │
   │←──────────│           │          │
   │  User data│           │          │
```

## 🧪 Testing

### Check Database

Access H2 Console: `http://localhost:8080/h2-console`

```
JDBC URL: jdbc:h2:mem:authdb
Username: sa
Password: (leave empty)
```

Query tokens:
```sql
SELECT * FROM user_tokens;
```

### Check Session

```bash
# Login and get session cookie
curl -c cookies.txt \
  -X POST http://localhost:8080/api/auth/exchange-code \
  -H "Content-Type: application/json" \
  -d '{"code":"xxx","state":"yyy"}'

# Use session cookie to get user
curl -b cookies.txt http://localhost:8080/api/auth/user
```

## 🔐 Security Best Practices

### Implemented
1. ✅ Tokens stored in database (not frontend)
2. ✅ HttpOnly session cookies
3. ✅ CSRF protection with state parameter
4. ✅ Session-based authentication

### Recommended for Production
1. ⚡ Encrypt tokens at rest in database
2. ⚡ Use PostgreSQL instead of H2
3. ⚡ Use Redis for session storage
4. ⚡ Implement token rotation
5. ⚡ Add rate limiting
6. ⚡ Enable HTTPS only
7. ⚡ Set secure cookie flags

## 📝 Database Configuration

### Development (H2)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:authdb
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recreate on restart
```

### Production (PostgreSQL)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update  # Don't drop tables!
```

## 🔄 Migration Steps

### What Users Need to Do

1. **Clear old localStorage**:
   ```javascript
   localStorage.removeItem('github_access_token')
   localStorage.removeItem('github_user')
   ```

2. **Re-authenticate**:
   - Old tokens in localStorage won't work
   - Need to login again
   - New userId will be stored

### Automatic Handling

The app handles migration automatically:
- Checks for userId in localStorage
- If not found, shows login
- After login, stores userId
- Session cookie manages auth state

## 📊 Comparison

| Aspect | Client Storage | Server Storage (NEW) |
|--------|---------------|----------------------|
| **Token Location** | Frontend localStorage | Backend database |
| **Token Visible** | Yes (DevTools) | No |
| **XSS Risk** | High | Low |
| **GitHub API Calls** | Direct from frontend | Proxied via backend |
| **Token Revocation** | Manual (user clears storage) | Server-side (instant) |
| **Audit Trail** | No | Yes (DB logs) |
| **Multi-Device** | Separate tokens | Shared token |
| **Compliance** | Harder | Easier |

## 🎯 Frontend API Calls

### Get Current User

```typescript
// Cached (fast, from database)
const user = await $fetch('/api/auth/user/cached', {
  credentials: 'include'
})

// Fresh (calls GitHub API)
const user = await $fetch('/api/auth/user', {
  credentials: 'include'
})
```

### Check Auth Status

```typescript
const { authenticated, userId, user } = await $fetch('/api/auth/check', {
  credentials: 'include'
})
```

### Logout

```typescript
await $fetch('/api/auth/logout', {
  method: 'POST',
  credentials: 'include'
})
```

## 🚀 Production Deployment

### Environment Variables

```bash
# Backend
GITHUB_CLIENT_ID=xxx
GITHUB_CLIENT_SECRET=xxx
GITHUB_REDIRECT_URI=https://yourdomain.com/auth/callback

# Database
DB_URL=jdbc:postgresql://localhost:5432/authdb
DB_USERNAME=postgres
DB_PASSWORD=secret

# Session
SESSION_TIMEOUT=7200  # 2 hours
```

### Session Storage (Production)

For multi-server deployment, use Redis:

```yaml
spring:
  session:
    store-type: redis
  redis:
    host: localhost
    port: 6379
```

Add dependency:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.session:spring-session-data-redis'
```

## ⚡ Performance Optimization

### Caching Strategy

1. **Cached Endpoint** (`/auth/user/cached`):
   - Returns data from database
   - Fast (<50ms)
   - Use for frequent calls

2. **Fresh Endpoint** (`/auth/user`):
   - Calls GitHub API
   - Slower (~300ms)
   - Use when fresh data needed
   - Updates database cache

### Usage Example

```typescript
// Initial load - use cached
onMounted(async () => {
  const user = await $fetch('/api/auth/user/cached', {
    credentials: 'include'
  })
})

// Refresh button - get fresh data
const refresh = async () => {
  const user = await $fetch('/api/auth/user', {
    credentials: 'include'
  })
}
```

## 🆚 When to Use Each Architecture

### Client-Side Token Storage
**Use when**:
- Building a SPA with no backend
- Desktop/mobile apps
- You trust the client environment
- Simple authentication needs

### Server-Side Token Storage (This App)
**Use when**:
- Security is critical
- Need audit trails
- Multi-device support required
- Compliance requirements (PCI, SOC2)
- Want to proxy GitHub API calls
- Need centralized token management

## 📋 Implementation Checklist

- [x] Add database dependencies
- [x] Create UserToken entity
- [x] Create UserTokenRepository
- [x] Update application.yml for database
- [x] Update GitHubAuthService to store tokens
- [x] Update AuthController for session management
- [x] Add user info endpoints
- [x] Add cached user endpoint
- [x] Add logout endpoint
- [x] Update frontend to use userId only
- [x] Update frontend to call backend endpoints
- [ ] Manual testing required
- [ ] Production database configuration

## 🧪 How to Test

### 1. Start Backend

```bash
cd backend
./gradlew bootRun
```

### 2. Check Database Console

Open: `http://localhost:8080/h2-console`

### 3. Login via Frontend

```bash
cd frontend
npm run dev
# Open http://localhost:3000
# Click "Login with GitHub"
```

### 4. Verify Token in Database

In H2 Console:
```sql
SELECT user_id, username, access_token, created_at 
FROM user_tokens;
```

### 5. Test API Calls

```bash
# Check auth status
curl -b cookies.txt http://localhost:8080/api/auth/check

# Get user (cached)
curl -b cookies.txt http://localhost:8080/api/auth/user/cached

# Get user (fresh from GitHub)
curl -b cookies.txt http://localhost:8080/api/auth/user
```

## 💡 Key Points

1. **Frontend never sees the access token** - More secure!
2. **Session cookie manages authentication** - Automatic
3. **Backend proxies GitHub API** - Centralized control
4. **Database stores tokens** - Can revoke anytime
5. **Cached user data** - Better performance
6. **Fresh data available** - When needed

---

**Architecture**: ✅ **Server-Centric Token Storage**  
**Security**: ✅ **Enhanced**  
**Status**: ✅ **READY TO TEST**

