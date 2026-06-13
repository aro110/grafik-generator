<script setup>
import { computed } from 'vue'
import { cn } from '../../lib/utils'

const props = defineProps({
  orientation: {
    type: String,
    default: 'horizontal',
    validator: (v) => ['horizontal', 'vertical'].includes(v),
  },
})

const classes = computed(() =>
  cn(
    'shrink-0 bg-[var(--color-border)]',
    props.orientation === 'horizontal' ? 'h-px w-full' : 'h-full w-px',
  ),
)
</script>

<template>
  <div :class="classes" role="separator" :aria-orientation="orientation" />
</template>
