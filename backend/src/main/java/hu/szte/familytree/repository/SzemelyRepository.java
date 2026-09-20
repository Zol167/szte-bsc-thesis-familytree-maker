package hu.szte.familytree.repository;

import hu.szte.familytree.model.Szemely;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SzemelyRepository extends Neo4jRepository<Szemely, Long> {
    // A Spring Data Neo4j automatikusan generálja a lekérdezést a metódusnév alapján!
    List<Szemely> findByKeresztnev(String keresztnev);
}