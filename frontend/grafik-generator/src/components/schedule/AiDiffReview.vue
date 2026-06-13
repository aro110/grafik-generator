<script setup>
import { computed, ref } from 'vue'
import { ArrowRight, Search, ShieldAlert } from 'lucide-vue-next'
import { cn } from '../../lib/utils'
import Badge from '../ui/Badge.vue'

const props = defineProps({
  diff: {
    type: Array,
    default: () => [],
  },
  title: {
    type: String,
    default: 'Przegląd zmian',
  },
  description: {
    type: String,
    default: 'Każda pozycja pokazuje starą wartość i propozycję wygenerowaną przez Gemini.',
  },
})

const query = ref('')
const activeFilter = ref('all')

const freeValues = new Set(['', '-', 'W', 'WOLNE', 'FREE', 'OFF'])

const normalizeShift = (value) => {
  const text = String(value ?? '').trim()
  return text || '-'
}

const isFreeShift = (value) => freeValues.has(normalizeShift(value).toUpperCase())

const getChangeType = (item) => {
  const beforeFree = isFreeShift(item.before)
  const afterFree = isFreeShift(item.after)
  if (beforeFree && !afterFree) return 'added'
  if (!beforeFree && afterFree) return 'removed'
  return 'changed'
}

const changeLabels = {
  added: 'Dodana zmiana',
  removed: 'Usunięta zmiana',
  changed: 'Zmienione godziny',
}

const filterOptions = computed(() => [
  { value: 'all', label: 'Wszystkie', count: props.diff.length },
  {
    value: 'added',
    label: 'Dodane',
    count: props.diff.filter((item) => getChangeType(item) === 'added').length,
  },
  {
    value: 'removed',
    label: 'Usunięte',
    count: props.diff.filter((item) => getChangeType(item) === 'removed').length,
  },
  {
    value: 'changed',
    label: 'Zmienione',
    count: props.diff.filter((item) => getChangeType(item) === 'changed').length,
  },
  {
    value: 'protected',
    label: 'Chronione',
    count: props.diff.filter((item) => item.protectedDate).length,
  },
])

const normalizedDiff = computed(() =>
  props.diff.map((item, index) => ({
    ...item,
    id: `${item.employeeId}-${item.day}-${index}`,
    beforeLabel: normalizeShift(item.before),
    afterLabel: normalizeShift(item.after),
    type: getChangeType(item),
  })),
)

const summary = computed(() => {
  const employees = new Set(normalizedDiff.value.map((item) => item.employeeName || item.employeeId))
  const days = new Set(normalizedDiff.value.map((item) => item.day))
  return {
    changes: normalizedDiff.value.length,
    employees: employees.size,
    days: days.size,
    protected: normalizedDiff.value.filter((item) => item.protectedDate).length,
  }
})

const filteredDiff = computed(() => {
  const text = query.value.trim().toLowerCase()
  return normalizedDiff.value.filter((item) => {
    const matchesFilter =
      activeFilter.value === 'all' ||
      item.type === activeFilter.value ||
      (activeFilter.value === 'protected' && item.protectedDate)

    if (!matchesFilter) return false
    if (!text) return true

    return [
      item.employeeName,
      String(item.day),
      item.beforeLabel,
      item.afterLabel,
      item.reason,
      changeLabels[item.type],
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(text))
  })
})

const getTypeBadgeVariant = (type) => {
  if (type === 'added') return 'success'
  if (type === 'removed') return 'danger'
  return 'info'
}
</script>

<template>
  <section class="border-b border-[var(--color-border)] bg-[var(--color-surface)]">
    <div class="space-y-4 px-4 py-4">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div class="min-w-0">
          <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-primary)]">
            Porównanie Gemini
          </p>
          <h3 class="mt-1 text-base font-semibold text-[var(--color-foreground)]">
            {{ title }}
          </h3>
          <p class="mt-1 max-w-3xl text-sm text-[var(--color-foreground-muted)]">
            {{ description }}
          </p>
        </div>

        <div class="grid grid-cols-2 gap-2 text-sm sm:grid-cols-4 lg:min-w-[420px]">
          <div class="rounded-md border border-[var(--color-border)] px-3 py-2">
            <p class="text-[10px] font-medium uppercase tracking-wide text-[var(--color-muted)]">
              Zmiany
            </p>
            <p class="mt-1 text-lg font-semibold text-[var(--color-foreground)]">
              {{ summary.changes }}
            </p>
          </div>
          <div class="rounded-md border border-[var(--color-border)] px-3 py-2">
            <p class="text-[10px] font-medium uppercase tracking-wide text-[var(--color-muted)]">
              Osoby
            </p>
            <p class="mt-1 text-lg font-semibold text-[var(--color-foreground)]">
              {{ summary.employees }}
            </p>
          </div>
          <div class="rounded-md border border-[var(--color-border)] px-3 py-2">
            <p class="text-[10px] font-medium uppercase tracking-wide text-[var(--color-muted)]">
              Dni
            </p>
            <p class="mt-1 text-lg font-semibold text-[var(--color-foreground)]">
              {{ summary.days }}
            </p>
          </div>
          <div class="rounded-md border border-[var(--color-border)] px-3 py-2">
            <p class="text-[10px] font-medium uppercase tracking-wide text-[var(--color-muted)]">
              Chronione
            </p>
            <p class="mt-1 text-lg font-semibold text-[var(--color-warning-foreground)]">
              {{ summary.protected }}
            </p>
          </div>
        </div>
      </div>

      <div class="grid gap-3 lg:grid-cols-[minmax(220px,320px)_1fr] lg:items-center">
        <label class="relative block">
          <Search
            class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--color-muted)]"
            aria-hidden="true"
          />
          <input
            v-model="query"
            type="search"
            class="h-10 w-full rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] pl-9 pr-3 text-sm text-[var(--color-foreground)] outline-none placeholder:text-[var(--color-muted)] focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
            placeholder="Szukaj osoby, dnia lub zmiany"
          />
        </label>

        <div class="flex flex-wrap gap-2">
          <button
            v-for="option in filterOptions"
            :key="option.value"
            type="button"
            :class="cn(
              'inline-flex h-9 items-center gap-2 rounded-md border px-3 text-xs font-medium transition-colors',
              activeFilter === option.value
                ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)]'
                : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-foreground-muted)] hover:bg-[var(--color-surface-muted)]'
            )"
            @click="activeFilter = option.value"
          >
            {{ option.label }}
            <span class="rounded-full bg-[var(--color-surface-muted)] px-1.5 py-0.5 text-[10px]">
              {{ option.count }}
            </span>
          </button>
        </div>
      </div>

      <div class="flex flex-wrap gap-2 text-xs text-[var(--color-foreground-muted)]">
        <span class="inline-flex items-center gap-1 rounded-md bg-[var(--color-danger-soft)] px-2 py-1 text-[var(--color-danger-foreground)]">
          Było
        </span>
        <span class="inline-flex items-center gap-1 rounded-md bg-[var(--color-success-soft)] px-2 py-1 text-[var(--color-success-foreground)]">
          Będzie
        </span>
        <span class="inline-flex items-center gap-1 rounded-md bg-[var(--color-warning-soft)] px-2 py-1 text-[var(--color-warning-foreground)]">
          <ShieldAlert class="h-3.5 w-3.5" aria-hidden="true" />
          Dzień chroniony
        </span>
      </div>
    </div>

    <div v-if="filteredDiff.length" class="overflow-x-auto border-t border-[var(--color-border)]">
      <table class="min-w-full text-left text-sm">
        <thead class="bg-[var(--color-surface-muted)] text-xs uppercase text-[var(--color-foreground-muted)]">
          <tr>
            <th scope="col" class="px-4 py-3 font-semibold">Pracownik</th>
            <th scope="col" class="w-20 px-4 py-3 text-center font-semibold">Dzień</th>
            <th scope="col" class="px-4 py-3 font-semibold">Zmiana</th>
            <th scope="col" class="px-4 py-3 font-semibold">Typ</th>
            <th scope="col" class="px-4 py-3 font-semibold">Uwaga</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[var(--color-border)]">
          <tr
            v-for="item in filteredDiff"
            :key="item.id"
            class="transition-colors hover:bg-[var(--color-surface-muted)]/40"
          >
            <td class="px-4 py-3 font-medium text-[var(--color-foreground)]">
              {{ item.employeeName || `Pracownik ${item.employeeId}` }}
            </td>
            <td class="px-4 py-3 text-center text-[var(--color-foreground-muted)]">
              {{ item.day }}
            </td>
            <td class="px-4 py-3">
              <div class="flex min-w-[220px] flex-wrap items-center gap-2">
                <span
                  class="rounded-md border border-[var(--color-danger)]/30 bg-[var(--color-danger-soft)] px-2 py-1 text-xs font-semibold text-[var(--color-danger-foreground)] line-through decoration-2"
                >
                  {{ item.beforeLabel }}
                </span>
                <ArrowRight class="h-4 w-4 text-[var(--color-muted)]" aria-hidden="true" />
                <span
                  class="rounded-md border border-[var(--color-success)]/30 bg-[var(--color-success-soft)] px-2 py-1 text-xs font-semibold text-[var(--color-success-foreground)]"
                >
                  {{ item.afterLabel }}
                </span>
              </div>
            </td>
            <td class="px-4 py-3">
              <Badge :variant="getTypeBadgeVariant(item.type)">
                {{ changeLabels[item.type] }}
              </Badge>
            </td>
            <td class="px-4 py-3">
              <div class="flex flex-wrap gap-1.5">
                <Badge v-if="item.protectedDate" variant="warning">
                  <ShieldAlert class="h-3 w-3" aria-hidden="true" />
                  chronione
                </Badge>
                <span class="text-xs text-[var(--color-foreground-muted)]">
                  {{ item.reason || 'Zmiana zaproponowana przez Gemini' }}
                </span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="border-t border-[var(--color-border)] px-4 py-6 text-sm text-[var(--color-muted)]">
      Nie ma zmian pasujących do wybranych filtrów.
    </div>
  </section>
</template>
