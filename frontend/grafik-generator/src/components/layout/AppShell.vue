<script setup>
import ContextualHeader from './ContextualHeader.vue'
</script>

<template>
  <div class="min-h-screen bg-[var(--color-background)] text-[var(--color-foreground)]">
    <ContextualHeader>
      <template v-if="$slots['header-breadcrumb']" #breadcrumb>
        <slot name="header-breadcrumb" />
      </template>
      <template v-if="$slots['header-actions']" #actions>
        <slot name="header-actions" />
      </template>
      <template v-if="$slots['header-subbar']" #subbar>
        <slot name="header-subbar" />
      </template>
    </ContextualHeader>

    <main>
      <slot />
    </main>
  </div>
</template>
