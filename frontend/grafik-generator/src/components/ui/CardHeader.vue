<script setup>
defineProps({
  title: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <div class="border-b border-[var(--color-border)] px-6 py-4">
    <div class="flex items-start justify-between gap-4">
      <div class="min-w-0">
        <h3 v-if="title" class="text-base font-semibold text-[var(--color-foreground)]">
          {{ title }}
        </h3>
        <p v-if="description" class="mt-1 text-sm text-[var(--color-muted)]">
          {{ description }}
        </p>
        <slot name="title" />
      </div>
      <div v-if="$slots.actions" class="flex shrink-0 items-center gap-2">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>
