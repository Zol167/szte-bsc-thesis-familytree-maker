package hu.szte.familytree.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Egy felhasználó egy importált családfája; ez választja el a különböző felhasználók
// adatait (a GEDCOM azonosítók, pl. @I1@, fájlonként ismétlődnek).
@Node("Csaladfa")
public class Csaladfa {

    @Id
    @GeneratedValue
    private Long id;

    private String nev;
    private String tulajdonos;
    private LocalDateTime importIdo;

    @Relationship(type = "TARTALMAZ")
    private List<Szemely> szemelyek = new ArrayList<>();

    @Relationship(type = "TARTALMAZ")
    private List<Csalad> csaladok = new ArrayList<>();

    public Csaladfa() {
    }

    public Csaladfa(String nev, String tulajdonos) {
        this.nev = nev;
        this.tulajdonos = tulajdonos;
        this.importIdo = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNev() {
        return nev;
    }

    public void setNev(String nev) {
        this.nev = nev;
    }

    public String getTulajdonos() {
        return tulajdonos;
    }

    public void setTulajdonos(String tulajdonos) {
        this.tulajdonos = tulajdonos;
    }

    public LocalDateTime getImportIdo() {
        return importIdo;
    }

    public void setImportIdo(LocalDateTime importIdo) {
        this.importIdo = importIdo;
    }

    public List<Szemely> getSzemelyek() {
        return szemelyek;
    }

    public void setSzemelyek(List<Szemely> szemelyek) {
        this.szemelyek = szemelyek;
    }

    public List<Csalad> getCsaladok() {
        return csaladok;
    }

    public void setCsaladok(List<Csalad> csaladok) {
        this.csaladok = csaladok;
    }
}
