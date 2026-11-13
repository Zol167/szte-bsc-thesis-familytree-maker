package hu.szte.familytree.service;

import hu.szte.familytree.dto.CsaladfaGrafDto;
import hu.szte.familytree.dto.CsaladfaOsszegzesDto;
import hu.szte.familytree.model.Csaladfa;
import hu.szte.familytree.repository.CsaladfaRepository;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CsaladfaService {

    private final CsaladfaRepository csaladfaRepository;
    private final GedcomExportService exportService;
    private final Neo4jClient neo4jClient;

    public CsaladfaService(CsaladfaRepository csaladfaRepository, GedcomExportService exportService,
                           Neo4jClient neo4jClient) {
        this.csaladfaRepository = csaladfaRepository;
        this.exportService = exportService;
        this.neo4jClient = neo4jClient;
    }

    // Saját Cypher a listázáshoz, hogy ne kelljen minden fát a teljes tartalmával betölteni
    public List<CsaladfaOsszegzesDto> osszegzesek() {
        return List.copyOf(neo4jClient.query("""
                        MATCH (f:Csaladfa)
                        RETURN id(f) AS id, f.nev AS nev, f.tulajdonos AS tulajdonos, f.importIdo AS importIdo,
                               size([(f)-[:TARTALMAZ]->(sz:Szemely) | sz]) AS szemelyDb,
                               size([(f)-[:TARTALMAZ]->(cs:Csalad) | cs]) AS csaladDb
                        ORDER BY id""")
                .fetchAs(CsaladfaOsszegzesDto.class)
                .mappedBy((tipusok, sor) -> new CsaladfaOsszegzesDto(
                        sor.get("id").asLong(),
                        sor.get("nev").asString(null),
                        sor.get("tulajdonos").asString(null),
                        sor.get("importIdo").asLocalDateTime(null),
                        sor.get("szemelyDb").asLong(),
                        sor.get("csaladDb").asLong()))
                .all());
    }

    public CsaladfaGrafDto graf(Long id) {
        return CsaladfaGrafDto.bol(betoltes(id));
    }

    public String export(Long id) {
        return exportService.exportalas(betoltes(id));
    }

    private Csaladfa betoltes(Long id) {
        return csaladfaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nincs ilyen családfa: " + id));
    }
}
