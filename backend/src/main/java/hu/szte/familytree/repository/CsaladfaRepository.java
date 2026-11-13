package hu.szte.familytree.repository;

import hu.szte.familytree.model.Csaladfa;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsaladfaRepository extends Neo4jRepository<Csaladfa, Long> {
}
