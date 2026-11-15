<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { csaladfaGraf, csaladfakListaja } from './api/kliens'
import type { CsaladfaGrafDto, CsaladfaOsszegzesDto } from './api/tipusok'
import CsaladfaRajz from './components/CsaladfaRajz.vue'
import { elrendezes } from './elrendezes'
import { gedcomSorrend } from './utils/formazas'

const fak = ref<CsaladfaOsszegzesDto[]>([])
const valasztottId = ref<number | null>(null)
const graf = ref<CsaladfaGrafDto | null>(null)
const betoltes = ref(false)
const hiba = ref<string | null>(null)
const igazitas = ref(true)

// A kiválasztott fa az URL-ben is megjelenik (?fa=<id>), így egy adott fa linkelhető
onMounted(async () => {
  try {
    fak.value = await csaladfakListaja()
    const kertId = Number(new URLSearchParams(window.location.search).get('fa'))
    valasztottId.value = fak.value.find((fa) => fa.id === kertId)?.id ?? fak.value[0]?.id ?? null
  } catch (e) {
    hiba.value = (e as Error).message
  }
})

watch(valasztottId, async (id) => {
  graf.value = null
  if (id == null) return
  const url = new URL(window.location.href)
  url.searchParams.set('fa', String(id))
  window.history.replaceState(null, '', url)
  betoltes.value = true
  hiba.value = null
  try {
    graf.value = await csaladfaGraf(id)
  } catch (e) {
    hiba.value = (e as Error).message
  } finally {
    betoltes.value = false
  }
})

// Az elrendezés a böngészőben fut; a számítási idő a szakdolgozat méréseihez is jól jön
const eredmeny = computed(() => {
  if (!graf.value) return null
  const bemenet = {
    szemelyek: [...graf.value.szemelyek].sort(gedcomSorrend),
    csaladok: [...graf.value.csaladok].sort(gedcomSorrend),
  }
  const kezdet = performance.now()
  const kesz = elrendezes(bemenet)
  return { elrendezes: kesz, ido: performance.now() - kezdet }
})

const szemelyek = computed(() => new Map(graf.value?.szemelyek.map((sz) => [sz.id, sz]) ?? []))
</script>

<template>
  <header class="fejlec">
    <h1>Családfa</h1>
    <label v-if="fak.length > 0" class="fa-valaszto">
      <span>Családfa:</span>
      <select v-model="valasztottId">
        <option v-for="fa in fak" :key="fa.id" :value="fa.id">
          {{ fa.nev }} ({{ fa.tulajdonos }}) – {{ fa.szemelyDb }} személy
        </option>
      </select>
    </label>
    <label v-if="eredmeny" class="kapcsolo">
      <input v-model="igazitas" type="checkbox" />
      <span>Igazítás az ablakhoz</span>
    </label>
    <dl v-if="eredmeny && graf" class="statisztika">
      <div><dt>Személyek</dt><dd>{{ graf.szemelyek.length }}</dd></div>
      <div><dt>Családok</dt><dd>{{ graf.csaladok.length }}</dd></div>
      <div><dt>Generációk</dt><dd>{{ eredmeny.elrendezes.generaciokSzama }}</dd></div>
      <div><dt>Keresztezések</dt><dd>{{ eredmeny.elrendezes.keresztezesek }}</dd></div>
      <div><dt>Elrendezés</dt><dd>{{ eredmeny.ido.toFixed(1) }} ms</dd></div>
    </dl>
  </header>

  <main class="tartalom">
    <p v-if="hiba" class="uzenet hiba">{{ hiba }}</p>
    <p v-else-if="betoltes" class="uzenet">Betöltés…</p>
    <p v-else-if="fak.length === 0 && !graf" class="uzenet">
      Még nincs családfa. Importálj egy GEDCOM fájlt a backendbe (lásd: backend/demo.http).
    </p>
    <template v-else-if="eredmeny">
      <ul v-if="eredmeny.elrendezes.figyelmeztetesek.length > 0" class="uzenet figyelmeztetes">
        <li v-for="(f, i) in eredmeny.elrendezes.figyelmeztetesek" :key="i">{{ f }}</li>
      </ul>
      <div class="rajzlap">
        <CsaladfaRajz :elrendezes="eredmeny.elrendezes" :szemelyek="szemelyek" :igazitas="igazitas" />
      </div>
    </template>
  </main>
</template>
