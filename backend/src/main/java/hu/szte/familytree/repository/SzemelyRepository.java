package hu.szte.familytree.repository;

import hu.szte.familytree.model.Szemely;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

@Repository
public interface SzemelyRepository extends Neo4jRepository<Szemely, Long> {

    // Saját Cypher lekérdezés: keresés a kereszt- és vezetéknévben (case-insensitive részleges egyezés)
    @Query("MATCH (sz:Szemely) WHERE toLower(sz.keresztnev) CONTAINS toLower($nev) "
            + "OR toLower(sz.vezeteknev) CONTAINS toLower($nev) RETURN sz")
    List<Szemely> keresesNevAlapjan(String nev);

    // Változó hosszúságú út: Szemely -SZULO_A-> Csalad -GYERMEKE-> Szemely -SZULO_A-> ...
    // Relációs adatbázisban ez rekurzív CTE lenne, itt egyetlen minta.
    @Query("MATCH (os:Szemely)-[:SZULO_A|GYERMEKE*2..]->(sz:Szemely) WHERE id(sz) = $id RETURN DISTINCT os")
    List<Szemely> osok(Long id);

    @Query("MATCH (sz:Szemely)-[:SZULO_A|GYERMEKE*2..]->(utod:Szemely) WHERE id(sz) = $id RETURN DISTINCT utod")
    List<Szemely> leszarmazottak(Long id);
}
