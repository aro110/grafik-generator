<script setup>
import {
  AlertDialogRoot,
  AlertDialogPortal,
  AlertDialogOverlay,
  AlertDialogContent,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogCancel,
  AlertDialogAction,
} from 'radix-vue'
import Button from './Button.vue'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: 'Czy na pewno?',
  },
  description: {
    type: String,
    default: '',
  },
  confirmLabel: {
    type: String,
    default: 'Potwierdź',
  },
  cancelLabel: {
    type: String,
    default: 'Anuluj',
  },
  variant: {
    type: String,
    default: 'destructive',
    validator: (v) => ['default', 'destructive'].includes(v),
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:open', 'confirm', 'cancel'])

const onConfirm = () => emit('confirm')
const onCancel = () => emit('cancel')
</script>

<template>
  <AlertDialogRoot :open="open" @update:open="emit('update:open', $event)">
    <AlertDialogPortal>
      <AlertDialogOverlay
        class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm transition-opacity duration-150
          data-[state=closed]:opacity-0 data-[state=open]:opacity-100"
      />
      <AlertDialogContent
        class="fixed left-1/2 top-1/2 z-50 grid w-full max-w-lg -translate-x-1/2 -translate-y-1/2 gap-4
          rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-lg
          transition-all duration-150
          data-[state=closed]:scale-95 data-[state=closed]:opacity-0
          data-[state=open]:scale-100 data-[state=open]:opacity-100"
      >
        <div class="space-y-2">
          <AlertDialogTitle class="text-lg font-semibold text-[var(--color-foreground)]">
            {{ title }}
          </AlertDialogTitle>
          <AlertDialogDescription
            v-if="description || $slots.description"
            class="text-sm text-[var(--color-foreground-muted)]"
          >
            <slot name="description">{{ description }}</slot>
          </AlertDialogDescription>
        </div>

        <slot />

        <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <AlertDialogCancel as-child>
            <Button variant="outline" :disabled="loading" @click="onCancel">
              {{ cancelLabel }}
            </Button>
          </AlertDialogCancel>
          <AlertDialogAction as-child>
            <Button :variant="variant === 'destructive' ? 'destructive' : 'default'"
              :loading="loading" @click="onConfirm">
              {{ confirmLabel }}
            </Button>
          </AlertDialogAction>
        </div>
      </AlertDialogContent>
    </AlertDialogPortal>
  </AlertDialogRoot>
</template>
