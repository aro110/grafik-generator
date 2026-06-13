<script setup>
import { computed } from 'vue'
import { cn } from '../../lib/utils'

const props = defineProps({
  for: {
    type: String,
    default: undefined,
  },
  required: {
    type: Boolean,
    default: false,
  },
})

const classes = computed(() =>
  cn('block text-sm font-medium text-[var(--color-foreground)] select-none'),
)
</script>

<template>
  <label :for="props.for" :class="classes">
    <slot />
    <span v-if="required" class="ml-1 text-[var(--color-danger)]" aria-hidden="true">*</span>
  </label>
</template>
