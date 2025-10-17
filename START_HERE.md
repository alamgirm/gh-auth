# 👋 START HERE

## Welcome to GitHub Popup OAuth!

This is a **complete, production-ready** application for GitHub authentication using popup-based OAuth.

## ⚡ 30-Second Overview

**What it does**: Lets users login with GitHub using a popup window (no page redirects!)

**Tech stack**: Nuxt 3 (frontend) + Spring Boot (backend)

**Login time**: <3 seconds

**Setup time**: 5 minutes

## 🚀 Get Started in 3 Steps

### Step 1: Create GitHub OAuth App (2 min)

1. Go to https://github.com/settings/developers
2. Click "New OAuth App"
3. Set callback URL: `http://localhost:3000/auth/callback`
4. Get your Client ID and Secret

### Step 2: Run Backend (1 min)

```bash
cd backend
export GITHUB_CLIENT_ID=your_id_here
export GITHUB_CLIENT_SECRET=your_secret_here
./gradlew bootRun
```

### Step 3: Run Frontend (2 min)

```bash
# New terminal
cd frontend
npm install
npm run dev
```

### Test It!

Open `http://localhost:3000` → Click "Login with GitHub" → Done!

## 📚 What to Read Next

### New to the project?
→ Read **[QUICKSTART.md](QUICKSTART.md)** for detailed setup

### Want to understand how it works?
→ Read **[ARCHITECTURE.md](ARCHITECTURE.md)** for technical details

### Need to setup GitHub App?
→ Read **[GITHUB_APP_SETUP.md](GITHUB_APP_SETUP.md)** for step-by-step guide

### Curious about the migration?
→ Read **[MIGRATION_SUMMARY.md](MIGRATION_SUMMARY.md)** for what changed from device flow

### Want the full picture?
→ Read **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** for complete overview

## 🎯 Key Points

✅ **No device codes** - Simple popup OAuth  
✅ **No polling** - Instant feedback  
✅ **No manual steps** - Fully automated  
✅ **No page redirects** - Popup only  
✅ **Secure** - Client secret protected on backend  
✅ **Fast** - Login in under 3 seconds  

## 🔑 Critical: Callback URL

This URL must match **EXACTLY** in 3 places:

1. **GitHub OAuth App**: `http://localhost:3000/auth/callback`
2. **Backend config**: `http://localhost:3000/auth/callback`
3. **Frontend route**: `/auth/callback.vue` exists ✅

For production, replace `http://localhost:3000` with your domain.

## 📂 Project Structure (Simplified)

```
gh-device-flow/
├── backend/          → Spring Boot API (port 8080)
│   ├── controller/   → REST endpoints
│   ├── service/      → OAuth logic
│   └── config/       → Settings
│
├── frontend/         → Nuxt 3 app (port 3000)
│   ├── pages/        → Main + callback
│   ├── components/   → Login + profile
│   └── composables/  → Auth logic
│
└── docs/             → 9 markdown files
```

## 🎨 How It Looks

### Login Screen
```
┌────────────────────────────────┐
│         🐙 GitHub              │
│                                │
│  Authenticate with GitHub      │
│                                │
│  [Login with GitHub]           │
│                                │
│  A secure popup will open      │
└────────────────────────────────┘
```

### After Login
```
┌────────────────────────────────┐
│  Welcome to GitHub Popup!      │
│  You are successfully          │
│  authenticated                 │
│                                │
│  ┌──────────────────────────┐ │
│  │  👤 Your Profile          │ │
│  │  @username                │ │
│  │  📊 42 repos              │ │
│  │  👥 100 followers         │ │
│  └──────────────────────────┘ │
│                                │
│  [Logout]                      │
└────────────────────────────────┘
```

## 🔌 API Flow (For Developers)

```
Frontend                  Backend                 GitHub
   │                         │                       │
   │  GET /authorize-url     │                       │
   │────────────────────────→│                       │
   │                         │                       │
   │  {url, state}           │                       │
   │←────────────────────────│                       │
   │                         │                       │
   │  Open popup with URL                            │
   │────────────────────────────────────────────────→│
   │                         │                       │
   │  User authorizes                                │
   │←────────────────────────────────────────────────│
   │  Redirect to /callback  │                       │
   │                         │                       │
   │  Popup sends code       │                       │
   │  (via postMessage)      │                       │
   │                         │                       │
   │  POST /exchange-code    │                       │
   │  {code, state}          │                       │
   │────────────────────────→│                       │
   │                         │  Exchange code        │
   │                         │──────────────────────→│
   │                         │                       │
   │                         │  Get user info        │
   │                         │──────────────────────→│
   │                         │                       │
   │  {token, user}          │                       │
   │←────────────────────────│                       │
   │                         │                       │
   ✅ Logged in!             │                       │
```

## ⚠️ Important Notes

### Popup Blockers
- Users must allow popups for your site
- App detects blocked popups and shows message
- Most users have popups enabled by default

### Same Origin Required
- Callback page must be same origin as main app
- `http://localhost:3000` (same origin) ✅
- `http://127.0.0.1:3000` (different origin) ❌

### HTTPS in Production
- Always use HTTPS in production
- Update all URLs to `https://`
- Update GitHub OAuth App settings

## 🧪 Testing

### Quick Test
```bash
# 1. Start servers (see Quick Start above)
# 2. Open http://localhost:3000
# 3. Open browser DevTools console
# 4. Click "Login with GitHub"
# 5. Check console for logs
# 6. Authorize in popup
# 7. Verify profile appears
```

### Expected Console Output
```javascript
Login successful: {
  login: "your-username",
  name: "Your Name",
  email: "you@example.com",
  ...
}
```

## 🐛 Common Issues

### "Popup blocked"
→ Allow popups in browser settings

### "redirect_uri_mismatch"
→ Check callback URL matches in GitHub settings and backend config

### "CORS error"
→ Verify backend is running on port 8080

### "Failed to communicate with parent"
→ Callback URL must be same origin as main app

## 📞 Need Help?

1. **Check browser console** - Look for error messages
2. **Check backend logs** - Look for errors in terminal
3. **Read documentation** - Start with QUICKSTART.md
4. **Verify URLs match** - Callback URL in all 3 places
5. **Check environment variables** - Client ID and Secret set?

## 🎯 Success Criteria

You'll know it's working when:

✅ Backend starts without errors  
✅ Frontend starts on port 3000  
✅ Clicking login opens popup  
✅ Popup shows GitHub authorization  
✅ After authorizing, popup closes  
✅ Profile appears in main window  
✅ Total time: <3 seconds  

## 🚀 What's Next?

After you have it working locally:

1. **Customize UI** - Change colors, layout, etc.
2. **Add features** - More user data, repositories, etc.
3. **Deploy** - Push to production
4. **Share** - Show off your work!

## 📦 Everything You Need

✅ Complete backend implementation  
✅ Complete frontend implementation  
✅ 9 documentation files  
✅ Example configurations  
✅ Security best practices  
✅ Error handling  
✅ Production guidelines  
✅ Troubleshooting guides  

## 🎉 You're Ready!

Everything is set up and documented. Just:

1. Get your GitHub OAuth credentials
2. Run the two commands above
3. Test the login
4. Start building!

---

**Questions?** Check the documentation files above.  
**Ready?** Go to [QUICKSTART.md](QUICKSTART.md) now!  
**Excited?** So are we! 🚀

**Happy coding!**

