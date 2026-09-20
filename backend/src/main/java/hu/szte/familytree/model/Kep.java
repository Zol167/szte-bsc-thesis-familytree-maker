package hu.szte.familytree.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

@Node("Kep")
public class Kep {
    @Id @GeneratedValue
    private Long id;
    
    private String fajlUtvonal;
    private String feltoltIdo;

    // Getterek és Setterek...
}