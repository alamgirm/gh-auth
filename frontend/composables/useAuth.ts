export const useAuth = () => {
  const config = useRuntimeConfig()
  const apiBaseUrl = config.public.apiBaseUrl
  
  const user = useState<any>('user', () => null)
  const userId = useState<string | null>('userId', () => null)
  const isAuthenticated = computed(() => !!user.value && !!userId.value)
  
  // Load auth state from localStorage (only userId)
  const loadAuthState = () => {
    if (process.client) {
      const storedUserId = localStorage.getItem('github_user_id')
      
      if (storedUserId) {
        userId.value = storedUserId
        // Fetch user profile from backend
        fetchUserProfile()
      }
    }
  }
  
  // Save userId to localStorage
  const saveAuthState = (userIdValue: string, userData: any) => {
    if (process.client) {
      localStorage.setItem('github_user_id', userIdValue)
      userId.value = userIdValue
      user.value = userData
    }
  }
  
  // Clear auth state
  const clearAuthState = () => {
    if (process.client) {
      localStorage.removeItem('github_user_id')
      userId.value = null
      user.value = null
    }
  }
  
  // Get authorization URL from backend
  const getAuthorizationUrl = async () => {
    try {
      const response: any = await $fetch(`${apiBaseUrl}/api/auth/authorize-url`, {
        method: 'GET',
        credentials: 'include', // Important: include cookies for session
      })
      return response
    } catch (error) {
      console.error('Error getting authorization URL:', error)
      throw error
    }
  }
  
  // Exchange authorization code for userId
  // Backend stores the token and returns userId
  const exchangeCodeForUserId = async (code: string, state: string) => {
    try {
      const response: any = await $fetch(`${apiBaseUrl}/api/auth/exchange-code`, {
        method: 'POST',
        credentials: 'include', // Important: include cookies for session
        body: {
          code,
          state
        }
      })
      return response
    } catch (error) {
      console.error('Error exchanging code:', error)
      throw error
    }
  }
  
  // Fetch user profile from backend (backend calls GitHub API with stored token)
  const fetchUserProfile = async (refresh: boolean = false) => {
    try {
      const endpoint = refresh ? '/api/auth/user' : '/api/auth/user/cached'
      const response: any = await $fetch(`${apiBaseUrl}${endpoint}`, {
        method: 'GET',
        credentials: 'include', // Important: include cookies for session
      })
      
      user.value = response
      return response
    } catch (error: any) {
      console.error('Error fetching user profile:', error)
      if (error.status === 401) {
        clearAuthState()
      }
      throw error
    }
  }
  
  // Check authentication status
  const checkAuthStatus = async () => {
    try {
      const response: any = await $fetch(`${apiBaseUrl}/api/auth/check`, {
        method: 'GET',
        credentials: 'include',
      })
      
      if (response.authenticated && response.userId) {
        userId.value = response.userId
        user.value = response.user
        
        // Save userId to localStorage
        if (process.client) {
          localStorage.setItem('github_user_id', response.userId)
        }
        
        return true
      } else {
        clearAuthState()
        return false
      }
    } catch (error) {
      console.error('Error checking auth status:', error)
      clearAuthState()
      return false
    }
  }
  
  // Login with popup
  const loginWithPopup = (): Promise<any> => {
    return new Promise(async (resolve, reject) => {
      try {
        // Get authorization URL from backend
        const { url, state } = await getAuthorizationUrl()
        
        // Open popup
        const width = 600
        const height = 700
        const left = window.screen.width / 2 - width / 2
        const top = window.screen.height / 2 - height / 2
        
        const popup = window.open(
          url,
          'GitHub Login',
          `width=${width},height=${height},left=${left},top=${top},toolbar=no,menubar=no`
        )
        
        if (!popup) {
          reject(new Error('Failed to open popup. Please allow popups for this site.'))
          return
        }
        
        // Listen for messages from popup
        const messageHandler = async (event: MessageEvent) => {
          // Verify origin
          if (event.origin !== window.location.origin) {
            return
          }
          
          if (event.data.type === 'github-auth-success') {
            const { code, receivedState } = event.data
            
            // Verify state matches
            if (receivedState !== state) {
              window.removeEventListener('message', messageHandler)
              popup.close()
              reject(new Error('State mismatch - possible CSRF attack'))
              return
            }
            
            try {
              // Exchange code - backend stores token and returns userId
              const data = await exchangeCodeForUserId(code, state)
              
              // Save only userId (token is stored in backend database)
              saveAuthState(data.userId, data.user)
              
              window.removeEventListener('message', messageHandler)
              popup.close()
              resolve(data.user)
            } catch (error) {
              window.removeEventListener('message', messageHandler)
              popup.close()
              reject(error)
            }
          } else if (event.data.type === 'github-auth-error') {
            window.removeEventListener('message', messageHandler)
            popup.close()
            reject(new Error(event.data.error || 'Authentication failed'))
          }
        }
        
        window.addEventListener('message', messageHandler)
        
        // Check if popup was closed
        const checkPopupClosed = setInterval(() => {
          if (popup.closed) {
            clearInterval(checkPopupClosed)
            window.removeEventListener('message', messageHandler)
            reject(new Error('Popup was closed'))
          }
        }, 1000)
        
      } catch (error) {
        reject(error)
      }
    })
  }
  
  // Logout
  const logout = async () => {
    try {
      await $fetch(`${apiBaseUrl}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include',
      })
      clearAuthState()
    } catch (error) {
      console.error('Error during logout:', error)
      // Clear local state anyway
      clearAuthState()
    }
  }
  
  // Refresh user data from GitHub
  const refreshUserData = async () => {
    try {
      await fetchUserProfile(true)
    } catch (error) {
      console.error('Error refreshing user data:', error)
      throw error
    }
  }
  
  return {
    user,
    userId,
    isAuthenticated,
    loadAuthState,
    saveAuthState,
    clearAuthState,
    getAuthorizationUrl,
    exchangeCodeForUserId,
    loginWithPopup,
    logout,
    checkAuthStatus,
    fetchUserProfile,
    refreshUserData,
  }
}
