import type { Graf, Komponens } from './graf'

// Egy egység az egy sorban egymás mellett álló házastársak lánca (egyedülálló személynél egy tag).
// Az elrendezés során az egység egyben mozog, így a házastársak mindig szomszédosak maradnak.
export interface Egyseg {
  id: number
  generacio: number
  tagok: number[] // balról jobbra
}

// 2. lépés: egységek képzése a házassági kapcsolatok összefüggő részeiből, generációnként.
export function egysegekKepzese(komponens: Komponens, graf: Graf, generacio: Map<number, number>): Egyseg[] {
  const hazastarsak = new Map<number, number[]>(komponens.szemelyIdk.map((id) => [id, []]))
  for (const cs of komponens.csaladok) {
    if (cs.szulok.length !== 2) continue
    const [a, b] = cs.szulok
    // eltérő generációjú házastárs csak körkörös adatnál fordulhat elő; ilyenkor külön egységek
    if (generacio.get(a) !== generacio.get(b)) continue
    if (!hazastarsak.get(a)!.includes(b)) hazastarsak.get(a)!.push(b)
    if (!hazastarsak.get(b)!.includes(a)) hazastarsak.get(b)!.push(a)
  }

  const sorszam = (id: number) => graf.szemelyek.get(id)!.sorszam
  const besorolt = new Set<number>()
  const egysegek: Egyseg[] = []
  for (const kezdo of [...komponens.szemelyIdk].sort((a, b) => sorszam(a) - sorszam(b))) {
    if (besorolt.has(kezdo)) continue
    const csoport: number[] = []
    const sor = [kezdo]
    besorolt.add(kezdo)
    while (sor.length > 0) {
      const id = sor.shift()!
      csoport.push(id)
      for (const tars of hazastarsak.get(id)!) {
        if (!besorolt.has(tars)) {
          besorolt.add(tars)
          sor.push(tars)
        }
      }
    }
    egysegek.push({ id: egysegek.length, generacio: generacio.get(kezdo)!, tagok: tagokSorrendje(csoport, hazastarsak, graf) })
  }
  return egysegek
}

// A tagok sorrendje egy egységen belül:
// - házaspár: a férj balra, a feleség jobbra (ha a nem ismert), különben bemeneti sorrend;
// - lánc (pl. két házasság): a lánc mentén, így a középső személy két házastársa két oldalra kerül;
// - csillag (3+ házasság): a középső személy körül felváltva balra és jobbra, a házasságok sorrendjében;
// - egyéb (nagyon ritka, pl. "körbeházasodás"): szélességi bejárás a legtöbb házasságú személytől.
function tagokSorrendje(csoport: number[], hazastarsak: Map<number, number[]>, graf: Graf): number[] {
  const sorszam = (id: number) => graf.szemelyek.get(id)!.sorszam
  if (csoport.length === 1) return csoport
  if (csoport.length === 2) {
    const [a, b] = [...csoport].sort((x, y) => sorszam(x) - sorszam(y))
    const nemA = graf.szemelyek.get(a)!.nem
    const nemB = graf.szemelyek.get(b)!.nem
    return nemA === 'F' && nemB === 'M' ? [b, a] : [a, b]
  }

  const fok = (id: number) => hazastarsak.get(id)!.length
  const elekSzama = csoport.reduce((osszeg, id) => osszeg + fok(id), 0) / 2
  const faE = elekSzama === csoport.length - 1

  if (faE && csoport.every((id) => fok(id) <= 2)) {
    // lánc: a kisebb sorszámú végpontból indulva végigjárjuk
    const vegpontok = csoport.filter((id) => fok(id) === 1).sort((a, b) => sorszam(a) - sorszam(b))
    const sorrend = [vegpontok[0]]
    const latott = new Set(sorrend)
    while (sorrend.length < csoport.length) {
      const kovetkezo = hazastarsak.get(sorrend[sorrend.length - 1])!.find((id) => !latott.has(id))!
      sorrend.push(kovetkezo)
      latott.add(kovetkezo)
    }
    return sorrend
  }

  const kozep = csoport.reduce((a, b) => (fok(b) > fok(a) ? b : a))
  if (faE && csoport.every((id) => id === kozep || fok(id) === 1)) {
    const tarsak = hazastarsak.get(kozep)!
    const bal = tarsak.filter((_, i) => i % 2 === 0).reverse()
    const jobb = tarsak.filter((_, i) => i % 2 === 1)
    return [...bal, kozep, ...jobb]
  }

  const sorrend = [kozep]
  const latott = new Set(sorrend)
  for (let i = 0; i < sorrend.length; i++) {
    for (const tars of hazastarsak.get(sorrend[i])!) {
      if (!latott.has(tars)) {
        latott.add(tars)
        sorrend.push(tars)
      }
    }
  }
  return sorrend
}
