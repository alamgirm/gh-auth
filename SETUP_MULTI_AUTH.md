# Multi-Provider Authentication Setup

## 🎯 Quick Setup Guide

Set up **both GitHub and Azure Entra authentication** in your application.

## 📋 Prerequisites

- Java 17+
- Node.js 18+
- GitHub account
- Azure account (for Azure Entra setup)

## 🔧 Step-by-Step Setup

### Step 1: GitHub OAuth App (3 minutes)

1. Go to https://github.com/settings/developers
2. Click **"New OAuth App"**
3. Configure:
   ```
   Application name: Multi-Auth Demo
   Homepage URL: http://localhost:3000
   Callback URL: http://localhost:3000/auth/callback
   ```
4. Get **Client ID** and **Client Secret**

### Step 2: Azure Entra App (5 minutes)

1. Go to https://portal.azure.com
2. Navigate to **Azure Active Directory** → **App registrations**
3. Click **"New registration"**
4. Configure:
   ```
   Name: Multi-Auth Demo
   Supported accounts: Multi-tenant and personal accounts
   Redirect URI: SPA → http://localhost:3000
   ```
5. Get **Application (client) ID** and **Directory (tenant) ID**

6. **Configure API Permissions**:
   - API permissions → Add permission
   - Microsoft Graph → Delegated → `User.Read`
   - Add permissions

7. **Configure Authentication**:
   - Authentication → Implicit grant
   - ✅ Access tokens
   - ✅ ID tokens
   - Save

### Step 3: Backend Configuration (2 minutes)

Create `backend/.env` file:

```bash
# GitHub
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_REDIRECT_URI=http://localhost:3000/auth/callback

# Azure
AZURE_CLIENT_ID=your_azure_client_id
AZURE_TENANT_ID=common
```

Or export environment variables:

```bash
export GITHUB_CLIENT_ID=xxx
export GITHUB_CLIENT_SECRET=xxx
export AZURE_CLIENT_ID=xxx
export AZURE_TENANT_ID=common
```

### Step 4: Frontend Configuration (1 minute)

Create `frontend/.env` file:

```bash
NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
NUXT_PUBLIC_AZURE_CLIENT_ID=your_azure_client_id
NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/common
```

### Step 5: Install Dependencies

```bash
# Backend
cd backend
./gradlew build

# Frontend
cd frontend
npm install
```

### Step 6: Run Both Servers

```bash
# Terminal 1: Backend
cd backend
./gradlew bootRun

# Terminal 2: Frontend
cd frontend
npm run dev
```

### Step 7: Test!

```bash
# Open browser
open http://localhost:3000

# Test GitHub
1. Click "Login with GitHub"
2. Authorize in popup
3. ✅ Logged in!

# Test Azure (logout first)
1. Click "Logout"
2. Click "Login with Microsoft"
3. Sign in with Microsoft account
4. ✅ Logged in!

# Test Silent Token (Azure)
1. Stay logged in with Azure
2. Close browser completely
3. Reopen http://localhost:3000
4. Click "Login with Microsoft"
5. ✅ Logged in instantly (no popup)! ⚡
```

## 🔍 Verify Setup

### Check Backend Logs

```
Generating authorization URL with state: xxx           (GitHub)
Successfully exchanged code for access token          (GitHub)
GitHub session created for userId: 12345              (GitHub)

Validating Azure Entra token                          (Azure)
Token validated for user: john.doe@company.com        (Azure)
Azure token stored for user: john.doe@company.com     (Azure)
Azure session created for user: john.doe@company.com  (Azure)
```

### Check Database

Access H2 Console: http://localhost:8080/h2-console

```sql
-- Check stored tokens
SELECT user_id, username, created_at 
FROM user_tokens;

-- Should see:
-- github:12345    | johndoe    | 2025-10-17 ...
-- azure:abc-def   | john.doe   | 2025-10-17 ...
```

### Check Frontend Console

```javascript
// GitHub login
Login successful: { login: 'johndoe', id: 12345, ... }
User authenticated with provider: github

// Azure login
Azure login successful: john.doe@company.com
Silent token acquired successfully  // ← Only on subsequent logins
Login successful: { login: 'john.doe', email: '...', ... }
User authenticated with provider: azure
```

## 🎨 UI Features

### Login Screen

Two prominent buttons:

```
┌──────────────────────────────────┐
│  Choose Authentication Provider  │
│                                  │
│  [🐙 Login with GitHub]         │
│  [🪟 Login with Microsoft]      │
└──────────────────────────────────┘
```

### After Login

Provider badge shows which method was used:

```
┌──────────────────────────────────┐
│  Welcome!                        │
│  [🐙 GitHub] ← Provider badge   │
│                                  │
│  @johndoe                        │
│  ...profile...                   │
└──────────────────────────────────┘
```

## 🐛 Troubleshooting

### GitHub Issues

**"redirect_uri_mismatch"**
- Verify callback URL: `http://localhost:3000/auth/callback`
- Must match in GitHub app settings

**Popup blocked**
- Allow popups for localhost:3000
- Check browser settings

### Azure Issues

**"AADSTS50011: Reply URL mismatch"**
- Verify redirect URI: `http://localhost:3000`
- Must be registered as SPA type

**"Silent token acquisition failed"**
- Normal for first login
- Should work on subsequent logins
- Falls back to popup automatically

**MSAL initialization error**
- Check `NUXT_PUBLIC_AZURE_CLIENT_ID` is set
- Verify client ID is correct

### General Issues

**CORS errors**
- Ensure backend CORS allows `http://localhost:3000`
- Check `credentials: 'include'` in all fetch calls

**Session not persisting**
- Check cookies are enabled
- Verify `credentials: 'include'` in API calls
- Look for `JSESSIONID` cookie in DevTools

## 📊 File Changes Summary

### Backend Files Added/Modified
- ✅ `AzureEntraConfig.java` - Azure configuration
- ✅ `AzureTokenValidationService.java` - JWT validation
- ✅ `UserToken.java` - Database entity
- ✅ `UserTokenRepository.java` - JPA repository
- ✅ `AuthController.java` - Multi-provider endpoints
- ✅ `GitHubAuthService.java` - Token storage
- ✅ `application.yml` - Azure config added
- ✅ `build.gradle` - MSAL and JWT dependencies

### Frontend Files Added/Modified
- ✅ `useAzureAuth.ts` - Azure MSAL composable
- ✅ `useMultiAuth.ts` - Unified auth interface
- ✅ `LoginFlow.vue` - Both login buttons
- ✅ `index.vue` - Provider badge
- ✅ `nuxt.config.ts` - Azure config
- ✅ `package.json` - MSAL dependency

### Documentation
- ✅ `MULTI_PROVIDER_AUTH.md` - Architecture guide
- ✅ `SETUP_MULTI_AUTH.md` - This file

## 🎯 Testing Checklist

- [ ] Backend starts without errors
- [ ] Frontend starts without errors
- [ ] Database creates user_tokens table
- [ ] GitHub login works
- [ ] GitHub token stored in DB with "github:" prefix
- [ ] Azure login works (popup)
- [ ] Azure token stored in DB with "azure:" prefix
- [ ] Azure silent login works (no popup on return)
- [ ] Provider badge shows correctly
- [ ] Logout works for both providers
- [ ] Can switch between providers
- [ ] Session persists on refresh
- [ ] Both providers can be used by same person

## 🚀 Production Checklist

- [ ] Update GitHub OAuth app for production URLs
- [ ] Update Azure app registration for production URLs
- [ ] Use PostgreSQL instead of H2
- [ ] Use Redis for session storage
- [ ] Enable HTTPS
- [ ] Set secure cookie flags
- [ ] Implement token encryption at rest
- [ ] Add rate limiting
- [ ] Set up monitoring
- [ ] Configure logging

## 💡 Pro Tips

### Tip 1: Silent Token is Amazing

Azure's silent token acquisition means:
- User logs in once
- Close and reopen browser
- Click "Login with Microsoft"
- **Logged in instantly with no popup!** ⚡

### Tip 2: Provider Prefixing

Always prefix user IDs:
```
github:12345
azure:abc-def
google:xyz789
```

This prevents ID collisions and makes provider clear.

### Tip 3: Cached vs Fresh

Use `/user/cached` for most requests (fast), use `/user` when you need fresh data:

```typescript
// Fast: Cached from database
const user = await $fetch('/api/auth/user/cached')

// Slower: Fresh from provider API
const user = await $fetch('/api/auth/user')
```

## 📞 Support

### Common Questions

**Q: Can one user login with both providers?**  
A: Yes! They'll have two separate accounts in the database.

**Q: How do I add a third provider?**  
A: Follow the pattern: Create service, add endpoints, create composable, update UI.

**Q: Does Azure silent token work offline?**  
A: No, it needs network to validate. But it doesn't show a popup!

**Q: Are GitHub and Azure tokens stored differently?**  
A: Both in same table, but GitHub tokens are OAuth, Azure tokens are JWTs.

## 🎉 You're Done!

You now have:
- ✅ GitHub OAuth (popup flow)
- ✅ Azure Entra (with silent tokens!)
- ✅ Unified backend API
- ✅ Secure token storage
- ✅ Ready for more providers

**Total setup time**: ~15 minutes  
**Providers supported**: 2 (with room for more!)  
**Security level**: High 🔒

---

**Happy authenticating! 🚀**

