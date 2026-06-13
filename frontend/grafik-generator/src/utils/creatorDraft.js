export const WEEKDAY_OPTIONS = [
  { key: 'MONDAY', label: 'Poniedziałek', shortLabel: 'Pon' },
  { key: 'TUESDAY', label: 'Wtorek', shortLabel: 'Wt' },
  { key: 'WEDNESDAY', label: 'Środa', shortLabel: 'Śr' },
  { key: 'THURSDAY', label: 'Czwartek', shortLabel: 'Czw' },
  { key: 'FRIDAY', label: 'Piątek', shortLabel: 'Pt' },
  { key: 'SATURDAY', label: 'Sobota', shortLabel: 'Sob' },
  { key: 'SUNDAY', label: 'Niedziela', shortLabel: 'Nd' },
]

export const OPTIMIZATION_PRESETS = [
  {
    key: 'balanced',
    label: 'Zbalansowany',
    description: 'Uniwersalne ustawienia do codziennego użycia.',
  },
  {
    key: 'coverage',
    label: 'Pokrycie obsady',
    description: 'Priorytet dla lepszego pokrycia obsady i peak hours.',
  },
  {
    key: 'fairness',
    label: 'Równomierny rozkład',
    description: 'Priorytet dla bardziej równego rozłożenia pracy między pracowników.',
  },
  {
    key: 'fast',
    label: 'Szybka generacja',
    description: 'Mniejsza liczba iteracji dla szybszego wyniku testowego.',
  },
]

const DEFAULT_STORE_HOURS = {
  MONDAY: { enabled: true, open: '08:00', close: '20:00' },
  TUESDAY: { enabled: true, open: '08:00', close: '20:00' },
  WEDNESDAY: { enabled: true, open: '08:00', close: '20:00' },
  THURSDAY: { enabled: true, open: '08:00', close: '20:00' },
  FRIDAY: { enabled: true, open: '08:00', close: '20:00' },
  SATURDAY: { enabled: true, open: '09:00', close: '18:00' },
  SUNDAY: { enabled: false, open: '00:00', close: '00:00' },
}

export const STAFFING_TARGET_MODES = {
  PERCENT: 'PERCENT',
  COUNT: 'COUNT',
}

const DEFAULT_STAFFING_TARGETS = {
  MONDAY: { mode: 'PERCENT', percent: 60, count: 0, peakEnabled: true, peakStart: '12:00', peakEnd: '18:00' },
  TUESDAY: { mode: 'PERCENT', percent: 60, count: 0, peakEnabled: true, peakStart: '12:00', peakEnd: '18:00' },
  WEDNESDAY: { mode: 'PERCENT', percent: 60, count: 0, peakEnabled: true, peakStart: '12:00', peakEnd: '18:00' },
  THURSDAY: { mode: 'PERCENT', percent: 60, count: 0, peakEnabled: true, peakStart: '12:00', peakEnd: '18:00' },
  FRIDAY: { mode: 'PERCENT', percent: 70, count: 0, peakEnabled: true, peakStart: '12:00', peakEnd: '19:00' },
  SATURDAY: { mode: 'PERCENT', percent: 75, count: 0, peakEnabled: true, peakStart: '10:00', peakEnd: '16:00' },
  SUNDAY: { mode: 'PERCENT', percent: 0, count: 0, peakEnabled: false, peakStart: '00:00', peakEnd: '00:00' },
}

const DEFAULT_SHIFT_RULES = {
  shiftLengths: [6, 8],
  maxWorkingDaysInARow: 5,
  grantFreeWeekend: true,
  maxPeoplePerShiftStart: 2,
}

const DEFAULT_GA_PARAMETERS = {
  populationSize: 300,
  generations: 5000,
  eliteCount: 5,
  tournamentSize: 3,
  mutationRate: 0.03,
  seed: '',
}

const DEFAULT_EMPLOYEE = {
  id: null,
  name: '',
  surname: '',
  totalHours: 160,
  totalDays: 20,
  daysOff: [],
  vacations: [],
}

const isPlainObject = (value) => value !== null && typeof value === 'object' && !Array.isArray(value)

const clone = (value) => structuredClone(value)

const buildStoreHours = (source = {}) =>
  WEEKDAY_OPTIONS.reduce((acc, day) => {
    const existing = source[day.key]
    acc[day.key] = {
      enabled:
        typeof existing?.enabled === 'boolean'
          ? existing.enabled
          : DEFAULT_STORE_HOURS[day.key].enabled,
      open: existing?.open || DEFAULT_STORE_HOURS[day.key].open,
      close: existing?.close || DEFAULT_STORE_HOURS[day.key].close,
    }
    return acc
  }, {})

const buildStaffingTargets = (source = {}) =>
  WEEKDAY_OPTIONS.reduce((acc, day) => {
    const existing = source[day.key]
    acc[day.key] = {
      mode:
        existing?.mode === 'COUNT' || existing?.mode === 'PERCENT'
          ? existing.mode
          : DEFAULT_STAFFING_TARGETS[day.key].mode,
      percent:
        typeof existing?.percent === 'number'
          ? existing.percent
          : DEFAULT_STAFFING_TARGETS[day.key].percent,
      count:
        typeof existing?.count === 'number'
          ? existing.count
          : DEFAULT_STAFFING_TARGETS[day.key].count,
      peakEnabled:
        typeof existing?.peakEnabled === 'boolean'
          ? existing.peakEnabled
          : DEFAULT_STAFFING_TARGETS[day.key].peakEnabled,
      peakStart: existing?.peakStart || DEFAULT_STAFFING_TARGETS[day.key].peakStart,
      peakEnd: existing?.peakEnd || DEFAULT_STAFFING_TARGETS[day.key].peakEnd,
    }
    return acc
  }, {})

const buildEmployee = (source = {}) => ({
  id: source.id ?? null,
  name: source.name || '',
  surname: source.surname || '',
  totalHours:
    typeof source.totalHours === 'number' ? source.totalHours : DEFAULT_EMPLOYEE.totalHours,
  totalDays: typeof source.totalDays === 'number' ? source.totalDays : DEFAULT_EMPLOYEE.totalDays,
  daysOff: Array.isArray(source.daysOff) ? [...source.daysOff] : [],
  vacations: Array.isArray(source.vacations) ? [...source.vacations] : [],
})

export const createDefaultDraft = () => ({
  configName: 'Grafik miesięczny',
  selectedMonth: '',
  sections: [{ id: 1, name: '', employees: [clone(DEFAULT_EMPLOYEE)] }],
  vacationConfig: {
    workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
    hoursPerDay: 8,
    subtractHolidays: true,
  },
  calendar: {
    holidays: [],
    tradingSundays: [],
    storeHours: clone(DEFAULT_STORE_HOURS),
  },
  goals: {
    staffingTargets: clone(DEFAULT_STAFFING_TARGETS),
  },
  rules: clone(DEFAULT_SHIFT_RULES),
  weights: {
    preset: 'balanced',
  },
  params: clone(DEFAULT_GA_PARAMETERS),
})

export const normalizeDraft = (source) => {
  const draft = createDefaultDraft()

  if (!isPlainObject(source)) {
    return draft
  }

  return {
    configName: source.configName || draft.configName,
    selectedMonth: source.selectedMonth || '',
    sections: Array.isArray(source.sections) && source.sections.length > 0
      ? source.sections.map((sec, i) => ({
        id: sec.id || i + 1,
        name: sec.name || '',
        employees: Array.isArray(sec.employees) && sec.employees.length > 0
          ? sec.employees.map(buildEmployee)
          : [clone(DEFAULT_EMPLOYEE)]
      }))
      : (source.sectionName || (Array.isArray(source.employees) && source.employees.length > 0))
        ? [{
          id: 1,
          name: source.sectionName || '',
          employees: Array.isArray(source.employees) && source.employees.length > 0
            ? source.employees.map(buildEmployee)
            : [clone(DEFAULT_EMPLOYEE)]
        }]
        : [{ id: 1, name: '', employees: [clone(DEFAULT_EMPLOYEE)] }],
    vacationConfig: source.vacationConfig ? { ...draft.vacationConfig, ...source.vacationConfig } : draft.vacationConfig,
    calendar: {
      holidays: Array.isArray(source.calendar?.holidays) ? [...source.calendar.holidays] : [],
      tradingSundays: Array.isArray(source.calendar?.tradingSundays)
        ? [...source.calendar.tradingSundays]
        : [],
      storeHours: buildStoreHours(source.calendar?.storeHours),
    }, goals: {
      staffingTargets: buildStaffingTargets(source.goals?.staffingTargets),
    },
    rules: {
      shiftLengths: Array.isArray(source.rules?.shiftLengths)
        ? [...new Set(source.rules.shiftLengths.map(Number).filter(Number.isFinite))].sort(
          (a, b) => a - b,
        )
        : clone(DEFAULT_SHIFT_RULES.shiftLengths),
      maxWorkingDaysInARow:
        typeof source.rules?.maxWorkingDaysInARow === 'number'
          ? source.rules.maxWorkingDaysInARow
          : DEFAULT_SHIFT_RULES.maxWorkingDaysInARow,
      grantFreeWeekend:
        typeof source.rules?.grantFreeWeekend === 'boolean'
          ? source.rules.grantFreeWeekend
          : DEFAULT_SHIFT_RULES.grantFreeWeekend,
      maxPeoplePerShiftStart:
        typeof source.rules?.maxPeoplePerShiftStart === 'number'
          ? source.rules.maxPeoplePerShiftStart
          : DEFAULT_SHIFT_RULES.maxPeoplePerShiftStart,
    },
    weights: {
      preset: source.weights?.preset || 'balanced',
    },
    params: {
      populationSize:
        typeof source.params?.populationSize === 'number'
          ? source.params.populationSize
          : DEFAULT_GA_PARAMETERS.populationSize,
      generations:
        typeof source.params?.generations === 'number'
          ? source.params.generations
          : DEFAULT_GA_PARAMETERS.generations,
      eliteCount:
        typeof source.params?.eliteCount === 'number'
          ? source.params.eliteCount
          : DEFAULT_GA_PARAMETERS.eliteCount,
      tournamentSize:
        typeof source.params?.tournamentSize === 'number'
          ? source.params.tournamentSize
          : DEFAULT_GA_PARAMETERS.tournamentSize,
      mutationRate:
        typeof source.params?.mutationRate === 'number'
          ? source.params.mutationRate
          : DEFAULT_GA_PARAMETERS.mutationRate,
      seed:
        typeof source.params?.seed === 'string' || typeof source.params?.seed === 'number'
          ? String(source.params.seed)
          : '',
    },
  }
}

export const createEmptyEmployee = () => clone(DEFAULT_EMPLOYEE)

export const createEmptySection = (id = 1) => ({
  id,
  name: '',
  employees: [createEmptyEmployee()],
})

export const addUniqueDate = (collection, date) => {
  if (!date || collection.includes(date)) return collection
  return [...collection, date].sort()
}

export const removeDate = (collection, date) => collection.filter((item) => item !== date)

export const mergeUniqueDates = (existing, newDates) => {
  const merged = new Set([...(existing || []), ...(newDates || [])])
  return [...merged].sort()
}

export const isSundayIsoDate = (date) => {
  if (!date) return false
  const parsed = new Date(`${date}T12:00:00`)
  return !Number.isNaN(parsed.getTime()) && parsed.getDay() === 0
}

export const buildMonthDays = (selectedMonth, mode = 'any') => {
  if (!selectedMonth) return []

  const [year, month] = selectedMonth.split('-').map(Number)
  if (!year || !month) return []

  const daysInMonth = new Date(year, month, 0).getDate()
  const firstWeekday = new Date(year, month - 1, 1).getDay()
  const mondayBasedOffset = (firstWeekday + 6) % 7

  const cells = []
  for (let i = 0; i < mondayBasedOffset; i += 1) {
    cells.push({ empty: true })
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    const isoDate = `${selectedMonth}-${String(day).padStart(2, '0')}`
    const dateObj = new Date(year, month - 1, day)
    const isSunday = dateObj.getDay() === 0
    const disabled = mode === 'sundayOnly' && !isSunday

    cells.push({
      empty: false,
      day,
      isoDate,
      isSunday,
      disabled,
    })
  }

  return cells
}

const getMinSectionEmployeeCount = (draft) => {
  if (!Array.isArray(draft.sections) || draft.sections.length === 0) {
    return 0
  }

  return Math.min(
    ...draft.sections.map((section) =>
      Array.isArray(section.employees) ? section.employees.length : 0,
    ),
  )
}

export const addShiftLength = (shiftLengths, nextValue) => {
  const numericValue = Number(nextValue)
  if (!Number.isInteger(numericValue) || numericValue <= 0) {
    return shiftLengths
  }

  return [...new Set([...shiftLengths, numericValue])].sort((a, b) => a - b)
}

export const removeShiftLength = (shiftLengths, valueToRemove) =>
  shiftLengths.filter((value) => value !== valueToRemove)

export const applyOptimizationPreset = (draft, presetKey) => {
  draft.weights.preset = presetKey

  if (presetKey === 'coverage') {
    draft.params.populationSize = 350
    draft.params.generations = 21000
    draft.params.mutationRate = 0.04
  } else if (presetKey === 'fairness') {
    draft.params.populationSize = 320
    draft.params.generations = 19500
    draft.params.mutationRate = 0.03
  } else if (presetKey === 'fast') {
    draft.params.populationSize = 150
    draft.params.generations = 3000
    draft.params.mutationRate = 0.05
  } else {
    draft.params.populationSize = DEFAULT_GA_PARAMETERS.populationSize
    draft.params.generations = DEFAULT_GA_PARAMETERS.generations
    draft.params.mutationRate = DEFAULT_GA_PARAMETERS.mutationRate
  }

  return draft
}

const isTimeValueValid = (value) => /^\d{2}:\d{2}$/.test(value)

const isAfter = (start, end) => start < end

const getDaysInMonth = (selectedMonth) => {
  if (!selectedMonth) return 0
  const [year, month] = selectedMonth.split('-').map(Number)
  if (!year || !month) return 0
  return new Date(year, month, 0).getDate()
}

const dateBelongsToSelectedMonth = (date, selectedMonth) => {
  if (!date || !selectedMonth) return false
  return date.startsWith(`${selectedMonth}-`)
}

const dateToDayIndex = (date) => {
  const day = Number(date.split('-')[2])
  return Number.isInteger(day) ? day - 1 : null
}

const dateAtDayIndex = (selectedMonth, dayIndex) => {
  const [year, month] = selectedMonth.split('-').map(Number)
  return new Date(year, month - 1, dayIndex + 1)
}

const weekdayKeys = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

const weekdayKeyAtDayIndex = (selectedMonth, dayIndex) =>
  weekdayKeys[dateAtDayIndex(selectedMonth, dayIndex).getDay()]

const isoDateAtDayIndex = (selectedMonth, dayIndex) =>
  `${selectedMonth}-${String(dayIndex + 1).padStart(2, '0')}`

const isClosedDayIndex = (draft, dayIndex) => {
  const isoDate = isoDateAtDayIndex(draft.selectedMonth, dayIndex)
  const weekday = weekdayKeyAtDayIndex(draft.selectedMonth, dayIndex)

  return draft.calendar.holidays.includes(isoDate) ||
    (weekday === 'SUNDAY' && !draft.calendar.tradingSundays.includes(isoDate))
}

const canAssignHoursToDays = (totalHours, totalDays, shiftLengths) => {
  let possible = new Set([0])

  for (let day = 0; day < totalDays; day += 1) {
    const next = new Set()
    possible.forEach((hours) => {
      shiftLengths.forEach((length) => next.add(hours + length))
    })
    possible = next
  }

  return possible.has(totalHours)
}

const calculateEffectiveWorkload = (draft, employee) => {
  let totalHours = Number(employee.totalHours)
  let totalDays = Number(employee.totalDays)
  const vacations = Array.isArray(employee.vacations) ? employee.vacations : []
  const workingDays = draft.vacationConfig?.workingDays || []
  const hoursPerDay = Number(draft.vacationConfig?.hoursPerDay || 0)
  const subtractHolidays = Boolean(draft.vacationConfig?.subtractHolidays)

  vacations.forEach((date) => {
    if (!dateBelongsToSelectedMonth(date, draft.selectedMonth)) return

    const dayIndex = dateToDayIndex(date)
    const weekday = weekdayKeyAtDayIndex(draft.selectedMonth, dayIndex)

    if (!workingDays.includes(weekday)) return
    if (subtractHolidays && draft.calendar.holidays.includes(date)) return

    totalHours -= hoursPerDay
    totalDays -= 1
  })

  return { totalHours, totalDays }
}

export const CREATOR_STEPS = {
  CALENDAR: 1,
  EMPLOYEES: 2,
  GOALS: 3,
  RULES: 4,
  WEIGHTS: 5,
  PARAMS: 6,
  REVIEW: 7,
}

export const validateDraft = (draft) => {
  const errors = []
  const warnings = []
  const fieldErrors = {}
  const errorsByStep = { 1: [], 2: [], 3: [], 4: [], 5: [], 6: [], 7: [] }
  const warningsByStep = { 1: [], 2: [], 3: [], 4: [], 5: [], 6: [], 7: [] }

  const addError = (message, { path, step = CREATOR_STEPS.REVIEW } = {}) => {
    errors.push(message)
    if (path && !fieldErrors[path]) {
      fieldErrors[path] = message
    }
    if (errorsByStep[step]) {
      errorsByStep[step].push(message)
    }
  }

  const addWarning = (message, { step = CREATOR_STEPS.REVIEW } = {}) => {
    warnings.push(message)
    if (warningsByStep[step]) {
      warningsByStep[step].push(message)
    }
  }

  if (!draft.configName?.trim()) {
    addError('Podaj nazwę konfiguracji grafiku.', {
      path: 'configName',
      step: CREATOR_STEPS.CALENDAR,
    })
  }

  if (!Array.isArray(draft.sections) || draft.sections.length === 0) {
    addError('Dodaj co najmniej jedną sekcję.', {
      path: 'sections',
      step: CREATOR_STEPS.EMPLOYEES,
    })
  }

  if (!draft.selectedMonth) {
    addError('Wybierz miesiąc docelowy.', {
      path: 'selectedMonth',
      step: CREATOR_STEPS.CALENDAR,
    })
  }

  const daysInMonth = getDaysInMonth(draft.selectedMonth)

  const hasTradingSundays = draft.calendar.tradingSundays.length > 0
  const minSectionEmployees = getMinSectionEmployeeCount(draft)

  WEEKDAY_OPTIONS.forEach((day) => {
    const hours = draft.calendar.storeHours[day.key]
    if (!hours) {
      addError(`Brakuje godzin otwarcia dla dnia: ${day.label}.`, {
        path: `calendar.storeHours.${day.key}`,
        step: CREATOR_STEPS.CALENDAR,
      })
      return
    }

    if (!hours.enabled) {
      if (day.key !== 'SUNDAY') {
        addError(`${day.label} musi mieć włączone godziny otwarcia.`, {
          path: `calendar.storeHours.${day.key}.enabled`,
          step: CREATOR_STEPS.CALENDAR,
        })
      }

      if (day.key === 'SUNDAY' && hasTradingSundays) {
        addError('Dla niedziel handlowych musisz włączyć godziny otwarcia w niedzielę.', {
          path: 'calendar.storeHours.SUNDAY.enabled',
          step: CREATOR_STEPS.CALENDAR,
        })
      }
    } else {
      if (!isTimeValueValid(hours.open) || !isTimeValueValid(hours.close)) {
        addError(`Godziny otwarcia dla ${day.label} muszą mieć format HH:mm.`, {
          path: `calendar.storeHours.${day.key}.open`,
          step: CREATOR_STEPS.CALENDAR,
        })
        return
      }

      if (!isAfter(hours.open, hours.close)) {
        addError(`Godzina zamknięcia dla ${day.label} musi być późniejsza niż otwarcia.`, {
          path: `calendar.storeHours.${day.key}.close`,
          step: CREATOR_STEPS.CALENDAR,
        })
      }
    }

    const target = draft.goals.staffingTargets[day.key]
    if (!target) {
      addError(`Brakuje celu obsady dla dnia: ${day.label}.`, {
        path: `goals.staffingTargets.${day.key}`,
        step: CREATOR_STEPS.GOALS,
      })
      return
    }

    const targetMode = target.mode === 'COUNT' ? 'COUNT' : 'PERCENT'

    if (targetMode === 'COUNT') {
      if (!Number.isInteger(Number(target.count)) || Number(target.count) < 0) {
        addError(`Cel obsady (liczba osób) dla ${day.label} musi być liczbą całkowitą ≥ 0.`, {
          path: `goals.staffingTargets.${day.key}.count`,
          step: CREATOR_STEPS.GOALS,
        })
      } else if (minSectionEmployees > 0 && Number(target.count) > minSectionEmployees) {
        addError(
          `Cel obsady dla ${day.label} (${target.count} os.) przekracza najmniejszą sekcję (${minSectionEmployees} pracowników).`,
          { path: `goals.staffingTargets.${day.key}.count`, step: CREATOR_STEPS.GOALS },
        )
      }
    } else if (
      !Number.isFinite(Number(target.percent)) ||
      Number(target.percent) < 0 ||
      Number(target.percent) > 100
    ) {
      addError(`Cel obsady (%) dla ${day.label} musi być liczbą od 0 do 100.`, {
        path: `goals.staffingTargets.${day.key}.percent`,
        step: CREATOR_STEPS.GOALS,
      })
    }

    if (hasTradingSundays && day.key === 'SUNDAY') {
      if (!hours.enabled) {
        addError('Dla niedziel handlowych włącz godziny otwarcia w niedzielę.', {
          path: 'calendar.storeHours.SUNDAY.enabled',
          step: CREATOR_STEPS.CALENDAR,
        })
      }
      if (target.peakEnabled && hours.enabled) {
        if (!isAfter(hours.open, target.peakStart) || !isAfter(target.peakEnd, hours.close)) {
          addError('Godziny peak w niedzielę muszą mieścić się w godzinach otwarcia sklepu.', {
            path: 'goals.staffingTargets.SUNDAY.peakStart',
            step: CREATOR_STEPS.GOALS,
          })
        }
      }
    }

    if (target.peakEnabled) {
      if (!isTimeValueValid(target.peakStart) || !isTimeValueValid(target.peakEnd)) {
        addError(`Godziny peak dla ${day.label} muszą mieć format HH:mm.`, {
          path: `goals.staffingTargets.${day.key}.peakStart`,
          step: CREATOR_STEPS.GOALS,
        })
      } else if (!isAfter(target.peakStart, target.peakEnd)) {
        addError(`Koniec peak window dla ${day.label} musi być po początku.`, {
          path: `goals.staffingTargets.${day.key}.peakEnd`,
          step: CREATOR_STEPS.GOALS,
        })
      }
    }
  })

  if (draft.calendar.storeHours.SUNDAY?.enabled && !hasTradingSundays) {
    addWarning(
      'Masz włączone godziny otwarcia w niedzielę, ale nie dodano żadnej niedzieli handlowej.',
      { step: CREATOR_STEPS.CALENDAR },
    )
  }

  draft.calendar.holidays.forEach((date) => {
    if (!dateBelongsToSelectedMonth(date, draft.selectedMonth)) {
      addError(`Święto ${date} nie należy do wybranego miesiąca.`, {
        path: 'calendar.holidays',
        step: CREATOR_STEPS.CALENDAR,
      })
    }
  })

  draft.calendar.tradingSundays.forEach((date) => {
    if (!dateBelongsToSelectedMonth(date, draft.selectedMonth)) {
      addError(`Niedziela handlowa ${date} nie należy do wybranego miesiąca.`, {
        path: 'calendar.tradingSundays',
        step: CREATOR_STEPS.CALENDAR,
      })
    } else if (!isSundayIsoDate(date)) {
      addError(`Data ${date} nie jest niedzielą — dodaj tylko niedziele handlowe.`, {
        path: 'calendar.tradingSundays',
        step: CREATOR_STEPS.CALENDAR,
      })
    }
  })

  draft.sections.forEach((section, sIndex) => {
    if (!section.name?.trim()) {
      addError(`Sekcja ${sIndex + 1}: podaj nazwę sekcji.`, {
        path: `sections.${sIndex}.name`,
        step: CREATOR_STEPS.EMPLOYEES,
      })
    }

    if (!Array.isArray(section.employees) || section.employees.length === 0) {
      addError(`Sekcja ${sIndex + 1}: dodaj co najmniej jednego pracownika.`, {
        path: `sections.${sIndex}.employees`,
        step: CREATOR_STEPS.EMPLOYEES,
      })
    }

    section.employees.forEach((employee, index) => {
      const employeeLabel = `Sekcja ${sIndex + 1}, Pracownik ${index + 1}`
      const empBasePath = `sections.${sIndex}.employees.${index}`

      if (!employee.name?.trim()) {
        addError(`${employeeLabel}: podaj imię.`, {
          path: `${empBasePath}.name`,
          step: CREATOR_STEPS.EMPLOYEES,
        })
      }

      if (!employee.surname?.trim()) {
        addError(`${employeeLabel}: podaj nazwisko.`, {
          path: `${empBasePath}.surname`,
          step: CREATOR_STEPS.EMPLOYEES,
        })
      }

      if (!Number.isInteger(Number(employee.totalHours)) || Number(employee.totalHours) < 0) {
        addError(
          `${employeeLabel}: liczba godzin musi być liczbą całkowitą większą lub równą 0.`,
          { path: `${empBasePath}.totalHours`, step: CREATOR_STEPS.EMPLOYEES },
        )
      }

      if (!Number.isInteger(Number(employee.totalDays)) || Number(employee.totalDays) <= 0) {
        addError(`${employeeLabel}: liczba dni pracy musi być większa od 0.`, {
          path: `${empBasePath}.totalDays`,
          step: CREATOR_STEPS.EMPLOYEES,
        })
      }

      employee.daysOff.forEach((date) => {
        if (!dateBelongsToSelectedMonth(date, draft.selectedMonth)) {
          addError(`${employeeLabel}: dzień wolny ${date} nie należy do wybranego miesiąca.`, {
            path: `${empBasePath}.daysOff`,
            step: CREATOR_STEPS.EMPLOYEES,
          })
        }
      })

      ;(employee.vacations || []).forEach((date) => {
        if (!dateBelongsToSelectedMonth(date, draft.selectedMonth)) {
          addError(`${employeeLabel}: urlop ${date} nie należy do wybranego miesiąca.`, {
            path: `${empBasePath}.vacations`,
            step: CREATOR_STEPS.EMPLOYEES,
          })
        }
      })

      const daysOffCount = employee.daysOff
        .map(dateToDayIndex)
        .filter((value) => Number.isInteger(value)).length

      if (daysInMonth > 0 && Number(employee.totalDays) > daysInMonth) {
        addError(
          `${employeeLabel}: liczba dni pracy nie może przekraczać liczby dni w miesiącu.`,
          { path: `${empBasePath}.totalDays`, step: CREATOR_STEPS.EMPLOYEES },
        )
      }

      if (daysInMonth > 0 && daysOffCount >= daysInMonth) {
        addWarning(`${employeeLabel}: liczba dni wolnych obejmuje prawie cały miesiąc.`, {
          step: CREATOR_STEPS.EMPLOYEES,
        })
      }

      const shiftLengths = Array.isArray(draft.rules.shiftLengths)
        ? [
            ...new Set(
              draft.rules.shiftLengths
                .map(Number)
                .filter((value) => Number.isInteger(value) && value > 0),
            ),
          ]
        : []

      if (
        draft.selectedMonth &&
        daysInMonth > 0 &&
        shiftLengths.length > 0 &&
        Number.isInteger(Number(employee.totalHours)) &&
        Number.isInteger(Number(employee.totalDays))
      ) {
        const effective = calculateEffectiveWorkload(draft, employee)
        const minHours = Math.min(...shiftLengths) * effective.totalDays
        const maxHours = Math.max(...shiftLengths) * effective.totalDays

        if (effective.totalDays <= 0) {
          addError(`${employeeLabel}: po urlopach liczba dni pracy musi być większa od 0.`, {
            path: `${empBasePath}.totalDays`,
            step: CREATOR_STEPS.EMPLOYEES,
          })
        } else if (effective.totalHours < minHours || effective.totalHours > maxHours) {
          addError(
            `${employeeLabel}: ${effective.totalHours} h w ${effective.totalDays} dni nie mieści się w zakresie ${minHours}-${maxHours} h dla wybranych długości zmian.`,
            { path: `${empBasePath}`, step: CREATOR_STEPS.EMPLOYEES },
          )
        } else if (!canAssignHoursToDays(effective.totalHours, effective.totalDays, shiftLengths)) {
          addError(
            `${employeeLabel}: ${effective.totalHours} h w ${effective.totalDays} dni nie da się złożyć z wybranych długości zmian (${shiftLengths.join(', ')} h).`,
            { path: `${empBasePath}`, step: CREATOR_STEPS.EMPLOYEES },
          )
        }

        const unavailableDays = new Set()
        for (let dayIndex = 0; dayIndex < daysInMonth; dayIndex += 1) {
          if (isClosedDayIndex(draft, dayIndex)) {
            unavailableDays.add(dayIndex)
          }
        }

        ;[...(employee.daysOff || []), ...(employee.vacations || [])].forEach((date) => {
          if (dateBelongsToSelectedMonth(date, draft.selectedMonth)) {
            unavailableDays.add(dateToDayIndex(date))
          }
        })

        const availableWorkingDays = daysInMonth - unavailableDays.size
        if (effective.totalDays > availableWorkingDays) {
          addError(
            `${employeeLabel}: ${effective.totalDays} dni pracy przekracza dostępne dni po wolnym, urlopach i dniach zamkniętych (${availableWorkingDays}).`,
            { path: `${empBasePath}.totalDays`, step: CREATOR_STEPS.EMPLOYEES },
          )
        }
      }
    })
  })

  if (!Array.isArray(draft.rules.shiftLengths) || draft.rules.shiftLengths.length === 0) {
    addError('Dodaj co najmniej jedną długość zmiany.', {
      path: 'rules.shiftLengths',
      step: CREATOR_STEPS.RULES,
    })
  }

  draft.rules.shiftLengths.forEach((value) => {
    if (!Number.isInteger(Number(value)) || Number(value) <= 0) {
      addError('Każda długość zmiany musi być dodatnią liczbą całkowitą.', {
        path: 'rules.shiftLengths',
        step: CREATOR_STEPS.RULES,
      })
    }
  })

  if (
    !Number.isInteger(Number(draft.rules.maxWorkingDaysInARow)) ||
    Number(draft.rules.maxWorkingDaysInARow) <= 0
  ) {
    addError('Maksymalna liczba dni pracy z rzędu musi być większa od 0.', {
      path: 'rules.maxWorkingDaysInARow',
      step: CREATOR_STEPS.RULES,
    })
  }

  if (
    !Number.isInteger(Number(draft.rules.maxPeoplePerShiftStart)) ||
    Number(draft.rules.maxPeoplePerShiftStart) < 0
  ) {
    addError('Limit osób na start zmiany musi być liczbą większą lub równą 0.', {
      path: 'rules.maxPeoplePerShiftStart',
      step: CREATOR_STEPS.RULES,
    })
  }

  if (
    !Number.isInteger(Number(draft.params.populationSize)) ||
    Number(draft.params.populationSize) <= 0
  ) {
    addError('Population size musi być większe od 0.', {
      path: 'params.populationSize',
      step: CREATOR_STEPS.PARAMS,
    })
  }

  if (!Number.isInteger(Number(draft.params.generations)) || Number(draft.params.generations) <= 0) {
    addError('Liczba generacji musi być większa od 0.', {
      path: 'params.generations',
      step: CREATOR_STEPS.PARAMS,
    })
  }

  if (!Number.isInteger(Number(draft.params.eliteCount)) || Number(draft.params.eliteCount) < 0) {
    addError('Elite count musi być większe lub równe 0.', {
      path: 'params.eliteCount',
      step: CREATOR_STEPS.PARAMS,
    })
  }

  if (
    Number(draft.params.eliteCount) >= Number(draft.params.populationSize) &&
    Number.isInteger(Number(draft.params.populationSize))
  ) {
    addError('Elite count musi być mniejsze niż population size.', {
      path: 'params.eliteCount',
      step: CREATOR_STEPS.PARAMS,
    })
  }

  if (
    !Number.isInteger(Number(draft.params.tournamentSize)) ||
    Number(draft.params.tournamentSize) <= 0
  ) {
    addError('Tournament size musi być większe od 0.', {
      path: 'params.tournamentSize',
      step: CREATOR_STEPS.PARAMS,
    })
  }

  if (
    !Number.isFinite(Number(draft.params.mutationRate)) ||
    Number(draft.params.mutationRate) < 0 ||
    Number(draft.params.mutationRate) > 1
  ) {
    addError('Mutation rate musi być w zakresie od 0 do 1.', {
      path: 'params.mutationRate',
      step: CREATOR_STEPS.PARAMS,
    })
  }

  if (
    draft.params.seed !== '' &&
    (!Number.isInteger(Number(draft.params.seed)) || Number(draft.params.seed) < 0)
  ) {
    addError('Seed musi być pusty albo dodatnią liczbą całkowitą.', {
      path: 'params.seed',
      step: CREATOR_STEPS.PARAMS,
    })
  }

  if (
    draft.weights.preset &&
    !OPTIMIZATION_PRESETS.some((preset) => preset.key === draft.weights.preset)
  ) {
    addWarning(
      'Wybrany preset optymalizacji nie jest rozpoznawany. Zostanie użyty tryb domyślny.',
      { step: CREATOR_STEPS.WEIGHTS },
    )
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
    fieldErrors,
    errorsByStep,
    warningsByStep,
  }
}

export const buildCreateSectionPayload = (draft) => ({
  name: draft.sectionName.trim(),
})

export const buildCreateEmployeesPayload = (draft, sectionId) =>
  draft.employees.map((employee) => ({
    name: employee.name.trim(),
    surname: employee.surname.trim(),
    sectionId,
    totalHours: Number(employee.totalHours),
    totalDays: Number(employee.totalDays),
    daysOff: employee.daysOff
      .map(dateToDayIndex)
      .filter((value) => Number.isInteger(value))
      .sort((a, b) => a - b),
  }))

export const buildCreateScheduleConfigPayload = (draft) => {
  const storeHours = {}
  const staffingTargets = {
    percentByDay: {},
    countByDay: {},
    peakByDay: {},
  }

  WEEKDAY_OPTIONS.forEach((day) => {
    const hours = draft.calendar.storeHours[day.key]
    if (hours?.enabled) {
      storeHours[day.key] = {
        open: hours.open,
        close: hours.close,
      }
    }

    const target = draft.goals.staffingTargets[day.key]
    if (target.mode === 'COUNT') {
      staffingTargets.countByDay[day.key] = Number(target.count)
    } else {
      staffingTargets.percentByDay[day.key] = Number(target.percent)
    }

    if (target.peakEnabled) {
      staffingTargets.peakByDay[day.key] = {
        start: target.peakStart,
        end: target.peakEnd,
      }
    }
  })

  return {
    name: draft.configName.trim(),
    yearMonth: `${draft.selectedMonth}-01`,
    storeHours: {
      hours: storeHours,
    },
    staffingTargets,
    calendar: {
      yearMonth: draft.selectedMonth,
      holidays: [...draft.calendar.holidays],
      tradingSundays: [...draft.calendar.tradingSundays],
    },
    shiftRules: {
      shiftLengths: [...draft.rules.shiftLengths].map(Number).sort((a, b) => a - b),
      maxWorkingDaysInARow: Number(draft.rules.maxWorkingDaysInARow),
      grantFreeWeekend: Boolean(draft.rules.grantFreeWeekend),
      maxPeoplePerShiftStart: Number(draft.rules.maxPeoplePerShiftStart),
    },
    gaParameters: {
      populationSize: Number(draft.params.populationSize),
      generations: Number(draft.params.generations),
      eliteCount: Number(draft.params.eliteCount),
      tournamentSize: Number(draft.params.tournamentSize),
      mutationRate: Number(draft.params.mutationRate),
    },
  }
}

export const buildStartGenerationPayload = (draft, configId, sectionIds) => {
  const seed = draft.params.seed === '' ? null : Number(draft.params.seed)

  return {
    configId,
    sectionIds,
    seed,
  }
}

export const summarizeDraft = (draft) => ({
  configName: draft.configName,
  selectedMonth: draft.selectedMonth,
  sectionsCount: draft.sections.length,
  employeesCount: draft.sections.reduce((acc, sec) => acc + sec.employees.length, 0),
  holidaysCount: draft.calendar.holidays.length,
  tradingSundaysCount: draft.calendar.tradingSundays.length,
  shiftLengths: [...draft.rules.shiftLengths],
  preset: draft.weights.preset,
})
