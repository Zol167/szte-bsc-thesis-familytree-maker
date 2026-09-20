package hu.szte.familytree.model;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class SzerepelRajta {
    @RelationshipId
    private Long id;

    // A kép-taggelés koordinátái közvetlenül a kapcsolaton!
    private int xPozicio;
    private int yPozicio;
    private int szelesseg;
    private int magassag;

    @TargetNode
    private Kep kep;

    // Getterek és Setterek...
}