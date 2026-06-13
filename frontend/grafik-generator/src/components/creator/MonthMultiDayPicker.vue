<script setup>
import { computed, ref } from 'vue'
import { X } from 'lucide-vue-next'
import { buildMonthDays, mergeUniqueDates, removeDate } from '../../utils/creatorDraft'
import Button from '../ui/Button.vue'
import Alert from '../ui/Alert.vue'

const props = defineProps({
  mode: {
    type: String,
    default: 'any',
    validator: (value) => ['any', 'sundayOnly', 'employee'].includes(value),
  },
  selectedMonth: {
    type: String,
    default: '',
  },
  markedDates: {
    type: Array,
    default: () => [],
  },
})

const modelValue = defineModel({
  type: Array,
  default: () => [],
})

const emit = defineEmits(['applied'])

const pickerSelection = ref([])
const lastClickedDate = ref(null)
const showClearConfirm = ref(false)

const monthCells = computed(() => buildMonthDays(props.selectedMonth, props.mode))
const selectionCount = computed(() => pickerSelection.value.length)
const markedDateSet = computed(() => new Set(props.markedDates || []))

const isSelectedInPicker = (isoDate) => pickerSelection.value.includes(isoDate)
const isSaved = (isoDate) => modelValue.value.includes(isoDate)
const isMarked = (isoDate) => markedDateSet.value.has(isoDate)

const toggleDate = (cell, event) => {
  if (cell.empty || cell.disabled || !cell.isoDate) return
  const isoDate = cell.isoDate

  if (event?.shiftKey && lastClickedDate.value) {
    applyRangeSelection(lastClickedDate.value, isoDate)
    return
  }

  lastClickedDate.value = isoDate
  if (pickerSelection.value.includes(isoDate)) {
    pickerSelection.value = pickerSelection.value.filter((date) => date !== isoDate)
  } else {
    pickerSelection.value = [...pickerSelection.value, isoDate].sort()
  }
}

const toLocalIsoDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const applyRangeSelection = (startIso, endIso) => {
  const start = new Date(`${startIso}T12:00:00`)
  const end = new Date(`${endIso}T12:00:00`)
  const from = start <= end ? start : end
  const to = start <= end ? end : start
  const rangeDates = []
  const cursor = new Date(from)

  while (cursor <= to) {
    const iso = toLocalIsoDate(cursor)
    if (iso.startsWith(props.selectedMonth)) {
      const cell = monthCells.value.find((item) => item.isoDate === iso)
      if (cell && !cell.disabled) {
        rangeDates.push(iso)
      }
    }
    cursor.setDate(cursor.getDate() + 1)
  }

  pickerSelection.value = mergeUniqueDates(pickerSelection.value, rangeDates)
  lastClickedDate.value = endIso
}

const applySelection = () => {
  if (pickerSelection.value.length === 0) return
  modelValue.value = mergeUniqueDates(modelValue.value, pickerSelection.value)
  pickerSelection.value = []
  lastClickedDate.value = null
  emit('applied', [...modelValue.value])
}

const clearPickerSelection = () => {
  pickerSelection.value = []
  lastClickedDate.value = null
}

const requestClearAll = () => {
  if (modelValue.value.length === 0) return
  showClearConfirm.value = true
}

const performClearAll = () => {
  modelValue.value = []
  showClearConfirm.value = false
}

const removeSavedDate = (date) => {
  modelValue.value = removeDate(modelValue.value, date)
}

const cellClass = (cell) => {
  if (cell.empty) return 'invisible pointer-events-none'

  const base =
    'rounded-md border px-1 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-ring)]'

  if (cell.disabled) {
    return `${base} cursor-not-allowed border-[var(--color-border)] bg-[var(--color-surface-muted)] text-[var(--color-muted)] opacity-50`
  }
  if (isSelectedInPicker(cell.isoDate)) {
    return `${base} border-[var(--color-primary)] bg-[var(--color-primary)] text-[var(--color-primary-foreground)]`
  }
  if (isSaved(cell.isoDate)) {
    return `${base} border-[var(--color-success)] bg-[var(--color-success-soft)] text-[var(--color-success-foreground)]`
  }
  if (isMarked(cell.isoDate)) {
    return `${base} border-[var(--color-warning)]/50 bg-[var(--color-warning-soft)] text-[var(--color-warning-foreground)]`
  }
  return `${base} border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-foreground)] hover:border-[var(--color-primary)]/40 hover:bg-[var(--color-primary-soft)]/40`
}
</script>

<template>
  <div class="space-y-3">
    <Alert v-if="!selectedMonth" variant="warning" :icon="true">
      Najpierw wybierz miesiąc docelowy w kroku kalendarza.
    </Alert>

    <template v-else>
      <div
        class="grid grid-cols-7 gap-1 text-center text-[10px] font-semibold uppercase tracking-wide text-[var(--color-muted)]"
      >
        <span>Pn</span><span>Wt</span><span>Śr</span><span>Czw</span><span>Pt</span><span>Sob</span><span>Nd</span>
      </div>

      <div class="grid grid-cols-7 gap-1">
        <button
          v-for="(cell, index) in monthCells"
          :key="cell.isoDate || `empty-${index}`"
          type="button"
          :disabled="cell.empty || cell.disabled"
          :class="cellClass(cell)"
          :title="cell.disabled ? 'Tylko niedziele' : undefined"
          @click="toggleDate(cell, $event)"
        >
          <span v-if="!cell.empty">{{ cell.day }}</span>
        </button>
      </div>

      <div class="flex flex-wrap items-center gap-2 pt-1">
        <span class="text-xs text-[var(--color-foreground-muted)]">
          Zaznaczono: <strong class="text-[var(--color-foreground)]">{{ selectionCount }}</strong> dni
        </span>
        <div class="ml-auto flex flex-wrap gap-2">
          <Button size="sm" :disabled="selectionCount === 0" @click="applySelection">
            Zastosuj
          </Button>
          <Button variant="ghost" size="sm" @click="clearPickerSelection">
            Wyczyść zaznaczenie
          </Button>
          <Button
            variant="ghost"
            size="sm"
            :disabled="modelValue.length === 0"
            class="!text-[var(--color-danger)]"
            @click="requestClearAll"
          >
            Usuń wszystkie
          </Button>
        </div>
      </div>

      <p class="text-xs text-[var(--color-muted)]">
        <span v-if="mode === 'sundayOnly'">
          Możesz zaznaczać tylko niedziele. Shift+klik zaznacza ciągły zakres.
        </span>
        <span v-else>
          Kliknij wiele dni, potem „Zastosuj”. Shift+klik zaznacza ciągły zakres.
        </span>
      </p>
    </template>

    <div v-if="modelValue.length > 0" class="flex flex-wrap gap-1.5">
      <span
        v-for="date in modelValue"
        :key="date"
        class="inline-flex items-center gap-1.5 rounded-full border border-[var(--color-border-strong)] bg-[var(--color-surface-muted)] px-2.5 py-1 text-xs text-[var(--color-foreground)]"
      >
        {{ date }}
        <button
          type="button"
          class="-mr-1 rounded-full p-0.5 text-[var(--color-muted)] hover:bg-[var(--color-danger-soft)] hover:text-[var(--color-danger)]"
          :aria-label="`Usuń ${date}`"
          @click="removeSavedDate(date)"
        >
          <X class="h-3 w-3" aria-hidden="true" />
        </button>
      </span>
    </div>
    <p v-else-if="selectedMonth" class="text-xs text-[var(--color-muted)]">
      Brak zapisanych dat.
    </p>

    <Teleport to="body">
      <div
        v-if="showClearConfirm"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
        @click.self="showClearConfirm = false"
      >
        <div
          class="w-full max-w-sm rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-lg"
        >
          <h3 class="text-base font-semibold text-[var(--color-foreground)]">Usunąć wszystkie?</h3>
          <p class="mt-2 text-sm text-[var(--color-foreground-muted)]">
            Wszystkie zapisane daty zostaną usunięte.
          </p>
          <div class="mt-4 flex justify-end gap-2">
            <Button variant="outline" size="sm" @click="showClearConfirm = false">Anuluj</Button>
            <Button variant="destructive" size="sm" @click="performClearAll">Usuń wszystkie</Button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
