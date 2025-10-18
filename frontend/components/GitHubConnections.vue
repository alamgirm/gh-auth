<template>
  <div class="space-y-4">
    <!-- Ghec Connection -->
    <div class="github-connection p-6 border rounded-lg" :class="isGhecConnected ? 'bg-gray-50 border-gray-400' : 'bg-gray-50 border-gray-300'">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center">
          <svg class="w-8 h-8 mr-3 text-gray-800" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
          </svg>
          
          <div>
            <h3 class="text-lg font-semibold text-gray-800 flex items-center">
              Ghec (github.com)
            </h3>
            <p class="text-sm text-gray-600">
              {{ isGhecConnected ? 'Connected' : 'Optional - for github.com repositories' }}
            </p>
          </div>
        </div>
        
        <div>
          <span v-if="isGhecConnected" class="px-3 py-1 bg-green-500 text-white rounded-full text-sm font-semibold">
            ✓ Connected
          </span>
          <span v-else class="px-3 py-1 bg-gray-400 text-white rounded-full text-sm">
            Not Connected
          </span>
        </div>
      </div>
      
      <!-- Ghec User Info -->
      <div v-if="isGhecConnected && ghecUser" class="mb-4 p-4 bg-white rounded-lg">
        <div class="flex items-center space-x-3">
          <img
            :src="ghecUser.avatarUrl"
            :alt="ghecUser.login"
            class="w-12 h-12 rounded-full border-2 border-gray-200"
          />
          <div>
            <p class="font-semibold text-gray-800">@{{ ghecUser.login }}</p>
            <p class="text-sm text-gray-600">{{ ghecUser.name }}</p>
          </div>
        </div>
      </div>
      
      <!-- Ghec Action Buttons -->
      <div class="flex space-x-3">
        <button
          v-if="!isGhecConnected"
          @click="$emit('connect', 'ghec')"
          :disabled="isConnectingGhec"
          class="flex-1 bg-gray-800 hover:bg-gray-900 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <span v-if="isConnectingGhec" class="inline-flex items-center">
            <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Connecting...
          </span>
          <span v-else>Connect Ghec</span>
        </button>
        
        <button
          v-else
          @click="$emit('disconnect', 'ghec')"
          :disabled="isDisconnectingGhec"
          class="flex-1 bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 disabled:opacity-50"
        >
          {{ isDisconnectingGhec ? 'Disconnecting...' : 'Disconnect Ghec' }}
        </button>
      </div>
      
      <p v-if="!isGhecConnected" class="text-xs text-gray-500 mt-3">
        Connect your github.com account to access public repositories and your personal repos.
      </p>
    </div>
    
    <!-- Ghes Connection -->
    <div v-if="ghesEnabled" class="github-connection p-6 border rounded-lg" :class="isGhesConnected ? 'bg-blue-50 border-blue-400' : 'bg-blue-50 border-blue-300'">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center">
          <svg class="w-8 h-8 mr-3 text-blue-800" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
          </svg>
          
          <div>
            <h3 class="text-lg font-semibold text-gray-800 flex items-center">
              Ghes (Enterprise Server)
              <span class="ml-2 px-2 py-0.5 text-xs bg-blue-600 text-white rounded">ENTERPRISE</span>
            </h3>
            <p class="text-sm text-gray-600">
              {{ isGhesConnected ? 'Connected' : 'Optional - for your enterprise server' }}
            </p>
          </div>
        </div>
        
        <div>
          <span v-if="isGhesConnected" class="px-3 py-1 bg-green-500 text-white rounded-full text-sm font-semibold">
            ✓ Connected
          </span>
          <span v-else class="px-3 py-1 bg-gray-400 text-white rounded-full text-sm">
            Not Connected
          </span>
        </div>
      </div>
      
      <!-- Ghes User Info -->
      <div v-if="isGhesConnected && ghesUser" class="mb-4 p-4 bg-white rounded-lg">
        <div class="flex items-center space-x-3">
          <img
            v-if="ghesUser.avatarUrl"
            :src="ghesUser.avatarUrl"
            :alt="ghesUser.login"
            class="w-12 h-12 rounded-full border-2 border-blue-200"
          />
          <div class="w-12 h-12 rounded-full bg-blue-200 flex items-center justify-center text-blue-800 font-bold text-xl" v-else>
            {{ ghesUser.login[0].toUpperCase() }}
          </div>
          <div>
            <p class="font-semibold text-gray-800">@{{ ghesUser.login }}</p>
            <p class="text-sm text-gray-600">{{ ghesUser.name }}</p>
          </div>
        </div>
      </div>
      
      <!-- Ghes Action Buttons -->
      <div class="flex space-x-3">
        <button
          v-if="!isGhesConnected"
          @click="$emit('connect', 'ghes')"
          :disabled="isConnectingGhes"
          class="flex-1 bg-blue-800 hover:bg-blue-900 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <span v-if="isConnectingGhes" class="inline-flex items-center">
            <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Connecting...
          </span>
          <span v-else>Connect Ghes</span>
        </button>
        
        <button
          v-else
          @click="$emit('disconnect', 'ghes')"
          :disabled="isDisconnectingGhes"
          class="flex-1 bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 disabled:opacity-50"
        >
          {{ isDisconnectingGhes ? 'Disconnecting...' : 'Disconnect Ghes' }}
        </button>
      </div>
      
      <p v-if="!isGhesConnected" class="text-xs text-gray-500 mt-3">
        Connect to your GitHub Enterprise Server to access your organization's private repositories.
      </p>
    </div>
    
    <!-- Info if Ghes not enabled -->
    <div v-else class="p-4 bg-gray-100 border border-gray-300 rounded-lg">
      <p class="text-sm text-gray-600 text-center">
        <span class="font-semibold">GitHub Enterprise Server</span> is not configured.
        Contact your administrator to enable Ghes integration.
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  isGhecConnected: boolean
  isGhesConnected: boolean
  ghecUser: any
  ghesUser: any
  ghesEnabled: boolean
  isConnectingGhec?: boolean
  isConnectingGhes?: boolean
  isDisconnectingGhec?: boolean
  isDisconnectingGhes?: boolean
}>()

defineEmits(['connect', 'disconnect'])
</script>

