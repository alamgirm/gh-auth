export const useMultiAuth = () => {
  const config = useRuntimeConfig()
  const apiBaseUrl = config.public.apiBaseUrl
  
  const azureUser = useState<any>('azureUser', () => null)
  const ghecUser = useState<any>('ghecUser', () => null)
  const ghesUser = useState<any>('ghesUser', () => null)
  const azureToken = useState<string | null>('azureToken', () => null)
  const isAzureAuthenticated = computed(() => !!azureUser.value && !!azureToken.value)
  const isGhecConnected = useState<boolean>('isGhecConnected', () => false)
  const isGhesConnected = useState<boolean>('isGhesConnected', () => false)
  
  const { loginWithAzure } = useAzureAuth()
  
  // Get Azure token from MSAL
  const getAzureToken = async () => {
    const { initializeMsal, getCurrentAccount } = useAzureAuth()
    
    try {
      const msal = await initializeMsal()
      const account = await getCurrentAccount()
      
      if (!account) {
        return null
      }
      
      // Acquire token silently
      const response = await msal.acquireTokenSilent({
        scopes: [config.public.azureApiScope || 'User.Read'],
        account: account
      })
      
      return response.accessToken
    } catch (error) {
      console.error('Failed to get Azure token:', error)
      return null
    }
  }
  
  // Login with Azure (primary authentication)
  const loginWithMicrosoft = async () => {
    try {
      console.log('Logging in with Azure (primary)...')
      const result = await loginWithAzure()
      
      // Redirect was initiated
      if (result === null) {
        console.log('Azure redirect initiated')
        return null
      }
      
      // Silent token acquired
      if (result && result.user) {
        azureUser.value = result.user
        // Get and store token
        const token = await getAzureToken()
        if (token) {
          azureToken.value = token
        }
        await checkAuthStatus()
        return result.user
      }
      
      throw new Error('Azure login failed')
    } catch (error) {
      console.error('Azure login failed:', error)
      throw error
    }
  }
  
  // Handle Azure redirect callback
  const handleAzureRedirect = async () => {
    const { handleRedirectCallback } = useAzureAuth()
    
    try {
      const response = await handleRedirectCallback()
      
      if (response && response.user) {
        azureUser.value = response.user
        // Get and store token
        const token = await getAzureToken()
        if (token) {
          azureToken.value = token
        }
        await checkAuthStatus()
        return response.user
      }
      
      return null
    } catch (error) {
      console.error('Error handling Azure redirect:', error)
      throw error
    }
  }
  
  // Check auth status from backend
  const checkAuthStatus = async () => {
    try {
      const token = await getAzureToken()
      
      if (!token) {
        return false
      }
      
      const response: any = await $fetch(`${apiBaseUrl}/api/auth/status`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`
        }
      })
      
      console.log('Backend auth status response:', response)
      
      if (response.azureAuthenticated) {
        azureUser.value = response.azureUser
        azureToken.value = token
        isGhecConnected.value = response.ghecConnected
        isGhesConnected.value = response.ghesConnected
        
        console.log('Auth status updated:', {
          ghec: response.ghecConnected ? 'connected' : 'not connected',
          ghes: response.ghesConnected ? 'connected' : 'not connected'
        })
        
        if (response.ghecUser) {
          ghecUser.value = response.ghecUser
        } else {
          ghecUser.value = null
        }
        
        if (response.ghesUser) {
          ghesUser.value = response.ghesUser
        } else {
          ghesUser.value = null
        }
        
        return true
      }
      
      return false
    } catch (error) {
      console.error('Error checking auth status:', error)
      return false
    }
  }
  
  // Connect GitHub account (Ghec or Ghes) - uses full-page redirect
  const connectGitHub = async (provider: 'ghec' | 'ghes') => {
    try {
      const token = await getAzureToken()
      
      if (!token) {
        throw new Error('Azure authentication required first')
      }
      
      // Get GitHub OAuth URL
      const response: any = await $fetch(`${apiBaseUrl}/api/auth/github/${provider}/authorize-url`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`
        }
      })
      
      const { url, state } = response
      
      // Save state and provider to sessionStorage for callback verification
      if (process.client) {
        sessionStorage.setItem('github_oauth_state', state)
        sessionStorage.setItem('github_oauth_provider', provider)
      }
      
      // Full-page redirect to GitHub OAuth
      window.location.href = url
      
    } catch (error) {
      console.error(`Error connecting ${provider}:`, error)
      throw error
    }
  }
  
  // Disconnect GitHub (Ghec or Ghes)
  const disconnectGitHub = async (provider: 'ghec' | 'ghes') => {
    try {
      const token = await getAzureToken()
      if (!token) return
      
      await $fetch(`${apiBaseUrl}/api/auth/github/${provider}/unlink`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`
        }
      })
      
      if (provider === 'ghec') {
        isGhecConnected.value = false
        ghecUser.value = null
      } else {
        isGhesConnected.value = false
        ghesUser.value = null
      }
    } catch (error) {
      console.error(`Error disconnecting ${provider}:`, error)
      throw error
    }
  }
  
  // Logout (Azure only, clears MSAL)
  const logout = async () => {
    const { logoutAzure } = useAzureAuth()
    
    try {
      await logoutAzure()
      azureUser.value = null
      azureToken.value = null
      ghecUser.value = null
      ghesUser.value = null
      isGhecConnected.value = false
      isGhesConnected.value = false
    } catch (error) {
      console.error('Error during logout:', error)
    }
  }
  
  return {
    azureUser,
    ghecUser,
    ghesUser,
    isAzureAuthenticated,
    isGhecConnected,
    isGhesConnected,
    loginWithMicrosoft,
    connectGitHub,
    disconnectGitHub,
    handleAzureRedirect,
    checkAuthStatus,
    logout,
    getAzureToken,
  }
}
