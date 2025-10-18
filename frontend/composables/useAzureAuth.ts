import { PublicClientApplication, type Configuration, type AccountInfo } from '@azure/msal-browser'

export const useAzureAuth = () => {
  const config = useRuntimeConfig()
  const apiBaseUrl = config.public.apiBaseUrl
  
  let msalInstance: PublicClientApplication | null = null
  
  // MSAL configuration
  const getMsalConfig = (): Configuration => {
    return {
      auth: {
        clientId: config.public.azureClientId || 'your_azure_client_id',
        authority: config.public.azureAuthority || 'https://login.microsoftonline.com/common',
        redirectUri: window.location.origin,
      },
      cache: {
        cacheLocation: 'localStorage',
        storeAuthStateInCookie: false,
      }
    }
  }
  
  // Get scopes for token request
  const getScopes = () => {
    const apiScope = config.public.azureApiScope || 'User.Read'
    return [apiScope]
  }
  
  //console.log('config', config)
  // Initialize MSAL
  const initializeMsal = async () => {
    if (!msalInstance) {
      msalInstance = new PublicClientApplication(getMsalConfig())
      await msalInstance.initialize()
    }
    return msalInstance
  }
  
  // Login with Azure (redirect flow with silent token acquisition)
  const loginWithAzure = async () => {
    try {
      const msal = await initializeMsal()
      
      // Try silent sign-in first
      const accounts = msal.getAllAccounts()
      
      if (accounts.length > 0) {
        // User already signed in, acquire token silently
        return await acquireTokenSilent(accounts[0])
      } else {
        // No account found, use redirect (full page navigation)
        await loginWithRedirect()
        // This will navigate away, so we never return from here
        return null
      }
    } catch (error) {
      console.error('Error during Azure login:', error)
      throw error
    }
  }
  
  // Acquire token silently
  const acquireTokenSilent = async (account: AccountInfo) => {
    try {
      const msal = await initializeMsal()
      
      const silentRequest = {
        scopes: getScopes(),
        account: account,
      }
      
      console.log('Attempting silent token acquisition for:', account.username)
      console.log('Requesting scopes:', getScopes())
      
      const response = await msal.acquireTokenSilent(silentRequest)
      
      console.log('Silent token acquired successfully')
      
      // Validate token with backend
      return await validateTokenWithBackend(response.accessToken)
      
    } catch (error) {
      console.error('Silent token acquisition failed:', error)
      // Fall back to interactive redirect
      await loginWithRedirect()
      return null
    }
  }
  
  // Login with redirect (full page navigation)
  const loginWithRedirect = async () => {
    try {
      const msal = await initializeMsal()
      
      const loginRequest = {
        scopes: getScopes(),
      }
      
      console.log('Redirecting to Azure login...')
      console.log('Requesting scopes:', getScopes())
      
      // This will navigate away from the page
      await msal.loginRedirect(loginRequest)
      
    } catch (error) {
      console.error('Azure redirect login failed:', error)
      throw error
    }
  }
  
  // Handle redirect callback after Azure authentication
  const handleRedirectCallback = async () => {
    try {
      const msal = await initializeMsal()
      
      // Handle redirect response
      const response = await msal.handleRedirectPromise()
      
      if (response) {
        console.log('Azure redirect successful:', response.account.username)
        // Validate token with backend
        return await validateTokenWithBackend(response.accessToken)
      } else {
        // Check if there's an existing account (silent flow)
        const accounts = msal.getAllAccounts()
        if (accounts.length > 0) {
          return await acquireTokenSilent(accounts[0])
        }
      }
      
      return null
      
    } catch (error) {
      console.error('Error handling redirect:', error)
      throw error
    }
  }
  
  // Validate token with backend and get user info
  // Note: Backend validates but doesn't store Azure token
  const validateTokenWithBackend = async (accessToken: string) => {
    try {
      const response: any = await $fetch(`${apiBaseUrl}/api/auth/user`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${accessToken}`
        }
      })
      
      console.log('Azure token validated, user:', response)
      return { user: response, userId: response.id }
      
    } catch (error) {
      console.error('Token validation failed:', error)
      throw error
    }
  }
  
  // Get current Azure account
  const getCurrentAccount = async () => {
    const msal = await initializeMsal()
    const accounts = msal.getAllAccounts()
    return accounts.length > 0 ? accounts[0] : null
  }
  
  // Logout from Azure (redirect)
  const logoutAzure = async () => {
    try {
      const msal = await initializeMsal()
      const account = await getCurrentAccount()
      
      if (account) {
        await msal.logoutRedirect({
          account: account,
        })
      }
      
      console.log('Azure logout redirect initiated')
    } catch (error) {
      console.error('Azure logout error:', error)
    }
  }
  
  return {
    loginWithAzure,
    acquireTokenSilent,
    handleRedirectCallback,
    getCurrentAccount,
    logoutAzure,
    initializeMsal,
  }
}

