package hu.szte.familytree.model;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class SzerepelRajta {
    @RelationshipId
    private Long id;

    // A kép-taggelés kerete közvetlenül a kapcsolaton, a kép méretéhez viszonyítva (0–1),
    // így a frontend bármilyen méretben kirajzolhatja átszámítás nélkül
    private double xPozicio;
    private double yPozicio;
    private double szelesseg;
    private double magassag;

    // Ha igaz, ez a keret a személy profilképe (a frontend ezt a részt vágja ki a fotóból)
    private boolean profilkep;

    @TargetNode
    private Kep kep;

    public SzerepelRajta() {
    }

    public Long getId() {
        return id;
    }

    public double getxPozicio() {
        return xPozicio;
    }

    public void setxPozicio(double xPozicio) {
        this.xPozicio = xPozicio;
    }

    public double getyPozicio() {
        return yPozicio;
    }

    public void setyPozicio(double yPozicio) {
        this.yPozicio = yPozicio;
    }

    public double getSzelesseg() {
        return szelesseg;
    }

    public void setSzelesseg(double szelesseg) {
        this.szelesseg = szelesseg;
    }

    public double getMagassag() {
        return magassag;
    }

    public void setMagassag(double magassag) {
        this.magassag = magassag;
    }

    public boolean isProfilkep() {
        return profilkep;
    }

    public void setProfilkep(boolean profilkep) {
        this.profilkep = profilkep;
    }

    public Kep getKep() {
        return kep;
    }

    public void setKep(Kep kep) {
        this.kep = kep;
    }
}
