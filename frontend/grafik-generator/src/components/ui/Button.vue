<script setup>
import { computed } from 'vue'
import { cn } from '../../lib/utils'

const props = defineProps({
  variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'outline', 'ghost', 'destructive', 'subtle', 'link'].includes(v),
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'icon'].includes(v),
  },
  as: {
    type: String,
    default: 'button',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  block: {
    type: Boolean,
    default: false,
  },
})

const base =
  'inline-flex items-center justify-center gap-2 rounded-md font-medium transition-colors ' +
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-background)] ' +
  'disabled:cursor-not-allowed disabled:opacity-50 whitespace-nowrap'

const variants = {
  default:
    'bg-[var(--color-primary)] text-[var(--color-primary-foreground)] hover:bg-[var(--color-primary-hover)] shadow-sm',
  outline:
    'border border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-foreground)] hover:bg-[var(--color-surface-muted)]',
  ghost: 'bg-transparent text-[var(--color-foreground)] hover:bg-[var(--color-surface-muted)]',
  destructive:
    'bg-[var(--color-danger)] text-white hover:opacity-90 shadow-sm',
  subtle:
    'bg-[var(--color-primary-soft)] text-[var(--color-primary)] hover:bg-[var(--color-primary-soft)]/80',
  link: 'bg-transparent text-[var(--color-primary)] underline-offset-4 hover:underline px-0 h-auto',
}

const sizes = {
  sm: 'h-8 px-3 text-xs',
  md: 'h-10 px-4 text-sm',
  lg: 'h-11 px-6 text-base',
  icon: 'h-10 w-10',
}

const classes = computed(() =>
  cn(base, variants[props.variant], sizes[props.size], props.block && 'w-full'),
)
</script>

<template>
  <component
    :is="as"
    :class="classes"
    :disabled="as === 'button' ? disabled || loading : undefined"
    :aria-disabled="disabled || loading || undefined"
    :data-loading="loading || undefined"
  >
    <span
      v-if="loading"
      class="inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
      aria-hidden="true"
    />
    <slot />
  </component>
</template>
