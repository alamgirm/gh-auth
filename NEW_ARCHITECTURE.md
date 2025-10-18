# New Architecture: Azure Primary + Optional GitHub

## ✅ Architecture Redesign Complete

The application now implements a **dual-authentication model** where:
- **Azure Entra ID** = Primary (required for app access)
- **GitHub** = Secondary (optional, for additional features)

## 🏗️ New Architecture

```
┌──────────────────────────────────────────────────────────┐
│  Frontend (Nuxt 3 + MSAL)                                 │
│                                                           │
│  MSAL Storage (localStorage):                            │
│  ├─ Azure account info                                   │
│  └─ Azure access token ✅                                │
│                                                           │
│  App State:                                              │
│  ├─ azureUser (from Azure token)                         │
│  ├─ githubUser (if connected)                            │
│  ├─ isAzureAuthenticated                                 │
│  └─ isGitHubConnected                                    │
└──────────────┬───────────────────────────────────────────┘
               │
               │ All requests include:
               │ Authorization: Bearer {azure_token}
               ▼
┌──────────────────────────────────────────────────────────┐
│  Backend (Spring Boot)                                    │
│  ┌────────────────────────────────────────────────────┐ │
│  │  On Every Request:                                 │ │
│  │  1. Validate Azure JWT token                       │ │
│  │  2. Extract user ID from token                     │ │
│  │  3. Process request for that user                  │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Database (user_tokens):                           │ │
│  │  └─ azure:{userId}:github → GitHub OAuth token    │ │
│  │     (Only stores GitHub tokens for linked accounts)│ │
│  └────────────────────────────────────────────────────┘ │
└──────────────┬───────────────────────────────────────────┘
               │
               ├──→ Validates Azure JWT (no storage)
               └──→ Uses stored GitHub token (if linked)
```

## 🔄 Authentication Flows

### 1. Azure Login (Primary - Required)

```
User Not Authenticated
   ↓
Click "Sign in with Microsoft"
   ↓
Page redirects to login.microsoftonline.com
   ↓
User signs in
   ↓
Redirects back to app
   ↓
MSAL stores token in browser
   ↓
Frontend: GET /api/auth/user
   Headers: Authorization: Bearer {azure_token}
   ↓
Backend validates JWT
   ↓
Backend returns user info
   ↓
✅ User authenticated with Azure
```

### 2. GitHub Connection (Optional - After Azure)

```
User authenticated with Azure
   ↓
UI shows: "GitHub Integration" (Not Connected)
   ↓
Click "Connect GitHub"
   ↓
GET /api/auth/github/authorize-url
   Headers: Authorization: Bearer {azure_token}
   ↓
Backend verifies Azure token first
   ↓
Opens GitHub OAuth popup
   ↓
User authorizes GitHub
   ↓
POST /api/auth/github/link {code}
   Headers: Authorization: Bearer {azure_token}
   ↓
Backend:
   ├─ Validates Azure token → Gets Azure user ID
   ├─ Exchanges code for GitHub token
   ├─ Stores: "azure:{azureId}:github" → GitHub token
   └─ Returns GitHub user info
   ↓
✅ GitHub connected to Azure account
```

## 💾 Data Storage

### Frontend (MSAL - localStorage)
```javascript
// MSAL manages these automatically:
{
  "msal.account.keys": [...],
  "msal.token.keys.{clientId}": [...],
  "{clientId}.{tenantId}.idtoken": "eyJ0...",
  "{clientId}.{tenantId}.accesstoken": "eyJ0..."  ← Azure token here
}
```

### Backend Database
```sql
user_tokens table:
-- No Azure tokens stored!
-- Only GitHub tokens for linked accounts:

| user_id                        | username  | access_token |
|--------------------------------|-----------|--------------|
| azure:abc-123-def:github       | johndoe   | gho_xxxxx   |
| azure:xyz-456-ghi:github       | janedoe   | gho_yyyyy   |
```

## 🔌 API Endpoints

### Azure User Endpoints (Require Azure Token)

| Method | Endpoint | Headers | Description |
|--------|----------|---------|-------------|
| GET | `/api/auth/user` | `Authorization: Bearer {azure_token}` | Get current Azure user |
| GET | `/api/auth/status` | `Authorization: Bearer {azure_token}` | Get both auth statuses |

### GitHub Linking Endpoints (Require Azure Token)

| Method | Endpoint | Headers | Description |
|--------|----------|---------|-------------|
| GET | `/api/auth/github/authorize-url` | `Authorization: Bearer {azure_token}` | Get GitHub OAuth URL |
| POST | `/api/auth/github/link` | `Authorization: Bearer {azure_token}` | Link GitHub account |
| POST | `/api/auth/github/unlink` | `Authorization: Bearer {azure_token}` | Unlink GitHub account |

## 🎨 UI Layout

### Before Azure Login
```
┌──────────────────────────────────────┐
│                                      │
│   🪟 Sign in with Microsoft          │
│                                      │
│   Azure Entra ID authentication      │
│   is required to use this app        │
│                                      │
│   [Sign in with Microsoft]           │
└──────────────────────────────────────┘
```

### After Azure Login
```
┌──────────────────────────────────────┐
│  Welcome!                            │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ 🪟 Primary Authentication       │ │
│  │                                 │ │
│  │ user1@alamgir99gmail.onmicrosoft.com │
│  │ Status: ✓ Authenticated        │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ 🐙 GitHub Integration           │ │
│  │                                 │ │
│  │ Status: ⚪ Not Connected        │ │
│  │ [Connect GitHub]                │ │
│  └────────────────────────────────┘ │
│                                      │
│  [Sign Out]                          │
└──────────────────────────────────────┘
```

### After GitHub Connected
```
┌──────────────────────────────────────┐
│  Welcome!                            │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ 🪟 Primary Authentication       │ │
│  │ user1@alamgir99gmail.onmicrosoft.com │
│  │ Status: ✓ Authenticated        │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ 🐙 GitHub Integration           │ │
│  │                                 │ │
│  │ Status: ✓ Connected             │ │
│  │ @johndoe                        │ │
│  │ [Disconnect GitHub]             │ │
│  └────────────────────────────────┘ │
│                                      │
│  [Sign Out]                          │
└──────────────────────────────────────┘
```

## 🔒 Security Model

### Azure Token
- **Storage**: MSAL in browser localStorage (managed automatically)
- **Transmission**: Sent in `Authorization` header with every request
- **Validation**: Backend validates JWT signature on every request
- **Backend Storage**: NO - token not stored on backend
- **Renewal**: MSAL handles automatic refresh

### GitHub Token (When Connected)
- **Storage**: Backend database
- **Transmission**: Never sent to frontend
- **Validation**: Used by backend to make GitHub API calls
- **Backend Storage**: YES - stored as `azure:{azureId}:github`
- **Renewal**: Manual re-connection required

## 📊 Request Flow Examples

### Get User Info
```
Frontend                     Backend
   │                            │
   │  GET /api/auth/user        │
   │  Authorization: Bearer eyJ0...
   │───────────────────────────→│
   │                            │
   │                            │  Validate JWT
   │                            │  Extract user claims
   │                            │  Return user info
   │                            │
   │←───────────────────────────│
   │  {name, email, ...}        │
```

### Connect GitHub
```
Frontend                     Backend                    GitHub
   │                            │                          │
   │  GET /github/authorize-url │                          │
   │  Authorization: Bearer azure_token                    │
   │───────────────────────────→│                          │
   │                            │  Validate Azure token    │
   │←───────────────────────────│  Return GitHub OAuth URL │
   │  {url, state}              │                          │
   │                            │                          │
   │  Open popup with URL       │                          │
   │───────────────────────────────────────────────────────→│
   │                            │                          │
   │  Popup receives code       │                          │
   │                            │                          │
   │  POST /github/link {code}  │                          │
   │  Authorization: Bearer azure_token                    │
   │───────────────────────────→│                          │
   │                            │  Validate Azure token    │
   │                            │  Get Azure user ID       │
   │                            │  Exchange code for token │
   │                            │────────────────────────→│
   │                            │                          │
   │                            │  Store GitHub token as:  │
   │                            │  azure:{id}:github       │
   │                            │                          │
   │←───────────────────────────│                          │
   │  {githubUser}              │                          │
   │                            │                          │
   ✅ GitHub connected!         │                          │
```

## 🎯 Key Differences from Before

### What Changed

| Aspect | Before | After |
|--------|--------|-------|
| **Azure Role** | Alternative to GitHub | Primary (required) |
| **GitHub Role** | Alternative to Azure | Optional (additional) |
| **Azure Token** | Stored in DB | MSAL (frontend only) |
| **GitHub Token** | Stored in DB | Still stored in DB |
| **Sessions** | Server-side sessions | Stateless (token validation) |
| **Auth Check** | Session cookie | Azure token in header |

### Architecture Benefits

✅ **Azure Primary**:
- Enterprise authentication required
- MSAL handles token refresh automatically
- No backend token storage needed
- JWT contains user info

✅ **GitHub Optional**:
- Only connected when needed
- Token stored securely on backend
- Linked to Azure user ID
- Used for GitHub API features

✅ **Stateless Backend**:
- No session storage needed
- Validates Azure token on each request
- Easy to scale horizontally
- No session affinity required

## 🧪 Testing

### Test Azure Authentication

```bash
1. Open http://localhost:3000
2. Should see "Sign in with Microsoft"
3. Click button
4. Redirects to Microsoft login
5. Sign in with: user1@alamgir99gmail.onmicrosoft.com
6. Redirects back
7. Should see:
   - Azure user profile
   - "GitHub Integration: Not Connected"
```

### Test GitHub Connection

```bash
1. While authenticated with Azure
2. Click "Connect GitHub"
3. Popup opens with GitHub OAuth
4. Authorize GitHub
5. Popup closes
6. Should see:
   - "GitHub Integration: ✓ Connected"
   - GitHub username and avatar
7. Check backend logs:
   - "Linking GitHub account {username} to Azure user {azureId}"
```

### Test Stateless Auth

```bash
1. Refresh the page
2. Frontend gets Azure token from MSAL
3. Calls /api/auth/status with token
4. Backend validates token
5. Returns both Azure and GitHub status
6. UI shows both authentications
```

## 📝 Environment Variables

### Backend
```bash
# Azure Backend API
BE_CLIENT_ID=5640aba4-729d-4e6f-a34d-86beccf78df5
BE_TENANT_ID=a95973d1-dd8e-4baa-af11-d18f9863793b

# GitHub
GITHUB_CLIENT_ID=Iv23liqRwUp1z3IgnKe3
GITHUB_CLIENT_SECRET=your_github_secret
```

### Frontend
```bash
# Azure Frontend SPA
NUXT_PUBLIC_AZURE_CLIENT_ID=0f494122-cb51-41da-82be-3619a4fc352c
NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/a95973d1-dd8e-4baa-af11-d18f9863793b
NUXT_PUBLIC_AZURE_API_SCOPE=api://5640aba4-729d-4e6f-a34d-86beccf78df5/access_as_user
```

## 🔐 Security Benefits

### Azure Token (Frontend)
- ✅ Managed by Microsoft's MSAL library
- ✅ Automatic refresh before expiration
- ✅ HttpOnly storage options available
- ✅ Validated on every backend request
- ✅ Never stored on backend (stateless)

### GitHub Token (Backend)
- ✅ Stored securely in database
- ✅ Only accessible by owning Azure user
- ✅ Used for GitHub API calls on user's behalf
- ✅ Can be revoked anytime (unlink)
- ✅ Linked to Azure identity

## 📊 User Experience

### First Time User
```
1. Visit app → "Sign in with Microsoft"
2. Redirect → Sign in → Redirect back
3. See Azure profile
4. See "GitHub: Not Connected" with "Connect" button
5. Optionally click "Connect GitHub" if needed
```

### Returning User (MSAL Silent Token)
```
1. Visit app
2. MSAL acquires token silently ⚡
3. Instantly logged in (< 1 second)
4. Both Azure and GitHub status shown
```

## 🎯 Use Cases

### Use Case 1: Enterprise User (Azure Only)
```
User: employee@company.com
Needs: Access to app
Connects GitHub: No

Result:
✅ Full app access with Azure
⚪ GitHub features disabled
```

### Use Case 2: Developer (Azure + GitHub)
```
User: dev@company.com
Needs: App access + GitHub features
Connects GitHub: Yes

Result:
✅ Full app access with Azure
✅ GitHub API access enabled
✅ Can manage repositories, view code, etc.
```

## ✨ New Components

### Frontend
- ✅ **`GitHubConnection.vue`** - Shows GitHub status and connect/disconnect
- ✅ **`useMultiAuth.ts`** - Manages dual authentication
- ✅ Updated **`LoginFlow.vue`** - Azure-first login
- ✅ Updated **`index.vue`** - Shows both auth statuses

### Backend
- ✅ **`AuthStatus.java`** - Model for dual auth status
- ✅ **`UserLinkingService.java`** - Links GitHub to Azure users
- ✅ Updated **`AuthController.java`** - Token-based validation
- ✅ Updated **`AzureTokenValidationService.java`** - No storage

## 🔄 Migration from Previous Version

### What's Removed
- ❌ Session-based authentication for Azure
- ❌ Azure token storage in database
- ❌ `/api/auth/azure/login` endpoint
- ❌ `/api/auth/check` endpoint (replaced with `/status`)
- ❌ `/api/auth/logout` endpoint (Azure logout is client-side)

### What's Added
- ✅ Token-based authentication
- ✅ `/api/auth/status` endpoint
- ✅ `/api/auth/github/link` endpoint
- ✅ `/api/auth/github/unlink` endpoint
- ✅ GitHub connection UI component

## 📋 API Documentation

### GET /api/auth/user

Get current Azure user info.

**Headers**:
```
Authorization: Bearer {azure_access_token}
```

**Response**:
```json
{
  "id": 12345,
  "login": "user1",
  "name": "User One",
  "email": "user1@alamgir99gmail.onmicrosoft.com",
  "bio": "Azure Entra User"
}
```

### GET /api/auth/status

Get authentication status for both providers.

**Headers**:
```
Authorization: Bearer {azure_access_token}
```

**Response**:
```json
{
  "azureAuthenticated": true,
  "githubConnected": true,
  "azureUser": {...},
  "githubUser": {...},
  "message": "Authenticated"
}
```

### POST /api/auth/github/link

Link GitHub account to Azure user.

**Headers**:
```
Authorization: Bearer {azure_access_token}
```

**Body**:
```json
{
  "code": "github_oauth_code"
}
```

**Response**:
```json
{
  "message": "GitHub account linked successfully",
  "githubUser": {...}
}
```

## 🚀 Running the App

```bash
# Backend
cd /Users/alamgir/projects/gh-device-flow/backend
source .env
./gradlew bootRun

# Frontend
cd /Users/alamgir/projects/gh-device-flow/frontend
source .env
npm run dev

# Open
open http://localhost:3000
```

## ✅ Success Indicators

### Azure Login Success
```
Frontend console:
- "Silent token acquired successfully" (returning user)
- OR "Azure redirect successful: user1@..."
- "Azure token validated, user: {...}"

Backend logs:
- "Azure token validated for user: User One (user1@...)"

UI shows:
- Azure user profile
- "Primary Authentication: ✓ Authenticated"
```

### GitHub Connection Success
```
Frontend console:
- "Linking GitHub account..."

Backend logs:
- "Linking GitHub account johndoe to Azure user 12345"
- "GitHub account linked successfully"

UI shows:
- "GitHub Integration: ✓ Connected"
- GitHub username and avatar
- "Disconnect GitHub" button
```

## 📚 Documentation

- **[NEW_ARCHITECTURE.md](NEW_ARCHITECTURE.md)** - This file
- **[AZURE_BACKEND_API_SETUP.md](AZURE_BACKEND_API_SETUP.md)** - Azure app setup
- **[YOUR_CONFIGURATION.md](YOUR_CONFIGURATION.md)** - Your specific values

---

**Status**: ✅ **REDESIGNED & IMPLEMENTED**  
**Azure**: Primary authentication (MSAL managed)  
**GitHub**: Optional connection (backend stored)  
**Architecture**: Stateless with token validation

