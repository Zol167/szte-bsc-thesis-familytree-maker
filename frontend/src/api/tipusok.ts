// A backend DTO-inak (hu.szte.familytree.dto) TypeScript megfelelői

export interface SzemelyDto {
  id: number
  gedcomId: string | null
  keresztnev: string | null
  vezeteknev: string | null
  nem: string | null
  szuletesiDatum: string | null
  szuletesiEv: number | null
  halalozasiDatum: string | null
}

export interface CsaladDto {
  id: number
  gedcomId: string | null
  hazassagDatum: string | null
  szuloIdk: number[]
  gyermekIdk: number[]
}

export interface ProfilkepDto {
  szemelyId: number
  kepId: number
  url: string
  x: number
  y: number
  szelesseg: number
  magassag: number
}

export interface CsaladfaGrafDto {
  id: number
  nev: string
  tulajdonos: string
  szemelyek: SzemelyDto[]
  csaladok: CsaladDto[]
  profilkepek: ProfilkepDto[]
}

export interface CsaladfaOsszegzesDto {
  id: number
  nev: string
  tulajdonos: string
  importIdo: string
  szemelyDb: number
  csaladDb: number
  egyezesDb: number
}
