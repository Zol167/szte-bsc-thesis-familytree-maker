import type { SzemelyDto } from '../api/tipusok'

// Magyar névsorrend: vezetéknév, utána keresztnév
export function teljesNev(sz: Pick<SzemelyDto, 'vezeteknev' | 'keresztnev'>): string {
  return [sz.vezeteknev, sz.keresztnev].filter(Boolean).join(' ') || 'Névtelen'
}

// Az első 3–4 jegyű szám egy GEDCOM dátumban ("ABT 1905", "14 Apr 1905"), mint a backend parserében
export function evADatumbol(datum: string | null): number | null {
  const talalat = datum?.match(/\b(\d{3,4})\b/)
  return talalat ? Number(talalat[1]) : null
}

// "1822–1905", "sz. 1822", "† 1905" vagy üres
export function eletEvek(sz: Pick<SzemelyDto, 'szuletesiEv' | 'halalozasiDatum'>): string {
  const szuletes = sz.szuletesiEv
  const halal = evADatumbol(sz.halalozasiDatum)
  if (szuletes != null && halal != null) return `${szuletes}–${halal}`
  if (szuletes != null) return `sz. ${szuletes}`
  if (halal != null) return `† ${halal}`
  return ''
}

// A GEDCOM fájlbeli sorrend visszaállítása az azonosítókból ("@I2@" < "@I10@"). A backend tetszőleges
// sorrendben adja vissza a személyeket; az elrendezés döntetlen esetekben (pl. ismeretlen születési évű
// testvérek, a házasságok sorrendje) a bemeneti sorrendet követi, így az eredmény ettől független lesz.
export function gedcomSorrend(a: { gedcomId: string | null; id: number }, b: { gedcomId: string | null; id: number }): number {
  const szam = (gedcomId: string | null) => Number(gedcomId?.match(/\d+/)?.[0] ?? NaN)
  const sa = szam(a.gedcomId)
  const sb = szam(b.gedcomId)
  if (!Number.isNaN(sa) && !Number.isNaN(sb) && sa !== sb) return sa - sb
  return (a.gedcomId ?? '').localeCompare(b.gedcomId ?? '') || a.id - b.id
}

// A kártyára nem férő szöveg levágása három ponttal
export function rovidites(szoveg: string, maxHossz: number): string {
  return szoveg.length > maxHossz ? szoveg.slice(0, maxHossz - 1) + '…' : szoveg
}
