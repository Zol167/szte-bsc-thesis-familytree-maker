package hu.szte.familytree.service;

import hu.szte.familytree.dto.CsaladfaGrafDto;
import hu.szte.familytree.dto.CsaladfaOsszegzesDto;
import hu.szte.familytree.model.Csaladfa;
import hu.szte.familytree.repository.CsaladfaRepository;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.List;

@Service
public class CsaladfaService {

    private final CsaladfaRepository csaladfaRepository;
    private final GrafEpitoService grafEpitoService;
    private final UtkozesDetektaloService utkozesDetektalo;
    private final GedcomExportService exportService;
    private final KepTaroloService kepTarolo;
    private final Neo4jClient neo4jClient;

    public CsaladfaService(CsaladfaRepository csaladfaRepository, GrafEpitoService grafEpitoService,
                           UtkozesDetektaloService utkozesDetektalo, GedcomExportService exportService,
                           KepTaroloService kepTarolo, Neo4jClient neo4jClient) {
        this.csaladfaRepository = csaladfaRepository;
        this.grafEpitoService = grafEpitoService;
        this.utkozesDetektalo = utkozesDetektalo;
        this.exportService = exportService;
        this.kepTarolo = kepTarolo;
        this.neo4jClient = neo4jClient;
    }

    // Import és ütközés-detektálás egy tranzakcióban: vagy mindkettő megtörténik, vagy egyik sem
    @Transactional("transactionManager")
    public CsaladfaOsszegzesDto importalas(InputStream gedcom, String nev, String tulajdonos) {
        Csaladfa fa = grafEpitoService.importalas(gedcom, nev, tulajdonos);
        long egyezesDb = utkozesDetektalo.keresesFaban(fa.getId());
        return CsaladfaOsszegzesDto.bol(fa, egyezesDb);
    }

    // Saját Cypher a listázáshoz, hogy ne kelljen minden fát a teljes tartalmával betölteni
    public List<CsaladfaOsszegzesDto> osszegzesek() {
        return List.copyOf(neo4jClient.query("""
                        MATCH (f:Csaladfa)
                        RETURN id(f) AS id, f.nev AS nev, f.tulajdonos AS tulajdonos, f.importIdo AS importIdo,
                               size([(f)-[:TARTALMAZ]->(sz:Szemely) | sz]) AS szemelyDb,
                               size([(f)-[:TARTALMAZ]->(cs:Csalad) | cs]) AS csaladDb,
                               size([(f)-[:TARTALMAZ]->(:Szemely)-[e:LEHETSEGES_EGYEZES]-() | e]) AS egyezesDb
                        ORDER BY id""")
                .fetchAs(CsaladfaOsszegzesDto.class)
                .mappedBy((tipusok, sor) -> new CsaladfaOsszegzesDto(
                        sor.get("id").asLong(),
                        sor.get("nev").asString(null),
                        sor.get("tulajdonos").asString(null),
                        sor.get("importIdo").asLocalDateTime(null),
                        sor.get("szemelyDb").asLong(),
                        sor.get("csaladDb").asLong(),
                        sor.get("egyezesDb").asLong()))
                .all());
    }

    public CsaladfaGrafDto graf(Long id) {
        return CsaladfaGrafDto.bol(betoltes(id));
    }

    public String export(Long id) {
        return exportService.exportalas(betoltes(id));
    }

    // A fa és minden, amit tartalmaz (személyek, családok, fotók) törlése; a DETACH DELETE a címkéket
    // és a más fákkal talált egyezéseket is viszi. A fotófájlok a sikeres törlés után kerülnek le a lemezről.
    public void torles(Long id) {
        TorlesEredmeny eredmeny = neo4jClient.query("""
                        MATCH (f:Csaladfa) WHERE id(f) = $id
                        OPTIONAL MATCH (f)-[:TARTALMAZ]->(n)
                        WITH f, collect(n) AS tartalom
                        WITH f, tartalom, [k IN tartalom WHERE k:Kep | k.fajlNev] AS fajlok
                        FOREACH (n IN tartalom | DETACH DELETE n)
                        DETACH DELETE f
                        RETURN fajlok""")
                .bind(id).to("id")
                .fetchAs(TorlesEredmeny.class)
                .mappedBy((tipusok, sor) -> new TorlesEredmeny(sor.get("fajlok").asList(Value::asString)))
                .one()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nincs ilyen családfa: " + id));
        eredmeny.fajlok().forEach(kepTarolo::torles);
    }

    private record TorlesEredmeny(List<String> fajlok) {
    }

    private Csaladfa betoltes(Long id) {
        return csaladfaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nincs ilyen családfa: " + id));
    }
}
