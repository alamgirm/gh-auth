<template>
  <div v-if="user" class="user-profile">
    <div class="flex items-center space-x-6 mb-6">
      <img
        :src="user.avatarUrl"
        :alt="user.login"
        class="w-24 h-24 rounded-full border-4 border-purple-200"
      />
      <div>
        <h2 class="text-2xl font-bold text-gray-800">
          {{ user.name || user.login }}
        </h2>
        <p class="text-gray-600">@{{ user.login }}</p>
      </div>
    </div>
    
    <div v-if="user.bio" class="mb-6">
      <p class="text-gray-700">{{ user.bio }}</p>
    </div>
    
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="text-center p-4 bg-gray-50 rounded-lg">
        <div class="text-2xl font-bold text-purple-600">
          {{ user.publicRepos || 0 }}
        </div>
        <div class="text-sm text-gray-600">Repositories</div>
      </div>
      
      <div class="text-center p-4 bg-gray-50 rounded-lg">
        <div class="text-2xl font-bold text-purple-600">
          {{ user.followers || 0 }}
        </div>
        <div class="text-sm text-gray-600">Followers</div>
      </div>
      
      <div class="text-center p-4 bg-gray-50 rounded-lg">
        <div class="text-2xl font-bold text-purple-600">
          {{ user.following || 0 }}
        </div>
        <div class="text-sm text-gray-600">Following</div>
      </div>
      
      <div v-if="user.location" class="text-center p-4 bg-gray-50 rounded-lg">
        <div class="text-sm font-semibold text-gray-800">
          📍 {{ user.location }}
        </div>
        <div class="text-sm text-gray-600">Location</div>
      </div>
    </div>
    
    <div v-if="user.email" class="text-sm text-gray-600 mb-4">
      <strong>Email:</strong> {{ user.email }}
    </div>
    
    <div class="text-sm text-gray-600">
      <strong>Member since:</strong> 
      {{ formatDate(user.createdAt) }}
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  user: any
}>()

const formatDate = (dateString: string) => {
  if (!dateString) return 'N/A'
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric' 
  })
}
</script>

