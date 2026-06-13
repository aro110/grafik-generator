<script setup>
import ScheduleCalendarView from './ScheduleCalendarView.vue'
import ScheduleTable from './ScheduleTable.vue'

defineProps({
  beforeSchedule: {
    type: Object,
    required: true,
  },
  afterSchedule: {
    type: Object,
    required: true,
  },
  diff: {
    type: Array,
    default: () => [],
  },
  viewMode: {
    type: String,
    default: 'table',
    validator: (value) => ['table', 'calendar'].includes(value),
  },
  beforeTitle: {
    type: String,
    default: 'Obecny grafik',
  },
  afterTitle: {
    type: String,
    default: 'Propozycja Gemini',
  },
})
</script>

<template>
  <div class="grid gap-4 xl:grid-cols-2">
    <section class="min-w-0 overflow-hidden rounded-md border border-[var(--color-border)] bg-[var(--color-surface)]">
      <div class="border-b border-[var(--color-border)] px-4 py-3">
        <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-danger-foreground)]">
          Było
        </p>
        <h3 class="mt-1 text-sm font-semibold text-[var(--color-foreground)]">
          {{ beforeTitle }}
        </h3>
        <p class="mt-1 text-xs text-[var(--color-foreground-muted)]">
          {{ beforeSchedule.sectionName }} · Fitness {{ beforeSchedule.fitness }}
        </p>
      </div>
      <div v-if="viewMode === 'table'">
        <ScheduleTable
          :schedule-details="beforeSchedule"
          :diff="diff"
          diff-mode="before"
          :show-legend="false"
        />
      </div>
      <div v-else class="p-4">
        <ScheduleCalendarView :schedule-details="beforeSchedule" :diff="diff" />
      </div>
    </section>

    <section class="min-w-0 overflow-hidden rounded-md border border-[var(--color-success)]/40 bg-[var(--color-surface)]">
      <div class="border-b border-[var(--color-border)] px-4 py-3">
        <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-success-foreground)]">
          Będzie
        </p>
        <h3 class="mt-1 text-sm font-semibold text-[var(--color-foreground)]">
          {{ afterTitle }}
        </h3>
        <p class="mt-1 text-xs text-[var(--color-foreground-muted)]">
          {{ afterSchedule.sectionName }} · Fitness {{ afterSchedule.fitness }}
        </p>
      </div>
      <div v-if="viewMode === 'table'">
        <ScheduleTable
          :schedule-details="afterSchedule"
          :diff="diff"
          diff-mode="after"
          :show-legend="false"
        />
      </div>
      <div v-else class="p-4">
        <ScheduleCalendarView :schedule-details="afterSchedule" :diff="diff" />
      </div>
    </section>
  </div>
</template>
