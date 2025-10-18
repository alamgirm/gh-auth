# Ghec and Ghes Migration Summary

## 🎯 Overview

The application has been updated to support **both GitHub Enterprise Cloud (Ghec)** and **GitHub Enterprise Server (Ghes)** as optional secondary authentication providers alongside Azure Entra ID (primary).

## 📋 What Changed

### Terminology Update
- **GitHub** → **Ghec** (GitHub Enterprise Cloud / github.com)
- Added **Ghes** (GitHub Enterprise Server / self-hosted)

### Architecture
```
Azure Entra ID (Primary - Required)
├─ Ghec (Optional - github.com)
└─ Ghes (Optional - Enterprise Server)
```

## 🔧 Backend Changes

### Configuration Files

**`application.yml`**
```yaml
# Before
github:
  app:
    client-id: ${GITHUB_CLIENT_ID}
    ...

# After
ghec:
  app:
    client-id: ${GHEC_CLIENT_ID}
    redirect-uri: http://localhost:3000/auth/callback/ghec
    ...

ghes:
  app:
    enabled: ${GHES_ENABLED:false}
    base-url: ${GHES_BASE_URL}
    client-id: ${GHES_CLIENT_ID}
    redirect-uri: http://localhost:3000/auth/callback/ghes
    ...
```

### Config Classes

| File | Purpose |
|------|---------|
| `GhecOAuthConfig.java` | Configuration for github.com |
| `GhesOAuthConfig.java` | Configuration for Enterprise Server |
| ~~`GitHubOAuthConfig.java`~~ | Removed |

### Services

**GitHubAuthService**
- Now accepts `provider` parameter ("ghec" or "ghes")
- Methods: `getAuthorizationUrl(provider, state)`, `exchangeCodeWithoutStoring(provider, code)`
- Method: `isGhesEnabled()` - Check if Ghes is configured

**UserLinkingService**
- Updated to support both providers
- Storage key format: `azure:{azureId}:{provider}`
  - Example: `azure:12345:ghec`
  - Example: `azure:12345:ghes`

**AuthController**
- New endpoints with provider parameter:
  - `GET /api/auth/github/{provider}/authorize-url`
  - `POST /api/auth/github/{provider}/link`
  - `POST /api/auth/github/{provider}/unlink`
- Updated `/api/auth/status` to return both Ghec and Ghes status

### Models

**AuthStatus**
```java
// Before
private boolean githubConnected;
private GitHubUser githubUser;

// After
private boolean ghecConnected;
private boolean ghesConnected;
private GitHubUser ghecUser;
private GitHubUser ghesUser;
```

### Environment Variables

**Backend `.env`**
```bash
# Before
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
GITHUB_REDIRECT_URI=http://localhost:3000/auth/callback

# After
GHEC_CLIENT_ID=...
GHEC_CLIENT_SECRET=...
GHEC_REDIRECT_URI=http://localhost:3000/auth/callback/ghec

# New (Optional - leave empty if not using)
GHES_BASE_URL=
GHES_CLIENT_ID=
GHES_CLIENT_SECRET=
GHES_REDIRECT_URI=http://localhost:3000/auth/callback/ghes
```

## 🎨 Frontend Changes

### Composables

**useMultiAuth.ts**
```typescript
// Before
const githubUser = useState<any>('githubUser', () => null)
const isGitHubConnected = useState<boolean>('isGitHubConnected', () => false)
const connectGitHub = () => { ... }
const disconnectGitHub = () => { ... }

// After
const ghecUser = useState<any>('ghecUser', () => null)
const ghesUser = useState<any>('ghesUser', () => null)
const isGhecConnected = useState<boolean>('isGhecConnected', () => false)
const isGhesConnected = useState<boolean>('isGhesConnected', () => false)
const connectGitHub = (provider: 'ghec' | 'ghes') => { ... }
const disconnectGitHub = (provider: 'ghec' | 'ghes') => { ... }
```

### Components

**GitHubConnections.vue** (NEW)
- Replaces `GitHubConnection.vue` for multi-provider support
- Shows both Ghec and Ghes connection status
- Separate connect/disconnect buttons for each
- Only shows Ghes section if enabled

**Callback Pages**
- `pages/auth/callback/ghec.vue` - Handles Ghec OAuth callback
- `pages/auth/callback/ghes.vue` - Handles Ghes OAuth callback
- Both auto-detect provider from URL path

### Routes

| Route | Provider | Purpose |
|-------|----------|---------|
| `/auth/callback/ghec` | Ghec | github.com OAuth redirect |
| `/auth/callback/ghes` | Ghes | Enterprise Server OAuth redirect |

## 🗄️ Database Changes

### UserToken Storage

**Before:**
```
user_id: "azure:12345:github"
```

**After:**
```
user_id: "azure:12345:ghec"  ← github.com
user_id: "azure:12345:ghes"  ← Enterprise Server
```

A single Azure user can have both Ghec and Ghes accounts linked simultaneously.

## 🔌 API Endpoints

### Status Endpoint

**GET `/api/auth/status`**

**Before Response:**
```json
{
  "azureAuthenticated": true,
  "githubConnected": true,
  "azureUser": {...},
  "githubUser": {...}
}
```

**After Response:**
```json
{
  "azureAuthenticated": true,
  "ghecConnected": true,
  "ghesConnected": false,
  "azureUser": {...},
  "ghecUser": {...},
  "ghesUser": null
}
```

### Linking Endpoints

**Before:**
```
GET /api/auth/github/authorize-url
POST /api/auth/github/link
POST /api/auth/github/unlink
```

**After:**
```
GET /api/auth/github/{provider}/authorize-url    # provider = "ghec" or "ghes"
POST /api/auth/github/{provider}/link            # provider = "ghec" or "ghes"
POST /api/auth/github/{provider}/unlink          # provider = "ghec" or "ghes"
```

## 🎨 UI/UX Changes

### Connection Status Display

**Before:** Single "GitHub Integration" card

**After:** Two separate cards:
1. **Ghec (github.com)** - Gray card with GitHub logo
2. **Ghes (Enterprise Server)** - Blue card with "ENTERPRISE" badge

### User Experience

```
1. Login with Azure (required)
   ↓
2. See two optional connections:
   ┌─────────────────────────────┐
   │ Ghec (github.com)           │
   │ Status: ⚪ Not Connected    │
   │ [Connect Ghec]              │
   └─────────────────────────────┘
   
   ┌─────────────────────────────┐
   │ Ghes (Enterprise Server)    │
   │ Status: ⚪ Not Connected    │
   │ [Connect Ghes]              │
   └─────────────────────────────┘
   (Only shown if GHES_ENABLED=true)
```

### Connection Flow

```
User clicks "Connect Ghec"
   ↓
GET /api/auth/github/ghec/authorize-url
   ↓
Popup opens: github.com OAuth
   ↓
User authorizes
   ↓
Redirect to: /auth/callback/ghec
   ↓
POST /api/auth/github/ghec/link
   ↓
Token stored as: azure:{userId}:ghec
   ↓
✅ Ghec Connected!
```

## 🔧 Configuration

### Enabling Ghes

1. **Backend `.env`:**
   ```bash
   GHES_BASE_URL=https://github.yourcompany.com
   GHES_CLIENT_ID=your_ghes_client_id
   GHES_CLIENT_SECRET=your_ghes_client_secret
   GHES_REDIRECT_URI=http://localhost:3000/auth/callback/ghes
   ```

2. **Create Ghes OAuth App:**
   - Go to your GitHub Enterprise Server
   - Settings → Developer settings → OAuth Apps
   - Create new app with callback: `http://localhost:3000/auth/callback/ghes`

3. **Restart backend:**
   ```bash
   cd backend
   source .env
   ./gradlew bootRun
   ```

4. **Ghes card will appear in UI** automatically when configured

### Disabling Ghes

Simply leave `GHES_BASE_URL` and `GHES_CLIENT_ID` empty in backend `.env` and restart.  
The Ghes card will be hidden, showing only "Ghes not configured" message.

## 📊 Use Cases

### Use Case 1: Cloud Only
```
User needs: github.com repos only
Configuration: Only Ghec configured
Result:
✅ Azure (primary)
✅ Ghec (optional) - github.com access
⚪ Ghes (hidden) - not configured
```

### Use Case 2: Enterprise Only
```
User needs: Internal enterprise repos only
Configuration: Only Ghes configured
Result:
✅ Azure (primary)
⚪ Ghec (optional) - not needed
✅ Ghes (connected) - enterprise access
```

### Use Case 3: Both Platforms
```
User needs: Both github.com and enterprise repos
Configuration: Both Ghec and Ghes configured
Result:
✅ Azure (primary)
✅ Ghec (connected) - github.com access
✅ Ghes (connected) - enterprise access
```

## 🔐 Security

### Token Storage

| Provider | Storage Location | Format |
|----------|-----------------|--------|
| Azure | MSAL (browser localStorage) | JWT |
| Ghec | Backend database | OAuth token |
| Ghes | Backend database | OAuth token |

### Database Keys

```
azure:12345:ghec  → Ghec token for Azure user 12345
azure:12345:ghes  → Ghes token for Azure user 12345
```

Both can coexist for the same Azure user.

## 🧪 Testing

### Test Ghec Connection
```bash
1. Open http://localhost:3000
2. Login with Azure
3. Click "Connect Ghec"
4. Authorize on github.com
5. See: "Ghec: ✓ Connected" with github.com username
```

### Test Ghes Connection (if enabled)
```bash
1. Ensure GHES_ENABLED=true in backend
2. Open http://localhost:3000
3. Login with Azure
4. Click "Connect Ghes"
5. Authorize on your Enterprise Server
6. See: "Ghes: ✓ Connected" with enterprise username
```

### Test Dual Connection
```bash
1. Connect Ghec first
2. Then connect Ghes
3. Both should show as connected
4. Disconnect one - other remains connected
5. Can reconnect independently
```

## 📝 Migration Checklist

If upgrading from previous version:

- [x] Update backend `application.yml`
- [x] Create `GhecOAuthConfig.java` and `GhesOAuthConfig.java`
- [x] Delete old `GitHubOAuthConfig.java`
- [x] Update `GitHubAuthService` to support providers
- [x] Update `UserLinkingService` with provider parameter
- [x] Update `AuthController` with provider routes
- [x] Update `AuthStatus` model
- [x] Update backend `.env` file
- [x] Update `useMultiAuth.ts` composable
- [x] Create `GitHubConnections.vue` component
- [x] Create separate callback pages for ghec/ghes
- [x] Update `index.vue` to use new component
- [x] Update frontend `.env.example`
- [x] Test Ghec connection
- [x] Test Ghes connection (if enabled)
- [x] Update documentation

## 🎉 Benefits

### Flexibility
- ✅ Support both github.com and self-hosted GitHub
- ✅ Each can be enabled/disabled independently
- ✅ Users can connect to one or both

### Scalability
- ✅ Easy to add more GitHub instances (Ghes2, Ghes3, etc.)
- ✅ Provider pattern is extensible

### Enterprise-Ready
- ✅ Supports corporate GitHub Enterprise deployments
- ✅ Clear separation between cloud and on-prem

### User Experience
- ✅ Clear visual distinction between providers
- ✅ Independent connection management
- ✅ Status always visible

## 📚 Related Documentation

- `README.md` - Quick start guide
- `NEW_ARCHITECTURE.md` - Complete architecture details
- `AZURE_BACKEND_API_SETUP.md` - Azure configuration
- `CLEANUP_SUMMARY.md` - Recent cleanup changes

---

**Migration Date**: October 18, 2025  
**Status**: ✅ Complete  
**Providers Supported**: Azure (primary), Ghec (optional), Ghes (optional)

