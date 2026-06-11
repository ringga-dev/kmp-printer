<script setup lang="ts">
definePageMeta({
  layout: 'default'
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-b from-slate-100/50 to-white dark:from-slate-900/30 dark:to-slate-950">
    <!-- Hero Section -->
    <section class="pt-24 pb-16">
      <div class="container mx-auto px-4">
        <div class="max-w-4xl">
          <div class="enterprise-badge mb-4">Enterprise Pro Edition</div>
          <h1 class="text-6xl md:text-8xl font-black tracking-tighter mb-6 leading-none">
            STUDIO <br />
            <span class="text-primary-600">PRINTER</span>
          </h1>
          <p class="text-xl text-slate-500 dark:text-slate-400 max-w-2xl mb-10 font-medium">
            High-performance Kotlin Multiplatform library for professional ESC/POS printing. 
            Calibrated for reliability across Android, iOS, JVM, and Wasm.
          </p>

          <div class="flex flex-wrap gap-4">
            <UButton
              to="/docs/getting-started"
              size="xl"
              color="primary"
              class="px-10 font-bold"
            >
              START DISCOVERY
            </UButton>
            <UButton
              to="/docs/printing"
              size="xl"
              variant="outline"
              color="gray"
              class="px-10 font-bold"
            >
              EXPERT SANDBOX
            </UButton>
          </div>
        </div>
      </div>
    </section>

    <!-- Console Section (Mirroring Reliability Console) -->
    <section class="py-12">
      <div class="container mx-auto px-4">
        <div class="grid lg:grid-cols-2 gap-12 items-center">
          <div>
            <div class="flex items-center gap-2 text-red-600 dark:text-red-500 font-bold text-sm uppercase tracking-widest mb-4">
              <UIcon name="i-heroicons-bolt-solid" class="w-5 h-5" />
              Reliability Console
            </div>
            <div class="console-box h-64 border border-slate-800 shadow-2xl">
              <div class="mb-2 text-slate-500">[SYSTEM] Initializing KmpPrinter Engine...</div>
              <div class="mb-2 text-slate-500">[HARDWARE] Searching for BT:00:11:22:33:44...</div>
              <div class="mb-2 text-green-400">[READY] Hardware calibration successful. Printable area: 384 dots.</div>
              <div class="mb-2 text-primary-400">[CMD] SelectCodePage(PC437)</div>
              <div class="mb-2 text-primary-400">[CMD] SetAlignment(CENTER)</div>
              <div class="mb-2 text-primary-400">[CMD] WriteText("KmpPrinter Enterprise")</div>
              <div class="mb-2 text-primary-400">[CMD] Feed(3)</div>
              <div class="mb-2 text-green-400">[SUCCESS] Stream finished. Paper cut.</div>
              <div class="animate-pulse">_</div>
            </div>
          </div>

          <div class="space-y-8">
            <h2 class="text-3xl font-black tracking-tight">Hardened for Production.</h2>
            <div class="space-y-4 text-slate-600 dark:text-slate-400">
              <div class="flex gap-4">
                <div class="w-10 h-10 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center flex-shrink-0">
                  <UIcon name="i-heroicons-check-badge" class="w-6 h-6 text-primary-600" />
                </div>
                <div>
                  <h4 class="font-bold text-slate-900 dark:text-white">Smart Calibration</h4>
                  <p>Automatically calculates printable dots and margins for pixel-perfect centering on 58mm and 80mm paper.</p>
                </div>
              </div>
              <div class="flex gap-4">
                <div class="w-10 h-10 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center flex-shrink-0">
                  <UIcon name="i-heroicons-cpu-chip" class="w-6 h-6 text-green-600" />
                </div>
                <div>
                  <h4 class="font-bold text-slate-900 dark:text-white">Dithering Engine</h4>
                  <p>Supports Floyd-Steinberg and Atkinson dithering for high-quality grayscale image printing.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Platform Grid -->
    <section class="py-24 border-t border-slate-200 dark:border-slate-800 mt-20">
      <div class="container mx-auto px-4 text-center">
        <p class="text-sm font-bold tracking-[0.2em] text-slate-400 uppercase mb-12">Universal Compatibility</p>
        <div class="flex flex-wrap justify-center gap-16">
          <PlatformIcon name="Android" icon="i-simple-icons-android" />
          <PlatformIcon name="iOS" icon="i-simple-icons-apple" />
          <PlatformIcon name="Desktop" icon="i-simple-icons-kotlin" />
          <PlatformIcon name="Web" icon="i-simple-icons-webassembly" />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
@reference "tailwindcss";
</style>
