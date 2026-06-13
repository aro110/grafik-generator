import { computed, inject, provide } from 'vue'

const VALIDATION_INJECTION_KEY = Symbol('creator-validation')

export function provideValidation(validationRef) {
  provide(VALIDATION_INJECTION_KEY, validationRef)
}

export function useFieldValidation() {
  const validation = inject(VALIDATION_INJECTION_KEY, null)

  const getError = (path) => {
    if (!validation?.value) return ''
    return validation.value.fieldErrors?.[path] || ''
  }

  const hasError = (path) => Boolean(getError(path))

  const getStepErrors = (step) => {
    if (!validation?.value) return []
    return validation.value.errorsByStep?.[step] || []
  }

  const getStepWarnings = (step) => {
    if (!validation?.value) return []
    return validation.value.warningsByStep?.[step] || []
  }

  const stepHasErrors = (step) => getStepErrors(step).length > 0

  return {
    validation: computed(() => validation?.value || null),
    getError,
    hasError,
    getStepErrors,
    getStepWarnings,
    stepHasErrors,
  }
}
