const API_BASE_URL = '/api/schedules'

const extractApiPayload = async (response, fallbackMessage) => {
  const payload = await response.json().catch(() => null)

  if (!response.ok || payload?.success === false) {
    throw new Error(payload?.message || fallbackMessage)
  }

  return payload?.data ?? null
}

const extractErrorMessage = async (response, fallbackMessage) => {
  try {
    const data = await response.json()
    return data?.message || fallbackMessage
  } catch {
    return fallbackMessage
  }
}

const downloadBlob = (blob, fileName) => {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(objectUrl)
}

const resolveFileName = (response, defaultFileName) => {
  const disposition = response.headers.get('content-disposition')
  if (!disposition) return defaultFileName

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }

  const simpleMatch = disposition.match(/filename="?([^"]+)"?/i)
  if (simpleMatch?.[1]) {
    return simpleMatch[1]
  }

  return defaultFileName
}

const fetchJson = async (url, options, fallbackMessage) => {
  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
      ...options?.headers,
    },
    ...options,
  })

  return extractApiPayload(response, fallbackMessage)
}

export const fetchStoreSummary = async (groupId) =>
  fetchJson(
    `${API_BASE_URL}/summary/${groupId}`,
    {
      method: 'GET',
    },
    'Nie udało się pobrać podsumowania dla sklepu.',
  )

export const fetchScheduleDetails = async (scheduleId) =>
  fetchJson(
    `${API_BASE_URL}/${scheduleId}/details`,
    {
      method: 'GET',
    },
    'Nie udało się pobrać szczegółów grafiku.',
  )

export const proposeScheduleAiEdit = async (scheduleId, payload) =>
  fetchJson(
    `${API_BASE_URL}/${scheduleId}/ai-edits`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
    'Nie udało się przygotować propozycji AI.',
  )

export const proposeGroupScheduleAiEdit = async (groupId, payload) =>
  fetchJson(
    `${API_BASE_URL}/summary/${groupId}/ai-edits`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
    'Nie udało się przygotować propozycji AI dla całej konfiguracji.',
  )

export const applyScheduleAiEdit = async (scheduleId, editId) => {
  const response = await fetch(`${API_BASE_URL}/${scheduleId}/ai-edits/${editId}/apply`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(await extractErrorMessage(response, 'Nie udało się zaakceptować propozycji AI.'))
  }

  return response.json()
}

export const applyGroupScheduleAiEdit = async (groupId, editId) => {
  const response = await fetch(`${API_BASE_URL}/summary/${groupId}/ai-edits/${editId}/apply`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(await extractErrorMessage(response, 'Nie udało się zaakceptować propozycji AI.'))
  }

  return response.json()
}

export const rejectScheduleAiEdit = async (scheduleId, editId) => {
  const response = await fetch(`${API_BASE_URL}/${scheduleId}/ai-edits/${editId}/reject`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(await extractErrorMessage(response, 'Nie udało się odrzucić propozycji AI.'))
  }

  return response.json()
}

export const rejectGroupScheduleAiEdit = async (groupId, editId) => {
  const response = await fetch(`${API_BASE_URL}/summary/${groupId}/ai-edits/${editId}/reject`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(await extractErrorMessage(response, 'Nie udało się odrzucić propozycji AI.'))
  }

  return response.json()
}

export const exportScheduleToExcel = async (scheduleId) => {
  const response = await fetch(`${API_BASE_URL}/${scheduleId}/export`, {
    method: 'GET',
  })

  if (!response.ok) {
    throw new Error(await extractErrorMessage(response, 'Nie udało się wyeksportować grafiku.'))
  }

  const blob = await response.blob()
  const fileName = resolveFileName(response, `grafik-${scheduleId}.xls`)
  downloadBlob(blob, fileName)
}

export const exportGroupToExcel = async (groupId) => {
  const response = await fetch(`${API_BASE_URL}/summary/${groupId}/export`, {
    method: 'GET',
  })

  if (!response.ok) {
    throw new Error(await extractErrorMessage(response, 'Nie udało się wyeksportować grafiku sklepu.'))
  }

  const blob = await response.blob()
  const fileName = resolveFileName(response, `grafik-grupa-${groupId}.xls`)
  downloadBlob(blob, fileName)
}

export const publishSchedule = async (scheduleId) => {
  const response = await fetch(`${API_BASE_URL}/${scheduleId}/publish`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(await extractErrorMessage(response, 'Nie udało się opublikować grafiku.'))
  }

  return response.json()
}
