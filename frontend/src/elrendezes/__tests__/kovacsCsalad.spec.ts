import { describe, expect, it } from 'vitest'
import { ALAP_BEALLITASOK, elrendezes } from '..'
import type { CsaladfaGrafDto } from '../../api/tipusok'
import { gedcomSorrend } from '../../utils/formazas'
import kovacsGraf from './adatok/kovacs-graf.json'
import { helyek, invariansokEllenorzese, kozepX } from './tesztSegedek'

// A demo-adatok/kovacs-csalad.ged fájlból a valódi backend által előállított gráf
// (GET /api/csaladfak/{id}/graf válasza), ugyanúgy rendezve, ahogy az alkalmazás teszi
const graf = kovacsGraf as CsaladfaGrafDto
const bemenet = {
  szemelyek: [...graf.szemelyek].sort(gedcomSorrend),
  csaladok: [...graf.csaladok].sort(gedcomSorrend),
}
const e = elrendezes(bemenet)
const hely = helyek(e)
const h = (gedcomId: string) => hely.get(graf.szemelyek.find((sz) => sz.gedcomId === gedcomId)!.id)!
const W = ALAP_BEALLITASOK.kartyaSzelesseg
const SZOMSZED = W + ALAP_BEALLITASOK.hazastarsTav

describe('a Kovács család (valódi backend-adat, 39 személy, 6 generáció)', () => {
  it('teljesíti a szerkezeti feltételeket', () => {
    invariansokEllenorzese(bemenet, e)
    expect(e.generaciokSzama).toBe(6)
    expect(e.figyelmeztetesek).toEqual([])
  })

  it('kevés keresztezés marad (a kereszt-házasságok miatt nulla nem érhető el)', () => {
    expect(e.keresztezesek).toBeLessThanOrEqual(5)
  })

  it('többszörös házasság: Nagy Julianna, Kovács Péter és Varga Rozália egymás mellett, Péter középen', () => {
    const [julianna, peter, rozalia] = [h('@I13@'), h('@I9@'), h('@I14@')]
    const balrol = [julianna, rozalia].sort((a, b) => a.x - b.x)
    expect(peter.x - balrol[0].x).toBeCloseTo(SZOMSZED, 6)
    expect(balrol[1].x - peter.x).toBeCloseTo(SZOMSZED, 6)
  })

  it('ősvesztés: az unokatestvér Horváth Béla és Kovács Margit egymás mellett, fiuk alattuk', () => {
    const [bela, margit, odon] = [h('@I24@'), h('@I19@'), h('@I30@')]
    expect(Math.abs(bela.x - margit.x)).toBeCloseTo(SZOMSZED, 6)
    expect(odon.y).toBeGreaterThan(bela.y)
  })

  it('Horváth Ödön és Molnár Klára a két szülőpár között, mindketten a saját szüleik felé', () => {
    const [odon, klara] = [h('@I30@'), h('@I31@')]
    const odonSzulei = (kozepX(h('@I24@')) + kozepX(h('@I19@'))) / 2 // Béla és Margit
    const klaraSzulei = (kozepX(h('@I26@')) + kozepX(h('@I22@'))) / 2 // Zoltán és Erzsébet
    // azonos irányú sorrend: a két leszármazási vonal nem keresztezi egymást
    expect(Math.sign(kozepX(odon) - kozepX(klara))).toBe(Math.sign(odonSzulei - klaraSzulei))
    const parKozepe = (kozepX(odon) + kozepX(klara)) / 2
    expect(parKozepe).toBeGreaterThanOrEqual(Math.min(odonSzulei, klaraSzulei))
    expect(parKozepe).toBeLessThanOrEqual(Math.max(odonSzulei, klaraSzulei))
  })

  it('a beházasodott Fekete Rózsa szülei közvetlenül fölötte vannak, nem a legfelső sorban', () => {
    expect(h('@I16@').y).toBe(h('@I7@').y) // Fekete Antal a Kovács János generációjában
    expect(h('@I27@').y - h('@I16@').y).toBe(ALAP_BEALLITASOK.kartyaMagassag + ALAP_BEALLITASOK.generacioTav)
  })

  it('egyszülős család: Kovács Gábor pontosan Kovács Gyula alatt', () => {
    expect(kozepX(h('@I32@'))).toBeCloseTo(kozepX(h('@I21@')), 6)
  })
})
