<script setup>
import { SwitchRoot, SwitchThumb } from 'radix-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  id: {
    type: String,
    default: undefined,
  },
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <SwitchRoot
    :id="props.id"
    :checked="modelValue"
    :disabled="disabled"
    class="peer relative inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent
      bg-[var(--color-surface-muted)] transition-colors
      focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-background)]
      disabled:cursor-not-allowed disabled:opacity-50
      data-[state=checked]:bg-[var(--color-primary)]"
    @update:checked="emit('update:modelValue', $event)"
  >
    <SwitchThumb
      class="pointer-events-none block h-5 w-5 rounded-full bg-white shadow-sm ring-0 transition-transform
        data-[state=checked]:translate-x-5 data-[state=unchecked]:translate-x-0"
    />
  </SwitchRoot>
</template>
