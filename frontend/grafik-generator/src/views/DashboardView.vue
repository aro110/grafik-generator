<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  ArrowRight,
  CalendarPlus,
  CheckCircle2,
  ClipboardList,
  Clock,
  FileCheck2,
  Loader2,
  Plus,
  RefreshCw,
  Trash2,
  XCircle,
} from 'lucide-vue-next'
import { deleteGenerationGroup, fetchDashboardData } from '../services/dashboardService'
import PageContainer from '../components/layout/PageContainer.vue'
import Card from '../components/ui/Card.vue'
import Button from '../components/ui/Button.vue'
import Badge from '../components/ui/Badge.vue'
import Alert from '../components/ui/Alert.vue'
import Progress from '../components/ui/Progress.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'

const dashboard = ref({
  recentGenerations: [],
  publishedSchedules: [],
})

const isLoading = ref(false)
const loadError = ref('')
const deletingGroupIds = ref({})
const confirmState = ref({ open: false, groupId: null, label: '' })

const fetchDashboard = async () => {
  isLoading.value = true
  loadError.value = ''

  try {
    dashboard.value = (await fetchDashboardData()) || {
      recentGenerations: [],
      publishedSchedules: [],
    }
  } catch (error) {
    loadError.value = error?.message || 'Nie udało się pobrać danych dashboardu.'
  } finally {
    isLoading.value = false
  }
}

const recentGenerations = computed(() => dashboard.value?.recentGenerations || [])
const publishedSchedules = computed(() => dashboard.value?.publishedSchedules || [])

const successCount = computed(
  () => recentGenerations.value.filter((r) => r.status === 'SUCCESS').length,
)
const runningCount = computed(
  () =>
    recentGenerations.value.filter((r) => r.status === 'RUNNING' || r.status === 'PENDING').length,
)

const formatDateTime = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('pl-PL', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

const formatMonth = (value) => {
  if (!value) return '—'
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('pl-PL', {
    month: 'long',
    year: 'numeric',
  }).format(date)
}

const statusMap = {
  PENDING: { label: 'Oczekuje', variant: 'warning', icon: Clock },
  RUNNING: { label: 'Trwa', variant: 'info', icon: Loader2 },
  SUCCESS: { label: 'Zakończona', variant: 'success', icon: CheckCircle2 },
  FAILED: { label: 'Błąd', variant: 'danger', icon: XCircle },
}

const getStatus = (status) =>
  statusMap[status] || { label: status || 'Nieznany', variant: 'neutral', icon: Clock }

const openDeleteConfirm = (groupId, configName) => {
  confirmState.value = {
    open: true,
    groupId,
    label: configName || `grupa ${groupId}`,
  }
}

const closeConfirm = () => {
  confirmState.value = { open: false, groupId: null, label: '' }
}

const confirmDelete = async () => {
  const groupId = confirmState.value.groupId
  if (!groupId) return

  deletingGroupIds.value = { ...deletingGroupIds.value, [groupId]: true }
  closeConfirm()

  try {
    await deleteGenerationGroup(groupId)
    await fetchDashboard()
  } catch (error) {
    loadError.value = error?.message || 'Nie udało się usunąć grafiku.'
  } finally {
    const next = { ...deletingGroupIds.value }
    delete next[groupId]
    deletingGroupIds.value = next
  }
}

onMounted(() => {
  fetchDashboard()
})
</script>

<template>
  <PageContainer>
    <Card padding="none" class="overflow-hidden">
      <div
        class="grid gap-6 bg-gradient-to-br from-[var(--color-primary-soft)] via-[var(--color-surface)] to-[var(--color-surface)]
          p-6 sm:p-8 md:grid-cols-[1fr_auto] md:items-center"
      >
        <div class="space-y-3">
          <p class="text-sm font-medium text-[var(--color-primary)]">Witaj w Grafik Generator</p>
          <h1 class="text-3xl font-bold tracking-tight text-[var(--color-foreground)] sm:text-4xl">
            Zaplanuj nowy grafik w kilka minut
          </h1>
          <p class="max-w-2xl text-base text-[var(--color-foreground-muted)]">
            Skonfiguruj sklep, pracowników i cele obsady — algorytm wygeneruje optymalny grafik
            dla wybranego miesiąca.
          </p>
        </div>
        <div class="flex flex-col gap-2 md:items-end">
          <Button as="RouterLink" to="/create" size="lg" class="!gap-2">
            <Plus class="h-5 w-5" aria-hidden="true" />
            Nowy grafik
          </Button>
          <Button
            variant="ghost"
            size="sm"
            :loading="isLoading"
            @click="fetchDashboard"
          >
            <RefreshCw class="h-4 w-4" aria-hidden="true" />
            Odśwież
          </Button>
        </div>
      </div>
    </Card>

    <section v-if="!isLoading && !loadError && recentGenerations.length > 0" class="mt-6 grid gap-4 sm:grid-cols-3">
      <Card padding="md">
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">
              Wszystkie generacje
            </p>
            <p class="mt-2 text-2xl font-semibold text-[var(--color-foreground)]">
              {{ recentGenerations.length }}
            </p>
          </div>
          <ClipboardList class="h-5 w-5 text-[var(--color-muted)]" aria-hidden="true" />
        </div>
      </Card>
      <Card padding="md">
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">
              Zakończone
            </p>
            <p class="mt-2 text-2xl font-semibold text-[var(--color-success)]">
              {{ successCount }}
            </p>
          </div>
          <CheckCircle2 class="h-5 w-5 text-[var(--color-success)]" aria-hidden="true" />
        </div>
      </Card>
      <Card padding="md">
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">
              Opublikowane
            </p>
            <p class="mt-2 text-2xl font-semibold text-[var(--color-foreground)]">
              {{ publishedSchedules.length }}
            </p>
          </div>
          <FileCheck2 class="h-5 w-5 text-[var(--color-primary)]" aria-hidden="true" />
        </div>
      </Card>
    </section>

    <div v-if="isLoading" class="mt-6 space-y-4">
      <Card v-for="i in 3" :key="i" padding="md">
        <div class="flex animate-pulse items-center gap-4">
          <div class="h-10 w-10 rounded-md bg-[var(--color-surface-muted)]" />
          <div class="flex-1 space-y-2">
            <div class="h-4 w-1/3 rounded bg-[var(--color-surface-muted)]" />
            <div class="h-3 w-1/2 rounded bg-[var(--color-surface-muted)]" />
          </div>
          <div class="h-8 w-24 rounded bg-[var(--color-surface-muted)]" />
        </div>
      </Card>
    </div>

    <Alert v-else-if="loadError" variant="danger" title="Nie udało się załadować dashboardu" class="mt-6">
      {{ loadError }}
    </Alert>

    <template v-else>
      <section class="mt-8">
        <div class="mb-4 flex items-end justify-between gap-4">
          <div>
            <h2 class="text-xl font-semibold text-[var(--color-foreground)]">Ostatnie generacje</h2>
            <p class="mt-1 text-sm text-[var(--color-foreground-muted)]">
              Najnowsze uruchomienia algorytmu wraz ze statusem i postępem.
            </p>
          </div>
          <Badge v-if="runningCount > 0" variant="info">
            <Loader2 class="h-3 w-3 animate-spin" aria-hidden="true" />
            {{ runningCount }} w trakcie
          </Badge>
        </div>

        <Card v-if="recentGenerations.length === 0" padding="lg">
          <div class="flex flex-col items-center justify-center py-8 text-center">
            <div
              class="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-[var(--color-primary-soft)]"
            >
              <CalendarPlus class="h-7 w-7 text-[var(--color-primary)]" aria-hidden="true" />
            </div>
            <h3 class="text-base font-semibold text-[var(--color-foreground)]">
              Brak generacji
            </h3>
            <p class="mt-2 max-w-md text-sm text-[var(--color-foreground-muted)]">
              Nie ma jeszcze żadnych generacji. Utwórz pierwszy grafik, aby zacząć.
            </p>
            <Button as="RouterLink" to="/create" class="mt-4">
              <Plus class="h-4 w-4" aria-hidden="true" />
              Utwórz pierwszy grafik
            </Button>
          </div>
        </Card>

        <div v-else class="space-y-3">
          <Card v-for="run in recentGenerations" :key="run.groupId" padding="md" interactive>
            <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div class="min-w-0 flex-1 space-y-2">
                <div class="flex flex-wrap items-center gap-2">
                  <h3 class="truncate text-base font-semibold text-[var(--color-foreground)]">
                    {{ run.configName || 'Bez nazwy konfiguracji' }}
                  </h3>
                  <Badge :variant="getStatus(run.status).variant">
                    <component
                      :is="getStatus(run.status).icon"
                      class="h-3 w-3"
                      :class="{ 'animate-spin': run.status === 'RUNNING' }"
                      aria-hidden="true"
                    />
                    {{ getStatus(run.status).label }}
                  </Badge>
                </div>

                <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-[var(--color-foreground-muted)]">
                  <span>Sekcje: <strong class="text-[var(--color-foreground)]">{{ run.successSections }} z {{ run.totalSections }}</strong></span>
                  <span aria-hidden="true">·</span>
                  <span>Utworzono {{ formatDateTime(run.createdAt) }}</span>
                  <template v-if="run.finishedAt">
                    <span aria-hidden="true">·</span>
                    <span>Zakończono {{ formatDateTime(run.finishedAt) }}</span>
                  </template>
                </div>

                <Progress
                  v-if="run.status !== 'SUCCESS'"
                  :value="run.progress ?? 0"
                  class="max-w-sm"
                />
              </div>

              <div class="flex shrink-0 flex-wrap items-center gap-2">
                <Button
                  v-if="run.status === 'SUCCESS'"
                  as="RouterLink"
                  :to="`/summary/${run.groupId}`"
                  variant="outline"
                  size="sm"
                >
                  Otwórz
                  <ArrowRight class="h-4 w-4" aria-hidden="true" />
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  :loading="Boolean(deletingGroupIds[run.groupId])"
                  :aria-label="`Usuń ${run.configName || run.groupId}`"
                  @click="openDeleteConfirm(run.groupId, run.configName)"
                >
                  <Trash2 class="h-4 w-4 text-[var(--color-danger)]" aria-hidden="true" />
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </section>

      <section v-if="publishedSchedules.length > 0" class="mt-10">
        <div class="mb-4">
          <h2 class="text-xl font-semibold text-[var(--color-foreground)]">Opublikowane grafiki</h2>
          <p class="mt-1 text-sm text-[var(--color-foreground-muted)]">
            Ostatnio opublikowane harmonogramy gotowe do przeglądania.
          </p>
        </div>

        <div class="grid gap-3 md:grid-cols-2">
          <Card v-for="schedule in publishedSchedules" :key="schedule.groupId" padding="md" interactive>
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0 space-y-1.5">
                <h3 class="truncate text-sm font-semibold text-[var(--color-foreground)]">
                  {{ schedule.configName || 'Bez nazwy konfiguracji' }}
                </h3>
                <p class="text-xs text-[var(--color-foreground-muted)]">
                  {{ formatMonth(schedule.yearMonth) }}
                  · Opublikowano {{ formatDateTime(schedule.publishedAt) }}
                </p>
                <p v-if="schedule.averageFitness" class="text-xs text-[var(--color-foreground-muted)]">
                  Średni fitness: <strong class="text-[var(--color-foreground)]">{{ schedule.averageFitness.toFixed(2) }}</strong>
                </p>
              </div>
              <div class="flex shrink-0 flex-col items-end gap-1">
                <Button as="RouterLink" :to="`/summary/${schedule.groupId}`" variant="ghost" size="sm">
                  Otwórz
                  <ArrowRight class="h-4 w-4" aria-hidden="true" />
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  :loading="Boolean(deletingGroupIds[schedule.groupId])"
                  :aria-label="`Usuń ${schedule.configName || schedule.groupId}`"
                  @click="openDeleteConfirm(schedule.groupId, schedule.configName)"
                >
                  <Trash2 class="h-4 w-4 text-[var(--color-danger)]" aria-hidden="true" />
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </section>
    </template>

    <ConfirmDialog
      :open="confirmState.open"
      title="Usunąć grafik?"
      :description="`Czy na pewno chcesz trwale usunąć „${confirmState.label}” wraz z konfiguracją, sekcjami, pracownikami i wynikami generacji? Tej operacji nie można cofnąć.`"
      confirm-label="Usuń trwale"
      cancel-label="Anuluj"
      variant="destructive"
      @update:open="confirmState.open = $event"
      @confirm="confirmDelete"
      @cancel="closeConfirm"
    />
  </PageContainer>
</template>
