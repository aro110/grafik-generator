<script setup>
import { computed } from 'vue'
import { cn } from '../../lib/utils'

const props = defineProps({
  variant: {
    type: String,
    default: 'neutral',
    validator: (v) =>
      ['neutral', 'primary', 'success', 'warning', 'danger', 'info', 'outline'].includes(v),
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md'].includes(v),
  },
})

const base =
  'inline-flex items-center gap-1.5 rounded-full font-medium whitespace-nowrap'

const variants = {
  neutral: 'bg-[var(--color-surface-muted)] text-[var(--color-foreground-muted)]',
  primary: 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]',
  success: 'bg-[var(--color-success-soft)] text-[var(--color-success-foreground)]',
  warning: 'bg-[var(--color-warning-soft)] text-[var(--color-warning-foreground)]',
  danger: 'bg-[var(--color-danger-soft)] text-[var(--color-danger-foreground)]',
  info: 'bg-[var(--color-info-soft)] text-[var(--color-info-foreground)]',
  outline: 'border border-[var(--color-border-strong)] text-[var(--color-foreground-muted)]',
}

const sizes = {
  sm: 'px-2 py-0.5 text-[11px]',
  md: 'px-2.5 py-1 text-xs',
}

const classes = computed(() => cn(base, variants[props.variant], sizes[props.size]))
</script>

<template>
  <span :class="classes">
    <slot />
  </span>
</template>
