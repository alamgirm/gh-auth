<template>
  <div id="app">
    <NuxtPage />
  </div>
</template>

<script setup lang="ts">
const { handleAzureRedirect } = useMultiAuth()

// Handle Azure redirect on mount
onMounted(async () => {
  try {
    // Check for Azure redirect response
    // MSAL will handle it if there's a redirect response
    const azureResult = await handleAzureRedirect()
    
    if (azureResult) {
      console.log('Azure redirect handled successfully, user logged in')
      // Clean URL to remove query parameters
      window.history.replaceState({}, document.title, window.location.pathname)
    }
    // Note: Auth status check happens in index.vue to avoid duplicate calls
  } catch (error) {
    console.error('Error handling Azure redirect:', error)
  }
})
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}

#app {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>

