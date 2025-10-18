# GitHub OAuth Frontend (Nuxt 3)

Modern single-page application for GitHub OAuth popup-based authentication.

## 🏗️ Technology Stack

- **Nuxt 3** - Vue.js framework
- **Vue 3** - Progressive JavaScript framework
- **TypeScript** - Type safety
- **Tailwind CSS** - Utility-first CSS
- **Composables** - Reusable logic
- **window.postMessage** - Secure popup communication

## 📁 Project Structure

```
frontend/
├── pages/
│   ├── index.vue                  # Main application page
│   └── auth/
│       └── callback.vue           # OAuth callback (popup)
├── components/
│   ├── LoginFlow.vue              # Login button & flow
│   └── UserProfile.vue            # User profile display
├── composables/
│   └── useAuth.ts                 # Auth logic & popup handling
├── app.vue                        # Root component
├── nuxt.config.ts                 # Nuxt configuration
├── tailwind.config.js             # Tailwind configuration
├── tsconfig.json                  # TypeScript configuration
└── package.json                   # Dependencies
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

## 🎨 Components

### LoginFlow.vue

Handles the popup-based authentication:

**States**:
- Default: Login button
- Loading: "Authenticating..." message
- Error: Error message with retry button

**Features**:
- Opens popup for OAuth
- Handles popup communication
- Error handling
- Loading states

### UserProfile.vue

Displays GitHub user information:
- Avatar
- Name and username
- Bio
- Repository count
- Followers/following
- Location
- Email
- Join date

### auth/callback.vue (Popup Page)

Handles the OAuth callback in the popup:

**Flow**:
1. Extract code and state from URL
2. Validate parameters
3. Send to parent via postMessage
4. Display status
5. Auto-close popup

**States**:
- Processing: Extracting code
- Success: Sending to parent
- Error: Display error

## 🔑 useAuth Composable

Central authentication logic:

```typescript
const {
  user,                    // Current user object
  accessToken,             // Access token
  isAuthenticated,         // Boolean auth status
  loadAuthState,           // Load from localStorage
  saveAuthState,           // Save to localStorage
  clearAuthState,          // Clear auth data
  getAuthorizationUrl,     // Get OAuth URL from backend
  exchangeCodeForToken,    // Exchange code for token
  loginWithPopup,          // Main login method - opens popup
  verifyToken,             // Verify token
  logout,                  // Logout user
  checkAuthStatus,         // Check if token valid
} = useAuth()
```

### loginWithPopup() Method

The core popup authentication method:

```typescript
// Usage
const { loginWithPopup } = useAuth()

try {
  const user = await loginWithPopup()
  console.log('Logged in as:', user.login)
} catch (error) {
  console.error('Login failed:', error)
}
```

**What it does**:
1. Fetches OAuth URL from backend
2. Opens popup window (600x700)
3. Listens for postMessage from popup
4. Validates state parameter
5. Exchanges code for token
6. Saves auth state
7. Returns user data

## 🔄 Authentication Flow

### 1. User Clicks Login

```vue
<button @click="handleLogin">
  Login with GitHub
</button>
```

### 2. Open Popup

```typescript
const { url, state } = await getAuthorizationUrl()

const popup = window.open(
  url,
  'GitHub Login',
  'width=600,height=700,left=X,top=Y'
)
```

### 3. Callback Handles Redirect

```typescript
// In popup: /auth/callback?code=xxx&state=yyy
const code = urlParams.get('code')
const state = urlParams.get('state')

window.opener.postMessage({
  type: 'github-auth-success',
  code,
  receivedState: state
}, window.location.origin)
```

### 4. Main Window Receives Message

```typescript
window.addEventListener('message', async (event) => {
  if (event.origin !== window.location.origin) return
  
  if (event.data.type === 'github-auth-success') {
    const { code, receivedState } = event.data
    
    // Validate state
    if (receivedState === state) {
      const tokenData = await exchangeCodeForToken(code, state)
      saveAuthState(tokenData.accessToken, tokenData.user)
    }
  }
})
```

### 5. Store Auth Data

```typescript
localStorage.setItem('github_access_token', token)
localStorage.setItem('github_user', JSON.stringify(user))
```

## 💾 Data Storage

### LocalStorage Keys

- `github_access_token` - GitHub access token
- `github_user` - User profile data (JSON)

### State Management

Uses Vue 3 Composition API:

```typescript
const user = useState<any>('user', () => null)
const accessToken = useState<string | null>('accessToken', () => null)
const isAuthenticated = computed(() => !!user.value && !!accessToken.value)
```

## 🎨 Styling

### Tailwind CSS

Beautiful gradient design:

```vue
<!-- Main background -->
<div class="min-h-screen bg-gradient-to-br from-purple-600 to-blue-500">

<!-- Card -->
<div class="bg-white rounded-lg shadow-2xl p-8">

<!-- Button -->
<button class="bg-gray-800 hover:bg-gray-900 text-white font-semibold py-3 px-8 rounded-lg">
  Login with GitHub
</button>
```

### Responsive Design

```vue
<!-- Stats grid -->
<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
  <!-- Auto-adjusts for mobile/desktop -->
</div>
```

## 🔌 API Integration

### Configuration

```typescript
const config = useRuntimeConfig()
const apiBaseUrl = config.public.apiBaseUrl  // http://localhost:8080
```

### API Calls

```typescript
// Get authorization URL
const { url, state } = await $fetch(`${apiBaseUrl}/api/auth/authorize-url`)

// Exchange code for token
const tokenData = await $fetch(`${apiBaseUrl}/api/auth/exchange-code`, {
  method: 'POST',
  body: { code, state }
})

// Verify token
const user = await $fetch(`${apiBaseUrl}/api/auth/verify`, {
  headers: {
    Authorization: `Bearer ${token}`
  }
})
```

## 🔐 Security Features

### Origin Validation

```typescript
window.addEventListener('message', (event) => {
  // Only accept messages from same origin
  if (event.origin !== window.location.origin) {
    return
  }
  // Process message...
})
```

### State Validation (CSRF Protection)

```typescript
if (receivedState !== state) {
  throw new Error('State mismatch - possible CSRF attack')
}
```

### Popup Cleanup

```typescript
// Check if popup was closed
const checkPopupClosed = setInterval(() => {
  if (popup.closed) {
    clearInterval(checkPopupClosed)
    window.removeEventListener('message', messageHandler)
  }
}, 1000)
```

## 🧪 Testing

### Manual Testing

1. Start dev server: `npm run dev`
2. Open `http://localhost:3000`
3. Open browser DevTools console
4. Click "Login with GitHub"
5. Check console for logs:
   ```
   Login successful: { login: 'username', ... }
   ```

### Test Popup Blocker

```typescript
const popup = window.open(url, ...)

if (!popup) {
  // Popup was blocked
  throw new Error('Failed to open popup. Please allow popups.')
}
```

### Test localStorage

```javascript
// Check stored data
console.log(localStorage.getItem('github_access_token'))
console.log(localStorage.getItem('github_user'))

// Clear data
localStorage.clear()
location.reload()
```

## 📱 Mobile Support

The popup flow works on mobile:
- Safari: Opens in new tab (same effect)
- Chrome Mobile: Opens in new tab
- postMessage works cross-tab

For true mobile apps, consider:
- Deep linking
- In-app browser
- Native OAuth flows

## 🚀 Production Deployment

### Environment Variables

```bash
# Set in your hosting platform
NUXT_PUBLIC_API_BASE_URL=https://api.yourdomain.com
```

### Build

```bash
npm run build
```

### Deploy

**Static Hosting** (Vercel, Netlify):
- Deploy `.output/public` directory

**SSR Hosting** (Node.js):
- Deploy entire `.output` directory

### Update Callback URL

Change all instances of `http://localhost:3000` to your production URL:
- GitHub OAuth App settings
- Backend configuration
- Frontend environment variables

## 🐛 Troubleshooting

### Popup blocked

**Solution**: Check browser popup blocker settings

### postMessage not working

**Solution**: 
- Ensure same origin for main app and callback
- Check console for errors
- Verify `window.opener` exists in popup

### State mismatch

**Solution**: 
- Don't use browser back button during auth
- Complete flow in one session
- Check that state is being passed correctly

### CORS errors

**Solution**:
- Verify backend is running
- Check backend CORS settings
- Ensure `allowed-origins` includes frontend URL

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

## 🎯 Key Features

- ✅ Popup-based OAuth (no page redirects)
- ✅ Secure postMessage communication
- ✅ CSRF protection
- ✅ Error handling
- ✅ Loading states
- ✅ Auto-popup cleanup
- ✅ localStorage persistence
- ✅ Token verification
- ✅ Responsive design
- ✅ Mobile friendly

## 📝 Notes

- Access tokens stored in localStorage
- Tokens don't auto-expire (GitHub OAuth Apps)
- Popup auto-closes on success
- State parameter prevents CSRF
- Origin validation for security

---

Built with ❤️ using Nuxt 3 and the magic of popup windows!
