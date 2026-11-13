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

    public Kep() {
    }

    public Kep(String fajlUtvonal, String feltoltIdo) {
        this.fajlUtvonal = fajlUtvonal;
        this.feltoltIdo = feltoltIdo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFajlUtvonal() {
        return fajlUtvonal;
    }

    public void setFajlUtvonal(String fajlUtvonal) {
        this.fajlUtvonal = fajlUtvonal;
    }

    public String getFeltoltIdo() {
        return feltoltIdo;
    }

    public void setFeltoltIdo(String feltoltIdo) {
        this.feltoltIdo = feltoltIdo;
    }
}
