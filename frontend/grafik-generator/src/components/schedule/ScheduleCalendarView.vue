<script setup>
import { computed, ref } from 'vue'
import { ArrowRight, ShieldAlert, X } from 'lucide-vue-next'
import { cn } from '../../lib/utils'
import CoverageLegend from './CoverageLegend.vue'

const props = defineProps({
  scheduleDetails: {
    type: Object,
    required: true,
  },
  diff: {
    type: Array,
    default: () => [],
  },
})

const yearMonth = computed(() => props.scheduleDetails?.yearMonth || '')

const monthInfo = computed(() => {
  if (!yearMonth.value) return null
  const [year, monthRaw] = yearMonth.value.split('-').map(Number)
  if (!year || !monthRaw) return null
  return { year, month: monthRaw }
})

const monthCells = computed(() => {
  const info = monthInfo.value
  if (!info) return []
  const { year, month } = info
  const daysInMonth = new Date(year, month, 0).getDate()
  const firstWeekday = new Date(year, month - 1, 1).getDay()
  const mondayBasedOffset = (firstWeekday + 6) % 7
  const cells = []
  for (let i = 0; i < mondayBasedOffset; i += 1) cells.push({ empty: true })
  for (let day = 1; day <= daysInMonth; day += 1) cells.push({ empty: false, day })
  return cells
})

const coverageByDay = computed(() => {
  const coverage = props.scheduleDetails?.coverage || []
  return coverage.reduce((acc, day) => {
    acc[day.day] = day
    return acc
  }, {})
})

const diffByDay = computed(() => {
  return (props.diff || []).reduce((acc, item) => {
    if (!acc[item.day]) acc[item.day] = []
    acc[item.day].push(item)
    return acc
  }, {})
})

const getDayDiff = (day) => diffByDay.value[day] || []

const getDayTitle = (day) => {
  const changes = getDayDiff(day)
  if (changes.length === 0) return ''
  return changes.map((item) => `${item.employeeName}: ${item.before || '-'} → ${item.after || '-'}`).join('\n')
}

const normalizeShift = (value) => {
  const text = String(value ?? '').trim()
  return text || '-'
}

const shiftsByDay = computed(() => {
  const employees = props.scheduleDetails?.employees || []
  const result = {}
  const dayCount = (props.scheduleDetails?.coverage || []).length
  for (let day = 1; day <= dayCount; day += 1) {
    result[day] = []
  }
  employees.forEach((employee) => {
    ;(employee.shifts || []).forEach((shift, index) => {
      const day = index + 1
      if (!result[day]) return
      if (!shift || shift === 'W' || shift === '-') return
      result[day].push({
        name: `${employee.name} ${employee.surname}`,
        shift,
      })
    })
  })
  return result
})

const getCellTone = (day) => {
  const t = coverageByDay.value[day]
  if (!t) return 'border-[var(--color-border)] bg-[var(--color-surface)]'
  if (t.closedDay)
    return 'border-[var(--color-border)] bg-[var(--color-surface-muted)] text-[var(--color-muted)]'
  const ratio = t.required > 0 ? t.actual / t.required : 1
  if (ratio < 1)
    return 'border-[var(--color-danger)]/30 bg-[var(--color-danger-soft)]/50'
  if (ratio > 1)
    return 'border-[var(--color-warning)]/30 bg-[var(--color-warning-soft)]/50'
  return 'border-[var(--color-success)]/30 bg-[var(--color-success-soft)]/50'
}

const getCoverageBar = (day) => {
  const t = coverageByDay.value[day]
  if (!t) return { color: 'bg-[var(--color-muted)]', percent: 0 }
  if (t.closedDay) return { color: 'bg-[var(--color-muted)]', percent: 100 }
  const ratio = t.required > 0 ? t.actual / t.required : 1
  const percent = Math.min(100, Math.max(5, Math.round(ratio * 100)))
  if (ratio < 1) return { color: 'bg-[var(--color-danger)]', percent }
  if (ratio > 1) return { color: 'bg-[var(--color-warning)]', percent }
  return { color: 'bg-[var(--color-success)]', percent }
}

const selectedDay = ref(null)

const selectDay = (day) => {
  const t = coverageByDay.value[day]
  if (!t || (t.closedDay && getDayDiff(day).length === 0)) {
    selectedDay.value = null
    return
  }
  selectedDay.value = day
}

const selectedShifts = computed(() => {
  if (!selectedDay.value) return []
  return shiftsByDay.value[selectedDay.value] || []
})

const selectedCoverage = computed(() => {
  if (!selectedDay.value) return null
  return coverageByDay.value[selectedDay.value]
})

const selectedChanges = computed(() => {
  if (!selectedDay.value) return []
  return getDayDiff(selectedDay.value)
})
</script>

<template>
  <div class="space-y-4">
    <div
      class="grid grid-cols-7 gap-1 text-center text-xs font-semibold uppercase tracking-wide text-[var(--color-muted)]"
    >
      <span>Pn</span><span>Wt</span><span>Śr</span><span>Czw</span><span>Pt</span><span>Sob</span><span>Nd</span>
    </div>

    <div class="grid grid-cols-7 gap-1.5">
      <button
        v-for="(cell, idx) in monthCells"
        :key="cell.day || `e${idx}`"
        type="button"
        :disabled="cell.empty"
        :class="cn(
          'flex flex-col items-stretch justify-between rounded-md border p-2 text-left transition-colors min-h-[80px]',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-ring)]',
          cell.empty
            ? 'invisible'
            : cn(
                getCellTone(cell.day),
                'hover:ring-2 hover:ring-[var(--color-primary)]/30 cursor-pointer',
                selectedDay === cell.day && 'ring-2 ring-[var(--color-primary)]',
                getDayDiff(cell.day).length > 0 &&
                  'ring-2 ring-[var(--color-info)] ring-offset-1 ring-offset-[var(--color-surface)]'
              )
        )"
        :title="!cell.empty ? getDayTitle(cell.day) : ''"
        @click="!cell.empty && selectDay(cell.day)"
      >
        <template v-if="!cell.empty">
          <div class="flex items-start justify-between gap-1">
            <span class="text-sm font-semibold text-[var(--color-foreground)]">{{ cell.day }}</span>
            <span
              v-if="coverageByDay[cell.day] && !coverageByDay[cell.day].closedDay"
              class="text-[10px] font-medium text-[var(--color-foreground-muted)]"
            >
              {{ coverageByDay[cell.day].actual }}/{{ coverageByDay[cell.day].required }}
            </span>
            <span
              v-if="getDayDiff(cell.day).length > 0"
              class="rounded-full bg-[var(--color-info)] px-1.5 py-0.5 text-[9px] font-semibold text-white"
            >
              {{ getDayDiff(cell.day).length }} zm.
            </span>
          </div>

          <div v-if="coverageByDay[cell.day]?.closedDay" class="mt-1 text-[10px] uppercase tracking-wide text-[var(--color-muted)]">
            Zamknięte
          </div>
          <div v-else class="mt-1 space-y-1">
            <div class="h-1.5 w-full rounded-full bg-[var(--color-border)]/50">
              <div
                :class="['h-full rounded-full', getCoverageBar(cell.day).color]"
                :style="{ width: `${getCoverageBar(cell.day).percent}%` }"
              />
            </div>
            <p class="text-[10px] font-medium text-[var(--color-foreground)]">
              {{ coverageByDay[cell.day]?.percentage ?? '—' }}%
            </p>
          </div>
        </template>
      </button>
    </div>

    <CoverageLegend class="pt-2" />

    <Teleport to="body">
      <div
        v-if="selectedDay"
        class="fixed inset-0 z-50 flex items-end justify-end bg-black/30 backdrop-blur-sm sm:items-center sm:justify-center"
        @click.self="selectedDay = null"
      >
        <div
          class="w-full max-w-md rounded-t-lg border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-xl sm:rounded-lg"
        >
          <div class="flex items-start justify-between gap-3">
            <div>
              <h3 class="text-lg font-semibold text-[var(--color-foreground)]">
                Dzień {{ selectedDay }}
              </h3>
              <p v-if="selectedCoverage" class="mt-1 text-sm text-[var(--color-foreground-muted)]">
                Obsada: {{ selectedCoverage.actual }} / {{ selectedCoverage.required }}
                ({{ selectedCoverage.percentage }}%)
              </p>
            </div>
            <button
              type="button"
              class="rounded-md p-1.5 text-[var(--color-muted)] hover:bg-[var(--color-surface-muted)]"
              aria-label="Zamknij"
              @click="selectedDay = null"
            >
              <X class="h-4 w-4" />
            </button>
          </div>

          <div
            v-if="selectedChanges.length > 0"
            class="mt-4 rounded-md border border-[var(--color-info)]/30 bg-[var(--color-info-soft)]/35 p-3"
          >
            <div class="flex items-center justify-between gap-2">
              <h4 class="text-sm font-semibold text-[var(--color-foreground)]">
                Zmiany Gemini w tym dniu
              </h4>
              <span class="text-xs font-medium text-[var(--color-info-foreground)]">
                {{ selectedChanges.length }}
              </span>
            </div>
            <div class="mt-3 space-y-2">
              <div
                v-for="change in selectedChanges"
                :key="`${change.employeeId}-${change.day}-${change.before}-${change.after}`"
                class="rounded-md bg-[var(--color-surface)] px-3 py-2"
              >
                <div class="flex flex-wrap items-center justify-between gap-2">
                  <span class="text-sm font-medium text-[var(--color-foreground)]">
                    {{ change.employeeName }}
                  </span>
                  <span
                    v-if="change.protectedDate"
                    class="inline-flex items-center gap-1 rounded-full bg-[var(--color-warning-soft)] px-2 py-0.5 text-[10px] font-semibold text-[var(--color-warning-foreground)]"
                  >
                    <ShieldAlert class="h-3 w-3" aria-hidden="true" />
                    chronione
                  </span>
                </div>
                <div class="mt-2 flex flex-wrap items-center gap-2">
                  <span class="rounded-md bg-[var(--color-danger-soft)] px-2 py-1 text-xs font-semibold text-[var(--color-danger-foreground)] line-through decoration-2">
                    {{ normalizeShift(change.before) }}
                  </span>
                  <ArrowRight class="h-4 w-4 text-[var(--color-muted)]" aria-hidden="true" />
                  <span class="rounded-md bg-[var(--color-success-soft)] px-2 py-1 text-xs font-semibold text-[var(--color-success-foreground)]">
                    {{ normalizeShift(change.after) }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedShifts.length > 0" class="mt-4 max-h-80 space-y-2 overflow-auto">
            <div
              v-for="entry in selectedShifts"
              :key="`${entry.name}-${entry.shift}`"
              class="flex items-center justify-between gap-3 rounded-md border border-[var(--color-border)] px-3 py-2"
            >
              <span class="text-sm font-medium text-[var(--color-foreground)]">{{ entry.name }}</span>
              <span
                class="rounded-md bg-[var(--color-primary-soft)] px-2 py-1 text-xs font-medium text-[var(--color-primary)]"
              >
                {{ entry.shift }}
              </span>
            </div>
          </div>
          <p v-else class="mt-4 text-sm text-[var(--color-muted)]">
            Brak przypisanych zmian dla tego dnia.
          </p>
        </div>
      </div>
    </Teleport>
  </div>
</template>
