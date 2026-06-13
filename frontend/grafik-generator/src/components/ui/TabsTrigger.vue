<script setup>
import { TabsTrigger } from 'radix-vue'

defineProps({
  value: {
    type: [String, Number],
    required: true,
  },
})
</script>

<template>
  <TabsTrigger
    :value="value"
    class="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md px-3 py-1.5 text-sm font-medium
      transition-colors
      focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-ring)]
      disabled:pointer-events-none disabled:opacity-50
      data-[state=active]:bg-[var(--color-surface)] data-[state=active]:text-[var(--color-foreground)]
      data-[state=active]:shadow-sm"
  >
    <slot />
  </TabsTrigger>
</template>
