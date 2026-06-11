<script setup lang="ts">
const route = useRoute()
const { data: page } = await useAsyncData(route.path, () => {
  return queryCollection('docs').path(route.path).first()
})

if (!page.value) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

// Get all docs for sidebar
const { data: navigation } = await useAsyncData('navigation', () => {
  return queryCollectionNavigation('docs')
})

const isSidebarOpen = ref(false)
</script>

<template>
  <div class="container mx-auto px-4 py-12 flex flex-col md:flex-row gap-12">
    <!-- Mobile Sidebar Toggle -->
    <div class="md:hidden flex items-center justify-between mb-8 pb-4 border-b border-slate-200 dark:border-slate-800">
      <UButton
        icon="i-heroicons-bars-3-bottom-left"
        variant="ghost"
        color="gray"
        @click="isSidebarOpen = true"
      >
        Menu
      </UButton>
      <span class="text-sm font-medium text-slate-500">{{ page?.title }}</span>
    </div>

    <!-- Sidebar Navigation -->
    <aside class="w-full md:w-64 flex-shrink-0 hidden md:block">
      <nav class="sticky top-28 overflow-y-auto max-h-[calc(100vh-160px)]">
        <div v-for="item in navigation" :key="item.path" class="mb-8">
          <h4 class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-4 px-3">
            {{ item.title }}
          </h4>
          <ul class="space-y-1">
            <li v-for="child in item.children" :key="child.path">
              <NuxtLink
                :to="child.path"
                class="block px-3 py-2 rounded-lg text-sm transition-colors hover:bg-slate-100 dark:hover:bg-slate-800"
                :class="route.path === child.path ? 'bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 font-semibold' : 'text-slate-600 dark:text-slate-400'"
              >
                {{ child.title }}
              </NuxtLink>
            </li>
          </ul>
        </div>
      </nav>
    </aside>

    <!-- Content -->
    <article class="flex-grow max-w-4xl min-w-0">
      <div v-if="page" class="prose prose-slate dark:prose-invert max-w-none prose-headings:scroll-mt-28 prose-a:text-primary-600 dark:prose-a:text-primary-400 prose-pre:bg-slate-900 prose-pre:border prose-pre:border-slate-800">
        <header class="mb-10 pb-10 border-b border-slate-200 dark:border-slate-800">
          <div class="flex items-center gap-2 text-sm text-primary-600 dark:text-primary-400 font-medium mb-4">
            <span>Documentation</span>
            <UIcon name="i-heroicons-chevron-right" class="w-3 h-3" />
            <span class="text-slate-500">{{ page.title }}</span>
          </div>
          <h1 class="text-4xl md:text-5xl font-extrabold tracking-tight mb-4">
            {{ page.title }}
          </h1>
          <p class="text-xl text-slate-500 dark:text-slate-400">
            {{ page.description }}
          </p>
        </header>

        <ContentRenderer :value="page" />

        <!-- Page Navigation -->
        <div class="mt-20 pt-10 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <UButton
            v-if="page.prev"
            :to="page.prev"
            variant="ghost"
            color="gray"
            icon="i-heroicons-arrow-left"
          >
            Previous
          </UButton>
          <div v-else></div>
          
          <UButton
            v-if="page.next"
            :to="page.next"
            color="primary"
            trailing-icon="i-heroicons-arrow-right"
          >
            Next
          </UButton>
        </div>
      </div>
    </article>

    <!-- Mobile Sidebar Drawer -->
    <USlideover v-model="isSidebarOpen" side="left">
      <div class="p-6">
        <Logo class="mb-10" />
        <nav>
          <div v-for="item in navigation" :key="item.path" class="mb-8">
            <h4 class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-4 px-3">
              {{ item.title }}
            </h4>
            <ul class="space-y-1">
              <li v-for="child in item.children" :key="child.path">
                <NuxtLink
                  :to="child.path"
                  class="block px-3 py-2 rounded-lg text-sm"
                  :class="route.path === child.path ? 'bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 font-semibold' : 'text-slate-600 dark:text-slate-400'"
                  @click="isSidebarOpen = false"
                >
                  {{ child.title }}
                </NuxtLink>
              </li>
            </ul>
          </div>
        </nav>
      </div>
    </USlideover>
  </div>
</template>

