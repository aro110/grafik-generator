<script setup>
import { cn } from '../../lib/utils'
import Badge from '../ui/Badge.vue'

defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
  days: {
    type: Array,
    default: () => [],
  },
  tone: {
    type: String,
    default: 'neutral',
    validator: (value) => ['neutral', 'before', 'after'].includes(value),
  },
})

const getCoverageClass = (day) => {
  if (day.closedDay) return 'bg-[var(--color-surface-muted)] text-[var(--color-muted)]'
  const ratio = day.totalRequired > 0 ? day.totalActual / day.totalRequired : 1
  if (ratio < 1)
    return 'bg-[var(--color-danger-soft)] text-[var(--color-danger-foreground)] font-semibold'
  if (ratio > 1)
    return 'bg-[var(--color-warning-soft)] text-[var(--color-warning-foreground)] font-semibold'
  return 'bg-[var(--color-success-soft)] text-[var(--color-success-foreground)] font-semibold'
}

const getToneClass = (tone) => {
  if (tone === 'before') return 'border-[var(--color-danger)]/30'
  if (tone === 'after') return 'border-[var(--color-success)]/40'
  return 'border-[var(--color-border)]'
}
</script>

<template>
  <section :class="cn('min-w-0 overflow-hidden rounded-md border bg-[var(--color-surface)]', getToneClass(tone))">
    <div class="border-b border-[var(--color-border)] px-4 py-3">
      <p
        :class="cn(
          'text-xs font-medium uppercase tracking-wide',
          tone === 'before' && 'text-[var(--color-danger-foreground)]',
          tone === 'after' && 'text-[var(--color-success-foreground)]',
          tone === 'neutral' && 'text-[var(--color-primary)]'
        )"
      >
        {{ tone === 'before' ? 'Było' : tone === 'after' ? 'Będzie' : 'Podsumowanie' }}
      </p>
      <h3 class="mt-1 text-sm font-semibold text-[var(--color-foreground)]">
        {{ title }}
      </h3>
      <p v-if="description" class="mt-1 text-xs text-[var(--color-foreground-muted)]">
        {{ description }}
      </p>
    </div>

    <div class="overflow-x-auto">
      <table class="w-full text-left text-sm">
        <thead class="bg-[var(--color-surface-muted)] text-xs uppercase text-[var(--color-foreground-muted)]">
          <tr>
            <th scope="col" class="w-16 px-4 py-3 text-center font-semibold">Dzień</th>
            <th scope="col" class="px-4 py-3 font-semibold">Data</th>
            <th scope="col" class="px-4 py-3 text-center font-semibold">Status</th>
            <th scope="col" class="px-4 py-3 text-center font-semibold">Pokrycie</th>
            <th scope="col" class="px-4 py-3 font-semibold">Na sekcjach</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[var(--color-border)]">
          <tr
            v-for="day in days"
            :key="day.day"
            class="transition-colors hover:bg-[var(--color-surface-muted)]/40"
          >
            <td class="px-4 py-3 text-center font-medium text-[var(--color-foreground)]">
              {{ day.day }}
            </td>
            <td class="whitespace-nowrap px-4 py-3 text-[var(--color-foreground-muted)]">
              {{ day.date }}
            </td>
            <td class="px-4 py-3 text-center">
              <Badge :variant="day.closedDay ? 'neutral' : 'info'">
                {{ day.closedDay ? 'Zamknięte' : 'Otwarte' }}
              </Badge>
            </td>
            <td class="px-4 py-3 text-center">
              <span :class="cn('inline-flex rounded-md px-3 py-1 text-sm', getCoverageClass(day))">
                <template v-if="day.closedDay">-</template>
                <template v-else>
                  {{ day.totalActual }} / {{ day.totalRequired }}
                  ({{ day.percentage }}%)
                </template>
              </span>
            </td>
            <td class="px-4 py-3">
              <div v-if="!day.closedDay" class="flex flex-wrap gap-1.5">
                <Badge
                  v-for="sec in day.sections"
                  :key="sec.sectionId"
                  variant="neutral"
                >
                  {{ sec.sectionName }}: <strong>{{ sec.working }}</strong>
                </Badge>
              </div>
              <span v-else class="text-xs text-[var(--color-muted)]">-</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
