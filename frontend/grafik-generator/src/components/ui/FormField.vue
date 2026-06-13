<script setup>
import { computed } from 'vue'
import Label from './Label.vue'

const props = defineProps({
  label: {
    type: String,
    default: '',
  },
  hint: {
    type: String,
    default: '',
  },
  error: {
    type: String,
    default: '',
  },
  required: {
    type: Boolean,
    default: false,
  },
  for: {
    type: String,
    default: undefined,
  },
})

const hasError = computed(() => Boolean(props.error))
</script>

<template>
  <div class="space-y-1.5">
    <Label v-if="label" :for="props.for" :required="required">{{ label }}</Label>
    <slot :invalid="hasError" />
    <p v-if="hasError" class="text-xs font-medium text-[var(--color-danger)]" role="alert">
      {{ error }}
    </p>
    <p v-else-if="hint" class="text-xs text-[var(--color-muted)]">
      {{ hint }}
    </p>
  </div>
</template>
