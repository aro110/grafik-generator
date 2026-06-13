<script setup>
import { ref } from 'vue'
import { Plus, X } from 'lucide-vue-next'
import { addShiftLength, removeShiftLength } from '../../utils/creatorDraft'
import { useFieldValidation } from '../../composables/useFieldValidation'
import Card from '../ui/Card.vue'
import CardHeader from '../ui/CardHeader.vue'
import FormField from '../ui/FormField.vue'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'
import Button from '../ui/Button.vue'
import Alert from '../ui/Alert.vue'

const draft = defineModel({
  type: Object,
  required: true,
})

const { getError } = useFieldValidation()

const newShiftLength = ref('')
const recommendedShiftLengths = [4, 6, 8, 10, 12]

const addCustomShiftLength = () => {
  draft.value.rules.shiftLengths = addShiftLength(
    draft.value.rules.shiftLengths,
    newShiftLength.value,
  )
  newShiftLength.value = ''
}

const addRecommendedShiftLength = (value) => {
  draft.value.rules.shiftLengths = addShiftLength(draft.value.rules.shiftLengths, value)
}

const removeLength = (value) => {
  draft.value.rules.shiftLengths = removeShiftLength(draft.value.rules.shiftLengths, value)
}
</script>

<template>
  <div class="space-y-6">
    <Card padding="none">
      <CardHeader
        title="Długości zmian"
        description="Dodaj wszystkie dozwolone długości zmian w godzinach, np. 6h i 8h."
      />

      <div class="space-y-4 p-6">
        <div>
          <Label class="mb-2">Sugerowane</Label>
          <div class="flex flex-wrap gap-2">
            <Button
              v-for="length in recommendedShiftLengths"
              :key="length"
              variant="outline"
              size="sm"
              @click="addRecommendedShiftLength(length)"
            >
              <Plus class="h-3.5 w-3.5" aria-hidden="true" />
              {{ length }}h
            </Button>
          </div>
        </div>

        <div class="flex flex-wrap items-end gap-3">
          <FormField label="Własna długość (h)" for="custom-shift" class="flex-1 max-w-xs">
            <Input
              id="custom-shift"
              v-model="newShiftLength"
              type="number"
              min="1"
              step="1"
              placeholder="np. 7"
            />
          </FormField>
          <Button variant="outline" @click="addCustomShiftLength">
            <Plus class="h-4 w-4" aria-hidden="true" />
            Dodaj
          </Button>
        </div>

        <div v-if="draft.rules.shiftLengths.length > 0">
          <Label class="mb-2">Aktywne długości zmian</Label>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="length in draft.rules.shiftLengths"
              :key="length"
              class="inline-flex items-center gap-1.5 rounded-full bg-[var(--color-primary-soft)] px-3 py-1 text-xs font-medium text-[var(--color-primary)]"
            >
              {{ length }}h
              <button
                type="button"
                class="-mr-1 rounded-full p-0.5 hover:bg-[var(--color-danger-soft)] hover:text-[var(--color-danger)]"
                :aria-label="`Usuń ${length}h`"
                @click="removeLength(length)"
              >
                <X class="h-3 w-3" aria-hidden="true" />
              </button>
            </span>
          </div>
        </div>

        <p
          v-if="getError('rules.shiftLengths')"
          class="text-xs font-medium text-[var(--color-danger)]"
        >
          {{ getError('rules.shiftLengths') }}
        </p>
      </div>
    </Card>

    <Card padding="none">
      <CardHeader title="Ograniczenia pracy" description="Limity stosowane podczas generacji." />

      <div class="grid gap-6 p-6 md:grid-cols-2">
        <FormField
          label="Maksymalna liczba dni pracy z rzędu"
          for="max-days"
          hint="Po przekroczeniu tej wartości algorytm unika dalszych przydziałów z rzędu."
          :error="getError('rules.maxWorkingDaysInARow')"
        >
          <Input
            id="max-days"
            v-model.number="draft.rules.maxWorkingDaysInARow"
            type="number"
            min="1"
            step="1"
            :invalid="Boolean(getError('rules.maxWorkingDaysInARow'))"
          />
        </FormField>

        <FormField
          label="Limit osób na ten sam start zmiany"
          for="max-people"
          hint="0 = brak limitu."
          :error="getError('rules.maxPeoplePerShiftStart')"
        >
          <Input
            id="max-people"
            v-model.number="draft.rules.maxPeoplePerShiftStart"
            type="number"
            min="0"
            step="1"
            :invalid="Boolean(getError('rules.maxPeoplePerShiftStart'))"
          />
        </FormField>

        <label
          class="flex items-start gap-3 rounded-md border border-[var(--color-border)] bg-[var(--color-surface-muted)]/50 p-4 md:col-span-2"
        >
          <input
            v-model="draft.rules.grantFreeWeekend"
            type="checkbox"
            class="mt-1 h-4 w-4 rounded border-[var(--color-border-strong)] text-[var(--color-primary)] focus:ring-[var(--color-ring)]"
          />
          <span>
            <span class="block text-sm font-medium text-[var(--color-foreground)]">
              Przyznawaj wolny weekend
            </span>
            <span class="mt-1 block text-sm text-[var(--color-foreground-muted)]">
              Jeśli włączone, algorytm spróbuje przydzielić każdemu pracownikowi przynajmniej jeden
              wolny weekend.
            </span>
          </span>
        </label>
      </div>
    </Card>

    <Alert variant="info" title="Rekomendacja">
      Najbezpieczniejszy startowy zestaw: zmiany <strong>6h i 8h</strong>,
      max <strong>5 dni</strong> pracy z rzędu, limit <strong>2 osoby</strong> na start zmiany.
    </Alert>
  </div>
</template>
