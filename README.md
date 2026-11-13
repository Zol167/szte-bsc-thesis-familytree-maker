# szte-bsc-thesis-familytree-maker
My BSc Thesis on a Family Tree Maker web application

## Futtatás

```bash
docker compose up -d                 # Neo4j 5.26 (Browser: http://localhost:7474, neo4j / SzakdogaJelszo123)
cd backend
mvn test                             # parser + import/export oda-vissza tesztek (Neo4j nélkül)
mvn spring-boot:run                  # első indításkor a dummy.ged betöltődik "Demo" fának
```

A demó import kikapcsolható: `familytree.demo-import=false` (`application.properties`).

## Gráfmodell

```
(:Csaladfa {nev, tulajdonos, importIdo})-[:TARTALMAZ]->(:Szemely | :Csalad)
(:Szemely)-[:SZULO_A]->(:Csalad {gedcomId, hazassagDatum})-[:GYERMEKE]->(:Szemely)
(:Szemely)-[:SZEREPEL_RAJTA {xPozicio, yPozicio, szelesseg, magassag}]->(:Kep)
```

## REST API

| Metódus | Végpont | Leírás |
|---|---|---|
| POST | `/api/csaladfak` | GEDCOM import (multipart: `fajl`, `nev`, `tulajdonos`) |
| GET | `/api/csaladfak` | Családfák listája (személy- és családszámmal) |
| GET | `/api/csaladfak/{id}/graf` | Lapos `{szemelyek, csaladok}` gráf a frontendnek |
| GET | `/api/csaladfak/{id}/export` | GEDCOM export |
| GET | `/api/szemelyek/search?nev=` | Keresés kereszt- vagy vezetéknévre |
| GET | `/api/szemelyek/{id}/osok` | Összes ős (változó hosszúságú Cypher út) |
| GET | `/api/szemelyek/{id}/leszarmazottak` | Összes leszármazott |

```bash
curl -F fajl=@backend/src/main/resources/dummy.ged -F nev=Teszt -F tulajdonos=zoli localhost:8080/api/csaladfak
curl localhost:8080/api/csaladfak/1/export
```

## Hasznos Cypher lekérdezések (Neo4j Browser)

```cypher
// A teljes gráf
MATCH (n)-[r]->(m) RETURN n, r, m

// Duplikált élek ellenőrzése (üres eredmény = rendben)
MATCH (a)-[r]->(b) WITH a, b, type(r) AS t, count(*) AS c WHERE c > 1 RETURN a, b, t, c
```
