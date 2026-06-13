<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CheckCircle2,
  Download,
  Loader2,
  RefreshCw,
  Send,
  Calendar as CalendarIcon,
  Table as TableIcon,
} from 'lucide-vue-next'
import {
  fetchStoreSummary,
  fetchScheduleDetails,
  proposeGroupScheduleAiEdit,
  applyGroupScheduleAiEdit,
  rejectGroupScheduleAiEdit,
  exportScheduleToExcel,
  exportGroupToExcel,
  publishSchedule,
} from '../services/scheduleService'
import { regenerateGroup, pollGenerationUntilFinished } from '../services/creatorService'
import PageContainer from '../components/layout/PageContainer.vue'
import Card from '../components/ui/Card.vue'
import CardHeader from '../components/ui/CardHeader.vue'
import Button from '../components/ui/Button.vue'
import Badge from '../components/ui/Badge.vue'
import Alert from '../components/ui/Alert.vue'
import Progress from '../components/ui/Progress.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import Tabs from '../components/ui/Tabs.vue'
import TabsList from '../components/ui/TabsList.vue'
import TabsTrigger from '../components/ui/TabsTrigger.vue'
import ScheduleTable from '../components/schedule/ScheduleTable.vue'
import ScheduleCalendarView from '../components/schedule/ScheduleCalendarView.vue'
import AiDiffReview from '../components/schedule/AiDiffReview.vue'
import ScheduleSideBySideReview from '../components/schedule/ScheduleSideBySideReview.vue'
import StoreCoverageTable from '../components/schedule/StoreCoverageTable.vue'

const route = useRoute()
const router = useRouter()

const groupId = computed(() => route.params.id || '')
const summary = ref(null)
const isLoading = ref(false)
const loadError = ref('')
const schedulesDetails = ref({})
const isExporting = ref({})
const isExportingGroup = ref(false)
const isPublishing = ref({})
const isRegenerating = ref(false)
const regenerateProgress = ref(0)
const regenerateMessage = ref('')
const showRegenerateConfirm = ref(false)
const aiInstruction = ref('')
const aiAllowProtected = ref(false)
const aiProposal = ref(null)
const aiError = ref('')
const aiMessage = ref('')
const isAiLoading = ref(false)
const isAiApplying = ref(false)
const isAiRejecting = ref(false)

const sectionViewModes = ref({})

const currentMonth = computed(() => {
  if (!summary.value?.yearMonth) return '—'
  const date = new Date(`${summary.value.yearMonth}T00:00:00`)
  if (Number.isNaN(date.getTime())) return summary.value.yearMonth
  return new Intl.DateTimeFormat('pl-PL', { month: 'long', year: 'numeric' }).format(date)
})

const days = computed(() => summary.value?.days || [])

const aggregateSchedulesToStoreDays = (schedules) => {
  const coverageRows = schedules.flatMap((schedule) =>
    (schedule.coverage || []).map((day) => ({
      ...day,
      sectionId: schedule.sectionId,
      sectionName: schedule.sectionName,
    })),
  )

  const daysMap = coverageRows.reduce((acc, day) => {
    if (!acc[day.day]) acc[day.day] = []
    acc[day.day].push(day)
    return acc
  }, {})

  return Object.entries(daysMap)
    .map(([day, rows]) => {
      const openRows = rows.filter((row) => !row.closedDay)
      const totalRequired = openRows.reduce((sum, row) => sum + (row.required || 0), 0)
      const totalActual = openRows.reduce((sum, row) => sum + (row.actual || 0), 0)
      const closedDay = rows.length > 0 && openRows.length === 0

      return {
        day: Number(day),
        date: rows[0]?.date || '',
        closedDay,
        totalRequired,
        totalActual,
        percentage: totalRequired > 0 ? Math.round((totalActual / totalRequired) * 100) : 100,
        sections: rows.map((row) => ({
          sectionId: row.sectionId,
          sectionName: row.sectionName,
          working: row.closedDay ? 0 : row.actual || 0,
        })),
      }
    })
    .sort((a, b) => a.day - b.day)
}

const proposedStoreDays = computed(() =>
  aggregateSchedulesToStoreDays(aiProposal.value?.proposedSchedules || []),
)

const proposalPairs = computed(() => {
  const proposed = aiProposal.value?.proposedSchedules || []
  return (summary.value?.scheduleIds || [])
    .map((scheduleId) => {
      const before = schedulesDetails.value[scheduleId]
      const after = proposed.find((item) => item.sectionId === before?.sectionId)
      return { scheduleId, before, after }
    })
    .filter((pair) => pair.before && pair.after)
})

const loadSummary = async () => {
  loadError.value = ''
  const currentGroupId = groupId.value
  if (!currentGroupId) {
    summary.value = null
    loadError.value = 'Brak identyfikatora grupy generacji.'
    return
  }
  isLoading.value = true
  try {
    aiProposal.value = null
    aiError.value = ''
    aiMessage.value = ''
    summary.value = await fetchStoreSummary(currentGroupId)
    if (summary.value?.scheduleIds) {
      await Promise.all(
        summary.value.scheduleIds.map(async (id) => {
          const details = await fetchScheduleDetails(id)
          schedulesDetails.value = { ...schedulesDetails.value, [id]: details }
          if (!sectionViewModes.value[id]) sectionViewModes.value[id] = 'table'
        }),
      )
    }
  } catch (error) {
    summary.value = null
    loadError.value = error.message || 'Nie udało się pobrać podsumowania sklepu.'
  } finally {
    isLoading.value = false
  }
}

const handleAiPropose = async () => {
  aiError.value = ''
  aiMessage.value = ''
  aiProposal.value = null
  if (!groupId.value) {
    aiError.value = 'Brak identyfikatora grupy generacji.'
    return
  }
  if (!aiInstruction.value.trim()) {
    aiError.value = 'Wpisz polecenie dla AI.'
    return
  }

  isAiLoading.value = true
  try {
    aiProposal.value = await proposeGroupScheduleAiEdit(groupId.value, {
      instruction: aiInstruction.value.trim(),
      allowProtectedDateChanges: aiAllowProtected.value,
    })
    aiMessage.value = 'Propozycja AI dla całej konfiguracji jest gotowa do sprawdzenia.'
  } catch (error) {
    aiError.value = error.message || 'Nie udało się przygotować propozycji AI.'
  } finally {
    isAiLoading.value = false
  }
}

const handleAiApply = async () => {
  aiError.value = ''
  aiMessage.value = ''
  if (!groupId.value || !aiProposal.value?.editId) return

  isAiApplying.value = true
  try {
    const response = await applyGroupScheduleAiEdit(groupId.value, aiProposal.value.editId)
    const newGroupId = response?.data?.acceptedGroupId
    aiMessage.value = response?.message || 'Propozycja AI została zapisana jako nowa grupa grafików.'
    aiProposal.value = null
    if (newGroupId) {
      router.push(`/summary/${newGroupId}`).then(() => {
        setTimeout(() => {
          window.location.reload()
        }, 500)
      })
    } else {
      await loadSummary()
    }
  } catch (error) {
    aiError.value = error.message || 'Nie udało się zaakceptować propozycji AI.'
  } finally {
    isAiApplying.value = false
  }
}

const handleAiReject = async () => {
  aiError.value = ''
  aiMessage.value = ''
  if (!groupId.value || !aiProposal.value?.editId) return

  isAiRejecting.value = true
  try {
    const response = await rejectGroupScheduleAiEdit(groupId.value, aiProposal.value.editId)
    aiProposal.value = null
    aiMessage.value = response?.message || 'Propozycja AI została odrzucona.'
  } catch (error) {
    aiError.value = error.message || 'Nie udało się odrzucić propozycji AI.'
  } finally {
    isAiRejecting.value = false
  }
}

const requestRegenerate = () => {
  showRegenerateConfirm.value = true
}

const performRegenerate = async () => {
  showRegenerateConfirm.value = false
  if (!groupId.value) return

  isRegenerating.value = true
  regenerateMessage.value = 'Wznawianie generacji...'
  regenerateProgress.value = 0

  try {
    const newGroup = await regenerateGroup(groupId.value)
    const newGroupId = newGroup?.id
    if (!newGroupId) throw new Error('Backend nie zwrócił identyfikatora nowej grupy generacji.')

    regenerateMessage.value = 'Trwa generowanie nowego grafiku...'
    await pollGenerationUntilFinished(newGroupId, {
      onProgress: (info) => {
        regenerateProgress.value = info?.progress ?? 0
        regenerateMessage.value = `Trwa generowanie nowego grafiku... (${regenerateProgress.value}%)`
      },
    })

    regenerateProgress.value = 100
    regenerateMessage.value = 'Generowanie zakończyło się sukcesem. Odświeżanie widoku...'

    router.push(`/summary/${newGroupId}`).then(() => {
      setTimeout(() => {
        window.location.reload()
      }, 500)
    })
  } catch (error) {
    loadError.value = error.message || 'Wystąpił błąd podczas ponawiania generacji.'
  } finally {
    isRegenerating.value = false
  }
}

const handleExport = async (scheduleId) => {
  isExporting.value[scheduleId] = true
  try {
    await exportScheduleToExcel(scheduleId)
  } catch (error) {
    loadError.value = error.message || 'Wystąpił błąd podczas eksportu grafiku.'
  } finally {
    isExporting.value[scheduleId] = false
  }
}

const handleGroupExport = async () => {
  if (!groupId.value) return
  isExportingGroup.value = true
  try {
    await exportGroupToExcel(groupId.value)
  } catch (error) {
    loadError.value = error.message || 'Wystąpił błąd podczas eksportu grafiku sklepu.'
  } finally {
    isExportingGroup.value = false
  }
}

const handlePublish = async (scheduleId) => {
  isPublishing.value[scheduleId] = true
  try {
    await publishSchedule(scheduleId)
    schedulesDetails.value[scheduleId].published = true
  } catch (error) {
    loadError.value = error.message || 'Wystąpił błąd podczas publikacji grafiku.'
  } finally {
    isPublishing.value[scheduleId] = false
  }
}

onMounted(() => {
  loadSummary()
})
</script>

<template>
  <PageContainer width="wide" title="Podsumowanie sklepu">
    <template #header>
      <div>
        <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-primary)]">
          Grupa generacji
        </p>
        <h1 class="mt-1 text-2xl font-semibold text-[var(--color-foreground)]">
          {{ currentMonth }}
        </h1>
        <p v-if="summary?.scheduleIds" class="mt-1 text-sm text-[var(--color-foreground-muted)]">
          Sekcje: <strong class="text-[var(--color-foreground)]">{{ summary.scheduleIds.length }}</strong>
          · Dni w miesiącu: <strong class="text-[var(--color-foreground)]">{{ days.length }}</strong>
        </p>
      </div>
    </template>

    <template #actions>
      <Button
        variant="outline"
        :loading="isExportingGroup"
        :disabled="isExportingGroup || isLoading"
        @click="handleGroupExport"
      >
        <Download v-if="!isExportingGroup" class="h-4 w-4" aria-hidden="true" />
        {{ isExportingGroup ? 'Eksportowanie...' : 'Eksport całości' }}
      </Button>
      <Button
        :loading="isRegenerating"
        :disabled="isRegenerating || isLoading"
        @click="requestRegenerate"
      >
        <RefreshCw v-if="!isRegenerating" class="h-4 w-4" aria-hidden="true" />
        {{ isRegenerating ? 'Generowanie...' : 'Generuj ponownie' }}
      </Button>
    </template>

    <Alert v-if="isRegenerating" variant="info" title="Generowanie w toku" class="mb-4">
      <p>{{ regenerateMessage }}</p>
      <Progress
        :value="regenerateProgress"
        :show-value="true"
        label="Postęp"
        class="mt-3"
      />
    </Alert>

    <Alert v-if="loadError" variant="danger" title="Coś poszło nie tak" class="mb-4">
      {{ loadError }}
      <div class="mt-3">
        <Button variant="outline" size="sm" @click="loadSummary">
          <RefreshCw class="h-4 w-4" aria-hidden="true" />
          Spróbuj ponownie
        </Button>
      </div>
    </Alert>

    <div v-if="isLoading && !summary" class="space-y-4">
      <Card v-for="i in 3" :key="i" padding="md">
        <div class="h-6 animate-pulse rounded bg-[var(--color-surface-muted)]" />
      </Card>
    </div>

    <template v-else-if="summary">
      <Card padding="md" class="mb-6">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end">
          <div class="min-w-0 flex-1">
            <label for="group-ai-instruction" class="text-sm font-medium text-[var(--color-foreground)]">
              Popraw całą konfigurację z AI
            </label>
            <textarea
              id="group-ai-instruction"
              v-model="aiInstruction"
              rows="3"
              class="mt-2 w-full resize-y rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-foreground)] shadow-sm outline-none transition-colors placeholder:text-[var(--color-muted)] focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20"
              placeholder="Np. popraw obsadę weekendów we wszystkich sekcjach, zachowując urlopy i liczbę godzin."
              :disabled="isAiLoading || isAiApplying"
            />
            <label class="mt-3 flex items-center gap-2 text-xs text-[var(--color-foreground-muted)]">
              <input
                v-model="aiAllowProtected"
                type="checkbox"
                class="h-4 w-4 rounded border-[var(--color-border)] text-[var(--color-primary)]"
                :disabled="isAiLoading || isAiApplying"
              />
              Pozwól AI proponować pracę w urlopy i ręcznie wybrane wolne tylko w nowej grupie grafików
            </label>
          </div>
          <Button
            :loading="isAiLoading"
            :disabled="isAiLoading || isAiApplying || !aiInstruction.trim()"
            @click="handleAiPropose"
          >
            <Send v-if="!isAiLoading" class="h-4 w-4" aria-hidden="true" />
            {{ isAiLoading ? 'AI analizuje...' : 'Przygotuj propozycję' }}
          </Button>
        </div>
        <Alert v-if="aiMessage" variant="success" class="mt-4">
          {{ aiMessage }}
        </Alert>
        <Alert v-if="aiError" variant="danger" class="mt-4">
          {{ aiError }}
        </Alert>
      </Card>

      <StoreCoverageTable
        v-if="!aiProposal?.proposedSchedules?.length"
        class="mb-6"
        title="Pokrycie sklepu"
        description="Dzienna obsada we wszystkich sekcjach łącznie."
        :days="days"
      />

      <section v-if="!aiProposal?.proposedSchedules?.length" class="space-y-6">
        <Card
          v-for="scheduleId in summary.scheduleIds"
          :key="scheduleId"
          padding="none"
        >
          <CardHeader
            :title="schedulesDetails[scheduleId]?.sectionName || 'Ładowanie...'"
            :description="schedulesDetails[scheduleId]?.configName ? `Konfiguracja: ${schedulesDetails[scheduleId].configName} · Fitness ${schedulesDetails[scheduleId].fitness}` : ''"
          >
            <template #actions>
              <Badge
                v-if="schedulesDetails[scheduleId]"
                :variant="schedulesDetails[scheduleId].published ? 'success' : 'warning'"
              >
                <CheckCircle2
                  v-if="schedulesDetails[scheduleId].published"
                  class="h-3 w-3"
                  aria-hidden="true"
                />
                {{ schedulesDetails[scheduleId].published ? 'Opublikowany' : 'Nieopublikowany' }}
              </Badge>
              <Button
                variant="outline"
                size="sm"
                :loading="isExporting[scheduleId]"
                :disabled="isExporting[scheduleId]"
                @click="handleExport(scheduleId)"
              >
                <Download v-if="!isExporting[scheduleId]" class="h-4 w-4" aria-hidden="true" />
                Excel
              </Button>
              <Button
                size="sm"
                :loading="isPublishing[scheduleId]"
                :disabled="isPublishing[scheduleId] || schedulesDetails[scheduleId]?.published"
                @click="handlePublish(scheduleId)"
              >
                <Send v-if="!isPublishing[scheduleId]" class="h-4 w-4" aria-hidden="true" />
                {{ schedulesDetails[scheduleId]?.published ? 'Opublikowany' : 'Publikuj' }}
              </Button>
            </template>
          </CardHeader>

          <div v-if="schedulesDetails[scheduleId]?.employees">
            <div class="flex items-center gap-3 border-b border-[var(--color-border)] px-4 py-3">
              <Tabs
                :model-value="sectionViewModes[scheduleId] || 'table'"
                default-value="table"
                @update:model-value="sectionViewModes[scheduleId] = $event"
              >
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
            </div>

            <div v-if="sectionViewModes[scheduleId] === 'table'">
              <ScheduleTable :schedule-details="schedulesDetails[scheduleId]" :show-legend="false" />
            </div>
            <div v-else class="p-4">
              <ScheduleCalendarView :schedule-details="schedulesDetails[scheduleId]" />
            </div>
          </div>
          <div v-else class="flex items-center justify-center gap-2 p-8 text-sm text-[var(--color-muted)]">
            <Loader2 class="h-4 w-4 animate-spin" aria-hidden="true" />
            Pobieranie grafiku dla sekcji...
          </div>
        </Card>
      </section>

      <Card v-if="aiProposal?.proposedSchedules?.length" padding="none" class="mt-6">
        <div class="flex flex-col gap-3 border-b border-[var(--color-border)] px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-primary)]">
              Propozycja AI
            </p>
            <h2 class="mt-1 text-lg font-semibold text-[var(--color-foreground)]">
              Nowa grupa grafików do akceptacji
            </h2>
            <p class="mt-1 text-sm text-[var(--color-foreground-muted)]">
              Porównaj stare i nowe wartości poniżej. Obecna grupa pozostaje bez zmian.
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            <Button
              variant="outline"
              :disabled="isAiApplying || isAiRejecting"
              :loading="isAiRejecting"
              @click="handleAiReject"
            >
              Odrzuć
            </Button>
            <Button
              :disabled="isAiApplying || isAiRejecting"
              :loading="isAiApplying"
              @click="handleAiApply"
            >
              <CheckCircle2 v-if="!isAiApplying" class="h-4 w-4" aria-hidden="true" />
              Zapisz jako nową grupę
            </Button>
          </div>
        </div>
        <div v-if="aiProposal.warnings?.length" class="border-b border-[var(--color-border)] px-4 py-3 text-sm text-[var(--color-warning-foreground)]">
          {{ aiProposal.warnings.join(' ') }}
        </div>
        <AiDiffReview
          :diff="aiProposal.diff"
          title="Co Gemini zmieniło w całej konfiguracji"
          description="Wspólna lista zmian ze wszystkich sekcji: pracownik, dzień, stara wartość oraz nowa propozycja przed zapisaniem nowej grupy."
        />

        <section class="space-y-6 p-4">
          <div>
            <h3 class="text-base font-semibold text-[var(--color-foreground)]">
              Podsumowanie wszystkich działów
            </h3>
            <p class="mt-1 text-sm text-[var(--color-foreground-muted)]">
              Ten widok pokazuje całkowite pokrycie sklepu przed zmianą i po propozycji Gemini.
            </p>
            <div class="mt-3 grid gap-4 xl:grid-cols-2">
              <StoreCoverageTable
                title="Obecne pokrycie sklepu"
                description="Aktualna obsada we wszystkich sekcjach."
                :days="days"
                tone="before"
              />
              <StoreCoverageTable
                title="Proponowane pokrycie sklepu"
                description="Obsada po zastosowaniu propozycji Gemini."
                :days="proposedStoreDays"
                tone="after"
              />
            </div>
          </div>

          <div>
            <h3 class="text-base font-semibold text-[var(--color-foreground)]">
              Porównanie sekcji
            </h3>
            <p class="mt-1 text-sm text-[var(--color-foreground-muted)]">
              Każda sekcja ma po lewej obecny grafik, a po prawej nową propozycję.
            </p>
            <div class="mt-3 space-y-6">
              <ScheduleSideBySideReview
                v-for="pair in proposalPairs"
                :key="`proposal-pair-${pair.scheduleId}`"
                :before-schedule="pair.before"
                :after-schedule="pair.after"
                :diff="aiProposal.diff"
                view-mode="table"
                :before-title="`Obecnie: ${pair.before.sectionName}`"
                :after-title="`Propozycja: ${pair.after.sectionName}`"
              />
            </div>
          </div>
        </section>
      </Card>
    </template>

    <ConfirmDialog
      :open="showRegenerateConfirm"
      title="Wygenerować ponownie?"
      description="Dotychczasowe wyniki tej grupy zostaną zastąpione. Konfiguracja i pracownicy pozostają bez zmian."
      confirm-label="Tak, generuj"
      cancel-label="Anuluj"
      variant="default"
      @update:open="showRegenerateConfirm = $event"
      @confirm="performRegenerate"
      @cancel="showRegenerateConfirm = false"
    />
  </PageContainer>
</template>
