<template>
  <div class="login-flow">
    <!-- Login State -->
    <div v-if="!loading && !error" class="text-center">
      <div class="mb-6">
        <svg class="w-20 h-20 mx-auto text-gray-700" fill="currentColor" viewBox="0 0 24 24">
          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
        </svg>
      </div>
      
      <h2 class="text-2xl font-bold text-gray-800 mb-4">
        Authenticate with GitHub
      </h2>
      
      <p class="text-gray-600 mb-6">
        Click below to login securely with your GitHub account
      </p>
      
      <button
        @click="handleLogin"
        class="bg-gray-800 hover:bg-gray-900 text-white font-semibold py-3 px-8 rounded-lg transition duration-200 inline-flex items-center"
      >
        <svg class="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 24 24">
          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
        </svg>
        Login with GitHub
      </button>
      
      <p class="text-sm text-gray-500 mt-4">
        A secure popup will open for authentication
      </p>
    </div>
    
    <!-- Loading State -->
    <div v-else-if="loading" class="text-center">
      <div class="mb-4">
        <svg class="animate-spin h-12 w-12 mx-auto text-purple-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      </div>
      
      <h2 class="text-2xl font-bold text-gray-800 mb-4">
        Authenticating...
      </h2>
      
      <p class="text-gray-600">
        Please complete the login in the popup window
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
        class="bg-gray-800 hover:bg-gray-900 text-white font-semibold py-3 px-6 rounded-lg transition duration-200"
      >
        Try Again
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
const emit = defineEmits(['authenticated'])

const { loginWithPopup } = useAuth()

const loading = ref(false)
const error = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  loading.value = true
  error.value = false
  errorMessage.value = ''
  
  try {
    const user = await loginWithPopup()
    console.log('Login successful:', user)
    
    // Small delay to show success before transitioning
    await new Promise(resolve => setTimeout(resolve, 500))
    
    emit('authenticated')
    
  } catch (err: any) {
    console.error('Login failed:', err)
    error.value = true
    errorMessage.value = err.message || 'Failed to authenticate. Please try again.'
  } finally {
    loading.value = false
  }
}

const resetAndRetry = () => {
  error.value = false
  errorMessage.value = ''
  handleLogin()
}
</script>
