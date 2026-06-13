<script setup>
import { useFieldValidation } from '../../composables/useFieldValidation'
import Card from '../ui/Card.vue'
import CardHeader from '../ui/CardHeader.vue'
import FormField from '../ui/FormField.vue'
import Input from '../ui/Input.vue'
import Alert from '../ui/Alert.vue'

const draft = defineModel({
  type: Object,
  required: true,
})

const { getError } = useFieldValidation()
</script>

<template>
  <div class="space-y-6">
    <Card padding="none">
      <CardHeader
        title="Parametry algorytmu genetycznego"
        description="Większe wartości zwykle poprawiają jakość wyniku, ale wydłużają czas generowania."
      />

      <div class="grid gap-6 p-6 md:grid-cols-2">
        <FormField
          label="Population size"
          for="ga-pop"
          hint="Liczba osobników w populacji."
          :error="getError('params.populationSize')"
        >
          <Input
            id="ga-pop"
            v-model.number="draft.params.populationSize"
            type="number"
            min="1"
            step="1"
            :invalid="Boolean(getError('params.populationSize'))"
          />
        </FormField>

        <FormField
          label="Liczba generacji"
          for="ga-gen"
          hint="Maksymalna liczba iteracji algorytmu."
          :error="getError('params.generations')"
        >
          <Input
            id="ga-gen"
            v-model.number="draft.params.generations"
            type="number"
            min="1"
            step="1"
            :invalid="Boolean(getError('params.generations'))"
          />
        </FormField>

        <FormField
          label="Elite count"
          for="ga-elite"
          hint="Najlepsi osobnicy przenoszeni bez zmian do kolejnej generacji."
          :error="getError('params.eliteCount')"
        >
          <Input
            id="ga-elite"
            v-model.number="draft.params.eliteCount"
            type="number"
            min="0"
            step="1"
            :invalid="Boolean(getError('params.eliteCount'))"
          />
        </FormField>

        <FormField
          label="Tournament size"
          for="ga-tour"
          hint="Liczba kandydatów porównywanych przy wyborze rodziców."
          :error="getError('params.tournamentSize')"
        >
          <Input
            id="ga-tour"
            v-model.number="draft.params.tournamentSize"
            type="number"
            min="1"
            step="1"
            :invalid="Boolean(getError('params.tournamentSize'))"
          />
        </FormField>

        <FormField
          label="Mutation rate"
          for="ga-mut"
          hint="Wartość 0–1. Wyższa = większa losowość."
          :error="getError('params.mutationRate')"
        >
          <Input
            id="ga-mut"
            v-model.number="draft.params.mutationRate"
            type="number"
            min="0"
            max="1"
            step="0.01"
            :invalid="Boolean(getError('params.mutationRate'))"
          />
        </FormField>

        <FormField
          label="Seed (opcjonalnie)"
          for="ga-seed"
          hint="Ustaw seed tylko, by odtworzyć konkretny przebieg generacji."
          :error="getError('params.seed')"
        >
          <Input
            id="ga-seed"
            v-model="draft.params.seed"
            type="number"
            min="0"
            step="1"
            placeholder="Pozostaw puste"
            :invalid="Boolean(getError('params.seed'))"
          />
        </FormField>
      </div>
    </Card>

    <Alert variant="info" title="Rekomendowany start">
      Dla pierwszych testów: <strong>population 300</strong>, <strong>15000 generacji</strong>,
      <strong>elite 5</strong>, <strong>tournament 3</strong>, <strong>mutation 0.03</strong>.
    </Alert>
  </div>
</template>
