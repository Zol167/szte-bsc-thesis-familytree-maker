package hu.szte.familytree.service;

import hu.szte.familytree.dto.CimkeDto;
import hu.szte.familytree.dto.CimkeKeres;
import hu.szte.familytree.dto.KepDto;
import hu.szte.familytree.model.Kep;
import hu.szte.familytree.repository.CsaladfaRepository;
import hu.szte.familytree.repository.KepRepository;
import hu.szte.familytree.repository.SzemelyRepository;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Fotók és fotó-taggelés. A címkéket (SZEREPEL_RAJTA élek) Cypherrel írjuk, nem a Szemely entitás
// mentésével: az SDN mentéskor egy entitás összes kapcsolatát újraírja, így egy részlegesen betöltött
// személy mentése (pl. keresési találat) törölné a címkéit.
@Service
public class KepService {

    // Közös vetítés minden képlekérdezéshez: a kép adatai és a rajta lévő címkék.
    // A k és f változókat a hívó MATCH köti; a listaképzésben szereplő sz új változó.
    private static final String KEP_VETITES = """
            RETURN id(k) AS id, id(f) AS csaladfaId, k.eredetiNev AS eredetiNev, k.mimeTipus AS mimeTipus,
                   k.szelesseg AS szelesseg, k.magassag AS magassag, k.leiras AS leiras, k.feltoltIdo AS feltoltIdo,
                   [(sz:Szemely)-[r:SZEREPEL_RAJTA]->(k) | {
                       szemelyId: id(sz), keresztnev: sz.keresztnev, vezeteknev: sz.vezeteknev,
                       x: r.xPozicio, y: r.yPozicio, szelesseg: r.szelesseg, magassag: r.magassag,
                       profilkep: coalesce(r.profilkep, false)}] AS cimkek
            """;

    public record KepFajl(Resource tartalom, String mimeTipus) {
    }

    private final Neo4jClient neo4jClient;
    private final KepTaroloService tarolo;
    private final KepRepository kepRepository;
    private final CsaladfaRepository csaladfaRepository;
    private final SzemelyRepository szemelyRepository;

    public KepService(Neo4jClient neo4jClient, KepTaroloService tarolo, KepRepository kepRepository,
                      CsaladfaRepository csaladfaRepository, SzemelyRepository szemelyRepository) {
        this.neo4jClient = neo4jClient;
        this.tarolo = tarolo;
        this.kepRepository = kepRepository;
        this.csaladfaRepository = csaladfaRepository;
        this.szemelyRepository = szemelyRepository;
    }

    public KepDto feltoltes(Long faId, MultipartFile fajl, String leiras) {
        if (!csaladfaRepository.existsById(faId)) {
            throw nemTalalhato("Nincs ilyen családfa: " + faId);
        }
        KepTaroloService.TaroltKep tarolt = tarolo.mentes(fajl);
        try {
            Map<String, Object> parameterek = new HashMap<>(); // HashMap: a leírás és a méret lehet null
            parameterek.put("faId", faId);
            parameterek.put("fajlNev", tarolt.fajlNev());
            parameterek.put("eredetiNev", fajl.getOriginalFilename());
            parameterek.put("mimeTipus", tarolt.mimeTipus());
            parameterek.put("szelesseg", tarolt.szelesseg());
            parameterek.put("magassag", tarolt.magassag());
            parameterek.put("leiras", leiras);
            parameterek.put("most", LocalDateTime.now());
            Long kepId = neo4jClient.query("""
                            MATCH (f:Csaladfa) WHERE id(f) = $faId
                            CREATE (f)-[:TARTALMAZ]->(k:Kep {fajlNev: $fajlNev, eredetiNev: $eredetiNev,
                                mimeTipus: $mimeTipus, szelesseg: $szelesseg, magassag: $magassag,
                                leiras: $leiras, feltoltIdo: $most})
                            RETURN id(k)""")
                    .bindAll(parameterek)
                    .fetchAs(Long.class)
                    .one()
                    .orElseThrow(() -> nemTalalhato("Nincs ilyen családfa: " + faId));
            return kep(kepId);
        } catch (RuntimeException e) {
            tarolo.torles(tarolt.fajlNev()); // ne maradjon árva fájl a lemezen
            throw e;
        }
    }

    public List<KepDto> fanakKepei(Long faId) {
        if (!csaladfaRepository.existsById(faId)) {
            throw nemTalalhato("Nincs ilyen családfa: " + faId);
        }
        return kepek("MATCH (f:Csaladfa)-[:TARTALMAZ]->(k:Kep) WHERE id(f) = $id\n"
                + KEP_VETITES + "ORDER BY feltoltIdo, id", faId);
    }

    public KepDto kep(Long kepId) {
        return kepek("MATCH (f:Csaladfa)-[:TARTALMAZ]->(k:Kep) WHERE id(k) = $id\n" + KEP_VETITES, kepId)
                .stream()
                .findFirst()
                .orElseThrow(() -> nemTalalhato("Nincs ilyen kép: " + kepId));
    }

    // Azok a fotók, amelyeken a személy meg van jelölve (sz0, hogy ne ütközzön a vetítés sz változójával)
    public List<KepDto> szemelyKepei(Long szemelyId) {
        if (!szemelyRepository.existsById(szemelyId)) {
            throw nemTalalhato("Nincs ilyen személy: " + szemelyId);
        }
        return kepek("MATCH (sz0:Szemely)-[:SZEREPEL_RAJTA]->(k:Kep)<-[:TARTALMAZ]-(f:Csaladfa) WHERE id(sz0) = $id\n"
                + KEP_VETITES + "ORDER BY feltoltIdo, id", szemelyId);
    }

    public KepFajl fajl(Long kepId) {
        Kep kep = kepRepository.findById(kepId)
                .orElseThrow(() -> nemTalalhato("Nincs ilyen kép: " + kepId));
        return new KepFajl(tarolo.betoltes(kep.getFajlNev()), kep.getMimeTipus());
    }

    public void torles(Long kepId) {
        String fajlNev = neo4jClient.query("""
                        MATCH (k:Kep) WHERE id(k) = $id
                        WITH k, k.fajlNev AS fajlNev
                        DETACH DELETE k
                        RETURN fajlNev""")
                .bind(kepId).to("id")
                .fetchAs(String.class)
                .one()
                .orElseThrow(() -> nemTalalhato("Nincs ilyen kép: " + kepId));
        tarolo.torles(fajlNev);
    }

    // Címke létrehozása vagy áthelyezése: személyenként egy keret képenként (MERGE).
    // Csak a kép családfájában lévő személy jelölhető meg.
    public KepDto cimkezes(Long kepId, Long szemelyId, CimkeKeres keres) {
        keres.ellenoriz();
        long db = neo4jClient.query("""
                        MATCH (f:Csaladfa)-[:TARTALMAZ]->(k:Kep) WHERE id(k) = $kepId
                        MATCH (f)-[:TARTALMAZ]->(sz:Szemely) WHERE id(sz) = $szemelyId
                        MERGE (sz)-[r:SZEREPEL_RAJTA]->(k)
                          ON CREATE SET r.profilkep = false
                        SET r.xPozicio = $x, r.yPozicio = $y, r.szelesseg = $szelesseg, r.magassag = $magassag
                        RETURN count(r)""")
                .bindAll(Map.of("kepId", kepId, "szemelyId", szemelyId, "x", keres.x(), "y", keres.y(),
                        "szelesseg", keres.szelesseg(), "magassag", keres.magassag()))
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
        if (db == 0) {
            if (!kepRepository.existsById(kepId)) throw nemTalalhato("Nincs ilyen kép: " + kepId);
            if (!szemelyRepository.existsById(szemelyId)) throw nemTalalhato("Nincs ilyen személy: " + szemelyId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A személy nem a kép családfájához tartozik, ezért nem jelölhető meg rajta");
        }
        return kep(kepId);
    }

    public void cimkeTorles(Long kepId, Long szemelyId) {
        long db = neo4jClient.query("""
                        MATCH (sz:Szemely)-[r:SZEREPEL_RAJTA]->(k:Kep)
                        WHERE id(sz) = $szemelyId AND id(k) = $kepId
                        DELETE r
                        RETURN count(r)""")
                .bindAll(Map.of("kepId", kepId, "szemelyId", szemelyId))
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
        if (db == 0) {
            throw nemTalalhato("A személy nincs megjelölve ezen a képen");
        }
    }

    // A személy egyik meglévő címkéje lesz a profilképe; a többi címkéjéről lekerül a jelzés
    public void profilkepBeallitas(Long szemelyId, Long kepId) {
        if (kepId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A kepId megadása kötelező");
        }
        long db = neo4jClient.query("""
                        MATCH (sz:Szemely)-[cel:SZEREPEL_RAJTA]->(k:Kep)
                        WHERE id(sz) = $szemelyId AND id(k) = $kepId
                        MATCH (sz)-[r:SZEREPEL_RAJTA]->(:Kep)
                        SET r.profilkep = (r = cel)
                        RETURN count(r)""")
                .bindAll(Map.of("kepId", kepId, "szemelyId", szemelyId))
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
        if (db == 0) {
            if (!szemelyRepository.existsById(szemelyId)) throw nemTalalhato("Nincs ilyen személy: " + szemelyId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A személy nincs megjelölve ezen a képen, ezért nem lehet a profilképe");
        }
    }

    public void profilkepTorles(Long szemelyId) {
        if (!szemelyRepository.existsById(szemelyId)) {
            throw nemTalalhato("Nincs ilyen személy: " + szemelyId);
        }
        neo4jClient.query("MATCH (sz:Szemely)-[r:SZEREPEL_RAJTA]->(:Kep) WHERE id(sz) = $szemelyId SET r.profilkep = false")
                .bind(szemelyId).to("szemelyId")
                .run();
    }

    private List<KepDto> kepek(String cypher, Long id) {
        return List.copyOf(neo4jClient.query(cypher)
                .bind(id).to("id")
                .fetchAs(KepDto.class)
                .mappedBy((tipusok, sor) -> kepDto(sor))
                .all());
    }

    private static KepDto kepDto(Record sor) {
        Long id = sor.get("id").asLong();
        List<CimkeDto> cimkek = sor.get("cimkek").asList(c -> new CimkeDto(
                c.get("szemelyId").asLong(),
                c.get("keresztnev").asString(null),
                c.get("vezeteknev").asString(null),
                c.get("x").asDouble(),
                c.get("y").asDouble(),
                c.get("szelesseg").asDouble(),
                c.get("magassag").asDouble(),
                c.get("profilkep").asBoolean()));
        return new KepDto(id,
                sor.get("csaladfaId").asLong(),
                sor.get("eredetiNev").asString(null),
                sor.get("mimeTipus").asString(null),
                egeszVagyNull(sor.get("szelesseg")),
                egeszVagyNull(sor.get("magassag")),
                sor.get("leiras").asString(null),
                sor.get("feltoltIdo").asLocalDateTime(null),
                KepDto.url(id),
                cimkek);
    }

    private static Integer egeszVagyNull(Value ertek) {
        return ertek.isNull() ? null : ertek.asInt();
    }

    private static ResponseStatusException nemTalalhato(String uzenet) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, uzenet);
    }
}
