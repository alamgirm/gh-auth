<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="max-w-2xl w-full">
      <!-- Authenticated View -->
      <div v-if="isAuthenticated" class="bg-white rounded-lg shadow-2xl p-8">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-gray-800 mb-2">
            Welcome to GitHub OAuth!
          </h1>
          <p class="text-gray-600">You are successfully authenticated</p>
          <p class="text-xs text-gray-400 mt-2">
            User ID: {{ userId }} (Token stored securely on backend)
          </p>
        </div>
        
        <UserProfile :user="user" />
        
        <div class="mt-8 text-center space-x-4">
          <button
            @click="handleRefresh"
            :disabled="refreshing"
            class="bg-purple-500 hover:bg-purple-600 text-white font-semibold py-3 px-8 rounded-lg transition duration-200 disabled:opacity-50"
          >
            {{ refreshing ? 'Refreshing...' : 'Refresh Profile' }}
          </button>
          
          <button
            @click="handleLogout"
            class="bg-red-500 hover:bg-red-600 text-white font-semibold py-3 px-8 rounded-lg transition duration-200"
          >
            Logout
          </button>
        </div>
        
        <div class="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg text-sm text-gray-700">
          <p class="font-semibold mb-1">🔒 Security Note:</p>
          <p>Your GitHub access token is stored securely in the backend database. The frontend only has a session identifier.</p>
        </div>
      </div>
      
      <!-- Login View -->
      <div v-else class="bg-white rounded-lg shadow-2xl p-8">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-gray-800 mb-2">
            GitHub OAuth Authentication
          </h1>
          <p class="text-gray-600">
            Secure authentication with backend token storage
          </p>
        </div>
        
        <LoginFlow @authenticated="onAuthenticated" />
        
        <div class="mt-6 p-4 bg-green-50 border border-green-200 rounded-lg text-sm text-gray-700">
          <p class="font-semibold mb-1">✨ How it works:</p>
          <ul class="list-disc list-inside space-y-1 text-xs">
            <li>You authenticate via popup</li>
            <li>Backend securely stores your GitHub token</li>
            <li>Frontend receives only a session ID</li>
            <li>All GitHub API calls go through backend</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const { user, userId, isAuthenticated, logout, checkAuthStatus, refreshUserData } = useAuth()
const refreshing = ref(false)

onMounted(async () => {
  // Check if user has a valid session
  await checkAuthStatus()
})

const handleLogout = async () => {
  await logout()
}

const handleRefresh = async () => {
  refreshing.value = true
  try {
    await refreshUserData()
  } catch (error) {
    console.error('Failed to refresh user data:', error)
  } finally {
    refreshing.value = false
  }
}

const onAuthenticated = () => {
  // User has been authenticated successfully
  console.log('User authenticated with userId:', userId.value)
}
</script>
