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
  const ghesEnabled = useState<boolean>('ghesEnabled', () => false)
  
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
      
      if (response.azureAuthenticated) {
        azureUser.value = response.azureUser
        azureToken.value = token
        isGhecConnected.value = response.ghecConnected
        isGhesConnected.value = response.ghesConnected
        ghesEnabled.value = response.ghesEnabled
        
        if (response.ghecUser) {
          ghecUser.value = response.ghecUser
        }
        
        if (response.ghesUser) {
          ghesUser.value = response.ghesUser
        }
        
        return true
      }
      
      return false
    } catch (error) {
      console.error('Error checking auth status:', error)
      return false
    }
  }
  
  // Connect GitHub account (Ghec or Ghes)
  const connectGitHub = (provider: 'ghec' | 'ghes'): Promise<any> => {
    return new Promise(async (resolve, reject) => {
      try {
        const token = await getAzureToken()
        
        if (!token) {
          reject(new Error('Azure authentication required first'))
          return
        }
        
        // Get GitHub OAuth URL
        const response: any = await $fetch(`${apiBaseUrl}/api/auth/github/${provider}/authorize-url`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        
        const { url, state } = response
        
        // Open popup
        const width = 600
        const height = 700
        const left = window.screen.width / 2 - width / 2
        const top = window.screen.height / 2 - height / 2
        
        const popup = window.open(
          url,
          `${provider.toUpperCase()} Connect`,
          `width=${width},height=${height},left=${left},top=${top},toolbar=no,menubar=no`
        )
        
        if (!popup) {
          reject(new Error('Failed to open popup. Please allow popups for this site.'))
          return
        }
        
        // Listen for messages from popup
        let checkPopupInterval: any = null
        
        const messageHandler = async (event: MessageEvent) => {
          if (event.origin !== window.location.origin) {
            return
          }
          
          if (event.data.type === 'github-auth-success' && event.data.provider === provider) {
            const { code, receivedState } = event.data
            
            // Stop checking if popup is closed - we got the message!
            if (checkPopupInterval) {
              clearInterval(checkPopupInterval)
              checkPopupInterval = null
            }
            
            if (receivedState !== state) {
              window.removeEventListener('message', messageHandler)
              popup.close()
              reject(new Error('State mismatch'))
              return
            }
            
            try {
              // Link GitHub account
              const linkResponse: any = await $fetch(`${apiBaseUrl}/api/auth/github/${provider}/link`, {
                method: 'POST',
                headers: {
                  Authorization: `Bearer ${token}`
                },
                body: { code }
              })
              
              if (provider === 'ghec') {
                ghecUser.value = linkResponse.githubUser
                isGhecConnected.value = true
              } else {
                ghesUser.value = linkResponse.githubUser
                isGhesConnected.value = true
              }
              
              window.removeEventListener('message', messageHandler)
              popup.close()
              resolve(linkResponse.githubUser)
            } catch (error) {
              window.removeEventListener('message', messageHandler)
              popup.close()
              reject(error)
            }
          } else if (event.data.type === 'github-auth-error') {
            // Stop checking if popup is closed
            if (checkPopupInterval) {
              clearInterval(checkPopupInterval)
              checkPopupInterval = null
            }
            
            window.removeEventListener('message', messageHandler)
            popup.close()
            reject(new Error(event.data.error))
          }
        }
        
        window.addEventListener('message', messageHandler)
        
        // Check if popup is closed (but stop checking once we get a message)
        checkPopupInterval = setInterval(() => {
          if (popup.closed) {
            clearInterval(checkPopupInterval)
            window.removeEventListener('message', messageHandler)
            reject(new Error('Popup was closed'))
          }
        }, 1000)
        
      } catch (error) {
        reject(error)
      }
    })
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
    ghesEnabled,
    loginWithMicrosoft,
    connectGitHub,
    disconnectGitHub,
    handleAzureRedirect,
    checkAuthStatus,
    logout,
    getAzureToken,
  }
}
