<script setup>
import { computed } from 'vue'
import { Check, AlertCircle } from 'lucide-vue-next'
import { cn } from '../../lib/utils'
import { useFieldValidation } from '../../composables/useFieldValidation'

const props = defineProps({
  steps: {
    type: Array,
    required: true,
  },
  currentStepIndex: {
    type: Number,
    required: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select'])

const { validation, stepHasErrors, getStepErrors, getStepWarnings } = useFieldValidation()

const totalErrors = computed(() => validation.value?.errors.length || 0)
const totalWarnings = computed(() => validation.value?.warnings.length || 0)

const onSelect = (index) => {
  if (props.disabled) return
  emit('select', index)
}

const stepStatus = (index) => {
  if (index === props.currentStepIndex) return 'active'
  if (stepHasErrors(props.steps[index].id)) return 'error'
  if (index < props.currentStepIndex) return 'completed'
  return 'upcoming'
}
</script>

<template>
  <nav aria-label="Postęp kreatora" class="space-y-1">
    <button
      v-for="(step, index) in steps"
      :key="step.id"
      type="button"
      :disabled="disabled"
      :aria-current="index === currentStepIndex ? 'step' : undefined"
      :class="cn(
        'group flex w-full items-start gap-3 rounded-lg border px-3 py-3 text-left transition-colors',
        stepStatus(index) === 'active'
          ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
          : 'border-transparent hover:bg-[var(--color-surface-muted)]',
        disabled && 'cursor-not-allowed opacity-60'
      )"
      @click="onSelect(index)"
    >
      <span
        :class="cn(
          'mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold transition-colors',
          stepStatus(index) === 'active' && 'bg-[var(--color-primary)] text-[var(--color-primary-foreground)]',
          stepStatus(index) === 'completed' && 'bg-[var(--color-success)] text-white',
          stepStatus(index) === 'error' && 'bg-[var(--color-danger)] text-white',
          stepStatus(index) === 'upcoming' && 'bg-[var(--color-surface-muted)] text-[var(--color-foreground-muted)]'
        )"
        aria-hidden="true"
      >
        <Check v-if="stepStatus(index) === 'completed'" class="h-4 w-4" />
        <AlertCircle v-else-if="stepStatus(index) === 'error'" class="h-4 w-4" />
        <span v-else>{{ step.id }}</span>
      </span>

      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-2">
          <span
            :class="cn(
              'text-sm font-semibold',
              stepStatus(index) === 'active' ? 'text-[var(--color-foreground)]' : 'text-[var(--color-foreground-muted)]'
            )"
          >
            {{ step.title }}
          </span>
          <span
            v-if="stepHasErrors(step.id) && index !== currentStepIndex"
            class="h-1.5 w-1.5 rounded-full bg-[var(--color-danger)]"
            aria-label="Krok zawiera błędy"
          />
        </div>
        <p
          :class="cn(
            'mt-0.5 text-xs leading-snug',
            stepStatus(index) === 'active'
              ? 'text-[var(--color-foreground-muted)]'
              : 'text-[var(--color-muted)]'
          )"
        >
          {{ step.description }}
        </p>
        <p
          v-if="stepHasErrors(step.id)"
          class="mt-1 text-xs font-medium text-[var(--color-danger)]"
        >
          {{ getStepErrors(step.id).length }}
          {{ getStepErrors(step.id).length === 1 ? 'błąd' : 'błędów' }}
          <template v-if="getStepWarnings(step.id).length">
            · {{ getStepWarnings(step.id).length }} ostrz.
          </template>
        </p>
      </div>
    </button>

    <div
      v-if="totalErrors > 0 || totalWarnings > 0"
      class="mt-4 rounded-md border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-xs"
    >
      <p class="text-[var(--color-foreground-muted)]">
        Łącznie:
        <span class="font-semibold text-[var(--color-danger)]">{{ totalErrors }}</span>
        {{ totalErrors === 1 ? 'błąd' : 'błędów' }}
        ·
        <span class="font-semibold text-[var(--color-warning-foreground)]">{{ totalWarnings }}</span>
        ostrz.
      </p>
    </div>
  </nav>
</template>
