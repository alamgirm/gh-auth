# Multi-Auth Application

A full-stack application demonstrating **dual authentication** with Azure Entra ID (primary) and GitHub (optional secondary) using Nuxt 3 and Spring Boot.

## 🎯 Architecture Overview

- **Azure Entra ID**: Primary authentication (required)
  - Token managed by MSAL in browser
  - Sent with every backend request in Authorization header
  - Backend validates JWT on each request (stateless)

- **GitHub**: Optional secondary authentication
  - Only needed for GitHub-specific features
  - Token stored securely on backend
  - Linked to Azure user account

## 🚀 Quick Start

### Prerequisites
- Node.js 18+
- Java 17+
- Azure Entra ID tenant and app registrations
- GitHub OAuth App (optional, for GitHub features)

### 1. Backend Setup

```bash
cd backend

# Configure environment variables
cp env.example .env
# Edit .env with your values:
# - BE_CLIENT_ID (Azure backend API client ID)
# - BE_TENANT_ID (Azure tenant ID)
# - GITHUB_CLIENT_ID (GitHub OAuth app ID)
# - GITHUB_CLIENT_SECRET (GitHub OAuth secret)

# Run backend
./run.sh
# Or: ./gradlew bootRun
```

Backend runs on: `http://localhost:8080`

### 2. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Configure environment variables
cp env.example .env
# Edit .env with your values:
# - NUXT_PUBLIC_AZURE_CLIENT_ID (Azure frontend SPA client ID)
# - NUXT_PUBLIC_AZURE_AUTHORITY (Azure authority URL)
# - NUXT_PUBLIC_AZURE_API_SCOPE (Backend API scope)

# Run frontend
npm run dev
```

Frontend runs on: `http://localhost:3000`

### 3. Open Browser

```bash
open http://localhost:3000
```

## 📚 Documentation

- **[NEW_ARCHITECTURE.md](NEW_ARCHITECTURE.md)** - Complete architecture guide, API documentation, and testing
- **[AZURE_BACKEND_API_SETUP.md](AZURE_BACKEND_API_SETUP.md)** - Step-by-step Azure app registration setup
- **[AZURE_SCOPE_CREATION_GUIDE.md](AZURE_SCOPE_CREATION_GUIDE.md)** - Visual guide for creating API scopes

## 🔑 Azure Configuration

You need **TWO** Azure App Registrations:

### Backend API App
- Platform: Web API
- Exposes API: `api://{backend-client-id}/access_as_user`
- Used for: JWT token validation

### Frontend SPA App
- Platform: Single Page Application
- Redirect URI: `http://localhost:3000`
- API Permissions: Access to backend API
- Used for: User authentication via MSAL

See [AZURE_BACKEND_API_SETUP.md](AZURE_BACKEND_API_SETUP.md) for detailed setup.

## 🔐 Security Features

✅ **Azure Token**
- Managed by MSAL in browser localStorage
- Automatic token refresh
- Validated on every backend request
- Stateless authentication

✅ **GitHub Token** (Optional)
- Stored securely on backend database
- Only accessible by owning Azure user
- Linked with composite key: `azure:{azureId}:github`
- Used for GitHub API calls on user's behalf

✅ **No Session Storage**
- Stateless backend (easy to scale)
- Token-based validation
- No session affinity needed

## 🎨 User Experience

### First Time User
1. Click "Sign in with Microsoft"
2. Redirected to Microsoft login
3. Sign in with Azure credentials
4. Redirected back to app
5. Logged in with Azure
6. (Optional) Click "Connect GitHub" for GitHub features

### Returning User
1. Visit app
2. MSAL acquires token silently
3. Instantly logged in (< 1 second)
4. Both Azure and GitHub status shown

## 🏗️ Tech Stack

### Frontend
- **Nuxt 3** - Vue 3 framework with SSR
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first styling
- **MSAL Browser** - Microsoft authentication library
- **$fetch** - HTTP client for API calls

### Backend
- **Spring Boot 3.2** - Java web framework
- **Spring WebFlux** - Reactive web support
- **Spring Data JPA** - Database access
- **H2 Database** - In-memory database (dev)
- **Nimbus JOSE+JWT** - JWT validation
- **Lombok** - Reduce boilerplate

## 📁 Project Structure

```
gh-device-flow/
├── backend/
│   ├── src/main/java/com/github/deviceflow/
│   │   ├── config/         # Configuration classes
│   │   ├── controller/     # REST controllers
│   │   ├── entity/         # JPA entities
│   │   ├── model/          # Data models
│   │   ├── repository/     # Data repositories
│   │   └── service/        # Business logic
│   ├── build.gradle        # Dependencies
│   ├── env.example         # Backend env template
│   └── run.sh             # Run script
│
├── frontend/
│   ├── components/         # Vue components
│   │   ├── GitHubConnections.vue  # Ghec and Ghes
│   │   ├── LoginFlow.vue
│   │   └── UserProfile.vue
│   ├── composables/        # Composables
│   │   ├── useAzureAuth.ts
│   │   └── useMultiAuth.ts
│   ├── pages/             # Page components
│   │   ├── auth/
│   │   │   └── callback/
│   │   │       ├── ghec.vue
│   │   │       └── ghes.vue
│   │   └── index.vue
│   ├── app.vue            # Root component
│   ├── nuxt.config.ts     # Nuxt configuration
│   └── env.example        # Frontend env template
│
└── Documentation/
    ├── README.md (this file)
    ├── NEW_ARCHITECTURE.md
    ├── AZURE_BACKEND_API_SETUP.md
    └── AZURE_SCOPE_CREATION_GUIDE.md
```

## 🧪 Testing

### Test Azure Authentication
```bash
1. Open http://localhost:3000
2. Click "Sign in with Microsoft"
3. Sign in with Azure credentials
4. Should see Azure user profile
5. Status: "Primary Authentication: ✓ Authenticated"
```

### Test GitHub Connection
```bash
1. While authenticated with Azure
2. Click "Connect GitHub"
3. Authorize in popup
4. Popup closes automatically
5. Status: "GitHub Integration: ✓ Connected"
6. See GitHub username and avatar
```

### Test Disconnect
```bash
1. Click "Disconnect GitHub"
2. Confirm
3. Status changes to "Not Connected"
4. GitHub data removed from database
```

### Test Logout
```bash
1. Click "Sign Out"
2. Azure logout (MSAL clears tokens)
3. Redirected to login page
4. Both Azure and GitHub cleared
```

## 🐛 Troubleshooting

### Backend Issues

**"GitHub client ID is empty"**
```bash
# Make sure you source .env before running
cd backend
source .env
./gradlew bootRun

# Or use the run script
./run.sh
```

**"Transaction required" errors**
```bash
# Fixed: @Transactional annotations added
# Restart backend if you see this
```

### Frontend Issues

**"MSAL not initialized"**
```bash
# Check environment variables
cat frontend/.env

# Should have:
# NUXT_PUBLIC_AZURE_CLIENT_ID=...
# NUXT_PUBLIC_AZURE_AUTHORITY=...
# NUXT_PUBLIC_AZURE_API_SCOPE=...
```

**"401 Unauthorized" on backend calls**
```bash
# Check backend .env has correct values:
# BE_CLIENT_ID should match backend app registration
# BE_TENANT_ID should match your Azure tenant

# Check frontend API scope matches backend:
# NUXT_PUBLIC_AZURE_API_SCOPE=api://{BE_CLIENT_ID}/access_as_user
```

## 📊 API Endpoints

### Azure User (Require Azure Token in Authorization Header)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/user` | Get current Azure user |
| GET | `/api/auth/status` | Get both auth statuses |

### GitHub Linking (Require Azure Token)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/github/authorize-url` | Get GitHub OAuth URL |
| POST | `/api/auth/github/link` | Link GitHub account |
| POST | `/api/auth/github/unlink` | Unlink GitHub account |

## 🎯 Use Cases

### Enterprise User (Azure Only)
```
User: employee@company.com
Authentication: Azure Entra ID
GitHub: Not connected
Result: Full app access
```

### Developer (Azure + GitHub)
```
User: dev@company.com
Authentication: Azure Entra ID
GitHub: Connected
Result: Full app access + GitHub features
```

## 🔄 Token Flow

```
Frontend Request:
GET /api/auth/status
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhb... (Azure JWT)

Backend:
1. Extract token from Authorization header
2. Validate JWT signature using Azure JWKS
3. Verify issuer and audience
4. Extract user info from claims
5. Return user data

If GitHub connected:
6. Look up GitHub token in DB: azure:{azureId}:github
7. Make GitHub API call with stored token
8. Return combined data
```

## 🚀 Production Considerations

### Backend
- [ ] Switch from H2 to PostgreSQL/MySQL
- [ ] Enable HTTPS
- [ ] Configure CORS for production domain
- [ ] Add rate limiting
- [ ] Enable logging to file/service
- [ ] Add health check endpoints
- [ ] Consider token refresh strategy for GitHub

### Frontend
- [ ] Update redirect URIs in Azure app registration
- [ ] Update CORS settings in backend
- [ ] Enable production build optimizations
- [ ] Add error tracking (e.g., Sentry)
- [ ] Add analytics
- [ ] Consider CDN for static assets

### Security
- [ ] Review MSAL cache options (localStorage vs memory)
- [ ] Implement token refresh for GitHub
- [ ] Add request signing/encryption if needed
- [ ] Regular security audits
- [ ] Monitor for suspicious activity

## 📝 License

MIT

## 👥 Contributing

This is a demonstration project. Feel free to fork and modify for your needs.

## 🙋 Support

For issues or questions:
1. Check [NEW_ARCHITECTURE.md](NEW_ARCHITECTURE.md) for detailed information
2. Review [AZURE_BACKEND_API_SETUP.md](AZURE_BACKEND_API_SETUP.md) for Azure configuration
3. Check backend/frontend logs for error messages

---

**Built with ❤️ using Nuxt 3 and Spring Boot**
