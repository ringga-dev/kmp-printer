import type { PageCollectionItemBase, DataCollectionItemBase } from '@nuxt/content'

declare module '@nuxt/content' {
   interface DocsCollectionItem extends PageCollectionItemBase {
    title: string
    description: string
    prev?: string
    next?: string
  }
  

  interface PageCollections {
    docs: DocsCollectionItem
  }

  interface Collections {
    docs: DocsCollectionItem
  }
}
