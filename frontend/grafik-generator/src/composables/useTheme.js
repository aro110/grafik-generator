import { ref, watch } from 'vue'

const STORAGE_KEY = 'grafik_theme'
const VALID_THEMES = ['light', 'dark']

const readInitialTheme = () => {
  if (typeof window === 'undefined') return 'light'

  const saved = window.localStorage.getItem(STORAGE_KEY)
  if (VALID_THEMES.includes(saved)) return saved

  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  return prefersDark ? 'dark' : 'light'
}

const applyTheme = (value) => {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  if (value === 'dark') root.classList.add('dark')
  else root.classList.remove('dark')
}

const theme = ref(readInitialTheme())

applyTheme(theme.value)

watch(theme, (value) => {
  applyTheme(value)
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, value)
  }
})

export function useTheme() {
  const setTheme = (value) => {
    if (!VALID_THEMES.includes(value)) return
    theme.value = value
  }

  const toggleTheme = () => {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  return {
    theme,
    setTheme,
    toggleTheme,
  }
}
