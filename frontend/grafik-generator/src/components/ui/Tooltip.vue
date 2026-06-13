<script setup>
import {
  TooltipProvider,
  TooltipRoot,
  TooltipTrigger,
  TooltipPortal,
  TooltipContent,
} from 'radix-vue'

defineProps({
  content: {
    type: String,
    default: '',
  },
  side: {
    type: String,
    default: 'top',
  },
  delayDuration: {
    type: Number,
    default: 150,
  },
})
</script>

<template>
  <TooltipProvider :delay-duration="delayDuration">
    <TooltipRoot>
      <TooltipTrigger as-child>
        <slot />
      </TooltipTrigger>
      <TooltipPortal>
        <TooltipContent
          :side="side"
          :side-offset="6"
          class="z-50 max-w-xs rounded-md border border-[var(--color-border)] bg-[var(--color-surface)]
            px-3 py-1.5 text-xs text-[var(--color-foreground)] shadow-md"
        >
          <slot name="content">{{ content }}</slot>
        </TooltipContent>
      </TooltipPortal>
    </TooltipRoot>
  </TooltipProvider>
</template>
