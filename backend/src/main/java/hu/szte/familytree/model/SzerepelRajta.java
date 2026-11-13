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

    public SzerepelRajta() {
    }

    public SzerepelRajta(Kep kep, int xPozicio, int yPozicio, int szelesseg, int magassag) {
        this.kep = kep;
        this.xPozicio = xPozicio;
        this.yPozicio = yPozicio;
        this.szelesseg = szelesseg;
        this.magassag = magassag;
    }

    public Long getId() {
        return id;
    }

    public int getxPozicio() {
        return xPozicio;
    }

    public void setxPozicio(int xPozicio) {
        this.xPozicio = xPozicio;
    }

    public int getyPozicio() {
        return yPozicio;
    }

    public void setyPozicio(int yPozicio) {
        this.yPozicio = yPozicio;
    }

    public int getSzelesseg() {
        return szelesseg;
    }

    public void setSzelesseg(int szelesseg) {
        this.szelesseg = szelesseg;
    }

    public int getMagassag() {
        return magassag;
    }

    public void setMagassag(int magassag) {
        this.magassag = magassag;
    }

    public Kep getKep() {
        return kep;
    }

    public void setKep(Kep kep) {
        this.kep = kep;
    }
}
