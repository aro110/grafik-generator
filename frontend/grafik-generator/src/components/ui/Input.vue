<script setup>
import { computed } from 'vue'
import { cn } from '../../lib/utils'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: '',
  },
  type: {
    type: String,
    default: 'text',
  },
  invalid: {
    type: Boolean,
    default: false,
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md'].includes(v),
  },
})

const emit = defineEmits(['update:modelValue'])

const base =
  'block w-full rounded-md border bg-[var(--color-surface)] text-[var(--color-foreground)] ' +
  'placeholder:text-[var(--color-muted)] shadow-sm transition-colors ' +
  'focus:outline-none focus:ring-2 focus:ring-[var(--color-ring)]/30 ' +
  'disabled:cursor-not-allowed disabled:opacity-60'

const sizes = {
  sm: 'h-8 px-2.5 text-xs',
  md: 'h-10 px-3 text-sm',
}

const classes = computed(() =>
  cn(
    base,
    sizes[props.size],
    props.invalid
      ? 'border-[var(--color-danger)] focus:border-[var(--color-danger)] focus:ring-[var(--color-danger)]/30'
      : 'border-[var(--color-border-strong)] focus:border-[var(--color-primary)]',
  ),
)

const onInput = (event) => {
  let value = event.target.value
  if (props.type === 'number' && value !== '') {
    const numeric = Number(value)
    value = Number.isNaN(numeric) ? value : numeric
  }
  emit('update:modelValue', value)
}
</script>

<template>
  <input
    :type="type"
    :value="modelValue"
    :class="classes"
    :aria-invalid="invalid || undefined"
    v-bind="$attrs"
    @input="onInput"
  />
</template>
