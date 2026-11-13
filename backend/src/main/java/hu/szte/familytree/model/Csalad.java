package hu.szte.familytree.model;

import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

// Egy GEDCOM FAM rekord a gráfban: (:Szemely)-[:SZULO_A]->(:Csalad)-[:GYERMEKE]->(:Szemely)
// Így megmarad, melyik gyermek melyik házasságból született (többszörös házasság).
@Node("Csalad")
public class Csalad {

    @Id
    @GeneratedValue
    private Long id;

    private String gedcomId;
    private String hazassagDatum;

    @Relationship(type = "SZULO_A", direction = Relationship.Direction.INCOMING)
    private List<Szemely> szulok = new ArrayList<>();

    @Relationship(type = "GYERMEKE", direction = Relationship.Direction.OUTGOING)
    private List<Szemely> gyermekek = new ArrayList<>();

    // A parser által beolvasott nyers GEDCOM hivatkozások, ezeket a GrafEpitoService oldja fel
    @Transient
    private String husbId;
    @Transient
    private String wifeId;
    @Transient
    private List<String> chilIds = new ArrayList<>();

    public Csalad() {
    }

    public Csalad(String gedcomId) {
        this.gedcomId = gedcomId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGedcomId() {
        return gedcomId;
    }

    public void setGedcomId(String gedcomId) {
        this.gedcomId = gedcomId;
    }

    public String getHazassagDatum() {
        return hazassagDatum;
    }

    public void setHazassagDatum(String hazassagDatum) {
        this.hazassagDatum = hazassagDatum;
    }

    public List<Szemely> getSzulok() {
        return szulok;
    }

    public void setSzulok(List<Szemely> szulok) {
        this.szulok = szulok;
    }

    public List<Szemely> getGyermekek() {
        return gyermekek;
    }

    public void setGyermekek(List<Szemely> gyermekek) {
        this.gyermekek = gyermekek;
    }

    public String getHusbId() {
        return husbId;
    }

    public void setHusbId(String husbId) {
        this.husbId = husbId;
    }

    public String getWifeId() {
        return wifeId;
    }

    public void setWifeId(String wifeId) {
        this.wifeId = wifeId;
    }

    public List<String> getChilIds() {
        return chilIds;
    }

    public void setChilIds(List<String> chilIds) {
        this.chilIds = chilIds;
    }

    public void addChilId(String chilId) {
        if (!this.chilIds.contains(chilId)) {
            this.chilIds.add(chilId);
        }
    }
}
