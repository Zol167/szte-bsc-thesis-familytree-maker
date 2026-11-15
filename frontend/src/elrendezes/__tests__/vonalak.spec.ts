import { describe, expect, it } from 'vitest'
import { ALAP_BEALLITASOK, elrendezes } from '..'
import { savokKiosztasa, savokSorrendje } from '../vonalak'
import { helyek, kozepX, TesztFa } from './tesztSegedek'

const W = ALAP_BEALLITASOK.kartyaSzelesseg
const H = ALAP_BEALLITASOK.kartyaMagassag

describe('sávkiosztás a gyűjtővonalakhoz', () => {
  it('nem átfedő intervallumok egy sávba kerülnek', () => {
    expect(savokKiosztasa([{ kezd: 0, veg: 10 }, { kezd: 20, veg: 30 }], 6)).toEqual({ sav: [0, 0], savokSzama: 1 })
  })

  it('az átfedők külön sávba, a felszabadult sáv újra használható', () => {
    const eredmeny = savokKiosztasa(
      [
        { kezd: 0, veg: 50 },
        { kezd: 40, veg: 80 },
        { kezd: 60, veg: 100 },
      ],
      6,
    )
    expect(eredmeny).toEqual({ sav: [0, 1, 0], savokSzama: 2 })
  })

  it('a hézagnál közelebb álló intervallumok is külön sávba kerülnek', () => {
    expect(savokKiosztasa([{ kezd: 0, veg: 10 }, { kezd: 14, veg: 20 }], 6).savokSzama).toBe(2)
  })
})

describe('a sávok sorrendje', () => {
  it('úgy rendezi a sávokat, hogy a függőleges vonalak ne metsszék a gyűjtővonalakat', () => {
    // 0. család: széles (0–1000), gyermekei a két szélen; 1. család: horgonya 950-nél (a széles vonal
    // fölött), gyermeke 1100-nál. Kezdeti sorrendben 2 metszés van, a sávok cseréjével egy sem.
    const fesuk = [
      { kezd: 0, veg: 1000, horgonyX: 500, gyermekXek: [0, 1000] },
      { kezd: 950, veg: 1100, horgonyX: 950, gyermekXek: [1100] },
    ]
    const sorrend = savokSorrendje(fesuk, [0, 1], 2)
    expect(sorrend[1]).toBeLessThan(sorrend[0]) // az 1. család sávja kerül feljebb
  })

  it('ha a csere nem javít, marad az eredeti sorrend', () => {
    const fesuk = [
      { kezd: 0, veg: 1000, horgonyX: 500, gyermekXek: [0, 1000] },
      { kezd: 300, veg: 400, horgonyX: 350, gyermekXek: [300, 400] },
    ]
    expect(savokSorrendje(fesuk, [0, 1], 2)).toEqual([0, 1])
  })

  it('egy sávnál nincs mit rendezni', () => {
    expect(savokSorrendje([{ kezd: 0, veg: 10, horgonyX: 5, gyermekXek: [0, 10] }], [0], 1)).toEqual([0])
  })
})

describe('családi vonalak geometriája', () => {
  const fa = new TesztFa().sz('Apa', 1900, 'M').sz('Anya', 1902, 'F').sz('Gyerek', 1925).cs(['Apa', 'Anya'], ['Gyerek'])
  const e = elrendezes(fa.bemenet())
  const hely = helyek(e)
  const apa = hely.get(fa.id('Apa'))!
  const anya = hely.get(fa.id('Anya'))!
  const gyerek = hely.get(fa.id('Gyerek'))!
  const [vonal] = e.vonalak

  it('a házastársi vonal a két kártya között, félmagasságban fut', () => {
    expect(vonal.hazassag).toBe(`M ${apa.x + W} ${apa.y + H / 2} H ${anya.x}`)
  })

  it('a leszármazási vonal a házastársi vonal közepéből indul és a gyermek kártyájának tetején ér véget', () => {
    const horgonyX = (apa.x + W + anya.x) / 2
    expect(vonal.leszarmazas!.startsWith(`M ${horgonyX} ${apa.y + H / 2} V `)).toBe(true)
    expect(vonal.leszarmazas!.endsWith(`M ${kozepX(gyerek)} ${gyerek.y - ALAP_BEALLITASOK.generacioTav / 2} V ${gyerek.y}`)).toBe(true)
  })

  it('egyszülős családnál a horgony a szülő kártyájának alja', () => {
    const egyszulos = new TesztFa().sz('Anya', 1900, 'F').sz('Gyerek', 1925).cs(['Anya'], ['Gyerek'])
    const e2 = elrendezes(egyszulos.bemenet())
    const anya2 = helyek(e2).get(egyszulos.id('Anya'))!
    expect(e2.vonalak[0].hazassag).toBeNull()
    expect(e2.vonalak[0].leszarmazas!.startsWith(`M ${kozepX(anya2)} ${anya2.y + H} V `)).toBe(true)
  })

  it('gyermektelen házaspárnak csak házastársi vonala van', () => {
    const par = new TesztFa().sz('A', 1900, 'M').sz('B', 1901, 'F').cs(['A', 'B'])
    const [v] = elrendezes(par.bemenet()).vonalak
    expect(v.hazassag).not.toBeNull()
    expect(v.leszarmazas).toBeNull()
  })
})
