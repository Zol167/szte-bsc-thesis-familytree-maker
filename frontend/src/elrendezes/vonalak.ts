import type { Graf } from './graf'
import type { CsaladVonal, ElhelyezettSzemely, ElrendezesBeallitasok } from './tipusok'

export interface Intervallum {
  kezd: number
  veg: number
}

interface Pont {
  x: number
  y: number
}

// Egy család "fésűje": a horgonytól függőlegesen le a gyűjtővonalig, a gyűjtővonal vízszintesen
// végig a gyermekek fölött, onnan függőlegesen le minden gyermekhez
interface Fesu {
  vonalIndex: number
  horgony: Pont | null
  gyermekek: ElhelyezettSzemely[]
  savTeteje: number // a generációk közötti sáv felső széle (a szülők sorának alja)
  kezd: number
  veg: number
}

const SAV_LEPES = 14 // két egymás alatti gyűjtővonal távolsága (sok sávnál összébb húzódik)
const SAV_MARGO = 14 // a gyűjtővonalak és a kártyák között hagyott hely
const U_MELYSEG = 12 // nem szomszédos házastársak összekötőjének mélysége a kártyák alatt

// Mohó sávkiosztás: az egymást (hézaggal együtt) átfedő intervallumok különböző sávba kerülnek.
// Kezdőpont szerint rendezve és az első szabad sávot választva ez a lehető legkevesebb sávot adja
// (intervallumgráf színezése; a sávok száma a legnagyobb egyszerre átfedő csoport mérete).
export function savokKiosztasa(intervallumok: Intervallum[], hezag: number): { sav: number[]; savokSzama: number } {
  const sorrend = intervallumok.map((_, i) => i).sort((a, b) => intervallumok[a].kezd - intervallumok[b].kezd || a - b)
  const savVegek: number[] = []
  const sav: number[] = new Array(intervallumok.length)
  for (const i of sorrend) {
    const { kezd, veg } = intervallumok[i]
    let s = savVegek.findIndex((v) => v + hezag < kezd)
    if (s === -1) {
      s = savVegek.length
      savVegek.push(veg)
    } else {
      savVegek[s] = veg
    }
    sav[i] = s
  }
  return { sav, savokSzama: savVegek.length }
}

export interface SavFesu extends Intervallum {
  horgonyX: number | null
  gyermekXek: number[]
}

// A sávok egymás alatti sorrendje. Egy család horgonyától lefutó függőleges vonal a fölötte lévő
// sávok gyűjtővonalait metszheti, a gyermekeihez lefutó vonalak az alatta lévőkét. A sávok minden
// sorrendjét kipróbáljuk (legfeljebb 5 sávnál, 5! = 120 eset), és a legkevesebb metszést adót választjuk.
// Eredmény: régi sáv → új sáv.
export function savokSorrendje(fesuk: SavFesu[], sav: number[], savokSzama: number): number[] {
  const azonos = Array.from({ length: savokSzama }, (_, i) => i)
  if (savokSzama < 2 || savokSzama > 5) return azonos
  const metszesek = (perm: number[]) => {
    let db = 0
    fesuk.forEach((fi, i) => {
      fesuk.forEach((fj, j) => {
        const si = perm[sav[i]]
        const sj = perm[sav[j]]
        if (sj < si && fi.horgonyX !== null && fj.kezd < fi.horgonyX && fi.horgonyX < fj.veg) db++
        if (sj > si) for (const x of fi.gyermekXek) if (fj.kezd < x && x < fj.veg) db++
      })
    })
    return db
  }
  let legjobb = azonos
  let legjobbErtek = metszesek(azonos)
  for (const perm of permutaciok(azonos)) {
    const ertek = metszesek(perm)
    if (ertek < legjobbErtek) {
      legjobb = perm
      legjobbErtek = ertek
    }
  }
  return legjobb
}

function permutaciok(elemek: number[]): number[][] {
  if (elemek.length <= 1) return [elemek]
  return elemek.flatMap((e, i) => permutaciok([...elemek.slice(0, i), ...elemek.slice(i + 1)]).map((p) => [e, ...p]))
}

// 5. lépés: a családok vonalai az elhelyezett kártyák alapján
export function vonalakSzamitasa(
  graf: Graf,
  hely: Map<number, ElhelyezettSzemely>,
  be: ElrendezesBeallitasok,
): CsaladVonal[] {
  const W = be.kartyaSzelesseg
  const H = be.kartyaMagassag
  const kozepX = (h: ElhelyezettSzemely) => h.x + W / 2

  // soronként a kártyák bal szélei: két házastárs között áll-e más
  const sorok = new Map<number, number[]>()
  for (const h of hely.values()) {
    if (!sorok.has(h.y)) sorok.set(h.y, [])
    sorok.get(h.y)!.push(h.x)
  }
  const vanKozottuk = (bal: ElhelyezettSzemely, jobb: ElhelyezettSzemely) =>
    sorok.get(bal.y)!.some((x) => x > bal.x && x < jobb.x)

  const vonalak: CsaladVonal[] = []
  const fesuk: Fesu[] = []
  for (const cs of graf.csaladok) {
    const szulok = cs.szulok.map((id) => hely.get(id)!).sort((a, b) => a.x - b.x)
    const gyermekek = cs.gyermekek.map((id) => hely.get(id)!)
    let hazassag: string | null = null
    let horgony: Pont | null = null

    if (szulok.length === 2) {
      const [bal, jobb] = szulok
      if (bal.y === jobb.y && !vanKozottuk(bal, jobb)) {
        // szomszédos házastársak: vízszintes vonal a kártyák között, középen a horgony
        const y = bal.y + H / 2
        hazassag = `M ${k(bal.x + W)} ${k(y)} H ${k(jobb.x)}`
        horgony = { x: (bal.x + W + jobb.x) / 2, y }
      } else if (bal.y === jobb.y) {
        // a két házastárs között más is áll (3+ házasság): U alakú összekötő a kártyák alatt
        const alj = bal.y + H
        hazassag = `M ${k(kozepX(bal))} ${k(alj)} V ${k(alj + U_MELYSEG)} H ${k(kozepX(jobb))} V ${k(alj)}`
        horgony = { x: (kozepX(bal) + kozepX(jobb)) / 2, y: alj + U_MELYSEG }
      } else {
        // eltérő sorban álló házastársak (csak körkörös adatnál): ferde összekötő
        hazassag = `M ${k(kozepX(bal))} ${k(bal.y + H / 2)} L ${k(kozepX(jobb))} ${k(jobb.y + H / 2)}`
        horgony = { x: (kozepX(bal) + kozepX(jobb)) / 2, y: Math.max(bal.y, jobb.y) + H }
      }
    } else if (szulok.length === 1) {
      horgony = { x: kozepX(szulok[0]), y: szulok[0].y + H }
    }

    vonalak.push({ csaladId: cs.id, hazassag, leszarmazas: null })
    // szülő nélküli családnál egyetlen gyermeket nincs mivel összekötni
    if (gyermekek.length === 0 || (horgony === null && gyermekek.length < 2)) continue

    const xek = gyermekek.map(kozepX)
    if (horgony) xek.push(horgony.x)
    const legfelsoGyermek = Math.min(...gyermekek.map((h) => h.y))
    fesuk.push({
      vonalIndex: vonalak.length - 1,
      horgony,
      gyermekek,
      savTeteje: szulok.length > 0 ? Math.max(...szulok.map((h) => h.y)) + H : legfelsoGyermek - be.generacioTav,
      kezd: Math.min(...xek),
      veg: Math.max(...xek),
    })
  }

  // ugyanabban a generációk közötti sávban lévő gyűjtővonalak sávokra osztása
  const savonkent = new Map<number, Fesu[]>()
  for (const f of fesuk) {
    if (!savonkent.has(f.savTeteje)) savonkent.set(f.savTeteje, [])
    savonkent.get(f.savTeteje)!.push(f)
  }
  for (const [savTeteje, csoport] of savonkent) {
    const { sav, savokSzama } = savokKiosztasa(csoport.map((f) => ({ kezd: f.kezd, veg: f.veg })), 6)
    const sorrend = savokSorrendje(
      csoport.map((f) => ({ kezd: f.kezd, veg: f.veg, horgonyX: f.horgony?.x ?? null, gyermekXek: f.gyermekek.map(kozepX) })),
      sav,
      savokSzama,
    )
    const lepes = savokSzama > 1 ? Math.min(SAV_LEPES, (be.generacioTav - 2 * SAV_MARGO) / (savokSzama - 1)) : 0
    csoport.forEach((f, i) => {
      const y = savTeteje + be.generacioTav / 2 + (sorrend[sav[i]] - (savokSzama - 1) / 2) * lepes
      let d = f.horgony ? `M ${k(f.horgony.x)} ${k(f.horgony.y)} V ${k(y)} ` : ''
      d += `M ${k(f.kezd)} ${k(y)} H ${k(f.veg)}`
      for (const gy of f.gyermekek) d += ` M ${k(kozepX(gy))} ${k(y)} V ${k(gy.y)}`
      vonalak[f.vonalIndex].leszarmazas = d
    })
  }
  return vonalak
}

// Egy tizedesjegyre kerekítve, hogy az SVG útvonal rövid és olvasható maradjon
function k(n: number): number {
  return Math.round(n * 10) / 10
}
