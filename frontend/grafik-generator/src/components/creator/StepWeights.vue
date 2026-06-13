<script setup>
import { Check } from 'lucide-vue-next'
import { OPTIMIZATION_PRESETS, applyOptimizationPreset } from '../../utils/creatorDraft'
import { cn } from '../../lib/utils'
import Card from '../ui/Card.vue'
import CardHeader from '../ui/CardHeader.vue'
import Alert from '../ui/Alert.vue'

const draft = defineModel({
  type: Object,
  required: true,
})

const selectPreset = (presetKey) => {
  applyOptimizationPreset(draft.value, presetKey)
}
</script>

<template>
  <div class="space-y-6">
    <div class="grid gap-4 md:grid-cols-2">
      <button
        v-for="preset in OPTIMIZATION_PRESETS"
        :key="preset.key"
        type="button"
        :class="cn(
          'rounded-lg border bg-[var(--color-surface)] p-5 text-left transition-all',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-ring)]',
          draft.weights.preset === preset.key
            ? 'border-[var(--color-primary)] ring-2 ring-[var(--color-primary)]/20'
            : 'border-[var(--color-border)] hover:border-[var(--color-border-strong)]'
        )"
        @click="selectPreset(preset.key)"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <h3 class="text-base font-semibold text-[var(--color-foreground)]">
              {{ preset.label }}
            </h3>
            <p class="mt-2 text-sm text-[var(--color-foreground-muted)]">
              {{ preset.description }}
            </p>
          </div>
          <span
            :class="cn(
              'mt-1 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border',
              draft.weights.preset === preset.key
                ? 'border-[var(--color-primary)] bg-[var(--color-primary)] text-[var(--color-primary-foreground)]'
                : 'border-[var(--color-border-strong)]'
            )"
            aria-hidden="true"
          >
            <Check v-if="draft.weights.preset === preset.key" class="h-3 w-3" />
          </span>
        </div>
      </button>
    </div>

    <Card padding="none">
      <CardHeader
        title="Aktualne wartości presetu"
        description="Wybrany preset ustawia parametry algorytmu w kolejnym kroku — możesz je jeszcze ręcznie zmienić."
      />
      <div class="grid gap-4 p-6 sm:grid-cols-2 lg:grid-cols-4">
        <div class="rounded-md border border-[var(--color-border)] p-4">
          <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">
            Population size
          </p>
          <p class="mt-2 text-lg font-semibold text-[var(--color-foreground)]">
            {{ draft.params.populationSize }}
          </p>
        </div>
        <div class="rounded-md border border-[var(--color-border)] p-4">
          <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">
            Generations
          </p>
          <p class="mt-2 text-lg font-semibold text-[var(--color-foreground)]">
            {{ draft.params.generations }}
          </p>
        </div>
        <div class="rounded-md border border-[var(--color-border)] p-4">
          <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">
            Mutation rate
          </p>
          <p class="mt-2 text-lg font-semibold text-[var(--color-foreground)]">
            {{ draft.params.mutationRate }}
          </p>
        </div>
        <div class="rounded-md border border-[var(--color-border)] p-4">
          <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">
            Wybrany preset
          </p>
          <p class="mt-2 text-lg font-semibold text-[var(--color-foreground)]">
            {{ OPTIMIZATION_PRESETS.find((p) => p.key === draft.weights.preset)?.label || 'Domyślny' }}
          </p>
        </div>
      </div>
    </Card>

    <Alert variant="warning" title="Jak działa ten krok?">
      Backend nie ma osobnego pola <strong>weights</strong>, dlatego ten krok ustawia rekomendowane
      wartości startowe dla parametrów algorytmu genetycznego w następnym kroku.
    </Alert>
  </div>
</template>
