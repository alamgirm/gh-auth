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
  
  // Initiate device flow
  const initiateDeviceFlow = async () => {
    try {
      const response = await $fetch(`${apiBaseUrl}/api/auth/device/code`, {
        method: 'POST',
      })
      return response
    } catch (error) {
      console.error('Error initiating device flow:', error)
      throw error
    }
  }
  
  // Poll for authorization
  const pollForAuthorization = async (deviceCode: string) => {
    try {
      const response: any = await $fetch(
        `${apiBaseUrl}/api/auth/device/poll?device_code=${deviceCode}`,
        {
          method: 'GET',
        }
      )
      return response
    } catch (error) {
      console.error('Error polling for authorization:', error)
      throw error
    }
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
    initiateDeviceFlow,
    pollForAuthorization,
    verifyToken,
    logout,
    checkAuthStatus,
  }
}

