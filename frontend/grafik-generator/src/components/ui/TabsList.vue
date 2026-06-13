<script setup>
import { TabsList } from 'radix-vue'
</script>

<template>
  <TabsList
    class="inline-flex h-10 items-center justify-center gap-1 rounded-lg
      bg-[var(--color-surface-muted)] p-1 text-[var(--color-foreground-muted)]"
  >
    <slot />
  </TabsList>
</template>
