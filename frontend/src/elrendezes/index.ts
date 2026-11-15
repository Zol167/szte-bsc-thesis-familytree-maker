import type { Egyseg } from './egysegek'
import { egysegekKepzese } from './egysegek'
import { generaciokKiosztasa } from './generaciok'
import { grafEpites, komponensek } from './graf'
import { koordinatakSzamitasa } from './koordinatak'
import { kezdoSorrend, sorrendOptimalizalas, type Kontextus } from './sorrend'
import {
  ALAP_BEALLITASOK,
  type ElhelyezettSzemely,
  type Elrendezes,
  type ElrendezesBeallitasok,
  type ElrendezesBemenet,
} from './tipusok'
import { vonalakSzamitasa } from './vonalak'

export * from './tipusok'

// Saját családfa-elrendező algoritmus (külső elrendező modul nélkül). A backend gráfjából
// (személyek + családok) kiszámítja minden személy kártyájának helyét és a vonalakat:
//   1. generációk kiosztása (leghosszabb út + házastársi egyenlőség, gyökerek leengedése),
//   2. házastársi egységek képzése,
//   3. sorrend a generációkon belül (mélységi bejárás, majd baricentrikus keresztezés-csökkentés),
//   4. vízszintes koordináták (izotonikus regresszió váltakozó söprésekkel),
//   5. vonalak (házassági összekötők és leszármazási "fésűk" sávkiosztással).
// Az egymással rokonságban nem álló ágak (komponensek) külön rendeződnek, majd egymás mellé kerülnek.
export function elrendezes(bemenet: ElrendezesBemenet, be: ElrendezesBeallitasok = ALAP_BEALLITASOK): Elrendezes {
  const graf = grafEpites(bemenet)
  const sorMagassag = be.kartyaMagassag + be.generacioTav
  const lepes = be.kartyaSzelesseg + be.hazastarsTav
  const hely = new Map<number, ElhelyezettSzemely>()
  const figyelmeztetesek: string[] = []
  let eltolas = 0
  let generaciokSzama = 0
  let keresztezesek = 0
  let elozoEgyszemelyes = false

  for (const komponens of komponensek(graf)) {
    const { generacio, korkoros } = generaciokKiosztasa(komponens, graf)
    if (korkoros) {
      figyelmeztetesek.push(
        `Egy ${komponens.szemelyIdk.length} fős ágban körkörös szülő–gyermek kapcsolat van ` +
          '(valaki a saját őse); ott a generációk nem lehetnek teljesen helyesek.',
      )
    }
    const egysegek = egysegekKepzese(komponens, graf, generacio)
    const egysegeOf = new Map<number, Egyseg>()
    for (const e of egysegek) for (const tag of e.tagok) egysegeOf.set(tag, e)
    const ktx: Kontextus = { graf, generacio, egysegeOf }

    const rendezes = sorrendOptimalizalas(kezdoSorrend(egysegek, ktx), ktx, be.sorrendIteraciok)
    keresztezesek += rendezes.keresztezesek
    const x = koordinatakSzamitasa(rendezes.retegek, ktx, be)

    // az egyedülálló, kapcsolat nélküli személyek szorosabban követik egymást
    const egyszemelyes = komponens.szemelyIdk.length === 1
    if (hely.size > 0) eltolas += egyszemelyes && elozoEgyszemelyes ? be.csaladTav : be.komponensTav
    let jobbSzel = 0
    for (const e of egysegek) {
      e.tagok.forEach((tag, i) => {
        const bal = x.get(e.id)! + i * lepes
        hely.set(tag, { id: tag, x: eltolas + bal, y: e.generacio * sorMagassag, generacio: e.generacio })
        jobbSzel = Math.max(jobbSzel, bal + be.kartyaSzelesseg)
      })
      generaciokSzama = Math.max(generaciokSzama, e.generacio + 1)
    }
    eltolas += jobbSzel
    elozoEgyszemelyes = egyszemelyes
  }

  return {
    szemelyek: [...graf.szemelyek.keys()].map((id) => hely.get(id)!),
    vonalak: vonalakSzamitasa(graf, hely, be),
    szelesseg: eltolas,
    magassag: generaciokSzama > 0 ? generaciokSzama * sorMagassag - be.generacioTav : 0,
    generaciokSzama,
    keresztezesek,
    figyelmeztetesek,
  }
}
