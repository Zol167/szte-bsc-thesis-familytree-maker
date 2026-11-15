import { describe, expect, it } from 'vitest'
import { eletEvek, evADatumbol, rovidites, teljesNev } from '../formazas'

describe('formázás', () => {
  it('teljes név magyar sorrendben, hiányzó részekkel is', () => {
    expect(teljesNev({ vezeteknev: 'Kovács', keresztnev: 'János' })).toBe('Kovács János')
    expect(teljesNev({ vezeteknev: null, keresztnev: 'János' })).toBe('János')
    expect(teljesNev({ vezeteknev: null, keresztnev: null })).toBe('Névtelen')
  })

  it('évszám a GEDCOM dátumokból', () => {
    expect(evADatumbol('14 Apr 1905')).toBe(1905)
    expect(evADatumbol('ABT 987')).toBe(987)
    expect(evADatumbol('ismeretlen')).toBeNull()
    expect(evADatumbol(null)).toBeNull()
  })

  it('életévek a rendelkezésre álló adatokból', () => {
    expect(eletEvek({ szuletesiEv: 1822, halalozasiDatum: '14 Apr 1905' })).toBe('1822–1905')
    expect(eletEvek({ szuletesiEv: 1822, halalozasiDatum: null })).toBe('sz. 1822')
    expect(eletEvek({ szuletesiEv: null, halalozasiDatum: '1905' })).toBe('† 1905')
    expect(eletEvek({ szuletesiEv: null, halalozasiDatum: null })).toBe('')
  })

  it('a hosszú szöveg három ponttal rövidül', () => {
    expect(rovidites('Rövid', 10)).toBe('Rövid')
    expect(rovidites('Nagyon hosszú vezetéknév', 10)).toBe('Nagyon ho…')
  })
})
