package hu.szte.familytree.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

import java.time.LocalDateTime;

// Egy feltöltött fotó. Maga a fájl a lemezen van (familytree.kep-konyvtar), a gráfban csak a
// generált fájlnév és a metaadatok; a családfához a (:Csaladfa)-[:TARTALMAZ]->(:Kep) él köti.
@Node("Kep")
public class Kep {
    @Id @GeneratedValue
    private Long id;

    private String fajlNev; // generált UUID + kiterjesztés, soha nem abszolút útvonal
    private String eredetiNev;
    private String mimeTipus;
    private Integer szelesseg; // pixelben; null, ha a formátumból nem olvasható ki
    private Integer magassag;
    private String leiras;
    private LocalDateTime feltoltIdo;

    public Kep() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFajlNev() {
        return fajlNev;
    }

    public void setFajlNev(String fajlNev) {
        this.fajlNev = fajlNev;
    }

    public String getEredetiNev() {
        return eredetiNev;
    }

    public void setEredetiNev(String eredetiNev) {
        this.eredetiNev = eredetiNev;
    }

    public String getMimeTipus() {
        return mimeTipus;
    }

    public void setMimeTipus(String mimeTipus) {
        this.mimeTipus = mimeTipus;
    }

    public Integer getSzelesseg() {
        return szelesseg;
    }

    public void setSzelesseg(Integer szelesseg) {
        this.szelesseg = szelesseg;
    }

    public Integer getMagassag() {
        return magassag;
    }

    public void setMagassag(Integer magassag) {
        this.magassag = magassag;
    }

    public String getLeiras() {
        return leiras;
    }

    public void setLeiras(String leiras) {
        this.leiras = leiras;
    }

    public LocalDateTime getFeltoltIdo() {
        return feltoltIdo;
    }

    public void setFeltoltIdo(LocalDateTime feltoltIdo) {
        this.feltoltIdo = feltoltIdo;
    }
}
