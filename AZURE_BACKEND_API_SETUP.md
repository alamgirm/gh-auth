# Azure Backend API Registration Setup

## 🎯 Proper Azure Architecture: Frontend + Backend API

We'll create **TWO app registrations**:
1. **Backend API** - Exposes protected API
2. **Frontend SPA** - Calls the backend API

## 📋 Step-by-Step Setup

### Part 1: Create Backend API App Registration (5 minutes)

#### Step 1: Register Backend API

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** → **App registrations**
3. Click **"New registration"**
4. Fill in:
   ```
   Name: My App Backend API
   Supported account types: Accounts in any organizational directory and personal Microsoft accounts
   Redirect URI: (leave empty - backend doesn't need redirect)
   ```
5. Click **"Register"**
6. **Copy these values**:
   - **Application (client) ID** → This is your `BACKEND_AZURE_CLIENT_ID`
   - **Directory (tenant) ID** → This is your `AZURE_TENANT_ID`

#### Step 2: Expose the API

1. In your **Backend API** app registration:
2. Click **"Expose an API"** (left menu)
3. Click **"Add"** next to "Application ID URI"
4. Accept the default: `api://your-backend-client-id`
   - Or use custom: `api://myapp-backend`
5. Click **"Save"**

#### Step 3: Add a Scope

1. Still in **"Expose an API"**
2. Click **"Add a scope"**
3. Fill in:
   ```
   Scope name: access_as_user
   Who can consent: Admins and users
   Admin consent display name: Access the backend API
   Admin consent description: Allows the app to access the backend API as the signed-in user
   User consent display name: Access the backend
   User consent description: Allows the app to access the backend on your behalf
   State: Enabled
   ```
4. Click **"Add scope"**
5. **Copy the full scope value**: `api://your-backend-client-id/access_as_user`

### Part 2: Create/Update Frontend App Registration (3 minutes)

#### Step 1: Register Frontend SPA (or use existing)

1. Go to **App registrations** → **"New registration"** (or edit existing)
2. Fill in:
   ```
   Name: My App Frontend
   Supported account types: Same as backend
   Redirect URI: Single-page application (SPA) → http://localhost:3000
   ```
3. Click **"Register"**
4. **Copy**: **Application (client) ID** → This is your `FRONTEND_AZURE_CLIENT_ID`

#### Step 2: Grant API Permissions

1. In your **Frontend** app registration:
2. Click **"API permissions"** (left menu)
3. Click **"Add a permission"**
4. Click **"My APIs"** tab
5. Select **"My App Backend API"** (your backend app)
6. Check **"access_as_user"**
7. Click **"Add permissions"**
8. Click **"Grant admin consent for [your organization]"** (if available)

#### Step 3: Configure Authentication

1. Click **"Authentication"** (left menu)
2. Under **Implicit grant and hybrid flows**:
   - ✅ Check "Access tokens (used for implicit flows)"
   - ✅ Check "ID tokens (used for implicit and hybrid flows)"
3. Under **Advanced settings**:
   - Allow public client flows: **Yes**
4. Click **"Save"**

## 🔧 Backend Configuration

Update your backend environment variables:

```bash
# Backend API app registration
export AZURE_CLIENT_ID=your_backend_client_id  # From Backend app registration
export AZURE_TENANT_ID=your_tenant_id

# Optional: Explicitly set expected audience
export AZURE_EXPECTED_AUDIENCE=api://your_backend_client_id
```

Update `backend/src/main/resources/application.yml`:

```yaml
azure:
  entra:
    client-id: ${AZURE_CLIENT_ID}
    tenant-id: ${AZURE_TENANT_ID}
    authority: https://login.microsoftonline.com/${AZURE_TENANT_ID}
    jwks-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID}/discovery/v2.0/keys
    issuer: https://login.microsoftonline.com/${AZURE_TENANT_ID}/v2.0
    expected-audience: ${AZURE_EXPECTED_AUDIENCE:api://${AZURE_CLIENT_ID}}
```

## 🔧 Frontend Configuration

Update your frontend to request tokens for the backend API:

```bash
# Frontend app registration
export NUXT_PUBLIC_AZURE_CLIENT_ID=your_frontend_client_id  # From Frontend app
export NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/your_tenant_id

# Backend API scope
export NUXT_PUBLIC_AZURE_API_SCOPE=api://your_backend_client_id/access_as_user
```

## 📝 Summary of What You Created

You now have:

1. **Backend API App Registration**
   - Client ID: `abc-123-backend`
   - Exposes: `api://abc-123-backend/access_as_user`
   - Purpose: Validates tokens meant for your backend

2. **Frontend SPA App Registration**
   - Client ID: `xyz-456-frontend`
   - Permissions: Can call `api://abc-123-backend/access_as_user`
   - Purpose: Gets tokens for your backend API

## 🔧 Configuration

### Backend Environment Variables

```bash
# Backend API app registration values
export AZURE_CLIENT_ID=your_backend_client_id  # From Backend app
export AZURE_TENANT_ID=your_tenant_id
export AZURE_EXPECTED_AUDIENCE=api://your_backend_client_id  # Auto-configured if not set
```

### Frontend Environment Variables

```bash
# Frontend SPA app registration values
export NUXT_PUBLIC_AZURE_CLIENT_ID=your_frontend_client_id  # From Frontend app
export NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/your_tenant_id

# Backend API scope (from "Expose an API")
export NUXT_PUBLIC_AZURE_API_SCOPE=api://your_backend_client_id/access_as_user
```

## 🎯 Complete Example

Let's say you have:
- Backend Client ID: `11111111-2222-3333-4444-555555555555`
- Frontend Client ID: `66666666-7777-8888-9999-000000000000`
- Tenant ID: `aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee`

### Backend `.env`
```bash
AZURE_CLIENT_ID=11111111-2222-3333-4444-555555555555
AZURE_TENANT_ID=aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee
```

### Frontend `.env`
```bash
NUXT_PUBLIC_AZURE_CLIENT_ID=66666666-7777-8888-9999-000000000000
NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee
NUXT_PUBLIC_AZURE_API_SCOPE=api://11111111-2222-3333-4444-555555555555/access_as_user
```

## 🚀 Start Servers

```bash
# Backend (Terminal 1)
cd backend
export AZURE_CLIENT_ID=your_backend_client_id
export AZURE_TENANT_ID=your_tenant_id
./gradlew bootRun

# Frontend (Terminal 2)
cd frontend
export NUXT_PUBLIC_AZURE_CLIENT_ID=your_frontend_client_id
export NUXT_PUBLIC_AZURE_API_SCOPE=api://your_backend_client_id/access_as_user
npm run dev
```

## 🧪 Test

```bash
open http://localhost:3000

1. Click "Login with Microsoft"
2. Page redirects to Microsoft login
3. Sign in
4. **Consent screen appears** (first time only)
   - Shows: "My App Frontend wants to access the backend"
   - Click "Accept"
5. Redirects back to app
6. Check backend logs:
   - "Token audience validated: [api://your-backend-client-id]"
   - "Token validated successfully"
   - "Azure token stored for user: user1@alamgir99gmail.onmicrosoft.com"
7. ✅ Login successful!
```

## 📊 Token Flow

### What the Frontend Requests

```typescript
// Frontend MSAL requests:
scopes: ['api://backend-client-id/access_as_user']

// Azure returns token with:
aud: "api://backend-client-id"  ✅ Matches backend!
```

### What the Backend Validates

```java
// Backend checks:
expectedAudience: "api://backend-client-id"
tokenAudience: "api://backend-client-id"
// ✅ Match! Token valid!
```

## 🔐 Security Benefits

### Before (Graph Tokens)
- ⚠️ Token meant for Microsoft Graph
- ⚠️ Not specifically for your backend
- ⚠️ Could be used against any Graph API

### After (Backend API Tokens)
- ✅ Token specifically for YOUR backend
- ✅ Can only be used with your API
- ✅ More secure and proper architecture
- ✅ Follows Microsoft best practices

## 📝 Update Environment Files

### `backend/.env`
```bash
# GitHub
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# Azure - Backend API App Registration
AZURE_CLIENT_ID=your_backend_api_client_id
AZURE_TENANT_ID=your_tenant_id
```

### `frontend/.env`
```bash
NUXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Azure - Frontend SPA App Registration
NUXT_PUBLIC_AZURE_CLIENT_ID=your_frontend_spa_client_id
NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/your_tenant_id
NUXT_PUBLIC_AZURE_API_SCOPE=api://your_backend_api_client_id/access_as_user
```

## 🐛 Troubleshooting

### "Invalid audience" error

**Check**: Frontend is requesting the right scope

```javascript
// In browser console after clicking login:
console.log('Requesting scopes:', ['api://backend-id/access_as_user'])

// After redirect:
const token = "paste-token-here"
const payload = JSON.parse(atob(token.split('.')[1]))
console.log('Token audience:', payload.aud)  // Should be "api://backend-id"
```

### "Consent required" error

This is normal the first time! User needs to consent to the scope.

**Solution**: Click "Accept" on the consent screen.

To avoid consent screen:
- Azure Portal → Backend app → API permissions
- Grant admin consent for your organization

### Still getting errors?

1. **Verify app registration setup**:
   - Backend app has "Expose an API" configured
   - Frontend app has API permission added
   - Permission is granted (green checkmark)

2. **Verify scopes match**:
   - Backend exposes: `api://backend-id/access_as_user`
   - Frontend requests: `api://backend-id/access_as_user`
   - Must be EXACT match!

3. **Check backend logs** for detailed validation errors

## ✅ Success Indicators

You'll know it's working when you see:

**Backend logs**:
```
Token claims - Subject: xxx, Issuer: https://login.microsoftonline.com/xxx/v2.0, Audience: [api://your-backend-id]
Token audience validated: [api://your-backend-id]
Token validated successfully
Azure token stored for user: user1@alamgir99gmail.onmicrosoft.com
Azure session created for user: user1@alamgir99gmail.onmicrosoft.com
```

**Frontend console**:
```
Requesting scopes: ['api://your-backend-id/access_as_user']
Azure redirect successful: user1@alamgir99gmail.onmicrosoft.com
Backend authentication successful
```

**Browser**:
- Logged in successfully
- Profile displayed
- No 401 errors

---

**Status**: ✅ Code updated to support backend API tokens  
**Next**: Follow the setup steps above to create app registrations  
**Time**: ~10 minutes for full setup


