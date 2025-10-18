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
        
        <!-- GitHub Connection Status -->
        <div class="mb-6">
          <GitHubConnection
            :is-connected="isGitHubConnected"
            :github-user="githubUser"
            :is-connecting="isConnecting"
            @connect="handleConnectGitHub"
            @disconnect="handleDisconnectGitHub"
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
            <li><strong>GitHub:</strong> Token stored securely on backend (only if connected)</li>
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
            <li><strong>Optional:</strong> GitHub integration for repository features</li>
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
  githubUser, 
  isAzureAuthenticated, 
  isGitHubConnected,
  connectGitHub,
  disconnectGitHub,
  logout,
  checkAuthStatus
} = useMultiAuth()

// Note: Auth check happens in app.vue on mount
// No need to duplicate here

const isConnecting = ref(false)

const handleConnectGitHub = async () => {
  if (isConnecting.value) return // Prevent double-click
  
  isConnecting.value = true
  try {
    await connectGitHub()
    console.log('GitHub connected successfully')
  } catch (error: any) {
    console.error('Failed to connect GitHub:', error)
    
    // Check if GitHub was actually connected despite the error
    // (e.g., popup closed after successful auth but before promise resolved)
    await new Promise(resolve => setTimeout(resolve, 500))
    await checkAuthStatus()
    
    if (!isGitHubConnected.value) {
      // Still not connected, show the error
      alert(error.message || 'Failed to connect GitHub account')
    } else {
      // Actually connected, ignore the error
      console.log('GitHub connected successfully (despite error)')
    }
  } finally {
    isConnecting.value = false
  }
}

const handleDisconnectGitHub = async () => {
  if (confirm('Are you sure you want to disconnect your GitHub account?')) {
    try {
      await disconnectGitHub()
      console.log('GitHub disconnected successfully')
    } catch (error) {
      console.error('Failed to disconnect GitHub:', error)
      alert('Failed to disconnect GitHub account')
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
