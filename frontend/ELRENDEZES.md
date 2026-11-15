# A családfa-elrendező algoritmus

A frontend saját fejlesztésű elrendező algoritmusa (`src/elrendezes/`) a backend gráfjából
(`GET /api/csaladfak/{id}/graf`: személyek és családok) kiszámítja minden személy kártyájának
X, Y koordinátáját és a rokoni vonalakat. Külső elrendező modult nem használ. A rajzolás natív SVG elemekkel történik (`<rect>`, `<text>`, `<path>`), a Vue sablonból.

## Bemenet és kimenet

- **Bemenet:** személyek (`id`, születési év, nem) és családok (`szuloIdk`, `gyermekIdk`). A család a GEDCOM `FAM` rekordja, így a gráf páros: személy – család – személy.
- **Kimenet:**
  - minden személyhez a kártya bal felső sarka és a generációja
  - minden családhoz két SVG útvonal (házastársi összekötő, leszármazási „fésű”)
  - a rajz mérete
  - a megmaradt keresztezések száma
  - figyelmeztetések (pl. körkörös adat)

## A lépések

### 0. Előkészítés és komponensek (`graf.ts`)

- **Tisztítás:** az ismeretlen személyre mutató és az ismétlődő hivatkozások kimaradnak, és aki egy családban szülő, az ugyanott nem lehet gyermek.
- **Komponensek:** az egymással rokonságban nem álló ágakat unió–keresés (union–find) adatszerkezet választja szét. Az ágak külön rendeződnek, majd egymás mellé kerülnek; a kapcsolat nélküli személyek szorosabban.

### 1. Generációk (`generaciok.ts`)

A generáció a sor, amelybe a személy kerül (0 = legfelső).

1. **Leghosszabb út relaxációval** (Bellman–Ford-szerűen). Feltételek:
   - egy család két szülője (a házastársak) azonos generációban van,
   - minden gyermek legalább eggyel a szülei alatt van,
   - szülő nélküli családban a testvérek egy generációba kerülnek.

   Az értékek csak nőnek. A házastársi egyenlőség 0 súlyú, a leszármazás 1 súlyú él a
   feltételgráfban, így körmentes adatnál legfeljebb *n* körben beáll a fixpont. Ha nem áll be, pozitív kör van: az adat **körkörös** (valaki a saját őse). Ilyenkor az algoritmus nem akad el, hanem figyelmeztet.
2. **Gyökerek leengedése.** Akinek a szülei nincsenek a fában (pl. egy beházasodott házastárs
   szülei), az 1. lépés után a legfelső sorban állna. Helyette közvetlenül a legfelső gyermeke fölé kerül.
   - A mozgatás házastársi csoportonként történik, és csak lefelé, a gyermekekig, így egyik
     feltétel sem sérül.
   - Ha a csoport nem-gyökér házastárshoz kötődik, a helye rögzített.
3. **Tömörítés:** a használt generációszámok folytonos újraszámozása (üres sorok nélkül).

### 2. Házastársi egységek (`egysegek.ts`)

Egy egység az egy sorban egymás mellett álló házastársak lánca. Az elrendezés során egyben mozog,
így a házastársak mindig szomszédosak.

- **Házaspár:** alapértelmezésben a férj balra áll.
- **Lánc** (pl. két házasság): a középső személy két házastársa két oldalra kerül:
  [1. feleség, férj, 2. feleség].
- **Csillag** (3+ házasság): a középső személy körül felváltva balra és jobbra.

### 3. Sorrend a generációkon belül (`sorrend.ts`)

A cél a vonalkeresztezések számának csökkentése. Ez általában NP-nehéz feladat, ezért heurisztikák
oldják meg a Sugiyama-féle rétegezett gráfrajzolás mintájára.

1. **Kezdő sorrend mélységi bejárással** a családi szálak mentén: egy egység után rögtön a
   gyermekei következnek, születési sorrendben. Így minden leszármazotti ág kezdetben egy tömbben van.
2. **Baricentrum-módszer**, váltakozó söprésekkel:
   - lefelé minden egység a szülei átlagos pozíciójához,
   - felfelé a gyermekei átlagos pozíciójához rendeződik (stabil rendezéssel).
3. **Az egység tükrözése:** melyik házastárs álljon a saját szülei felé. A tükrözés csak az egység
   saját élei közötti keresztezéseket változtatja meg, így elsősorban ezek száma, döntetlennél a
   távolság dönt.
4. **Szomszédos csere** (transzpozíció, mint a Graphviz `dot` algoritmusában): két szomszédos egység
   helyet cserél, ha ettől csökken a keresztezések száma.
   - A csere csak a két egység saját éleinek egymáshoz viszonyított keresztezéseit változtatja meg,
     ezért a nyereség csak az ő éleikből számolható.
   - Csak javító csere történik, így az eljárás véges.
5. **Keresztezések számlálása:** két szomszédos generáció között egy él a család „horgonya” (a
   szülők átlagos pozíciója) és egy gyermek. Két él akkor keresztezi egymást, ha a végeik sorrendje
   ellentétes, ami inverziószámlálás: összefésülő rendezéssel O(E log E).
6. **A legkevesebb keresztezést adó sorrend marad meg** (legfeljebb 8 iteráció; nulla
   keresztezésnél azonnal megáll).

### 4. Vízszintes koordináták (`koordinatak.ts`)

A sorrend rögzített; minden sort ugyanaz az optimalizálási feladat helyez el:

> minimalizálni Σ sᵢ·(xᵢ − cᵢ)² összeget úgy, hogy x₍ᵢ₊₁₎ − xᵢ ≥ dᵢ,

ahol cᵢ az egység kívánt helye, sᵢ a súlya (a kapcsolatainak száma), dᵢ pedig a minimális távolság.
A dᵢ testvérek között kisebb, nem rokon egységek között nagyobb.

Az yᵢ = xᵢ − (d₀ + … + dᵢ₋₁) helyettesítéssel ez **súlyozott izotonikus regresszió** (y nem
csökkenő). Ezt a *Pool Adjacent Violators* algoritmus pontosan és O(n) időben megoldja: a sorrendet
sértő szomszédos blokkokat összevonja, és a blokk értéke a súlyozott átlag lesz. Ebből következik,
hogy az azonos célú testvérek a cél körül szimmetrikusan nyílnak szét.

A kívánt helyek váltakozó söprésekből jönnek (12 kör, a végén még egy lefelé):

- **lefelé:** minden személy a szülőcsaládja horgonya alá,
- **felfelé:** minden család horgonya a gyermekei által elfoglalt sáv közepe fölé.

### 5. Vonalak (`vonalak.ts`)

- **Házastársi vonal:** vízszintes vonal a két kártya között, félmagasságban; középen van a család
  horgonya. Ha a házastársak között más is áll (3+ házasság), U alakú összekötő fut a kártyák alatt.
  Egyszülős családnál a horgony a szülő kártyájának alja.
- **Leszármazási „fésű”:** a horgonytól függőlegesen le a gyűjtővonalig, a gyűjtővonal vízszintesen
  a gyermekek fölött, onnan függőlegesen le minden gyermekhez.
- **Sávkiosztás:** az egymást átfedő gyűjtővonalak külön magasságba (sávba) kerülnek.
  - A sávok kiosztása mohó intervallumgráf-színezés; kezdőpont szerint rendezve a lehető legkevesebb
    sávot adja.
  - A sávok egymás alatti sorrendjét teljes kereséssel választjuk ki, legfeljebb 5 sávig
    (5! = 120 eset): azt, amelyiknél a legkevesebb függőleges vonal metsz gyűjtővonalat.

## Speciális esetek

| Eset | Kezelés |
|---|---|
| Többszörös házasság | Láncként vagy csillagként egy egység; a gyermekek a saját házasságuk horgonya alá kerülnek. |
| Ősvesztés (pl. unokatestvérek házassága) | A közös ősök egyszer szerepelnek. A házaspár a két szülőcsalád közé kerül, mindkét házastárs a saját szülei felé. |
| Generációütközés (pl. nagybácsi–unokahúg) | A házastársi egyenlőség és a „gyermek a szülő alatt” feltétel együtt teljesül, legfeljebb hosszabb vonallal. |
| Örökbefogadás (két szülőcsalád) | A gyermek a két horgony közé kerül, mindkét családtól vezet hozzá vonal. |
| Beházasodott fél ismert szülei | A gyökerek leengedése miatt közvetlenül a házastárs fölé kerülnek. |
| Körkörös (hibás) adat | Az algoritmus megáll, és figyelmeztet. |

## Helyesség

A tesztek (`src/elrendezes/__tests__/`) minden esetben ellenőrzik a „strukturálisan helyes
családfa” feltételeit:

1. minden személy pontosan egyszer szerepel,
2. minden gyermek a szülei alatt van,
3. a házastársak egy sorban vannak,
4. egy soron belül nincs átfedés,
5. minden a rajz méretén belül van, és minden családnak van vonala.

A tesztek egyes esetekben a pontos elhelyezést is ellenőrzik: az egyke gyermek a szülők közepe
alatt van, a testvérek születési sorrendben állnak, a keresztezések száma 0. Az egyik teszt
(`kovacsCsalad.spec.ts`) a valódi backend által előállított gráfon fut.

## Mérések

Node 24, fejlesztői gép, egyszeri futás. A szintetikus fák álvéletlen, de érvényes családfák
(`szintetikusFa`).

| Bemenet | Személyek | Keresztezés szomszédos csere nélkül | Keresztezés a végleges algoritmussal | Idő |
|---|---|---|---|---|
| Kovács család (`demo-adatok/kovacs-csalad.ged`) | 39 | 7 | 5 | 3 ms (medián, 20 futás) |
| Szintetikus fa | 300 | 60 | 31 | 40 ms |
| Szintetikus fa | 1500 | 2305 | 1478 | 196 ms |
| Szintetikus fa | 3002 | 2232 | 1797 | 334 ms |

## Korlátok, továbbfejlesztés

- **Hosszú élek:** a több generációt átívelő éleket (generációütközésnél) nem bontjuk fel köztes
  „ál-csúcsokra”, így ritkán egy vonal áthaladhat egy közbülső sor kártyáján.
- **A keresztezés-csökkentés heurisztika:** a megtalált sorrend nem feltétlenül optimális.
- **Nagyítás és mozgatás a 6. sprintben jön;** addig az „Igazítás az ablakhoz” kapcsoló kicsinyíti
  a teljes fát az ablakba.

## Irodalom

- K. Sugiyama, S. Tagawa, M. Toda: *Methods for Visual Understanding of Hierarchical System
  Structures.* IEEE Transactions on Systems, Man, and Cybernetics 11(2), 109–125, 1981.
- E. R. Gansner, E. Koutsofios, S. C. North, K.-P. Vo: *A Technique for Drawing Directed Graphs.*
  IEEE Transactions on Software Engineering 19(3), 214–230, 1993.
- W. Barth, M. Jünger, P. Mutzel: *Simple and Efficient Bilayer Cross Counting.* Journal of Graph
  Algorithms and Applications 8(2), 179–194, 2004.
- M. Ayer, H. D. Brunk, G. M. Ewing, W. T. Reid, E. Silverman: *An Empirical Distribution Function
  for Sampling with Incomplete Information.* Annals of Mathematical Statistics 26(4), 641–647, 1955.
- R. E. Barlow, D. J. Bartholomew, J. M. Bremner, H. D. Brunk: *Statistical Inference under Order
  Restrictions.* Wiley, 1972.
- R. Bellman: *On a Routing Problem.* Quarterly of Applied Mathematics 16, 87–90, 1958.
