// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2026-05-08',
  future: {
    compatibilityVersion: 4
  },
  devtools: { enabled: true },

  modules: [
    '@nuxt/content',
    '@nuxt/image',
    '@nuxt/scripts',
    '@nuxt/ui'
  ],

  app: {
    head: {
      title: 'KmpPrinter - Professional KMP ESC/POS Library',
      meta: [
        { name: 'description', content: 'The ultimate Kotlin Multiplatform library for ESC/POS printing across Android, iOS, Desktop, and Web.' }
      ]
    }
  },

  content: {
    build: {
      markdown: {
        highlight: {
          theme: {
            default: 'github-dark',
            dark: 'github-dark'
          },
          langs: ['kotlin', 'bash', 'yaml', 'json']
        }
      }
    }
  },

  css: ['~/assets/css/main.css']
})