<script setup>
import { computed } from 'vue'
import { cn } from '../../lib/utils'

const props = defineProps({
  title: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
  width: {
    type: String,
    default: 'standard',
    validator: (v) => ['standard', 'wide', 'full'].includes(v),
  },
})

const widthClass = computed(() => {
  if (props.width === 'wide') return 'max-w-screen-2xl'
  if (props.width === 'full') return 'max-w-none'
  return 'max-w-7xl'
})

const containerClass = computed(() => cn('mx-auto px-4 py-6 sm:px-6 lg:px-8 lg:py-8', widthClass.value))
</script>

<template>
  <div :class="containerClass">
    <header v-if="title || $slots.header || $slots.actions" class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <slot name="header">
          <h1 v-if="title" class="text-2xl font-semibold tracking-tight text-[var(--color-foreground)]">
            {{ title }}
          </h1>
          <p v-if="description" class="mt-1 text-sm text-[var(--color-foreground-muted)]">
            {{ description }}
          </p>
        </slot>
      </div>
      <div v-if="$slots.actions" class="flex flex-wrap items-center gap-2">
        <slot name="actions" />
      </div>
    </header>

    <slot />
  </div>
</template>
