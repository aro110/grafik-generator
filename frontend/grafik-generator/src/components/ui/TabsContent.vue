<script setup>
import { TabsContent } from 'radix-vue'

defineProps({
  value: {
    type: [String, Number],
    required: true,
  },
})
</script>

<template>
  <TabsContent
    :value="value"
    class="mt-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-ring)]"
  >
    <slot />
  </TabsContent>
</template>
