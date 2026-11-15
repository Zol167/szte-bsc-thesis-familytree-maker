import type { Csalad, Graf } from './graf'
import { legkisebbSorszam, szuletesiSorrend } from './graf'
import type { Egyseg } from './egysegek'

export interface Kontextus {
  graf: Graf
  generacio: Map<number, number>
  egysegeOf: Map<number, Egyseg> // személy → egység
}

// retegek[g] = a g. generáció egységei balról jobbra
export type Retegek = Egyseg[][]

// 3/a: kezdeti sorrend mélységi bejárással a családi szálak mentén: egy egység után rögtön a
// gyermekei következnek (születési sorrendben), így minden leszármazotti ág kezdetben egy tömbben van.
export function kezdoSorrend(egysegek: Egyseg[], ktx: Kontextus): Retegek {
  const generaciokSzama = egysegek.reduce((max, e) => Math.max(max, e.generacio + 1), 0)
  const retegek: Retegek = Array.from({ length: generaciokSzama }, () => [])
  const latogatott = new Set<number>()

  const bejar = (e: Egyseg): void => {
    if (latogatott.has(e.id)) return
    latogatott.add(e.id)
    retegek[e.generacio].push(e)
    for (const tag of e.tagok) {
      // szülő nélküli családban a testvérek egymás mellé kerülnek
      for (const cs of ktx.graf.szuloCsaladjai.get(tag)!) {
        if (cs.szulok.length === 0) {
          for (const testver of szuletesiSorrend(cs.gyermekek, ktx.graf)) bejar(ktx.egysegeOf.get(testver)!)
        }
      }
      for (const cs of ktx.graf.sajatCsaladjai.get(tag)!) {
        for (const gyermek of szuletesiSorrend(cs.gyermekek, ktx.graf)) bejar(ktx.egysegeOf.get(gyermek)!)
      }
    }
  }

  const kezdok = [...egysegek].sort(
    (a, b) => a.generacio - b.generacio || legkisebbSorszam(a.tagok, ktx.graf) - legkisebbSorszam(b.tagok, ktx.graf),
  )
  kezdok.forEach(bejar)
  return retegek
}

// 3/b: keresztezés-csökkentés (Sugiyama-féle rétegenkénti baricentrum-módszer).
// Lefelé söpréskor minden egység a szülei átlagos pozíciójához, felfelé söpréskor a gyermekei átlagos
// pozíciójához rendeződik; utána az egység tükrözhető (pl. melyik házastárs álljon a saját szülei felé).
// A legkevesebb keresztezést adó sorrend marad meg.
export function sorrendOptimalizalas(
  retegek: Retegek,
  ktx: Kontextus,
  iteraciok: number,
): { retegek: Retegek; keresztezesek: number } {
  const poz = poziciok(retegek)
  szomszedosCsere(retegek, poz, ktx)
  let legjobb = pillanatkep(retegek)
  let legjobbErtek = keresztezesekSzama(retegek, poz, ktx)

  for (let it = 0; it < iteraciok && legjobbErtek > 0; it++) {
    for (let g = 1; g < retegek.length; g++) {
      retegek[g] = retegRendezese(retegek[g], (e) => baricentrumLefele(e, poz, ktx), poz)
      retegPoziciok(retegek[g], poz)
      for (const e of retegek[g]) tajolas(e, poz, ktx)
    }
    for (let g = retegek.length - 2; g >= 0; g--) {
      retegek[g] = retegRendezese(retegek[g], (e) => baricentrumFelfele(e, poz, ktx), poz)
      retegPoziciok(retegek[g], poz)
      for (const e of retegek[g]) tajolas(e, poz, ktx)
    }
    szomszedosCsere(retegek, poz, ktx)
    const ertek = keresztezesekSzama(retegek, poz, ktx)
    if (ertek < legjobbErtek) {
      legjobbErtek = ertek
      legjobb = pillanatkep(retegek)
    }
  }
  return { retegek: visszaallitas(legjobb), keresztezesek: legjobbErtek }
}

// 3/c: szomszédos egységek cseréje (transzpozíciós heurisztika, mint a Graphviz "dot" algoritmusában).
// Két szomszédos egység felcserélése csak az ő saját éleik egymáshoz viszonyított keresztezéseit
// változtatja meg (mindenki máshoz képest a sorrendjük ugyanaz marad), így a nyereség e két egység
// éleiből számolható. Csak javító cserét hajtunk végre, ezért a keresztezések száma szigorúan csökken,
// és az eljárás véges sok lépésben megáll.
function szomszedosCsere(retegek: Retegek, poz: Map<number, number>, ktx: Kontextus, maxKor = 20): void {
  for (let kor = 0; kor < maxKor; kor++) {
    let javult = false
    for (const reteg of retegek) {
      for (let i = 0; i + 1 < reteg.length; i++) {
        if (csereNyeresege(reteg[i], reteg[i + 1], poz, ktx) > 0) {
          ;[reteg[i], reteg[i + 1]] = [reteg[i + 1], reteg[i]]
          retegPoziciok(reteg, poz)
          javult = true
        }
      }
    }
    if (!javult) return
  }
}

// Mennyivel csökkenne a keresztezések száma, ha a bal oldali egység (bal) és a tőle jobbra álló
// szomszédja (jobb) helyet cserélne. Egy bal-élpár csere előtt akkor keresztezi a jobb-élpárt, ha a
// másik végük fordított sorrendben van (a > b), csere után akkor, ha egyező sorrendben (a < b).
function csereNyeresege(bal: Egyseg, jobb: Egyseg, poz: Map<number, number>, ktx: Kontextus): number {
  let nyereseg = 0
  const osszevet = (balVegek: number[], jobbVegek: number[]) => {
    for (const a of balVegek) {
      for (const b of jobbVegek) {
        if (a > b) nyereseg++
        else if (a < b) nyereseg--
      }
    }
  }
  osszevet(felsoVegek(bal, poz, ktx), felsoVegek(jobb, poz, ktx))
  osszevet(alsoVegek(bal, poz, ktx), alsoVegek(jobb, poz, ktx))
  return nyereseg
}

// Az egység tagjaiba érkező élek felső vége: a szülőcsalád horgonya az előző generációban
function felsoVegek(e: Egyseg, poz: Map<number, number>, ktx: Kontextus): number[] {
  const vegek: number[] = []
  for (const tag of e.tagok) {
    for (const cs of ktx.graf.szuloCsaladjai.get(tag)!) {
      if (cs.szulok.length > 0 && cs.szulok.every((p) => ktx.generacio.get(p) === e.generacio - 1)) {
        vegek.push(atlag(cs.szulok.map((p) => poz.get(p)!)))
      }
    }
  }
  return vegek
}

// Az egység családjaiból induló élek alsó vége: a gyermekek pozíciója a következő generációban
function alsoVegek(e: Egyseg, poz: Map<number, number>, ktx: Kontextus): number[] {
  const vegek: number[] = []
  for (const cs of gyermekesCsaladok(e, ktx)) {
    for (const gy of cs.gyermekek) {
      if (ktx.generacio.get(gy) === e.generacio + 1) vegek.push(poz.get(gy)!)
    }
  }
  return vegek
}

// A családok, amelyekben az egység valamelyik tagja szülő és van gyermekük
export function gyermekesCsaladok(e: Egyseg, ktx: Kontextus): Csalad[] {
  const csaladok = new Set<Csalad>()
  for (const tag of e.tagok) {
    for (const cs of ktx.graf.sajatCsaladjai.get(tag)!) if (cs.gyermekek.length > 0) csaladok.add(cs)
  }
  return [...csaladok]
}

// Keresztezések két szomszédos generáció között. Egy él: a család "horgonya" (a szülők átlagos
// pozíciója) és egy gyermek. Két él akkor keresztezi egymást, ha a felső végük sorrendje ellentétes
// az alsóéval; ez inverziószámlálás, összefésülő rendezéssel O(E log E).
export function keresztezesekSzama(retegek: Retegek, poz: Map<number, number>, ktx: Kontextus): number {
  let osszes = 0
  for (let g = 0; g + 1 < retegek.length; g++) {
    const elek: Array<[number, number]> = []
    for (const e of retegek[g]) {
      for (const cs of gyermekesCsaladok(e, ktx)) {
        const horgony = atlag(cs.szulok.map((p) => poz.get(p)!))
        for (const gy of cs.gyermekek) {
          if (ktx.generacio.get(gy) === g + 1) elek.push([horgony, poz.get(gy)!])
        }
      }
    }
    osszes += ketretegKeresztezesek(elek)
  }
  return osszes
}

export function ketretegKeresztezesek(elek: Array<[number, number]>): number {
  // felső vég szerint, egyezésnél alsó vég szerint rendezve az azonos horgonyú élek nem számítanak keresztezésnek
  const rendezett = [...elek].sort((a, b) => a[0] - b[0] || a[1] - b[1])
  return inverziokSzama(rendezett.map((el) => el[1]))
}

// Szigorú inverziók száma (i < j és t[i] > t[j]) összefésülő rendezéssel; a tömböt rendezi
function inverziokSzama(t: number[]): number {
  if (t.length < 2) return 0
  const kozep = t.length >> 1
  const bal = t.slice(0, kozep)
  const jobb = t.slice(kozep)
  let db = inverziokSzama(bal) + inverziokSzama(jobb)
  let i = 0
  let j = 0
  let k = 0
  while (i < bal.length && j < jobb.length) {
    if (bal[i] <= jobb[j]) {
      t[k++] = bal[i++]
    } else {
      t[k++] = jobb[j++]
      db += bal.length - i
    }
  }
  while (i < bal.length) t[k++] = bal[i++]
  while (j < jobb.length) t[k++] = jobb[j++]
  return db
}

export function poziciok(retegek: Retegek): Map<number, number> {
  const poz = new Map<number, number>()
  for (const reteg of retegek) retegPoziciok(reteg, poz)
  return poz
}

// Egy személy pozíciója a sorában (tagonként eggyel nő)
function retegPoziciok(reteg: Egyseg[], poz: Map<number, number>): void {
  let i = 0
  for (const e of reteg) for (const tag of e.tagok) poz.set(tag, i++)
}

// Az egység középpontjának kívánt pozíciója a szülők alapján: minden tag a szülei horgonya alá szeretne
// kerülni; a tag egységen belüli helyét levonva az egység középpontjára vetítjük
function baricentrumLefele(e: Egyseg, poz: Map<number, number>, ktx: Kontextus): number | null {
  const kozep = (e.tagok.length - 1) / 2
  let osszeg = 0
  let db = 0
  e.tagok.forEach((tag, k) => {
    for (const cs of ktx.graf.szuloCsaladjai.get(tag)!) {
      if (cs.szulok.length === 0) continue
      osszeg += atlag(cs.szulok.map((p) => poz.get(p)!)) - k + kozep
      db++
    }
  })
  return db > 0 ? osszeg / db : null
}

// Ugyanez felfelé: minden család horgonya a gyermekei átlagos pozíciója fölé szeretne kerülni
function baricentrumFelfele(e: Egyseg, poz: Map<number, number>, ktx: Kontextus): number | null {
  const kozep = (e.tagok.length - 1) / 2
  let osszeg = 0
  let db = 0
  for (const cs of gyermekesCsaladok(e, ktx)) {
    const szuloIndexek = cs.szulok.map((p) => e.tagok.indexOf(p)).filter((i) => i >= 0)
    osszeg += atlag(cs.gyermekek.map((gy) => poz.get(gy)!)) - atlag(szuloIndexek) + kozep
    db++
  }
  return db > 0 ? osszeg / db : null
}

// Stabil rendezés a kulcs szerint; akinek nincs kulcsa (nincs szülője/gyermeke), a jelenlegi helyén marad
function retegRendezese(
  reteg: Egyseg[],
  kulcs: (e: Egyseg) => number | null,
  poz: Map<number, number>,
): Egyseg[] {
  const elemek = reteg.map((e, i) => ({ e, i, k: kulcs(e) ?? poz.get(e.tagok[0])! + (e.tagok.length - 1) / 2 }))
  elemek.sort((a, b) => a.k - b.k || a.i - b.i)
  return elemek.map((elem) => elem.e)
}

// Egy többtagú egység tükrözése (pl. melyik házastárs álljon a saját szülei felé). A tükrözés csak
// az egység tagjainak egymáshoz viszonyított sorrendjét változtatja, így csak az egység saját élei
// közötti keresztezések változhatnak: elsősorban ezek számát csökkentjük, döntetlennél a tagok és a
// szüleik/gyermekeik közötti távolság dönt.
function tajolas(e: Egyseg, poz: Map<number, number>, ktx: Kontextus): void {
  if (e.tagok.length < 2) return
  const kezdo = poz.get(e.tagok[0])!
  const ertekeles = (tagok: number[]) => {
    const hely = (id: number) => kezdo + tagok.indexOf(id)
    const felso: Array<[number, number]> = [] // szülőcsalád horgonya → tag
    for (const tag of tagok) {
      for (const cs of ktx.graf.szuloCsaladjai.get(tag)!) {
        if (cs.szulok.length > 0) felso.push([atlag(cs.szulok.map((p) => poz.get(p)!)), hely(tag)])
      }
    }
    const also: Array<[number, number]> = [] // saját család horgonya → gyermek
    for (const cs of gyermekesCsaladok(e, ktx)) {
      const horgony = atlag(cs.szulok.filter((p) => tagok.includes(p)).map(hely))
      for (const gy of cs.gyermekek) also.push([horgony, poz.get(gy)!])
    }
    const tavolsag = [...felso, ...also].reduce((osszeg, [a, b]) => osszeg + Math.abs(a - b), 0)
    return { keresztezes: ketretegKeresztezesek(felso) + ketretegKeresztezesek(also), tavolsag }
  }
  const jelenlegi = ertekeles(e.tagok)
  const forditott = [...e.tagok].reverse()
  const uj = ertekeles(forditott)
  if (
    uj.keresztezes < jelenlegi.keresztezes ||
    (uj.keresztezes === jelenlegi.keresztezes && uj.tavolsag < jelenlegi.tavolsag - 1e-9)
  ) {
    e.tagok = forditott
    e.tagok.forEach((tag, i) => poz.set(tag, kezdo + i))
  }
}

type Pillanatkep = Array<Array<{ e: Egyseg; tagok: number[] }>>

function pillanatkep(retegek: Retegek): Pillanatkep {
  return retegek.map((reteg) => reteg.map((e) => ({ e, tagok: [...e.tagok] })))
}

function visszaallitas(kep: Pillanatkep): Retegek {
  return kep.map((reteg) =>
    reteg.map(({ e, tagok }) => {
      e.tagok = tagok
      return e
    }),
  )
}

export function atlag(ertekek: number[]): number {
  let osszeg = 0
  for (const e of ertekek) osszeg += e
  return osszeg / ertekek.length
}
