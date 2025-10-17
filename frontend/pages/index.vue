<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="max-w-2xl w-full">
      <!-- Authenticated View -->
      <div v-if="isAuthenticated" class="bg-white rounded-lg shadow-2xl p-8">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-gray-800 mb-2">
            Welcome to GitHub Device Flow!
          </h1>
          <p class="text-gray-600">You are successfully authenticated</p>
        </div>
        
        <UserProfile :user="user" />
        
        <div class="mt-8 text-center">
          <button
            @click="handleLogout"
            class="bg-red-500 hover:bg-red-600 text-white font-semibold py-3 px-8 rounded-lg transition duration-200"
          >
            Logout
          </button>
        </div>
      </div>
      
      <!-- Login View -->
      <div v-else class="bg-white rounded-lg shadow-2xl p-8">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-gray-800 mb-2">
            GitHub Device Flow Authentication
          </h1>
          <p class="text-gray-600">
            Secure OAuth authentication without exposing client secrets
          </p>
        </div>
        
        <LoginFlow @authenticated="onAuthenticated" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const { user, isAuthenticated, logout, checkAuthStatus } = useAuth()

onMounted(async () => {
  // Check if stored token is still valid
  await checkAuthStatus()
})

const handleLogout = () => {
  logout()
}

const onAuthenticated = () => {
  // User has been authenticated successfully
  console.log('User authenticated:', user.value)
}
</script>

