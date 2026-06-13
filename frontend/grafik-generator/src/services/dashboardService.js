const API_BASE_URL = '/api/dashboard'

const extractApiPayload = async (response, fallbackMessage) => {
  const payload = await response.json().catch(() => null)

  if (!response.ok || payload?.success === false) {
    throw new Error(payload?.message || fallbackMessage)
  }

  return payload?.data ?? null
}

export const deleteGenerationGroup = async (groupId) => {
  const response = await fetch(`/api/generation-runs/groups/${groupId}`, {
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
    },
  })

  return extractApiPayload(response, 'Nie udało się usunąć grafiku.')
}

export const fetchDashboardData = async () => {
  const response = await fetch(API_BASE_URL, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
  })

  return extractApiPayload(response, 'Nie udało się pobrać danych dashboardu.')
}
