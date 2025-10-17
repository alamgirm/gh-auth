export const useAuth = () => {
  const config = useRuntimeConfig()
  const apiBaseUrl = config.public.apiBaseUrl
  
  const user = useState<any>('user', () => null)
  const accessToken = useState<string | null>('accessToken', () => null)
  const isAuthenticated = computed(() => !!user.value && !!accessToken.value)
  
  // Load auth state from localStorage on mount
  const loadAuthState = () => {
    if (process.client) {
      const storedToken = localStorage.getItem('github_access_token')
      const storedUser = localStorage.getItem('github_user')
      
      if (storedToken && storedUser) {
        accessToken.value = storedToken
        user.value = JSON.parse(storedUser)
      }
    }
  }
  
  // Save auth state to localStorage
  const saveAuthState = (token: string, userData: any) => {
    if (process.client) {
      localStorage.setItem('github_access_token', token)
      localStorage.setItem('github_user', JSON.stringify(userData))
      accessToken.value = token
      user.value = userData
    }
  }
  
  // Clear auth state
  const clearAuthState = () => {
    if (process.client) {
      localStorage.removeItem('github_access_token')
      localStorage.removeItem('github_user')
      accessToken.value = null
      user.value = null
    }
  }
  
  // Get authorization URL from backend
  const getAuthorizationUrl = async () => {
    try {
      const response: any = await $fetch(`${apiBaseUrl}/api/auth/authorize-url`, {
        method: 'GET',
      })
      return response
    } catch (error) {
      console.error('Error getting authorization URL:', error)
      throw error
    }
  }
  
  // Exchange authorization code for token
  const exchangeCodeForToken = async (code: string, state: string) => {
    try {
      const response: any = await $fetch(`${apiBaseUrl}/api/auth/exchange-code`, {
        method: 'POST',
        body: {
          code,
          state
        }
      })
      return response
    } catch (error) {
      console.error('Error exchanging code for token:', error)
      throw error
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
              // Exchange code for token
              const tokenData = await exchangeCodeForToken(code, state)
              
              // Save auth state
              saveAuthState(tokenData.accessToken, tokenData.user)
              
              window.removeEventListener('message', messageHandler)
              popup.close()
              resolve(tokenData.user)
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
  
  // Verify token
  const verifyToken = async (token: string) => {
    try {
      const response = await $fetch(`${apiBaseUrl}/api/auth/verify`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })
      return response
    } catch (error) {
      console.error('Error verifying token:', error)
      throw error
    }
  }
  
  // Logout
  const logout = () => {
    clearAuthState()
  }
  
  // Check if current token is valid
  const checkAuthStatus = async () => {
    if (accessToken.value) {
      try {
        const userData = await verifyToken(accessToken.value)
        if (userData) {
          user.value = userData
          return true
        }
      } catch (error) {
        clearAuthState()
      }
    }
    return false
  }
  
  return {
    user,
    accessToken,
    isAuthenticated,
    loadAuthState,
    saveAuthState,
    clearAuthState,
    getAuthorizationUrl,
    exchangeCodeForToken,
    loginWithPopup,
    verifyToken,
    logout,
    checkAuthStatus,
  }
}
