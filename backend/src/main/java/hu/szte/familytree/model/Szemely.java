package hu.szte.familytree.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import java.util.List;

@Node("Szemely")
public class Szemely {
    @Id @GeneratedValue
    private Long id;
    
    private String keresztnev;
    private String vezeteknev;
    private String szuletesiDatum;

    // Kapcsolat a másik csomóponthoz (gyermek)
    @Relationship(type = "GYERMEKE", direction = Relationship.Direction.OUTGOING)
    private List<Szemely> gyermekek;

    // Kapcsolat a gazdag éllel (kép koordináták)
    @Relationship(type = "SZEREPEL_RAJTA")
    private List<SzerepelRajta> szereplesekKepeken;

    // Getterek és Setterek...
}