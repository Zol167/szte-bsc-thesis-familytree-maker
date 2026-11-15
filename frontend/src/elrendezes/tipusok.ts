// Az elrendező algoritmus bemenete: a backend /api/csaladfak/{id}/graf végpontjának szerkezete,
// csak az elrendezéshez szükséges mezőkkel (a SzemelyDto és CsaladDto típusok ennek megfelelnek).
export interface BemenetiSzemely {
  id: number
  szuletesiEv?: number | null // a testvérek sorrendjéhez
  nem?: string | null // házaspárnál alapértelmezésben a férj kerül balra
}

export interface BemenetiCsalad {
  id: number
  szuloIdk: number[]
  gyermekIdk: number[]
}

export interface ElrendezesBemenet {
  szemelyek: BemenetiSzemely[]
  csaladok: BemenetiCsalad[]
}

// Méretek képpontban és az iterációk száma
export interface ElrendezesBeallitasok {
  kartyaSzelesseg: number
  kartyaMagassag: number
  hazastarsTav: number // két házastárs kártyája között
  testverTav: number // testvérek (azonos szülőcsalád) egységei között
  csaladTav: number // nem rokon egységek között egy soron belül
  generacioTav: number // két generáció sora között
  komponensTav: number // egymással rokonságban nem álló ágak között
  sorrendIteraciok: number // keresztezés-csökkentő söprések száma
  koordinataIteraciok: number // koordináta-kiegyenlítő söprések száma
}

export const ALAP_BEALLITASOK: ElrendezesBeallitasok = {
  kartyaSzelesseg: 160,
  kartyaMagassag: 70,
  hazastarsTav: 30,
  testverTav: 30,
  csaladTav: 60,
  generacioTav: 80,
  komponensTav: 120,
  sorrendIteraciok: 8,
  koordinataIteraciok: 12,
}

export interface ElhelyezettSzemely {
  id: number
  x: number // a kártya bal felső sarka
  y: number
  generacio: number
}

// Egy család vonalai SVG útvonalként (path "d" attribútum)
export interface CsaladVonal {
  csaladId: number
  hazassag: string | null // a két szülőt összekötő vonal
  leszarmazas: string | null // a családtól a gyermekekig vezető "fésű"
}

export interface Elrendezes {
  szemelyek: ElhelyezettSzemely[]
  vonalak: CsaladVonal[]
  szelesseg: number
  magassag: number
  generaciokSzama: number
  keresztezesek: number // a megmaradt vonalkeresztezések száma (szomszédos generációk között)
  figyelmeztetesek: string[]
}
