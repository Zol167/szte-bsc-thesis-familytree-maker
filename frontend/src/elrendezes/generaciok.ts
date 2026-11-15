import type { Csalad, Graf, Komponens } from './graf'
import { UnioHalmaz } from './graf'

export interface GeneracioEredmeny {
  generacio: Map<number, number> // személy → generáció (0 = legfelső sor)
  korkoros: boolean // az adat körkörös (pl. valaki a saját őse), a feltételek nem teljesíthetők
}

// 1. lépés: generációk (sorok) kiosztása három fázisban.
export function generaciokKiosztasa(komponens: Komponens, graf: Graf): GeneracioEredmeny {
  const generacio = new Map<number, number>(komponens.szemelyIdk.map((id) => [id, 0]))
  const korkoros = leghosszabbUtRelaxacio(komponens.csaladok, generacio, komponens.szemelyIdk.length + 2)
  if (!korkoros) gyokerekLeengedese(komponens, graf, generacio)
  tomorites(generacio)
  return { generacio, korkoros }
}

// a) Leghosszabb út felülről lefelé, Bellman–Ford-szerű relaxációval. Feltételek:
//    - egy család szülei (a házastársak) azonos generációban vannak,
//    - minden gyermek legalább eggyel a szülei alatt van,
//    - szülő nélküli családban a testvérek egy generációba kerülnek.
//    Az értékek csak nőnek, ezért körmentes adatnál legfeljebb n körben beáll a fixpont
//    (a házastársi egyenlőség nulla súlyú, a leszármazás egy súlyú él a feltételgráfban).
//    Ha n kör után is változik valami, pozitív kör van: az adat körkörös.
function leghosszabbUtRelaxacio(csaladok: Csalad[], gen: Map<number, number>, maxKor: number): boolean {
  for (let kor = 0; kor < maxKor; kor++) {
    let valtozott = false
    const emel = (id: number, ertek: number) => {
      if (gen.get(id)! < ertek) {
        gen.set(id, ertek)
        valtozott = true
      }
    }
    for (const cs of csaladok) {
      if (cs.szulok.length > 0) {
        const g = maximum(cs.szulok.map((id) => gen.get(id)!))
        cs.szulok.forEach((id) => emel(id, g))
        cs.gyermekek.forEach((id) => emel(id, g + 1))
      } else if (cs.gyermekek.length > 1) {
        const g = maximum(cs.gyermekek.map((id) => gen.get(id)!))
        cs.gyermekek.forEach((id) => emel(id, g))
      }
    }
    if (!valtozott) return false
  }
  return true
}

// b) Gyökerek leengedése. A "gyökér" az, akinek a szülei nincsenek a fában (pl. egy beházasodott
//    házastárs szülei). Az a) fázis után ők a legfelső sorban állnának, messze a gyermeküktől;
//    ehelyett közvetlenül a legfelső gyermekük fölé kerülnek. A gyökereket házastársi csoportonként
//    mozgatjuk (a csoport együtt marad); ha a csoport nem-gyökér házastárshoz kötődik, a helye rögzített.
//    Csak lefelé mozgatunk, és legfeljebb a gyermekek fölé, így egyik feltétel sem sérül.
function gyokerekLeengedese(komponens: Komponens, graf: Graf, gen: Map<number, number>): void {
  const gyermekek = new Set(komponens.csaladok.flatMap((cs) => cs.gyermekek))
  const gyokerek = komponens.szemelyIdk.filter((id) => !gyermekek.has(id))
  const csoportositas = new UnioHalmaz(gyokerek)
  const rogzitett = new Set<number>()
  for (const cs of komponens.csaladok) {
    const gyokerSzulok = cs.szulok.filter((id) => !gyermekek.has(id))
    for (let i = 1; i < gyokerSzulok.length; i++) csoportositas.egyesit(gyokerSzulok[0], gyokerSzulok[i])
    if (gyokerSzulok.length < cs.szulok.length) gyokerSzulok.forEach((id) => rogzitett.add(id))
  }

  const csoportok = new Map<number, number[]>()
  for (const id of gyokerek) {
    const kulcs = csoportositas.keres(id)
    if (!csoportok.has(kulcs)) csoportok.set(kulcs, [])
    csoportok.get(kulcs)!.push(id)
  }
  for (const tagok of csoportok.values()) {
    if (tagok.some((id) => rogzitett.has(id))) continue
    let felsoHatar = Infinity
    for (const id of tagok) {
      for (const cs of graf.sajatCsaladjai.get(id)!) {
        for (const gy of cs.gyermekek) felsoHatar = Math.min(felsoHatar, gen.get(gy)! - 1)
      }
    }
    if (Number.isFinite(felsoHatar) && felsoHatar > gen.get(tagok[0])!) {
      tagok.forEach((id) => gen.set(id, felsoHatar))
    }
  }
}

// c) Tömörítés: a használt generációszámok 0-tól folytonosan újraszámozva (üres sorok nélkül).
//    Monoton átszámozás, így a "gyermek a szülő alatt" és a házastársi egyenlőség megmarad.
function tomorites(gen: Map<number, number>): void {
  const hasznalt = [...new Set(gen.values())].sort((a, b) => a - b)
  const uj = new Map(hasznalt.map((g, i) => [g, i]))
  for (const [id, g] of gen) gen.set(id, uj.get(g)!)
}

function maximum(ertekek: number[]): number {
  let max = -Infinity
  for (const e of ertekek) if (e > max) max = e
  return max
}
