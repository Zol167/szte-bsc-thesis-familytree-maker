import { describe, expect, it } from 'vitest'
import { ketretegKeresztezesek } from '../sorrend'

// Élek: [felső vég pozíciója, alsó vég pozíciója]
describe('két réteg közötti keresztezések száma', () => {
  it('párhuzamos élek nem keresztezik egymást', () => {
    expect(ketretegKeresztezesek([[0, 0], [1, 1], [2, 2]])).toBe(0)
  })

  it('fordított sorrendű élpár egy keresztezés', () => {
    expect(ketretegKeresztezesek([[0, 1], [1, 0]])).toBe(1)
  })

  it('teljesen megfordított sorrend: minden pár keresztezi egymást', () => {
    expect(ketretegKeresztezesek([[0, 3], [1, 2], [2, 1], [3, 0]])).toBe(6)
  })

  it('közös felső vagy alsó végű élek nem keresztezik egymást', () => {
    expect(ketretegKeresztezesek([[0.5, 0], [0.5, 1], [0.5, 2]])).toBe(0)
    expect(ketretegKeresztezesek([[0, 1], [2, 1]])).toBe(0)
    expect(ketretegKeresztezesek([[0, 0], [0, 1], [1, 0]])).toBe(1)
  })

  it('üres és egyelemű bemenet', () => {
    expect(ketretegKeresztezesek([])).toBe(0)
    expect(ketretegKeresztezesek([[3, 4]])).toBe(0)
  })
})
