// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  
  modules: ['@nuxtjs/tailwindcss'],
  
  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
      azureClientId: process.env.NUXT_PUBLIC_AZURE_CLIENT_ID || '',
      azureAuthority: process.env.NUXT_PUBLIC_AZURE_AUTHORITY || 'https://login.microsoftonline.com/common',
      azureApiScope: process.env.NUXT_PUBLIC_AZURE_API_SCOPE || 'User.Read', // Backend API scope
    }
  },
  
  app: {
    head: {
      title: 'GitHub Device Flow Authentication',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'GitHub OAuth Device Flow Demo' }
      ]
    }
  }
})

