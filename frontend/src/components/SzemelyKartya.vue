<script setup lang="ts">
import { computed } from 'vue'
import type { SzemelyDto } from '../api/tipusok'
import type { ElhelyezettSzemely } from '../elrendezes'
import { eletEvek, rovidites, teljesNev } from '../utils/formazas'

const props = defineProps<{
  hely: ElhelyezettSzemely
  szemely?: SzemelyDto
  szelesseg: number
  magassag: number
}>()

// kb. ennyi karakter fér el egy sorban a kártya szélességén
const sorHossz = computed(() => Math.floor((props.szelesseg - 22) / 7.5))

const nemOsztaly = computed(() => {
  if (props.szemely?.nem === 'M') return 'ferfi'
  if (props.szemely?.nem === 'F') return 'no'
  return 'ismeretlen'
})
const vezeteknev = computed(() => rovidites(props.szemely?.vezeteknev ?? '', sorHossz.value))
const keresztnev = computed(() => rovidites(props.szemely?.keresztnev ?? '', sorHossz.value))
const evek = computed(() => (props.szemely ? eletEvek(props.szemely) : ''))
const elonezet = computed(() => {
  if (!props.szemely) return ''
  const sorok = [teljesNev(props.szemely)]
  if (props.szemely.szuletesiDatum) sorok.push(`Született: ${props.szemely.szuletesiDatum}`)
  if (props.szemely.halalozasiDatum) sorok.push(`Elhunyt: ${props.szemely.halalozasiDatum}`)
  return sorok.join('\n')
})
</script>

<template>
  <g class="szemely-kartya" :class="nemOsztaly" :transform="`translate(${hely.x} ${hely.y})`">
    <title>{{ elonezet }}</title>
    <rect class="hatter" :width="szelesseg" :height="magassag" rx="8" />
    <rect class="nem-jelzo" width="6" :height="magassag" rx="3" />
    <text class="vezeteknev" x="16" y="24">{{ vezeteknev }}</text>
    <text class="keresztnev" x="16" y="42">{{ keresztnev }}</text>
    <text class="evek" x="16" y="60">{{ evek }}</text>
  </g>
</template>
