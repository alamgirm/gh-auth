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
   - **Application name**: `Device Flow Test`
   - **Homepage URL**: `http://localhost:3000`
   - **Authorization callback URL**: (leave empty)
4. Click **"Register application"**
5. Copy your **Client ID**
6. Click **"Generate a new client secret"**
7. Copy your **Client Secret**

## Step 2: Setup Backend (1 minute)

```bash
cd backend

# Set environment variables (use your credentials from Step 1)
export GITHUB_CLIENT_ID=your_client_id_here
export GITHUB_CLIENT_SECRET=your_client_secret_here

# Start the backend
./gradlew bootRun
```

✅ Backend should now be running on `http://localhost:8080`

## Step 3: Setup Frontend (2 minutes)

Open a **new terminal**:

```bash
cd frontend

# Install dependencies
npm install

# Start the development server
npm run dev
```

✅ Frontend should now be running on `http://localhost:3000`

## Step 4: Test It! (1 minute)

1. Open your browser to **http://localhost:3000**
2. Click **"Login with GitHub"**
3. You'll see a device code (e.g., `WDJB-MJHT`)
4. Click **"Open GitHub"** (opens in new tab)
5. Enter the code when prompted
6. Click **"Authorize"**
7. Return to the app - you're logged in! 🎉

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
rm -rf node_modules
npm install
```

### Authorization not working
- Double-check your Client ID and Secret
- Make sure backend is running on port 8080
- Try refreshing the page

## What's Next?

- Read the [Main README](README.md) for detailed docs
- Check out the [Backend README](backend/README.md)
- Check out the [Frontend README](frontend/README.md)
- Explore the code and customize it!

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

---

**That's it! You're all set! 🚀**

