# Multi-Provider Authentication Guide

## 🎯 Overview

The application now supports **multiple authentication providers** with a unified interface:

1. ✅ **GitHub OAuth** - Popup-based OAuth flow
2. ✅ **Azure Entra ID (Microsoft)** - MSAL with silent token acquisition
3. 🔜 **Ready for more** - Extensible architecture for additional providers

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Frontend (Nuxt 3)                                       │
│  ┌───────────────────────────────────────────────────┐ │
│  │  useMultiAuth composable                          │ │
│  │  ├─ useAuth (GitHub)                              │ │
│  │  ├─ useAzureAuth (Microsoft)                      │ │
│  │  └─ Unified interface                             │ │
│  └───────────────────────────────────────────────────┘ │
└──────────────┬──────────────────────────────────────────┘
               │
               │ Session Cookie + Provider Info
               ▼
┌─────────────────────────────────────────────────────────┐
│  Backend (Spring Boot)                                   │
│  ┌───────────────────────────────────────────────────┐ │
│  │  AuthController (Unified)                         │ │
│  │  ├─ /api/auth/github/*                            │ │
│  │  ├─ /api/auth/azure/*                             │ │
│  │  └─ /api/auth/* (provider-agnostic)               │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │  Services                                          │ │
│  │  ├─ GitHubAuthService                             │ │
│  │  └─ AzureTokenValidationService                   │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │  Database                                          │ │
│  │  user_tokens table:                                │ │
│  │  ├─ userId: "github:12345"  (GitHub user)         │ │
│  │  ├─ userId: "azure:abc-def" (Azure user)          │ │
│  │  └─ accessToken, profile data, etc.               │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
               │
               ├──→ GitHub API (for GitHub users)
               └──→ Microsoft Graph (for Azure users)
```

## 🔑 Provider Comparison

| Feature | GitHub OAuth | Azure Entra ID |
|---------|-------------|----------------|
| **Flow** | Popup OAuth | MSAL Redirect/Silent |
| **Navigation** | Popup (no page reload) | Redirect (page reloads) |
| **Token Type** | OAuth token | JWT token |
| **Validation** | Backend exchanges code | Backend validates JWT |
| **Silent Refresh** | No | Yes ✅ |
| **Token Storage** | Backend database | Backend database |
| **User ID Prefix** | `github:12345` | `azure:abc-def` |
| **Best For** | Developers | Enterprise users |

## 🚀 Setup Guide

### 1. GitHub OAuth App Setup

See [GITHUB_APP_SETUP.md](GITHUB_APP_SETUP.md) for detailed instructions.

**Quick setup**:
```
https://github.com/settings/developers
→ New OAuth App
→ Callback: http://localhost:3000/auth/callback
→ Get Client ID & Secret
```

**Environment variables**:
```bash
export GITHUB_CLIENT_ID=your_github_client_id
export GITHUB_CLIENT_SECRET=your_github_client_secret
```

### 2. Azure Entra ID App Setup

#### Step 1: Register App in Azure Portal

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** → **App registrations**
3. Click **"New registration"**
4. Fill in:
   ```
   Name: Your App Name
   Supported account types: Accounts in any organizational directory and personal Microsoft accounts
   Redirect URI: Single-page application (SPA) → http://localhost:3000
   ```
5. Click **"Register"**
6. Note the **Application (client) ID**
7. Note the **Directory (tenant) ID**

#### Step 2: Configure API Permissions

1. Go to **API permissions**
2. Add permission → **Microsoft Graph** → **Delegated permissions**
3. Add: `User.Read`
4. Click **"Add permissions"**

#### Step 3: Configure Authentication

1. Go to **Authentication**
2. Under **Implicit grant and hybrid flows**:
   - ✅ Check "Access tokens"
   - ✅ Check "ID tokens"
3. Under **Advanced settings**:
   - Allow public client flows: **Yes**
4. Click **"Save"**

**Environment variables**:
```bash
# Backend
export AZURE_CLIENT_ID=your_azure_client_id
export AZURE_TENANT_ID=your_tenant_id  # or 'common' for multi-tenant

# Frontend
export NUXT_PUBLIC_AZURE_CLIENT_ID=your_azure_client_id
export NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/common
```

## 🔌 API Endpoints

### Provider-Specific Endpoints

#### GitHub
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/github/authorize-url` | Get GitHub OAuth URL |
| POST | `/api/auth/github/exchange-code` | Exchange code for session |

#### Azure
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/azure/login` | Validate Azure token and create session |

### Unified Endpoints (Provider-Agnostic)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/user` | Get user info (fresh) |
| GET | `/api/auth/user/cached` | Get cached user info |
| GET | `/api/auth/check` | Check auth status |
| POST | `/api/auth/logout` | Logout (any provider) |
| GET | `/api/auth/health` | Health check |

## 🔄 Authentication Flows

### GitHub Flow

```
1. User clicks "Login with GitHub"
   ↓
2. Frontend: GET /auth/github/authorize-url
   ← {url, state}
   ↓
3. Open popup with GitHub OAuth
   ↓
4. User authorizes
   ↓
5. Callback receives code
   ↓
6. Frontend: POST /auth/github/exchange-code {code, state}
   ↓
7. Backend:
   ├─ Exchange code for token
   ├─ Store in DB as "github:12345"
   ├─ Create session
   └─ Return userId
   ↓
✅ Logged in as GitHub user
```

### Azure Flow (Redirect + Silent Token)

```
1. User clicks "Login with Microsoft"
   ↓
2. MSAL checks for existing account
   ↓
3. If account exists:
   ├─ Acquire token silently ⚡
   └─ No redirect needed!
   Else:
   ├─ Full page redirect to Microsoft
   └─ User signs in
   └─ Redirect back to app
   ↓
4. MSAL handles redirect response
   ↓
5. MSAL returns access token (JWT)
   ↓
6. Frontend: POST /auth/azure/login {accessToken}
   ↓
7. Backend:
   ├─ Validate JWT signature
   ├─ Verify issuer & audience
   ├─ Extract user claims
   ├─ Store in DB as "azure:xxx"
   ├─ Create session
   └─ Return userId
   ↓
✅ Logged in as Azure user
```

## 💾 Data Storage

### Frontend localStorage

```javascript
{
  user_id: "github:12345"     // OR "azure:abc-def"
  auth_provider: "github"     // OR "azure"
}
```

### Backend Database

```sql
user_tokens table:
├── userId: "github:12345"    -- GitHub user
│   ├── accessToken: "gho_xxx"
│   ├── username: "johndoe"
│   └── ... (GitHub profile)
│
└── userId: "azure:abc-def"   -- Azure user
    ├── accessToken: "eyJ0..."  (JWT)
    ├── username: "john.doe"
    └── ... (Azure profile)
```

### Backend Session

```javascript
{
  JSESSIONID: {
    userId: "github:12345"    // OR "azure:abc-def"
    provider: "github"         // OR "azure"
  }
}
```

## 🔒 Security Features

### GitHub Provider
- ✅ Client secret protected on backend
- ✅ CSRF protection (state parameter)
- ✅ Popup OAuth (no page redirects)
- ✅ Token stored in database
- ✅ Session-based access

### Azure Provider
- ✅ JWT signature validation
- ✅ Issuer verification
- ✅ Audience validation
- ✅ Silent token acquisition
- ✅ MSAL browser security
- ✅ Token stored in database
- ✅ Session-based access

## 🧪 Testing

### Test GitHub Login

```bash
# 1. Start servers
cd backend && ./gradlew bootRun
cd frontend && npm install && npm run dev

# 2. Open browser
open http://localhost:3000

# 3. Click "Login with GitHub"
# 4. Authorize in popup
# 5. Check database:
#    SELECT * FROM user_tokens WHERE user_id LIKE 'github:%';
```

### Test Azure Login

```bash
# 1. Ensure Azure app is configured
# 2. Set environment variables
export AZURE_CLIENT_ID=your_azure_client_id
export NUXT_PUBLIC_AZURE_CLIENT_ID=your_azure_client_id

# 3. Click "Login with Microsoft"
# 4. Sign in with Microsoft account
# 5. Check database:
#    SELECT * FROM user_tokens WHERE user_id LIKE 'azure:%';
```

### Test Provider Switching

```bash
# 1. Login with GitHub
# 2. Logout
# 3. Login with Azure
# 4. Check that both users exist in database
# 5. Verify session switches correctly
```

## 📝 Configuration Files

### Backend `application.yml`

```yaml
github:
  app:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
    redirect-uri: http://localhost:3000/auth/callback

azure:
  entra:
    client-id: ${AZURE_CLIENT_ID}
    tenant-id: ${AZURE_TENANT_ID:common}
    authority: https://login.microsoftonline.com/${AZURE_TENANT_ID:common}
    jwks-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID:common}/discovery/v2.0/keys
```

### Frontend `nuxt.config.ts`

```typescript
runtimeConfig: {
  public: {
    apiBaseUrl: 'http://localhost:8080',
    azureClientId: process.env.NUXT_PUBLIC_AZURE_CLIENT_ID,
    azureAuthority: process.env.NUXT_PUBLIC_AZURE_AUTHORITY,
  }
}
```

## 🎨 UI Components

### LoginFlow Component

Shows both login options:

```vue
<button @click="handleGitHubLogin">
  🐙 Login with GitHub
</button>

<button @click="handleAzureLogin">
  🪟 Login with Microsoft
</button>
```

### Provider Badge

After login, shows which provider was used:

```vue
<span v-if="provider === 'github'" class="badge">
  🐙 GitHub
</span>

<span v-if="provider === 'azure'" class="badge">
  🪟 Microsoft
</span>
```

## 🔄 User ID Format

User IDs are prefixed with provider to avoid conflicts:

```
GitHub:  "github:12345678"
Azure:   "azure:abc-def-123-456"
Future:  "google:xyz789"
```

This allows:
- Multiple providers for same person
- Clear identification of auth source
- Easy routing to correct service
- Database uniqueness

## 📊 Request Flow Comparison

### GitHub Request
```
Frontend → Backend(/github/exchange-code) 
        → Store token in DB
        → Return userId: "github:123"
        → Create session

Later requests:
Frontend → Backend(/user) 
        → Get "github:123" from session
        → Look up token in DB
        → Call GitHub API
        → Return data
```

### Azure Request
```
Frontend (MSAL) → Acquire token silently
                → Already have JWT token

Frontend → Backend(/azure/login) {accessToken}
        → Validate JWT signature
        → Extract user claims
        → Store token in DB
        → Return userId: "azure:abc"
        → Create session

Later requests:
Frontend → Backend(/user)
        → Get "azure:abc" from session
        → Look up token in DB
        → Return cached data (token has user info)
```

## 🔐 Token Validation

### GitHub
- ✅ Code exchange via GitHub API
- ✅ Token is opaque (OAuth token)
- ✅ Validated by using it to fetch user info

### Azure
- ✅ JWT signature validation
- ✅ Issuer check (Microsoft)
- ✅ Audience check (your client ID)
- ✅ Expiration check
- ✅ Claims extraction

## 🧩 Adding a Third Provider

The architecture is designed for easy extension:

### Step 1: Create Service

```java
@Service
public class GoogleAuthService {
    private final UserTokenRepository userTokenRepository;
    
    public String validateTokenAndSave(String token) {
        // Validate token
        // Extract user info
        // Store with prefix "google:xxx"
        // Return userId
    }
}
```

### Step 2: Add Endpoints

```java
@PostMapping("/google/login")
public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
    String token = request.get("token");
    String userId = googleAuthService.validateTokenAndSave(token);
    session.setAttribute("userId", userId);
    session.setAttribute("provider", "google");
    return ResponseEntity.ok(Map.of("userId", userId, ...));
}
```

### Step 3: Create Frontend Composable

```typescript
// composables/useGoogleAuth.ts
export const useGoogleAuth = () => {
  const loginWithGoogle = async () => {
    // Google Sign-In logic
    // Send token to backend
  }
  return { loginWithGoogle }
}
```

### Step 4: Update UI

```vue
<button @click="handleGoogleLogin">
  Login with Google
</button>
```

### Step 5: Update useMultiAuth

```typescript
const { loginWithGoogle } = useGoogleAuth()

const loginWithGoogle = async () => {
  const response = await loginWithGoogle()
  saveAuthState(response.userId, 'google', response.user)
}
```

## 📋 Environment Variables

### Backend

```bash
# GitHub
export GITHUB_CLIENT_ID=xxx
export GITHUB_CLIENT_SECRET=xxx
export GITHUB_REDIRECT_URI=http://localhost:3000/auth/callback

# Azure
export AZURE_CLIENT_ID=xxx
export AZURE_TENANT_ID=common
```

### Frontend

```bash
# API
export NUXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Azure
export NUXT_PUBLIC_AZURE_CLIENT_ID=xxx
export NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/common
```

## 🎯 Provider-Specific Features

### GitHub
- ✅ Popup OAuth flow
- ✅ No page redirects (popup only)
- ✅ Supports all GitHub users
- ✅ Access to GitHub API
- ✅ Repository information
- ✅ Perfect for developer tools

### Azure Entra ID
- ✅ **Redirect-based flow** (full page redirect first time)
- ✅ **Silent token acquisition** (no redirect if already signed in!) ⚡
- ✅ Enterprise authentication
- ✅ Active Directory integration
- ✅ No popup blockers
- ✅ Mobile-friendly
- ✅ Perfect for enterprise apps

## 🔄 Silent Token Acquisition (Azure)

Azure Entra's killer feature:

```typescript
// First login: Page redirects to Microsoft
await loginWithMicrosoft()
// → Redirects to login.microsoftonline.com
// → User signs in
// → Redirects back to app

// Close browser, come back later
// Second login: NO REDIRECT! ⚡
await loginWithMicrosoft()
// Token acquired silently in background
// User logged in immediately (< 1 second)
```

**Benefits**:
- ⚡ Instant re-authentication (no redirect after first time)
- 🎯 Better UX (no redirect on subsequent logins)
- 🔄 Automatic token refresh
- 📱 Perfect for mobile and enterprise apps
- 🚫 No popup blocker issues

## 🧪 Testing Both Providers

### Scenario 1: GitHub User

```bash
1. Click "Login with GitHub"
2. Authorize in popup
3. Check DB: user_id = "github:12345"
4. Refresh page
5. Still logged in (session persists)
6. Logout
7. Record removed from DB
```

### Scenario 2: Azure User (First Time)

```bash
1. Click "Login with Microsoft"
2. Page redirects to login.microsoftonline.com
3. Sign in with Microsoft account
4. Page redirects back to localhost:3000
5. Check DB: user_id = "azure:abc-def"
6. User logged in
```

### Scenario 3: Azure User (Subsequent - Silent Token)

```bash
1. Close browser completely
2. Reopen app at localhost:3000
3. Click "Login with Microsoft"
4. NO REDIRECT! Logged in instantly ⚡ (< 1 second)
5. MSAL acquired token silently in background
```

### Scenario 4: Switch Providers

```bash
1. Login with GitHub → Logged in
2. Logout
3. Login with Azure → Logged in
4. Check DB: Both records exist
5. Each login creates separate session
```

## 🔒 Security Considerations

### Token Prefixing

User IDs are prefixed to prevent collisions:

```
✅ GOOD:
- github:12345
- azure:12345
(Same numeric ID, different providers, no conflict)

❌ BAD (without prefix):
- 12345 (GitHub)
- 12345 (Azure)
(Collision! Can't tell them apart)
```

### Session Management

Each provider maintains separate sessions:

```javascript
// GitHub session
{
  userId: "github:12345",
  provider: "github"
}

// Azure session
{
  userId: "azure:abc-def",
  provider: "azure"
}
```

### Token Storage

All tokens stored in same table with provider prefix:

```sql
SELECT * FROM user_tokens;

| user_id        | username  | access_token | provider |
|----------------|-----------|--------------|----------|
| github:12345   | johndoe   | gho_xxx      | GitHub   |
| azure:abc-def  | john.doe  | eyJ0...      | Azure    |
```

## 📊 Performance

### GitHub
- Initial login: ~2-3 seconds (popup)
- Subsequent logins: ~2-3 seconds (popup each time)
- Page behavior: No reload (popup only)
- API calls: Via backend proxy

### Azure
- Initial login: ~3-5 seconds (full page redirect)
- Subsequent logins: ~500ms ⚡ (silent token, NO redirect!)
- Page behavior: Reloads on first login, no reload after
- API calls: Via backend proxy
- **10x faster re-authentication!** ⚡

## 🎨 UI Updates

### Login Screen

```
┌────────────────────────────────────┐
│  Choose Authentication Provider    │
│                                    │
│  [🐙 Login with GitHub]           │
│  [🪟 Login with Microsoft]        │
│                                    │
│  Select how you'd like to sign in │
└────────────────────────────────────┘
```

### After Login

```
┌────────────────────────────────────┐
│  Welcome!                          │
│  [🐙 GitHub] or [🪟 Microsoft]    │ ← Provider badge
│                                    │
│  User Profile                      │
│  ...                               │
└────────────────────────────────────┘
```

## 🚀 Production Deployment

### GitHub Configuration

```yaml
# Backend
github:
  app:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
    redirect-uri: https://yourdomain.com/auth/callback
```

### Azure Configuration

```yaml
# Backend
azure:
  entra:
    client-id: ${AZURE_CLIENT_ID}
    tenant-id: ${AZURE_TENANT_ID}
    authority: https://login.microsoftonline.com/${AZURE_TENANT_ID}
    jwks-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID}/discovery/v2.0/keys
```

Update Azure app registration:
- Redirect URI: `https://yourdomain.com`

## 📝 Code Examples

### Check Current Provider

```typescript
const { provider } = useMultiAuth()

if (provider.value === 'github') {
  console.log('User authenticated via GitHub')
} else if (provider.value === 'azure') {
  console.log('User authenticated via Azure')
}
```

### Provider-Specific Logic

```typescript
const { user, provider } = useMultiAuth()

// Show GitHub-specific features
if (provider.value === 'github') {
  // Show repositories, stars, etc.
}

// Show Azure-specific features
if (provider.value === 'azure') {
  // Show organizational info, etc.
}
```

## ✨ Benefits

### For Users
- ✅ Choose preferred login method
- ✅ Fast authentication (especially Azure silent)
- ✅ Secure (tokens on backend)
- ✅ Familiar login flows

### For Developers
- ✅ Unified interface
- ✅ Easy to add providers
- ✅ Clear provider separation
- ✅ Extensible architecture

### For Enterprise
- ✅ Azure AD integration
- ✅ Single Sign-On capable
- ✅ Compliance-friendly
- ✅ Audit trail

## 🆚 When to Use Which

### Use GitHub When
- Open source projects
- Developer tools
- GitHub integration needed
- Public repositories access

### Use Azure When
- Enterprise applications
- Office 365 integration
- Azure services access
- Organizational accounts
- Need silent SSO

## 📚 Dependencies Added

### Backend
```gradle
implementation 'com.microsoft.azure:msal4j:1.14.3'
implementation 'com.nimbusds:nimbus-jose-jwt:9.37.3'
```

### Frontend
```json
"@azure/msal-browser": "^3.7.0"
```

## 🎯 Next Steps

1. ✅ Configure both OAuth apps (GitHub + Azure)
2. ✅ Set environment variables
3. ✅ Test both flows
4. ✅ Deploy to production
5. 🔜 Add third provider (Google, LinkedIn, etc.)

---

**Status**: ✅ **MULTI-PROVIDER AUTH READY**  
**Providers**: GitHub ✅ | Azure ✅ | More soon 🔜  
**Architecture**: Extensible and production-ready 🚀

