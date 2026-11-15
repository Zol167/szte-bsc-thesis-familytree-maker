import { describe, expect, it } from 'vitest'
import { sorrendTartoElhelyezes } from '../koordinatak'

describe('sorrendtartó elhelyezés (súlyozott izotonikus regresszió)', () => {
  it('a már teljesíthető célokat változatlanul hagyja', () => {
    expect(sorrendTartoElhelyezes([0, 100, 300], [1, 1, 1], [50, 50])).toEqual([0, 100, 300])
  })

  it('azonos célú elemek a cél körül szimmetrikusan nyílnak szét', () => {
    expect(sorrendTartoElhelyezes([100, 100, 100], [1, 1, 1], [10, 10])).toEqual([90, 100, 110])
  })

  it('a sorrendet sértő célokat összevonja, a távolságot megtartja', () => {
    // a második elem a első elé szeretne kerülni: együtt a célok átlaga körül állnak
    expect(sorrendTartoElhelyezes([50, 0], [1, 1], [10])).toEqual([20, 30])
  })

  it('a nagyobb súlyú elem közelebb marad a céljához', () => {
    // 3·x₀² + (x₀ + 10)² minimuma: x₀ = −2,5
    const [x0, x1] = sorrendTartoElhelyezes([0, 0], [3, 1], [10])
    expect(x0).toBeCloseTo(-2.5, 9)
    expect(x1).toBeCloseTo(7.5, 9)
  })

  it('a minimális távolságok mindig teljesülnek', () => {
    const tavok = [30, 50, 20, 40]
    const x = sorrendTartoElhelyezes([500, 10, 300, 0, 250], [1, 2, 1, 5, 1], tavok)
    for (let i = 0; i < tavok.length; i++) expect(x[i + 1] - x[i]).toBeGreaterThanOrEqual(tavok[i] - 1e-9)
  })

  it('üres sor', () => {
    expect(sorrendTartoElhelyezes([], [], [])).toEqual([])
  })
})
