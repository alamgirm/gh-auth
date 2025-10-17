<template>
  <div id="app">
    <NuxtPage />
  </div>
</template>

<script setup lang="ts">
const { loadAuthState } = useAuth()

// Load auth state on mount
onMounted(() => {
  loadAuthState()
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

