# Your Azure Configuration Values

## ✅ Your App Registration Details

Based on your setup:

### Backend API App Registration
- **Client ID**: `5640aba4-729d-4e6f-a34d-86beccf78df5`
- **Purpose**: Validates tokens for your backend

### Frontend SPA App Registration
- **Client ID**: `0f494122-cb51-41da-82be-3619a4fc352c`
- **Purpose**: Requests tokens from Azure

### Tenant
- **Tenant ID**: `a95973d1-dd8e-4baa-af11-d18f9863793b`

## 🔧 Backend Configuration

### Environment Variables to Export

```bash
cd /Users/alamgir/projects/gh-device-flow/backend

# GitHub (if using)
export GITHUB_CLIENT_ID=Iv23liqRwUp1z3IgnKe3
export GITHUB_CLIENT_SECRET=your_github_secret_if_you_have_it

# Azure - Backend API
export BE_CLIENT_ID=5640aba4-729d-4e6f-a34d-86beccf78df5
export BE_TENANT_ID=a95973d1-dd8e-4baa-af11-d18f9863793b

# Now start backend
./gradlew bootRun
```

### Or Create `backend/.env` File

```bash
cd /Users/alamgir/projects/gh-device-flow/backend

cat > .env << 'EOF'
GITHUB_CLIENT_ID=Iv23liqRwUp1z3IgnKe3
GITHUB_CLIENT_SECRET=

BE_CLIENT_ID=5640aba4-729d-4e6f-a34d-86beccf78df5
BE_TENANT_ID=a95973d1-dd8e-4baa-af11-d18f9863793b
EOF
```

## 🔧 Frontend Configuration

### Environment Variables to Export

```bash
cd /Users/alamgir/projects/gh-device-flow/frontend

export NUXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Azure - Frontend SPA
export NUXT_PUBLIC_AZURE_CLIENT_ID=0f494122-cb51-41da-82be-3619a4fc352c
export NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/a95973d1-dd8e-4baa-af11-d18f9863793b
export NUXT_PUBLIC_AZURE_API_SCOPE=api://5640aba4-729d-4e6f-a34d-86beccf78df5/access_as_user

# Now start frontend
npm run dev
```

### Or Create `frontend/.env` File

```bash
cd /Users/alamgir/projects/gh-device-flow/frontend

cat > .env << 'EOF'
NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
NUXT_PUBLIC_AZURE_CLIENT_ID=0f494122-cb51-41da-82be-3619a4fc352c
NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/a95973d1-dd8e-4baa-af11-d18f9863793b
NUXT_PUBLIC_AZURE_API_SCOPE=api://5640aba4-729d-4e6f-a34d-86beccf78df5/access_as_user
EOF
```

## 🎯 What Each Value Means

### Backend

| Variable | Your Value | Purpose |
|----------|------------|---------|
| `BE_CLIENT_ID` | `5640aba4-729d-4e6f-a34d-86beccf78df5` | Backend API app's client ID |
| `BE_TENANT_ID` | `a95973d1-dd8e-4baa-af11-d18f9863793b` | Your tenant/directory ID |

### Frontend

| Variable | Your Value | Purpose |
|----------|------------|---------|
| `NUXT_PUBLIC_AZURE_CLIENT_ID` | `0f494122-cb51-41da-82be-3619a4fc352c` | Frontend SPA app's client ID |
| `NUXT_PUBLIC_AZURE_AUTHORITY` | `https://login.microsoftonline.com/a95973d1-dd8e-4baa-af11-d18f9863793b` | Tenant-specific login URL |
| `NUXT_PUBLIC_AZURE_API_SCOPE` | `api://5640aba4-729d-4e6f-a34d-86beccf78df5/access_as_user` | Scope for backend API |

## ✅ Verification Checklist

### Before Starting Servers

Check in Azure Portal:

**Backend App** (`5640aba4-729d-4e6f-a34d-86beccf78df5`):
- [ ] "Expose an API" is configured
- [ ] Application ID URI: `api://5640aba4-729d-4e6f-a34d-86beccf78df5`
- [ ] Scope created: `access_as_user`
- [ ] Full scope: `api://5640aba4-729d-4e6f-a34d-86beccf78df5/access_as_user`

**Frontend App** (`0f494122-cb51-41da-82be-3619a4fc352c`):
- [ ] API permissions added for backend app
- [ ] Permission: `api://5640aba4-729d-4e6f-a34d-86beccf78df5/access_as_user`
- [ ] Consent granted (green checkmark)
- [ ] Redirect URI set: `http://localhost:3000` (SPA type)

## 🚀 Start Commands

### Option 1: Using Environment Variables

```bash
# Terminal 1 - Backend
cd /Users/alamgir/projects/gh-device-flow/backend
export BE_CLIENT_ID=5640aba4-729d-4e6f-a34d-86beccf78df5
export BE_TENANT_ID=a95973d1-dd8e-4baa-af11-d18f9863793b
./gradlew bootRun

# Terminal 2 - Frontend
cd /Users/alamgir/projects/gh-device-flow/frontend
export NUXT_PUBLIC_AZURE_CLIENT_ID=0f494122-cb51-41da-82be-3619a4fc352c
export NUXT_PUBLIC_AZURE_AUTHORITY=https://login.microsoftonline.com/a95973d1-dd8e-4baa-af11-d18f9863793b
export NUXT_PUBLIC_AZURE_API_SCOPE=api://5640aba4-729d-4e6f-a34d-86beccf78df5/access_as_user
npm run dev
```

### Option 2: Create .env Files (Easier)

**Backend .env file**:
```bash
cd /Users/alamgir/projects/gh-device-flow/backend
nano .env
# Paste the backend values from above
# Save and exit (Ctrl+X, Y, Enter)
```

**Frontend .env file**:
```bash
cd /Users/alamgir/projects/gh-device-flow/frontend
nano .env
# Paste the frontend values from above
# Save and exit (Ctrl+X, Y, Enter)
```

Then just run:
```bash
./gradlew bootRun  # Backend
npm run dev        # Frontend
```

## 🧪 Test

```bash
open http://localhost:3000

1. Click "Login with Microsoft"
2. Should redirect to: 
   https://login.microsoftonline.com/a95973d1-dd8e-4baa-af11-d18f9863793b/...
3. Sign in with: user1@alamgir99gmail.onmicrosoft.com
4. Accept consent (if shown)
5. Redirects back
6. Should be logged in!
```

## 🔍 Expected Backend Logs

```
Token claims - Subject: xxx, Issuer: https://login.microsoftonline.com/a95973d1-dd8e-4baa-af11-d18f9863793b/v2.0, Audience: [api://5640aba4-729d-4e6f-a34d-86beccf78df5]
Token audience validated: [api://5640aba4-729d-4e6f-a34d-86beccf78df5]
Token validated successfully
Azure token stored for user: user1@alamgir99gmail.onmicrosoft.com
```

## 🔍 Expected Frontend Console

```
Requesting scopes: ['api://5640aba4-729d-4e6f-a34d-86beccf78df5/access_as_user']
Azure redirect successful: user1@alamgir99gmail.onmicrosoft.com
Backend authentication successful
```

## ⚠️ Important: Scope Must Exist in Azure

Before testing, make sure you've created the scope in Azure Portal:

1. Go to Backend app (`5640aba4-729d-4e6f-a34d-86beccf78df5`)
2. "Expose an API" → Should show:
   ```
   Application ID URI: api://5640aba4-729d-4e6f-a34d-86beccf78df5
   Scope: access_as_user
   Full value: api://5640aba4-729d-4e6f-a34d-86beccf78df5/access_as_user
   ```

If not created yet, follow [AZURE_SCOPE_CREATION_GUIDE.md](AZURE_SCOPE_CREATION_GUIDE.md)!

---

**Your configuration looks correct!** 🎉  
**Next**: Create the scope in Azure Portal, then test!

