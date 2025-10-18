<template>
  <div class="login-flow text-center">
    <!-- Not authenticated with Azure -->
    <div v-if="!loading && !error">
      <div class="mb-6">
        <svg class="w-16 h-16 mx-auto text-blue-600 mb-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M11.4 24H0V12.6h11.4V24zM24 24H12.6V12.6H24V24zM11.4 11.4H0V0h11.4v11.4zm12.6 0H12.6V0H24v11.4z"/>
        </svg>
        
        <h2 class="text-2xl font-bold text-gray-800 mb-2">
          Sign in with Microsoft
        </h2>
        
        <p class="text-gray-600 mb-6">
          Azure Entra ID authentication is required to use this application
        </p>
      </div>
      
      <button
        @click="handleAzureLogin"
        class="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-4 px-6 rounded-lg transition duration-200 flex items-center justify-center shadow-lg"
      >
        <svg class="w-6 h-6 mr-3" fill="currentColor" viewBox="0 0 24 24">
          <path d="M11.4 24H0V12.6h11.4V24zM24 24H12.6V12.6H24V24zM11.4 11.4H0V0h11.4v11.4zm12.6 0H12.6V0H24v11.4z"/>
        </svg>
        Sign in with Microsoft
      </button>
      
      <p class="text-sm text-gray-500 mt-4">
        Will redirect to Microsoft for secure authentication
      </p>
    </div>
    
    <!-- Loading State -->
    <div v-else-if="loading" class="text-center">
      <div class="mb-4">
        <svg class="animate-spin h-12 w-12 mx-auto text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      </div>
      
      <h2 class="text-2xl font-bold text-gray-800 mb-4">
        Authenticating...
      </h2>
      
      <p class="text-gray-600">
        {{ loadingMessage }}
      </p>
    </div>
    
    <!-- Error State -->
    <div v-else-if="error" class="text-center">
      <div class="mb-4">
        <svg class="w-20 h-20 mx-auto text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
      </div>
      
      <h2 class="text-2xl font-bold text-gray-800 mb-4">
        Authentication Failed
      </h2>
      
      <p class="text-gray-600 mb-6">
        {{ errorMessage }}
      </p>
      
      <button
        @click="resetAndRetry"
        class="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-6 rounded-lg transition duration-200"
      >
        Try Again
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
const emit = defineEmits(['authenticated'])

const { loginWithMicrosoft } = useMultiAuth()

const loading = ref(false)
const error = ref(false)
const errorMessage = ref('')
const loadingMessage = ref('')

const handleAzureLogin = async () => {
  loading.value = true
  error.value = false
  errorMessage.value = ''
  loadingMessage.value = 'Redirecting to Microsoft...'
  
  try {
    const result = await loginWithMicrosoft()
    
    // Redirect initiated
    if (result === null) {
      console.log('Azure redirect initiated')
      return
    }
    
    // Silent token acquired
    console.log('Azure authentication successful')
    await new Promise(resolve => setTimeout(resolve, 500))
    emit('authenticated')
    
  } catch (err: any) {
    console.error('Azure login failed:', err)
    error.value = true
    errorMessage.value = err.message || 'Failed to authenticate with Microsoft. Please try again.'
    loading.value = false
  }
}

const resetAndRetry = () => {
  error.value = false
  errorMessage.value = ''
}
</script>
