<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="max-w-3xl w-full">
      <!-- Authenticated View -->
      <div v-if="isAzureAuthenticated" class="bg-white rounded-lg shadow-2xl p-8">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-gray-800 mb-2">
            Welcome to Multi-Auth Application
          </h1>
          <p class="text-gray-600">Authenticated with Azure Entra ID</p>
        </div>
        
        <!-- Azure User Profile -->
        <div class="mb-6 p-6 bg-blue-50 border-2 border-blue-300 rounded-lg">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-xl font-bold text-gray-800 flex items-center">
              <svg class="w-6 h-6 mr-2 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
                <path d="M11.4 24H0V12.6h11.4V24zM24 24H12.6V12.6H24V24zM11.4 11.4H0V0h11.4v11.4zm12.6 0H12.6V0H24v11.4z"/>
              </svg>
              Primary Authentication
            </h2>
            <span class="px-4 py-1 bg-blue-600 text-white rounded-full text-sm font-semibold">
              Microsoft
            </span>
          </div>
          
          <UserProfile v-if="azureUser" :user="azureUser" />
        </div>
        
        <!-- GitHub Connections Status (Ghec and Ghes) -->
        <div class="mb-6">
          <GitHubConnections
            :is-ghec-connected="isGhecConnected"
            :is-ghes-connected="isGhesConnected"
            :ghec-user="ghecUser"
            :ghes-user="ghesUser"
            :is-connecting-ghec="isConnectingGhec"
            :is-connecting-ghes="isConnectingGhes"
            :is-disconnecting-ghec="isDisconnectingGhec"
            :is-disconnecting-ghes="isDisconnectingGhes"
            @connect="handleConnect"
            @disconnect="handleDisconnect"
          />
        </div>
        
        <!-- Actions -->
        <div class="text-center">
          <button
            @click="handleLogout"
            class="bg-red-500 hover:bg-red-600 text-white font-semibold py-3 px-8 rounded-lg transition duration-200"
          >
            Sign Out
          </button>
        </div>
        
        <!-- Security Note -->
        <div class="mt-6 p-4 bg-gray-50 border border-gray-300 rounded-lg text-sm text-gray-700">
          <p class="font-semibold mb-2">🔒 Security Architecture:</p>
          <ul class="list-disc list-inside space-y-1 text-xs">
            <li><strong>Azure:</strong> Token managed by MSAL in browser, sent with each request</li>
            <li><strong>Ghec:</strong> Token stored securely on backend (only if connected)</li>
            <li><strong>Ghes:</strong> Token stored securely on backend (only if connected)</li>
            <li><strong>API Calls:</strong> Backend validates Azure token and proxies GitHub requests</li>
          </ul>
        </div>
      </div>
      
      <!-- Login View -->
      <div v-else class="bg-white rounded-lg shadow-2xl p-8">
        <LoginFlow @authenticated="onAuthenticated" />
        
        <div class="mt-8 p-4 bg-blue-50 border border-blue-300 rounded-lg text-sm text-gray-700">
          <p class="font-semibold mb-2">ℹ️ About this application:</p>
          <ul class="list-disc list-inside space-y-1 text-xs">
            <li><strong>Primary:</strong> Microsoft Azure Entra ID (required)</li>
            <li><strong>Optional:</strong> Ghec (github.com) integration</li>
            <li><strong>Optional:</strong> Ghes (Enterprise Server) integration</li>
            <li><strong>Security:</strong> Tokens handled appropriately by each provider</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const { 
  azureUser, 
  ghecUser,
  ghesUser,
  isAzureAuthenticated, 
  isGhecConnected,
  isGhesConnected,
  connectGitHub,
  disconnectGitHub,
  logout,
  checkAuthStatus
} = useMultiAuth()

const isConnectingGhec = ref(false)
const isConnectingGhes = ref(false)
const isDisconnectingGhec = ref(false)
const isDisconnectingGhes = ref(false)

// Always refresh auth status on mount to get latest connection status
onMounted(async () => {
  console.log('Checking auth status on mount...')
  await checkAuthStatus()
  
  // Check if just returned from GitHub connection
  if (process.client) {
    const justConnected = sessionStorage.getItem('github_just_connected')
    if (justConnected) {
      console.log(`Just connected ${justConnected}, status refreshed`)
      sessionStorage.removeItem('github_just_connected')
    }
  }
})

const handleConnect = async (provider: 'ghec' | 'ghes') => {
  if (provider === 'ghec') {
    if (isConnectingGhec.value) return
    isConnectingGhec.value = true
  } else {
    if (isConnectingGhes.value) return
    isConnectingGhes.value = true
  }
  
  try {
    // This will trigger a full-page redirect
    await connectGitHub(provider)
    // Note: Code after this won't execute because of redirect
  } catch (error: any) {
    console.error(`Failed to connect ${provider}:`, error)
    alert(error.message || `Failed to connect ${provider.toUpperCase()} account`)
    
    if (provider === 'ghec') {
      isConnectingGhec.value = false
    } else {
      isConnectingGhes.value = false
    }
  }
}

const handleDisconnect = async (provider: 'ghec' | 'ghes') => {
  const providerName = provider === 'ghec' ? 'Ghec (github.com)' : 'Ghes (Enterprise Server)'
  
  if (confirm(`Are you sure you want to disconnect your ${providerName} account?`)) {
    if (provider === 'ghec') {
      isDisconnectingGhec.value = true
    } else {
      isDisconnectingGhes.value = true
    }
    
    try {
      await disconnectGitHub(provider)
      console.log(`${provider.toUpperCase()} disconnected successfully`)
    } catch (error) {
      console.error(`Failed to disconnect ${provider}:`, error)
      alert(`Failed to disconnect ${provider.toUpperCase()} account`)
    } finally {
      if (provider === 'ghec') {
        isDisconnectingGhec.value = false
      } else {
        isDisconnectingGhes.value = false
      }
    }
  }
}

const handleLogout = async () => {
  if (confirm('Are you sure you want to sign out?')) {
    await logout()
  }
}

const onAuthenticated = () => {
  console.log('User authenticated with Azure')
}
</script>
