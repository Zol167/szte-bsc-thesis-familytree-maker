import type { Egyseg } from './egysegek'
import type { ElrendezesBeallitasok } from './tipusok'
import { atlag, gyermekesCsaladok, type Kontextus, type Retegek } from './sorrend'

// Kapcsolat nélküli egység (nincs szülője/gyermeke a fában) kis súllyal a helyén marad
const KIS_SULY = 1e-3

// Egy sor optimális elhelyezése adott sorrend mellett.
// Feladat: minimalizálni Σ sᵢ·(xᵢ − cᵢ)² összeget úgy, hogy x(i+1) − xᵢ ≥ dᵢ (a sorrend és a minimális
// távolság megmaradjon). Az yᵢ = xᵢ − (d₀ + … + dᵢ₋₁) helyettesítéssel ez súlyozott izotonikus
// regresszió (y nem csökkenő), amit a "Pool Adjacent Violators" algoritmus pontosan, O(n) időben old meg:
// a sorrendet sértő szomszédos blokkokat összevonja, és a blokk értéke a súlyozott átlag lesz.
export function sorrendTartoElhelyezes(celok: number[], sulyok: number[], minTavok: number[]): number[] {
  const n = celok.length
  const eltolas: number[] = []
  let kumulalt = 0
  for (let i = 0; i < n; i++) {
    eltolas.push(kumulalt)
    if (i < n - 1) kumulalt += minTavok[i]
  }

  const blokkok: Array<{ osszeg: number; suly: number; db: number }> = []
  for (let i = 0; i < n; i++) {
    blokkok.push({ osszeg: sulyok[i] * (celok[i] - eltolas[i]), suly: sulyok[i], db: 1 })
    while (blokkok.length > 1) {
      const utolso = blokkok[blokkok.length - 1]
      const elozo = blokkok[blokkok.length - 2]
      if (elozo.osszeg / elozo.suly <= utolso.osszeg / utolso.suly) break
      elozo.osszeg += utolso.osszeg
      elozo.suly += utolso.suly
      elozo.db += utolso.db
      blokkok.pop()
    }
  }

  const x: number[] = []
  for (const blokk of blokkok) {
    const ertek = blokk.osszeg / blokk.suly
    for (let k = 0; k < blokk.db; k++) x.push(ertek + eltolas[x.length])
  }
  return x
}

// 4. lépés: vízszintes koordináták. A sorok sorrendje rögzített; váltakozó söprésekkel minden sor
// a szomszédos sorhoz igazodik: lefelé a gyermekek a szüleik horgonya alá, felfelé a szülők a
// gyermekeik fölé törekszenek, és minden sort a fenti optimális elhelyezés old meg.
// Eredmény: egységenként a bal szél x koordinátája (a komponens bal széle 0).
export function koordinatakSzamitasa(retegek: Retegek, ktx: Kontextus, be: ElrendezesBeallitasok): Map<number, number> {
  const lepes = be.kartyaSzelesseg + be.hazastarsTav
  const fel = be.kartyaSzelesseg / 2
  const tagIndex = new Map<number, number>()
  for (const reteg of retegek) for (const e of reteg) e.tagok.forEach((tag, i) => tagIndex.set(tag, i))

  const x = new Map<number, number>() // egység id → bal szél
  const kozepX = (szemelyId: number) =>
    x.get(ktx.egysegeOf.get(szemelyId)!.id)! + tagIndex.get(szemelyId)! * lepes + fel
  const tagKozep = (tag: number) => tagIndex.get(tag)! * lepes + fel // az egység bal széléhez képest
  const minTavok = retegek.map((reteg) =>
    reteg.slice(0, -1).map((e, i) => egysegSzelesseg(e, be) + egysegTav(e, reteg[i + 1], ktx, be)),
  )

  // kiindulás: balra zárt sorok
  retegek.forEach((reteg, g) => {
    let akt = 0
    reteg.forEach((e, i) => {
      x.set(e.id, akt)
      if (i < reteg.length - 1) akt += minTavok[g][i]
    })
  })

  const elhelyez = (g: number, celok: number[], sulyok: number[]) => {
    const uj = sorrendTartoElhelyezes(celok, sulyok, minTavok[g])
    retegek[g].forEach((e, i) => x.set(e.id, uj[i]))
  }

  // minden tag a saját szülőcsaládjának horgonya (a szülők középpontjainak átlaga) alá törekszik
  const lefele = (g: number) => {
    if (retegek[g].length === 0) return
    const celok: number[] = []
    const sulyok: number[] = []
    for (const e of retegek[g]) {
      let osszeg = 0
      let db = 0
      for (const tag of e.tagok) {
        for (const cs of ktx.graf.szuloCsaladjai.get(tag)!) {
          if (cs.szulok.length === 0) continue
          osszeg += atlag(cs.szulok.map(kozepX)) - tagKozep(tag)
          db++
        }
      }
      celok.push(db > 0 ? osszeg / db : x.get(e.id)!)
      sulyok.push(db > 0 ? db : KIS_SULY)
    }
    elhelyez(g, celok, sulyok)
  }

  // minden család horgonya a gyermekei által elfoglalt sáv közepe fölé törekszik
  const felfele = (g: number) => {
    if (retegek[g].length === 0) return
    const celok: number[] = []
    const sulyok: number[] = []
    for (const e of retegek[g]) {
      let osszeg = 0
      let db = 0
      for (const cs of gyermekesCsaladok(e, ktx)) {
        const horgony = atlag(cs.szulok.filter((p) => e.tagok.includes(p)).map(tagKozep))
        const gyermekKozepek = cs.gyermekek.map(kozepX)
        osszeg += (Math.min(...gyermekKozepek) + Math.max(...gyermekKozepek)) / 2 - horgony
        db++
      }
      celok.push(db > 0 ? osszeg / db : x.get(e.id)!)
      sulyok.push(db > 0 ? db : KIS_SULY)
    }
    elhelyez(g, celok, sulyok)
  }

  for (let it = 0; it < be.koordinataIteraciok; it++) {
    for (let g = 1; g < retegek.length; g++) lefele(g)
    for (let g = retegek.length - 2; g >= 0; g--) felfele(g)
  }
  // a végén még egy lefelé söprés: a gyermekek a végleges szülőpozíciók alá kerülnek
  for (let g = 1; g < retegek.length; g++) lefele(g)

  let min = Infinity
  for (const ertek of x.values()) min = Math.min(min, ertek)
  if (Number.isFinite(min)) for (const [id, ertek] of x) x.set(id, ertek - min)
  return x
}

export function egysegSzelesseg(e: Egyseg, be: ElrendezesBeallitasok): number {
  return e.tagok.length * be.kartyaSzelesseg + (e.tagok.length - 1) * be.hazastarsTav
}

// Testvérek (közös szülőcsalád) között kisebb, nem rokon egységek között nagyobb a hézag
function egysegTav(a: Egyseg, b: Egyseg, ktx: Kontextus, be: ElrendezesBeallitasok): number {
  const aCsaladjai = new Set(a.tagok.flatMap((tag) => ktx.graf.szuloCsaladjai.get(tag)!))
  const testverek = b.tagok.some((tag) => ktx.graf.szuloCsaladjai.get(tag)!.some((cs) => aCsaladjai.has(cs)))
  return testverek ? be.testverTav : be.csaladTav
}
