<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-800 to-gray-900 p-4">
    <div class="bg-white rounded-lg shadow-2xl p-8 max-w-md w-full text-center">
      <div v-if="status === 'processing'" class="text-center">
        <svg class="animate-spin h-12 w-12 mx-auto text-gray-800 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        
        <h2 class="text-2xl font-bold text-gray-800 mb-2">
          Connecting Ghec Account
        </h2>
        
        <p class="text-gray-600">
          {{ statusMessage }}
        </p>
      </div>
      
      <div v-else-if="status === 'success'" class="text-center">
        <svg class="w-16 h-16 mx-auto text-green-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        
        <h2 class="text-2xl font-bold text-gray-800 mb-2">
          Ghec Connected!
        </h2>
        
        <p class="text-gray-600 mb-4">
          Redirecting you back...
        </p>
      </div>
      
      <div v-else-if="status === 'error'" class="text-center">
        <svg class="w-16 h-16 mx-auto text-red-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        
        <h2 class="text-2xl font-bold text-gray-800 mb-2">
          Connection Failed
        </h2>
        
        <p class="text-gray-600 mb-4">
          {{ errorMessage }}
        </p>
        
        <button
          @click="goBack"
          class="bg-gray-800 hover:bg-gray-900 text-white font-semibold py-2 px-6 rounded-lg transition duration-200"
        >
          Go Back
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const config = useRuntimeConfig()
const apiBaseUrl = config.public.apiBaseUrl

const status = ref<'processing' | 'success' | 'error'>('processing')
const statusMessage = ref('Verifying authorization...')
const errorMessage = ref('')

const goBack = () => {
  navigateTo('/')
}

onMounted(async () => {
  try {
    const provider = 'ghec'
    
    // Get OAuth callback parameters
    const urlParams = new URLSearchParams(window.location.search)
    const code = urlParams.get('code')
    const state = urlParams.get('state')
    const error = urlParams.get('error')
    const errorDescription = urlParams.get('error_description')
    
    console.log('Ghec callback received:', { code: !!code, state: !!state, error })
    
    // Check for OAuth errors
    if (error) {
      status.value = 'error'
      errorMessage.value = errorDescription || error || 'Authorization failed'
      return
    }
    
    if (!code || !state) {
      status.value = 'error'
      errorMessage.value = 'Missing authorization code or state'
      return
    }
    
    // Verify state matches
    const savedState = sessionStorage.getItem('github_oauth_state')
    const savedProvider = sessionStorage.getItem('github_oauth_provider')
    
    if (state !== savedState) {
      status.value = 'error'
      errorMessage.value = 'State mismatch - possible CSRF attack'
      return
    }
    
    if (savedProvider !== provider) {
      status.value = 'error'
      errorMessage.value = 'Provider mismatch'
      return
    }
    
    // Get Azure token
    statusMessage.value = 'Getting authentication token...'
    const { getAzureToken } = useMultiAuth()
    const azureToken = await getAzureToken()
    
    if (!azureToken) {
      status.value = 'error'
      errorMessage.value = 'Azure authentication expired. Please login again.'
      setTimeout(() => {
        navigateTo('/')
      }, 3000)
      return
    }
    
    // Link GitHub account
    statusMessage.value = 'Linking Ghec account...'
    const response: any = await $fetch(`${apiBaseUrl}/api/auth/github/${provider}/link`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${azureToken}`
      },
      body: { code }
    })
    
    console.log('Ghec linked successfully:', response.githubUser.login)
    
    // Clear OAuth session storage
    sessionStorage.removeItem('github_oauth_state')
    sessionStorage.removeItem('github_oauth_provider')
    
    // Set success flag for main page
    sessionStorage.setItem('github_just_connected', 'ghec')
    
    // Success!
    status.value = 'success'
    
    // Redirect back to main page
    setTimeout(() => {
      navigateTo('/')
    }, 1500)
    
  } catch (err: any) {
    console.error('Error in Ghec callback:', err)
    status.value = 'error'
    errorMessage.value = err.data?.error || err.message || 'Failed to connect Ghec account'
    
    // Clear session storage on error
    sessionStorage.removeItem('github_oauth_state')
    sessionStorage.removeItem('github_oauth_provider')
  }
})
</script>
