package hu.szte.familytree.repository;

import hu.szte.familytree.model.Kep;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KepRepository extends Neo4jRepository<Kep, Long> {
}
