<script setup>
import { computed } from 'vue'
import { CheckCircle2, AlertCircle, AlertTriangle, ArrowRight } from 'lucide-vue-next'
import {
  OPTIMIZATION_PRESETS,
  WEEKDAY_OPTIONS,
  summarizeDraft,
  validateDraft,
} from '../../utils/creatorDraft'
import Card from '../ui/Card.vue'
import CardHeader from '../ui/CardHeader.vue'
import Badge from '../ui/Badge.vue'
import Alert from '../ui/Alert.vue'

const draft = defineModel({
  type: Object,
  required: true,
})

const emit = defineEmits(['go-to-step'])

const validation = computed(() => validateDraft(draft.value))
const summary = computed(() => summarizeDraft(draft.value))

const selectedPreset = computed(
  () =>
    OPTIMIZATION_PRESETS.find((p) => p.key === draft.value.weights.preset) ||
    OPTIMIZATION_PRESETS[0],
)

const openDays = computed(() =>
  WEEKDAY_OPTIONS.filter((d) => draft.value.calendar.storeHours[d.key]?.enabled),
)

const peakDays = computed(() =>
  WEEKDAY_OPTIONS.filter((d) => draft.value.goals.staffingTargets[d.key]?.peakEnabled),
)

const sectionsWithEmployees = computed(() =>
  (draft.value.sections || []).map((section, sectionIndex) => ({
    ...section,
    displayName: section.name?.trim() || `Sekcja ${sectionIndex + 1}`,
    employees: Array.isArray(section.employees) ? section.employees : [],
  })),
)

const errorsByStep = computed(() => validation.value.errorsByStep || {})

const stepDescriptors = [
  { id: 1, label: 'Kalendarz' },
  { id: 2, label: 'Pracownicy' },
  { id: 3, label: 'Cele obsady' },
  { id: 4, label: 'Reguły' },
  { id: 5, label: 'Priorytet' },
  { id: 6, label: 'Parametry GA' },
]

const stepsWithIssues = computed(() =>
  stepDescriptors
    .map((s) => ({
      ...s,
      errors: errorsByStep.value[s.id] || [],
    }))
    .filter((s) => s.errors.length > 0),
)

const formatShiftLengths = (lengths) =>
  lengths.length > 0 ? lengths.map((v) => `${v}h`).join(', ') : 'Brak'

const formatEmployeeName = (e) => `${e.name || '—'} ${e.surname || ''}`.trim()

const formatStoreHours = (dayKey) => {
  const h = draft.value.calendar.storeHours[dayKey]
  if (!h?.enabled) return 'Zamknięte'
  return `${h.open}–${h.close}`
}

const formatPeakWindow = (dayKey) => {
  const t = draft.value.goals.staffingTargets[dayKey]
  if (!t?.peakEnabled) return 'Brak'
  return `${t.peakStart}–${t.peakEnd}`
}

const formatStaffingTarget = (dayKey) => {
  const t = draft.value.goals.staffingTargets[dayKey]
  if (t?.mode === 'COUNT') return `${t.count} os.`
  return `${t.percent}%`
}
</script>

<template>
  <div class="space-y-6">
    <Alert
      :variant="validation.isValid ? 'success' : 'danger'"
      :title="
        validation.isValid
          ? 'Konfiguracja wygląda poprawnie'
          : 'Konfiguracja wymaga poprawek'
      "
    >
      <p>
        {{
          validation.isValid
            ? 'Możesz uruchomić generowanie grafiku. Warto jeszcze przejrzeć ostrzeżenia.'
            : 'Przed uruchomieniem generowania popraw błędy wskazane poniżej.'
        }}
      </p>
      <div class="mt-3 flex flex-wrap gap-2">
        <Badge :variant="validation.errors.length === 0 ? 'success' : 'danger'">
          <AlertCircle v-if="validation.errors.length > 0" class="h-3 w-3" aria-hidden="true" />
          <CheckCircle2 v-else class="h-3 w-3" aria-hidden="true" />
          Błędy: {{ validation.errors.length }}
        </Badge>
        <Badge variant="warning">
          <AlertTriangle class="h-3 w-3" aria-hidden="true" />
          Ostrzeżenia: {{ validation.warnings.length }}
        </Badge>
      </div>
    </Alert>

    <Card v-if="stepsWithIssues.length > 0" padding="none">
      <CardHeader title="Błędy do poprawy" description="Kliknij krok, aby tam wrócić." />
      <div class="divide-y divide-[var(--color-border)]">
        <button
          v-for="step in stepsWithIssues"
          :key="step.id"
          type="button"
          class="flex w-full items-start justify-between gap-3 px-6 py-4 text-left transition-colors hover:bg-[var(--color-surface-muted)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-ring)]"
          @click="emit('go-to-step', step.id - 1)"
        >
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <Badge variant="danger">Krok {{ step.id }}</Badge>
              <p class="text-sm font-semibold text-[var(--color-foreground)]">{{ step.label }}</p>
            </div>
            <ul class="mt-2 space-y-1">
              <li
                v-for="err in step.errors"
                :key="err"
                class="text-sm text-[var(--color-foreground-muted)]"
              >
                · {{ err }}
              </li>
            </ul>
          </div>
          <ArrowRight class="h-4 w-4 shrink-0 text-[var(--color-muted)]" aria-hidden="true" />
        </button>
      </div>
    </Card>

    <Card v-if="validation.warnings.length > 0" padding="none">
      <CardHeader title="Ostrzeżenia" />
      <ul class="divide-y divide-[var(--color-border)]">
        <li
          v-for="w in validation.warnings"
          :key="w"
          class="px-6 py-3 text-sm text-[var(--color-foreground-muted)]"
        >
          · {{ w }}
        </li>
      </ul>
    </Card>

    <div class="grid gap-6 xl:grid-cols-2">
      <Card padding="none">
        <CardHeader title="Informacje podstawowe" />
        <dl class="grid gap-4 p-6 sm:grid-cols-2">
          <div class="rounded-md bg-[var(--color-surface-muted)] p-3">
            <dt class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">Nazwa</dt>
            <dd class="mt-1 text-sm font-semibold text-[var(--color-foreground)]">
              {{ summary.configName || '—' }}
            </dd>
          </div>
          <div class="rounded-md bg-[var(--color-surface-muted)] p-3">
            <dt class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">Miesiąc</dt>
            <dd class="mt-1 text-sm font-semibold text-[var(--color-foreground)]">
              {{ summary.selectedMonth || '—' }}
            </dd>
          </div>
          <div class="rounded-md bg-[var(--color-surface-muted)] p-3">
            <dt class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">Sekcje</dt>
            <dd class="mt-1 text-sm font-semibold text-[var(--color-foreground)]">
              {{ summary.sectionsCount }}
            </dd>
          </div>
          <div class="rounded-md bg-[var(--color-surface-muted)] p-3">
            <dt class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">Pracownicy</dt>
            <dd class="mt-1 text-sm font-semibold text-[var(--color-foreground)]">
              {{ summary.employeesCount }}
            </dd>
          </div>
          <div class="rounded-md bg-[var(--color-surface-muted)] p-3">
            <dt class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">Święta</dt>
            <dd class="mt-1 text-sm font-semibold text-[var(--color-foreground)]">
              {{ summary.holidaysCount }}
            </dd>
          </div>
          <div class="rounded-md bg-[var(--color-surface-muted)] p-3">
            <dt class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">
              Niedziele handlowe
            </dt>
            <dd class="mt-1 text-sm font-semibold text-[var(--color-foreground)]">
              {{ summary.tradingSundaysCount }}
            </dd>
          </div>
        </dl>
      </Card>

      <Card padding="none">
        <CardHeader title="Reguły i parametry" />
        <div class="space-y-2 p-6">
          <div
            v-for="row in [
              { label: 'Długości zmian', value: formatShiftLengths(summary.shiftLengths) },
              { label: 'Preset optymalizacji', value: selectedPreset.label },
              { label: 'Maks. dni pracy z rzędu', value: draft.rules.maxWorkingDaysInARow },
              { label: 'Limit osób na ten sam start', value: draft.rules.maxPeoplePerShiftStart },
              { label: 'Wolny weekend', value: draft.rules.grantFreeWeekend ? 'Tak' : 'Nie' },
              { label: 'Population / Generations', value: `${draft.params.populationSize} / ${draft.params.generations}` },
              { label: 'Mutation / Seed', value: `${draft.params.mutationRate} / ${draft.params.seed || 'losowy'}` },
            ]"
            :key="row.label"
            class="flex items-start justify-between gap-4 rounded-md bg-[var(--color-surface-muted)] px-4 py-2.5"
          >
            <dt class="text-sm text-[var(--color-foreground-muted)]">{{ row.label }}</dt>
            <dd class="text-sm font-medium text-[var(--color-foreground)]">{{ row.value }}</dd>
          </div>
        </div>
      </Card>
    </div>

    <div class="grid gap-6 xl:grid-cols-2">
      <Card padding="none">
        <CardHeader
          title="Godziny otwarcia"
          :description="`Aktywne dni: ${openDays.length}`"
        />
        <div class="grid gap-3 p-6 sm:grid-cols-2">
          <div
            v-for="day in WEEKDAY_OPTIONS"
            :key="day.key"
            class="rounded-md border border-[var(--color-border)] px-3 py-2"
          >
            <p class="text-sm font-medium text-[var(--color-foreground)]">{{ day.label }}</p>
            <p class="mt-1 text-sm text-[var(--color-foreground-muted)]">{{ formatStoreHours(day.key) }}</p>
          </div>
        </div>
      </Card>

      <Card padding="none">
        <CardHeader
          title="Cele obsady i peak hours"
          :description="`Dni z peak window: ${peakDays.length}`"
        />
        <div class="space-y-2 p-6">
          <div
            v-for="day in WEEKDAY_OPTIONS"
            :key="day.key"
            class="flex items-center justify-between gap-3 rounded-md border border-[var(--color-border)] px-3 py-2"
          >
            <div>
              <p class="text-sm font-medium text-[var(--color-foreground)]">{{ day.label }}</p>
              <p class="text-xs text-[var(--color-muted)]">Peak: {{ formatPeakWindow(day.key) }}</p>
            </div>
            <p class="text-sm font-semibold text-[var(--color-foreground)]">
              {{ formatStaffingTarget(day.key) }}
            </p>
          </div>
        </div>
      </Card>
    </div>

    <Card padding="none">
      <CardHeader title="Lista pracowników" />
      <div class="space-y-6 p-6">
        <div v-for="section in sectionsWithEmployees" :key="section.id || section.displayName">
          <div
            class="mb-3 flex items-center justify-between gap-3 border-b border-[var(--color-border)] pb-2"
          >
            <h4 class="text-sm font-semibold text-[var(--color-foreground)]">
              {{ section.displayName }}
            </h4>
            <Badge variant="neutral">{{ section.employees.length }} prac.</Badge>
          </div>
          <div v-if="section.employees.length > 0" class="grid gap-2 md:grid-cols-2">
            <div
              v-for="(employee, index) in section.employees"
              :key="`${section.id}-${index}`"
              class="rounded-md border border-[var(--color-border)] px-3 py-2"
            >
              <p class="text-sm font-medium text-[var(--color-foreground)]">
                {{ formatEmployeeName(employee) }}
              </p>
              <p class="text-xs text-[var(--color-muted)]">
                {{ employee.totalHours }} h / {{ employee.totalDays }} dni
              </p>
            </div>
          </div>
          <p v-else class="text-sm text-[var(--color-muted)]">Brak pracowników w tej sekcji.</p>
        </div>
      </div>
    </Card>
  </div>
</template>
