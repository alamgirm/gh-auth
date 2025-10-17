# GitHub Device Flow OAuth Authentication

A complete full-stack application demonstrating GitHub OAuth2 Device Flow authentication with **Nuxt 3** frontend and **Spring Boot** backend.

## 🎯 Overview

This project implements the [GitHub OAuth Device Flow](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow), which is ideal for:
- Desktop applications
- CLI tools
- Devices without browsers
- Applications where client secrets cannot be safely stored

## 🏗️ Architecture

```
┌──────────────────┐
│  Nuxt 3 Frontend │
│  (Port 3000)     │
└────────┬─────────┘
         │
         │ REST API
         │
┌────────▼───────────┐      ┌─────────────────┐
│ Spring Boot Backend│─────→│  GitHub OAuth   │
│  (Port 8080)       │←─────│  API            │
└────────────────────┘      └─────────────────┘
```

### Flow Diagram

```
1. User clicks "Login"
   │
2. Frontend → Backend: POST /api/auth/device/code
   │
3. Backend → GitHub: Request device code
   │
4. GitHub → Backend: Returns device code + user code
   │
5. Backend → Frontend: Returns codes + verification URL
   │
6. Frontend displays user code and opens GitHub URL
   │
7. User enters code on GitHub and authorizes
   │
8. Frontend polls → Backend: GET /api/auth/device/poll
   │
9. Backend polls → GitHub: Check authorization status
   │
10. Once authorized, GitHub returns access token
    │
11. Backend fetches user info and returns to frontend
    │
12. Frontend stores token and displays user profile
```

## 🚀 Features

- ✅ **Secure Authentication**: No client secret exposed on frontend
- ✅ **Device Flow Implementation**: Full OAuth2 device flow support
- ✅ **Stateless Backend**: No session storage required
- ✅ **Modern UI**: Beautiful Tailwind CSS interface
- ✅ **Real-time Polling**: Automatic authorization checking
- ✅ **User Profile**: Display GitHub user information
- ✅ **Token Persistence**: LocalStorage for token management
- ✅ **Error Handling**: Comprehensive error states

## 📋 Prerequisites

- **Java 17+** (for backend)
- **Node.js 18+** (for frontend)
- **GitHub OAuth App** credentials

## 🔑 Setup GitHub OAuth App

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "New OAuth App"
3. Fill in the details:
   - **Application name**: Your app name
   - **Homepage URL**: `http://localhost:3000`
   - **Authorization callback URL**: Leave empty (not used in device flow)
4. Click "Register application"
5. Note your **Client ID**
6. Generate a **Client Secret**

## 🛠️ Installation & Setup

### Backend Setup

```bash
cd backend

# Set environment variables
export GITHUB_CLIENT_ID=your_client_id_here
export GITHUB_CLIENT_SECRET=your_client_secret_here

# Run the backend
./gradlew bootRun
```

The backend will start on `http://localhost:8080`

### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Run the development server
npm run dev
```

The frontend will start on `http://localhost:3000`

## 🎮 Usage

1. **Start Both Servers**
   - Backend on port 8080
   - Frontend on port 3000

2. **Open Browser**
   - Navigate to `http://localhost:3000`

3. **Login**
   - Click "Login with GitHub"
   - Copy the displayed device code
   - Click "Open GitHub"
   - Paste the code when prompted
   - Authorize the application

4. **Enjoy!**
   - You'll be automatically logged in
   - Your profile will be displayed

## 📁 Project Structure

```
gh-device-flow/
├── backend/                    # Spring Boot Backend
│   ├── src/
│   │   └── main/
│   │       ├── java/com/github/deviceflow/
│   │       │   ├── controller/     # REST Controllers
│   │       │   ├── service/        # Business Logic
│   │       │   ├── model/          # Data Models
│   │       │   └── config/         # Configuration
│   │       └── resources/
│   │           └── application.yml # Config file
│   ├── build.gradle
│   └── README.md
│
├── frontend/                   # Nuxt 3 Frontend
│   ├── pages/
│   │   └── index.vue          # Main page
│   ├── components/
│   │   ├── LoginFlow.vue      # Login flow component
│   │   └── UserProfile.vue    # User profile component
│   ├── composables/
│   │   └── useAuth.ts         # Auth composable
│   ├── nuxt.config.ts
│   ├── package.json
│   └── README.md
│
└── README.md                   # This file
```

## 🔌 API Endpoints

### Backend REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/device/code` | Initiate device flow |
| GET | `/api/auth/device/poll?device_code=xxx` | Poll for authorization |
| GET | `/api/auth/verify` | Verify access token |
| GET | `/api/auth/health` | Health check |

## 🔐 Security Features

1. **No Client Secret on Frontend**: The client secret is only stored on the backend
2. **CORS Protection**: Backend validates allowed origins
3. **Token Validation**: Backend verifies tokens with GitHub
4. **No Session Storage**: Stateless backend design
5. **LocalStorage Only**: Frontend stores tokens client-side

## 🧪 Testing

### Test Backend

```bash
cd backend
./gradlew test
```

### Test Frontend

```bash
cd frontend
npm run test
```

### Manual Testing

```bash
# Test backend health
curl http://localhost:8080/api/auth/health

# Test device code initiation
curl -X POST http://localhost:8080/api/auth/device/code
```

## 📝 Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
github:
  oauth:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}

cors:
  allowed-origins: http://localhost:3000
```

### Frontend Configuration

Edit `frontend/nuxt.config.ts`:

```typescript
runtimeConfig: {
  public: {
    apiBaseUrl: 'http://localhost:8080'
  }
}
```

## 🚢 Production Deployment

### Backend

```bash
cd backend
./gradlew build
java -jar build/libs/github-device-flow-backend-1.0.0.jar
```

### Frontend

```bash
cd frontend
npm run build
npm run preview
```

## 🐛 Troubleshooting

### Backend not starting
- Check Java version: `java -version` (should be 17+)
- Verify environment variables are set
- Check port 8080 is not in use

### Frontend not connecting
- Verify backend is running on port 8080
- Check CORS settings in backend
- Clear browser cache and localStorage

### Authorization failing
- Verify GitHub OAuth app credentials
- Check GitHub app is not suspended
- Ensure device flow is enabled (it is by default)

## 📚 Resources

- [GitHub OAuth Device Flow Docs](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow)
- [Nuxt 3 Documentation](https://nuxt.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

## 📄 License

MIT License - feel free to use this project for learning and development!

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 👤 Author

Built as a demonstration of GitHub OAuth Device Flow implementation.

---

**Happy Coding! 🎉**

