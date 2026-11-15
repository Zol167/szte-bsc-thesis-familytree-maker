import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { CsaladDto, SzemelyDto } from '../../api/tipusok'
import { elrendezes } from '../../elrendezes'
import CsaladfaRajz from '../CsaladfaRajz.vue'

const szemely = (id: number, vezeteknev: string, keresztnev: string, nem: string, ev: number, halal: string | null): SzemelyDto => ({
  id,
  gedcomId: `@I${id}@`,
  keresztnev,
  vezeteknev,
  nem,
  szuletesiDatum: String(ev),
  szuletesiEv: ev,
  halalozasiDatum: halal,
})

const szemelyek = [
  szemely(1, 'Kovács', 'István', 'M', 1842, '1910'),
  szemely(2, 'Szabó', 'Erzsébet', 'F', 1846, '1915'),
  szemely(3, 'Kovács', 'János', 'M', 1868, null),
]
const csaladok: CsaladDto[] = [{ id: 10, gedcomId: '@F1@', hazassagDatum: null, szuloIdk: [1, 2], gyermekIdk: [3] }]

describe('CsaladfaRajz', () => {
  const rajz = mount(CsaladfaRajz, {
    props: {
      elrendezes: elrendezes({ szemelyek, csaladok }),
      szemelyek: new Map(szemelyek.map((sz) => [sz.id, sz])),
    },
  })

  it('minden személyhez egy kártyát rajzol natív SVG elemekből', () => {
    expect(rajz.findAll('g.szemely-kartya')).toHaveLength(3)
    expect(rajz.findAll('g.szemely-kartya rect.hatter')).toHaveLength(3)
  })

  it('kirajzolja a házastársi és a leszármazási vonalat', () => {
    expect(rajz.findAll('path.hazassag')).toHaveLength(1)
    expect(rajz.findAll('path.leszarmazas')).toHaveLength(1)
  })

  it('a kártyán a név magyar sorrendben és az életévek látszanak, a nem színnel jelölve', () => {
    const kartya = rajz.findAll('g.szemely-kartya')[0]
    expect(kartya.find('.vezeteknev').text()).toBe('Kovács')
    expect(kartya.find('.keresztnev').text()).toBe('István')
    expect(kartya.find('.evek').text()).toBe('1842–1910')
    expect(kartya.classes()).toContain('ferfi')
    expect(rajz.findAll('g.szemely-kartya')[1].classes()).toContain('no')
  })

  it('1:1 méretben a rajz akkora, mint az elrendezés (plusz margó); igazításnál a viewBox méretez', () => {
    const e = elrendezes({ szemelyek, csaladok })
    expect(rajz.attributes('width')).toBe(String(e.szelesseg + 80))
    expect(rajz.attributes('viewBox')).toBe(`0 0 ${e.szelesseg + 80} ${e.magassag + 80}`)
    const igazitott = mount(CsaladfaRajz, {
      props: { elrendezes: e, szemelyek: new Map(szemelyek.map((sz) => [sz.id, sz])), igazitas: true },
    })
    expect(igazitott.classes()).toContain('igazitott')
    expect(igazitott.attributes('width')).toBeUndefined()
  })

  it('a kártyák a kiszámított koordinátákra kerülnek', () => {
    const e = elrendezes({ szemelyek, csaladok })
    const elso = e.szemelyek[0]
    expect(rajz.findAll('g.szemely-kartya')[0].attributes('transform')).toBe(`translate(${elso.x} ${elso.y})`)
  })
})
