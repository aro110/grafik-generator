<script setup>
const items = [
  { label: 'Optymalnie (100%)', dot: 'bg-[var(--color-success)]' },
  { label: 'Niedobór (<100%)', dot: 'bg-[var(--color-danger)]' },
  { label: 'Nadmiar (>100%)', dot: 'bg-[var(--color-warning)]' },
  { label: 'Dzień zamknięty', dot: 'bg-[var(--color-muted)]' },
]
</script>

<template>
  <div
    class="flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-[var(--color-foreground-muted)]"
  >
    <span class="font-medium text-[var(--color-foreground)]">Legenda obsady:</span>
    <span v-for="item in items" :key="item.label" class="flex items-center gap-2">
      <span :class="['inline-block h-2.5 w-2.5 rounded-full', item.dot]" aria-hidden="true" />
      {{ item.label }}
    </span>
  </div>
</template>
