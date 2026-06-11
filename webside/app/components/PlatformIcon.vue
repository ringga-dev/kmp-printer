<script setup lang="ts">
defineProps<{
  name: string
  icon: string
}>()
</script>

<template>
  <div class="flex flex-col items-center gap-3 group">
    <div class="w-16 h-16 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 flex items-center justify-center shadow-sm group-hover:border-blue-500 group-hover:shadow-md transition-all">
      <UIcon :name="icon" class="w-8 h-8 text-slate-400 group-hover:text-blue-500 transition-colors" />
    </div>
    <span class="text-xs font-bold text-slate-500 group-hover:text-slate-900 dark:group-hover:text-white">{{ name }}</span>
  </div>
</template>
