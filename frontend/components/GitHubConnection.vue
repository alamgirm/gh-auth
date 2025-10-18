<template>
  <div class="github-connection p-6 border rounded-lg" :class="isConnected ? 'bg-green-50 border-green-300' : 'bg-gray-50 border-gray-300'">
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center">
        <svg class="w-8 h-8 mr-3 text-gray-800" fill="currentColor" viewBox="0 0 24 24">
          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
        </svg>
        
        <div>
          <h3 class="text-lg font-semibold text-gray-800">GitHub Integration</h3>
          <p class="text-sm text-gray-600">
            {{ isConnected ? 'Connected' : 'Optional - for repository features' }}
          </p>
        </div>
      </div>
      
      <div>
        <span v-if="isConnected" class="px-3 py-1 bg-green-500 text-white rounded-full text-sm font-semibold">
          ✓ Connected
        </span>
        <span v-else class="px-3 py-1 bg-gray-400 text-white rounded-full text-sm">
          Not Connected
        </span>
      </div>
    </div>
    
    <!-- GitHub User Info (if connected) -->
    <div v-if="isConnected && githubUser" class="mb-4 p-4 bg-white rounded-lg">
      <div class="flex items-center space-x-3">
        <img
          :src="githubUser.avatarUrl"
          :alt="githubUser.login"
          class="w-12 h-12 rounded-full border-2 border-gray-200"
        />
        <div>
          <p class="font-semibold text-gray-800">@{{ githubUser.login }}</p>
          <p class="text-sm text-gray-600">{{ githubUser.name }}</p>
        </div>
      </div>
    </div>
    
    <!-- Action Buttons -->
    <div class="flex space-x-3">
      <button
        v-if="!isConnected"
        @click="handleConnect"
        :disabled="isConnecting"
        class="flex-1 bg-gray-800 hover:bg-gray-900 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <span v-if="isConnecting" class="inline-flex items-center">
          <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          Connecting...
        </span>
        <span v-else>Connect GitHub</span>
      </button>
      
      <button
        v-else
        @click="handleDisconnect"
        :disabled="disconnecting"
        class="flex-1 bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 disabled:opacity-50"
      >
        {{ disconnecting ? 'Disconnecting...' : 'Disconnect GitHub' }}
      </button>
    </div>
    
    <p v-if="!isConnected" class="text-xs text-gray-500 mt-3">
      Connect your GitHub account to access repository features, manage code, and more.
    </p>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  isConnected: boolean
  githubUser: any
  isConnecting?: boolean
}>()

const emit = defineEmits(['connect', 'disconnect'])

const disconnecting = ref(false)

const handleConnect = () => {
  emit('connect')
}

const handleDisconnect = async () => {
  disconnecting.value = true
  try {
    await emit('disconnect')
  } finally {
    disconnecting.value = false
  }
}
</script>

