<script setup>
import { computed } from 'vue'
import { STAFFING_TARGET_MODES, WEEKDAY_OPTIONS } from '../../utils/creatorDraft'
import { useFieldValidation } from '../../composables/useFieldValidation'
import Card from '../ui/Card.vue'
import CardHeader from '../ui/CardHeader.vue'
import FormField from '../ui/FormField.vue'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'
import Switch from '../ui/Switch.vue'
import Alert from '../ui/Alert.vue'

const draft = defineModel({
  type: Object,
  required: true,
})

const { getError } = useFieldValidation()

const staffingTargets = draft.value.goals.staffingTargets

const minSectionEmployees = computed(() => {
  if (!Array.isArray(draft.value.sections) || draft.value.sections.length === 0) return 0
  return Math.min(
    ...draft.value.sections.map((s) => (Array.isArray(s.employees) ? s.employees.length : 0)),
  )
})

const setTargetMode = (dayKey, mode) => {
  staffingTargets[dayKey].mode = mode
}
</script>

<template>
  <div class="space-y-6">
    <Card padding="none">
      <CardHeader
        title="Cele obsady"
        description="Określ wymagany poziom obsady dla każdego dnia tygodnia oraz opcjonalne godziny wzmożonego ruchu. Możesz podać procent lub konkretną liczbę osób."
      />

      <div class="divide-y divide-[var(--color-border)]">
        <div
          v-for="day in WEEKDAY_OPTIONS"
          :key="day.key"
          class="grid gap-4 px-6 py-5 lg:grid-cols-[1fr_2fr_1fr_2fr] lg:items-start"
        >
          <div>
            <p class="text-sm font-semibold text-[var(--color-foreground)]">{{ day.label }}</p>
            <p class="text-xs text-[var(--color-muted)]">{{ day.shortLabel }}</p>
          </div>

          <div class="space-y-2">
            <Label>Tryb i wartość</Label>
            <div class="flex flex-wrap gap-2">
              <button
                type="button"
                class="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors"
                :class="
                  staffingTargets[day.key].mode !== STAFFING_TARGET_MODES.COUNT
                    ? 'border-[var(--color-primary)] bg-[var(--color-primary)] text-[var(--color-primary-foreground)]'
                    : 'border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-foreground-muted)] hover:bg-[var(--color-surface-muted)]'
                "
                @click="setTargetMode(day.key, STAFFING_TARGET_MODES.PERCENT)"
              >
                Procent
              </button>
              <button
                type="button"
                class="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors"
                :class="
                  staffingTargets[day.key].mode === STAFFING_TARGET_MODES.COUNT
                    ? 'border-[var(--color-primary)] bg-[var(--color-primary)] text-[var(--color-primary-foreground)]'
                    : 'border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-foreground-muted)] hover:bg-[var(--color-surface-muted)]'
                "
                @click="setTargetMode(day.key, STAFFING_TARGET_MODES.COUNT)"
              >
                Liczba osób
              </button>
            </div>

            <div v-if="staffingTargets[day.key].mode === STAFFING_TARGET_MODES.COUNT" class="max-w-xs">
              <Input
                v-model.number="staffingTargets[day.key].count"
                type="number"
                :min="0"
                :max="minSectionEmployees || undefined"
                step="1"
                :invalid="Boolean(getError(`goals.staffingTargets.${day.key}.count`))"
              />
              <p
                v-if="getError(`goals.staffingTargets.${day.key}.count`)"
                class="mt-1 text-xs font-medium text-[var(--color-danger)]"
              >
                {{ getError(`goals.staffingTargets.${day.key}.count`) }}
              </p>
              <p v-else-if="minSectionEmployees > 0" class="mt-1 text-xs text-[var(--color-muted)]">
                Max {{ minSectionEmployees }} (najmniejsza sekcja)
              </p>
            </div>
            <div v-else class="relative max-w-xs">
              <Input
                v-model.number="staffingTargets[day.key].percent"
                type="number"
                min="0"
                max="100"
                step="1"
                class="pr-10"
                :invalid="Boolean(getError(`goals.staffingTargets.${day.key}.percent`))"
              />
              <span
                class="pointer-events-none absolute inset-y-0 right-3 flex items-center text-sm text-[var(--color-muted)]"
              >
                %
              </span>
              <p
                v-if="getError(`goals.staffingTargets.${day.key}.percent`)"
                class="mt-1 text-xs font-medium text-[var(--color-danger)]"
              >
                {{ getError(`goals.staffingTargets.${day.key}.percent`) }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <Switch
              :model-value="staffingTargets[day.key].peakEnabled"
              :id="`peak-${day.key}`"
              @update:model-value="staffingTargets[day.key].peakEnabled = $event"
            />
            <Label :for="`peak-${day.key}`" class="!font-normal !text-[var(--color-foreground-muted)]">
              Peak window
            </Label>
          </div>

          <div>
            <div v-if="staffingTargets[day.key].peakEnabled" class="grid grid-cols-2 gap-3">
              <FormField
                label="Od"
                :for="`peak-start-${day.key}`"
                :error="getError(`goals.staffingTargets.${day.key}.peakStart`)"
              >
                <Input
                  :id="`peak-start-${day.key}`"
                  v-model="staffingTargets[day.key].peakStart"
                  type="time"
                  :invalid="Boolean(getError(`goals.staffingTargets.${day.key}.peakStart`))"
                />
              </FormField>
              <FormField
                label="Do"
                :for="`peak-end-${day.key}`"
                :error="getError(`goals.staffingTargets.${day.key}.peakEnd`)"
              >
                <Input
                  :id="`peak-end-${day.key}`"
                  v-model="staffingTargets[day.key].peakEnd"
                  type="time"
                  :invalid="Boolean(getError(`goals.staffingTargets.${day.key}.peakEnd`))"
                />
              </FormField>
            </div>
            <p v-else class="text-xs text-[var(--color-muted)]">Brak okna peak dla tego dnia.</p>
          </div>
        </div>
      </div>
    </Card>

    <Alert variant="info" title="Jak to działa?">
      <ul class="list-disc space-y-1 pl-5">
        <li>
          <strong>Procent</strong> — wymagana obsada liczona od liczby pracowników w sekcji.
        </li>
        <li>
          <strong>Liczba osób</strong> — stała liczba pracowników, niezależnie od wielkości sekcji
          (ograniczona do najmniejszej sekcji).
        </li>
        <li>
          <strong>Peak window</strong> wskazuje godziny wzmożonego ruchu priorytetowe dla algorytmu.
        </li>
      </ul>
    </Alert>
  </div>
</template>
