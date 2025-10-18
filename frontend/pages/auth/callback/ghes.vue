<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-800 to-blue-900 p-4">
    <div class="bg-white rounded-lg shadow-2xl p-8 max-w-md w-full text-center">
      <div v-if="status === 'processing'" class="text-center">
        <svg class="animate-spin h-12 w-12 mx-auto text-blue-800 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        
        <h2 class="text-2xl font-bold text-gray-800 mb-2">
          Completing Ghes Authentication
        </h2>
        
        <p class="text-gray-600">
          Please wait while we finish logging you in...
        </p>
      </div>
      
      <div v-else-if="status === 'success'" class="text-center">
        <svg class="w-16 h-16 mx-auto text-green-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        
        <h2 class="text-2xl font-bold text-gray-800 mb-2">
          Success!
        </h2>
        
        <p class="text-gray-600">
          You can close this window now.
        </p>
      </div>
      
      <div v-else-if="status === 'error'" class="text-center">
        <svg class="w-16 h-16 mx-auto text-red-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        
        <h2 class="text-2xl font-bold text-gray-800 mb-2">
          Authentication Failed
        </h2>
        
        <p class="text-gray-600 mb-4">
          {{ errorMessage }}
        </p>
        
        <p class="text-sm text-gray-500">
          You can close this window and try again.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const status = ref<'processing' | 'success' | 'error'>('processing')
const errorMessage = ref('')

onMounted(() => {
  try {
    const provider = 'ghes'
    const urlParams = new URLSearchParams(window.location.search)
    const code = urlParams.get('code')
    const state = urlParams.get('state')
    const error = urlParams.get('error')
    const errorDescription = urlParams.get('error_description')
    
    console.log('Ghes callback received:', { code, state, error })
    
    if (error) {
      status.value = 'error'
      errorMessage.value = errorDescription || error || 'Authentication failed'
      
      if (window.opener) {
        window.opener.postMessage({
          type: 'github-auth-error',
          error: errorMessage.value,
          provider: provider
        }, window.location.origin)
      }
      
      setTimeout(() => window.close(), 3000)
      return
    }
    
    if (!code || !state) {
      status.value = 'error'
      errorMessage.value = 'Missing authorization code or state'
      
      if (window.opener) {
        window.opener.postMessage({
          type: 'github-auth-error',
          error: errorMessage.value,
          provider: provider
        }, window.location.origin)
      }
      return
    }
    
    if (window.opener) {
      window.opener.postMessage({
        type: 'github-auth-success',
        code: code,
        receivedState: state,
        provider: provider
      }, window.location.origin)
      
      status.value = 'success'
      setTimeout(() => window.close(), 2000)
    } else {
      status.value = 'error'
      errorMessage.value = 'Could not communicate with parent window'
    }
    
  } catch (err: any) {
    console.error('Error in callback:', err)
    status.value = 'error'
    errorMessage.value = err.message || 'An unexpected error occurred'
    
    if (window.opener) {
      window.opener.postMessage({
        type: 'github-auth-error',
        error: errorMessage.value,
        provider: 'ghes'
      }, window.location.origin)
    }
  }
})
</script>

