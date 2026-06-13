<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ChevronLeft, LayoutDashboard } from 'lucide-vue-next'
import ThemeToggle from './ThemeToggle.vue'

const route = useRoute()

const context = computed(() => ({
  isDashboard: route.name === 'dashboard',
  isCreator: route.name === 'create',
  isSchedule: route.name === 'schedule',
  isSummary: route.name === 'summary',
}))
</script>

<template>
  <header
    class="sticky top-0 z-40 border-b border-[var(--color-border)] bg-[var(--color-surface)]/80
      backdrop-blur supports-[backdrop-filter]:bg-[var(--color-surface)]/70"
  >
    <div class="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
      <div class="flex min-w-0 items-center gap-3">
        <RouterLink
          to="/"
          class="flex items-center gap-2.5 transition-opacity hover:opacity-80"
          aria-label="Strona główna"
        >
          <span
            class="flex h-8 w-8 items-center justify-center rounded-md bg-[var(--color-primary)]
              text-base font-bold text-[var(--color-primary-foreground)]"
          >
            G
          </span>
          <span class="hidden text-base font-semibold text-[var(--color-foreground)] sm:block">
            Grafik Generator
          </span>
        </RouterLink>

        <template v-if="!context.isDashboard">
          <span class="text-[var(--color-border-strong)]" aria-hidden="true">/</span>
          <slot name="breadcrumb">
            <RouterLink
              to="/"
              class="inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-sm font-medium
                text-[var(--color-foreground-muted)] transition-colors
                hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-foreground)]"
            >
              <ChevronLeft class="h-4 w-4" aria-hidden="true" />
              Dashboard
            </RouterLink>
          </slot>
        </template>
      </div>

      <div class="flex items-center gap-2">
        <slot name="actions" />
        <ThemeToggle />
      </div>
    </div>

    <div v-if="$slots.subbar" class="border-t border-[var(--color-border)] bg-[var(--color-surface)]">
      <div class="mx-auto max-w-7xl px-4 py-3 sm:px-6 lg:px-8">
        <slot name="subbar" />
      </div>
    </div>
  </header>
</template>
