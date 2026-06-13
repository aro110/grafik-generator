<script setup>
import { computed } from 'vue'
import { Moon, Sun } from 'lucide-vue-next'
import { useTheme } from '../../composables/useTheme'
import Button from '../ui/Button.vue'

const { theme, toggleTheme } = useTheme()

const label = computed(() =>
  theme.value === 'dark' ? 'Przełącz na motyw jasny' : 'Przełącz na motyw ciemny',
)
</script>

<template>
  <Button
    variant="ghost"
    size="icon"
    :aria-label="label"
    :title="label"
    @click="toggleTheme"
  >
    <Sun v-if="theme === 'dark'" class="h-5 w-5" aria-hidden="true" />
    <Moon v-else class="h-5 w-5" aria-hidden="true" />
  </Button>
</template>
