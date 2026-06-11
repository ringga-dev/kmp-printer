<template>
  <div class="min-h-screen flex flex-col font-sans">
    <!-- Header -->
    <header class="sticky top-0 z-50 glass-panel border-b border-slate-200 dark:border-slate-800">
      <div class="container mx-auto px-4 h-16 flex items-center justify-between">
        <NuxtLink to="/">
          <Logo />
        </NuxtLink>

        <nav class="hidden md:flex items-center gap-8">
          <NuxtLink to="/docs/getting-started" class="text-sm font-medium hover:text-primary-600 transition-colors">Documentation</NuxtLink>
          <NuxtLink to="/examples" class="text-sm font-medium hover:text-primary-600 transition-colors">Examples</NuxtLink>
          <a href="https://github.com/ringga-dev/Printer-ESC-POS" target="_blank" class="text-sm font-medium hover:text-primary-600 transition-colors">GitHub</a>
        </nav>

        <div class="flex items-center gap-4">
          <ColorModeButton />
          <UButton
            to="/docs/getting-started"
            color="primary"
            variant="solid"
            size="sm"
            class="hidden sm:flex"
          >
            Get Started
          </UButton>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-grow">
      <slot />
    </main>

    <!-- Footer -->
    <footer class="border-t border-slate-200 dark:border-slate-800 py-12 bg-slate-50 dark:bg-slate-900/50">
      <div class="container mx-auto px-4 text-center">
        <Logo class="justify-center mb-6" />
        <p class="text-slate-500 dark:text-slate-400 max-w-md mx-auto mb-8">
          The ultimate Kotlin Multiplatform library for professional ESC/POS printing.
        </p>
        <div class="flex justify-center gap-6 mb-8">
          <a href="#" class="text-slate-400 hover:text-primary-600 transition-colors"><UIcon name="i-simple-icons-github" class="w-6 h-6" /></a>
          <a href="#" class="text-slate-400 hover:text-primary-600 transition-colors"><UIcon name="i-simple-icons-x" class="w-6 h-6" /></a>
        </div>
        <p class="text-sm text-slate-400">
          &copy; {{ new Date().getFullYear() }} KmpPrinter Enterprise. All rights reserved.
        </p>
      </div>
    </footer>
  </div>
</template>

