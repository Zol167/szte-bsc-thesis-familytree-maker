# szte-bsc-thesis-familytree-maker
My BSc Thesis on a Family Tree Maker web application

## Futtatás

```bash
docker compose up -d                 # Neo4j 5.26 (Browser: http://localhost:7474, neo4j / SzakdogaJelszo123)
cd backend
mvn spring-boot:run                  # első indításkor a dummy.ged betöltődik "Demo" fának (tulajdonos: demo)
```

A demó import kikapcsolható: `familytree.demo-import=false` (`application.properties`).
A feltöltött fotók a `backend/feltoltesek/` könyvtárba kerülnek (`familytree.kep-konyvtar`), a feltöltési korlát 50 MB.

### Frontend (Vue 3 + TypeScript, Node.js 24 LTS)

```bash
cd frontend
npm install
npm run dev                          # http://localhost:5173 – a /api kéréseket a Vite a backendhez (8080) továbbítja
```

Egy adott fa közvetlenül is megnyitható: `http://localhost:5173/?fa=<családfa azonosító>`.
A rajz a saját elrendező algoritmussal készül (külső elrendező modul nélkül, natív SVG-vel).
Az algoritmus leírása, mérései és irodalma: [frontend/ELRENDEZES.md](frontend/ELRENDEZES.md).

### Demó adatok

A `demo-adatok/kovacs-csalad.ged` egy hat generációs, 39 fős magyar családfa. Szándékosan tartalmazza
az elrendezés nehéz eseteit:
- többszörös házasság (Kovács Péter)
- unokatestvérek házassága, azaz ősvesztés (Horváth Béla és Kovács Margit)
- beházasodott házastárs ismert szülőkkel (Fekete Rózsa)
- egyszülős család

Importálása: a `backend/demo.http` 19. kérése, vagy:

```bash
curl -F fajl=@demo-adatok/kovacs-csalad.ged -F nev="Kovács család" -F tulajdonos=demo localhost:8080/api/csaladfak
```

## Tesztek

```bash
cd backend
mvn test     # egységtesztek (parser, export, egyezési kulcs, képtárolás) – Docker nélkül
mvn verify   # + integrációs tesztek valódi Neo4j-jel (Testcontainers, neo4j:5.26-community)

cd frontend
npm test                    # az elrendező algoritmus és a komponensek tesztjei (Vitest)
npm run tipusellenorzes     # TypeScript típusellenőrzés (vue-tsc)
npm run build               # éles build a dist/ könyvtárba
```

A `mvn verify`-hez a felhasználónak sudo nélkül kell elérnie a Dockert (`docker` csoport tagsága).

## Gráfmodell

```
(:Csaladfa {nev, tulajdonos, importIdo})-[:TARTALMAZ]->(:Szemely | :Csalad | :Kep)
(:Szemely)-[:SZULO_A]->(:Csalad {gedcomId, hazassagDatum})-[:GYERMEKE]->(:Szemely)
(:Szemely)-[:SZEREPEL_RAJTA {xPozicio, yPozicio, szelesseg, magassag, profilkep}]->(:Kep)
(:Szemely {egyezesKulcs})-[:LEHETSEGES_EGYEZES {letrehozva, lattak}]-(:Szemely)
```

- **Fotó-taggelés:** a keret a kapcsolaton van, a kép méretéhez viszonyított 0–1 koordinátákkal. Személyenként egy keret képenként. A profilkép a személy egyik `profilkep: true` jelzésű kerete (a frontend ezt vágja ki a fotóból).
- **Ütközés-detektálás:** két személy lehetséges egyezés, ha különböző tulajdonosok fáiban vannak, és a normalizált vezetéknév, keresztnév és születési év pontosan egyezik. A normalizálás: Unicode NFC, szóközök, kisbetűk; az ékezetek megmaradnak. Az `egyezesKulcs` (`"vezeteknev|keresztnev|ev"`) indexelt, így a keresés személyenként egy indexkeresés. A `lattak` azoknak a tulajdonosoknak a listája, akik az értesítést már olvasták.

## REST API

| Metódus | Végpont | Leírás |
|---|---|---|
| POST | `/api/csaladfak` | GEDCOM import (multipart: `fajl`, `nev`, `tulajdonos`); a válasz `egyezesDb`-je az importkor talált egyezések száma |
| GET | `/api/csaladfak` | Családfák listája (személyek, családok, egyezések száma) |
| GET | `/api/csaladfak/{id}/graf` | Lapos `{szemelyek, csaladok, profilkepek}` gráf a frontendnek |
| GET | `/api/csaladfak/{id}/export` | GEDCOM export |
| DELETE | `/api/csaladfak/{id}` | A fa törlése a fotóival és egyezéseivel együtt |
| POST | `/api/csaladfak/{id}/kepek` | Fotó feltöltése (multipart: `fajl`, opcionális `leiras`; JPEG, PNG, GIF, WebP) |
| GET | `/api/csaladfak/{id}/kepek` | A fa fotói a címkéikkel |
| GET | `/api/kepek/{id}` | Egy fotó a címkéivel |
| GET | `/api/kepek/{id}/fajl` | Maga a képfájl |
| DELETE | `/api/kepek/{id}` | Fotó törlése (a fájllal együtt) |
| PUT | `/api/kepek/{kepId}/cimkek/{szemelyId}` | Személy megjelölése / keret áthelyezése (`{x, y, szelesseg, magassag}`, 0–1) |
| DELETE | `/api/kepek/{kepId}/cimkek/{szemelyId}` | Címke törlése |
| GET | `/api/szemelyek/search?nev=` | Keresés kereszt- vagy vezetéknévre |
| GET | `/api/szemelyek/{id}/osok` | Összes ős (változó hosszúságú Cypher út) |
| GET | `/api/szemelyek/{id}/leszarmazottak` | Összes leszármazott |
| GET | `/api/szemelyek/{id}/kepek` | Fotók, amelyeken a személy szerepel |
| PUT / DELETE | `/api/szemelyek/{id}/profilkep` | Profilkép beállítása (`{kepId}`: a személy kerete azon a fotón) / törlése |
| GET | `/api/egyezesek?tulajdonos=` | Egy felhasználó értesítései (`olvasatlanDb` + lista) |
| PATCH | `/api/egyezesek/{id}/olvasott?tulajdonos=` | Egy értesítés olvasottnak jelölése |
| PATCH | `/api/egyezesek/olvasott?tulajdonos=` | Az összes olvasottnak jelölése |
| POST | `/api/egyezesek/ujrakereses` | Teljes újrakeresés minden fa között (idempotens) |

Hiba esetén a válasz törzse: `{"hiba": "..."}` (400 hibás kérés / GEDCOM, 404 nem található, 413 túl nagy fájl, 415 nem támogatott képformátum).

## 4. sprint demó

A `backend/demo.http` fájl a konzultáció forgatókönyve (VS Code **REST Client** bővítmény). Előtte tegyél egy saját családi fotót ide: `backend/demo-kep.jpg`.
A fájl a következőket mutatja be:
- egy második felhasználó (`bela`) fájának importja a `dummy2.ged`-ből
- az ütközés-értesítések mindkét felhasználónál
- egy fotó feltöltése és címkézése
- profilkép beállítása
- takarítás

A `dummy2.ged` szándékos esetei:
- `Robert Eugene /WILLIAMS/` (1822) egyezik: a nagybetűs írás nem számít
- `Joe /Williams/` (1861) egyezik
- `Mary /Wilson/` (1828) **nem** egyezik a `Mary Ann /Wilson/` személlyel: ez a pontos egyezés korlátja, a hasonlóság-alapú keresés későbbi továbbfejlesztés

## Hasznos Cypher lekérdezések (Neo4j Browser)

```cypher
// A teljes gráf
MATCH (n)-[r]->(m) RETURN n, r, m

// Fotó-taggelés: ki szerepel melyik képen (a keret a kapcsolat tulajdonsága)
MATCH p=(:Szemely)-[:SZEREPEL_RAJTA]->(:Kep) RETURN p

// Ütközések a különböző felhasználók fái között
MATCH p=(:Csaladfa)-[:TARTALMAZ]->(:Szemely)-[:LEHETSEGES_EGYEZES]-(:Szemely)<-[:TARTALMAZ]-(:Csaladfa) RETURN p

// Duplikált élek ellenőrzése (üres eredmény = rendben)
MATCH (a)-[r]->(b) WITH a, b, type(r) AS t, count(*) AS c WHERE c > 1 RETURN a, b, t, c
```

Az indexelt egyezési kulcs és a naiv összehasonlítás végrehajtási terve (a szakdolgozathoz; nagy fánál látványos a különbség):

```cypher
// Naiv: minden személypárt összehasonlít (CartesianProduct), a toLower() miatt index nélkül
PROFILE MATCH (a:Szemely), (b:Szemely)
WHERE id(a) < id(b) AND toLower(a.vezeteknev) = toLower(b.vezeteknev)
  AND toLower(a.keresztnev) = toLower(b.keresztnev) AND a.szuletesiEv = b.szuletesiEv
RETURN count(*)

// Kulccsal: személyenként egy indexkeresés (NodeIndexSeek)
PROFILE MATCH (a:Szemely) WHERE a.egyezesKulcs IS NOT NULL
MATCH (b:Szemely {egyezesKulcs: a.egyezesKulcs}) WHERE id(a) < id(b)
RETURN count(*)
```
