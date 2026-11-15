import { describe, expect, it } from 'vitest'
import { ALAP_BEALLITASOK, elrendezes } from '..'
import { helyek, invariansokEllenorzese, kozepX, szintetikusFa, TesztFa } from './tesztSegedek'

const W = ALAP_BEALLITASOK.kartyaSzelesseg
const be = ALAP_BEALLITASOK

function rendez(fa: TesztFa) {
  const bemenet = fa.bemenet()
  const e = elrendezes(bemenet)
  invariansokEllenorzese(bemenet, e)
  const hely = helyek(e)
  const h = (nev: string) => hely.get(fa.id(nev))!
  const kx = (nev: string) => kozepX(h(nev))
  return { e, h, kx }
}

describe('egyszerű ős/leszármazott ágak', () => {
  it('házaspár két gyermekkel: a férj balra, a gyermekek születési sorrendben, középen alattuk', () => {
    const { e, h, kx } = rendez(
      new TesztFa()
        .sz('Apa', 1900, 'M').sz('Anya', 1902, 'F').sz('Fiatalabb', 1930).sz('Idosebb', 1925)
        .cs(['Anya', 'Apa'], ['Fiatalabb', 'Idosebb']),
    )
    expect(h('Anya').x - h('Apa').x).toBeCloseTo(W + be.hazastarsTav, 6)
    expect(h('Idosebb').x).toBeLessThan(h('Fiatalabb').x)
    expect((kx('Idosebb') + kx('Fiatalabb')) / 2).toBeCloseTo((kx('Apa') + kx('Anya')) / 2, 6)
    expect(e.generaciokSzama).toBe(2)
    expect(e.keresztezesek).toBe(0)
  })

  it('egyetlen gyermek pontosan a szülők közepe alatt', () => {
    const { kx } = rendez(new TesztFa().sz('Apa', 1900, 'M').sz('Anya', 1902, 'F').sz('Gyerek', 1925).cs(['Apa', 'Anya'], ['Gyerek']))
    expect(kx('Gyerek')).toBeCloseTo((kx('Apa') + kx('Anya')) / 2, 6)
  })

  it('a testvérek születési év szerint rendeződnek, az ismeretlen évű a helyén marad', () => {
    const { h } = rendez(
      new TesztFa()
        .sz('Apa', 1880, 'M').sz('Anya', 1882, 'F').sz('A', 1910).sz('B').sz('C', 1900)
        .cs(['Apa', 'Anya'], ['A', 'B', 'C']),
    )
    expect(h('C').x).toBeLessThan(h('B').x)
    expect(h('B').x).toBeLessThan(h('A').x)
  })

  it('négy generáció egyenes ágon, beházasodott házastársakkal', () => {
    const { e, kx } = rendez(
      new TesztFa()
        .sz('Dedapa', 1840, 'M').sz('Dedanya', 1842, 'F').sz('Nagyapa', 1865, 'M').sz('Nagyanya', 1867, 'F')
        .sz('Apa', 1890, 'M').sz('Anya', 1892, 'F').sz('En', 1920)
        .cs(['Dedapa', 'Dedanya'], ['Nagyapa'])
        .cs(['Nagyapa', 'Nagyanya'], ['Apa'])
        .cs(['Apa', 'Anya'], ['En']),
    )
    expect(e.generaciokSzama).toBe(4)
    expect(e.keresztezesek).toBe(0)
    // minden egyke gyermek a szülei horgonya alatt
    expect(kx('Nagyapa')).toBeCloseTo((kx('Dedapa') + kx('Dedanya')) / 2, 6)
    expect(kx('En')).toBeCloseTo((kx('Apa') + kx('Anya')) / 2, 6)
  })

  it('a beházasodott házastárs ismert szülei a házastárs fölé kerülnek, keresztezés nélkül', () => {
    const { e, h, kx } = rendez(
      new TesztFa()
        .sz('G1', 1850, 'M').sz('G2', 1852, 'F').sz('X', 1875, 'M').sz('Y', 1877, 'F').sz('A', 1900, 'M')
        .sz('B', 1902, 'F').sz('K', 1930).sz('H1', 1872, 'M').sz('H2', 1874, 'F')
        .cs(['G1', 'G2'], ['X'])
        .cs(['X', 'Y'], ['A'])
        .cs(['A', 'B'], ['K'])
        .cs(['H1', 'H2'], ['B']),
    )
    expect(h('H1').y).toBe(h('X').y) // nem a legfelső sorban
    expect(h('A').x).toBeLessThan(h('B').x)
    expect((kx('X') + kx('Y')) / 2).toBeLessThan((kx('H1') + kx('H2')) / 2)
    expect(e.keresztezesek).toBe(0)
  })
})

describe('összetett esetek', () => {
  it('többszörös házasság: a személy két házastársa két oldalra, a gyermekek a megfelelő házasság alá kerülnek', () => {
    const { e, h, kx } = rendez(
      new TesztFa()
        .sz('Elso', 1880, 'F').sz('Ferj', 1878, 'M').sz('Masodik', 1885, 'F').sz('A', 1900).sz('B', 1902).sz('C', 1910)
        .cs(['Ferj', 'Elso'], ['A', 'B'])
        .cs(['Ferj', 'Masodik'], ['C']),
    )
    expect(h('Elso').x).toBeLessThan(h('Ferj').x)
    expect(h('Ferj').x).toBeLessThan(h('Masodik').x)
    expect(h('B').x).toBeLessThan(h('C').x) // a féltestvérek házasságonként csoportosulnak
    const elsoHorgony = (kx('Elso') + kx('Ferj')) / 2
    const masodikHorgony = (kx('Ferj') + kx('Masodik')) / 2
    expect(Math.abs((kx('A') + kx('B')) / 2 - elsoHorgony)).toBeLessThan(W)
    expect(Math.abs(kx('C') - masodikHorgony)).toBeLessThan(W)
    expect(e.keresztezesek).toBe(0)
  })

  it('ősvesztés (unokatestvérek házassága): a közös ősök egyszer szerepelnek, keresztezés nélkül', () => {
    const fa = new TesztFa()
      .sz('G1', 1850, 'M').sz('G2', 1852, 'F').sz('A', 1875, 'M').sz('B', 1878, 'M').sz('SA', 1877, 'F')
      .sz('SB', 1880, 'F').sz('C', 1900, 'M').sz('D', 1902, 'F').sz('E', 1925)
      .cs(['G1', 'G2'], ['A', 'B'])
      .cs(['A', 'SA'], ['C'])
      .cs(['B', 'SB'], ['D'])
      .cs(['C', 'D'], ['E'])
    const { e, h } = rendez(fa)
    expect(e.generaciokSzama).toBe(4)
    expect(Math.abs(h('C').x - h('D').x)).toBeCloseTo(W + be.hazastarsTav, 6) // az unokatestvér-házaspár egymás mellett
    expect(e.keresztezesek).toBe(0)
  })

  it('generációütközés (nagybácsi és unokahúga házassága): a szerkezet így is helyes', () => {
    const { e } = rendez(
      new TesztFa()
        .sz('G1', 1840, 'M').sz('G2', 1842, 'F').sz('U', 1870, 'M').sz('S', 1865, 'F').sz('T', 1863, 'M')
        .sz('N', 1890, 'F').sz('K', 1915)
        .cs(['G1', 'G2'], ['U', 'S'])
        .cs(['T', 'S'], ['N'])
        .cs(['U', 'N'], ['K']),
    )
    expect(e.generaciokSzama).toBe(4)
    expect(e.figyelmeztetesek).toEqual([])
  })

  it('örökbefogadás: két szülőcsaládhoz tartozó gyermek (a dummy.ged szerkezete)', () => {
    const { kx } = rendez(
      new TesztFa()
        .sz('Robert', 1822, 'M').sz('Mary', 1828, 'F').sz('Joe', 1861, 'M')
        .cs(['Robert', 'Mary'], ['Joe'])
        .cs(['Robert'], ['Joe']),
    )
    // a gyermek a két szülőcsalád horgonya között
    const horgonyok = [(kx('Robert') + kx('Mary')) / 2, kx('Robert')]
    expect(kx('Joe')).toBeGreaterThanOrEqual(Math.min(...horgonyok) - 1e-6)
    expect(kx('Joe')).toBeLessThanOrEqual(Math.max(...horgonyok) + 1e-6)
  })

  it('három házasság: csillag alakú egység, a középső személy mindkét oldalán házastárssal', () => {
    const { h } = rendez(
      new TesztFa()
        .sz('Ferj', 1870, 'M').sz('F1', 1872, 'F').sz('F2', 1875, 'F').sz('F3', 1880, 'F')
        .sz('A', 1895).sz('B', 1900).sz('C', 1905)
        .cs(['Ferj', 'F1'], ['A'])
        .cs(['Ferj', 'F2'], ['B'])
        .cs(['Ferj', 'F3'], ['C']),
    )
    const balra = ['F1', 'F2', 'F3'].filter((n) => h(n).x < h('Ferj').x)
    expect(balra.length).toBeGreaterThan(0)
    expect(balra.length).toBeLessThan(3)
  })
})

describe('szélsőséges bemenetek', () => {
  it('az egymással rokonságban nem álló ágak egymás mellé kerülnek, átfedés nélkül', () => {
    const fa = new TesztFa()
      .sz('A1', 1900, 'M').sz('A2', 1902, 'F').sz('A3', 1925)
      .sz('B1', 1910, 'M').sz('B2', 1912, 'F').sz('B3', 1935)
      .cs(['A1', 'A2'], ['A3'])
      .cs(['B1', 'B2'], ['B3'])
    const { h } = rendez(fa)
    const elsoJobbSzele = Math.max(h('A1').x, h('A2').x, h('A3').x) + W
    expect(Math.min(h('B1').x, h('B2').x, h('B3').x) - elsoJobbSzele).toBeCloseTo(be.komponensTav, 6)
  })

  it('a kapcsolat nélküli személyek szorosan egymás mellé kerülnek', () => {
    const { h } = rendez(new TesztFa().sz('A').sz('B').sz('C'))
    expect(h('B').x - h('A').x).toBe(W + be.csaladTav)
    expect(h('C').x - h('B').x).toBe(W + be.csaladTav)
  })

  it('üres családfa', () => {
    const e = elrendezes({ szemelyek: [], csaladok: [] })
    expect(e).toMatchObject({ szemelyek: [], vonalak: [], szelesseg: 0, magassag: 0, generaciokSzama: 0 })
  })

  it('körkörös adat esetén is befejeződik és figyelmeztet', () => {
    const fa = new TesztFa().sz('A').sz('B').cs(['A'], ['B']).cs(['B'], ['A'])
    const e = elrendezes(fa.bemenet())
    expect(e.szemelyek).toHaveLength(2)
    expect(e.figyelmeztetesek).toHaveLength(1)
  })

  it('ismeretlen személyre mutató és ismétlődő hivatkozásokat figyelmen kívül hagy', () => {
    const e = elrendezes({
      szemelyek: [{ id: 1 }, { id: 2 }, { id: 3 }],
      csaladok: [{ id: 10, szuloIdk: [1, 2, 99], gyermekIdk: [3, 3, 98] }],
    })
    expect(e.szemelyek).toHaveLength(3)
    expect(e.generaciokSzama).toBe(2)
  })

  it('determinisztikus: ugyanarra a bemenetre ugyanazt adja', () => {
    const bemenet = szintetikusFa(300, 7)
    expect(elrendezes(bemenet)).toEqual(elrendezes(bemenet))
  })

  it('nagy fa (1500 személy): helyes szerkezet, elfogadható idő alatt', () => {
    const bemenet = szintetikusFa(1500, 42)
    const kezdet = performance.now()
    const e = elrendezes(bemenet)
    const ido = performance.now() - kezdet
    invariansokEllenorzese(bemenet, e)
    expect(e.generaciokSzama).toBeGreaterThan(5)
    expect(ido).toBeLessThan(3000)
  })
})
