<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: Number,
    default: 0,
  },
  max: {
    type: Number,
    default: 100,
  },
  label: {
    type: String,
    default: '',
  },
  showValue: {
    type: Boolean,
    default: false,
  },
})

const percentage = computed(() => {
  const v = Math.max(0, Math.min(props.value, props.max))
  return props.max > 0 ? (v / props.max) * 100 : 0
})
</script>

<template>
  <div class="space-y-1.5">
    <div v-if="label || showValue" class="flex items-center justify-between text-xs">
      <span class="text-[var(--color-foreground-muted)]">{{ label }}</span>
      <span v-if="showValue" class="font-medium text-[var(--color-foreground)]">
        {{ Math.round(percentage) }}%
      </span>
    </div>
    <div
      class="h-2 w-full overflow-hidden rounded-full bg-[var(--color-surface-muted)]"
      role="progressbar"
      :aria-valuenow="value"
      :aria-valuemax="max"
      :aria-valuemin="0"
    >
      <div
        class="h-full rounded-full bg-[var(--color-primary)] transition-[width] duration-300 ease-out"
        :style="{ width: `${percentage}%` }"
      />
    </div>
  </div>
</template>
