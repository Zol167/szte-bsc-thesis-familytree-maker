package hu.szte.familytree.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

// A családi kapcsolatok (szülő, gyermek, házastárs) a Csalad csomóponton keresztül élnek,
// így a Szemely entitásból nem indul ki kör, és egy személy betöltése nem húzza be az egész gráfot.
@Node("Szemely")
public class Szemely {

    @Id
    @GeneratedValue
    private Long id;

    private String gedcomId;

    private String keresztnev;
    private String vezeteknev;
    private String nem; // GEDCOM SEX: M, F, U
    private String szuletesiDatum;
    private Integer szuletesiEv; // Az ütközés-detektáláshoz (név + születési év)
    private String halalozasiDatum;

    // Normalizált "vezetéknév|keresztnév|év" kulcs, indexelve: a pontos egyezés indexkereséssé válik
    private String egyezesKulcs;

    // Képek inicializálása
    @Relationship(type = "SZEREPEL_RAJTA")
    private List<SzerepelRajta> szereplesekKepeken = new ArrayList<>();

    public Szemely() {
    }

    // --- Getterek és Setterek ---

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

    public String getNem() {
        return nem;
    }

    public void setNem(String nem) {
        this.nem = nem;
    }

    public String getSzuletesiDatum() {
        return szuletesiDatum;
    }

    public void setSzuletesiDatum(String szuletesiDatum) {
        this.szuletesiDatum = szuletesiDatum;
    }

    public Integer getSzuletesiEv() {
        return szuletesiEv;
    }

    public void setSzuletesiEv(Integer szuletesiEv) {
        this.szuletesiEv = szuletesiEv;
    }

    public String getHalalozasiDatum() {
        return halalozasiDatum;
    }

    public void setHalalozasiDatum(String halalozasiDatum) {
        this.halalozasiDatum = halalozasiDatum;
    }

    public String getEgyezesKulcs() {
        return egyezesKulcs;
    }

    public void setEgyezesKulcs(String egyezesKulcs) {
        this.egyezesKulcs = egyezesKulcs;
    }

    public List<SzerepelRajta> getSzereplesekKepeken() {
        return szereplesekKepeken;
    }

    public void setSzereplesekKepeken(List<SzerepelRajta> szereplesekKepeken) {
        this.szereplesekKepeken = szereplesekKepeken;
    }
}
