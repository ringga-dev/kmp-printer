import { defineContentConfig, defineCollection, z } from '@nuxt/content'

export default defineContentConfig({
  collections: {
    docs: defineCollection({
      type: 'page',
      source: 'docs/**',
      schema: z.object({
        title: z.string(),
        description: z.string(),
        prev: z.string().optional(),
        next: z.string().optional()
      })
    })
  }
})
