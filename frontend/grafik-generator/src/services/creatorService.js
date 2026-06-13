export const GENERATION_POLL_INTERVAL_MS = 2000
export const GENERATION_POLL_TIMEOUT_MS = 10 * 60 * 1000
export const GENERATION_POLL_MAX_ATTEMPTS = Math.ceil(
  GENERATION_POLL_TIMEOUT_MS / GENERATION_POLL_INTERVAL_MS,
)

const API_URLS = {
  sections: '/api/sections',
  employees: '/api/employees',
  scheduleConfigs: '/api/schedule-configs',
  generationRuns: '/api/generation-runs',
  schedules: '/api/schedules',
}

const parseJsonResponse = async (response) => {
  try {
    return await response.json()
  } catch {
    return null
  }
}

const extractApiData = async (response, fallbackMessage) => {
  const payload = await parseJsonResponse(response)

  if (!response.ok || payload?.success === false) {
    throw new Error(payload?.message || fallbackMessage)
  }

  return payload?.data ?? null
}

const fetchJson = async (url, options, fallbackMessage) => {
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options?.body ? { 'Content-Type': 'application/json' } : {}),
      ...options?.headers,
    },
  })

  return extractApiData(response, fallbackMessage)
}

const toInteger = (value, fallback = 0) => {
  const number = Number(value)
  return Number.isFinite(number) ? Math.trunc(number) : fallback
}

const toFloat = (value, fallback = 0) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

const toIsoDate = (value) => {
  if (!value) return null
  return String(value).slice(0, 10)
}

const toYearMonthStart = (selectedMonth) => {
  if (!selectedMonth) {
    throw new Error('Brakuje wybranego miesiąca grafiku.')
  }

  const normalizedMonth = String(selectedMonth).slice(0, 7)
  return `${normalizedMonth}-01`
}

const toDayIndex = (dateValue, selectedMonth) => {
  const isoDate = toIsoDate(dateValue)
  if (!isoDate || !selectedMonth || !isoDate.startsWith(`${selectedMonth}-`)) {
    return null
  }

  const day = Number(isoDate.split('-')[2])
  if (!Number.isInteger(day) || day <= 0) {
    return null
  }

  return day - 1
}

const mapDaysOff = (daysOff, selectedMonth) => {
  if (!Array.isArray(daysOff)) return []

  return [...new Set(daysOff.map((value) => toDayIndex(value, selectedMonth)).filter((value) => value !== null))].sort(
    (a, b) => a - b,
  )
}

const mapStoreHours = (draft) => {
  const storeHours = draft?.calendar?.storeHours || {}

  return Object.entries(storeHours).reduce((acc, [day, hours]) => {
    if (!hours?.enabled) {
      return acc
    }

    acc[day] = {
      open: String(hours.open || '08:00').slice(0, 5),
      close: String(hours.close || '20:00').slice(0, 5),
    }

    return acc
  }, {})
}

const mapStaffingTargets = (draft) => {
  const staffingTargets = draft?.goals?.staffingTargets || {}

  return Object.entries(staffingTargets).reduce(
    (acc, [day, target]) => {
      if (target?.mode === 'COUNT') {
        acc.countByDay[day] = toInteger(target?.count, 0)
      } else {
        acc.percentByDay[day] = toInteger(target?.percent, 50)
      }

      if (target?.peakEnabled) {
        acc.peakByDay[day] = {
          start: String(target.peakStart || '12:00').slice(0, 5),
          end: String(target.peakEnd || '16:00').slice(0, 5),
        }
      }

      return acc
    },
    {
      percentByDay: {},
      countByDay: {},
      peakByDay: {},
    },
  )
}

const mapShiftRules = (draft) => ({
  shiftLengths: [...new Set((draft?.rules?.shiftLengths || []).map((value) => toInteger(value, 0)).filter((value) => value > 0))].sort(
    (a, b) => a - b,
  ),
  maxWorkingDaysInARow: toInteger(draft?.rules?.maxWorkingDaysInARow, 5),
  grantFreeWeekend: Boolean(draft?.rules?.grantFreeWeekend),
  maxPeoplePerShiftStart: toInteger(draft?.rules?.maxPeoplePerShiftStart, 0),
})

const mapGaParameters = (draft) => ({
  populationSize: toInteger(draft?.params?.populationSize, 300),
  generations: toInteger(draft?.params?.generations, 5000),
  eliteCount: toInteger(draft?.params?.eliteCount, 5),
  tournamentSize: toInteger(draft?.params?.tournamentSize, 3),
  mutationRate: toFloat(draft?.params?.mutationRate, 0.03),
})

const buildCreateSectionPayload = (sectionDraft) => ({
  name: sectionDraft.name.trim(),
})

const buildCreateEmployeesPayload = (sectionDraft, sectionId, selectedMonth) =>
  (sectionDraft.employees || []).map((employee) => ({
    name: employee.name.trim(),
    surname: employee.surname.trim(),
    sectionId,
    totalHours: toInteger(employee.totalHours, 0),
    totalDays: toInteger(employee.totalDays, 0),
    daysOff: mapDaysOff(employee.daysOff, selectedMonth),
  }))

const buildCreateScheduleConfigPayload = (draft) => {
  const yearMonth = toYearMonthStart(draft.selectedMonth)

  return {
    name: draft.configName.trim(),
    yearMonth,
    storeHours: {
      hours: mapStoreHours(draft),
    },
    staffingTargets: mapStaffingTargets(draft),
    calendar: {
      yearMonth: draft.selectedMonth,
      holidays: Array.isArray(draft?.calendar?.holidays)
        ? draft.calendar.holidays.map((value) => toIsoDate(value)).filter(Boolean)
        : [],
      tradingSundays: Array.isArray(draft?.calendar?.tradingSundays)
        ? draft.calendar.tradingSundays.map((value) => toIsoDate(value)).filter(Boolean)
        : [],
    },
    shiftRules: mapShiftRules(draft),
    vacationConfig: {
      workingDays: draft.vacationConfig?.workingDays || ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
      hoursPerDay: toInteger(draft.vacationConfig?.hoursPerDay, 8),
      subtractHolidays: Boolean(draft.vacationConfig?.subtractHolidays),
    },
    gaParameters: mapGaParameters(draft),
  }
}

const buildStartGenerationPayload = (draft, configId, sectionIds) => {
  const seedValue = draft?.params?.seed
  const hasSeed = seedValue !== '' && seedValue !== null && seedValue !== undefined

  return {
    configId,
    sectionIds,
    seed: hasSeed ? toInteger(seedValue, 0) : null,
  }
}

const validateDraft = (draft) => {
  if (!draft?.configName?.trim()) {
    throw new Error('Uzupełnij nazwę konfiguracji grafiku.')
  }

  if (!draft?.selectedMonth) {
    throw new Error('Wybierz miesiąc docelowy.')
  }

  if (!Array.isArray(draft?.sections) || draft.sections.length === 0) {
    throw new Error('Dodaj przynajmniej jedną sekcję.')
  }

  const incompleteSection = draft.sections.find((sec) => !sec?.name?.trim())
  if (incompleteSection) {
    throw new Error('Każda sekcja musi mieć uzupełnioną nazwę.')
  }

  draft.sections.forEach((sec) => {
    if (!Array.isArray(sec?.employees) || sec.employees.length === 0) {
      throw new Error(`Sekcja "${sec.name}" musi mieć przynajmniej jednego pracownika.`)
    }
    const incompleteEmployee = sec.employees.find(
      (employee) => !employee?.name?.trim() || !employee?.surname?.trim(),
    )
    if (incompleteEmployee) {
      throw new Error(`Każdy pracownik w sekcji "${sec.name}" musi mieć uzupełnione imię i nazwisko.`)
    }
  })

  const shiftLengths = draft?.rules?.shiftLengths || []
  if (!Array.isArray(shiftLengths) || shiftLengths.length === 0) {
    throw new Error('Dodaj przynajmniej jedną długość zmiany.')
  }
}

const createSection = async (payload) =>
  fetchJson(
    API_URLS.sections,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    'Nie udało się utworzyć sekcji.',
  )

const createEmployee = async (payload) =>
  fetchJson(
    API_URLS.employees,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    'Nie udało się utworzyć pracownika.',
  )

const createScheduleConfig = async (payload) =>
  fetchJson(
    API_URLS.scheduleConfigs,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    'Nie udało się utworzyć konfiguracji grafiku.',
  )

const startGeneration = async (payload) =>
  fetchJson(
    API_URLS.generationRuns,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    'Nie udało się uruchomić generowania grafiku.',
  )

const findSchedulesByRun = async (runId) =>
  fetchJson(
    `${API_URLS.schedules}?runId=${runId}`,
    {
      method: 'GET',
    },
    'Nie udało się pobrać listy grafików.',
  )

const buildFullGenerationPayload = (draft) => {
  const config = buildCreateScheduleConfigPayload(draft)

  const sections = draft.sections.map(sectionDraft => ({
    name: sectionDraft.name.trim(),
    employees: (sectionDraft.employees || []).map(employee => ({
      name: employee.name.trim(),
      surname: employee.surname.trim(),
      totalHours: toInteger(employee.totalHours, 0),
      totalDays: toInteger(employee.totalDays, 0),
      daysOff: mapDaysOff(employee.daysOff, draft.selectedMonth),
      vacations: mapDaysOff(employee.vacations, draft.selectedMonth),
    }))
  }))

  const seedValue = draft?.params?.seed
  const seed = (seedValue !== '' && seedValue !== null && seedValue !== undefined) ? toInteger(seedValue, 0) : null

  return {
    config,
    sections,
    seed,
  }
}

const startFullGeneration = async (payload) =>
  fetchJson(
    `${API_URLS.generationRuns}/full-start`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    'Nie udało się uruchomić pełnego generowania grafiku.',
  )

export const submitCreatorDraft = async (draft) => {
  validateDraft(draft)

  const payload = buildFullGenerationPayload(draft)
  const generationRunGroup = await startFullGeneration(payload)

  return {
    generationRunGroup,
  }
}

export const regenerateGroup = async (groupId) =>
  fetchJson(
    `${API_URLS.generationRuns}/groups/${groupId}/regenerate`,
    {
      method: 'POST',
    },
    'Nie udało się ponowić generacji grafiku.',
  )

export const pollGenerationUntilFinished = async (
  groupId,
  {
    intervalMs = GENERATION_POLL_INTERVAL_MS,
    maxAttempts = GENERATION_POLL_MAX_ATTEMPTS,
    onProgress = null,
  } = {},
) => {
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const group = await fetchJson(
      `${API_URLS.generationRuns}/groups/${groupId}`,
      {
        method: 'GET',
      },
      'Nie udało się sprawdzić statusu generacji.',
    )

    const runs = group?.runs || []
    const allFinished = runs.length > 0 && runs.every(r => r.status === 'SUCCESS' || r.status === 'FAILED')
    const anyFailed = runs.some(r => r.status === 'FAILED')

    if (typeof onProgress === 'function') {
      const avgProgress = runs.length > 0
        ? runs.reduce((acc, r) => acc + (r.progress || 0), 0) / runs.length
        : 0
      onProgress({ progress: Math.round(avgProgress) })
    }

    if (allFinished) {
      if (anyFailed) {
        const failureDetails = runs
          .filter(r => r.status === 'FAILED')
          .map(r => {
            const sectionLabel = r.sectionName || (r.sectionId ? `Sekcja ID ${r.sectionId}` : 'Sekcja')
            return r.errorMessage ? `${sectionLabel}: ${r.errorMessage}` : sectionLabel
          })
          .join(' ')

        throw new Error(
          failureDetails
            ? `Generowanie grafiku zakończyło się błędem. ${failureDetails}`
            : `Generowanie grafiku zakończyło się błędem. Backend oznaczył generację jako FAILED, ale nie zwrócił szczegółów. Statusy sekcji: ${runs.map(r => r.status || 'UNKNOWN').join(', ') || 'brak runów'}.`,
        )
      }
      return {
        group,
      }
    }

    await new Promise((resolve) => {
      window.setTimeout(resolve, intervalMs)
    })
  }

  throw new Error(
    'Przekroczono maksymalny czas oczekiwania na zakończenie generowania grafiku (10 minut).',
  )
}
