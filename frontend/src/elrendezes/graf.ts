import type { ElrendezesBemenet } from './tipusok'

// Belső, bejárásra előkészített gráf. A család (GEDCOM FAM) a szülőket és a gyermekeket köti össze,
// így a gráf páros: személy – család – személy.
export interface Szemely {
  id: number
  szuletesiEv: number | null
  nem: string | null
  sorszam: number // a bemeneti sorrend, minden rendezés stabil döntetlen-feloldásához
}

export interface Csalad {
  id: number
  szulok: number[]
  gyermekek: number[]
  sorszam: number
}

export interface Graf {
  szemelyek: Map<number, Szemely>
  csaladok: Csalad[]
  szuloCsaladjai: Map<number, Csalad[]> // a családok, amelyekben a személy gyermek
  sajatCsaladjai: Map<number, Csalad[]> // a családok, amelyekben a személy szülő
}

// Egymással rokoni kapcsolatban álló személyek csoportja; a komponensek külön rajzolódnak
export interface Komponens {
  szemelyIdk: number[]
  csaladok: Csalad[]
}

export function grafEpites(bemenet: ElrendezesBemenet): Graf {
  const szemelyek = new Map<number, Szemely>()
  bemenet.szemelyek.forEach((sz, i) => {
    if (!szemelyek.has(sz.id)) {
      szemelyek.set(sz.id, { id: sz.id, szuletesiEv: sz.szuletesiEv ?? null, nem: sz.nem ?? null, sorszam: i })
    }
  })
  const szuloCsaladjai = new Map<number, Csalad[]>()
  const sajatCsaladjai = new Map<number, Csalad[]>()
  for (const id of szemelyek.keys()) {
    szuloCsaladjai.set(id, [])
    sajatCsaladjai.set(id, [])
  }

  const csaladok: Csalad[] = []
  bemenet.csaladok.forEach((cs, i) => {
    // Csak a fában lévő személyekre mutató, ismétlés nélküli hivatkozások; aki a családban szülő,
    // az ugyanott nem lehet gyermek is (hibás adat lenne)
    const szulok = egyedi(cs.szuloIdk.filter((id) => szemelyek.has(id)))
    const gyermekek = egyedi(cs.gyermekIdk.filter((id) => szemelyek.has(id) && !szulok.includes(id)))
    if (szulok.length + gyermekek.length === 0) return
    const csalad: Csalad = { id: cs.id, szulok, gyermekek, sorszam: i }
    csaladok.push(csalad)
    szulok.forEach((id) => sajatCsaladjai.get(id)!.push(csalad))
    gyermekek.forEach((id) => szuloCsaladjai.get(id)!.push(csalad))
  })
  return { szemelyek, csaladok, szuloCsaladjai, sajatCsaladjai }
}

export function komponensek(graf: Graf): Komponens[] {
  const halmazok = new UnioHalmaz(graf.szemelyek.keys())
  for (const cs of graf.csaladok) {
    const tagok = [...cs.szulok, ...cs.gyermekek]
    for (let i = 1; i < tagok.length; i++) halmazok.egyesit(tagok[0], tagok[i])
  }
  // A Map beszúrási sorrendje miatt a komponensek az első személyük bemeneti sorrendjét követik
  const csoportok = new Map<number, Komponens>()
  const sorrendben = [...graf.szemelyek.values()].sort((a, b) => a.sorszam - b.sorszam)
  for (const sz of sorrendben) {
    const kulcs = halmazok.keres(sz.id)
    if (!csoportok.has(kulcs)) csoportok.set(kulcs, { szemelyIdk: [], csaladok: [] })
    csoportok.get(kulcs)!.szemelyIdk.push(sz.id)
  }
  for (const cs of graf.csaladok) {
    const tag = cs.szulok[0] ?? cs.gyermekek[0]
    csoportok.get(halmazok.keres(tag))!.csaladok.push(cs)
  }
  return [...csoportok.values()]
}

// Az egyes generációk bemeneti sorrendjéhez: a legkisebb sorszámú tag
export function legkisebbSorszam(idk: number[], graf: Graf): number {
  let min = Infinity
  for (const id of idk) min = Math.min(min, graf.szemelyek.get(id)!.sorszam)
  return min
}

// Testvérek születési sorrendben. Az ismert születési évű testvérek év szerint rendeződnek abba
// a pozícióhalmazba, amelyet együtt elfoglalnak; az ismeretlen évűek a GEDCOM-beli helyükön maradnak.
export function szuletesiSorrend(idk: number[], graf: Graf): number[] {
  const ismert = idk.filter((id) => graf.szemelyek.get(id)!.szuletesiEv != null)
  const rendezett = [...ismert].sort(
    (a, b) =>
      graf.szemelyek.get(a)!.szuletesiEv! - graf.szemelyek.get(b)!.szuletesiEv! ||
      graf.szemelyek.get(a)!.sorszam - graf.szemelyek.get(b)!.sorszam,
  )
  let k = 0
  return idk.map((id) => (graf.szemelyek.get(id)!.szuletesiEv != null ? rendezett[k++] : id))
}

function egyedi(idk: number[]): number[] {
  return [...new Set(idk)]
}

// Unió–keresés (union–find, diszjunkt halmazok) adatszerkezet útvonal-tömörítéssel
export class UnioHalmaz {
  private readonly szulo = new Map<number, number>()

  constructor(elemek: Iterable<number>) {
    for (const e of elemek) this.szulo.set(e, e)
  }

  keres(elem: number): number {
    let gyoker = elem
    while (this.szulo.get(gyoker) !== gyoker) gyoker = this.szulo.get(gyoker)!
    let akt = elem
    while (this.szulo.get(akt) !== gyoker) {
      const kovetkezo = this.szulo.get(akt)!
      this.szulo.set(akt, gyoker)
      akt = kovetkezo
    }
    return gyoker
  }

  egyesit(a: number, b: number): void {
    const ga = this.keres(a)
    const gb = this.keres(b)
    if (ga !== gb) this.szulo.set(gb, ga)
  }
}
