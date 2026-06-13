<script setup>
import { computed } from 'vue'
import { cn } from '../../lib/utils'

const props = defineProps({
  padding: {
    type: String,
    default: 'md',
    validator: (v) => ['none', 'sm', 'md', 'lg'].includes(v),
  },
  interactive: {
    type: Boolean,
    default: false,
  },
})

const paddings = {
  none: '',
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8',
}

const classes = computed(() =>
  cn(
    'rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] shadow-sm',
    paddings[props.padding],
    props.interactive && 'transition-colors hover:border-[var(--color-border-strong)]',
  ),
)
</script>

<template>
  <div :class="classes">
    <slot />
  </div>
</template>
