<script setup>
import { TabsRoot } from 'radix-vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: undefined,
  },
  defaultValue: {
    type: [String, Number],
    default: undefined,
  },
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <TabsRoot
    :model-value="modelValue"
    :default-value="defaultValue"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <slot />
  </TabsRoot>
</template>
