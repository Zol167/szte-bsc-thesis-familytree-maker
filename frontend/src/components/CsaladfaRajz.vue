<script setup lang="ts">
import { computed } from 'vue'
import type { SzemelyDto } from '../api/tipusok'
import { ALAP_BEALLITASOK, type Elrendezes, type ElrendezesBeallitasok } from '../elrendezes'
import SzemelyKartya from './SzemelyKartya.vue'

// A kiszámított koordináták kirajzolása natív SVG elemekkel: a vonalak útvonalak (<path>),
// a személyek téglalapok és szövegek. Elrendezési logika itt nincs, csak megjelenítés.
const props = withDefaults(
  defineProps<{
    elrendezes: Elrendezes
    szemelyek: Map<number, SzemelyDto>
    beallitasok?: ElrendezesBeallitasok
    igazitas?: boolean // a teljes fa az ablakba kicsinyítve (különben 1:1 méret, görgetéssel)
  }>(),
  { beallitasok: () => ALAP_BEALLITASOK, igazitas: false },
)

const MARGO = 40
const szelesseg = computed(() => props.elrendezes.szelesseg + 2 * MARGO)
const magassag = computed(() => props.elrendezes.magassag + 2 * MARGO)
</script>

<template>
  <svg
    class="csaladfa-rajz"
    :class="{ igazitott: igazitas }"
    :viewBox="`0 0 ${szelesseg} ${magassag}`"
    :width="igazitas ? undefined : szelesseg"
    :height="igazitas ? undefined : magassag"
    preserveAspectRatio="xMidYMin meet"
    role="img"
    aria-label="Családfa"
  >
    <g :transform="`translate(${MARGO} ${MARGO})`">
      <!-- előbb a vonalak, hogy a kártyák fölöttük legyenek -->
      <g class="vonalak">
        <template v-for="vonal in elrendezes.vonalak" :key="vonal.csaladId">
          <path v-if="vonal.leszarmazas" class="leszarmazas" :d="vonal.leszarmazas" />
          <path v-if="vonal.hazassag" class="hazassag" :d="vonal.hazassag" />
        </template>
      </g>
      <SzemelyKartya
        v-for="hely in elrendezes.szemelyek"
        :key="hely.id"
        :hely="hely"
        :szemely="szemelyek.get(hely.id)"
        :szelesseg="beallitasok.kartyaSzelesseg"
        :magassag="beallitasok.kartyaMagassag"
      />
    </g>
  </svg>
</template>
