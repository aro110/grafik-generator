<script setup>
import { WEEKDAY_OPTIONS } from '../../utils/creatorDraft'
import { useFieldValidation } from '../../composables/useFieldValidation'
import MonthMultiDayPicker from './MonthMultiDayPicker.vue'
import Card from '../ui/Card.vue'
import CardHeader from '../ui/CardHeader.vue'
import FormField from '../ui/FormField.vue'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'
import Switch from '../ui/Switch.vue'
import Alert from '../ui/Alert.vue'

const draft = defineModel({
  type: Object,
  required: true,
})

const { getError } = useFieldValidation()

const onTradingSundaysApplied = () => {
  const sundayHours = draft.value.calendar.storeHours.SUNDAY
  if (sundayHours && !sundayHours.enabled && draft.value.calendar.tradingSundays.length > 0) {
    sundayHours.enabled = true
    sundayHours.open = '09:00'
    sundayHours.close = '18:00'
  }
}

const setDayEnabled = (dayKey, value) => {
  const dayHours = draft.value.calendar.storeHours[dayKey]
  if (!dayHours) return
  dayHours.enabled = value
  if (value && dayHours.open === '00:00' && dayHours.close === '00:00') {
    dayHours.open = dayKey === 'SUNDAY' ? '09:00' : '08:00'
    dayHours.close = dayKey === 'SUNDAY' ? '18:00' : '20:00'
  }
}

const toggleVacationDay = (dayKey) => {
  if (!draft.value.vacationConfig) return
  const currentDays = draft.value.vacationConfig.workingDays || []
  if (currentDays.includes(dayKey)) {
    draft.value.vacationConfig.workingDays = currentDays.filter((d) => d !== dayKey)
  } else {
    draft.value.vacationConfig.workingDays = [...currentDays, dayKey]
  }
}
</script>

<template>
  <div class="space-y-6">
    <Card padding="md">
      <div class="grid gap-4 md:grid-cols-2">
        <FormField
          label="Nazwa konfiguracji"
          for="cfg-name"
          :error="getError('configName')"
          required
        >
          <Input
            id="cfg-name"
            v-model="draft.configName"
            placeholder="Np. Grafik sierpień 2026"
            :invalid="Boolean(getError('configName'))"
          />
        </FormField>

        <FormField
          label="Miesiąc docelowy"
          for="cfg-month"
          :error="getError('selectedMonth')"
          required
        >
          <Input
            id="cfg-month"
            v-model="draft.selectedMonth"
            type="month"
            :invalid="Boolean(getError('selectedMonth'))"
          />
        </FormField>
      </div>
    </Card>

    <Card padding="none">
      <CardHeader
        title="Konfiguracja urlopów"
        description="Ustal ile godzin i dni odlicza każdy dzień urlopu z miesięcznej puli pracownika."
      />
      <div class="grid gap-6 p-6 md:grid-cols-2">
        <div class="space-y-4">
          <label class="flex items-center gap-3">
            <input
              v-model="draft.vacationConfig.subtractHolidays"
              type="checkbox"
              class="h-4 w-4 rounded border-[var(--color-border-strong)] text-[var(--color-primary)] focus:ring-[var(--color-ring)]"
            />
            <span class="text-sm font-medium text-[var(--color-foreground)]">
              Pomiń święta w urlopie (nie odliczaj godzin)
            </span>
          </label>

          <FormField
            label="Godziny odliczane za 1 dzień urlopu"
            for="vac-hours"
            hint="Standardowo 8 godzin."
          >
            <Input
              id="vac-hours"
              v-model.number="draft.vacationConfig.hoursPerDay"
              type="number"
              min="1"
              max="24"
            />
          </FormField>
        </div>

        <div>
          <Label class="mb-2">Dni tygodnia, za które odliczamy</Label>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="day in WEEKDAY_OPTIONS"
              :key="day.key"
              type="button"
              class="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-ring)]"
              :class="
                (draft.vacationConfig.workingDays || []).includes(day.key)
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary)] text-[var(--color-primary-foreground)]'
                  : 'border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-foreground-muted)] hover:bg-[var(--color-surface-muted)]'
              "
              @click="toggleVacationDay(day.key)"
            >
              {{ day.shortLabel }}
            </button>
          </div>
        </div>
      </div>
    </Card>

    <Card padding="none">
      <CardHeader
        title="Godziny otwarcia"
        description="Włącz dni, w których sklep działa, i ustaw godziny otwarcia. Dla niedziel handlowych włącz niedzielę i zaznacz konkretne daty niżej."
      />
      <div class="divide-y divide-[var(--color-border)]">
        <div
          v-for="day in WEEKDAY_OPTIONS"
          :key="day.key"
          class="grid gap-4 px-6 py-4 md:grid-cols-[1.2fr_auto_1fr_1fr] md:items-center"
        >
          <div>
            <p class="text-sm font-medium text-[var(--color-foreground)]">{{ day.label }}</p>
            <p class="text-xs text-[var(--color-muted)]">{{ day.shortLabel }}</p>
            <p
              v-if="getError(`calendar.storeHours.${day.key}.enabled`)"
              class="mt-1 text-xs font-medium text-[var(--color-danger)]"
            >
              {{ getError(`calendar.storeHours.${day.key}.enabled`) }}
            </p>
          </div>

          <div class="flex items-center gap-2">
            <Switch
              :model-value="draft.calendar.storeHours[day.key].enabled"
              :id="`open-${day.key}`"
              @update:model-value="setDayEnabled(day.key, $event)"
            />
            <Label :for="`open-${day.key}`" class="!font-normal !text-[var(--color-foreground-muted)]">
              {{ draft.calendar.storeHours[day.key].enabled ? 'Otwarte' : 'Zamknięte' }}
            </Label>
          </div>

          <FormField
            label="Od"
            :for="`open-from-${day.key}`"
            :error="getError(`calendar.storeHours.${day.key}.open`)"
          >
            <Input
              :id="`open-from-${day.key}`"
              v-model="draft.calendar.storeHours[day.key].open"
              type="time"
              :disabled="!draft.calendar.storeHours[day.key].enabled"
              :invalid="Boolean(getError(`calendar.storeHours.${day.key}.open`))"
            />
          </FormField>

          <FormField
            label="Do"
            :for="`open-to-${day.key}`"
            :error="getError(`calendar.storeHours.${day.key}.close`)"
          >
            <Input
              :id="`open-to-${day.key}`"
              v-model="draft.calendar.storeHours[day.key].close"
              type="time"
              :disabled="!draft.calendar.storeHours[day.key].enabled"
              :invalid="Boolean(getError(`calendar.storeHours.${day.key}.close`))"
            />
          </FormField>
        </div>
      </div>
    </Card>

    <div class="grid gap-4 xl:grid-cols-2">
      <Card padding="none">
        <CardHeader
          title="Święta i dni zamknięte"
          description="Zaznacz wiele dni naraz, a następnie kliknij „Zastosuj zaznaczenie”."
        />
        <div class="p-6">
          <MonthMultiDayPicker
            v-model="draft.calendar.holidays"
            mode="any"
            :selected-month="draft.selectedMonth"
          />
          <p
            v-if="getError('calendar.holidays')"
            class="mt-2 text-xs font-medium text-[var(--color-danger)]"
          >
            {{ getError('calendar.holidays') }}
          </p>
        </div>
      </Card>

      <Card padding="none">
        <CardHeader
          title="Niedziele handlowe"
          description="Tylko niedziele, w których sklep ma być otwarty. Przy pierwszym zastosowaniu niedziela zostanie automatycznie włączona."
        />
        <div class="p-6">
          <MonthMultiDayPicker
            v-model="draft.calendar.tradingSundays"
            mode="sundayOnly"
            :selected-month="draft.selectedMonth"
            @applied="onTradingSundaysApplied"
          />
          <p
            v-if="getError('calendar.tradingSundays')"
            class="mt-2 text-xs font-medium text-[var(--color-danger)]"
          >
            {{ getError('calendar.tradingSundays') }}
          </p>
        </div>
      </Card>
    </div>

    <Alert variant="info" title="Wskazówka">
      Najlepiej ustaw pełny miesiąc, sekcję i godziny otwarcia już teraz — kolejne kroki będą
      korzystać dokładnie z tych danych przy walidacji i generowaniu grafiku.
    </Alert>
  </div>
</template>
