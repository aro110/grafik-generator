<script setup>
import { computed, ref } from 'vue'
import { Plus, Trash2, Users, Wand2, Settings2, Briefcase, CalendarOff } from 'lucide-vue-next'
import { createEmptyEmployee, createEmptySection } from '../../utils/creatorDraft'
import { useFieldValidation } from '../../composables/useFieldValidation'
import MonthMultiDayPicker from './MonthMultiDayPicker.vue'
import Card from '../ui/Card.vue'
import CardHeader from '../ui/CardHeader.vue'
import Button from '../ui/Button.vue'
import Badge from '../ui/Badge.vue'
import FormField from '../ui/FormField.vue'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'
import Alert from '../ui/Alert.vue'

const draft = defineModel({
  type: Object,
  required: true,
})

const { getError } = useFieldValidation()

const calendarMarkedDates = computed(() => draft.value.calendar?.holidays || [])

const bulkGenerate = (sectionIndex) => {
  const countStr = prompt('Ilu pracowników chcesz wygenerować?')
  const count = parseInt(countStr, 10)
  if (Number.isNaN(count) || count <= 0) return

  const employees = []
  for (let i = 1; i <= count; i += 1) {
    const emp = createEmptyEmployee()
    emp.name = 'Pracownik'
    emp.surname = i.toString()
    employees.push(emp)
  }
  draft.value.sections[sectionIndex].employees = employees
}

const applyToAll = (sectionIndex) => {
  const hoursStr = prompt('Wpisz nową wartość godzin miesięcznych dla wszystkich:')
  const daysStr = prompt('Wpisz nową wartość dni pracy dla wszystkich:')
  const hours = parseInt(hoursStr, 10)
  const days = parseInt(daysStr, 10)

  if (!Number.isNaN(hours) && hours >= 0) {
    draft.value.sections[sectionIndex].employees.forEach((emp) => {
      emp.totalHours = hours
    })
  }
  if (!Number.isNaN(days) && days > 0) {
    draft.value.sections[sectionIndex].employees.forEach((emp) => {
      emp.totalDays = days
    })
  }
}

const getEffectiveHoursAndDays = (employee) => {
  let effectiveHours = Number(employee.totalHours)
  let effectiveDays = Number(employee.totalDays)
  const config = draft.value.vacationConfig
  if (!config) return { h: effectiveHours, d: effectiveDays }
  const vacations = employee.vacations || []
  if (vacations.length === 0) return { h: effectiveHours, d: effectiveDays }

  const [year, month] = draft.value.selectedMonth.split('-').map(Number)
  if (!year || !month) return { h: effectiveHours, d: effectiveDays }
  const holidays = draft.value.calendar?.holidays || []

  vacations.forEach((dateStr) => {
    const dateObj = new Date(dateStr)
    const dow = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][
      dateObj.getDay()
    ]
    if ((config.workingDays || []).includes(dow)) {
      if (config.subtractHolidays && holidays.includes(dateStr)) return
      effectiveHours -= Number(config.hoursPerDay || 8)
      effectiveDays -= 1
    }
  })

  return { h: effectiveHours, d: effectiveDays }
}

const addSection = () => {
  draft.value.sections.push(createEmptySection(draft.value.sections.length + 1))
}

const removeSection = (sectionIndex) => {
  if (draft.value.sections.length === 1) return
  draft.value.sections.splice(sectionIndex, 1)
}

const addEmployee = (sectionIndex) => {
  draft.value.sections[sectionIndex].employees.push(createEmptyEmployee())
}

const removeEmployee = (sectionIndex, employeeIndex) => {
  if (draft.value.sections[sectionIndex].employees.length === 1) return
  draft.value.sections[sectionIndex].employees.splice(employeeIndex, 1)
}

const expandedEmployeeKeys = ref(new Set())

const toggleEmployeeExpanded = (key) => {
  if (expandedEmployeeKeys.value.has(key)) {
    expandedEmployeeKeys.value.delete(key)
  } else {
    expandedEmployeeKeys.value.add(key)
  }
  expandedEmployeeKeys.value = new Set(expandedEmployeeKeys.value)
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between gap-3">
      <p class="text-sm text-[var(--color-foreground-muted)]">
        Liczba sekcji:
        <Badge variant="neutral">{{ draft.sections.length }}</Badge>
      </p>
      <Button @click="addSection">
        <Plus class="h-4 w-4" aria-hidden="true" />
        Dodaj sekcję
      </Button>
    </div>

    <div class="space-y-4">
      <Card v-for="(section, sectionIndex) in draft.sections" :key="sectionIndex" padding="none">
        <CardHeader>
          <template #title>
            <div class="flex items-center gap-2">
              <Briefcase class="h-4 w-4 text-[var(--color-primary)]" aria-hidden="true" />
              <span class="text-base font-semibold text-[var(--color-foreground)]">
                Sekcja {{ sectionIndex + 1 }}
              </span>
              <Badge variant="neutral">{{ section.employees.length }} prac.</Badge>
            </div>
          </template>
          <template #actions>
            <Button
              variant="ghost"
              size="sm"
              :disabled="draft.sections.length === 1"
              :aria-label="`Usuń sekcję ${sectionIndex + 1}`"
              @click="removeSection(sectionIndex)"
            >
              <Trash2 class="h-4 w-4 text-[var(--color-danger)]" aria-hidden="true" />
            </Button>
          </template>
        </CardHeader>

        <div class="space-y-4 p-6">
          <FormField
            label="Nazwa sekcji"
            :for="`section-name-${sectionIndex}`"
            :error="getError(`sections.${sectionIndex}.name`)"
            required
          >
            <Input
              :id="`section-name-${sectionIndex}`"
              v-model="section.name"
              placeholder="Np. Kasa, Magazyn, Obsługa"
              :invalid="Boolean(getError(`sections.${sectionIndex}.name`))"
            />
          </FormField>

          <div class="flex flex-wrap items-center justify-between gap-2 border-t border-[var(--color-border)] pt-4">
            <div class="flex items-center gap-2 text-sm text-[var(--color-foreground-muted)]">
              <Users class="h-4 w-4" aria-hidden="true" />
              <span>Pracownicy ({{ section.employees.length }})</span>
            </div>
            <div class="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" @click="applyToAll(sectionIndex)">
                <Settings2 class="h-4 w-4" aria-hidden="true" />
                Zmień etaty
              </Button>
              <Button variant="outline" size="sm" @click="bulkGenerate(sectionIndex)">
                <Wand2 class="h-4 w-4" aria-hidden="true" />
                Wygeneruj listę
              </Button>
              <Button variant="subtle" size="sm" @click="addEmployee(sectionIndex)">
                <Plus class="h-4 w-4" aria-hidden="true" />
                Dodaj pracownika
              </Button>
            </div>
          </div>

          <div class="space-y-3">
            <div
              v-for="(employee, employeeIndex) in section.employees"
              :key="employeeIndex"
              class="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)]/50 p-4 space-y-4"
            >
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="text-sm font-semibold text-[var(--color-foreground)]">
                    Pracownik {{ employeeIndex + 1 }}
                  </span>
                  <Badge
                    v-if="employee.vacations && employee.vacations.length > 0"
                    variant="info"
                  >
                    Do zaplanowania:
                    {{ getEffectiveHoursAndDays(employee).h }}h w
                    {{ getEffectiveHoursAndDays(employee).d }} dni
                  </Badge>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  :disabled="section.employees.length === 1"
                  :aria-label="`Usuń pracownika ${employeeIndex + 1}`"
                  @click="removeEmployee(sectionIndex, employeeIndex)"
                >
                  <Trash2 class="h-4 w-4 text-[var(--color-danger)]" aria-hidden="true" />
                </Button>
              </div>

              <div class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                <FormField
                  label="Imię"
                  :for="`emp-name-${sectionIndex}-${employeeIndex}`"
                  :error="getError(`sections.${sectionIndex}.employees.${employeeIndex}.name`)"
                  required
                >
                  <Input
                    :id="`emp-name-${sectionIndex}-${employeeIndex}`"
                    v-model="employee.name"
                    placeholder="Np. Jan"
                    :invalid="Boolean(getError(`sections.${sectionIndex}.employees.${employeeIndex}.name`))"
                  />
                </FormField>
                <FormField
                  label="Nazwisko"
                  :for="`emp-surname-${sectionIndex}-${employeeIndex}`"
                  :error="getError(`sections.${sectionIndex}.employees.${employeeIndex}.surname`)"
                  required
                >
                  <Input
                    :id="`emp-surname-${sectionIndex}-${employeeIndex}`"
                    v-model="employee.surname"
                    placeholder="Np. Kowalski"
                    :invalid="Boolean(getError(`sections.${sectionIndex}.employees.${employeeIndex}.surname`))"
                  />
                </FormField>
                <FormField
                  label="Liczba godzin"
                  :for="`emp-hours-${sectionIndex}-${employeeIndex}`"
                  :error="getError(`sections.${sectionIndex}.employees.${employeeIndex}.totalHours`)"
                >
                  <Input
                    :id="`emp-hours-${sectionIndex}-${employeeIndex}`"
                    v-model.number="employee.totalHours"
                    type="number"
                    min="0"
                    step="1"
                    :invalid="Boolean(getError(`sections.${sectionIndex}.employees.${employeeIndex}.totalHours`))"
                  />
                </FormField>
                <FormField
                  label="Liczba dni"
                  :for="`emp-days-${sectionIndex}-${employeeIndex}`"
                  :error="getError(`sections.${sectionIndex}.employees.${employeeIndex}.totalDays`)"
                >
                  <Input
                    :id="`emp-days-${sectionIndex}-${employeeIndex}`"
                    v-model.number="employee.totalDays"
                    type="number"
                    min="1"
                    step="1"
                    :invalid="Boolean(getError(`sections.${sectionIndex}.employees.${employeeIndex}.totalDays`))"
                  />
                </FormField>
              </div>

              <div>
                <button
                  type="button"
                  class="text-xs font-medium text-[var(--color-primary)] hover:underline"
                  @click="toggleEmployeeExpanded(`${sectionIndex}-${employeeIndex}`)"
                >
                  {{ expandedEmployeeKeys.has(`${sectionIndex}-${employeeIndex}`) ? 'Ukryj' : 'Pokaż' }}
                  urlopy i dni wolne
                </button>
              </div>

              <div
                v-if="expandedEmployeeKeys.has(`${sectionIndex}-${employeeIndex}`)"
                class="grid gap-4 border-t border-[var(--color-border)] pt-4 lg:grid-cols-2"
              >
                <div class="space-y-2 rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] p-3">
                  <Label>
                    <span class="inline-flex items-center gap-2">
                      <CalendarOff class="h-4 w-4 text-[var(--color-warning-foreground)]" aria-hidden="true" />
                      Urlopy
                    </span>
                  </Label>
                  <MonthMultiDayPicker
                    v-model="employee.vacations"
                    mode="employee"
                    :selected-month="draft.selectedMonth"
                    :marked-dates="calendarMarkedDates"
                  />
                </div>

                <div class="space-y-2 rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] p-3">
                  <Label>
                    <span class="inline-flex items-center gap-2">
                      <CalendarOff class="h-4 w-4 text-[var(--color-muted)]" aria-hidden="true" />
                      Dni wolne (W)
                    </span>
                  </Label>
                  <MonthMultiDayPicker
                    v-model="employee.daysOff"
                    mode="employee"
                    :selected-month="draft.selectedMonth"
                    :marked-dates="calendarMarkedDates"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <Alert variant="info" title="Wskazówka">
      Skonfiguruj wszystkie sekcje i pracowników teraz — kolejne kroki bazują na tych danych przy
      walidacji puli godzin/dni i sprawdzaniu, czy grafik jest możliwy do ułożenia.
    </Alert>
  </div>
</template>
