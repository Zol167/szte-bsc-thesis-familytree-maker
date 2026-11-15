import { describe, expect, it } from 'vitest'
import { generaciokKiosztasa } from '../generaciok'
import { grafEpites, komponensek } from '../graf'
import { TesztFa } from './tesztSegedek'

function generaciok(fa: TesztFa) {
  const graf = grafEpites(fa.bemenet())
  const [komponens] = komponensek(graf)
  const eredmeny = generaciokKiosztasa(komponens, graf)
  return {
    gen: (nev: string) => eredmeny.generacio.get(fa.id(nev)),
    osszes: [...eredmeny.generacio.values()],
    korkoros: eredmeny.korkoros,
  }
}

describe('generációk kiosztása', () => {
  it('egyenes ágon minden gyermek eggyel a szülei alatt, a beházasodott fél a párja sorában', () => {
    const { gen } = generaciok(
      new TesztFa().sz('A').sz('B').sz('C').sz('D').sz('E').cs(['A', 'B'], ['C']).cs(['C', 'D'], ['E']),
    )
    expect([gen('A'), gen('B'), gen('C'), gen('D'), gen('E')]).toEqual([0, 0, 1, 1, 2])
  })

  it('a beházasodott házastárs szülei közvetlenül fölé kerülnek, nem a legfelső sorba', () => {
    const { gen } = generaciok(
      new TesztFa()
        .sz('G1').sz('G2').sz('X').sz('Y').sz('A').sz('B').sz('K').sz('H1').sz('H2')
        .cs(['G1', 'G2'], ['X'])
        .cs(['X', 'Y'], ['A'])
        .cs(['A', 'B'], ['K'])
        .cs(['H1', 'H2'], ['B']),
    )
    expect(gen('B')).toBe(2)
    expect(gen('H1')).toBe(1)
    expect(gen('H2')).toBe(1)
    expect(gen('G1')).toBe(0)
  })

  it('nagybácsi–unokahúg házasság: a házastársak egy sorba kerülnek, a gyermekek a szülők alá', () => {
    const { gen } = generaciok(
      new TesztFa()
        .sz('G1').sz('G2').sz('U').sz('S').sz('T').sz('N').sz('K')
        .cs(['G1', 'G2'], ['U', 'S'])
        .cs(['S', 'T'], ['N'])
        .cs(['U', 'N'], ['K']),
    )
    expect(gen('U')).toBe(gen('N'))
    expect(gen('N')).toBeGreaterThan(gen('S')!)
    expect(gen('K')).toBeGreaterThan(gen('U')!)
    expect(gen('U')).toBeGreaterThan(gen('G1')!)
  })

  it('szülő nélküli családban a testvérek egy generációba kerülnek', () => {
    const { gen } = generaciok(
      new TesztFa().sz('A').sz('B').sz('X').sz('C').cs([], ['A', 'B']).cs(['A', 'X'], ['C']),
    )
    expect(gen('A')).toBe(gen('B'))
  })

  it('a generációk számozása folytonos, nincs üres sor', () => {
    const { osszes } = generaciok(
      new TesztFa()
        .sz('G1').sz('G2').sz('X').sz('Y').sz('A').sz('B').sz('K').sz('H1').sz('H2')
        .cs(['G1', 'G2'], ['X'])
        .cs(['X', 'Y'], ['A'])
        .cs(['A', 'B'], ['K'])
        .cs(['H1', 'H2'], ['B']),
    )
    const max = Math.max(...osszes)
    expect(new Set(osszes)).toEqual(new Set(Array.from({ length: max + 1 }, (_, i) => i)))
  })

  it('felismeri a körkörös adatot (valaki a saját őse) és nem akad el', () => {
    const { korkoros } = generaciok(new TesztFa().sz('A').sz('B').cs(['A'], ['B']).cs(['B'], ['A']))
    expect(korkoros).toBe(true)
  })
})
