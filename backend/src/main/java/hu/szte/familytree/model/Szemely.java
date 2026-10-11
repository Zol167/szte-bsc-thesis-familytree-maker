package hu.szte.familytree.model;

import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node("Szemely")
public class Szemely {

    @Id @GeneratedValue
    private Long id;

    @Transient // Ezt a Neo4j nem fogja elmenteni, csak a parser használja
    private String gedcomId;

    private String keresztnev;
    private String vezeteknev;
    private String szuletesiDatum;
    private String halalozasiDatum;

    // Kapcsolat a másik csomóponthoz (gyermek)
    @Relationship(type = "GYERMEKE", direction = Relationship.Direction.OUTGOING)
    private List<Szemely> gyermekek;

    // Kapcsolat a gazdag éllel (kép koordináták)
    @Relationship(type = "SZEREPEL_RAJTA")
    private List<SzerepelRajta> szereplesekKepeken;

    public Szemely() {
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

    public String getKeresztnev() {
        return keresztnev;
    }

    public void setKeresztnev(String keresztnev) {
        this.keresztnev = keresztnev;
    }

    public String getVezeteknev() {
        return vezeteknev;
    }

    public void setVezeteknev(String vezeteknev) {
        this.vezeteknev = vezeteknev;
    }

    public String getSzuletesiDatum() {
        return szuletesiDatum;
    }

    public void setSzuletesiDatum(String szuletesiDatum) {
        this.szuletesiDatum = szuletesiDatum;
    }

    public String getHalalozasiDatum() {
        return halalozasiDatum;
    }

    public void setHalalozasiDatum(String halalozasiDatum) {
        this.halalozasiDatum = halalozasiDatum;
    }

    public List<Szemely> getGyermekek() {
        return gyermekek;
    }

    public void setGyermekek(List<Szemely> gyermekek) {
        this.gyermekek = gyermekek;
    }

    public List<SzerepelRajta> getSzereplesekKepeken() {
        return szereplesekKepeken;
    }

    public void setSzereplesekKepeken(List<SzerepelRajta> szereplesekKepeken) {
        this.szereplesekKepeken = szereplesekKepeken;
    }
}
