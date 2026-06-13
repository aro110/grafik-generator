<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Users,
  Target,
  ListChecks,
  Sparkles,
  Sliders,
  ClipboardCheck,
  ChevronLeft,
  ChevronRight,
  PlayCircle,
  Trash2,
  X,
} from 'lucide-vue-next'

import StepCalendar from '../components/creator/StepCalendarForm.vue'
import StepEmployees from '../components/creator/StepEmployees.vue'
import StepGoals from '../components/creator/StepGoals.vue'
import StepRules from '../components/creator/StepRules.vue'
import StepWeights from '../components/creator/StepWeights.vue'
import StepParams from '../components/creator/StepParams.vue'
import StepReview from '../components/creator/StepReview.vue'
import CreatorSidebar from '../components/creator/CreatorSidebar.vue'

import PageContainer from '../components/layout/PageContainer.vue'
import Card from '../components/ui/Card.vue'
import Button from '../components/ui/Button.vue'
import Alert from '../components/ui/Alert.vue'
import Progress from '../components/ui/Progress.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'

import { pollGenerationUntilFinished, submitCreatorDraft } from '../services/creatorService'
import { createDefaultDraft, normalizeDraft, validateDraft } from '../utils/creatorDraft'
import { provideValidation } from '../composables/useFieldValidation'

const router = useRouter()

const steps = [
  { id: 1, title: 'Kalendarz', description: 'Miesiąc i godziny', icon: Calendar, component: StepCalendar },
  { id: 2, title: 'Pracownicy', description: 'Sekcje i dostępność', icon: Users, component: StepEmployees },
  { id: 3, title: 'Cele obsady', description: 'Targety i peak hours', icon: Target, component: StepGoals },
  { id: 4, title: 'Reguły', description: 'Długości zmian i limity', icon: ListChecks, component: StepRules },
  { id: 5, title: 'Priorytet', description: 'Preset optymalizacji', icon: Sparkles, component: StepWeights },
  { id: 6, title: 'Parametry GA', description: 'Ustawienia algorytmu', icon: Sliders, component: StepParams },
  { id: 7, title: 'Podsumowanie', description: 'Walidacja i start', icon: ClipboardCheck, component: StepReview },
]

const DRAFT_KEY = 'grafik_generator_draft_v2'

const currentStepIndex = ref(0)
const draftConfig = ref(createDefaultDraft())
const isSubmitting = ref(false)
const submitMessage = ref('')
const submitError = ref('')
const generationProgress = ref(0)
const showResetConfirm = ref(false)
const showCancelConfirm = ref(false)

const validation = computed(() => validateDraft(draftConfig.value))
provideValidation(validation)

const currentStep = computed(() => steps[currentStepIndex.value])
const isLastStep = computed(() => currentStepIndex.value === steps.length - 1)

watch(
  draftConfig,
  (newValue) => {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(newValue))
  },
  { deep: true },
)

watch(
  () => steps[currentStepIndex.value]?.id,
  () => {
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  },
)

onMounted(() => {
  const savedDraft = localStorage.getItem(DRAFT_KEY)
  if (!savedDraft) {
    draftConfig.value = createDefaultDraft()
    return
  }
  try {
    draftConfig.value = normalizeDraft(JSON.parse(savedDraft))
  } catch (error) {
    console.error('Nie udało się wczytać draftu kreatora.', error)
    draftConfig.value = createDefaultDraft()
  }
})

const nextStep = () => {
  if (currentStepIndex.value < steps.length - 1) {
    currentStepIndex.value += 1
    submitError.value = ''
    submitMessage.value = ''
  }
}

const prevStep = () => {
  if (currentStepIndex.value > 0) {
    currentStepIndex.value -= 1
    submitError.value = ''
    submitMessage.value = ''
  }
}

const goToStep = (index) => {
  if (isSubmitting.value) return
  currentStepIndex.value = index
}

const requestReset = () => {
  showResetConfirm.value = true
}

const performReset = () => {
  draftConfig.value = createDefaultDraft()
  localStorage.removeItem(DRAFT_KEY)
  currentStepIndex.value = 0
  showResetConfirm.value = false
}

const requestCancel = () => {
  if (isSubmitting.value) return
  showCancelConfirm.value = true
}

const performCancel = () => {
  showCancelConfirm.value = false
  router.push('/')
}

const submit = async () => {
  submitError.value = ''
  submitMessage.value = ''
  generationProgress.value = 0

  const result = validateDraft(draftConfig.value)
  if (!result.isValid) {
    submitError.value =
      'Konfiguracja zawiera błędy. Popraw je w podsumowaniu przed uruchomieniem generacji.'
    currentStepIndex.value = steps.length - 1
    return
  }

  isSubmitting.value = true

  try {
    submitMessage.value = 'Tworzenie sekcji, pracowników i konfiguracji grafiku...'
    const created = await submitCreatorDraft(draftConfig.value)

    const groupId = created?.generationRunGroup?.id
    if (!groupId) {
      throw new Error('Backend nie zwrócił identyfikatora grupy generacji.')
    }

    submitMessage.value = 'Generowanie grafiku zostało uruchomione. Oczekiwanie na wynik...'

    await pollGenerationUntilFinished(groupId, {
      onProgress: (info) => {
        generationProgress.value = info?.progress ?? 0
        submitMessage.value = `Trwa generowanie grafiku... (${generationProgress.value}%)`
      },
    })

    generationProgress.value = 100
    submitMessage.value = 'Generowanie zakończyło się sukcesem.'

    router.push(`/summary/${groupId}`)
  } catch (error) {
    submitError.value =
      error?.message ||
      'Wystąpił błąd podczas zapisywania danych lub generowania grafiku.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <PageContainer width="wide" title="Kreator grafiku" description="Skonfiguruj parametry, a algorytm wygeneruje grafik bez opuszczania kreatora.">
    <template #actions>
      <Button variant="ghost" size="sm" :disabled="isSubmitting" @click="requestCancel">
        <X class="h-4 w-4" aria-hidden="true" />
        Anuluj
      </Button>
      <Button variant="ghost" size="sm" :disabled="isSubmitting" @click="requestReset">
        <Trash2 class="h-4 w-4" aria-hidden="true" />
        Wyczyść draft
      </Button>
    </template>

    <div class="grid gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
      <aside class="lg:sticky lg:top-20 lg:self-start">
        <Card padding="md">
          <CreatorSidebar
            :steps="steps"
            :current-step-index="currentStepIndex"
            :disabled="isSubmitting"
            @select="goToStep"
          />
        </Card>
      </aside>

      <div class="min-w-0 space-y-4">
        <Card padding="none">
          <div class="flex items-center gap-3 border-b border-[var(--color-border)] px-6 py-4">
            <component
              :is="currentStep.icon"
              class="h-5 w-5 text-[var(--color-primary)]"
              aria-hidden="true"
            />
            <div class="min-w-0">
              <p class="text-xs font-medium uppercase tracking-wide text-[var(--color-muted)]">
                Krok {{ currentStep.id }} z {{ steps.length }}
              </p>
              <h2 class="text-base font-semibold text-[var(--color-foreground)]">
                {{ currentStep.title }}
              </h2>
            </div>
          </div>

          <div class="px-6 py-6">
            <component
              :is="currentStep.component"
              v-model="draftConfig"
              @go-to-step="goToStep"
            />
          </div>

          <div class="border-t border-[var(--color-border)] bg-[var(--color-surface-muted)]/40 px-6 py-4">
            <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p class="text-xs text-[var(--color-foreground-muted)]">
                Draft zapisuje się automatycznie ·
                <span class="font-semibold text-[var(--color-foreground)]">{{ validation.errors.length }}</span>
                {{ validation.errors.length === 1 ? 'błąd' : 'błędów' }} ·
                <span class="font-semibold text-[var(--color-foreground)]">{{ validation.warnings.length }}</span>
                ostrz.
              </p>

              <div class="flex items-center gap-2">
                <Button
                  v-if="currentStepIndex > 0"
                  variant="outline"
                  :disabled="isSubmitting"
                  @click="prevStep"
                >
                  <ChevronLeft class="h-4 w-4" aria-hidden="true" />
                  Wstecz
                </Button>
                <Button v-if="!isLastStep" :disabled="isSubmitting" @click="nextStep">
                  Dalej
                  <ChevronRight class="h-4 w-4" aria-hidden="true" />
                </Button>
                <Button
                  v-else
                  :loading="isSubmitting"
                  :disabled="isSubmitting"
                  @click="submit"
                >
                  <PlayCircle v-if="!isSubmitting" class="h-4 w-4" aria-hidden="true" />
                  {{ isSubmitting ? 'Trwa generowanie...' : 'Rozpocznij generowanie' }}
                </Button>
              </div>
            </div>
          </div>
        </Card>

        <Alert v-if="submitMessage" variant="info" :title="isSubmitting ? 'Generowanie w toku' : 'Status'">
          <p>{{ submitMessage }}</p>
          <Progress
            v-if="isSubmitting"
            :value="generationProgress"
            :show-value="true"
            label="Postęp generacji"
            class="mt-3"
          />
        </Alert>

        <Alert v-if="submitError" variant="danger" title="Wystąpił błąd">
          {{ submitError }}
        </Alert>
      </div>
    </div>

    <ConfirmDialog
      :open="showResetConfirm"
      title="Wyczyścić draft?"
      description="Wszystkie wprowadzone dane zostaną usunięte i wrócisz do pierwszego kroku."
      confirm-label="Wyczyść"
      cancel-label="Anuluj"
      variant="destructive"
      @update:open="showResetConfirm = $event"
      @confirm="performReset"
      @cancel="showResetConfirm = false"
    />

    <ConfirmDialog
      :open="showCancelConfirm"
      title="Wyjść z kreatora?"
      description="Wersja robocza zostanie zachowana i będzie dostępna po powrocie do kreatora."
      confirm-label="Wyjdź do dashboardu"
      cancel-label="Zostań w kreatorze"
      variant="default"
      @update:open="showCancelConfirm = $event"
      @confirm="performCancel"
      @cancel="showCancelConfirm = false"
    />
  </PageContainer>
</template>
