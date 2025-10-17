# Quick Start Guide

Get up and running in 5 minutes!

## Prerequisites

- Java 17+
- Node.js 18+
- GitHub account

## Step 1: Create GitHub OAuth App (2 minutes)

1. Go to https://github.com/settings/developers
2. Click **"New OAuth App"**
3. Fill in:
   ```
   Application name: My Test App
   Homepage URL: http://localhost:3000
   Callback URL: http://localhost:3000/auth/callback
   ```
   ⚠️ **Callback URL must be EXACT!**

4. Click **"Register application"**
5. Copy your **Client ID**
6. Click **"Generate a new client secret"**
7. Copy your **Client Secret**

## Step 2: Setup Backend (1 minute)

```bash
cd backend

# Set your credentials
export GITHUB_CLIENT_ID=your_client_id_here
export GITHUB_CLIENT_SECRET=your_client_secret_here

# Start the backend
./gradlew bootRun
```

✅ Backend running on `http://localhost:8080`

## Step 3: Setup Frontend (2 minutes)

Open a **new terminal**:

```bash
cd frontend

# Install dependencies
npm install

# Start the development server
npm run dev
```

✅ Frontend running on `http://localhost:3000`

## Step 4: Test It! (1 minute)

1. Open `http://localhost:3000` in your browser
2. Click **"Login with GitHub"**
3. Popup opens automatically
4. Click **"Authorize"** on GitHub
5. Popup closes
6. **You're logged in!** 🎉

## 🎯 That's It!

Total time: **~5 minutes**

## What Just Happened?

1. ✅ GitHub OAuth App created
2. ✅ Backend running with your credentials
3. ✅ Frontend connected to backend
4. ✅ Secure popup OAuth flow working
5. ✅ User authenticated and profile displayed

## Next Steps

- ✅ Explore the user profile display
- ✅ Try logging out and back in
- ✅ Check browser DevTools console for logs
- ✅ Read [ARCHITECTURE.md](ARCHITECTURE.md) for technical details

## Troubleshooting

### Backend won't start
```bash
# Check Java version
java -version  # Should be 17 or higher

# Verify environment variables
echo $GITHUB_CLIENT_ID
echo $GITHUB_CLIENT_SECRET
```

### Frontend won't start
```bash
# Try clearing node_modules
rm -rf node_modules package-lock.json
npm install
```

### Popup is blocked
- Allow popups for `localhost:3000` in your browser settings
- Look for the popup blocker icon in the address bar

### "Callback URL mismatch" error
Make sure these match EXACTLY:
- GitHub OAuth App: `http://localhost:3000/auth/callback`
- Backend config: `http://localhost:3000/auth/callback`
- Frontend route: `/auth/callback` page exists

## Common Commands

```bash
# Backend
cd backend
./gradlew bootRun          # Run backend
./gradlew build            # Build backend
./gradlew test             # Run tests

# Frontend
cd frontend
npm run dev                # Run dev server
npm run build              # Build for production
npm run preview            # Preview production build
```

## Environment Variables (Optional)

Instead of `export`, you can create a `.env` file:

**backend/.env**
```
GITHUB_CLIENT_ID=your_client_id
GITHUB_CLIENT_SECRET=your_client_secret
```

**frontend/.env**
```
NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

## What's Different from Device Flow?

This app used to use "Device Flow" (manual code entry). Now it uses:

| Feature | Old (Device Flow) | New (Popup OAuth) |
|---------|------------------|-------------------|
| Click to Login | 15+ seconds | <3 seconds ⚡ |
| User Actions | 5 steps | 2 steps |
| Manual Code | Yes ❌ | No ✅ |
| Page Redirects | None | None |
| UX | Okay | Excellent ✨ |

## Testing Tips

### Check Logs

**Backend logs**:
```bash
# Look for:
# "Generating authorization URL with state: xxx"
# "Successfully exchanged code for access token"
```

**Frontend logs** (Browser DevTools):
```javascript
// Look for:
// "Login successful: { login: 'username', ... }"
// "Device flow response: { url: '...', state: '...' }"
```

### Test API Directly

```bash
# Get OAuth URL
curl http://localhost:8080/api/auth/authorize-url

# Health check
curl http://localhost:8080/api/auth/health
```

## Production Deployment

When ready for production:

1. **Update GitHub OAuth App URLs** to your production domain
2. **Set environment variables** on your hosting platform
3. **Deploy backend and frontend**
4. **Test the popup flow**

See [GITHUB_APP_SETUP.md](GITHUB_APP_SETUP.md) for detailed production setup.

---

**That's it! You're all set! 🚀**

Happy coding!
