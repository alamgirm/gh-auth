# GitHub Device Flow Frontend (Nuxt 3)

Modern single-page application for GitHub OAuth Device Flow authentication.

## 🏗️ Technology Stack

- **Nuxt 3** - Vue.js framework
- **Vue 3** - Progressive JavaScript framework
- **TypeScript** - Type safety
- **Tailwind CSS** - Utility-first CSS
- **Composables** - Reusable logic

## 📁 Project Structure

```
frontend/
├── pages/
│   └── index.vue              # Main page
├── components/
│   ├── LoginFlow.vue          # Login flow component
│   └── UserProfile.vue        # User profile display
├── composables/
│   └── useAuth.ts             # Authentication composable
├── public/                    # Static assets
├── nuxt.config.ts             # Nuxt configuration
├── tailwind.config.js         # Tailwind configuration
└── package.json
```

## 🚀 Setup

### Install Dependencies

```bash
npm install
```

### Development Server

```bash
npm run dev
```

Starts the dev server on `http://localhost:3000`

### Build for Production

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## 🎨 Features

### LoginFlow Component

Multi-step authentication flow:

1. **Start**: Initial state with login button
2. **Show Code**: Displays user code and verification URL
3. **Polling**: Automatically polls backend for authorization
4. **Success**: Shows success message
5. **Error**: Handles and displays errors

### UserProfile Component

Displays GitHub user information:
- Avatar
- Name and username
- Bio
- Repository count
- Followers/following
- Location
- Email
- Join date

### useAuth Composable

Provides authentication functionality:

```typescript
const {
  user,                    // Current user object
  accessToken,             // Access token
  isAuthenticated,         // Boolean auth status
  loadAuthState,           // Load from localStorage
  saveAuthState,           // Save to localStorage
  clearAuthState,          // Clear auth data
  initiateDeviceFlow,      // Start device flow
  pollForAuthorization,    // Poll for auth status
  verifyToken,             // Verify token
  logout,                  // Logout user
  checkAuthStatus,         // Check if token valid
} = useAuth()
```

## 🔑 Configuration

### Environment Variables

Create `.env` file:

```bash
NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### Nuxt Config

Edit `nuxt.config.ts`:

```typescript
export default defineNuxtConfig({
  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
    }
  }
})
```

## 💾 Data Storage

### LocalStorage Keys

- `github_access_token`: Stores the GitHub access token
- `github_user`: Stores user profile data (JSON string)

### State Management

Uses Vue 3 Composition API with `useState`:

```typescript
const user = useState<any>('user', () => null)
const accessToken = useState<string | null>('accessToken', () => null)
```

## 🔄 Authentication Flow

### 1. User Initiates Login

```vue
<button @click="startDeviceFlow">
  Login with GitHub
</button>
```

### 2. Backend Request

```typescript
const response = await initiateDeviceFlow()
// Returns: deviceCode, userCode, verificationUri, interval
```

### 3. Display Code

```vue
<div class="user-code">
  {{ userCode }}
</div>
<a :href="verificationUri" target="_blank">
  Open GitHub
</a>
```

### 4. Polling

```typescript
const pollInterval = setInterval(async () => {
  const response = await pollForAuthorization(deviceCode)
  
  if (response.status === 'authorized') {
    saveAuthState(response.accessToken, response.user)
    // Success!
  }
}, intervalSeconds * 1000)
```

### 5. Store Auth Data

```typescript
localStorage.setItem('github_access_token', token)
localStorage.setItem('github_user', JSON.stringify(user))
```

## 🎨 Styling

### Tailwind CSS

The app uses Tailwind CSS for styling. Key classes:

```vue
<!-- Gradient background -->
<div class="bg-gradient-to-br from-purple-600 to-blue-500">

<!-- Card -->
<div class="bg-white rounded-lg shadow-2xl p-8">

<!-- Button -->
<button class="bg-gray-800 hover:bg-gray-900 text-white font-semibold py-3 px-8 rounded-lg">

<!-- Loading spinner -->
<svg class="animate-spin h-5 w-5">
```

### Custom Styles

App-level styles in `app.vue`:

```vue
<style>
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

#app {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>
```

## 🔌 API Integration

### Base URL

```typescript
const config = useRuntimeConfig()
const apiBaseUrl = config.public.apiBaseUrl
```

### API Calls

```typescript
// Initiate device flow
await $fetch(`${apiBaseUrl}/api/auth/device/code`, {
  method: 'POST',
})

// Poll for authorization
await $fetch(
  `${apiBaseUrl}/api/auth/device/poll?device_code=${deviceCode}`,
  { method: 'GET' }
)

// Verify token
await $fetch(`${apiBaseUrl}/api/auth/verify`, {
  method: 'GET',
  headers: {
    Authorization: `Bearer ${token}`,
  },
})
```

## 🧪 Testing

### Manual Testing

1. Start the dev server
2. Open browser to `http://localhost:3000`
3. Click "Login with GitHub"
4. Verify code is displayed
5. Click "Open GitHub"
6. Enter code on GitHub
7. Authorize the app
8. Verify successful login

### Testing Auth States

```typescript
// Test localStorage
localStorage.setItem('github_access_token', 'test_token')
localStorage.setItem('github_user', JSON.stringify({
  login: 'testuser',
  name: 'Test User'
}))

// Reload page to test auth persistence
location.reload()
```

## 📱 Responsive Design

The app is fully responsive:

- **Mobile**: Single column layout
- **Tablet**: 2-column stats grid
- **Desktop**: 4-column stats grid

```vue
<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
  <!-- Stats cards -->
</div>
```

## 🐛 Troubleshooting

### Backend not reachable

Check `nuxt.config.ts`:
```typescript
runtimeConfig: {
  public: {
    apiBaseUrl: 'http://localhost:8080'  // Verify this matches backend
  }
}
```

### CORS errors

Ensure backend allows your origin in `application.yml`

### LocalStorage not persisting

Check browser settings - ensure localStorage is enabled

### Polling not stopping

Verify cleanup in `onUnmounted`:
```typescript
onUnmounted(() => {
  if (pollInterval) {
    clearInterval(pollInterval)
  }
})
```

## 🚀 Production Deployment

### Build

```bash
npm run build
```

### Deploy Static

Deploy the `.output/public` directory to:
- Vercel
- Netlify
- GitHub Pages
- Any static host

### Deploy SSR

Deploy the entire `.output` directory to:
- Vercel
- Netlify Functions
- AWS Lambda
- Node.js server

### Environment Variables

Set in your hosting platform:
```
NUXT_PUBLIC_API_BASE_URL=https://your-backend-api.com
```

### Example: Vercel

```json
{
  "buildCommand": "npm run build",
  "outputDirectory": ".output/public",
  "env": {
    "NUXT_PUBLIC_API_BASE_URL": "https://your-backend.com"
  }
}
```

## 📦 Dependencies

```json
{
  "dependencies": {
    "nuxt": "^3.13.0",
    "vue": "^3.4.0",
    "vue-router": "^4.4.0"
  },
  "devDependencies": {
    "@nuxtjs/tailwindcss": "^6.12.0",
    "@types/node": "^20.11.0"
  }
}
```

## 🔐 Security Considerations

1. **No Client Secret**: Client secret stays on backend
2. **Token Storage**: Tokens stored in localStorage (consider alternatives for high-security apps)
3. **HTTPS**: Use HTTPS in production
4. **Token Expiration**: Implement token refresh logic
5. **XSS Protection**: Sanitize user inputs

## 📝 Notes

- Access tokens are stored in localStorage
- Tokens do not expire automatically (implement refresh logic if needed)
- Polling interval is 5 seconds by default
- Device codes expire after 15 minutes

## 🎯 Future Enhancements

- [ ] Token refresh mechanism
- [ ] Better error handling
- [ ] Loading states
- [ ] Dark mode toggle
- [ ] Multiple OAuth providers
- [ ] Remember me functionality
- [ ] Token expiration handling

---

Built with ❤️ using Nuxt 3

