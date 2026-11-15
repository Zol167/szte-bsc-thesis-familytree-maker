import type { CsaladfaGrafDto, CsaladfaOsszegzesDto } from './tipusok'

// Fejlesztés közben üres: a Vite a /api kéréseket a backendhez továbbítja (vite.config.ts).
// Külön kiszolgált build esetén a VITE_API_URL környezeti változóval adható meg a backend címe.
const API_CIM: string = import.meta.env.VITE_API_URL ?? ''

export class ApiHiba extends Error {
  constructor(
    message: string,
    readonly statusz: number,
  ) {
    super(message)
  }
}

async function lekeres<T>(utvonal: string): Promise<T> {
  let valasz: Response
  try {
    valasz = await fetch(API_CIM + utvonal)
  } catch {
    throw new ApiHiba('A backend nem érhető el. Fut a Spring Boot alkalmazás (mvn spring-boot:run)?', 0)
  }
  if (!valasz.ok) {
    // a backend hibái egységesen {"hiba": "..."} formájúak
    const torzs = (await valasz.json().catch(() => null)) as { hiba?: string } | null
    throw new ApiHiba(torzs?.hiba ?? `A kérés sikertelen (HTTP ${valasz.status})`, valasz.status)
  }
  return (await valasz.json()) as T
}

export function csaladfakListaja(): Promise<CsaladfaOsszegzesDto[]> {
  return lekeres('/api/csaladfak')
}

export function csaladfaGraf(id: number): Promise<CsaladfaGrafDto> {
  return lekeres(`/api/csaladfak/${id}/graf`)
}
