# GitHub App OAuth Setup Guide

## ✅ Migration Complete: Device Flow → Popup OAuth

The application has been migrated from **Device Flow** to **GitHub App OAuth** with **popup-based authentication**.

## 🔄 What Changed

### Before (Device Flow)
- User clicked login
- Device code displayed
- User manually entered code on GitHub
- Frontend polled backend every 10 seconds
- Waited up to 15 minutes for authorization

### After (Popup OAuth)
- User clicks "Login with GitHub"
- Popup window opens with GitHub authorization
- User authorizes in popup
- Popup automatically closes
- User is logged in instantly
- **No page redirects on main window**

## 🎯 Benefits

✅ **Faster** - No waiting, instant login  
✅ **Better UX** - No manual code entry  
✅ **Popup-based** - No full-page redirects  
✅ **Standard OAuth** - Well-supported flow  
✅ **Simpler** - Less backend complexity  

## 📋 GitHub App Setup

### Step 1: Create a GitHub OAuth App

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click **"New OAuth App"** (NOT "New GitHub App")
3. Fill in the application details:

```
Application name: Your App Name
Homepage URL: http://localhost:3000
Application description: Your app description (optional)
Authorization callback URL: http://localhost:3000/auth/callback
```

4. Click **"Register application"**
5. You'll see your **Client ID**
6. Click **"Generate a new client secret"**
7. Copy your **Client Secret** (you won't see it again!)

### Step 2: Configure Backend

Update `backend/src/main/resources/application.yml`:

```yaml
github:
  app:
    client-id: YOUR_CLIENT_ID_HERE
    client-secret: YOUR_CLIENT_SECRET_HERE
    redirect-uri: http://localhost:3000/auth/callback
```

Or use environment variables:

```bash
export GITHUB_CLIENT_ID=your_client_id
export GITHUB_CLIENT_SECRET=your_client_secret
export GITHUB_REDIRECT_URI=http://localhost:3000/auth/callback
```

### Step 3: Run the Application

```bash
# Backend
cd backend
./gradlew bootRun

# Frontend (new terminal)
cd frontend
npm run dev
```

### Step 4: Test

1. Open `http://localhost:3000`
2. Click "Login with GitHub"
3. Popup opens with GitHub login
4. Authorize the app
5. Popup closes automatically
6. You're logged in! ✅

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│  Main Window (localhost:3000)           │
│  ┌───────────────────────────────────┐  │
│  │  [Login with GitHub] Button       │  │
│  └───────────────────────────────────┘  │
└──────────────┬──────────────────────────┘
               │ Click
               ▼
┌─────────────────────────────────────────┐
│  Popup Window                            │
│  https://github.com/login/oauth/        │
│  authorize?client_id=xxx                 │
│                                          │
│  User authorizes ✓                       │
└──────────────┬──────────────────────────┘
               │ Redirect
               ▼
┌─────────────────────────────────────────┐
│  Popup Callback                          │
│  localhost:3000/auth/callback?code=xxx   │
│                                          │
│  Extracts code, sends to parent         │
└──────────────┬──────────────────────────┘
               │ postMessage
               ▼
┌─────────────────────────────────────────┐
│  Main Window                             │
│  Receives code                           │
│  Sends to backend                        │
└──────────────┬──────────────────────────┘
               │ POST /api/auth/exchange-code
               ▼
┌─────────────────────────────────────────┐
│  Backend                                 │
│  Exchanges code for access token        │
│  Fetches user info                       │
│  Returns token + user                    │
└──────────────┬──────────────────────────┘
               │
               ▼
           ✅ Logged In!
```

## 🔌 API Endpoints

### Backend

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/authorize-url` | Get GitHub OAuth URL |
| POST | `/api/auth/exchange-code` | Exchange code for token |
| GET | `/api/auth/verify` | Verify access token |
| GET | `/api/auth/health` | Health check |

### Frontend

| Route | Purpose |
|-------|---------|
| `/` | Main application page |
| `/auth/callback` | OAuth callback handler (popup) |

## 🔒 Security Features

1. **CSRF Protection** - Random state parameter
2. **Origin Validation** - postMessage origin check
3. **Popup Communication** - window.postMessage API
4. **HTTPS Ready** - Secure in production
5. **Client Secret Protected** - Only on backend

## 📝 Files Changed

### Backend
- ✅ `GitHubOAuthConfig.java` - Updated config structure
- ✅ `GitHubAuthService.java` - NEW: OAuth service
- ✅ `AuthController.java` - Updated endpoints
- ✅ `application.yml` - Updated configuration
- ⚠️ `GitHubDeviceFlowService.java` - No longer used (can delete)

### Frontend
- ✅ `useAuth.ts` - Rewritten for popup OAuth
- ✅ `LoginFlow.vue` - Simplified to button
- ✅ `auth/callback.vue` - NEW: Callback page

## 🧪 Testing

### Test Popup Blocker

If popups are blocked:
- User will see error: "Failed to open popup"
- Browser will show blocked popup icon
- User needs to allow popups for the site

### Test State Mismatch

The app validates the OAuth state parameter to prevent CSRF attacks.

### Test Network Errors

The app handles:
- Backend unavailable
- GitHub API errors
- Network timeouts
- Invalid tokens

## 🚀 Production Deployment

### Update GitHub OAuth App

1. Go to your OAuth App settings
2. Update **Homepage URL** to your production URL
3. Update **Authorization callback URL** to:
   ```
   https://yourdomain.com/auth/callback
   ```

### Backend Configuration

```yaml
github:
  app:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
    redirect-uri: https://yourdomain.com/auth/callback

cors:
  allowed-origins: https://yourdomain.com
```

### Frontend Configuration

```typescript
// nuxt.config.ts
runtimeConfig: {
  public: {
    apiBaseUrl: 'https://api.yourdomain.com'
  }
}
```

## 🔄 Migration from Device Flow

### What to Delete

You can safely remove these files (no longer used):
- `backend/.../model/DeviceCodeResponse.java` (optional)
- `backend/.../model/PollStatusResponse.java` (optional)
- Old device flow polling logic (if any)

### What to Keep

These files are still used:
- ✅ `AccessTokenResponse.java`
- ✅ `GitHubUser.java`
- ✅ `UserProfile.vue`

## 🆘 Troubleshooting

### Popup is blocked

**Solution**: Allow popups in browser settings

### "Failed to communicate with parent window"

**Solution**: Check that callback URL matches exactly:
- GitHub OAuth App setting
- Backend `redirect-uri`
- Must be same origin as main app

### CORS errors

**Solution**: Update `application.yml`:
```yaml
cors:
  allowed-origins: http://localhost:3000
```

### State mismatch error

**Solution**: This is a security feature. Make sure:
- Not using browser "back" button
- Completing auth flow in one session
- Cookies are enabled

## 📊 Comparison: Device Flow vs Popup OAuth

| Feature | Device Flow | Popup OAuth |
|---------|-------------|-------------|
| **Speed** | Slow (polling) | Fast (instant) |
| **UX** | Manual code entry | Automatic |
| **Redirects** | None | Popup only |
| **Complexity** | High | Low |
| **Backend Load** | High (polling) | Low |
| **Timeout** | 15 minutes | ~1 minute |
| **Mobile Friendly** | Yes | Yes (with fallback) |

## ✨ Next Steps

1. ✅ Create GitHub OAuth App
2. ✅ Configure backend with credentials
3. ✅ Test locally
4. ✅ Deploy to production
5. ✅ Update OAuth App URLs for production

## 📚 Resources

- [GitHub OAuth Documentation](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps)
- [OAuth 2.0 Spec](https://oauth.net/2/)
- [window.postMessage API](https://developer.mozilla.org/en-US/docs/Web/API/Window/postMessage)

---

**Migration Date**: Today  
**Status**: ✅ **COMPLETE**  
**Ready for**: Testing & Production

