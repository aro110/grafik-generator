<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Calendar as CalendarIcon,
  CheckCircle2,
  Download,
  LayoutGrid,
  Send,
  Sparkles,
  Table as TableIcon,
  XCircle,
} from 'lucide-vue-next'
import {
  applyScheduleAiEdit,
  exportScheduleToExcel,
  fetchScheduleDetails,
  publishSchedule,
  proposeScheduleAiEdit,
  rejectScheduleAiEdit,
} from '../services/scheduleService'
import PageContainer from '../components/layout/PageContainer.vue'
import Card from '../components/ui/Card.vue'
import Button from '../components/ui/Button.vue'
import Badge from '../components/ui/Badge.vue'
import Alert from '../components/ui/Alert.vue'
import Tabs from '../components/ui/Tabs.vue'
import TabsList from '../components/ui/TabsList.vue'
import TabsTrigger from '../components/ui/TabsTrigger.vue'
import ScheduleTable from '../components/schedule/ScheduleTable.vue'
import ScheduleCalendarView from '../components/schedule/ScheduleCalendarView.vue'
import AiDiffReview from '../components/schedule/AiDiffReview.vue'
import ScheduleSideBySideReview from '../components/schedule/ScheduleSideBySideReview.vue'

const route = useRoute()
const router = useRouter()

const scheduleId = computed(() => route.params.id || '')
const scheduleDetails = ref(null)
const isLoading = ref(false)
const loadError = ref('')
const exportInProgress = ref(false)
const publishInProgress = ref(false)
const actionMessage = ref('')
const actionError = ref('')
const aiInstruction = ref('')
const aiAllowProtectedDateChanges = ref(false)
const aiProposal = ref(null)
const aiInProgress = ref(false)
const aiApplyInProgress = ref(false)
const aiRejectInProgress = ref(false)
const aiError = ref('')
const aiMessage = ref('')

const viewMode = ref(route.query.view === 'calendar' ? 'calendar' : 'table')

watch(viewMode, (value) => {
  router.replace({ query: { ...route.query, view: value } })
})

const currentMonth = computed(() => {
  if (!scheduleDetails.value?.yearMonth) return '—'
  const date = new Date(`${scheduleDetails.value.yearMonth}T00:00:00`)
  if (Number.isNaN(date.getTime())) return scheduleDetails.value.yearMonth
  return new Intl.DateTimeFormat('pl-PL', { month: 'long', year: 'numeric' }).format(date)
})

const averageCoverage = computed(() => {
  const coverage = scheduleDetails.value?.coverage || []
  const valid = coverage.filter((d) => !d.closedDay && typeof d.percentage === 'number')
  if (valid.length === 0) return null
  const sum = valid.reduce((acc, d) => acc + d.percentage, 0)
  return Math.round(sum / valid.length)
})

const loadScheduleDetails = async () => {
  actionMessage.value = ''
  actionError.value = ''
  loadError.value = ''
  aiProposal.value = null
  aiError.value = ''
  aiMessage.value = ''

  const id = scheduleId.value
  if (!id || id === 'mock') {
    scheduleDetails.value = null
    loadError.value = 'Brak identyfikatora grafiku.'
    return
  }

  isLoading.value = true
  try {
    scheduleDetails.value = await fetchScheduleDetails(id)
  } catch (error) {
    scheduleDetails.value = null
    loadError.value = error.message || 'Nie udało się pobrać szczegółów grafiku.'
  } finally {
    isLoading.value = false
  }
}

const handleExport = async () => {
  actionMessage.value = ''
  actionError.value = ''
  if (!scheduleDetails.value?.id) {
    actionError.value = 'Najpierw załaduj prawdziwy grafik.'
    return
  }
  exportInProgress.value = true
  try {
    await exportScheduleToExcel(scheduleDetails.value.id)
    actionMessage.value = 'Plik Excela został pobrany.'
  } catch (error) {
    actionError.value = error.message || 'Wystąpił błąd podczas eksportu grafiku.'
  } finally {
    exportInProgress.value = false
  }
}

const handlePublish = async () => {
  actionMessage.value = ''
  actionError.value = ''
  if (!scheduleDetails.value?.id) {
    actionError.value = 'Najpierw załaduj prawdziwy grafik.'
    return
  }
  publishInProgress.value = true
  try {
    const response = await publishSchedule(scheduleDetails.value.id)
    actionMessage.value = response?.message || 'Grafik został opublikowany.'
    if (scheduleDetails.value) {
      scheduleDetails.value = { ...scheduleDetails.value, published: true }
    }
  } catch (error) {
    actionError.value = error.message || 'Wystąpił błąd podczas publikacji grafiku.'
  } finally {
    publishInProgress.value = false
  }
}

const handleAiPropose = async () => {
  aiError.value = ''
  aiMessage.value = ''
  aiProposal.value = null
  if (!scheduleDetails.value?.id) {
    aiError.value = 'Najpierw załaduj prawdziwy grafik.'
    return
  }
  if (!aiInstruction.value.trim()) {
    aiError.value = 'Wpisz polecenie dla AI.'
    return
  }

  aiInProgress.value = true
  try {
    aiProposal.value = await proposeScheduleAiEdit(scheduleDetails.value.id, {
      instruction: aiInstruction.value.trim(),
      allowProtectedDateChanges: aiAllowProtectedDateChanges.value,
    })
    aiMessage.value = 'Propozycja AI jest gotowa do sprawdzenia.'
  } catch (error) {
    aiError.value = error.message || 'Nie udało się przygotować propozycji AI.'
  } finally {
    aiInProgress.value = false
  }
}

const handleAiApply = async () => {
  aiError.value = ''
  aiMessage.value = ''
  if (!scheduleDetails.value?.id || !aiProposal.value?.editId) return

  aiApplyInProgress.value = true
  try {
    const response = await applyScheduleAiEdit(scheduleDetails.value.id, aiProposal.value.editId)
    const acceptedId = response?.data?.acceptedScheduleId || response?.data?.acceptedSchedule?.id
    aiMessage.value = response?.message || 'Propozycja AI została zapisana jako nowy grafik.'
    aiProposal.value = null
    if (acceptedId) {
      router.push({ name: 'schedule', params: { id: acceptedId }, query: { view: viewMode.value } })
    } else {
      loadScheduleDetails()
    }
  } catch (error) {
    aiError.value = error.message || 'Nie udało się zaakceptować propozycji AI.'
  } finally {
    aiApplyInProgress.value = false
  }
}

const handleAiReject = async () => {
  aiError.value = ''
  aiMessage.value = ''
  if (!scheduleDetails.value?.id || !aiProposal.value?.editId) return

  aiRejectInProgress.value = true
  try {
    const response = await rejectScheduleAiEdit(scheduleDetails.value.id, aiProposal.value.editId)
    aiProposal.value = null
    aiMessage.value = response?.message || 'Propozycja AI została odrzucona.'
  } catch (error) {
    aiError.value = error.message || 'Nie udało się odrzucić propozycji AI.'
  } finally {
    aiRejectInProgress.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    loadScheduleDetails()
  },
)

onMounted(() => {
  loadScheduleDetails()
})
</script>

<template>
  <PageContainer width="wide">
    <template #header>
      <div>
        <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-primary)]">
          Grafik miesięczny
        </p>
        <h1 class="mt-1 text-2xl font-semibold text-[var(--color-foreground)]">
          {{ scheduleDetails?.sectionName || 'Podgląd grafiku' }}
        </h1>
        <p v-if="scheduleDetails" class="mt-1 text-sm text-[var(--color-foreground-muted)]">
          {{ currentMonth }}
          · Konfiguracja: <strong class="text-[var(--color-foreground)]">{{ scheduleDetails.configName }}</strong>
        </p>
      </div>
    </template>

    <template #actions>
      <Button
        variant="outline"
        :loading="exportInProgress"
        :disabled="exportInProgress || isLoading || !scheduleDetails"
        @click="handleExport"
      >
        <Download v-if="!exportInProgress" class="h-4 w-4" aria-hidden="true" />
        {{ exportInProgress ? 'Eksportowanie...' : 'Eksport Excel' }}
      </Button>
      <Button
        :loading="publishInProgress"
        :disabled="publishInProgress || isLoading || !scheduleDetails || scheduleDetails?.published"
        @click="handlePublish"
      >
        <Send v-if="!publishInProgress" class="h-4 w-4" aria-hidden="true" />
        {{
          scheduleDetails?.published
            ? 'Opublikowany'
            : publishInProgress
              ? 'Publikowanie...'
              : 'Publikuj'
        }}
      </Button>
    </template>

    <Alert v-if="actionMessage" variant="success" class="mb-4">{{ actionMessage }}</Alert>
    <Alert v-if="actionError" variant="danger" class="mb-4">{{ actionError }}</Alert>

    <div v-if="isLoading" class="space-y-4">
      <Card v-for="i in 5" :key="i" padding="md">
        <div class="h-6 animate-pulse rounded bg-[var(--color-surface-muted)]" />
      </Card>
    </div>

    <Alert v-else-if="loadError" variant="danger" title="Nie udało się załadować grafiku">
      {{ loadError }}
    </Alert>

    <template v-else-if="scheduleDetails">
      <section class="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Card padding="sm">
          <p class="text-[10px] font-medium uppercase tracking-wide text-[var(--color-muted)]">
            Sekcja
          </p>
          <p class="mt-1.5 truncate text-sm font-semibold text-[var(--color-foreground)]">
            {{ scheduleDetails.sectionName }}
          </p>
        </Card>
        <Card padding="sm">
          <p class="text-[10px] font-medium uppercase tracking-wide text-[var(--color-muted)]">
            Miesiąc
          </p>
          <p class="mt-1.5 text-sm font-semibold text-[var(--color-foreground)]">
            {{ currentMonth }}
          </p>
        </Card>
        <Card padding="sm">
          <p class="text-[10px] font-medium uppercase tracking-wide text-[var(--color-muted)]">
            Status
          </p>
          <div class="mt-1.5">
            <Badge :variant="scheduleDetails.published ? 'success' : 'warning'">
              <CheckCircle2 v-if="scheduleDetails.published" class="h-3 w-3" aria-hidden="true" />
              {{ scheduleDetails.published ? 'Opublikowany' : 'Nieopublikowany' }}
            </Badge>
          </div>
        </Card>
        <Card padding="sm">
          <p class="text-[10px] font-medium uppercase tracking-wide text-[var(--color-muted)]">
            Średnie pokrycie
          </p>
          <p class="mt-1.5 text-sm font-semibold text-[var(--color-foreground)]">
            {{ averageCoverage !== null ? `${averageCoverage}%` : '—' }}
            <span class="ml-2 text-xs font-normal text-[var(--color-muted)]">
              Fitness {{ scheduleDetails.fitness }}
            </span>
          </p>
        </Card>
      </section>

      <Card padding="md" class="mb-4">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end">
          <div class="min-w-0 flex-1">
            <label
              for="ai-instruction"
              class="text-sm font-medium text-[var(--color-foreground)]"
            >
              Popraw z AI
            </label>
            <textarea
              id="ai-instruction"
              v-model="aiInstruction"
              rows="3"
              class="mt-2 w-full resize-y rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-foreground)] shadow-sm outline-none transition-colors placeholder:text-[var(--color-muted)] focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
              placeholder="Np. zamień zmianę Ani z 12 dnia z Markiem, ale zachowaj urlopy i liczbę godzin."
              :disabled="aiInProgress || aiApplyInProgress"
            />
            <label class="mt-3 flex items-center gap-2 text-xs text-[var(--color-foreground-muted)]">
              <input
                v-model="aiAllowProtectedDateChanges"
                type="checkbox"
                class="h-4 w-4 rounded border-[var(--color-border)] text-[var(--color-primary)]"
                :disabled="aiInProgress || aiApplyInProgress"
              />
              Pozwól AI proponować pracę w urlopy i ręcznie wybrane wolne tylko w nowym grafiku
            </label>
          </div>
          <Button
            :loading="aiInProgress"
            :disabled="aiInProgress || aiApplyInProgress || !aiInstruction.trim()"
            @click="handleAiPropose"
          >
            <Sparkles v-if="!aiInProgress" class="h-4 w-4" aria-hidden="true" />
            {{ aiInProgress ? 'AI analizuje...' : 'Przygotuj propozycję' }}
          </Button>
        </div>

        <Alert v-if="aiMessage" variant="success" class="mt-4">{{ aiMessage }}</Alert>
        <Alert v-if="aiError" variant="danger" class="mt-4">{{ aiError }}</Alert>
      </Card>

      <Card v-if="!aiProposal?.proposedSchedule" padding="none">
        <div
          class="flex items-center justify-between gap-3 border-b border-[var(--color-border)] px-4 py-3"
        >
          <Tabs v-model="viewMode" :default-value="viewMode">
            <TabsList>
              <TabsTrigger value="table">
                <TableIcon class="h-4 w-4" aria-hidden="true" />
                Tabela
              </TabsTrigger>
              <TabsTrigger value="calendar">
                <CalendarIcon class="h-4 w-4" aria-hidden="true" />
                Kalendarz
              </TabsTrigger>
            </TabsList>
          </Tabs>
          <p class="hidden text-xs text-[var(--color-muted)] sm:block">
            <LayoutGrid class="inline h-3 w-3" aria-hidden="true" />
            Wybierz widok grafiku
          </p>
        </div>

        <div v-if="viewMode === 'table'">
          <ScheduleTable :schedule-details="scheduleDetails" />
        </div>
        <div v-else class="p-4">
          <ScheduleCalendarView :schedule-details="scheduleDetails" />
        </div>
      </Card>

      <Card v-if="aiProposal?.proposedSchedule" padding="none">
        <div
          class="flex flex-col gap-3 border-b border-[var(--color-border)] px-4 py-3 lg:flex-row lg:items-center lg:justify-between"
        >
          <div>
            <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-primary)]">
              Propozycja AI
            </p>
            <h2 class="mt-1 text-lg font-semibold text-[var(--color-foreground)]">
              Nowy grafik do akceptacji
            </h2>
            <p class="mt-1 text-sm text-[var(--color-foreground-muted)]">
              Porównaj stare i nowe wartości poniżej. Stary grafik nie został zastąpiony.
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            <Button
              variant="outline"
              :disabled="aiApplyInProgress || aiRejectInProgress"
              :loading="aiRejectInProgress"
              @click="handleAiReject"
            >
              <XCircle v-if="!aiRejectInProgress" class="h-4 w-4" aria-hidden="true" />
              Odrzuć
            </Button>
            <Button
              :disabled="aiApplyInProgress || aiRejectInProgress"
              :loading="aiApplyInProgress"
              @click="handleAiApply"
            >
              <Send v-if="!aiApplyInProgress" class="h-4 w-4" aria-hidden="true" />
              Zapisz jako nowy grafik
            </Button>
          </div>
        </div>

        <div
          v-if="aiProposal.warnings?.length"
          class="border-b border-[var(--color-border)] px-4 py-3 text-sm text-[var(--color-warning-foreground)]"
        >
          {{ aiProposal.warnings.join(' ') }}
        </div>

        <AiDiffReview
          :diff="aiProposal.diff"
          title="Co Gemini zmieniło w grafiku"
          description="Lista poniżej daje szybki dostęp do starej wartości oraz nowej propozycji. Niżej zobaczysz oba grafiki obok siebie."
        />

        <div
          class="flex items-center justify-between gap-3 border-b border-[var(--color-border)] px-4 py-3"
        >
          <Tabs v-model="viewMode" :default-value="viewMode">
            <TabsList>
              <TabsTrigger value="table">
                <TableIcon class="h-4 w-4" aria-hidden="true" />
                Tabela
              </TabsTrigger>
              <TabsTrigger value="calendar">
                <CalendarIcon class="h-4 w-4" aria-hidden="true" />
                Kalendarz
              </TabsTrigger>
            </TabsList>
          </Tabs>
          <p class="hidden text-xs text-[var(--color-muted)] sm:block">
            Porównanie side-by-side
          </p>
        </div>

        <div class="p-4">
          <ScheduleSideBySideReview
            :before-schedule="scheduleDetails"
            :after-schedule="aiProposal.proposedSchedule"
            :diff="aiProposal.diff"
            :view-mode="viewMode"
          />
        </div>
      </Card>
    </template>
  </PageContainer>
</template>
