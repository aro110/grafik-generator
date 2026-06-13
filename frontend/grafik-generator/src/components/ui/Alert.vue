<script setup>
import { computed } from 'vue'
import { CheckCircle2, AlertCircle, AlertTriangle, Info } from 'lucide-vue-next'
import { cn } from '../../lib/utils'

const props = defineProps({
  variant: {
    type: String,
    default: 'info',
    validator: (v) => ['info', 'success', 'warning', 'danger'].includes(v),
  },
  title: {
    type: String,
    default: '',
  },
  icon: {
    type: Boolean,
    default: true,
  },
})

const variants = {
  info: 'border-[var(--color-info)]/30 bg-[var(--color-info-soft)] text-[var(--color-info-foreground)]',
  success:
    'border-[var(--color-success)]/30 bg-[var(--color-success-soft)] text-[var(--color-success-foreground)]',
  warning:
    'border-[var(--color-warning)]/30 bg-[var(--color-warning-soft)] text-[var(--color-warning-foreground)]',
  danger:
    'border-[var(--color-danger)]/30 bg-[var(--color-danger-soft)] text-[var(--color-danger-foreground)]',
}

const icons = {
  info: Info,
  success: CheckCircle2,
  warning: AlertTriangle,
  danger: AlertCircle,
}

const classes = computed(() => cn('flex gap-3 rounded-lg border p-4', variants[props.variant]))
const IconComponent = computed(() => icons[props.variant])
</script>

<template>
  <div :class="classes" role="alert">
    <component
      v-if="icon"
      :is="IconComponent"
      class="mt-0.5 h-5 w-5 shrink-0"
      aria-hidden="true"
    />
    <div class="min-w-0 flex-1">
      <p v-if="title" class="text-sm font-semibold">{{ title }}</p>
      <div :class="title ? 'mt-1 text-sm' : 'text-sm'">
        <slot />
      </div>
    </div>
  </div>
</template>
