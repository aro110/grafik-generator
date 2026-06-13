<script setup>
import { computed } from 'vue'
import { cn } from '../../lib/utils'
import CoverageLegend from './CoverageLegend.vue'

const props = defineProps({
  scheduleDetails: {
    type: Object,
    required: true,
  },
  showLegend: {
    type: Boolean,
    default: true,
  },
  diff: {
    type: Array,
    default: () => [],
  },
  diffMode: {
    type: String,
    default: 'inline',
    validator: (value) => ['inline', 'before', 'after'].includes(value),
  },
})

const daysInMonth = computed(() => (props.scheduleDetails?.coverage || []).map((d) => d.day))
const employees = computed(() => props.scheduleDetails?.employees || [])
const coverageByDay = computed(() => {
  const coverage = props.scheduleDetails?.coverage || []
  return coverage.reduce((acc, day) => {
    acc[day.day] = day
    return acc
  }, {})
})

const diffByCell = computed(() => {
  return (props.diff || []).reduce((acc, item) => {
    acc[`${item.employeeId}-${item.day}`] = item
    return acc
  }, {})
})

const getDiff = (employeeId, day) => diffByCell.value[`${employeeId}-${day}`]

const freeValues = new Set(['', '-', 'W', 'WOLNE', 'FREE', 'OFF'])

const normalizeShift = (value) => {
  const text = String(value ?? '').trim()
  return text || '-'
}

const isFreeShift = (value) => freeValues.has(normalizeShift(value).toUpperCase())

const getDiffType = (diff) => {
  if (!diff) return null
  const beforeFree = isFreeShift(diff.before)
  const afterFree = isFreeShift(diff.after)
  if (beforeFree && !afterFree) return 'added'
  if (!beforeFree && afterFree) return 'removed'
  return 'changed'
}

const getDiffCellClass = (diff) => {
  if (props.diffMode === 'before')
    return 'border border-[var(--color-danger)]/35 bg-[var(--color-danger-soft)]/45 ring-2 ring-[var(--color-danger)]/30 ring-offset-1 ring-offset-[var(--color-surface)]'
  if (props.diffMode === 'after')
    return 'border border-[var(--color-success)]/35 bg-[var(--color-success-soft)]/45 ring-2 ring-[var(--color-success)]/30 ring-offset-1 ring-offset-[var(--color-surface)]'

  const type = getDiffType(diff)
  if (type === 'added')
    return 'border border-[var(--color-success)]/40 bg-[var(--color-success-soft)]/45 ring-2 ring-[var(--color-success)]/35 ring-offset-1 ring-offset-[var(--color-surface)]'
  if (type === 'removed')
    return 'border border-[var(--color-danger)]/40 bg-[var(--color-danger-soft)]/45 ring-2 ring-[var(--color-danger)]/35 ring-offset-1 ring-offset-[var(--color-surface)]'
  return 'border border-[var(--color-info)]/40 bg-[var(--color-info-soft)]/45 ring-2 ring-[var(--color-info)]/35 ring-offset-1 ring-offset-[var(--color-surface)]'
}

const getCoverageClass = (day) => {
  const target = coverageByDay.value[day]
  if (!target) return 'bg-[var(--color-surface-muted)] text-[var(--color-muted)]'
  if (target.closedDay)
    return 'bg-[var(--color-surface-muted)] text-[var(--color-muted)] font-medium'
  const ratio = target.required > 0 ? target.actual / target.required : 1
  if (ratio < 1)
    return 'bg-[var(--color-danger-soft)] text-[var(--color-danger-foreground)] font-semibold'
  if (ratio > 1)
    return 'bg-[var(--color-warning-soft)] text-[var(--color-warning-foreground)] font-semibold'
  return 'bg-[var(--color-success-soft)] text-[var(--color-success-foreground)] font-semibold'
}

const getShiftClass = (shift) => {
  if (!shift) return 'text-[var(--color-muted)]'
  if (shift === 'W')
    return 'bg-[var(--color-surface-muted)] text-[var(--color-foreground-muted)] font-medium'
  if (shift.includes('08:00'))
    return 'bg-[var(--color-info-soft)] text-[var(--color-info-foreground)]'
  if (shift.includes('12:00')) return 'bg-[#f5d0fe]/40 text-[#86198f] dark:bg-[#86198f]/30 dark:text-[#f5d0fe]'
  return 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]'
}

const getCoverageLabel = (day) => {
  const t = coverageByDay.value[day]
  if (!t) return '-'
  if (t.closedDay) return 'Z'
  if (typeof t.percentage === 'number') return `${t.percentage}%`
  return '-'
}

const getCoverageTooltip = (day) => {
  const t = coverageByDay.value[day]
  if (!t) return ''
  if (t.closedDay) return 'Dzień zamknięty'
  return `Obsada: ${t.actual} / ${t.required} (${t.percentage ?? '?'}%)`
}

const getShiftTitle = (shift, employeeId, day) => {
  const diff = getDiff(employeeId, day)
  if (!diff) return shift || 'Brak'
  return `${diff.before || '-'} → ${diff.after || '-'}${diff.protectedDate ? ' · dzień chroniony' : ''}`
}

const getDiffModeLabel = () => (props.diffMode === 'before' ? 'było' : 'będzie')

const getDiffModeValue = (diff) =>
  props.diffMode === 'before' ? normalizeShift(diff.before) : normalizeShift(diff.after)

const getDiffModeValueClass = () =>
  props.diffMode === 'before'
    ? 'bg-[var(--color-danger-soft)] text-[var(--color-danger-foreground)] line-through decoration-2'
    : 'bg-[var(--color-success-soft)] text-[var(--color-success-foreground)]'
</script>

<template>
  <div class="overflow-hidden">
    <div class="overflow-x-auto">
      <table class="min-w-full border-collapse text-sm">
        <thead class="bg-[var(--color-surface-muted)]">
          <tr>
            <th
              scope="col"
              class="sticky left-0 z-10 min-w-[180px] border-r border-[var(--color-border)] bg-[var(--color-surface-muted)] px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-[var(--color-foreground-muted)]"
            >
              Pracownik
            </th>
            <th
              v-for="day in daysInMonth"
              :key="day"
              scope="col"
              :title="getCoverageTooltip(day)"
              class="min-w-[60px] border-r border-[var(--color-border)] px-2 py-3 text-center text-xs font-semibold uppercase tracking-wider text-[var(--color-foreground-muted)]"
            >
              {{ day }}
            </th>
          </tr>
        </thead>

        <tbody class="divide-y divide-[var(--color-border)] bg-[var(--color-surface)]">
          <tr
            v-for="employee in employees"
            :key="employee.employeeId"
            class="transition-colors hover:bg-[var(--color-surface-muted)]/50"
          >
            <td
              class="sticky left-0 z-10 border-r border-[var(--color-border)] bg-[var(--color-surface)] px-4 py-3 text-sm shadow-[2px_0_5px_-2px_rgba(0,0,0,0.06)]"
            >
              <div class="font-medium text-[var(--color-foreground)]">
                {{ employee.name }} {{ employee.surname }}
              </div>
              <div class="text-xs text-[var(--color-muted)]">
                {{ employee.totalHours }} h / {{ employee.totalDays }} dni
              </div>
            </td>

            <td
              v-for="(shift, index) in employee.shifts"
              :key="`${employee.employeeId}-${index}`"
              class="border-r border-[var(--color-border)] px-1 py-2 text-center text-xs"
            >
              <div
                :class="cn(
                  'rounded-md p-1.5',
                  getDiff(employee.employeeId, index + 1)
                    ? getDiffCellClass(getDiff(employee.employeeId, index + 1))
                    : cn('truncate', getShiftClass(shift))
                )"
                :title="getShiftTitle(shift, employee.employeeId, index + 1)"
              >
                <template v-if="getDiff(employee.employeeId, index + 1) && diffMode === 'inline'">
                  <div class="flex flex-col items-center gap-1">
                    <span
                      class="max-w-full rounded bg-[var(--color-danger-soft)] px-1.5 py-0.5 text-[10px] font-semibold text-[var(--color-danger-foreground)] line-through decoration-2"
                    >
                      {{ normalizeShift(getDiff(employee.employeeId, index + 1).before) }}
                    </span>
                    <span
                      class="max-w-full rounded bg-[var(--color-success-soft)] px-1.5 py-0.5 text-[10px] font-semibold text-[var(--color-success-foreground)]"
                    >
                      {{ normalizeShift(getDiff(employee.employeeId, index + 1).after) }}
                    </span>
                    <span
                      v-if="getDiff(employee.employeeId, index + 1).protectedDate"
                      class="text-[9px] font-semibold uppercase tracking-wide text-[var(--color-warning-foreground)]"
                    >
                      chron.
                    </span>
                  </div>
                </template>
                <template v-else-if="getDiff(employee.employeeId, index + 1)">
                  <div class="flex flex-col items-center gap-1">
                    <span class="text-[9px] font-semibold uppercase tracking-wide text-[var(--color-muted)]">
                      {{ getDiffModeLabel() }}
                    </span>
                    <span
                      :class="cn(
                        'max-w-full rounded px-1.5 py-0.5 text-[10px] font-semibold',
                        getDiffModeValueClass()
                      )"
                    >
                      {{ getDiffModeValue(getDiff(employee.employeeId, index + 1)) }}
                    </span>
                    <span
                      v-if="getDiff(employee.employeeId, index + 1).protectedDate"
                      class="text-[9px] font-semibold uppercase tracking-wide text-[var(--color-warning-foreground)]"
                    >
                      chron.
                    </span>
                  </div>
                </template>
                <template v-else>
                  {{ shift || '-' }}
                </template>
              </div>
            </td>
          </tr>

          <tr class="bg-[var(--color-surface-muted)] font-medium">
            <td
              class="sticky left-0 z-10 border-r border-t-2 border-[var(--color-border)] border-t-[var(--color-border-strong)] bg-[var(--color-surface-muted)] px-4 py-3 text-sm text-[var(--color-foreground-muted)]"
            >
              Pokrycie obsady
            </td>
            <td
              v-for="d in scheduleDetails.coverage"
              :key="d.day"
              class="border-r border-t-2 border-[var(--color-border)] border-t-[var(--color-border-strong)] px-1 py-2 text-center text-xs"
            >
              <div
                :class="cn('rounded-md p-1.5', getCoverageClass(d.day))"
                :title="getCoverageTooltip(d.day)"
              >
                {{ getCoverageLabel(d.day) }}
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div
      v-if="showLegend"
      class="border-t border-[var(--color-border)] bg-[var(--color-surface-muted)]/40 px-4 py-3"
    >
      <CoverageLegend />
    </div>
  </div>
</template>
