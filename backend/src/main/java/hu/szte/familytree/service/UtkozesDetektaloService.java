package hu.szte.familytree.service;

import hu.szte.familytree.dto.EgyezesDto;
import hu.szte.familytree.dto.EgyezesListaDto;
import hu.szte.familytree.dto.SzemelyDto;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Ütközés-detektálás: ugyanaz a személy (pontos név + azonos születési év) különböző felhasználók fáiban.
// Az egyezés maga is a gráf része: (:Szemely)-[:LEHETSEGES_EGYEZES {letrehozva, lattak}]-(:Szemely),
// ahol a lattak azoknak a tulajdonosoknak a listája, akik az értesítést már elolvasták.
@Service
public class UtkozesDetektaloService {

    // A személy adatai térképként (map projection), ebből készül a SzemelyDto
    private static final String SZEMELY_MEZOK =
            "{id: id(%s), .gedcomId, .keresztnev, .vezeteknev, .nem, .szuletesiDatum, .szuletesiEv, .halalozasiDatum}";

    private final Neo4jClient neo4jClient;

    public UtkozesDetektaloService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    // Egy újonnan importált fa személyeinek egyeztetése az összes többi felhasználó fáival.
    // A b csomópontot az egyezesKulcs indexéből keresi ki (személyenként egy indexkeresés,
    // nem minden pár összehasonlítása). A MERGE miatt az ismételt futtatás nem duplikál.
    public long keresesFaban(Long faId) {
        return neo4jClient.query("""
                        MATCH (uj:Csaladfa)-[:TARTALMAZ]->(a:Szemely)
                        WHERE id(uj) = $faId AND a.egyezesKulcs IS NOT NULL
                        MATCH (masFa:Csaladfa)-[:TARTALMAZ]->(b:Szemely {egyezesKulcs: a.egyezesKulcs})
                        WHERE masFa.tulajdonos <> uj.tulajdonos
                        MERGE (a)-[e:LEHETSEGES_EGYEZES]-(b)
                          ON CREATE SET e.letrehozva = $most, e.lattak = []
                        RETURN count(DISTINCT e)""")
                .bindAll(Map.of("faId", faId, "most", LocalDateTime.now()))
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
    }

    // Teljes újrakeresés az összes fa között (pl. a szabályok változása után); idempotens
    public long teljesKereses() {
        return neo4jClient.query("""
                        MATCH (fa1:Csaladfa)-[:TARTALMAZ]->(a:Szemely)
                        WHERE a.egyezesKulcs IS NOT NULL
                        MATCH (fa2:Csaladfa)-[:TARTALMAZ]->(b:Szemely {egyezesKulcs: a.egyezesKulcs})
                        WHERE id(a) < id(b) AND fa1.tulajdonos <> fa2.tulajdonos
                        MERGE (a)-[e:LEHETSEGES_EGYEZES]-(b)
                          ON CREATE SET e.letrehozva = $most, e.lattak = []
                        RETURN count(DISTINCT e)""")
                .bind(LocalDateTime.now()).to("most")
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
    }

    // Egy felhasználó értesítései: minden egyezés, amelynek egyik oldala az ő valamelyik fájában van
    public EgyezesListaDto egyezesek(String tulajdonos) {
        List<EgyezesDto> egyezesek = List.copyOf(neo4jClient.query("""
                        MATCH (sajatFa:Csaladfa {tulajdonos: $tulajdonos})-[:TARTALMAZ]->(sajat:Szemely)
                              -[e:LEHETSEGES_EGYEZES]-(masik:Szemely)<-[:TARTALMAZ]-(masikFa:Csaladfa)
                        WHERE masikFa.tulajdonos <> $tulajdonos
                        RETURN id(e) AS id, e.letrehozva AS letrehozva,
                               $tulajdonos IN coalesce(e.lattak, []) AS olvasott,
                               sajat %s AS sajatSzemely, id(sajatFa) AS sajatFaId, sajatFa.nev AS sajatFaNev,
                               masik %s AS masikSzemely, id(masikFa) AS masikFaId, masikFa.nev AS masikFaNev,
                               masikFa.tulajdonos AS masikTulajdonos
                        ORDER BY letrehozva DESC, id""".formatted(
                        SZEMELY_MEZOK.formatted("sajat"), SZEMELY_MEZOK.formatted("masik")))
                .bind(tulajdonos).to("tulajdonos")
                .fetchAs(EgyezesDto.class)
                .mappedBy((tipusok, sor) -> egyezesDto(sor))
                .all());
        long olvasatlanDb = egyezesek.stream().filter(e -> !e.olvasott()).count();
        return new EgyezesListaDto(olvasatlanDb, egyezesek);
    }

    // Olvasottnak jelölés egy tulajdonos nevében; a másik fél számára az értesítés olvasatlan marad
    public void olvasottnakJelol(Long egyezesId, String tulajdonos) {
        long db = neo4jClient.query("""
                        MATCH (:Csaladfa {tulajdonos: $tulajdonos})-[:TARTALMAZ]->(:Szemely)-[e:LEHETSEGES_EGYEZES]-()
                        WHERE id(e) = $id
                        WITH DISTINCT e
                        SET e.lattak = CASE WHEN $tulajdonos IN coalesce(e.lattak, []) THEN e.lattak
                                            ELSE coalesce(e.lattak, []) + $tulajdonos END
                        RETURN count(e)""")
                .bindAll(Map.of("id", egyezesId, "tulajdonos", tulajdonos))
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
        if (db == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nincs ilyen egyezés a(z) " + tulajdonos + " felhasználó fáiban: " + egyezesId);
        }
    }

    public long mindOlvasott(String tulajdonos) {
        return neo4jClient.query("""
                        MATCH (:Csaladfa {tulajdonos: $tulajdonos})-[:TARTALMAZ]->(:Szemely)-[e:LEHETSEGES_EGYEZES]-()
                        WITH DISTINCT e
                        WHERE NOT $tulajdonos IN coalesce(e.lattak, [])
                        SET e.lattak = coalesce(e.lattak, []) + $tulajdonos
                        RETURN count(e)""")
                .bind(tulajdonos).to("tulajdonos")
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
    }

    private static EgyezesDto egyezesDto(Record sor) {
        return new EgyezesDto(
                sor.get("id").asLong(),
                sor.get("letrehozva").asLocalDateTime(null),
                sor.get("olvasott").asBoolean(),
                szemelyDto(sor.get("sajatSzemely")),
                sor.get("sajatFaId").asLong(),
                sor.get("sajatFaNev").asString(null),
                szemelyDto(sor.get("masikSzemely")),
                sor.get("masikFaId").asLong(),
                sor.get("masikFaNev").asString(null),
                sor.get("masikTulajdonos").asString(null));
    }

    private static SzemelyDto szemelyDto(Value v) {
        return new SzemelyDto(
                v.get("id").asLong(),
                v.get("gedcomId").asString(null),
                v.get("keresztnev").asString(null),
                v.get("vezeteknev").asString(null),
                v.get("nem").asString(null),
                v.get("szuletesiDatum").asString(null),
                v.get("szuletesiEv").isNull() ? null : v.get("szuletesiEv").asInt(),
                v.get("halalozasiDatum").asString(null));
    }
}
