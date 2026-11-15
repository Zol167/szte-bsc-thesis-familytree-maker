import { expect } from 'vitest'
import {
  ALAP_BEALLITASOK,
  type BemenetiCsalad,
  type BemenetiSzemely,
  type ElhelyezettSzemely,
  type Elrendezes,
  type ElrendezesBemenet,
} from '..'

// Tesztfák építése nevekkel: new TesztFa().sz('Apa', 1900, 'M').sz('Anya').cs(['Apa', 'Anya'], ['Gyerek'])
export class TesztFa {
  readonly szemelyek: BemenetiSzemely[] = []
  readonly csaladok: BemenetiCsalad[] = []
  private readonly idk = new Map<string, number>()

  sz(nev: string, szuletesiEv: number | null = null, nem: 'M' | 'F' | null = null): this {
    const id = this.szemelyek.length + 1
    this.szemelyek.push({ id, szuletesiEv, nem })
    this.idk.set(nev, id)
    return this
  }

  cs(szulok: string[], gyermekek: string[] = []): this {
    this.csaladok.push({
      id: 1000 + this.csaladok.length,
      szuloIdk: szulok.map((nev) => this.id(nev)),
      gyermekIdk: gyermekek.map((nev) => this.id(nev)),
    })
    return this
  }

  id(nev: string): number {
    const id = this.idk.get(nev)
    if (id === undefined) throw new Error(`Ismeretlen név a tesztfában: ${nev}`)
    return id
  }

  bemenet(): ElrendezesBemenet {
    return { szemelyek: this.szemelyek, csaladok: this.csaladok }
  }
}

export function helyek(e: Elrendezes): Map<number, ElhelyezettSzemely> {
  return new Map(e.szemelyek.map((h) => [h.id, h]))
}

export function kozepX(h: ElhelyezettSzemely, be = ALAP_BEALLITASOK): number {
  return h.x + be.kartyaSzelesseg / 2
}

// A "strukturálisan helyes családfa" feltételei, amelyeknek minden elrendezésnek teljesülnie kell
export function invariansokEllenorzese(bemenet: ElrendezesBemenet, e: Elrendezes, be = ALAP_BEALLITASOK): void {
  const hely = helyek(e)

  // 1. minden személy pontosan egyszer szerepel (ősvesztésnél sem duplikálódik)
  expect(e.szemelyek).toHaveLength(bemenet.szemelyek.length)
  expect(hely.size).toBe(bemenet.szemelyek.length)

  for (const cs of bemenet.csaladok) {
    // 2. minden gyermek a szülei alatt van
    for (const szulo of cs.szuloIdk) {
      for (const gyermek of cs.gyermekIdk) {
        expect(hely.get(gyermek)!.y, `a(z) ${gyermek} gyermek a(z) ${szulo} szülő alatt`).toBeGreaterThan(
          hely.get(szulo)!.y,
        )
      }
    }
    // 3. a házastársak egy sorban vannak
    if (cs.szuloIdk.length === 2) {
      expect(hely.get(cs.szuloIdk[0])!.y).toBe(hely.get(cs.szuloIdk[1])!.y)
    }
  }

  // 4. egy soron belül nincs átfedés (legalább a legkisebb megengedett hézag)
  const minHezag = Math.min(be.hazastarsTav, be.testverTav, be.csaladTav)
  const sorok = new Map<number, ElhelyezettSzemely[]>()
  for (const h of e.szemelyek) {
    if (!sorok.has(h.y)) sorok.set(h.y, [])
    sorok.get(h.y)!.push(h)
  }
  for (const sor of sorok.values()) {
    sor.sort((a, b) => a.x - b.x)
    for (let i = 1; i < sor.length; i++) {
      expect(sor[i].x - sor[i - 1].x).toBeGreaterThanOrEqual(be.kartyaSzelesseg + minHezag - 1e-6)
    }
  }

  // 5. minden a rajz méretén belül van, és minden családnak van vonala
  for (const h of e.szemelyek) {
    expect(h.x).toBeGreaterThanOrEqual(-1e-6)
    expect(h.x + be.kartyaSzelesseg).toBeLessThanOrEqual(e.szelesseg + 1e-6)
    expect(h.y + be.kartyaMagassag).toBeLessThanOrEqual(e.magassag + 1e-6)
  }
  expect(e.vonalak).toHaveLength(bemenet.csaladok.length)
}

// Determinisztikus álvéletlen-generátor (mulberry32) a szintetikus tesztfákhoz
export function alveletlen(mag: number): () => number {
  let a = mag >>> 0
  return () => {
    a = (a + 0x6d2b79f5) >>> 0
    let t = a
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

// Nagy, de érvényes szintetikus fa: alapító házaspárok, generációnként 0–4 gyermek, akik gyakran
// házasodnak; a beházasodott fél szülei néha szintén ismertek, és ritkán van második házasság is.
export function szintetikusFa(celSzam: number, mag: number): ElrendezesBemenet {
  const veletlen = alveletlen(mag)
  const szemelyek: BemenetiSzemely[] = []
  const csaladok: BemenetiCsalad[] = []
  const uj = (szuletesiEv: number, nem: 'M' | 'F') => {
    const id = szemelyek.length + 1
    szemelyek.push({ id, szuletesiEv, nem })
    return id
  }
  const csalad = (szuloIdk: number[], gyermekIdk: number[]) =>
    csaladok.push({ id: csaladok.length + 1, szuloIdk, gyermekIdk })

  let hazasparok: Array<{ ferj: number; feleseg: number; ev: number }> = []
  for (let i = 0; i < 6; i++) hazasparok.push({ ferj: uj(1700, 'M'), feleseg: uj(1702, 'F'), ev: 1700 })
  while (szemelyek.length < celSzam && hazasparok.length > 0) {
    const kovetkezo: typeof hazasparok = []
    for (const { ferj, feleseg, ev } of hazasparok) {
      const gyermekek: number[] = []
      const db = Math.floor(veletlen() * 5)
      for (let k = 0; k < db && szemelyek.length < celSzam; k++) {
        const nem = veletlen() < 0.5 ? 'M' : 'F'
        const gyermek = uj(ev + 25 + 2 * k, nem)
        gyermekek.push(gyermek)
        if (veletlen() < 0.8) {
          const tars = uj(ev + 26, nem === 'M' ? 'F' : 'M')
          if (veletlen() < 0.2) csalad([uj(ev, 'M'), uj(ev + 2, 'F')], [tars])
          kovetkezo.push(nem === 'M' ? { ferj: gyermek, feleseg: tars, ev: ev + 25 } : { ferj: tars, feleseg: gyermek, ev: ev + 25 })
          if (veletlen() < 0.05) {
            const masodik = uj(ev + 30, nem === 'M' ? 'F' : 'M')
            kovetkezo.push(nem === 'M' ? { ferj: gyermek, feleseg: masodik, ev: ev + 30 } : { ferj: masodik, feleseg: gyermek, ev: ev + 30 })
          }
        }
      }
      csalad([ferj, feleseg], gyermekek)
    }
    hazasparok = kovetkezo
  }
  return { szemelyek, csaladok }
}
